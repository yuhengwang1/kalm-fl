import kalmfl.multistanza.models.depparse.arbori as arbori

def GetKBest(weights, k):
    V = set(range(len(weights)))
    E = set()
    scores = {}
    for i in range(len(weights)):
        for j in range(len(weights)):
            e = arbori.Edge(j, i)
            E.add(e)
            scores[e] = weights[i, j]
        
    return arbori.GetKBest(k, V, E, scores)
