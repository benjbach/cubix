#include "LKH.h"

/*
 * The Distance_SPECIAL function may be used to specify a user defined
 * distance fuction. The function is used when the EDGE_WEIGHT_TYPE is
 * SPECIAL. 
 * 
 * Example:
 *  
 *      int Distance_SPECIAL(Node * Na, Node * Nb) 
 *      {
 *           double dx = Na->X - Nb->X;
 *           double dy = Na->Y - Nb->Y;
 *           return 1000 * sqrt(dx * dx + dy * dy);
 *      }           
 */

int Distance_SPECIAL(Node * Na, Node * Nb)
{
    const double GridSize = 100000000;
    double dx = Na->X - Nb->X;
    double dy = Na->Y - Nb->Y;
    if (dx < 0)
        dx = -dx;
    if (dy < 0)
        dy = -dy;
    if (GridSize - dx < dx)
        dx = GridSize - dx;
    if (GridSize - dy < dy)
        dy = GridSize - dy;
    return sqrt(dx * dx + dy * dy) + 0.5;
}
