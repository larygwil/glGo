/*
 *  StoredSGFTableModel.java
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

import javax.swing.table.DefaultTableModel;

/**
 *  A subclass of DefaultTableModel, used for the stored and sgf tables in the Playerinfo dialog.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/18 00:02:07 $
 */
class StoredSGFTableModel extends DefaultTableModel {
    //{{{ StoredSGFTableModel() constructor
    /**Constructor for the StoredSGFTableModel object */
    StoredSGFTableModel() {
        super(0, 1);
    } //}}}

    //{{{ initGames() method
    /**
     *  Init the tablemodel with a games list
     *
     *@param  games  Array of games
     */
    void initGames(String[] games) {
        clear();
        for (int i = 0, sz = games.length - 1; i < sz; i++)
            addRow(new Object[]{games[i]});
    } //}}}

    //{{{ getColumnName() method
    /**
     *  Gets the column title, null in this case to prevent a titlebar
     *
     *@param  column  Column
     *@return         Null
     */
    public String getColumnName(int column) {
        return null;
    } //}}}

    //{{{ isCellEditable() method
    /**
     *  Check if a cell is editable
     *
     *@param  row     Row
     *@param  column  Column
     *@return         True if editable, else false
     */
    public boolean isCellEditable(int row, int column) {
        return false;
    } //}}}

    //{{{ clear() method
    /**  Clear the table */
    public void clear() {
        while (getRowCount() > 0)
            removeRow(0);
    } //}}}
}

