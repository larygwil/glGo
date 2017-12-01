/*
 *  MarkCircle.java
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
import java.awt.geom.*;
import ggo.Defines;

/**
 *  Circle mark
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class MarkCircle extends Mark implements Defines {
    //{{{ MarkCircle constructor
    /**
     *  Constructor for the MarkCircle object
     *
     *@param  x  X coordinate of the mark
     *@param  y  Y coordinate of the mark
     */
    public MarkCircle(int x, int y) {
        this.x = x;
        this.y = y;
    } //}}}

    //{{{ getType() method
    public int getType() {
        return MARK_CIRCLE;
    } //}}}

    //{{{ drawShape() method
    public void drawShape(Graphics2D g, int x, int y, int w, int h) {
        int sizeX = (int)(w * 0.5);
        int sizeY = (int)(h * 0.5);

        double xd = x - (double)w / 4.0;
        double yd = y - (double)h / 4.0;

        // Even size gives better result
        if (sizeX % 2 > 0) {
            sizeX ++;
            sizeY ++;
            xd -= 0.5;
            yd -= 0.5;
        }

        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke((float)w / 15.0f));
        g.draw(new Ellipse2D.Double(xd, yd, sizeX, sizeY));
        g.setStroke(oldStroke);
    } //}}}
}

