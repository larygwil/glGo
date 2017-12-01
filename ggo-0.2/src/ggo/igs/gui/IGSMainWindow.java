/*
 *  IGSMainWindow.java
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
import javax.swing.text.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.text.MessageFormat;
import javax.swing.plaf.metal.*;
import ggo.*;
import ggo.dialogs.*;
import ggo.igs.*;
import ggo.igs.gui.*;
import ggo.utils.*;
import ggo.gui.*;
import ggo.igs.chatter.*;
import ggo.utils.sound.SoundHandler;
import javax.help.*;

/**
 *  Main window for server connections
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.33 $, $Date: 2002/10/26 00:49:18 $
 */
public class IGSMainWindow extends JFrame implements Defines {
    //{{{ private members
    private IGSActionListener igsActionListener;
    private JToolBar toolBar;
    private JMenuItem connectMenuItem, disconnectMenuItem;
    private JCheckBoxMenuItem viewShowShout, viewShowToolbar, toggleAway, viewShowAutoupdate;
    private JButton connectButton, disconnectButton, historyButton;
    private JToggleButton playersToggleButton, gamesToggleButton, chatToggleButton, shoutToggleButton, channelsToggleButton;
    private HistoryScroller historyScroller;
    private JScrollPane outputScrollPane;
    private JTextArea outputTextArea;
    private JTextField inputTextField;
    private static IGSConnection igsConnection = null;
    private PlayerTable playerTable = null;
    private GamesTable gamesTable = null;
    private static HostConfig hostConfig;
    private JPopupMenu popup;
    private IGSChatter chatter;
    private IGSShouter shouter;
    private IGSChannels channels;
    private boolean blockInfo = false;
    private javax.swing.Timer disconnectTimer;
    private ResourceBundle igs_resources;
    private BozoHandler bozoHandler;
    private BozoListDialog bozoListDialog;
    //}}}

    //{{{ IGSMainWindow constructor
    /**Constructor for the IGSMainWindow object */
    public IGSMainWindow() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        igsActionListener = new IGSActionListener();
        igs_resources = gGo.getIGSResources();

        initComponents();

        historyScroller = new HistoryScroller(inputTextField);

        // Restore location
        Point p = gGo.getSettings().getStoredLocation(getClass().getName());
        if (p == null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((screenSize.width - getWidth()) / 4, (screenSize.height - getHeight()) / 3);
        }
        else
            setLocation(p);

        // Restore size
        Dimension size = gGo.getSettings().getStoredSize(getClass().getName());
        if (size != null)
            setSize(size);

        bozoHandler = new BozoHandler();

        playerTable = new PlayerTable();
        gamesTable = new GamesTable();
        if (gGo.getSettings().getIGSChatterType() == IGSChatter.IGSCHATTER_DESKTOPPANE)
            chatter = new ChatHandler(chatToggleButton);
        else if (gGo.getSettings().getIGSChatterType() == IGSChatter.IGSCHATTER_ONEFRAME)
            chatter = new IGSSingleChatter(chatToggleButton);
        else if (gGo.getSettings().getIGSChatterType() == IGSChatter.IGSCHATTER_SINGLEFRAMES)
            chatter = new IGSSingleChatter(chatToggleButton); // TODO
        shouter = new IGSShouter(shoutToggleButton);
        channels = new IGSChannels(channelsToggleButton);

        //{{{ If windows are closed, unselect button
        // Players
        playerTable.getCloseButton().addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    playersToggleButton.setSelected(false);
                }
            });
        playerTable.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    playersToggleButton.setSelected(false);
                }
            });

        // Games
        gamesTable.getCloseButton().addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    gamesToggleButton.setSelected(false);
                }
            });
        gamesTable.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    gamesToggleButton.setSelected(false);
                }
            });

        // Chatter
        chatter.getCloseButton().addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    chatToggleButton.setSelected(false);
                }
            });
        try {
            chatter.getCloseMenuItem().addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        chatToggleButton.setSelected(false);
                    }
                });
        } catch (NullPointerException e) {}
        chatter.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    chatToggleButton.setSelected(false);
                }
            });

        // Shouter
        shouter.getCloseButton().addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    shoutToggleButton.setSelected(false);
                }
            });
        shouter.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    shoutToggleButton.setSelected(false);
                }
            });

        // Channels
        channels.getCloseButton().addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    channelsToggleButton.setSelected(false);
                }
            });
        channels.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    channelsToggleButton.setSelected(false);
                }
            }); //}}}

        hostConfig = gGo.getSettings().getCurrentHostConfig();
        updateCaption();

        // Create an icon for the appliction
        Image icon = ImageHandler.loadImage("32blue.png");
        try {
            setIconImage(icon);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icon image.");
        }

        // Create disconnect timer
        disconnectTimer = new javax.swing.Timer(3000,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    disconnect();
                }
            });
        disconnectTimer.setRepeats(false);

        setVisible(true);
    } //}}}

    //{{{ initComponents() method
    /**  Init the GUI elements */
    private void initComponents() {
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    closeFrame();
                }
            });

        getContentPane().setLayout(new BorderLayout(3, 3));

        //{{{ menus
        JMenuBar menuBar = new JMenuBar();
        JMenu menu;
        JMenuItem menuItem;

        //{{{ Connection menu
        menu = new JMenu();
        MainFrame.setMnemonicText(menu, igs_resources.getString("Connection"));
        menuBar.add(menu);

        // Connection Connect
        connectMenuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/WebComponent16.gif")));
        MainFrame.setMnemonicText(connectMenuItem, igs_resources.getString("Connect"));
        connectMenuItem.setToolTipText(igs_resources.getString("connect_tooltip"));
        connectMenuItem.setActionCommand("Connect");
        connectMenuItem.addActionListener(igsActionListener);
        menu.add(connectMenuItem);

        // Connection Disconnect
        disconnectMenuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Stop16.gif")));
        MainFrame.setMnemonicText(disconnectMenuItem, igs_resources.getString("Disconnect"));
        disconnectMenuItem.setToolTipText(igs_resources.getString("disconnect_tooltip"));
        disconnectMenuItem.setActionCommand("Disconnect");
        disconnectMenuItem.addActionListener(igsActionListener);
        disconnectMenuItem.setEnabled(false);
        menu.add(disconnectMenuItem);

        // Connection configure
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Preferences16.gif")));
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("Configure"));
        menuItem.setToolTipText(igs_resources.getString("configure_tooltip"));
        menuItem.setActionCommand("Configure");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        // Connection Close
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("Close"));
        menuItem.setToolTipText(igs_resources.getString("close_mainwindow_tooltip"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
        menuItem.setActionCommand("Close");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        menu.addSeparator();

        // Connection Exit
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/exit.gif")));
        MainFrame.setMnemonicText(menuItem, gGo.getBoardResources().getString("Exit"));
        menuItem.setToolTipText(gGo.getBoardResources().getString("exit_tooltip"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        menuItem.setActionCommand("Exit");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);
        //}}}

        //{{{ Control menu
        menu = new JMenu();
        MainFrame.setMnemonicText(menu, igs_resources.getString("Control"));
        menuBar.add(menu);

        menuItem = new JMenuItem();
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("mystats"));
        menuItem.setToolTipText(igs_resources.getString("mystats_tooltip"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        menuItem.setActionCommand("MyStats");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem();
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("user_stats"));
        menuItem.setToolTipText(igs_resources.getString("user_stats_tooltip"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK));
        menuItem.setActionCommand("UserStats");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        menuItem = new JMenuItem();
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("read_messages"));
        menuItem.setToolTipText(igs_resources.getString("read_messages_tooltip"));
        menuItem.setActionCommand("ReadMessages");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        menu.addSeparator();

        toggleAway = new JCheckBoxMenuItem();
        MainFrame.setMnemonicText(toggleAway, igs_resources.getString("Away"));
        toggleAway.setToolTipText(igs_resources.getString("away_toggle_tooltip"));
        toggleAway.addItemListener(igsActionListener);
        toggleAway.setSelected(false);
        menu.add(toggleAway);

        menuItem = new JMenuItem();
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("Autoaway"));
        menuItem.setToolTipText(igs_resources.getString("autoaway_tooltip"));
        menuItem.setActionCommand("Autoaway");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem();
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("start_teach"));
        menuItem.setToolTipText(igs_resources.getString("start_teach_tooltip"));
        menuItem.setActionCommand("StartTeach");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);
        //}}}

        //{{{ Settings menu
        menu = new JMenu();
        MainFrame.setMnemonicText(menu, gGo.getBoardResources().getString("Settings"));
        menuBar.add(menu);

        // Settings Preferences
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Preferences16.gif")));
        MainFrame.setMnemonicText(menuItem, gGo.getBoardResources().getString("Preferences"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
        menuItem.setToolTipText(gGo.getBoardResources().getString("preferences_tooltip"));
        menuItem.setActionCommand("Preferences");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        // Settings Bozo management
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("user_management"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));
        menuItem.setToolTipText(igs_resources.getString("user_management_tooltip"));
        menuItem.setActionCommand("BozoList");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        // Settings Memory status
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        MainFrame.setMnemonicText(menuItem, gGo.getBoardResources().getString("Memory_status"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK));
        menuItem.setToolTipText(gGo.getBoardResources().getString("memory_status_tooltip"));
        menuItem.setActionCommand("MemoryStatus");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        // Settings Memory cleanup
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        MainFrame.setMnemonicText(menuItem, gGo.getBoardResources().getString("Memory_cleanup"));
        menuItem.setToolTipText(gGo.getBoardResources().getString("memory_cleanup_tooltip"));
        menuItem.setActionCommand("MemoryCleanup");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);
        //}}}

        //{{{ View menu
        menu = new JMenu();
        MainFrame.setMnemonicText(menu, gGo.getBoardResources().getString("View"));
        menuBar.add(menu);

        // View open local board
        menuItem = new JMenuItem();
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("Open_local_board"));
        menuItem.setToolTipText(igs_resources.getString("open_local_board_tooltip"));
        menuItem.setActionCommand("OpenLocalBoard");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        // View Clear
        menuItem = new JMenuItem();
        MainFrame.setMnemonicText(menuItem, igs_resources.getString("Clear_output"));
        menuItem.setToolTipText(igs_resources.getString("clear_output_tooltip"));
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    outputTextArea.setText("");
                }
            });
        menu.add(menuItem);

        // View Show toolbar
        viewShowToolbar = new JCheckBoxMenuItem(gGo.getBoardResources().getString("Show_toolbar"));
        viewShowToolbar.setToolTipText(gGo.getBoardResources().getString("Show_toolbar_tooltip"));
        viewShowToolbar.addItemListener(igsActionListener);
        viewShowToolbar.setSelected(true);
        menu.add(viewShowToolbar);

        // View Show shouts
        viewShowShout = new JCheckBoxMenuItem();
        MainFrame.setMnemonicText(viewShowShout, igs_resources.getString("Terminal_window_shouts"));
        viewShowShout.setToolTipText(igs_resources.getString("terminal_window_shouts_tooltip"));
        viewShowShout.addItemListener(igsActionListener);
        viewShowShout.setSelected(gGo.getSettings().getIGSshowShouts());
        menu.add(viewShowShout);

        // View Show autoupdate
        viewShowAutoupdate = new JCheckBoxMenuItem();
        MainFrame.setMnemonicText(viewShowAutoupdate, igs_resources.getString("Terminal_window_autoupdate"));
        viewShowAutoupdate.setToolTipText(igs_resources.getString("terminal_window_autoupdate_tooltip"));
        viewShowAutoupdate.addItemListener(igsActionListener);
        viewShowAutoupdate.setSelected(false);
        menu.add(viewShowAutoupdate);
        //}}}

        //{{{ Help menu
        menu = new JMenu();
        MainFrame.setMnemonicText(menu, gGo.getBoardResources().getString("Help"));
        menuBar.add(menu);

        // Help manual
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Help16.gif")));
        MainFrame.setMnemonicText(menuItem, gGo.getBoardResources().getString("Manual"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        menuItem.setToolTipText(gGo.getBoardResources().getString("manual_tooltip"));
        if (gGo.getHelpBroker() != null)
            menuItem.addActionListener(new CSH.DisplayHelpFromSource(gGo.getHelpBroker()));
        else
            menuItem.setEnabled(false);
        menu.add(menuItem);

        // Help webpage
        menuItem = new JMenuItem(gGo.getBoardResources().getString("ggo_webpage"), new ImageIcon(getClass().getResource("/images/WebComponent16.gif")));
        menuItem.setToolTipText(gGo.getBoardResources().getString("ggo_webpage_tooltip"));
        menuItem.setActionCommand("Webpage");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        // Help about
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/About16.gif")));
        MainFrame.setMnemonicText(menuItem, gGo.getBoardResources().getString("About"));
        menuItem.setToolTipText(gGo.getBoardResources().getString("about_tooltip"));
        menuItem.setActionCommand("About");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        // Help update
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Information16.gif")));
        MainFrame.setMnemonicText(menuItem, gGo.getBoardResources().getString("check_update"));
        menuItem.setToolTipText(gGo.getBoardResources().getString("check_update_tooltip"));
        menuItem.setActionCommand("CheckUpdate");
        menuItem.addActionListener(igsActionListener);
        menu.add(menuItem);

        setJMenuBar(menuBar);

        //}}}

        //}}}

        //{{{ toolbar
        toolBar = new JToolBar();
        toolBar.setFloatable(true);

        // Connect button
        connectButton = new JButton();
        connectButton.setIcon(new ImageIcon(getClass().getResource("/images/WebComponent24.gif")));
        connectButton.setToolTipText(igs_resources.getString("connect_tooltip"));
        connectButton.setBorderPainted(false);
        connectButton.setActionCommand("Connect");
        connectButton.addActionListener(igsActionListener);
        toolBar.add(connectButton);

        // Disconnect button
        disconnectButton = new JButton();
        disconnectButton.setIcon(new ImageIcon(getClass().getResource("/images/Stop24.gif")));
        disconnectButton.setToolTipText(igs_resources.getString("disconnect_tooltip"));
        disconnectButton.setBorderPainted(false);
        disconnectButton.setActionCommand("Disconnect");
        disconnectButton.addActionListener(igsActionListener);
        disconnectButton.setEnabled(false);
        toolBar.add(disconnectButton);

        JPanel infoPanel = new JPanel();

        // Players button
        playersToggleButton = new JToggleButton(igs_resources.getString("Players"));
        playersToggleButton.setToolTipText(igs_resources.getString("players_tooltip"));
        playersToggleButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    playersToggleButtonActionPerformed();
                }
            });
        infoPanel.add(playersToggleButton);

        // Games button
        gamesToggleButton = new JToggleButton(igs_resources.getString("Games"));
        gamesToggleButton.setToolTipText(igs_resources.getString("games_tooltip"));
        gamesToggleButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    gamesToggleButtonActionPerformed();
                }
            });
        infoPanel.add(gamesToggleButton);

        // Chat button
        chatToggleButton = new JToggleButton(igs_resources.getString("Chat"));
        chatToggleButton.setToolTipText(igs_resources.getString("chat_tooltip"));
        chatToggleButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    chatToggleButtonActionPerformed();
                }
            });
        infoPanel.add(chatToggleButton);

        // Channels button
        channelsToggleButton = new JToggleButton(igs_resources.getString("Channels"));
        channelsToggleButton.setToolTipText(igs_resources.getString("channels_tooltip"));
        channelsToggleButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    channelsToggleButtonActionPerformed();
                }
            });
        infoPanel.add(channelsToggleButton);

        // Shout button
        shoutToggleButton = new JToggleButton(igs_resources.getString("Shouts"));
        shoutToggleButton.setToolTipText(igs_resources.getString("shouts_tooltip"));
        shoutToggleButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    shoutToggleButtonActionPerformed();
                }
            });
        infoPanel.add(shoutToggleButton);

        toolBar.add(infoPanel);

        // Status buttons
        ButtonGroup gameStatusButtonGroup = new ButtonGroup();
        JPanel gameStatusPanel = new JPanel();

        // X button
        JToggleButton gameStatusToggleButton1 = new JToggleButton("X");
        gameStatusToggleButton1.setToolTipText(igs_resources.getString("X_button_tooltip"));
        gameStatusButtonGroup.add(gameStatusToggleButton1);
        gameStatusToggleButton1.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        igsConnection.sendCommand("toggle open off");
                        igsConnection.sendCommand("toggle looking off");
                    } catch (NullPointerException e) {
                        System.err.println("No connection.");
                    }
                }
            });
        gameStatusPanel.add(gameStatusToggleButton1);

        // O button
        JToggleButton gameStatusToggleButton2 = new JToggleButton("O");
        gameStatusToggleButton2.setToolTipText(igs_resources.getString("O_button_tooltip"));
        gameStatusButtonGroup.add(gameStatusToggleButton2);
        gameStatusToggleButton2.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        igsConnection.sendCommand("toggle open on");
                        igsConnection.sendCommand("toggle looking off");
                    } catch (NullPointerException e) {
                        System.err.println("No connection.");
                    }
                }
            });
        gameStatusPanel.add(gameStatusToggleButton2);

        // ! button
        JToggleButton gameStatusToggleButton3 = new JToggleButton("!");
        gameStatusToggleButton3.setToolTipText(igs_resources.getString("I_button_tooltip"));
        gameStatusButtonGroup.add(gameStatusToggleButton3);
        gameStatusToggleButton3.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        igsConnection.sendCommand("toggle open on");
                        igsConnection.sendCommand("toggle looking on");
                    } catch (NullPointerException e) {
                        System.err.println("No connection.");
                    }
                }
            });
        gameStatusPanel.add(gameStatusToggleButton3);
        toolBar.add(gameStatusPanel);
        getContentPane().add(toolBar, BorderLayout.NORTH);
        //}}}

        // Output field
        Font terminalFont = new Font("Monospaced", 0, gGo.getSettings().getMonospacedFontSize());
        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        outputTextArea.setFont(terminalFont);

        outputScrollPane = new JScrollPane();
        outputScrollPane.setPreferredSize(new Dimension(30 + 80 * getFontMetrics(terminalFont).charWidth('A'), 450));
        outputScrollPane.setViewportView(outputTextArea);
        getContentPane().add(outputScrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        // History button
        historyButton = new JButton();
        historyButton.setIcon(new ImageIcon(getClass().getResource("/images/1rightarrow.gif")));
        historyButton.setToolTipText(igs_resources.getString("history_button_tooltip"));
        historyButton.setPreferredSize(new Dimension(24, 26));
        historyButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    historyButtonActionPerformed();
                }
            });
        bottomPanel.add(historyButton, BorderLayout.WEST);

        // Input field
        inputTextField = new JTextField();
        inputTextField.setFont(new Font("Sans Serif", 0, gGo.getSettings().getSansSerifFontSize()));
        inputTextField.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    inputTextFieldActionPerformed(evt);
                }
            });
        bottomPanel.add(inputTextField, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        pack();
    } //}}}

    //{{{ updateCaption() method
    /**  Set frame title */
    private void updateCaption() {
        setTitle(MessageFormat.format(igs_resources.getString("mainwindow_title"), new Object[]{hostConfig.getName()}));
    } //}}}

    //{{{ isConnected() method
    /**
     *  Check if currently a connection to a server exists
     *
     *@return    True if connected, else false
     */
    public boolean isConnected() {
        return igsConnection != null;
    } //}}}

    //{{{ getHostConfig() method
    /**
     *  Gets the hostConfig attribute of the IGSMainWindow class
     *
     *@return    The hostConfig value
     */
    public static HostConfig getHostConfig() {
        return hostConfig;
    } //}}}

    //{{{ getIGSConnection() method
    /**
     *  Gets the iGSConnection attribute of the IGSMainWindow class
     *
     *@return    The iGSConnection value
     */
    public static IGSConnection getIGSConnection() {
        return igsConnection;
    } //}}}

    //{{{ gamesToggleButtonActionPerformed() method
    /**  Open the games table window */
    private void gamesToggleButtonActionPerformed() {
        try {
            if (gamesToggleButton.isSelected()) {
                gamesTable.setVisible(true);
                gamesTable.doRefresh();
            }
            else
                gamesTable.setVisible(false);
        } catch (NullPointerException e) {
            System.err.println("Failed to open gamestable: " + e);
        }
    } //}}}

    //{{{ chatToggleButtonActionPerformed() method
    /**  Open the chat window */
    private void chatToggleButtonActionPerformed() {
        try {
            if (chatToggleButton.isSelected()) {
                chatter.setVisible(true);
                chatter.setInputFocus();
            }
            else
                chatter.setVisible(false);
        } catch (NullPointerException e) {
            System.err.println("Failed to open chatter: " + e);
        }
    } //}}}

    //{{{ shoutToggleButtonActionPerformed() method
    /**  Open the shout window */
    private void shoutToggleButtonActionPerformed() {
        try {
            if (shoutToggleButton.isSelected()) {
                shouter.setVisible(true);
                shouter.setInputFocus();
            }
            else
                shouter.setVisible(false);
        } catch (NullPointerException e) {
            System.err.println("Failed to open shouter: " + e);
        }
    } //}}}

    //{{{ channelsToggleButtonActionPerformed() method
    /**  Open the shout window */
    private void channelsToggleButtonActionPerformed() {
        try {
            if (channelsToggleButton.isSelected()) {
                channels.setVisible(true);
                channels.setInputFocus();
            }
            else
                channels.setVisible(false);
        } catch (NullPointerException e) {
            System.err.println("Failed to open channels: " + e);
        }
    } //}}}

    //{{{ playersToggleButtonActionPerformed() method
    /**  Open the player table window */
    private void playersToggleButtonActionPerformed() {
        try {
            if (playersToggleButton.isSelected()) {
                playerTable.setVisible(true);
                playerTable.doRefresh();
            }
            else
                playerTable.setVisible(false);
        } catch (NullPointerException e) {
            System.err.println("Failed to open playertable: " + e);
        }
    } //}}}

    //{{{ historyButtonActionPerformed() method
    /**  History button was clicked */
    private void historyButtonActionPerformed() {
        if (historyScroller.getHistoryCounter() == 0)
            return;

        popup = new JPopupMenu();

        for (int i = historyScroller.getHistoryCounter() <= 10 ? 0 : historyScroller.getHistoryCounter() - 10;
                i < historyScroller.getHistoryCounter(); i++) {
            final String cmd = (String)historyScroller.getHistoryItem(i);
            JMenuItem menuItem = new JMenuItem(cmd);
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        IGSConnection.sendCommand(cmd);
                    }
                });
            popup.add(menuItem);
        }
        popup.show(historyButton, 3, 3);
    } //}}}

    //{{{ inputTextFieldActionPerformed() method
    /**
     *  Input was done in the input text field
     *
     *@param  evt  ActionEvent
     */
    private void inputTextFieldActionPerformed(ActionEvent evt) {
        String command = inputTextField.getText();

        if (command.length() == 0)
            return;

        appendOutput("# " + command);
        inputTextField.setText("");

        try {
            igsConnection.recieveInput(command);
        } catch (NullPointerException e) {
            System.err.println("No connection.");
        }
    } //}}}

    //{{{ connectButtonActionPerformed() method
    /**  Connect to server */
    private void connectButtonActionPerformed() {
        if (igsConnection == null) {
            igsConnection = new IGSConnection(outputTextArea, playerTable, gamesTable, chatter, shouter, channels, this);
            if (!igsConnection.connectIGS(hostConfig)) {
                appendOutput(igs_resources.getString("failed_connect_error"));
                igsConnection = null;
                return;
            }
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            connectMenuItem.setEnabled(false);
            disconnectMenuItem.setEnabled(true);
            disconnectTimer.stop();

            setInputFocus();
        }
        else
            System.err.println("Already connected.");
    } //}}}

    //{{{ disconnectButtonActionPerformed() method
    /**  Disconnect from server */
    private void disconnectButtonActionPerformed() {
        igsConnection.sendCommand("exit");
        blockInfo = true;

        // Force disconnect if no response on exit command after 3 seconds
        disconnectTimer.stop();
        disconnectTimer.start();
    } //}}}

    //{{{ disconnect() method
    /**  Disconnect from host, shut down connection classes and adjust toolbar */
    public void disconnect() {
        if (igsConnection != null) {
            igsConnection.closeAll();
            igsConnection = null;
            appendOutput(igs_resources.getString("disconnect_message"));
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            connectMenuItem.setEnabled(true);
            disconnectMenuItem.setEnabled(false);
            IGSConnection.getAutoUpdater().clearHashs();
            IGSConnection.getGameHandler().notifyDisconnect();
            if (!blockInfo)
                displayInfo(igs_resources.getString("disconnect_info"));
        }
        blockInfo = false;
    } //}}}

    //{{{ appendOutput() method
    /**
     *  Append text to the output textarea, and scroll it
     *
     *@param  str  Text to append
     */
    public void appendOutput(String str) {
        try {
            outputTextArea.append(str + "\n");
        } catch (Error e) {
            // This is extremely ugly, but I don't know why sometimes an error is thrown here.
            // Trying to append text while scrolling?
            System.err.println("Failed to append text to outputTextArea: " + e);
        }

        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        outputTextArea.scrollRectToVisible(outputTextArea.modelToView(
                                outputTextArea.getDocument().getLength()));
                    } catch (BadLocationException e) {
                        System.err.println("Failed to scroll: " + e);
                    }
                }
            });
    } //}}}

    //{{{ closeFrame() method
    /**  Description of the Method */
    private void closeFrame() {
        if (isConnected()) {
            if (JOptionPane.showConfirmDialog(
                    this,
                    igs_resources.getString("confirm_close_server"),
                    PACKAGE,
                    JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                return;
        }
        saveSubframeLocations();
        playerTable.setVisible(false);
        playerTable.dispose();
        gamesTable.setVisible(false);
        gamesTable.dispose();
        chatter.setVisible(false);
        chatter.dispose();
        shouter.setVisible(false);
        shouter.dispose();
        channels.setVisible(false);
        channels.dispose();
        if (igsConnection != null) {
            blockInfo = true;
            igsConnection.closeAll();
            igsConnection = null;
        }
        IGSConnection.getGameHandler().clear();
        IGSConnection.getGameObserver().clear();
        IGSConnection.getAutoUpdater().finishAutoupdate();
        setVisible(false);
        dispose();

        try {
            gGo.closeIGSWindow();
            if (gGo.getNumberOfOpenFrames() == 0 && !gGo.hasStartUpFrame())
                gGo.exitApp(0);
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ openMatchDialog() method
    /**
     *  Open a match dialog, either when recieving or sending a match request
     *
     *@param  player1      Description of the Parameter
     *@param  player2      Description of the Parameter
     *@param  size         Description of the Parameter
     *@param  mainTime     Description of the Parameter
     *@param  byoyomiTime  Description of the Parameter
     *@param  challenger   Description of the Parameter
     *@param  requesting   Description of the Parameter
     *@param  byoStones    Description of the Parameter
     *@param  isAutomatch  Description of the Parameter
     */
    public void openMatchDialog(boolean requesting, String player1, String player2, int size,
            int mainTime, int byoyomiTime, int byoStones, String challenger, boolean isAutomatch) {
        new IGSMatcher(requesting, player1, player2, size, mainTime, byoyomiTime, byoStones, challenger, isAutomatch);

        // Play a sound for incoming requests and if the chat sound is enabled
        if (!requesting && gGo.getSettings().getPlayChatSound())
            SoundHandler.playMatchRequestSound();
    } //}}}

    //{{{ displayInfo() method
    /**
     *  Display an information messagebox
     *
     *@param  txt  String to display
     */
    public void displayInfo(final String txt) {
        final SwingWorker worker =
            new SwingWorker() {
                public Object construct() {
                    JOptionPane.showMessageDialog(null, txt,
                            igs_resources.getString("Information"), JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
            };
        worker.start();
    } //}}}

    //{{{ main() method
    /**
     *  Main method. For debugging.
     *
     *@param  args  Description of the Parameter
     */
    public static void main(String args[]) {
        gGo.openIGSWindow();
    } //}}}

    //{{{ changeLookAndFeel() method
    /**
     *  Change the look and feel
     *
     *@param  oldlnf    Description of the Parameter
     *@param  oldTheme  Description of the Parameter
     */
    protected void changeLookAndFeel(int oldlnf, String oldTheme) {
        String lnfName = gGo.convertLookAndFeel(gGo.getSettings().getLookAndFeel());

        System.err.println("Trying to set LookAndFeel to: " + lnfName);

        try {
            if (gGo.getSettings().getLookAndFeel() == LOOKANDFEEL_SKIN) {
                if (!gGo.setTheme(gGo.getSettings().getThemePack()))
                    return;
            }

            if (oldlnf != gGo.getSettings().getLookAndFeel()) {
                // Apply own theme to Metal look and feel
                if (lnfName.equals("javax.swing.plaf.metal.MetalLookAndFeel"))
                    MetalLookAndFeel.setCurrentTheme(new gGoMetalTheme());
                LookAndFeel lf = (LookAndFeel)(gGo.class.getClassLoader().loadClass(lnfName).newInstance());
                UIManager.setLookAndFeel(lf);
                UIManager.getLookAndFeelDefaults().put("ClassLoader", gGo.class.getClassLoader());
            }

            // Quick and ugly... TODO
            if (UIManager.getLookAndFeel().getClass().getName().equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel") &&
                    gGo.getSettings().getFixWindowsFont()) {
                java.awt.Font f = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12);
                java.util.Enumeration keys = UIManager.getDefaults().keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    Object value = UIManager.get(key);
                    if (value instanceof javax.swing.plaf.FontUIResource)
                        UIManager.put(key, f);
                }
                System.err.println("Japanese locale with Windows look and feel detected, Windows fonts adjusted");
            }

            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        SwingUtilities.updateComponentTreeUI(IGSMainWindow.this);
                        IGSMainWindow.this.pack();
                        chatter.updateLookAndFeel();
                        SwingUtilities.updateComponentTreeUI(shouter);
                        shouter.pack();
                        SwingUtilities.updateComponentTreeUI(channels);
                        channels.pack();
                        SwingUtilities.updateComponentTreeUI(playerTable);
                        playerTable.pack();
                        SwingUtilities.updateComponentTreeUI(gamesTable);
                        gamesTable.pack();
                    }
                });
        } catch (Exception e) {
            System.err.println("Failed to set LookAndFeel '" + lnfName + "': " + e);
            gGo.getSettings().setLookAndFeel(oldlnf);
        }
    } //}}}

    //{{{ changeFontSize() method
    /**  Change font sizes in this window and all subwindows */
    public void changeFontSize() {
        chatter.changeFontSize();
        shouter.changeFontSize();
        channels.changeFontSize();
        inputTextField.setFont(new Font("Sans Serif", 0, gGo.getSettings().getSansSerifFontSize()));

        Font terminalFont = new Font("Monospaced", 0, gGo.getSettings().getMonospacedFontSize());
        outputTextArea.setFont(terminalFont);
        outputScrollPane.setPreferredSize(new Dimension(30 + 80 * getFontMetrics(terminalFont).charWidth('A'), 450));
        pack();
    } //}}}

    //{{{ checkVisible() method
    /**  Raise and bring this window to front */
    public void checkVisible() {
        if (getState() != Frame.NORMAL)
            setState(Frame.NORMAL);
        toFront();
        setInputFocus();
    } //}}}

    //{{{ setInputFocus() method
    /**  Request focus for the input text field */
    public void setInputFocus() {
        // --- 1.3 ---
        if (!gGo.is13())
            inputTextField.requestFocusInWindow();
        else
            inputTextField.requestFocus();
    } //}}}

    //{{{ saveSubframeLocations() method
    /**  Save location of subframes */
    private void saveSubframeLocations() {
        if (gGo.getSettings().getStoreLocation()) {
            gGo.getSettings().setStoredLocation(getClass().getName(), getLocation());
            gGo.getSettings().setStoredLocation(playerTable.getClass().getName(), playerTable.getLocation());
            gGo.getSettings().setStoredLocation(gamesTable.getClass().getName(), gamesTable.getLocation());
            gGo.getSettings().setStoredLocation(chatter.getClass().getName(), chatter.getLocation());
            gGo.getSettings().setStoredLocation(shouter.getClass().getName(), shouter.getLocation());
            gGo.getSettings().setStoredLocation(channels.getClass().getName(), channels.getLocation());
        }
        if (gGo.getSettings().getStoreSize()) {
            gGo.getSettings().setStoredSize(getClass().getName(), getSize());
            gGo.getSettings().setStoredSize(playerTable.getClass().getName(), playerTable.getSize());
            gGo.getSettings().setStoredSize(gamesTable.getClass().getName(), gamesTable.getSize());
            gGo.getSettings().setStoredSize(chatter.getClass().getName(), chatter.getSize());
            gGo.getSettings().setStoredSize(shouter.getClass().getName(), shouter.getSize());
            gGo.getSettings().setStoredSize(channels.getClass().getName(), channels.getSize());
        }
        gGo.getSettings().saveSettings();
    } //}}}

    //{{{ getGamesToggleButton() method
    /**
     *  Gets the gamesToggleButton attribute of the IGSMainWindow object
     *
     *@return    The gamesToggleButton value
     */
    public JToggleButton getGamesToggleButton() {
        return gamesToggleButton;
    } //}}}

    //{{{ getPlayersToggleButton() method
    /**
     *  Gets the playersToggleButton attribute of the IGSMainWindow object
     *
     *@return    The playersToggleButton value
     */
    public JToggleButton getPlayersToggleButton() {
        return playersToggleButton;
    } //}}}

    //{{{ getViewAutoupdate() method
    /**
     *  Gets the viewAutoupdate attribute of the IGSMainWindow object
     *
     *@return    The viewAutoupdate value
     */
    public boolean getViewAutoupdate() {
        return viewShowAutoupdate.isSelected();
    } //}}}

    //{{{ getBozoHandler() method
    /**
     *  Gets the bozoHandler attribute of the IGSMainWindow class
     *
     *@return    The bozoHandler value
     */
    public BozoHandler getBozoHandler() {
        return bozoHandler;
    } //}}}

    //{{{ updateBozoListDialog() method
    /**  Update the bozo list fields */
    public void updateBozoListDialog() {
        if (bozoListDialog != null)
            bozoListDialog.updateLists();
    } //}}}

    //{{{ local class IGSActionListener
    /**
     *  Local ActionListener class.
     *
     *@author     Peter Strempel
     *@version    $Revision: 1.33 $, $Date: 2002/10/26 00:49:18 $
     */
    class IGSActionListener implements ActionListener, ItemListener {
        //{{{ actionPerformed() method
        /**
         *  ActionListener method
         *
         *@param  e  ActionEvent
         */
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();

            //{{{ Connection Connect
            if (cmd.equals("Connect")) {
                connectButtonActionPerformed();
            } //}}}
            //{{{ Connection Disconnect
            else if (cmd.equals("Disconnect")) {
                disconnectButtonActionPerformed();
            } //}}}
            //{{{ Connection Configure
            else if (cmd.equals("Configure")) {
                new IGSHostDialog(IGSMainWindow.this, true, hostConfig.getID());
                hostConfig = gGo.getSettings().getCurrentHostConfig();
                updateCaption();
            } //}}}
            //{{{ Connection Close
            else if (cmd.equals("Close")) {
                closeFrame();
            } //}}}
            //{{{ Connection Exit
            else if (cmd.equals("Exit")) {
                if (isConnected()) {
                    if (JOptionPane.showConfirmDialog(
                            IGSMainWindow.this,
                            igs_resources.getString("confirm_close_server"),
                            PACKAGE,
                            JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                        return;
                }

                if (gGo.hasModifiedBoards()) {
                    if (JOptionPane.showConfirmDialog(
                            IGSMainWindow.this,
                            gGo.getBoardResources().getString("warn_exit"),
                            PACKAGE,
                            JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                        return;
                }

                if (igsConnection != null) {
                    blockInfo = true;
                    igsConnection.closeAll();
                }
                saveSubframeLocations();
                setVisible(false);
                dispose();
                gGo.exitApp(0);
            } //}}}
            //{{{ Control My Stats
            else if (cmd.equals("MyStats")) {
                IGSConnection.sendCommand("stats " + IGSConnection.getLoginName());
            } //}}}
            //{{{ Control User Stats
            else if (cmd.equals("UserStats")) {
                String s = JOptionPane.showInputDialog(
                        IGSMainWindow.this,
                        gGo.getIGSResources().getString("user_stats_message"));
                if (s != null && s.length() > 0)
                    IGSConnection.sendCommand("stats " + s);
            } //}}}
            //{{{ Control Autoaway
            else if (cmd.equals("Autoaway")) {
                new AutoawayDialog(IGSMainWindow.this, false);
            } //}}}
            //{{{ Control Read messages
            else if (cmd.equals("ReadMessages")) {
                IGSReader.requestSilentMessages = false;
                IGSConnection.sendCommand("message");
            } //}}}
            //{{{ Control Teach
            else if (cmd.equals("StartTeach")) {
                String s = JOptionPane.showInputDialog(
                        IGSMainWindow.this,
                        gGo.getIGSResources().getString("start_teach_input"));
                if (s != null && s.length() > 0)
                    igsConnection.recieveInput("teach " + s);
            } //}}}
            //{{{ Settings Preferences
            else if (cmd.equals("Preferences")) {
                int oldlnf = gGo.getSettings().getLookAndFeel();
                String oldTheme = gGo.getSettings().getThemePack();
                PreferencesDialog dlg = new PreferencesDialog(IGSMainWindow.this, true, gGo.getSettings());
                if (dlg.getResult()) {
                    if (oldlnf != gGo.getSettings().getLookAndFeel() ||
                            !oldTheme.equals(gGo.getSettings().getThemePack()))
                        changeLookAndFeel(oldlnf, oldTheme);
                    changeFontSize();
                    gGo.getSettings().saveSettings();
                }
            } //}}}
            //{{{ Settings Memory
            else if (cmd.equals("MemoryStatus"))
                JOptionPane.showMessageDialog(IGSMainWindow.this,
                        gGo.getBoardResources().getString("Memory_message") + ": " + gGo.getMemoryStatus());
            else if (cmd.equals("MemoryCleanup"))
                JOptionPane.showMessageDialog(IGSMainWindow.this,
                        gGo.getBoardResources().getString("memory_released") + ": " +
                        gGo.forceGarbageCollection() + gGo.getBoardResources().getString("KB"));
            //}}}
            //{{{ Settings Bozo management
            else if (cmd.equals("BozoList")) {
                if (bozoListDialog == null)
                    bozoListDialog = new BozoListDialog(IGSMainWindow.this, false, bozoHandler);
                bozoListDialog.updateLists();
                bozoListDialog.setVisible(true);
            } //}}}
            //{{{ View Open local board
            else if (cmd.equals("OpenLocalBoard")) {
                gGo.openNewMainFrame();
            } //}}}
            //{{{ Help About
            else if (cmd.equals("About")) {
                gGo.showAbout(IGSMainWindow.this);
            } //}}}
            //{{{ Help Update
            else if (cmd.equals("CheckUpdate")) {
                new UpdateChecker(IGSMainWindow.this).start();
            } //}}}
            //{{{ Help Webpage
            else if (cmd.equals("Webpage")) {
                BrowserControl.displayURL(GGO_URL);
            } //}}}
        } //}}}

        //{{{ itemStateChanged() method
        /**
         *  ItemListener method
         *
         *@param  e  ItemEvent
         */
        public void itemStateChanged(ItemEvent e) {
            Object source = e.getItemSelectable();

            if (source == viewShowShout) {
                gGo.getSettings().setIGSshowShouts(e.getStateChange() == ItemEvent.SELECTED);
                gGo.getSettings().saveSettings();
            }
            else if (source == viewShowToolbar && toolBar != null)
                toolBar.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            else if (source == toggleAway && igsConnection != null)
                IGSConnection.setAway(e.getStateChange() == ItemEvent.SELECTED);
        } //}}}

    } //}}}
}

