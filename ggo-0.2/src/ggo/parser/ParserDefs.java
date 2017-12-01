/*
 *  ParserDefs.java
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

/**
 *  Interface with definitions for SGF and XML Parser
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/10/09 16:01:56 $
 */
public interface ParserDefs {
    /**  Description of the Field */
    public final static int STATE_VAR_BEGIN = 0;
    /**  Description of the Field */
    public final static int STATE_NODE = 1;
    /**  Description of the Field */
    public final static int STATE_VAR_END = 2;
    /**  Description of the Field */
    public final static int PROPERTY_MOVE_BLACK = 0;
    /**  Description of the Field */
    public final static int PROPERTY_MOVE_WHITE = 1;
    /**  Description of the Field */
    public final static int PROPERTY_EDIT_BLACK = 2;
    /**  Description of the Field */
    public final static int PROPERTY_EDIT_WHITE = 3;
    /**  Description of the Field */
    public final static int PROPERTY_EDIT_ERASE = 4;
    /**  Description of the Field */
    public final static int PROPERTY_COMMENT = 5;
    /**  Description of the Field */
    public final static int PROPERTY_EDIT_MARK = 6;
    /**  Default SGF charset for reading and writing files */
    public final static String defaultCharset = "ISO-8859-1";
    /**  Default gGo charset for reading and writing files */
    public final static String defaultgGoCharset = "UTF-8";
    /**  Default UGF charset for reading and writing files */
    public final static String defaultUGFCharset = "US-ASCII";
}

