/*
 * board_eventhandler.h
 *
 * $Id: board_eventhandler.h,v 1.10 2003/10/02 14:15:03 peter Exp $
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

#ifndef BOARD_EVENTHANDLER_H
#define BOARD_EVENTHANDLER_H

#ifdef __GNUG__
#pragma interface "board_eventhandler.h"
#endif

#include "events.h"


class Board;


/**
 * @defgroup boardevents Board event handling
 *
 * This module contains the events and eventhandler, attached to the board, which
 * are responsible for the communication between board, boardhandler and (later)
 * GTP and IGS threads.
 *
 * Certainly this could be done by a simple function call from Board to BoardHandler
 * and vice versa, but experience in the Java version of gGo has shown this is
 * going to create problems sooner or later. If the whole communication is only
 * done over events, this allows a clean and thread-safe interface. For each
 * special action performed on the board a subclass of wxEvent should be available.
 *
 * The goal is to subclass this and create various handlers for editing, GTP,
 * IGS play, IGS observe etc. board types.
 *
 * @{
 */

/**
 * A pluggable eventhandler which is responsible for the interaction from and
 * to the Board and BoardHandler.
 * This eventhandler has to be attached to a board window via PushEventHandler(),
 * which should be done when the board is created in the MainFrame constructor.
 */
class BoardEventhandler : public wxEvtHandler
{
public:
    /** Default constructor */
    BoardEventhandler();

    /** Paramter constructor */
    BoardEventhandler(Board *const board);

    /** A move was played. Forward this to the associated boardhandler. */
    void OnPlayMove(EventPlayMove &event);

    /** A game navigation was called. Forward this to the associated boardhandler. */
    void OnNavigate(EventNavigate &event);

    /** Handicap is setup at the beginning of the game.
     *  @todo Split into fixed and non-fixed setup */
    void OnHandicapSetup(EventHandicapSetup &event);

protected:
    Board *const board;

private:

DECLARE_EVENT_TABLE()
};

/** @} */  // End of group

#endif
