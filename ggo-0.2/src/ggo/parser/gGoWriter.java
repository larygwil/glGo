/*
 *  gGoWriter.java
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
 *  A subclass of Writer. This class serializes the game tree, using the .ggo format.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/05 16:04:52 $
 */
public class gGoWriter extends Writer {
    //{{{ saveFile(String, BoardHandler) method
    /**
     *  Overwrites Writer.saveFile(String, BoardHandler). Entry method of this Writer class.
     *
     *@param  fileName      Filename of the file to save
     *@param  boardHandler  BoardHandler object this Writer instance is attached to
     *@return               True if successful, else false
     *@see                  ggo.parser.Writer#saveFile(String, BoardHandler)
     */
    public boolean saveFile(String fileName, BoardHandler boardHandler) {
        try {
            FileOutputStream fs = new FileOutputStream(fileName);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(boardHandler.getGameData());
            os.writeObject(boardHandler.getTree());
            os.close();
            fs.close();
        } catch (IOException e) {
            System.err.println("Failed to serialize game to file " + fileName + ": " + e);
            return false;
        }
        return true;
    } //}}}

    //{{{ saveFile(File, BoardHandler) method
    /**
     *  Overwrites Writer.saveFile(File, BoardHandler). Entry method of this Writer class.
     *
     *@param  file          File to save
     *@param  boardHandler  BoardHandler object this Writer instance is attached to
     *@return               True if successful, else false
     *@see                  ggo.parser.Writer#saveFile(File, BoardHandler)
     */
    public boolean saveFile(File file, BoardHandler boardHandler) {
        return saveFile(file.getAbsolutePath(), boardHandler);
    } //}}}

    //{{{ doWrite() method
    /**
     *  Overwrites Writer.doWrite(). Does nothing in this class.
     *
     *@return    Always null
     */
    String doWrite() {
        return null;
    } //}}}
}

