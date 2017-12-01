/*
 *  SGFParser.java
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

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.text.MessageFormat;
// --- 1.3 ---
// import java.nio.charset.*;
import ggo.*;
import ggo.utils.*;

/**
 *  This class loads, parses and saves SGF files. It can convert old long sgf format to
 *  FF(4) automatically, if long format is detected while loading.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/10/08 14:47:35 $
 */
public class SGFParser extends Parser {
    //{{{ private members
    private String charset = null;
    //}}}

    //{{{ doParseFile() method
    /**
     *  Load and parse a sgf file, given by the filename. This is the entry method called to actually load a sgf file.
     *  Overwrites Parser.doParseFile(String)
     *
     *@param  fileName  Filename of the sgf file
     *@return           True if successful, else false
     *@see              ggo.parser.Parser#doParseFile(String)
     */
    boolean doParseFile(String fileName) {
        if (fileName == null || fileName.length() == 0)
            return false;

        return parseString(loadFile(fileName));
    } //}}}

    //{{{ loadFile(File) method
    /**
     *  Loads a file and returns the content as String.
     *
     *@param  file  File to load from
     *@return       File content as string
     */
    private String loadFile(File file) {
        StringBuffer s = new StringBuffer();

        try {
            if (!file.exists()) {
                displayError(
                        MessageFormat.format(gGo.getSGFResources().getString("file_does_not_exist_error"), new Object[]{file.getName()}));
                return null;
            }
            if (!file.canRead()) {
                displayError(
                        MessageFormat.format(gGo.getSGFResources().getString("cannot_read_file_error"), new Object[]{file.getName()}));
                return null;
            }

            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file),
            // --- 1.3 ---
            // Charset.forName(charset == null ? defaultCharset : charset)));
                    (charset == null ? defaultCharset : charset)));
            String line;
            while ((line = f.readLine()) != null) {
                if (charset == null) {
                    String tmp = parseProperty(line, "CA");
                    if (tmp.length() > 0) {
                        // --- 1.3 ---
                        /*
                         *  if (Charset.isSupported(tmp)) {
                         *  charset = tmp;
                         *  System.err.println("CHARSET SET TO " + charset);
                         *  f.close();
                         *  return loadFile(file);
                         *  }
                         *  else {
                         *  System.err.println("Warning: Charset " + tmp + " is not supported.\n" +
                         *  "Using default charset " + defaultCharset);
                         *  }
                         */
                        boolean err = false;
                        try {
                            tmp.getBytes(tmp);
                        } catch (UnsupportedEncodingException e) {
                            displayError(
                                    MessageFormat.format(gGo.getSGFResources().getString("unsupported_charset_error"), new Object[]{tmp, defaultCharset}));
                            err = true;
                        }
                        if (!err) {
                            charset = tmp;
                            System.err.println("CHARSET SET TO " + charset);
                            f.close();
                            return loadFile(file);
                        }
                    }
                }
                s.append(line + "\n");
            }
            f.close();
        } catch (IOException e) {
            System.err.println("Error reading file '" + file.getName() + "': " + e);
            displayError(
                    MessageFormat.format(gGo.getSGFResources().getString("reading_file_error"), new Object[]{file.getName()}));
            charset = null;
            return null;
        } catch (SecurityException e) {
            displayError(gGo.getSGFResources().getString("access_denied_error"));
            charset = null;
            return null;
        }
        charset = null;
        return s.toString();
    } //}}}

    //{{{ loadFile(String) method
    /**
     *  Loads a file and returns the content as String.
     *
     *@param  fileName  Filename to load from
     *@return           File content as string
     */
    private String loadFile(String fileName) {
        return loadFile(new File(fileName));
    } //}}}

    //{{{ parseString() methods
    /**
     *  Parse a String containing the SGF tags.
     *
     *@param  toParse  String to parse
     *@return          True if successful, else false
     */
    private boolean parseString(String toParse) {
        if (boardHandler == null || toParse == null || toParse.length() == 0)
            return false;

        // Convert old sgf/mgt format into new
        if (toParse.indexOf("White[") != -1) { // Do a quick test if this is necassary.
            if (gGo.is13()) {
                displayError(gGo.getSGFResources().getString("cannot_convert_error"));
                return false;
            }
            toParse = convertOldSgf(toParse);
        }

        // Init game
        if (!initGame(toParse))
            return corruptSgf(-1);

        // Parse game
        return doParse(toParse);
    }

    /**
     *  Parse a String containing the SGF tags.
     *  This method is public as it is directly called from BoardHandler for loading
     *  the temporary files when editing played or observed games
     *
     *@param  toParse       String to parse
     *@param  boardHandler  Pointer to the BoardHandler instance, as this method is called directly
     *@return               True if successful, else false
     */
    public boolean parseString(String toParse, BoardHandler boardHandler) {
        this.boardHandler = boardHandler;
        return parseString(toParse);
    } //}}}

    //{{{ doParse() method
    /**
     *  Main SGF parsing method. This method loops through the whole SGF file, parses for the SGF tags and
     *  calls the methods in the Parser superclass to modify the tree.
     *
     *@param  toParse  String to parse, containing the complete SGF code
     *@return          True if successful, else false
     */
    private boolean doParse(String toParse) {
        int pos = 0;
        int posVarBegin = 0;
        int posVarEnd = 0;
        int posNode = 0;
        int pointer = 0;
        int moves = 0;
        int x = -1;
        int y = -1;
        int markType = MARK_NONE;
        String commentStr;
        String moveStr;
        boolean new_node = false;
        boolean isRoot = true;
        boolean black = true;
        boolean setup = false;
        boolean oldLabel = false;
        int strLength = toParse.length();
        int state = STATE_VAR_BEGIN;
        Stack stack = new Stack();
        Stack toRemove = new Stack();
        Stack movesStack = new Stack();
        Tree tree = boardHandler.getTree();

        boolean cancel = false;
        int progressCounter = 0;

        ProgressMonitor progress = new ProgressMonitor(boardHandler.getBoard(),
                gGo.getSGFResources().getString("reading_sgf_file"),
                null, 0, strLength);

        do {
            // Update progress monitor
            if ((++progressCounter % 10) == 0) {
                progress.setProgress(pointer);
                if (progress.isCanceled()) {
                    progress.close();
                    cancel = true;
                    break;
                }
            }

            //{{{ Check states
            posVarBegin = toParse.indexOf('(', pointer);
            posVarEnd = toParse.indexOf(')', pointer);
            posNode = toParse.indexOf(';', pointer);

            pos = minPos(posVarBegin, posVarEnd, posNode);

            // Switch states

            // Node . VarEnd
            if (state == STATE_NODE && pos == posVarEnd)
                state = STATE_VAR_END;

            // Node . VarBegin
            if (state == STATE_NODE && pos == posVarBegin)
                state = STATE_VAR_BEGIN;

            // VarBegin . Node
            else if (state == STATE_VAR_BEGIN && pos == posNode)
                state = STATE_NODE;

            // VarEnd . VarBegin
            else if (state == STATE_VAR_END && pos == posVarBegin)
                state = STATE_VAR_BEGIN;
            //}}}

            // Do the work
            switch (state) {
                //{{{ Var begin
                case STATE_VAR_BEGIN:
                    if (pos != posVarBegin)
                        return corruptSgf(pos);

                    stack.push(tree.getCurrent());
                    movesStack.push(new Integer(moves));

                    pointer = pos + 1;
                    break; //}}}
                //{{{ Var end
                case STATE_VAR_END:
                    if (pos != posVarEnd)
                        return corruptSgf(pos);

                    if (!movesStack.isEmpty() && !stack.isEmpty()) {
                        Move m = (Move)stack.pop();
                        x = ((Integer)movesStack.pop()).intValue();

                        for (int i = moves; i > x; i--) {
                            Position position = (Position)toRemove.pop();
                            if (position == null)
                                continue;
                            boardHandler.getStoneHandler().removeStone(position.x, position.y, false);
                        }

                        moves = x;

                        boardHandler.getStoneHandler().updateAll(m.getMatrix(), true, true);

                        tree.setCurrent(m);
                    }

                    pointer = pos + 1;
                    break; //}}}
                //{{{ Var node
                case STATE_NODE:
                    if (pos != posNode)
                        return corruptSgf(pos);

                    commentStr = "";
                    setup = false;
                    // markType = markNone;

                    // Create empty node
                    if (!isRoot)
                        createNode();
                    else
                        isRoot = false;

                    new_node = true;
                    int prop;
                    pos++;

                    do {
                        int tmppos = 0;
                        pos = next_nonspace(toParse, pos);

                        // System.err.println("READING PROPERTY AT " + pos + ": " + toParse.charAt(pos));

                        //{{{ Parse properties
                        if (toParse.charAt(pos) == 'B' && toParse.charAt(tmppos = next_nonspace(toParse, pos + 1)) == '[') {
                            prop = PROPERTY_MOVE_BLACK;
                            pos = tmppos;
                            black = true;
                        }
                        else if (toParse.charAt(pos) == 'W' && toParse.charAt(tmppos = next_nonspace(toParse, pos + 1)) == '[') {
                            prop = PROPERTY_MOVE_WHITE;
                            pos = tmppos;
                            black = false;
                        }
                        else if (toParse.charAt(pos) == 'A' && toParse.charAt(pos + 1) == 'B' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_BLACK;
                            pos = tmppos;
                            setup = true;
                            black = true;
                        }
                        else if (toParse.charAt(pos) == 'A' && toParse.charAt(pos + 1) == 'W' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_WHITE;
                            pos = tmppos;
                            setup = true;
                            black = false;
                        }
                        else if (toParse.charAt(pos) == 'A' && toParse.charAt(pos + 1) == 'E' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_ERASE;
                            pos = tmppos;
                            setup = true;
                        }
                        else if (toParse.charAt(pos) == 'T' && toParse.charAt(pos + 1) == 'R' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_MARK;
                            markType = MARK_TRIANGLE;
                            pos = tmppos;
                        }
                        else if (toParse.charAt(pos) == 'C' && toParse.charAt(pos + 1) == 'R' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_MARK;
                            markType = MARK_CIRCLE;
                            pos = tmppos;
                        }
                        else if (toParse.charAt(pos) == 'S' && toParse.charAt(pos + 1) == 'Q' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_MARK;
                            markType = MARK_SQUARE;
                            pos = tmppos;
                        }
                        else if (toParse.charAt(pos) == 'M' && toParse.charAt(pos + 1) == 'A' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_MARK;
                            markType = MARK_CROSS;
                            pos = tmppos;
                        }
                        else if (toParse.charAt(pos) == 'L' && toParse.charAt(pos + 1) == 'B' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_MARK;
                            markType = MARK_TEXT;
                            pos = tmppos;
                            oldLabel = false;
                        }
                        // Added old L property. This is not SGF4, but many files contain this tag.
                        else if (toParse.charAt(pos) == 'L' && toParse.charAt(tmppos = next_nonspace(toParse, pos + 1)) == '[') {
                            prop = PROPERTY_EDIT_MARK;
                            markType = MARK_TEXT;
                            pos = tmppos;
                            oldLabel = true;
                        }
                        else if (toParse.charAt(pos) == 'C' && toParse.charAt(tmppos = next_nonspace(toParse, pos + 1)) == '[') {
                            prop = PROPERTY_COMMENT;
                            pos = tmppos;
                        }
                        else if (toParse.charAt(pos) == 'T' && toParse.charAt(pos + 1) == 'B' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_MARK;
                            markType = MARK_TERR_BLACK;
                            pos = tmppos;
                            black = true;
                        }
                        else if (toParse.charAt(pos) == 'T' && toParse.charAt(pos + 1) == 'W' &&
                                toParse.charAt(tmppos = next_nonspace(toParse, pos + 2)) == '[') {
                            prop = PROPERTY_EDIT_MARK;
                            markType = MARK_TERR_WHITE;
                            pos = tmppos;
                            black = false;
                        }
                        // Empty node
                        else if (toParse.charAt(pos) == ';' || toParse.charAt(pos) == '(' || toParse.charAt(pos) == ')') {
                            while (toParse.charAt(pos) == '\n')
                                pos++;

                            continue;
                        }
                        else {
                            int tmp = toParse.indexOf("]", pos) + 1;
                            if (tmp <= 0) {
                                pointer = pos + 1;
                                break;
                            }
                            pos = tmp;
                            while (toParse.charAt(pos) == '\n')
                                pos++;

                            continue;
                        } //}}}

                        //{{{ Parse values
                        // Next is one or more '[xx]'.
                        // Only one in a move property, several in a setup propery
                        do {
                            if (toParse.charAt(pos) != '[')
                                return corruptSgf(pos);

                            // Empty type
                            if (toParse.charAt(pos + 1) == ']') {
                                // CGoban stores pass as 'B[]' or 'W[]'
                                if (prop == PROPERTY_MOVE_BLACK || prop == PROPERTY_MOVE_WHITE) {
                                    createPass();
                                    // Remember this move for later, to remove from the matrix.
                                    toRemove.push(new Position(x, y));
                                    moves++;
                                }
                                pos += 2;
                                continue;
                            }

                            switch (prop) {
                                //{{{ MOVE or ADD or ERASE
                                case PROPERTY_MOVE_BLACK:
                                case PROPERTY_MOVE_WHITE:
                                case PROPERTY_EDIT_BLACK:
                                case PROPERTY_EDIT_WHITE:
                                case PROPERTY_EDIT_ERASE:
                                    x = toParse.charAt(pos + 1) - 'a' + 1;
                                    y = toParse.charAt(pos + 2) - 'a' + 1;

                                    setMode(setup ? MODE_EDIT : MODE_NORMAL);

                                    if (x == 20 && y == 20)
                                        createPass();

                                    else if (prop == PROPERTY_EDIT_ERASE)
                                        removeStone(x, y);

                                    else
                                        addMove(black ? STONE_BLACK : STONE_WHITE, x, y, new_node);

                                    // Remember this move for later, to remove from the matrix.
                                    toRemove.push(new Position(x, y));
                                    moves++;

                                    new_node = false;

                                    // Advance pos by 4
                                    pos += 4;
                                    break; //}}}
                                //{{{ COMMENT
                                case PROPERTY_COMMENT:
                                    commentStr = "";

                                    while (toParse.charAt(++pos) != ']' ||
                                            (toParse.charAt(pos - 1) == '\\' && toParse.charAt(pos) == ']')) {
                                        if (!(toParse.charAt(pos) == '\\' &&
                                                toParse.charAt(pos + 1) == ']') &&
                                                !(toParse.charAt(pos) == '\\' &&
                                                toParse.charAt(pos + 1) == '[') &&
                                                !(toParse.charAt(pos) == '\\' &&
                                                toParse.charAt(pos + 1) == ')') &&
                                                !(toParse.charAt(pos) == '\\' &&
                                                toParse.charAt(pos + 1) == '('))
                                            commentStr += toParse.charAt(pos);

                                        if (pos > strLength)
                                            return corruptSgf(-1);
                                    }
                                    setComment(commentStr);
                                    pos++;

                                    break; //}}}
                                //{{{ EDIT_MARK
                                case PROPERTY_EDIT_MARK:
                                    while (toParse.charAt(pos) == '[' &&
                                            pos < strLength) {
                                        x = toParse.charAt(pos + 1) - 'a' + 1;
                                        y = toParse.charAt(pos + 2) - 'a' + 1;
                                        pos += 3;

                                        // 'LB' property? Then we need to get the text
                                        if (markType == MARK_TEXT && !oldLabel) {
                                            if (toParse.charAt(pos) != ':')
                                                return corruptSgf(pos);
                                            moveStr = "";
                                            while (toParse.charAt(++pos) != ']' && pos < strLength)
                                                moveStr += toParse.charAt(pos);
                                            // It might be a number mark?
                                            try {
                                                int n = Integer.parseInt(moveStr);
                                                // Yes, its a number
                                                addMark(x, y, MARK_NUMBER);
                                            } catch (NumberFormatException e) {
                                                // Nope, its a letter
                                                addMark(x, y, MARK_TEXT);
                                            }
                                            setMarkText(x, y, moveStr);
                                        }
                                        // Other mark
                                        else
                                            addMark(x, y, markType);

                                        pos++;
                                        while (toParse.charAt(pos) == '\n')
                                            pos++;
                                    }
                                    oldLabel = false;
                                    break; //}}}
                            }
                            while (toParse.charAt(pos) == '\n')
                                pos++;

                        } while (setup && toParse.charAt(pos) == '['); //}}}

                        while (toParse.charAt(pos) == '\n')
                            pos++;

                    } while (toParse.charAt(pos) != ';' && toParse.charAt(pos) != '(' && toParse.charAt(pos) != ')' &&
                            pos < strLength);

                    // Advance pointer
                    pointer = pos;

                    break; //}}}
                default:
                    return corruptSgf(pointer);
            }
        } while (pointer < strLength && pos >= 0);

        progress.close();
        return !cancel;
    } //}}}

    //{{{ corruptSgf() method
    /**
     *  A corrupt sgf format was found, give the user some feedback.
     *
     *@param  where  Position in the string where the error was found
     *@return        Returns always false.
     */
    private boolean corruptSgf(int where) {
        if (where == -1)
            displayError(gGo.getSGFResources().getString("corrupt_sgf_error"));
        else
            displayError(
                    MessageFormat.format(gGo.getSGFResources().getString("corrupt_sgf_at_error"), new Object[]{new Integer(where)}));
        return false;
    } //}}}

    //{{{ minPos() method
    /**
     *  Calculate the minimum of the given three values
     *
     *@param  n1  First number
     *@param  n2  Second number
     *@param  n3  Third number
     *@return     Smallest of the given three numbers
     */
    private int minPos(int n1, int n2, int n3) {
        int min;

        if (n1 != -1)
            min = n1;
        else if (n2 != -1)
            min = n2;
        else
            min = n3;

        if (n1 < min && n1 != -1)
            min = n1;

        if (n2 < min && n2 != -1)
            min = n2;

        if (n3 < min && n3 != -1)
            min = n3;

        return min;
    } //}}}

    //{{{ next_nonspace() method
    /**
     *  Fine next character that is not a whitespace, CR or tab
     *
     *@param  toParse  Parsed string
     *@param  i        Current position
     *@return          Found position
     */
    private int next_nonspace(String toParse, int i) {
        while (toParse.charAt(i) == ' ' || toParse.charAt(i) == '\n' || toParse.charAt(i) == '\t')
            i++;

        return i;
    } //}}}

    //{{{ parseProperty() method
    /**
     *  Parse a string for a SGF property
     *
     *@param  toParse  String to parse
     *@param  prop     Property to search
     *@return          Value of found property. If not found, empty string ("")
     *      is returned
     */
    private String parseProperty(String toParse, String prop) {
        int pos;
        int strLength = toParse.length();
        String result = "";

        pos = toParse.indexOf(prop + "[");
        if (pos == -1)
            return result;
        pos += 2;
        if (toParse.charAt(pos) != '[') {
            corruptSgf(pos);
            return "";
        }
        while (toParse.charAt(++pos) != ']' && pos < strLength)
            result += toParse.charAt(pos);

        if (pos > strLength) {
            corruptSgf(pos);
            return "";
        }
        // System.err.println("Parse property: " + prop + " -> " + result);
        return result;
    } //}}}

    //{{{ initGame() method
    /**
     *  Read header properties and values from sgf file
     *
     *@param  toParse  String with sgf file text
     *@return          True if successful, else false
     */
    private boolean initGame(String toParse) {
        String tmp = "";
        GameData gameData = new GameData();

        // Charset
        tmp = parseProperty(toParse, "CA");
        // --- 1.3 ---
        // if (tmp.length() > 0 && Charset.isSupported(tmp))
        if (tmp.length() > 0) {
            boolean err = false;
            try {
                tmp.getBytes(tmp);
            } catch (UnsupportedEncodingException e) {
                gameData.charset = defaultCharset;
                err = true;
            }
            if (!err)
                gameData.charset = tmp;
        }
        /*
         *  else
         *  gameData.charset = defaultCharset;
         */
        // White player name
        tmp = parseProperty(toParse, "PW");
        if (tmp.length() > 0)
            gameData.playerWhite = tmp;
        else
            gameData.playerWhite = "White";

        // White player rank
        tmp = parseProperty(toParse, "WR");
        if (tmp.length() > 0)
            gameData.rankWhite = tmp;
        else
            gameData.rankWhite = "";

        // Black player name
        tmp = parseProperty(toParse, "PB");
        if (tmp.length() > 0)
            gameData.playerBlack = tmp;
        else
            gameData.playerBlack = "Black";

        // Black player rank
        tmp = parseProperty(toParse, "BR");
        if (tmp.length() > 0)
            gameData.rankBlack = tmp;
        else
            gameData.rankBlack = "";

        // Board size
        tmp = parseProperty(toParse, "SZ");
        if (tmp.length() > 0)
            gameData.size = Integer.parseInt(tmp);
        else
            gameData.size = 19;

        // Komi
        tmp = parseProperty(toParse, "KM");
        if (tmp.length() > 0)
            gameData.komi = Float.parseFloat(tmp);
        else
            gameData.komi = 0.0f;

        // Handicap
        tmp = parseProperty(toParse, "HA");
        if (tmp.length() > 0)
            gameData.handicap = Integer.parseInt(tmp);
        else
            gameData.handicap = 0;

        // Result
        tmp = parseProperty(toParse, "RE");
        if (tmp.length() > 0)
            gameData.result = tmp;
        else
            gameData.result = "";

        // Date
        tmp = parseProperty(toParse, "DT");
        if (tmp.length() > 0)
            gameData.date = tmp;
        else
            gameData.date = "";

        // Place
        tmp = parseProperty(toParse, "PC");
        if (tmp.length() > 0)
            gameData.place = tmp;
        else
            gameData.place = "";

        // Copyright
        tmp = parseProperty(toParse, "CP");
        if (tmp.length() > 0)
            gameData.copyright = tmp;
        else
            gameData.copyright = "";

        // Game Name
        tmp = parseProperty(toParse, "GN");
        if (tmp.length() > 0)
            gameData.gameName = tmp;
        else
            gameData.gameName = "";

        initGame(gameData);
        return true;
    } //}}}

    //{{{ convertOldSgf() method
    /**
     *  Convert old sgf format. This only works with JRE 1.4, as this method uses String.replaceAll(String,String), which
     *  can replace a string matching a regular expression. Unfortunately, replaceAll does not exist in Java 1.3.
     *
     *@param  toParse  String with complete sgf file
     *@return          Converted String
     */
    private String convertOldSgf(String toParse) {
        System.err.println("Converting old sgf format...");

        // \x5B is the pattern for [ character

        // Replace 'GaMe' with 'GM'
        toParse = toParse.replaceAll("GaMe\\x5B", "GM[");

        // Replace 'SiZe' with 'SZ'
        toParse = toParse.replaceAll("SiZe\\x5B", "SZ[");

        // Replace 'FileFormat' with 'FF'
        toParse = toParse.replaceAll("FileFormat\\x5B", "FF[");

        // Replace 'KoMi' with 'KM'
        toParse = toParse.replaceAll("KoMi\\x5B", "KM[");

        // Replace all 'AddBlack' with 'AB'
        toParse = toParse.replaceAll("AddBlack\\x5B", "AB[");

        // Replace all 'AddWhite' with 'AW'
        toParse = toParse.replaceAll("AddWhite\\x5B", "AW[");

        // Replace all 'AddEmpty' with 'AE'
        toParse = toParse.replaceAll("AddEmpty\\x5B", "AE[");

        // Replace all 'White' with 'W'
        toParse = toParse.replaceAll("White\\x5B", "W[");

        // Replace all 'Black' with 'B'
        toParse = toParse.replaceAll("Black\\x5B", "B[");

        // Replace all 'Letter' with 'L'
        toParse = toParse.replaceAll("Letter\\x5B", "L[");

        // Replace all 'Comment' with 'C'
        toParse = toParse.replaceAll("Comment\\x5B", "C[");

        return toParse;
    } //}}}

    //{{{ displayError() method
    /**
     *  Display a messagebox with an error text
     *
     *@param  txt  Error text
     */
    private void displayError(String txt) {
        JOptionPane.showMessageDialog(
                boardHandler.getBoard().getMainFrame(),
                txt,
                MessageFormat.format(
                gGo.getSGFResources().getString("file_read_error_title"),
                new Object[]{gGo.getSGFResources().getString("SGF")}),
                JOptionPane.ERROR_MESSAGE);
    } //}}}
}

