/*
 *  SGFPopup.java
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

import javax.swing.*;
import java.awt.event.*;
import ggo.igs.IGSConnection;
import ggo.gGo;

/**
 *  Popup menu for the sgf games table in the Playerinfo dialog
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/10/23 01:49:29 $
 */
class SGFPopup extends StoredSGFPopup {
    //{{{ SGFPopup() constructor
    /**
     *Constructor for the SGFPopup object
     *
     *@param  parent  Parent table
     */
    SGFPopup(JTable parent) {
        super(parent);
    } //}}}

    //{{{ initPopupMenu() method
    /**  Init popup menu */
    protected void initPopupMenu() {
        JMenuItem menuItem = new JMenuItem(gGo.getIGSPlayerResources().getString("game_popup_mail_me"));
        menuItem.setActionCommand("MailMe");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSPlayerResources().getString("game_popup_teach"));
        menuItem.setActionCommand("Teach");
        menuItem.addActionListener(this);
        add(menuItem);
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  ActionListener method
     *
     *@param  e  ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        if (popupRow == -1)
            return;

        String command = e.getActionCommand();

        if (command.equals("MailMe"))
            IGSMainWindow.getIGSConnection().recieveInput("mail me " + (String)(parent.getModel().getValueAt(popupRow, 0)));
        else if (command.equals("Teach"))
            IGSMainWindow.getIGSConnection().recieveInput("teach " + (String)(parent.getModel().getValueAt(popupRow, 0)));
    } //}}}
}

