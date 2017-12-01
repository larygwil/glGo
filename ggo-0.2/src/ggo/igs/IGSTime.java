/*
 *  IGSTime.java
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
package ggo.igs;

/**
 *  Class describing the time used on IGS: Absolute time plus byo-yomi period with n stones.
 *  The time attribute means either absolute time OR byoyomi time. In first case, stones is -1.
 *  The initByoTime attribute is only used to init the clocks properly. Byo-time during a game
 *  uses the time attribute.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class IGSTime {
    //{{{ private members
    private int time, stones, initByoTime;
    //}}}

    //{{{ IGSTime(int, int) constructor
    /**
     *Constructor for the IGSTime object
     *
     *@param  t  Time value
     *@param  s  Number of stones in period
     */
    public IGSTime(int t, int s) {
        time = t;
        stones = s;
        initByoTime = -1;
    } //}}}

    //{{{ IGSTime(int, int, int) constructor
    /**
     *Constructor for the IGSTime object
     *
     *@param  t  Description of the Parameter
     *@param  b  Description of the Parameter
     *@param  s  Description of the Parameter
     */
    public IGSTime(int t, int b, int s) {
        this(t, s);
        initByoTime = b;
    } //}}}

    //{{{ getTime() method
    /**
     *  Gets the time value
     *
     *@return    The time value
     */
    public int getTime() {
        return time;
    } //}}}

    //{{{ setTime() method
    /**
     *  Sets the time attribute of the IGSTime object
     *
     *@param  t  The new time value
     */
    public void setTime(int t) {
        time = t;
    } //}}}

    //{{{ getStones() method
    /**
     *  Gets the number of stones per period
     *
     *@return    The number of stones
     */
    public int getStones() {
        return stones;
    } //}}}

    //{{{ setStones() method
    /**
     *  Sets the stones attribute of the IGSTime object
     *
     *@param  s  The new stones value
     */
    public void setStones(int s) {
        stones = s;
    } //}}}

    //{{{ getInitByoTime() method
    /**
     *  Gets the initByoTime attribute of the IGSTime object
     *
     *@return    The initByoTime value
     */
    public int getInitByoTime() {
        return initByoTime;
    } //}}}

    //{{{ setInitByoTime() method
    /**
     *  Sets the initByoTime attribute of the IGSTime object
     *
     *@param  b  The new initByoTime value
     */
    public void setInitByoTime(int b) {
        initByoTime = b;
    } //}}}

    //{{{ toString() method
    /**
     *  Convert this object to a String. For debugging.
     *
     *@return    Converted String
     */
    public String toString() {
        return "[Time = " + time + ", Stones = " + stones + ", initByoTime = " + initByoTime + "]";
    } //}}}
}

