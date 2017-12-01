/*
 *  Parser.java
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
 *  Abstract superclass of all parser implementations.
 *  The methods for communication with the BoardHandler instance are
 *  implemented here and can be called from subclasses to avoid
 *  code repetition.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/10/06 19:23:29 $
 */
public abstract class Parser implements Defines, ParserDefs {
    /**  BoardHandler object this Parser instance is attached to */
    protected BoardHandler boardHandler;

    //{{{ loadFile() method
    /**
     *  Entry method, load and parse a specified file for the specified board
     *
     *@param  fileName      File to load
     *@param  boardHandler  BoardHandler object this Parser is attached to
     *@return               True if parsing and loading was successful, else false
     */
    public boolean loadFile(String fileName, BoardHandler boardHandler) {
        this.boardHandler = boardHandler;
        if (boardHandler == null) {
            System.err.println("Error: No BoardHandler instance!");
            return false;
        }

        return doParseFile(fileName);
    } //}}}

    //{{{ doParse() method
    /**
     *  Parse the given file. Subclasses have to implement this method for the various
     *  formats.
     *
     *@param  fileName  File to load
     *@return           True if parsing was successful, else false
     */
    abstract boolean doParseFile(String fileName); //}}}

    //{{{ createNode(boolean) method
    /**
     *  Create a new node
     *
     *@param  brother  If true, add the node as brother of the current move, else as son
     */
    void createNode(boolean brother) {
        // System.err.println("CREATE NODE, brother = " + brother);
        boardHandler.createMoveSGF(MODE_NORMAL, brother);
    } //}}}

    //{{{ createNode() method
    /**  Create a new node as son of the current move */
    void createNode() {
        createNode(false);
    } //}}}

    //{{{ setMode() method
    /**
     *  Set game mode for the current move
     *
     *@param  mode  The new game mode
     */
    void setMode(int mode) {
        boardHandler.setGameModeSGF(mode);
    } //}}}

    //{{{ addMove() method
    /**
     *  Add a move
     *
     *@param  color     Stone color
     *@param  x         X position
     *@param  y         Y position
     *@param  new_node  If true, handle the move as new node
     */
    void addMove(int color, int x, int y, boolean new_node) {
        // System.err.println("ADD MOVE: " + (color == STONE_BLACK ? "B " : "W ") + x + "/" + y);
        boardHandler.addStoneSGF(color, x, y, new_node);
    } //}}}

    //{{{ createPass() method
    /**  Create a pass move */
    void createPass() {
        boardHandler.doPass(true);
    } //}}}

    //{{{ removeStone() method
    /**
     *  Remove a stone from the current move
     *
     *@param  x  X position
     *@param  y  Y position
     */
    void removeStone(int x, int y) {
        // System.err.println("REMOVE STONE: " + x + "/" + y);
        boardHandler.removeStone(x, y, false);
    } //}}}

    //{{{ addMark() method
    /**
     *  Add a mark to the current move
     *
     *@param  x     X position
     *@param  y     Y position
     *@param  type  Mark type
     */
    void addMark(int x, int y, int type, Move m) {
        m.getMatrix().insertMark(x, y, type);
    } //}}}

    void addMark(int x, int y, int type) {
        addMark(x, y, type, boardHandler.getTree().getCurrent());
    }

    //{{{ setMarkText() method
    /**
     *  Set the mark text for the text or number mark at the given position
     *
     *@param  x  X position
     *@param  y  Y position
     *@param  s  Text for the mark
     */
    void setMarkText(int x, int y, String s, Move m) {
        m.getMatrix().setMarkText(x, y, s);
    } //}}}

    void setMarkText(int x, int y, String s) {
        setMarkText(x, y, s, boardHandler.getTree().getCurrent());
    }

    //{{{ setComment(String, Move) method
    /**
     *  Set comment for the given move to the given String
     *
     *@param  s  New comment text
     *@param  m  Change comment of this move
     */
    void setComment(String s, Move m) {
        m.setComment(s);
    } //}}}

    //{{{ setComment(String) method
    /**
     *  Set comment of the current move to the given String
     *
     *@param  s  New comment text
     */
    void setComment(String s) {
        setComment(s, boardHandler.getTree().getCurrent());
    } //}}}

    //{{{ initGame() method
    /**
     *  Init game with specificed GameData parameters
     *
     *@param  gameData  GameData object containing the game information
     */
    void initGame(GameData gameData) {
        boardHandler.getBoard().initGame(gameData, true);
    } //}}}
}

