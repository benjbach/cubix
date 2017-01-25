#ifndef SEQUENCE_H
#define SEQUENCE_H

/* 
 * This header specifies the interface for the use of node sequences.
 *   
 * The functions BestKOptMove and BacktrackKOptMove are implemented 
 * by means of such sequences. 
 */

#include "LKH.h"

Node **t, **T, **tSaved;
int *p, *q;
int *incl, *cycle;
GainType *G;
int K;

int FeasibleKOptMove(int k);
void FindPermutation(int k);
int Cycles(int k);

int Added(Node * ta, Node * tb, int k);
int Deleted(Node * ta, Node * tb, int k);

#endif
