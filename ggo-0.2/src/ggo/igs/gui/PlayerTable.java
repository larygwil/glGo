/*
 *  PlayerTable.java
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

import ggo.igs.*;
import ggo.igs.gui.*;
import ggo.utils.*;
import ggo.gGo;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.text.*;

/**
 *  Frame showing the table with the player list
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.12 $, $Date: 2002/09/25 11:20:58 $
 */
public class PlayerTable extends JFrame implements PlayerPopupParent {
    //{{{ private members
    private PlayerTableModel playerTableModel;
    private PlayerPopup popup;
    private PTable table;
    private JTextField fromTextField, toTextField;
    private JButton closeButton;
    private JToggleButton openButton, friendsButton;
    private JLabel totalLabel;
    private ResourceBundle igs_resources;
    private MessageFormat msgFormat;
    //}}}

    //{{{ PlayerTable() constructor
    /**Constructor for the PlayerTable object */
    public PlayerTable() {
        igs_resources = gGo.getIGSResources();

        playerTableModel = new PlayerTableModel();
        popup = new PlayerPopup(this);
        initComponents();

        // Create MessageFormat for the total players label
        msgFormat = new MessageFormat(igs_resources.getString("players_total"));
        double[] limits = {0, 1, 2};
        String[] totalStrings = {
                igs_resources.getString("players"),
                igs_resources.getString("player"),
                igs_resources.getString("players")};
        msgFormat.setFormats(new Format[]{NumberFormat.getInstance(gGo.getLocale()), new ChoiceFormat(limits, totalStrings)});
        setTotal();

        pack();

        // Restore location
        Point p = gGo.getSettings().getStoredLocation(getClass().getName());
        if (p == null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(screenSize.width - getWidth(), 0);
        }
        else
            setLocation(p);

        // Restore size
        Dimension size = gGo.getSettings().getStoredSize(getClass().getName());
        if (size != null)
            setSize(size);

        // Create an icon for the appliction
        Image icon = ImageHandler.loadImage("32red.png");
        try {
            setIconImage(icon);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icon image.");
        }
    } //}}}

    //{{{ initComponents() method
    /**  Init GUI elements */
    private void initComponents() {
        setTitle(igs_resources.getString("Players"));
        getContentPane().setLayout(new BorderLayout(5, 5));

        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED), new EmptyBorder(new Insets(5, 5, 5, 5))));

        table = new PTable(this);
        table.setModel(playerTableModel);
        playerTableModel.init(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(false);
        table.addMouseListener(popup);
        // Set table column widths
        for (int i = 0; i < PlayerTableModel.numCols; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(PlayerTableModel.widths[i]);

        JScrollPane jScrollPane1 = new JScrollPane();
        jScrollPane1.setViewportView(table);
        tablePanel.add(jScrollPane1, BorderLayout.CENTER);

        getContentPane().add(tablePanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

        totalLabel = new JLabel();
        totalLabel.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(new Insets(4, 6, 4, 6))));
        bottomPanel.add(totalLabel);

        JPanel rangePanel = new JPanel();
        rangePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JPanel rangeSelectPanel = new JPanel();
        rangeSelectPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));
        rangeSelectPanel.setBorder(new EtchedBorder());

        JLabel fromLabel = new JLabel(igs_resources.getString("From:"));
        rangeSelectPanel.add(fromLabel);

        fromTextField = new JTextField();
        fromTextField.setHorizontalAlignment(JTextField.CENTER);
        fromTextField.setText(gGo.getSettings().getUpperRank());
        fromTextField.setPreferredSize(new Dimension(30, 20));
        rangeSelectPanel.add(fromTextField);

        JLabel toLabel = new JLabel(igs_resources.getString("To:"));
        rangeSelectPanel.add(toLabel);

        toTextField = new JTextField();
        toTextField.setHorizontalAlignment(JTextField.CENTER);
        toTextField.setText(gGo.getSettings().getLowerRank());
        toTextField.setPreferredSize(new Dimension(30, 20));
        rangeSelectPanel.add(toTextField);

        rangePanel.add(rangeSelectPanel);

        JPanel buttonPanel = new JPanel();
        bottomPanel.add(rangePanel);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        friendsButton = new JToggleButton(igs_resources.getString("Friends"));
        friendsButton.setToolTipText(igs_resources.getString("friends_tooltip"));
        friendsButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    doRefresh();
                }
            });
        buttonPanel.add(friendsButton);

        openButton = new JToggleButton(igs_resources.getString("Available"));
        openButton.setToolTipText(igs_resources.getString("available_tooltip"));
        openButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    doRefresh();
                }
            });
        buttonPanel.add(openButton);

        JButton refreshButton = new JButton(igs_resources.getString("Refresh"));
        refreshButton.setToolTipText(igs_resources.getString("refresh_tooltip"));
        refreshButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    doRefresh();
                }
            });
        buttonPanel.add(refreshButton);

        closeButton = new JButton(igs_resources.getString("Close"));
        closeButton.setToolTipText(igs_resources.getString("close_window_tooltip"));
        closeButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    setVisible(false);
                }
            });
        buttonPanel.add(closeButton);

        bottomPanel.add(buttonPanel);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
    } //}}}

    //{{{ setTotal() method
    /**  Sets the total attribute of the PlayerTable object */
    public void setTotal() {
        int rows = playerTableModel.getRowCount();

        try {
            totalLabel.setText(msgFormat.format(new Object[]{new Integer(rows), new Integer(rows)}));
        } catch (NullPointerException e) {
            System.err.println("Failed to set total player value: " + e);
            totalLabel.setText(String.valueOf(rows) + (rows == 1 ? " player" : " players"));
        }
    } //}}}

    //{{{ doClear() method
    /**  Clear the table, remove all entries */
    public void doClear() {
        playerTableModel.clear();
    } //}}}

    //{{{ doRefresh() method
    /**  Refresh the table, called when Refresh button is clicked */
    protected void doRefresh() {
        // Clear table
        doClear();

        String command;

        // If friends is toggled, ignore rank range
        if (friendsOnly()) {
            command = "who all";
        }
        else {
            // Assemble command
            String from = fromTextField.getText();
            String to = toTextField.getText();

            // Remember ranks
            gGo.getSettings().setUpperRank(from);
            gGo.getSettings().setLowerRank(to);
            gGo.getSettings().saveSettings();

            // Transform ??? to 31k
            if (from.equals("???"))
                from = "31k";
            if (to.equals("???"))
                to = "31k";

            // Transform NR to 32k
            if (from.equals("NR"))
                from = "32k";
            if (to.equals("NR"))
                to = "32k";

            // Empty range fields?
            if (from == null || to == null || (from.length() == 0 && to.length() == 0))
                command = "who all";
            else
                command = "who " + from + "-" + to;
        }

        // Open only?
        if (openButton.isSelected())
            command += " o";

        // Send command to IGS
        IGSConnection.sendCommand(command);
    } //}}}

    //{{{ addPlayer() method
    /**
     *  Add a player to the table
     *
     *@param  p  Object of the Player to add
     */
    public void addPlayer(Player p) {
        if (!friendsOnly() ||
                IGSConnection.getMainWindow().getBozoHandler().getBozoStatus(p.getName()) == BozoHandler.PLAYER_STATUS_FRIEND)
            playerTableModel.addPlayer(p);
    } //}}}

    //{{{ removePlayer(String) method
    /**
     *  Remove a player from the table
     *
     *@param  name  String with the name of the player to remove
     */
    public void removePlayer(String name) {
        playerTableModel.removePlayer(name);
    } //}}}

    //{{{ sortTable() method
    /**  Sort the table */
    public void sortTable() {
        playerTableModel.sortColumn(
                playerTableModel.getRememberColumn(), playerTableModel.getRememberAscending());

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
     *  Gets the closeButton object
     *
     *@return    The closeButton object
     */
    public JButton getCloseButton() {
        return closeButton;
    } //}}}

    //{{{ availableOnly() method
    /**
     *  Check if the Available toggle button is selected
     *
     *@return    True if the button is selected, else false
     */
    public boolean availableOnly() {
        return openButton.isSelected();
    } //}}}

    //{{{ friendsOnly() method
    /**
     *  Check if the Friends toggle button is selected
     *
     *@return    True if the button is selected, else false
     */
    public boolean friendsOnly() {
        return friendsButton.isSelected();
    } //}}}

    //{{{ getUpperRank() method
    /**
     *  Gets the upperRank attribute of the PlayerTable object
     *
     *@return    The upperRank value
     */
    public String getUpperRank() {
        return fromTextField.getText();
    } //}}}

    //{{{ getLowerRank() method
    /**
     *  Gets the lowerRank attribute of the PlayerTable object
     *
     *@return    The lowerRank value
     */
    public String getLowerRank() {
        return toTextField.getText();
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
        return playerTableModel.getNameAtRow(row);
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
        return playerTableModel.getGameOfPlayer(row);
    } //}}}

    //{{{ mayPopup() method
    /**
     *  Check if the player popup menu may appear on this table column.
     *  Always true for this class.
     *
     *@param  col  Table column
     *@return      True if the popup is permitted, else false
     *@see         PlayerPopupParent#mayPopup(int)
     */
    public boolean mayPopup(int col) {
        return true;
    } //}}}
}

/**
 *  Subclass of JTable, to provide custom renderes for friends/bozo colors
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.12 $, $Date: 2002/09/25 11:20:58 $
 */
class PTable extends JTable implements MouseListener {

    private DefaultTableCellRenderer friendsRenderer, bozoRenderer;
    private PlayerTable playerTable;

    //{{{ PTable() constructor
    /**
     *Constructor for the PTable object
     *
     *@param  playerTable  Description of the Parameter
     */
    PTable(PlayerTable playerTable) {
        this.playerTable = playerTable;
        friendsRenderer = new DefaultTableCellRenderer();
        friendsRenderer.setForeground(Color.blue);
        bozoRenderer = new DefaultTableCellRenderer();
        bozoRenderer.setForeground(Color.red);
        addMouseListener(this);
    } //}}}

    //{{{ getCellRenderer() method
    /**
     *  Obserwrites getCellRenderer() in JTable, checks for friend/bozo status and returns
     *  the customized renderer
     *
     *@param  row     the row of the cell to render, where 0 is the first row
     *@param  column  the column of the cell to render, where 0 is the first column
     *@return         the assigned renderer; if <code>null</code> returns the default renderer
     *                for this type of object
     *@see            JTable#getCellRenderer(int, int)
     */
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (IGSConnection.getMainWindow().getBozoHandler().getBozoStatus(((PlayerTableModel)getModel()).getNameAtRow(row)) ==
                BozoHandler.PLAYER_STATUS_FRIEND)
            return friendsRenderer;
        else if (IGSConnection.getMainWindow().getBozoHandler().getBozoStatus(((PlayerTableModel)getModel()).getNameAtRow(row)) ==
                BozoHandler.PLAYER_STATUS_BOZO)
            return bozoRenderer;
        else
            return super.getCellRenderer(row, column);
    } //}}}

    //{{{ MouseListener methods
    /**
     *  Mousebutton was clicked. Quick access to player actions:
     *  Double-click left button sends stats
     *  Double-click middle button or double-click left button + shift sends match
     *
     *@param  e  MouseEvent
     */
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() != 2)
            return;

        try {
            // Middle doubleclick or Left doubleclick + shift: match
            if (((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK) ||
                    ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK &&
                    (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK))
                IGSConnection.sendMatch(playerTable.getPlayerName(rowAtPoint(e.getPoint()), 0));
            // Left doubleclick: stats
            else if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK)
                IGSConnection.sendCommand("stats " + playerTable.getPlayerName(rowAtPoint(e.getPoint()), 0));
        } catch (NullPointerException ex) {
            System.err.println("Failed to quick-access player: " + ex);
        }
    }

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
     *  Mousebutton was pressed. Empty method.
     *
     *@param  e  MouseEvent
     */
    public void mousePressed(MouseEvent e) { }

    /**
     *  Mousebutton was released. Empty method.
     *
     *@param  e  MouseEvent
     */
    public void mouseReleased(MouseEvent e) { }
    //}}}
}

