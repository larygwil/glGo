/*
 *  HashMatrix.java
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
package ggo.utils;

import ggo.Matrix;
import ggo.utils.Position;
import java.util.*;

/**
 *  This class is a storage in form of a HashMap for a Matrix object, used for
 *  serialization of a game tree. Serializing in form of a HashMap is more
 *  efficient than as Matrix, nevertheless operating on the Matrix is more
 *  performant than operating on a HashMap, so this class is not used for
 *  normal gGo operations.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/06 15:54:56 $
 */
public class HashMatrix extends HashMap {
    //{{{ private members
    private int size;
    private Hashtable markTexts;
    //}}}

    //{{{ HashMatrix(Matrix) constructor
    /**
     *Constructor for the HashMatrix object
     *
     *@param  matrix  Matrix to create this object from
     */
    public HashMatrix(Matrix matrix) {
        size = matrix.getSize();
        markTexts = matrix.getMarkTexts();

        for (short i = 0; i < size; i++)
            for (short j = 0; j < size; j++)
                if (matrix.at(i, j) != 0)
                    put(coordsToKey(i, j), new Short(matrix.at(i, j)));
    } //}}}

    //{{{ getSize() method
    /**
     *  Gets the size attribute of the HashMatrix object
     *
     *@return    The size value
     */
    public int getSize() {
        return size;
    } //}}}

    //{{{ getMarkTexts() method
    /**
     *  Gets the markTexts attribute of the HashMatrix object
     *
     *@return    The markTexts value
     */
    public Hashtable getMarkTexts() {
        return markTexts;
    } //}}}

    //{{{ coordsToKey() method
    /**
     *  Convert coordinates to Short key
     *
     *@param  x  X position
     *@param  y  Y position
     *@return    Short key object
     */
    public static Short coordsToKey(short x, short y) {
        return new Short((short)(x * 100 + y));
    } //}}}

    //{{{ keyToCoords() method
    /**
     *  Convert a Short key object to a Position
     *
     *@param  key  Key object
     *@return      Converted Position object
     */
    public static Position keyToCoords(Short key) {
        int x = key.intValue() / 100;
        int y = key.intValue() - x * 100;
        return new Position(x, y);
    } //}}}
}

