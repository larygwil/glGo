/*
 * gtp_eventhandler.h
 *
 * $Id: gtp_eventhandler.h,v 1.6 2003/10/02 14:17:33 peter Exp $
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

#ifndef GTP_EVENTHANDLER_H
#define GTP_EVENTHANDLER_H

#ifdef __GNUG__
#pragma interface "gtp_eventhandler.h"
#endif

#include "board_eventhandler.h"
#include "gtp_events.h"

class GTP;

/**
 * A subclass of BoardEventhandler used for GTP games.
 * @ingroup gtp
 */
class GTPEventhandler : public BoardEventhandler
{
public:
    /** Default constructor */
    GTPEventhandler() : gtp(NULL) { }

    /** Parameter constructor */
    GTPEventhandler(Board *const board, GTP *const gtp);

    /** Callback for playing a human move, incoming from the Board */
    void OnPlayMove(EventPlayMove &event);

    /** Callback for a GTP move incoming from the GTP program */
    void OnPlayGTPMove(EventPlayGTPMove &event);

    /** Callback for navigation event. This will block as there is no navigation in GTP games. */
    void OnNavigate(EventNavigate &event);

private:
    GTP *const gtp;
DECLARE_EVENT_TABLE()
};

#endif
