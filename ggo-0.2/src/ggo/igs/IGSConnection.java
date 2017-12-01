/*
 *  IGSConnection.java
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
import java.net.*;
import java.util.Enumeration;
import javax.swing.*;
import java.awt.event.*;
import java.text.MessageFormat;
import ggo.*;
import ggo.igs.*;
import ggo.igs.gui.*;
import ggo.igs.chatter.*;

/**
 *  This class connects to IGS, parses and forwards commands to IGS and controls
 *  the subclasses that handle the input and output streams.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.18 $, $Date: 2002/10/24 08:33:00 $
 */
public class IGSConnection implements Defines {
    //{{{ private members
    private Socket igsSocket;
    private IGSReader igsIn;
    private static PrintWriter igsOut = null;
    private IGSInput igsInput;
    private IGSClientWriter clientWriter;
    private JTextArea clientOut;
    private static PlayerTable playerTable = null;
    private static GamesTable gamesTable = null;
    private static IGSGameObserver gameObserver = null;
    private static IGSGameHandler gameHandler = null;
    private static IGSChatter chatter = null;
    private static IGSShouter shouter = null;
    private static IGSChannels channels = null;
    private static IGSMainWindow mainWindow;
    private static Timer aytTimer = null;
    private static AutoUpdater autoUpdater = null;
    private static long idleTimeStamp, idleTimeStampTmp;
    private static boolean away = false, alternateTimer = false;
    //}}}

    //{{{ IGSConnection constructors
    /**
     *Constructor for the IGSConnection object
     *
     *@param  clientOut    Pointer to the textarea where unparsed IGS input is displayed
     *@param  playerTable  Pointer to the frame with the players table
     *@param  gamesTable   Pointer to the frame with the games table
     *@param  chatter      Pointer to the chatter frame
     *@param  shouter      Pointer to the shouter frame
     *@param  channels     Description of the Parameter
     *@param  mainWindow   Description of the Parameter
     */
    public IGSConnection(JTextArea clientOut, PlayerTable playerTable, GamesTable gamesTable,
            IGSChatter chatter, IGSShouter shouter, IGSChannels channels, IGSMainWindow mainWindow) {
        this.clientOut = clientOut;
        this.playerTable = playerTable;
        this.gamesTable = gamesTable;
        this.chatter = chatter;
        this.shouter = shouter;
        this.channels = channels;
        this.mainWindow = mainWindow;
    }

    static {
        // Run ayt timer, every 5 minutes, and send alternate "who" and "games"
        aytTimer = new Timer(300000,
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    // Stop aytTimer if 1 hour idle
                    if (getIdleTime() > 3600) {
                        aytTimer.stop();
                        System.err.println("Timer stopped, idle too long.");
                        return;
                    }
                    blockIdle(true);
                    if (alternateTimer) {
                        IGSReader.requestSilentWho = true;
                        sendCommand("who");
                    }
                    else {
                        IGSReader.requestSilentGames = true;
                        sendCommand("games");
                    }
                    alternateTimer = !alternateTimer;
                    blockIdle(false);
                }
            });

        // Has to stay over disconnection
        autoUpdater = new AutoUpdater();
        autoUpdater.start();
        gameObserver = new IGSGameObserver();
        gameHandler = new IGSGameHandler();
    } //}}}

    //{{{ getLoginName() method
    /**
     *  Gets the loginName
     *
     *@return    The loginName
     */
    public static String getLoginName() {
        return IGSMainWindow.getHostConfig().getName();
    } //}}}

    //{{{ getPassword() method
    /**
     *  Gets the password
     *
     *@return    The password string
     */
    public static String getPassword() {
        return IGSMainWindow.getHostConfig().getPassword();
    } //}}}

    //{{{ getGameObserver() method
    /**
     *  Returns a pointer to the game observer. We need to access
     *  that from outside.
     *
     *@return    The gameObserver object
     */
    public static IGSGameObserver getGameObserver() {
        return gameObserver;
    } //}}}

    //{{{ getGameHandler() method
    /**
     *  Gets the gameHandler
     *
     *@return    The gameHandler value
     */
    public static IGSGameHandler getGameHandler() {
        return gameHandler;
    } //}}}

    //{{{ getGamesTable() method
    /**
     *  Gets the gamesTable
     *
     *@return    The gamesTable
     */
    public static GamesTable getGamesTable() {
        return gamesTable;
    } //}}}

    //{{{ getPlayerTable() method
    /**
     *  Gets the playerTable
     *
     *@return    The playerTable
     */
    public static PlayerTable getPlayerTable() {
        return playerTable;
    } //}}}

    //{{{ getMainWindow() method
    /**
     *  Gets the mainWindow attribute of the IGSConnection class
     *
     *@return    The mainWindow value
     */
    public static IGSMainWindow getMainWindow() {
        return mainWindow;
    } //}}}

    //{{{ getAutoUpdater() method
    /**
     *  Gets the autoUpdater attribute of the IGSConnection class
     *
     *@return    The autoUpdater value
     */
    public static AutoUpdater getAutoUpdater() {
        return autoUpdater;
    } //}}}

    //{{{ getChatter() method
    /**
     *  Gets the chatter attribute of the IGSConnection class
     *
     *@return    The chatter value
     */
    public static IGSChatter getChatter() {
        return chatter;
    } //}}}

    //{{{ getIGSReader() method
    /**
     *  Gets the iGSReader attribute of the IGSConnection object
     *
     *@return    The iGSReader value
     */
    public IGSReader getIGSReader() {
        return igsIn;
    } //}}}

    //{{{ connectIGS() method
    /**
     *  Connect to server
     *
     *@param  hostConfig  Pointer to the HostConfig class that hold the settings for the server
     *@return             True if connected successfuly, else false
     */
    public boolean connectIGS(HostConfig hostConfig) {
        try {
            igsSocket = new Socket(hostConfig.getHost(), hostConfig.getPort());

            InputStreamReader inputStream;
            OutputStreamWriter outputStream;
            // Use system default
            if (hostConfig.getEncoding() == null) {
                inputStream = new InputStreamReader(igsSocket.getInputStream());
                outputStream = new OutputStreamWriter(igsSocket.getOutputStream());
            }
            // Use given encoding from host configuration
            else {
                inputStream = new InputStreamReader(igsSocket.getInputStream(), hostConfig.getEncoding());
                outputStream = new OutputStreamWriter(igsSocket.getOutputStream(), hostConfig.getEncoding());
            }

            autoUpdater.resumeMe();
            igsOut = new PrintWriter(outputStream, true);
            clientWriter = new IGSClientWriter(clientOut, chatter, shouter, channels);
            igsIn = new IGSReader(
                    new BufferedReader(inputStream),
                    clientWriter,
                    autoUpdater);

            System.err.println("Socket to " + hostConfig.getHost() + " " + hostConfig.getPort() +
                    " created using encoding " + inputStream.getEncoding() + ".");
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host '" + hostConfig.getHost() + "': " + e);
            return false;
        } catch (UnsupportedEncodingException e) {
            System.err.println("Invalid encoding '" + hostConfig.getEncoding() + "': " + e);
            return false;
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection '" +
                    hostConfig.getHost() + ":" + hostConfig.getPort() + "': " + e);
            return false;
        }

        igsInput = new IGSInput(igsIn);
        igsInput.start();
        aytTimer.start();

        return true;
    } //}}}

    //{{{ getIdleTime() method
    /**
     *  Gets users idle time in seconds
     *
     *@return    Idle time in seconds
     */
    public static long getIdleTime() {
        return (System.currentTimeMillis() - idleTimeStamp) / 1000;
    } //}}}

    //{{{ blockIdle() method
    /**
     *  When sending a command, use this method to block updating the idle timestamp.
     *  Call blockIdle(true) before sending the command, afterwards unblock with blockIdle(false).
     *
     *@param  block  Block if true, unblock if false
     */
    public static void blockIdle(boolean block) {
        if (block)
            idleTimeStampTmp = idleTimeStamp;
        else
            idleTimeStamp = idleTimeStampTmp;
    } //}}}

    //{{{ setAway() method
    /**
     *  Toggle away mode
     *
     *@param  b  New away mode
     */
    public static void setAway(boolean b) {
        away = b;
    } //}}}

    //{{{ isAway() method
    /**
     *  Check if the user is away
     *
     *@return    The away value
     */
    public static boolean isAway() {
        return away;
    } //}}}

    //{{{ recieveInput() method
    /**
     *  Input from the textfield that wants to be sent to the server
     *
     *@param  command  Command to send to server
     */
    public void recieveInput(String command) {
        if (parseCommand(command))
            sendCommand(command);
    } //}}}

    //{{{ sendCommand() methods
    /**
     *  Send command to server. This is static so we
     *  can call it from outside easily.
     *
     *@param  command  Command to send to server
     *@param  doLog    Print debug log. Switch off for sending password.
     */
    public static void sendCommand(String command, boolean doLog) {
        if (doLog)
            System.err.println("SENDING '" + command + "'");

        try {
            igsOut.println(command);
        } catch (NullPointerException e) {
            System.err.println("No stream to server: " + e);
            // Stop timer if there is no connection
            aytTimer.stop();
            return;
        }

        idleTimeStamp = System.currentTimeMillis();

        // Restart aytTimer if necassary
        if (!aytTimer.isRunning() && mainWindow.isConnected()) {
            System.err.println("Restarted aytTimer.");
            aytTimer.start();
        }
    }

    /**
     *  Send command to server. This is static so we
     *  can call it from outside easily. Print debug log.
     *
     *@param  command  Command to send to server
     */
    public static void sendCommand(String command) {
        sendCommand(command, true);
    } //}}}

    //{{{ parseCommand() method
    /**
     *  Parse a commmand before we send it to the server
     *
     *@param  command  Command we are about to send that should be parsed
     *@return          True if command has to be send to server, else false
     */
    private boolean parseCommand(String command) {
        //{{{ Observe
        if (command.startsWith("ob")) {
            int gameID = 0;
            try {
                gameID = Integer.parseInt(command.substring(command.indexOf(" "), command.length()).trim());
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse game id: " + e);
                return false;
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse game id: " + e);
                return false;
            }
            startObserve(gameID);
            gamesTable.observeGame(gameID);
            return false;
        } //}}}

        //{{{ Unobserve
        else if (command.startsWith("uno")) {
            int gameID = 0;
            // Check if an argument was given
            try {
                gameID = Integer.parseInt(command.substring(command.indexOf(" "), command.length()).trim());
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse game id: " + e);
                return false;
            } catch (StringIndexOutOfBoundsException e) {
                // Unobserve without arguments given: unobserve all
                gameObserver.closeAllFrames();
                return true;
            }
            endObserve(gameID);
            gamesTable.unobserveGame(gameID);
            return false;
        } //}}}

        //{{{ Tell
        else if (command.startsWith("tel") || command.startsWith(". ")) {
            String target;
            String txt;
            try {
                int pos = command.indexOf(" ");
                target = command.substring(pos++, (pos = command.indexOf(" ", pos))).trim();
                txt = command.substring(pos + 1, command.length()).trim();
            } catch (StringIndexOutOfBoundsException e) {
                return true;
            }
            chatter.sendChat(target, txt, false);
            return true;
        } //}}}

        //{{{ Shout
        else if (command.startsWith("shou")) {
            String txt;
            try {
                txt = command.substring(command.indexOf(" "), command.length()).trim();
            } catch (StringIndexOutOfBoundsException e) {
                return true;
            }
            shouter.checkVisible();
            shouter.append(IGSConnection.getLoginName() + ": " + txt);
            return true;
        } //}}}

        //{{{ Yell
        // Yell \-1
        if (command.equals("yell \\-1") || command.equals("; \\-1")) {
            channels.leaveChannel();
            return true;
        }

        // Yell <text>
        else if ((command.startsWith("yell ") || command.startsWith("; ")) &&
                !command.startsWith("yell \\") && !command.startsWith("; \\")) {
            try {
                channels.append(getLoginName() + ": " + command.substring(command.indexOf(" "), command.length()).trim());
            } catch (StringIndexOutOfBoundsException e) {
                return true;
            }
            channels.checkVisible();
            channels.setInputFocus();
            return true;
        } //}}}

        //{{{ Refresh
        else if (command.startsWith("ref")) {
            int gameID = 0;
            try {
                gameID = Integer.parseInt(command.substring(command.indexOf(" "), command.length()).trim());
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse game id: " + e);
                return false;
            } catch (StringIndexOutOfBoundsException e) {
                // No arguments given, just forward to IGS
                return true;
            }
            if (gameObserver.observesGame(gameID) || gameHandler.playsGame(gameID)) {
                IGSReader.requestRefresh = gameID;
                sendCommand("status " + gameID);
            }
            return true;
        } //}}}

        //{{{ Stats
        else if (command.startsWith("stats")) {
            // No player info dialog when typed in the command line
            IGSReader.requestSilentStats = true;
            return true;
        } //}}}

        //{{{ Look
        else if (command.startsWith("loo")) {
            // No argument given. Let IGS handle the error.
            if (command.indexOf(" ") == -1)
                return true;
            IGSReader.requestRefresh = 9999;
            return true;
        } //}}}

        //{{{ All
        else if (command.startsWith("all")) {
            int gameID = 0;
            try {
                gameID = Integer.parseInt(command.substring(command.indexOf(" "), command.length()).trim());
            } catch (NumberFormatException e) {
                System.err.println("Failed to parse game id: " + e);
                return true;
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse game id: " + e);
                return true;
            }
            IGSReader.requestObservers = gameID;
            return true;
        } //}}}

        //{{{ Teach
        else if (command.startsWith("teac")) {
            igsIn.setTeachFlag();
            return true;
        } //}}}

        //{{{ Automatch
        else if (command.startsWith("au")) {
            IGSReader.automatchFlag = true;
            return true;
        } //}}}

        //{{{ Message
        else if (command.startsWith("me")) {
            IGSReader.requestSilentMessages = true;
            return true;
        } //}}}

        //{{{ Showgame - gGo special command
        else if (command.startsWith("showgame")) {
            try {
                showgame(command.substring(9).trim());
            } catch (StringIndexOutOfBoundsException e) {
                clientWriter.write("Usage is: showgame <playername>");
            }
            return false;
        } //}}}

        return true;
    } //}}}

    //{{{ startObserve() method
    /**
     *  Start observing a game
     *
     *@param  gameID  ID of game to observer
     *@return         Description of the Return Value
     */
    public static boolean startObserve(int gameID) {
        if (!gameHandler.isEmpty()) {
            System.err.println("Cannot observe while playing.");
            return false;
        }
        if (autoUpdater.hasGame(gameID))
            autoUpdater.getGame(gameID).setObserved(true);
        gameObserver.initGame(gameID);
        // Get game info
        sendCommand("status " + gameID);
        // "moves and "observe" command will be sent later
        return true;
    } //}}}

    //{{{ endObserve() method
    /**
     *  End obsering a game
     *
     *@param  gameID  ID of game ot unobserve
     */
    public static void endObserve(int gameID) {
        gameObserver.closeFrame(gameID);
    } //}}}

    //{{{ reloadObserve() method
    /**
     *  Reload an observed game after it was adjourned
     *
     *@param  gameID  ID of game to observer
     *@param  couple  Player name couple
     */
    public static void reloadObserve(int gameID, Couple couple) {
        if (autoUpdater.hasGame(gameID))
            autoUpdater.getGame(gameID).setObserved(true);
        gameObserver.reloadGame(gameID, couple);
        // Get game info
        sendCommand("status " + gameID);
        // "moves and "observe" command will be sent later
    } //}}}

    //{{{ resumeObserve() method
    /**  Resume observed games after a disconnection */
    private void resumeObserve() {
        Enumeration e = gameObserver.getAllObservedGameIDs();
        if (!e.hasMoreElements())
            return;

        Timer timer;
        int n = 0;
        while (e.hasMoreElements()) {
            int gameID = ((Integer)e.nextElement()).intValue();
            System.err.println("Resuming to observe game " + gameID);
            final int tmpID = gameID;
            timer = new Timer(3000 * n + 100,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        gameObserver.resumeGame(tmpID);
                        sendCommand("status " + tmpID);
                        // "moves and "observe" command will be sent later
                        if (autoUpdater.hasGame(tmpID))
                            autoUpdater.getGame(tmpID).setObserved(true);
                    }
                });
            timer.setRepeats(false);
            timer.start();

            n++;
        }
    } //}}}

    //{{{ initGame() method
    /**
     *  Init playing a new game
     *
     *@param  gameID     ID of new game
     *@param  data       GameData object with the game setup information
     *@param  whiteTime  IGSTime with the data for the white players time
     *@param  blackTime  IGSTime with the data for the black players time
     */
    public static void initGame(int gameID, GameData data, IGSTime whiteTime, IGSTime blackTime) {
        int playerColor;

        if (data.playerBlack.equals(getLoginName()))
            playerColor = STONE_BLACK;
        else if (data.playerWhite.equals(getLoginName()))
            playerColor = STONE_WHITE;
        else {
            System.err.println("Which color do I play?? Bailing out...");
            return;
        }

        if (gameHandler.initGame(gameID, data.playerBlack.equals(data.playerWhite) ? -1 : playerColor, data, whiteTime, blackTime))
            gameHandler.setGameInfo(data);
    } //}}}

    //{{{ sendChat() method
    /**
     *  Send a chat to another player
     *
     *@param  name  Name of the player to sent the chat to
     */
    public static void sendChat(String name) {
        try {
            chatter.setName(name);
            chatter.checkVisible();
            chatter.setInputFocus();
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ closeAll() method
    /**  Close all streams and quit reader thread */
    public void closeAll() {
        try {
            // This is odd. 1.3 locks up here.
            if (!gGo.is13()) {
                igsOut.close();
                igsIn.close();
            }
            igsSocket.close();
            aytTimer.stop();
            autoUpdater.suspendMe();
            igsInput.interrupt();
            igsInput = null;
        } catch (IOException e) {
            System.err.println("Failed to close connection: " + e);
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ notifyConnectionLost() method
    /**  IGSInput thread has ended and called this method. */
    static void notifyConnectionLost() {
        mainWindow.disconnect();
    } //}}}

    //{{{ handleLogin() method
    /**
     *  Upon login, send ID of this client and a delayed "who", resume observed games
     * (if any). All commands are sent with some delay.
     */
    public void handleLogin() {
        // Guest don't have this command
        if (getLoginName().equals("guest"))
            return;

        Timer timer = new Timer(1000,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    sendCommand("id " + PACKAGE + " " + VERSION);
                }
            });
        timer.setRepeats(false);
        timer.start();

        timer = new Timer(2000,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    IGSReader.requestSilentWho = true;
                    sendCommand("who");
                }
            });
        timer.setRepeats(false);
        timer.start();

        timer = new Timer(3000,
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    resumeObserve();
                }
            });
        timer.setRepeats(false);
        timer.start();
    } //}}}

    //{{{ sendAutoMatch() method
    /**
     *  Static method, used from PlayerPopup and PlayerInfoDialog to send out an automatch request to a player.
     *
     *@param  opponent  Name of the opponent the request is sent to
     */
    public static void sendAutoMatch(String opponent) {
        if (JOptionPane.showOptionDialog(
                null,
                MessageFormat.format(gGo.getIGSResources().getString("confirm_automatch"), new Object[]{opponent}),
                gGo.getIGSResources().getString("automatch_title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{gGo.getIGSResources().getString("confirm_automatch_yes"), gGo.getIGSResources().getString("confirm_automatch_no")},
                new String(gGo.getIGSResources().getString("confirm_automatch_no"))) == JOptionPane.YES_OPTION) {
            IGSConnection.sendCommand("automatch " + opponent);

            // Of course, if the opponent refuses, the flag is wrongly set. But if the defs are identical, IGS won't
            // notify about the new match. Sometimes I hate that protocol...
            // Fortunatly, setting the flag does no harm if it's not used later. Only unnecassary "status" traffic.
            IGSReader.automatchFlag = true;
        }
    } //}}}

    //{{{ sendMatch() method
    /**
     *  Static method, used from PlayerPopup and PlayerInfoDialog to send out a match request to a player.
     *
     *@param  opponent  Name of the opponent the request is sent to
     */
    public static void sendMatch(String opponent) {
        try {
            IGSConnection.getMainWindow().openMatchDialog(
                    true,
                    opponent,
                    IGSConnection.getLoginName(),
                    19,
                    gGo.getSettings().getIGSMatchMainTime(),
                    gGo.getSettings().getIGSMatchByoyomiTime(),
                    25,
                    opponent,
                    false);
        } catch (NullPointerException ex) {}
        return;
    } //}}}

    //{{{ showgame() method
    /**
     *  gGo-internal command: show current game of a player
     *
     *@param  args  Player name
     */
    private void showgame(String args) {
        if (args == null || args.length() == 0) {
            clientWriter.write("Usage is: showgame <playername>");
            return;
        }

        try {
            if (!autoUpdater.hasPlayer(args)) {
                clientWriter.write("Could not find player " + args + ".");
                return;
            }

            int gameID = autoUpdater.getPlayer(args).getGame();
            if (gameID <= 1) {
                clientWriter.write(args + " is not playing currently.");
                return;
            }

            clientWriter.write(args + " playing in game " + gameID);
        } catch (NullPointerException e) {
            System.err.println("Failed to retrieve player from database. Sorry.");
        }
    } //}}}
}

