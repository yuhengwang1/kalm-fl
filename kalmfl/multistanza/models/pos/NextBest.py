import heapq
import torch

# (npred, nwords)
# sent: (nwords, feature_size)
# scores: (npred, nwords)
# start_index = (0*nwords) (because scores[0, :] gives the best prediction)
# so if we have multiple features
# for each word we decide which feature to decrement
# start_index = index_ij. i: nword, j: nfeature

# What we actually want is to return the sequence
class WordNextBest:
    def __init__(self, scores, indices):
        # shape: (npred, nfeatures)
        self._scores = scores
        self._indices = indices
        self._n_pred = scores.shape[0]
        self._n_features = scores.shape[1]

    def __index_hash(self, t):
        total = 0
        for i in range(self._n_features):
            total += t[0, i] * self._n_pred**i

        return total.item()

    def __iter__(self):
        start_index = torch.zeros((1, self._n_features), dtype=torch.int64)
        features_score = torch.gather(self._scores, 0, start_index).squeeze()
        features = torch.gather(self._indices, 0, start_index).squeeze()
        self._seen = set()
        self._tie_counter = 0
        tc = self._tie_counter
        self._tie_counter += 1
        self._queue = [(-torch.mean(features_score), tc, features, start_index)]
        self._seen.add(self.__index_hash(start_index))
        self._tie_counter += 1
        return self

    def __next__(self):
        if len(self._queue) == 0:
            raise StopIteration

        features_score_ret, tc, features_ret, ci = heapq.heappop(self._queue)
        for i in range(self._n_features):
            if ci[0, i] + 1 >= self._n_pred:
                continue

            ni = torch.clone(ci)
            ni[0, i] += 1
            ni_hash = self.__index_hash(ni)
            if ni_hash in self._seen:
                continue
            features_score = torch.gather(self._scores, 0, ni).squeeze()
            if torch.any(features_score < 0):
                continue

            self._seen.add(ni_hash)
            features = torch.gather(self._indices, 0, ni).squeeze()
            tc = self._tie_counter
            heapq.heappush(self._queue, (-torch.mean(features_score), tc, features, ni))
            self._tie_counter += 1
        return -features_score_ret, features_ret

class NextBest:
    def __init__(self, scores, indices):
        # shape: (npred, nfeatures)
        self._scores = scores
        self._indices = indices
        self._n_pred = scores.shape[0]
        self._n_words = scores.shape[1]
        self._n_features = scores.shape[2]

    def __iter__(self):
        scores = self._scores
        indices = self._indices

        self._queues = [iter(WordNextBest(scores[:, i, :], indices[:, i, :])) for i in range(self._n_words) if torch.any(scores[:, i, :] != 0)]
        self._values = None
        return self

    def __next__(self):
        if self._values is None:
            self._values = list(map(next, self._queues))
            return tuple(self._values)

        did_update = False
        for i, wnb in enumerate(self._queues):
            try:
                self._values[i] = next(wnb)
                did_update = True
            except StopIteration:
                pass

        if did_update:
            return tuple(self._values)

        raise StopIteration
