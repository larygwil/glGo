/*
 * igs_events.h
 *
 * $Id: igs_events.h,v 1.11 2003/11/04 17:04:13 peter Exp $
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

#ifndef IGS_EVENTS_H
#define IGS_EVENTS_H

#ifdef __GNUG__
#pragma interface "igs_events.h"
#endif

#include "events.h"
#include "stones.h"


BEGIN_DECLARE_EVENT_TYPES()
    DECLARE_EVENT_TYPE(EVT_PLAY_IGS_MOVE, 5006)
    DECLARE_EVENT_TYPE(EVT_IGS_COMM, 5007)
    DECLARE_EVENT_TYPE(EVT_IGS_COMMAND, 5009)
END_DECLARE_EVENT_TYPES()


/** @addtogroup igs
 * @{ */

// -----------------------------------------------------------------------------
//                           Class EventPlayIGSMove
// -----------------------------------------------------------------------------

/** A move coming from IGS */
class EventPlayIGSMove : public EventPlayMove
{
    DECLARE_DYNAMIC_CLASS(EventPlayIGSMove)

public:
    /** Default constructor */
    EventPlayIGSMove();

    /** Parameter constructor */
    EventPlayIGSMove(short x, short y, Color color, const Stones &captures,
                     unsigned short game_id, unsigned short move_number=0,
                     int white_time=0, int black_time=0,
                     short white_stones=-1, short black_stones=-1,
                     bool is_new=true);

    /** Copy constructor */
    EventPlayIGSMove(const EventPlayIGSMove &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventPlayIGSMove(*this); }

    /** Get game ID. */
    unsigned short getGameID() const { return game_id; }

    /** Get move number. */
    unsigned short getMoveNumber() const { return move_number; }

    /** Get is_new flag. This is false if the move is part of a sequence sent by "moves". */
    bool isNew() const { return is_new; }

    /** Gets the list of captures */
    const Stones& getCaptures() const { return captures; }

    /** Gets the white time. This is either absolute or byo-yomi time. */
    int getWhiteTime() const { return white_time; }

    /** Gets the black time. This is either absolute or byo-yomi time. */
    int getBlackTime() const { return black_time; }

    /** Gets the number of white byo-yomi stones. This is -1 if we are still in absolute time. */
    short getWhiteStones() const { return white_stones; }

    /** Gets the number of black byo-yomi stones. This is -1 if we are still in absolute time. */
    short getBlackStones() const { return black_stones; }

protected:
    Stones captures;
    unsigned short game_id, move_number;
    int white_time, black_time;
    short white_stones, black_stones;
    bool is_new;
};


// -----------------------------------------------------------------------------
//                           Class EventIGSComm
// -----------------------------------------------------------------------------

/** Types for EventIGSComm */
enum IGSCommType
{
    IGS_COMM_TYPE_UNDEFINED,
    IGS_COMM_TYPE_TELL,
    IGS_COMM_TYPE_SHOUT,
    IGS_COMM_TYPE_KIBITZ,
    IGS_COMM_TYPE_CHATTER,
    IGS_COMM_TYPE_SAY
};

/** An IGS communication event used for shouts, tells, kibitz, chatter etc. */
class EventIGSComm : public wxEvent
{
    DECLARE_DYNAMIC_CLASS(EventIGSComm)

public:
    /** Default constructor */
    EventIGSComm();

    /** Parameter constructor */
    EventIGSComm(const wxString &text, const wxString &name, IGSCommType type, int id=0);

    /** Copy constructor */
    EventIGSComm(const EventIGSComm &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventIGSComm(*this); }

    /** Get game ID. */
    IGSCommType getType() const { return type; }

    const wxString& getText() const { return text; }
    const wxString& getName() const { return name; }
    int getID() const { return id; }

private:
    wxString text, name;
    IGSCommType type;
    int id;
};


// -----------------------------------------------------------------------------
//                           Class EventIGSCommand
// -----------------------------------------------------------------------------

/** A command sent from the client to IGS. */
class EventIGSCommand : public wxEvent
{
    DECLARE_DYNAMIC_CLASS(EventIGSCommand)

public:
    /** Default constructor */
    EventIGSCommand();

    /** Parameter constructor */
    EventIGSCommand(const wxString &command);

    /** Copy constructor */
    EventIGSCommand(const EventIGSCommand &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventIGSCommand(*this); }

    /** Get the command */
    const wxString& getCommand() const { return command; }

private:
    wxString command;
};

/** @} */  // End of group

#endif
