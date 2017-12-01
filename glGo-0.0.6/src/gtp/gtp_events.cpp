/*
 * gtp_events.cpp
 *
 * $Id: gtp_events.cpp,v 1.2 2003/10/02 14:17:33 peter Exp $
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
#pragma implementation "gtp_events.h"
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

#include "gtp_scorer.h"
#include "gtp_events.h"


// -----------------------------------------------------------------------------
//                          Class EventPlayGTPMove
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventPlayGTPMove, EventPlayMove)
DEFINE_EVENT_TYPE(EVT_PLAY_GTP_MOVE)

EventPlayGTPMove::EventPlayGTPMove()
{
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_PLAY_GTP_MOVE);
}

EventPlayGTPMove::EventPlayGTPMove(int x, int y, Color color)
    : EventPlayMove(x, y, color)
{
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_PLAY_GTP_MOVE);
}

EventPlayGTPMove::EventPlayGTPMove(const EventPlayGTPMove &evt)
{
    x = evt.x;
    y = evt.y;
    color = evt.color;
    ok = false;
    SetId(ID_WINDOW_BOARD);
    SetEventType(EVT_PLAY_GTP_MOVE);
}


// -----------------------------------------------------------------------------
//                             Class EventGTPScore
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventGTPScore, wxEvent)
DEFINE_EVENT_TYPE(EVT_GTP_SCORE)

EventGTPScore::EventGTPScore()
{
    result = wxEmptyString;
    error_flag = false;
    scorer = NULL;
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_GTP_SCORE);
}

EventGTPScore::EventGTPScore(const wxString &result, bool error_flag, GTPScorer *scorer)
    : result(result), error_flag(error_flag), scorer(scorer)
{
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_GTP_SCORE);
}

EventGTPScore::EventGTPScore(const EventGTPScore &evt)
{
    result = evt.result;
    error_flag = evt.error_flag;
    scorer = evt.scorer;
    SetId(ID_WINDOW_MAINFRAME);
    SetEventType(EVT_GTP_SCORE);
}
