/*
 *  PlayingMainFrame.java
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
import ggo.*;
import ggo.gui.*;
import ggo.utils.*;

/**
 *  Abstract superclass of a board frame that is used for playing games.
 *  Subclasses have to implement connectivity to an engine like IGS or GTP
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:55 $
 */
public abstract class PlayingMainFrame extends MainFrame {
    //{{{ protected members
    /**  Color we are playing this game as */
    protected int playerColor;
    /**  Flag, true if the game has ended */
    protected boolean isFinished;
    //}}}

    //{{{ PlayingMainFrame() constructor
    /**Constructor for the PlayingMainFrame object */
    public PlayingMainFrame() {
        super();
        switchMode(MODE_NORMAL);
        isPlayingFrame = true;
        playerColor = STONE_NONE;
        sliderPanel.setVisible(false);
        isFinished = false;
    } //}}}

    //{{{ closeFrame() method
    /**
     *  Close this frame and stop thread. If it is the last frame, exit application
     *
     *@return    Always true
     */
    public boolean closeFrame() {
        setVisible(false);
        dispose();
        getClockBlack().stop();
        getClockWhite().stop();
        if (mirrorFrame != null)
            mirrorFrame.setSynchFrame(null);
        runMe = false;
        if (gGo.getNumberOfOpenFrames() == 0 && !gGo.hasStartUpFrame())
            gGo.exitApp(0);
        return true;
    } //}}}

    //{{{ updateCaption() method
    /**  Update the frame title */
    public void updateCaption() {
        // Print caption
        // example: Zotan 8k vs. tgmouse 10k
        // or if game name is given: gGo Kogo's Joseki Dictionary
        try {
            setTitle(
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

    //{{{ getPlayerColor() method
    /**
     *  Gets the color we are playing in this game
     *
     *@return    The playerColor value
     */
    public int getPlayerColor() {
        return playerColor;
    } //}}}

    //{{{ isFinished() method
    /**
     *  Gets the isFinished attribute of the PlayingMainFrame object
     *
     *@return    The finished value
     */
    public boolean isFinished() {
        return isFinished;
    } //}}}

    //{{{ setFinished() method
    /**  Sets the isFinished attribute of the PlayingMainFrame object */
    public void setFinished() {
        isFinished = true;
    } //}}}

    //{{{ switchMode() method
    /**
     *  Description of the Method
     *
     *@param  mode  Description of the Parameter
     */
    public void switchMode(int mode) {
        sideBar.switchMode(mode);
        board.getBoardHandler().setGameMode(mode);
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
                    JOptionPane.showMessageDialog(PlayingMainFrame.this, txt);
                    return null;
                }
            };
        worker.start();
        setFinished();
    } //}}}

    //{{{ abstract methods

    //{{{ initMenus() method
    /**  Init the menus */
    protected abstract void initMenus(); //}}}

    //{{{ initToolBar() method
    /**  Init the tool bar */
    protected abstract void initToolBar(); //}}}

    //{{{ doMove() method
    /**
     *  Engine has sent a move. Display it on the board.
     *
     *@param  color  Move color
     *@param  x      X position
     *@param  y      Y position
     */
    public abstract void doMove(int color, int x, int y); //}}}

    //{{{ moveDone() method
    /**
     *  Human player has done a move by clicking on the board. Send this move to the engine.
     *
     *@param  color  Move color
     *@param  x      X position
     *@param  y      Y position
     */
    public abstract void moveDone(int color, int x, int y); //}}}

    //{{{ undoMove() method
    /**
     *  Send undo command to engine
     *
     *@return    Number of moves to take back
     */
    public abstract int undoMove(); //}}}

    //{{{ doResign() method
    /**
     *  Resign the game
     *
     *@param  color  Color of the resigning player
     */
    public abstract void doResign(int color); //}}}

    //{{{ doDone() method
    /**  Finish game */
    public abstract void doDone(); //}}}

    //{{{ mayMove() method
    /**
     *  Check if it is the turn of the given color
     *
     *@param  color  Color to check
     *@return        True if we may move, else false
     */
    public abstract boolean mayMove(int color); //}}}

    //}}}
}

