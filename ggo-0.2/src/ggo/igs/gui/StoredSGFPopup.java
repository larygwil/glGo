/*
 *  StoredSGFPopup.java
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

/**
 *  Abstract superclass for the StoredPopup and SGFPopup classes.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/18 00:01:57 $
 */
abstract class StoredSGFPopup extends JPopupMenu implements ActionListener, MouseListener {
    //{{{ protected members
    protected JTable parent;
    protected int popupRow;
    //}}}

    //{{{ StoredSGFPopup() constructor
    /**
     *Constructor for the StoredSGFPopup object
     *
     *@param  parent  Parent table
     */
    StoredSGFPopup(JTable parent) {
        initPopupMenu();
        this.parent = parent;
        popupRow = -1;
    } //}}}

    //{{{ initPopupMenu() method
    /**  Init popup menu */
    protected abstract void initPopupMenu(); //}}}

    //{{{ actionPerformed() method
    /**
     *  ActionListener method
     *
     *@param  e  ActionEvent
     */
    public abstract void actionPerformed(ActionEvent e); //}}}

    //{{{ MouseListener methods
    /**
     *  Mousebutton was clicked. Empty method.
     *
     *@param  e  MouseEvent
     */
    public void mouseClicked(MouseEvent e) { }

    /**
     *  Mouse entered. Empty method.
     *
     *@param  e  MouseEvent
     */
    public void mouseEntered(MouseEvent e) { }

    /**
     *  Mouse exited. Empty method.
     *
     *@param  e  MouseEvent
     */
    public void mouseExited(MouseEvent e) { }

    /**
     *  Mousebutton was pressed. Call popup menu.
     *
     *@param  e  MouseEvent
     */
    public void mousePressed(MouseEvent e) {
        popupMenu(e);
    }

    /**
     *  Mousebutton was released. Call popup menu.
     *
     *@param  e  MouseEvent
     */
    public void mouseReleased(MouseEvent e) {
        popupMenu(e);
    } //}}}

    //{{{ popupMenu() method
    /**
     *  Show the popup menu
     *
     *@param  e  MouseEvent
     */
    private void popupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            try {
                popupRow = ((JTable)e.getComponent()).rowAtPoint(e.getPoint());
                show(e.getComponent(), e.getX(), e.getY());
            } catch (NullPointerException ex) {
                System.err.println("Failed to open popup menu: " + ex);
                popupRow = -1;
            }
        }
    } //}}}
}

