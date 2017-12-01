/*
 *  MarkHandler.java
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

import java.util.*;
import ggo.*;
import ggo.utils.*;
import ggo.gui.marks.*;

/**
 *  Collection class to store the Marks on the board
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/10/17 19:05:48 $
 */
public class MarkHandler extends Hashtable implements Defines {
    //{{{ private members
    private boolean numberPool[], letterPool[];
    private int stringCounter;
    private String customText;
    //}}}

    //{{{ MarkHandler constructor
    /**Constructor for the MarkHandler object */
    public MarkHandler() {
        // Init number and letter pools
        numberPool = new boolean[400];
        letterPool = new boolean[52];
        for (int i = 0; i < 400; i++) {
            if (i < 52)
                letterPool[i] = false;
            numberPool[i] = false;
        }
        customText = null;
    } //}}}

    //{{{ clear() method
    /**  Clears the Hashtable and the letter and number pools */
    public void clear() {
        super.clear();
        for (int i = 0; i < 400; i++) {
            if (i < 52)
                letterPool[i] = false;
            numberPool[i] = false;
        }
    } //}}}

    //{{{ addMark() method
    /**
     *  Adds a mark
     *
     *@param  m  The Mark object to be added
     */
    public void addMark(Mark m) {
        put(Utils.coordsToKey(m.getX(), m.getY()), m);
    } //}}}

    //{{{ removeMark(Mark) method
    /**
     *  Removes a mark
     *
     *@param  m  The mark to be removed
     *@see       #removeMark(int, int)
     */
    public void removeMark(Mark m) {
        // Remove entry from number or letterpool if necessary
        if (m.getType() == MARK_TEXT || m.getType() == MARK_NUMBER) {
            MarkText tmp = (MarkText)m;
            if (tmp.getCounter() != -1) {
                if (tmp.getType() == MARK_TEXT)
                    letterPool[tmp.getCounter()] = false;
                else
                    numberPool[tmp.getCounter()] = false;
            }
        }

        remove(Utils.coordsToKey(m.getX(), m.getY()));
    } //}}}

    //{{{ removeMark(int, int) method
    /**
     *  Removes a mark
     *
     *@param  x  X position of the mark to be removed
     *@param  y  Y position of the mark to be removed
     *@see       #removeMark(Mark)
     */
    public void removeMark(int x, int y) {
        removeMark((Mark)get(Utils.coordsToKey(x, y)));
    } //}}}

    //{{{ hasMark(int, int) method
    /**
     *  Checks if a mark is existing at the given position
     *
     *@param  x  X coordinate of the position to check
     *@param  y  Y coordinate of the position to check
     *@return    True if mark exists, else false
     */
    public boolean hasMark(int x, int y) {
        return containsKey(Utils.coordsToKey(x, y));
    } //}}}

    //{{{ getMark() method
    /**
     *  Gets a mark from the Hashtable
     *
     *@param  x  Description of the Parameter
     *@param  y  Description of the Parameter
     *@return    The mark value
     */
    public Mark getMark(int x, int y) {
        return (Mark)get(Utils.coordsToKey(x, y));
    } //}}}

    //{{{ getNextNumber() method
    /**
     *  Gets the next available number for number marks
     *
     *@return    The next number value
     */
    int getNextNumber() {
        int n = 0;
        while (numberPool[n] && n < 399)
            n++;
        numberPool[n] = true;

        return n + 1;
    } //}}}

    //{{{ setNumberOccupied() method
    /**
     *  Sets a number as used. Needed for numbering all moves
     *
     *@param  n  The used number value
     */
    void setNumberOccupied(int n) {
        if (n < 1) // Should not happen
            n = 1;
        numberPool[n - 1] = true;
    } //}}}

    //{{{ getNextLetter() method
    /**
     *  Gets the next available letter for text marks, or a previously set customized
     *  text.
     *
     *@return    The letter value
     */
    String getNextLetter() {
        // Customized text
        if (customText != null) {
            String tmp = customText;
            customText = null;
            return tmp;
        }

        int n = 0;
        while (letterPool[n] && n < 51)
            n++;
        letterPool[n] = true;
        stringCounter = n;

        return String.valueOf((char)('A' + (n >= 26 ? n + 6 : n)));
    } //}}}

    //{{{ getStringCounter() method
    /**
     *  Gets the current available free letter for text marks
     *
     *@return    The letter
     */
    int getStringCounter() {
        return stringCounter;
    } //}}}

    //{{{ rememberCustomizedText() method
    /**
     *  Remember customized text for a textmark, forwarded to the next created textmark.
     *
     *@param  txt  Customized text from user input
     */
    void rememberCustomizedText(String txt) {
        customText = txt;

        // Check if the text can converted to a single letter. If yes, mark
        // the letter as occupied
        if (txt.length() == 1) {
            int n = -1;
            char c = txt.charAt(0);
            if (c >= 'A' && c <= 'Z')
                n = c - 'A';
            else if (c >= 'a' && c <= 'a')
                n = c - 'a' + 26;

            if (n > -1)
                letterPool[n] = true;
        }
    } //}}}
}

