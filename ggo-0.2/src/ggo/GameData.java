/*
 *  GameData.java
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

import ggo.Defines;
import ggo.gGo;
import ggo.parser.ParserDefs;
import java.io.Serializable;

/**
 *  Storage class for the data of one game. Mostly SGF fields except the
 *  filename.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.5 $, $Date: 2002/10/05 16:06:00 $
 */
public class GameData implements Defines, Serializable {
    //{{{ public members
    /**  Name of black player */
    public String playerBlack;
    /**  Name of white player */
    public String playerWhite;
    /**  Rank of black player */
    public String rankBlack;
    /**  Rank of white player */
    public String rankWhite;
    /**  Game result */
    public String result;
    /**  Date of the game */
    public String date;
    /**  Location of the game */
    public String place;
    /**  Copyright information */
    public String copyright;
    /**  Name of the game */
    public String gameName;
    /**  Filename of this game */
    public String fileName;
    /**  Board size */
    public int size;
    /**  Handicap given */
    public int handicap;
    /**  Komi given */
    public float komi;
    /**  White territory */
    public int scoreTerrWhite;
    /**  White captures */
    public int scoreCapsWhite;
    /**  White total score */
    public int scoreTerrBlack;
    /**  Black captures */
    public int scoreCapsBlack;
    /** Charset */
    public String charset;
    //}}}

    //{{{ GameData() constructor
    /**Constructor for the GameData object */
    public GameData() {
        // Init with default values
        size = 19;
        playerBlack = gGo.getBoardResources().getString("Black");
        playerWhite = gGo.getBoardResources().getString("White");
        rankBlack = "";
        rankWhite = "";
        komi = 5.5f;
        handicap = 0;
        fileName = "";
        gameName = "";
        date = "";
        place = "";
        copyright = "";
        scoreTerrWhite = scoreTerrBlack = scoreCapsWhite = scoreCapsBlack = 0;
        charset = ParserDefs.defaultgGoCharset;
    } //}}}

    //{{{ GameData(int) constructor
    /**
     *Constructor for the GameData object
     *
     *@param  boardSize  Board size
     */
    public GameData(int boardSize) {
        this();
        size = boardSize;
    } //}}}

    //{{{ toString() method
    /**
     *  Converts the GameData to a string. For debugging.
     *
     *@return    Converted string
     */
    public String toString() {
        return "[GAME DATA" +
                "\n  Black = " + playerBlack + " / " + rankBlack +
                "\n  White = " + playerWhite + " / " + rankWhite +
                "\n  Size = " + size +
                "\n  Komi = " + komi +
                "\n  Handicap = " + handicap +
                "\n  Result = " + result +
                "\n  Date = " + date + ", Place = " + place +
                "\n  Copyright = " + copyright +
                "\n  Gamename = " + gameName +
                "\n  Score white = " + scoreTerrWhite + " + " + scoreCapsWhite +
                "\n  Score black = " + scoreTerrBlack + " + " + scoreCapsBlack +
                "\n  Charset  = " + charset +
                "\n  Filename = " + fileName + "]\n";
    } //}}}
}

