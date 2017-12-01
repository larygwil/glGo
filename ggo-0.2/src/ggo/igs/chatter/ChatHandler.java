/*
 * ChatHandler.java
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
package ggo.igs.chatter;

import java.util.*;
import javax.swing.*;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.event.*;
import java.text.MessageFormat;
import ggo.igs.*;
import ggo.gGo;

/**
 * Distributer and controller for the multiple frame chat system. All
 * incoming and outcoming chats of each frame should pass this class.
 *
 * @author  Peter Strempel
 * @version $Revision: 1.8 $, $Date: 2002/09/21 12:39:56 $
 */
public class ChatHandler extends Hashtable implements IGSChatter {
    private ChatterPane chatterPane;
    private Chatter lastSource;
    private JToggleButton toggleButton;
    private ResourceBundle igs_player_resources;

    public ChatHandler(JToggleButton toggleButton) {
        igs_player_resources = gGo.getIGSPlayerResources();

        chatterPane = new ChatterPane(this);
        chatterPane.setVisible(false);
        this.toggleButton = toggleButton;
    }

    public JToggleButton getToggleButton() {
        return toggleButton;
    }

    void addChat(String name) {
        if (hasTarget(name)) {
            getChat(name).checkVisible();
            return;
        }

        String rank = null;
        try {
            if (IGSConnection.getAutoUpdater().hasPlayer(name))
                rank = IGSConnection.getAutoUpdater().getPlayer(name).getRank().toString();
        } catch (NullPointerException e) {}

        Chatter c = new ChatterInternalFrame(name, rank, this);
        ((ChatterInternalFrame)c).setLocation(size() * 20, size() * 20);
        chatterPane.addChat((ChatterInternalFrame)c);
        put(name, c);
        lastSource = c;
    }

    void removeChat(String name) {
        remove(name);
    }

    public boolean hasTarget(String name) {
        return containsKey(name);
    }

    private void append(Chatter c, String txt) {
        try {
            c.append(txt);
        } catch (NullPointerException e) { }
    }

    public void notifyOnline(String name) {
        // append(getChat(name), "[" + name + " is now online.]");
        append(getChat(name), MessageFormat.format(igs_player_resources.getString("notify_online"), new Object[] {name}));
    }

    public void notifyOffline(String name) {
        // append(getChat(name), "[" + name + " is now offline.]");
        append(getChat(name), MessageFormat.format(igs_player_resources.getString("notify_offline"), new Object[] {name}));
    }

    Chatter getChat(String name) {
        return (Chatter)get(name);
    }

    void sendChatToIGS(String target, String txt, Chatter source) {
        IGSConnection.sendCommand("tell " + target + " " + txt);
        lastSource = source;
    }

    public void recieveChat(String fromName, String txt) {
        if (!hasTarget(fromName))
            addChat(fromName);

        checkVisible();
        try {
            ((Chatter)getChat(fromName)).recieveChat(fromName, txt);
        } catch (NullPointerException e) {
            System.err.println("Failed to display chat: " + e);
        }
    }

    public void recieveChatError(String txt) {
        checkVisible();
        try {
            lastSource.recieveChatError(txt);
        } catch (NullPointerException e) {
            System.err.println("Failed to display chat error: " + e);
        }
    }

    void requestUserInfo(String target) {
        IGSConnection.sendCommand("stats " + target);
    }

    void requestAutomatch(String target) {
        IGSConnection.sendAutoMatch(target);
    }

    void requestMatch(String target) {
        IGSConnection.sendMatch(target);
    }

    void requestObserve(String target) {
        try {
            int game = IGSConnection.getAutoUpdater().getPlayer(target).getGame();
            if (game > 0)
                IGSConnection.startObserve(game);
        } catch (NullPointerException e) {}
    }

    int getFontSize() {
        return chatterPane.getFontSize();
    }

    public JButton getCloseButton() {
        return chatterPane.getCloseButton();
    }

    public JMenuItem getCloseMenuItem() {
        return chatterPane.getCloseMenuItem();
    }

    public void addWindowListener(WindowListener l) {
        chatterPane.addWindowListener(l);
    }

    public void setVisible(boolean b) {
        chatterPane.setVisible(b);
    }

    public void checkVisible() {
        chatterPane.checkVisible();
    }

    public void setInputFocus() {
        chatterPane.setInputFocus();
    }

    public void dispose() {
        chatterPane.dispose();
    }

    public void sendChat(String target, String txt, boolean sendIt) {
        checkVisible();
        setInputFocus();

        if (!hasTarget(target))
            addChat(target);

        try {
            Chatter c = getChat(target);
            try {
                c.append(IGSConnection.getLoginName() + ": " + txt);
            } catch (NullPointerException e) {
                c.append("-> " + target + ": " + txt);
            }
            c.checkVisible();
            c.setInputFocus();
            if (sendIt)
                sendChatToIGS(target, txt, c);
        } catch (NullPointerException e) {
            System.err.println("Failed to display chat: " + e);
        }
    }

    public void setName(String name) {
        addChat(name);
        checkVisible();
        chatterPane.toFront();
        // This is called from the playertable popup menu. Force inputfocus here.
        try {
            Chatter c = getChat(name);
            c.checkVisible();
            c.setInputFocus();
        } catch (NullPointerException e) {}
    }

    boolean getPlaySound() {
        return chatterPane.getPlaySound();
    }

    public void updateLookAndFeel() {
        chatterPane.updateLookAndFeel();
    }

    public void changeFontSize() {
        chatterPane.changeFontSize();
    }

    public Point getLocation() {
        return chatterPane.getLocation();
    }

    public Dimension getSize() {
        return chatterPane.getSize();
    }

    public ResourceBundle getIGSPlayerResources() {
        return igs_player_resources;
    }
}
