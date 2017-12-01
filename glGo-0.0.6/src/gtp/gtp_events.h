/*
 * gtp_events.h
 *
 * $Id: gtp_events.h,v 1.2 2003/10/02 14:17:33 peter Exp $
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

#ifndef GTP_EVENTS_H
#define GTP_EVENTS_H

#ifdef __GNUG__
#pragma interface "gtp_events.h"
#endif

#include "events.h"


BEGIN_DECLARE_EVENT_TYPES()
    DECLARE_EVENT_TYPE(EVT_PLAY_GTP_MOVE, 5003)
    DECLARE_EVENT_TYPE(EVT_GTP_SCORE,     5008)
END_DECLARE_EVENT_TYPES()


// -----------------------------------------------------------------------------
//                          Class EventPlayGTPMove
// -----------------------------------------------------------------------------

/**
 * An event which is called when a move was played from the GTP engine.
 * It contains the x/y position and color.
 * @ingroup gtp
 */
class EventPlayGTPMove : public EventPlayMove
{
    DECLARE_DYNAMIC_CLASS(EventPlayGTPMove)

public:
    /** Default constructor */
    EventPlayGTPMove();

    /** Parameter constructor */
    EventPlayGTPMove(int x, int y, Color color);

    /** Copy constructor */
    EventPlayGTPMove(const EventPlayGTPMove &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventPlayGTPMove(*this); }
};


// -----------------------------------------------------------------------------
//                             Class EventGTPScore
// -----------------------------------------------------------------------------

class GTPScorer;

/**
 * An event containing the score estimation from the GTP engine. This event is created
 * be GTPScorer after running a score estimation using a GTP program and will be sent
 * to the MainFrame from which the scoring was called.
 * @ingroup gtp
 */
class EventGTPScore : public wxEvent
{
    DECLARE_DYNAMIC_CLASS(EventGTPScore)

public:
    /** Default constructor */
    EventGTPScore();

    /** Parameter constructor */
    EventGTPScore(const wxString &result, bool error_flag=false, GTPScorer *scorer=NULL);

    /** Copy constructor */
    EventGTPScore(const EventGTPScore &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventGTPScore(*this); }

    const wxString& getResult() const { return result; }
    bool getErrorFlag() const { return error_flag; }
    GTPScorer* getScorer() { return scorer; }

private:
    wxString result;
    bool error_flag;
    GTPScorer *scorer;
};

#endif
