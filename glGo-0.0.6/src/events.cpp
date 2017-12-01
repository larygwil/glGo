/*
 * events.cpp
 *
 * $Id: events.cpp,v 1.8 2003/10/08 13:02:25 peter Exp $
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
#pragma implementation "events.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/event.h"
#include "wx/log.h"
#endif

#include "events.h"
#include "stone.h"


// -----------------------------------------------------------------------------
//                           Class EventPlayMove
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventPlayMove, wxEvent)
DEFINE_EVENT_TYPE(EVT_PLAY_MOVE)

EventPlayMove::EventPlayMove()
{
    x = y = -1;
    color = STONE_UNDEFINED;
    ok = false;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_PLAY_MOVE);
}

EventPlayMove::EventPlayMove(short x, short y, Color color)
    : x(x), y(y), color(color)
{
    ok = false;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_PLAY_MOVE);
}

EventPlayMove::EventPlayMove(const EventPlayMove &evt)
{
    x = evt.x;
    y = evt.y;
    color = evt.color;
    ok = evt.ok;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_PLAY_MOVE);
}


// -----------------------------------------------------------------------------
//                           Class EventNavigate
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventNavigate, wxEvent)
DEFINE_EVENT_TYPE(EVT_NAVIGATE)

EventNavigate::EventNavigate()
{
    direction = NAVIGATE_DIRECTION_INVALID;
    ok = false;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_NAVIGATE);
}

EventNavigate::EventNavigate(unsigned short direction)
    : direction(direction)
{
    ok = false;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_NAVIGATE);
}

EventNavigate::EventNavigate(const EventNavigate &evt)
{
    direction = evt.direction;
    ok = evt.ok;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_NAVIGATE);
}


// -----------------------------------------------------------------------------
//                           Class EventHandicapSetup
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventHandicapSetup, wxEvent)
DEFINE_EVENT_TYPE(EVT_HANDICAP_SETUP)

EventHandicapSetup::EventHandicapSetup()
{
    handicap = 0;
    positions = NULL;
    ok = false;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_HANDICAP_SETUP);
}

EventHandicapSetup::EventHandicapSetup(unsigned short handicap, Position **pos)
    : handicap(handicap), positions(pos)
{
    ok = false;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_HANDICAP_SETUP);
}

EventHandicapSetup::EventHandicapSetup(const EventHandicapSetup &evt)
{
    handicap = evt.handicap;
    positions = evt.positions;
    ok = evt.ok;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_HANDICAP_SETUP);
}


// -----------------------------------------------------------------------------
//                           Class EventInterfaceUpdate
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventInterfaceUpdate, wxEvent)
DEFINE_EVENT_TYPE(EVT_INTERFACE_UPDATE)

EventInterfaceUpdate::EventInterfaceUpdate()
{
    move_number = brothers = sons = 0;
    toPlay = STONE_UNDEFINED;
    caps_white = caps_black = 0;
    move_str = comment = wxEmptyString;
    force_clock_update = false;
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_INTERFACE_UPDATE);
}

EventInterfaceUpdate::EventInterfaceUpdate(unsigned short move_number,
                                           unsigned short brothers,
                                           unsigned short sons,
                                           Color toPlay,
                                           unsigned short caps_white,
                                           unsigned short caps_black,
                                           const wxString &move_str,
                                           const wxString &comment,
                                           bool force_clock_update)
    : move_number(move_number), brothers(brothers), sons(sons), toPlay(toPlay),
      caps_white(caps_white), caps_black(caps_black), move_str(move_str),
      comment(comment), force_clock_update(force_clock_update)
{
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_INTERFACE_UPDATE);
}

EventInterfaceUpdate::EventInterfaceUpdate(const EventInterfaceUpdate &evt)
{
    move_number = evt.move_number;
    brothers = evt.brothers;
    sons = evt.sons;
    toPlay = evt.toPlay;
    caps_white = evt.caps_white;
    caps_black = evt.caps_black;
    move_str = evt.move_str;
    comment = evt.comment;
    force_clock_update = evt.force_clock_update;
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_INTERFACE_UPDATE);
}
