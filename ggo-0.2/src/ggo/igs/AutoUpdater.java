/*
 *  AutoUpdater.java
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
import ggo.igs.Couple;
import ggo.igs.gui.*;
import ggo.igs.chatter.IGSChatter;

/**
 *  Storage and parser class to store the player and game data incoming
 *  by the "toggle quiet off" autoupdate messages from IGS.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.13 $, $Date: 2002/10/18 16:55:30 $
 */
public class AutoUpdater extends Thread {
    //{{{ private members
    /**  Update frequency for the player and games table. Update runs alternated. */
    private final static int UPDATE_FREQUENCY = 30;
    private Hashtable playerHash, gamesHash;
    private Vector adjournReloadVector, adjournReloadOwnVector;
    private boolean alternateFlag, threadSuspended;
    //}}}

    //{{{ AutoUpdater() constructor
    /**Constructor for the AutoUpdater object */
    public AutoUpdater() {
        super("autoupdater");

        playerHash = new Hashtable();
        gamesHash = new Hashtable();
        adjournReloadVector = new Vector();
        adjournReloadOwnVector = new Vector();
        alternateFlag = true;
    } //}}}

    //{{{ run() method
    /**  Main processing method for the AutoUpdater object */
    public void run() {
        threadSuspended = false;

        while (!isInterrupted()) {
            try {
                Thread.sleep(UPDATE_FREQUENCY * 1000);
                synchronized (this) {
                    while (threadSuspended)
                        wait();
                }
            } catch (InterruptedException e) {
                finishAutoupdate();
                return;
            }
            alternateSynch();
        }
        finishAutoupdate();
    } //}}}

    //{{{ suspendMe() method
    /**  Suspend the thread. Avoid using deprecated Thread.suspend() */
    public void suspendMe() {
        threadSuspended = true;
    } //}}}

    //{{{ resumeMe() method
    /**  Resume the thread. Avoid using deprecated Thread.resume() */
    public synchronized void resumeMe() {
        if (threadSuspended)
            notify();
        threadSuspended = false;
    } //}}}

    //{{{ alternateSynch() method
    /**  Description of the Method */
    private void alternateSynch() {
        if (alternateFlag)
            synchPlayerTable();
        else
            synchGamesTable();
        alternateFlag = !alternateFlag;
    } //}}}

    //{{{ doParse() method
    /**
     *  Description of the Method
     *
     *@param  toParse  Description of the Parameter
     */
    public void doParse(String toParse) {
        // System.err.println(toParse);

        try {
            //{{{ Zotan has disconnected
            if (toParse.endsWith("has disconnected")) {
                removePlayer(toParse.substring(0, toParse.indexOf(" ")));
            } //}}}

            //{{{ Zotan [10k*] has connected.
            else if (toParse.endsWith("has connected.")) {
                int pos = toParse.indexOf("[");
                String name = toParse.substring(0, pos).trim();
                if (adjournReloadOwnVector.contains(name)) {
                    adjournReloadOwnVector.remove(name);
                    IGSConnection.getMainWindow().displayInfo(name + " is now online.");
                }
                addPlayer(
                        name,
                        toParse.substring(pos + 1, toParse.indexOf("]")).trim());
            } //}}}

            //{{{ Match 34: slaughter [ 3d*] vs. goohkubo [ 3d*]
            else if (toParse.startsWith("Match ")) {
                int pos = toParse.indexOf(" ");
                String gameIDStr = toParse.substring(pos, (pos = toParse.indexOf(":", pos))).trim();
                int gameID = 0;
                try {
                    gameID = Integer.parseInt(gameIDStr);
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse gameID in: " + toParse + "\n" + e);
                    return;
                }
                addGame(
                        gameID,
                        toParse.substring(++pos, (pos = toParse.indexOf("[", pos))).trim(),
                        new IGSRank(toParse.substring(++pos, (pos = toParse.indexOf("]", pos))).trim()),
                        toParse.substring((pos = toParse.indexOf("vs. ", pos) + 3), (pos = toParse.indexOf("[", pos))).trim(),
                        new IGSRank(toParse.substring(++pos, (pos = toParse.indexOf("]", pos))).trim()));
            } //}}}

            //{{{ Game 97: kkim vs Tgreen : W 77.5 B 67.0
            else if (toParse.startsWith("Game ")) {
                int pos = toParse.indexOf(" ");
                String gameIDStr = toParse.substring(pos, (pos = toParse.indexOf(":", pos))).trim();
                int gameID = 0;
                try {
                    gameID = Integer.parseInt(gameIDStr);
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse gameID in: " + toParse + "\n" + e);
                    return;
                }
                if (toParse.indexOf(" @ Move ") == -1)
                    removeGame(gameID, toParse.substring(toParse.indexOf(":", ++pos) + 1, toParse.length()).trim());
                // Game 152: kk002 vs yulong @ Move 159  Argh, why is no rank here?
                else {
                    String w = toParse.substring(toParse.indexOf(":") + 1, toParse.indexOf("vs")).trim();
                    String b = toParse.substring(toParse.indexOf("vs") + 2, toParse.indexOf("@")).trim();
                    IGSRank wr;
                    IGSRank br;
                    if (hasPlayer(w))
                        wr = getPlayer(w).getRank();
                    else
                        wr = new IGSRank("");
                    if (hasPlayer(b))
                        br = getPlayer(b).getRank();
                    else
                        br = new IGSRank("");
                    addGame(gameID, w, wr, b, br);
                    notifyReload(gameID, w, b);
                }
            } //}}}
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse: " + toParse + "\n" + e);
        }
    } //}}}

    //{{{ addPlayer(String, String) method
    /**
     *  Adds a feature to the Player attribute of the AutoUpdater object
     *
     *@param  name  The feature to be added to the Player attribute
     *@param  rank  The feature to be added to the Player attribute
     */
    public void addPlayer(String name, String rank) {
        // System.err.println("addPlayer: " + name + " " + rank);
        addPlayer(new Player(name, new IGSRank(rank), "", 0, 0, ""));

        // Display info message in chatter
        if (IGSConnection.getChatter().hasTarget(name))
            IGSConnection.getChatter().notifyOnline(name);
    } //}}}

    //{{{ addPlayer(Player) method
    /**
     *  Adds a feature to the Player attribute of the AutoUpdater object
     *
     *@param  p  The feature to be added to the Player attribute
     */
    public void addPlayer(Player p) {
        playerHash.put(p.getName(), p);
    } //}}}

    //{{{ removePlayer() method
    /**
     *  Description of the Method
     *
     *@param  name  Description of the Parameter
     */
    public void removePlayer(String name) {
        // System.err.println("removePlayer: " + name);
        playerHash.remove(name);
        if (IGSConnection.getPlayerTable().availableOnly()) {
            IGSConnection.getPlayerTable().removePlayer(name);
            IGSConnection.getPlayerTable().setTotal();
        }

        // Display info message in chatter
        if (IGSConnection.getChatter().hasTarget(name))
            IGSConnection.getChatter().notifyOffline(name);

        // Remove from trail list
        IGSMainWindow.getIGSConnection().getIGSReader().removeTrail(name);
    } //}}}

    //{{{ hasPlayer() method
    /**
     *  Description of the Method
     *
     *@param  name  Description of the Parameter
     *@return       Description of the Return Value
     */
    public boolean hasPlayer(String name) {
        return playerHash.containsKey(name);
    } //}}}

    //{{{ getPlayer() method
    /**
     *  Gets the player attribute of the AutoUpdater object
     *
     *@param  name  Description of the Parameter
     *@return       The player value
     */
    public Player getPlayer(String name) {
        return (Player)(playerHash.get(name));
    } //}}}

    //{{{ synchPlayerTable() method
    /**  Description of the Method */
    private void synchPlayerTable() {
        // Do not synch if available only button is enabled, as the information
        // from this autoupdater is not sufficient to determine X flags
        if (!IGSConnection.getPlayerTable().isVisible() ||
                IGSConnection.getPlayerTable().availableOnly() ||
                playerHash.size() == 0)
            return;

        // System.err.println("synchPlayerTable");

        IGSConnection.getPlayerTable().doClear();

        IGSRank upper = null;
        IGSRank lower = null;
        try {
            upper = new IGSRank(IGSConnection.getPlayerTable().getUpperRank());
            lower = new IGSRank(IGSConnection.getPlayerTable().getLowerRank());
        } catch (NumberFormatException e) {
            return;
        }

        for (Enumeration e = playerHash.elements(); e.hasMoreElements(); ) {
            try {
                Player p = (Player)e.nextElement();

                // Only check rank range if Friends button is not enabled.
                if (!IGSConnection.getPlayerTable().friendsOnly()) {
                    int c1 = p.getRank().compareTo(upper);
                    int c2 = p.getRank().compareTo(lower);

                    if ((c1 <= 0 && c2 >= 0) || (c1 >= 0 && c2 <= 0))
                        IGSConnection.getPlayerTable().addPlayer(p);
                }
                else
                    IGSConnection.getPlayerTable().addPlayer(p);
            } catch (NullPointerException ex) {}
        }

        IGSConnection.getPlayerTable().sortTable();

        // System.err.println("Number of internal players: " + playerHash.size());
    } //}}}

    //{{{ addGame(int, String, IGSRank, String, IGSRank) method
    /**
     *  Adds a feature to the Game attribute of the AutoUpdater object
     *
     *@param  gameID     The feature to be added to the Game attribute
     *@param  whiteName  The feature to be added to the Game attribute
     *@param  whiteRank  The feature to be added to the Game attribute
     *@param  blackName  The feature to be added to the Game attribute
     *@param  blackRank  The feature to be added to the Game attribute
     */
    public void addGame(int gameID, String whiteName, IGSRank whiteRank, String blackName, IGSRank blackRank) {
        // System.err.println("addGame: " + gameID + ": " + whiteName + " " + whiteRank + " " + blackName + " " + blackRank);
        addGame(new Game(gameID, whiteName, whiteRank, blackName, blackRank, 0, 0, 0, 0, 0, "", 0, false));
    } //}}}

    //{{{ addGame(Game) method
    /**
     *  Adds a feature to the Game attribute of the AutoUpdater object
     *
     *@param  g  The feature to be added to the Game attribute
     */
    public void addGame(Game g) {
        gamesHash.put(new Integer(g.getGameID()), g);

        // Add an entry in both players. If not existing, also add the player
        if (hasPlayer(g.getWhiteName()))
            getPlayer(g.getWhiteName()).setGame(g.getGameID());
        else
            addPlayer(new Player(g.getWhiteName(), g.getWhiteRank(), "", g.getGameID(), 0, ""));
        if (hasPlayer(g.getBlackName()))
            getPlayer(g.getBlackName()).setGame(g.getGameID());
        else
            addPlayer(new Player(g.getBlackName(), g.getBlackRank(), "", g.getGameID(), 0, ""));

        // If available button is selected, remove the players from the list
        if (IGSConnection.getPlayerTable().availableOnly()) {
            IGSConnection.getPlayerTable().removePlayer(g.getWhiteName());
            IGSConnection.getPlayerTable().removePlayer(g.getBlackName());
            IGSConnection.getPlayerTable().setTotal();
        }
    } //}}}

    //{{{ removeGame() method
    /**
     *  Description of the Method
     *
     *@param  gameID  Description of the Parameter
     *@param  result  Description of the Parameter
     */
    public void removeGame(int gameID, String result) {
        // System.err.println("removeGame: " + gameID + ": " + result);
        gamesHash.remove(new Integer(gameID));

        // Remove the game from both players, if existing in the hashtable
        removePlayersOfGame(gameID);

        if (IGSConnection.getGameObserver().observesGame(gameID)) {
            // Ignore adjourn messages, they are repetitive
            if (result.endsWith("has adjourned."))
                return;
            // Score following? Parse status matrix. Else the flag must not be set.
            if (result.startsWith("W ")) {
                IGSReader.requestRefresh = gameID;
            }
            IGSConnection.getGameObserver().saveResult(gameID, result, true);
        }
        // Unfortunately the autoupdate message comes after the adjourn message
        else if (result.endsWith("has adjourned.") &&
                gameID == IGSConnection.getGameObserver().getLastAdjournID()) {
            try {
                int n = result.indexOf(":") + 2;
                String whiteName = result.substring(n, (n = result.indexOf(" vs ", n))).trim();
                String blackName = result.substring(n + 4, result.indexOf(" has ", n + 4)).trim();
                // Remember player names of observed adjourned games for adjourn reload.
                adjournReloadVector.add(new Couple(whiteName, blackName));
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Error parsing player names: " + e);
            }
        }
    } //}}}

    //{{{ hasGame() method
    /**
     *  Description of the Method
     *
     *@param  gameID  Description of the Parameter
     *@return         Description of the Return Value
     */
    public boolean hasGame(int gameID) {
        return gamesHash.containsKey(new Integer(gameID));
    } //}}}

    //{{{ getGame() method
    /**
     *  Gets the game attribute of the AutoUpdater object
     *
     *@param  gameID  Description of the Parameter
     *@return         The game value
     */
    public Game getGame(int gameID) {
        return (Game)gamesHash.get(new Integer(gameID));
    } //}}}

    //{{{ synchGamesTable() method
    /**  Description of the Method */
    private void synchGamesTable() {
        if (!IGSConnection.getGamesTable().isVisible() ||
                gamesHash.size() == 0)
            return;

        // System.err.println("synchGamesTable");

        IGSConnection.getGamesTable().doClear();

        for (Enumeration e = gamesHash.elements(); e.hasMoreElements(); ) {
            try {
                IGSConnection.getGamesTable().addGame((Game)e.nextElement());
            } catch (NullPointerException ex) {}
        }

        IGSConnection.getGamesTable().sortTable();

        // System.err.println("Number of internal games: " + gamesHash.size());
    } //}}}

    //{{{ finishAutoupdate() method
    /**  Stop this thread */
    public void finishAutoupdate() {
        clearHashs();
        threadSuspended = true;
    } //}}}

    //{{{ clearHashs() method
    /**  Clear the hashtables with the players and games */
    public void clearHashs() {
        playerHash.clear();
        gamesHash.clear();
    } //}}}

    //{{{ removePlayersOfGame() method
    /**
     *  Description of the Method
     *
     *@param  gameID  Description of the Parameter
     */
    private void removePlayersOfGame(int gameID) {
        // Loop players in hashtable and search the game id
        for (Enumeration e = playerHash.elements(); e.hasMoreElements(); ) {
            Player p = (Player)e.nextElement();
            if (p.getGame() == gameID)
                p.setGame(0);
        }
    } //}}}

    //{{{ notifyReload() method
    /**
     *  Check if a reloaded game should be continued observing
     *
     *@param  gameID     Game ID
     *@param  whiteName  White player name
     *@param  blackName  Black player name
     */
    private void notifyReload(int gameID, String whiteName, String blackName) {
        for (Enumeration e = adjournReloadVector.elements(); e.hasMoreElements(); ) {
            Couple c = (Couple)e.nextElement();
            if (c.getWhiteName().equals(whiteName) && c.getBlackName().equals(blackName)) {
                IGSConnection.reloadObserve(gameID, c);
                adjournReloadVector.remove(c);
                break;
            }
        }
    } //}}}

    //{{{ addAdjournOpponent() method
    /**
     *  Add an opponents name to the notify list. When he reconnects, the user gets notified.
     *
     *@param  name  The feature to be added to the AdjournOpponent attribute
     */
    public void addAdjournOpponent(String name) {
        System.err.println("addAdjournOpponent: " + name);
        adjournReloadOwnVector.add(name);
    } //}}}
}

