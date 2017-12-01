/*
 *  MarkNumber.java
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

import ggo.Defines;

/**
 *  Number mark
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class MarkNumber extends MarkText implements Defines {
    //{{{ private members
    private int num;
    //}}}

    //{{{ MarkNumber constructors
    /**
     *Constructor for the MarkNumber object
     *
     *@param  x    X coordinate of the mark
     *@param  y    Y coordinate of the mark
     *@param  txt  String with the number value
     */
    public MarkNumber(int x, int y, String txt) {
        super(x, y, txt);
        try {
            num = Integer.parseInt(txt);
        } catch (NumberFormatException e) {
            System.err.println("Failed to convert value '" + txt + "' of MarkNumber: " + e);
            num = 1;
            this.txt = "1";
        }
    }

    /**
     *Constructor for the MarkNumber object
     *
     *@param  x  X coordinate of the mark
     *@param  y  Y coordinate of the mark
     *@param  n  Number value
     */
    public MarkNumber(int x, int y, int n) {
        super(x, y, Integer.toString(n));
        num = n;
    } //}}}

    //{{{ getType() method
    public int getType() {
        return MARK_NUMBER;
    } //}}}

    //{{{ getNumber() method
    /**
     *  Get the number value of this mark
     *
     *@return    The number value
     */
    public int getNumber() {
        return num;
    } //}}}

    //{{{ setNumber() method
    /**
     *  Sets the number value of this mark
     *
     *@param  n  The new number value
     */
    public void setNumber(int n) {
        num = n;
    } //}}}
}

