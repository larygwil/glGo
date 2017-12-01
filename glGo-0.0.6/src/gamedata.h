/*
 * gamedata.h
 *
 * $Id: gamedata.h,v 1.14 2003/10/02 14:15:28 peter Exp $
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

#ifndef GAMEDATA_H
#define GAMEDATA_H

#ifdef __GNUG__
#pragma interface "gamedata.h"
#endif

#include "defines.h"

extern const wxString INVALID_HANDICAP_VALUE;


/**
 * Class storing the information of one Game. An instance of this
 * class is associated in the Game class. This is a seperated class so
 * it can be used to pass information to and from the NewGameDialog.
 * All members are public for easy access.
 */
class GameData
{
public:

    /** Constructor. Initialize with default values. */
    GameData();

    /** Destructor */
    virtual ~GameData() {}

    /** Copy constructor */
    GameData(const GameData &data);

    unsigned short board_size; ///< Board size
    unsigned short handicap;   ///< Handicap
    float komi;                ///< Komi
    wxString whiteName;        ///< White player name
    wxString whiteRank;        ///< White player rank
    wxString blackName;        ///< Black player name
    wxString blackRank;        ///< Black player rank
    wxString result;           ///< Result string
    wxString gameName;         ///< Name of the game
    wxString copyright;        ///< Copyright
    wxString place;            ///< Place of the game
    wxString date;             ///< Date of the game
    wxString time;             ///< Time limit @todo Should be an integer?
    wxString filename;         ///< Filename of this game. Empty if not yet saved.
    short igs_type;            ///< IGS game type
};

#endif
