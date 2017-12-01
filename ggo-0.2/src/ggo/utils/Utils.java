/*
 *  Utils.java
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

import java.io.File;
import java.util.ArrayList;
import ggo.Defines;
import ggo.gGo;

/**
 *  Common utilities
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.6 $, $Date: 2002/10/24 12:58:32 $
 */
public final class Utils implements Defines {
    //{{{ getExtension() methods
    /**
     *  Gets the extension of a file (without the dot)
     *
     *@param  f  File to check for extension
     *@return    The extension string, for example "sgf" or "txt"
     *@see       ggo.utils.Utils#getExtension(String)
     */
    public static String getExtension(File f) {
        return getExtension(f.getName());
    }

    /**
     *  Gets the extension of a file (without the dot)
     *
     *@param  s  Filename to check for extension
     *@return    The extension string, for example "sgf" or "txt"
     *@see       ggo.utils.Utils#getExtension(File)
     */
    public static String getExtension(String s) {
        String ext = null;
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1)
            ext = s.substring(i + 1).toLowerCase();

        return ext;
    } //}}}

    //{{{ coordsToKey() method
    /**
     *  Convert two coordinates into a key
     *
     *@param  x  X position
     *@param  y  Y position
     *@return    Key value as Long variable
     */
    public static Long coordsToKey(int x, int y) {
        return new Long(x * 100 + y);
    } //}}}

    //{{{ keyToCoords() method
    /**
     *  Convert a key into a Position with two coordinates
     *
     *@param  key  Key value as Long variable
     *@return      Position instance with the x/y coordinates
     */
    public static Position keyToCoords(Long key) {
        int x = (int)(key.longValue() / 100);
        int y = (int)key.longValue() - x * 100;
        return new Position(x, y);
    } //}}}

    //{{{ moveToString() method
    /**
     *  Convert a move to a String, like 10 (B K10) or 250 (W Pass)
     *
     *@param  number     Move number
     *@param  color      Move color
     *@param  x          X position
     *@param  y          Y position
     *@param  boardSize  Board size
     *@return            Converted string
     */
    public static String moveToString(int number, int color, int x, int y, int boardSize) {
        // Pass
        if (x == 20 && y == 20)
            return number + " (" + (color == STONE_BLACK ?
                    (gGo.getBoardResources().getString("black_short") + " ") :
                    (gGo.getBoardResources().getString("white_short") + " ")) +
                    " " + gGo.getBoardResources().getString("Pass") + ")";
        // Edit move
        else if (x == -1 && y == -1)
            return String.valueOf(number);
        // Invalid move
        else if (x < 1 || x > boardSize || y < 1 || y > boardSize)
            return "0";
        // Normal move
        else
            return String.valueOf(number) + " (" + (color == STONE_BLACK ?
                    (gGo.getBoardResources().getString("black_short") + " ") :
                    (gGo.getBoardResources().getString("white_short") + " ")) +
                    (char)('A' + (x < 9 ? x : x + 1) - 1) + String.valueOf(boardSize - y + 1) + ")";
    } //}}}

    //{{{ coordsToString() method
    /**
     *  Convert coordinates to a String, like 10/10 to kk
     *
     *@param  x  X position
     *@param  y  Y position
     *@return    Converted String
     */
    public static String coordsToString(int x, int y) {
        return String.valueOf((char)('a' + x)) + String.valueOf((char)('a' + y));
    } //}}}

    //{{{ convertStringToInt() method
    /**
     *  Convert a string to a number and valdidate the value
     *
     *@param  str  The string to convert
     *@return      The integer, or -1 if validation failed
     */
    public static int convertStringToInt(String str) {
        int number = 0;
        try {
            number = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            System.err.println("Failed to convert string to integer: " + e);
            return -1;
        } catch (NullPointerException e) {
            System.err.println("Failed to convert string to integer: " + e);
            return -1;
        }
        return number;
    } //}}}

    //{{{ convertStringToFloat() method
    /**
     *  Convert a string to a number and valdidate the value
     *
     *@param  str  The string to convert
     *@return      The float, or -1 if validation failed
     */
    public static float convertStringToFloat(String str) {
        float number = 0.0f;
        try {
            number = Float.parseFloat(str);
        } catch (NumberFormatException e) {
            System.err.println("Failed to convert string to float: " + e);
            return -1.0f;
        } catch (NullPointerException e) {
            System.err.println("Failed to convert string to float: " + e);
            return -1.0f;
        }
        return number;
    } //}}}

    //{{{ splitString() method
    // --- 1.3 ---
    /**
     *  Splits a string in an array of Strings. Reimplementation of 1.4 String.split() method
     *  for 1.3 usage
     *
     *@param  str  String to split
     *@param  cut  String to split the original String at
     *@return      Array of Strings
     */
    public static String[] splitString(String str, String cut) {
        ArrayList list = new ArrayList();
        str += cut;
        int pos = 0;
        int oldpos = 0;
        try {
            while ((pos = str.indexOf(cut, oldpos)) != -1) {
                list.add(str.substring(oldpos, pos).trim());
                oldpos = pos + 1;
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Problem parsing game move: " + e);
            return new String[]{str};
        }
        final int sz = list.size();
        String res[] = new String[sz];
        for (int i = 0; i < sz; i++)
            res[i] = (String)list.get(i);
        return res;
    } //}}}

    //{{{ formatTime() method
    /**
     *  Format a time to a string, like 4 -> "04"
     *
     *@param  t  Time in seconds
     *@return    Formatted time string
     */
    public static String formatTime(int t) {
        if (t < 10)
            return "0" + String.valueOf(t);
        return String.valueOf(t);
    } //}}}
}

