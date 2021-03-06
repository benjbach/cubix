#include "LKH.h"

/*
 * After the candidate set has been created the FindTour function is called 
 * a predetermined number of times (Runs). 
 *
 * FindTour performs a number of trials, where in each trial it attempts 
 * to improve a chosen initial tour using the modified Lin-Kernighan edge 
 * exchange heuristics. 
 *
 * Each time a better tour is found, the tour is recorded, and the candidates 
 * are reorderded by the AdjustCandidateSet function. Precedence is given to 
 * edges that are common to two currently best tours. The candidate set is 
 * extended with those tour edges that are not present in the current set. 
 * The original candidate set is re-established at exit from FindTour.  
 */

static void SwapCandidateSets();

GainType FindTour()
{
    GainType Cost;
    Node *t;
    double EntryTime = GetTime();

    t = FirstNode;
    do
        t->OldPred = t->OldSuc = t->NextBestSuc = t->BestSuc = 0;
    while ((t = t->Suc) != FirstNode);
    BetterCost = PLUS_INFINITY;
    if (MaxTrials > 0)
        HashInitialize(HTable);
    else {
        Trial = 1;
        ChooseInitialTour();
    }

    for (Trial = 1; Trial <= MaxTrials; Trial++) {
        if (GetTime() - EntryTime >= TimeLimit) {
            if (TraceLevel >= 1)
                printff("*** Time limit exceeded ***\n");
            break;
        }
        /* Choose FirstNode at random */
        if (SubproblemSize == 0)
            FirstNode = &NodeSet[1 + Random() % Dimension];
        else {
            int i;
            for (i = Random() % Dimension; i > 0; i--)
                FirstNode = FirstNode->Suc;
        }
        ChooseInitialTour();
        Cost = LinKernighan();
        /* Merge tour with current best tour */
        if (FirstNode->BestSuc && Cost != BetterCost && Cost != Optimum) {
            t = FirstNode;
            while ((t = t->Next = t->BestSuc) != FirstNode);
            Cost = MergeWithTour();
        }
        if (Cost < BetterCost) {
            if (TraceLevel >= 1) {
                printff("* %d: Cost = " GainFormat, Trial, Cost);
                if (Optimum != MINUS_INFINITY && Optimum != 0)
                    printff(", Gap = %0.3f%%",
                            100.0 * (Cost - Optimum) / Optimum);
                printff(", Time = %0.1f sec. %s\n",
                        fabs(GetTime() - EntryTime),
                        Cost < Optimum ? "<" : Cost == Optimum ? "=" : "");
            }
            BetterCost = Cost;
            RecordBetterTour();
            if (Dimension == DimensionSaved)
                WriteTour(OutputTourFileName, BetterTour, BetterCost);
            if (StopAtOptimum && BetterCost == Optimum)
                break;
            AdjustCandidateSet();
            HashInitialize(HTable);
            HashInsert(HTable, Hash, Cost);
        } else if (TraceLevel >= 2)
            printff("  %d: Cost = " GainFormat ", Time = %0.1f sec.\n",
                    Trial, Cost, fabs(GetTime() - EntryTime));
        if (Trial == BackboneTrials && TraceLevel >= 1) {
            printff("# %d: Backbone candidates ->\n", Trial);
            CandidateReport();
        }
        /* Record backbones if wanted */
        if (Trial <= BackboneTrials && BackboneTrials < MaxTrials) {
            SwapCandidateSets();
            AdjustCandidateSet();
            if (Trial == BackboneTrials)
                AddTourCandidates();
            else
                SwapCandidateSets();
        }

        if (Trial == BackboneTrials && TraceLevel >= 1) {
            printff("# %d: Backbone candidates ->\n", Trial);
            CandidateReport();
        }
    }
    if (BackboneTrials > 0 && BackboneTrials < MaxTrials) {
        if (Trial >= BackboneTrials)
            SwapCandidateSets();
        t = FirstNode;
        do {
            free(t->BackboneCandidateSet);
            t->BackboneCandidateSet = 0;
        } while ((t = t->Suc) != FirstNode);
    }
    if (Trial > MaxTrials)
        Trial = MaxTrials;
    ResetCandidateSet();
    return BetterCost;
}

/*
 * The SwapCandidateSets function swaps the normal and backbone candidate sets.
 */

static void SwapCandidateSets()
{
    Node *t = FirstNode;
    do {
        Candidate *Temp = t->CandidateSet;
        t->CandidateSet = t->BackboneCandidateSet;
        t->BackboneCandidateSet = Temp;
    } while ((t = t->Suc) != FirstNode);
}
