#include "LKH.h"
#include "Delaunay.h"

/*
 * The SolveDelaunaySubproblems function attempts to improve a given tour
 * by means of a partitioning scheme based on Delaunay clustering. 
 *
 * The set of nodes is subdivided into clusters, with at most SubproblemSize 
 * nodes in each one. Each cluster together with the given tour induces a 
 * subproblem consisting of all nodes in the cluster, and with edges fixed 
 * between nodes that are connected by tour segments whose interior points
 * do not belong the cluster.  
 *  
 * If an improvement is found, the new tour is written to TourFile. 
 * The original tour is given by the SubproblemSuc references of the nodes.
 */

static int DelaunayClustering(int MaxClusterSize);

void SolveDelaunaySubproblems()
{
    Node *N;
    GainType GlobalBestCost;
    int CurrentSubproblem, Subproblems;
    int RestrictedSearchSaved = RestrictedSearch;
    double EntryTime = GetTime();

    AllocateStructures();
    ReadPenalties();
    /* Compute upper bound for the original problem */
    GlobalBestCost = 0;
    N = FirstNode;
    do {
        if (!Fixed(N, N->SubproblemSuc))
            GlobalBestCost += Distance(N, N->SubproblemSuc);
        N->Subproblem = 0;
    }
    while ((N = N->BestSuc = N->SubproblemSuc) != FirstNode);
    if (TraceLevel >= 1) {
        if (TraceLevel >= 2)
            printff("\n");
        printff("*** Delaunay partitioning *** [Cost = " GainFormat "]\n",
                GlobalBestCost);
    }
    Subproblems = DelaunayClustering(SubproblemSize);
    for (CurrentSubproblem = 1;
         CurrentSubproblem <= Subproblems; CurrentSubproblem++) {
        SolveSubproblem(CurrentSubproblem, Subproblems, &GlobalBestCost);
        if (SubproblemsCompressed) {
            if (TraceLevel >= 1)
                printff("\nCompress subproblem %d:\n", CurrentSubproblem);
            RestrictedSearch = 0;
            SolveSubproblem(CurrentSubproblem, Subproblems,
                            &GlobalBestCost);
            RestrictedSearch = RestrictedSearchSaved;
        }
    }
    printff("\nCost = " GainFormat, GlobalBestCost);
    if (Optimum != MINUS_INFINITY && Optimum != 0)
        printff(", Gap = %0.3f%%",
                100.0 * (GlobalBestCost - Optimum) / Optimum);
    printff(", Time = %0.1f sec. %s\n", fabs(GetTime() - EntryTime),
            GlobalBestCost < Optimum ? "<" : GlobalBestCost ==
            Optimum ? "=" : "");
    if (SubproblemBorders && Subproblems > 1)
        SolveSubproblemBorderProblems(Subproblems);
}

typedef struct Edge {
    Node *From, *To;
    int Cost;
} Edge;

static int compare(const void *e1, const void *e2)
{
    int d1 = ((Edge *) e1)->Cost;
    int d2 = ((Edge *) e2)->Cost;
    return d1 < d2 ? -1 : d1 == d2 ? 0 : 1;
}

#define Size V

static int DelaunayClustering(int MaxClusterSize)
{
    int Count, i;
    edge_ref *edges, e;
    Edge *EdgeSet;
    Node *From, *To, *N, *F, *T;

    edges = delaunay_edges(Dimension);
    for (Count = 0; edges[Count]; Count++);
    assert(EdgeSet = (Edge *) malloc(Count * sizeof(Edge)));
    for (i = 0; edges && (e = edges[i]); i++) {
        EdgeSet[i].From = From = (Node *) ODATA(e);
        EdgeSet[i].To = To = (Node *) DDATA(e);
        EdgeSet[i].Cost = FixedOrCommon(From, To) ? INT_MIN :
            Distance(From, To) * Precision + From->Pi + To->Pi;
        destroy_edge(e);
    }
    free(edges);
    qsort(EdgeSet, Count, sizeof(Edge), compare);

    /* Union-Find with path compression */
    N = FirstNode;
    do {
        N->Next = N;
        N->Size = 1;
    } while ((N = N->Suc) != FirstNode);
    for (i = 0; i < Count; i++) {
        for (F = EdgeSet[i].From; F != F->Next; F = F->Next)
            F->Next = F->Next->Next;
        for (T = EdgeSet[i].To; T != T->Next; T = T->Next)
            T->Next = T->Next->Next;
        if (F != T && F->Size + T->Size <= MaxClusterSize) {
            if (F->Size < T->Size) {
                F->Next = T;
                T->Size += F->Size;
            } else {
                T->Next = F;
                F->Size += T->Size;
            }
        }
    }
    free(EdgeSet);

    Count = 0;
    N = FirstNode;
    do {
        if (N->Next == N && N->Size > 3)
            N->Subproblem = ++Count;
    } while ((N = N->Suc) != FirstNode);
    do {
        From = N;
        while (From->Next != From)
            From = From->Next;
        N->Subproblem = From->Subproblem;
    } while ((N = N->Suc) != FirstNode);
    return Count;
}
