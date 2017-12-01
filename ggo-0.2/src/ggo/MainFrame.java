/*
 *  MainFrame.java
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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.plaf.metal.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.text.MessageFormat;
import ggo.*;
import ggo.dialogs.*;
import ggo.gui.*;
import ggo.utils.*;
import javax.help.*;

/**
 * Main Frame, responsible for the GUI elements.
 * Every MainFrame has to run in its own thread. This class implements Runnable. To open
 * a new frame, the static method gGo.openNewMainFrame() should be used.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.21 $, $Date: 2002/10/23 01:50:49 $
 *@see        gGo#openNewMainFrame()
 */
public class MainFrame extends JFrame implements Defines, MenuDefines, Runnable {
    //{{{ protected members
    /**  Pointer to the board */
    protected Board board;
    /**  Pointer to the sidebar */
    protected SideBar sideBar;
    /**  Pointer to the comment field */
    protected JTextArea commentEdit;
    /**  pointer to the splitpane */
    protected JSplitPane splitPane;
    /**  Pointer to the toolbar */
    protected JToolBar toolBar;
    /**  Pointers to menu checkbox items */
    protected JCheckBoxMenuItem viewHorizontalComment, viewStatusBar, viewCoordinates,
            viewSidebar, viewToolBar, viewShowVariations, viewSlider, viewCursor;
    /**  Autoplay button */
    protected JToggleButton autoplayButton;
    /**  Pointer to the status bar */
    protected StatusBar statusBar;
    /**  Pointer to the listener class */
    protected MainFrameListener mainFrameListener;
    /**  Description of the Field */
    protected int oldlnf;
    /**  Description of the Field */
    protected String oldTheme;
    /**  Action objects */
    protected Action fileNewAction, fileOpenAction, fileSaveAction, fileSaveAsAction,
            editDeleteAction, settingsGameinfoAction,
            navMainBranchAction, navVarStartAction, navNextBranchAction;
    /**  Pointer to the slider */
    protected JSlider slider;
    /**  Pointer to the slider max label */
    protected JLabel sliderMaxLabel;
    /**  Pointer to the slider panel */
    protected JPanel sliderPanel;
    /**  Flag used controling the slider listener */
    protected boolean dontFireSlider = false;
    /**  Flag if this is a frame for playing. */
    protected boolean isPlayingFrame = false;
    /** Autoplay timer */
    protected javax.swing.Timer autoplayTimer;
    /** Autoplay delay ButtonGroup */
    protected ButtonGroup autoplayButtonGroup;
    /** Autoplay delays */
    protected final static int autoplayDelays[] = {500, 1000, 2000, 5000};
    /** Temporary storage for the last directory */
    protected String tmpRemDir = null;
    /** Flag for Thread control */
    protected boolean runMe = false;
    /** Counter for frame location offset */
    protected static int frameCounter = 0;
    /** Board resource bundle */
    protected static ResourceBundle board_resources;
    /** Mirror edit board and pointer to the synchronized board */
    protected MainFrame mirrorFrame, synchFrame;
    /** Pointer to the ClipImporter */
    protected ClipImporter clipImporter;
    //}}}

    //{{{ static constructor
    static {
        board_resources = gGo.getBoardResources();
    } //}}}

    //{{{ MainFrame Constructors
    /**
     *  Constructor for the MainFrame object
     *
     *@param  synchFrame  Frame this board is synchronized to
     */
    public MainFrame(MainFrame synchFrame) {
        super(PACKAGE + " " + VERSION);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        this.synchFrame = synchFrame;

        mainFrameListener = new MainFrameListener();

        initActions();
        initComponents();
        initMenus();
        pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
                (screenSize.width - getWidth()) / 2 + frameCounter * 40,
                (screenSize.height - getHeight()) / 2 + frameCounter * 20);
        frameCounter++;

        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    if (closeFrame() && gGo.getNumberOfOpenFrames() == 0 &&
                            !gGo.hasStartUpFrame() &&
                            (MainFrame.this.getClass().getName().equals("ggo.MainFrame") ||
                            MainFrame.this.getClass().getName().equals("ggo.gtp.GTPMainFrame")))
                        gGo.exitApp(0);
                }
            });

        // Create an icon for the appliction
        Image icon = ImageHandler.loadImage("32navy.png");
        try {
            setIconImage(icon);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icon image.");
        }

        if (toolBar != null)
            toolBar.setVisible(gGo.getSettings().getShowToolbar());
        sideBar.setVisible(gGo.getSettings().getShowSidebar());
        sliderPanel.setVisible(gGo.getSettings().getShowSlider());
        statusBar.setVisible(gGo.getSettings().getShowStatusbar());

        updateCaption();
        setVisible(true);

        clipImporter = null;
    }

    /**Constructor for the MainFrame object. This MainFrame is not synchronized. */
    public MainFrame() {
        this(null);
    } //}}}

    //{{{ run() method
    /**
     *  Main processing method for the MainFrame object. Every MainFrame has to run in its own thread.
     */
    public void run() {
        runMe = true;
        while (runMe && !Thread.interrupted()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                runMe = false;
            }
        }
    } //}}}

    //{{{ initComponents() method
    /**  Create the GUI elements */
    private void initComponents() {
        // Create the toolbar
        initToolBar();

        // Create the board, slider and sidebar
        JPanel boardSidebarPanel = new JPanel();
        JPanel boardPanel = new JPanel();
        sliderPanel = new JPanel();
        boardSidebarPanel.setLayout(new BorderLayout());
        boardPanel.setLayout(new BorderLayout());
        sliderPanel.setLayout(new BorderLayout());
        sliderPanel.setBorder(BorderFactory.createEtchedBorder());
        board = new Board(this,
                getClass().getName().equals("ggo.MainFrame") ||
                getClass().getName().equals("ggo.gtp.GTPMainFrame") ||
                getClass().getName().equals("ggo.igs.gui.IGSPlayingFrame") ||
                getClass().getName().equals("ggo.igs.gui.IGSTeachingFrame"));
        board.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        sideBar = new SideBar(board, getClass().getName());
        sideBar.setPreferredSize(new Dimension(140, board.getPreferredSize().height));
        // --- 1.3 ---
        // slider = new JSlider(0, 0);
        slider = new JSlider();
        slider.setValue(0);
        slider.setMinimum(0);
        slider.setMaximum(0);
        slider.addChangeListener(mainFrameListener);
        slider.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        JLabel sliderMinLabel = new JLabel("0");
        sliderMaxLabel = new JLabel("0");
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(sliderMaxLabel, BorderLayout.EAST);
        sliderPanel.add(sliderMinLabel, BorderLayout.WEST);
        boardPanel.add(board, BorderLayout.CENTER);
        boardPanel.add(sliderPanel, BorderLayout.SOUTH);
        boardSidebarPanel.add(boardPanel, BorderLayout.CENTER);
        boardSidebarPanel.add(sideBar,
                gGo.getSettings().getSidebarLayout() == SIDEBAR_WEST ? BorderLayout.WEST : BorderLayout.EAST);

        // Create the Comment Edit field
        commentEdit = new JTextArea();
        commentEdit.setLineWrap(true);
        commentEdit.setWrapStyleWord(true);
        commentEdit.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        commentEdit.setFont(new Font("Serif", 0, gGo.getSettings().getSerifFontSize()));
        Component commentField = initCommentEdit();

        // Create the SplitPane
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, boardSidebarPanel, commentField);
        splitPane.setOneTouchExpandable(true);
        splitPane.resetToPreferredSizes();

        // Create the StatusBar
        statusBar = new StatusBar(board.getPlayMode() == PLAY_MODE_EDIT);

        // Create main content panel
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        if (toolBar != null)
            contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(splitPane, BorderLayout.CENTER);
        contentPane.add(statusBar, BorderLayout.SOUTH);
        setContentPane(contentPane);
    } //}}}

    //{{{ initCommentEdit() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    protected JComponent initCommentEdit() {
        JScrollPane commentScrollPane = new JScrollPane(commentEdit);
        commentScrollPane.setPreferredSize(new Dimension(100, 60));
        commentScrollPane.setMinimumSize(new Dimension(0, 0));
        commentEdit.getDocument().addDocumentListener(mainFrameListener);
        return commentScrollPane;
    } //}}}

    //{{{ initActions() method
    /**  Init the actions */
    protected void initActions() {
        //{{{ fileNewAction
        fileNewAction =
            new AbstractAction(board_resources.getString("New"), new ImageIcon(getClass().getResource("/images/New16.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    newGame();
                }
            }; //}}}

        //{{{ fileOpenAction
        fileOpenAction =
            new AbstractAction(board_resources.getString("Open"), new ImageIcon(getClass().getResource("/images/Open16.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    if (checkModified()) {
                        final JFileChooser fc = new JFileChooser(gGo.getSettings().getRemDir());
                        // Looks strange, but the SGF filter should be first and selected
                        SGFFileFilter sgf_filter = new SGFFileFilter();
                        fc.addChoosableFileFilter(sgf_filter);
                        fc.addChoosableFileFilter(new XMLFileFilter());
                        fc.addChoosableFileFilter(new UGFFileFilter());
                        fc.addChoosableFileFilter(new gGoFileFilter());
                        fc.setFileFilter(sgf_filter);
                        if (fc.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                            openSGF(fc.getSelectedFile().getAbsolutePath(), true);
                            tmpRemDir = fc.getSelectedFile().getParent(); // Ugly, but there is no return value from openSGF
                        }
                    }
                }
            }; //}}}

        //{{{ fileSaveAction
        fileSaveAction =
            new AbstractAction(board_resources.getString("Save"), new ImageIcon(getClass().getResource("/images/Save16.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    saveSGF();
                }
            }; //}}}

        //{{{ fileSaveAsAction
        fileSaveAsAction =
            new AbstractAction(board_resources.getString("SaveAs"), new ImageIcon(getClass().getResource("/images/SaveAs16.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    doSave();
                }
            }; //}}}

        //{{{ navMainBranchAction
        navMainBranchAction =
            new AbstractAction(board_resources.getString("Main_Branch"), new ImageIcon(getClass().getResource("/images/start.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    board.getBoardHandler().gotoMainBranch();
                }
            }; //}}}

        //{{{ navVarStartAction
        navVarStartAction =
            new AbstractAction(board_resources.getString("Variation_start"), new ImageIcon(getClass().getResource("/images/top.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    board.getBoardHandler().gotoVarStart();
                }
            }; //}}}

        //{{{ navNextBranchAction
        navNextBranchAction =
            new AbstractAction(board_resources.getString("Next_branch"), new ImageIcon(getClass().getResource("/images/bottom.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    board.getBoardHandler().gotoNextBranch();
                }
            }; //}}}

        //{{{ editDeleteAction
        editDeleteAction =
            new AbstractAction(board_resources.getString("Delete"), new ImageIcon(getClass().getResource("/images/Delete16.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    board.getBoardHandler().deleteNode();
                }
            }; //}}}

        //{{{ settingsGameinfoAction
        settingsGameinfoAction =
            new AbstractAction(board_resources.getString("Game_Info"), new ImageIcon(getClass().getResource("/images/Information16.gif"))) {
                public void actionPerformed(ActionEvent e) {
                    GameData data = board.getBoardHandler().getGameData();
                    GameInfoDialog dlg = new GameInfoDialog(MainFrame.this, true, data);
                    if (dlg.hasResult()) {
                        updateCaption();
                        setGameInfo(
                                data.playerWhite +
                                (data.rankWhite != null && data.rankWhite.length() > 0 ? " " + data.rankWhite : ""),
                                data.playerBlack +
                                (data.rankBlack != null && data.rankBlack.length() > 0 ? " " + data.rankBlack : ""),
                                data.handicap,
                                data.komi);
                    }
                }
            }; //}}}
    } //}}}

    //{{{ Menus

    //{{{ initMenus() method
    /**  Init the menus */
    protected void initMenus() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File menu
        menuBar.add(initFileMenu(FILE_NEW_BOARD | FILE_NEW | FILE_OPEN | FILE_SAVE | FILE_SAVE_AS | FILE_CLOSE |
                FILE_CONNECT_IGS | FILE_CONNECT_GTP | FILE_EXIT,
                FILE_IMPORTEXPORT_IMPORT_SGF));

        // Edit menu
        menuBar.add(initEditMenu(EDIT_DELETE | EDIT_SWAP_VARIATIONS | EDIT_NUMBER_MOVES | EDIT_MARK_BROTHERS |
                EDIT_MARK_SONS | EDIT_GUESS_SCORE));

        // Settings menu
        menuBar.add(initSettingsMenu(SETTINGS_PREFERENCES | SETTINGS_GAME_INFO | SETTINGS_MEMORY_STATUS |
                SETTINGS_MEMORY_CLEANUP | SETTINGS_AUTOPLAY_DELAY));

        // View menu
        menuBar.add(initViewMenu(VIEW_TOOLBAR | VIEW_STATUSBAR | VIEW_COORDS | VIEW_SIDEBAR | VIEW_CURSOR | VIEW_SLIDER | VIEW_VARIATIONS |
                VIEW_HORIZONTAL_COMMENT | VIEW_SAVE_SIZE));

        // Help Menu
        menuBar.add(initHelpMenu());
    } //}}}

    //{{{ initFileMenu() methods
    /**
     *  Description of the Method
     *
     *@param  i  Description of the Parameter
     *@param  e  Description of the Parameter
     *@return    Description of the Return Value
     */
    protected JMenu initFileMenu(int i, int e) {
        JMenu menu = new JMenu();
        setMnemonicText(menu, board_resources.getString("File"));
        JMenuItem menuItem;

        // File New Board
        if ((i & FILE_NEW_BOARD) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/newboard.gif")));
            setMnemonicText(menuItem, board_resources.getString("New_Board"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
            menuItem.setToolTipText(board_resources.getString("open_new_board_tooltip"));
            menuItem.setActionCommand("NewBoard");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        // File New
        if ((i & FILE_NEW) > 0) {
            menuItem = menu.add(fileNewAction);
            setMnemonicText(menuItem, menuItem.getText());
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
            menuItem.setToolTipText(board_resources.getString("new_game_tooltip"));
        }

        // File Open
        if ((i & FILE_OPEN) > 0) {
            menuItem = menu.add(fileOpenAction);
            setMnemonicText(menuItem, menuItem.getText());
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
            menuItem.setToolTipText(board_resources.getString("open_sgf_file_tooltip"));
        }

        // File Save
        if ((i & FILE_SAVE) > 0) {
            menuItem = menu.add(fileSaveAction);
            setMnemonicText(menuItem, menuItem.getText());
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
            menuItem.setToolTipText(board_resources.getString("save_game_tooltip"));
        }

        // File SaveAs
        if ((i & FILE_SAVE_AS) > 0) {
            menuItem = menu.add(fileSaveAsAction);
            setMnemonicText(menuItem, menuItem.getText());
            menuItem.setToolTipText(board_resources.getString("save_as_tooltip"));
        }

        // File Close
        if ((i & FILE_CLOSE) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Close"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
            menuItem.setToolTipText(board_resources.getString("close_tooltip"));
            menuItem.setActionCommand("Close");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        // Check if we add the import/export submenu
        if (e > 0) {
            menu.addSeparator();

            // A bit messy, but JMenu has no constructor with an icon
            JMenu ieMenu = new JMenu(
                new AbstractAction(board_resources.getString("import_export"), new ImageIcon(getClass().getResource("/images/Blank16.gif"))) {
                    public void actionPerformed(ActionEvent e) { }
                });
            ieMenu.setMnemonic(KeyEvent.VK_I);

            // File connect to server
            if ((e & FILE_IMPORTEXPORT_IMPORT_SGF) > 0) {
                menuItem = new JMenuItem(board_resources.getString("import_sgf"));
                menuItem.setActionCommand("ImportSGF");
                menuItem.addActionListener(mainFrameListener);
                ieMenu.add(menuItem);
            }

            menu.add(ieMenu);
        }

        if ((i & FILE_CONNECT_IGS) > 0 || (i & FILE_CONNECT_GTP) > 0)
            menu.addSeparator();

        // File connect to server
        if ((i & FILE_CONNECT_IGS) > 0) {
            menuItem = new JMenuItem(gGo.getgGoResources().getString("connect_igs"), new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            menuItem.setMnemonic(KeyEvent.VK_I);
            menuItem.setToolTipText(gGo.getgGoResources().getString("connect_igs_tooltip"));
            menuItem.setActionCommand("ConnectToIGS");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        // File Play against computer
        if ((i & FILE_CONNECT_GTP) > 0) {
            menuItem = new JMenuItem(gGo.getgGoResources().getString("play_gnugo"), new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            menuItem.setMnemonic(KeyEvent.VK_G);
            menuItem.setToolTipText(gGo.getgGoResources().getString("play_gnugo_tooltip"));
            menuItem.setActionCommand("PlayGnugo");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        if ((i & FILE_CONNECT_IGS) > 0 || (i & FILE_CONNECT_GTP) > 0)
            menu.addSeparator();

        // File Exit
        if ((i & FILE_EXIT) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/exit.gif")));
            setMnemonicText(menuItem, board_resources.getString("Exit"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
            menuItem.setToolTipText(board_resources.getString("exit_tooltip"));
            menuItem.setActionCommand("Exit");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        return menu;
    }

    /**
     *  Description of the Method
     *
     *@param  i  Description of the Parameter
     *@return    Description of the Return Value
     */
    protected JMenu initFileMenu(int i) {
        return initFileMenu(i, 0);
    } //}}}

    //{{{ initEditMenu() method
    /**
     *  Description of the Method
     *
     *@param  i  Description of the Parameter
     *@return    Description of the Return Value
     */
    protected JMenu initEditMenu(int i) {
        JMenu menu = new JMenu();
        setMnemonicText(menu, board_resources.getString("Edit"));
        JMenuItem menuItem;

        // Edit delete
        if ((i & EDIT_DELETE) > 0) {
            menuItem = menu.add(editDeleteAction);
            setMnemonicText(menuItem, menuItem.getText());
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
            menuItem.setToolTipText(board_resources.getString("delete_tooltip"));
        }

        // Edit Swap variations
        if ((i & EDIT_DELETE) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Swap_variations"));
            menuItem.setToolTipText(board_resources.getString("swap_variations_tooltip"));
            menuItem.setActionCommand("SwapVariations");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        if ((i & EDIT_NUMBER_MOVES) > 0 ||
                (i & EDIT_MARK_BROTHERS) > 0 ||
                (i & EDIT_MARK_SONS) > 0)
            menu.addSeparator();

        // Edit Number moves
        if ((i & EDIT_NUMBER_MOVES) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Number_moves"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, ActionEvent.SHIFT_MASK));
            menuItem.setToolTipText(board_resources.getString("number_moves_tooltip"));
            menuItem.setActionCommand("NumberMoves");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        // Edit Mark brothers
        if ((i & EDIT_MARK_BROTHERS) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Mark_brothers"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, ActionEvent.SHIFT_MASK));
            menuItem.setToolTipText(board_resources.getString("mark_brothers_tooltip"));
            menuItem.setActionCommand("MarkBrothers");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        // Edit Mark sons
        if ((i & EDIT_MARK_SONS) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Mark_sons"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.SHIFT_MASK));
            menuItem.setToolTipText(board_resources.getString("mark_sons_tooltip"));
            menuItem.setActionCommand("MarkSons");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        if ((i & EDIT_NUMBER_MOVES) > 0 ||
                (i & EDIT_MARK_BROTHERS) > 0 ||
                (i & EDIT_MARK_SONS) > 0)
            menu.addSeparator();

        // Refresh board
        if ((i & EDIT_IGS) > 0)
            initIGSEditMenu(menu);

        // Edit game
        if ((i & EDIT_GAME) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Edit_game"));
            menuItem.setToolTipText(board_resources.getString("edit_game_tooltip"));
            menuItem.setActionCommand("EditGame");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        // Edit Guess score
        if ((i & EDIT_GUESS_SCORE) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Guess_score"));
            menuItem.setToolTipText(board_resources.getString("guess_score_tooltip"));
            menuItem.setActionCommand("GuessScore");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        return menu;
    } //}}}

    //{{{ initIGSEditMenu() method
    /**
     *  Description of the Method
     *
     *@param  menu  Description of the Parameter
     */
    protected void initIGSEditMenu(JMenu menu) { } //}}}

    //{{{ initSettingsMenu() method
    /**
     *  Description of the Method
     *
     *@param  i  Description of the Parameter
     *@return    Description of the Return Value
     */
    protected JMenu initSettingsMenu(int i) {
        JMenu menu = new JMenu();
        setMnemonicText(menu, board_resources.getString("Settings"));
        JMenuItem menuItem;

        // Settings Preferences
        if ((i & SETTINGS_PREFERENCES) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Preferences16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Preferences"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
            menuItem.setToolTipText(board_resources.getString("preferences_tooltip"));
            menuItem.setActionCommand("Preferences");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        // Settings Game SETTINGS_GAME_INFO
        if ((i & SETTINGS_PREFERENCES) > 0) {
            menuItem = menu.add(settingsGameinfoAction);
            setMnemonicText(menuItem, menuItem.getText());
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
            menuItem.setToolTipText(board_resources.getString("game_info_tooltip"));
        }

        // Settings Memory status
        if ((i & SETTINGS_MEMORY_STATUS) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Memory_status"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK));
            menuItem.setToolTipText(board_resources.getString("memory_status_tooltip"));
            menuItem.setActionCommand("MemoryStatus");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        // Settings Memory cleanup
        if ((i & SETTINGS_MEMORY_CLEANUP) > 0) {
            menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
            setMnemonicText(menuItem, board_resources.getString("Memory_cleanup"));
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
            menuItem.setToolTipText(board_resources.getString("memory_cleanup_tooltip"));
            menuItem.setActionCommand("MemoryCleanup");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        //{{{ Autoplay delay
        if ((i & SETTINGS_AUTOPLAY_DELAY) > 0) {
            menu.addSeparator();
            // A bit messy, but JMenu has no constructor with an icon
            JMenu submenu = new JMenu(
                new AbstractAction(board_resources.getString("Autoplay_delay"), new ImageIcon(getClass().getResource("/images/Blank16.gif"))) {
                    public void actionPerformed(ActionEvent e) { }
                });
            submenu.setMnemonic(KeyEvent.VK_A);
            autoplayButtonGroup = new ButtonGroup();

            MessageFormat msgFormat = new MessageFormat(board_resources.getString("autoplay_delay_description"));

            JRadioButtonMenuItem rbMenuItem =
                    new JRadioButtonMenuItem(msgFormat.format(new Object[]{new Float(0.5f), board_resources.getString("seconds")}));
            autoplayButtonGroup.add(rbMenuItem);
            submenu.add(rbMenuItem);

            rbMenuItem = new JRadioButtonMenuItem(msgFormat.format(new Object[]{new Integer(1), board_resources.getString("second")}));
            rbMenuItem.setSelected(true);
            autoplayButtonGroup.add(rbMenuItem);
            submenu.add(rbMenuItem);

            rbMenuItem = new JRadioButtonMenuItem(msgFormat.format(new Object[]{new Integer(2), board_resources.getString("seconds")}));
            autoplayButtonGroup.add(rbMenuItem);
            submenu.add(rbMenuItem);

            rbMenuItem = new JRadioButtonMenuItem(msgFormat.format(new Object[]{new Integer(5), board_resources.getString("seconds")}));
            autoplayButtonGroup.add(rbMenuItem);
            submenu.add(rbMenuItem);

            menu.add(submenu);
        }
        //}}}

        return menu;
    } //}}}

    //{{{ initViewMenu() method
    /**
     *  Description of the Method
     *
     *@param  i  Description of the Parameter
     *@return    Description of the Return Value
     */
    protected JMenu initViewMenu(int i) {
        JMenu menu = new JMenu();
        setMnemonicText(menu, board_resources.getString("View"));
        JMenuItem menuItem;

        if ((i & VIEW_CLEAR) > 0) {
            // View Clear comment
            menuItem = new JMenuItem();
            setMnemonicText(menuItem, board_resources.getString("Clear_kibitz"));
            menuItem.setToolTipText(board_resources.getString("clear_kibitz_tooltip"));
            menuItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setCommentText("");
                    }
                });
            menu.add(menuItem);
        }

        // View Show ToolBar
        if ((i & VIEW_TOOLBAR) > 0) {
            viewToolBar = new JCheckBoxMenuItem(board_resources.getString("Show_toolbar"));
            viewToolBar.setToolTipText(board_resources.getString("Show_toolbar_tooltip"));
            viewToolBar.addItemListener(mainFrameListener);
            viewToolBar.setSelected(gGo.getSettings().getShowToolbar());
            menu.add(viewToolBar);
        }

        // View Show StatusBar
        if ((i & VIEW_STATUSBAR) > 0) {
            viewStatusBar = new JCheckBoxMenuItem(board_resources.getString("Show_statusbar"));
            viewStatusBar.setToolTipText(board_resources.getString("Show_statusbar_tooltip"));
            viewStatusBar.addItemListener(mainFrameListener);
            viewStatusBar.setSelected(gGo.getSettings().getShowStatusbar());
            menu.add(viewStatusBar);
        }

        // View Show coordinates
        if ((i & VIEW_COORDS) > 0) {
            viewCoordinates = new JCheckBoxMenuItem(board_resources.getString("Show_coordinates"));
            viewCoordinates.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, ActionEvent.CTRL_MASK));
            viewCoordinates.setToolTipText(board_resources.getString("Show_coordinates_tooltip"));
            viewCoordinates.addItemListener(mainFrameListener);
            viewCoordinates.setSelected(gGo.getSettings().getShowCoords());
            menu.add(viewCoordinates);
        }

        // View Show sidebar
        if ((i & VIEW_SIDEBAR) > 0) {
            viewSidebar = new JCheckBoxMenuItem(board_resources.getString("Show_sidebar"));
            viewSidebar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
            viewSidebar.setToolTipText(board_resources.getString("Show_sidebar_tooltip"));
            viewSidebar.addItemListener(mainFrameListener);
            viewSidebar.setSelected(gGo.getSettings().getShowSidebar());
            menu.add(viewSidebar);
        }

        // View Show cursor
        if ((i & VIEW_CURSOR) > 0) {
            viewCursor = new JCheckBoxMenuItem(board_resources.getString("Show_cursor"));
            viewCursor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
            viewCursor.setToolTipText(board_resources.getString("Show_cursor_tooltip"));
            viewCursor.addItemListener(mainFrameListener);
            viewCursor.setSelected(gGo.getSettings().getShowCursor());
            menu.add(viewCursor);
        }

        // View Show slider
        if ((i & VIEW_SLIDER) > 0) {
            viewSlider = new JCheckBoxMenuItem(board_resources.getString("Show_slider"));
            viewSlider.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8, ActionEvent.CTRL_MASK));
            viewSlider.setToolTipText(board_resources.getString("Show_slider_tooltip"));
            viewSlider.addItemListener(mainFrameListener);
            viewSlider.setSelected(gGo.getSettings().getShowSlider());
            menu.add(viewSlider);
        }

        // View Show variations
        if ((i & VIEW_VARIATIONS) > 0) {
            viewShowVariations = new JCheckBoxMenuItem(board_resources.getString("Show_variations"));
            viewShowVariations.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
            viewShowVariations.setToolTipText(board_resources.getString("Show_variations_tooltip"));
            // First toggle it on, then add the ItemListener. Else deadlock in updateGraphics()
            viewShowVariations.setSelected(gGo.getSettings().getShowVariationGhosts());
            viewShowVariations.addItemListener(mainFrameListener);
            menu.add(viewShowVariations);
        }

        // View Show horizontal comment field
        if ((i & VIEW_HORIZONTAL_COMMENT) > 0) {
            viewHorizontalComment = new JCheckBoxMenuItem(board_resources.getString("Show_horizontal_comment"));
            viewHorizontalComment.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10, ActionEvent.SHIFT_MASK));
            viewHorizontalComment.setToolTipText(board_resources.getString("Show_horizontal_comment_tooltip"));
            viewHorizontalComment.addItemListener(mainFrameListener);
            viewHorizontalComment.setSelected(gGo.getSettings().getShowHorizontalComment());
            menu.add(viewHorizontalComment);
        }

        // Save size
        if ((i & VIEW_SAVE_SIZE) > 0) {
            menuItem = new JMenuItem();
            setMnemonicText(menuItem, board_resources.getString("Save_size"));
            menuItem.setToolTipText(board_resources.getString("save_size_tooltip"));
            menuItem.setActionCommand("SaveSize");
            menuItem.addActionListener(mainFrameListener);
            menu.add(menuItem);
        }

        return menu;
    } //}}}

    //{{{ initHelpMenu() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    protected JMenu initHelpMenu() {
        JMenu menu = new JMenu();
        setMnemonicText(menu, board_resources.getString("Help"));

        // Help manual
        JMenuItem menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Help16.gif")));
        setMnemonicText(menuItem, board_resources.getString("Manual"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        menuItem.setToolTipText(board_resources.getString("manual_tooltip"));
        if (gGo.getHelpBroker() != null)
            menuItem.addActionListener(new CSH.DisplayHelpFromSource(gGo.getHelpBroker()));
        else
            menuItem.setEnabled(false);
        menu.add(menuItem);

        // Help webpage
        menuItem = new JMenuItem(board_resources.getString("ggo_webpage"), new ImageIcon(getClass().getResource("/images/WebComponent16.gif")));
        menuItem.setToolTipText(board_resources.getString("ggo_webpage_tooltip"));
        menuItem.setActionCommand("Webpage");
        menuItem.addActionListener(mainFrameListener);
        menu.add(menuItem);

        // Help about
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/About16.gif")));
        setMnemonicText(menuItem, board_resources.getString("About"));
        menuItem.setToolTipText(board_resources.getString("about_tooltip"));
        menuItem.setActionCommand("About");
        menuItem.addActionListener(mainFrameListener);
        menu.add(menuItem);

        // Help update
        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Information16.gif")));
        setMnemonicText(menuItem, board_resources.getString("check_update"));
        menuItem.setToolTipText(board_resources.getString("check_update_tooltip"));
        menuItem.setActionCommand("CheckUpdate");
        menuItem.addActionListener(mainFrameListener);
        menu.add(menuItem);

        return menu;
    } //}}}

    //{{{ setMnemonicText() method
    /**
     *  Set text and add a mnemonic to the AbstractButton by reading a '&' character from the text.
     *  If no '&' is given, no mnemonic is set.
     *
     *@param  button  The AbstractButton to set text and mnemonic
     *@param  text    The text to be set
     */
    public static void setMnemonicText(AbstractButton button, String text) {
        int index = text.indexOf('&');
        if (index >= 0 && index < text.length() - 1) {
            button.setText(text.substring(0, index) + text.substring(index + 1));
            char ch = text.charAt(index + 1);
            button.setMnemonic(ch);
        }
        else {
            button.setText(text);
        }
    } //}}}

    //}}}

    //{{{ initToolBar() method
    /**  Init the toolbar */
    protected void initToolBar() {
        toolBar = new JToolBar();

        try {
            //{{{ File buttons
            JButton newButton = toolBar.add(fileNewAction);
            newButton.setBorderPainted(false);
            newButton.setToolTipText(board_resources.getString("new_game_tooltip"));

            JButton openButton = toolBar.add(fileOpenAction);
            openButton.setBorderPainted(false);
            openButton.setToolTipText(board_resources.getString("open_sgf_file_tooltip"));

            JButton saveButton = toolBar.add(fileSaveAction);
            saveButton.setBorderPainted(false);
            saveButton.setToolTipText(board_resources.getString("save_game_tooltip"));

            JButton saveAsButton = toolBar.add(fileSaveAsAction);
            saveAsButton.setBorderPainted(false);
            saveAsButton.setToolTipText(board_resources.getString("save_as_tooltip"));
            //}}}

            toolBar.addSeparator();

            //{{{ Navigation buttons
            JButton twoLeftButton = new JButton(new ImageIcon(getClass().getResource("/images/2leftarrow.gif")));
            twoLeftButton.setBorderPainted(false);
            twoLeftButton.setActionCommand("FirstMove");
            twoLeftButton.setToolTipText(board_resources.getString("First_move_(Home)"));
            twoLeftButton.addActionListener(mainFrameListener);
            toolBar.add(twoLeftButton);

            JButton leftButton = new JButton(new ImageIcon(getClass().getResource("/images/1leftarrow.gif")));
            leftButton.setBorderPainted(false);
            leftButton.setActionCommand("PrevMove");
            leftButton.setToolTipText(board_resources.getString("Previous_move_(Left)"));
            leftButton.addActionListener(mainFrameListener);
            toolBar.add(leftButton);

            JButton rightButton = new JButton(new ImageIcon(getClass().getResource("/images/1rightarrow.gif")));
            rightButton.setBorderPainted(false);
            rightButton.setActionCommand("NextMove");
            rightButton.setToolTipText(board_resources.getString("Next_move_(Right)"));
            rightButton.addActionListener(mainFrameListener);
            toolBar.add(rightButton);

            JButton twoRightButton = new JButton(new ImageIcon(getClass().getResource("/images/2rightarrow.gif")));
            twoRightButton.setBorderPainted(false);
            twoRightButton.setActionCommand("LastMove");
            twoRightButton.setToolTipText(board_resources.getString("Last_move_(End)"));
            twoRightButton.addActionListener(mainFrameListener);
            toolBar.add(twoRightButton);

            toolBar.addSeparator();

            JButton mainBranchButton = toolBar.add(navMainBranchAction);
            mainBranchButton.setBorderPainted(false);
            mainBranchButton.setToolTipText(board_resources.getString("Main_branch_(Ins)"));

            JButton varStartButton = toolBar.add(navVarStartAction);
            varStartButton.setBorderPainted(false);
            varStartButton.setToolTipText(board_resources.getString("Variation_start_(PgUp)"));

            JButton upButton = new JButton(new ImageIcon(getClass().getResource("/images/up.gif")));
            upButton.setBorderPainted(false);
            upButton.setActionCommand("PrevVar");
            upButton.setToolTipText(board_resources.getString("Previous_variation_(Up)"));
            upButton.addActionListener(mainFrameListener);
            toolBar.add(upButton);

            JButton downButton = new JButton(new ImageIcon(getClass().getResource("/images/down.gif")));
            downButton.setBorderPainted(false);
            downButton.setActionCommand("NextVar");
            downButton.setToolTipText(board_resources.getString("Next_variation_(Down)"));
            downButton.addActionListener(mainFrameListener);
            toolBar.add(downButton);

            JButton nextBranchButton = toolBar.add(navNextBranchAction);
            nextBranchButton.setBorderPainted(false);
            nextBranchButton.setToolTipText(board_resources.getString("Next_branch_(PgDown)"));

            toolBar.addSeparator();

            autoplayButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/Play16.gif")));
            autoplayButton.setBorderPainted(false);
            autoplayButton.setToolTipText(board_resources.getString("Start/Stop_autoplay"));
            autoplayButton.addItemListener(mainFrameListener);
            toolBar.add(autoplayButton);
            //}}}

            toolBar.addSeparator();

            //{{{ Info buttons
            JButton gameInfoButton = toolBar.add(settingsGameinfoAction);
            gameInfoButton.setBorderPainted(false);
            gameInfoButton.setToolTipText(board_resources.getString("game_info_tooltip"));
            //}}}
        } catch (NullPointerException e) {
            System.err.println("Failed to load icons for toolbar: " + e);
        }
    } //}}}

    //{{{ closeFrame() method
    /**
     *  Close this frame and stop the thread
     *
     *@return    Description of the Return Value
     */
    public boolean closeFrame() {
        if (checkModified()) {
            gGo.unregisterFrame(this);
            setVisible(false);
            dispose();
            getClockBlack().stop();
            getClockWhite().stop();
            if (clipImporter != null) {
                clipImporter.setVisible(false);
                clipImporter.dispose();
            }
            if (mirrorFrame != null)
                mirrorFrame.setSynchFrame(null);
            if (synchFrame != null)
                synchFrame.setMirrorFrame(null);
            runMe = false;
            frameCounter--;
            return true;
        }
        return false;
    } //}}}

    //{{{ Getter & Setter

    //{{{ getBoard() method
    /**
     *  Gets the board attribute of the MainFrame object
     *
     *@return    The board value
     */
    public Board getBoard() {
        return board;
    } //}}}

    //{{{ getSideBar() method
    /**
     *  Gets the sideBar attribute of the MainFrame object
     *
     *@return    The sideBar value
     */
    public SideBar getSideBar() {
        return sideBar;
    } //}}}

    //{{{ getStatusBar() method
    /**
     *  Gets the statusBar attribute of the MainFrame object
     *
     *@return    The statusBar value
     */
    public StatusBar getStatusBar() {
        return statusBar;
    } //}}}

    //{{{ setCommentText() method
    /**
     *  Write text in the comment field
     *
     *@param  t  Text to write
     */
    public void setCommentText(String t) {
        commentEdit.setText(t);
    } //}}}

    //{{{ appendCommentText() method
    /**
     *  Append a kibitz or say line to the comment field and scroll it down
     *
     *@param  s  String to append
     */
    public void appendCommentText(String s) {
        commentEdit.setText(commentEdit.getText() + s + "\n");

        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        commentEdit.scrollRectToVisible(commentEdit.modelToView(
                                commentEdit.getDocument().getLength()));
                    } catch (BadLocationException e) {
                        System.err.println("Failed to scroll: " + e);
                    }
                }
            });
    } //}}}

    //{{{ getMarkType() method
    /**
     *  Gets the markType attribute of the MainFrame object
     *
     *@return    The markType value
     */
    public int getMarkType() {
        return sideBar.getMarkType();
    } //}}}

    //{{{ setMarkType() method
    /**
     *  Sets the markType attribute of the MainFrame object
     *
     *@param  t  The new markType value
     */
    public void setMarkType(int t) {
        sideBar.setMarkType(t);
    } //}}}

    //{{{ setSplitPaneOrientation() method
    /**
     *  Sets the splitPaneOrientation attribute of the MainFrame object
     *
     *@param  o  The new splitPaneOrientation value
     */
    public void setSplitPaneOrientation(int o) {
        splitPane.setOrientation(o);
    } //}}}

    //{{{ getSplitPaneOrientation() method
    /**
     *  Gets the splitPaneOrientation attribute of the MainFrame object
     *
     *@return    The splitPaneOrientation value
     */
    public int getSplitPaneOrientation() {
        return splitPane.getOrientation();
    } //}}}

    //{{{ getSlider() method
    /**
     *  Gets the slider attribute of the MainFrame object
     *
     *@return    The slider value
     */
    public JSlider getSlider() {
        return slider;
    } //}}}

    //{{{ getSliderMaxLabel() method
    /**
     *  Gets the sliderMaxLabel attribute of the MainFrame object
     *
     *@return    The sliderMaxLabel value
     */
    public JLabel getSliderMaxLabel() {
        return sliderMaxLabel;
    } //}}}

    //{{{ getClockBlack() method
    /**
     *  Returns a pointer to the clock of the black player
     *
     *@return    The clockBlack value
     */
    public Clock getClockBlack() {
        try {
            return sideBar.getClockBlack();
        } catch (NullPointerException e) {
            return null;
        }
    } //}}}

    //{{{ getClockWhite() method
    /**
     *  Returns a pointer to the clock of the white player
     *
     *@return    The clockWhite value
     */
    public Clock getClockWhite() {
        try {
            return sideBar.getClockWhite();
        } catch (NullPointerException e) {
            return null;
        }
    } //}}}

    //{{{ setGameInfo() method
    /**
     *  Sets the player names, handicap and komi
     *
     *@param  white     White name
     *@param  black     Black name
     *@param  handicap  Handicap
     *@param  komi      Komi
     */
    public void setGameInfo(final String white, final String black, final int handicap, final float komi) {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        sideBar.setGameInfo(white, black, handicap, komi);
                    } catch (NullPointerException e) {}
                }
            });
    } //}}}

    //{{{ getMirrorFrame() method
    /**
     *  Gets the mirrorFrame attribute of the MainFrame object
     *
     *@return    The mirrorFrame value
     */
    MainFrame getMirrorFrame() {
        return mirrorFrame;
    } //}}}

    //{{{ setMirrorFrame() method
    /**
     *  Sets the mirrorFrame attribute of the MainFrame object
     *
     *@param  mf  The new mirrorFrame value
     */
    void setMirrorFrame(MainFrame mf) {
        mirrorFrame = mf;
    } //}}}

    //{{{ getSynchFrame() method
    /**
     *  Gets the synchFrame attribute of the MainFrame object
     *
     *@return    The synchFrame value
     */
    public MainFrame getSynchFrame() {
        return synchFrame;
    } //}}}

    //{{{ setSynchFrame() method
    /**
     *  Sets the synchFrame attribute of the MainFrame object
     *
     *@param  mf  The new synchFrame value
     */
    public void setSynchFrame(MainFrame mf) {
        synchFrame = mf;
    } //}}}

    //}}}

    //{{{ newGame() method
    /**  Description of the Method */
    protected void newGame() {
        if (checkModified()) {
            NewGameDialog dlg = new NewGameDialog(MainFrame.this, true);
            if (dlg.getResult()) {
                GameData data = new GameData();
                data.playerBlack = dlg.getBlackName();
                data.playerWhite = dlg.getWhiteName();
                data.size = dlg.getBoardSize();
                data.komi = dlg.getKomi();
                data.handicap = dlg.getHandicap();

                board.initGame(data, false);
            }
        }
    } //}}}

    //{{{ updateCaption() method
    /**  Update the frame title */
    public void updateCaption() {
        // Print caption
        // example: Zotan 8k vs. tgmouse 10k
        // or if game name is given: Kogo's Joseki Dictionary
        try {
            setTitle(
                    (board.isModified() ? "* " : "") +
                    (board.getBoardHandler().getGameData().gameName == null ||
                    board.getBoardHandler().getGameData().gameName.length() == 0 ?
                    board.getBoardHandler().getGameData().playerWhite +
                    (board.getBoardHandler().getGameData().rankWhite != null &&
                    board.getBoardHandler().getGameData().rankWhite.length() > 0 ?
                    " " + board.getBoardHandler().getGameData().rankWhite : "") +
                    " " + board_resources.getString("vs.") + " " +
                    board.getBoardHandler().getGameData().playerBlack +
                    (board.getBoardHandler().getGameData().rankBlack != null &&
                    board.getBoardHandler().getGameData().rankBlack.length() > 0 ?
                    " " + board.getBoardHandler().getGameData().rankBlack : "") :
                    board.getBoardHandler().getGameData().gameName));
        } catch (NullPointerException e) {
            System.err.println("Failed to set title: " + e);
        }
    } //}}}

    //{{{ changeLookAndFeel() method
    /**  Change the look and feel */
    protected void changeLookAndFeel() {
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
                        SwingUtilities.updateComponentTreeUI(MainFrame.this);
                        MainFrame.this.pack();
                    }
                });
        } catch (Exception e) {
            System.err.println("Failed to set LookAndFeel '" + lnfName + "': " + e);
            gGo.getSettings().setLookAndFeel(oldlnf);
        }
    } //}}}

    //{{{ applySettings() method
    /**  Check for changes and apply preferences settings */
    protected void applySettings() {
        // Look and feel
        if (oldlnf != gGo.getSettings().getLookAndFeel() ||
                !oldTheme.equals(gGo.getSettings().getThemePack()))
            changeLookAndFeel();

        // Show sidebar
        if (gGo.getSettings().getShowSidebar() != sideBar.isVisible())
            viewSidebar.setSelected(gGo.getSettings().getShowSidebar());

        // showCursor
        if (viewCursor != null)
            viewCursor.setSelected(gGo.getSettings().getShowCursor());

        // Show toolbar
        if (toolBar != null && gGo.getSettings().getShowToolbar() != toolBar.isVisible())
            viewToolBar.setSelected(gGo.getSettings().getShowToolbar());

        // Show coordinates
        if (gGo.getSettings().getShowCoords() != board.getShowCoords())
            viewCoordinates.setSelected(gGo.getSettings().getShowCoords());

        // Show variation ghosts
        if (viewShowVariations != null)
            viewShowVariations.setSelected(gGo.getSettings().getShowVariationGhosts());

        // Show horizontal comment
        if ((gGo.getSettings().getShowHorizontalComment() &&
                (getSplitPaneOrientation() == JSplitPane.VERTICAL_SPLIT)) ||
                (!gGo.getSettings().getShowHorizontalComment() &&
                (getSplitPaneOrientation() == JSplitPane.HORIZONTAL_SPLIT)))
            viewHorizontalComment.setSelected(gGo.getSettings().getShowHorizontalComment());

        // Show slider
        if (!isPlayingFrame && gGo.getSettings().getShowSlider() != sliderPanel.isVisible())
            viewSlider.setSelected(gGo.getSettings().getShowSlider());

        // Show statusbar
        statusBar.setVisible(gGo.getSettings().getShowStatusbar());

        // Font size
        commentEdit.setFont(new Font("Serif", 0, gGo.getSettings().getSerifFontSize()));
        if (gGo.hasIGSWindow())
            gGo.getIGSWindow().changeFontSize();

        gGo.getSettings().saveSettings();
    } //}}}

    //{{{ openSGF() method
    /**
     *  Load a file and open a new game
     *
     *@param  fileName  Filename to load from
     *@param  remName   If true, remember the filename
     */
    public void openSGF(final String fileName, final boolean remName) {
        final SwingWorker worker =
            new SwingWorker() {
                public Object construct() {
                    board.lock();
                    if (board.openSGF(fileName, remName)) {
                        statusBar.printMessage(fileName + " " + board_resources.getString("loaded") + ".");
                        return new Boolean(true);
                    }
                    return new Boolean(false);
                }

                public void finished() {
                    board.unlock();
                    board.repaint();
                    if (new Boolean(true).equals(get()))
                        setTmpRemDir();
                    if (synchFrame != null)
                        board.getBoardHandler().gotoLastMove(true);
                }
            };
        worker.start(); //required for SwingWorker 3
    } //}}}

    //{{{ setTmpRemDir() method
    /**  Set remDir property in settings after a file was successfully loaded */
    private void setTmpRemDir() {
        if (tmpRemDir != null) {
            gGo.getSettings().setRemDir(tmpRemDir);
            tmpRemDir = null;
        }
    } //}}}

    //{{{ saveSGF() method
    /**
     *  Save a game.
     *
     *@return    True if successful, else false
     */
    public boolean saveSGF() {
        if (board.getBoardHandler().getGameData().fileName != null &&
                board.getBoardHandler().getGameData().fileName.length() > 0) {
            boolean res = board.saveSGF(board.getBoardHandler().getGameData().fileName);
            if (res)
                statusBar.printMessage(board.getBoardHandler().getGameData().fileName + " " + board_resources.getString("saved") + ".");
            return res;
        }
        else
            return doSave();
    } //}}}

    //{{{ assembleDefaultFilename() method
    /**
     *  Convert player names and rank to a default filename
     *
     *@return    Suggested filename
     */
    private String assembleDefaultFilename() {
        // Remove IGS '*' from rank
        String rw = board.getBoardHandler().getGameData().rankWhite;
        if (rw.length() > 0 && rw.charAt(rw.length() - 1) == '*')
            rw = rw.substring(0, rw.length() - 1);

        String rb = board.getBoardHandler().getGameData().rankBlack;
        if (rb.length() > 0 && rb.charAt(rb.length() - 1) == '*')
            rb = rb.substring(0, rb.length() - 1);

        return board.getBoardHandler().getGameData().playerWhite + rw + "-" +
                board.getBoardHandler().getGameData().playerBlack + rb;
    } //}}}

    //{{{ doSave() method
    /**
     *  Do the actual save process. Check if a filename in the GameData exists, else
     *  use the filechooser dialog. Prompt the user if the file already exists.
     *
     *@return    True if successful, else false
     */
    protected boolean doSave() {
        final JFileChooser fc = new JFileChooser(gGo.getSettings().getRemDir());
        // Assemble default filename
        String fn = assembleDefaultFilename();
        if (fn.length() > 0)
            fc.setSelectedFile(new File(
                    (gGo.getSettings().getDoRemDir() && gGo.getSettings().getRemDir() != null &&
                    gGo.getSettings().getRemDir().length() > 0 ? gGo.getSettings().getRemDir() + "/" + fn :
                    fn) + ".sgf"));
        // Looks strange, but the SGF filter should be first and selected
        SGFFileFilter sgf_filter = new SGFFileFilter();
        fc.addChoosableFileFilter(sgf_filter);
        fc.addChoosableFileFilter(new gGoFileFilter());
        fc.setFileFilter(sgf_filter);
        if (fc.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file.exists()) {
                if (JOptionPane.showConfirmDialog(
                        this,
                        board_resources.getString("ask_overwrite"),
                        PACKAGE,
                        JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                    return false;
            }
            gGo.getSettings().setRemDir(file.getParent());
            boolean res = board.saveSGF(file);
            if (res)
                statusBar.printMessage(file.getAbsolutePath() + " " + board_resources.getString("saved") + ".");
            return res;
        }
        return false;
    } //}}}

    //{{{ checkModified() method
    /**
     *  Check if the board was modified, if yes prompt the user if he wants to save the board.
     *
     *@return    True of board was not modified, or if the game was saved.
     *            False if the operation has to be aborted
     */
    public boolean checkModified() {
        if (!board.isModified())
            return true;
        else {
            switch (JOptionPane.showConfirmDialog(
                    this,
                    board_resources.getString("ask_modified"),
                    PACKAGE,
                    JOptionPane.YES_NO_CANCEL_OPTION)) {
                case JOptionPane.YES_OPTION:
                    return saveSGF();
                case JOptionPane.NO_OPTION:
                    return true;
                case JOptionPane.CANCEL_OPTION:
                    return false;
            }
        }
        return false;
    } //}}}

    //{{{ showScore() method
    /**
     *  Show a messagebox with the score
     *
     *@param  result  String with the score to be shown.
     */
    public void showScore(String result) {
        JOptionPane.showMessageDialog(this, board_resources.getString("gnugo_thinks") + ": " + result);
    } //}}}

    //{{{ writeCommentToSGF() method
    /**  Write the comment textarea to SGF comment */
    public void writeCommentToSGF() {
        try {
            board.getBoardHandler().getTree().getCurrent().setComment(commentEdit.getText());
        } catch (NullPointerException ex) {}
    } //}}}

    //{{{ class MainFrameListener
    /**
     *  Local listener clss for Menu, toolbar actions and the comment field.
     *
     *@author     Peter Strempel
     *@version    $Revision: 1.21 $, $Date: 2002/10/23 01:50:49 $
     */
    class MainFrameListener implements ActionListener, ItemListener, DocumentListener, ChangeListener {
        //{{{ actionPerformed() method
        /**
         *  Action handler for menus
         *
         *@param  e  Action Event
         */
        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();

            //{{{ File New Board
            if (cmd.equals("NewBoard")) {
                if (gGo.openNewMainFrame() == null)
                    System.err.println("Failed to open new board.");
            } //}}}
            //{{{ File Close
            else if (cmd.equals("Close")) {
                if (closeFrame() && gGo.getNumberOfOpenFrames() == 0 &&
                        !gGo.hasStartUpFrame() &&
                        MainFrame.this.getClass().getName().equals("ggo.MainFrame"))
                    gGo.exitApp(0);
            } //}}}
            //{{{ File Exit
            else if (cmd.equals("Exit")) {
                if (gGo.hasIGSConnection()) {
                    if (JOptionPane.showConfirmDialog(
                            MainFrame.this,
                            board_resources.getString("ask_exit"),
                            PACKAGE,
                            JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                        return;
                }
                else if (gGo.getNumberOfOpenFrames() == 1) {
                    if (closeFrame())
                        gGo.exitApp(0);
                    else
                        return;
                }
                else if (gGo.hasModifiedBoards()) {
                    if (JOptionPane.showConfirmDialog(
                            MainFrame.this,
                            board_resources.getString("warn_exit"),
                            PACKAGE,
                            JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
                        return;
                }
                setVisible(false);
                dispose();
                gGo.exitApp(0);
            } //}}}
            //{{{ File Import/Export ImportSGF
            else if (cmd.equals("ImportSGF")) {
                if (clipImporter == null)
                    clipImporter = new ClipImporter(MainFrame.this, true);
                else
                    clipImporter.setVisible(true);
            } //}}}
            //{{{ File Connect to IGS
            else if (cmd.equals("ConnectToIGS")) {
                gGo.openIGSWindow();
            } //}}}
            //{{{ File Play against computer
            else if (cmd.equals("PlayGnugo")) {
                gGo.openGTPWindow(MainFrame.this);
            } //}}}
            //{{{ Edit automark
            else if (cmd.equals("NumberMoves"))
                board.getBoardHandler().numberMoves();
            else if (cmd.equals("MarkBrothers"))
                board.getBoardHandler().markVariations(false);
            else if (cmd.equals("MarkSons"))
                board.getBoardHandler().markVariations(true);
            //}}}
            //{{{ Edit Game
            else if (cmd.equals("EditGame")) {
                if (board.getPlayMode() != PLAY_MODE_EDIT)
                    board.openEditBoard();
            } //}}}
            //{{{ Edit Swap variations
            else if (cmd.equals("SwapVariations")) {
                board.getBoardHandler().swapVariations();
            } //}}}
            //{{{ Edit Guess score
            else if (cmd.equals("GuessScore")) {
                File tmpFile;
                try {
                    tmpFile = File.createTempFile("ggo", ".sgf");
                } catch (IOException ex) {
                    System.err.println("Failed to create temporary file: " + ex);
                    return;
                }
                board.getBoardHandler().saveGame(tmpFile);
                String result =
                        GnuGoScorer.guessScore(tmpFile.getAbsolutePath(),
                        board.getBoardHandler().getTree().getCurrent().getMoveNumber() + 1);
                // GnuGo not found?
                if (result == null)
                    JOptionPane.showMessageDialog(MainFrame.this,
                            board_resources.getString("gnugo_not_found"),
                            PACKAGE + " " + "Error",
                            JOptionPane.WARNING_MESSAGE);
                else
                    showScore(result);
            } //}}}
            //{{{ Settings Preferences
            else if (cmd.equals("Preferences")) {
                oldlnf = gGo.getSettings().getLookAndFeel();
                oldTheme = gGo.getSettings().getThemePack();
                PreferencesDialog dlg = new PreferencesDialog(MainFrame.this, true, gGo.getSettings());
                if (dlg.getResult())
                    applySettings();
            } //}}}
            //{{{ Settings Memory
            else if (cmd.equals("MemoryStatus"))
                JOptionPane.showMessageDialog(MainFrame.this,
                        board_resources.getString("Memory_message") + ": " + gGo.getMemoryStatus());
            else if (cmd.equals("MemoryCleanup"))
                JOptionPane.showMessageDialog(MainFrame.this,
                        board_resources.getString("memory_released") + ": " + gGo.forceGarbageCollection() + board_resources.getString("KB"));
            //}}}
            //{{{ View Save size
            else if (cmd.equals("SaveSize")) {
                // gGo.getSettings().setFrameSize(getSize());
                gGo.getSettings().setFrameSize(board.getSize());
                System.err.println("SIZE SAVED: " + board.getSize());
            }
            //}}}
            //{{{ Help
            else if (cmd.equals("About"))
                gGo.showAbout(MainFrame.this);
            else if (cmd.equals("Webpage"))
                BrowserControl.displayURL(GGO_URL);
            else if (cmd.equals("CheckUpdate")) {
                new UpdateChecker(MainFrame.this).start();
            }
            //}}}
            //{{{ Move navigation
            else if (cmd.equals("FirstMove"))
                board.getBoardHandler().gotoFirstMove();
            else if (cmd.equals("PrevMove"))
                board.getBoardHandler().previousMove();
            else if (cmd.equals("NextMove"))
                board.getBoardHandler().nextMove(false);
            else if (cmd.equals("LastMove"))
                board.getBoardHandler().gotoLastMove(true);
            else if (cmd.equals("PrevVar"))
                board.getBoardHandler().previousVariation();
            else if (cmd.equals("NextVar"))
                board.getBoardHandler().nextVariation();
            //}}}
        } //}}}

        //{{{ itemStateChanged() method
        /**
         *  ItemListener method
         *
         *@param  e  ItemEvent
         */
        public void itemStateChanged(ItemEvent e) {
            Object source = e.getItemSelectable();

            //{{{ View horizontal comment field
            if (source == viewHorizontalComment) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    setSplitPaneOrientation(JSplitPane.HORIZONTAL_SPLIT);
                else if (e.getStateChange() == ItemEvent.DESELECTED)
                    setSplitPaneOrientation(JSplitPane.VERTICAL_SPLIT);
                splitPane.resetToPreferredSizes();
            } //}}}
            //{{{ View toolbar
            else if (source == viewToolBar && toolBar != null) {
                toolBar.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            } //}}}
            //{{{ View statusbar
            else if (source == viewStatusBar) {
                statusBar.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            } //}}}
            //{{{ View sidebar
            else if (source == viewSidebar) {
                sideBar.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            } //}}}
            //{{{ View cursor
            else if (source == viewCursor) {
                gGo.getSettings().setShowCursor(e.getStateChange() == ItemEvent.SELECTED);
            } //}}}
            //{{{ View coordinates
            else if (source == viewCoordinates) {
                gGo.getSettings().setShowCoords(e.getStateChange() == ItemEvent.SELECTED);
                board.setShowCoords(e.getStateChange() == ItemEvent.SELECTED);
                board.repaint();
            } //}}}
            //{{{ View slider
            else if (source == viewSlider) {
                sliderPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED && !isPlayingFrame);
            } //}}}
            //{{{ View show variations
            else if (source == viewShowVariations) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    gGo.getSettings().setShowVariationGhosts(true);
                    board.getBoardHandler().updateVariationGhosts(false);
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    gGo.getSettings().setShowVariationGhosts(false);
                    board.removeGhosts();
                }
                board.updateGraphics();
            } //}}}
            //{{{ Autoplay
            else if (source != null && source == autoplayButton) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    // Get delay
                    int delay = 1000;
                    int i = 0;
                    for (Enumeration en = autoplayButtonGroup.getElements(); en.hasMoreElements(); i++) {
                        AbstractButton b = (AbstractButton)en.nextElement();
                        if (b.isSelected()) {
                            delay = autoplayDelays[i];
                            break;
                        }
                    }
                    autoplayTimer = new javax.swing.Timer(delay,
                        new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                                if (!board.getBoardHandler().nextMove(true) ||
                                        (!gGo.is13() && !isActive()))
                                    autoplayButton.setSelected(false);
                            }
                        });
                    autoplayTimer.start();
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    autoplayTimer.stop();
                    autoplayTimer = null;
                }
            } //}}}

        } //}}}

        //{{{ DocumentListener methods()
        /**
         *  Gives notification that the comment edit field has changed.
         *
         *@param  e  The document event
         */
        public void insertUpdate(DocumentEvent e) {
            writeCommentToSGF();
        }

        /**
         *  Gives notification that the comment edit field has changed.
         *
         *@param  e  The document event
         */
        public void removeUpdate(DocumentEvent e) {
            writeCommentToSGF();
        }

        /**
         *  Gives notification that the comment edit field has changed. - Not used.
         *
         *@param  e  The document event
         */
        public void changedUpdate(DocumentEvent e) { } //}}}

        //{{{ ChangeListener method()
        /**
         *  Description of the Method
         *
         *@param  e  Description of the Parameter
         */
        public void stateChanged(ChangeEvent e) {
            if (!dontFireSlider) {
                if (MainFrame.this.getClass().getName().equals("ggo.MainFrame"))
                    board.getBoardHandler().gotoNthMoveInVar((int)slider.getValue());
                else
                    board.getBoardHandler().gotoNthMove((int)slider.getValue());
            }
        } //}}}
    } //}}}
}

