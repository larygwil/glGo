/*
 * PlayerInfo.java
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

import ggo.igs.gui.PlayerInfoDialog;

/**
 *  This class is used to store player informations from stats, stored and results commands.
 *  The inforation of this class is displayed in the PlayerInfoDialog class.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.9 $, $Date: 2002/10/18 00:03:06 $
 *@see        ggo.igs.gui.PlayerInfoDialog
 */
public class PlayerInfo {
    private String name, gameSetting, language, rating, rank, access, email, regDate, info, defs, results, storedGames, sgfGames;
    private int ratedGames, wins, losses, stored;
    private boolean trailed;
    private PlayerInfoDialog dialog;
    private UserDefs userDefs;

    /** PlayerInfo constructor */
    public PlayerInfo(String name, String gameSetting, String language, String rating, String rank,
    int ratedGames, int wins, int losses, String access, String email, String regDate,
    String info, String defs) {
        this.name = name;
        this.gameSetting = gameSetting;
        this.language = language;
        this.rating = rating;
        this.rank = rank;
        this.ratedGames = ratedGames;
        this.wins = wins;
        this.losses = losses;
        this.access = access;
        this.email = email;
        this.regDate = regDate;
        this.info = info;
        this.defs = defs;
        stored = -1;
        trailed = false;
        userDefs = null;
    }

    public String getName() { return name; }
    public String getGameSetting() { return gameSetting; }
    public String getLanguage() { return language; }
    public String getRating() { return rating; }
    public String getRank() { return rank; }
    public int getRatedGames() { return ratedGames; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public String getAccess() { return access; }
    public String getEmail() { return email; }
    public String getRegDate() { return regDate; }
    public String getInfo() { return info; }
    public String getDefs() { return defs; }
    public String getResults() { return results; }
    public void setResults(String s) { results = s; }
    public int getStored() { return stored; }
    public void setStored(int i) { stored = i; }
    public String getStoredGames() { return storedGames; }
    public void setStoredGames(String s) { storedGames = s; }
    public String getSGFGames() { return sgfGames; }
    public void setSGFGames(String s) { sgfGames = s; }
    public boolean getTrailed() { return trailed; }
    public void setTrailed(boolean b) { trailed = b; }
    public UserDefs getUserDefs() { return userDefs; }
    public void setUserDefs(UserDefs ud) { userDefs = ud; }
    public void setDialog(PlayerInfoDialog dlg) { dialog = dlg; }
    public PlayerInfoDialog getDialog() { return dialog; }

    public int getPlayingGame() {
        try {
            // Playing in game:  118 (I)
            int pos = access.indexOf("Playing in game:  ");
            if (pos == -1)
                return -1;
            pos += 18;
            int game = Integer.parseInt(access.substring(pos, access.indexOf(" ", pos+1)).trim());
            return game;
        } catch (StringIndexOutOfBoundsException e) {
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
