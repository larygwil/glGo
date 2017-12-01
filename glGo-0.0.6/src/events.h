/*
 * events.h
 *
 * $Id: events.h,v 1.9 2003/10/08 13:02:25 peter Exp $
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

#ifndef EVENTS_H
#define EVENTS_H

#ifdef __GNUG__
#pragma interface "events.h"
#endif

#include "defines.h"


class Position;


BEGIN_DECLARE_EVENT_TYPES()
    DECLARE_EVENT_TYPE(EVT_PLAY_MOVE, 5000)
    DECLARE_EVENT_TYPE(EVT_NAVIGATE,  5001)
    DECLARE_EVENT_TYPE(EVT_HANDICAP_SETUP, 5004)
    DECLARE_EVENT_TYPE(EVT_INTERFACE_UPDATE, 5005)
END_DECLARE_EVENT_TYPES()


// -----------------------------------------------------------------------------
//                           Class EventPlayMove
// -----------------------------------------------------------------------------

/**
 * An event which is called when a move was played.
 * It contains the x/y position and optional a color. If no color is given, the
 * receiver of this event has to take care of this itself.
 * @ingroup boardevents
 */
class EventPlayMove : public wxEvent
{
    DECLARE_DYNAMIC_CLASS(EventPlayMove)

public:
    /** Default constructor */
    EventPlayMove();

    /** Parameter constructor */
    EventPlayMove(short x, short y, Color color=STONE_UNDEFINED);

    /** Copy constructor */
    EventPlayMove(const EventPlayMove &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventPlayMove(*this); }

    /** Get move x position. */
    short getX() const { return x; }

    /** Get move y position. */
    short getY() const { return y; }

    /** Get move color. Can be STONE_UNDEFINED. */
    Color getColor() const { return color; }

    /** Set move color. */
    void setColor(Color c) { color = c; }

    /** True if the board needs to be updated. This is only used in the non-thread safe mechanism. */
    bool Ok() const { return ok; }

    /** Set this if the event was properly processed. */
    void setOk(bool b) { ok = b; }

protected:
    short x, y;
    Color color;
    bool ok;
};


// -----------------------------------------------------------------------------
//                           Class EventNavigate
// -----------------------------------------------------------------------------

/**
 * An event which is called when there is a call to navigate through the game.
 * It contains the direction of the navigation. Possible direction values can be
 * found in defines.h
 * @ingroup boardevents
 */
class EventNavigate : public wxEvent
{
    DECLARE_DYNAMIC_CLASS(EventNavigate)

public:
    /** Default constructor */
    EventNavigate();

    /** Parameter constructor */
    EventNavigate(unsigned short direction);

    /** Copy constructor */
    EventNavigate(const EventNavigate &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventNavigate(*this); }

    /** Gets the navigation direction. */
    unsigned short getDirection() const { return direction; }

    /** True if the board needs to be updated. This is only used in the non-thread safe mechanism. */
    bool Ok() const { return ok; }

    /** Set this if the event was properly processed. */
    void setOk(bool b) { ok = b; }

private:
    unsigned short direction;
    bool ok;
};


// -----------------------------------------------------------------------------
//                           Class EventHandicapSetup
// -----------------------------------------------------------------------------

/**
 * An event which is called when handicap is setup at the beginning of the game.
 * @todo: Make fixed and non-fixed handicap functions, and delete the array *there* !!!
 * found in defines.h
 * @ingroup boardevents
 */
class EventHandicapSetup : public wxEvent
{
    DECLARE_DYNAMIC_CLASS(EventHandicapSetup)

public:
    /** Default constructor */
    EventHandicapSetup();

    /** Parameter constructor */
    EventHandicapSetup(unsigned short handicap, Position **pos);

    /** Copy constructor */
    EventHandicapSetup(const EventHandicapSetup &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventHandicapSetup(*this); }

    unsigned short getHandicapNumber() const { return handicap; }

    Position** getHandicapPositions() const { return positions; }

    /** True if the board needs to be updated. This is only used in the non-thread safe mechanism. */
    bool Ok() const { return ok; }

    /** Set this if the event was properly processed. */
    void setOk(bool b) { ok = b; }

private:
    unsigned short handicap;
    Position **positions;
    bool ok;
};


// -----------------------------------------------------------------------------
//                           Class EventInterfaceUpdate
// -----------------------------------------------------------------------------

/**
 * An event sent to the MainFrame to call an update to the interface, if a move
 * or navigation event had happened.
 * @ingroup boardevents
 */
class EventInterfaceUpdate : public wxEvent
{
    DECLARE_DYNAMIC_CLASS(EventInterfaceUpdate)

public:
    /** Default constructor */
    EventInterfaceUpdate();

    /**
     * Parameter constructor
     * @param move_number Number of current move
     * @param brothers Number of brothers
     * @param sons Number of sons
     * @param toPlay Color of the player to play next
     * @param caps_white Number of white captures
     * @param caps_black Number of black captures
     * @param move_str String displaying the move, like "B Q16", "W Pass"
     * @param comment Comment associated with this move
     * @param force_clock_update If true, force update of both clock labels
     */
    EventInterfaceUpdate(unsigned short move_number,
                         unsigned short brothers,
                         unsigned short sons,
                         Color toPlay,
                         unsigned short caps_white,
                         unsigned short caps_black,
                         const wxString &move_str,
                         const wxString &comment,
                         bool force_clock_update);

    /** Copy constructor */
    EventInterfaceUpdate(const EventInterfaceUpdate &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventInterfaceUpdate(*this); }

    /** Gets the current move number. */
    unsigned short getMoveNumber() const { return move_number; }

    /** Gets the number of brothers. */
    unsigned short getBrothers() const { return brothers; }

    /** Gets the number of sons. */
    unsigned short getSons() const { return sons; }

    /** Gets the color of the player to play next. */
    Color getToPlay() const { return toPlay; }

    /** Gets the number of white captures */
    unsigned short getCapsWhite() const { return caps_white; }

    /** Gets the number of black captures */
    unsigned short getCapsBlack() const { return caps_black; }

    /** Gets the move string. See constructor description. */
    const wxString& getMoveStr() const { return move_str; }

    /** Gets the comment string. */
    const wxString& getComment() const { return comment; }

    /**
     * Do we need to update both clock labels?
     * Otherwise only the color of the last move is updated.
     * @return True if both clocks should be updated
     */
    bool getForceClockUpdate() const { return force_clock_update; }

private:
    unsigned short move_number, brothers, sons;
    Color toPlay;
    unsigned short caps_white, caps_black;
    wxString move_str, comment;
    bool force_clock_update;
};

#endif
