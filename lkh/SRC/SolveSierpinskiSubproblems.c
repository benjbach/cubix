#include "LKH.h"

/*
 * The SolveTourSierpinskiSubproblems function attempts to improve a 
 * given tour by means of Sierpinski partitioning. If an improvement 
 * is found, the new tour is written to TourFile.
 * 
 * The tour is divided into segments of equal size. Each segment 
 * constitutes a subproblem to be solved by the LinKernighan function. 
 * 
 * The original tour is given by the SubproblemSuc references of the nodes. 
 * The size of each segment is SubproblemSize.
 *
 * The function runs in two rounds. In the first round the first segment 
 * starts at FirstNode. In the second round the middle of this segment is 
 * used as a starting point for the first segment. 
 */

void SolveSierpinskiSubproblems()
{
    Node *FirstNodeSaved, *N;
    int CurrentSubproblem, Subproblems, Round, i;
    int RestrictedSearchSaved = RestrictedSearch;
    GainType GlobalBestCost;
    Node **SierpinskiSuc;
    double EntryTime = GetTime();

    SierpinskiTour();
    assert(SierpinskiSuc =
           (Node **) malloc((1 + Dimension) * sizeof(Node *)));
    N = FirstNode;
    do
        SierpinskiSuc[N->Id] = N->Suc;
    while ((N = N->Suc) != FirstNode);
    AllocateStructures();
    Subproblems = ceil((double) Dimension / SubproblemSize);
    ReadPenalties();
    FirstNode = &NodeSet[Random() % Dimension + 1];

    /* Compute upper bound for the original problem */
    GlobalBestCost = 0;
    N = FirstNodeSaved = FirstNode;
    do {
        if (!Fixed(N, N->SubproblemSuc))
            GlobalBestCost += Distance(N, N->SubproblemSuc);
        N->Subproblem = 0;
    }
    while ((N = N->BestSuc = N->SubproblemSuc) != FirstNode);
    for (Round = 1; Round <= 2; Round++) {
        if (Round == 2 && Subproblems == 1)
            break;
        if (TraceLevel >= 1) {
            if (Round == 2 || TraceLevel >= 2)
                printff("\n");
            printff
                ("*** Sierpinski partitioning *** [Round %d of %d, Cost = "
                 GainFormat "]\n", Round, Subproblems > 1 ? 2 : 1,
                 GlobalBestCost);
        }
        FirstNode = FirstNodeSaved;
        if (Round == 2)
            for (i = SubproblemSize / 2; i > 0; i--)
                FirstNode = SierpinskiSuc[FirstNode->Id];
        for (CurrentSubproblem = 1;
             CurrentSubproblem <= Subproblems; CurrentSubproblem++) {
            for (i = 0, N = FirstNode; i < SubproblemSize;
                 i++, N = SierpinskiSuc[N->Id])
                N->Subproblem =
                    (Round - 1) * Subproblems + CurrentSubproblem;
            SolveSubproblem((Round - 1) * Subproblems + CurrentSubproblem,
                            Subproblems, &GlobalBestCost);
            if (SubproblemsCompressed) {
                if (TraceLevel >= 1)
                    printff("\nCompress subproblem %d\n",
                            CurrentSubproblem);
                RestrictedSearch = 0;
                SolveSubproblem((Round - 1) * Subproblems +
                                CurrentSubproblem, Subproblems,
                                &GlobalBestCost);
                RestrictedSearch = RestrictedSearchSaved;
            }
            FirstNode = N;
        }
    }
    free(SierpinskiSuc);
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
