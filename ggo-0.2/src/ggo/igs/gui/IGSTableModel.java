/*
 *  IGSTableModel.java
 */
package ggo.igs.gui;

import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.JTable;
import javax.swing.table.*;
import java.util.*;
import ggo.igs.*;

/**
 *  Abstract superclass for the games and player table
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1.1.1 $, $Date: 2002/07/29 03:24:24 $
 */
public abstract class IGSTableModel extends DefaultTableModel implements MouseListener, TableModelListener {
    //{{{ protected members
    /**  Pointer to the table this model belongs to */
    protected JTable table;
    /**  Array of the column classes */
    protected Class[] types;
    /**  Last column sort */
    protected int rememberColumn;
    /**  Last ascending or descending sort */
    protected boolean rememberAscending;
    //}}}

    //{{{ IGSTableModel() constructor
    /**
     *Constructor for the IGSTableModel object
     *
     *@param  s  Init string
     */
    public IGSTableModel(String[] s) {
        super(s, 0);
    } //}}}

    //{{{ getRememberColumn() method
    /**
     *  Gets the rememberColumn attribute of the IGSTableModel object
     *
     *@return    The rememberColumn value
     */
    public int getRememberColumn() {
        return rememberColumn;
    } //}}}

    //{{{ getRememberAscending() method
    /**
     *  Gets the rememberAscending attribute of the IGSTableModel object
     *
     *@return    The rememberAscending value
     */
    public boolean getRememberAscending() {
        return rememberAscending;
    } //}}}

    //{{{ abstract sortColumn() method
    /**
     *  Sort a column, ascending or descending
     *
     *@param  column     Column to sort
     *@param  ascending  True of ascending sort, false if descending.
     */
    protected abstract void sortColumn(int column, boolean ascending); //}}}

    //{{{ init() method
    /**
     *  Init the table model
     *
     *@param  table  Table this model belongs to
     */
    protected void init(JTable table) {
        this.table = table;
        table.setColumnSelectionAllowed(false);
        JTableHeader th = table.getTableHeader();
        th.addMouseListener(this);
        addTableModelListener(this);
    } //}}}

    //{{{ getValueAt() method
    /**
     *  Overwritten from DefaultTableModel, to catch ArrayIndexOutOfBoundsException.
     *
     *@param  row     Table row
     *@param  column  Table column
     *@return         Object in the table cell at row, column
     */
    public Object getValueAt(int row, int column) {
        Object o = null;
        try {
            o = super.getValueAt(row, column);
        } catch (ArrayIndexOutOfBoundsException e) {
            // System.err.println("Problem while sorting table at row " + row + ", column " + column + ": " + e);
        } catch (NullPointerException e) {
            // System.err.println("Problem while sorting table at row " + row + ", column " + column + ": " + e);
        }
        return o;
    } //}}}

    //{{{ getColumnClass() method
    /**
     *  Gets the class of a given column
     *
     *@param  c  Column
     *@return    Class used by the column
     */
    public Class getColumnClass(int c) {
        return types[c];
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

    //{{{ sort() method
    /**
     *  Quicksort implemention. We need to sort two arrays at the same time.
     *
     *@param  rows       List with the table rows
     *@param  keys       List with the keys
     *@param  l          Left border
     *@param  r          Right border
     *@param  ascending  If true, the list is sorted ascending, else descending
     *@param  type       Class of the list elements
     */
    protected void sort(ArrayList rows, ArrayList keys, int l, int r, boolean ascending, Class type) {
        int i;
        int j;
        Object v;
        if (r > l) {
            v = rows.get(r);
            i = l - 1;
            j = r;
            while (true) {
                if (type == Integer.class) {
                    while (++i < rows.size() && ((Integer)rows.get(i)).compareTo(v) * (ascending ? 1 : -1) < 0)
                        ;
                    while (--j > 0 && ((Integer)rows.get(j)).compareTo(v) * (ascending ? 1 : -1) > 0)
                        ;
                }
                else if (type == String.class) {
                    while (++i < rows.size() && ((String)rows.get(i)).toLowerCase().compareTo(((String)v).toLowerCase()) * (ascending ? 1 : -1) < 0)
                        ;
                    while (--j > 0 && ((String)rows.get(j)).toLowerCase().compareTo(((String)v).toLowerCase()) * (ascending ? 1 : -1) > 0)
                        ;
                }
                else if (type == IGSRank.class) {
                    while (++i < rows.size() && ((IGSRank)rows.get(i)).compareTo(v) * (ascending ? 1 : -1) < 0)
                        ;
                    while (--j > 0 && ((IGSRank)rows.get(j)).compareTo(v) * (ascending ? 1 : -1) > 0)
                        ;
                }
                else {
                    System.err.println("Don't know how to sort this class.");
                    return;
                }
                if (i >= j)
                    break;
                // --- 1.3 ---
                // Collections.swap(rows, i, j);
                // Collections.swap(keys, i, j);
                swap(rows, i, j);
                swap(keys, i, j);
            }
            // --- 1.3 ---
            // Collections.swap(rows, i, r);
            // Collections.swap(keys, i, r);
            swap(rows, i, r);
            swap(keys, i, r);
            sort(rows, keys, l, i - 1, ascending, type);
            sort(rows, keys, i + 1, r, ascending, type);
        }
    } //}}}

    //{{{ swap() method
    // --- 1.3 ---
    /**
     *  Swap two list elements. Own implementation for Java 1.3
     *
     *@param  list  List with the elements to swap
     *@param  i     First element to swap
     *@param  j     Second element to swap
     */
    private void swap(ArrayList list, int i, int j) {
        Object o = list.get(j);
        list.set(j, list.get(i));
        list.set(i, o);
    } //}}}

    //{{{ mouseClicked() method
    /**
     *  MouseListener method
     *
     *@param  e  MouseEvent
     */
    public void mouseClicked(MouseEvent e) {
        TableColumnModel columnModel = table.getColumnModel();
        int viewColumn = columnModel.getColumnIndexAtX(e.getX());
        int column = table.convertColumnIndexToModel(viewColumn);
        if (e.getClickCount() == 1 && column != -1) {
            // Left click: ascending, right click: descending
            boolean ascending = (e.getModifiers() & MouseEvent.BUTTON3_MASK) != MouseEvent.BUTTON3_MASK;
            sortColumn(column, ascending);
        }
    } //}}}

    //{{{ tableChanged(TableModelEvent) method
    /**
     *  TableModelListener method. A table cell was changed
     *
     *@param  e  TableModelEvent
     */
    public void tableChanged(TableModelEvent e) {
        int row = e.getFirstRow();
        int column = e.getColumn();
        if (isCellEditable(row, column))
            tableChanged(row, column);
    } //}}}

    //{{{ abstract tableChanged(int, int) method
    /**
     *  A table cell was updated. Has to be implemented by subclasses
     *
     *@param  row     Row
     *@param  column  Column
     */
    protected abstract void tableChanged(int row, int column); //}}}

    //{{{ empty mouse methods
    /**
     *  MouseListener method
     *
     *@param  e  MouseEvent
     */
    public void mouseEntered(MouseEvent e) { }

    /**
     *  MouseListener method
     *
     *@param  e  MouseEvent
     */
    public void mouseExited(MouseEvent e) { }

    /**
     *  MouseListener method
     *
     *@param  e  MouseEvent
     */
    public void mousePressed(MouseEvent e) { }

    /**
     *  MouseListener method
     *
     *@param  e  MouseEvent
     */
    public void mouseReleased(MouseEvent e) { } //}}}
}

