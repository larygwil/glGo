/*
 * marks.cpp
 *
 * $Id: marks.cpp,v 1.7 2003/10/31 22:02:30 peter Exp $
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
#pragma implementation "marks.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#endif

#include "marks.h"

Mark* Mark::createMark(unsigned short x, unsigned short y, MarkType t, const wxString &txt)
{
    switch (t)
    {
    case MARK_NONE:
        wxFAIL_MSG(_T("Cannot create a mark of type NONE"));
        return NULL;
    case MARK_CIRCLE:
        return new MarkCircle(x, y);
    case MARK_SQUARE:
        return new MarkSquare(x, y);
    case MARK_TRIANGLE:
        return new MarkTriangle(x, y);
    case MARK_CROSS:
        return new MarkCross(x, y);
    case MARK_TEXT:
        return new MarkText(x, y, txt);
    case MARK_TERR_WHITE:
        return new MarkTerrWhite(x, y);
    case MARK_TERR_BLACK:
        return new MarkTerrBlack(x, y);
    default:
        wxFAIL_MSG(wxString::Format(_T("Unknown mark type: %d"), t));
        return NULL;
    }
}
