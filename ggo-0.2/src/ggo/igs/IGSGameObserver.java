/*
 *  IGSGameObserver.java
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
package ggo.igs;

import java.util.*;
import java.text.MessageFormat;
import ggo.*;
import ggo.igs.*;
import ggo.igs.gui.*;

/**
 *  Handler that stores the observed games and distributes the game data to the specific board.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.21 $, $Date: 2002/10/26 00:49:02 $
 */
public class IGSGameObserver extends Hashtable {
    //{{{ private members
    private int lastGameID, lastUndoID, lastAdjournID;
    private Hashtable adjournedBoards;
    //}}}

    //{{{ IGSGameObserver constructor
    /**Constructor for the IGSGameObserver object */
    public IGSGameObserver() {
        adjournedBoards = new Hashtable();
        lastGameID = lastUndoID = lastAdjournID = -1;
    } //}}}

    //{{{ observesGame() method
    /**
     *  Check if a certain game is observed
     *
     *@param  id  Game id to check
     *@return     True if the game is observed, else false
     */
    public boolean observesGame(int id) {
        return containsKey(new Integer(id));
    } //}}}

    //{{{ initGame() method
    /**
     *  Add a game to the observer list and open a board frame.
     *
     *@param  gameID  Id of the observed game
     *@return         True if successful, else false
     */
    public boolean initGame(int gameID) {
        if (observesGame(gameID)) {
            System.err.println("initGame(): Already observing this game.");
            return false;
        }

        MainFrame mf = null;
        try {
            mf = new IGSObserverFrame(gameID);
            Thread t = new Thread(mf);
            t.start();
        } catch (NullPointerException e) {
            System.err.println("Failed to open board window: " + e);
            return false;
        }
        put(new Integer(gameID), mf);

        lastGameID = gameID;

        return true;
    } //}}}

    //{{{ closeFrame() method
    /**
     *  Close a board frame, called when "unobserve" command is typed
     *
     *@param  gameID  ID of game we want to unobserve
     */
    public void closeFrame(int gameID) {
        if (!observesGame(gameID)) {
            System.err.println("closeFrame(): I am not observing game with id " + gameID);
            return;
        }

        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(gameID));
            mf.closeFrame();
        } catch (NullPointerException e) {
            System.err.println("Failed to close board window: " + e);
        }
    } //}}}

    //{{{ closeAllFrames() method
    /**  Close all observed frames, called when typing "unobserve" */
    public void closeAllFrames() {
        if (isEmpty())
            return;
        for (Enumeration e = keys(); e.hasMoreElements(); ) {
            Integer gameID = (Integer)e.nextElement();
            try {
                IGSObserverFrame mf = (IGSObserverFrame)get(gameID);
                mf.setFinished();
                mf.closeFrame();
            } catch (NullPointerException ex) {
                System.err.println("Failed to close board window: " + ex);
            }
        }
        clear();
    } //}}}

    //{{{ removeGame() method
    /**
     *  Remove a game from the observers list and send unobserve command to IGS
     *
     *@param  gameID  ID of game we want to unobserve
     */
    public void removeGame(int gameID) {
        System.err.println("IGSGameObserver.removeGame() " + gameID);

        if (!observesGame(gameID)) {
            System.err.println("removeGame(): I don't observe game " + gameID);
            return;
        }

        // Only send unobserve if the game is not finished
        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(gameID));
            if (!mf.isFinished())
                IGSConnection.sendCommand("unobserve " + gameID);
        } catch (NullPointerException e) {
            IGSConnection.sendCommand("unobserve " + gameID);
        }

        remove(new Integer(gameID));
        IGSConnection.getGamesTable().unobserveGame(gameID);

        if (IGSConnection.getAutoUpdater().hasGame(gameID))
            IGSConnection.getAutoUpdater().getGame(gameID).setObserved(false);
    } //}}}

    //{{{ doMoves() method
    /**
     *  Description of the Method
     *
     *@param  moves  Description of the Parameter
     *@return        Description of the Return Value
     */
    public synchronized boolean doMoves(ArrayList moves) {
        if (moves == null)
            return false;

        // Ignore move IGS sends after an undo
        try {
            if (lastUndoID == ((IGSMove)moves.get(0)).gameID) {
                lastUndoID = -1;
                return false;
            }
        } catch (IndexOutOfBoundsException e) {
            return false;
        }

        int sz = moves.size();
        int counter = 0;
        boolean playSound = sz-- <= 1;
        boolean disabled = gGo.getSettings().getIGSDisplayMoves();
        for (Iterator it = moves.iterator(); it.hasNext(); ) {
            IGSMove m = (IGSMove)it.next();
            if (!doMove(m, playSound,
                    (playSound || disabled ? 0 : (counter == 0 ? 1 : (counter == sz ? 2 : 0)))))
                return false;
            counter++;
        }
        return true;
    } //}}}

    //{{{ doMove() method
    /**
     *  Description of the Method
     *
     *@param  move       Description of the Parameter
     *@param  playSound  Description of the Parameter
     *@param  state      Description of the Parameter
     *@return            Description of the Return Value
     */
    private synchronized boolean doMove(IGSMove move, boolean playSound, int state) {
        if (!observesGame(move.gameID)) {
            System.err.println("doMove(): I don't observe game " + move.gameID);
            return false;
        }

        // MOVE DEBUG
        if (ggo.Defines.moveDebug)
            System.err.println("IGSGameObserver.doMove()\n" + move);

        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(move.gameID));

            // Init clocks if not yet done
            if (!mf.getClockWhite().isIGSClock()) {
                mf.getClockWhite().init(move.whiteTime);
                mf.getClockBlack().init(move.blackTime);
            }

            // Several moves, first move - lock board
            if (state == 1)
                mf.getBoard().lock();

            mf.getBoard().doIGSMove(move.color, move.x, move.y, move.captures, playSound, move.whiteTime, move.blackTime);
            mf.printTimeStamp(move.moveNum);

            // Several moves, last move - unlock and update board
            if (state == 2) {
                mf.getBoard().unlock();
                mf.getBoard().setPositionModified(true);
                mf.getBoard().repaint();
            }
        } catch (NullPointerException e) {
            System.err.println("Failed to send move to board: " + e);
            return false;
        }

        return true;
    } //}}}

    //{{{ setGameInfo() method
    /**
     *  Sets the gameInfo attribute of the IGSGameObserver object
     *
     *@param  gameData  The new gameInfo value
     *@return           Description of the Return Value
     */
    public int setGameInfo(GameData gameData) {
        if (lastGameID == -1)
            return -1;

        try {
            MainFrame mf = (MainFrame)get(new Integer(lastGameID));
            // Keep title in trailed games
            String gn = mf.getBoard().getBoardHandler().getGameData().gameName;
            if (gn != null && gn.length() > 0)
                gameData.gameName = gn;
            mf.getBoard().initGame(gameData, false);
        } catch (NullPointerException e) {
            System.err.println("Failed to send game info: " + e);
        }
        int tmp = lastGameID;
        lastGameID = -1;
        return tmp;
    } //}}}

    //{{{ writeKibitz() method
    /**
     *  Description of the Method
     *
     *@param  gameID  Description of the Parameter
     *@param  s       Description of the Parameter
     */
    public void writeKibitz(int gameID, String s) {
        if (!observesGame(gameID)) {
            System.err.println("writeKibitz(): I don't observe game " + gameID);
            return;
        }

        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(gameID));
            mf.addKibitz(s);
        } catch (NullPointerException e) {
            System.err.println("Failed to send kibitz: " + e);
        }
    } //}}}

    //{{{ getBoardSize() method
    /**
     *  Gets the boardSize attribute of the IGSGameObserver object
     *
     *@param  gameID  Description of the Parameter
     *@return         The boardSize value
     */
    public int getBoardSize(int gameID) {
        if (!observesGame(gameID)) {
            // System.err.println("getBoardSize(): I don't observe game " + gameID);
            return -1;
        }

        int s = -1;
        try {
            MainFrame mf = (MainFrame)get(new Integer(gameID));
            s = mf.getBoard().getBoardSize();
        } catch (NullPointerException e) {
            System.err.println("Failed to get board size: " + e);
        }
        return s;
    } //}}}

    //{{{ saveResult() method
    /**
     *  Description of the Method
     *
     *@param  gameID      Description of the Parameter
     *@param  result      Description of the Parameter
     *@param  autoupdate  Description of the Parameter
     */
    public void saveResult(int gameID, String result, boolean autoupdate) {
        if (!observesGame(gameID)) {
            System.err.println("saveResult(): I don't observe game " + gameID);
            return;
        }

        // Translate resign and time messages
        String msg = result;
        boolean adjournResignFlag = true;
        try {
            int pos;
            // Resign?
            if ((pos = result.indexOf(" resigns.")) != -1) {
                String name = result.substring(0, pos).trim();
                if (name.equals("Black"))
                    name = gGo.getIGSResources().getString("Black");
                else if (name.equals("White"))
                    name = gGo.getIGSResources().getString("White");
                msg = MessageFormat.format(
                        gGo.getIGSResources().getString("player_resigned_message_observe"), new Object[]{name});
            }
            // Timeout?
            else if ((pos = result.indexOf(" forfeits on time.")) != -1) {
                String name = result.substring(0, pos).trim();
                if (name.equals("Black"))
                    name = gGo.getIGSResources().getString("Black");
                else if (name.equals("White"))
                    name = gGo.getIGSResources().getString("White");
                msg = MessageFormat.format(
                        gGo.getIGSResources().getString("out_of_time_message_observe"), new Object[]{name});
            }
            else
                adjournResignFlag = false;  // Normal score
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to translate result message: " + result + "\n" + e);
            msg = result;
        }

        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(gameID));
            mf.addKibitz(msg);
            mf.getBoard().getBoardHandler().getGameData().result = msg;
            mf.setFinished();
            if (gGo.getSettings().getIGSDisplayInfo())
                mf.displayInfo(msg);

            // Stop clocks
            mf.getClockBlack().stop();
            mf.getClockWhite().stop();

            // Remove from list (don't if this is called from AutoUpdater and scored)
            if (!autoupdate || adjournResignFlag)
                removeGame(gameID);
        } catch (NullPointerException e) {
            System.err.println("Failed to send result to board: " + e);
        }
    } //}}}

    //{{{ peekBoard() method
    /**
     *  Description of the Method
     *
     *@param  gameID          Description of the Parameter
     *@param  matrix          Description of the Parameter
     *@param  clearDeadMarks  If true, remove the dead/seki marks from the stoneHandler.
     *@param  capsBlack       Description of the Parameter
     *@param  capsWhite       Description of the Parameter
     *@param  komi            Description of the Parameter
     */
    public synchronized void peekBoard(int gameID, Matrix matrix, boolean clearDeadMarks, int capsBlack, int capsWhite, float komi) {
        if (!observesGame(gameID)) {
            System.err.println("peekBoard(): I don't observe game " + gameID);
            return;
        }

        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(gameID));

            mf.getBoard().getBoardHandler().gotoLastMove(false);
            mf.getBoard().getBoardHandler().getTree().getCurrent().setMatrix(matrix);
            if (clearDeadMarks)
                mf.getBoard().getBoardHandler().getStoneHandler().removeDeadMarks();
            mf.getBoard().getBoardHandler().getTree().getCurrent().setCaptures(capsBlack, capsWhite);
            // Adjust komi if necassary
            if (komi != mf.getBoard().getBoardHandler().getGameData().komi) {
                mf.getBoard().getBoardHandler().getGameData().komi = komi;
                mf.setGameInfo(
                        mf.getBoard().getBoardHandler().getGameData().playerWhite,
                        mf.getBoard().getBoardHandler().getGameData().playerBlack,
                        mf.getBoard().getBoardHandler().getGameData().handicap,
                        komi);
                System.err.println("IGSGameObserver.peekBoard(): Adjusted komi to " + komi);
            }
            mf.getBoard().getBoardHandler().getStoneHandler().updateAll(mf.getBoard().getBoardHandler().getTree().getCurrent().getMatrix(), true);
            mf.getBoard().setPositionModified(true);
            mf.getBoard().repaint();
            mf.getBoard().getBoardHandler().updateGUI();

            // If this is called with toggle-quiet-true, we need to remove the game from the list here
            if (mf.isFinished())
                removeGame(gameID);
        } catch (NullPointerException e) {
            System.err.println("Failed to update board position: " + e);
        }
    } //}}}

    //{{{ adjournGame() method
    /**
     *  Game has been adjourned
     *
     *@param  gameID  Game ID
     */
    public void adjournGame(int gameID) {
        if (!observesGame(gameID)) {
            System.err.println("adjournGame(): I don't observe game " + gameID);
            return;
        }

        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(gameID));
            mf.addKibitz(gGo.getIGSResources().getString("adjourn_message"));
            if (gGo.getSettings().getIGSDisplayInfo())
                mf.displayInfo(gGo.getIGSResources().getString("adjourn_message"));

            // Stop clocks
            mf.getClockBlack().stop();
            mf.getClockWhite().stop();

            mf.setFinished();

            // Remove from list, but remember this board in case we want to reload
            // if the game gets continued
            removeGame(gameID);
            adjournedBoards.put(
                    new Couple(
                    mf.getBoard().getBoardHandler().getGameData().playerWhite,
                    mf.getBoard().getBoardHandler().getGameData().playerBlack),
                    mf);
            lastAdjournID = gameID;
        } catch (NullPointerException e) {
            System.err.println("Failed to send adjourn info: " + e);
        }
    } //}}}

    //{{{ getLastAdjournID() method
    /**
     *  Gets the game ID of the last adjourned game
     *
     *@return    The last adjourned game ID
     */
    public int getLastAdjournID() {
        int n = lastAdjournID;
        lastAdjournID = 0;
        return n;
    } //}}}

    //{{{ removeAdjournedGame() method
    /**
     *  Remove a game from the adjourned Hashtable
     *
     *@param  couple  Key to remove
     */
    public void removeAdjournedGame(Couple couple) {
        adjournedBoards.remove(couple);
    } //}}}

    //{{{ willReloadGame() method
    /**
     *  Check if a game of the given player couple would be reloaded. Required to prevent trail
     *
     *@param  c  Couple to check
     *@return    True if the game will be loaded, else false
     */
    public boolean willReloadGame(Couple c) {
        return adjournedBoards.containsKey(c);
    } //}}}

    //{{{ reloadGame() method
    /**
     *  Reobserve a formerly observed game that was adjourned and reloaded
     *
     *@param  gameID  Game ID
     *@param  couple  Playername couple
     */
    public void reloadGame(int gameID, Couple couple) {
        System.err.println("Reloading game " + gameID + " " + couple);

        if (adjournedBoards.containsKey(couple)) {
            IGSObserverFrame mf = (IGSObserverFrame)adjournedBoards.get(couple);
            if (mf != null) {
                // Adjust gameID in board
                mf.setGameID(gameID);
                put(new Integer(gameID), mf);
                lastGameID = gameID;
                mf.addKibitz(gGo.getIGSResources().getString("reload_message"));
            }
            else
                initGame(gameID);
            removeAdjournedGame(couple);
        }
        else
            initGame(gameID);
    } //}}}

    //{{{ setGameTitle() method
    /**
     *  Sets the game title
     *
     *@param  gameID  Game ID
     *@param  title   Game title string
     *@return         True if game is observed, else false
     */
    public boolean setGameTitle(int gameID, String title) {
        if (!observesGame(gameID))
            return false;

        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(gameID));
            String gn = mf.getBoard().getBoardHandler().getGameData().gameName;
            if (gn == null || gn.length() == 0 || !title.equals(gn)) {
                mf.getBoard().getBoardHandler().getGameData().gameName = title;
                mf.updateCaption();
            }
        } catch (NullPointerException e) {
            System.err.println("Failed to send game title: " + e);
        }
        return true;
    } //}}}

    //{{{ doUndo() method
    /**
     *  Undo the last move
     *
     *@param  gameID  Current game ID
     *@param  txt     Description of the Parameter
     *@return         Description of the Return Value
     */
    public synchronized boolean doUndo(int gameID, String txt) {
        if (!observesGame(gameID)) {
            System.err.println("doUndo(): I don't observe game " + gameID);
            return false;
        }

        try {
            IGSObserverFrame mf = (IGSObserverFrame)get(new Integer(gameID));
            // If we were not in the last move, ignore the refresh move IGS will send next
            if (mf.getBoard().getBoardHandler().silentDeleteNode())
                lastUndoID = gameID;
            mf.addKibitz(txt);
        } catch (NullPointerException e) {
            System.err.println("Failed to update board position for undo: " + e);
        }
        return true;
    } //}}}

    //{{{ openLookBoard() method
    /**
     *  Open a board for "look" command.
     *
     *@param  gameData  GameData for this game, read from look output
     *@param  matrix    Game position matrix
     *@param  notes     Additional info sent from IGS: Date and which player disconnected
     *@return           True if successful, else false
     */
    public boolean openLookBoard(GameData gameData, Matrix matrix, String notes) {
        MainFrame mf = null;
        try {
            mf = new MainFrame();
            Thread t = new Thread(mf);
            t.start();
            mf.getBoard().initGame(gameData, false);
            mf.getBoard().getBoardHandler().gotoLastMove(false);
            mf.getBoard().getBoardHandler().getTree().getCurrent().setMatrix(matrix);
            mf.getBoard().getBoardHandler().getTree().getCurrent().setCaptures(gameData.scoreCapsBlack, gameData.scoreCapsWhite);
            mf.getBoard().getBoardHandler().getStoneHandler().updateAll(mf.getBoard().getBoardHandler().getTree().getCurrent().getMatrix(), true);
            mf.getBoard().setPositionModified(true);
            mf.getBoard().repaint();
            mf.getBoard().getBoardHandler().updateGUI();
            mf.setCommentText(notes);
        } catch (NullPointerException e) {
            System.err.println("Failed to open board window for look: " + e);
            return false;
        }

        return true;
    } //}}}

    //{{{ distributeTerminalOutput() method
    /**
     *  Forward terminal output like yell and shout to the kibitz area
     *
     *@param  s  Text to forward
     */
    public void distributeTerminalOutput(String s) {
        for (Enumeration e = elements(); e.hasMoreElements(); ) {
            IGSObserverFrame mf = (IGSObserverFrame)e.nextElement();
            if (mf.getViewComm())
                mf.addKibitz(s);
        }
    } //}}}

    //{{{ clear() method
    /**  Clear the adjourned boards hashtable. Called when terminal frame is closed. */
    public void clear() {
        super.clear();
        adjournedBoards.clear();
    } //}}}

    //{{{ getAllObservedGameIDs() method
    /**
     *  Returns all currently observed game IDs
     *
     *@return    Enumeration containing Integer values with the observed IDs
     */
    public Enumeration getAllObservedGameIDs() {
        return keys();
    } //}}}

    //{{{ resumeGame() method
    /**
     *  Resume a game.
     *
     *@param  gameID  Game ID to resume
     */
    public void resumeGame(int gameID) {
        lastGameID = gameID;
        // Rest is handled in IGSReader
    } //}}}
}

