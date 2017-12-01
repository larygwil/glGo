/*
 *  GnuGoScorer.java
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

import ggo.gGo;
import ggo.utils.Settings;
import java.io.*;

/**
 *  Simple class to call an external gnugo process and let it guess the
 *  score of a file, usually a temporary file of the current game position.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:56 $
 */
public class GnuGoScorer {
    //{{{ guessScore() method
    /**
     *  Guess score from a given sgf file at a certain move.
     *
     *@param  fileName  File name of the sgf file to score
     *@param  move      Move number to score. If -1, score the last move
     *@return           String with GnuGos result. Null if gnugo was not found.
     */
    public static String guessScore(String fileName, int move) {
        String program =
                gGo.getSettings().getGnugoPath() +
                " --score estimate" +
                (move != -1 ? " -L " + move : " ") +
                " --quiet -l " + fileName;
        System.err.println("Executing: " + program);

        Process process;
        try {
            process = Runtime.getRuntime().exec(program);
        } catch (IOException e) {
            System.err.println("Failed to connect to gnugo.\n" + e);
            return null;
        }

        BufferedReader inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String input = "";
        String result = "";

        try {
            while ((input = inputStream.readLine()) != null)
                if (input.length() > 0)
                    result += input;
        } catch (IOException e) {
            System.err.println("Failed to read from gnugo: " + e);
        }

        return result;
    } //}}}
}

