/*
 * igs_events.cpp
 *
 * $Id: igs_events.cpp,v 1.8 2003/10/14 15:47:49 peter Exp $
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
#pragma implementation "igs_events.h"
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

#include "igs_events.h"


// -----------------------------------------------------------------------------
//                           Class EventPlayIGSMove
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventPlayIGSMove, EventPlayMove)
DEFINE_EVENT_TYPE(EVT_PLAY_IGS_MOVE)

EventPlayIGSMove::EventPlayIGSMove()
    : EventPlayMove()
{
    game_id = move_number = white_time = black_time = 0;
    white_stones = black_stones = -1;
    is_new = true;
    SetId(ID_WINDOW_IGSMAINFRAME);
    SetEventType(EVT_PLAY_IGS_MOVE);
}

EventPlayIGSMove::EventPlayIGSMove(short x, short y, Color color, const Stones &captures, unsigned short game_id,
                                   unsigned short move_number,
                                   int white_time, int black_time,
                                   short white_stones, short black_stones,
                                   bool is_new)
    : EventPlayMove(x, y, color), captures(captures), game_id(game_id), move_number(move_number),
      white_time(white_time), black_time(black_time), white_stones(white_stones), black_stones(black_stones),
	  is_new(is_new)
{
    SetId(ID_WINDOW_IGSMAINFRAME);
    SetEventType(EVT_PLAY_IGS_MOVE);
}

EventPlayIGSMove::EventPlayIGSMove(const EventPlayIGSMove &evt)
    : EventPlayMove(evt)
{
    game_id = evt.game_id;
    white_time = evt.white_time;
    black_time = evt.black_time;
    white_stones = evt.white_stones;
    black_stones = evt.black_stones;
    move_number = evt.move_number;
    captures = evt.captures;
    is_new = evt.is_new;
    SetId(ID_WINDOW_IGSMAINFRAME);
    SetEventType(EVT_PLAY_IGS_MOVE);
}


// -----------------------------------------------------------------------------
//                           Class EventIGSComm
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventIGSComm, wxEvent)
DEFINE_EVENT_TYPE(EVT_IGS_COMM)

EventIGSComm::EventIGSComm()
{
    text = name = wxEmptyString;
    type = IGS_COMM_TYPE_UNDEFINED;
    id = 0;
    SetId(ID_WINDOW_IGSMAINFRAME);
    SetEventType(EVT_IGS_COMM);
}

EventIGSComm::EventIGSComm(const wxString &text, const wxString &name, IGSCommType type, int id)
    : text(text), name(name), type(type), id(id)
{
    SetId(ID_WINDOW_IGSMAINFRAME);
    SetEventType(EVT_IGS_COMM);
}

EventIGSComm::EventIGSComm(const EventIGSComm &evt)
{
    text = evt.text;
    name = evt.name;
    type = evt.type;
    id = evt.id;
    SetId(ID_WINDOW_IGSMAINFRAME);
    SetEventType(EVT_IGS_COMM);
}


// -----------------------------------------------------------------------------
//                           Class EventIGSCommand
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventIGSCommand, wxEvent)
DEFINE_EVENT_TYPE(EVT_IGS_COMMAND)

EventIGSCommand::EventIGSCommand()
{
    command = wxEmptyString;
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_IGS_COMMAND);
}

EventIGSCommand::EventIGSCommand(const wxString &command)
    : command(command)
{
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_IGS_COMMAND);
}

EventIGSCommand::EventIGSCommand(const EventIGSCommand &evt)
{
    command = evt.command;
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_IGS_COMMAND);
}
