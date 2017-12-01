/*
 *  WriterFactory.java
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

import ggo.BoardHandler;
import java.io.File;
import ggo.utils.Utils;

/**
 *  Factory class for creation of <code>Writer</code> objects. The <code>saveSGF</code>
 *  methods offer a convinient way to save in SGF format. Saving XML is not yet supported.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/10/05 16:05:11 $
 */
public class WriterFactory {
    //{{{ WriterFactory() null constructor
    /** Null constructor. Don't allow instantiation of this class */
    private WriterFactory() { } //}}}

    //{{{ createSGFWriter() method
    /**
     *  Create a new SGFWriter instance
     *
     *@return    The new SGFWriter instance
     */
    public static SGFWriter createSGFWriter() {
        return new SGFWriter();
    } //}}}

    //{{{ creategGoWriter() method
    /**
     *  Create a new gGoWriter instance
     *
     *@return    The new gGoWriter instance
     */
    public static gGoWriter creategGoWriter() {
        return new gGoWriter();
    } //}}}

    //{{{ saveFile(String, BoardHandler) method
    /**
     *  Save a file. This method will try to detect the format using the filename extension.
     *  If no extension is given, SGF format is used by default. Saving XML is not yet supported.
     *
     *@param  fileName      File to save
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean saveFile(String fileName, BoardHandler boardHandler) {
        String extension = Utils.getExtension(fileName);

        if (extension != null && extension.equals("sgf"))
            return saveSGF(fileName, boardHandler);
        else if (extension != null && extension.equals("xml")) {
            // Bad user...
            System.err.println("XML saving not yet implemented. Sorry.");
            return false;
        }
        else if (extension != null && extension.equals("ggo"))
            return savegGo(fileName, boardHandler);
        else {
            System.err.println("Unknown filename suffix, using SGF.");
            return saveSGF(fileName, boardHandler);
        }
    } //}}}

    //{{{ saveFile(File, BoardHandler) method
    /**
     *  Save a file. This method will try to detect the format using the filename extension.
     *  If no extension is given, SGF format is used by default. Saving XML is not yet supported.
     *
     *@param  file      File to save
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean saveFile(File file, BoardHandler boardHandler) {
        String extension = Utils.getExtension(file);

        if (extension != null && extension.equals("sgf"))
            return saveSGF(file, boardHandler);
        else if (extension != null && extension.equals("xml")) {
            // Bad user...
            System.err.println("XML saving not yet implemented. Sorry.");
            return false;
        }
        else if (extension != null && extension.equals("ggo"))
            return savegGo(file, boardHandler);
        else {
            System.err.println("Unknown filename suffix, using SGF.");
            return saveSGF(file, boardHandler);
        }
    } //}}}

    //{{{ saveSGF(String, BoardHandler) method
    /**
     *  Save file using SGF format, using the specified BoardHandler object
     *
     *@param  fileName      Filename of the file to save to
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     *@see                  #saveSGF(File, BoardHandler)
     */
    public static boolean saveSGF(String fileName, BoardHandler boardHandler) {
        return createSGFWriter().saveFile(fileName, boardHandler);
    } //}}}

    //{{{ saveSGF(File, BoardHandler) method
    /**
     *  Save file using SGF format, using the specified BoardHandler object
     *
     *@param  file          File to save to
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     *@see                  #saveSGF(String, BoardHandler)
     */
    public static boolean saveSGF(File file, BoardHandler boardHandler) {
        return createSGFWriter().saveFile(file, boardHandler);
    } //}}}

    //{{{ savegGo()String, BoardHandler method
    /**
     *  Save file using SGF format, using the specified BoardHandler object.
     *  This is a gGo-specific format. Basically, it serializes the Tree.
     *
     *@param  fileName      File to save to
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean savegGo(String fileName, BoardHandler boardHandler) {
        return creategGoWriter().saveFile(fileName, boardHandler);
    } //}}}

    //{{{ savegGo(File, BoardHandler) method
    /**
     *  Save file using SGF format, using the specified BoardHandler object.
     *  This is a gGo-specific format. Basically, it serializes the Tree.
     *
     *@param  file      File to save to
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean savegGo(File file, BoardHandler boardHandler) {
        return creategGoWriter().saveFile(file, boardHandler);
    } //}}}
}

