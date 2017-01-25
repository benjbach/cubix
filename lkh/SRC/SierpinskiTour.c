#include "LKH.h"

/*
 * The SiepinskiTour function computes a tour using the space filling
 * curve heuristic described in
 *
 *    Loren K. Platzman and John J. Bartholdi III,
 *    Spacefilling curves and the planar travelling salesman problem,
 *    J. ACM, Vol. 36, 4, pp. 710-737 (1989).
 *
 * The function returns the cost of the resulting tour. 
 */

static int SierpinskiIndex(double x, double y);
static int compare(const void *Na, const void *Nb);

GainType SierpinskiTour()
{
    double XMin = DBL_MAX, XMax = -DBL_MAX,
        YMin = DBL_MAX, YMax = -DBL_MAX;
    Node *N, **perm;
    int i;
    GainType Cost;
    double EntryTime = GetTime();

    if (TraceLevel >= 1)
        printff("Sierpinski = ");

    N = FirstNode;
    do {
        if (N->X < XMin)
            XMin = N->X;
        if (N->X > XMax)
            XMax = N->X;
        if (N->Y < YMin)
            YMin = N->Y;
        if (N->Y > YMax)
            YMax = N->Y;
        N->LastV = 0;
    } while ((N = N->Suc) != FirstNode);
    if (XMax == XMin)
        XMax = XMin + 1;
    if (YMax == YMin)
        YMax = YMin + 1;

    assert(perm = (Node **) malloc(Dimension * sizeof(Node *)));
    for (i = 0, N = FirstNode; i < Dimension; i++, N = N->Suc)
        (perm[i] = N)->V =
            SierpinskiIndex((N->X - XMin) / (XMax - XMin),
                            (N->Y - YMin) / (YMax - YMin));
    qsort(perm, Dimension, sizeof(Node *), compare);
    for (i = 1; i < Dimension; i++)
        Follow(perm[i], perm[i - 1]);
    free(perm);

    /* Assure that all fixed or common edges belong to the tour */
    N = FirstNode;
    do {
        N->LastV = 1;
        if (!FixedOrCommon(N, N->Suc) && N->CandidateSet) {
            Candidate *NN;
            for (NN = N->CandidateSet; NN->To; NN++) {
                if (!NN->To->LastV && FixedOrCommon(N, NN->To)) {
                    Follow(NN->To, N);
                    break;
                }
            }
        }
    } while ((N = N->Suc) != FirstNode);
    do
        N->LastV = 0;
    while ((N = N->Suc) != FirstNode);
    do {
        N->LastV = 1;
        if (!FixedOrCommon(N, N->Pred) && N->CandidateSet) {
            Candidate *NN;
            for (NN = N->CandidateSet; NN->To; NN++) {
                if (!NN->To->LastV && FixedOrCommon(N, NN->To)) {
                    Precede(NN->To, N);
                    break;
                }
            }
        }
    } while ((N = N->Pred) != FirstNode);
    Cost = 0;
    N = FirstNode;
    do
        Cost += Distance(N, N->Suc);
    while ((N = N->Suc) != FirstNode);
    if (TraceLevel >= 1) {
        printff(GainFormat, Cost);
        if (Optimum != MINUS_INFINITY && Optimum != 0)
            printff(", Gap = %0.1f%%", 100.0 * (Cost - Optimum) / Optimum);
        printff(", Time = %0.1f sec.\n", fabs(GetTime() - EntryTime));
    }
    return Cost;
}

static int SierpinskiIndex(double x, double y)
{
    int idx = 0;
    double oldx;
    int i = 8 * sizeof(int);

    if (x > y) {
        idx = 1;
        x = 1 - x;
        y = 1 - y;
    }
    while (--i > 0) {
        idx *= 2;
        if (x + y > 1) {
            idx++;
            oldx = x;
            x = 1 - y;
            y = oldx;
        }
        if (--i <= 0)
            break;
        x *= 2;
        y *= 2;
        idx *= 2;
        if (y > 1) {
            idx++;
            oldx = x;
            x = y - 1;
            y = 1 - oldx;
        }
    }
    return idx;
}

static int compare(const void *Na, const void *Nb)
{
    int NaV = (*(Node **) Na)->V;
    int NbV = (*(Node **) Nb)->V;
    return NaV < NbV ? -1 : NaV > NbV ? 1 : 0;
}
