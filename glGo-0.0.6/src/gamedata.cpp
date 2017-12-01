/*
 * gamedata.cpp
 *
 * $Id: gamedata.cpp,v 1.15 2003/10/12 00:30:50 peter Exp $
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
#pragma implementation "gamedata.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#include "wx/intl.h"
#endif

#include "gamedata.h"


extern const wxString INVALID_HANDICAP_VALUE = _("Invalid handicap value: %d\nPlease select a handicap between 2 and 9.");


GameData::GameData()
{
    board_size = DEFAULT_BOARD_SIZE;
    komi = DEFAULT_KOMI;
    handicap = DEFAULT_HANDICAP;
    whiteName = _("White");
    whiteRank = wxEmptyString;
    blackName = _("Black");
    blackRank = wxEmptyString;
    result = wxEmptyString;
    gameName = wxEmptyString;
    copyright = wxEmptyString;
    place = wxEmptyString;
    date = wxEmptyString;
    time = wxEmptyString;
    filename = wxEmptyString;
    igs_type = 0;
}

GameData::GameData(const GameData &data)
{
    board_size = data.board_size;
    komi = data.komi;
    handicap = data.handicap;
    whiteName = data.whiteName;
    whiteRank = data.whiteRank;
    blackName = data.blackName;
    blackRank = data.blackRank;
    result = data.result;
    gameName = data.gameName;
    copyright = data.copyright;
    place = data.place;
    date = data.date;
    time = data.time;
    filename = data.filename;
    igs_type = data.igs_type;
}
