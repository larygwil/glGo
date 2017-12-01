/*
 *  GTPConnection.java
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
import javax.swing.JOptionPane;
import ggo.Defines;
import ggo.gGo;
import ggo.gtp.*;

/**
 *  This class handles a connection to a GTP engine and forwards commands
 *  to the GTP output.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class GTPConnection implements Defines {
    //{{{ private members
    private Process process;
    private InputStream inputStream, errorStream;
    private OutputStream outputStream;
    private GTPInput gtpInput;
    private GTPError gtpError;
    private static PrintWriter gtpOutput = null;
    private static GTPGameHandler gameHandler = null;
    private static GTPMainFrame board = null;
    //}}}

    //{{{ GTPConnection constructor
    /**
     *Constructor for the GTPConnection object
     *
     *@param  board  Pointer to the board frame
     */
    public GTPConnection(GTPMainFrame board) {
        gameHandler = new GTPGameHandler();
        this.board = board;
    } //}}}

    //{{{ getGTPGameHandler() method
    /**
     *  Gets the gameHandler attribute of the GTPConnection class
     *
     *@return    The gameHandler value
     */
    public static GTPGameHandler getGTPGameHandler() {
        return gameHandler;
    } //}}}

    //{{{ getBoard() method
    /**
     *  Gets the board attribute of the GTPConnection class
     *
     *@return    The board value
     */
    public static GTPMainFrame getBoard() {
        return board;
    } //}}}

    //{{{ connect() method
    /**
     *  Connect to a GTP engine starting a seperate process
     *
     *@param  args  Array of command arguments. The first value is the programname, following
     *              values the parameters passed to the engine.
     */
    public void connect(String[] args) {
        if (args.length == 0) {
            System.err.println("No program to connect to given! Aborting connection...");
            return;
        }

        if (process != null) {
            System.err.println("A GTP process is already existing.\n" +
                    "gGo currently does not support more than one GTP connection.");
            return;
        }

        String program = args[0];
        for (int i = 1, sz = args.length; i < sz; i++)
            program += " " + args[i];

        System.err.println("Executing: " + program);

        try {
            process = Runtime.getRuntime().exec(program);
        } catch (IOException e) {
            System.err.println("Failed to connect to GTP engine.\n" + e);
            JOptionPane.showMessageDialog(board,
                    gGo.getGTPResources().getString("cannot_find_gnugo_error"),
                    PACKAGE + " " + gGo.getBoardResources().getString("error"),
                    JOptionPane.WARNING_MESSAGE);
            process = null;
            return;
        }

        inputStream = process.getInputStream();
        errorStream = process.getErrorStream();
        outputStream = process.getOutputStream();

        gtpInput = new GTPInput(inputStream);
        gtpInput.start();

        gtpError = new GTPError(errorStream);
        gtpError.start();

        gtpOutput = new PrintWriter(new BufferedOutputStream(outputStream));

        System.err.println("Connection to GTP engine established.");
    } //}}}

    //{{{ sendCommand() method
    /**
     *  Send a comment to the GTP engine
     *
     *@param  command  Command to send
     */
    public static synchronized void sendCommand(String command) {
        System.err.println("Sending command to GTP engine: " + command);

        try {
            GTPError.appendOutput(command);
            gtpOutput.println(command);
            gtpOutput.flush();
        } catch (NullPointerException e) {
            System.err.println("Failed to send command to GTP engine: " + e);
        }
    } //}}}

    //{{{ closeAll() method
    /**  Close all streams and quit reader thread */
    public void closeAll() {
        try {
            gtpInput.interrupt();
            gtpError.interrupt();
            try {
                gtpOutput.close();
                inputStream.close();
                errorStream.close();
                outputStream.close();
            } catch (IOException e) {
                System.err.println("Failed to close connection: " + e);
            }
        } catch (NullPointerException ex) {}
    } //}}}
}

