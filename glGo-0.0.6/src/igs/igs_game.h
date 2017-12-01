/*
 * igs_game.h
 *
 * $Id: igs_game.h,v 1.7 2003/10/10 16:28:32 peter Exp $
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

#ifndef IGS_GAME_H
#define IGS_GAME_H

#include <wx/dynarray.h>
#include "igs_rank.h"

/**
 * @file
 * @ingroup igs
 *
 * This file contains the IGSGame class and array sorting functions.
 * The IGSGame class and sorting functions are declared inline to improve
 * performance.
 */

 /** @addtogroup igs
 * @{
 */

 /** Various types of IGS games. */
 enum IGSGameType
 {
     IGS_GAME_TYPE_UNKNOWN,
     IGS_GAME_TYPE_RATED,
     IGS_GAME_TYPE_FREE,
     IGS_GAME_TYPE_TEACH
 };


// ------------------------------------------------------------------------
//                            IGSGame array
// ------------------------------------------------------------------------

class IGSGame;

/** The GamesList array declaration */
WX_DECLARE_OBJARRAY(IGSGame, GamesList);

/** Represents one game on IGS with the data read from "games" command. */
class IGSGame
{
public:
    /** Constructor */
    IGSGame(int id, const wxString &white_name, const wxString &white_rank,
            const wxString &black_name, const wxString &black_rank,
            int moves, int size, int handicap, int byo, int observers,
            float komi, IGSGameType type)
        : id(id), white_name(white_name), white_rank(white_rank),
        black_name(black_name), black_rank(black_rank), moves(moves),
        size(size), handicap(handicap), byo(byo), observers(observers),
        komi(komi), type(type)
        { }

    /** Copy constructor */
    IGSGame(const IGSGame &g)
        : id(g.id), white_name(g.white_name), white_rank(g.white_rank),
        black_name(g.black_name), black_rank(g.black_rank), moves(g.moves),
        size(g.size), handicap(g.handicap), byo(g.byo), observers(g.observers),
        komi(g.komi), type(g.type)
        { }

    int id;
    wxString white_name, white_rank, black_name, black_rank;
    int moves, size, handicap, byo, observers;
    float komi;
    IGSGameType type;
};


// ------------------------------------------------------------------------
//                       Sorting functions (inline)
// ------------------------------------------------------------------------

/** Sort ascending by ID */
inline int IGSGame_sort_ID_ascending(IGSGame **first, IGSGame **second)
{
    if ((*first)->id < (*second)->id)
        return -1;
    else if ((*first)->id > (*second)->id)
        return 1;
    return 0;
}

/** Sort descending by ID */
inline int IGSGame_sort_ID_descending(IGSGame **first, IGSGame **second)
{
    if ((*first)->id < (*second)->id)
        return 1;
    else if ((*first)->id > (*second)->id)
        return -1;
    return 0;
}

/** Sort ascending by white name */
inline int IGSGame_sort_WhiteName_ascending(IGSGame **first, IGSGame **second)
{
    return ((*first)->white_name).CmpNoCase((*second)->white_name);
}

/** Sort descending by white name */
inline int IGSGame_sort_WhiteName_descending(IGSGame **first, IGSGame **second)
{
    return ((*second)->white_name).CmpNoCase((*first)->white_name);
}

/** Sort ascending by white rank */
inline int IGSGame_sort_WhiteRank_ascending(IGSGame **first, IGSGame **second)
{
    return IGSRank((*first)->white_rank).Cmp(IGSRank((*second)->white_rank));
}
/** Sort descending by white rank */
inline int IGSGame_sort_WhiteRank_descending(IGSGame **first, IGSGame **second)
{
    return IGSRank((*second)->white_rank).Cmp(IGSRank((*first)->white_rank));
}

/** Sort ascending by black name */
inline int IGSGame_sort_BlackName_ascending(IGSGame **first, IGSGame **second)
{
    return ((*first)->black_name).CmpNoCase((*second)->black_name);
}

/** Sort descending by black name */
inline int IGSGame_sort_BlackName_descending(IGSGame **first, IGSGame **second)
{
    return ((*second)->black_name).CmpNoCase((*first)->black_name);
}

/** Sort ascending by black rank */
inline int IGSGame_sort_BlackRank_ascending(IGSGame **first, IGSGame **second)
{
    return IGSRank((*first)->black_rank).Cmp(IGSRank((*second)->black_rank));
}
/** Sort descending by black rank */
inline int IGSGame_sort_BlackRank_descending(IGSGame **first, IGSGame **second)
{
    return IGSRank((*second)->black_rank).Cmp(IGSRank((*first)->black_rank));
}

/** Sort ascending by moves */
inline int IGSGame_sort_Moves_ascending(IGSGame **first, IGSGame **second)
{
    if ((*first)->moves < (*second)->moves)
        return -1;
    else if ((*first)->moves > (*second)->moves)
        return 1;
    return 0;
}

/** Sort descending by moves */
inline int IGSGame_sort_Moves_descending(IGSGame **first, IGSGame **second)
{
    if ((*first)->moves < (*second)->moves)
        return 1;
    else if ((*first)->moves > (*second)->moves)
        return -1;
    return 0;
}

/** Sort ascending by size */
inline int IGSGame_sort_Size_ascending(IGSGame **first, IGSGame **second)
{
    if ((*first)->size < (*second)->size)
        return -1;
    else if ((*first)->size > (*second)->size)
        return 1;
    return 0;
}

/** Sort descending by size */
inline int IGSGame_sort_Size_descending(IGSGame **first, IGSGame **second)
{
    if ((*first)->size < (*second)->size)
        return 1;
    else if ((*first)->size > (*second)->size)
        return -1;
    return 0;
}

/** Sort ascending by handicap */
inline int IGSGame_sort_Handicap_ascending(IGSGame **first, IGSGame **second)
{
    if ((*first)->handicap < (*second)->handicap)
        return -1;
    else if ((*first)->handicap > (*second)->handicap)
        return 1;
    return 0;
}

/** Sort descending by handicap */
inline int IGSGame_sort_Handicap_descending(IGSGame **first, IGSGame **second)
{
    if ((*first)->handicap < (*second)->handicap)
        return 1;
    else if ((*first)->handicap > (*second)->handicap)
        return -1;
    return 0;
}

/** Sort ascending by komi */
inline int IGSGame_sort_Komi_ascending(IGSGame **first, IGSGame **second)
{
    if ((*first)->komi < (*second)->komi)
        return -1;
    else if ((*first)->komi > (*second)->komi)
        return 1;
    return 0;
}

/** Sort descending by komi */
inline int IGSGame_sort_Komi_descending(IGSGame **first, IGSGame **second)
{
    if ((*first)->komi < (*second)->komi)
        return 1;
    else if ((*first)->komi > (*second)->komi)
        return -1;
    return 0;
}

/** Sort ascending by byo */
inline int IGSGame_sort_Byo_ascending(IGSGame **first, IGSGame **second)
{
    if ((*first)->byo < (*second)->byo)
        return -1;
    else if ((*first)->byo > (*second)->byo)
        return 1;
    return 0;
}

/** Sort descending by byo */
inline int IGSGame_sort_Byo_descending(IGSGame **first, IGSGame **second)
{
    if ((*first)->byo < (*second)->byo)
        return 1;
    else if ((*first)->byo > (*second)->byo)
        return -1;
    return 0;
}

/** Sort ascending by observers */
inline int IGSGame_sort_Observers_ascending(IGSGame **first, IGSGame **second)
{
    if ((*first)->observers < (*second)->observers)
        return -1;
    else if ((*first)->observers > (*second)->observers)
        return 1;
    return 0;
}

/** Sort descending by observers */
inline int IGSGame_sort_Observers_descending(IGSGame **first, IGSGame **second)
{
    if ((*first)->observers < (*second)->observers)
        return 1;
    else if ((*first)->observers > (*second)->observers)
        return -1;
    return 0;
}

/** @} */  // End of group

#endif
