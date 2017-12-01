/*
 * gtp.cpp
 *
 * $Id: gtp.cpp,v 1.24 2003/10/19 04:46:53 peter Exp $
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
#pragma implementation "gtp.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/process.h>
#include "gtp.h"
#include "gtp_handler.h"
#include "gtp_console.h"
#include "gtp_eventhandler.h"
#include "gtp_game.h"
#include "board.h"
#include "mainframe.h"
#include "stone.h"
#include "utils/utils.h"


// Default command to connect to GNU Go in GTP mode
const wxString GNUGO_ARGS = _T(" --mode=gtp");


// -----------------------------------------------------------------------------
//                                 Class GTP
// -----------------------------------------------------------------------------

BEGIN_EVENT_TABLE(GTP, wxEvtHandler)
    EVT_END_PROCESS(ID_GTP_PROCESS, GTP::OnProcessTerm)
    EVT_CUSTOM(EVT_GTP_COMMAND, wxID_ANY, GTP::OnGTPCommand)
    EVT_CUSTOM(EVT_PLAY_MOVE, wxID_ANY, GTP::OnPlayHumanMove)
END_EVENT_TABLE()


GTP::GTP()
{
    gtp_handler = NULL;
    gtp_console = NULL;
    gtp_game = NULL;
    gtp_eventhandler = NULL;
    mainframe = NULL;
    window = NULL;
}

GTP::~GTP()
{
    if (gtp_console != NULL)
        gtp_console->Destroy();

    // Unsure if this is ok
    if (gtp_eventhandler != NULL && window != NULL)
        window->PopEventHandler(true);

    if (gtp_handler != NULL)
        // GTPHandler will delete itself
        gtp_handler->KillProcess(true);

    if (gtp_game != NULL)
        delete gtp_game;

    window = NULL;
    mainframe = NULL;
}

bool GTP::Connect(GTPConfig *data, MainFrame *frame, Board *board, wxWindow *win)
{
    // Not yet connected?
    if (gtp_handler == NULL)
    {
        // Create new process as child of this class, so this receives an event when
        // the process terminates.
        wxProcess *proc = new wxProcess(this, ID_GTP_PROCESS);

        // Tell process we want stdin, stderr and stdout
        proc->Redirect();

        // Try to start process and execute gnugo command in GTP mode
        wxString cmd = data->gtp_path + GNUGO_ARGS;
        wxLogDebug(_T("Trying to execute: %s"), cmd.c_str());
        long pid = 0;
        {
            wxLogNull logNo;  // Avoid messagebox, we handle error ourselves
            pid = wxExecute(cmd, wxEXEC_ASYNC, proc);
        }

        // Failed to create process. Most likely reason is, the command was not found.
        wxLogDebug(_T("PID = %d"), static_cast<int>(pid));
        if (pid == -1)
        {
            wxLogDebug(_T("Failed to start process: %s"),
                       wxString(data->gtp_path + GNUGO_ARGS).c_str());
            delete proc;
            return false;
        }

        // Create new GTPHandler thread
        gtp_handler = new GTPHandler(proc, pid, this);
        if (!(gtp_handler->Create() == wxTHREAD_NO_ERROR &&
              gtp_handler->Run() == wxTHREAD_NO_ERROR))
        {
            // Creation of thread failed, cleanup
            wxMessageBox(_("A serious problem happened, I failed to create the GNU Go thread.\nAborting..."),
                         _("GTP Error"), wxOK | wxICON_ERROR);
            gtp_handler->Delete();
            gtp_handler = NULL;
            proc->Detach();
            proc = NULL;
            wxProcess::Kill(pid, wxSIGKILL);
            return false;
        }
    }
    else
        wxLogDebug(_T("Already connected."));

    // Allright, process and thread up and running
    wxLogDebug(_T("Process and thread running"));

    // Create new GTPGame object
    if (gtp_game != NULL)
        delete gtp_game;
    gtp_game = new GTPGame(this, data);

    // Push a new handler to the Board window for the move communication
    wxASSERT(board != NULL && frame != NULL && win != NULL);
    if (gtp_eventhandler != NULL)
        // Remove and delete old GTPEventhandler if we reuse the process
        win->PopEventHandler(true);
    gtp_eventhandler = new GTPEventhandler(board, this);
    win->PushEventHandler(gtp_eventhandler);
    // Remember pointer to the frame and board so we can unplug the eventhandler again
    window = win;
    mainframe = frame;
    this->board = board;

    // Create console, but keep it hidden
    gtp_console = new GTPConsole(this, _("GTP"));

    // Start the game
    gtp_game->initGame();

    return true;
}

void GTP::OpenConsole()
{
    if (gtp_console != NULL)
    {
        if (!gtp_console->IsShown())
            gtp_console->Show();
        gtp_console->Raise();
    }
    else
    {
        gtp_console = new GTPConsole(this, _("GTP"));
        gtp_console->Show();
    }
}

wxString GTP::getGTPName()
{
    if (gtp_handler == NULL || gtp_game == NULL)
        return wxEmptyString;
    return gtp_game->getGTPConfig()->gtp_name;
}

wxString GTP::getGTPVersion()
{
    if (gtp_handler == NULL || gtp_game == NULL)
        return wxEmptyString;
    return gtp_game->getGTPConfig()->gtp_version;
}

bool GTP::mayMove(Color color)
{
    return gtp_game->mayMove(color);
}

void GTP::cleanup()
{
    wxLogDebug(_T("GTP::cleanup()"));

    // Hard debug way
    wxASSERT(window != NULL && gtp_eventhandler!= NULL);

    // Smooth release way
    if (window == NULL || gtp_eventhandler == NULL)
        return;

    // Unplug GTPEventhandler from current board
    window->PopEventHandler(true);
    gtp_eventhandler = NULL;
    window = NULL;

    // Tell MainFrame GTP session ended
    mainframe->setGameType(GAME_TYPE_PLAY);
    mainframe = NULL;
}

void GTP::OnGTPCommand(EventGTPCommand &event)
{
    switch (event.getType())
    {
    case GTP_COMMAND_SEND:
        // Tell gtp_handler thread to forward this command to the GTP process
        if (gtp_handler != NULL)
            gtp_handler->sendCommand(event.getCommand());

        // Send to console to display
        if (gtp_console != NULL)
            wxPostEvent(gtp_console, event);
        break;
    case GTP_COMMAND_RECEIVE:
        // Send to console to display
        if (gtp_console != NULL)
            wxPostEvent(gtp_console, event);

        // Parse command
        parseGTPCommand(event.getCommand());
        break;
    default:
        wxFAIL_MSG(_T("Unknown event type"));
    }
}

void GTP::OnPlayHumanMove(EventPlayMove &event)
{
    wxLogDebug(_T("GTP::OnPlayHumanMove()"));
    gtp_game->receiveMoveFromHuman(event.getX(), event.getY(), event.getColor());
}

void GTP::OnProcessTerm(wxCommandEvent& WXUNUSED(event))
{
    wxLogDebug(_T("GTP::OnProcessTerm()"));

    // Stop GTPHandler thread
    if (gtp_handler != NULL && gtp_handler->IsAlive() && !gtp_handler->isQuitting())
        gtp_handler->Delete();
    gtp_handler = NULL;

    // Close console
    if (gtp_console != NULL)
    {
        gtp_console->Destroy();
        gtp_console = NULL;
    }

    cleanup();
}

void GTP::parseGTPCommand(const wxString &toParse)
{
    wxLogDebug(_T("parseGTPCommand: %s"), toParse.c_str());

    int l = toParse.length();
    if (l > 0)
    {
        if (toParse.StartsWith("?"))
            parseError(toParse.Mid(l == 1 ? 1 : 2));
        else if (toParse.StartsWith("="))
            parseOk(toParse.Mid(l == 1 ? 1 : 2));
    }
}

void GTP::parseOk(const wxString &toParse)
{
#ifdef __WXDEBUG__
    if (!toParse.empty())
        wxLogDebug("Ok: %s", toParse.c_str());
#endif

    switch (gtp_game->getState())
    {
    case GTP_STATE_UNKNOWN:
        // wxLogDebug("*** Parsing in GTP_STATE_UNKNOWN ***");
        wxFAIL_MSG(_T("*** Parsing in GTP_STATE_UNKNOWN ***"));  // TODO
        break;
    case GTP_STATE_SETUP_GAME_NAME:
        if (gtp_game->getGTPConfig()->white == GTP_COMPUTER)
            gtp_game->getGTPConfig()->whiteName = toParse;
        if (gtp_game->getGTPConfig()->black == GTP_COMPUTER)
            gtp_game->getGTPConfig()->blackName = toParse;
        gtp_game->getGTPConfig()->gtp_name = toParse;
        gtp_game->setState(GTP_STATE_SETUP_GAME_VERSION);
        break;
    case GTP_STATE_SETUP_GAME_VERSION:
#if 0
        // Bit too crowded in the sidebar with the version
        if (gtp_game->getGTPConfig()->white == GTP_COMPUTER)
            gtp_game->getGTPConfig()->whiteName.Append(" " + toParse);
        if (gtp_game->getGTPConfig()->black == GTP_COMPUTER)
            gtp_game->getGTPConfig()->blackName.Append(" " + toParse);
#endif
        gtp_game->getGTPConfig()->gtp_version = toParse;
        gtp_game->setState(GTP_STATE_SETUP_GAME_BOARDSIZE);
        // Update the MainFrame sidebar
        wxASSERT(mainframe != NULL);
        mainframe->updateSidebar();
        break;
    case GTP_STATE_SETUP_GAME_BOARDSIZE:
        gtp_game->setState(GTP_STATE_SETUP_GAME_KOMI);
        break;
    case GTP_STATE_SETUP_GAME_KOMI:
        gtp_game->setState(GTP_STATE_SETUP_GAME_LEVEL);
        break;
    case GTP_STATE_SETUP_GAME_LEVEL:
        if (gtp_game->getGTPConfig()->handicap > 0)
            gtp_game->setState(GTP_STATE_SETUP_GAME_HANDICAP);
        else
        {
            gtp_game->setState(GTP_STATE_MOVE_BLACK);
            gtp_game->startGame();
        }
        break;
    case GTP_STATE_SETUP_GAME_HANDICAP:
        parseHandicap(toParse);
        break;
    case GTP_STATE_MOVE_WHITE:
    case GTP_STATE_MOVE_BLACK:
        if (!toParse.empty())
            parseMove(toParse);
        break;
    case GTP_STATE_CONFIRMING_MOVE:
#ifdef __WXDEBUG__
        if (!toParse.empty())
            wxLogDebug(_T("*** Something went wrong, was expecting = only after a move. ***"));
#endif
        gtp_game->confirmMove(true);
        break;
    case GTP_STATE_SCORING:
        // GTPConnection.getBoard().showScore(toParse);
        // TODO
        break;
    case GTP_STATE_DONE:
        break;
    case GTP_STATE_RESUME_GAME:
        parseTurn(toParse);
    }
}

void GTP::parseError(const wxString &toParse)
{
    wxLogDebug(_T("Error: %s"), toParse.c_str());

    // Illegal move
    if (!toParse.CmpNoCase(_T("illegal move")))
        gtp_game->confirmMove(false);
    // Other errors can be parsed here
    else
        wxLogDebug(_T("GTP Error: %s"), toParse.c_str());
}

void GTP::parseMove(const wxString &toParse)
{
    short x = 0;
    short y = 0;

    // Pass?
    if (!toParse.CmpNoCase(_T("pass")))
        x = y = -1;
    else
    {
        if (!parseStringMove(toParse, gtp_game->getGTPConfig()->board_size, x, y))
            return;
        wxLogDebug(_T("Parsed move: %d/%d"), x, y);
    }

    gtp_game->receiveMoveFromGTP(x, y);
}


void GTP::parseHandicap(const wxString &toParse)
{
    const unsigned int board_size = gtp_game->getGTPConfig()->board_size;
    Position **handicap = new Position*[gtp_game->getGTPConfig()->handicap];

    size_t pos = 0;
    int newpos = 0;
    unsigned short counter = 0;
    wxString sub;
    while (newpos != -1)
    {
        sub = toParse.Mid(pos);
        newpos = sub.Find(" ");
        sub = sub.Mid(0, (newpos < static_cast<int>(toParse.length()) && newpos > 0 ?
                          newpos : toParse.length()));

        if (sub.empty())
            break;

        short x = 0;
        short y = 0;

        if (!parseStringMove(sub, board_size, x, y))
            return;

        wxLogDebug(_T("Parsed handicap position: %d/%d"), x, y);

        Position *p = new Position(x, y);
        handicap[counter++] = p;
        pos += newpos + 1;

        // Security against endless loop
        if (counter > static_cast<unsigned short>(gtp_game->getGTPConfig()->handicap))
        {
            wxFAIL_MSG(_T("Endless loop in parseHandicap prevented !"));
            break;
        }
    }

    gtp_game->recieveHandicapFromGTP(handicap);
}


void GTP::parseTurn(const wxString &toParse)
{
    if (!toParse.CmpNoCase(_T("black")))
        gtp_game->startGame(STONE_BLACK);
    else if (!toParse.CmpNoCase(_T("white")))
        gtp_game->startGame(STONE_WHITE);
    else
        wxLogDebug(_T("Don't know whose turn it is! Bailing out..."));
}


// -----------------------------------------------------------------------------
//                           Class EventGTPCommand
// -----------------------------------------------------------------------------

IMPLEMENT_DYNAMIC_CLASS(EventGTPCommand, wxEvent)
DEFINE_EVENT_TYPE(EVT_GTP_COMMAND)

EventGTPCommand::EventGTPCommand()
{
    command = wxEmptyString;
    type = GTP_COMMAND_RECEIVE;
    SetEventType(EVT_GTP_COMMAND);
}

EventGTPCommand::EventGTPCommand(const wxString &command, GTPCommandType type)
    : command(command), type(type)
{
    SetEventType(EVT_GTP_COMMAND);
}

EventGTPCommand::EventGTPCommand(const EventGTPCommand &evt)
{
    command = evt.command;
    type = evt.type;
    SetEventType(EVT_GTP_COMMAND);
}
