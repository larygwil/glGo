/*
 *  GTPGameHandler.java
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

import ggo.*;
import ggo.gtp.*;
import java.util.ArrayList;

/**
 *  This class manages the flow of a game using a GTP engine.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class GTPGameHandler implements Defines, GTPDefines {
    //{{{ private members
    private int state, turn, boardSize, handicap, black, white, lastState;
    private float komi;
    private boolean lastMoveWasPass;
    private String resumeFileName;
    //}}}

    //{{{ GTPGameHandler() constructor
    /** Constructor for the GTPGameHandler object */
    public GTPGameHandler() {
        state = lastState = STATE_UNKNOWN;
        lastMoveWasPass = false;
    } //}}}

    //{{{ getState() method
    /**
     *  Gets the current state of the gamae
     *
     *@return    The state value
     */
    protected int getState() {
        return state;
    } //}}}

    //{{{ mayMove() method
    /**
     *  Check if it is the turn of the given color in GTP/IGS game mode
     *
     *@param  color  Color to check
     *@return        True if we may move, else false
     */
    public boolean mayMove(int color) {
        if (state == STATE_SCORING)
            return false;
        if (state == STATE_DONE)
            return false;
        if (color == STONE_BLACK)
            return black == GTP_HUMAN;
        return white == GTP_HUMAN;
    } //}}}

    //{{{ getBoardSize() method
    /**
     *  Gets the board size of the current game
     *
     *@return    The board size value
     */
    public int getBoardSize() {
        return boardSize;
    } //}}}

    //{{{ initGame() method
    /**
     *  Init and start a new game.
     *
     *@param  gtpConfig  Pointer to the GTP configuration object
     */
    public void initGame(GTPConfig gtpConfig) {
        // Init game values
        this.boardSize = gtpConfig.getSize();
        this.handicap = gtpConfig.getHandicap();
        this.komi = gtpConfig.getKomi();
        this.black = gtpConfig.getBlack();
        this.white = gtpConfig.getWhite();

        // Init clocks
        GTPConnection.getBoard().getClockWhite().init(gtpConfig.getMainTime(),
                gtpConfig.getByoYomiTime(), gtpConfig.getByoYomiStones());
        GTPConnection.getBoard().getClockBlack().init(gtpConfig.getMainTime(),
                gtpConfig.getByoYomiTime(), gtpConfig.getByoYomiStones());
        if (black == GTP_HUMAN)
            GTPConnection.getBoard().getClockBlack().setIsMyClock(true);
        if (white == GTP_HUMAN)
            GTPConnection.getBoard().getClockWhite().setIsMyClock(true);
        // TODO: Adjust clocks for resume. Needs SGF clock tags. Not supported yet.

        lastMoveWasPass = false;

        // New game
        if ((resumeFileName = gtpConfig.getResumeFileName()) == null) {
            GTPConnection.sendCommand("boardsize " + boardSize);
            GTPConnection.sendCommand("komi " + komi);
            GTPConnection.sendCommand("level " + gtpConfig.getLevel());

            if (handicap < 2) {
                turn = STONE_BLACK;
            }
            else {
                turn = STONE_WHITE;
                state = STATE_SETUP_HANDICAP;
                GTPConnection.sendCommand("fixed_handicap " + handicap);
            }

            startGame();
        }

        // Resumed game
        else
            GTPConnection.getBoard().openSGF(resumeFileName, true);
    } //}}}

    //{{{ resumeGame() method
    /**
     *  Resume a game, called after opening the SGF file in GTPMainFrame.openSGF()
     *
     *@param  loaded  True if game was loaded successful, else false
     */
    void resumeGame(boolean loaded) {
        if (loaded && resumeFileName != null) {
            GTPConnection.getBoard().getBoard().getBoardHandler().gotoLastMove(true);
            state = STATE_RESUME_GAME;
            GTPConnection.sendCommand("loadsgf " + resumeFileName);
            // GTP will send which turn it is and call startGame(boolean)
        }
        // Loading failed, start new game
        else {
            turn = STONE_BLACK;
            startGame();
        }
    } //}}}

    //{{{ startGame(boolean) method
    /**
     *  Start the game and set the turn
     *
     *@param  blackTurn  True if blacks turn, false if whites turn
     */
    void startGame(boolean blackTurn) {
        if (blackTurn)
            turn = STONE_BLACK;
        else
            turn = STONE_WHITE;
        startGame();
    } //}}}

    //{{{ startGame() method
    /**  Start the game. Turn is already set. */
    private void startGame() {
        // Wait a bit, then start game
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}

        // Start clock
        if (turn == STONE_BLACK)
            GTPConnection.getBoard().getClockBlack().start();
        else
            GTPConnection.getBoard().getClockWhite().start();

        generateMove();
    } //}}}

    //{{{ generateMove() method
    /**  Tell GTP engine to generate a move. Color is calculated automatically. */
    public void generateMove() {
        if (turn == STONE_WHITE) {
            state = STATE_MOVE_WHITE;

            if (white == GTP_COMPUTER)
                GTPConnection.sendCommand("genmove_white");
        }
        else {
            state = STATE_MOVE_BLACK;

            if (black == GTP_COMPUTER)
                GTPConnection.sendCommand("genmove_black");
        }
    } //}}}

    //{{{ switchTurn() method
    /**
     *  Switch turns after a move was made
     *
     *@return    Color of the turn after the switch
     */
    private int switchTurn() {
        int color = STONE_NONE;

        if (turn == STONE_BLACK) {
            color = STONE_BLACK;
            turn = STONE_WHITE;
            state = STATE_MOVE_BLACK;
            GTPConnection.getBoard().getClockBlack().stop();
            if (!GTPConnection.getBoard().getClockBlack().playStone()) {
                System.err.println("TIMEOUT BLACK");
                // TODO
            }
            // else
            GTPConnection.getBoard().getClockWhite().start();
        }
        else {
            color = STONE_WHITE;
            turn = STONE_BLACK;
            state = STATE_MOVE_WHITE;
            GTPConnection.getBoard().getClockWhite().stop();
            if (!GTPConnection.getBoard().getClockWhite().playStone()) {
                System.err.println("TIMEOUT WHITE");
                // TODO
            }
            // else
            GTPConnection.getBoard().getClockBlack().start();
        }

        return color;
    } //}}}

    //{{{ recieveMoveFromHuman() method
    /**
     *  Human player has done a move. Forward it to the GTP engine and generate next move.
     *
     *@param  color  Color of the played move
     *@param  x      X coordinate of the move
     *@param  y      Y coordinate of the move
     */
    public void recieveMoveFromHuman(int color, int x, int y) {
        System.err.println("HUMAN MOVE: " + x + "/" + y);

        // Verify turn
        if (color != turn) {
            System.err.println("Error: Turn mismatches!");
            return;
        }

        // Send move to GTP
        String command = "";
        if (color == STONE_WHITE)
            command = "white ";
        else if (color == STONE_BLACK)
            command = "black ";
        if (x == -1 && y == -1)
            command += "pass";
        else
            command += (char)('A' + (x < 9 ? x : x + 1) - 1) + String.valueOf(boardSize - y + 1);
        GTPConnection.sendCommand(command);

        switchTurn();

        if (!checkGameFinished(x, y))
            generateMove();
    } //}}}

    //{{{ recieveMoveFromGTP() method
    /**
     *  The GTP engine has done a move. Forward it to the board and generate next move.
     *
     *@param  x  X coordinate of the move
     *@param  y  Y coordinate of the move
     */
    public void recieveMoveFromGTP(int x, int y) {
        System.err.println("GTP MOVE: " + x + "/" + y);

        GTPConnection.getBoard().doMove(switchTurn(), x, y);

        if (!checkGameFinished(x, y))
            generateMove();
    } //}}}

    //{{{ recieveHandicapFromGTP() method
    /**
     *  GTP engine wants to setup handicap stones
     *
     *@param  handicap  Vector of Positions
     */
    public void recieveHandicapFromGTP(ArrayList handicap) {
        GTPConnection.getBoard().getBoard().getBoardHandler().setHandicap(handicap);
        GTPConnection.getBoard().getBoard().setPositionModified(true);
        GTPConnection.getBoard().getBoard().repaint();
        state = STATE_MOVE_WHITE;
    } //}}}

    //{{{ checkGameFinished() method
    /**
     *  Check if the game has finished by passing twice in a row.
     *  If the game has ended, the interface switched to scoring.
     *
     *@param  x  X coordinate of last move. -1 if pass
     *@param  y  Y coordinate of last move. -1 if pass
     *@return    True if game has ended, else false.
     */
    private boolean checkGameFinished(int x, int y) {
        lastState = state;

        if (x == -1 && y == -1) {
            if (lastMoveWasPass) {
                state = STATE_SCORING;
                GTPConnection.getBoard().switchMode(MODE_SCORE);
                // Stop clocks
                GTPConnection.getBoard().getClockBlack().stop();
                GTPConnection.getBoard().getClockWhite().stop();
                return true;
            }
            lastMoveWasPass = true;
        }
        else
            lastMoveWasPass = false;
        return false;
    } //}}}

    //{{{ undoMove() method
    /**
     *  Undo one move
     *
     *@return    Number of moves taken back
     */
    public int undoMove() {
        int counter = 1;

        if (black == GTP_COMPUTER || white == GTP_COMPUTER)
            counter = 2;

        for (int i = 0; i < counter; i++)
            GTPConnection.sendCommand("undo");

        state = lastState;

        return counter;
    } //}}}

    //{{{ finishGame() method
    /**  Mark the game as finished, so no further moves can be done. */
    protected void finishGame() {
        state = STATE_DONE;

        // Stop clocks
        GTPConnection.getBoard().getClockBlack().stop();
        GTPConnection.getBoard().getClockWhite().stop();
    } //}}}
}

