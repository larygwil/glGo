/*
 *  PlayerTableModel.java
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

import java.util.*;
import ggo.gGo;
import ggo.igs.*;
import ggo.igs.gui.*;

/**
 *  Table model for the Player table. This handles sorting, storing and querying
 *  methods operating on the player data.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.6 $, $Date: 2002/10/05 11:21:31 $
 */
public class PlayerTableModel extends IGSTableModel {
    final static int numCols = 6;
    final static int[] widths = {40, 100, 40, 40, 40, 40};

    //{{{ PlayerTableModel() constructor
    /**Constructor for the PlayerTableModel object */
    public PlayerTableModel() {
        super(new String[]{
                gGo.getIGSResources().getString("Status"),
                gGo.getIGSResources().getString("Name"),
                gGo.getIGSResources().getString("Rank"),
                gGo.getIGSResources().getString("Idle"),
                gGo.getIGSResources().getString("Playing"),
                gGo.getIGSResources().getString("Observing")});
        types = new Class[]{
                String.class, String.class, IGSRank.class, String.class, String.class, String.class
                };

        // Default sorting: Rank, ascending
        rememberColumn = 2;
        rememberAscending = true;
    } //}}}

    //{{{ addPlayer() method
    /**
     *  Add a player as last row to the table
     *
     *@param  p  Player to add
     */
    public void addPlayer(Player p) {
        addRow(new Object[]{
                p.getStatus(),
                p.getName(),
                p.getRank(),
                p.getIdle(),
                (p.getGame() == 0 ? "--" : String.valueOf(p.getGame())),
                (p.getObserve() == 0 ? "--" : String.valueOf(p.getObserve()))
                });
    } //}}}

    //{{{ removePlayer() method
    /**
     *  Remove a player
     *
     *@param  name  Name of the player to remove
     */
    public void removePlayer(String name) {
        for (int i = 0; i < getRowCount(); i++) {
            String s = (String)getValueAt(i, 1);
            if (s.equals(name)) {
                removeRow(i);
                break;
            }
        }
    } //}}}

    //{{{ getNameAtRow() method
    /**
     *  Get the playername at a given row
     *
     *@param  row  Row to check
     *@return      Name of player
     */
    public String getNameAtRow(int row) {
        if (row < 0 || row >= getRowCount())
            return null;
        return (String)getValueAt(row, 1);
    } //}}}

    //{{{ getGameOfPlayer() method
    /**
     *  Get the game ID of a player specified by the given row
     *
     *@param  row  Row to check
     *@return      Game ID or 0 if not playing
     */
    public int getGameOfPlayer(int row) {
        String gameStr = (String)getValueAt(row, 4);
        int game = 0;
        try {
            game = Integer.parseInt(gameStr.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
        return game;
    } //}}}

    //{{{ sortColumn() method
    /**
     *  Sort this table
     *
     *@param  column     Which column to sort
     *@param  ascending  True will sort ascending, false descending
     */
    protected void sortColumn(int column, boolean ascending) {
        if (getRowCount() < 2)
            return;

        // Remember sort type
        rememberColumn = column;
        rememberAscending = ascending;

        try {
            // Assemble data
            Vector dataVector = new Vector(getDataVector());
            int dvSize = dataVector.size();
            ArrayList rows = new ArrayList(dvSize);
            ArrayList keys = new ArrayList(dvSize);
            Hashtable all = new Hashtable(dvSize);
            Class typeClass = types[column];
            for (int i = 0, sz = dataVector.size(); i < sz; i++) {
                // If the rank column is sorted, concatenate rank (as integer) and name (lowercase) to have
                // the name as secondary sorting value
                if (column == 2) {
                    rows.add(
                            String.valueOf(((IGSRank)((Vector)dataVector.elementAt(i)).elementAt(column)).getRank() + 10) +
                            (String)(((Vector)dataVector.elementAt(i)).elementAt(1)));
                    typeClass = String.class;
                }
                else
                    rows.add(((Vector)dataVector.elementAt(i)).elementAt(column));
                keys.add(((Vector)dataVector.elementAt(i)).elementAt(1));
                all.put(((Vector)dataVector.elementAt(i)).elementAt(1),
                        ((Vector)dataVector.elementAt(i)));
            }

            // Sort column ascending or descending
            sort(rows, keys, 0, rows.size() - 1, ascending, typeClass);

            // Modify table
            clear();
            for (int i = 0, sz = rows.size(); i < sz; i++) {
                String key = (String)keys.get(i);
                Vector row = (Vector)all.get(key);
                addRow(new Object[]{
                        (String)row.elementAt(0),
                        (String)row.elementAt(1),
                        (IGSRank)row.elementAt(2),
                        (String)row.elementAt(3),
                        (String)row.elementAt(4),
                        (String)row.elementAt(5)
                        });
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Failed sorting table: " + e);
        }
    } //}}}

    //{{{ tableChanged() method
    /**
     *  A table cell was updated. Does nothing in this class.
     *
     *@param  row     Row
     *@param  column  Column
     */
    protected void tableChanged(int row, int column) { } //}}}
}

