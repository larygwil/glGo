/*
 *  StoredPopup.java
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
import ggo.*;

/**
 *  Popup menu for the stored games table in the Playerinfo dialog
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/10/23 01:49:40 $
 */
class StoredPopup extends StoredSGFPopup implements Defines {
    //{{{ StoredPopup() constructor
    /**
     *Constructor for the StoredPopup object
     *
     *@param  parent  Parent table
     */
    StoredPopup(JTable parent) {
        super(parent);
    } //}}}

    //{{{ initPopupMenu() method
    /**  Init popup menu */
    protected void initPopupMenu() {
        JMenuItem menuItem = new JMenuItem(gGo.getIGSPlayerResources().getString("game_popup_look"));
        menuItem.setActionCommand("Look");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSPlayerResources().getString("game_popup_load"));
        menuItem.setActionCommand("Load");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getBoardResources().getString("Resign"));
        menuItem.setActionCommand("Resign");
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

        if (command.equals("Look"))
            IGSMainWindow.getIGSConnection().recieveInput("look " + (String)(parent.getModel().getValueAt(popupRow, 0)));
        else if (command.equals("Load"))
            IGSMainWindow.getIGSConnection().recieveInput("load " + (String)(parent.getModel().getValueAt(popupRow, 0)));
        else if (command.equals("Resign")) {
            if (JOptionPane.showOptionDialog(
                    null,
                    gGo.getIGSResources().getString("confirm_resign"),
                    PACKAGE,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{gGo.getIGSResources().getString("Yes_resign"), gGo.getIGSResources().getString("No_do_not_resign")},
                    new String(gGo.getIGSResources().getString("No_do_not_resign"))) == JOptionPane.YES_OPTION)
                IGSMainWindow.getIGSConnection().recieveInput("resign " + (String)(parent.getModel().getValueAt(popupRow, 0)));
        }
    } //}}}
}

