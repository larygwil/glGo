/*
 *  IGSPlayingFrame.java
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
import java.text.MessageFormat;
import java.text.NumberFormat;
import ggo.*;
import ggo.igs.*;
import ggo.utils.*;
import ggo.gui.*;

/**
 *  A subclass of PlayingMainFrame used for games on IGS
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.20 $, $Date: 2002/10/25 04:15:49 $
 */
public class IGSPlayingFrame extends PlayingMainFrame {
    //{{{ protected members
    /**  Description of the Field */
    protected int gameID;
    /**  Description of the Field */
    protected JTextField inputTextField;
    /**  Description of the Field */
    protected JComboBox kibitzChatterChooser;
    /**  Description of the Field */
    protected StringBuffer currentSay;
    /**  Description of the Field */
    protected long lastMoveTimeStamp;
    /**  Description of the Field */
    protected float komiRequest;
    /**  Description of the Field */
    protected JMenuItem handicapMenuItem;
    //}}}

    //{{{ IGSPlayingFrame() constructor
    /**
     *Constructor for the IGSPlayingFrame object
     *
     *@param  gameID       Game ID of this game
     *@param  playerColor  Playing this game as black or white
     */
    public IGSPlayingFrame(int gameID, int playerColor) {
        super();
        this.gameID = gameID;
        this.playerColor = playerColor;
        currentSay = new StringBuffer();
        komiRequest = -9999.999f;
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

    //{{{ initThinkingTime() method
    /**  Init thinking time */
    public void initThinkingTime() {
        lastMoveTimeStamp = System.currentTimeMillis();
    } //}}}

    //{{{ initMenus() method
    /**  Init the menus */
    protected void initMenus() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File menu
        menuBar.add(initFileMenu(FILE_SAVE | FILE_SAVE_AS | FILE_CLOSE));

        // Edit menu
        menuBar.add(initEditMenu(EDIT_IGS | EDIT_GAME | EDIT_GUESS_SCORE));

        // Settings menu
        menuBar.add(initSettingsMenu(SETTINGS_PREFERENCES | SETTINGS_GAME_INFO | SETTINGS_MEMORY_STATUS | SETTINGS_MEMORY_CLEANUP));

        // View menu
        menuBar.add(initViewMenu(VIEW_CLEAR | VIEW_STATUSBAR | VIEW_COORDS | VIEW_SIDEBAR | VIEW_CURSOR | VIEW_HORIZONTAL_COMMENT | VIEW_SAVE_SIZE));

        // Help menu
        menuBar.add(initHelpMenu());
    } //}}}

    //{{{ initIGSEditMenu() method
    /**
     *  Description of the Method
     *
     *@param  menu  Description of the Parameter
     */
    protected void initIGSEditMenu(JMenu menu) {
        JMenuItem menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        setMnemonicText(menuItem, gGo.getIGSResources().getString("Refresh_board"));
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        menuItem.setToolTipText(gGo.getIGSResources().getString("refresh_board_tooltip"));
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (IGSConnection.getGameHandler().playsGame(gameID)) {
                        IGSReader.requestRefresh = gameID;
                        IGSConnection.sendCommand("status " + gameID);
                        IGSConnection.sendCommand("refresh " + gameID);
                    }
                }
            });
        menu.add(menuItem);

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

        menu.addSeparator();

        menuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        setMnemonicText(menuItem, gGo.getIGSResources().getString("change_komi"));
        menuItem.setToolTipText(gGo.getIGSResources().getString("change_komi_tooltip"));
        menuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String s = JOptionPane.showInputDialog(
                            IGSPlayingFrame.this,
                            gGo.getIGSResources().getString("enter_komi"));
                    if (s != null && s.length() > 0) {
                        // Validate input
                        float komi;
                        try {
                            komi = Float.parseFloat(s);
                        } catch (NumberFormatException ex) {
                            System.err.println("Invalid komi value: " + ex);
                            JOptionPane.showMessageDialog(
                                    IGSPlayingFrame.this,
                                    MessageFormat.format(
                                    gGo.getIGSResources().getString("invalid_komi_request"),
                                    new Object[]{s}),
                                    gGo.getBoardResources().getString("error"),
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        IGSConnection.sendCommand("komi " + komi);
                        komiRequest = komi;
                    }
                }
            });
        menu.add(menuItem);

        handicapMenuItem = new JMenuItem(new ImageIcon(getClass().getResource("/images/Blank16.gif")));
        setMnemonicText(handicapMenuItem, gGo.getIGSResources().getString("change_handicap"));
        handicapMenuItem.setToolTipText(gGo.getIGSResources().getString("change_handicap_tooltip"));
        handicapMenuItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String s = JOptionPane.showInputDialog(
                            IGSPlayingFrame.this,
                            gGo.getIGSResources().getString("enter_handicap"));
                    if (s != null && s.length() > 0) {
                        // Validate input
                        int handicap;
                        try {
                            handicap = Integer.parseInt(s);
                            if (handicap < 2 || handicap > 9)
                                throw new NumberFormatException();
                        } catch (NumberFormatException ex) {
                            System.err.println("Invalid handicap value: " + ex);
                            JOptionPane.showMessageDialog(
                                    IGSPlayingFrame.this,
                                    MessageFormat.format(
                                    gGo.getIGSResources().getString("invalid_handicap_request"),
                                    new Object[]{s}),
                                    gGo.getBoardResources().getString("error"),
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        IGSConnection.sendCommand("handicap " + handicap);
                    }
                }
            });
        menu.add(handicapMenuItem);

        menu.addSeparator();
    } //}}}

    //{{{ initToolBar() method
    /**  Init the tool bar. Does nothing in this case. */
    protected void initToolBar() {
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
                gGo.getIGSResources().getString("Say"),
                gGo.getIGSResources().getString("Command"),
                gGo.getIGSResources().getString("Chatter")
                };
        kibitzChatterChooser = new JComboBox(args);
        kibitzChatterChooser.setToolTipText(gGo.getIGSResources().getString("say_command_chatter_tooltip"));

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
                    // When the game is marked as finished, send 'tell' instead 'say'
                    if (!isFinished)
                        sendIt = "say ";
                    else
                        sendIt = "tell " +
                                (playerColor == STONE_BLACK ? board.getBoardHandler().getGameData().playerWhite :
                                board.getBoardHandler().getGameData().playerBlack) + " ";
                    addSay(IGSConnection.getLoginName() + ": " + command);
                    break;
                case 1:
                    addSay("[Command] " + command);
                    break;
                case 2:
                    sendIt = "chatter " + gameID + " ";
                    addSay("[Chatter] " + IGSConnection.getLoginName() + ": " + command);
            }

            IGSMainWindow.getIGSConnection().recieveInput(sendIt + command);
        } catch (NullPointerException e) {
            System.err.println("Failed to send say to server: " + e);
        }

        inputTextField.setText("");
    } //}}}

    //{{{ closeFrame() method
    /**
     *  Close the frame, overwrites method of superclass.
     *  Unregisters itself at the IGSGameHandler object it belongs to.
     *
     *@return    Always true
     */
    public boolean closeFrame() {
        if (!isFinished &&
                gGo.hasIGSConnection() &&
                JOptionPane.showConfirmDialog(
                this,
                gGo.getIGSResources().getString("confirm_close_while_playing"),
                PACKAGE,
                JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
            return false;

        try {
            IGSConnection.getGameHandler().removeGame(gameID);
            IGSGameHandler.removeAdjournedGame(
                    new Couple(getBoard().getBoardHandler().getGameData().playerWhite,
                    getBoard().getBoardHandler().getGameData().playerBlack));
        } catch (NullPointerException e) {
            System.err.println("Failed to remove frame from playing handler: " + e);
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

    //{{{ doMove() method
    /**
     *  IGS has sent a move. Display it on the board.
     *
     *@param  color  Move color
     *@param  x      X position
     *@param  y      Y position
     */
    public void doMove(int color, int x, int y) {
        lastMoveTimeStamp = System.currentTimeMillis();
        checkHandicapMenuItem();
    } //}}}

    //{{{ moveDone() method
    /**
     *  Human player has done a move by clicking on the board. Send this move to IGS.
     *
     *@param  color  Move color
     *@param  x      X position
     *@param  y      Y position
     */
    public void moveDone(int color, int x, int y) {
        int thinking_time = (int)((System.currentTimeMillis() - lastMoveTimeStamp) / 1000);
        IGSConnection.getGameHandler().recieveMoveFromHuman(gameID, color, x, y, board.getBoardSize(), thinking_time);
        checkHandicapMenuItem();
    } //}}}

    //{{{ undoMove() method
    /**
     *  Send undo command to GTP engine
     *
     *@return    Number of moves to take back
     */
    public int undoMove() {
        IGSConnection.sendCommand("undo");
        if (board.getBoardHandler().getGameMode() == MODE_SCORE)
            return 0;
        return 1;
    } //}}}

    //{{{ notifyUndo() method
    /**  IGS sent an undo */
    public void notifyUndo() {
        checkHandicapMenuItem();
    } //}}}

    //{{{ checkHandicapMenuItem() method
    /**  Enable or disable the handicap menuitem */
    public void checkHandicapMenuItem() {
        if (playerColor == STONE_BLACK &&
                board.getBoardHandler().getTree().getCurrent().getMoveNumber() == 0)
            handicapMenuItem.setEnabled(true);
        else if (handicapMenuItem.isEnabled())
            handicapMenuItem.setEnabled(false);
    } //}}}

    //{{{ doResign() method
    /**
     *  Resign game
     *
     *@param  color  Color of the resigning player
     */
    public void doResign(int color) {
        if (JOptionPane.showOptionDialog(
                this,
                gGo.getIGSResources().getString("confirm_resign"),
                PACKAGE,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{gGo.getIGSResources().getString("Yes_resign"), gGo.getIGSResources().getString("No_do_not_resign")},
                new String(gGo.getIGSResources().getString("No_do_not_resign"))) == JOptionPane.NO_OPTION)
            return;

        IGSConnection.sendCommand("resign");
    } //}}}

    //{{{ adjournGame() method
    /**  Send adjourn command to IGS */
    public void adjournGame() {
        IGSConnection.sendCommand("adjourn");
    } //}}}

    //{{{ mayMove() method
    /**
     *  Check if it is the turn of the given color in GTP/IGS game mode
     *
     *@param  color  Color to check
     *@return        True if we may move, else false
     */
    public boolean mayMove(int color) {
        return (color == playerColor || board.getBoardHandler().getGameMode() == MODE_SCORE) && !isFinished;
    } //}}}

    //{{{ doDone() method
    /**  Send done to IGS */
    public void doDone() {
        IGSConnection.sendCommand("done");
    } //}}}

    //{{{ addSay() method
    /**
     *  Add a say line to the comment textarea and save it in the currentSay StringBuffer
     *
     *@param  s  Say String
     */
    public void addSay(String s) {
        currentSay.append(s + "\n");
        appendCommentText(s);
        writeCommentToSGF();
    } //}}}

    //{{{ clearComment() method
    /**  Clear the currentSay StringBuffer, called when doing a new move */
    public void clearComment() {
        currentSay = new StringBuffer();
    } //}}}

    //{{{ writeCommentToSGF() method
    /**  Write the currentSay StringBuffer as SGF comment */
    public void writeCommentToSGF() {
        try {
            board.getBoardHandler().getTree().getCurrent().setComment(currentSay.toString());
        } catch (NullPointerException ex) {}
    } //}}}

    //{{{ displayAdjournRequest() method
    /**  Opponent asks for adjourn */
    public void displayAdjournRequest() {
        final SwingWorker worker =
            new SwingWorker() {
                public Object construct() {
                    if (JOptionPane.showOptionDialog(
                            IGSPlayingFrame.this,
                            gGo.getIGSResources().getString("adjourn_request"),
                            PACKAGE,
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new String[]{gGo.getIGSResources().getString("yes_adjourn"), gGo.getIGSResources().getString("no_do_not_adjourn")},
                            new String(gGo.getIGSResources().getString("no_do_not_adjourn"))) == JOptionPane.YES_OPTION) {
                        setFinished();
                        IGSConnection.sendCommand("adjourn");
                    }
                    return null;
                }
            };
        worker.start(); //required for SwingWorker 3
    } //}}}

    //{{{ toggleAdjournReload() method
    /**
     *  Toggle adjourn button to reload, or vice versa
     *
     *@param  toggle  True:  adjourn -> reload
     *                False: reload -> adjourn
     *@see            ggo.gui.PlayButtonBar#toggleAdjournReload(boolean)
     */
    public void toggleAdjournReload(boolean toggle) {
        ((PlayButtonBar)(sideBar.getButtonBar())).toggleAdjournReload(toggle);
    } //}}}

    //{{{ reloadGame() method
    /**  Reload a game while the board is opened in adjourned state */
    public void reloadGame() {
        IGSConnection.sendCommand(
                "load " +
                board.getBoardHandler().getGameData().playerWhite + "-" +
                board.getBoardHandler().getGameData().playerBlack);
    } //}}}

    //{{{ displayKomiRequest() method
    /**
     *  Opponent requested komi change, display a messagebox to the user
     *
     *@param  opponent  Name of opponent
     *@param  komi      Requested komi value
     *@return           True if user accepted, false if refused
     */
    public boolean displayKomiRequest(String opponent, float komi) {
        if (JOptionPane.showOptionDialog(
                this,
                MessageFormat.format(
                gGo.getIGSResources().getString("komi_change_requested"),
                new Object[]{opponent, NumberFormat.getNumberInstance(gGo.getLocale()).format(komi)}),
                gGo.getIGSResources().getString("Komi"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{gGo.getIGSResources().getString("ok_change_komi"), gGo.getIGSResources().getString("no_do_not_change_komi")},
                new String(gGo.getIGSResources().getString("ok_change_komi"))) == JOptionPane.YES_OPTION) {
            IGSConnection.sendCommand("komi " + komi);
            komiRequest = komi;
            return true;
        }
        return false;
    } //}}}

    //{{{ didKomiRequest() method
    /**
     *  Check if we sent a komi change request from this game
     *
     *@return    Requested komi. -9999.999f if not requested.
     */
    public float didKomiRequest() {
        float tmp = komiRequest;
        komiRequest = -9999.999f;
        return tmp;
    } //}}}

    //{{{ setKomiRequest() method
    /**
     *  Mark we requested a komi change for this game
     *
     *@param  f  The new komi value
     */
    public void setKomiRequest(float f) {
        komiRequest = f;
    } //}}}
}

