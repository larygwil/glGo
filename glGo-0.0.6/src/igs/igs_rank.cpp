/*
 * igs_rank.cpp
 *
 * $Id: igs_rank.cpp,v 1.5 2003/10/07 21:13:28 peter Exp $
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
#pragma implementation "igs_rank.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#endif

#include "igs_rank.h"
#include "defines.h"

unsigned short IGSRank::convertRank(const wxString &s) const
{
    int pos = -1;

    // No length -> unknown rank
    if (s.length() == 0)
        return 54;

    // NR or NR*
    else if (s.StartsWith(_T("NR")))
        return 53;

    // ??? or ???*
    else if (s.StartsWith("???"))
        return 52;

    // Kyus
    else if ((pos = s.Find("k")) != -1)
        return 21 + wxAtoi(s.Mid(0, pos));

    // Dans
    else if ((pos = s.Find("d")) != -1)
        return 22 - wxAtoi(s.Mid(0, pos));

    // Pros
    else if ((pos = s.Find("p")) != -1)
        return 10 - wxAtoi(s.Mid(0, pos));

    // Shit happened
    else
    {
        LOG_IGS(wxString::Format(_T("Failed to parse rank: %s"), s.c_str()));
        return 54;
    }
}
