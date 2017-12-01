/*
 * igs_parser.cpp
 *
 * $Id: igs_parser.cpp,v 1.51 2003/11/24 15:58:11 peter Exp $
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

#ifdef __GNUG__
#pragma implementation "igs_parser.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/log.h>
#include <wx/intl.h>
#include <wx/frame.h>
#endif

#include <wx/config.h>
#include "igs_parser.h"
#include "igs_connection.h"
#include "igs_mainframe.h"
#include "utils/utils.h"
#include "stones.h"
#include "autoupdater.h"
#include "igs_regex.h"


/** @addtogroup igs
 * @{ */

/** Convinience macro for better readability */
#define REGEXENTRY(ex,fun) RegExEntry(ex, &IGSParser::fun)

/** Size of regex list */
#define REGEXLIST_SIZE 24

const RegExEntry IGSParser::regExEntryList[] = {
    REGEXENTRY( IGSREGEX_UNUSED,         parseUnused ),
    REGEXENTRY( IGSREGEX_STATE,          parseState ),
    REGEXENTRY( IGSREGEX_ERROR_INFO,     parseErrorInfo ),
    REGEXENTRY( IGSREGEX_AUTOMSG,        parseAutoMessage ),
    REGEXENTRY( IGSREGEX_GAMEMOVEHEADER, parseGameMoveHeader ),
    REGEXENTRY( IGSREGEX_GAMEMOVE,       parseGameMove ),
    REGEXENTRY( IGSREGEX_GAMEINFO_START, parseGameInfoStart ),
    REGEXENTRY( IGSREGEX_GAMEINFO,       parseGameInfo ),
    REGEXENTRY( IGSREGEX_USER_START,     parseUserStart ),
    REGEXENTRY( IGSREGEX_USER,           parseUser ),
    REGEXENTRY( IGSREGEX_SHOUT,          parseShout ),
    REGEXENTRY( IGSREGEX_TELL,           parseTell ),
    REGEXENTRY( IGSREGEX_KIBITZ,         parseKibitz ),
    REGEXENTRY( IGSREGEX_SAY,            parseSay ),
    REGEXENTRY( IGSREGEX_ADJOURNREQUEST, parseAdjournRequest ),
    REGEXENTRY( IGSREGEX_ADJOURNDECLINE, parseAdjournDecline ),
    REGEXENTRY( IGSREGEX_UNDO_OWN,       parseUndoOwn ),
    REGEXENTRY( IGSREGEX_UNDO_OBSERVED,  parseUndoObserved ),
    REGEXENTRY( IGSREGEX_STORED,         parseStored ),
    REGEXENTRY( IGSREGEX_SCORE_REMOVE,   parseScoreRemove ),
    REGEXENTRY( IGSREGEX_SCORE_DONE,     parseMatchEnd ),
    REGEXENTRY( IGSREGEX_STATUS,         parseStatus ),
    REGEXENTRY( IGSREGEX_FILE,           parseFile ),
    REGEXENTRY( IGSREGEX_LOGINMSG,       parseLoginMsg )
};

/** @} */  // End of group


IGSParser::IGSParser(IGSConnection *con)
    : connection(con)
{
    wxASSERT(con != NULL);
    prepareRegEx();
    reset();
}

IGSParser::~IGSParser()
{
    wxLogDebug("~IGSParser()");

    // Delete regEx list
    for (int i=0; i<REGEXLIST_SIZE; i++)
        delete regExList[i];
    delete[] regExList;

    gamesList.Empty();
}

void IGSParser::reset()
{
    logged_on = files_flag = parse_decline_flag = parse_player_stats_flag = stats_flag =
        enter_score_flag = skip_guests = false;
    rest = wxEmptyString;
    mod_str = wxEmptyString;
    game_title = wxEmptyString;
    moves_flag = gameinfo_flag = players_flag = reload_flag = game_title_id = undo_id = move_header_id = 0;
    tmpMove.id = 0;
    tmpMove.is_new = false;
    prog_lock = 0;
    all_id = -1;
    all_str = wxEmptyString;
    reload_opponent = wxEmptyString;
    statusArray.Clear();
}

void IGSParser::prepareRegEx()
{
    // Compile some special entries.
    if (!lineEx.Compile(IGSREGEX_LINE, wxRE_NEWLINE) ||
        !loginEx.Compile(IGSREGEX_LOGIN) ||
        !gameIDEx.Compile(IGSREGEX_GAMEID) ||
        !gameEndEx.Compile(IGSREGEX_GAMEEND))
    {
        wxFAIL_MSG(_T("Invalid common RegEx"));
        return;
    }

    // Compile the table
    regExList = new wxRegEx*[REGEXLIST_SIZE];
    for (int i=0; i<REGEXLIST_SIZE; i++)
    {
        regExList[i] = new wxRegEx();
        if (!regExList[i]->Compile(regExEntryList[i].expr))
        {
            wxFAIL_MSG(wxString::Format(_T("Invalid RegEx %d: %s"), i, regExEntryList[i].expr.c_str()));
            return;
        }
    }
}

void IGSParser::parseBuffer(const wxString &toParse, wxArrayString &result)
{
    // Prepend rest to buffer
    wxString s = rest.Append(toParse);
    wxString t = s;

    // Loop through the whole buffer, extracting full lines from the buffer until
    // the buffer is empty or only contains an incomplete line, which will be
    // remembered as rest for the next call of this function.
    while (true)
    {
        // Login and password must be handled here, they use a special format
        // The boolean flag is checked to avoid repetitive regEx calls once logged on
        if (!logged_on && loginEx.Matches(t))
        {
            // Send login name
            wxString login = connection->getLoginName();
            if (!login.empty())
                connection->sendCommand(login);
            // Send password
            login = connection->getPassword();
            if (!login.empty())
                connection->sendCommand(login);
            t = wxEmptyString;
            break;
        }

        // Full line match?
        if (!lineEx.Matches(t))
            break;

        // Extract match and parse line
        size_t start, len;
        lineEx.GetMatch(&start, &len);
        wxString line = t.Mid(start, len-1);
        // Parse line and kill empty lines if we are not within a "8/9 File" section
        if (!parseLine(line) && (files_flag || !line.Strip(wxString::both).empty()))
            // Return the original or modified string if set
            result.Add(mod_str.empty() ? line : mod_str);

        // Cut off anything before our extracted match. Break if buffer is empty.
        int l = t.Length()-len-1;
        if (l <= 0)
            break;
        t = t.Right(l);
    }
    // Remember rest of the buffer which contains an unfinished line
    rest = t;
}

bool IGSParser::parseLine(const wxString &toParse)
{
    // wxLogDebug("Line: %s", toParse.c_str());

    // Reset modified buffer
    mod_str = wxEmptyString;

    // Loop through all available regExs and if there is a match, call the proper function
    // I think this is quite nifty, it reduces the 800 lines in doParse() of gGo/Java to
    // only 9 lines. :*)
    for (int i=0; i<REGEXLIST_SIZE; i++)
    {
        if (regExList[i]->Matches(toParse))
        {
            size_t start, len;
            regExList[i]->GetMatch(&start, &len);
            return (this->*(regExEntryList[i].fp))(toParse.Mid(start, len));
        }
    }
    // If no match, print line to terminal
    return false;
}

bool IGSParser::parseLoginMsg(const wxString &toParse)
{
    // Typing "help motd" will trigger this again
    if (logged_on)
        return false;

    logged_on = true;
    connection->sendCommand(_T("toggle client true"));
    // Do not send "id" as guest
    if ((connection->getLoginName()).Cmp(_T("guest")))
        connection->sendCommand(wxString::Format("id %s %s", PACKAGE, VERSION));
    return false;
}

bool IGSParser::parseGameMoveHeader(const wxString &toParse)
{
    // wxLogDebug("parseGameMoveHeader: %s", toParse.c_str());

    // 15 Game 1 I: death (4 4629 -1) vs Bob (2 4486 -1)
    size_t start, len;

    // Game ID
    if (gameIDEx.Matches(toParse))
    {
        gameIDEx.GetMatch(&start, &len);
        wxString s = toParse.Mid(start+5, len);
        tmpMove.id = wxAtoi(s);
    }
    else
        return false;

    // White name
    wxRegEx exW(IGSREGEX_GAMEHEADER_WHITE);
    wxASSERT(exW.IsValid());
    if (exW.Matches(toParse))
    {
        exW.GetMatch(&start, &len);
        tmpMove.white = toParse.Mid(start, len-2);
    }
    else
        return false;

    // Black name
    wxRegEx exB(IGSREGEX_GAMEHEADER_BLACK);
    wxASSERT(exB.IsValid());
    if (exB.Matches(toParse))
    {
        exB.GetMatch(&start, &len);
        tmpMove.black = toParse.Mid(start+3, len-5);
    }
    else
        return false;

    // White time
    wxRegEx exWT(IGSREGEX_GAMEHEADER_WHITE_TIME);
    wxASSERT(exWT.IsValid());
    if (exWT.Matches(toParse))
    {
        exWT.GetMatch(&start, &len);
        wxString time_str = toParse.Mid(start+1, len-5);
        int pos = time_str.find(" ");
        int pos2 = time_str.find(" ", ++pos);
        wxString time = time_str.Mid(pos, pos2-pos);
        wxString stones = time_str.Mid(pos2+1);

        tmpMove.white_time = wxAtoi(time);
        tmpMove.white_stones = wxAtoi(stones);
    }
    else
        return false;

    // Black time
    wxRegEx exBT(IGSREGEX_GAMEHEADER_BLACK_TIME);
    wxASSERT(exBT.IsValid());
    if (exBT.Matches(toParse))
    {
        exBT.GetMatch(&start, &len);
        wxString time_str = toParse.Mid(start+1, len-2);
        int pos = time_str.find(" ");
        int pos2 = time_str.find(" ", ++pos);
        wxString time = time_str.Mid(pos, pos2-pos);
        wxString stones = time_str.Mid(pos2+1);

        tmpMove.black_time = wxAtoi(time);
        tmpMove.black_stones = wxAtoi(stones);
    }
    else
        return false;

    if (!moves_flag)
        // Regular move
        tmpMove.is_new = true;
    else
        // Any move in a "moves" sequence.
        // Note: Careful, if a regular move comes in after sending "moves", this flag
        // will still be set. Logic check happens below in parseGameMove()
        tmpMove.is_new = false;

    // Required for automatch and observed games at move 0 to detect headers without moves
    move_header_id = tmpMove.id;

    // Eat line
    return true;
}

bool IGSParser::parseGameMove(const wxString &toParse)
{
    // wxLogDebug("parseGameMove: %s", toParse.c_str());

    move_header_id = 0;
    undo_id = 0;

    int move_num = 0;
    wxString col = wxEmptyString;
    wxString move = wxEmptyString;

    // Move number
    move_num = wxAtoi(toParse.Mid(3, 5).Trim(false));

    // Color (B/W)
    col = toParse.Mid(7, 1);
    Color turn = !col.Cmp("B") ? STONE_BLACK : STONE_WHITE;

    short x, y;
    Stones captures;

    // Handicap?  [15   0(B): Handicap 6]
    if (toParse.Find("Handicap") != -1)
    {
        wxString handicap_str = toParse.Mid(20);
        // x = -2 (handicap setup code), y = number of handicap stones
        x = -2;
        y = wxAtoi(handicap_str);
    }
    // Pass?      [15   2(B): Pass]
    else if (toParse.Find("Pass") != -1)
    {
        x = y = -1;
    }
    else
    {
        // Move
        move = toParse.Mid(11, 3).Trim();  // This is the first move in the list
        parseStringMove(move, 19 /* TODO */, x, y);

        // Captures, if any
        wxString caps = toParse.Mid(14);
        // Any captures given?
        if (!caps.empty())
        {
            wxRegEx rex(IGSREGEX_GAMEMOVE_CAPS);
            wxASSERT(rex.IsValid());
            size_t start, len;

            // Iterate through captures and assemble a list
            while (rex.Matches(caps))
            {
                rex.GetMatch(&start, &len);
                wxString s = caps.Mid(start, len);
                short x, y;
                parseStringMove(s, 19 /* TODO */, x, y);
                captures.push_back(Stone(x, y, reverseColor(turn)));
                caps = caps.Mid(len+1);
            }
        }
    }

    // Logic check: If we sent "moves" and then a move from "observe" comes in, we
    // need to avoid unblocking the board because if that move already
    // Catch the definate _first_ move of a game. If the game has zero moves and "moves"
    // gives an empty list, then this won't cause a problem.
    if (moves_flag == 1 && move_num == 0)
        moves_flag = 2;

    // Load check
    if (reload_flag == 1)
        reload_flag = 2;

    // Generate event
    EventPlayIGSMove evt(x, y, turn, captures, tmpMove.id, move_num,
                         tmpMove.white_time, tmpMove.black_time,
                         tmpMove.white_stones, tmpMove.black_stones,
                         tmpMove.is_new);
    connection->AddPendingEvent(evt);

    // Last of three passes?
    if (enter_score_flag && x == -1 && y == -1)
    {
        // Generate dummy event to tell the board to score after the above pass
        EventPlayIGSMove evt(-2, -2, STONE_UNDEFINED, Stones(), tmpMove.id);
        connection->AddPendingEvent(evt);
        connection->enterScoreMode(tmpMove.id);
        enter_score_flag = false;
    }

    // Eat line
    return true;
}

bool IGSParser::parseState(const wxString &toParse)
{
    // wxLogDebug("parseState: %s", toParse.c_str());

    // End of "moves" sequence when observing or reloading
    if (moves_flag == 2 && (!toParse.Cmp("1 8") || !toParse.Cmp("1 6")))
    {
        wxLogDebug("END OF MOVES");
        moves_flag = 0;

        // Generate dummy event so the eventhandler will unblock the board
        EventPlayIGSMove evt(-1, -1, STONE_UNDEFINED, Stones(), tmpMove.id);
        connection->AddPendingEvent(evt);
    }

    // End of "game <id>" command when observe or own game started
    else if (gameinfo_flag == 2 && (!toParse.Cmp("1 8") || !toParse.Cmp("1 6")))
    {
        // Send game data to board
        // The list should (!) have exactly one item right now which is the new observed game
        wxASSERT(gamesList.GetCount() == 1);
        if (!gamesList.IsEmpty())
        {
            connection->updateGameData(gamesList.Item(0), gamesList.Item(0).id == game_title_id ? game_title : "");
            game_title = wxEmptyString;
            gameinfo_flag = game_title_id = 0;
        }
    }

    // End of "games" batch
    else if (gameinfo_flag == 4)
    {
        wxLogDebug("END OF GAMES BATCH");
        wxASSERT(connection->getIGSMainFrame() != NULL);
        connection->getIGSMainFrame()->updateGamesList(gamesList);
        gameinfo_flag = 0;
        prog_lock = 0;
    }

    // Load
    else if (reload_flag && !reload_opponent.empty() && !toParse.Cmp("1 6"))
    {
        wxASSERT(!reload_opponent.Cmp(tmpMove.white) ||
                 !reload_opponent.Cmp(tmpMove.black));
        if (!(!reload_opponent.Cmp(tmpMove.white) ||
              !reload_opponent.Cmp(tmpMove.black)))
        {
            LOG_IGS(_T("Trying to load game, but opponents do not match!"));
            return false;
        }
        reload_flag = false;
        reload_opponent = wxEmptyString;
        connection->startMatch(tmpMove.id, tmpMove.white, tmpMove.black,
                               tmpMove.white_time, tmpMove.black_time, true);
        move_header_id = 0;
    }

    // Move header without move: Automatch and Observed games with 0 moves
    else if (move_header_id && move_header_id == tmpMove.id)
    {
        // Automatch start. moves_flag check to prevent opening two boards
        // when reloading a game with zero moves.
        if (!toParse.Cmp("1 6") && moves_flag != 1 && undo_id != tmpMove.id)
        {
            wxLogDebug("Automatch start detected: id = %d, moves_flag = %d", tmpMove.id, moves_flag);
            connection->startMatch(tmpMove.id, tmpMove.white, tmpMove.black,
                                   tmpMove.white_time, tmpMove.black_time, false);
            move_header_id = 0;
            undo_id = 0;
        }
        // Observed game at move 0 (common in tournament broadcasts)
        else if (!toParse.Cmp("1 8") && undo_id != tmpMove.id)
        {
            wxLogDebug("Observed game with 0 moves detected: %d", tmpMove.id);
            connection->startObserve(tmpMove.id);
            move_header_id = 0;
        }
    }

    // Eat line
    return true;
}

bool IGSParser::parseShout(const wxString &toParse)
{
    // wxLogDebug("parseShout: %s", toParse.c_str());

    wxString name = wxEmptyString;
    wxString text = wxEmptyString;

    wxRegEx shoutEx(IGSREGEX_SHOUTNAME);
    wxASSERT(shoutEx.IsValid());
    if (shoutEx.Matches(toParse))
    {
        size_t start, len;
        shoutEx.GetMatch(&start, &len);
        name = toParse.Mid(start+1, len-2);
        text = toParse.Mid(len+5);
    }
    else
        return false;

    EventIGSComm evt(text, name, IGS_COMM_TYPE_SHOUT);
    connection->getIGSMainFrame()->AddPendingEvent(evt);

    // Show line without "21 " if the option is enabled
    bool show_shouts;
    wxConfig::Get()->Read(_T("IGS/ShoutsInTerminal"), &show_shouts, true);
    if (show_shouts)
    {
        mod_str = toParse.Mid(3).Trim();
        return false;
    }
    // If disabled, eat line
    return true;
}

bool IGSParser::parseTell(const wxString &toParse)
{
    // wxLogDebug("parseTell: %s", toParse.c_str());

    // No tell frame for this, too annoying
    // qgodev: CLIENT: <cgoban 1.9.14> match qgodev wants handicap 0, komi 5.5, free
    if (toParse.Find("CLIENT: <cgoban ") != -1)
    {
        mod_str = toParse.Mid(3).Trim();
        return false;
    }

    wxString name = wxEmptyString;
    wxString text = wxEmptyString;

    wxRegEx shoutEx(IGSREGEX_TELLNAME);
    wxASSERT(shoutEx.IsValid());
    if (shoutEx.Matches(toParse))
    {
        size_t start, len;
        shoutEx.GetMatch(&start, &len);
        name = toParse.Mid(start+1, len-2);
        text = toParse.Mid(len+5);
    }
    else
        return false;

    EventIGSComm evt(text, name, IGS_COMM_TYPE_TELL);
    connection->getIGSMainFrame()->AddPendingEvent(evt);

    // Show line without "24 "
    // TODO: Probably can be removed
    mod_str = toParse.Mid(3).Trim();
    return false;
}

bool IGSParser::parseKibitz(const wxString &toParse)
{
    // wxLogDebug("parseKibitz: %s", toParse.c_str());

    size_t start, len;

    wxRegEx nameEx(IGSREGEX_KIBITZ_NAME);
    wxASSERT(nameEx.IsValid());
    if (nameEx.Matches(toParse))
    {
        nameEx.GetMatch(&start, &len);
        kibitz.name = toParse.Mid(start, len);

        wxRegEx idEx(IGSREGEX_KIBITZ_ID);
        wxASSERT(idEx.IsValid());
        if (idEx.Matches(toParse))
        {
            idEx.GetMatch(&start, &len);
            kibitz.id = wxAtoi(toParse.Mid(start+1, len-1));
        }
    }
    else
    {
        kibitz.txt = toParse.Mid(2).Trim(false);

        EventIGSComm evt(kibitz.txt, kibitz.name, IGS_COMM_TYPE_KIBITZ, kibitz.id);
        connection->getIGSMainFrame()->AddPendingEvent(evt);
    }

    // Eat line
    return true;
}

bool IGSParser::parseSay(const wxString &toParse)
{
    // wxLogDebug("parseSay: %s", toParse.c_str());

    // 19 *qgodev*: hello

    size_t start, len;
    wxString name, txt;

    wxRegEx nameEx(IGSREGEX_SAY_NAME);
    wxASSERT(nameEx.IsValid());
    if (nameEx.Matches(toParse))
    {
        nameEx.GetMatch(&start, &len);
        name = toParse.Mid(start+1, len-2);
        txt = toParse.Mid(len+5);
    }
    else
    {
        // Oops
        mod_str = toParse.Mid(3).Trim();
        return false;
    }

    EventIGSComm evt(txt, name, IGS_COMM_TYPE_SAY);
    connection->getIGSMainFrame()->AddPendingEvent(evt);

    // Eat line
    return true;
}

bool IGSParser::parseAdjournRequest(const wxString &toParse)
{
    // wxLogDebug("parseAdjournRequest: %s", toParse.c_str());

    // 48 Game 141 qgodev requests an adjournment
    int id = wxAtoi(toParse.Mid(8, toParse.find(" ", 8) - 8));
    if (connection->distributeAdjournRequest(id))
        // Eat line
        return true;
    mod_str = toParse.Mid(3);
    return false;
}

bool IGSParser::parseAdjournDecline(const wxString &toParse)
{
    // wxLogDebug("parseAdjournDecline: %s, parse_decline_flag = %d", toParse.c_str(), parse_decline_flag);

    // This is crap, but IGS sends this message even when the user declined himself!
    if (!parse_decline_flag)
    {
        // 53 Game 8 adjournment is declined.
        int id = wxAtoi(toParse.Mid(8, toParse.find(" ", 8) - 8));
        parse_decline_flag = false;
        if (connection->distributeAdjournDecline(id))
            // Eat line
            return true;
    }
    parse_decline_flag = false;
    mod_str = toParse.Mid(2);
    return false;
}

bool IGSParser::parseUndoOwn(const wxString &toParse)
{
    // wxLogDebug("parseUndoOwn: %s", toParse.c_str());

    // 28 qgodev undid the last move (A1) .

    short x, y;
    wxString move_str = wxEmptyString;
    wxRegEx moveEx(IGSREGEX_UNDO_MOVE);
    wxASSERT(moveEx.IsValid());
    if (moveEx.Matches(toParse))
    {
        size_t start, len;
        moveEx.GetMatch(&start, &len);
        move_str = toParse.Mid(start+1, len-2);
        // wxLogDebug("UNDO MOVE: <%s>", move_str.c_str());
        if (move_str.Cmp("Pass"))
            // Normal move
            parseStringMove(move_str, 19 /* TODO */, x, y);
        else
            // Pass
            x = y = -1;
    }
    else
    {
        LOG_IGS(wxString::Format("Failed to parse undo string: %s", toParse.c_str()));
        return false;
    }

    // Prevent opening a new board when undo'ing the first move
    undo_id = tmpMove.id;

    // parseGameMoveHeader was just called before, so we have the ID here
    if (!connection->distributeUndo(tmpMove.id, x, y, move_str))
    {
        mod_str = toParse.Mid(3);
        return false;
    }

    // Eat line
    return true;
}

bool IGSParser::parseUndoObserved(const wxString &toParse)
{
    // wxLogDebug("parseUndoObserved: %s", toParse.c_str());

    // Undo in game 293: ggodev vs qgodev:   T1

    // Get id and move
    int id = 0;
    id = wxAtoi(toParse.Mid(15, toParse.find(':', 15)-15));
    wxString move_str = toParse.Right(3).Trim(false);

    wxLogDebug("Undo observed: ID =  %d, move_str = %s", id, move_str.c_str());

    // Parse move
    short x, y;
    if (move_str.Cmp("Pass"))
        // Normal move
        parseStringMove(move_str, 19 /* TODO */, x, y);
    else
        // Pass
        x = y = -1;

    // Prevent opening a new board when undo'ing the first move
    undo_id = tmpMove.id;

    if (!connection->distributeUndo(id, x, y, move_str))
    {
        mod_str = toParse.Mid(3);
        return false;
    }

    // Eat line
    return true;
}

bool IGSParser::parseErrorInfo(const wxString &toParse)
{
    wxLogDebug("parseErrorInfo: %s", toParse.c_str());

    int pos;
    // 9 Game is titled: 28th Meijin 2nd game Yoda Norimoto Meijin(W) vs Yamashita Keigo Kisei(B)
    if ((pos = toParse.Find(_T("Game is titled: "))) != -1)
    {
        game_title = toParse.Mid(18);
        game_title_id = tmpMove.id;
        return true;
    }

    // "All" feedback:
    // 9 Observing game 3 (kosumi vs. denghui) :
    // 9       guest1316 NR          guest4760 NR
    else if (toParse.StartsWith(_T("9 Observing game ")))
    {
        wxString id_str = toParse.Mid(17, toParse.find(" ", 17)-17);
        all_id = wxAtoi(id_str);
        all_str = wxEmptyString;
    }
    else if (all_id != -1 && toParse.StartsWith("9       "))
    {
        all_str += toParse.Mid(2) + "\n";
    }
    else if (all_id != -1 && toParse.StartsWith(_T("9 Found ")) &&
             toParse.Find(_T("observer")) != -1)
    {
        all_str += toParse.Mid(2) + "\n";
        connection->distributeObservers(all_id, all_str);
        all_id = -1;
        all_str = wxEmptyString;
    }

    // Game end: 9 {Game 129: Order vs j33 : W 66.5 B 82.0}
    else if (gameEndEx.Matches(toParse))
        return parseGameEnd(toParse);

    // Match start:
    else if (toParse.StartsWith(_T("9 Creating match [")) ||
             toParse.StartsWith(_T("9 Match [")))
        return parseMatchStart(toParse);

    // Match end:
    // 9 qgodev has resigned the game
    // 9 ggodev has run out of time.
    else if (toParse.Find(_T("has resigned the game")) != -1 ||
             toParse.Find(_T("has run out of time.")) != -1)
        return parseMatchEnd(toParse);

    // Load: 9 ggodev has restarted your game.
    else if (toParse.Find(_T("has restarted your game.")) != -1)
    {
        // Remember opponent for the next incoming game move
        reload_flag = 1;
        reload_opponent = toParse.Mid(2, toParse.find(" ", 2) - 2);
    }

    // Error while playing
    // 5 It is not your turn
    // 5 Illegal move
    else if (!toParse.Cmp(_T("5 It is not your turn")) ||
             !toParse.Cmp(_T("5 Illegal move")))
        playSound(SOUND_BEEP);  // Be annoying :*)

    // Adjourn, own game: 9 Game has been adjourned.
    else if (!toParse.Cmp(_T("9 Game has been adjourned.")))
        // Thank you, no game ID here... ;(
        // And even worse, sometimes IGS sends this message twice!
        connection->distributeAdjournOwn();

    // Adjourn, observed game: 9 Game 98: mesa vs kc1234 has adjourned.
    else if (toParse.StartsWith(_T("9 Game ")) && toParse.Find(_T("has adjourned.")) != -1)
        connection->distributeAdjournObserved(wxAtoi(toParse.Mid(7, toParse.find(":", 7) - 7)));

    // Broadcast: 9 !!*TODAY*!!: Meijin Title live on IGS - see    help motd
    else if (toParse.StartsWith("9 !!"))
    {
        EventIGSComm evt(toParse.Mid(2), wxEmptyString, IGS_COMM_TYPE_SHOUT);
        connection->getIGSMainFrame()->AddPendingEvent(evt);
    }

    // End of "user".
    // We need to catch this to intercept other input while the user list is incoming.
    // Unfortunately this trick does not work with the games list.
    else if (toParse.StartsWith(_T("9                 ******** ")) && players_flag == 2)
    {
        wxLogDebug("END OF PLAYERS BATCH");
        wxASSERT(connection->getIGSMainFrame() != NULL);
        connection->getIGSMainFrame()->updatePlayerList(playerList);
        players_flag = 0;
        prog_lock = 0;
    }

    // Begin of "stats" feedback: 9 Player:      ggodev
    else if (stats_flag && toParse.StartsWith("9 Player:") || parse_player_stats_flag)
    {
        parsePlayerStats(toParse);
        return true;  // Eat line, this was called from the player table.
    }

    // 9 Use <match qgodev B 19 10 20> or <decline qgodev> to respond.
    else if (toParse.StartsWith("9 Use <match "))
        parseMatch(toParse);

    // Some common error messages to display in a messagebox
    // 5 Cannot find player
    // 5 That player is currently not accepting matches.
    // 5 qgodev is currently involved in a match against someone else.
    else if (!toParse.Cmp("5 Cannot find player.") ||
             !toParse.Cmp("5 That player is currently not accepting matches.") ||
             toParse.Find(" is currently involved in a match against someone else.") != -1)
        connection->displayMessage(toParse.Mid(2), _("Error"), wxOK | wxICON_ERROR);

    // 5 Cannot find recipient. - Forward this to the tell frame if we have one
    else if (!toParse.Cmp("5 Cannot find recipient."))
        connection->getIGSMainFrame()->distributeTellError(toParse.Mid(2));

    // Enter score mode
    // 9 You can check your score with the score command, type 'done' when finished.
    else if (!toParse.Cmp("9 You can check your score with the score command, type 'done' when finished."))
        enter_score_flag = true;

    // Undo in score mode - no game ID given, thank you.
    // 9 Board is restored to what it was when you started scoring
    else if (!toParse.Cmp("9 Board is restored to what it was when you started scoring"))
        connection->undoScore(toParse.Mid(2));

    // 9 qgodev has typed done.
    else if (!toParse.Cmp("9 qgodev has typed done."))
        connection->distributeScoreDone(toParse.Mid(2, toParse.find(" ", 2) - 2), toParse.Mid(2));

    // Strip info and error lines, but display them
    mod_str = toParse.Mid(2);
    return false;
}

bool IGSParser::parseUnused(const wxString &toParse)
{
    // wxLogDebug("parseUnused: %s", toParse.c_str());

    // This is basically just a dummy function
    // Return true to eat the line
    return true;
}

bool IGSParser::parseGameInfoStart(const wxString &toParse)
{
    // wxLogDebug("parseGameInfoStart: %s", toParse.c_str());

    // Clear games list, a new batch is incoming
    gamesList.Empty();

    if (gameinfo_flag == 1)
        gameinfo_flag = 2;
    else if (gameinfo_flag == 3)
        gameinfo_flag = 4;

    // Eat line when the games are incoming from "observe" or the table
    if (gameinfo_flag != 0)
        return true;
    // Cut leading "7"
    mod_str = toParse.Mid(2).Trim();
    return false;
}

bool IGSParser::parseGameInfo(const wxString &toParse)
{
    // wxLogDebug("parseGameInfo: %s", toParse.c_str());

    // 7 [##]  white name [ rk ]      black name [ rk ] (Move size H Komi BY FR) (###)
    // 7 [ 2]      sanchi [ 8d*] vs.       torah [ 8d*] (  2   19  0  6.5  5  I) (  1)

    int game_id = 0, observers = 0;
    float data[5];
    wxString white_name = wxEmptyString;
    wxString white_rank = wxEmptyString;
    wxString black_name = wxEmptyString;
    wxString black_rank = wxEmptyString;
    IGSGameType type = IGS_GAME_TYPE_UNKNOWN;
    size_t start, len;
    wxString s;

    // Game ID
    wxRegEx rex(IGSREGEX_GAMEINFO_GAMEID);
    wxASSERT(rex.IsValid());
    if (rex.Matches(toParse))
    {
        rex.GetMatch(&start, &len);
        s = toParse.Mid(start+1, len-2).Trim(false);
        game_id = wxAtoi(s);
    }

    // White name
    rex.Compile(IGSREGEX_GAMEINFO_WHITE_NAME);
    wxASSERT(rex.IsValid());
    if (rex.Matches(toParse))
    {
        rex.GetMatch(&start, &len);
        s = toParse.Mid(start+1, len).Trim(false);
        white_name = s.Left(s.find("[")).Trim();
    }

    // White rank
    rex.Compile(IGSREGEX_GAMEINFO_WHITE_RANK);
    wxASSERT(rex.IsValid());
    if (rex.Matches(toParse))
    {
        rex.GetMatch(&start, &len);
        s = toParse.Mid(start+1, len).Trim(false);
        white_rank = s.Left(s.find("]")).Trim();
    }

    // Black name
    rex.Compile(IGSREGEX_GAMEINFO_BLACK_NAME);
    wxASSERT(rex.IsValid());
    if (rex.Matches(toParse))
    {
        rex.GetMatch(&start, &len);
        s = toParse.Mid(start+3, len).Trim(false);
        black_name = s.Left(s.find("[")).Trim();
    }

    // Black rank
    rex.Compile(IGSREGEX_GAMEINFO_BLACK_RANK);
    wxASSERT(rex.IsValid());
    if (rex.Matches(toParse))
    {
        rex.GetMatch(&start, &len);
        s = toParse.Mid(start+1, len).Trim(false);
        black_rank = s.Left(s.find("]")).Trim();
    }

    // Game info
    rex.Compile(IGSREGEX_GAMEINFO_DATA);
    wxASSERT(rex.IsValid());
    if (rex.Matches(toParse))
    {
        rex.GetMatch(&start, &len);
        s = toParse.Mid(start+1, len-3).Trim(false);

        wxString info;
        rex.Compile(IGSREGEX_GAMEINFO_DATAFIELD);
        wxASSERT(rex.IsValid());
        int i=0;
        while (rex.Matches(s))
        {
            rex.GetMatch(&start, &len);
            info = s.Mid(start, len).Trim();
            data[i++] = wxAtof(info);
            s = s.Mid(len).Trim(false);
        }
        // s has "FI" etc. now
        if (!s.Cmp("I"))
            type = IGS_GAME_TYPE_RATED;
        else if (!s.Cmp(_T("FI")))
            type = IGS_GAME_TYPE_FREE;
        else if (!s.Cmp(_T("TI")))
            type = IGS_GAME_TYPE_TEACH;
    }

    // Observers
    observers = wxAtoi(toParse.Mid(toParse.length()-4, 3));

    // Drop read data into class IGSGame and add it to the list
    gamesList.Add(IGSGame(game_id, white_name, white_rank, black_name, black_rank,
                          static_cast<int>(data[0]), static_cast<int>(data[1]), static_cast<int>(data[2]),
                          static_cast<int>(data[4]), observers, data[3], type));

    // Update gauge in IGSMainFrame statusbar
    if (gameinfo_flag == 4 && (++prog_lock % 4) == 0)
        connection->getIGSMainFrame()->updateGauge(gamesList.GetCount());

    // Eat line when the games are incoming from "observe" or the table
    if (gameinfo_flag != 0)
        return true;
    // Cut leading "7"
    mod_str = toParse.Mid(2).Trim();
    return false;
}

bool IGSParser::parseUserStart(const wxString &toParse)
{
    // wxLogDebug("parseUserStart: %s", toParse.c_str());

    // Clear player list, a new batch is incoming
    playerList.Empty();

    if (players_flag == 1)
        players_flag = 2;

    // Skip guests if this is options is set
    wxConfig::Get()->Read(_T("IGS/SkipGuests"), &skip_guests, true);

    // Eat line when the games are incoming from the table
    if (players_flag != 0)
        return true;
    // Cut leading "42"
    mod_str = toParse.Mid(3).Trim();
    return false;
}

bool IGSParser::parseUser(const wxString &toParse)
{
    // wxLogDebug("parseUser: %s", toParse.c_str());

    wxString name = toParse.Mid(3, 10).Trim(false);
    wxString info = toParse.Mid(15, 14).Trim();
    wxString country = toParse.Mid(31, 9).Trim();
    wxString rank = toParse.Mid(40, 4).Trim(false).Trim();
    wxString win_loss = toParse.Mid(44, 10).Trim();
    wxString obs = toParse.Mid(54, 3).Trim();
    wxString play = toParse.Mid(58, 3).Trim();
    wxString idle = toParse.Mid(63, 4).Trim(false).Trim();
    wxString flags = toParse.Mid(70, 3).Trim(false).Trim();

    // Drop read data into class IGSPlayer and add it to the list.
    if (!skip_guests || !name.StartsWith(_T("guest")))
        playerList.Add(IGSPlayer(name, rank, obs, play, win_loss, idle, flags, info, country));

    // Update gauge in IGSMainFrame statusbar
    if (players_flag == 2 && (++prog_lock % 4) == 0)
        connection->getIGSMainFrame()->updateGauge(playerList.GetCount(), false);

    // Eat line when the games are incoming from the table
    if (players_flag != 0)
        return true;
    // Cut leading "42"
    mod_str = toParse.Mid(3).Trim();
    return false;
}

bool IGSParser::parseFile(const wxString &toParse)
{
    // wxLogDebug("parseFile: %s", toParse.c_str());
    files_flag = !files_flag;
    // wxLogDebug("Files_flag now %s", (files_flag ? "On" : "Off"));

    // Eat line
    return true;
}

bool IGSParser::parseGameEnd(const wxString &toParse)
{
    // wxLogDebug("parseGameEnd: %s", toParse.c_str());

    int id=0;
    wxString result = wxEmptyString;

    if (gameIDEx.Matches(toParse))
    {
        size_t start, len;
        gameIDEx.GetMatch(&start, &len);
        wxString id_str = toParse.Mid(start+5, len-5);
        id = wxAtoi(id_str);

        wxRegEx resEx(IGSREGEX_GAMEEND_RESULT);
        wxASSERT(resEx.IsValid());
        if (resEx.Matches(toParse))
        {
            resEx.GetMatch(&start, &len);
            result = toParse.Mid(start+3, len-4);
        }
    }

    // Display territory from IGS status output when this was a real scored game
    if (statusArray.GetCount() != 0)
    {
        connection->parseStatus(wxEmptyString, id, statusArray);
        statusArray.Clear();
    }

    if (!connection->distributeGameEnd(id, result))
    {
        // Show line and cut leading "9" if no mainframe was found
        mod_str = toParse.Mid(2).Trim();
        return false;
    }

    // Eat line, result was displayed in the mainframe
    return true;
}

bool IGSParser::parseMatchStart(const wxString &toParse)
{
    // wxLogDebug("parseMatchStart: %s", toParse.c_str());

    move_header_id = 0;

    // 9 Creating match [64] with qgodev.          (Incoming match request)
    // 9 Match [243] with qgodev in 75 accepted.   (Outgoing match request)

    // Get the game ID and the opponents name. The time in the second form is ignored,
    // we get the time otherwise

    int id = 0;
    wxString opp_name = wxEmptyString;
    wxRegEx rex(IGSREGEX_MATCH_START);
    wxASSERT(rex.IsValid());
    if (rex.Matches(toParse))
    {
        size_t start, len;
        rex.GetMatch(&start, &len);
        wxString s = toParse.Mid(start, len);
        id = wxAtoi(s.Mid(7, len-s.Find(']')));
        opp_name = s.Mid(s.Find(' ', true) + 1);
    }
    else
    {
        // Should not happen, but who knows...
        LOG_IGS(wxString::Format(_T("Bad, match start could not be parsed in: %s"), toParse.c_str()));
        return false;
    }

    LOG_IGS(wxString::Format(_T("Match start: Game ID %d, Opponent: %s"), id, opp_name.c_str()));

    wxASSERT(id = tmpMove.id);
    if (id != tmpMove.id)
    {
        LOG_IGS(_T("Bad, match start game ID does not match. Aborting."));
        return false;
    }

    wxASSERT(!opp_name.Cmp(tmpMove.white) || !opp_name.Cmp(tmpMove.black));
    if (!(!opp_name.Cmp(tmpMove.white) || !opp_name.Cmp(tmpMove.black)))
    {
        LOG_IGS(_T("Bad, player names do not match. Aborting."));
        return false;
    }

    connection->startMatch(id, tmpMove.white, tmpMove.black, tmpMove.white_time, tmpMove.black_time);

    return false;
}

bool IGSParser::parseMatchEnd(const wxString &toParse)
{
    // wxLogDebug("parseMatchEnd: %s", toParse.c_str());

    wxString opp_name = wxEmptyString, result, sgf_result = wxEmptyString;
    int cut_off = 2;

    // 20 ggodev (W:O): 85.5 to qgodev (B:#): 84.0
    if (toParse.StartsWith("20 "))
    {
        // Turn this into: 20 ggodev: 85.5 to qgodev: 84.0
        result = toParse.Mid(3);
        result.Replace(" (W:O)", "");
        result.Replace(" (B:#)", "");
        cut_off = 3;
        // Get opponent name, unfortunately IGS does not tell us the game id
        wxString white = result.Left(result.Find(':'));
        wxRegEx blackEx(IGSREGEX_MATCH_END_BLACK);
        wxASSERT(blackEx.IsValid());
        wxString black = wxEmptyString;
        if (blackEx.Matches(result))
        {
            size_t start, len;
            blackEx.GetMatch(&start, &len);
            black = result.Mid(start + 4, len - 5);
        }
        else
            LOG_IGS(wxString::Format(_T("Failed to parse result: %s"), toParse.c_str()));
        opp_name = !white.Cmp(connection->getLoginName()) ? black : white;
    }
    else
    {
        // 9 qgodev has resigned the game
        if (toParse.Find(_T("has resigned the game")) != -1)
            opp_name = toParse(2, toParse.length() - 25);
        // 9 ggodev has run out of time.
        else if (toParse.Find(_T("has run out of time.")) != -1)
            opp_name = toParse(2, toParse.length() - 23);
        result = toParse.Mid(2);
        statusArray.Clear();
    }

    // Display territory from IGS status output when this was a real scored game
    if (statusArray.GetCount() != 0)
    {
        connection->parseStatus(opp_name, -1, statusArray);
        statusArray.Clear();
    }

    connection->endMatch(opp_name, -1, result);

    // Show line and cut leading "9" or "20"
    mod_str = toParse.Mid(cut_off).Trim();
    return false;
}

bool IGSParser::parsePlayerStats(const wxString &toParse)
{
    // wxLogDebug("parsePlayerStats: %s, flag = %d", toParse.c_str(), parse_player_stats_flag);

    if (!parse_player_stats_flag)
    {
        parse_player_stats_flag = true;

        // Forget previous results
        playerInfo.name = playerInfo.rating = playerInfo.rank = playerInfo.email = playerInfo.info =
            playerInfo.access = playerInfo.reg_date = playerInfo.defaults = wxEmptyString;
        playerInfo.rated_games = playerInfo.wins = playerInfo.losses = 0;
    }

    if (toParse.StartsWith("9 Player:"))
        playerInfo.name = toParse.Mid(9).Trim(false);
    else if (toParse.StartsWith("9 Rating:"))
        playerInfo.rating = toParse.Mid(9).Trim(false);
    else if (toParse.StartsWith("9 Rank:"))
        playerInfo.rank = toParse.Mid(7).Trim(false);
    else if (toParse.StartsWith("9 Rated Games:"))
        playerInfo.rated_games = wxAtoi(toParse.Mid(14).Trim(false));
    else if (toParse.StartsWith("9 Wins:"))
        playerInfo.wins = wxAtoi(toParse.Mid(7).Trim(false));
    else if (toParse.StartsWith("9 Losses:"))
        playerInfo.losses = wxAtoi(toParse.Mid(9).Trim(false));
    else if (toParse.StartsWith("9 Address:"))
        playerInfo.email = toParse.Mid(10).Trim(false);
    else if (toParse.StartsWith("9 Country:"))
        playerInfo.country = toParse.Mid(10).Trim(false);
    else if (toParse.StartsWith("9 Info:"))
        playerInfo.info = toParse.Mid(7).Trim(false);
    else if (toParse.StartsWith("9 Idle Time:"))
        playerInfo.access = toParse.Mid(12).Trim(false);
    else if (toParse.StartsWith("9 Last Access(GMT):"))
        playerInfo.access = toParse.Mid(19).Trim(false);
    else if (toParse.StartsWith("9 Reg date:"))
        playerInfo.reg_date = toParse.Mid(11).Trim(false);
    else if (toParse.StartsWith("9 Defaults (help defs):"))
        playerInfo.defaults = toParse.Mid(23).Trim(false);
    else
        return false;
    return true;
}

bool IGSParser::parseStored(const wxString &toParse)
{
    // wxLogDebug("parseStored: %s, flag = %d", toParse.c_str(), parse_player_stats_flag);

    // Not coming from GUI, user typed "stored xxx" in the terminal. So we display this.
    if (!parse_player_stats_flag || !stats_flag)
    {
        mod_str = toParse.Mid(3).Trim();
        return false;
    }

    // Incoming stored list triggered from GUI. Eat line and ignore for now.
    // TODO: Parse and display in playerinfo dialog
    if (!toParse.StartsWith("18 Found "))
        return true;

    // End of "stats" and "stored" command from GUI.
    parse_player_stats_flag = false;
    stats_flag = false;

    // Get number of stored games: 18 Found 1 stored games.
    playerInfo.stored = wxAtoi(toParse.Mid(9, toParse.find(" ", 9) - 9));

    // Open playerinfo dialog
    connection->getIGSMainFrame()->openPlayerinfoDialog(playerInfo);

    return true;
}

bool IGSParser::parseMatch(const wxString &toParse)
{
    // 9 Use <match qgodev B 19 10 20> or <decline qgodev> to respond.
    size_t start, len;
    int size = 19, main_time = 0, byo_time = 0;
    Color col = STONE_UNDEFINED;
    wxString opp_name = wxEmptyString, white, black;

    wxRegEx infoEx(IGSREGEX_MATCH);
    wxASSERT(infoEx.IsValid());
    if (infoEx.Matches(toParse))
    {
        infoEx.GetMatch(&start, &len);
        wxString info = toParse.Mid(start+7, len-4);
        // Info has "qgodev B 19 10 20" now

        // Opponent name
        int pos = info.find(' ');
        opp_name = info.Left(pos);

        // Color
        wxString s = info.Mid(++pos, 1);
        col = !s.Cmp("W") ? STONE_WHITE : STONE_BLACK;

        // Board size
        s = info.Mid((pos=info.find(' ', pos)+1), 2);
        size = wxAtoi(s);

        // Main time
        s = info.Mid((pos=info.find(' ', pos)+1), 2);
        main_time = wxAtoi(s);

        // Byoyomi time
        s = info.Mid((pos=info.find(' ', pos)+1), 2);
        byo_time = wxAtoi(s);

        wxLogDebug("Match: Opponent: %s, col: %d, size: %d, time: %d/%d", opp_name.c_str(), col, size, main_time, byo_time);

        if (col == STONE_BLACK)  // col is the color we play
        {
            white = opp_name;
            black = connection->getLoginName();
        }
        else
        {
            white = connection->getLoginName();
            black = opp_name;
        }

        connection->getIGSMainFrame()->openMatchDialog(new Match(white, black, opp_name, col, size, main_time, byo_time,
                                                                 MATCH_TYPE_INCOMING));
    }
    else
        LOG_IGS(wxString::Format("Failed to parse match request: %s", toParse.c_str()));

    // Show line and cut leading "9"
    mod_str = toParse.Mid(2).Trim();
    return false;
}

wxString IGSParser::translateResultToSGF(const wxString &result)
{
    wxString sgfMsg = wxEmptyString;
    int pos;
    // Resign?
    if ((pos = result.Find(" resigns.")) != -1)
    {
        wxString name = result.Mid(0, pos);
        if (!name.Cmp("Black"))
            sgfMsg = "W+R";
        else if (!name.Cmp("White"))
            sgfMsg = "B+R";
    }
    // Timeout?
    else if ((pos = result.Find(" forfeits on time.")) != -1)
    {
        wxString name = result.Mid(0, pos);
        if (!name.Cmp("Black"))
            sgfMsg = "W+T";
        else if (!name.Cmp("White"))
            sgfMsg = "B+T";
    }
    // Normal score
    else
    {
        // Parse W 85.0 B 84.5 and turn it into W+0.5
        if (result.Find(" lost by ") == -1) // Not a request result
        {
            double whiteScore, blackScore;
            pos = result.Find("B ");
            if (pos == -1 ||
                !result.Mid(1, result.find(' ', 3) - 1).Trim(false).ToDouble(&whiteScore) ||
                !result.Mid(pos + 2).Trim(false).ToDouble(&blackScore))
            {
                LOG_IGS(wxString::Format("Failed to parse result message: %s\n", result.c_str()));
                return wxEmptyString;
            }
            double diff = whiteScore - blackScore;
            if (diff < 0)
                diff *= -1;
            sgfMsg = wxString::Format("%s+%.1f", whiteScore > blackScore ? "W" : "B", diff);
        }
        else
        {
            // Try request game syntax: White lost by 0.5
            pos = result.Find("White lost by");
            int pos2 = result.Find("Black lost by");
            if (pos2 != -1)
                pos = pos2;
            else if (pos2 == -1 && pos == -1)
            {
                LOG_IGS(wxString::Format("Failed to parse result message: %s\n", result.c_str()));
                return wxEmptyString;
            }
            if (result.Find("by Resign") != -1)
                sgfMsg = pos2 != -1 ? "W+R" : "B+R";
            else if (result.Find("by Time") != -1)
                sgfMsg = pos2 != -1 ? "W+R" : "B+T";
            else
            {
                double diff;
                int pos3 = result.Find("by ");
                if (pos3 == -1 ||
                    !result.Mid(pos3 + 3).ToDouble(&diff))
                {
                    LOG_IGS(wxString::Format("Failed to parse result message: %s\n", result.c_str()));
                    return wxEmptyString;
                }
                sgfMsg = wxString::Format("%s+%.1f", pos2 != -1 ? "W" : "B", diff);
            }
        }
    }
    return sgfMsg;
}

bool IGSParser::parseScoreRemove(const wxString &toParse)
{
    // 49 Game 85 qgodev is removing @ Q16
    int id = wxAtoi(toParse.Mid(8, toParse.find(' ', 8) - 8));
    wxString pos = toParse.Mid(toParse.Find("@ ") + 2);
    short x, y;
    parseStringMove(pos, 19 /* TODO */, x, y);

    if (!connection->distributeScoreRemove(id, x, y, toParse.Mid(3)))
    {
        // Show line and cut leading "49" if no frame was found
        mod_str = toParse.Mid(3).Trim();
        return false;
    }
    return true;
}

bool IGSParser::parseStatus(const wxString &toParse)
{
    wxLogDebug("parseStatus: %s", toParse.c_str());
    statusArray.Add(toParse);
    return true;  // Eat line
}

bool IGSParser::parseAutoMessage(const wxString &toParse)
{
    // Send to automessage module, parsed there
    connection->getAutoUpdater()->doParse(toParse.Mid(3));

    // Eat  line
    return true;
}
