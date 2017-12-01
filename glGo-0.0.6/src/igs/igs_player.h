/*
 * igs_player.h
 *
 * $Id: igs_player.h,v 1.11 2003/11/10 13:48:32 peter Exp $
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

#ifndef IGS_PLAYER_H
#define IGS_PLAYER_H

#include <wx/dynarray.h>
#include "igs_rank.h"

/**
 * @file
 * @ingroup igs
 *
 * This file contains the IGSPlayer class and array sorting functions.
 * The IGSPlayer class and sorting functions are declared inline to improve
 * performance.
 * Additionally this file has the PlayerInfo struct which is used for "stats"
 * command parsing and the playerinfo dialog.
 */

 /** @addtogroup igs
 * @{
 */


// ------------------------------------------------------------------------
//                           Player Status defines
// ------------------------------------------------------------------------

#define PLAYER_STATUS_NEUTRAL 0  ///< Status neutral (default)
#define PLAYER_STATUS_FRIEND  1  ///< Status friend
#define PLAYER_STATUS_BOZO    2  ///< Status bozo


// ------------------------------------------------------------------------
//                           IGSPlayer array
// ------------------------------------------------------------------------

class IGSPlayer;

/** The PlayerList array declaration */
WX_DECLARE_OBJARRAY(IGSPlayer, PlayerList);

/** Represents one player on IGS with the data read from "user" command. */
class IGSPlayer
{
public:
    /** Constructor */
    IGSPlayer(const wxString& name, const wxString &rank,
              const wxString& obs=wxEmptyString, const wxString& play=wxEmptyString,
              const wxString& win_loss=wxEmptyString, const wxString& idle=wxEmptyString,
              const wxString& flags=wxEmptyString,
              const wxString& info=wxEmptyString, const wxString& country=wxEmptyString)
        : name(name), rank(rank), obs(obs), play(play), win_loss(win_loss),
        idle(idle), flags(flags), info(info), country(country), status(PLAYER_STATUS_NEUTRAL)
        { }

    /** Copy constructor */
    IGSPlayer(const IGSPlayer &p)
        : name(p.name), rank(p.rank), obs(p.obs), play(p.play), win_loss(p.win_loss),
        idle(p.idle), flags(p.flags), info(p.info), country(p.country), status(p.status)
        { }

    wxString name, rank, obs, play, win_loss, idle, flags, info, country;
    int status;
};


// ------------------------------------------------------------------------
//                       Sorting functions (inline)
// ------------------------------------------------------------------------

/** Sort ascending by Name */
inline int IGSPlayer_sort_Name_ascending(IGSPlayer **first, IGSPlayer **second)
{
    return ((*first)->name).CmpNoCase((*second)->name);
}

/** Sort descending by Name */
inline int IGSPlayer_sort_Name_descending(IGSPlayer **first, IGSPlayer **second)
{
    return ((*second)->name).CmpNoCase((*first)->name);
}

/** Sort ascending by rank. This includes a second level sort by name. */
inline int IGSPlayer_sort_Rank_ascending(IGSPlayer **first, IGSPlayer **second)
{
    int res = IGSRank((*first)->rank).Cmp(IGSRank((*second)->rank));
    if (res != 0)
        return res;
    return IGSPlayer_sort_Name_ascending(first, second);
}

/** Sort descending by rank. This includes a second level sort by name. */
inline int IGSPlayer_sort_Rank_descending(IGSPlayer **first, IGSPlayer **second)
{
    int res = IGSRank((*second)->rank).Cmp(IGSRank((*first)->rank));
    if (res != 0)
        return res;
    return IGSPlayer_sort_Name_ascending(first, second);
}

/** Sort ascending by country */
inline int IGSPlayer_sort_Country_ascending(IGSPlayer **first, IGSPlayer **second)
{
    return ((*first)->country).CmpNoCase((*second)->country);
}

/** Sort descending by country */
inline int IGSPlayer_sort_Country_descending(IGSPlayer **first, IGSPlayer **second)
{
    return ((*second)->country).CmpNoCase((*first)->country);
}


// ------------------------------------------------------------------------
//                              PlayerInfo
// ------------------------------------------------------------------------

/** Struct containing all information coming from "stats" command. */
struct PlayerInfo
{
    wxString name, rating, rank, email, info, access, reg_date, defaults, country;
    int rated_games, wins, losses, stored;
};

/** @} */  // End of group

#endif
