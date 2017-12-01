/*
 *  BozoHandler.java
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

import ggo.gGo;
import ggo.utils.Settings;
import java.util.*;

/**
 *  Storage class to manage the friends and bozo list. This class saves itself
 *  by serialisation.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/10/17 19:04:26 $
 */
public class BozoHandler extends Hashtable {
    //{{{ Public members
    /**  Neutral status */
    public final static int PLAYER_STATUS_NEUTRAL = 0;
    /**  Friend status */
    public final static int PLAYER_STATUS_FRIEND = 1;
    /**  Bozo status */
    public final static int PLAYER_STATUS_BOZO = 2;
    //}}}

    //{{{ BozoHandler() constructor
    /**Constructor for the BozoHandler object */
    public BozoHandler() {
        parseList(gGo.getSettings().getFriends(), PLAYER_STATUS_FRIEND);
        parseList(gGo.getSettings().getBozos(), PLAYER_STATUS_BOZO);
    } //}}}

    //{{{ parseList() method
    /**
     *  Parse the save list read from Settings
     *
     *@param  list    String with the players
     *@param  status  Parsing for friend or bozo status
     */
    private void parseList(String list, int status) {
        if (list == null || list.length() == 0)
            return;

        int pos = 0;
        int oldpos = 0;
        while ((pos = list.indexOf("#", oldpos)) != -1) {
            String s = list.substring(oldpos, pos);
            put(s, new Integer(status));
            oldpos = pos + 1;
        }
    } //}}}

    //{{{ getBozoStatus() method
    /**
     *  Gets bozo status of a player
     *
     *@param  name  Player name
     *@return       The bozo status
     */
    public int getBozoStatus(String name) {
        try {
            if (isEmpty() || !containsKey(name))
                return PLAYER_STATUS_NEUTRAL;
            return ((Integer)get(name)).intValue();
        } catch (NullPointerException e) {
            return PLAYER_STATUS_NEUTRAL;
        }
    } //}}}

    //{{{ setBozoStatus() method
    /**
     *  Sets the bozo status of a player
     *
     *@param  name        Player name
     *@param  bozoStatus  The new bozo status
     */
    public void setBozoStatus(String name, int bozoStatus) {
        try {
            if (bozoStatus == PLAYER_STATUS_NEUTRAL)
                remove(name);
            else
                put(name, new Integer(bozoStatus));
            saveLists();
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ getFriends() method
    /**
     *  Gets a sorted array of all friends
     *
     *@return    The friends array
     */
    public Object[] getFriends() {
        return getFriendsBozos(PLAYER_STATUS_FRIEND);
    } //}}}

    //{{{ getBozos() method
    /**
     *  Gets a sorted array of all bozos
     *
     *@return    The bozos array
     */
    public Object[] getBozos() {
        return getFriendsBozos(PLAYER_STATUS_BOZO);
    } //}}}

    //{{{ getFriendsBozos() method
    /**
     *  Gets the sorted array of all friends of bozos
     *
     *@param  status  Friend of bozo status
     *@return         The friends or bozos array
     */
    private Object[] getFriendsBozos(int status) {
        Vector v = new Vector();
        for (Enumeration e = keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            if (getBozoStatus(name) == status)
                v.add(name);
        }
        // Sort the name alphabetically
        Object[] oa = v.toArray();
        Arrays.sort(oa);
        return oa;
    } //}}}

    //{{{ saveLists() method
    /**  Convert and save the lists */
    public void saveLists() {
        gGo.getSettings().setFriends(convertFriends());
        gGo.getSettings().setBozos(convertBozos());
        gGo.getSettings().saveSettings();
    } //}}}

    //{{{ convertFriends() method
    /**
     *  Convert friends list to a String
     *
     *@return    Converted String
     */
    private String convertFriends() {
        Object[] o = getFriends();
        String s = "";
        for (int i = 0, sz = o.length; i < sz; i++)
            s += (String)o[i] + "#";
        return s;
    } //}}}

    //{{{ convertBozos() method
    /**
     *  Convert bozos list to a String
     *
     *@return    Converted String
     */
    private String convertBozos() {
        Object[] o = getBozos();
        String s = "";
        for (int i = 0, sz = o.length; i < sz; i++)
            s += (String)o[i] + "#";
        return s;
    } //}}}

    //{{{ toString() method
    /**
     *  Convert this Hashtable to a string. For debugging
     *
     *@return    Converted String
     */
    public String toString() {
        String s = "Bozo list:\n";
        for (Enumeration e = keys(); e.hasMoreElements(); ) {
            String name = (String)e.nextElement();
            s += name + " - " + (Integer)get(name) + "\n";
        }
        return s;
    } //}}}
}

