/*
 * igs_parser.h
 *
 * $Id: igs_parser.h,v 1.28 2003/11/24 15:58:11 peter Exp $
 *
 * glGo, a prototype for a 3D Goban based on wxWindows, OpenGL and SDL.
 * Copyright (c) 2003, Peter Strempel <pstrempel@gmx.de>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#ifndef IGS_PARSER_H
#define IGS_PARSER_H

#ifdef __GNUG__
#pragma interface "igs_parser.h"
#endif

#include <wx/regex.h>
#include "defines.h"
#include "igs_game.h"
#include "igs_player.h"

class IGSParser;
class IGSConnection;

/** @addtogroup igs
 * @{ */

typedef bool (IGSParser::*regExFunction)(const wxString&);

/** An entry in the regex table used by the IGSParser class. */
struct RegExEntry
{
    RegExEntry(const wxString &expr, regExFunction fp)
        : expr(expr), fp(fp) {}
    wxString expr;
    regExFunction fp;
};

/** A move in an IGS game. */
struct GameMove
{
    short id, white_stones, black_stones;
    int white_time, black_time;
    wxString white, black;
    bool is_new;
};

/** Temporary used struct for the two-line kibitz/chatter parsing. */
struct Kibitz
{
    wxString name, txt;
    int id;
};


/**
 * Main IGS stream parser.
 * this class receives the unparsed buffer read from the network socket. First it
 * extracts complete lines from the buffer, and then parses each line using
 * a table of precompiled regexp. Subsequent functions depending on the line-parsing
 * step will be called.
 * All regexp are defined in igs_regex.h
 * @todo this class can be optimized by precompiling a couple of the locally used
 * regexp, however for debugging and development purposes it's currently much easier
 * to define them locally.
 */
class IGSParser
{
public:
    /** Constructor */
    IGSParser(IGSConnection *con);

    /** Destructor */
    ~IGSParser();

    /** Reset parser to offline state */
    void reset();

    /**
     * The entry method for parsing the read socket buffer.
     * The buffer received from the socket IGSConnection will be parsed line by line in this function.
     * @param toParse Complete buffer read from socket
     * @param result Reference to an empty array list which will be filled with the parse
     *       lines for display in the output textarea.
     */
    void parseBuffer(const wxString &toParse, wxArrayString &result);

    /**
     * Parse a game result and translate it into a legal SGF parameter.
     * For example, the IGS result message "W 85.0 B 84.5" will be translated
     * into "W+0.5". This is only used for observed games.
     * @param result IGS result to parse
     * @return SGF parameter
     */
    wxString translateResultToSGF(const wxString &result);

    short moves_flag, gameinfo_flag, players_flag, reload_flag;
    bool files_flag, parse_decline_flag, stats_flag, enter_score_flag;
    wxString reload_opponent;

private:
    void prepareRegEx();

    /**
     * Parse a one-liner IGS command. This is the main parsing function for all IGS input.
     * this function loops through the regex table until one pattern matches, then calls
     * the proper function pointer associated with the matched regex. If that function
     * handles the line properly, it should return true to indicate the line should not
     * be displayed in the textarea. if no regex matches, this function returns false
     * so the line appears in the console. if the line must be altered, the subfunction
     * can copy the modified line into the class member mod_str, which will be then passed
     * back to the textcontrol instead of the original line. if mod_str is empty and the
     * subfunction returns false, the original is printed to the console.
     * @param toParse String containing a single line.
     * @return true if the line was completely handled and should not be displayed in the
     *         terminal, false if the line should be displayed in the output textcontrol
     */
    bool parseLine(const wxString &toParse);
    bool parseLoginMsg(const wxString &toParse);
    /** @todo Some cleanups required here */
    bool parseGameMoveHeader(const wxString &toParse);
    /** @todo only 19x19 supported right now */
    bool parseGameMove(const wxString &toParse);
    bool parseGameInfoStart(const wxString &toParse);
    bool parseGameInfo(const wxString &toParse);
    bool parseUserStart(const wxString &toParse);
    bool parseUser(const wxString &toParse);
    bool parseState(const wxString &toParse);
    bool parseShout(const wxString &toParse);
    bool parseTell(const wxString &toParse);
    bool parseKibitz(const wxString &toParse);
    bool parseSay(const wxString &toParse);
    bool parseAdjournRequest(const wxString &toParse);
    bool parseAdjournDecline(const wxString &toParse);
    /** @todo only 19x19 supported right now */
    bool parseUndoOwn(const wxString &toParse);
    /** @todo only 19x19 supported right now */
    bool parseUndoObserved(const wxString &toParse);
    bool parseErrorInfo(const wxString &toParse);
    bool parseFile(const wxString &toParse);
    bool parseGameEnd(const wxString &toParse);
    bool parseUnused(const wxString &toParse);
    bool parseMatchStart(const wxString &toParse);
    bool parseMatchEnd(const wxString &toParse);
    bool parsePlayerStats(const wxString &toParse);
    bool parseStored(const wxString &toParse);
    bool parseMatch(const wxString &toParse);
    /** @todo only 19x19 supported right now */
    bool parseScoreRemove(const wxString &toParse);
    bool parseStatus(const wxString &toParse);
    bool parseAutoMessage(const wxString &toParse);

    IGSConnection *connection;
    wxString rest, mod_str, game_title, all_str;
    bool logged_on, parse_player_stats_flag, skip_guests;
    short all_id, game_title_id, move_header_id, undo_id;
    const static RegExEntry regExEntryList[];
    wxRegEx **regExList;
    wxRegEx lineEx, loginEx, gameIDEx, gameEndEx;
    GameMove tmpMove;
    PlayerList playerList;
    GamesList gamesList;
    size_t prog_lock;
    Kibitz kibitz;
    PlayerInfo playerInfo;
    wxArrayString statusArray;
};

/** @} */  // End of group

#endif
