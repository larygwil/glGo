/*
 *  Game.java
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
 *  Class describing an IGS game
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class Game {
    //{{{ private members
    private String whiteName, blackName, type;
    private IGSRank whiteRank, blackRank;
    private int gameID, observers, move, size, handicap, byoYomi;
    private float komi;
    private boolean isObserved;
    //}}}

    //{{{ Game constructor
    /**
     *Constructor for the Game object
     *
     *@param  gameID      Game ID
     *@param  whiteName   White player name
     *@param  whiteRank   White player rank
     *@param  blackName   Black player name
     *@param  blackRank   Black player rank
     *@param  observers   Number of observers
     *@param  isObserved  True, if user is currently observing this game
     *@param  move        Move number
     *@param  size        Board size
     *@param  handicap    Handicap
     *@param  komi        Komi
     *@param  byoYomi     ByoYomi time
     *@param  type        Description of the Parameter
     */
    public Game(int gameID, String whiteName, IGSRank whiteRank, String blackName, IGSRank blackRank,
            int move, int size, int handicap, float komi, int byoYomi, String type, int observers, boolean isObserved) {
        this.gameID = gameID;
        this.whiteName = whiteName;
        this.whiteRank = whiteRank;
        this.blackName = blackName;
        this.blackRank = blackRank;
        this.move = move;
        this.size = size;
        this.handicap = handicap;
        this.komi = komi;
        this.byoYomi = byoYomi;
        this.type = type;
        this.observers = observers;
        this.isObserved = isObserved;
    } //}}}

    //{{{ getGameID() method
    /**
     *  Gets the gameID attribute of the Game object
     *
     *@return    The gameID value
     */
    public int getGameID() {
        return gameID;
    } //}}}

    //{{{ getWhiteName() method
    /**
     *  Gets the whiteName attribute of the Game object
     *
     *@return    The whiteName value
     */
    public String getWhiteName() {
        return whiteName;
    } //}}}

    //{{{ getWhiteRank() method
    /**
     *  Gets the whiteRank attribute of the Game object
     *
     *@return    The whiteRank value
     */
    public IGSRank getWhiteRank() {
        return whiteRank;
    } //}}}

    //{{{ getBlackName() method
    /**
     *  Gets the blackName attribute of the Game object
     *
     *@return    The blackName value
     */
    public String getBlackName() {
        return blackName;
    } //}}}

    //{{{ getBlackRank() method
    /**
     *  Gets the blackRank attribute of the Game object
     *
     *@return    The blackRank value
     */
    public IGSRank getBlackRank() {
        return blackRank;
    } //}}}

    //{{{ getMove() method
    /**
     *  Gets the move attribute of the Game object
     *
     *@return    The move value
     */
    public int getMove() {
        return move;
    } //}}}

    //{{{ getSize() method
    /**
     *  Gets the size attribute of the Game object
     *
     *@return    The size value
     */
    public int getSize() {
        return size;
    } //}}}

    //{{{ getHandicap() method
    /**
     *  Gets the handicap attribute of the Game object
     *
     *@return    The handicap value
     */
    public int getHandicap() {
        return handicap;
    } //}}}

    //{{{ getKomi() method
    /**
     *  Gets the komi attribute of the Game object
     *
     *@return    The komi value
     */
    public float getKomi() {
        return komi;
    } //}}}

    //{{{ getByoYomi() method
    /**
     *  Gets the byoYomi attribute of the Game object
     *
     *@return    The byoYomi value
     */
    public int getByoYomi() {
        return byoYomi;
    } //}}}

    //{{{ getObservers() method
    /**
     *  Gets the observers attribute of the Game object
     *
     *@return    The observers value
     */
    public int getObservers() {
        return observers;
    } //}}}

    //{{{ isObserved() method
    /**
     *  Gets the observed attribute of the Game object
     *
     *@return    The observed value
     */
    public boolean isObserved() {
        return isObserved;
    } //}}}

    //{{{ setObserved() method
    /**
     *  Sets the observed attribute of the Game object
     *
     *@param  b  The new observed value
     */
    public void setObserved(boolean b) {
        isObserved = b;
    } //}}}

    //{{{ getType() method
    /**
     *  Gets the type attribute of the Game object
     *
     *@return    The type value
     */
    public String getType() {
        return type;
    } //}}}

    //{{{ toString() method
    /**
     *  Converts the object to a string. For debugging.
     *
     *@return    Converted string
     */
    public String toString() {
        return "\n[White : " + whiteName + " " + whiteRank +
                "\n Black : " + blackName + " " + blackRank +
                "\n Move: " + move +
                "\n Size: " + size +
                "\n Handicap: " + handicap +
                "\n Komi: " + komi +
                "\n ByoYomi: " + byoYomi +
                "\n Type: " + type +
                "\n Obs   : " + observers +
                "]";
    } //}}}
}

