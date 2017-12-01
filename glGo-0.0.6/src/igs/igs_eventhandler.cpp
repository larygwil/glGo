/*
 * igs_eventhandler.cpp
 *
 * $Id: igs_eventhandler.cpp,v 1.19 2003/11/02 07:52:19 peter Exp $
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
#pragma implementation "igs_eventhandler.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#include "wx/intl.h"
#include "wx/event.h"
#include "wx/frame.h"
#endif

#include "igs_eventhandler.h"
#include "mainframe.h"
#include "board.h"
#include "boardhandler.h"
#include "igs_connection.h"
#include "utils/utils.h"


//-------------------------------------------------------------------------
//                          Class IGSEventhandler
//-------------------------------------------------------------------------

BEGIN_EVENT_TABLE(IGSEventhandler, BoardEventhandler)
    EVT_CUSTOM(EVT_PLAY_IGS_MOVE, ID_WINDOW_IGSMAINFRAME, IGSEventhandler::OnPlayIGSMove)
    EVT_CUSTOM(EVT_NAVIGATE, ID_WINDOW_BOARD, IGSEventhandler::OnNavigate)
    EVT_CUSTOM(EVT_PLAY_MOVE, ID_WINDOW_BOARD, IGSEventhandler::OnPlayMove)
END_EVENT_TABLE()


IGSEventhandler::IGSEventhandler(Board *const board, GameType game_type)
    : BoardEventhandler(board), game_type(game_type)
{
    wxASSERT(board != NULL &&
             (game_type == GAME_TYPE_IGS_OBSERVE ||
              game_type == GAME_TYPE_IGS_PLAY));
}

void IGSEventhandler::OnPlayIGSMove(EventPlayIGSMove &event)
{
#if 0
    wxLogDebug("IGSEventhandler::OnPlayIGSMove() %d/%d %d #%d",
               event.getX(), event.getY(), event.getColor(), event.getMoveNumber());
#endif

    wxASSERT(board != NULL);

    // Check for dummy move to unblock the board after a "moves" sequence ended
    if (event.getX() == -1 && event.getY() == -1 && event.getColor() == STONE_UNDEFINED)
    {
        board->block(false);
        wxLogDebug("Unblocking board");
        event.setOk(false);
        board->updateMainframe(true);
        board->getBoardHandler()->processEditParams(EDIT_PARAM_NUMBER_MOVES);
        return;  // Eat dummy move
    }

    // Check for score dummy move
    else if (event.getX() == -2 && event.getY() == -2 && event.getColor() == STONE_UNDEFINED)
    {
        wxLogDebug("Got score dummy");
        event.setOk(false);
        board->getBoardHandler()->score();
        return;  // Eat dummy move
    }

    bool res;
    // Handicap? x == -2 is the code for handicap setup, y the number of stones
    if (event.getX() == -2 && event.getColor() == STONE_BLACK)
        res = board->getBoardHandler()->setupHandicap(event.getY(), true);
    else
        // Normal move
        res = board->getBoardHandler()->playMoveIGS(event.getX(), event.getY(), event.getColor(),
                                                    event.getCaptures(), event.getMoveNumber(), !event.isNew());

    // If false, then IGS had sent an invalid move!
    if (!res)
    {
        wxLogDebug("Ignoring: move at %d/%d %d", event.getX(), event.getY(), event.getColor());
        event.setOk(false);
        return;
    }

    event.setOk(res);

    // Tell board not to update at the moment when a sequence of moves is incoming
    if (!event.isNew())
        board->block(true);

    // Update clock
    board->updateClock(event.getColor(),
                       event.getColor() == STONE_WHITE ? event.getWhiteTime() : event.getBlackTime(),
                       event.getColor() == STONE_WHITE ? event.getWhiteStones() : event.getBlackStones());

    // Notify mainframe about the event, so the interface can be updated
    if (res && event.isNew())
    {
        board->updateMainframe();
        board->getBoardHandler()->processEditParams(EDIT_PARAM_NUMBER_MOVES);
    }
}

void IGSEventhandler::OnPlayMove(EventPlayMove &event)
{
    if (game_type == GAME_TYPE_IGS_OBSERVE)
    {
        wxLogDebug(_T("IGSEventhandler::OnPlayMove - Ignored. No moves here"));
        // No moves in observed games
        event.setOk(false);
    }
    else
    {
        // Forward the move to IGS for confirmation. We do not post it to the BoardHandler yet.
        // This will be handled when IGS sends the move (or error) back.
        MainFrame *frame = board->getParentFrame();
        wxASSERT(frame != NULL);
        if (frame == NULL)
            return;

        EventIGSCommand evt = wxString::Format("%c%d %d -1",
                                               'a' + (event.getX() < 9 ? event.getX()-1 : event.getX()),
                                               board->getBoardSize() - event.getY() + 1,
                                               frame->getGameID());
        frame->GetEventHandler()->AddPendingEvent(evt);
        event.setOk(false);
    }
}

void IGSEventhandler::OnNavigate(EventNavigate &event)
{
    if (game_type == GAME_TYPE_IGS_PLAY)
    {
        wxLogDebug(_T("IGSEventhandler::OnNavigate - Ignored."));
        // No navigation in own played IGS games.
        event.setOk(false);
    }
    else
        // Navigation is ok in observed games
        BoardEventhandler::OnNavigate(event);
}


//-------------------------------------------------------------------------
//                       Class IGSFrameEventhandler
//-------------------------------------------------------------------------

BEGIN_EVENT_TABLE(IGSFrameEventhandler, wxEvtHandler)
    EVT_CLOSE(IGSFrameEventhandler::OnClose)
    EVT_MENU(wxID_CLOSE, IGSFrameEventhandler::OnClose)
    EVT_CUSTOM(EVT_IGS_COMMAND, ID_WINDOW_MAINFRAME, IGSFrameEventhandler::OnCommand)
END_EVENT_TABLE()


void IGSFrameEventhandler::OnClose(wxCloseEvent& event)
{
    // Own game? Block event.
    if (game_type == GAME_TYPE_IGS_PLAY && event.CanVeto())
    {
        playSound(SOUND_BEEP);
        event.Veto();
        return;
    }

    connection->unobserveGame(id, false);
    event.Skip();
}

void IGSFrameEventhandler::OnCommand(EventIGSCommand& event)
{
    connection->sendCommand(event.getCommand());
}
