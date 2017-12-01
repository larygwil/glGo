/*
 *  GTPConfig.java
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
package ggo.gtp;

import ggo.Defines;

/**
 *  Configuration for GTP games
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class GTPConfig implements Defines {
    //{{{ private members
    private int size, handicap, level, black, white, mainTime, byoYomiTime, byoYomiStones;
    private float komi;
    private String resumeFileName;
    //}}}

    //{{{ GTPConfig() constructor
    /**Constructor for the GTPConfig object */
    public GTPConfig() {
        size = 19;
        handicap = 0;
        komi = 5.5f;
        level = 10;
        black = GTP_HUMAN;
        white = GTP_COMPUTER;
        mainTime = 10 * 60;
        byoYomiTime = 10 * 60;
        byoYomiStones = 25;
        resumeFileName = null;
    } //}}}

    //{{{ Getter & Setter
    /**
     *  Gets the size attribute of the GTPConfig object
     *
     *@return    The size value
     */
    public int getSize() {
        return size;
    }

    /**
     *  Sets the size attribute of the GTPConfig object
     *
     *@param  size  The new size value
     */
    public void setSize(int size) {
        if (size == -1)
            size = 19;
        this.size = size;
    }

    /**
     *  Gets the handicap attribute of the GTPConfig object
     *
     *@return    The handicap value
     */
    public int getHandicap() {
        return handicap;
    }

    /**
     *  Sets the handicap attribute of the GTPConfig object
     *
     *@param  handicap  The new handicap value
     */
    public void setHandicap(int handicap) {
        if (handicap == -1)
            handicap = 0;
        this.handicap = handicap;
    }

    /**
     *  Gets the black attribute of the GTPConfig object
     *
     *@return    The black value
     */
    public int getBlack() {
        return black;
    }

    /**
     *  Sets the black attribute of the GTPConfig object
     *
     *@param  black  The new black value
     */
    public void setBlack(int black) {
        if (black == -1)
            black = GTP_HUMAN;
        this.black = black;
    }

    /**
     *  Gets the white attribute of the GTPConfig object
     *
     *@return    The white value
     */
    public int getWhite() {
        return white;
    }

    /**
     *  Sets the white attribute of the GTPConfig object
     *
     *@param  white  The new white value
     */
    public void setWhite(int white) {
        if (white == -1)
            white = GTP_COMPUTER;
        this.white = white;
    }

    /**
     *  Gets the level attribute of the GTPConfig object
     *
     *@return    The level value
     */
    public int getLevel() {
        return level;
    }

    /**
     *  Sets the level attribute of the GTPConfig object
     *
     *@param  level  The new level value
     */
    public void setLevel(int level) {
        if (level == -1)
            level = 10;
        this.level = level;
    }

    /**
     *  Gets the komi attribute of the GTPConfig object
     *
     *@return    The komi value
     */
    public float getKomi() {
        return komi;
    }

    /**
     *  Sets the komi attribute of the GTPConfig object
     *
     *@param  komi  The new komi value
     */
    public void setKomi(float komi) {
        if (komi == -1.0f)
            komi = 5.5f;
        this.komi = komi;
    }

    /**
     *  Gets the mainTime attribute of the GTPConfig object
     *
     *@return    The mainTime value
     */
    public int getMainTime() {
        return mainTime;
    }

    /**
     *  Sets the mainTime attribute of the GTPConfig object
     *
     *@param  t  The new mainTime value
     */
    public void setMainTime(int t) {
        mainTime = t;
    }

    /**
     *  Gets the byoYomiTime attribute of the GTPConfig object
     *
     *@return    The byoYomiTime value
     */
    public int getByoYomiTime() {
        return byoYomiTime;
    }

    /**
     *  Sets the byoYomiTime attribute of the GTPConfig object
     *
     *@param  t  The new byoYomiTime value
     */
    public void setByoYomiTime(int t) {
        byoYomiTime = t;
    }

    /**
     *  Gets the byoYomiStones attribute of the GTPConfig object
     *
     *@return    The byoYomiStones value
     */
    public int getByoYomiStones() {
        return byoYomiStones;
    }

    /**
     *  Sets the byoYomiStones attribute of the GTPConfig object
     *
     *@param  s  The new byoYomiStones value
     */
    public void setByoYomiStones(int s) {
        byoYomiStones = s;
    }
    //}}}

    //{{{ getResumeFileName() method
    /**
     *  Gets the resumeFileName attribute of the GTPConfig object
     *
     *@return    The resumeFileName value
     */
    public String getResumeFileName() {
        return resumeFileName;
    } //}}}

    //{{{ setResumeFileName() method
    /**
     *  Sets the resumeFileName attribute of the GTPConfig object
     *
     *@param  s  The new resumeFileName value
     */
    public void setResumeFileName(String s) {
        resumeFileName = s;
    } //}}}

    //{{{ toString() method
    /**
     *  Converts this Class to a string. For debugging.
     *
     *@return    Converted string
     */
    public String toString() {
        return
                "[GTPConfig" +
                "\nSize:    " + size +
                "\nHandicap: " + handicap +
                "\nKomi:     " + komi +
                "\nBlack:    " + (black == GTP_COMPUTER ? "Computer" : "Human") +
                "\nWhite:    " + (white == GTP_COMPUTER ? "Computer" : "Human") +
                "\nLevel:    " + level +
                "\nResume:   " + resumeFileName +
                "]";
    } //}}}
}

