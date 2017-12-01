/*
 *  Group.java
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
import java.util.*;

/**
 *  A group of stones, used to check for liberties and captures of a whole
 *  group.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class Group extends ArrayList {
    //{{{ public members
    private int liberties;
    //}}}

    //{{{ Group contructor
    /**  Constructor for the Group object */
    public Group() {
        liberties = 0;
    } //}}}

    //{{{ getLiberties() method
    /**
     *  Gets the liberties attribute of the Group object
     *
     *@return    The liberties value
     */
    public int getLiberties() {
        return liberties;
    } //}}}

    //{{{ setLiberties() method
    /**
     *  Sets the liberties attribute of the Group object
     *
     *@param  l  The new liberties value
     */
    public void setLiberties(int l) {
        liberties = l;
    } //}}}

    //{{{ isAttachedTo() method
    /**
     *  Checks if this group is attached to a given stone
     *
     *@param  s  Stone to check
     *@return    True if the group is attached to the given stone, else false
     */
    public boolean isAttachedTo(Stone s) {
        int stoneX = s.getX();
        int stoneY = s.getY();
        int x;
        int y;
        int col = s.getColor();
        int c;
        Stone tmp;

        if (isEmpty())
            return false;

        for (int i = 0; i < size(); i++) {
            tmp = (Stone)get(i);
            x = tmp.getX();
            y = tmp.getY();
            c = tmp.getColor();

            if (((stoneX == x && (stoneY == y - 1 || stoneY == y + 1)) ||
                    (stoneY == y && (stoneX == x - 1 || stoneX == x + 1))) &&
                    c == col)
                return true;
        }

        return false;
    } //}}}

    //{{{ containsStone() method
    /**
     *  Check if this group contains the given stone
     *
     *@param  stone  Stone to check for
     *@return        True if the group contains the given stone, else fase
     */
    public boolean containsStone(Stone stone) {
        Stone s;
        for (Iterator it = iterator(); it.hasNext(); ) {
            s = (Stone)it.next();
            if (stone == s)
                return true;
        }
        return false;
    } //}}}

    //{{{ toString() method
    /**
     *  Converts the group to a string
     *
     *@return    Converted string
     */
    public String toString() {
        Stone s;
        String str = "GROUP:\n";
        for (Iterator it = iterator(); it.hasNext(); ) {
            s = (Stone)it.next();
            str += s + " ";
        }
        return str;
    } //}}}
}

