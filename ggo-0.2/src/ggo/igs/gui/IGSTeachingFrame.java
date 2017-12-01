/*
 *  IGSTeachingFrame.java
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

/**
 *  Subclass of IGSPlayingFrame for teaching games.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/23 01:49:15 $
 */
public class IGSTeachingFrame extends IGSPlayingFrame {
    //{{{ private members
    private JLabel markLabel;
    private boolean markLabelUpdated;
    //}}}

    //{{{ IGSTeachingFrame() constructor
    /**
     *Constructor for the IGSTeachingFrame object
     *
     *@param  gameID  ID of this game
     */
    public IGSTeachingFrame(int gameID) {
        super(gameID, -1);
    } //}}}

    //{{{ mayMove() method
    public boolean mayMove(int color) {
        return !isFinished;
    } //}}}

    //{{{ initMenus() method
    protected void initMenus() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File menu
        menuBar.add(initFileMenu(FILE_SAVE | FILE_SAVE_AS | FILE_CLOSE));

        // Edit menu
        menuBar.add(initEditMenu(EDIT_IGS | EDIT_GAME | EDIT_GUESS_SCORE));

        // Settings menu
        menuBar.add(initSettingsMenu(SETTINGS_PREFERENCES | SETTINGS_GAME_INFO | SETTINGS_MEMORY_STATUS | SETTINGS_MEMORY_CLEANUP));

        // View menu
        menuBar.add(initViewMenu(VIEW_CLEAR | VIEW_TOOLBAR | VIEW_STATUSBAR | VIEW_COORDS | VIEW_SIDEBAR | VIEW_SLIDER | VIEW_HORIZONTAL_COMMENT | VIEW_SAVE_SIZE));

        // Help menu
        menuBar.add(initHelpMenu());
    } //}}}

    //{{{ initToolBar() method
    protected void initToolBar() {
        toolBar = new JToolBar();

        try {
            JButton leftButton = new JButton(new ImageIcon(getClass().getResource("/images/1leftarrow.gif")));
            leftButton.setBorderPainted(false);
            leftButton.setToolTipText(board_resources.getString("Backward"));
            leftButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        IGSConnection.sendCommand("<");
                    }
                });
            toolBar.add(leftButton);

            JButton rightButton = new JButton(new ImageIcon(getClass().getResource("/images/1rightarrow.gif")));
            rightButton.setBorderPainted(false);
            rightButton.setToolTipText(board_resources.getString("Forward"));
            rightButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        IGSConnection.sendCommand(">");
                    }
                });
            toolBar.add(rightButton);

            toolBar.addSeparator();

            JButton markButton = new JButton(new ImageIcon(getClass().getResource("/images/ZoomIn16.gif")));
            markButton.setBorderPainted(false);
            markButton.setToolTipText(board_resources.getString("mark_in_teach_tooltip"));
            markButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        IGSConnection.sendCommand("mark");
                    }
                });
            toolBar.add(markButton);

            toolBar.addSeparator();

            markLabel = new JLabel();
            toolBar.add(markLabel);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icons for toolbar: " + e);
        }
    } //}}}

    //{{{ updateMarkLabel() method
    /**
     *  Update the toolbar label with the "You have marks at: 1,2,3." text
     *
     *@param  txt  Text sent from IGS
     */
    public void updateMarkLabel(String txt) {
        markLabel.setText(txt);
        markLabelUpdated = true;
    } //}}}

    //{{{ doMove() method
    public void doMove(int color, int x, int y) {
        super.doMove(color, x, y);
        if (!markLabelUpdated)
            markLabel.setText("");
        markLabelUpdated = false;
    } //}}}
}

