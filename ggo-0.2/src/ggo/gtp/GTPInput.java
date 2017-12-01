/*
 *  GTPInput.java
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
import java.util.ArrayList;
import ggo.gtp.*;
import ggo.utils.Position;

/**
 *  Parser class for the output sent from a GTP engine. The output is parsed and
 *  forwarder to the GTPGameHandler.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:55 $
 */
public class GTPInput extends Thread implements GTPDefines {
    //{{{ private members
    private BufferedReader bufIn;
    //}}}

    //{{{ GTPInput() constructor
    /**
     *Constructor for the GTPInput object
     *
     *@param  in  Pointer to the gnugo process input stream
     */
    public GTPInput(InputStream in) {
        bufIn = new BufferedReader(new InputStreamReader(in));
    } //}}}

    //{{{ run() method
    /**  Main processing method for the GTPInput object */
    public void run() {
        String input;

        try {
            while ((input = bufIn.readLine()) != null && !isInterrupted())
                doParse(input);
        } catch (IOException e) {
            System.err.println("Failed to read from input stream: " + e);
        } catch (NullPointerException e) {
            System.err.println("Failed to read from input stream: " + e);
        }
    } //}}}

    //{{{ doParse() method
    /**
     *  Parse GTP engine output
     *
     *@param  toParse  String from GTP engine to parse
     */
    private synchronized void doParse(String toParse) {
        GTPError.appendOutput(toParse);

        if (toParse.length() > 0) {
            if (toParse.startsWith("? ")) {
                parseError(toParse.substring(2));
            }
            else if (toParse.startsWith("= ")) {
                parseOk(toParse.substring(2));
            }
        }
    } //}}}

    //{{{ parseError() method
    /**
     *  Parse an error output from GTP engine
     *
     *@param  toParse  String to parse
     */
    private synchronized void parseError(String toParse) {
        System.err.println("Error: " + toParse);
    } //}}}

    //{{{ parseOk() method
    /**
     *  Parse an Ok output from GTP engine
     *
     *@param  toParse  String to parse
     */
    private synchronized void parseOk(String toParse) {
        if (toParse.length() == 0)
            return;

        System.err.println("Ok: " + toParse);

        switch (GTPConnection.getGTPGameHandler().getState()) {
            case STATE_UNKNOWN:
                break;
            case STATE_MOVE_BLACK:
                parseMove(toParse);
                break;
            case STATE_MOVE_WHITE:
                parseMove(toParse);
                break;
            case STATE_SETUP_HANDICAP:
                parseHandicap(toParse);
                break;
            case STATE_RESUME_GAME:
                parseTurn(toParse);
                break;
            case STATE_SCORING:
                GTPConnection.getBoard().showScore(toParse);
                break;
        }
    } //}}}

    //{{{ parseMove() method
    /**
     *  Parse a move from GTP engine
     *
     *@param  toParse  String to parse
     */
    private synchronized void parseMove(String toParse) {
        int boardSize = GTPConnection.getGTPGameHandler().getBoardSize();
        int x = 0;
        int y = 0;

        if (toParse.equals("PASS"))
            x = y = -1;
        else {
            try {
                x = toParse.charAt(0) - 'A' + (toParse.charAt(0) < 'J' ? 1 : 0);
                y = boardSize - Integer.parseInt(toParse.substring(1, toParse.length())) + 1;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing move (" + toParse + "): " + e);
                return;
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Error parsing move (" + toParse + "): " + e);
                return;
            }
        }

        GTPConnection.getGTPGameHandler().recieveMoveFromGTP(x, y);
    } //}}}

    //{{{ parseHandicap() method
    /**
     *  Parse handicap setup
     *
     *@param  toParse  String to parse
     */
    private synchronized void parseHandicap(String toParse) {
        int boardSize = GTPConnection.getGTPGameHandler().getBoardSize();
        ArrayList handicap = new ArrayList();

        int pos = 0;
        while (pos != -1) {
            String sub = toParse.substring(pos++,
                    ((pos = toParse.indexOf(" ", pos)) < toParse.length() && pos > 0 ? pos :
                    toParse.length())).trim();

            if (sub.length() == 0)
                break;

            int x = 0;
            int y = 0;

            try {
                x = sub.charAt(0) - 'A' + (sub.charAt(0) < 'J' ? 1 : 0);
                y = boardSize - Integer.parseInt(sub.substring(1, sub.length())) + 1;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing move (" + sub + "): " + e);
                return;
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Error parsing move (" + sub + "): " + e);
                return;
            }

            Position p = new Position(x, y);
            handicap.add(p);
        }

        GTPConnection.getGTPGameHandler().recieveHandicapFromGTP(handicap);
    } //}}}

    //{{{ parseTurn() method
    /**
     *  Parse which players turn it is after a sgf file was loaded to resume a game
     *
     *@param  toParse  String to parse
     */
    private void parseTurn(String toParse) {
        if (toParse.equals("black"))
            GTPConnection.getGTPGameHandler().startGame(true);
        else if (toParse.equals("white"))
            GTPConnection.getGTPGameHandler().startGame(false);
        else
            System.err.println("Don't know whose turn it is! Bailing out...");
    } //}}}
}

