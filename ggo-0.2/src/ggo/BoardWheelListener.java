/*
 *  BoardWheelListener.java
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
package ggo;

import java.awt.event.*;
import ggo.*;

// --- 1.3 ---
/**
 *  MouseWheelListener class. Listens to mousewheel events over the board.
 *  Separated to have 1.3 ignoring this.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
class BoardWheelListener implements MouseWheelListener {
    //{{{ private members
    private Board board;
    //}}}

    //{{{ init() method
    /**
     *  Init this listener and add it to the board. Has to be here for 1.3.
     *
     *@param  b  Board to add this listener too
     */
    public void init(Board b) {
        board = b;
        board.addMouseWheelListener(this);
    } //}}}

    //{{{ mouseWheelMoved() method
    /**
     *  Mouse wheel moved
     *
     *@param  e  MouseWheelEvent
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        try {
            // Wheel - next/previous move
            if (e.getModifiers() == 0) {
                if (e.getWheelRotation() > 0)
                    board.getBoardHandler().nextMove(false);
                else
                    board.getBoardHandler().previousMove();
            }
            // Wheel + right button - next/previous variation
            else if (board.isEditable() &&
                    (e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK) {
                if (e.getWheelRotation() > 0)
                    board.getBoardHandler().nextVariation();
                else
                    board.getBoardHandler().previousVariation();
            }
        } catch (NullPointerException ex) {}
    } //}}}
}

