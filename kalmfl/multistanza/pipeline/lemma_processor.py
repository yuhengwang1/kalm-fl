"""
Processor for performing lemmatization
"""

from kalmfl.multistanza.models.common import doc
from kalmfl.multistanza.models.lemma.data import DataLoader
from kalmfl.multistanza.models.lemma.trainer import Trainer
from kalmfl.multistanza.pipeline._constants import *
from kalmfl.multistanza.pipeline.processor import UDProcessor, register_processor
import itertools
import heapq

class NextBest:
    # We want to return the next-best overall prediction
    # For a single sentence out of all of them, we modify the lemma
    def __init__(self, preds, scores):
        # preds: (n_pred, n_word)
        # score: (n_pred, n_word)
        self._preds = preds
        self._scores = scores
        self._n_preds = len(self._preds)
        self._n_words = len(self._preds[0])

    def _access(self, index):
        # index: (nwords)
        # each entry is < n_preds
        return [self._preds[idx][i] for i, idx in enumerate(index)]

    def _score(self, index):
        # Simple summation across all scores and all features

        score = sum(self._scores[idx][i] for i, idx in enumerate(index))
        return score.item()

    def __iter__(self):
        # What does an index look like?
        # index is (n_sentence, n_word)
        # Use simple summation to combine score across features
        if self._n_words == 0:
            return iter(((1, []),))

        self._seen = set()
        start_index = (0,) * self._n_words
        score = self._score(start_index)
        self._tie_counter = 0
        tc = self._tie_counter
        self._tie_counter += 1
        self._queue = [(-score, tc, start_index)]
        self._seen.add(start_index)

        return self

    def __next__(self):
        if len(self._queue) == 0:
            raise StopIteration

        score_ret, _, index_ret = heapq.heappop(self._queue)
        for i in range(self._n_words):
            if index_ret[i] + 1 >= self._n_preds:
                continue

            new_index = list(index_ret)
            new_index[i] += 1
            new_index = tuple(new_index)

            if new_index in self._seen:
                continue

            self._seen.add(new_index)
            new_score = self._score(new_index)
            tc = self._tie_counter
            self._tie_counter += 1
            heapq.heappush(self._queue, (-new_score, tc, new_index))

        return -score_ret, self._access(index_ret)

@register_processor(name=LEMMA)
class LemmaProcessor(UDProcessor):

    # set of processor requirements this processor fulfills
    PROVIDES_DEFAULT = set([LEMMA])
    # set of processor requirements for this processor
    # pos will be added later for non-identity lemmatizerx
    REQUIRES_DEFAULT = set([TOKENIZE])
    # default batch size
    DEFAULT_BATCH_SIZE = 5000

    def __init__(self, config, pipeline, use_gpu):
        # run lemmatizer in identity mode
        self._use_identity = None
        self._pretagged = None
        self._n_preds = config.get('n_preds', 3)
        super().__init__(config, pipeline, use_gpu)

    @property
    def use_identity(self):
        return self._use_identity

    def _set_up_model(self, config, use_gpu):
        if config.get('use_identity') in ['True', True]:
            self._use_identity = True
            self._config = config
            self.config['batch_size'] = LemmaProcessor.DEFAULT_BATCH_SIZE
        else:
            self._use_identity = False
            self._trainer = Trainer(model_file=config['model_path'], use_cuda=use_gpu, n_preds=self._n_preds)

    def _set_up_requires(self):
        self._pretagged = self._config.get('pretagged', None)
        if self._pretagged:
            self._requires = set()
        elif self.config.get('pos') and not self.use_identity:
            self._requires = LemmaProcessor.REQUIRES_DEFAULT.union(set([POS]))
        else:
            self._requires = LemmaProcessor.REQUIRES_DEFAULT

    def process(self, document):
        if not self.use_identity:
            batch = DataLoader(document, self.config['batch_size'], self.config, vocab=self.vocab, evaluation=True)
        else:
            batch = DataLoader(document, self.config['batch_size'], self.config, evaluation=True, conll_only=True)
        if self.use_identity:
            preds = [[word.text for sent in batch.doc.sentences for word in sent.words]]
        elif self.config.get('dict_only', False):
            preds = [self.trainer.predict_dict(batch.doc.get([doc.TEXT, doc.UPOS]))]
        else:
            if self.config.get('ensemble_dict', False):
                # skip the seq2seq model when we can
                skip = self.trainer.skip_seq2seq(batch.doc.get([doc.TEXT, doc.UPOS]))
                seq2seq_batch = DataLoader(document, self.config['batch_size'], self.config, vocab=self.vocab,
                                           evaluation=True, skip=skip)
            else:
                seq2seq_batch = batch

            preds = []
            edits = []
            scores = []
            for i, b in enumerate(seq2seq_batch):
                ps, ss, es = self.trainer.predict(b, self.config['beam_size'])
                preds.append(ps)
                scores.append(ss)
                if es is not None:
                    edits += es

            # Rearrange to be (n_pred, document)
            rearranged_preds = []
            rearranged_scores = []
            for i in range(self._n_preds):
                pred = []
                score = []
                for pred_group, score_group  in zip(preds, scores):
                    pred.extend(pred_group[i])
                    score.extend(score_group[i])

                rearranged_preds.append(pred)
                rearranged_scores.append(score)

            preds = rearranged_preds
            scores = rearranged_scores

            # TODO: check if this is OK? This would mean that we skipped everything
            preds = preds if len(preds) > 0 else [[]]

            total_scores = []
            complete_preds = []
            if self.config.get('ensemble_dict', False):
                best_preds = itertools.islice(NextBest(preds, scores), self._n_preds)
                for score, pred in best_preds:
                    pred = self.trainer.postprocess([x for x, y in zip(batch.doc.get([doc.TEXT]), skip) if not y], pred, edits=edits)
                    # expand seq2seq predictions to the same size as all words
                    i = 0
                    preds1 = []
                    for s in skip:
                        if s:
                            preds1.append('')
                        else:
                            preds1.append(pred[i])
                            i += 1
                    pred = self.trainer.ensemble(batch.doc.get([doc.TEXT, doc.UPOS]), preds1)
                    complete_preds.append(pred)
                    total_scores.append(score)
            else:
                best_preds = itertools.islice(NextBest(preds, scores), self._n_preds)
                for score, pred in best_preds:
                    pred = self.trainer.postprocess(batch.doc.get([doc.TEXT]), pred, edits=edits)
                    complete_preds.append(pred)
                    total_scores.append(score)

        # map empty string lemmas to '_'
        docs = []
        serialized = batch.doc.to_serialized()
        for score, pred in zip(total_scores, complete_preds):
            copy = doc.Document.from_serialized(serialized)
            pred = [max([(len(x), x), (0, '_')])[1] for x in pred]
            copy.set([doc.LEMMA], pred)
            copy.set([doc.LEMMA_SCORE], [score], to_document=True)
            docs.append(copy)

        return tuple(docs)
