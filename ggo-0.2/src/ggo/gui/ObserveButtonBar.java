/*
 *  ObserveButtonBar.java
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
package ggo.gui;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import ggo.gui.*;
import ggo.*;
import ggo.igs.*;
import ggo.igs.gui.*;

/**
 *  Buttonbar on the sidebar containing the Edit Game button
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class ObserveButtonBar extends JPanel implements ButtonBar, ActionListener {
    //{{{ private members
    private JButton editButton, allButton;
    private SideBar sideBar;
    //}}}

    //{{{ ObserveButtonBar constructor
    /**
     *  ObserveButtonBar constructor
     *
     *@param  sb  Sidebar containing this GUI element
     */
    public ObserveButtonBar(SideBar sb) {
        sideBar = sb;
        initComponents();
    } //}}}

    //{{{ initComponents() method
    /**  Init the gui components */
    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(25, 0, 25, 0));
        setLayout(new GridLayout(2, 1));

        editButton = new JButton(gGo.getBoardResources().getString("Edit_game"));
        editButton.setActionCommand("edit");
        editButton.addActionListener(this);
        editButton.setToolTipText(gGo.getBoardResources().getString("edit_game_tooltip"));
        add(editButton);

        allButton = new JButton(gGo.getIGSResources().getString("Observers"));
        allButton.setActionCommand("observers");
        allButton.addActionListener(this);
        allButton.setToolTipText(gGo.getIGSResources().getString("observers_tooltip"));
        add(allButton);
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  ActionListener method implementation
     *
     *@param  e  ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("edit"))
            sideBar.board.openEditBoard();
        else if (cmd.equals("observers")) {
            int id = ((IGSObserverFrame)(sideBar.board.getMainFrame())).getGameID();
            IGSReader.requestObservers = id;
            IGSConnection.sendCommand("all " + id);
        }
    } //}}}}

    //{{{ switchMode() method
    /**
     *  Switch game mode. Does nothing here, we have only one mode.
     *
     *@param  mode  New game mode  - ignored
     */
    public void switchMode(int mode) {
        // Do nothing, no mode in this frame.
    } //}}}
}

