/*
 *  SGFWriter.java
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
package ggo.parser;

import ggo.*;

/**
 *  A subclass of Writer. This class converts a game into SGF format and saves the
 *  SGF string as the specified file.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/05 11:22:49 $
 */
public class SGFWriter extends Writer {
    private StringBuffer stream;
    private boolean isRoot;

    //{{{ doWrite() method
    /**
     *  Writes a given game tree to a string in sgf format.
     *
     *@return    String in sgf format
     *@see       ggo.parser.Writer#doWrite()
     */
    String doWrite() {
        Move root;
        try {
            root = boardHandler.getTree().getRoot();
        } catch (NullPointerException e) {
            System.err.println("SGFWriter.doWrite() - No game to save: " + e);
            e.printStackTrace();
            return null;
        }

        GameData data = boardHandler.getGameData();
        if (data == null) {
            System.err.println("SGFParser.doWrite() - No game data!");
            return null;
        }

        stream = new StringBuffer();

        // Traverse the tree recursive in pre-order
        isRoot = true;
        traverse(root, data);
        stream.append("\n");

        return stream.toString();
    } //}}}

    //{{{ writeGameHeader() method
    /**
     *  Print the sgf headers to the stream
     *
     *@param  data  Pointer of the GameData object
     */
    private void writeGameHeader(GameData data) {
        try {
            // Assemble data for root node
            stream.append(";GM[1]FF[4]" +  // We play Go, we use FF 4
            "CA[" + data.charset + "]" +  // Charset
            "AP[" + PACKAGE + ":" + VERSION + "]" // Application name : Version
             + "ST[1]"); // We show vars of current node
            if (data.gameName != null && data.gameName.length() > 0) // Skip game name if empty
                stream.append("GN[" + data.gameName + "]\n"); // Game Name
            else
                stream.append("\n");

            stream.append("SZ[" + data.size + "]" +  // Board size
            (data.handicap > 0 ? "HA[" + data.handicap + "]" : "") +  // Handicap
            "KM[" + data.komi + "]\n"); // Komi

            if (data.playerWhite != null && data.playerWhite.length() > 0)
                stream.append("PW[" + data.playerWhite + "]"); // White name

            if (data.rankWhite != null && data.rankWhite.length() > 0)
                stream.append("WR[" + data.rankWhite + "]"); // White rank

            if (data.playerBlack != null && data.playerBlack.length() > 0)
                stream.append("PB[" + data.playerBlack + "]"); // Black name

            if (data.rankBlack != null && data.rankBlack.length() > 0)
                stream.append("BR[" + data.rankBlack + "]"); // Black rank

            if (data.result != null && data.result.length() > 0)
                stream.append("RE[" + data.result + "]"); // Result

            if (data.date != null && data.date.length() > 0)
                stream.append("DT[" + data.date + "]"); // Date

            if (data.place != null && data.place.length() > 0)
                stream.append("PC[" + data.place + "]"); // Place

            if (data.copyright != null && data.copyright.length() > 0)
                stream.append("CP[" + data.copyright + "]"); // Copyright

            stream.append("\n");
        } catch (NullPointerException e) {
            System.err.println("Failed to write game: " + e);
        }
    } //}}}

    //{{{ traverse() method
    /**
     *  Method to traverse the tree and write the moves to sgf
     *
     *@param  t     Current move
     *@param  data  Pointer to the GameData object
     */
    private void traverse(Move t, GameData data) {
        stream.append("(");
        do {
            if (isRoot) {
                writeGameHeader(data);
                stream.append(t.saveMove(isRoot));
                isRoot = false;
            }
            else
                stream.append(t.saveMove(false));

            Move tmp = t.son;
            if (tmp != null && tmp.brother != null) {
                do {
                    stream.append("\n");
                    traverse(tmp, null);
                } while ((tmp = tmp.brother) != null);
                break;
            }
        } while ((t = t.son) != null);
        stream.append("\n)");
    } //}}}
}

