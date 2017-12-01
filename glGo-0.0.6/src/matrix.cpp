/*
 * matrix.cpp
 */

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#endif

#include "defines.h"
#include "matrix.h"
#include "marks.h"


Matrix::Matrix(unsigned short s)
    : size(s)
{
    wxASSERT(size >= BOARD_SIZE_MIN && size <= BOARD_SIZE_MAX);

    init();
}

Matrix::Matrix(const Matrix &m)
{
    size = m.getSize();
    wxASSERT(size >= BOARD_SIZE_MIN && size <= BOARD_SIZE_MAX);

    init();

    for (unsigned short i=0; i<size; i++)
        for (unsigned short j=0; j<size; j++)
            matrix[i][j] = m.at(i, j);
}

Matrix::~Matrix()
{
    wxLogDebug("~Matrix()");

    wxASSERT(size >= BOARD_SIZE_MIN && size <= BOARD_SIZE_MAX);

    for (int i=0; i<size; i++)
        delete [] matrix[i];
    delete [] matrix;
}

void Matrix::init()
{
    matrix = new short*[size];

    for (unsigned short i=0; i<size; i++)
    {
        matrix[i] = new short[size];

        for (unsigned short j=0; j<size; j++)
            matrix[i][j] = STONE_UNDEFINED;
    }
}

void Matrix::clear()
{
    wxASSERT(size >= BOARD_SIZE_MIN && size <= BOARD_SIZE_MAX);

    for (unsigned short i=0; i<size; i++)
        for (unsigned short j=0; j<size; j++)
            matrix[i][j] = STONE_UNDEFINED;
}

#ifdef __WXDEBUG__
void Matrix::debug()
{
    for (unsigned short i=0; i<size; i++)
    {
        wxString s = wxEmptyString;
        for (unsigned short j=0; j<size; j++)
            s += wxString::Format("%d ", at(j, i));
        wxLogDebug(s);
    }
}
#endif

Matrix* Matrix::countScore()
{
    int i=0, j=0;
    terrWhite = 0;
    terrBlack = 0;
    dame = 0;

    // Create target matrix
    Matrix *matrix = new Matrix(size);

    // Find uncounted starting point
    while (true)
    {
        bool found = false;

        for (i=0; i<size; i++)
        {
            for (j=0; j<size; j++)
            {
                if (at(i, j) <= 0)
                {
                    found = true;
                    break;
                }
            }
            if (found)
                break;
        }

        if (!found)
            break;

        // Traverse the enclosed territory. Resulting color is in col afterwards
        Color col = STONE_UNDEFINED;
        traverseTerritory(i, j, col);

        // Now turn the result into real territory or dame points and store the
        // territory in the target matrix
        for (i=0; i<size; i++)
        {
            for (j=0; j<size; j++)
            {
                if (at(i, j) == MARK_TERRITORY_VISITED)
                {
                    // Black territory
                    if (col == STONE_BLACK)
                    {
                        matrix->set(i, j, STONE_BLACK);
                        terrBlack++;
                        set(i, j, MARK_TERRITORY_DONE_BLACK);
                    }
                    // White territory
                    else if (col == STONE_WHITE)
                    {
                        matrix->set(i, j, STONE_WHITE);
                        terrWhite++;
                        set(i, j, MARK_TERRITORY_DONE_WHITE);
                    }
                    // Dame
                    else
                    {
                        dame++;
                        set(i, j, MARK_TERRITORY_DONE_DAME);
                    }
                }
            }
        }
    }

    wxLogDebug("terrBlack = %d, terrWhite = %d, dame = %d", terrBlack, terrWhite, dame);

    return matrix;
}

void Matrix::traverseTerritory(int x, int y, Color &col)
{
    // Mark visited
    set(x, y, MARK_TERRITORY_VISITED);

    // North
    if (checkNeighbourTerritory(x, y-1, col))
        traverseTerritory(x, y-1, col);

    // East
    if (checkNeighbourTerritory(x+1, y, col))
        traverseTerritory(x+1, y, col);

    // South
    if (checkNeighbourTerritory(x, y+1, col))
        traverseTerritory(x, y+1, col);

    // West
    if (checkNeighbourTerritory(x-1, y, col))
        traverseTerritory(x-1, y, col);
}

bool Matrix::checkNeighbourTerritory(const int &x, const int &y, Color &col)
{
    // Off the board? Dont continue
    if (x < 0 || x >= size || y < 0 || y >= size)
        return false;

    // No stone? Continue
    if (at(x, y) <= 0)
        return true;

    // A stone, but no color found yet? Then set this color and dont continue
    // The stone must not be marked as alive in seki.
    if (col == STONE_UNDEFINED && at(x, y) > 0 && at(x, y) < MARK_SEKI)
    {
        col = static_cast<Color>(at(x, y));
        return false;
    }

    // A stone, but wrong color? Set abort flag but continue to mark the rest of the dame points
    int tmpCol = STONE_UNDEFINED;
    if (col == STONE_BLACK)
        tmpCol = STONE_WHITE;
    else if (col == STONE_WHITE)
        tmpCol = STONE_BLACK;
    if ((tmpCol == STONE_BLACK || tmpCol == STONE_WHITE) && at(x, y) == tmpCol)
    {
        col = STONE_REMOVED;
        return false;
    }

    // A stone, correct color, or already visited, or seki. Dont continue
    return false;
}

void Matrix::createFromIGSStatus(const wxArrayString &statusArray)
{
    const int size = statusArray.GetCount();
    wxLogDebug("create Matrix, size: %d", size);

    for (int row=0; row<size; row++)
    {
        wxString s = statusArray[row].Mid(7);
        for (int col = 0; col < size; col++)
        {
            short spot = s.GetChar(col) - '1' + 1;
#if 0
            // This can be used for "look" later but for scoring we only want marks
            // So look might need a parameter flag "addStones" or something
            if (spot == 0)
                set(row, col, STONE_BLACK);
            else if (spot == 1)
                set(row, col, STONE_WHITE);
#endif
            if (spot == 4)
                set(row, col, MARK_TERR_WHITE);
            else if (spot == 5)
                set(row, col, MARK_TERR_BLACK);
        }
    }
}
