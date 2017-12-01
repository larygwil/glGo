/*
 *  GameTableModel.java
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
 *  TableModel for the games table
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.5 $, $Date: 2002/09/21 12:39:56 $
 */
public class GamesTableModel extends IGSTableModel {
    final static int numCols = 13;
    private final static int observeColumn = 12;
    final static int[] widths = {30, 80, 30, 80, 30, 40, 30, 30, 30, 30, 20, 30, 30};

    //{{{ GamesTableModel() constructor
    /**Constructor for the GamesTableModel object */
    public GamesTableModel() {
        super(new String[]{
                gGo.getIGSResources().getString("ID"),
                gGo.getIGSResources().getString("White"),
                gGo.getIGSResources().getString("Rank"),
                gGo.getIGSResources().getString("Black"),
                gGo.getIGSResources().getString("Rank"),
                gGo.getIGSResources().getString("Move"),
                gGo.getIGSResources().getString("Size"),
                gGo.getIGSResources().getString("Handicap"),
                gGo.getIGSResources().getString("Komi"),
                gGo.getIGSResources().getString("ByoYomi"),
                gGo.getIGSResources().getString("Type"),
                gGo.getIGSResources().getString("Obs"),
                gGo.getIGSResources().getString("Observe")});
        types = new Class[]{
                Integer.class, String.class, IGSRank.class, String.class, IGSRank.class, Integer.class, Integer.class,
                Integer.class, Float.class, Integer.class, String.class, Integer.class, Boolean.class
                };

        // Default sorting: White rank, ascending
        rememberColumn = 2;
        rememberAscending = true;
    } //}}}

    //{{{ tableChanged() method
    /**
     *  A table cell was updated. If it is the observe column, trigger observe or unobserve.
     *
     *@param  row     Row
     *@param  column  Column
     */
    protected void tableChanged(int row, int column) {
        if (column != observeColumn)
            return;
        int gameID = ((Integer)getValueAt(row, 0)).intValue();
        boolean onOff = ((Boolean)getValueAt(row, observeColumn)).booleanValue();

        try {
            // Start observe
            if (onOff && !IGSConnection.getGameObserver().observesGame(gameID)) {
                if (!IGSConnection.startObserve(gameID))
                    setValueAt(new Boolean(false), row, column);
            }
            // Stop observe
            else if (!onOff && IGSConnection.getGameObserver().observesGame(gameID)) {
                IGSConnection.endObserve(gameID);
            }
        } catch (NullPointerException e) {
            System.err.println("Failed to observe/unobserve game: " + e);
        }
    } //}}}

    //{{{ isCellEditable() method
    /**
     *  Check if a cell is editable
     *
     *@param  row     Row
     *@param  column  Column
     *@return         True if cell is editable, else false
     */
    public boolean isCellEditable(int row, int column) {
        if (column == observeColumn)
            return true;
        return false;
    } //}}}

    //{{{ addGame() method
    /**
     *  Adds a game to the table
     *
     *@param  g  Game to add
     */
    public void addGame(Game g) {
        for (int i = 0, sz = getRowCount(); i < sz; i++) {
            int gameID = ((Integer)getValueAt(i, 0)).intValue();
            if (gameID == g.getGameID()) {
                removeRow(i);
                break;
            }
        }

        addRow(new Object[]{
                new Integer(g.getGameID()),
                g.getWhiteName(),
                g.getWhiteRank(),
                g.getBlackName(),
                g.getBlackRank(),
                new Integer(g.getMove()),
                new Integer(g.getSize()),
                new Integer(g.getHandicap()),
                new Float(g.getKomi()),
                new Integer(g.getByoYomi()),
                g.getType(),
                new Integer(g.getObservers()),
        // --- 1.3 ---
        // Boolean.valueOf(g.isObserved())
                new Boolean(g.isObserved())
                });
    } //}}}

    //{{{ observeGame() method
    /**
     *  Start observing a game
     *
     *@param  game  Game ID
     */
    public void observeGame(int game) {
        for (int i = 0, sz = getRowCount(); i < sz; i++) {
            int gameID = ((Integer)getValueAt(i, 0)).intValue();
            if (gameID == game) {
                // --- 1.3 ---
                // setValueAt(Boolean.valueOf(true), i, observeColumn);
                setValueAt(new Boolean(true), i, observeColumn);
                break;
            }
        }
    } //}}}

    //{{{ unobserveGame() method
    /**
     *  Unobserve a game
     *
     *@param  game  Game ID
     */
    public void unobserveGame(int game) {
        for (int i = 0, sz = getRowCount(); i < sz; i++) {
            int gameID = ((Integer)getValueAt(i, 0)).intValue();
            if (gameID == game) {
                // --- 1.3 ---
                // setValueAt(Boolean.valueOf(false), i, observeColumn);
                setValueAt(new Boolean(false), i, observeColumn);
                break;
            }
        }
    } //}}}

    //{{{ sortColumn() method
    /**
     *  Sort a column ascending or descending
     *
     *@param  column     Column to sort
     *@param  ascending  True if ascending, else descending
     */
    protected void sortColumn(int column, boolean ascending) {
        if (column == observeColumn) {
            System.err.println("Sorting this column is not supported.");
            return;
        }

        // Remember sort type
        rememberColumn = column;
        rememberAscending = ascending;

        if (getRowCount() < 2)
            return;

        // Reverse observer row
        if (column == 6)
            ascending = !ascending;

        try {
            // Assemble data
            Vector dataVector = new Vector(getDataVector());
            int dvSize = dataVector.size();
            ArrayList rows = new ArrayList(dvSize);
            ArrayList keys = new ArrayList(dvSize);
            Hashtable all = new Hashtable(dvSize);
            for (int i = 0, sz = dataVector.size(); i < sz; i++) {
                rows.add(((Vector)dataVector.elementAt(i)).elementAt(column));
                keys.add(((Vector)dataVector.elementAt(i)).elementAt(0));
                all.put(((Vector)dataVector.elementAt(i)).elementAt(0),
                        ((Vector)dataVector.elementAt(i)));
            }

            // Sort column ascending or descending
            sort(rows, keys, 0, rows.size() - 1, ascending, types[column]);

            // Modify table
            clear();
            for (int i = 0, sz = rows.size(); i < sz; i++) {
                Integer key = (Integer)keys.get(i);
                Vector row = (Vector)all.get(key);
                addRow(new Object[]{
                        (Integer)row.elementAt(0),
                        (String)row.elementAt(1),
                        (IGSRank)row.elementAt(2),
                        (String)row.elementAt(3),
                        (IGSRank)row.elementAt(4),
                        (Integer)row.elementAt(5),
                        (Integer)row.elementAt(6),
                        (Integer)row.elementAt(7),
                        (Float)row.elementAt(8),
                        (Integer)row.elementAt(9),
                        (String)row.elementAt(10),
                        (Integer)row.elementAt(11),
                        (Boolean)row.elementAt(12),
                        });
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Failed sorting table: " + e);
        }
    } //}}}
}

