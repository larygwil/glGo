/*
 * igs_regex.h
 *
 * $Id: igs_regex.h,v 1.24 2003/11/24 15:58:11 peter Exp $
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

/**
 * @file
 * @ingroup igs
 * This file contains all regular expressions used in the IGS parser.
 */

#ifndef IGS_REGEX_H
#define IGS_REGEX_H


//-------------------------------------------------------------------------
//                       The RegExEntries table
//-------------------------------------------------------------------------

#define IGSREGEX_UNUSED "^((2 )|(40 .*))"
#define IGSREGEX_STATE "^(1 [5-9])$"  // 1 5 - 1 9
#define IGSREGEX_ERROR_INFO "^(5|9) .+$"
#define IGSREGEX_AUTOMSG "^21 \\{.+$"
#define IGSREGEX_GAMEMOVEHEADER "^15 Game [0-9]+ I: [a-zA-Z0-9*]+ \\([0-9]+ [0-9]+ -?[0-9]+\\) vs " \
                                 "[a-zA-Z0-9*]+ \\([0-9]+ [0-9]+ -?[0-9]+\\)"
#define IGSREGEX_GAMEMOVE "^15[ ]+[0-9]+\\((W|B)\\):((( [A-Z][0-9]+)+)|( Handicap [2-9])|( Pass))"
#define IGSREGEX_GAMEINFO_START "^7 \\[##\\]  white.*$"
#define IGSREGEX_GAMEINFO "^7 \\[[0-9 ]+\\].*$"
#define IGSREGEX_USER_START "^42 Name .*$"
#define IGSREGEX_USER "^42 .*$"
#define IGSREGEX_SHOUT "^21 ![a-zA-Z0-9]+!: .+"
#define IGSREGEX_TELL "^24 \\*[a-zA-Z0-9]+\\*: .+"
#define IGSREGEX_KIBITZ "^11 .*$"
#define IGSREGEX_SAY "^19 .*$"
#define IGSREGEX_ADJOURNREQUEST "^48 Game [0-9]+ [a-zA-Z]+ requests an adjournment$"
#define IGSREGEX_ADJOURNDECLINE "^53 Game [0-9]+ adjournment is declined.$"
#define IGSREGEX_UNDO_OWN "^28 [a-zA-Z]+ undid the last move \\(.+\\) .$"
#define IGSREGEX_UNDO_OBSERVED "^28 Undo in game [0-9]+: [a-zA-Z]+ vs [a-zA-Z]+:[ ]*[A-Z][0-9]+$"
#define IGSREGEX_STORED "^18 .*$"
#define IGSREGEX_SCORE_REMOVE "^49 Game .*$"
#define IGSREGEX_SCORE_DONE "^20 .*$"
#define IGSREGEX_STATUS "^22 [0-9 ]+: .*$"
#define IGSREGEX_FILE "^[89] File$"
#define IGSREGEX_LOGINMSG "You have entered IGS at igs.joyjoy.net "

// Common expressions
#define IGSREGEX_LINE "^.*\r$"
#define IGSREGEX_LOGIN "^Login:"
#define IGSREGEX_GAMEID "Game [0-9]+"
#define IGSREGEX_GAMEEND "^9 \\{Game [0-9]+: .+vs.+:.*\\}$"

// parseGameMoveHeader
#define IGSREGEX_GAMEHEADER_WHITE "[a-zA-Z0-9*]+ \\("
#define IGSREGEX_GAMEHEADER_BLACK "vs [a-zA-Z0-9*]+ \\("
#define IGSREGEX_GAMEHEADER_WHITE_TIME "\\([0-9]+ [0-9]+ -?[0-9]+\\) vs"
#define IGSREGEX_GAMEHEADER_BLACK_TIME "\\([0-9]+ [0-9]+ -?[0-9]+\\)$"

// parseGameMove
#define IGSREGEX_GAMEMOVE_CAPS "[A-Z][0-9]+"

// parseShout
#define IGSREGEX_SHOUTNAME "![a-zA-Z0-9]+!"

// parseTell
#define IGSREGEX_TELLNAME "\\*[a-zA-Z0-9]+\\*"

// parseGameInfo
#define IGSREGEX_GAMEINFO_GAMEID     "\\[[0-9 ]+\\]"
#define IGSREGEX_GAMEINFO_WHITE_NAME "\\][ ]*[a-zA-Z0-9*]+ \\[.+vs"
#define IGSREGEX_GAMEINFO_WHITE_RANK "\\[[0-9a-zA-Z ]+[*]?\\] vs[.]"
#define IGSREGEX_GAMEINFO_BLACK_NAME "vs[.][ ]*[a-zA-Z0-9*]+ \\["
#define IGSREGEX_GAMEINFO_BLACK_RANK "\\[[0-9a-zA-Z ]+[*]?\\] \\("
#define IGSREGEX_GAMEINFO_DATA       "\\([ ]*[0-9]+[ ]+[0-9]+[ ]+[0-9][ ]+[0-9.]+[ ]+[0-9]+[ ]+[FTI]+\\) "
#define IGSREGEX_GAMEINFO_DATAFIELD  "[0-9.]+ "

// parseKibitz
#define IGSREGEX_KIBITZ_NAME "[a-zA-Z0-9]+ \\[[a-zA-Z0-9?* ]+\\]"
#define IGSREGEX_KIBITZ_ID "\\[[0-9]+\\]$"

// parseSay
#define IGSREGEX_SAY_NAME "\\*[a-zA-Z0-9]+\\*"

// parseGameEnd
#define IGSREGEX_GAMEEND_RESULT " : .+}"

// parseMatchStart
#define IGSREGEX_MATCH_START "[Mm]atch \\[[0-9]+\\] with [a-zA-Z0-9]+"

// parseMatchEnd
#define IGSREGEX_MATCH_END_BLACK " to [A-Za-z0-9]*:"

// parseUndo
#define IGSREGEX_UNDO_MOVE "\\(.+\\)"

// parseMatch
#define IGSREGEX_MATCH "<match .+> or"

#endif
