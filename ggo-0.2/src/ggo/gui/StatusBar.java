/*
 *  StatusBar.java
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
import ggo.utils.Utils;
import ggo.Defines;
import ggo.gGo;

/**
 *  The StatusBar panel
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class StatusBar extends JPanel implements Defines {
    //{{{ private members
    private CoordsDisplay coordsDisplay;
    private JLabel moveDisplay, navigationDisplay;
    private boolean editable;
    //}}}

    //{{{ StatusBar constructor
    /**
     *  Constructor for the StatusBar object
     *
     *@param  editable  Description of the Parameter
     */
    public StatusBar(boolean editable) {
        this.editable = editable;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setPreferredSize(new Dimension(400, 22));
        initComponents();
    } //}}}

    //{{{ initComponents() method
    /**  Init the GUI elements */
    private void initComponents() {
        coordsDisplay = new CoordsDisplay();
        coordsDisplay.setBorder(BorderFactory.createLoweredBevelBorder());
        add(coordsDisplay);

        add(Box.createHorizontalGlue());

        moveDisplay = new JLabel("0");
        moveDisplay.setBorder(BorderFactory.createLoweredBevelBorder());
        moveDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        moveDisplay.setVerticalAlignment(SwingConstants.CENTER);
        add(moveDisplay);

        navigationDisplay = new JLabel("0/0");
        navigationDisplay.setBorder(BorderFactory.createLoweredBevelBorder());
        navigationDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        navigationDisplay.setVerticalAlignment(SwingConstants.CENTER);
        if (editable)
            navigationDisplay.setToolTipText(gGo.getBoardResources().getString("son_brother_label_tooltip"));
        else
            navigationDisplay.setToolTipText(gGo.getBoardResources().getString("caps_white_black_label_tooltip"));
        add(navigationDisplay);
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

    //{{{ printMessage() method
    /**
     *  Print a message on the lower left side of the status bar
     *
     *@param  msg  String to display
     */
    public void printMessage(String msg) {
        coordsDisplay.setText(msg);
        // TODO: Timeout until setCoords dont overwrite this.
    } //}}}

    //{{{ setMove() method
    /**
     *  Set the move data on the right part, something like 24 B(K10)
     *
     *@param  number     Move number
     *@param  color      Color of the played stone
     *@param  x          X coordinate
     *@param  y          Y coordinate
     *@param  boardSize  board size
     */
    public void setMove(int number, int color, int x, int y, int boardSize) {
        moveDisplay.setText(Utils.moveToString(number, color, x, y, boardSize));
    } //}}}

    //{{{ setNavigation() method
    /**
     *  Set the navigation data on the right part, something like 1/0 for 1 son and 0 brothers
     *
     *@param  sons      Number of sons
     *@param  brothers  Number of brothers
     */
    public void setNavigation(int sons, int brothers) {
        navigationDisplay.setText(sons + "/" + brothers);
    } //}}}
}

