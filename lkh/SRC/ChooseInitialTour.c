#include "LKH.h"

/*
 * The ChooseInitialTour function generates a pseudo-random initial tour. 
 * The algorithm constructs a tour as follows. 
 *
 * First, a random node N is chosen.
 *
 * Then, as long as no all nodes have been chosen, choose the next node to 
 * follow N in the tour, NextN, and set N equal to NextN.
 *
 * NextN is chosen as follows:
 *  
 *  (A) If possible, choose NextN such that (N,NextN) is a fixed edge, or
 *      is common to two or more tours to be merged.
 *  (B) Otherwise, if possible, and Trial = 1, choose NextN such that 
 *      (N,NextN) is an edge of a given initial tour.
 *  (C) Otherwise, if possible, choose NextN so that (N,NextN) is a 
 *      candidate edge, the alpha-value of (N,NextN) is zero, and (N,NextN) 
 *      belongs to the current best or next best tour.
 *  (D) Otherwise, if possible, choose NextN such that (N,NextN) is a 
 *      candidate edge.
 *  (E) Otherwise, choose NextN at random among those nodes not already 
 *      chosen.
 *
 *  When more than one node may be chosen, the node is chosen at random 
 *  among the alternatives (a one-way list of nodes). 
 *
 *  The sequence of chosen nodes constitutes the initial tour.
 */

void ChooseInitialTour()
{
    Node *N, *NextN, *FirstAlternative, *Last;
    Candidate *NN;
    int Alternatives, Count = 0, i;

    if (KickType > 0 && Kicks > 0 && Trial > 1) {
        for (Last = FirstNode; (N = Last->BestSuc) != FirstNode; Last = N)
            Follow(N, Last);
        for (i = 1; i <= Kicks; i++)
            KSwapKick(KickType);
        return;
    }
    if (Trial == 1 && (!FirstNode->InitialSuc || InitialTourFraction < 1)) {
        if (InitialTourAlgorithm == BORUVKA ||
            InitialTourAlgorithm == GREEDY ||
            InitialTourAlgorithm == NEAREST_NEIGHBOR ||
            InitialTourAlgorithm == QUICK_BORUVKA) {
            GainType Cost = GreedyTour();
            if (MaxTrials == 0) {
                BetterCost = Cost;
                RecordBetterTour();
            }
            if (!FirstNode->InitialSuc)
                return;
        } else if (InitialTourAlgorithm == SIERPINSKI) {
            GainType Cost = SierpinskiTour();
            if (MaxTrials == 0) {
                BetterCost = Cost;
                RecordBetterTour();
            }
            if (!FirstNode->InitialSuc)
                return;
        }
    }
    /* Mark all nodes as "not chosen" by setting their V field to zero */
    N = FirstNode;
    do
        N->V = 0;
    while ((N = N->Suc) != FirstNode);

    /* Choose FirstNode without two incident fixed edges */
    do {
        if (!N->FixedTo2)
            break;
    }
    while ((N = N->Suc) != FirstNode);
    FirstNode = N;

    /* Move nodes with two incident fixed edges in 
       front of FirstNode */
    for (Last = FirstNode->Pred; N != Last; N = NextN) {
        NextN = N->Suc;
        if (N->FixedTo2)
            Follow(N, Last);
    }

    /* Mark FirstNode as chosen */
    FirstNode->V = 1;
    N = FirstNode;

    /* Loop as long as not all nodes have been chosen */
    while (N->Suc != FirstNode) {
        FirstAlternative = 0;
        Alternatives = 0;
        Count++;

        /* Case A */
        for (NN = N->CandidateSet; (NextN = NN->To); NN++) {
            if (!NextN->V && FixedOrCommon(N, NextN)) {
                Alternatives++;
                NextN->Next = FirstAlternative;
                FirstAlternative = NextN;
            }
        }
        if (Alternatives == 0 && InitialTourFile && Trial == 1 &&
            Count <= InitialTourFraction * Dimension) {
            /* Case B */
            for (NN = N->CandidateSet; (NextN = NN->To); NN++) {
                if (!NextN->V &&
                    (N->InitialSuc == NextN || NextN->InitialSuc == N)) {
                    Alternatives++;
                    NextN->Next = FirstAlternative;
                    FirstAlternative = NextN;
                }
            }
        }
        if (Alternatives == 0 && Trial > 1 &&
            ProblemType != HCP && ProblemType != HPP) {
            /* Case C */
            for (NN = N->CandidateSet; (NextN = NN->To); NN++) {
                if (!NextN->V && !NextN->FixedTo2 &&
                    NN->Alpha == 0 && (InBestTour(N, NextN) ||
                                       InNextBestTour(N, NextN))) {
                    Alternatives++;
                    NextN->Next = FirstAlternative;
                    FirstAlternative = NextN;
                }
            }
        }
        if (Alternatives == 0) {
            /* Case D */
            for (NN = N->CandidateSet; (NextN = NN->To); NN++) {
                if (!NextN->V && !NextN->FixedTo2) {
                    Alternatives++;
                    NextN->Next = FirstAlternative;
                    FirstAlternative = NextN;
                }
            }
        }
        if (Alternatives == 0) {
            /* Case E (actually not really a random choice) */
            NextN = N->Suc;
            while ((NextN->FixedTo2 || Forbidden(N, NextN))
                   && NextN->Suc != FirstNode)
                NextN = NextN->Suc;
        } else {
            NextN = FirstAlternative;
            if (Alternatives > 1) {
                /* Select NextN at random among the alternatives */
                int i = Random() % Alternatives;
                while (i--)
                    NextN = NextN->Next;
            }
        }
        /* Include NextN as the successor of N */
        Follow(NextN, N);
        N = NextN;
        N->V = 1;
    }
    if (MaxTrials == 0) {
        GainType Cost = 0;
        N = FirstNode;
        do
            Cost += C(N, N->Suc) - N->Pi - N->Suc->Pi;
        while ((N = N->Suc) != FirstNode);
        Cost /= Precision;
        if (Cost < BetterCost) {
            BetterCost = Cost;
            RecordBetterTour();
        }
    }
}
