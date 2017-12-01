/*
 *  ParserFactory.java
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
import ggo.utils.Utils;

/**
 *  Factory class for creation of <code>Parser</code> objects. The <code>loadXXX</code>
 *  methods offer a convinient way to load specified formats. The <code>loadFile</code>
 *  method will try to detect the format by the used file extension. If no file extension
 *  is available, it will use SGF.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/10/08 13:52:50 $
 */
public class ParserFactory {
    //{{{ ParserFactory() null constructor
    /** Null constructor. Don't allow instantiation of this class */
    private ParserFactory() { } //}}}

    //{{{ createSGFParser() method
    /**
     *  Create a new SGFParser instance
     *
     *@return    The new SGFParser instance
     */
    public static SGFParser createSGFParser() {
        return new SGFParser();
    } //}}}

    //{{{ createXMLParser() method
    /**
     *  Create a new XMLParser instance
     *
     *@return    The new XMLParser instance
     */
    public static XMLParser createXMLParser() {
        return new XMLParser();
    } //}}}

    //{{{ creategGoParser() method
    /**
     *  Create a new gGoParser instance
     *
     *@return    The new gGoParser instance
     */
    public static gGoParser creategGoParser() {
        return new gGoParser();
    } //}}}

    //{{{ createUGFParser() method
    /**
     *  Create a new UGFParser instance
     *
     *@return    The new UGFParser instance
     */
    public static UGFParser createUGFParser() {
        return new UGFParser();
    } //}}}

    //{{{ loadFile() method
    /**
     *  Load and parse a file. This method will try to detect the format using the filename extension.
     *  If no extension is given, SGF format is used by default.
     *
     *@param  fileName      File to load
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean loadFile(String fileName, BoardHandler boardHandler) {
        String extension = Utils.getExtension(fileName);

        if (extension != null && extension.equals("sgf"))
            return loadSGF(fileName, boardHandler);
        else if (extension != null && extension.equals("xml"))
            return loadXML(fileName, boardHandler);
        else if (extension != null && extension.equals("ggo"))
            return loadgGo(fileName, boardHandler);
        else if (extension != null && (extension.equals("ugf") || extension.equals("ugi")))
            return loadUGF(fileName, boardHandler);
        else {
            System.err.println("Unknown filename suffix, using SGF.");
            return loadSGF(fileName, boardHandler);
        }
    } //}}}

    //{{{ loadSGF() method
    /**
     *  Load and parse SGF file
     *
     *@param  fileName      File to load
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean loadSGF(String fileName, BoardHandler boardHandler) {
        return createSGFParser().loadFile(fileName, boardHandler);
    } //}}}

    //{{{ loadXML() method
    /**
     *  Load and parse XML file
     *
     *@param  fileName      File to load
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean loadXML(String fileName, BoardHandler boardHandler) {
        return createXMLParser().loadFile(fileName, boardHandler);
    } //}}}

    //{{{ loadgGo() method
    /**
     *  Load gGo file. This is a gGo-specific format and restores a serialized game tree.
     *
     *@param  fileName      File to load
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean loadgGo(String fileName, BoardHandler boardHandler) {
        return creategGoParser().loadFile(fileName, boardHandler);
    } //}}}

    //{{{ loadUGF() method
    /**
     *  Load and parse an UGF file.
     *
     *@param  fileName      File to load
     *@param  boardHandler  BoardHandler containing the game to save
     *@return               True if successful, else false
     */
    public static boolean loadUGF(String fileName, BoardHandler boardHandler) {
        return createUGFParser().loadFile(fileName, boardHandler);
    } //}}}
}

