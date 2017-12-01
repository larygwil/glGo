/*
 * clock.cpp
 *
 * $Id: clock.cpp,v 1.4 2003/10/23 03:52:28 peter Exp $
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
#pragma implementation "clock.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/string.h>
#endif

#include "clock.h"

Clock::Clock()
{
    time = 10;
    stones = -1;
    byotime = NO_BYO;
    running = in_byo = false;
}

Clock::Clock(int time, short stones)
{
    this->time = time;
    this->stones = stones;
    byotime = NO_BYO;
    running = in_byo = false;
}

void Clock::setCurrentTime(int time, short stones)
{
    this->time = time;
    this->stones = stones;

    if (in_byo && stones == -1)
        in_byo = false;
}

int Clock::Tick(int seconds)
{
    // Check for absolute time -> byoyomi period transition
    if (!in_byo && time <= 0 && byotime != NO_BYO)
    {
        time = byotime;
        in_byo = true;
        if (stones == -1)
            stones = DEFAULT_BYO_STONES;  // TODO: Wont work for automatch
    }

    time -= seconds;

    return in_byo ? time : NO_BYO;
}

wxString Clock::format() const
{
    wxString str = wxString(time < 0 ? "-" : "") + formatTime(abs(time));
    if (stones != -1)
        str += wxString::Format(" (%d)", stones);
    return str;
}

wxString Clock::Format(int t, short s)
{
    wxString str = wxString(t < 0 ? "-" : "") + formatTime(abs(t));
    if (s != -1)
        str += wxString::Format(" (%d)", s);
    return str;
}

wxString Clock::formatTime(int t)
{
    wxString s = wxEmptyString;
    if (t >= 3600)
        s = formatTimePart(t / 3600) + ":";
    s += formatTimePart((t % 3600) / 60) + ":" + formatTimePart((t % 3600) % 60);
    return s;
}

wxString Clock::formatTimePart(int t)
{
    if (t < 10)
        return wxString::Format("0%d", t);
    return wxString::Format("%d", t);
}
