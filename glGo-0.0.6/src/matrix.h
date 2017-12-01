/*
 * matrix.h
 */

#ifndef MATRIX_H
#define MATRIX_H

#define MARK_TERRITORY_VISITED     99
#define MARK_TERRITORY_DONE_BLACK 996
#define MARK_TERRITORY_DONE_WHITE 997
#define MARK_TERRITORY_DONE_DAME  998
#define MARK_TERRITORY_FALSE_EYE  999
#define MARK_SEKI                  10


/**
 * Matrix for score calculation.
 * This class is similar to the gGo/Java Matrix class but used differently, as unlike
 * the Java version glGo does not store a matrix of each move of the game tree.
 * To score a move create a Matrix from the current position and call countScore().
 * This class is also used for the IGS "status" command output to display the score
 * of observed games (and possible "look" command later, but this is not yet implemented).
 */
class Matrix
{
public:
    /** Constructor */
    Matrix(unsigned short s = DEFAULT_BOARD_SIZE);

    /** Copy constructor */
    Matrix(const Matrix &m);

    /** Destructor */
    ~Matrix();

    /** Gets the matrix size */
    unsigned short getSize() const { return size; }

    /** Clear matrix */
    void clear();

    /** Gets the matrix value of x/y position */
    short at(short x, short y) const
        {
            wxASSERT(x >= 0 && x < size && y >= 0 && y < size);
            return matrix[x][y];
        }

    /** Sets the matrix value at x/y position */
    void set(short x, short y, short n)
        {
            wxASSERT(x >= 0 && x < size && y >= 0 && y < size);
            matrix[x][y] = n;
        }

    /** Count score on matrix position. This is called from BoardHandler after filling the
     * matrix with the current position. The resulting territory is written into another
     * matrix which is returned by this function. Territory and dame values can be accessed
     * after calling this function with getTerrWhite(), getTerrBlack() and getDame()
     * @return Matrix containing territory marks
     */
    Matrix* countScore();

    /**
     * Gets the white territory. Only valid after a call to countScore().
     * @see countScore()
     */
    int getTerrWhite() const { return terrWhite; }

    /**
     * Gets the black territory. Only valid after a call to countScore().
     * @see countScore()
     */
    int getTerrBlack() const { return terrBlack; }

    /**
     * Gets the number of dame points. Only valid after a call to countScore().
     * @see countScore()
     */
    int getDame() const { return dame; }

    /** Create a matrix from the IGS status output. */
    void createFromIGSStatus(const wxArrayString &statusArray);

#ifdef __WXDEBUG__
    void debug();
#endif

protected:
    void init();
    void traverseTerritory(int x, int y, Color &col);
    bool checkNeighbourTerritory(const int &x, const int &y, Color &col);

private:
    short **matrix;
    unsigned short size;
    int terrWhite, terrBlack, dame;
};

#endif
