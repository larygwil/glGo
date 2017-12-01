/*
 *  Couple.java
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
 *  A simple class representing a couple of players.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class Couple extends Object {
    private String whiteName, blackName;

    /**
     *Constructor for the Couple object
     *
     *@param  whiteName  Description of the Parameter
     *@param  blackName  Description of the Parameter
     */
    public Couple(String whiteName, String blackName) {
        this.whiteName = whiteName;
        this.blackName = blackName;
    }

    /**
     *  Gets the whiteName attribute of the Couple object
     *
     *@return    The whiteName value
     */
    public String getWhiteName() {
        return whiteName;
    }

    /**
     *  Gets the blackName attribute of the Couple object
     *
     *@return    The blackName value
     */
    public String getBlackName() {
        return blackName;
    }

    /**
     *  Overwrites Object.equals(Object). Tests if two instances of this class are equals.
     *  Required for key checks in the adjourn Hashtable.
     *
     *@param  obj  Object to compare with
     *@return      True if both objects are equals, else false
     */
    public boolean equals(Object obj) {
        if (((Couple)obj).getWhiteName().equals(whiteName) &&
                ((Couple)obj).getBlackName().equals(blackName))
            return true;
        return false;
    }


    /**
     *  Overwrites Object.hashCode().
     *  Required for key checks in the adjourn Hashtable.
     *
     *@return    Hash code for this instance
     */
    public int hashCode() {
        // Quick and dirty. But the comparison should rely on equals, not hashCode()
        return 123;
    }

    /**
     *  Convert this class to a String. For debugging.
     *
     *@return    Converted String
     */
    public String toString() {
        return "[" + whiteName + "-" + blackName + "]";
    }
}

