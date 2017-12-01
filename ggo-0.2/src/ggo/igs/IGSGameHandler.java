/*
 *  IGSGameHandler.java
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
import ggo.utils.DeadGroupData;
import ggo.igs.*;
import ggo.igs.gui.*;

/**
 *  Handler that stores the played games and distributes the game data to the specific board.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.21 $, $Date: 2002/10/25 04:15:23 $
 */
public class IGSGameHandler extends Hashtable implements Defines {
    //{{{ private members
    private int lastGameID = -1;
    private static Hashtable adjournedBoards;
    private String komiRequestOpponent;
    //}}}

    //{{{ IGSGameHandler() constructors
    static {
        adjournedBoards = new Hashtable();
    }

    /**Constructor for the IGSGameHandler object */
    public IGSGameHandler() {
        komiRequestOpponent = null;
    } //}}}

    //{{{ playsGame() method
    /**
     *  Description of the Method
     *
     *@param  id  Description of the Parameter
     *@return     Description of the Return Value
     */
    public boolean playsGame(int id) {
        return containsKey(new Integer(id));
    } //}}}

    //{{{ initGame() method
    /**
     *  Description of the Method
     *
     *@param  gameID       Description of the Parameter
     *@param  playerColor  Description of the Parameter
     *@param  whiteTime    Description of the Parameter
     *@param  blackTime    Description of the Parameter
     *@param  data         Description of the Parameter
     *@return              Description of the Return Value
     */
    boolean initGame(int gameID, int playerColor, GameData data, IGSTime whiteTime, IGSTime blackTime) {
        if (playsGame(gameID)) {
            System.err.println("initGame(): Already playing this game.");
            return false;
        }

        MainFrame mf = null;
        Couple c = null;

        try {
            // Check if this was a reloaded game and we can reuse an already opened board
            c = new Couple(data.playerWhite, data.playerBlack);
            if (adjournedBoards.containsKey(c)) {
                System.err.println("Found a board to reuse.");

                if (playerColor != -1)
                    mf = (IGSPlayingFrame)adjournedBoards.get(c);
                else
                    mf = (IGSTeachingFrame)adjournedBoards.get(c);
                // Adjust gameID in board
                ((IGSPlayingFrame)mf).setGameID(gameID);
                // Switch reload button to adjourn
                ((IGSPlayingFrame)mf).toggleAdjournReload(false);
                // Add message to text field
                ((IGSPlayingFrame)mf).addSay(gGo.getIGSResources().getString("reload_message"));
            }
            // Cannot reuse a board, open new
            else {
                System.err.println("Did not find a board to reuse, opening new.");

                if (playerColor != -1)
                    mf = new IGSPlayingFrame(gameID, playerColor);
                else
                    mf = new IGSTeachingFrame(gameID);
                Thread t = new Thread(mf);
                t.start();
            }

            // Init clocks
            mf.getClockWhite().init(whiteTime);
            mf.getClockBlack().init(blackTime);
            if (playerColor == STONE_BLACK)
                mf.getClockBlack().setIsMyClock(true);
            else
                mf.getClockWhite().setIsMyClock(true);

            // Init thinking time
            ((IGSPlayingFrame)mf).initThinkingTime();

            // Init handicap menuitem
            ((IGSPlayingFrame)mf).checkHandicapMenuItem();
        } catch (NullPointerException e) {
            System.err.println("Failed to open board window: " + e);
            removeAdjournedGame(c);
            return false;
        }
        put(new Integer(gameID), mf);

        lastGameID = gameID;

        return true;
    } //}}}

    //{{{ removeGame() method
    /**
     *  Description of the Method
     *
     *@param  gameID  Description of the Parameter
     */
    public void removeGame(int gameID) {
        remove(new Integer(gameID));
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
    public synchronized boolean doMove(IGSMove move, boolean playSound, int state) {
        if (!playsGame(move.gameID)) {
            System.err.println("doMove(): I don't play game " + move.gameID);
            return false;
        }

        // MOVE DEBUG
        if (moveDebug)
            System.err.println("IGSGameHandler.doMove()\n" + move);

        try {
            IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(move.gameID));

            // Several moves, first move - lock board
            if (state == 1)
                mf.getBoard().lock();

            mf.getBoard().doIGSMove(move.color, move.x, move.y, move.captures, playSound, move.whiteTime, move.blackTime);
            mf.clearComment();
            mf.doMove(move.color, move.x, move.y);

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

    //{{{ recieveMoveFromHuman() method
    /**
     *  Description of the Method
     *
     *@param  gameID         Description of the Parameter
     *@param  color          Description of the Parameter
     *@param  x              Description of the Parameter
     *@param  y              Description of the Parameter
     *@param  boardSize      Description of the Parameter
     *@param  thinking_time  Description of the Parameter
     */
    public void recieveMoveFromHuman(int gameID, int color, int x, int y, int boardSize, int thinking_time) {
        if (!playsGame(gameID)) {
            System.err.println("recieveMoveFromHuman(): I don't play game " + gameID);
            return;
        }

        // Pass
        if (x == -1 && y == -1) {
            IGSConnection.sendCommand("pass");
            return;
        }

        IGSConnection.sendCommand(
                (char)('A' + (x < 9 ? x : x + 1) - 1) + String.valueOf(boardSize - y + 1) +
                " " + gameID + " " + thinking_time);
    } //}}}

    //{{{ setGameInfo() method
    /**
     *  Sets the gameInfo attribute of the IGSGameHandler object
     *
     *@param  gameData  The new gameInfo value
     *@return           Description of the Return Value
     */
    public int setGameInfo(GameData gameData) {
        if (lastGameID == -1)
            return -1;

        try {
            MainFrame mf = (MainFrame)get(new Integer(lastGameID));
            if (!adjournedBoards.containsKey(new Couple(gameData.playerWhite, gameData.playerBlack)))
                mf.getBoard().initGame(gameData, false);
            mf.updateCaption();

            // Start clock
            if (gameData.handicap < 2)
                mf.getClockBlack().start();
            else
                mf.getClockWhite().start();
        } catch (NullPointerException e) {
            System.err.println("Failed to send game info: " + e);
        }
        int tmp = lastGameID;
        lastGameID = -1;
        return tmp;
    } //}}}

    //{{{ writeSay() method
    /**
     *  Description of the Method
     *
     *@param  gameID  Description of the Parameter
     *@param  s       Description of the Parameter
     */
    public void writeSay(int gameID, String s) {
        if (!playsGame(gameID)) {
            System.err.println("writeSay(): I don't play game " + gameID);
            return;
        }

        try {
            IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(gameID));
            mf.addSay(s);
        } catch (NullPointerException e) {
            System.err.println("Failed to send say: " + e);
        }
    } //}}}

    //{{{ getBoardSize() method
    /**
     *  Gets the boardSize attribute of the IGSGameHandler object
     *
     *@param  gameID  Description of the Parameter
     *@return         The boardSize value
     */
    public int getBoardSize(int gameID) {
        if (!playsGame(gameID)) {
            System.err.println("getBoardSize(): I don't play game " + gameID);
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
     *@param  gameID  Description of the Parameter
     *@param  result  Description of the Parameter
     */
    public void saveResult(int gameID, String result) {
        System.err.println("IGSGameHandler.saveResult: " + gameID + " " + result);

        if (!playsGame(gameID)) {
            System.err.println("saveResult(): I don't play game " + gameID);
            return;
        }

        try {
            PlayingMainFrame mf = (PlayingMainFrame)get(new Integer(gameID));
            mf.appendCommentText(result);
            ((MainFrame)mf).writeCommentToSGF();
            mf.getBoard().getBoardHandler().getGameData().result = result;
            mf.displayInfo(result);
            mf.setFinished();

            // Stop clocks
            mf.getClockBlack().stop();
            mf.getClockWhite().stop();

            // Remove from list
            removeGame(gameID);
        } catch (NullPointerException e) {
            System.err.println("Failed to send result to board: " + e);
        }
    } //}}}

    //{{{ startScoring() method
    /**
     *  Description of the Method
     *
     *@param  gameID  Description of the Parameter
     */
    public void startScoring(int gameID) {
        if (!playsGame(gameID)) {
            System.err.println("startScoring(): I don't play game " + gameID);
            return;
        }

        System.err.println("Entering score mode for game " + gameID);
        try {
            PlayingMainFrame mf = (PlayingMainFrame)get(new Integer(gameID));
            mf.switchMode(MODE_SCORE);
            mf.getBoard().getBoardHandler().enterScoreMode(0, 0);
            mf.getBoard().getBoardHandler().countScore();

            // Stop clocks
            mf.getClockBlack().stop();
            mf.getClockWhite().stop();
        } catch (NullPointerException e) {
            System.err.println("Failed to switch board to score mode: " + e);
        }
    } //}}}

    //{{{ removeStone() method
    /**
     *  Remove a stone during scoring
     *
     *@param  gameID  Game ID
     *@param  x       X position of the stone to remove
     *@param  y       Y position of the stone to remove
     *@param  notice  Description of the Parameter
     */
    public void removeStone(int gameID, int x, int y, String notice) {
        if (!playsGame(gameID)) {
            System.err.println("removeStone(): I don't play game " + gameID);
            return;
        }

        try {
            IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(gameID));
            DeadGroupData data = mf.getBoard().getBoardHandler().getStoneHandler().removeDeadGroupIGS(x, y);
            if (data != null) {
                mf.getBoard().getBoardHandler().setCaptures(data.col == STONE_BLACK ? STONE_WHITE : STONE_BLACK, data.caps);
                mf.getBoard().getBoardHandler().countScore();
            }
            mf.addSay(notice);
        } catch (NullPointerException e) {
            System.err.println("Failed to remove stone: " + e);
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
        if (!playsGame(gameID)) {
            System.err.println("peekBoard(): I don't play game " + gameID);
            return;
        }

        try {
            IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(gameID));

            mf.getBoard().getBoardHandler().gotoLastMove(false);
            mf.getBoard().getBoardHandler().getTree().getCurrent().setMatrix(matrix);
            // This is true if we did undo in score mode
            if (clearDeadMarks) {
                mf.getBoard().getBoardHandler().getStoneHandler().removeDeadMarks();
                mf.getBoard().getBoardHandler().enterScoreMode(0, 0);
                mf.getBoard().getBoardHandler().countScore();
            }
            mf.getBoard().getBoardHandler().getTree().getCurrent().setCaptures(capsBlack, capsWhite);
            // Adjust komi if necassary
            if (komi != mf.getBoard().getBoardHandler().getGameData().komi) {
                mf.getBoard().getBoardHandler().getGameData().komi = komi;
                mf.setGameInfo(
                        mf.getBoard().getBoardHandler().getGameData().playerWhite,
                        mf.getBoard().getBoardHandler().getGameData().playerBlack,
                        mf.getBoard().getBoardHandler().getGameData().handicap,
                        komi);
                System.err.println("IGSGameHandler.peekBoard(): Adjusted komi to " + komi);
            }
            mf.getBoard().getBoardHandler().getStoneHandler().updateAll(mf.getBoard().getBoardHandler().getTree().getCurrent().getMatrix(), true);
            mf.getBoard().setPositionModified(true);
            mf.getBoard().repaint();
            mf.getBoard().getBoardHandler().updateGUI();
        } catch (NullPointerException e) {
            System.err.println("Failed to update board position: " + e);
            e.printStackTrace();
        }
    } //}}}

    //{{{ setGameTitle() method
    /**
     *  Sets the game title
     *
     *@param  gameID  Game ID
     *@param  title   Game title string
     */
    public void setGameTitle(int gameID, String title) {
        if (!playsGame(gameID)) {
            System.err.println("setGameTitle(): I don't play game " + gameID);
            return;
        }

        try {
            IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(gameID));
            String gn = mf.getBoard().getBoardHandler().getGameData().gameName;
            if (gn == null || gn.length() == 0 || !title.equals(gn)) {
                mf.getBoard().getBoardHandler().getGameData().gameName = title;
                mf.updateCaption();
            }
        } catch (NullPointerException e) {
            System.err.println("Failed to send game title: " + e);
        }
    } //}}}

    //{{{ doUndo() method
    /**
     *  Description of the Method
     *
     *@param  gameID  Description of the Parameter
     *@param  txt     Description of the Parameter
     *@return         Description of the Return Value
     */
    public synchronized boolean doUndo(int gameID, String txt) {
        if (!playsGame(gameID)) {
            System.err.println("doUndo(): I don't play game " + gameID);
            return false;
        }

        try {
            IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(gameID));
            mf.getBoard().getBoardHandler().deleteNode();
            mf.notifyUndo();
            mf.addSay(txt);
        } catch (NullPointerException e) {
            System.err.println("Failed to update board position for undo: " + e);
        }
        return true;
    } //}}}

    //{{{ requestAdjourn() method
    /**
     *  Opponent requested an adjourn
     *
     *@param  gameID  Game ID
     */
    public void requestAdjourn(int gameID) {
        if (!playsGame(gameID)) {
            System.err.println("adjournGame(): I don't play game " + gameID);
            return;
        }

        try {
            IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(gameID));
            mf.displayAdjournRequest();
        } catch (NullPointerException e) {
            System.err.println("Failed to send adjourn info: " + e);
        }
    } //}}}

    //{{{ guessWhichGameFinished() method
    /**
     *  Description of the Method
     *
     *@param  result   Description of the Parameter
     *@param  adjourn  Description of the Parameter
     *@return          Description of the Return Value
     */
    public boolean guessWhichGameFinished(final String result, final boolean adjourn) {
        if (isEmpty()) {
            System.err.println("I don't play in any game! This is strange !!");
            return false;
        }

        if (size() == 1) {
            try {
                IGSPlayingFrame mf = (IGSPlayingFrame)(elements().nextElement());
                mf.setFinished();
                mf.appendCommentText(result);
                ((MainFrame)mf).writeCommentToSGF();

                // Stop clocks
                mf.getClockBlack().stop();
                mf.getClockWhite().stop();

                String msg = result;

                // Adjourn?
                if (adjourn) {
                    // Remove from list, but remember this board in case we want to reload
                    // if the game gets continued
                    String w = mf.getBoard().getBoardHandler().getGameData().playerWhite;
                    String b = mf.getBoard().getBoardHandler().getGameData().playerBlack;
                    adjournedBoards.put(new Couple(w, b), mf);
                    // Switch adjourn button to reload
                    mf.toggleAdjournReload(true);
                    // Notify autoupdater
                    IGSConnection.getAutoUpdater().addAdjournOpponent(IGSConnection.getLoginName().equals(w) ? b : w);
                }
                else {
                    // Resign?
                    if (result.indexOf(" has resigned the game.") != -1) {
                        String name = result.substring(0, result.indexOf(" "));
                        if (mf.getBoard().getBoardHandler().getGameData().playerBlack.equals(name))
                            mf.getBoard().getBoardHandler().getGameData().result = "W + Res";
                        else
                            mf.getBoard().getBoardHandler().getGameData().result = "B + Res";
                        msg = MessageFormat.format(
                                gGo.getIGSResources().getString("player_resigned_message"),
                                new Object[]{result.substring(0, result.indexOf(" ", 1))});
                    }
                    // Timeout?
                    if (result.indexOf(" has run out of time.") != -1) {
                        String name = result.substring(0, result.indexOf(" "));
                        if (mf.getBoard().getBoardHandler().getGameData().playerBlack.equals(name))
                            mf.getBoard().getBoardHandler().getGameData().result = "W + Time";
                        else
                            mf.getBoard().getBoardHandler().getGameData().result = "B + Time";
                        msg = MessageFormat.format(
                                gGo.getIGSResources().getString("out_of_time_message"),
                                new Object[]{result.substring(0, result.indexOf(" ", 1))});
                    }
                }

                mf.displayInfo(msg);
                removeGame(mf.getGameID());
            } catch (NullPointerException e) {
                System.err.println("Failed to send info to board: " + e);
                return false;
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse result string: " + result + "\n" + e);
                return false;
            }
            return true;
        }
        else
            System.err.println("Sorry, playing in more than one game. Can't know which game has finished.");

        return false;
    } //}}}

    //{{{ removeAdjournedGame() method
    /**
     *  Remove a game from the adjourned Hashtable
     *
     *@param  couple  Key to remove
     */
    public static void removeAdjournedGame(Couple couple) {
        adjournedBoards.remove(couple);
    } //}}}

    //{{{ clear() method
    /**  Clear the adjourned boards hashtable. Called when terminal frame is closed. */
    public void clear() {
        super.clear();
        adjournedBoards.clear();
    } //}}}

    //{{{ findGameIDByOpponent() method
    /**
     *  Find game ID by opponent name
     *
     *@param  opponent  Opponent name
     *@return           Game ID, or -1 of not found
     */
    int findGameIDByOpponent(String opponent) {
        for (Enumeration e = keys(); e.hasMoreElements(); ) {
            Integer key = (Integer)e.nextElement();
            MainFrame mf = (MainFrame)(get(key));
            try {
                if (opponent.equals(mf.getBoard().getBoardHandler().getGameData().playerWhite) ||
                        opponent.equals(mf.getBoard().getBoardHandler().getGameData().playerBlack))
                    return key.intValue();
            } catch (NullPointerException ex) {
                System.err.println("Oops");
                ex.printStackTrace();
                return -1;
            }
        }
        return -1;
    } //}}}

    //{{{ requestKomiAdjustment() method
    /**
     *  Opponent requests a komi change
     *
     *@param  toParse  IGS input we need to parse
     */
    void requestKomiAdjustment(String toParse) {
        float komi;
        String opponent;
        try {
            komi = Float.parseFloat(toParse.substring(toParse.indexOf("be ") + 3).trim());
            opponent = toParse.substring(0, toParse.indexOf(" wants ")).trim();
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse komi request: " + e);
            return;
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse komi request: " + e);
            return;
        }

        int gameID = findGameIDByOpponent(opponent);
        if (gameID == -1) {
            System.err.println("Found no game with opponent " + opponent);
            return;
        }

        IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(gameID));
        if (mf == null) {
            System.err.println("Oops, found no board for game " + gameID);
            return;
        }

        mf.addSay(toParse);

        // Don't open messagebox if we sent the command ourselves or
        // if the opponent answered on our request
        if (opponent.equals(IGSConnection.getLoginName()) || mf.didKomiRequest() == komi) {
            komiRequestOpponent = opponent;
            mf.setKomiRequest(komi);
            return;
        }

        if (mf.displayKomiRequest(opponent, komi))
            komiRequestOpponent = opponent;
        else
            komiRequestOpponent = null;
    } //}}}

    //{{{ adjustKomiByOpponent() method
    /**
     *  Adjust komi of a game, identified by opponent name (not game ID in this case)
     *
     *@param  toParse  IGS input we need to parse
     */
    void adjustKomiByOpponent(String toParse) {
        float komi;
        try {
            komi = Float.parseFloat(toParse.substring(toParse.indexOf("to ") + 3, toParse.lastIndexOf(".")).trim());
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse komi adjustment: " + e);
            return;
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse komi adjustment: " + e);
            return;
        }
        if (komiRequestOpponent == null) {
            System.err.println("Oops, don't know which opponent requested the change!");
            return;
        }

        int gameID = findGameIDByOpponent(komiRequestOpponent);
        if (gameID == -1) {
            System.err.println("Found no game with opponent " + komiRequestOpponent);
            komiRequestOpponent = null;
            return;
        }

        try {
            IGSPlayingFrame mf = (IGSPlayingFrame)get(new Integer(gameID));

            mf.getBoard().getBoardHandler().getGameData().komi = komi;
            mf.setGameInfo(
                    mf.getBoard().getBoardHandler().getGameData().playerWhite,
                    mf.getBoard().getBoardHandler().getGameData().playerBlack,
                    mf.getBoard().getBoardHandler().getGameData().handicap,
                    komi);

            mf.addSay("Set the komi to " + komi + ".");
        } catch (NullPointerException e) {
            System.err.println("Failed to update komi: " + e);
        }

        komiRequestOpponent = null;
    } //}}}

    //{{{ reloadedGame() method
    /**
     *  Check if a game of the given player couple would be reloaded. Required to prevent trail
     *
     *@param  c  Couple to check
     *@return    True if the game will be loaded, else false
     */
    public static boolean reloadedGame(Couple c) {
        boolean res = adjournedBoards.containsKey(c);
        removeAdjournedGame(c);
        return res;
    } //}}}

    //{{{ notifyDisconnect() method
    /** When disconnecting, mark all ongoing games as adjourned */
    public void notifyDisconnect() {
        for (Enumeration e = elements(); e.hasMoreElements(); ) {
            try {
                IGSPlayingFrame mf = (IGSPlayingFrame)(e.nextElement());
                mf.setFinished();
                mf.addSay("Game adjourned on disconnection.");

                // Stop clocks
                mf.getClockBlack().stop();
                mf.getClockWhite().stop();

                // Remember game as adjourned
                adjournedBoards.put(
                        new Couple(
                        mf.getBoard().getBoardHandler().getGameData().playerWhite,
                        mf.getBoard().getBoardHandler().getGameData().playerBlack),
                        mf);
                // Switch adjourn button to reload
                mf.toggleAdjournReload(true);
                // Remove from hash
                removeGame(mf.getGameID());
            } catch (NullPointerException ex) {
                System.err.println("Failed to mark game as adjourned: " + e);
            }
        }
    } //}}}

    //{{{ updateTeachMarks() method
    /**
     *  Update the toolbar in teaching games
     *
     *@param  gameID  Game ID
     *@param  s       "You have marks at: 1,2,3." message
     */
    public void updateTeachMarks(int gameID, String s) {
        if (!playsGame(gameID)) {
            System.err.println("updateTeachMarks(): I don't play game " + gameID);
            return;
        }

        try {
            IGSTeachingFrame mf = (IGSTeachingFrame)get(new Integer(gameID));
            mf.updateMarkLabel(s);
        } catch (ClassCastException e) {
            System.err.println("Failed to update mark info: " + e);
        } catch (NullPointerException e) {
            System.err.println("Failed to update mark info: " + e);
        }
    } //}}}
}

