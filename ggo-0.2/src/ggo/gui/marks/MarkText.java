/*
 *  MarkText.java
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
 *  Text mark
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class MarkText extends Mark implements Defines {
    //{{{ private members
    /**  Mark text */
    protected String txt;
    /**  Counter how often this letter was used */
    protected int counter;
    //}}}

    //{{{ MarkText constructor
    /**
     *  Constructor for the MarkText object
     *
     *@param  x    X coordinate of the mark
     *@param  y    Y coordinate of the mark
     *@param  txt  Mark text
     */
    public MarkText(int x, int y, String txt) {
        this.x = x;
        this.y = y;
        this.txt = txt;
        counter = -1;
    } //}}}

    //{{{ getType() method
    public int getType() {
        return MARK_TEXT;
    } //}}}

    //{{{ getText() method
    /**
     *  Get the text of this mark
     *
     *@return    The mark text
     */
    public String getText() {
        return txt;
    } //}}}

    //{{{ setText() method
    /**
     *  Set the text of this mark
     *
     *@param  s  The new mark text
     */
    public void setText(String s) {
        txt = s;
    } //}}}

    //{{{ getCounter() method
    /**
     *  Gets the counter attribute of the MarkText object
     *
     *@return    The counter value
     */
    public int getCounter() {
        return counter;
    } //}}}

    //{{{ setCounter() method
    /**
     *  Sets the counter attribute of the MarkText object
     *
     *@param  c  The new counter value
     */
    public void setCounter(int c) {
        counter = c;
    } //}}}

    //{{{ drawShape() method
    public void drawShape(Graphics2D g, int x, int y, int w, int h) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font oldFont = g.getFont();

        w *= 0.6;
        int oldW = w;

        // Adjust font size to text length
        if (txt.length() > 1)
            w /= (double)(txt.length()) * 0.55;

        // System.err.println("Txt = " + txt + ", Length  = " + txt.length() + ", oldW = " + oldW + ", w = " + w);

        Font font = new Font(oldFont.getName(), Font.BOLD, w);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics();
        int sizeX = (int)(fm.getStringBounds(txt, g).getWidth());
        int sizeY = (int)(fm.getStringBounds(txt, g).getHeight());

        g.drawString(txt, x - sizeX / 2, y + sizeY / 3);

        g.setFont(oldFont);
    } //}}}
}

