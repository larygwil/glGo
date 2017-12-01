/*
 *  UGFParser.java
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
import java.text.MessageFormat;
import javax.swing.*;

/**
 *  This class loads and parses UGF files, a format used by PandaEgg client. gGo only supports
 *  loading of UGF games.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/10/11 05:34:55 $
 */
public class UGFParser extends Parser {
    //{{{ private members
    private int coordType, boardSize;
    private final static int COORD_TYPE_JPN = 0;
    private final static int COORD_TYPE_IGS = 1;
    private HashMap textMarks;
    private String charset = null;
    //}}}

    //{{{ loadFile(String) method
    /**
     *  Loads a file and returns the content as String.
     *
     *@param  fileName  Filename
     *@return           File content as string
     */
    private String loadFile(String fileName) {
        StringBuffer s = new StringBuffer();
        File file = null;

        try {
            file = new File(fileName);

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

            BufferedReader f =
                    new BufferedReader(new InputStreamReader(new FileInputStream(file), (charset == null ? defaultUGFCharset : charset)));
            String line;
            while ((line = f.readLine()) != null) {
                if (charset == null) {
                    // --- 1.3 ---
                    // 1.4 code removed, see ggo.parser.SGFParser for the 1.4 version
                    String tmp = parseLang(line);
                    if (tmp != null && tmp.length() > 0) {
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
                            return loadFile(fileName);
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

    //{{{ doParseFile() method
    /**
     *  Entry method to start parsing of the given file, overwrites Parser.doParseFile(String)
     *
     *@param  fileName  File to parse
     *@return           True if parsing was successful, else false
     *@see              ggo.parser.Parser#doParseFile(String)
     */
    boolean doParseFile(String fileName) {
        if (fileName == null || fileName.length() == 0)
            return false;

        boardSize = 19; // Default, if none given in header section
        coordType = COORD_TYPE_JPN; // Default, if none given in header section
        textMarks = new HashMap();
        String toParse = loadFile(fileName);
        if (parseUGFHeader(toParse) &&
                parseUGFData(toParse) &&
                parseUGFFigure(toParse))
            return true;
        return false;
    } //}}}

    //{{{ parseLang() method
    /**
     *  Parse Lang tag for stream encoding
     *
     *@param  toParse  Parsed line
     *@return          Encoding name
     */
    private String parseLang(String toParse) {
        if (toParse.indexOf("Lang=SJIS") != -1)
            return "SJIS";
        return null;
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
                new Object[]{gGo.getSGFResources().getString("UGF")}),
                JOptionPane.ERROR_MESSAGE);
    } //}}}

    //{{{ parseUGFHeader() method
    /**
     *  Parse UGF Header section
     *
     *@param  toParse  String to parse
     *@return          True if successful, else false
     */
    private boolean parseUGFHeader(String toParse) {
        GameData gameData = new GameData();

        // Extract Header lines
        int pos = toParse.indexOf("[Header]");
        if (pos == -1) {
            System.err.println("ERROR: Did not find UGF Header section.");
            return false;
        }
        int end = toParse.indexOf("[Data]");
        if (end == -1)
            end = toParse.length();
        toParse = toParse.substring(pos + 8, end);

        try {
            // Board size
            if ((pos = toParse.indexOf("Size=")) != -1) {
                boardSize = Utils.convertStringToInt(toParse.substring(pos + 5, toParse.indexOf("\n", pos)).trim());
                if (boardSize == -1) {
                    System.err.println("Failed to parse board size. Using 19x19.");
                    boardSize = 19;
                }
                else
                    gameData.size = boardSize;
            }

            // Coordinate type
            if ((pos = toParse.indexOf("CoordinateType=")) != -1) {
                String ct = toParse.substring(pos + 15, toParse.indexOf("\n", pos)).trim();
                if (ct.equals("IGS"))
                    coordType = COORD_TYPE_IGS;
                else
                    // Default, if none given (just a guess, this one looks less stupid)
                    coordType = COORD_TYPE_JPN;
            }

            // Black player
            if ((pos = toParse.indexOf("PlayerB=")) != -1) {
                String s = toParse.substring(pos + 8, toParse.indexOf("\n", pos)).trim();
                int k = s.indexOf(",");
                gameData.playerBlack = s.substring(0, k);
                gameData.rankBlack = s.substring(++k, s.indexOf(",", k));
            }

            // White player
            if ((pos = toParse.indexOf("PlayerW=")) != -1) {
                String s = toParse.substring(pos + 8, toParse.indexOf("\n", pos)).trim();
                int k = s.indexOf(",");
                gameData.playerWhite = s.substring(0, k);
                gameData.rankWhite = s.substring(++k, s.indexOf(",", k));
            }

            // Handicap and Komi
            if ((pos = toParse.indexOf("Hdcp=")) != -1) {
                String s = toParse.substring(pos + 5, toParse.indexOf("\n", pos)).trim();
                // Find first ',', seperator between handicap and komi
                int k = s.indexOf(",");
                String hs = s.substring(0, k);
                String ks = s.substring(k + 1, s.length());
                ks = ks.replace(',', '.'); // Replace , with .
                gameData.handicap = Utils.convertStringToInt(hs);
                gameData.komi = Utils.convertStringToFloat(ks);
            }

            // Title
            if ((pos = toParse.indexOf("Title=")) != -1) {
                String s = toParse.substring(pos + 6, toParse.indexOf("\n", pos)).trim();
                // This is PandaEgg default. Leave title empty then
                if (s.indexOf("(NoName)") == -1) {
                    // As SGF doesn't support those 3 title substrings, we try to munch this
                    // into one human readable title String
                    if (s.startsWith(",")) // Remove leading ,
                        s = s.substring(1);
                    int k = s.indexOf(",");
                    while (k != -1) {
                        if (k != -1) // Add a space after ,
                            s = s.substring(0, k + 1) + " " + s.substring(++k, s.length());
                        k = s.indexOf(",", k);
                    }
                    gameData.gameName = s;
                }
            }

            // Date
            if ((pos = toParse.indexOf("Date=")) != -1) {
                String s = toParse.substring(pos + 5, toParse.indexOf("\n", pos)).trim();
                // We only use the started date, as gGo and SGF don't support finished date
                int k = s.indexOf(",");
                if (k != -1) // Cut everything after first ,
                    s = s.substring(0, k);
                gameData.date = s;
            }

            // Winner
            if ((pos = toParse.indexOf("Winner=")) != -1) {
                String s = toParse.substring(pos + 7, toParse.indexOf("\n", pos)).trim();
                gameData.result = parseResult(s);
            }

            // Place
            if ((pos = toParse.indexOf("Place=")) != -1) {
                gameData.place = toParse.substring(pos + 6, toParse.indexOf("\n", pos)).trim();
            }

            // Copyright
            if ((pos = toParse.indexOf("Copyright=")) != -1) {
                gameData.copyright = toParse.substring(pos + 10, toParse.indexOf("\n", pos)).trim();
            }

        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("ERROR: Failed to parse UGF header: " + e);
            // TODO: display error
            return false;
        }

        initGame(gameData);

        return true;
    } //}}}

    //{{{ parseUGFData() method
    /**
     *  Parse UGF Data section
     *
     *@param  toParse  String to parse
     *@return          True if successful, else false
     */
    private boolean parseUGFData(String toParse) {
        // Extract Data lines
        int pos = toParse.indexOf("[Data]");
        if (pos == -1) {
            System.err.println("ERROR: Did not find UGF Data section.");
            return false;
        }
        int end = toParse.indexOf("[Figure]");
        if (end == -1)
            end = toParse.length();
        toParse = toParse.substring(pos + 6, end);

        // Loop through Data lines
        int lineStartPos = -1;
        int lineEndPos;
        while ((lineStartPos = toParse.indexOf("\n", ++lineStartPos)) != -1) {
            lineEndPos = toParse.indexOf("\n", lineStartPos + 1);
            try {
                if (!parseDataLine(toParse.substring(lineStartPos + 1, lineEndPos))) {
                    System.err.println("ERROR: Failed to parse UGF data section.");
                    return false;
                }
            } catch (StringIndexOutOfBoundsException e) {
                break;
            }
        }
        return true;
    } //}}}

    //{{{ parseDataLine() method
    /**
     *  Parse one line of the UGF Data section
     *
     *@param  toParse  String to parse
     *@return          True if successful, else false
     */
    private boolean parseDataLine(String toParse) {
        Position position = null;
        int turn;
        // int moveNumber;    // Unused. Stupid format.
        // int thinkingTime;  // Unused. gGo cannot use this yet. Stupid editor.

        try {
            int pos = 0;
            String positionStr = toParse.substring(0, (pos = toParse.indexOf(",", pos)));
            String turnStr = toParse.substring(++pos, (pos = toParse.indexOf(",", pos)));
            // String moveNumberStr = toParse.substring(++pos, (pos = toParse.indexOf(",", pos)));
            // String thinkingTimeStr = toParse.substring(++pos, toParse.length());

            // Convert position
            if (positionStr.equals("YA") || positionStr.equals("YB"))
                // Pass or pair-go penalty (what is this?)
                position = new Position(-1, -1);
            else {
                if (coordType == COORD_TYPE_JPN)
                    position = new Position(positionStr.charAt(0) - 'A' + 1, positionStr.charAt(1) - 'A' + 1);
                else if (coordType == COORD_TYPE_IGS)
                    position = new Position(positionStr.charAt(0) - 'A' + 1, boardSize - (positionStr.charAt(1) - 'A'));
                else {
                    System.err.println("ERROR: Unknown UGF coordinate type. Aborting.");
                    return false;
                }
            }

            // Convert turn
            if (turnStr.equals("W1") || turnStr.equals("W2"))
                turn = STONE_WHITE;
            else if (turnStr.equals("B1") || turnStr.equals("B2"))
                turn = STONE_BLACK;
            else {
                System.err.println("ERROR: Failed to parse line: " + toParse + "\nInvalid turn.");
                // TODO: Display error
                return false;
            }

            // Ignore move number and thinking time.
            // moveNumber = Integer.parseInt(moveNumberStr);
            // thinkingTime = Integer.parseInt(thinkingTimeStr);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("ERROR: Failed to parse line: " + toParse + "\n" + e);
            // TODO: Display error
            return false;
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Failed to parse line: " + toParse + "\n" + e);
            // TODO: Display error
            return false;
        }

        // Create move
        createNode();
        if (position.x == -1 && position.y == -1)
            createPass();
        else
            addMove(turn, position.x, position.y, true);

        return true;
    } //}}}

    //{{{ parseUGFFigure() method
    /**
     *  Parse UGF Figure section
     *
     *@param  toParse  String to parse
     *@return          True if successful, else false
     */
    private boolean parseUGFFigure(String toParse) {
        // Extract Figure lines
        int pos = toParse.indexOf("[Figure]");
        if (pos == -1)
            // No figure section
            return true;
        toParse = toParse.substring(pos + 8, toParse.length());

        // Loop through .Text sections
        pos = toParse.indexOf(".Text,");
        String txt = "";
        while (pos != -1) {
            int end = toParse.indexOf(",", pos + 6);
            int end2 = toParse.indexOf("\n", pos + 6);
            if (end == -1)
                end = end2;
            else
                end = Math.min(end, end2);
            try {
                txt = toParse.substring(pos + 6, end);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to convert move number. Skipping section.\n" + e);
                pos = toParse.indexOf(".Text", ++pos);
                continue;
            }
            int moveNumber = Utils.convertStringToInt(txt);
            if (moveNumber == -1) {
                System.err.println("Failed to convert move number. Skipping section.");
                pos = toParse.indexOf(".Text", ++pos);
                continue;
            }
            try {
                txt = toParse.substring(end2 + 1, toParse.indexOf(".EndText", end) - 1);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to parse comment text. Skipping section.\n" + e);
                pos = toParse.indexOf(".Text", ++pos);
                continue;
            }

            // Parse marks within text section, as in: .#,2,10,A
            txt = parseTextMark(txt, moveNumber);

            // Add comment and text marks to game tree
            try {
                Tree tree = boardHandler.getTree();
                Move m = tree.getRoot();
                while (true) {
                    if (m.getMoveNumber() == moveNumber) {
                        setComment(txt, m);
                        ArrayList marks = (ArrayList)textMarks.get(new Integer(moveNumber));
                        if (marks != null && !marks.isEmpty()) {
                            for (Iterator it = marks.iterator(); it.hasNext(); ) {
                                UGFMark mark = (UGFMark)it.next();
                                addMark(mark.x, mark.y, MARK_TEXT, m);
                                setMarkText(mark.x, mark.y, mark.text, m);
                            }
                        }
                        break;
                    }
                    if (m.son == null) {
                        System.err.println("Failed to find move " + moveNumber + " to add comment.");
                        break;
                    }
                    m = m.son;
                }
            } catch (NullPointerException e) {
                System.err.println("Failed to set comment for move " + moveNumber + ".\n" + e);
            }

            pos = toParse.indexOf(".Text", ++pos);
        }

        return true;
    } //}}}

    //{{{ parseTextMark() method
    /**
     *  Parse one line of the UGF Figure section, containing comment and textmarks
     *
     *@param  toParse     String to parse
     *@param  moveNumber  Move number
     *@return             True if successful, else false
     */
    private String parseTextMark(String toParse, int moveNumber) {
        // Loop through Text lines
        int pos = toParse.indexOf("#");
        if (pos == -1) // No marks given
            return toParse;

        ArrayList markList = new ArrayList();
        String comment = "";
        try {
            comment = toParse.substring(0, pos - 2);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse comment section: " + e);
            return toParse;
        }
        int end;
        do {
            end = toParse.indexOf("\n", pos);
            if (end == -1)
                end = toParse.length();
            try {
                String line = toParse.substring(pos, end);
                parseMark(line, markList);
            } catch (StringIndexOutOfBoundsException e) {
                System.err.println("Failed to loop through marks: " + e);
                return comment;
            }
        } while ((pos = toParse.indexOf("#", end)) != -1);

        if (!markList.isEmpty())
            textMarks.put(new Integer(moveNumber), markList);

        return comment;
    } //}}}

    //{{{ parseMark() method
    /**
     *  Parse a text mark line from UGF figure section
     *
     *@param  toParse   String to parse
     *@param  markList  ArrayList containing all marks of this move
     */
    private void parseMark(String toParse, ArrayList markList) {
        try {
            // X position
            int pos = toParse.indexOf(",");
            int end = toParse.indexOf(",", pos + 1);
            if (pos == -1 || end == -1)
                return;
            int x = Utils.convertStringToInt(toParse.substring(pos + 1, end));

            // Y position
            pos = toParse.indexOf(",", ++pos);
            end = toParse.indexOf(",", pos + 1);
            if (pos == -1 || end == -1)
                return;
            int y;
            if (coordType == COORD_TYPE_IGS)
                y = boardSize - Utils.convertStringToInt(toParse.substring(pos + 1, end)) + 1;
            else
                y = Utils.convertStringToInt(toParse.substring(pos + 1, end));

            // Mark letter
            pos = toParse.indexOf(",", ++pos);
            end = toParse.length();
            if (pos == -1 || end == -1)
                return;
            String txt = toParse.substring(pos + 1, end);

            // Store mark in array. We will add all marks and the comment to a move later in one step
            markList.add(new UGFMark(x, y, txt));
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse marks: " + e);
            return;
        }
    } //}}}

    //{{{ parseResult() method
    /**
     *  Parse and decode the UGF result section
     *
     *@param  toParse  String to parse
     *@return          Human readable result text
     */
    private String parseResult(String toParse) {
        String winner = "";
        String reason = "";
        String result = "";

        try {
            // Get first section: Winner
            int pos = toParse.indexOf(",");
            winner = toParse.substring(0, pos == -1 ? toParse.length() : pos);

            // Get second section: Reason
            if (pos != -1)
                reason = toParse.substring(++pos, toParse.length());
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse UGF result: " + e);
            return toParse;
        }

        // Parse Winner section
        if (winner.equals("B"))
            result = "Black";
        else if (winner.equals("W"))
            result = "White";
        else if (winner.equals("D"))
            return "Jigo";
        else if (winner.equals("P"))
            return "Playing";
        else if (winner.equals("A"))
            return "Adjourned";
        else if (winner.equals("N1"))
            return "No game";
        else if (winner.equals("O"))
            return "Both lost";
        else
            return winner;

        // Parse Reason section
        if (reason.equals("C"))
            result += "+Res";
        else if (reason.equals("T"))
            result += "+Time";
        else if (reason.equals("G"))
            result += "+Abstenation";
        else if (reason.equals("N"))
            result += "+Forfeit";
        else if (reason.length() > 0)
            result += "+" + reason;

        return result;
    } //}}}
}

//{{{ UGFMark class
/**
 *  Simple helper class. This is a subclass of Position, plus a String.
 *  Used for storing UGF marks. No need to use the MarkText class with its
 *  overhead.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $; $Date: 2002/10/11 05:34:55 $
 */
class UGFMark extends Position {

    String text;

    /**
     *Constructor for the UGFMark object
     *
     *@param  x    X position
     *@param  y    Y position
     *@param  txt  Mark text
     */
    UGFMark(int x, int y, String txt) {
        super(x, y);
        text = txt;
    }

    /**
     *  Convert this class to a String, for debugging
     *
     *@return    Converted String
     */
    public String toString() {
        return super.toString() + " - " + text;
    }
} //}}}

