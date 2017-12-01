/*
 *  IGSObserverFrame.java
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
import java.awt.*;
import java.awt.event.*;
import java.util.Date;
import java.text.DateFormat;
import ggo.*;
import ggo.igs.*;
import ggo.utils.*;

/**
 *  A subclass of MainFrame with some adjustments for online game observing
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.13 $, $Date: 2002/10/27 09:20:13 $
 */
public class IGSObserverFrame extends MainFrame {
    //{{{ private members
    private int gameID;
    private JTextField inputTextField;
    private JComboBox kibitzChatterChooser;
    private StringBuffer currentKibitz;
    private boolean isFinished;
    private JCheckBoxMenuItem viewComm, viewTimestamp;
    private long lastMoveTimeStamp;
    //}}}

    //{{{ IGSObserverFrame constructor
    /**
     *Constructor for the IGSObserverFrame object
     *
     *@param  gameID  Description of the Parameter
     */
    public IGSObserverFrame(int gameID) {
        super();
        this.gameID = gameID;
        currentKibitz = new StringBuffer();
        isFinished = false;
    } //}}}

    //{{{ getGameID() method
    /**
     *  Gets the gameID attribute of the IGSObserverFrame object
     *
     *@return    The gameID value
     */
    public int getGameID() {
        return gameID;
    } //}}}

    //{{{ setGameID() method
    /**
     *  Sets the gameID attribute of the IGSObserverFrame object
     *
     *@param  id  The new gameID value
     */
    public void setGameID(int id) {
        gameID = id;
        isFinished = false;
    } //}}}

    //{{{ initCommentEdit() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    protected JComponent initCommentEdit() {
        commentEdit.setEditable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(1, 1));

        JScrollPane commentScrollPane = new JScrollPane(commentEdit);
        commentScrollPane.setPreferredSize(new Dimension(100, 60));
        commentScrollPane.setMinimumSize(new Dimension(0, 0));
        commentEdit.getDocument().addDocumentListener(mainFrameListener);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        inputTextField = new JTextField();
        inputTextField.setFont(new Font("Sans Serif", 0, gGo.getSettings().getSansSerifFontSize()));
        inputTextField.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    inputTextFieldActionPerformed();
                }
            });

        String[] args = {
                gGo.getIGSResources().getString("Kibitz"),
                gGo.getIGSResources().getString("Chatter"),
                gGo.getIGSResources().getString("Command")
                };
        kibitzChatterChooser = new JComboBox(args);
        kibitzChatterChooser.setToolTipText(gGo.getIGSResources().getString("kibitz_chatter_command_tooltip"));

        bottomPanel.add(inputTextField, BorderLayout.CENTER);
        bottomPanel.add(kibitzChatterChooser, BorderLayout.EAST);

        panel.add(commentScrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    } //}}}

    //{{{ inputTextFieldActionPerformed() method
    /**  Text was typed in the input line and return pressed */
    public void inputTextFieldActionPerformed() {
        String command = inputTextField.getText();

        if (command.length() == 0)
            return;

        try {
            String sendIt = "";

            switch (kibitzChatterChooser.getSelectedIndex()) {
                case 0:
                    sendIt = "kibitz " + gameID + " ";
                    addKibitz(IGSConnection.getLoginName() + ": " + command);
                    break;
                case 1:
                    sendIt = "chatter " + gameID + " ";
                    addKibitz(IGSConnection.getLoginName() + ": " + command);
                    break;
                default:
                    addKibitz("[Command] " + command);
                    break;
            }

            IGSMainWindow.getIGSConnection().recieveInput(sendIt + command);
        } catch (NullPointerException e) {
            System.err.println("Failed to send kibitz to server: " + e);
        }

        inputTextField.setText("");
    } //}}}

    //{{{ initToolBar() method
    /**  Init the toolbar. Fewer buttons. */
    protected void initToolBar() {
        toolBar = new JToolBar();

        try {
            JButton focusMainWindowButton = new JButton(new ImageIcon(getClass().getResource("/images/Home16.png")));
            focusMainWindowButton.setBorderPainted(false);
            focusMainWindowButton.setToolTipText(gGo.getIGSResources().getString("focus_main_window_tooltip"));
            focusMainWindowButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            IGSConnection.getMainWindow().checkVisible();
                        } catch (NullPointerException ex) {}
                    }
                });
            toolBar.add(focusMainWindowButton);

            toolBar.addSeparator();

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

            JButton gameInfoButton = toolBar.add(settingsGameinfoAction);
            gameInfoButton.setBorderPainted(false);
            gameInfoButton.setToolTipText(board_resources.getString("game_info_tooltip"));
        } catch (NullPointerException e) {
            System.err.println("Failed to load icons for toolbar: " + e);
        }
    } //}}}

    //{{{ initMenus() method
    /**  Init the menus. Overrides method from superclass, some different menus */
    protected void initMenus() {
        // Use heavyweight menus in this frame for convinience.
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File menu
        menuBar.add(initFileMenu(FILE_SAVE | FILE_SAVE_AS | FILE_CLOSE));

        // Edit menu
        menuBar.add(initEditMenu(EDIT_IGS | EDIT_GAME | EDIT_GUESS_SCORE));

        // Settings menu
        menuBar.add(initSettingsMenu(SETTINGS_PREFERENCES | SETTINGS_GAME_INFO | SETTINGS_MEMORY_STATUS | SETTINGS_MEMORY_CLEANUP));

        // View menu
        menuBar.add(initViewMenu(VIEW_CLEAR | VIEW_TOOLBAR | VIEW_STATUSBAR | VIEW_COORDS | VIEW_SIDEBAR | VIEW_SLIDER | VIEW_HORIZONTAL_COMMENT | VIEW_SAVE_SIZE));

        // Help Menu
        menuBar.add(initHelpMenu());

        JPopupMenu.setDefaultLightWeightPopupEnabled(true);
    } //}}}

    //{{{ initViewMenu() method
    /**
     *  Description of the Method
     *
     *@param  i  Description of the Parameter
     *@return    Description of the Return Value
     */
    protected JMenu initViewMenu(int i) {
        JMenu menu = super.initViewMenu(i);

        // View communication
        menu.addSeparator();
        viewComm = new JCheckBoxMenuItem();
        setMnemonicText(viewComm, gGo.getIGSResources().getString("Display_communication"));
        viewComm.setToolTipText(gGo.getIGSResources().getString("display_communication_tooltip"));
        viewComm.setSelected(gGo.getSettings().getViewComm());
        viewComm.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    gGo.getSettings().setViewComm(e.getStateChange() == ItemEvent.SELECTED);
                }
            });
        menu.add(viewComm);

        // View Timestamp
        viewTimestamp = new JCheckBoxMenuItem();
        setMnemonicText(viewTimestamp, gGo.getIGSResources().getString("display_timestamps"));
        viewTimestamp.setToolTipText(gGo.getIGSResources().getString("display_timestamps_tooltip"));
        menu.add(viewTimestamp);

        return menu;
    } //}}}

    //{{{ initIGSEditMenu() method
    /**
     *  Description of the Method
     *
     *@param  menu  Description of the Parameter
     */
    protected void initIGSEditMenu(JMenu menu) {
        // Refresh board
        JMenuItem menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        setMnemonicText(menuItem, gGo.getIGSResources().getString("Refresh_board"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        menuItem.setToolTipText(gGo.getIGSResources().getString("refresh_board_tooltip"));
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (IGSConnection.getGameObserver().observesGame(gameID)) {
                        IGSReader.requestRefresh = gameID;
                        IGSConnection.sendCommand("status " + gameID);
                        IGSConnection.sendCommand("refresh " + gameID);
                    }
                }
            });
        menu.add(menuItem);

        // Show observers
        menuItem = new JMenuItem(gGo.getIGSResources().getString("Observers"), new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        menuItem.setToolTipText(gGo.getIGSResources().getString("observers_tooltip"));
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    IGSReader.requestObservers = gameID;
                    IGSConnection.sendCommand("all " + gameID);
                }
            });
        menu.add(menuItem);
    } //}}}

    //{{{ closeFrame() method
    /**
     *  Close the frame, overwrites method of superclass.
     *  Unregisters itself at the IGSGameObserver object it belongs to.
     *
     *@return    Always true
     */
    public boolean closeFrame() {
        try {
            IGSConnection.getGameObserver().removeGame(gameID);
            IGSConnection.getGameObserver().removeAdjournedGame(
                    new Couple(getBoard().getBoardHandler().getGameData().playerWhite,
                    getBoard().getBoardHandler().getGameData().playerBlack));
        } catch (NullPointerException e) {
            System.err.println("Failed to remove frame from observer list: " + e);
        }
        setVisible(false);
        dispose();
        getClockBlack().stop();
        getClockWhite().stop();
        if (mirrorFrame != null)
            mirrorFrame.setSynchFrame(null);
        runMe = false;
        frameCounter--;
        return true;
    } //}}}

    //{{{ updateCaption() methods
    /**
     *  Update the frame title. Overwrites method in MainFrame as this window needs a different caption.
     */
    public void updateCaption() {
        // Print caption
        // example: Game 10 - Zotan 8k vs. tgmouse 10k
        try {
            setTitle(
                    gGo.getIGSResources().getString("Game") + " " +
                    gameID + " - " +
                    (board.getBoardHandler().getGameData().gameName == null ||
                    board.getBoardHandler().getGameData().gameName.length() == 0 ?
                    board.getBoardHandler().getGameData().playerWhite +
                    (board.getBoardHandler().getGameData().rankWhite != null &&
                    board.getBoardHandler().getGameData().rankWhite.length() > 0 ?
                    " " + board.getBoardHandler().getGameData().rankWhite : "") +
                    " " + gGo.getBoardResources().getString("vs.") + " " +
                    board.getBoardHandler().getGameData().playerBlack +
                    (board.getBoardHandler().getGameData().rankBlack != null &&
                    board.getBoardHandler().getGameData().rankBlack.length() > 0 ?
                    " " + board.getBoardHandler().getGameData().rankBlack : "") :
                    board.getBoardHandler().getGameData().gameName));
        } catch (NullPointerException e) {
            System.err.println("Failed to set title: " + e);
        }
    } //}}}

    //{{{ addKibitz() method
    /**
     *  Add a kibitz line to the kibitz textarea and save it in the currentKibitz StringBuffer
     *
     *@param  s  Kibitz String
     */
    public void addKibitz(String s) {
        currentKibitz.append(s + "\n");
        appendCommentText(s);
        writeCommentToSGF();
    } //}}}

    //{{{ clearComment() method
    /**  Clear the currentKibitz StringBuffer, called when doing a new move */
    public void clearComment() {
        currentKibitz = new StringBuffer();
    } //}}}

    //{{{ writeCommentToSGF() method
    /**  Write the currentKibitz StringBuffer as SGF comment */
    public void writeCommentToSGF() {
        try {
            Move m = board.getBoardHandler().getLastValidMove();
            if (m == null)
                m = board.getBoardHandler().getTree().getCurrent();
            m.setComment(currentKibitz.toString());
        } catch (NullPointerException ex) {}
    } //}}}

    //{{{ isFinished() method
    /**
     *  Gets the isFinished attribute of the IGSObserverFrame object
     *
     *@return    The finished value
     */
    public boolean isFinished() {
        return isFinished;
    } //}}}

    //{{{ setFinished() method
    /**  Sets the isFinished attribute of the IGSObserverFrame object */
    public void setFinished() {
        isFinished = true;
    } //}}}

    //{{{ displayInfo() method
    /**
     *  Display an info messagebox
     *
     *@param  txt  Description of the Parameter
     */
    public void displayInfo(final String txt) {
        final SwingWorker worker =
            new SwingWorker() {
                public Object construct() {
                    JOptionPane.showMessageDialog(IGSObserverFrame.this, txt,
                            gGo.getIGSResources().getString("Game") +
                            " " + +gameID, JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
            };
        worker.start();
        setFinished();
    } //}}}

    //{{{ getViewComm() method
    /**
     *  Gets the viewComm attribute of the IGSObserverFrame object
     *
     *@return    The viewComm value
     */
    public boolean getViewComm() {
        return viewComm.isSelected();
    } //}}}

    //{{{ printTimeStamp() method
    /**
     *  Print the move timestamp in the kibitz field
     *
     *@param  moveNum  Move number of this move
     */
    public void printTimeStamp(int moveNum) {
        clearComment();

        if (viewTimestamp.isSelected()) {
            int diff = (int)((System.currentTimeMillis() - lastMoveTimeStamp) / 1000);
            addKibitz("[" + gGo.getIGSResources().getString("Move") + " " + (moveNum + 1) + ": " +
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()) +
                    (diff >= 0 ? " - " + Utils.formatTime(diff / 60) + ":" + Utils.formatTime(diff % 60) + "]" : "]"));
        }
        lastMoveTimeStamp = System.currentTimeMillis();
    } //}}}

    //{{{ addInputTextfieldCoords() method
    /**
     *  Adds a coordinate to the inputTextField, when doubleclicking on the board.
     *
     *@param  x          X position
     *@param  y          Y position
     *@param  boardSize  boardsize
     */
    public void addInputTextfieldCoords(int x, int y, int boardSize) {
        if (x >= 1 && x <= boardSize && y >= 1 && y <= boardSize) {
            try {
                String txt = inputTextField.getText();
                int caretPos = inputTextField.getCaretPosition();

                inputTextField.setText(
                        (caretPos > 0 ? txt.substring(0, caretPos) : "") +
                        ((char)('A' + (x < 9 ? x : x + 1) - 1) + String.valueOf(boardSize - y + 1)) + " " +
                        (txt.length() - caretPos > 0 ? txt.substring(caretPos, txt.length()) : ""));
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to insert coordinates in inputTextField: " + e);
            }
        }
    } //}}}
}

