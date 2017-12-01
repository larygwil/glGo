/*
 * igs_eventhandler.h
 *
 * $Id: igs_eventhandler.h,v 1.9 2003/11/24 14:38:47 peter Exp $
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

#ifndef IGS_EVENTHANDLER_H
#define IGS_EVENTHANDLER_H

#ifdef __GNUG__
#pragma interface "igs_eventhandler.h"
#endif

#include "board_eventhandler.h"
#include "igs_events.h"

class IGSConnection;


//-------------------------------------------------------------------------
//                          Class IGSEventhandler
//-------------------------------------------------------------------------

/**
 * To be plugged into a board for IGS games.
 * This subclass of BoardEventhandler can be plugged into a Board
 * to manage incoming and - in own games - outgoing moves.
 * @ingroup igs
 */
class IGSEventhandler : public BoardEventhandler
{
public:
    IGSEventhandler() {}
    IGSEventhandler(Board *const board, GameType game_type=GAME_TYPE_IGS_OBSERVE);
    void OnPlayIGSMove(EventPlayIGSMove &event);
    void OnNavigate(EventNavigate &event);
    void OnPlayMove(EventPlayMove &event);

private:
    GameType game_type;

DECLARE_EVENT_TABLE()
};


//-------------------------------------------------------------------------
//                       Class IGSFrameEventhandler
//-------------------------------------------------------------------------

/**
 * To be plugged into the MainFrame which embeds an IGS board.
 * This handler will catch and manage close events to unobserve games.
 * @ingroup igs
 */
class IGSFrameEventhandler : public wxEvtHandler
{
public:
    IGSFrameEventhandler() : id(0), connection(NULL) {}
    IGSFrameEventhandler(int id, IGSConnection *con, GameType game_type=GAME_TYPE_IGS_OBSERVE)
        : id(id), connection(con), game_type(game_type) {}
    void OnClose(wxCloseEvent& event);
    void OnCommand(EventIGSCommand& event);
#if 0
    // TODO: We don't need this yet until auto-reobserve is implemented
    void unplug() { connection = NULL; }
    void replug(IGSConnection *c) { connection = c; }
#endif

private:
    int id;
    IGSConnection *connection;
    GameType game_type;

DECLARE_EVENT_TABLE()
};

#endif
