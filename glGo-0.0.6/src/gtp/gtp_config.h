/*
 * gtp_config.h
 *
 * $Id: gtp_config.h,v 1.8 2003/10/02 14:17:33 peter Exp $
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

#ifndef GTP_CONFIG_H
#define GTP_CONFIG_H

#ifdef __GNUG__
#pragma interface "gtp.h"
#endif

#include "gamedata.h"


/**
 * GTP player type.
 * This enum defines which player is played by human or computer.
 * @ingroup gtp */
enum GTPPlayer
{
    GTP_HUMAN,     ///< GTP player is human
    GTP_COMPUTER   ///< GTP player is computer
};


/**
 * Class storing the information of one GTP Game, inheriting from GameData.
 * This class is used to get the user input to setup a GTP game.
 * All members are public for easy access.
 * @ingroup gtp
 */
class GTPConfig : public GameData
{
public:
    /** Constructor */
    GTPConfig();

    /** Copy constructor */
    GTPConfig(const GTPConfig &c);

    /** Copy constructor */
    GTPConfig(const GameData &d);

    GTPPlayer white;          ///< White player, computer or human.
    GTPPlayer black;          ///< Black player, computer or human.
    int level;                ///< GNU Go level, 0-10
    wxString gtp_name;        ///< Name of connected GTP engine
    wxString gtp_version;     ///< Version string of connected GTP engine
    wxString gtp_path;        ///< Full path to GTP binary (must include binary)
    wxString resumeFileName;  ///< Filename of the game to resume. Empty if not resuming (default)
#if 0
    int mainTime;
    int byoYomiTime;
    int byoYomiStones;
    int timeSystem;
#endif
};

#endif
