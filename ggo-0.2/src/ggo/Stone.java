/*
 *  Stone.java
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

/**
 *  Object representing a single stone.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/10/17 19:04:50 $
 */
public class Stone implements Defines {
    //{{{ private members
    private int color;
    private int x, y;
    private boolean dead;
    private boolean seki;
    private boolean checked;
    //}}}

    //{{{ Stone constructor
    /**
     *  Constructor for the Stone object
     *
     *@param  x  X position on the board (1-19 for 19x19 boards)
     *@param  y  Y position on the board (1-19 for 19x19 boards)
     *@param  c  Description of the Parameter
     */
    public Stone(int c, int x, int y) {
        color = c;
        this.x = x;
        this.y = y;
        dead = false;
        seki = false;
        checked = false;
    } //}}}

    //{{{ getColor() method
    /**
     *  Gets the color attribute of the Stone object
     *
     *@return    The color value
     */
    public int getColor() {
        return color;
    } //}}}

    //{{{ setColor() method
    /**
     *  Sets the color attribute of the Stone object
     *
     *@param  c  The new color value
     */
    public void setColor(int c) {
        color = c;
    } //}}}

    //{{{ getX() method
    /**
     *  Gets the x attribute of the Stone object
     *
     *@return    The x value
     */
    public int getX() {
        return x;
    } //}}}

    //{{{ getY() method
    /**
     *  Gets the y attribute of the Stone object
     *
     *@return    The y value
     */
    public int getY() {
        return y;
    } //}}}

    //{{{ isDead() method
    /**
     *  Gets the dead attribute of the Stone object
     *
     *@return    The dead value
     */
    public boolean isDead() {
        return dead;
    } //}}}

    //{{{ setDead() method
    /**
     *  Sets the dead attribute of the Stone object
     *
     *@param  b  The new dead value
     */
    public void setDead(boolean b) {
        dead = b;
        seki = false;
    } //}}}

    //{{{ isSeki() method
    /**
     *  Gets the seki attribute of the Stone object
     *
     *@return    The seki value
     */
    public boolean isSeki() {
        return seki;
    } //}}}

    //{{{ setSeki() method
    /**
     *  Sets the seki attribute of the Stone object
     *
     *@param  b  The new seki value
     */
    public void setSeki(boolean b) {
        seki = b;
        dead = false;
    } //}}}

    //{{{ isChecked() method
    /**
     *  Gets the checked attribute of the Stone object
     *
     *@return    The checked value
     */
    public boolean isChecked() {
        return checked;
    } //}}}

    //{{{ setChecked() method
    /**
     *  Sets the checked attribute of the Stone object
     *
     *@param  c  The new checked value
     */
    public void setChecked(boolean c) {
        checked = c;
    } //}}}

    //{{{ equals() method
    /**
     *  Compares this stone with another
     *
     *@param  o  Other stone to compare with
     *@return    True if both objects are at the same position and have the same
     *           color
     */
    public boolean equals(Object o) {
        Stone other = (Stone)o;

        if (getX() == other.getX() &&
                getY() == other.getY() &&
                getColor() == other.getColor())
            return true;

        return false;
    } //}}}

    //{{{ toString() method
    /**
     *  Converts the stone object to a string
     *
     *@return    The converted strong
     */
    public String toString() {
        return getX() + "/" + getY() +
                (getColor() == STONE_BLACK ? " B" : " W");
    } //}}}
}

