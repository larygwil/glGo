/*
 *  gGoParser.java
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
import java.io.*;

/**
 *  This class restores serialized game trees, using the .ggo file extension.
 *  This is an gGo-specific file format.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/05 16:04:43 $
 */
public class gGoParser extends Parser {
    //{{{ doParseFile() method
    /**
     *  Entry method to start parsing of the given file, overwrites Parser.doParseFile(String)
     *
     *@param  fileName  File to parse
     *@return           True if parsing was successful, else false
     *@see              ggo.parser.Parser#doParseFile(String)
     */
    boolean doParseFile(String fileName) {
        GameData gameData = null;
        Tree tree = null;
        try {
            FileInputStream fs = new FileInputStream(fileName);
            ObjectInputStream is = new ObjectInputStream(fs);
            gameData = (GameData)is.readObject();
            tree = (Tree)is.readObject();
            is.close();
            fs.close();
        } catch (InvalidClassException e) {
            System.err.println("Failed to restore game: " + e +
                    "\nThis savefile is incompatible with your version of " + PACKAGE + ".");
            return false;
        } catch (IOException e) {
            System.err.println("Failed to restore game: " + e);
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to restore game: " + e);
            return false;
        }

        if (gameData == null || tree == null) {
            System.err.println("Failed to restore game.");
            return false;
        }

        initGame(gameData);
        boardHandler.setTree(tree);
        return true;
    } //}}}
}

