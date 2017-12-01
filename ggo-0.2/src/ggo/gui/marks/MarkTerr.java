/*
 *  MarkTerr.java
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
package ggo.gui.marks;

import java.awt.*;
import ggo.Defines;

/**
 *  Territory mark
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class MarkTerr extends MarkCross {
    private int color;

    //{{{ MarkTerr constructor
    /**
     *  Constructor for the MarkTerr object
     *
     *@param  x      X coordinate of the mark
     *@param  y      Y coordinate of the mark
     *@param  color  Color of the marked territory
     */
    public MarkTerr(int x, int y, int color) {
        super(x, y);
        this.color = color;
    } //}}}

    //{{{ getType() method
    public int getType() {
        return color == STONE_BLACK ? MARK_TERR_BLACK : MARK_TERR_WHITE;
    } //}}}

    //{{{ getColor() method
    /**
     *  Get the color of the player the territory belongs to
     *
     *@return    The play color
     */
    public int getColor() {
        return color;
    } //}}}

    //{{{ drawShape() method
    public void drawShape(Graphics2D g, int x, int y, int w, int h) {
        Color oldColor = g.getColor();
        g.setColor(color == STONE_BLACK ? Color.black : Color.white);
        super.drawShape(g, x, y, w, h);
        g.setColor(oldColor);
    } //}}}
}

