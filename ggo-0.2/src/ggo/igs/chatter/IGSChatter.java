/*
 *  IGSChatter.java
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *  Interface for all classes that implement a chatter system.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:56 $
 */
public interface IGSChatter {
    /**  Chatsystem type: multiple chat frames inside a desktop pane */
    public final static int IGSCHATTER_DESKTOPPANE = 0;
    /**  Chatsystem type: One frame for all */
    public final static int IGSCHATTER_ONEFRAME = 1;
    /**  Chatsystem type: multiple individual chat frames - NOT IMPLEMENTED YET */
    public final static int IGSCHATTER_SINGLEFRAMES = 2;

    public JButton getCloseButton();
    public JMenuItem getCloseMenuItem();
    public void addWindowListener(WindowListener l);
    public void setVisible(boolean b);
    public void checkVisible();
    public void setInputFocus();
    public void dispose();
    public void sendChat(String target, String txt, boolean sendIt);
    public void setName(String name);
    public void recieveChat(String fromName, String txt);
    public void recieveChatError(String txt);
    public void updateLookAndFeel();
    public void changeFontSize();
    public boolean hasTarget(String name);
    public void notifyOnline(String name);
    public void notifyOffline(String name);
    public Point getLocation();
    public Dimension getSize();
}

