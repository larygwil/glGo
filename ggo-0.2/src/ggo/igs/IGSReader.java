/*
 *  IGSReader.java
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

import java.io.*;
import java.util.*;
import java.text.MessageFormat;
import ggo.igs.*;
import ggo.igs.gui.*;
import ggo.*;
import ggo.utils.*;

/**
 *  A subclass of FilterReader that gets all input from IGS telnet stream.
 *  The IGS input is parsed for the specific commands.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.39 $, $Date: 2002/10/25 04:15:32 $
 */
public class IGSReader extends FilterReader implements Defines {
    //{{{ private members
    private String lineRest;
    private IGSClientWriter out;
    private int state, gameID, newOwnGameID, gameCounter, stats_stored, parse_status, parse_status_stats;
    private final static int STATE_CONNECTED = 0,
            STATE_LOGIN = 1,
            STATE_LOGGEDON = 2;
    private ArrayList statusList, movesList;
    private String prevParse, kibitz, lookText;
    private StringBuffer playerStats, messagesBuffer;
    private PlayerInfo playerInfo;
    private GameData gameData;
    private boolean loadFlag, undoFlag, filesFlag, statsFlag, trailFlag, teachFlag, messagesFlag;
    private IGSTime whiteTime, blackTime;
    private AutoUpdater autoUpdater;
    private Vector trailHandler;
    private Hashtable timeRememberHash;

    private final static int PARSE_IDLE = 0;
    private final static int PARSE_MOVES = 1;
    private final static int PARSE_GAMES = 2;
    private final static int PARSE_GAME_INFO = 3;
    private final static int PARSE_GAME_STATUS = 4;
    private final static int PARSE_KIBITZ = 5;
    private final static int PARSE_PLAYERS = 6;
    private final static int PARSE_PLAYER_STATS = 7;
    private final static int PARSE_PLAYER_STATS_RESULTS = 8;
    private final static int PARSE_PLAYER_STATS_STORED = 9;
    private final static int PARSE_PLAYER_STATS_SGF = 10;
    //}}}

    //{{{ public members
    /**  Requesting "stats" from command line, dont popup dialog. */
    public static boolean requestSilentStats = false;
    /**  Requesting a board refresh for this gameID. 9999 for "look" command. */
    public static int requestRefresh = -1;
    /** Request "all" command for a certain observed game */
    public static int requestObservers = -1;
    /** Request "all" command for a certain own game */
    public static int requestObserversOwnGame = -1;
    /** Request a silent "who" without opening the table frame */
    public static boolean requestSilentWho = false;
    /** Request a silent "games" without opening the table frame */
    public static boolean requestSilentGames = false;
    /** Flag for automatch games, we need to access this from outside */
    public static boolean automatchFlag = false;
    /** Flag for accessing messages without dialog */
    public static boolean requestSilentMessages = false;
    //}}}

    //{{{ IGSReader constructor
    /**
     *Constructor for the IGSReader object
     *
     *@param  in   Reader with telnet input
     *@param  out  Pointer to the IGSClientWriter
     *@param  au   Description of the Parameter
     */
    public IGSReader(Reader in, IGSClientWriter out, AutoUpdater au) {
        super(in);
        this.out = out;

        statusList = new ArrayList();
        movesList = null;
        lineRest = lookText = "";
        state = STATE_CONNECTED;
        statsFlag = filesFlag = automatchFlag = undoFlag = loadFlag = trailFlag = teachFlag = messagesFlag = false;
        this.autoUpdater = au;
        trailHandler = new Vector();
        timeRememberHash = new Hashtable();
        parse_status = parse_status_stats = PARSE_IDLE;
        newOwnGameID = 0;
    } //}}}

    //{{{ getState() method
    /**
     *  Gets the current parse state
     *
     *@return    The state value
     */
    public int getState() {
        return state;
    } //}}}

    //{{{ read() method
    /**
     *  Description of the Method
     *
     *@param  cbuf             Description of the Parameter
     *@param  off              Description of the Parameter
     *@param  len              Description of the Parameter
     *@return                  Description of the Return Value
     *@exception  IOException  Description of the Exception
     */
    public int read(char[] cbuf, int off, int len) throws IOException {
        int readSize = in.read(cbuf, off, len);
        int lastPos = 0;

        // System.err.println("$ " + new String(cbuf, off, readSize));
        // System.err.println("readSize = " + readSize);

        try {
            for (int i = 0; i < readSize; i++) {
                if (cbuf[i] == '\n') {
                    // System.err.println("lastPos = " + lastPos + ", i = " + i);
                    String line = "";
                    if (lineRest.length() > 0) {
                        line = new String(lineRest);
                        // System.err.println("Rest: " + line);
                        lineRest = "";
                    }
                    line += String.valueOf(cbuf, lastPos, (i - lastPos > 0 ? i - lastPos - 1 : 0));
                    doParse(line);
                    lastPos = i + 1;
                }
            }
            if (readSize > lastPos) {
                lineRest += String.valueOf(cbuf, lastPos, readSize - lastPos);
                // System.err.println("LINE REST: '" + lineRest + "'");
                if (state == STATE_CONNECTED && lineRest.equals("Login: "))
                    doLoginName();
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Oops, failed to parse IGS input: " + e);
            return 1;
        }
        return readSize;
    } //}}}

    //{{{ doLoginName() method
    /**  Send login name */
    private void doLoginName() {
        if (state != STATE_CONNECTED) {
            System.err.println("Oops, wrong state! Aborting. Problem!");
            return;
        }

        if (IGSConnection.getLoginName().length() > 0) {
            IGSConnection.sendCommand(IGSConnection.getLoginName());

            if (IGSConnection.getLoginName().equals("guest"))
                state = STATE_LOGGEDON;
            else
                state = STATE_LOGIN;
        }
        else {
            out.write("Login:");
            state = STATE_LOGIN;
        }
    } //}}}

    //{{{ doLoginPassword() method
    /**  Send password */
    private void doLoginPassword() {
        if (state != STATE_LOGIN) {
            System.err.println("Oops, wrong state! Aborting. Problem!");
            return;
        }

        if (IGSConnection.getPassword().length() > 0) {
            IGSConnection.sendCommand(IGSConnection.getPassword(), false);
            System.err.println("SENDING '<password>'");
        }
        else
            out.write("Password:");
        state = STATE_LOGGEDON;
    } //}}}

    //{{{ doParse() method
    /**
     *  Main parsing function. Every IGS input is checked by this function for common
     *  patterns and commands. If found, subfunctions are called to process further actions.
     *
     *@param  toParse  Unmodified String sent by IGS
     */
    private void doParse(String toParse) {
        // System.err.println(toParse);

        String distributeString = null;

        //{{{ Prompt: 1 5 or or 1 6 or 1 8 or 1 7
        if (toParse.startsWith("1 5") || toParse.startsWith("1 8") ||
                toParse.startsWith("1 6") || toParse.startsWith("1 7")) {

            switch (parse_status) {
                case PARSE_MOVES:
                    parseGameMove(toParse);
                    break;
                case PARSE_GAMES:
                    if (!requestSilentGames) {
                        // We parsed only one game. Print this for convinience.
                        if (gameCounter == 1)
                            out.write(prevParse.substring(2));
                        else {
                            IGSConnection.getGamesTable().setVisible(true);
                            IGSConnection.getMainWindow().getGamesToggleButton().setSelected(true);
                        }
                        // Sort table
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {}
                        IGSConnection.getGamesTable().sortTable();
                    }
                    else
                        requestSilentGames = false;
                    break;
                case PARSE_PLAYERS:
                    if (!requestSilentWho) {
                        // Sort table
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {}
                        IGSConnection.getPlayerTable().sortTable();
                    }
                    else
                        requestSilentWho = false;
                    break;
                case PARSE_GAME_STATUS:
                {
                    // Undo?
                    if (undoFlag) {
                        parseStatusMatrix(true, true);
                        undoFlag = false;
                    }
                    else {
                        // Observing
                        if (toParse.startsWith("1 5") || toParse.startsWith("1 8")) {
                            if (requestRefresh != -1)
                                parseStatusMatrix(false, false);
                            else {
                                int id = IGSConnection.getGameObserver().setGameInfo(gameData);
                                if (id != -1) {
                                    // Get history of moves
                                    IGSConnection.sendCommand("moves " + id);
                                    // Tell server to start observing
                                    if (!trailFlag)
                                        IGSConnection.sendCommand("observe " + id);
                                    else
                                        trailFlag = false;
                                }
                            }
                        }

                        // Playing
                        else if (toParse.startsWith("1 6")) {
                            if (requestRefresh != -1)
                                parseStatusMatrix(true, false);
                            else {
                                // If we have a valid time from match parsing, init the game with them-
                                // Don't do that for adjourned games, they might already be in byo-yomi
                                String key =
                                        (gameData.playerWhite.equals(IGSConnection.getLoginName()) ? gameData.playerBlack :
                                        gameData.playerWhite);
                                System.err.println("Searching for remembered time settings for: " + key);
                                if (timeRememberHash.containsKey(key)) {
                                    IGSTime t = (IGSTime)timeRememberHash.remove(key);
                                    whiteTime.setTime(t.getTime() * 60);
                                    whiteTime.setStones(t.getStones());
                                    whiteTime.setInitByoTime(t.getInitByoTime() * 60);
                                    blackTime.setTime(t.getTime() * 60);
                                    blackTime.setStones(t.getStones());
                                    blackTime.setInitByoTime(t.getInitByoTime() * 60);
                                    System.err.println("Adjusted time with remembered settings.");
                                }
                                // Validate this game ID is correct
                                System.err.println("gameID = " + gameID + ", newOwnGameID = " + newOwnGameID);
                                if (gameID != newOwnGameID &&
                                        (IGSConnection.getGameObserver().observesGame(gameID) ||
                                        IGSConnection.getGameHandler().playsGame(gameID))) {
                                    gameID = newOwnGameID;
                                    System.err.println("Whoops, adjusted gameID " + gameID + " to " + newOwnGameID);
                                }
                                IGSConnection.initGame(gameID, gameData, whiteTime, blackTime);
                                // Adjourned game and a new board? Get moves
                                if (loadFlag) {
                                    loadFlag = false;
                                    if (!IGSGameHandler.reloadedGame(new Couple(gameData.playerWhite, gameData.playerBlack)))
                                        IGSConnection.sendCommand("moves " + gameID);
                                    else
                                        parseStatusMatrix(true, false);
                                }
                            }
                        }
                    }
                }
                    break;
                case PARSE_KIBITZ:
                    IGSConnection.getGameObserver().writeKibitz(gameID, kibitz);
                    kibitz = "";
                    break;
                default:
                    //{{{ Stats status
                    switch (parse_status_stats) {
                        case PARSE_IDLE:
                            break;
                        case PARSE_PLAYER_STATS:
                            playerInfo = parsePlayerInfo(playerStats.toString());
                            playerStats = null;
                            if (playerInfo != null) {
                                // Open the dialog with this info, rest will be updated later
                                PlayerInfoDialog dlg = PlayerInfoDialog.getDialog(playerInfo.getName());
                                if (dlg == null)
                                    dlg = new PlayerInfoDialog(null, false, playerInfo);
                                playerInfo.setDialog(dlg);
                                dlg.show();
                                // Now we get the game results, if any
                                parse_status_stats = PARSE_PLAYER_STATS_RESULTS;
                                statsFlag = false;
                                playerStats = new StringBuffer("");
                                IGSConnection.sendCommand("results -" + playerInfo.getName());
                                return;
                            }
                            break;
                        case PARSE_PLAYER_STATS_RESULTS:
                            // Ugly, but quiet-off or shout lines might come between.
                            if (!statsFlag)
                                return;
                            if (playerInfo != null) {
                                // Add results
                                playerInfo.setResults(playerStats.toString());
                                // Now check for number of stored games
                                parse_status_stats = PARSE_PLAYER_STATS_STORED;
                                statsFlag = false;
                                playerStats = new StringBuffer("");
                                IGSConnection.sendCommand("stored -" + playerInfo.getName());
                                return;
                            }
                            break;
                        case PARSE_PLAYER_STATS_STORED:
                            // Ugly, see above
                            if (!statsFlag)
                                return;
                            // Add stored games and update the dialog
                            if (playerInfo != null) {
                                playerInfo.setStored(stats_stored);
                                playerInfo.setStoredGames(playerStats.toString());
                                // Now check for sgf files
                                parse_status_stats = PARSE_PLAYER_STATS_SGF;
                                statsFlag = false;
                                playerStats = new StringBuffer("");
                                IGSConnection.sendCommand("sgf " + playerInfo.getName() + "-");
                                return;
                            }
                            break;
                        case PARSE_PLAYER_STATS_SGF:
                            // Ugly, see above
                            if (!statsFlag)
                                return;
                            // Add sgf games and update the dialog
                            if (playerInfo != null) {
                                playerInfo.setSGFGames(playerStats.toString());
                                playerInfo.getDialog().updatePlayerInfo(playerInfo);
                                // Clean up
                                playerInfo = null;
                                playerStats = null;
                                stats_stored = 0;
                                statsFlag = false;
                                parse_status_stats = PARSE_IDLE;
                            }
                            break;
                        default:
                            break;
                    } //}}}

                    // End of messages transfer
                    if (messagesFlag) {
                        if (messagesBuffer != null)
                            new ReadMessageDialog(IGSConnection.getMainWindow(), false, messagesBuffer.toString()).show();
                        messagesFlag = false;
                        messagesBuffer = null;
                    }
            }
            parse_status = PARSE_IDLE;
        } //}}}

        //{{{ { ... } autoupdate messages
        else if (toParse.startsWith("21 {")) {
            autoUpdater.doParse(toParse.substring(4, toParse.indexOf("}")));
            if (IGSConnection.getMainWindow().getViewAutoupdate())
                out.write(toParse.substring(3));
        } //}}}

        //{{{ Game move
        else if (toParse.startsWith("15 ")) {
            parseGameMove(toParse.substring(3));
        } //}}}

        //{{{ Game error
        else if (toParse.startsWith("50 ")) {
            out.write(toParse.substring(3));
            IGSConnection.getPlayerTable().getToolkit().beep();
        } //}}}

        //{{{ Who
        else if (toParse.startsWith("27 ")) {
            // First player incoming? Clean table
            if (!prevParse.startsWith("27 ") && !requestSilentWho) {
                IGSConnection.getPlayerTable().doClear();
                IGSConnection.getPlayerTable().setVisible(true);
                IGSConnection.getMainWindow().getPlayersToggleButton().setSelected(true);
            }

            try {
                String playerStr = toParse.substring(3);
                // Split left and right part
                int pos = playerStr.indexOf("|");
                if (pos != -1) {
                    parsePlayer(playerStr.substring(0, pos));
                    parsePlayer(playerStr.substring(pos + 2, playerStr.length()));
                }
                else
                    parsePlayer(playerStr);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Error parsing player list:\n" + toParse + "\n" + e);
            }
        } //}}}

        //{{{ Games
        else if (toParse.startsWith("7 ")) {
            // First player incoming? Clean table
            if (!prevParse.startsWith("7 ") && !requestSilentGames)
                IGSConnection.getGamesTable().doClear();
            parseGame(toParse.substring(2));
        } //}}}

        //{{{ Status
        else if (toParse.startsWith("22 ")) {
            if (parse_status == PARSE_IDLE) {
                gameData = new GameData();
                gameData.size = 0;
                statusList.clear();
                parseGameInfo(toParse.substring(3), STONE_WHITE, gameData); // White
                parse_status = PARSE_GAME_INFO;
            }
            else if (parse_status == PARSE_GAME_INFO) {
                parseGameInfo(toParse.substring(3), STONE_BLACK, gameData); // Black
                parse_status = PARSE_GAME_STATUS;
            }
            else if (parse_status == PARSE_GAME_STATUS) {
                gameData.size++;
                statusList.add(toParse.substring(3));
            }
        } //}}}

        //{{{ Stats handling
        // Collecting the String for "stats"
        // Of course one might also parse each line individually, but that would mean that
        // we would need to check for all "9 xyz" lines everytime doParse() is called.
        else if (toParse.startsWith("9 Player:")) {
            if (!requestSilentStats) {
                parse_status_stats = PARSE_PLAYER_STATS;
                playerStats = new StringBuffer(toParse.substring(2) + "\n");
            }
            else {
                requestSilentStats = false;
                out.write(toParse.substring(2));
            }
        }
        else if (parse_status_stats == PARSE_PLAYER_STATS && toParse.startsWith("9 ") && playerStats != null) {
            playerStats.append(toParse.substring(2) + "\n");
        }

        // Collecting the String for "results" of a player in combination with the "stats" command.
        // Ignore the "25 File" line.
        // Normal "result" input will not call this.
        else if (parse_status_stats == PARSE_PLAYER_STATS_RESULTS && filesFlag && !toParse.startsWith("25 File") && playerStats != null) {
            playerStats.append(toParse + "\n");
            statsFlag = true;
        }

        // Collecting the String for "stored" of a player in combination with the "stats" command.
        // 18 qgodev-Zotan          Zotan-qgodev
        // 18 Found 2 stored games.
        else if (parse_status_stats == PARSE_PLAYER_STATS_STORED && toParse.startsWith("18 ")) {
            parseStoredForStats(toParse);
            statsFlag = true;
        }

        // Collecting the String for "sgf" of a player in combination with the "stats" command.
        // 43 kris-shuzo-18-05-56            wahaha-shuzo-18-06-39
        // 43 There are 2 sgf games.
        else if (parse_status_stats == PARSE_PLAYER_STATS_SGF && toParse.startsWith("43 ")) {
            parseSGFForStats(toParse);
            statsFlag = true;
        } //}}}

        //{{{ Kibitz
        else if (toParse.startsWith("11 ")) {
            parseKibitz(toParse.substring(3));
        } //}}}

        //{{{ Say ID
        else if (toParse.startsWith("51 ")) {
            // Something like: "51 Say in game 104"
            try {
                gameID = Integer.parseInt(toParse.substring(15, toParse.length()).trim());
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse game id: " + e);
                gameID = 0;
            }
        } //}}}

        //{{{ Say
        else if (toParse.startsWith("19 ")) {
            if (gameID > 0)
                IGSConnection.getGameHandler().writeSay(gameID, toParse.substring(3));
        } //}}}

        //{{{ Logged on
        else if (toParse.startsWith("39 IGS entry on") || toParse.startsWith("IGS entry on")) {
            IGSConnection.sendCommand("toggle client true");
            IGSConnection.sendCommand("toggle quiet false");
            // Identify this client. Send this with delay
            IGSMainWindow.getIGSConnection().handleLogin();
            state = STATE_LOGGEDON;
            out.write(toParse.substring(3));
        } //}}}

        //{{{ Password
        else if (state == STATE_LOGIN &&
                (toParse.startsWith("1 1") || toParse.startsWith("Login: "))) {
            doLoginPassword();
        } //}}}

        //{{{ Match
        else if (toParse.startsWith("9 Creating match [") ||
                toParse.startsWith("9 Match [") ||
                (prevParse != null && prevParse.startsWith("9 Match[") && toParse.startsWith("9 Use <match ")) ||
                (toParse.startsWith("9 ") && toParse.indexOf("has restarted your game.") != -1)) {
            parseMatch(toParse.substring(2));
            out.write(toParse.substring(2));
        } //}}}

        //{{{ Automatch
        else if (toParse.startsWith("36 ")) {
            parseAutomatch(toParse.substring(3));
            out.write(toParse.substring(3));
        } //}}}

        //{{{ Automatch init
        else if (toParse.startsWith("35 ")) {
            parseAutomatchInit(toParse.substring(3));
        } //}}}

        //{{{ Adjourn
        else if (toParse.startsWith("48 Game ") &&
                toParse.indexOf("has been adjourned by") == -1) {
            parseAdjourn(toParse.substring(3));
            out.write(toParse.substring(3));
        }
        else if (toParse.startsWith("9 Game ") &&
                toParse.indexOf("has adjourned.") != -1) {
            parseAdjourn(toParse.substring(2));
            out.write(toParse.substring(2));
        }
        else if (toParse.startsWith("9 Game has been adjourned.")) {
            // Workaround. IGS sends this line twice. Why? No idea.
            if (!prevParse.startsWith("9 Game has been adjourned.")) {
                String adjournMsg = gGo.getIGSResources().getString("adjourn_message");
                if (!IGSConnection.getGameHandler().guessWhichGameFinished(adjournMsg, true))
                    IGSConnection.getMainWindow().displayInfo(adjournMsg);
            }
        } //}}}

        //{{{ Undo in game
        else if (toParse.startsWith("28 ")) {
            // Undo in game 169:  - Observed game
            try {
                int gameID = Integer.parseInt(toParse.substring(toParse.indexOf("game ") + 5, toParse.indexOf(":")));
                System.err.println("Undo in observed game " + gameID);
                if (!IGSConnection.getGameObserver().doUndo(gameID, toParse.substring(3)))
                    throw new Exception();
            } catch (Exception e) {
                // Zotan undid the last move (Q10) .  - Own game
                // No game ID sent here. We need to reuse the current. Multiple games? Need to check this.
                System.err.println("Undo in own game " + gameID);
                IGSConnection.getGameHandler().doUndo(gameID, toParse.substring(3));
            }

            out.write(toParse.substring(3));
        } //}}}

        //{{{ Undo in score
        else if (toParse.startsWith("9 Board is restored to what it was when you started scoring")) {
            undoFlag = true;
            IGSConnection.sendCommand("status " + gameID);
            out.write(toParse.substring(2));
        } //}}}

        //{{{ Result - observe
        else if (toParse.startsWith("9 {Game")) {
            parseResult(toParse.substring(2));
        } //}}}

        //{{{ Result - own game
        else if (toParse.startsWith("20 ")) {
            parseOwnResult(toParse.substring(3));
        } //}}}

        //{{{ Resign - own game and Timeout - own game
        else if (toParse.startsWith("9 ") &&
                (toParse.indexOf(" has resigned the game.") != -1 ||
                toParse.indexOf(" has run out of time.") != -1)) {
            out.write(toParse.substring(2));
            if (!IGSConnection.getGameHandler().guessWhichGameFinished(toParse.substring(2), false)) {
                String msg = toParse.substring(2);
                if (msg.indexOf(" has resigned the game.") != -1)
                    msg = MessageFormat.format(
                            gGo.getIGSResources().getString("player_resigned_message"),
                            new Object[]{msg.substring(0, msg.indexOf(" ", 1))});
                else
                    msg = MessageFormat.format(
                            gGo.getIGSResources().getString("out_of_time_message"),
                            new Object[]{msg.substring(0, msg.indexOf(" ", 1))});
                IGSConnection.getMainWindow().displayInfo(msg);
            }
        } //}}}

        //{{{ Remove while scoring
        else if (toParse.startsWith("49 Game")) {
            parseRemove(toParse.substring(3));
        } //}}}

        //{{{ Done while scoring
        else if (toParse.startsWith("9 ") && toParse.indexOf(" has typed done.") != -1) {
            out.write(toParse.substring(2));
            String name;
            try {
                name = toParse.substring(2, toParse.indexOf(" has")).trim();
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
                return;
            }
            IGSConnection.getGameHandler().writeSay(IGSConnection.getGameHandler().findGameIDByOpponent(name), toParse.substring(2));
        } //}}}

        //{{{ Shout
        else if (toParse.startsWith("21 !")) {
            parseShout(toParse.substring(3));
            distributeString = toParse.substring(3);
        } //}}}

        //{{{ Tell
        else if (toParse.startsWith("24 ")) {
            // Filter out Cgoban tells. Useless for now. Maybe implement this later.
            // Only print it in the terminal, but don't open a chat window. Annoying.
            if (toParse.indexOf("CLIENT: <") != -1) {
                out.write(toParse.substring(3));
                return;
            }
            if (parseTell(toParse.substring(3)))
                distributeString = toParse.substring(3);
        } //}}}

        //{{{ Tell feedback
        else if (toParse.startsWith("40 ") || toParse.startsWith("9 Setting your '.' to ")) {
            return; // Hide this
        } //}}}

        //{{{ Error: Cannot find recipient.
        else if (toParse.startsWith("5 Cannot find recipient.")) {
            out.recieveChatError(toParse.substring(2));
            distributeString = toParse.substring(2);
        } //}}}

        //{{{ Error: Invalid game number.
        else if (toParse.startsWith("5 Invalid game number.") && requestRefresh != -1) {
            System.err.println("Aborting refresh, invalid game.");
            requestRefresh = -1;
            out.write(toParse.substring(2));
        } //}}}

        //{{{ Title
        else if (toParse.startsWith("9 Game is titled")) {
            parseTitle(toParse.substring(2));
        } //}}}

        //{{{ Yell
        else if (toParse.startsWith("32 ")) {
            out.recieveYell(toParse.substring(3));
            distributeString = toParse.substring(3);
        } //}}}

        //{{{ Channels
        else if (toParse.startsWith("9 #")) {
            out.write(toParse.substring(2));
            out.recieveChannelList(toParse.substring(2));
        } //}}}

        //{{{ All
        // Observed game
        else if (requestObservers != -1 && toParse.startsWith("9 ")) {
            out.write(toParse.substring(2));
            IGSConnection.getGameObserver().writeKibitz(requestObservers, toParse.substring(2));
            if (toParse.startsWith("9 Found "))
                requestObservers = -1;
        }

        // Own game
        else if (requestObserversOwnGame != -1 && toParse.startsWith("9 ")) {
            out.write(toParse.substring(2));
            IGSConnection.getGameHandler().writeSay(requestObserversOwnGame, toParse.substring(2));
            if (toParse.startsWith("9 Found "))
                requestObserversOwnGame = -1;
        } //}}}

        //{{{ Trail
        else if (toParse.startsWith("45 You are now trailing ")) {
            String name = null;
            try {
                name = toParse.substring(24, toParse.indexOf(".", 24)).trim();
                if (!trailHandler.contains(name))
                    trailHandler.add(name);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse trailed name in: " + toParse + "\n" + e);
            }
            out.write(toParse.substring(3));
            try {
                PlayerInfoDialog.getDialog(name).toggleTrail(true);
            } catch (NullPointerException e) {}
        }

        else if (toParse.startsWith("45 You are NOT trailing ")) {
            String name = null;
            try {
                name = toParse.substring(24, toParse.indexOf(".", 24)).trim();
                trailHandler.remove(name);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse trailed name in: " + toParse + "\n" + e);
            }
            out.write(toParse.substring(3));
            try {
                PlayerInfoDialog.getDialog(name).toggleTrail(false);
            } catch (NullPointerException e) {}
        } //}}}

        //{{{ Look notes
        else if (toParse.startsWith("13 ")) {
            lookText += toParse.substring(3) + "\n";
            out.write(toParse.substring(3));
        } //}}}

        //{{{ Failed look. Reset refresh flag
        else if (requestRefresh == 9999 &&
                (toParse.startsWith("5 Game does not exist.") ||
                toParse.startsWith("5 look: Needs arguments, "))) {
            requestRefresh = -1;
            lookText = "";
            out.write(toParse.substring(2));
        } //}}}

        //{{{ Komi adjustment
        // Request
        else if (toParse.startsWith("9 ") && toParse.indexOf(" wants the komi to be ") != -1) {
            out.write(toParse.substring(2));
            IGSConnection.getGameHandler().requestKomiAdjustment(toParse.substring(2));
        }
        // Changed
        else if (toParse.startsWith("9 Set the komi to ")) {
            out.write(toParse.substring(2));
            IGSConnection.getGameHandler().adjustKomiByOpponent(toParse.substring(2));
        } //}}}

        //{{{ Teach marks
        else if (toParse.startsWith("9 You have marks at: ")) {
            // Global attribute gameID is ok here, this comes before the "1 6"
            IGSConnection.getGameHandler().updateTeachMarks(gameID, toParse.substring(2));
        } //}}}

        //{{{ 25 File
        else if (toParse.startsWith("25 File")) {
            filesFlag = !filesFlag;
            if (parse_status_stats == PARSE_PLAYER_STATS_RESULTS)
                statsFlag = true;
        } //}}}

        //{{{ 8 File, 9 File
        else if (toParse.startsWith("8 File") || toParse.startsWith("9 File")) {
            filesFlag = !filesFlag;
        } //}}}

        //{{{ 14 File
        else if (toParse.startsWith("14 File")) {
            if (!messagesFlag && !requestSilentMessages) {
                messagesBuffer = new StringBuffer();
                messagesFlag = true;
            }
        } //}}}

        //{{{ Beep
        else if (toParse.startsWith("9 ") && toParse.indexOf(" is beeping you.") != -1) {
            // Do a real beep, the parser doesn't handle that
            IGSConnection.getPlayerTable().getToolkit().beep();
        } //}}}

        //{{{ Others
        else if (toParse.startsWith("Login: ")) {
            // Login - Do nothing
            ;
        }
        else if (toParse.startsWith("18 ") ||
                toParse.startsWith("51 ") ||
                toParse.startsWith("43 ") ||
                toParse.startsWith("48 ") ||
                toParse.startsWith("42 ")) {
            out.write(toParse.substring(3));
        }
        else if (toParse.startsWith("9 ") ||
                toParse.startsWith("5 ") ||
                toParse.startsWith("4 ")) {
            out.write(toParse.substring(2));
        }
        else if (toParse.startsWith("2 ")) {
            return;
        } //}}}

        //{{{ Not parsed, send to text area
        else {
            // Filter anything size 0 or 1, those are IGS control signals
            // But keep linefeeds when in a File block
            if (!messagesFlag) {
                if (toParse.length() > 1 || filesFlag)
                    out.write(toParse);
            }
            // Reading messages
            else
                messagesBuffer.append(toParse + "\n");
        } //}}}

        // Distribute terminal output to observer windows (Yes, malf)
        if (distributeString != null && distributeString.length() > 0) {
            IGSConnection.getGameObserver().distributeTerminalOutput(distributeString);
        }

        prevParse = toParse;
    } //}}}

    //{{{ parsePlayer() method
    /**
     *  Parse a line of "who" output
     *
     *@param  toParse  String to parse
     */
    private void parsePlayer(String toParse) {
        // System.err.println("parsePlayer: " + toParse);

        parse_status = PARSE_PLAYERS;

        String name = toParse.substring(12, 23).trim();
        String rank = toParse.substring(30, 34).trim();

        // Filter out IGS header and footer
        if ((name.length() == 0 && rank.length() == 0) ||
                (name.equals("Name") && rank.equals("Rank")) ||
                name.equals("*******"))
            return;

        String status = toParse.substring(1, 3).trim();
        String observeStr = toParse.substring(3, 7).trim();
        String gameStr = toParse.substring(7, 11).trim();
        String idle = toParse.substring(23, 26).trim();

        int observe = 0;
        int game = 0;
        try {
            observe = Integer.parseInt(observeStr);
        } catch (NumberFormatException e) {}

        try {
            game = Integer.parseInt(gameStr);
        } catch (NumberFormatException e) {}

        IGSRank ir = null;
        try {
            ir = new IGSRank(rank);
        } catch (NumberFormatException e) {
            // System.err.println("Error parsing rank in " + toParse + ": " + e);
            return;
        }
        try {
            Player p = new Player(name, ir, status, game, observe, idle);
            autoUpdater.addPlayer(p);
            if (!requestSilentWho)
                IGSConnection.getPlayerTable().addPlayer(p);
        } catch (NullPointerException e) {
            System.err.println("Failed to add player to table: " + e);
        }
    } //}}}

    //{{{ parseGame() method
    /**
     *  Parse a line of "games" output
     *
     *@param  toParse  String to parse
     */
    private void parseGame(String toParse) {
        if (parse_status == PARSE_IDLE) {
            parse_status = PARSE_GAMES;
            gameCounter = 0;
        }

        int pos;
        String gameIDStr = toParse.substring(1, (pos = toParse.indexOf("]"))).trim();
        // Filter out IGS header
        if (gameIDStr.equals("##"))
            return;
        int gameID = 0;
        try {
            gameID = Integer.parseInt(gameIDStr);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse game id '" + gameIDStr + "': " + e);
            return;
        }

        // [170]       silta [ 5d*] vs.        YUZU [ 5d*] (132   19  0  5.5  5  I) ( 65)
        String whiteName = toParse.substring(++pos, (pos = toParse.indexOf("[", pos))).trim();
        String whiteRank = toParse.substring(++pos, (pos = toParse.indexOf("]", pos))).trim();
        pos += 5;
        String blackName = toParse.substring(pos, (pos = toParse.indexOf("[", pos))).trim();
        String blackRank = toParse.substring(++pos, (pos = toParse.indexOf("]", pos))).trim();
        pos += 3;
        String status = toParse.substring(pos, (pos = toParse.indexOf(")", pos)));
        String moveStr = status.substring(0, 3).trim();
        String sizeStr = status.substring(5, 8).trim();
        String handicapStr = status.substring(9, 11).trim();
        String komiStr = status.substring(12, 16).trim();
        String byoYomiStr = status.substring(17, 19).trim();
        String type = status.substring(20, 22).trim();
        pos += 2;
        String obsStr = toParse.substring(++pos, toParse.indexOf(")", pos)).trim();

        int obs = 0;
        int move = 0;
        int size = 19;
        int handicap = 0;
        float komi = 5.5f;
        int byoYomi = 10;
        try {
            move = Integer.parseInt(moveStr);
            size = Integer.parseInt(sizeStr);
            handicap = Integer.parseInt(handicapStr);
            komi = Float.parseFloat(komiStr);
            byoYomi = Integer.parseInt(byoYomiStr);
            obs = Integer.parseInt(obsStr);
        } catch (NumberFormatException e) {
            System.err.println("Failed to game data '" + toParse + "': " + e);
        }

        IGSRank irw = null;
        IGSRank irb = null;
        try {
            irw = new IGSRank(whiteRank);
            irb = new IGSRank(blackRank);
        } catch (NumberFormatException e) {
            return;
        }
        try {
            Game g = new Game(gameID, whiteName, irw, blackName, irb, move, size, handicap, komi, byoYomi,
                    type, obs, IGSConnection.getGameObserver().observesGame(gameID));
            autoUpdater.addGame(g);
            if (!requestSilentGames)
                IGSConnection.getGamesTable().addGame(g);
            gameCounter++;
        } catch (NullPointerException e) {
            System.err.println("Failed to add game to table: " + e);
        }
    } //}}}

    //{{{ parseGameMove() method
    /**
     *  Parse a game move
     *
     *@param  toParse  String to parse
     */
    private void parseGameMove(String toParse) {
        // System.err.println("parseGameMove: " + toParse);

        // MOVE DEBUG
        if (moveDebug && toParse.startsWith("Game "))
            System.err.println("*** MOVE DEBUG ***\nIGSReader.parseGameMove(): " + toParse);

        // Parse first line and get game ID
        if (toParse.startsWith("Game ")) {
            int pos = toParse.indexOf("I:");
            if (pos == -1)
                pos = toParse.indexOf("P:");
            if (pos == -1) {
                System.err.println("Error parsing game id at: " + toParse);
                return;
            }
            try {
                gameID = Integer.parseInt(toParse.substring(5, pos).trim());
            } catch (NumberFormatException e) {
                System.err.println("Error parsing game id: " + e);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Error parsing game id: " + e);
            }

            // Trail check, if we are not yet observing this game
            if (!IGSConnection.getGameObserver().observesGame(gameID)) {
                try {
                    int n = toParse.indexOf(":") + 2;
                    String whiteName = toParse.substring(n, (n = toParse.indexOf("(", n))).trim();
                    String blackName = toParse.substring((n = toParse.indexOf(" vs ", n) + 4), toParse.indexOf("(", n)).trim();

                    if (trailHandler.contains(whiteName) || trailHandler.contains(blackName)) {
                        // Check if the game will be opened via adjourn reload
                        if (!IGSConnection.getGameObserver().willReloadGame(new Couple(whiteName, blackName))) {
                            System.err.println("Trailing game " + gameID);
                            trailFlag = true;
                            IGSConnection.startObserve(gameID);
                        }
                        else {
                            System.err.println("Reloading anyways, will not trail.");
                            return;
                        }
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    System.err.println("Error parsing player names: " + e);
                }
            }

            //{{{ Time
            // Game 1 I: death (4 4629 -1) vs Bob (2 4486 -1)
            pos = 0;

            // White
            try {
                whiteTime = parseTime(toParse.substring(toParse.indexOf("(", pos) + 1, (pos = toParse.indexOf(")"))));
            } catch (NumberFormatException e) {
                System.err.println("Error parsing white time: " + e);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Error parsing white time: " + e);
            }
            // System.err.println("White time: " + whiteTime);

            // Black
            try {
                blackTime = parseTime(toParse.substring(toParse.indexOf("(", pos) + 1, toParse.indexOf(")", pos + 1)));
            } catch (NumberFormatException e) {
                System.err.println("Error parsing black time: " + e);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Error parsing black time: " + e);
            }
            // System.err.println("Black time: " + blackTime);
            //}}}

            // Required for automatch, load and teach
            if ((automatchFlag || teachFlag || loadFlag) && !IGSConnection.getGameObserver().observesGame(gameID)) {
                IGSConnection.sendCommand("status " + gameID);
                if (automatchFlag)
                    automatchFlag = false;
                if (teachFlag)
                    teachFlag = false;
                newOwnGameID = gameID;
            }

            return;
        }

        // End of transfer - Observing
        if ((toParse.startsWith("1 5") || toParse.startsWith("1 8")) && parse_status == PARSE_MOVES) {
            if (moveDebug)
                System.err.println("End of transfer - observe " + toParse.substring(3));
            IGSConnection.getGameObserver().doMoves(movesList);
            return;
        }
        // End of transfer - Playing
        else if (toParse.startsWith("1 6") && parse_status == PARSE_MOVES) {
            if (moveDebug)
                System.err.println("End of transfer - playing " + toParse.substring(3));
            if (!IGSConnection.getGameHandler().doMoves(movesList)) {
                // Observed game while playing
                if (moveDebug)
                    System.err.println("End of transfer - observe while playing" + toParse.substring(3));
                IGSConnection.getGameObserver().doMoves(movesList);
            }
            return;
        }
        // End of transfer - Scoring
        else if (toParse.startsWith("1 7") && parse_status == PARSE_MOVES) {
            IGSConnection.getGameHandler().doMoves(movesList);
            IGSConnection.getGameHandler().startScoring(gameID);
            return;
        }

        if (parse_status == PARSE_IDLE) {
            parse_status = PARSE_MOVES;
            // movesList.clear();
            movesList = new ArrayList();
            if (moveDebug)
                System.err.println("MOVE start, movesList created");
        }

        IGSMove move = new IGSMove(gameID, whiteTime, blackTime);
        parseMove(toParse, move);
        movesList.add(move);
    } //}}}

    //{{{ parseTime() method
    /**
     *  Parse the time part of a game move
     *
     *@param  toParse                              Description of the Parameter
     *@return                                      Description of the Return Value
     *@exception  NumberFormatException            Description of the Exception
     *@exception  StringIndexOutOfBoundsException  Description of the Exception
     */
    private IGSTime parseTime(String toParse) throws NumberFormatException, StringIndexOutOfBoundsException {
        int tPos;
        return (new IGSTime(Integer.parseInt(toParse.substring((tPos = toParse.indexOf(" ") + 1), (tPos = toParse.indexOf(" ", tPos)))),
                Integer.parseInt(toParse.substring((tPos = toParse.indexOf(" ", tPos) + 1), toParse.length()))));
    } //}}}

    //{{{ parseMove() method
    /**
     *  Parse the move coordinates
     *
     *@param  toParse  String to parse
     *@param  move     Description of the Parameter
     */
    private void parseMove(String toParse, IGSMove move) {
        int moveNum = 0;
        int color = 0;
        int x = 0;
        int y = 0;
        int handicap = -1;
        int boardSize = IGSConnection.getGameObserver().getBoardSize(move.gameID);
        if (boardSize == -1)
            boardSize = IGSConnection.getGameHandler().getBoardSize(move.gameID);
        if (boardSize == -1) {
            // Is this an adjourned or new automatch or teaching game?
            if (loadFlag || automatchFlag || teachFlag) {
                System.err.println("I don't know this ID. I assume it is an adjourned game, teaching game or a new automatch game.");
                parse_status = PARSE_IDLE;
                IGSConnection.sendCommand("status " + move.gameID);
                if (automatchFlag)
                    automatchFlag = false;
                if (teachFlag)
                    teachFlag = false;
                newOwnGameID = move.gameID;
                return;
            }
            else {
                System.err.println("Failed to get board size. Aborting move parsing.");
                parse_status = PARSE_IDLE;
                return;
            }
        }
        try {
            // Move number
            moveNum = Integer.parseInt(toParse.substring(0, 3).trim());

            // Color
            if (toParse.charAt(4) == 'B')
                color = STONE_BLACK;
            else
                color = STONE_WHITE;

            // Move coordinates
            String moveStr = toParse.substring(8, toParse.length()).trim();
            if (moveStr.equals("Pass")) {
                x = y = -1;
            }
            else if (moveStr.startsWith("Handicap ")) {
                String handicapStr = moveStr.substring(9, moveStr.length()).trim();
                try {
                    handicap = Integer.parseInt(handicapStr);
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse handicap: " + e);
                }
            }
            else {
                // --- 1.3 ---
                String s[];
                if (!gGo.is13())
                    s = moveStr.split(" ");
                else
                    s = Utils.splitString(moveStr, " ");
                if (s.length == 0) {
                    System.err.println("Invalid move!");
                    return;
                }

                x = s[0].charAt(0) - 'A' + (s[0].charAt(0) < 'J' ? 1 : 0);
                y = boardSize - Integer.parseInt(s[0].substring(1, s[0].length())) + 1;

                if (s.length > 1) {
                    ArrayList captures = new ArrayList();
                    for (int i = 1, sz = s.length; i < sz; i++) {
                        Position pos = new Position(
                                s[i].charAt(0) - 'A' + (s[i].charAt(0) < 'J' ? 1 : 0),
                                boardSize - Integer.parseInt(s[i].substring(1, s[i].length())) + 1);
                        captures.add(pos);
                    }
                    move.captures = captures;
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing game data for ID '" + gameID + "': " + e);
        }

        move.moveNum = moveNum;
        move.color = color;
        if (handicap == -1) {
            move.x = x;
            move.y = y;
        }
        // If it's the first handicap move, we encode it as x = -2 and y = number of handcap stones
        else {
            move.x = -2;
            move.y = handicap;
        }

        // MOVE DEBUG
        if (moveDebug)
            System.err.println("IGSReader.parseMove(): " + toParse + "\n" + move);
    } //}}}

    //{{{ parseGameInfo() method
    /**
     *  Parse the game information from "status"
     *
     *@param  toParse  String to parse
     *@param  col      Description of the Parameter
     *@param  data     Description of the Parameter
     */
    private void parseGameInfo(String toParse, int col, GameData data) {
        // Only read the first two lines
        if (toParse.startsWith(" "))
            return;

        try {
            int pos = 0;
            String name = toParse.substring(0, (pos = toParse.indexOf(" ")));
            String rank = toParse.substring(++pos, (pos = toParse.indexOf(" ", pos)));
            while (toParse.charAt(pos) == ' ') // Find next nonspace. IGS format is a bit odd.
                pos++;
            String caps = toParse.substring(pos, (pos = toParse.indexOf(" ", pos)));
            int tmp = toParse.indexOf("T", pos);
            if (tmp == -1)
                tmp = toParse.indexOf("F", pos);
            pos = tmp + 1;
            String komi = toParse.substring(++pos, (pos = toParse.indexOf(" ", pos)));
            String handicap = toParse.substring(++pos, pos + 1);

            if (col == STONE_WHITE) {
                data.playerWhite = name;
                data.rankWhite = rank;
                try {
                    data.komi = Float.parseFloat(komi);
                    data.handicap = Integer.parseInt(handicap);
                    data.scoreCapsWhite = Integer.parseInt(caps);
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse komi or handicap: " + e);
                    // Oops. Hope for the best.
                    data.scoreCapsWhite = 0;
                    data.komi = 5.5f;
                    data.handicap = 0;
                }
            }
            else {
                data.playerBlack = name;
                data.rankBlack = rank;
                try {
                    data.scoreCapsBlack = Integer.parseInt(caps);
                } catch (NumberFormatException e) {
                    data.scoreCapsBlack = 0;
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse game info: " + e);
        }
    } //}}}

    //{{{ parseStatusMatrix() method
    /**
     *  Parse the board matrix from "status"
     *
     *@param  own             Description of the Parameter
     *@param  clearDeadMarks  Description of the Parameter
     */
    private void parseStatusMatrix(boolean own, boolean clearDeadMarks) {
        System.err.println("parseStatusMatrix() " + requestRefresh);
        if (requestRefresh != -1)
            gameID = requestRefresh;
        System.err.println("gameID = " + gameID);
        requestRefresh = -1;
        final int size = statusList.size();
        if (size < 0 || size > 36)
            return;
        Matrix matrix = new Matrix(size);

        try {
            int row = 0;
            for (Iterator it = statusList.iterator(); it.hasNext(); row++) {
                String s = (String)it.next();
                if (s.charAt(2) != ':')
                    continue;

                s = s.substring(4, s.length());

                for (int i = 0; i < size; i++) {
                    short spot = (short)(s.charAt(i) - '1' + 1);
                    if (spot == 0)
                        matrix.insertStone(row + 1, i + 1, STONE_BLACK, MODE_EDIT);
                    else if (spot == 1)
                        matrix.insertStone(row + 1, i + 1, STONE_WHITE, MODE_EDIT);
                    else if (spot == 4)
                        matrix.insertMark(row + 1, i + 1, MARK_TERR_WHITE);
                    else if (spot == 5)
                        matrix.insertMark(row + 1, i + 1, MARK_TERR_BLACK);
                }
            }
        } catch (NumberFormatException ex) {
            System.err.println("Failed to parse status matrix: " + ex);
            return;
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println("Failed to parse status matrix: " + ex);
            return;
        } catch (ClassCastException ex) {
            // This is ok. No error.
            return;
        }

        if (gameID != 9999) {
            if (own)
                IGSConnection.getGameHandler().peekBoard(gameID, matrix, clearDeadMarks, gameData.scoreCapsBlack, gameData.scoreCapsWhite, gameData.komi);
            else
                IGSConnection.getGameObserver().peekBoard(gameID, matrix, clearDeadMarks, gameData.scoreCapsBlack, gameData.scoreCapsWhite, gameData.komi);
        }
        // "Look" command
        else {
            IGSConnection.getGameObserver().openLookBoard(gameData, matrix, lookText);
            lookText = "";
        }
    } //}}}

    //{{{ parseKibitz() method
    /**
     *  Parse a kibitz
     *
     *@param  toParse  String to parse
     */
    private void parseKibitz(String toParse) {
        // Parse first line
        if (parse_status == PARSE_IDLE) {
            parse_status = PARSE_KIBITZ;

            try {
                int pos;

                gameID = 0;
                String name = toParse.substring(toParse.indexOf(" "), (pos = toParse.indexOf("[")) - 1).trim();
                String rank = toParse.substring(++pos, (pos = toParse.indexOf("]", pos))).trim();
                String gameIDStr = toParse.substring((pos = toParse.indexOf("[", pos)) + 1, toParse.indexOf("]", pos)).trim();

                try {
                    gameID = Integer.parseInt(gameIDStr);
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing game id: " + e);
                }

                kibitz = name + " [" + rank + "]: ";
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse kibitz " + toParse + "\n" + e);
            }
        }

        // Parse second line
        else if (parse_status == PARSE_KIBITZ) {
            kibitz += toParse.trim();
        }
    } //}}}

    //{{{ parseResult() method
    /**
     *  Parse the result of observed games
     *
     *@param  toParse  String to parse
     */
    private void parseResult(String toParse) {
        String result = "";
        try {
            int pos;
            String gameIDStr = toParse.substring(toParse.indexOf(" "), (pos = toParse.indexOf(":"))).trim();
            try {
                gameID = Integer.parseInt(gameIDStr);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse game id: " + e);
                out.write(toParse); // Just print it, and abort.
                return;
            }
            result = toParse.substring(toParse.indexOf(":", ++pos) + 1, toParse.indexOf("}")).trim();
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse result " + toParse + "\n" + e);
        }

        // Display board position with territory marks
        requestRefresh = -1; // Make sure class attribute gameID is used
        parseStatusMatrix(false, false);

        // Send result to text area
        out.write(toParse);

        // Store result as comment of last move
        IGSConnection.getGameObserver().saveResult(gameID, result, false);
    } //}}}

    //{{{ parseOwnResult() method
    /**
     *  Parse the result of own games
     *
     *@param  toParse  String to parse
     */
    private void parseOwnResult(String toParse) {
        // Display board position with territory marks
        requestRefresh = -1; // Make sure class attribute gameID is used
        parseStatusMatrix(true, false);

        // Zotan (W:O):  0.5 to qgodev (B:#): 81.0
        String result = null;
        String opponent = null;
        try {
            int pos1 = toParse.indexOf("(");
            int pos2 = toParse.indexOf(")", pos1);
            int pos3 = toParse.indexOf("(", pos2);
            int pos4 = toParse.indexOf(")", pos3);

            result = toParse.substring(0, pos1).trim() +
                    toParse.substring(pos2 + 1, pos3).trim() +
                    toParse.substring(pos4 + 1, toParse.length()).trim();

            String white = toParse.substring(0, toParse.indexOf(" ")).trim();
            String black = toParse.substring(toParse.indexOf(" to ") + 4, toParse.indexOf("(B:#)")).trim();
            if (white.equals(IGSConnection.getLoginName()))
                opponent = black;
            else
                opponent = white;
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse result\n" + toParse + "\n" + e);
            result = toParse;
        }

        // Send result to text area
        out.write(result);

        // Store result as comment of last move
        IGSConnection.getGameHandler().saveResult(IGSConnection.getGameHandler().findGameIDByOpponent(opponent), result);
    } //}}}

    //{{{ parseTell() method
    /**
     *  Parse a tell input
     *
     *@param  toParse  String to parse
     *@return          Description of the Return Value
     */
    private boolean parseTell(String toParse) {
        try {
            int pos = toParse.indexOf("*");
            String name = toParse.substring(++pos, (pos = toParse.indexOf("*:", pos))).trim();
            String txt = toParse.substring(pos + 2, toParse.length()).trim();

            // Block bozos
            if (IGSConnection.getMainWindow().getBozoHandler().getBozoStatus(name) != BozoHandler.PLAYER_STATUS_BOZO) {
                out.recieveChat(name, txt);
                return true;
            }
            else {
                System.err.println("Bozo tell blocked: " + name + " - " + txt);
                out.sendBozoAutoReply(name);
                return false;
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse tell " + toParse + "\n" + e);
            return false;
        }
    } //}}}

    //{{{ parseShout() method
    /**
     *  Parse a shout input
     *
     *@param  toParse  String to parse
     */
    private void parseShout(String toParse) {
        try {
            int pos = toParse.indexOf("!");
            String name = toParse.substring(++pos, (pos = toParse.indexOf("!:", pos))).trim();
            String txt = toParse.substring(pos + 2, toParse.length()).trim();
            String rank = null;
            if (autoUpdater.hasPlayer(name))
                rank = autoUpdater.getPlayer(name).getRank().toString();
            out.recieveShout(
                    name +
                    (rank != null ? " [" + rank + "]: " : ": ") +
                    txt);
            if (gGo.getSettings().getIGSshowShouts())
                out.write(
                        "!" + name +
                        (rank != null ? " [" + rank + "]!: " : "!: ") +
                        txt);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse shout " + toParse + "\n" + e);
            if (gGo.getSettings().getIGSshowShouts())
                out.write(toParse.substring(3));
        }
    } //}}}

    //{{{ parseMatch() method
    /**
     *  Parse a match request
     *
     *@param  toParse  String to parse
     */
    private void parseMatch(String toParse) {
        // System.err.println("PARSE MATCH: " + toParse);

        //{{{ Use <match qgodev W 19 1 10> or <decline qgodev> to respond.
        if (toParse.startsWith("Use <match ")) {
            int pos = 11;
            String opponent = toParse.substring(11, (pos = toParse.indexOf(" ", pos)));

            // Block bozos
            if (IGSConnection.getMainWindow().getBozoHandler().getBozoStatus(opponent) == BozoHandler.PLAYER_STATUS_BOZO) {
                System.err.println("Bozo match blocked: " + opponent);
                out.sendBozoAutoReply(opponent);
                return;
            }

            String colorStr = toParse.substring(++pos, (pos = toParse.indexOf(" ", pos)));
            int color;
            if (colorStr.equals("B"))
                color = STONE_BLACK;
            else if (colorStr.equals("W"))
                color = STONE_WHITE;
            else {
                System.err.println("Unknown color.");
                return;
            }
            String sizeStr = toParse.substring(++pos, (pos = toParse.indexOf(" ", pos)));
            String mainTimeStr = toParse.substring(++pos, (pos = toParse.indexOf(" ", pos)));
            String byoyomiStr = toParse.substring(++pos, (pos = toParse.indexOf(">", pos)));

            int size = 0;
            int mainTime = 0;
            int byoyomiTime = 0;
            int byoyomiStones = 25;
            try {
                size = Integer.parseInt(sizeStr);
                mainTime = Integer.parseInt(mainTimeStr);
                byoyomiTime = Integer.parseInt(byoyomiStr);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse match request '" + toParse + "':" + e);
                mainTime = byoyomiTime = byoyomiStones = -99;
                return;
            }

            System.err.println("MATCH: " + opponent + " " + color + " " + size + " " + mainTime + " " + byoyomiTime);

            if (mainTime == 0 && byoyomiTime == 0)
                byoyomiStones = -1;

            rememberMatchTime(
                    opponent,
                    new IGSTime(mainTime, byoyomiTime, byoyomiStones));

            // Open match dialog
            IGSConnection.getMainWindow().openMatchDialog(
                    false,
                    (color == STONE_WHITE ? IGSConnection.getLoginName() : opponent),
                    (color == STONE_WHITE ? opponent : IGSConnection.getLoginName()),
                    size, mainTime, byoyomiTime, byoyomiStones, opponent, false);
        } //}}}

        //{{{ Creating match [233] with qgodev.
        else if (toParse.startsWith("Creating match [")) {
            String gameIDStr = toParse.substring(toParse.indexOf("[") + 1, toParse.indexOf("]"));
            newOwnGameID = 0;
            try {
                newOwnGameID = Integer.parseInt(gameIDStr);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse game id '" + gameIDStr + "':" + e);
                return;
            }
            // Make sure this game is not reloaded
            try {
                String opponent = toParse.substring(toParse.lastIndexOf(" "), toParse.indexOf(".")).trim();
                // Unfortunately we don't know about black/white here yet
                IGSGameHandler.removeAdjournedGame(new Couple(opponent, IGSConnection.getLoginName()));
                IGSGameHandler.removeAdjournedGame(new Couple(IGSConnection.getLoginName(), opponent));
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse opponent in " + toParse + "\n" + e);
            }
            IGSConnection.sendCommand("status " + newOwnGameID);
        } //}}}

        //{{{ Match [88] with qgodev in 0 accepted.
        else if (toParse.startsWith("Match [")) {
            String gameIDStr = toParse.substring(toParse.indexOf("[") + 1, toParse.indexOf("]"));
            newOwnGameID = 0;
            try {
                newOwnGameID = Integer.parseInt(gameIDStr);
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse game id '" + gameIDStr + "':" + e);
                return;
            }
            // Make sure this game is not reloaded
            try {
                String opponent = toParse.substring(toParse.indexOf("with ") + 5, toParse.indexOf(" in ")).trim();
                // Unfortunately we don't know about black/white here yet
                IGSGameHandler.removeAdjournedGame(new Couple(opponent, IGSConnection.getLoginName()));
                IGSGameHandler.removeAdjournedGame(new Couple(IGSConnection.getLoginName(), opponent));
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse opponent in " + toParse + "\n" + e);
            }
            IGSConnection.sendCommand("status " + newOwnGameID);
        } //}}}

        //{{{ qgodev has restarted your game.
        else if (toParse.indexOf("has restarted your game.") != -1) {
            // Unfortunately the game id is not given here. So we need to trick a little.
            loadFlag = true;
            System.err.println("Ok, remembering to start game. Have to get ID later.");
        } //}}}

        else {
            System.err.println("Don't know anything about this match string.");
        }

    } //}}}

    //{{{ parseAutomatch() method
    /**
     *  Description of the Method
     *
     *@param  toParse  Description of the Parameter
     */
    private void parseAutomatch(String toParse) {
        // System.err.println("parseAutomatch(): " + toParse);

        // qgodev wants a match with you:
        // qgodev wants 19x19 in 1 minutes with 10 byo-yomi and 25 byo-stones
        // To accept match type 'automatch qgodev'

        if (toParse.indexOf(" byo-stones") == -1)
            return;

        String opponent;
        int size;
        int mainTime = 0;
        int byoyomiTime = 0;
        int byoyomiStones = 0;

        try {
            int pos = toParse.indexOf(" ");
            opponent = toParse.substring(0, pos);

            // Block bozos
            if (IGSConnection.getMainWindow().getBozoHandler().getBozoStatus(opponent) == BozoHandler.PLAYER_STATUS_BOZO) {
                System.err.println("Bozo automatch blocked: " + opponent);
                out.sendBozoAutoReply(opponent);
                return;
            }

            String sizeStr = toParse.substring((pos = toParse.indexOf("wants", pos) + 6), (pos = toParse.indexOf("x", pos))).trim();
            String mainTimeStr = toParse.substring((pos = toParse.indexOf("in ", pos) + 3), (pos = toParse.indexOf(" minutes", pos))).trim();
            String byoTimeStr = toParse.substring((pos = toParse.indexOf("with ", pos) + 5), (pos = toParse.indexOf(" byo-yomi", pos))).trim();
            String byoyomiStonesStr = toParse.substring((pos = toParse.indexOf("and ", pos) + 4), toParse.indexOf(" byo-stones", pos)).trim();

            size = Integer.parseInt(sizeStr);
            mainTime = Integer.parseInt(mainTimeStr);
            byoyomiTime = Integer.parseInt(byoTimeStr);
            byoyomiStones = Integer.parseInt(byoyomiStonesStr);

        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse automatch: " + toParse + "\n" + e);
            mainTime = byoyomiTime = byoyomiStones = -99;
            return;
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse automatch: " + toParse + "\n" + e);
            mainTime = byoyomiTime = byoyomiStones = -99;
            return;
        }

        System.err.println("AUTOMATCH: " + opponent + " " + size + " " + mainTime + " " + byoyomiTime + " " + byoyomiStones);

        rememberMatchTime(
                opponent,
                new IGSTime(mainTime, byoyomiTime, byoyomiStones));

        // Yes, IGS -does- send automatch requests back. We parse time settings and abort here.
        if (opponent.equals(IGSConnection.getLoginName()))
            return;

        // Open match dialog
        IGSConnection.getMainWindow().openMatchDialog(
                false,
                opponent,
                IGSConnection.getLoginName(),
                size, mainTime, byoyomiTime, byoyomiStones, opponent, true);
    } //}}}

    //{{{ parseAutomatchInit() method
    /**
     *  Description of the Method
     *
     *@param  toParse  Description of the Parameter
     */
    private void parseAutomatchInit(String toParse) {
        // qgodev wants 19x19 in 30 minutes with 10 byo-yomi and 15 byo-stones
        // Zotan wants 19x19 in 10 minutes with 10 byo-yomi and 10 byo-stones
        // Using Zotan 'defs'.
        // Zotan

        // Might parse the game info here, but for now we get it from status.

        // Unfortunately the game id is not given here. So we need to trick a little.
        if (toParse.startsWith("Using ")) {
            automatchFlag = true;
            System.err.println("AUTOMATCH INIT: Ok, remembering to start game. Have to get ID later.");
        }
    } //}}}

    //{{{ parseRemove() method
    /**
     *  Parse the remove of a stone during scoring own games
     *
     *@param  toParse  String to parse
     */
    private void parseRemove(String toParse) {
        // Game 30 qgodev is removing @ A1

        int boardSize = IGSConnection.getGameHandler().getBoardSize(gameID);
        if (boardSize == -1) {
            System.err.println("Failed to get board size for game " + gameID + ". Aborting.");
            return;
        }

        String moveStr = null;
        int x = 0;
        int y = 0;
        try {
            int pos = toParse.indexOf("Game") + 5;
            String gameIDStr = toParse.substring(pos, toParse.indexOf(" ", pos));
            gameID = Integer.parseInt(gameIDStr);
            moveStr = toParse.substring(toParse.indexOf("@ ") + 2, toParse.length()).trim();
            x = moveStr.charAt(0) - 'A' + (moveStr.charAt(0) < 'J' ? 1 : 0);
            y = boardSize - Integer.parseInt(moveStr.substring(1, moveStr.length())) + 1;
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse gameID: " + e);
            return;
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse gameID: " + e);
            return;
        }

        String notice = "";
        // Own try-catch block. Not so bad if this fails.
        try {
            notice = toParse.substring(toParse.indexOf(" ", 5), toParse.length()).trim();
        } catch (StringIndexOutOfBoundsException e) {}

        IGSConnection.getGameHandler().removeStone(gameID, x, y, notice);
    } //}}}

    //{{{ parseAdjourn() method
    /**
     *  Parse an adjourn request or information
     *
     *@param  toParse  String to parse
     */
    private void parseAdjourn(String toParse) {
        // System.err.println("parseAdjourn: " + toParse);

        int gameID = 0;
        boolean own = false;
        String gameIDStr;

        try {
            // Observed game
            gameIDStr = toParse.substring(toParse.indexOf(" "), toParse.indexOf(":")).trim();
        } catch (StringIndexOutOfBoundsException e) {
            // Own game
            int pos = toParse.indexOf(" ");
            gameIDStr = toParse.substring(pos, toParse.indexOf(" ", pos + 1)).trim();
            own = true;
        }

        try {
            gameID = Integer.parseInt(gameIDStr);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse game id: " + e);
            return;
        }

        if (!own)
            IGSConnection.getGameObserver().adjournGame(gameID);
        else
            IGSConnection.getGameHandler().requestAdjourn(gameID);
    } //}}}

    //{{{ parseTitle() method
    /**
     *  Parse the game title
     *
     *@param  toParse  String to parse
     */
    private void parseTitle(String toParse) {
        try {
            String title = toParse.substring(toParse.indexOf(":") + 1, toParse.length()).trim();
            if (!IGSConnection.getGameObserver().setGameTitle(gameID, title))
                IGSConnection.getGameHandler().setGameTitle(gameID, title);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse title " + toParse + "\n" + e);
        }
    } //}}}

    //{{{ parsePlayerInfo() method
    /**
     *  Description of the Method
     *
     *@param  toParse  Description of the Parameter
     *@return          Description of the Return Value
     */
    private PlayerInfo parsePlayerInfo(String toParse) {
        PlayerInfo playerInfo = null;

        try {
            String name = toParse.substring(toParse.indexOf("Player:") + 7, toParse.indexOf("Game:")).trim();
            String gameSetting = toParse.substring(toParse.indexOf("Game:") + 5, toParse.indexOf("Language:")).trim();
            String language = toParse.substring(toParse.indexOf("Language:") + 9, toParse.indexOf("Rating:")).trim();
            String rating = toParse.substring(toParse.indexOf("Rating:") + 7, toParse.indexOf("Rated Games:")).trim();
            String rank = toParse.substring(toParse.indexOf("Rank:") + 5, toParse.indexOf("Wins:")).trim();
            int ratedGames = Integer.parseInt(toParse.substring(toParse.indexOf("Rated Games:") + 12, toParse.indexOf("Rank:")).trim());
            int wins = Integer.parseInt(toParse.substring(toParse.indexOf("Wins:") + 5, toParse.indexOf("Losses:")).trim());
            int pos = toParse.indexOf("Last Access(GMT):");
            int end;
            int l;
            if (pos == -1) {
                pos = toParse.indexOf("Idle Time:");
                end = toParse.indexOf("Address:");
                if (end == -1)
                    end = toParse.indexOf("IP addr:");
                l = 10;
            }
            else {
                end = toParse.indexOf("Last Access(local):");
                l = 17;
            }
            int losses = Integer.parseInt(toParse.substring(toParse.indexOf("Losses:") + 7, pos).trim());
            String access = toParse.substring(pos + l, end).trim();
            if ((pos = access.indexOf("Playing in game:")) != -1)
                access = access.substring(0, pos - 1) + " - " + access.substring(pos, access.length());
            if ((pos = access.indexOf("Observing game:")) != -1)
                access = access.substring(0, pos - 1) + " - " + access.substring(pos, access.length());
            pos = toParse.indexOf("Address:");
            if (pos == -1)
                pos = toParse.indexOf("IP addr:");
            String email = toParse.substring(pos + 8, toParse.indexOf("Reg date:")).trim();
            String regDate = toParse.substring(toParse.indexOf("Reg date:") + 9, toParse.indexOf("Info:")).trim();
            String info = toParse.substring(toParse.indexOf("Info:") + 5, toParse.indexOf("Defaults (help")).trim();
            int own = toParse.indexOf("Verbose");
            if (own == -1)
                end = toParse.length();
            else
                end = own;
            String defs = toParse.substring(toParse.indexOf("Defaults (help defs):") + 21, end).trim();

            playerInfo = new PlayerInfo(name, gameSetting, language, rating, rank, ratedGames, wins, losses, access, email, regDate, info, defs);

            // Stats on yourself, parse toggle string and attach UserDefs class to playerInfo
            if (own != -1) {
                end = toParse.indexOf("Chatter", own);
                String toggles = toParse.substring(toParse.indexOf("O", end), toParse.length()).trim();
                playerInfo.setUserDefs(new UserDefs(defs, toggles));
            }

        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse player info: " + e);
            return null;
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse player info: " + e);
            return null;
        }

        // Set trailed flag
        playerInfo.setTrailed(trailHandler.contains(playerInfo.getName()));

        return playerInfo;
    } //}}}

    //{{{ parseStoredForStats() method
    /**
     *  Description of the Method
     *
     *@param  toParse  Description of the Parameter
     */
    private void parseStoredForStats(String toParse) {
        try {
            if (toParse.startsWith("18 Found "))
                stats_stored = Integer.parseInt(toParse.substring(9, toParse.indexOf(" stored")).trim());
            else {
                // Replace the spaces with linebreaks
                // --- 1.3 ---
                String s[];
                if (!gGo.is13())
                    s = toParse.substring(3).split(" ");
                else
                    s = Utils.splitString(toParse.substring(3), " ");
                if (s.length > 0) {
                    for (int i = 0, sz = s.length; i < sz; i++) {
                        if (s[i].length() > 2)
                            playerStats.append(s[i] + "\n");
                    }
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse stored games: " + e);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse stored games: " + e);
        }
    } //}}}

    //{{{ parseSGFForStats() method
    /**
     *  Description of the Method
     *
     *@param  toParse  Description of the Parameter
     */
    private void parseSGFForStats(String toParse) {
        try {
            // Don't need this line
            if (toParse.startsWith("43 There "))
                return;

            // Replace the spaces with linebreaks
            // --- 1.3 ---
            String s[];
            if (!gGo.is13())
                s = toParse.substring(3).split(" ");
            else
                s = Utils.splitString(toParse.substring(3), " ");
            if (s.length > 0) {
                for (int i = 0, sz = s.length; i < sz; i++) {
                    if (s[i].length() > 2)
                        playerStats.append(s[i] + "\n");
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse sgf games: " + e);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse sgf games: " + e);
        }
    } //}}}

    //{{{ removeTrail() method
    /**
     *  Remove a player from internal trail list. Called from AutoUpdater.removePlayer()
     *
     *@param  name  Player name
     *@see          ggo.igs.AutoUpdater#removePlayer(String)
     */
    void removeTrail(String name) {
        trailHandler.remove(name);
    } //}}}

    //{{{ doesTrail() method
    /**
     *  Check if the given player is trailed.
     *
     *@param  name  Player name
     *@return       True if player is trailed, else false
     */
    public boolean doesTrail(String name) {
        return trailHandler.contains(name);
    } //}}}

    //{{{ rememberMatchTime() method
    /**
     *  Remember the time settings from match dialog
     *
     *@param  name  Opponent name
     *@param  t     IGSTime instance containing the time settings
     */
    public void rememberMatchTime(String name, IGSTime t) {
        timeRememberHash.put(name, t);
        System.err.println("Remembered match time with " + name + ": " + t);
    } //}}}

    //{{{ setTeachFlag() method
    /**  Notify we are requesting the start of a teaching game */
    void setTeachFlag() {
        teachFlag = true;
    } //}}}
}

