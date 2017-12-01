/*
 *  IGSMove.java
 */
package ggo.igs;

import java.util.ArrayList;
import ggo.igs.IGSTime;

/**
 *  Class describing a single move of an IGS game
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1.1.1 $, $Date: 2002/07/29 03:24:24 $
 */
public class IGSMove {
    /**  Move number */
    public int moveNum;
    /**  Color */
    public int color;
    /**  X and Y position */
    public int x, y;
    /**  Game ID */
    public int gameID;
    /**  Array list with capture positions */
    public ArrayList captures = null;
    /**  White time */
    public IGSTime whiteTime;
    /**  Black time */
    public IGSTime blackTime;

    /**
     *Constructor for the IGSMove object
     *
     *@param  gameID     Game ID this move belongs to
     *@param  whiteTime  White time data
     *@param  blackTime  Black time data
     */
    public IGSMove(int gameID, IGSTime whiteTime, IGSTime blackTime) {
        this.gameID = gameID;
        this.whiteTime = whiteTime;
        this.blackTime = blackTime;
    }

    /**
     *  Convert the move to a String, for debugging
     *
     *@return    Converted String
     */
    public String toString() {
        return "[GameID = " + gameID + " Num = " + moveNum + " Color = " + color +
                " x = " + x + " y = " + y +
                "\n White time = " + whiteTime +
                "\n Black time = " + blackTime +
                "\n Caps = " + captures + "]";
    }
}

