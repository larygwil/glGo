/*
 *  Player.java
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

import ggo.igs.*;
import ggo.igs.gui.IGSMainWindow;

/**
 *  Class describing a player object
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:56 $
 */
public class Player {
    //{{{ private members
    private String name, status, idle;
    private int game, observe;
    private IGSRank rank;
    private int bozoStatus;
    //}}}

    //{{{ Player constructor
    /**
     *Constructor for the Player object
     *
     *@param  name     Name
     *@param  rank     Rank
     *@param  status   Status flags like !, Q, X
     *@param  game     Game played
     *@param  observe  Game observed
     *@param  idle     Idle time
     */
    public Player(String name, IGSRank rank, String status, int game, int observe, String idle) {
        this.name = name;
        this.rank = rank;
        this.status = status;
        this.game = game;
        this.observe = observe;
        this.idle = idle;
        bozoStatus = IGSConnection.getMainWindow().getBozoHandler().getBozoStatus(name);
    } //}}}

    //{{{ getName() method
    /**
     *  Gets the name attribute of the Player object
     *
     *@return    The name value
     */
    public String getName() {
        return name;
    } //}}}

    //{{{ getRank() method
    /**
     *  Gets the rank attribute of the Player object
     *
     *@return    The rank value
     */
    public IGSRank getRank() {
        return rank;
    } //}}}

    //{{{ getStatus() method
    /**
     *  Gets the status attribute of the Player object
     *
     *@return    The status value
     */
    public String getStatus() {
        return status;
    } //}}}

    //{{{ getGame() method
    /**
     *  Gets the game attribute of the Player object
     *
     *@return    The game value
     */
    public int getGame() {
        return game;
    } //}}}

    //{{{ setGame() method
    /**
     *  Sets the game attribute of the Player object
     *
     *@param  g  The new game value
     */
    public void setGame(int g) {
        game = g;
    } //}}}

    //{{{ getObserve() method
    /**
     *  Gets the observe attribute of the Player object
     *
     *@return    The observe value
     */
    public int getObserve() {
        return observe;
    } //}}}

    //{{{ getIdle() method
    /**
     *  Gets the idle attribute of the Player object
     *
     *@return    The idle value
     */
    public String getIdle() {
        return idle;
    } //}}}

    //{{{ toString() method
    /**
     *  Converts the object to a string. For debugging.
     *
     *@return    Converted String
     */
    public String toString() {
        return "[Name: " + name +
                " Rank: " + rank +
                " Status: " + status +
                " Play: " + game +
                " Observe: " + observe +
                " Idle: " + idle +
                " Bozo: " + bozoStatus +
                "]";
    } //}}}
}

