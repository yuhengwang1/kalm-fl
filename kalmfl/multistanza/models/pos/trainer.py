"""
A trainer class to handle training and testing of models.
"""

import sys
import logging
import torch
from torch import nn

from kalmfl.multistanza.models.common.trainer import Trainer as BaseTrainer
from kalmfl.multistanza.models.common import utils, loss
from kalmfl.multistanza.models.pos.model import Tagger
from kalmfl.multistanza.models.pos.vocab import MultiVocab

from kalmfl.multistanza.models.pos.NextBest import NextBest

def get_indices(item, recurse=0):
    def helper(item, index):
        if type(item) is list or type(item) is tuple:
            cur_recurse = 0
            if recurse < len(item):
                cur_recurse = recurse
            next_item = item[cur_recurse]
            index.append(len(item))
            return helper(next_item, index)
        else:
            return index

    return helper(item, [])


logger = logging.getLogger('stanza')

def unpack_batch(batch, use_cuda):
    """ Unpack a batch from the data loader. """
    if use_cuda:
        inputs = [b.cuda() if b is not None else None for b in batch[:8]]
    else:
        inputs = batch[:8]
    orig_idx = batch[8]
    word_orig_idx = batch[9]
    sentlens = batch[10]
    wordlens = batch[11]
    return inputs, orig_idx, word_orig_idx, sentlens, wordlens

class Trainer(BaseTrainer):
    """ A trainer for training models. """
    def __init__(
            self, 
            args=None, 
            vocab=None, 
            pretrain=None, 
            model_file=None, 
            use_cuda=False, 
            n_upos_preds=1,
            n_xpos_preds=1,
            n_feats_preds=1
        ):
        self.use_cuda = use_cuda
        if model_file is not None:
            # load everything from file
            self.load(model_file, pretrain)
        else:
            # build model from scratch
            self.args = args
            self.vocab = vocab
            self.model = Tagger(args, vocab, emb_matrix=pretrain.emb if pretrain is not None else None, share_hid=args['share_hid'])
        self.parameters = [p for p in self.model.parameters() if p.requires_grad]
        if self.use_cuda:
            self.model.cuda()
        else:
            self.model.cpu()
        self.optimizer = utils.get_optimizer(self.args['optim'], self.parameters, self.args['lr'], betas=(0.9, self.args['beta2']), eps=1e-6)
        self.n_upos_preds = n_upos_preds
        self.n_xpos_preds = n_xpos_preds
        self.n_feats_preds = n_feats_preds

    def update(self, batch, eval=False):
        inputs, orig_idx, word_orig_idx, sentlens, wordlens = unpack_batch(batch, self.use_cuda)
        word, word_mask, wordchars, wordchars_mask, upos, xpos, ufeats, pretrained = inputs

        if eval:
            self.model.eval()
        else:
            self.model.train()
            self.optimizer.zero_grad()
        loss, _ = self.model(word, word_mask, wordchars, wordchars_mask, upos, xpos, ufeats, pretrained, word_orig_idx, sentlens, wordlens)
        loss_val = loss.data.item()
        if eval:
            return loss_val

        loss.backward()
        torch.nn.utils.clip_grad_norm_(self.model.parameters(), self.args['max_grad_norm'])
        self.optimizer.step()
        return loss_val

    def predict(self, batch, unsort=True):
        inputs, orig_idx, word_orig_idx, sentlens, wordlens = unpack_batch(batch, self.use_cuda)
        word, word_mask, wordchars, wordchars_mask, upos, xpos, ufeats, pretrained = inputs

        self.model.eval()
        batch_size = word.size(0)
        _, preds = self.model(word, word_mask, wordchars, wordchars_mask, upos, xpos, ufeats, pretrained, word_orig_idx, sentlens, wordlens)
        def unmap(feat, sent, k):
            def itemize(tensor):
                return (elem.item() for elem in tensor)
                
            def zipper(sent, score):
                unmapped_sent = self.vocab[feat].unmap(sent)
                return tuple(zip(self.vocab[feat].unmap(sent), itemize(score)))

            with torch.no_grad():
                sent = sent.transpose(0, 1) # Bring score to the front
                scores, indices = torch.topk(sent, min(k, sent.shape[0]), dim=0)
                # scores is (n, k)
                # unmap(tuple(indices[:, 0])

                return tuple(zipper(sentk, scorek) for sentk, scorek in zip(indices, scores))

        # TODO: for feats, generate the k-best feats
        # Theere's probably a smarter way to do this
        def unmap_ufeats(sent, k):
            def unmap(sent):
                scores = tuple(map(lambda x: x[0].item(), sent))
                features = tuple(map(lambda x: x[1], sent))
                features = self.vocab['feats'].unmap(features)
                return tuple(zip(features, scores))


            with torch.no_grad():
                sent = sent.transpose(0, 1) # Bring score to the front
                scores, indices = torch.topk(sent, min(k, sent.shape[0]), dim=0)
                nb = iter(NextBest(scores, indices))
                return tuple(unmap(next(nb)) for i in range(k))

        upos_seqs = [unmap('upos', sent, self.n_upos_preds) for sent in preds[0]]
        xpos_seqs = [unmap('xpos', sent, self.n_xpos_preds) for sent in preds[1]]
        feats_seqs = [unmap_ufeats(sent, self.n_feats_preds) for sent in preds[2]]

        # Tokens
        # Indices: ijk
        # i: n_pred 
        # j: n_sentence
        # k: n_word

        upos_tokens = [[[upos_seqs[i][k][j] for j in range(sentlens[i])] for i in range(batch_size)] for k in range(len(upos_seqs[0]))]
        xpos_tokens = [[[xpos_seqs[i][k][j] for j in range(sentlens[i])] for i in range(batch_size)] for k in range(len(xpos_seqs[0]))]
        feats_tokens = [[[feats_seqs[i][k][j] for j in range(sentlens[i])] for i in range(batch_size)] for k in range(len(feats_seqs[0]))]
        if unsort:
            #pred_tokens = utils.unsort(pred_tokens, orig_idx)
            # TODO: check if this is valid
            upos_tokens = [utils.unsort(sent, orig_idx) for sent in upos_tokens]
            xpos_tokens = [utils.unsort(sent, orig_idx) for sent in xpos_tokens]
            feats_tokens = [utils.unsort(sent, orig_idx) for sent in feats_tokens]
        
        return upos_tokens, xpos_tokens, feats_tokens

    def save(self, filename, skip_modules=True):
        model_state = self.model.state_dict()
        # skip saving modules like pretrained embeddings, because they are large and will be saved in a separate file
        if skip_modules:
            skipped = [k for k in model_state.keys() if k.split('.')[0] in self.model.unsaved_modules]
            for k in skipped:
                del model_state[k]
        params = {
                'model': model_state,
                'vocab': self.vocab.state_dict(),
                'config': self.args
                }
        try:
            torch.save(params, filename, _use_new_zipfile_serialization=False)
            logger.info("Model saved to {}".format(filename))
        except (KeyboardInterrupt, SystemExit):
            raise
        except Exception as e:
            logger.warning(f"Saving failed... {e} continuing anyway.")

    def load(self, filename, pretrain):
        """
        Load a model from file, with preloaded pretrain embeddings. Here we allow the pretrain to be None or a dummy input,
        and the actual use of pretrain embeddings will depend on the boolean config "pretrain" in the loaded args.
        """
        try:
            checkpoint = torch.load(filename, lambda storage, loc: storage)
        except BaseException:
            logger.error("Cannot load model from {}".format(filename))
            raise
        self.args = checkpoint['config']
        self.vocab = MultiVocab.load_state_dict(checkpoint['vocab'])
        # load model
        emb_matrix = None
        if self.args['pretrain'] and pretrain is not None: # we use pretrain only if args['pretrain'] == True and pretrain is not None
            emb_matrix = pretrain.emb
        self.model = Tagger(self.args, self.vocab, emb_matrix=emb_matrix, share_hid=self.args['share_hid'])
        self.model.load_state_dict(checkpoint['model'], strict=False)
