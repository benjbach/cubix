#include "LKH.h"

/*
 * The MergeWithTour function attempts to find a short tour
 * by merging a given tour, T1, with another tour, T2. 
 * T1 is given by the Suc pointers of its nodes. 
 * T2 is given by the Next pointers of its nodes.
 *
 * The merging algorithm may be described as follows:
 * Let G be the graph consisting of the nodes and the union of the 
 * edges of T1 and T2. Attempt - in all possible ways - 
 * to separate the nodes of G into two disjoint sets (A,B) such 
 * that the cardinality of edges connecting A with B is exactly two. 
 * If this is possible, any replacement of T1's A-edges with T2's
 * A-edges results in a tour. The same holds for the B-edges. 
 * If such a replacement reduces the cost of one the tours then make it. 
 *
 * If a tour T shorter than T2 is found, Pred and Suc of each node
 * point to its neighbors in T, and T's cost is returned.
 *
 * The function is called from the FindTour function.
 *
 * The implementation is inspired by the algorithm described in the
 * paper
 *
 *   A. Mobius, B. Freisleben, P. Merz, and M. Schreiber, 
 *   "Combinatorial Optimization by Iterative Partial Transcription", 
 *   Physical Review E, Volume 59, Number 4, pp. 4667-4674, 1999.
 */

GainType MergeWithTour()
{
    int Rank = 0, Improved1 = 0, Improved2 = 0;
    int SubSize1, SubSize2, MaxSubSize2, NewDimension = 0, Forward;
    int MinSubSize, BestMinSubSize = -1, MinForward = 0;
    GainType Cost1 = 0, Cost2 = 0, Gain, OldCost1, MinGain = 0;
    Node *N, *NNext, *N1, *N2, *MinN1, *MinN2, *First = 0, *Last;

    N = FirstNode;
    do
        N->Suc->Pred = N->Next->Prev = N;
    while ((N = N->Suc) != FirstNode);
    do {
        Cost1 += N->Cost = C(N, N->Suc) - N->Pi - N->Suc->Pi;
        Cost2 += N->NextCost = C(N, N->Next) - N->Pi - N->Next->Pi;
        if ((N->Suc == N->Prev || N->Suc == N->Next) &&
            (N->Pred == N->Prev || N->Pred == N->Next))
            N->V = 0;
        else {
            N->V = 1;
            NewDimension++;
            First = N;
        }
    } while ((N = N->Suc) != FirstNode);
    if (NewDimension == 0) {
        if (Distance == Distance_1 ||
            (MaxTrials == 0
             && (FirstNode->InitialSuc
                 || InitialTourAlgorithm == SIERPINSKI)))
            return Cost1;
        else
            return Cost1 / Precision;
    }
    OldCost1 = Cost1;

    /* Shrink the tours. 
       OldPred and OldSuc represent the shrunken T1. 
       Prev and Next represent the shrunken T2 */
    N = First;
    Last = 0;
    do {
        if (N->V) {
            N->Rank = ++Rank;
            if (Last) {
                Last->OldSuc = N;
                if (Last != N->Pred)
                    Last->Cost = 0;
                N->OldPred = Last;
            }
            Last = N;
        }
    } while ((N = N->Suc) != First);
    Last->OldSuc = First;
    First->OldPred = Last;
    if (Last != First->Pred)
        Last->Cost = 0;
    N = First;
    Last = 0;
    do {
        if (N->V) {
            if (Last) {
                Last->Next = N;
                if (Last != N->Prev) {
                    Last->NextCost = 0;
                    N->Prev = Last;
                }
            }
            Last = N;
        }
    } while ((N = N->Next) != First);
    Last->Next = First;
    if (Last != First->Prev) {
        Last->NextCost = 0;
        First->Prev = Last;
    }

    /* Merge the shrunken tours */
    do {
        MinN1 = MinN2 = 0;
        MinSubSize = NewDimension / 2;
        N1 = First;
        do {
            while (N1->OldSuc != First &&
                   (N1->OldSuc == N1->Next || N1->OldSuc == N1->Prev))
                N1 = N1->OldSuc;
            if (N1->OldSuc == First)
                break;
            for (Forward = 1, N2 = N1->Next; Forward >= 0;
                 Forward--, N2 = N1->Prev) {
                if (N2 == N1->OldSuc || N2 == N1->OldPred)
                    continue;
                SubSize1 = MaxSubSize2 = 0;
                do {
                    if (++SubSize1 >= MinSubSize)
                        break;
                    if ((SubSize2 = N2->Rank - N1->Rank) < 0)
                        SubSize2 += NewDimension;
                    if (SubSize2 > MaxSubSize2) {
                        if (SubSize2 >= MinSubSize)
                            break;
                        if (SubSize2 == SubSize1) {
                            for (N = N1, Gain = 0; N != N2; N = N->OldSuc)
                                Gain += N->Cost - N->NextCost;
                            if (!Forward)
                                Gain += N1->NextCost - N2->NextCost;
                            if (Gain != 0) {
                                MinSubSize = SubSize1;
                                MinN1 = N1;
                                MinN2 = N2;
                                MinGain = Gain;
                                MinForward = Forward;
                            }
                            break;
                        }
                        MaxSubSize2 = SubSize2;
                    }
                } while ((N2 = Forward ? N2->Next : N2->Prev) != N1);
            }
        } while ((N1 = N1->OldSuc) != First &&
                 MinSubSize != BestMinSubSize);
        if (MinN1) {
            BestMinSubSize = MinSubSize;
            if (MinGain > 0) {
                Improved1 = 1;
                Cost1 -= MinGain;
                Rank = MinN1->Rank;
                for (N = MinN1; N != MinN2; N = NNext) {
                    NNext = MinForward ? N->Next : N->Prev;
                    N->OldSuc = NNext;
                    NNext->OldPred = N;
                    N->Rank = Rank;
                    N->Cost = MinForward ? N->NextCost : NNext->NextCost;
                    if (++Rank > NewDimension)
                        Rank = 1;
                }
            } else {
                Improved2 = 1;
                Cost2 += MinGain;
                for (N = MinN1; N != MinN2; N = N->OldSuc) {
                    if (MinForward) {
                        N->Next = N->OldSuc;
                        N->Next->Prev = N;
                        N->NextCost = N->Cost;
                    } else {
                        N->Prev = N->OldSuc;
                        N->Prev->Next = N;
                        N->OldSuc->NextCost = N->Cost;
                    }
                }
                if (MinForward)
                    MinN2->Prev = N->OldPred;
                else {
                    MinN2->Next = N->OldPred;
                    MinN2->NextCost = N->OldPred->Cost;
                }
            }
        }
    } while (MinN1);

    if (Cost1 >= Cost2 ? !Improved2 : !Improved1)
        return OldCost1 / Precision;

    /* Expand the best tour into a full tour */
    N = FirstNode;
    do
        N->Mark = 0;
    while ((N = N->Suc) != FirstNode);
    N = First;
    N->Mark = N;
    do {
        if (!N->Suc->Mark && (!N->V || !N->Suc->V))
            N->OldSuc = N->Suc;
        else if (!N->Pred->Mark && (!N->V || !N->Pred->V))
            N->OldSuc = N->Pred;
        else if (Cost1 <= Cost2) {
            if (N->OldSuc->Mark)
                N->OldSuc = !N->OldPred->Mark ? N->OldPred : First;
        } else if (!N->Next->Mark)
            N->OldSuc = N->Next;
        else if (!N->Prev->Mark)
            N->OldSuc = N->Prev;
        else
            N->OldSuc = First;
        N->Mark = N;
    } while ((N = N->OldSuc) != First);
    do
        N->OldSuc->Pred = N;
    while ((N = N->Suc = N->OldSuc) != First);
    if (Distance == Distance_1 ||
        (MaxTrials == 0
         && (FirstNode->InitialSuc || InitialTourAlgorithm == SIERPINSKI)))
        return Cost1 <= Cost2 ? Cost1 : Cost2;
    else
        return (Cost1 <= Cost2 ? Cost1 : Cost2) / Precision;
}
