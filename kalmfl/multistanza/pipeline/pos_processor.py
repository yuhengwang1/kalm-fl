"""
Processor for performing part-of-speech tagging
"""

from kalmfl.multistanza.models.common import doc
from kalmfl.multistanza.models.common.pretrain import Pretrain
from kalmfl.multistanza.models.common.utils import unsort
from kalmfl.multistanza.models.pos.data import DataLoader
from kalmfl.multistanza.models.pos.trainer import Trainer
from kalmfl.multistanza.pipeline._constants import *
from kalmfl.multistanza.pipeline.processor import UDProcessor, register_processor
import heapq
import itertools
from functools import reduce
from kalmfl.multistanza.models.common.levels import Level
from collections import namedtuple

@register_processor(name=POS)
class POSProcessor(UDProcessor):
    # set of processor requirements this processor fulfills
    PROVIDES_DEFAULT = set([POS])
    # set of processor requirements for this processor
    REQUIRES_DEFAULT = set([TOKENIZE])

    def _set_up_model(self, config, use_gpu):
        # get pretrained word vectors
        self._pretrain = Pretrain(config['pretrain_path']) if 'pretrain_path' in config else None
        self._n_preds = config.get('n_preds', 1)
        self._trainer_n_upos_preds = config.get('trainer_n_upos_preds', 21) # Stanza generates 21 XPOS tags
        self._trainer_n_xpos_preds = config.get('trainer_n_xpos_preds', 54) # Stanza generates 54 XPOS tags
        self._trainer_n_feats_preds = config.get('trainer_n_feats_preds', 1) # Feature predictions are expensive and once we change the POS tag they often don't match
        # set up trainer
        self._trainer = Trainer(
                pretrain=self.pretrain, 
                model_file=config['model_path'], 
                use_cuda=use_gpu, 
                n_upos_preds=self._trainer_n_upos_preds,
                n_xpos_preds=self._trainer_n_xpos_preds,
                n_feats_preds=self._trainer_n_feats_preds
            )

        self._level = config.get('level', Level.WORD)
        self._next_upos = config.get('next_upos', lambda context: context.upos_index + 1)
        self._next_xpos = config.get('next_xpos', lambda context: context.xpos_index + 1)
        self._next_feats = config.get('next_feats', lambda context: context.feats_index + 1)
        self._scorer = config.get('scorer', lambda ups, xps, ufs: (ups + xps + ufs) / 3)

    def process(self, document):
        batch = DataLoader(
            document, self.config['batch_size'], self.config, self.pretrain, vocab=self.vocab, evaluation=True,
            sort_during_eval=True)
        upos_preds = []
        xpos_preds = []
        feats_preds = []
        for i, b in enumerate(batch):
            upos, xpos, feats = self.trainer.predict(b)
            upos_preds.append(upos)
            xpos_preds.append(xpos)
            feats_preds.append(feats)

        # n_batch, n_pred, n_sentence, n_word, feature

        # Get rid of batch
        merged_preds = []
        n_preds = min(self._trainer_n_upos_preds, len(upos_preds[0]))
        for i in range(n_preds):
            pred = []
            for pred_minibatch in upos_preds:
                pred.extend(pred_minibatch[i])
            merged_preds.append(pred)
        upos_preds = merged_preds

        merged_preds = []
        n_preds = min(self._trainer_n_xpos_preds, len(xpos_preds[0]))
        for i in range(n_preds):
            pred = []
            for pred_minibatch in xpos_preds:
                pred.extend(pred_minibatch[i])
            merged_preds.append(pred)
        xpos_preds = merged_preds

        merged_preds = []
        n_preds = min(self._trainer_n_feats_preds, len(feats_preds[0]))
        for i in range(n_preds):
            pred = []
            for pred_minibatch in feats_preds:
                pred.extend(pred_minibatch[i])
            merged_preds.append(pred)
        feats_preds = merged_preds

        docs = []
        scores = []
        serialized = batch.doc.to_serialized()
        nb = NextBest(upos_preds, xpos_preds, feats_preds, self._level, self._scorer, self._next_upos, self._next_xpos, self._next_feats)
        for score, word_scores, pred in itertools.islice(nb, self._n_preds):
            copy = doc.Document.from_serialized(serialized)
            # pred should be (n_sent, n_word, n_feature)
            pred = unsort(pred, batch.data_orig_idx)
            copy.set([doc.UPOS, doc.XPOS, doc.FEATS], [y for x in pred for y in x])
            copy.set([doc.UPOS_SCORE, doc.XPOS_SCORE, doc.FEATS_SCORE], [y for x in word_scores for y in x])
            copy.set([doc.POS_SCORE], [score], to_document=True)
            docs.append(copy)
            scores.append(score)

        return tuple(docs) #, scores TODO: return scores eventually

class NextBest:
    def __init__(self, upos_preds, xpos_preds, feats_preds, level, scorer, next_upos, next_xpos, next_feats):
        # preds: (n_pred, n_sentence, n_word, feature, score)
        self._n_upos_preds = len(upos_preds)
        self._n_xpos_preds = len(xpos_preds)
        self._n_feats_preds = len(feats_preds)
        self._n_sents = len(upos_preds[0])
        self._upos = upos_preds
        self._xpos = xpos_preds
        self._feats = feats_preds
        self._scorer = scorer
        self._next_upos = next_upos
        self._next_xpos = next_xpos
        self._next_feats = next_feats
        self._level = level

    @staticmethod
    def _pack_features(upos, xpos, feats):
        # Expects extracted upos, xpos, feats from _accesss
        # Should NOT have score index
        return [[(upos[i][j], xpos[i][j], feats[i][j]) for j in range(len(upos[i]))] for i in range(len(upos))]

    def _access(self, feat, index, score=False):
        si = 1 if score else 0
        empty_feature = [None, 0.0]
        return [[feat[index[i][j]][i][j][si] if index[i][j] >= 0 else empty_feature[si] for j in range(len(index[i]))] for i in range(len(index))]

    def _score(self, upos_index, xpos_index, feats_index):
        score = 0
        total_lengths = 0
        upos = self._access(self._upos, upos_index, score=True)
        xpos = self._access(self._xpos, xpos_index, score=True)
        feats = self._access(self._feats, feats_index, score=True)
        feats = self._pack_features(upos, xpos, feats)
        for sent in feats:
            score += sum(map(lambda x: self._scorer(*x), sent))
            total_lengths += len(sent)

        score /= total_lengths

        return score

    def __iter__(self):
        # What does an index look like?
        # index is (n_sentence, n_word)
        # Use simple summation to combine score across features
        self._seen = set()
        start_index = tuple(tuple(0 for i in range(len(sent))) for sent in self._upos[0])
        score = self._score(start_index, start_index, start_index)
        self._tie_counter = 0
        tc = self._tie_counter
        self._tie_counter += 1
        self._queue = [(-score, tc, start_index, start_index, start_index)]
        self._seen.add((start_index, start_index, start_index))

        return self

    def __next__(self):
        if len(self._queue) == 0:
            raise StopIteration

        score_ret, _, upos_index_ret, xpos_index_ret, feats_index_ret = heapq.heappop(self._queue)
        if self._level == Level.DOCUMENT:
            new_upos_index = [list(sent) for sent in upos_index_ret]
            new_xpos_index = [list(sent) for sent in xpos_index_ret]
            new_feats_index = [list(sent) for sent in feats_index_ret]

        for i in range(self._n_sents):
            n_words = len(self._upos[0][i])
            if self._level == Level.SENTENCE:
                new_upos_index = [list(sent) for sent in upos_index_ret]
                new_xpos_index = [list(sent) for sent in xpos_index_ret]
                new_feats_index = [list(sent) for sent in feats_index_ret]

            for j in range(n_words):
                context = POSContext(
                        next_upos=self._next_upos,
                        next_xpos=self._next_xpos,
                        next_feats=self._next_feats,
                        scorer=self._scorer,
                        upos=[self._upos[k][i][j] for k in range(len(self._upos))],
                        xpos=[self._xpos[k][i][j] for k in range(len(self._xpos))],
                        feats=[self._feats[k][i][j] for k in range(len(self._feats))],
                        word_index=(i, j),
                        upos_index=upos_index_ret[i][j],
                        xpos_index=xpos_index_ret[i][j],
                        feats_index=feats_index_ret[i][j]
                )

                next_upos = self._next_upos(context)
                next_xpos = self._next_xpos(context)
                next_feats = self._next_feats(context)

                if self._level == Level.WORD:
                    new_upos_index = [list(sent) for sent in upos_index_ret]
                    new_xpos_index = [list(sent) for sent in xpos_index_ret]
                    new_feats_index = [list(sent) for sent in feats_index_ret]

                if next_upos < self._n_upos_preds:
                    new_upos_index[i][j] = next_upos

                if next_xpos < self._n_xpos_preds:
                    new_xpos_index[i][j] = next_xpos

                if next_feats < self._n_feats_preds:
                    new_feats_index[i][j] = next_feats

                if self._level == Level.WORD:
                    self._handle_potential_new_index(new_upos_index, new_xpos_index, new_feats_index)

            if self._level == Level.SENTENCE:
                self._handle_potential_new_index(new_upos_index, new_xpos_index, new_feats_index)

        if self._level == Level.DOCUMENT:
            self._handle_potential_new_index(new_upos_index, new_xpos_index, new_feats_index)


        upos_ret = self._access(self._upos, upos_index_ret)
        xpos_ret = self._access(self._xpos, xpos_index_ret)
        feats_ret = self._access(self._feats, feats_index_ret)

        upos_score_ret = self._access(self._upos, upos_index_ret, score=True)
        xpos_score_ret = self._access(self._xpos, xpos_index_ret, score=True)
        feats_score_ret = self._access(self._feats, feats_index_ret, score=True)

        word_scores = self._pack_features(upos_score_ret, xpos_score_ret, feats_score_ret)
        features = self._pack_features(upos_ret, xpos_ret, feats_ret)

        return -score_ret, word_scores, features

    def _handle_potential_new_index(self, new_upos_index, new_xpos_index, new_feats_index):
        new_upos_index = tuple(tuple(sent) for sent in new_upos_index)
        new_xpos_index = tuple(tuple(sent) for sent in new_xpos_index)
        new_feats_index = tuple(tuple(sent) for sent in new_feats_index)

        new_index = (new_upos_index, new_xpos_index, new_feats_index)
        if new_index in self._seen:
            return

        self._seen.add(new_index)
        new_score = self._score(new_upos_index, new_xpos_index, new_feats_index)
        tc = self._tie_counter
        self._tie_counter += 1
        heapq.heappush(self._queue, (-new_score, tc, new_upos_index, new_xpos_index, new_feats_index))

POSContext = namedtuple('POSContext', 'next_upos next_xpos next_feats scorer upos xpos feats upos_index xpos_index feats_index word_index')
