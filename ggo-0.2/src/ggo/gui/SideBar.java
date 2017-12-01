/*
 *  SideBar.java
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
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import ggo.*;
import ggo.gui.*;
import ggo.utils.*;

/**
 *  The tools panel on the right side of the board. This panel shows move and navigation data
 *  and embeds the NormalTools and EditTools panels, either at one time.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.8 $, $Date: 2002/10/23 01:51:08 $
 */
public class SideBar extends JPanel implements Defines {
    //{{{ protected and private members
    private JLabel label1, label2, label3, label4;
    private CoordsDisplay coordsDisplay;
    private MessageFormat brotherMsgFormat, sonMsgFormat;
    /**  Button panel */
    protected ButtonBar buttonPanel;
    /**  Pointer to the board */
    protected Board board;
    /**  Tools panel: Normal tools */
    protected NormalTools normalTools;
    /**  Tools panel: Editing tools */
    protected EditTools editTools;
    /**  Tools panel: Score tools */
    protected ScoreTools scoreTools;
    //}}}

    //{{{ SideBar constructor
    /**
     *  Constructor for the SideBar object
     *
     *@param  b               Description of the Parameter
     *@param  superclassName  Description of the Parameter
     */
    public SideBar(Board b, String superclassName) {
        board = b;

        initComponents(superclassName);

        // Init messageformat for sons and brothers label
        if (superclassName.equals("ggo.MainFrame")) {
            brotherMsgFormat = new MessageFormat(gGo.getBoardResources().getString("brothers_description"));
            sonMsgFormat = new MessageFormat(gGo.getBoardResources().getString("sons_description"));
            double[] limits = {0, 1, 2};
            String[] brotherStrings = {
                    gGo.getBoardResources().getString("brothers"),
                    gGo.getBoardResources().getString("brother"),
                    gGo.getBoardResources().getString("brothers")};
            String[] sonStrings = {
                    gGo.getBoardResources().getString("sons"),
                    gGo.getBoardResources().getString("son"),
                    gGo.getBoardResources().getString("sons")};
            brotherMsgFormat.setFormats(new Format[]{NumberFormat.getInstance(gGo.getLocale()), new ChoiceFormat(limits, brotherStrings)});
            sonMsgFormat.setFormats(new Format[]{NumberFormat.getInstance(gGo.getLocale()), new ChoiceFormat(limits, sonStrings)});
            setNavigation(0, 0);
        }
    } //}}}

    //{{{ initComponents() method
    /**
     *  Init the GUI elements
     *
     *@param  superclassName  Name of the superclass containing this element
     */
    protected void initComponents(String superclassName) {
        setLayout(new BorderLayout());
        Border bevelLowered = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
        Border empty = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED), empty));

        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new GridLayout(superclassName.equals("ggo.MainFrame") ? 3 : 2, 1, 5, 5));

        // Panel 1
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.setBackground(Color.lightGray);
        panel1.setBorder(bevelLowered);

        label1 = new JLabel(gGo.getBoardResources().getString("Move") + " 0");
        label1.setBorder(empty);
        panel1.add(label1, BorderLayout.CENTER);

        navPanel.add(panel1);

        // Panel 2
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout());

        JPanel panel21 = new JPanel();
        panel21.setBackground(Color.lightGray);
        panel21.setBorder(bevelLowered);
        label2 = new JLabel(gGo.getBoardResources().getString("Black_to_play"));
        label2.setBorder(empty);
        panel21.add(label2, BorderLayout.CENTER);

        JPanel panel22 = new JPanel();
        panel22.setBackground(Color.lightGray);
        panel22.setBorder(bevelLowered);
        coordsDisplay = new CoordsDisplay();
        coordsDisplay.setPreferredSize(new Dimension(32, 18));
        panel22.add(coordsDisplay);

        panel2.add(panel21, BorderLayout.CENTER);
        panel2.add(panel22, BorderLayout.EAST);
        navPanel.add(panel2);

        // Panel 3
        if (superclassName.equals("ggo.MainFrame")) {
            Border empty2 = BorderFactory.createEmptyBorder(0, 5, 0, 5);

            JPanel panel3 = new JPanel();
            panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));
            panel3.setBackground(Color.lightGray);
            panel3.setBorder(bevelLowered);

            label3 = new JLabel();
            label3.setBorder(empty2);
            panel3.add(label3);

            label4 = new JLabel();
            label4.setBorder(empty2);
            panel3.add(label4);

            navPanel.add(panel3);
        }

        displayPanel.add(navPanel);

        // Button panel
        if (superclassName.equals("ggo.MainFrame"))
            buttonPanel = new EditButtonBar(this);
        else if (superclassName.equals("ggo.igs.gui.IGSObserverFrame"))
            buttonPanel = new ObserveButtonBar(this);
        else if (superclassName.equals("ggo.gtp.GTPMainFrame") ||
                superclassName.equals("ggo.igs.gui.IGSPlayingFrame") ||
                superclassName.equals("ggo.igs.gui.IGSTeachingFrame"))
            buttonPanel = new PlayButtonBar(this);
        else {
            // Should not happen, but safe is safe
            System.err.println("Unknown sidebar superclass, falling back to default");
            buttonPanel = new EditButtonBar(this);
        }
        displayPanel.add((JPanel)buttonPanel);

        add(displayPanel, BorderLayout.NORTH);

        // Tools panel
        Dimension dim = new Dimension(130, 240);
        JPanel toolsPanel = new JPanel();
        toolsPanel.setMinimumSize(new Dimension(120, 100));

        normalTools = new NormalTools(superclassName);
        normalTools.setPreferredSize(dim);
        toolsPanel.add(normalTools);

        if (superclassName.equals("ggo.MainFrame")) {
            editTools = new EditTools();
            editTools.setPreferredSize(dim);
            editTools.setVisible(false);
            toolsPanel.add(editTools);
        }

        if (superclassName.equals("ggo.MainFrame") ||
                superclassName.equals("ggo.gtp.GTPMainFrame") ||
                superclassName.equals("ggo.igs.gui.IGSPlayingFrame") ||
                superclassName.equals("ggo.igs.gui.IGSTeachingFrame")) {
            scoreTools = new ScoreTools();
            scoreTools.setPreferredSize(dim);
            scoreTools.setVisible(false);
            toolsPanel.add(scoreTools);
        }

        add(toolsPanel, BorderLayout.CENTER);
    } //}}}

    //{{{ getButtonBar() method
    /**
     *  Gets the button panel
     *
     *@return    The buttonBar value
     */
    public ButtonBar getButtonBar() {
        return buttonPanel;
    } //}}}

    //{{{ switchMode() method
    /**
     *  Switch game mode
     *
     *@param  mode  New game mode
     */
    public void switchMode(int mode) {
        buttonPanel.switchMode(mode);
    } //}}}

    //{{{ setMove() method
    /**
     *  Sets the move attribute of the SideBar object
     *
     *@param  number     The new move value
     *@param  color      The new move value
     *@param  x          The new move value
     *@param  y          The new move value
     *@param  boardSize  The new move value
     */
    public void setMove(int number, int color, int x, int y, int boardSize) {
        label1.setText(gGo.getBoardResources().getString("Move") + " " + Utils.moveToString(number, color, x, y, boardSize));
    } //}}}

    //{{{ setNavigation() method
    /**
     *  Sets the navigation attribute of the SideBar object
     *
     *@param  sons      The new navigation value
     *@param  brothers  The new navigation value
     */
    public void setNavigation(int sons, int brothers) {
        try {
            label3.setText(brotherMsgFormat.format(new Object[]{new Integer(brothers), new Integer(brothers)}));
            label4.setText(sonMsgFormat.format(new Object[]{new Integer(sons), new Integer(sons)}));
        } catch (NullPointerException e) {
            System.err.println("Failed to set navigation values: " + e);
        }
    } //}}}

    //{{{ setCoords() method
    /**
     *  Sets the coords where the mouse currently hovers over
     *
     *@param  x          X coordinate
     *@param  y          Y coordinate
     *@param  boardSize  board size
     */
    public void setCoords(int x, int y, int boardSize) {
        coordsDisplay.setCoords(x, y, boardSize);
    } //}}}

    //{{{ setTurn() method
    /**
     *  Sets the turn label in normal and edit mode, as in: Black to play
     *
     *@param  blackToPlay  The new turn value
     */
    public void setTurn(boolean blackToPlay) {
        label2.setText(blackToPlay ? gGo.getBoardResources().getString("Black_to_play") : gGo.getBoardResources().getString("White_to_play"));
    } //}}}

    //{{{ setScoreTurn() method
    /**  Sets the turn label in score mode */
    public void setScoreTurn() {
        label2.setText(gGo.getBoardResources().getString("score_mode"));
    } //}}}

    //{{{ setCaptures() method
    /**
     *  Sets the captures attribute of the SideBar object
     *
     *@param  w  The new captures value
     *@param  b  The new captures value
     */
    public void setCaptures(int w, int b) {
        normalTools.setCaptures(w, b);
    } //}}}

    //{{{ setScore() method
    /**
     *  Sets the score attribute of the SideBar object
     *
     *@param  w  The new score value
     *@param  b  The new score value
     */
    public void setScore(float w, float b) {
        normalTools.setScore(w, b);
    } //}}}

    //{{{ setResult() method
    /**
     *  Sets the result attribute of the SideBar object
     *
     *@param  terrW  The new result value
     *@param  capsW  The new result value
     *@param  terrB  The new result value
     *@param  capsB  The new result value
     *@param  komi   The new result value
     */
    public void setResult(int terrW, int capsW, int terrB, int capsB, float komi) {
        if (scoreTools != null)
            scoreTools.setResult(terrW, capsW, terrB, capsB, komi);
    } //}}}

    //{{{ setGameInfo() method
    /**
     *  Sets the player names, handicap and komi
     *
     *@param  white     White name
     *@param  black     Black name
     *@param  handicap  Handicap
     *@param  komi      Komi
     */
    public void setGameInfo(final String white, final String black, final int handicap, final float komi) {
        normalTools.setGameInfo(white, black, handicap, komi);
    } //}}}

    //{{{ getMarkType() method
    /**
     *  Gets the markType attribute of the SideBar object
     *
     *@return    The markType value
     */
    public int getMarkType() {
        return editTools.getMarkType();
    } //}}}

    //{{{ setMarkType() method
    /**
     *  Sets the markType attribute of the SideBar object
     *
     *@param  t  The new markType value
     */
    public void setMarkType(int t) {
        editTools.setMarkType(t);
    } //}}}

    //{{{ getClockBlack() method
    /**
     *  Gets the black clock object
     *
     *@return    The black clock object
     */
    public Clock getClockBlack() {
        return normalTools.getClockBlack();
    } //}}}

    //{{{ getClockWhite() method
    /**
     *  Gets the white clock object
     *
     *@return    The white clock object
     */
    public Clock getClockWhite() {
        return normalTools.getClockWhite();
    } //}}}
}

