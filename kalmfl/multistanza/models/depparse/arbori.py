import numpy as np
import random
import heapq

class Edge:
    def __init__(self, u, v, name=""):
        self.u = u
        self.v = v
        self.name = name

    def __iter__(self):
        return iter((self.u, self.v))

    def __repr__(self):
        if self.name == "":
            return repr((self.u, self.v))
        else:
            return self.name

def Get1Best(V, E, score):
#    print("Get1Best(")
#    print("\tV =", V)
#    print("\tE =", E)
#    print("\tscore =", score)
#    print(")")
    n_vertices = len(score)
    best_in_edge = {}
    for v in V - {0}:
        best_score = -np.inf
        for e in E:
            u, t = e
            if t != v:
                continue

            if score[e] > best_score:
                best_in_edge[v] = e
                best_score = score[e]

        cycle = contains_cycle(best_in_edge)
        if cycle is not None:
            C, C_E = cycle
            real = {}

            v_c = max(V) + 1
            V_p = (V | {v_c}) - C
            E_p = set()
            kicks_out = {}
            score_p = {}
            for e in E:
                e_p = None
                t, u = e
                if t not in C and u not in C:
                    e_p = e
                    score_p[e_p] = score[e]
                elif t in C and u not in C:
                    e_p = Edge(v_c, u, e.name)
                    score_p[e_p] = score[e]
                elif u in C and t not in C:
                    e_p = Edge(t, v_c, e.name)
                    kicks_out[e_p] = best_in_edge[u]
                    score_p[e_p] = score[e] - score[kicks_out[e_p]]
                else:
                    continue # remove internal edges within the cycle

                real[e_p] = e
                E_p.add(e_p)

            A = Get1Best(V_p, E_p, score_p)
            if V_p & set(A.keys()) != V_p - {0}:
#                print("V_p:", V_p)
#                print("A:", A)
                return {}

            for e in A.values():
                u, v = real[e]
                best_in_edge[v] = real[e]

            kicked_out_edge = kicks_out[A[v_c]]
            for e in C_E - {kicked_out_edge}:
                u, v = e
                best_in_edge[v] = e

            return best_in_edge

    return best_in_edge

def find_next_edge(e, A, V, E, score):
    diff = np.inf
    next_edge = None
    for e_next in E:
        if e == e_next:
            continue

        # they don't have the same destination
        if e_next.v != e.v:
            continue

        # is the source of the edge downstream from us
        if is_ancestor(A, e.v, e_next.u):
            continue

        if score[e] - score[e_next] < diff:
            diff = score[e] - score[e_next]
            next_edge = e_next

    return next_edge

def is_ancestor(A, u, v):
    cur = v
    while cur != 0:
        if cur == u: # did we reach our vertex?
            return True
#        print("taking edge:", A[cur])
        cur = A[cur].u # go backwards from our current edge

#    print("-----------------------")
    return False

def FindEdgeToBan(A, V, E, score, req, banned):
#    print("FindEdgeToBan(")
#    print("\tA=", A)
#    print("\tV =", V)
#    print("\tE =", E)
#    print("\treq =", req)
#    print("\tbanned =", banned)
#    print("\tscore=", score)
#    print(")")

    E = E - banned
    n_vertices = len(score)
    best_in_edge = {}
    diff = np.inf
    edge_to_ban = None

    for v in V - {0}:
        if v in A and A[v] not in banned:
            best_score = score[A[v]]
            best_in_edge[v] = A[v]
        else:
            best_score = -np.inf

        for e in E:
            u, t = e
            if t != v:
                continue

            if score[e] > best_score:
                best_in_edge[v] = e
                best_score = score[e]

        if best_in_edge[v] not in req and best_in_edge[v] in A.values():
            next_edge = find_next_edge(best_in_edge[v], A, V, E, score)
            if next_edge is not None and score[best_in_edge[v]] - score[next_edge] < diff:
                edge_to_ban = best_in_edge[v]
                diff = score[best_in_edge[v]] - score[next_edge]
#                print({e:score[e] for e in E})

        cycle = contains_cycle(best_in_edge)
        if cycle is not None:
            C, C_E = cycle
            real = {}

            v_c = max(V) + 1
            V_p = (V | {v_c}) - C
            E_p = set()
            kicks_out = {}
            score_p = {}
            req_p = set()
            banned_p = set()
            A_p = dict()
            for e in E | set(A.values()):
                e_p = None
                t, u = e
                if t not in C and u not in C:
                    e_p = e
                    score_p[e_p] = score[e]
                elif t in C and u not in C:
                    e_p = Edge(v_c, u, e.name)
                    score_p[e_p] = score[e]
                elif u in C and t not in C:
                    e_p = Edge(t, v_c, e.name)
                    kicks_out[e_p] = best_in_edge[u]
                    score_p[e_p] = score[e] - score[kicks_out[e_p]]
                else:
                    continue # remove internal edges within the cycle

                real[e_p] = e
                if e in E:
                    E_p.add(e_p)

                if e in req:
                    req_p.add(e_p)

                if e in banned:
                    banned_p.add(e_p)

                if e in A.values():
                    A_p[e_p.v] = e_p

            edge_to_ban_p, diff_p = FindEdgeToBan(A_p, V_p, E_p, score_p, req_p, banned_p)

            if diff_p < diff:
                diff = diff_p
                edge_to_ban = real[edge_to_ban_p]

            return edge_to_ban, diff

    return edge_to_ban, diff


# edges is an array in the form (vertex, parent)
def contains_cycle(edges):
    visited_vertices = set()
    visited_edges = set()
    unvisited = set(edges.keys())
    def dfs(v):
        if v in visited_vertices:
            return (visited_vertices, visited_edges)

        if v in unvisited:
            unvisited.remove(v)

        visited_vertices.add(v)
        for child, in_edge in edges.items():
            # disallow self-loops
            if in_edge.u == v and v != child:
                visited_edges.add(in_edge)
                cycle_found = dfs(child)
                if cycle_found:
                    return True

                visited_edges.remove(in_edge)

        visited_vertices.remove(v)
        return False

    while len(unvisited) > 0:
        v = random.sample(unvisited, 1)[0]

        cycle_found = dfs(v)
        if cycle_found:
            return visited_vertices, visited_edges


    return None

def GetConstrained1Best(V, E, score, req, banned):
#    print("GetConstrained1Best(req={}, banned={})".format(req, banned))
    E = E - banned
    req_vertices = set(map(lambda e: e.v, req))
    E = set(filter(lambda e: e.v not in req_vertices or e in req, E))
    return Get1Best(V, E, score)

def tree_score(A, score):
    return sum(map(lambda e: score[e], A.values()))

def GetKBest(k, V, E, score):
    req = set()
    banned = set()
    A = Get1Best(V, E, score)
#    print("A =", A)
    As = [(A, tree_score(A, score))]
    Q = []
    e_ban, diff = FindEdgeToBan(A, V, E, score, req, banned)
    heapq.heappush(Q, (-tree_score(A, score) + diff, req, banned, A, e_ban))
    while len(As) < k:
        weight, req, banned, A, e_ban = heapq.heappop(Q)
        while len(Q) > 0 and e_ban is None:
            weight, req, banned, A, e_ban = heapq.heappop(Q)

        if e_ban is None:
            return As

        req_p = req | {e_ban}
        banned_p = banned | {e_ban}
        A_p = GetConstrained1Best(V, E, score, req, banned_p)

        if set(A_p.keys()) == V - {0}:
            As.append((A_p, tree_score(A_p, score)))
            e_ban, diff = FindEdgeToBan(A_p, V, E, score, req, banned_p)
            heapq.heappush(Q, (-tree_score(A_p, score) + diff, req, banned_p, A_p, e_ban))

        e_ban, diff = FindEdgeToBan(A, V, E, score, req_p, banned)
        heapq.heappush(Q, (-tree_score(A, score) + diff, req_p, banned, A, e_ban))

    return As
