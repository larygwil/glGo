/*
 *  Server.java
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
package ggo.utils;

import java.net.*;
import java.io.*;
import ggo.gGo;

/**
 *  Server running on localhost, listening to new gGo instances. If a instance is started, forward
 *  the sgf file to an already running gGo. This way we don't need to start a VM again.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:56 $
 */
public class Server extends Thread {
    //{{{ private members
    private boolean runMe = true;
    private int port;
    private ServerSocket serverSocket;
    //}}}

    //{{{ Server constructor
    /**
     *Constructor for the Server object
     *
     *@param  port  Port used on localhost
     */
    public Server(int port) {
        super("ggoserver");
        this.port = port;
        setDaemon(true);
    } //}}}

    //{{{ init() method
    /**
     *  Init and start the server
     *
     *@return    True if server started, else false
     */
    public boolean init() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Failed to open server on port: " + port);
            runMe = false;
            return false;
        }
        return true;
    } //}}}

    //{{{ run() method
    /**  Main processing method for the Server object */
    public void run() {
        System.err.println("Starting server on localhost " + port);

        while (runMe && !isInterrupted()) {
            Socket client = null;
            try {
                client = serverSocket.accept();
                client.setSoTimeout(1000);

                System.err.println("Client connected.");

                DataInputStream in = new DataInputStream(client.getInputStream());
                handleClientConnection(in);
            } catch (IOException e) {
                System.err.println("Server failed to listen on port: " + e);
                runMe = false;
            }
        }
    } //}}}

    //{{{ closeAll() method
    /**  Close socket */
    public void closeAll() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Failed to close server socket: " + e);
        }
        System.err.println("Server: closed all");
    } //}}}

    //{{{ handleClientConnection() method
    /**
     *  Read input from client
     *
     *@param  in  Input stream of the connected client
     */
    private void handleClientConnection(DataInputStream in) {
        // Read sgf filename
        String fileName = "";
        try {
            while (true) {
                char c = in.readChar();
                if (c == '\0')
                    break;
                fileName += c;
            }
        } catch (IOException e) {
            System.err.println("Failed to read from client stream: " + e);
        }
        System.err.println("Filename: " + fileName);

        if (fileName.length() > 0)
            gGo.openNewMainFrame(fileName, true);
    } //}}}
}

