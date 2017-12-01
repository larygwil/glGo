/*
 *  XMLParser.java
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

import ggo.Defines;

/**
 *  General parser for XML files. The actual parsing is done in the
 *  XMLxxxHandler.java class, currently only the Jago XML format is
 *  supported.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/05 11:23:13 $
 */
public class XMLParser extends Parser {
    private XMLJagoHandler xmlJagoHandler;

    //{{{ doParseFile() method
    /**
     *  Entry method to start parsing of the given file, overwrites Parser.doParseFile(String)
     *
     *@param  fileName  File to parse
     *@return           True if parsing was successful, else false
     *@see              ggo.parser.Parser#doParseFile(String)
     */
    boolean doParseFile(String fileName) {
        if (xmlJagoHandler == null)
            xmlJagoHandler = new XMLJagoHandler(fileName, this);
        else
            xmlJagoHandler.parseFile(fileName, this);

        if (xmlJagoHandler.getErrorFlag()) {
            System.err.println("An error occured while parsing the file:\n" +
                    xmlJagoHandler.getErrorMsg());
            return false;
        }
        return true;
    } //}}}
}

