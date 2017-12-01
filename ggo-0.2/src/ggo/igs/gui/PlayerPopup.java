/*
 *  PlayerPopup.java
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

import ggo.gGo;
import ggo.igs.*;
import ggo.igs.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *  A popup menu called on a player in various components. It offers menuitems like
 *  "stats", "tell", "match" etc.
 *  The parent must implement the interface PlayerPopupParent.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.13 $, $Date: 2002/09/21 12:39:56 $
 */
public class PlayerPopup extends JPopupMenu implements ActionListener, MouseListener {
    //{{{ private members
    private int popupRow, popupCol;
    private PlayerPopupParent parent;
    private JRadioButtonMenuItem bozoMenuItemNeutral, bozoMenuItemFriend, bozoMenuItemBozo;
    private JCheckBoxMenuItem trailMenuItem;
    //}}}

    //{{{ PlayerPopup() constructor
    /**
     * Constructor for the PlayerPopup object
     *
     *@param  parent  The parent calling this PopupMenu. The parent must implement
     *                the interface PlayerPopupParent.
     */
    public PlayerPopup(PlayerPopupParent parent) {
        this.parent = parent;
        popupRow = popupCol = -1;
        initPopupMenu();
    } //}}}

    //{{{ initPopupMenu() method
    /**  Init popup menu */
    private void initPopupMenu() {
        JMenuItem menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_stats"));
        menuItem.setActionCommand("Stats");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_tell"));
        menuItem.setActionCommand("Tell");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_match"));
        menuItem.setActionCommand("Match");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_automatch"));
        menuItem.setActionCommand("Automatch");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_results"));
        menuItem.setActionCommand("Results");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_SGF"));
        menuItem.setActionCommand("SGF");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_stored"));
        menuItem.setActionCommand("Stored");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_probability"));
        menuItem.setActionCommand("Probability");
        menuItem.addActionListener(this);
        add(menuItem);

        menuItem = new JMenuItem(gGo.getIGSResources().getString("player_popup_observe"));
        menuItem.setActionCommand("Observe");
        menuItem.addActionListener(this);
        add(menuItem);

        trailMenuItem = new JCheckBoxMenuItem(gGo.getIGSResources().getString("player_popup_trail"));
        trailMenuItem.setActionCommand("Trail");
        trailMenuItem.addActionListener(this);
        add(trailMenuItem);

        JMenu statusMenu = new JMenu(gGo.getIGSBozoResources().getString("Status"));
        ButtonGroup statusGroup = new ButtonGroup();

        bozoMenuItemFriend = new JRadioButtonMenuItem(gGo.getIGSBozoResources().getString("Friend"));
        bozoMenuItemFriend.setActionCommand("Friend");
        bozoMenuItemFriend.addActionListener(this);
        statusGroup.add(bozoMenuItemFriend);
        statusMenu.add(bozoMenuItemFriend);

        bozoMenuItemNeutral = new JRadioButtonMenuItem(gGo.getIGSBozoResources().getString("Neutral"));
        bozoMenuItemNeutral.setActionCommand("Neutral");
        bozoMenuItemNeutral.addActionListener(this);
        statusGroup.add(bozoMenuItemNeutral);
        statusMenu.add(bozoMenuItemNeutral);

        bozoMenuItemBozo = new JRadioButtonMenuItem(gGo.getIGSBozoResources().getString("Ignore"));
        bozoMenuItemBozo.setActionCommand("Bozo");
        bozoMenuItemBozo.addActionListener(this);
        statusGroup.add(bozoMenuItemBozo);
        statusMenu.add(bozoMenuItemBozo);

        add(statusMenu);
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  Action of the popup menu was performed.
     *
     *@param  e  ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        String send = null;
        String suffix = "";

        if (command.equals("Stats")) {
            send = "stats ";
        }
        else if (command.equals("Tell")) {
            IGSConnection.sendChat(parent.getPlayerName(popupRow, popupCol));
            return;
        }
        else if (command.equals("Match")) {
            IGSConnection.sendMatch(parent.getPlayerName(popupRow, popupCol));
        }
        else if (command.equals("Automatch")) {
            IGSConnection.sendAutoMatch(parent.getPlayerName(popupRow, popupCol));
        }
        else if (command.equals("Results")) {
            send = "results -";
        }
        else if (command.equals("SGF")) {
            send = "sgf ";
            suffix = "-";
        }
        else if (command.equals("Stored")) {
            send = "stored -";
        }
        else if (command.equals("Probability")) {
            send = "prob ";
        }
        else if (command.equals("Observe")) {
            int gameID = parent.getGameOfPlayer(popupRow, popupCol);
            if (gameID == 0)
                return;
            IGSConnection.startObserve(gameID);
            IGSConnection.getGamesTable().observeGame(gameID);
            return;
        }
        else if (command.equals("Trail")) {
            send = "trail ";
        }
        else if (command.equals("Neutral")) {
            String name = parent.getPlayerName(popupRow, popupCol);
            IGSConnection.getMainWindow().getBozoHandler().setBozoStatus(
                    name,
                    BozoHandler.PLAYER_STATUS_NEUTRAL);
            updateBozoListDialog(name, BozoHandler.PLAYER_STATUS_NEUTRAL);
        }
        else if (command.equals("Friend")) {
            String name = parent.getPlayerName(popupRow, popupCol);
            IGSConnection.getMainWindow().getBozoHandler().setBozoStatus(
                    name,
                    BozoHandler.PLAYER_STATUS_FRIEND);
            updateBozoListDialog(name, BozoHandler.PLAYER_STATUS_FRIEND);
        }
        else if (command.equals("Bozo")) {
            String name = parent.getPlayerName(popupRow, popupCol);
            IGSConnection.getMainWindow().getBozoHandler().setBozoStatus(
                    name,
                    BozoHandler.PLAYER_STATUS_BOZO);
            updateBozoListDialog(name, BozoHandler.PLAYER_STATUS_BOZO);
        }

        // if (send != null && popupRow != -1) {  // This is necassary?
        if (send != null) {
            String name = parent.getPlayerName(popupRow, popupCol);
            if (name != null)
                IGSConnection.sendCommand(send + name + suffix);
            popupRow = popupCol = -1;
        }
    } //}}}

    //{{{ updateBozoListDialog() method
    /**
     *  Update the bozo and player dialog when the bozo status was changed.
     *
     *@param  name    Player name
     *@param  status  New status
     */
    private void updateBozoListDialog(String name, int status) {
        IGSConnection.getMainWindow().updateBozoListDialog();

        try {
            PlayerInfoDialog.getDialog(name).toggleBozoStatus(status);
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ MouseListener methods
    /**
     *  Mousebutton was clicked. Empty method.
     *
     *@param  e  MouseEvent
     */
    public void mouseClicked(MouseEvent e) { }

    /**
     *  Mouse entered. Empty method.
     *
     *@param  e  MouseEvent
     */
    public void mouseEntered(MouseEvent e) { }

    /**
     *  Mouse exited. Empty method.
     *
     *@param  e  MouseEvent
     */
    public void mouseExited(MouseEvent e) { }

    /**
     *  Mousebutton was pressed. Call popup menu.
     *
     *@param  e  MouseEvent
     */
    public void mousePressed(MouseEvent e) {
        popupMenu(e);
    }

    /**
     *  Mousebutton was released. Call popup menu.
     *
     *@param  e  MouseEvent
     */
    public void mouseReleased(MouseEvent e) {
        popupMenu(e);
    } //}}}

    //{{{ popupMenu() method
    /**
     *  Show the popup menu
     *
     *@param  e  MouseEvent
     */
    private void popupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            try {
                if (e.getComponent().getClass().getName().equals("javax.swing.JTable") ||
                        e.getComponent().getClass().getSuperclass().getName().equals("javax.swing.JTable")) {
                    popupRow = ((JTable)e.getComponent()).rowAtPoint(e.getPoint());
                    popupCol = ((JTable)e.getComponent()).columnAtPoint(e.getPoint());
                }
                else
                    popupRow = popupCol = -1;

                if (parent.mayPopup(popupCol)) {
                    String name = parent.getPlayerName(popupRow, popupCol);

                    // Select player status radiobutton
                    switch (IGSConnection.getMainWindow().getBozoHandler().getBozoStatus(name)) {
                        case BozoHandler.PLAYER_STATUS_NEUTRAL:
                            bozoMenuItemNeutral.setSelected(true);
                            break;
                        case BozoHandler.PLAYER_STATUS_FRIEND:
                            bozoMenuItemFriend.setSelected(true);
                            break;
                        case BozoHandler.PLAYER_STATUS_BOZO:
                            bozoMenuItemBozo.setSelected(true);
                            break;
                    }

                    // Set trail checkbox
                    trailMenuItem.setSelected(IGSMainWindow.getIGSConnection().getIGSReader().doesTrail(name));

                    // Show popup menu
                    show(e.getComponent(), e.getX(), e.getY());
                }
            } catch (NullPointerException ex) {
                System.err.println("Failed to open popup menu: " + ex);
            }
        }
    } //}}}
}

