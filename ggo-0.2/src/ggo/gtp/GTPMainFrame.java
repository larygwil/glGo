/*
 *  GTPMainFrame.java
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
package ggo.gtp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.MessageFormat;
import ggo.*;
import ggo.gtp.*;
import ggo.gui.*;
import ggo.utils.*;

/**
 *  A subclass of PlayingMainFrame used for games using a GTP engine
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.7 $, $Date: 2002/09/21 12:39:55 $
 */
public class GTPMainFrame extends PlayingMainFrame implements GTPDefines {
    //{{{ protected members
    /**  MenuItem Show GTP console */
    protected JCheckBoxMenuItem viewShowGTP;
    //}}}

    //{{{ private members
    private static ResourceBundle gtp_resources;
    //}}}

    //{{{ static constructor
    static {
        gtp_resources = gGo.getGTPResources();
    } //}}}

    //{{{ GTPMainFrame() constructor
    /**Constructor for the GTPMainFrame object */
    public GTPMainFrame() {
        super();
    } //}}}

    //{{{ initMenus() method
    /**  Init the menus */
    protected void initMenus() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        // File menu
        menuBar.add(initFileMenu(FILE_NEW | FILE_SAVE | FILE_SAVE_AS | FILE_CLOSE | FILE_EXIT));

        // Edit menu
        menuBar.add(initEditMenu(EDIT_GAME | EDIT_GUESS_SCORE));

        // Settings menu
        menuBar.add(initSettingsMenu(SETTINGS_PREFERENCES | SETTINGS_GAME_INFO | SETTINGS_MEMORY_STATUS | SETTINGS_MEMORY_CLEANUP));

        // View menu
        menuBar.add(initViewMenu(VIEW_STATUSBAR | VIEW_COORDS | VIEW_SIDEBAR | VIEW_CURSOR | VIEW_HORIZONTAL_COMMENT | VIEW_SAVE_SIZE));

        // Help Menu
        menuBar.add(initHelpMenu());
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

        // View Show GTP window
        menu.addSeparator();
        viewShowGTP = new JCheckBoxMenuItem(gtp_resources.getString("Show_GTP_window"));
        viewShowGTP.setToolTipText(gtp_resources.getString("show_GTP_window_tooltip"));
        viewShowGTP.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED)
                        GTPError.toggleDebugWindow(true);
                    else if (e.getStateChange() == ItemEvent.DESELECTED)
                        GTPError.toggleDebugWindow(false);
                }
            });
        viewShowGTP.setSelected(false);
        menu.add(viewShowGTP);

        return menu;
    } //}}}

    //{{{ initToolBar() method
    /**  Init the tool bar. Does nothing in this case. */
    protected void initToolBar() {
    } //}}}

    //{{{ closeFrame() method
    /**
     *  Close this frame and stop thread. If it is the last frame, exit application
     *
     *@return    Always true
     */
    public boolean closeFrame() {
        gGo.closeGTPWindow();
        return super.closeFrame();
    } //}}}

    //{{{ newGame() method
    /**  Start a new GTP game, reusing the board */
    protected void newGame() {
        GTPSetupDialog dlg = new GTPSetupDialog(this, gGo.getSettings().getGTPConfig());
        dlg.setVisible(true);

        if (!dlg.getResult())
            return;

        GameData data = new GameData();
        data.playerBlack = dlg.getGTPConfig().getBlack() == GTP_COMPUTER ? gtp_resources.getString("Computer") : gtp_resources.getString("Human");
        data.playerWhite = dlg.getGTPConfig().getWhite() == GTP_COMPUTER ? gtp_resources.getString("Computer") : gtp_resources.getString("Human");
        data.size = dlg.getGTPConfig().getSize();
        data.komi = dlg.getGTPConfig().getKomi();
        data.handicap = dlg.getGTPConfig().getHandicap();

        // Stop clocks
        getClockBlack().stop();
        getClockWhite().stop();

        board.initGame(data, true);

        try {
            gGo.getGTP().init(dlg.getGTPConfig());
        } catch (NullPointerException e) {
            System.err.println("Failed to init new GTP game: " + e);
        }
    } //}}}

    //{{{ doMove() method
    /**
     *  GTP engine has sent a move. Display it on the board.
     *
     *@param  color  Move color
     *@param  x      X position
     *@param  y      Y position
     */
    public void doMove(int color, int x, int y) {
        // Validate position
        if ((color != STONE_BLACK && color != STONE_WHITE) ||
                (x != -1 && y != -1 &&
                (x < 1 || y < 1 || x > board.getBoardSize() || y > board.getBoardSize()))) {
            System.err.println("Invalid move: " + x + "/" + y);
            return;
        }

        board.doGTPMove(color, x, y);
    } //}}}

    //{{{ moveDone() method
    /**
     *  Human player has done a move by clicking on the board. Send this move to the GTP engine.
     *
     *@param  color  Move color
     *@param  x      X position
     *@param  y      Y position
     */
    public void moveDone(int color, int x, int y) {
        GTPConnection.getGTPGameHandler().recieveMoveFromHuman(color, x, y);
    } //}}}

    //{{{ undoMove() method
    /**
     *  Send undo command to GTP engine
     *
     *@return    Number of moves to take back
     */
    public int undoMove() {
        return GTPConnection.getGTPGameHandler().undoMove();
    } //}}}

    //{{{ doResign() method
    /**
     *  Resign game
     *
     *@param  color  Color of the resigning player
     */
    public void doResign(int color) {
        MessageFormat msgFormat = new MessageFormat(gtp_resources.getString("resign_message"));
        JOptionPane.showMessageDialog(this,
                gtp_resources.getString("Game_over") + "\n" +
                msgFormat.format(new Object[]
                {color == STONE_BLACK ? gGo.getBoardResources().getString("Black") : gGo.getBoardResources().getString("White")}),
                gtp_resources.getString("Game_over"),
                JOptionPane.INFORMATION_MESSAGE);

        GTPConnection.getGTPGameHandler().finishGame();
    } //}}}

    //{{{ mayMove() method
    /**
     *  Check if it is the turn of the given color in GTP/IGS game mode
     *
     *@param  color  Color to check
     *@return        True if we may move, else false
     */
    public boolean mayMove(int color) {
        return GTPConnection.getGTPGameHandler().mayMove(color) ||
                board.getBoardHandler().getGameMode() == MODE_SCORE;
    } //}}}

    //{{{ doDone() method
    /**  Finish game. Does nothing in this class. */
    public void doDone() { } //}}}

    //{{{ doGTPScore() method
    /**
     *  Tell GTP engine to score game when game is finished
     *
     *@return    True if game was finished, scoring started, else false.
     */
    public boolean doGTPScore() {
        if (GTPConnection.getGTPGameHandler().getState() == STATE_SCORING) {
            GTPConnection.sendCommand("final_score");
            return true;
        }
        return false;
    } //}}}

    //{{{ openSGF() method
    /**
     *  Load a file and resume this game. Overwrites openSGF in MainFrame
     *
     *@param  fileName  Filename to load from
     *@param  remName   If true, remember the filename
     *@see              MainFrame#openSGF(String, boolean)
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
                    GTPConnection.getGTPGameHandler().resumeGame(new Boolean(true).equals(get()));
                }
            };
        worker.start(); //required for SwingWorker 3
    } //}}}
}

