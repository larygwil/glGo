/*
 *  GTP.java
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
package ggo.gtp;

import java.io.*;
import ggo.gtp.*;
import ggo.GameData;

/**
 *  Main class for setting up a GTP connection to a go playing engine like GnuGo.
 * This class does nothing by itself, it organizes the data and distributes the
 * tasks to GTPConnection and GTPInput.
 * This class wraps the GTP code into one thread.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class GTP extends Thread {
    //{{{ private members
    private GTPConnection gtpConnection;
    private GTPMainFrame gtpMainFrame;
    private boolean runMe;
    //}}}

    //{{{ GPP() constructor
    /**
     *Constructor for the GTP object
     *
     *@param  args       Command arguments for GTP engine
     *@param  gtpConfig  Pointer to the GTP configuration object
     *@param  data       Pointer to the Game configuration object
     */
    public GTP(String args[], GTPConfig gtpConfig, GameData data) {
        gtpMainFrame = new GTPMainFrame();
        gtpConnection = new GTPConnection(gtpMainFrame);
        gtpConnection.connect(args);
        gtpMainFrame.getBoard().initGame(data, true);
        GTPConnection.getGTPGameHandler().initGame(gtpConfig);
    } //}}}

    //{{{ run() method
    /**  Main processing method for the GTP object */
    public void run() {
        runMe = true;
        while (runMe && !Thread.interrupted()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                runMe = false;
            }
        }
        System.err.println("GTP thread ended");
    } //}}}

    //{{{ init() method
    /**
     *  Start a new game reusing this engine and board
     *
     *@param  gtpConfig  Description of the Parameter
     */
    protected void init(GTPConfig gtpConfig) {
        GTPConnection.getGTPGameHandler().initGame(gtpConfig);
    } //}}}

    //{{{ getGTPMainFrame() method
    /**
     *  Gets the gTPMainFrame attribute of the GTP object
     *
     *@return    The gTPMainFrame value
     */
    public GTPMainFrame getGTPMainFrame() {
        return gtpMainFrame;
    } //}}}

    //{{{ close() method
    /**  Close connection to GTP engine and stop threads */
    public void close() {
        gtpConnection.closeAll();
        runMe = false;
    } //}}}
}

