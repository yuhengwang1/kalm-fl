"""
Processor for performing dependency parsing
"""

from kalmfl.multistanza.models.common import doc
from kalmfl.multistanza.models.common.pretrain import Pretrain
from kalmfl.multistanza.models.common.utils import unsort
from kalmfl.multistanza.models.depparse.data import DataLoader
from kalmfl.multistanza.models.depparse.trainer import Trainer
from kalmfl.multistanza.pipeline._constants import *
from kalmfl.multistanza.pipeline.processor import UDProcessor, register_processor

import itertools
import heapq

DEFAULT_SEPARATE_BATCH=150

@register_processor(name=DEPPARSE)
class DepparseProcessor(UDProcessor):

    # set of processor requirements this processor fulfills
    PROVIDES_DEFAULT = set([DEPPARSE])
    # set of processor requirements for this processor
    REQUIRES_DEFAULT = set([TOKENIZE, POS, LEMMA])

    def __init__(self, config, pipeline, use_gpu):
        self._pretagged = None
        self._n_preds = config.get('n_preds', 3)
        super().__init__(config, pipeline, use_gpu)

    def _set_up_requires(self):
        self._pretagged = self._config.get('pretagged')
        if self._pretagged:
            self._requires = set()
        else:
            self._requires = self.__class__.REQUIRES_DEFAULT

    def _set_up_model(self, config, use_gpu):
        self._pretrain = Pretrain(config['pretrain_path']) if 'pretrain_path' in config else None
        self._trainer = Trainer(pretrain=self.pretrain, model_file=config['model_path'], use_cuda=use_gpu, n_preds=self._n_preds)

    def process(self, document):
        try:
            batch = DataLoader(document, self.config['batch_size'], self.config, self.pretrain, vocab=self.vocab, evaluation=True,
                               sort_during_eval=self.config.get('sort_during_eval', True),
                               min_length_to_batch_separately=self.config.get('min_length_to_batch_separately', DEFAULT_SEPARATE_BATCH))
            preds = []
            scores = []
            for i, b in enumerate(batch):
                pred, score = self.trainer.predict(b)
                preds.extend(pred)
                scores.extend(score)

            if batch.data_orig_idx is not None:
                preds = unsort(preds, batch.data_orig_idx)

            docs = []
            serialized = batch.doc.to_serialized()
            best_preds = itertools.islice(NextBest(preds, scores), self._n_preds)
            best_scores = []
            for score, pred in best_preds:
                best_scores.append(score)
                copy = doc.Document.from_serialized(serialized)
                copy.set([doc.HEAD, doc.DEPREL], [y for x in pred for y in x])
                copy.set([doc.DEPPARSE_SCORE], [score], to_document=True)
                for sentence in copy.sentences:
                    sentence.build_dependencies()

                docs.append(copy)

            return tuple(docs)
        except RuntimeError as e:
            if str(e).startswith("CUDA out of memory. Tried to allocate"):
                new_message = str(e) + " ... You may be able to compensate for this by separating long sentences into their own batch with a parameter such as depparse_min_length_to_batch_separately=150 or by limiting the overall batch size with depparse_batch_size=400."
                raise RuntimeError(new_message) from e
            else:
                raise

class NextBest:
    # We want to return the next-best overall prediction
    # For a single sentence out of all of them, we modify the lemma
    def __init__(self, preds, scores):
        # preds: (n_pred, n_word)
        # score: (n_pred, n_word)
        self._preds = preds
        self._scores = scores
        self._n_preds = len(self._preds[0][0][0])
        self._n_sents = len(self._preds)

    def _access(self, index):
        # index: (nwords)
        # each entry is < n_preds
        return [[(word[0][idx], word[1][idx]) for word in self._preds[i]] for i, idx in enumerate(index)]

    def _score(self, index):
        # Simple summation across all scores and all features

        score = sum(self._scores[i][idx] for i, idx in enumerate(index))
        return score

    def __iter__(self):
        # What does an index look like?
        # Use simple summation to combine score across features
        self._seen = set()
        start_index = (0,) * self._n_sents
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
        for i in range(self._n_sents):
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
