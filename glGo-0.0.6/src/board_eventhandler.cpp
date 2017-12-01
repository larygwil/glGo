/*
 * board_eventhandler.cpp
 *
 * $Id: board_eventhandler.cpp,v 1.14 2003/11/06 04:35:02 peter Exp $
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
#pragma implementation "board_eventhandler.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include "board_eventhandler.h"
#include "board.h"
#include "boardhandler.h"
#include "utils/utils.h"


BEGIN_EVENT_TABLE(BoardEventhandler, wxEvtHandler)
    EVT_CUSTOM(EVT_PLAY_MOVE, ID_WINDOW_BOARD, BoardEventhandler::OnPlayMove)
    EVT_CUSTOM(EVT_NAVIGATE, ID_WINDOW_BOARD, BoardEventhandler::OnNavigate)
    EVT_CUSTOM(EVT_HANDICAP_SETUP, ID_WINDOW_BOARD, BoardEventhandler::OnHandicapSetup)
END_EVENT_TABLE()

BoardEventhandler::BoardEventhandler()
    : board(NULL)
{ }

BoardEventhandler::BoardEventhandler(Board *const board)
    : board(board)
{ }

void BoardEventhandler::OnPlayMove(EventPlayMove &event)
{
    /* wxLogDebug(wxString::Format(_T("BoardEventhandler::OnPlayMove %d/%d %d"),
       event.getX(), event.getY(), event.getColor())); */
    wxASSERT(board != NULL && board->getBoardHandler() != NULL);
    event.setOk(board->getBoardHandler()->playMove(event.getX(), event.getY()));

    // Notify mainframe about the event, so the interface can be updated
    if (event.Ok())
    {
        board->getBoardHandler()->processEditParams();
        board->updateMainframe();
    }
}

void BoardEventhandler::OnNavigate(EventNavigate &event)
{
    // wxLogDebug(wxString::Format(_T("BoardEventhandler::OnNavigate %d"), event.getDirection()));
    wxASSERT(board != NULL && board->getBoardHandler() != NULL);

    // No navigation in edit mode
    if (board->getEditMode() == EDIT_MODE_SCORE)
    {
        playSound(SOUND_BEEP);
        event.setOk(false);
        return;
    }

    switch (event.getDirection())
    {
    case NAVIGATE_DIRECTION_INVALID:
        wxLogDebug(_T("Invalid direction"));
        break;
    case NAVIGATE_DIRECTION_NEXT_MOVE:
        event.setOk(board->getBoardHandler()->nextMove());
        break;
    case NAVIGATE_DIRECTION_PREVIOUS_MOVE:
        event.setOk(board->getBoardHandler()->previousMove());
        break;
    case NAVIGATE_DIRECTION_FIRST_MOVE:
        event.setOk(board->getBoardHandler()->firstMove());
        break;
    case NAVIGATE_DIRECTION_LAST_MOVE:
        event.setOk(board->getBoardHandler()->lastMove());
        break;
    case NAVIGATE_DIRECTION_NEXT_VARIATION:
        event.setOk(board->getBoardHandler()->nextVariation());
        break;
    case NAVIGATE_DIRECTION_PREVIOUS_VARIATION:
        event.setOk(board->getBoardHandler()->previousVariation());
        break;
    default:
        wxLogDebug(_T("Unknown direction"));
    }

    // Notify mainframe about the event, so the interface can be updated
    if (event.Ok())
    {
        board->getBoardHandler()->processEditParams();
        board->updateMainframe();
    }
}

void BoardEventhandler::OnHandicapSetup(EventHandicapSetup &event)
{
    wxLogDebug(wxString::Format(_T("BoardEventhandler::OnHandicapSetup %d"), event.getHandicapNumber()));
    wxASSERT(board != NULL && board->getBoardHandler() != NULL);

    // TODO: Now check if positions != NULL and then go into fixed_handicap or non_fixed_handicap
    event.setOk(board->getBoardHandler()->setupHandicap(event.getHandicapNumber()));  // This should be the fixed one

    // Notify mainframe about the event, so the interface can be updated
    if (event.Ok())
        board->updateMainframe();
}
