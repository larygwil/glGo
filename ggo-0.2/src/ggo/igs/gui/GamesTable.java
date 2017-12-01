/*
 *  GamesTable.java
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

import ggo.gGo;
import ggo.igs.*;
import ggo.igs.gui.*;
import ggo.utils.ImageHandler;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.ResourceBundle;
import java.text.*;

/**
 *  Frame showing the table with the games list
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.10 $, $Date: 2002/09/21 12:39:56 $
 */
public class GamesTable extends JFrame implements PlayerPopupParent {
    //{{{ private members
    private JTable table;
    private PlayerPopup popup;
    private JLabel totalLabel;
    private JButton closeButton;
    private GamesTableModel gamesTableModel;
    private ResourceBundle igs_resources;
    private MessageFormat msgFormat;
    //}}}

    //{{{ GamesTable() constructor
    /**Constructor for the GamesTable object */
    public GamesTable() {
        igs_resources = gGo.getIGSResources();

        gamesTableModel = new GamesTableModel();
        popup = new PlayerPopup(this);
        initComponents();

        // Create MessageFormat for the total games label
        msgFormat = new MessageFormat(igs_resources.getString("games_total"));
        double[] limits = {0, 1, 2};
        String[] totalStrings = {
                igs_resources.getString("games"),
                igs_resources.getString("game"),
                igs_resources.getString("games")};
        msgFormat.setFormats(new Format[]{NumberFormat.getInstance(gGo.getLocale()), new ChoiceFormat(limits, totalStrings)});
        setTotal();

        // Restore location
        Point p = gGo.getSettings().getStoredLocation(getClass().getName());
        if (p == null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(screenSize.width - getWidth(), screenSize.height - getHeight());
        }
        else
            setLocation(p);

        // Restore size
        Dimension size = gGo.getSettings().getStoredSize(getClass().getName());
        if (size != null)
            setSize(size);

        // Create an icon for the appliction
        Image icon = ImageHandler.loadImage("32yellow.png");
        try {
            setIconImage(icon);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icon image.");
        }
    } //}}}

    //{{{ initComponents() method
    /**  Init GUI elements */
    private void initComponents() {
        setTitle(igs_resources.getString("Games"));
        getContentPane().setLayout(new BorderLayout(5, 5));

        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new EmptyBorder(new Insets(5, 5, 5, 5))));

        table = new JTable();
        table.setModel(gamesTableModel);
        gamesTableModel.init(table);
        table.addMouseListener(popup);
        // Set table column widths
        for (int i = 0; i < GamesTableModel.numCols; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(GamesTableModel.widths[i]);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(tablePanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        totalLabel = new JLabel("0 games");
        totalLabel.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(new Insets(4, 6, 4, 6))));
        bottomPanel.add(totalLabel);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton refreshButton = new JButton(igs_resources.getString("Refresh"));
        refreshButton.setToolTipText(igs_resources.getString("refresh_tooltip"));
        refreshButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doRefresh();
                }
            });
        buttonPanel.add(refreshButton);

        closeButton = new JButton(igs_resources.getString("Close"));
        closeButton.setToolTipText(igs_resources.getString("close_window_tooltip"));
        closeButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
        buttonPanel.add(closeButton);

        bottomPanel.add(buttonPanel);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        pack();
    } //}}}

    //{{{ setTotal() method
    /**  Sets the total number of games */
    public void setTotal() {
        int rows = gamesTableModel.getRowCount();
        try {
            totalLabel.setText(msgFormat.format(new Object[]{new Integer(rows), new Integer(rows)}));
        } catch (NullPointerException e) {
            System.err.println("Failed to set total games value: " + e);
            totalLabel.setText(String.valueOf(rows) + (rows == 1 ? " game" : " games"));
        }
    } //}}}

    //{{{ doClear() method
    /**  Clear the table, remove all entries */
    public void doClear() {
        gamesTableModel.clear();
    } //}}}

    //{{{ doRefresh() method
    /**  Clear and refresh the table sending "games" to IGS */
    protected void doRefresh() {
        // Clear table
        doClear();

        // Send command to IGS
        try {
            IGSConnection.sendCommand("games");
        } catch (NullPointerException e) {
            System.err.println("Not connected.");
        }
    } //}}}

    //{{{ addGame() method
    /**
     *  Add a game to the table
     *
     *@param  g  Game to add
     */
    public void addGame(Game g) {
        gamesTableModel.addGame(g);
    } //}}}

    //{{{ observeGame() method
    /**
     *  Start observing a game, toggles the games observe button column
     *
     *@param  id  Game ID
     */
    public void observeGame(int id) {
        gamesTableModel.observeGame(id);
    } //}}}

    //{{{ unobserveGame() method
    /**
     *  End observing a game, toggles the games observe button column
     *
     *@param  id  Game ID
     */
    public void unobserveGame(int id) {
        gamesTableModel.unobserveGame(id);
    } //}}}

    //{{{ sortTable() method
    /**  Sort the table */
    public void sortTable() {
        gamesTableModel.sortColumn(
                gamesTableModel.getRememberColumn(), gamesTableModel.getRememberAscending());

        Timer timer = new Timer(1000,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(
                        new Runnable() {
                            public void run() {
                                setTotal();
                            }
                        });
                }
            });
        timer.setRepeats(false);
        timer.start();
    } //}}}

    //{{{ getCloseButton() method
    /**
     *  Gets the closeButton
     *
     *@return    The closeButton value
     */
    public JButton getCloseButton() {
        return closeButton;
    } //}}}

    //{{{ getPlayerName() method
    /**
     *  Gets the name of the player the popup menu was called for
     *
     *@param  row  Table row
     *@param  col  Table column
     *@return      Name of player
     *@see         PlayerPopupParent#getPlayerName(int, int)
     */
    public String getPlayerName(int row, int col) {
        try {
            return (String)gamesTableModel.getValueAt(row, col);
        } catch (ClassCastException e) {
            return "";
        }
    } //}}}

    //{{{ getGameOfPlayer() method
    /**
     *  Gets the current game of the player the popup menu was called for
     *
     *@param  row  Table row
     *@param  col  Table column
     *@return      Game ID of the game this player is currently playing
     *@see         PlayerPopupParent#getGameOfPlayer(int, int)
     */
    public int getGameOfPlayer(int row, int col) {
        try {
            return ((Integer)gamesTableModel.getValueAt(row, 0)).intValue();
        } catch (ClassCastException e) {
            return 0;
        }
    } //}}}

    //{{{ mayPopup() method
    /**
     *  Check if the player popup menu may appear on this table column
     *
     *@param  col  Table column
     *@return      True if the popup is permitted, else false
     *@see         PlayerPopupParent#mayPopup(int)
     */
    public boolean mayPopup(int col) {
        if (col == 1 || col == 3)
            return true;
        return false;
    } //}}}
}

