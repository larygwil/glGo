/*
 *  Move.java
 *
 *  gGo
 *  Copyright (C) 2002  Peter Strempel <pstrempel@t-online.de>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package ggo;

import ggo.*;
import ggo.utils.*;
import java.io.Serializable;

/**
 *  Class representing one move on the board. A move contains a matrix showing
 *  the current board position, exactly one stone or a pass, and pointers to its
 *  parent, son and brothers.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/10/05 16:03:56 $
 */
public class Move implements Defines, Serializable {
    //{{{ public members
    /**  Pointer to right brother */
    public Move brother;
    /**  Pointer to son */
    public Move son;
    /**  Pointer to parent */
    public Move parent;
    /**  Marker for waypoint marking through the tree */
    public transient Move marker;
    //}}}

    //{{{ private members
    private int x;
    private int y;
    private int stoneColor;
    private int moveNum;
    private int capturesWhite, capturesBlack;
    private Matrix matrix;
    private int gameMode;
    private String comment;
    private boolean terrMarked;
    private boolean scored;
    private float scoreWhite, scoreBlack;
    //}}}

    //{{{ Move constructors

    //{{{ Move(int) constructor
    /**
     *  Constructor for the Move object
     *
     *@param  size  Description of the Parameter
     */
    public Move(int size) {
        brother = null;
        son = null;
        parent = null;
        marker = null;
        stoneColor = STONE_NONE;
        x = y = -1;
        gameMode = MODE_NORMAL;
        moveNum = 0;
        comment = "";
        terrMarked = false;
        capturesBlack = capturesWhite = 0;
        // checked = true;
        // fastLoadMarkDict = null;
        scored = false;
        scoreWhite = scoreBlack = 0.0f;

        matrix = new Matrix(size);
    } //}}}

    //{{{ Move(int, int, int, int, int, Matrix) constructor
    /**
     *  Constructor for the Move object
     *
     *@param  c     Description of the Parameter
     *@param  mx    Description of the Parameter
     *@param  my    Description of the Parameter
     *@param  n     Description of the Parameter
     *@param  mode  Description of the Parameter
     *@param  mat   Description of the Parameter
     */
    public Move(int c, int mx, int my, int n, int mode, Matrix mat) {
        this(c, mx, my, n, mode);

        matrix = new Matrix(mat);
        // Make all matrix values positive
        matrix.absMatrix();
    } //}}}

    //{{{ Move(int, int, int, int, int) constructor
    /**
     *  Constructor for the Move object
     *
     *@param  c     Description of the Parameter
     *@param  mx    Description of the Parameter
     *@param  my    Description of the Parameter
     *@param  n     Description of the Parameter
     *@param  mode  Description of the Parameter
     */
    public Move(int c, int mx, int my, int n, int mode) {
        brother = null;
        son = null;
        parent = null;
        marker = null;
        stoneColor = c;
        x = mx;
        y = my;
        gameMode = mode;
        moveNum = n;
        comment = "";
        terrMarked = false;
        capturesBlack = capturesWhite = 0;
        // checked = true;
        // fastLoadMarkDict = null;
        scored = false;
        scoreWhite = scoreBlack = 0.0f;

        matrix = null;
    } //}}}

    //}}}

    //{{{ Getter & Setter

    //{{{ getX() method
    /**
     *  Gets the x attribute of the Move object
     *
     *@return    The x value
     */
    public int getX() {
        return x;
    } //}}}

    //{{{ setX() method
    /**
     *  Sets the x attribute of the Move object
     *
     *@param  n  The new x value
     */
    public void setX(int n) {
        x = n;
    } //}}}

    //{{{ getY() method
    /**
     *  Gets the x attribute of the Move object
     *
     *@return    The x value
     */
    public int getY() {
        return y;
    } //}}}

    //{{{ setY() method
    /**
     *  Sets the y attribute of the Move object
     *
     *@param  n  The new y value
     */
    public void setY(int n) {
        y = n;
    } //}}}

    //{{{ getColor() method
    /**
     *  Gets the color attribute of the Move object
     *
     *@return    The color value
     */
    public int getColor() {
        return stoneColor;
    } //}}}

    //{{{ setColor() method
    /**
     *  Sets the color attribute of the Move object
     *
     *@param  c  The new color value
     */
    public void setColor(int c) {
        stoneColor = c;
    } //}}}

    //{{{ getCapturesBlack() method
    /**
     *  Gets the capturesBlack attribute of the Move object
     *
     *@return    The capturesBlack value
     */
    public int getCapturesBlack() {
        return capturesBlack;
    } //}}}

    //{{{ getCapturesWhite() method
    /**
     *  Gets the capturesWhite attribute of the Move object
     *
     *@return    The capturesWhite value
     */
    public int getCapturesWhite() {
        return capturesWhite;
    } //}}}

    //{{{ setCaptures() method
    /**
     *  Sets the captures attribute of the Move object
     *
     *@param  cb  The new captures value
     *@param  cw  The new captures value
     */
    public void setCaptures(int cb, int cw) {
        capturesBlack = cb;
        capturesWhite = cw;
    } //}}}

    //{{{ getMatrix() method
    /**
     *  Gets the matrix attribute of the Move object
     *
     *@return    The matrix value
     */
    public Matrix getMatrix() {
        return matrix;
    } //}}}

    //{{{ setMatrix() method
    /**
     *  Sets the matrix attribute of the Move object
     *
     *@param  m  The new matrix value
     */
    public void setMatrix(Matrix m) {
        matrix = m;
    } //}}}

    //{{{ setMoveNumber() method
    /**
     *  Sets the moveNumber attribute of the Move object
     *
     *@param  n  The new moveNumber value
     */
    public void setMoveNumber(int n) {
        moveNum = n;
    } //}}}

    //{{{ getMoveNumber() method
    /**
     *  Gets the moveNumber attribute of the Move object
     *
     *@return    The moveNumber value
     */
    public int getMoveNumber() {
        return moveNum;
    } //}}}

    //{{{ getGameMode() method
    /**
     *  Gets the gameMode attribute of the Move object
     *
     *@return    The gameMode value
     */
    public int getGameMode() {
        return gameMode;
    } //}}}

    //{{{ setGameMode() method
    /**
     *  Sets the gameMode attribute of the Move object
     *
     *@param  m  The new gameMode value
     */
    public void setGameMode(int m) {
        gameMode = m;
    } //}}}

    //{{{ getComment() method
    /**
     *  Gets the comment attribute of the Move object
     *
     *@return    The comment value
     */
    public String getComment() {
        return comment;
    } //}}}

    //{{{ setComment() method
    /**
     *  Sets the comment attribute of the Move object
     *
     *@param  s  The new comment value
     */
    public void setComment(String s) {
        comment = s;
    } //}}}

    //{{{ getScored() method
    /**
     *  Gets the scored attribute of the Move object
     *
     *@return    The scored value
     */
    public boolean isScored() {
        return scored;
    } //}}}

    //{{{ setScored() method
    /**
     *  Sets the scored attribute of the Move object
     *
     *@param  b  The new scored value
     */
    public void setScored(boolean b) {
        scored = b;
    } //}}}

    //{{{ setScore() method
    /**
     *  Sets the score attribute of the Move object
     *
     *@param  scoreBlack  The new score value
     *@param  scoreWhite  The new score value
     */
    public void setScore(float scoreBlack, float scoreWhite) {
        scored = true;
        this.scoreBlack = scoreBlack;
        this.scoreWhite = scoreWhite;
    } //}}}

    //{{{ getScoreBlack() method
    /**
     *  Gets the scoreBlack attribute of the Move object
     *
     *@return    The scoreBlack value
     */
    public float getScoreBlack() {
        return scoreBlack;
    } //}}}

    //{{{ getScoreWhite() method
    /**
     *  Gets the scoreWhite attribute of the Move object
     *
     *@return    The scoreWhite value
     */
    public float getScoreWhite() {
        return scoreWhite;
    } //}}}

    //{{{ isTerrMarked() method
    /**
     *  Gets the territoryMarked attribute of the Move object
     *
     *@return    The territoryMarked value
     */
    public boolean isTerritoryMarked() {
        return terrMarked;
    } //}}}

    //{{{ setTerrMarked() method
    /**
     *  Sets the territoryMarked attribute of the Move object
     *
     *@param  b  The new territoryMarked value
     */
    public void setTerritoryMarked(boolean b) {
        terrMarked = b;
    } //}}}

    //}}}

    //{{{ equals() method
    /**
     *  Compares this Move object with another
     *
     *@param  o  Description of the Parameter
     *@return    true if equal, else false
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;

        Move m = null;
        try {
            m = (Move)o;
        } catch (ClassCastException e) {
            System.err.println("Move.equals - Failed to cast parameter object: " + e);
            return false;
        }

        if (x == m.getX() && y == m.getY() &&
                stoneColor == m.getColor() &&
                moveNum == m.getMoveNumber() &&
                gameMode == m.getGameMode())
            return true;

        return false;
    } //}}}

    //{{{ saveMove() method
    /**
     *  Description of the Method
     *
     *@param  isRoot  Description of the Parameter
     *@return         Description of the Return Value
     */
    public String saveMove(boolean isRoot) {
        StringBuffer str = new StringBuffer();

        if (!isRoot)
            str.append("\n;");

        if (x != -1 && y != -1 && gameMode == MODE_NORMAL) {
            // Write something like 'B[aa]'
            str.append(stoneColor == STONE_BLACK ? "B" : "W");
            str.append("[" + Utils.coordsToString(x - 1, y - 1) + "]");
        }

        // Save edited moves
        str.append(matrix.saveEditedMoves(parent != null ? parent.getMatrix() : null));

        // Save marks
        str.append(matrix.saveMarks());

        // Add comment, if we have one
        if (comment != null && comment.length() > 0) {
            // Replace "]" with "\]"
            StringBuffer tmp = new StringBuffer(comment);
            int pos = 0;
            while ((pos = tmp.toString().indexOf("]", pos)) != -1 && pos < tmp.length()) {
                tmp.replace(pos, pos+1, "\\]");
                pos += 2;
            }
            str.append("C[" + tmp.toString() + "]");
        }

        // System.err.println("SAVE MOVE: " + str);

        return str.toString();
    } //}}}

    //{{{ toString() method
    /**
     *  Converts the move data to a string. For debugging.
     *
     *@return    Converted string
     */
    public String toString() {
        return "Move number = " + moveNum + ", " + x + "/" + y +
                " " + (stoneColor == STONE_BLACK ? "B" : "W") +
                "\nMode = " + (gameMode == MODE_NORMAL ? "Normal" : "Edit") +
                "\nScored = " + scored + " W: " + scoreWhite + " B: " + scoreBlack +
                "\nCaps W: " + capturesWhite + "  B: " + capturesBlack +
                "\n" + matrix;
    } //}}}
}

