/*
 *  Position.java
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
package ggo.utils;

/**
 *  Simple class providing a position defined by X and Y coordinates
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:56 $
 */
public class Position {
    //{{{ public members
    /**  X position */
    public int x;
    /**  Y position */
    public int y;
    //}}}

    //{{{ Position() constructor
    /**
     *Constructor for the Position object
     *
     *@param  x  X position
     *@param  y  Y position
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    } //}}}

    //{{{ toString() method
    /**
     *  Convert to String, for debugging
     *
     *@return    Converted String
     */
    public String toString() {
        return "[" + x + "/" + y + "]";
    } //}}}
}

