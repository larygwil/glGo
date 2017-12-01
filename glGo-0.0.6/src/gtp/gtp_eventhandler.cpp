/*
 * gtp_eventhandler.cpp
 *
 * $Id: gtp_eventhandler.cpp,v 1.12 2003/10/19 04:49:20 peter Exp $
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
#pragma implementation "gtp_eventhandler.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/log.h>
#include <wx/intl.h>
#include <wx/event.h>
#endif

#include "gtp_eventhandler.h"
#include "gtp.h"
#include "board.h"
#include "boardhandler.h"
#include "utils/utils.h"


BEGIN_EVENT_TABLE(GTPEventhandler, BoardEventhandler)
    EVT_CUSTOM(EVT_PLAY_MOVE, ID_WINDOW_BOARD, GTPEventhandler::OnPlayMove)
    EVT_CUSTOM(EVT_PLAY_GTP_MOVE, ID_WINDOW_BOARD, GTPEventhandler::OnPlayGTPMove)
    EVT_CUSTOM(EVT_NAVIGATE, ID_WINDOW_BOARD, GTPEventhandler::OnNavigate)
END_EVENT_TABLE()


GTPEventhandler::GTPEventhandler(Board *const board, GTP *const gtp)
    : BoardEventhandler(board), gtp(gtp)
{
    wxLogDebug(_T("GTPEventhandler constructor"));
    wxASSERT(board != NULL && gtp != NULL);
}

void GTPEventhandler::OnPlayMove(EventPlayMove &event)
{
    wxLogDebug(wxString::Format(_T("GTPEventhandler::OnPlayMove %d/%d %d"),
                                event.getX(), event.getY(), event.getColor()));
    wxASSERT(gtp != NULL && board != NULL && board->getBoardHandler() != NULL);

    // If Ok flag is already set, this move is coming from GTPGame::confirmMove()
    if (event.Ok())
    {
        wxLogDebug(_T("Ok flag set, sending to Board"));

        // Superclass BoardEventhandler will send the move to BoardHandler
        BoardEventhandler::OnPlayMove(event);
        return;
    }

    wxLogDebug(_T("Ok flag not set, sending to GTP"));

    // If Ok flag is not set, this move is coming as reponse from a mouse click on the board
    if (!gtp->mayMove())
    {
        wxLogDebug(_T("Not your turn!"));
        playSound(SOUND_BEEP);
        event.setOk(false);
        return;
    }

    // Forward move to GTP engine for confirmation. We do not post it to the BoardHandler yet.
    // This will be done from GTPGame::confirmMove()
    // But we need to get the current turn color from BoardHandler
    event.setColor(board->getBoardHandler()->getCurrentTurnColor());
    gtp->AddPendingEvent(event);
}

void GTPEventhandler::OnPlayGTPMove(EventPlayGTPMove &event)
{
    wxLogDebug(wxString::Format(_T("GTPEventhandler::OnPlayGTPMove %d/%d %d"),
                                event.getX(), event.getY(), event.getColor()));
    wxASSERT(gtp != NULL && board != NULL && board->getBoardHandler() != NULL);

    bool res = board->getBoardHandler()->playMove(event.getX(), event.getY());

    // If false, then GNU Go had sent an invalid move!
    if (!res)
        board->displayMessageBox(
            wxString::Format(_("Looks GNU Go sent an invalid move!\n"
                               "Don't know what to do. Ignoring %s move at %d/%d."),
                             (event.getColor() == STONE_BLACK ? _("black") :
                              (event.getColor() == STONE_WHITE ? _("white") : _("unknown"))),
                             event.getX(), event.getY(),
                             _("Warning"), wxOK, wxICON_WARNING));
    event.setOk(res);

    // Notify mainframe about the event, so the interface can be updated
    if (res)
    {
        board->getBoardHandler()->processEditParams();
        board->updateMainframe();
    }
}

void GTPEventhandler::OnNavigate(EventNavigate &event)
{
    wxLogDebug(_T("GTPEventhandler::OnNavigate - Ignored. No navigation here"));

    // No navigation in GTP games
    event.setOk(false);
}
