/*
 *  UserDefs.java
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

/**
 * Simple class containing the information of the user defaults and toggle settings.
 * This is seperated from PlayerInfo as it is only required for one user.
 *
 * Parsing of the defaults and toggle Strings is done within this class.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/09/25 17:52:00 $
 */
public class UserDefs {
    public boolean open, looking, quiet, shouts, chatter, kibitz, automail, bell;
    public int time, size, byotime, byostones;

    /**
     *Constructor for the UserDefs object
     *
     *@param  defs     String with the user defaults
     *@param  toggles  String with the user toggles
     */
    public UserDefs(String defs, String toggles) {
        parseDefs(defs);
        parseToggles(toggles);
    }

    /**
     *  Parse the line with the user defaults
     *
     *@param  defs  String to parse
     */
    private void parseDefs(String defs) {
        // time 1, size 19, byo-yomi time 10, byo-yomi stones 10

        try {
            int pos;
            String timeStr = defs.substring(5, (pos = defs.indexOf(", "))).trim();
            String sizeStr = defs.substring(defs.indexOf("size ", pos++) + 5, (pos = defs.indexOf(", ", pos))).trim();
            String byoStr = defs.substring(defs.indexOf("byo-yomi time ", pos++) + 14, (pos = defs.indexOf(", ", pos))).trim();
            String byoStonesStr = defs.substring(defs.indexOf("byo-yomi stones ", pos) + 15, defs.length()).trim();

            time = Integer.parseInt(timeStr);
            size = Integer.parseInt(sizeStr);
            byotime = Integer.parseInt(byoStr);
            byostones = Integer.parseInt(byoStonesStr);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse user defs: " + e);
        }
    }

    /**
     *  DParse the line with the user toggles
     *
     *@param  toggles  String to parse
     */
    private void parseToggles(String toggles) {
        // Verbose  Bell  Quiet  Shout  Automail  Open  Looking  Client  Kibitz  Chatter
        //    Off    On    Off     On       Off    On      Off      On      On   On

        int pos = 0;
        final int sz = 10;
        int i = 0;
        Boolean[] b = new Boolean[sz];
        try {
            while ((pos = toggles.indexOf("O", pos)) != -1) {
                int p = toggles.indexOf(" ", pos);
                if (p == -1)
                    p = toggles.length();
                String s = toggles.substring(pos, p).trim();
                b[i++] = new Boolean(s.equals("On"));
                pos++;
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse toggles: " + e);
        }

        try {
            bell = b[1].booleanValue();
            quiet = b[2].booleanValue();
            shouts = b[3].booleanValue();
            automail = b[4].booleanValue();
            open = b[5].booleanValue();
            looking = b[6].booleanValue();
            kibitz = b[8].booleanValue();
            chatter = b[9].booleanValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Failed to parse toggles: " + e);
        }
    }
}

