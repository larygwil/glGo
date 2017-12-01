/*
 *  CoordsDisplay.java
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
import ggo.Defines;

/**
 *  Subclass of JLabel to display the coordinates of the current mouse position.
 *  Number values such as 10/10 are transformed into the proper point like K10.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
class CoordsDisplay extends JLabel implements Defines {
    //{{{ CoordsDisplay() constructor
    /**Constructor for the CoordsDisplay object */
    CoordsDisplay() {
        setText("A/1");
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
    } //}}}

    //{{{ setCoords() method
    /**
     *  Display the coordinates in the label. Positions are transformed into K10 etc.
     *
     *@param  x          x position
     *@param  y          y position
     *@param  boardSize  Current board size
     */
    void setCoords(int x, int y, int boardSize) {
        setText((char)('A' + (x < 9 ? x : x + 1) - 1) + " " + String.valueOf(boardSize - y + 1));
    } //}}}
}

