/*
 * stone.cpp
 *
 * $Id: stone.cpp,v 1.11 2003/11/24 14:38:41 peter Exp $
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
#pragma implementation "stone.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/log.h>
#endif

#include "stone.h"


// -----------------------------------------
//                Position
// -----------------------------------------

#ifdef __WXDEBUG__
int Position::counter = 0;
#endif

Position::Position()
    : x(0), y(0)
{
#ifdef __WXDEBUG__
    counter ++;
#endif
}

Position::Position(const Position& p)
    : x(p.x), y(p.y)
{
#ifdef __WXDEBUG__
    counter ++;
#endif
}

Position::Position(short x, short y)
    : x(x), y(y)
{
#ifdef __WXDEBUG__
    counter ++;
#endif
}

Position::~Position()
{
#ifdef __WXDEBUG__
    counter --;
#endif
}

Position& Position::operator=(const Position &p)
{
    // Avoid self-assignment
    if (this != &p)
    {
        x = p.x;
        y = p.y;
    }
    return *this;
}


// -----------------------------------------
//                Stone
// -----------------------------------------

Stone& Stone::operator=(const Stone &s)
{
    // Avoid self-assignment
    if (this != &s)
    {
        x = s.x;
        y = s.y;
        color = s.color;
    }
    return *this;
}
