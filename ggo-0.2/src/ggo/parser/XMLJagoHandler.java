/*
 *  XMLJagoHandler.java
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
import ggo.utils.*;
import java.io.*;
import java.util.*;
import java.net.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;

/**
 *  Parser for Go XML files as specified by Rene Grothmann for
 *  <a href="http://www.rene-grothmann.de/jago/">Jago</a>
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/10/05 13:39:02 $
 *@see        <a href="http://www.rene-grothmann.de/jago/">Jago webpage</a>
 */
final class XMLJagoHandler extends DefaultHandler implements Defines {
    //{{{ private members
    private BoardHandler boardHandler;
    private StringBuffer textBuffer;
    private Parser parser;
    private Position pos;
    private boolean errorFlag, isRoot, varStart, var, new_node;
    private String errorMsg;
    private Stack stack, movesStack, toRemove;
    private int moves, boardSize;
    private GameData gameData;
    //}}}

    //{{{ XMLJagoHandler() constructors
    /**Constructor for the XMLJagoHandler object */
    XMLJagoHandler() {
        pos = new Position(0, 0);
        gameData = new GameData();
        stack = new Stack();
        toRemove = new Stack();
        movesStack = new Stack();
    }

    /**
     *Constructor for the XMLJagoHandler object
     *
     *@param  fileName  File to load
     *@param  parser    Pointer to the XMLParser object that uses this class
     */
    XMLJagoHandler(String fileName, Parser parser) {
        this();
        parseFile(fileName, parser);
    } //}}}

    //{{{ parseFile() method
    /**
     *  Load and parse a Jago XML file
     *
     *@param  fileName  File to load
     *@param  parser    Pointer to the XMLParser object that uses this class
     */
    void parseFile(String fileName, Parser parser) {
        boardHandler = parser.boardHandler;
        this.parser = parser;
        errorFlag = false;

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new File(fileName), this);
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }
    } //}}}

    //{{{ getErrorFlag() method
    /**
     *  Gets the errorFlag attribute of the XMLJagoHandler object
     *
     *@return    The errorFlag value
     */
    boolean getErrorFlag() {
        return errorFlag;
    } //}}}

    //{{{ getErrorMsg() method
    /**
     *  Gets the errorMsg attribute of the XMLJagoHandler object
     *
     *@return    The errorMsg value
     */
    String getErrorMsg() {
        return errorMsg;
    } //}}}

    //{{{ resolveEntity() method
    /**
     *  Resolve external entity
     *  <p>Overwrites EntityResolver.resolveEntity() and returns go.dtd in the gGo.jar file
     *  if the go.dtd file as specified in systemId is not found.</p>
     *
     *@param  publicId          Public identifier
     *@param  systemId          System identifier
     *@return                   Input source object
     *@exception  SAXException  Any SAX exception
     *@see                      org.xml.sax.EntityResolver#resolveEntity(String, String)
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
        // Use go.dtd in the jar file, if the file go.dtd as specified in systemId is not found
        if (systemId.indexOf("go.dtd") != -1) {
            try {
                URI uri = new URI(systemId);
                if (uri.getScheme().equals("file")) {
                    File f = new File(uri);
                    if (!f.exists()) {
                        System.err.println("File " + uri.toString() + " not found. Using go.dtd in the jar.");
                        return new InputSource(getClass().getResource("/go.dtd").toString());
                    }
                }
            } catch (URISyntaxException e) {
                System.err.println("Could not resolve Entity System ID: " + systemId + "\n" + e);
            } catch (NullPointerException e) {
                System.err.println("Could not resolve Entity System ID: " + systemId + "\n" + e);
            }
        }
        return super.resolveEntity(publicId, systemId);
    } //}}}

    //{{{ convertPosition() method
    /**
     *  Convert a String position as defined in Jago go.dtd. The converted position is stored in the
     *  global attribute Position pos.
     *
     *@param  atStr  String to parse
     *@return        True if conversion was successful, else false
     */
    private boolean convertPosition(String atStr) {
        try {
            int x = atStr.charAt(0) - 'A' + (atStr.charAt(0) < 'J' ? 1 : 0);
            int y = boardSize - Integer.parseInt(atStr.substring(1, atStr.length()));
            pos.x = x;
            pos.y = y;
        } catch (NumberFormatException e) {
            System.err.println("Failed to convert position " + atStr + "\n" + e);
            return false;
        }
        return true;
    } //}}}

    //{{{ startDocument() method
    /**  XML event: Document start. Initialize some attributs */
    public void startDocument() {
        var = false;
        varStart = false;
        isRoot = true;
        boardSize = 19; // In case no BoardSize element is given
    } //}}}

    //{{{ endDocument() method
    /**  XML event: Document end. Clean up. */
    public void endDocument() {
        moves = 0;
        stack.clear();
        toRemove.clear();
        movesStack.clear();
    } //}}}

    //{{{ startVariation() method
    /**  New variation branches. Rearrange stacks */
    void startVariation() {
        stack.push(boardHandler.getTree().getCurrent());
        movesStack.push(new Integer(moves));
        varStart = true;
    } //}}}

    //{{{ endVariation() method
    /**  End of variation branch. Rearrange stacks */
    void endVariation() {
        if (!movesStack.isEmpty() && !stack.isEmpty()) {
            Move m = (Move)stack.pop();
            int x = ((Integer)movesStack.pop()).intValue();

            for (int i = moves; i > x; i--) {
                Position position;
                try {
                    position = (Position)toRemove.pop();
                } catch (EmptyStackException e) {
                    continue; // No error
                }
                if (position == null)
                    continue;
                boardHandler.getStoneHandler().removeStone(position.x, position.y, false);
            }

            moves = x;
            boardHandler.getStoneHandler().updateAll(m.getMatrix(), true, true);
            boardHandler.getTree().setCurrent(m);
        }
    } //}}}

    //{{{ createNode() method
    /**  Create a new node in the tree */
    void createNode() {
        if (!isRoot)
            parser.createNode(var);
        else
            isRoot = false;
        var = false;

        if (varStart && !toRemove.isEmpty()) {
            try {
                Position position = (Position)toRemove.pop();
                parser.removeStone(position.x, position.y);
            } catch (NullPointerException e) {
                System.err.println("Problem creating node: " + e);
                e.printStackTrace();
            }
        }
        varStart = false;
        new_node = true;
    } //}}}

    //{{{ startElement() method
    /**
     *  XML event: Start of element
     *
     *@param  namespaceURI      Namespace URI
     *@param  qName             local name
     *@param  eName             qualified XML 1.0 name
     *@param  atts              Attributes
     *@exception  SAXException  Any SAX Exception
     *@see                      org.xml.sax.helpers.DefaultHandler#endElement(String, String, String)
     */
    public void startElement(String namespaceURI, String eName, String qName, Attributes atts) throws SAXException {
        if (eName.equals(""))
            eName = qName; // not namespaceAware

        // Black / White
        if (eName.equals("Black") || eName.equals("White")) {
            int col = eName.equals("Black") ? STONE_BLACK : STONE_WHITE;
            String atStr = atts.getValue("at");

            if (!convertPosition(atStr)) {
                errorMsg = "Failed to convert position: " + atStr;
                errorFlag = true;
                return;
            }
            parser.setMode(MODE_NORMAL);
            createNode();
            parser.addMove(col, pos.x, pos.y, new_node);

            // Remember this move for later, to remove from the matrix.
            toRemove.push(new Position(pos.x, pos.y));
            moves++;

            new_node = false;
        }

        // Variation
        else if (eName.equals("Variation")) {
            startVariation();
            var = true;
        }

        // Nodes
        else if (eName.equals("Nodes")) {
            startVariation();
        }

        // Node
        else if (eName.equals("Node")) {
            createNode();
        }

        // Mark
        else if (eName.equals("Mark")) {
            String atStr = atts.getValue("at");
            String typeStr = atts.getValue("type");

            System.err.println("MARK " + typeStr + " " + atStr);

            if (!convertPosition(atStr)) {
                errorMsg = "Failed to convert position: " + atStr;
                errorFlag = true;
                return;
            }

            int markType;
            if (typeStr.equals("triangle"))
                markType = MARK_TRIANGLE;
            else if (typeStr.equals("square"))
                markType = MARK_SQUARE;
            else if (typeStr.equals("circle"))
                markType = MARK_CIRCLE;
            else
                markType = MARK_CROSS;

            parser.addMark(pos.x, pos.y, markType);
        }

        // AddBlack / AddWhite
        else if (eName.equals("AddBlack") || eName.equals("AddWhite")) {
            int col = eName.equals("AddBlack") ? STONE_BLACK : STONE_WHITE;
            String atStr = atts.getValue("at");

            System.err.println("ADD" + (col == STONE_BLACK ? "BLACK" : "WHITE") + " " + atStr);

            if (!convertPosition(atStr)) {
                errorMsg = "Failed to convert position: " + atStr;
                errorFlag = true;
                return;
            }

            parser.setMode(MODE_EDIT);
            parser.addMove(col, pos.x, pos.y, new_node);

            new_node = false;
        }

        // Delete
        else if (eName.equals("Delete")) {
            String atStr = atts.getValue("at");

            System.err.println("DELETE " + atStr);

            if (!convertPosition(atStr)) {
                errorMsg = "Failed to convert position: " + atStr;
                errorFlag = true;
                return;
            }

            parser.setMode(MODE_EDIT);
            parser.removeStone(pos.x, pos.y);
        }
    } //}}}

    //{{{ endElement() method
    /**
     *  XML event: End of element
     *
     *@param  namespaceURI      Namespace URI
     *@param  qName             local name
     *@param  eName             qualified XML 1.0 name
     *@exception  SAXException  Any SAX Exception
     *@see                      org.xml.sax.helpers.DefaultHandler#endElement(String, String, String)
     */
    public void endElement(String namespaceURI, String eName, String qName) throws SAXException {
        if (eName.equals(""))
            eName = qName; // not namespaceAware

        // System.err.println("END ELEMENT: " + eName + " - " + (textBuffer != null ? textBuffer.toString() : "EMPTY"));

        // Variation / Nodes
        if (eName.equals("Variation") || eName.equals("Nodes")) {
            endVariation();
        }

        // Comment
        else if (eName.equals("Comment")) {
            if (textBuffer != null)
                parser.setComment(textBuffer.toString());
            return;
        }

        // P
        else if (eName.equals("P")) {
            textBuffer.append("\n");
            return;
        }

        //{{{ Information element
        else if (eName.equals("BoardSize")) {
            boardSize = Utils.convertStringToInt(textBuffer.toString());
            gameData.size = boardSize;
        }

        else if (eName.equals("BlackPlayer"))
            gameData.playerBlack = textBuffer.toString();

        else if (eName.equals("BlackRank"))
            gameData.rankBlack = textBuffer.toString();

        else if (eName.equals("WhitePlayer"))
            gameData.playerWhite = textBuffer.toString();

        else if (eName.equals("WhiteRank"))
            gameData.rankWhite = textBuffer.toString();

        else if (eName.equals("Komi"))
            gameData.komi = Utils.convertStringToFloat(textBuffer.toString());

        else if (eName.equals("Handicap"))
            gameData.handicap = Utils.convertStringToInt(textBuffer.toString());

        else if (eName.equals("Date"))
            gameData.date = textBuffer.toString();

        else if (eName.equals("Result"))
            gameData.result = textBuffer.toString();

        else if (eName.equals("Copyright"))
            gameData.copyright = textBuffer.toString();

        else if (eName.equals("Information"))
            parser.initGame(gameData);
        //}}}

        if (textBuffer != null)
            textBuffer = null;
    } //}}}

    //{{{ characters() method
    /**
     *  XML event: Recieve notification of characters inside an element
     *
     *@param  buf               The characters
     *@param  offset            Offset
     *@param  len               Length
     *@exception  SAXException  Any SAX Exception
     *@see                      org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    public void characters(char buf[], int offset, int len) throws SAXException {
        String s = new String(buf, offset, len);
        if (textBuffer == null)
            textBuffer = new StringBuffer(s);
        else
            textBuffer.append(s);
    } //}}}
}

