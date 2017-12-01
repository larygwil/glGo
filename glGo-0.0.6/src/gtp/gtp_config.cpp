/*
 * gtp_config.cpp
 *
 * $Id: gtp_config.cpp,v 1.8 2003/10/12 00:30:46 peter Exp $
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
#pragma implementation "gtp_config.h"
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

#include "gtp_config.h"


GTPConfig::GTPConfig()
    : GameData()
{
    black = GTP_HUMAN;
    white = GTP_COMPUTER;
    level = 5;
    gtp_name = wxEmptyString;
    gtp_version = wxEmptyString;
#ifdef __WXMSW__
    gtp_path = _T("gnugo.exe");
#else
    gtp_path = _T("gnugo");
#endif
    resumeFileName = wxEmptyString;
#if 0
    mainTime = DEFAULT_MAINTIME;
    byoYomiTime = DEFAULT_BYOYOMI_TIME;
    byoYomiStones = DEFAULT_BYOYOMI_STONES;
    timeSystem = DEFAULT_TIME_SYSTEM;
#endif
}

GTPConfig::GTPConfig(const GTPConfig &c)
    : GameData(static_cast<GameData>(c))
{
    black = c.black;
    white = c.white;
    level = c.level;
    gtp_name = c.gtp_name;
    gtp_version = c.gtp_version;
    gtp_path = c.gtp_path;
    resumeFileName = c.resumeFileName;
}

GTPConfig::GTPConfig(const GameData &d)
    : GameData(d)
{
    black = GTP_HUMAN;
    white = GTP_COMPUTER;
    level = 5;
    gtp_name = wxEmptyString;
    gtp_version = wxEmptyString;
    gtp_path = wxEmptyString;
    resumeFileName = wxEmptyString;
}
