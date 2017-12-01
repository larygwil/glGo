/*
 *  PlayerPopupParent.java
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
package ggo.igs.gui;

/**
 *  Components that call a PlayerPopup popup menu have to implement this interface
 *  to guarantee the popup menu can access the required data from the component class.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:56 $
 */
public interface PlayerPopupParent {
    /**
     *  Gets the name of the player the popup menu was called for
     *
     *@param  row  Table row
     *@param  col  Table column
     *@return      Name of player
     */
    public String getPlayerName(int row, int col);

    /**
     *  Gets the current game of the player the popup menu was called for
     *
     *@param  row  Table row
     *@param  col  Table column
     *@return      Game ID of the game this player is currently playing
     */
    public int getGameOfPlayer(int row, int col);

    /**
     *  Check if the player popup menu may appear on this table column
     *
     *@param  col  Table column
     *@return      True if the popup is permitted, else false
     */
    public boolean mayPopup(int col);
}

