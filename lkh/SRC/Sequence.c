#include "Sequence.h"
#include "Segment.h"

/*
 * This file contains the functions FindPermutation and FeasibleKOptMove.
 */

Node **t;  /* The sequence of nodes to be used in a move */
Node **T;  /* The currently best t's */
Node **tSaved;  /* For saving t when using the BacktrackKOptMove function */
int *p;    /* The permutation corresponding to the sequence in which 
              the t's occur on the tour */
int *q;    /* The inverse permutation of p */
int *incl; /* Array: incl[i] == j, if (t[i], t[j]) is an inclusion edge */
int *cycle;     /* Array: cycle[i] is cycle number of t[i] */
GainType *G;    /* For storing the G-values in the BestKOptMove function */
int K;     /* The value K for the current K-opt move */
int *NodeCount; /* The number of nodes in each cycle */

static int excl(int i);

/*  
 * The FindPermutation function finds the permutation p[1:2k] corresponding 
 * to the sequence in which the nodes t[1:2k] occur on the tour.
 *   
 * The nodes are sorted using qsort. The BETWEEN function is used 
 * as comparator.
 *   
 * Postcondition:
 *   
 *     BETWEEN(t[p[i-1]], t[p[i]], t[p[i+1]]) for i = 2, ..., 2k-1
 */

static Node *tp1;

static int compare(const void *pa, const void *pb)
{
    return BETWEEN(tp1, t[*(int *) pa], t[*(int *) pb]) ? -1 : 1;
}

void FindPermutation(int k)
{
    int i, j;

    for (i = j = 1; j <= k; i += 2, j++)
        p[j] = SUC(t[i]) == t[i + 1] ? i : i + 1;
    tp1 = t[p[1]];
    qsort(p + 2, k - 1, sizeof(int), compare);
    for (j = 2 * k; j >= 2; j -= 2) {
        p[j - 1] = i = p[j / 2];
        p[j] = i & 1 ? i + 1 : i - 1;
    }
    for (i = 1; i <= 2 * k; i++)
        q[p[i]] = i;
}

/*  
 * The FeasibleKOptMove function tests whether the move given by
 * t[1..2k] and incl[1..2k] represents a feasible k-opt move,
 * i.e., making the move on the current tour will result in a tour.
 *   
 * In that case, 1 is returned. Otherwise, 0 is returned. 
 */

int FeasibleKOptMove(int k)
{
    int Count = 1, i = 2 * k;

    FindPermutation(k);
    while ((i = q[incl[p[i]]] ^ 1))
        Count++;
    return Count == k;
}

/*
 * The Cycles function returns the number of cycles that would appear if 
 * the move given by t[1..2k] and incl[1..2k] was made. 
 * In addition, cycle[i] is assigned the number of the cycle that node t[i] 
 * is a part of (an integer from 1 to Cycles).
 */

int Cycles(int k)
{
    int i, j, Count = 0;

    FindPermutation(k);
    for (i = 1; i <= 2 * k; i++)
        cycle[i] = 0;
    for (i = 1; i <= 2 * k; i++) {
        if (!cycle[p[i]]) {
            Count++;
            j = i;
            do {
                cycle[p[j]] = Count;
                j = q[incl[p[j]]];
                cycle[p[j]] = Count;
                if (p[j - 1] == excl(p[j])) {
                    if (++j > 2 * k)
                        j = 1;
                } else if (--j <= 1)
                    j = 2 * k;
            }
            while (j != i);
        }
    }
    return Count;
}

/* 
 * The Deleted function is used to test if an edge, (ta,tb), 
 * of the tour has been deleted in the move under construction.
 */

int Deleted(Node * ta, Node * tb, int k)
{
    int i = 2 * k + 2;

    while ((i -= 2) > 0)
        if ((ta == t[i - 1] && tb == t[i]) ||
            (ta == t[i] && tb == t[i - 1]))
            return 1;
    return 0;
}

/*
 * The Added function is used to test if an edge, (ta,tb),
 * has been added in the move under construction.
 */

int Added(Node * ta, Node * tb, int k)
{
    int i = 2 * k;

    while ((i -= 2) > 0)
        if ((ta == t[i] && tb == t[incl[i]]) ||
            (tb == t[i] && ta == t[incl[i]]))
            return 1;
    return 0;
}

/*
 * The excl function returns j, where (t[i],t[j]) is an edge to be 
 * excluded from the tour. For a given value of i the value of j is 
 * unique.
 */

static int excl(int i)
{
    return i & 1 ? i + 1 : i - 1;
}
