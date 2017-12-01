/*
 *  HistoryScroller.java
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
import java.awt.event.*;
import java.util.*;

/**
 *  Simple helper class that can be attached to a JInputField to implement cursor-up-down history scrolling.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class HistoryScroller extends KeyAdapter implements ActionListener {
    //{{{ private members
    private JTextField inputField;
    private ArrayList history;
    private int historyCounter;
    //}}}

    //{{{ HistoryScroller() constructor
    /**
     *Constructor for the HistoryScroller object
     *
     *@param  inputField  inputField this class is attached to
     */
    public HistoryScroller(JTextField inputField) {
        this.inputField = inputField;

        inputField.addActionListener(this);
        inputField.addKeyListener(new InputKeyListener());

        history = new ArrayList();
        historyCounter = 0;
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  ActionListener method
     *
     *@param  e  ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        String txt = e.getActionCommand();
        if (history.isEmpty() ||
                !txt.equals((String)history.get(history.size() - 1))) { // Ignore if last command identical
            if (history.size() > 20) { // Limit history to 20 entries
                for (int i = 10; i >= 0; i--)
                    history.remove(i);
            }
            history.add(txt);
        }
        historyCounter = history.size();
    } //}}}

    //{{{ getHistoryCounter() method
    /**
     *  Gets the historyCounter attribute of the HistoryScroller object
     *
     *@return    The historyCounter value
     */
    public int getHistoryCounter() {
        return historyCounter;
    } //}}}

    //{{{ getHistoryItem() method
    /**
     *  Gets the the history String at position i
     *
     *@param  i  Position in the history we want to get
     *@return    History command String value
     */
    public String getHistoryItem(int i) {
        return (String)history.get(i);
    } //}}}

    //{{{ local class InputKeyListener
    /**
     *  Local InputKeyListener class
     *
     *@author     Peter Strempel
     *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
     */
    class InputKeyListener extends KeyAdapter {
        /**
         *  Key was released
         *
         *@param  e  KeyEvent
         */
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (history.isEmpty())
                    return;
                historyCounter++;
                if (historyCounter >= history.size())
                    historyCounter = history.size() - 1;
                inputField.setText((String)history.get(historyCounter));
            }
            else if (e.getKeyCode() == KeyEvent.VK_UP) {
                if (history.isEmpty())
                    return;
                if (historyCounter > 0)
                    historyCounter--;
                if (historyCounter >= history.size())
                    historyCounter = history.size() - 1;
                inputField.setText((String)history.get(historyCounter));
            }
        }
    } //}}}
}

