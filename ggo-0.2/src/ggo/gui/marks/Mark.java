/*
 *  Mark.java
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
package ggo.gui.marks;

import java.awt.*;
import ggo.Defines;

/**
 *  Abstract superclass for marks. Marks have to be instantiated as one of the
 *  following subclasses:
 *  <ul><li>MarkSquare(int x, int y)</li>
 *  <li>MarkCross(int x, int y)</li>
 *  <li>MarkTriangle(int x, int y)</li>
 *  <li>MarkCircle(int x, int y)</li>
 *  <li>MarkText(int x, int y, String txt)</li>
 *  <li>MarkNumber(int x, int y, int number) (TODO)</li></ul>
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public abstract class Mark {
    //{{{ protected members
    /**  X coordinate of the mark on the board */
    protected int x;
    /**  Y coordinate of the mark on the board */
    protected int y;
    //}}}

    //{{{ abstract methods
    /**
     *  Returns the mark type. Overwritten by subclasses.
     *
     *@return    The mark type
     */
    public abstract int getType();

    /**
     *  Implementation of the actual drawing. Overwritten by subclasses.
     *
     *@param  g  Graphics object to draw on
     *@param  x  X coordinate of the mark
     *@param  y  Y coordinate of the mark
     *@param  w  Width of the square the mark is drawn on
     *@param  h  Height of the square the mark is drawn on
     */
    public abstract void drawShape(Graphics2D g, int x, int y, int w, int h);
    //}}}

    //{{{ getX() method
    /**
     *  Gets the x coordinate of the Mark object
     *
     *@return    The x coordinate
     */
    public int getX() {
        return x;
    } //}}}

    //{{{ getY() method
    /**
     *  Gets the y coordinate of the Mark object
     *
     *@return    The y coordinate
     */
    public int getY() {
        return y;
    } //}}}
}

