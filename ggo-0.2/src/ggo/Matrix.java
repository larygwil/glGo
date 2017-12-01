/*
 *  Matrix.java
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
import java.util.*;
import java.io.*;

/**
 *  Matrix storing all stones and mark information of one node.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.5 $, $Date: 2002/10/17 19:05:06 $
 */
public class Matrix implements Defines, Externalizable {
    //{{{ private members
    private short[][] matrix;
    private int size;
    private Hashtable markTexts;
    //}}}

    //{{{ Matrix constructors

    //{{{ Marix() default constructor
    /**Constructor for the Matrix object */
    public Matrix() {
        size = 0;
        markTexts = null;
    } //}}}

    //{{{ Matrix(int) constructor
    /**
     *  Constructor for the Matrix object
     *
     *@param  s  Board size
     */
    public Matrix(int s) {
        size = s;
        assertSize();
        matrix = new short[size][size];

        markTexts = null;
    } //}}}

    //{{{ Matrix(Matrix) constructor
    /**
     *  Copy constructor for the Matrix object
     *
     *@param  m  Original matrix object to copy
     */
    public Matrix(final Matrix m) {
        size = m.getSize();

        assertSize();

        matrix = new short[size][size];

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                matrix[i][j] = m.at(i, j);

        markTexts = null;
    } //}}}

    //}}}

    //{{{ assertSize() method
    /**  Check valid board size. If failed, exit application. */
    private void assertSize() {
        if (size < 0 || size > 36) {
            System.err.println("Error in Matrix: Invalid board size: " + size);
            gGo.exitApp(1);
        }
    } //}}}

    //{{{ assertXY() method
    /**
     *  Check valid x/y values. If failed, exit application.
     *
     *@param  x  Description of the Parameter
     *@param  y  Description of the Parameter
     */
    private void assertXY(int x, int y) {
        if (!(x >= 0 && x < size && y >= 0 && y < size)) {
            System.err.println("Error in Matrix: Invalid x/y value: " + x + "/" + y);
            gGo.exitApp(1);
        }
    } //}}}

    //{{{ getSize() method
    /**
     *  Gets the board size
     *
     *@return    The board size value
     */
    public int getSize() {
        return size;
    } //}}}

    //{{{ clear() method
    /**  Clear matrix */
    public void clear() {
        assertSize();

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                matrix[i][j] = STONE_NONE;
    } //}}}

    //{{{ at() method
    /**
     *  Get matrix value at a given position
     *
     *@param  x  X position
     *@param  y  Y position
     *@return    Value stored in matrix
     */
    public short at(int x, int y) {
        assertXY(x, y);

        return matrix[x][y];
    } //}}}

    //{{{ set() method
    /**
     *  Set a matrix value at a given position
     *
     *@param  x  X position
     *@param  y  Y position
     *@param  n  New matrix value
     */
    public void set(int x, int y, int n) {
        assertXY(x, y);

        matrix[x][y] = (short)n;
    } //}}}

    //{{{ insertStone() method
    /**
     *  Insert a stone at a given position
     *
     *@param  x     X position of stone
     *@param  y     Y position of stone
     *@param  c     Stone color
     *@param  mode  Game mode used to place stone
     */
    public void insertStone(int x, int y, int c, int mode) {
        assertXY(x - 1, y - 1);

        matrix[x - 1][y - 1] = (short)(Math.abs(matrix[x - 1][y - 1] / 10 * 10) + c);
        if (mode == MODE_EDIT)
            matrix[x - 1][y - 1] *= -1;
    } //}}}

    //{{{ removeStone() method
    /**
     *  Remve a stone at a given position
     *
     *@param  x  X position of stone
     *@param  y  Y position of stone
     */
    public void removeStone(int x, int y) {
        assertXY(x - 1, y - 1);

        matrix[x - 1][y - 1] = (short)Math.abs(matrix[x - 1][y - 1] / 10 * 10);
    } //}}}

    //{{{ eraseStone() method
    /**
     *  Erase a stone at a given position. Sets erased flag in matrix.
     *
     *@param  x  X position of stone
     *@param  y  Y position of stone
     */
    public void eraseStone(int x, int y) {
        assertXY(x - 1, y - 1);

        matrix[x - 1][y - 1] = (short)((Math.abs(matrix[x - 1][y - 1] / 10 * 10) + STONE_ERASE) * -1);
    } //}}}

    //{{{ insertMark() method
    /**
     *  Insert a mark at a given position
     *
     *@param  x  X position of mark
     *@param  y  Y position of mark
     *@param  t  Mark type
     */
    public void insertMark(int x, int y, int t) {
        assertXY(x - 1, y - 1);
        matrix[x - 1][y - 1] = (short)((Math.abs(matrix[x - 1][y - 1]) + 10 * t) * (matrix[x - 1][y - 1] < 0 ? -1 : 1));
    } //}}}

    //{{{ removeMark() method
    /**
     *  Remove a mark at a given position
     *
     *@param  x  X position of mark
     *@param  y  Y position of mark
     */
    public void removeMark(int x, int y) {
        assertXY(x - 1, y - 1);
        matrix[x - 1][y - 1] %= 10;
        if (markTexts != null && !markTexts.isEmpty())
            markTexts.remove(Utils.coordsToKey(x, y));
    } //}}}

    //{{{ clearAllMarks() method
    /**  Clear all marks in this matrix */
    public void clearAllMarks() {
        assertSize();

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                matrix[i][j] %= 10;

        if (markTexts != null) {
            markTexts.clear();
            markTexts = null;
        }
    } //}}}

    //{{{ clearTerritoryMarks() method
    /**  Clear all territory marks in this matrix */
    public void clearTerritoryMarks() {
        assertSize();

        short data;

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if ((data = (short)(Math.abs(matrix[i][j] / 10))) == MARK_TERR_BLACK ||
                        data == MARK_TERR_WHITE)
                    matrix[i][j] %= 10;
    } //}}}

    //{{{ absMatrix() method
    /**  Set all matrix fields to their absolute value */
    public void absMatrix() {
        assertSize();

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {
                matrix[i][j] = (short)Math.abs(matrix[i][j]);
                if (matrix[i][j] == STONE_ERASE)
                    matrix[i][j] = STONE_NONE;

                if (matrix[i][j] % 10 == STONE_ERASE)
                    matrix[i][j] = (short)(matrix[i][j] / 10 * 10);
            }
    } //}}}

    //{{{ setMarkText() method
    /**
     *  Set text of a textmark at a given position
     *
     *@param  x    X position
     *@param  y    Y position
     *@param  txt  Text of the mark
     */
    public void setMarkText(int x, int y, String txt) {
        assertXY(x - 1, y - 1);

        // We only create the hashtable if we really need it.
        if (markTexts == null)
            markTexts = new Hashtable();

        markTexts.put(Utils.coordsToKey(x, y), txt);
    } //}}}

    //{{{ getMarkText() method
    /**
     *  Get text of a textmark at a given position
     *
     *@param  x  X position
     *@param  y  Y position
     *@return    Text of the mark
     */
    public String getMarkText(int x, int y) {
        // We didn't store any texts in this matrix.
        if (markTexts == null || markTexts.isEmpty())
            return null;

        return (String)markTexts.get(Utils.coordsToKey(x, y));
    } //}}}

    //{{{ getMarkTexts() method
    /**
     *  Gets the markTexts attribute of the Matrix object
     *
     *@return    The markTexts value
     */
    public Hashtable getMarkTexts() {
        return markTexts;
    } //}}}

    //{{{ saveMarks() method
    /**
     *  Save all marks in sgf format, called from sgf writer
     *
     *@return    String with sgf mark notation
     */
    public String saveMarks() {
        assertSize();

        String txt;
        String sSQ = "";
        String sCR = "";
        String sTR = "";
        String sMA = "";
        String sLB = "";
        String sTB = "";
        String sTW = "";

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                switch (Math.abs(matrix[i][j] / 10)) {
                    case MARK_SQUARE:
                        if (sSQ.length() == 0)
                            sSQ += "SQ";
                        sSQ += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                    case MARK_CIRCLE:
                        if (sCR.length() == 0)
                            sCR += "CR";
                        sCR += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                    case MARK_TRIANGLE:
                        if (sTR.length() == 0)
                            sTR += "TR";
                        sTR += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                    case MARK_CROSS:
                        if (sMA.length() == 0)
                            sMA += "MA";
                        sMA += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                    case MARK_TEXT:
                    case MARK_NUMBER:
                        if (sLB.length() == 0)
                            sLB += "LB";
                        sLB += "[" + Utils.coordsToString(i, j);
                        sLB += ":";
                        txt = getMarkText(i + 1, j + 1);
                        if (txt == null || txt.length() == 0)
                            sLB += "?"; // Whoops
                        else
                            sLB += txt;
                        sLB += "]";
                        break;
                    case MARK_TERR_BLACK:
                        if (sTB.length() == 0)
                            sTB += "TB";
                        sTB += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                    case MARK_TERR_WHITE:
                        if (sTW.length() == 0)
                            sTW += "TW";
                        sTW += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                    default:
                        continue;
                }
            }
        }

        return sSQ + sCR + sTR + sMA + sLB + sTB + sTW;
    } //}}}

    //{{{ saveEditedMoves() method
    /**
     *  Save moves that were set in edit mode, called from sgf writer
     *
     *@param  parent  Matrix of parent move
     *@return         String with sgf notation
     */
    protected String saveEditedMoves(Matrix parent) {
        assertSize();

        String sAB = "";
        String sAW = "";
        String sAE = "";

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                switch (matrix[i][j] % 10) {
                    case STONE_BLACK * -1:
                        if (parent != null &&
                                parent.at(i, j) == STONE_BLACK)
                            break;
                        if (sAB.length() == 0)
                            sAB += "AB";
                        sAB += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                    case STONE_WHITE * -1:
                        if (parent != null &&
                                parent.at(i, j) == STONE_WHITE)
                            break;
                        if (sAW.length() == 0)
                            sAW += "AW";
                        sAW += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                    case STONE_ERASE * -1:
                        if (parent != null &&
                                (parent.at(i, j) == STONE_NONE ||
                                parent.at(i, j) == STONE_ERASE))
                            break;
                        if (sAE.length() == 0)
                            sAE += "AE";
                        sAE += "[" + Utils.coordsToString(i, j) + "]";
                        break;
                }
            }
        }

        // System.err.println("SAVEEDITEDMOVES: " + sAB + sAW + sAE);
        return sAB + sAW + sAE;
    } //}}}

    //{{{ toString() method
    /**
     *  Convert this matrix to a string, used for debugging
     *
     *@return    Converted String
     */
    public String toString() {
        assertSize();

        StringBuffer output = new StringBuffer();

        for (int i = 0; i < size; i++)
            output.append((i + 1) % 10).append(" ");

        output.append("\n");

        for (int i = 0; i < size; i++) {
            output.append((i + 1) % 10 + " ");
            for (int j = 0; j < size; j++)
                /*
                 *  switch (abs(matrix[j][i]))
                 *  {
                 *  case stoneNone:
                 *  case stoneErase: System.err.println("". "; break;
                 *  case stoneBlack: System.err.println(""B "; break;
                 *  case stoneWhite: System.err.println(""W "; break;
                 *  case markSquare*10: System.err.println(""[ "; break;
                 *  case markCircle*10: System.err.println(""O "; break;
                 *  case markTriangle*10: System.err.println(""T "; break;
                 *  case markCross*10: System.err.println(""X "; break;
                 *  case markText*10: System.err.println(""A "; break;
                 *  case markNumber*10: System.err.println(""1 "; break;
                 *  case markSquare*10+stoneBlack: System.err.println(""S "; break;
                 *  case markCircle*10+stoneBlack: System.err.println(""C "; break;
                 *  case markTriangle*10+stoneBlack: System.err.println(""D "; break;
                 *  case markCross*10+stoneBlack: System.err.println(""R "; break;
                 *  case markText*10+stoneBlack: System.err.println(""A "; break;
                 *  case markNumber*10+stoneBlack: System.err.println(""N "; break;
                 *  case markSquare*10+stoneWhite: System.err.println(""s "; break;
                 *  case markCircle*10+stoneWhite: System.err.println(""c "; break;
                 *  case markTriangle*10+stoneWhite: System.err.println(""d "; break;
                 *  case markCross*10+stoneWhite: System.err.println(""r "; break;
                 *  case markText*10+stoneWhite: System.err.println(""a "; break;
                 *  case markNumber*10+stoneWhite: System.err.println(""n "; break;
                 *  default: System.err.println(""? ";
                 *  }
                 */
                output.append(matrix[j][i] + " ");

            output.append((i + 1) % 10 + "\n");
        }

        output.append("  ");
        for (int i = 0; i < size; i++)
            output.append((i + 1) % 10 + " ");

        output.append("\nMarktexts:\n").append(markTexts);

        return output.toString();
    } //}}}

    //{{{ writeExternal() method
    /**
     *  Write content of this class to ObjectOutput
     *
     *@param  out              Stream to write this object to
     *@exception  IOException  Any IOException that may occur
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(new HashMatrix(this));
    } //}}}

    //{{{ readExternal() method
    /**
     *  Restore content of this class from ObjectInput
     *
     *@param  in                          Stream to read data from to restore this object
     *@exception  IOException             Any IOException that may occur
     *@exception  ClassNotFoundException  If the class to restore cannot be found
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        HashMatrix hashMatrix = (HashMatrix)in.readObject();

        size = hashMatrix.getSize();
        assertSize();
        markTexts = hashMatrix.getMarkTexts();
        matrix = new short[size][size];
        for (Iterator i = hashMatrix.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry)i.next();
            Position pos = HashMatrix.keyToCoords((Short)e.getKey());
            matrix[pos.x][pos.y] = ((Short)e.getValue()).shortValue();
        }
    } //}}}
}

