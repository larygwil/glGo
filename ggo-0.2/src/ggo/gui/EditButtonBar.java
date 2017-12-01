/*
 *  EditButtonBar.java
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
import java.awt.*;
import java.awt.event.*;
import ggo.*;
import ggo.gui.*;

/**
 *  Buttonbar on the sidebar containing the Pass, Pcore and Edit mode buttons.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:55 $
 */
public class EditButtonBar extends JPanel implements ButtonBar, ActionListener, ItemListener, Defines {
    //{{{ private members
    private JButton passButton;
    private JToggleButton scoreToggleButton, modeToggleButton;
    private SideBar sideBar;
    private boolean dontFire = false;
    //}}}

    //{{{ EditButtonBar constructor
    /**
     *  EditButtonBar constructor
     *
     *@param  sb  Sidebar containing the GUI element
     */
    public EditButtonBar(SideBar sb) {
        sideBar = sb;
        initComponents();
    } //}}}

    //{{{ initComponents() method
    /**  Init the gui components */
    private void initComponents() {
        boolean synchFlag = sideBar.board.getMainFrame().getSynchFrame() != null;
        setLayout(new GridLayout(synchFlag ? 4 : 3, 1));
        setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));

        passButton = new JButton(gGo.getBoardResources().getString("Pass"));
        passButton.setActionCommand("Pass");
        passButton.addActionListener(this);
        passButton.setToolTipText(gGo.getBoardResources().getString("pass_button_tooltip"));
        add(passButton);

        scoreToggleButton = new JToggleButton(gGo.getBoardResources().getString("Score"));
        scoreToggleButton.addItemListener(this);
        scoreToggleButton.setToolTipText(gGo.getBoardResources().getString("score_button_tooltip"));
        add(scoreToggleButton);

        if (synchFlag) {
            JButton synchButton = new JButton(gGo.getBoardResources().getString("Synch"));
            synchButton.setActionCommand("Synch");
            synchButton.addActionListener(this);
            synchButton.setToolTipText(gGo.getBoardResources().getString("synch_button_tooltip"));
            add(synchButton);
        }

        modeToggleButton = new JToggleButton(gGo.getBoardResources().getString("Edit_mode"));
        modeToggleButton.addItemListener(this);
        modeToggleButton.setToolTipText(gGo.getBoardResources().getString("edit_button_tooltip"));
        add(modeToggleButton);
    } //}}}

    //{{{ switchMode() method
    /**
     *  Switch game mode
     *
     *@param  mode  New game mode
     */
    public void switchMode(int mode) {
        // System.err.println("EDITBUTTONBAR: switchMode(): " + mode);

        if (mode == MODE_NORMAL) {
            modeToggleButton.setSelected(false);
            scoreToggleButton.setSelected(false);
        }
        else if (mode == MODE_EDIT) {
            modeToggleButton.setSelected(true);
            scoreToggleButton.setSelected(false);
        }
    } //}}}

    //{{{ itemStateChanged() method
    /**
     *  ItemListener method implementation
     *
     *@param  e  ItemEvent
     */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();

        if (source == scoreToggleButton) {
            // Enter score mode
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // Switch sidebar panels
                modeToggleButton.setEnabled(false);
                passButton.setText(gGo.getBoardResources().getString("Done"));
                sideBar.normalTools.setVisible(false);
                sideBar.editTools.setVisible(false);
                sideBar.scoreTools.setVisible(true);

                sideBar.board.countScore();
            }
            // Leave score mode
            else {
                // Restore sidebar panels
                modeToggleButton.setEnabled(true);
                passButton.setText(gGo.getBoardResources().getString("Pass"));
                sideBar.scoreTools.setVisible(false);
                if (modeToggleButton.isSelected()) {
                    sideBar.editTools.setVisible(true);
                    sideBar.board.getBoardHandler().setGameMode(MODE_EDIT);
                }
                else {
                    sideBar.normalTools.setVisible(true);
                    sideBar.board.getBoardHandler().setGameMode(MODE_NORMAL);
                }

                if (!dontFire)
                    sideBar.board.getBoardHandler().leaveScoreMode();
            }
        }
        else if (source == modeToggleButton) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                sideBar.normalTools.setVisible(false);
                sideBar.editTools.setVisible(true);
                sideBar.board.getBoardHandler().setGameMode(MODE_EDIT);
            }
            else if (e.getStateChange() == ItemEvent.DESELECTED) {
                sideBar.editTools.setVisible(false);
                sideBar.normalTools.setVisible(true);
                sideBar.board.getBoardHandler().setGameMode(MODE_NORMAL);
            }
        }
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  ActionListener method implementation
     *
     *@param  e  ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Pass") && sideBar.board.getBoardHandler().getGameMode() != MODE_SCORE)
            sideBar.board.getBoardHandler().doPass(false);
        else if (cmd.equals("Synch")) {
            try {
                sideBar.board.getMainFrame().getSynchFrame().getBoard().openEditBoard();
            } catch (NullPointerException ex) {
                System.err.println("Failed to synch board: " + ex);
            }
        }
        else {
            dontFire = true;
            scoreToggleButton.setSelected(false);
            dontFire = false;
            sideBar.board.doCountDone();
        }
    } //}}}}
}

