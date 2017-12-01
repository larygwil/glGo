/*
 *  PlayButtonBar.java
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
import ggo.gtp.GTPMainFrame;
import ggo.igs.*;
import ggo.igs.gui.*;

/**
 *  Buttonbar on the sidebar containing the Pass, Score and Undo buttons.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.8 $, $Date: 2002/10/24 08:32:02 $
 */
public class PlayButtonBar extends JPanel implements ButtonBar, ActionListener, ItemListener, Defines {
    //{{{ private members
    private JButton passButton, undoButton, resignButton, allButton, editButton, adjournButton;
    private JToggleButton scoreToggleButton;
    private SideBar sideBar;
    private boolean dontFire = false;
    //}}}

    //{{{ PlayButtonBar constructor
    /**
     *  PlayButtonBar constructor
     *
     *@param  sb  Description of the Parameter
     */
    public PlayButtonBar(SideBar sb) {
        sideBar = sb;
        initComponents();
    } //}}}

    //{{{ initComponents() method
    /**  Init the gui components */
    private void initComponents() {
        // This is somewhat ugly...
        boolean isIGSFrame = sideBar.board.getMainFrame().getClass().getName().equals("ggo.igs.gui.IGSPlayingFrame") ||
            sideBar.board.getMainFrame().getClass().getName().equals("ggo.igs.gui.IGSTeachingFrame");
        boolean isTeachFrame = isIGSFrame && sideBar.board.getMainFrame().getClass().getName().equals("ggo.igs.gui.IGSTeachingFrame");
        setLayout(new GridLayout(isIGSFrame ? 6 : 5, 1));
        setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));

        passButton = new JButton(gGo.getBoardResources().getString("Pass"));
        passButton.setActionCommand("Pass");
        passButton.addActionListener(this);
        passButton.setToolTipText(gGo.getBoardResources().getString("pass_button_tooltip"));
        add(passButton);

        if (isIGSFrame) {
            adjournButton = new JButton(gGo.getBoardResources().getString("Adjourn"));
            adjournButton.setActionCommand("Adjourn");
            adjournButton.addActionListener(this);
            adjournButton.setToolTipText(gGo.getBoardResources().getString("Adjourn_game"));
            add(adjournButton);
        }
        else {
            scoreToggleButton = new JToggleButton(gGo.getBoardResources().getString("Score"));
            scoreToggleButton.addItemListener(this);
            scoreToggleButton.setToolTipText(gGo.getBoardResources().getString("score_button_tooltip"));
            add(scoreToggleButton);
        }

        resignButton = new JButton(gGo.getBoardResources().getString("Resign"));
        resignButton.setActionCommand("Resign");
        resignButton.addActionListener(this);
        resignButton.setToolTipText(gGo.getBoardResources().getString("Resign_the_game"));
        add(resignButton);

        if (isIGSFrame) {
            allButton = new JButton(gGo.getIGSResources().getString("Observers"));
            allButton.setActionCommand("observers");
            allButton.addActionListener(this);
            allButton.setToolTipText(gGo.getIGSResources().getString("observers_tooltip"));
            if (isTeachFrame)
                add(allButton);
        }

        if (!isTeachFrame) {
            undoButton = new JButton(gGo.getBoardResources().getString("Undo"));
            undoButton.setActionCommand("Undo");
            undoButton.addActionListener(this);
            undoButton.setToolTipText(gGo.getBoardResources().getString("Undo_one_move"));
            add(undoButton);
        }
        else {
            JPanel teachNavPanel = new JPanel();
            teachNavPanel.setLayout(new GridLayout(1, 2));

            undoButton = new JButton("<");
            undoButton.setActionCommand("Backward");
            undoButton.addActionListener(this);
            undoButton.setToolTipText(gGo.getBoardResources().getString("Backward"));
            teachNavPanel.add(undoButton);

            JButton forwardButton = new JButton(">");
            forwardButton.setActionCommand("Forward");
            forwardButton.addActionListener(this);
            forwardButton.setToolTipText(gGo.getBoardResources().getString("Forward"));
            teachNavPanel.add(forwardButton);

            add(teachNavPanel);
        }

        if (!isTeachFrame) {
            editButton = new JButton(gGo.getBoardResources().getString("Edit_game"));
            editButton.setActionCommand("edit");
            editButton.setToolTipText(gGo.getBoardResources().getString("edit_game_tooltip"));
        }
        else {
            editButton = new JButton(gGo.getBoardResources().getString("mark_in_teach"));
            editButton.setActionCommand("Mark");
            editButton.setToolTipText(gGo.getBoardResources().getString("mark_in_teach_tooltip"));
            editButton.setToolTipText("Set mark");
        }
        editButton.addActionListener(this);
        add(editButton);

        if (isIGSFrame && !isTeachFrame)
            add(allButton);
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
                passButton.setText(gGo.getBoardResources().getString("Done"));
                passButton.setToolTipText(gGo.getBoardResources().getString("Finish_scoring"));
                passButton.setEnabled(true);
                sideBar.normalTools.setVisible(false);
                sideBar.scoreTools.setVisible(true);

                sideBar.board.countScore();
            }
            // Leave score mode
            else {
                // Restore sidebar panels
                passButton.setText(gGo.getBoardResources().getString("Pass"));
                passButton.setToolTipText(gGo.getBoardResources().getString("pass_button_tooltip"));
                passButton.setEnabled(false);
                sideBar.scoreTools.setVisible(false);
                sideBar.normalTools.setVisible(true);
                sideBar.board.getBoardHandler().setGameMode(MODE_NORMAL);

                if (!dontFire)
                    sideBar.board.getBoardHandler().leaveScoreMode();
            }
        }
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  ActionListener method
     *
     *@param  e  ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Resign")) {
            if (!(sideBar.board.getMainFrame().getClass().getName().equals("ggo.gtp.GTPMainFrame") &&
                    ((GTPMainFrame)(sideBar.board.getMainFrame())).doGTPScore()))
                sideBar.board.doResign();
        }
        else if (cmd.equals("Adjourn"))
            ((IGSPlayingFrame)(sideBar.board.getMainFrame())).adjournGame();
        else if (cmd.equals("Reload"))
            ((IGSPlayingFrame)(sideBar.board.getMainFrame())).reloadGame();
        else if (cmd.equals("Undo"))
            sideBar.board.undoMove();
        else if (cmd.equals("Pass") && sideBar.board.getBoardHandler().getGameMode() != MODE_SCORE)
            sideBar.board.doPass();
        else if (cmd.equals("igsdone"))
            sideBar.board.doIGSDone();
        else if (cmd.equals("edit"))
            sideBar.board.openEditBoard();
        else if (cmd.equals("observers")) {
            int id = ((IGSPlayingFrame)(sideBar.board.getMainFrame())).getGameID();
            IGSReader.requestObserversOwnGame = id;
            IGSConnection.sendCommand("all " + id);
        }
        else if (cmd.equals("Backward"))
            IGSConnection.sendCommand("<");
        else if (cmd.equals("Forward"))
            IGSConnection.sendCommand(">");
        else if (cmd.equals("Mark"))
            IGSConnection.sendCommand("mark");
        else {
            dontFire = true;
            if (scoreToggleButton != null)
                scoreToggleButton.setSelected(false);
            dontFire = false;
            sideBar.board.doCountDone();
        }
    } //}}}}

    //{{{ switchMode() method
    /**
     *  Switch game mode
     *
     *@param  mode  New game mode
     */
    public void switchMode(int mode) {
        if (mode == MODE_SCORE) {
            passButton.setEnabled(false);
            if (scoreToggleButton != null)
                scoreToggleButton.setEnabled(true);
            if (sideBar.board.getMainFrame().getClass().getName().equals("ggo.gtp.GTPMainFrame")) {
                resignButton.setText(gGo.getBoardResources().getString("Guess"));
                resignButton.setToolTipText(gGo.getBoardResources().getString("guess_score_tooltip"));
                undoButton.setEnabled(false);
            }
            else if (sideBar.board.getMainFrame().getClass().getName().equals("ggo.igs.gui.IGSPlayingFrame") ||
                     sideBar.board.getMainFrame().getClass().getName().equals("ggo.igs.gui.IGSTeachingFrame")) {
                resignButton.setText(gGo.getBoardResources().getString("Done"));
                resignButton.setToolTipText(gGo.getBoardResources().getString("finish_score_tooltip"));
                resignButton.setActionCommand("igsdone");
                sideBar.normalTools.setVisible(false);
                sideBar.scoreTools.setVisible(true);
            }
        }
        else {
            if (scoreToggleButton != null) {
                scoreToggleButton.setSelected(false);
                scoreToggleButton.setEnabled(false);
            }
            passButton.setEnabled(true);
            resignButton.setText(gGo.getBoardResources().getString("Resign"));
            resignButton.setToolTipText(gGo.getBoardResources().getString("Resign_the_game"));
            resignButton.setActionCommand("Resign");
            undoButton.setEnabled(true);
        }
    } //}}}

    //{{{ toggleAdjournReload() method
    /**
     *  Toggle adjourn button to reload, or vice versa
     *
     *@param  toggle  True:  adjourn -> reload
     *                False: reload -> adjourn
     *@see            ggo.igs.gui.IGSPlayingFrame#toggleAdjournReload(boolean)
     */
    public void toggleAdjournReload(boolean toggle) {
        if (toggle) {
            adjournButton.setText(gGo.getIGSResources().getString("Reload"));
            adjournButton.setActionCommand("Reload");
            adjournButton.setToolTipText(gGo.getIGSResources().getString("Reload_game"));
        }
        else {
            adjournButton.setText(gGo.getBoardResources().getString("Adjourn"));
            adjournButton.setActionCommand("Adjourn");
            adjournButton.setToolTipText(gGo.getBoardResources().getString("Adjourn_game"));
        }
    } //}}}
}

