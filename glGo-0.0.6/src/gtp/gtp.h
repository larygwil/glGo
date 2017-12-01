/*
 * gtp.h
 *
 * $Id: gtp.h,v 1.16 2003/10/19 04:47:12 peter Exp $
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

#ifndef GTP_H
#define GTP_H

#ifdef __GNUG__
#pragma interface "gtp.h"
#endif

#include "gtp_events.h"
#include "gtp_config.h"


/** GTP command flag. This indicated if a command is sent or received. */
enum GTPCommandType
{
    GTP_COMMAND_RECEIVE,
    GTP_COMMAND_SEND,
};

/** GTP IDs */
enum
{
    ID_GTP_PROCESS = 500,
    ID_PANEL,
    ID_TEXTINPUT,
    ID_TEXTOUTPUT,
    ID_CLOSE,
};


/**
 * @defgroup gtp GTP
 *
 * This module is responsible to connect to a GTP engine.
 *
 * @{
 */


// -----------------------------------------------------------------------------
//                                 Class GTP
// -----------------------------------------------------------------------------

class EventGTPCommand;
class GTPEventhandler;
class GTPHandler;
class GTPConsole;
class GTPGame;
class Board;
class GameData;
class MainFrame;


/**
 * Main entry point to start a GTP connection.
 * The framework should only call two functions within this module:
 * bool Connect() to connect to a GTP engine
 * void OpenConsole() to request a console frame
 */
class GTP : public wxEvtHandler
{
public:
    /** Constructor */
    GTP();

    /** Destructor */
    ~GTP();

    /**
     * Connect GTP engine.
     * This is the main entry method for the framework to call the GTP module.
     * @param data Pointer to a GameData object with the game information queries from the user
     * @param frame Pointer to MainFrame which embeds the used Board
     * @param board Pointer to the Board we want to attach the GTPEventHandler to
     * @param win Same as above, but already cast to wxWindow*. Yes, this is ugly.
     * @return True if GTP engine could be started, else false
     */
    bool Connect(GTPConfig *data, MainFrame *frame, Board *board, wxWindow *win);

    /**
     * Open a GTP console. This is the entry method for the framework to request
     * a console window. */
    void OpenConsole();

    /** Check if currently connected to a GTP engine. */
    bool isConnected() { return gtp_handler != NULL; }

    /**
     * Gets the name of the connected GTP engine or wxEmptyString if not connected.
     * @return Name, something like "GNU Go"
     */
    wxString getGTPName();

    /**
     * Gets the version string of the connected GTP engine or wxEmptyString if not connected.
     * @return Version, something like "3.4"
     */
    wxString getGTPVersion();

    /** Console closed. */
    void notifyConsoleClosed() { gtp_console = NULL; }

    /** Thread deleted itself. */
    void notifyThreadClosed() { gtp_handler = NULL; cleanup(); }

    /** Callback for GTP process termination event */
    void OnProcessTerm(wxCommandEvent& WXUNUSED(event));

    /** Main GTP callback function for sending and receeiving commands. */
    void OnGTPCommand(EventGTPCommand &event);

    /** Incoming move from human, forward it to the GTP engine. */
    void OnPlayHumanMove(EventPlayMove &event);

    /** Gets a pointer to the GTPEventhandler currently plugged into the Board. */
    GTPEventhandler* getGTPEventhandler() const { return gtp_eventhandler; }

    /** Gets a pointer to the Board used for this GTP session. */
    Board* getBoard() const { return board; }

    /**
     * Check if it is the turn of the given player color.
     * This function call is forwarded to GTPGame.
     * @param color Color to check. If STONE_UNDEFINED is given, the check uses the color of the current turn.
     * @return True if we may move, else false
     */
    bool mayMove(Color color=STONE_UNDEFINED);

private:
    /** Do some cleanups */
    void cleanup();

    /** Parse GTP engine output. */
    void parseGTPCommand(const wxString &toParse);

    /** Parse an Ok output from GTP engine. */
    void parseOk(const wxString &toParse);

    /** Parse an error output from GTP engine. */
    void parseError(const wxString &toParse);

    /** Parse a move from GTP engine. */
    void parseMove(const wxString &toParse);

    /** Parse handicap setup output from GTP engine. */
    void parseHandicap(const wxString &toParse);

    /** Parse which players turn it is after a sgf file was loaded to resume a game. */
    void parseTurn(const wxString &toParse);

    GTPHandler *gtp_handler;
    GTPConsole *gtp_console;
    GTPGame *gtp_game;
    GTPEventhandler *gtp_eventhandler;
    Board *board;
    wxWindow *window;
    MainFrame *mainframe;

DECLARE_EVENT_TABLE()
};


// -----------------------------------------------------------------------------
//                              The GTP events
// -----------------------------------------------------------------------------


BEGIN_DECLARE_EVENT_TYPES()
    DECLARE_EVENT_TYPE(EVT_GTP_COMMAND, 5002)
END_DECLARE_EVENT_TYPES()


// -----------------------------------------------------------------------------
//                           Class EventGTPCommand
// -----------------------------------------------------------------------------

/**
 * A command event contains a command string and a flag if it was sent or received.
 * The flag can be either GTP_COMMAND_SEND or GTP_COMMAND_RECEIVE.
 */
class EventGTPCommand : public wxEvent
{
    DECLARE_DYNAMIC_CLASS(EventGTPCommand)

public:
    /** Default constructor */
    EventGTPCommand();

    /** Parameter constructor */
    EventGTPCommand(const wxString &command, GTPCommandType type = GTP_COMMAND_RECEIVE);

    /** Copy constructor */
    EventGTPCommand(const EventGTPCommand &evt);

    /** Creates a new instance of this class. */
    wxEvent *Clone(void) const { return new EventGTPCommand(*this); }

    /** Gets the navigation direction. */
    const wxString& getCommand() const { return command; }

    GTPCommandType getType() const { return type; }

private:
    wxString command;
    GTPCommandType type;
};

/** @} */  // End of group

#endif
