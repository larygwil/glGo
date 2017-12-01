/*
 *  PopupLabel.java
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
package ggo.gui;

import javax.swing.JLabel;
import ggo.igs.gui.PlayerPopupParent;
import ggo.igs.gui.PlayerPopup;

/**
 *  A subclass of JLabel that supports the PlayerPopup menu for IGS game frames.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
class PopupLabel extends JLabel implements PlayerPopupParent {
    //{{{ private members
    private String playerName;
    private PlayerPopup popup;
    private boolean maypopup;
    //}}}

    //{{{ PopupLabel() constructor
    /**
     *Constructor for the PopupLabel object
     *
     *@param  txt       Description of the Parameter
     *@param  maypopup  Description of the Parameter
     */
    PopupLabel(String txt, boolean maypopup) {
        super(txt);
        this.maypopup = maypopup;
        playerName = parsePlayerName(txt);
        // Only create the popup if we really need it
        if (maypopup) {
            popup = new PlayerPopup(this);
            addMouseListener(popup);
        }
    } //}}}

    //{{{ setText() method
    /**
     *  Overwritten from JLabel. Adjust the internal player name as well.
     *
     *@param  text  New label text
     */
    public void setText(String text) {
        super.setText(text);
        playerName = parsePlayerName(text);
    } //}}}

    //{{{ parsePlayerName() method
    /**
     *  Extra the player name from the text that is something like "Zotan 10k*" for
     *  IGS frames.
     *
     *@param  txt  Text to parse
     *@return      Extracted player name
     */
    private String parsePlayerName(String txt) {
        try {
            return txt.substring(0, txt.indexOf(" ")).trim();
        } catch (StringIndexOutOfBoundsException e) {
            // This should occur regulary for non-IGS boards. So silent exception handling.
            return "";
        }
    } //}}}

    //{{{ getPlayerName() method
    /**
     *  Gets the name of the player the popup menu was called for
     *
     *@param  row  Table row. Ignored in this class.
     *@param  col  Table column. Ignored in this class.
     *@return      Name of player
     */
    public String getPlayerName(int row, int col) {
        return playerName;
    } //}}}

    //{{{ getGameOfPlayer() method
    /**
     *  Gets the current game of the player the popup menu was called for
     *
     *@param  row  Table row. Ignored in this class.
     *@param  col  Table column. Ignored in this class.
     *@return      Game ID of the game this player is currently playing. Ignored in this class, always 0.
     */
    public int getGameOfPlayer(int row, int col) {
        return 0;
    } //}}}

    //{{{ mayPopup() method
    /**
     *  Check if the player popup menu may appear on this table column.
     *  Returns true for an IGS window, false for all other windows
     *
     *@param  col  Table column. Ignored in this class.
     *@return      True if the popup is permitted, else false
     */
    public boolean mayPopup(int col) {
        return maypopup;
    } //}}}
}

