/*
 * igs_connection.cpp
 *
 * $Id: igs_connection.cpp,v 1.36 2003/11/24 15:58:10 peter Exp $
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
#pragma implementation "igs_connection.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/wx.h>
#endif

#include <wx/config.h>
#include <wx/filename.h>
#include "igs_connection.h"
#include "igs_parser.h"
#include "igs_mainframe.h"
#include "igs_eventhandler.h"
#include "autoupdater.h"
#include "mainframe.h"
#include "board.h"
#include "boardhandler.h"
#include "matrix.h"
#include "glGo.h"
#include "utils/utils.h"


/** IDs for socket and timer used for the server connection */
enum
{
    SOCKET_ID = 1000,
    SEND_TIMER_ID,
    AYT_TIMER_ID
};


BEGIN_EVENT_TABLE(IGSConnection, wxEvtHandler)
    EVT_SOCKET(SOCKET_ID, IGSConnection::OnSocketEvent)
    EVT_CUSTOM(EVT_PLAY_IGS_MOVE, ID_WINDOW_IGSMAINFRAME, IGSConnection::OnPlayIGSMove)
    EVT_TIMER(SEND_TIMER_ID, IGSConnection::OnSendTimer)
    EVT_TIMER(AYT_TIMER_ID, IGSConnection::OnAytTimer)
END_EVENT_TABLE()


IGSConnection::IGSConnection(IGSMainFrame *parent, wxTextCtrl *output)
    : parentFrame(parent), output(output)
{
    wxASSERT(output != NULL);
    is_connected = false;
    socket = NULL;
    block_idle = false;
    sendTimer.SetOwner(this, SEND_TIMER_ID);
    aytTimer.SetOwner(this, AYT_TIMER_ID);
    idleTimeStamp = wxDateTime::Now();
    parser = new IGSParser(this);
    autoUpdater = new AutoUpdater(this);
}

IGSConnection::~IGSConnection()
{
    wxLogDebug("~IGSConnection()");
    disconnect();
    command_queue.Clear();
    delete parser;
    delete autoUpdater;
    wxLogDebug("~IGSConnection() done");
}

bool IGSConnection::connect(const wxString &host, const wxString &port)
{
    // Connection already in progress?
    if (socket != NULL)
        return false;
    // Create socket
    socket = new wxSocketClient();
    socket->SetEventHandler(*this, SOCKET_ID);
    socket->SetNotify(wxSOCKET_INPUT_FLAG | wxSOCKET_LOST_FLAG);
    socket->Notify(true);

    // Create IPV4 address
    wxIPV4address adr;
    adr.Hostname(host);
    adr.Service(port);

    // Try to connect
    socket->Connect(adr, false);
    socket->WaitOnConnect();
    // If WaitOnConnect() fails, the socket event already disconnected and deleted the socket
    if (socket == NULL || !socket->IsConnected())
    {
        // Connection failed
        is_connected = false;
        // Make sure the socket is really gone
        if (socket != NULL)
        {
            socket->Destroy();
            socket = NULL;
        }
        LOG_IGS(wxString::Format(_T("Failed to connect to %s:%s"), adr.Hostname().c_str(), port.c_str()));
        return false;
    }
    // Connection successful
    is_connected = true;
    parser->reset();
    sendTimer.Start(1000);
    sendWatch.Start(0);
    bool use_ayt;
    wxConfig::Get()->Read(_T("IGS/AytTimer"), &use_ayt, true);
    if (use_ayt)
        aytTimer.Start(300000);  // 5 minutes
    LOG_IGS(wxString::Format(_T("Connected to %s:%s"), adr.Hostname().c_str(), port.c_str()));
    return true;
}

void IGSConnection::disconnect()
{
    wxLogDebug("IGSConnection::disconnect()");

    // Unplug IGSFrameEventhandlers and IGSEventhandlers from boards
    wxLogDebug("Unplugging board eventhandlers. %d observed, %d played.",
               observed_games.GetCount(), played_games.GetCount());
    int i, sz;  // MSVC fix. Stupid compiler bug.
    for (i=0, sz=observed_games.GetCount(); i<sz; i++)
        unplugEventhandlers(observed_games.Item(i)->frame);
    for (i=0, sz=played_games.GetCount(); i<sz; i++)
        unplugEventhandlers(played_games.Item(i)->frame);

    // Empty arrays.
    // TODO: Keep the games in the arrays so we can auto-resume on reconnect later, like gGo does it
    // However, to keep it simple now until this is implemented, once disconnected observed/played
    // games are forgotten
    observed_games.Clear();
    played_games.Clear();

    if (socket != NULL)
        socket->Destroy();
    socket = NULL;
    sendTimer.Stop();
    aytTimer.Stop();
    sendWatch.Pause();
    is_connected = false;
    LOG_IGS(_T("Disconnected"));
}

void IGSConnection::sendCommand(const wxString &cmd, IGSSendFlag flag)
{
    if (cmd.empty() || !is_connected)
        return;

    // Check flags
    if (flag == IGS_SENDFLAG_PLAYERS)
        parser->players_flag = 1;
    else if (flag == IGS_SENDFLAG_GAMES)
        parser->gameinfo_flag = 3;
    else if (flag == IGS_SENDFLAG_STATS)
        parser->stats_flag = true;

    command_queue.Add(cmd);
    if (sendWatch.Time() > 1000)
        doSend();

    if (!block_idle)
    {
        idleTimeStamp = wxDateTime::Now();

        // Restart aytTimer if necassary
        if (!aytTimer.IsRunning())
        {
            LOG_IGS(_T("Restarted aytTimer."));
            aytTimer.Start();
        }
    }
}

void IGSConnection::doSend()
{
    if (command_queue.IsEmpty() || socket == NULL ||
        socket->IsDisconnected() || !socket->WaitForWrite(1))
        return;

    wxString cmd = command_queue.Item(0);
    command_queue.Remove(0, 1);
    cmd += "\n";
    socket->Write(cmd.c_str(), cmd.Length());
    sendWatch.Start(0);
    LOG_IGS(wxString::Format(_T("Sending command: %s"), cmd.Trim().c_str()));
}

void IGSConnection::OnSocketEvent(wxSocketEvent &event)
{
    switch(event.GetSocketEvent())
    {
    case wxSOCKET_INPUT:
        socket->Read(buf, buf_size);
        if (!socket->Error())
        {
            wxArrayString output_str;
            parser->parseBuffer(wxString(buf, socket->LastCount()), output_str);
            for (int i=0, sz=output_str.GetCount(); i<sz; i++)
                output->AppendText(output_str[i] + "\n");
        }
        else
            wxLogDebug("*** Error reading from socket ***");
        break;
    case wxSOCKET_LOST:
        wxLogDebug("wxSOCKET_LOST");
        // Don't notify if the initial connection fails.
        if (is_connected)
            parentFrame->notifyConnectionLost();
        disconnect();
        break;
    default:
        wxLogDebug("Unexpected event.");
    }
}

wxString IGSConnection::getLoginName() const
{
    wxString s;
    wxConfig::Get()->Read(_T("IGS/loginname"), &s, _T("guest"));
    return s;
}

wxString IGSConnection::getPassword() const
{
    wxString s;
    wxConfig::Get()->Read(_T("IGS/password"), &s, "");
    return s;
}

void IGSConnection::OnPlayIGSMove(EventPlayIGSMove &event)
{
    if (!is_connected)
        return;

#if 0
    wxLogDebug("IGSConnection::OnPlayIGSMove() %d - %d/%d %d #%d",
               event.getGameID(), event.getX(), event.getY(), event.getColor(), event.getMoveNumber());
#endif

    if (event.getGameID() == 0)
    {
        wxLogDebug("Invalid game ID: 0");
        return;
    }

    // Is this an own played game?
    if (!played_games.IsEmpty())
    {
        for (int i=0, sz=played_games.GetCount(); i<sz; i++)
        {
            if (played_games.Item(i)->id == event.getGameID())
            {
                IGSEventhandler *eh = played_games.Item(i)->eh;
                wxASSERT(eh != NULL);
                if (eh != NULL)
                    eh->AddPendingEvent(event);
                return;
            }
        }
    }

    // No it is not a played game. It must be observed then.

    // This game is already observed?
    if (!observed_games.IsEmpty())
    {
        for (int i=0, sz=observed_games.GetCount(); i<sz; i++)
        {
            if (observed_games.Item(i)->id == event.getGameID())
            {
                IGSEventhandler *eh = observed_games.Item(i)->eh;
                wxASSERT(eh != NULL);
                if (eh != NULL)
                    eh->AddPendingEvent(event);
                return;
            }
        }
    }

    // Not yet observed, create a new board and plug an eventhandler
    wxLogDebug("Not yet observed");

    // Create board window and send "game <id>"
    startObserve(event.getGameID());

    // Send "moves <id>" command
    parser->moves_flag = 1;
    sendCommand(wxString::Format(_T("moves %d"), event.getGameID()));

    // Skip event. We need to get the moves in first
    // eh->AddPendingEvent(event);
}

void IGSConnection::startObserve(short id)
{
    MainFrame *mf = wxGetApp().newMainFrame(GAME_TYPE_IGS_OBSERVE);
    mf->PushEventHandler(new IGSFrameEventhandler(id, this));
    wxWindow *board = mf->getCurrentBoardWindow();
    IGSEventhandler *eh = new IGSEventhandler(mf->getBoard());
    board->PushEventHandler(eh);

    ObservedGame *og = new ObservedGame;
    og->id = id;
    og->eh = eh;
    og->frame = mf;
    observed_games.Add(og);

    // Send "game <id>" command
    parser->gameinfo_flag = 1;
    sendCommand(wxString::Format(_T("game %d"), id));
}

MainFrame* IGSConnection::unobserveGame(int id, bool unplug)
{
    if (!is_connected || observed_games.IsEmpty())
        return NULL;

    for (int i=0, sz=observed_games.GetCount(); i<sz; i++)
    {
        if (observed_games.Item(i)->id == id)
        {
            LOG_IGS(wxString::Format(_T("Unobserving game %d, frame closed."), observed_games.Item(i)->id));

            // Remove IGS eventhandlers
            MainFrame *frame = observed_games.Item(i)->frame;
            if (unplug)
            {
                unplugEventhandlers(frame);
                frame->StopClock(STONE_WHITE | STONE_BLACK);
            }

            // Remove game from observed list
            observed_games.RemoveAt(i);

            if (!unplug)
                sendCommand(wxString::Format(_T("unobserve %d"), id));

            return frame;
        }
    }
    wxLogDebug(_T("Unobserve: Did not find a game!"));
    return NULL;
}

MainFrame* IGSConnection::getMainFrameById(int id)
{
    wxASSERT(!(observed_games.IsEmpty() && played_games.IsEmpty()));
    if (!is_connected || (observed_games.IsEmpty() && played_games.IsEmpty()))
        return NULL;

    // Search observed games
    int i, sz;  // MSVC fix. Stupid compiler bug.
    for (i=0, sz=observed_games.GetCount(); i<sz; i++)
        if (observed_games.Item(i)->id == id)
            return observed_games.Item(i)->frame;

    // Search own games
    for (i=0, sz=played_games.GetCount(); i<sz; i++)
        if (played_games.Item(i)->id == id)
            return played_games.Item(i)->frame;

    // Bad, nothing found
    return NULL;
}

MainFrame* IGSConnection::getMainFrameByName(const wxString &name, int &pos)
{
    // Only required for played games
    wxASSERT(!played_games.IsEmpty());
    if (!is_connected || played_games.IsEmpty())
        return NULL;

    int sz;
    for (pos=0, sz=played_games.GetCount(); pos<sz; pos++)
        if (!(played_games.Item(pos)->white).Cmp(name) ||
            !(played_games.Item(pos)->black).Cmp(name))
            return played_games.Item(pos)->frame;

    return NULL;
}

void IGSConnection::unplugEventhandlers(MainFrame *frame)
{
    wxASSERT(frame != NULL);
    if (frame == NULL)
        return;

    if (frame->GetEventHandler() != frame)
    {
        // TODO: We don't need this yet until auto-reobserve is implemented
        // static_cast<IGSFrameEventhandler*>(frame->GetEventHandler())->unplug();
        frame->PopEventHandler(true);
    }
    if (frame->getCurrentBoardWindow() != NULL &&
        frame->getCurrentBoardWindow()->GetEventHandler() != frame->getCurrentBoardWindow())
        frame->getCurrentBoardWindow()->PopEventHandler(true);
}

void IGSConnection::updateGameData(const IGSGame game, const wxString &title)
{
    if (!is_connected)
        return;

    MainFrame *frame = getMainFrameById(game.id);
    wxASSERT(frame != NULL);
    if (frame == NULL)
        return;

    frame->updateGameData(game.white_name, game.white_rank,
                          game.black_name, game.black_rank,
                          game.size, game.handicap, game.komi, game.byo, game.type,
                          title, game.id);
    // For say/kibitz feedback
    frame->setMyName(getLoginName());
}

void IGSConnection::distributeKibitz(int id, const wxString &name, const wxString &txt)
{
    if (!is_connected)
        return;

    MainFrame *frame = getMainFrameById(id);
    wxASSERT(frame != NULL);
    if (frame == NULL)
        return;

    frame->appendComment(name + ": " + txt, true);
}

void IGSConnection::distributeSay(const wxString &name, const wxString &txt)
{
    if (!is_connected)
        return;

    int dummy=0;
    MainFrame *frame = getMainFrameByName(name, dummy);  // No id in 'say'
    wxASSERT(frame != NULL);
    if (frame == NULL)
        return;

    frame->appendComment(name + ": " + txt, true);
}

void IGSConnection::distributeObservers(int id, const wxString &txt)
{
    if (!is_connected)
        return;

    MainFrame *frame = getMainFrameById(id);
    // No problem if we don't find a frame here, the user might have
    // typed "all 123" in the terminal without observing the game.
    if (frame == NULL)
        return;

    frame->appendComment(txt, true);
}

bool IGSConnection::distributeGameEnd(int id, const wxString &result)
{
    if (!is_connected)
        return false;

    MainFrame *frame = unobserveGame(id, true);
    if (frame == NULL)
        return false;

    // Show game result in comment
    frame->appendComment(result, true);

    // Update GameData
    frame->setGameResult(parser->translateResultToSGF(result));

    // Autosave game
    bool b;
    wxConfig::Get()->Read(_T("IGS/AutosaveObserved"), &b, false);
    if (b)
    {
        wxString dir;
        wxConfig::Get()->Read(_T("IGS/AutosaveObsDir"), &dir, "");
        frame->doSave(dir + wxFileName::GetPathSeparator() + frame->createDefaultFilename() + ".sgf");
    }

    // Show messagebox?
    bool val = true;
    wxConfig::Get()->Read(_T("IGS/ShowObsMsgBox"), &val, true);
    if (val)
        wxMessageBox(wxString::Format(_("The game has ended: %s"), result.c_str()),
                     _("Information"), wxOK | wxICON_INFORMATION, frame);
    return true;
}

void IGSConnection::startMatch(int id, const wxString &white, const wxString &black,
                               int white_time, int black_time, bool load_flag)
{
    wxLogDebug(_T("%s: %d White: %s  Black: %s"),
               (!load_flag ? "Start Match" : "LoadMatch"), id, white.c_str(), black.c_str());

    if (id < 1)
        return;

    // Create a new board and plug an eventhandler
    // TODO: Reuse existing board, especialy for "load", like in gGo
    MainFrame *mf = wxGetApp().newMainFrame(GAME_TYPE_IGS_PLAY);
    mf->PushEventHandler(new IGSFrameEventhandler(id, this, GAME_TYPE_IGS_PLAY));
    wxWindow *board = mf->getCurrentBoardWindow();
    IGSEventhandler *eh = new IGSEventhandler(mf->getBoard(), GAME_TYPE_IGS_PLAY);
    board->PushEventHandler(eh);

    PlayedGame *pg = new PlayedGame;
    pg->id = id;
    pg->eh = eh;
    pg->frame = mf;
    pg->white = white;
    pg->black = black;
    played_games.Add(pg);

    // Send "game <id>" command
    parser->gameinfo_flag = 1;
    sendCommand(wxString::Format(_T("game %d"), id));

    if (load_flag)
    {
        // Send "moves <id>" command for "load"ed games
        parser->moves_flag = 1;
        sendCommand(wxString::Format(_T("moves %d"), id));
    }

    // Tell mainframe to init and start clocks
    mf->InitClocks(white_time, black_time);
    // Tell board which color we play
    // TODO: This will fail for teaching games
    mf->getBoard()->setMyColor(!white.Cmp(getLoginName()) ? STONE_WHITE : STONE_BLACK);
}

void IGSConnection::endMatch(const wxString &opp_name, int id, const wxString &msg)
{
    LOG_IGS(wxString::Format(_T("EndMatch: Opponent %s, Game ID: %d"), opp_name.c_str(), id));

    MainFrame *frame = NULL;
    int pos = 0;
    if (id != -1)
    {
        wxFAIL_MSG("Not yet implemented, this should not happen!");
        frame = getMainFrameById(id);
    }
    else
        // Unfortunately IGS does not always tell us the game ID
        frame = getMainFrameByName(opp_name, pos);

    wxASSERT(frame != NULL);
    if (frame == NULL)
    {
        LOG_IGS(_T("Did not find a board window. Aborting."));
        return;
    }

    // Remove IGS eventhandlers
    unplugEventhandlers(frame);

    // Remove from played_games list
    played_games.RemoveAt(pos);
    wxLogDebug(_T("Removed played game from list at pos %d"), pos);

    // Stop clocks
    frame->StopClock(STONE_WHITE | STONE_BLACK, true);

    // Show game result in comment
    frame->appendComment(msg, true);

    // Update gameinfo with SGF parameter for resign and timeout. Not for real scores.
    int resigned;
    if ((resigned = msg.Find("resigned")) != -1 || msg.Find("out of time") != -1)
    {
        Color col;
        if (!opp_name.Cmp(getLoginName())) // We lost?
            col = reverseColor(frame->getBoard()->getMyColor());
        else
            col = frame->getBoard()->getMyColor();
        frame->setGameResult(wxString::Format("%s+%s",
                                              col == STONE_WHITE ? _T("W") : _T("B"),
                                              resigned != -1 ? _T("R") : _T("T")));
    }

    // Autosave game
    bool b;
    wxConfig::Get()->Read(_T("IGS/AutosaveOwn"), &b, false);
    if (b)
    {
        wxString dir;
        wxConfig::Get()->Read(_T("IGS/AutosaveOwnDir"), &dir, "");
        frame->doSave(dir + wxFileName::GetPathSeparator() + frame->createDefaultFilename() + ".sgf");
    }

    // Show messagebox
    wxMessageBox(msg, _("Game result"), wxOK | wxICON_INFORMATION, frame);
}

bool IGSConnection::distributeAdjournRequest(int id)
{
    wxLogDebug(_T("Adjourn request: Game ID %d"), id);

    if (!is_connected)
        return false;

    MainFrame *frame = getMainFrameById(id);
    if (frame == NULL)
    {
        LOG_IGS(_T("Got an adjourn request but found no board window!"));
        return false;
    }

    if (wxMessageBox(_("Your opponent requests to adjourn the game.\n"
                       "Do you want to adjourn?"), _("Question"),
                     wxYES_NO | wxICON_QUESTION) == wxYES)
        // Send "adjourn 123"
        sendCommand(wxString::Format(_T("adjourn %d"), frame->getGameID()));
    else
    {
        // Send "decline 123"
        sendCommand(wxString::Format(_T("decline %d"), frame->getGameID()));
        parser->parse_decline_flag = true;
    }
    return true;
}

bool IGSConnection::distributeAdjournDecline(int id)
{
    wxLogDebug(_T("Adjourn declined: Game ID %d"), id);

    if (!is_connected)
        return false;

    MainFrame *frame = getMainFrameById(id);
    if (frame == NULL)
    {
        LOG_IGS(_T("Got an adjourn decline but found no board window!"));
        return false;
    }

    wxMessageBox(_("Your request for adjournment was declined by your opponent."),
                 _("Information"),
                 wxOK | wxICON_INFORMATION);
    return true;
}

void IGSConnection::distributeAdjournOwn()
{
    // No game ID given by IGS. We have to guess.
    if (!is_connected || played_games.IsEmpty())
        return;

    if (played_games.GetCount() > 1)
    {
        LOG_IGS(_T("I got an adjourn message without ID but play multiple games. Don't know what to do..."));
        return;
    }

    MainFrame *frame = played_games.Item(0)->frame;
    wxASSERT(frame != NULL);
    if (frame == NULL)
        return;

    // Remove IGS eventhandlers
    unplugEventhandlers(frame);

    // Remove from played_games list
    played_games.RemoveAt(0);

    // Show messagebox
    wxMessageBox(_("The game has been adjourned."), _("Information"),
                 wxOK | wxICON_INFORMATION, frame);
}

void IGSConnection::distributeAdjournObserved(int id)
{
    MainFrame *frame = unobserveGame(id, true);
    if (frame != NULL)
        wxMessageBox(_("The game has been adjourned."), _("Information"),
                     wxOK | wxICON_INFORMATION, frame);
}

bool IGSConnection::distributeUndo(int id, short x, short y, const wxString &move_str)
{
    MainFrame *frame = getMainFrameById(id);
    if (frame == NULL)
    {
        LOG_IGS(wxString::Format(_T("Got undo for game %d, but cannot find a board!"), id));
        return false;
    }

    // Do the actual undo in the board
    frame->getBoard()->undoMove();

    // Print a message in the sidebar, like "Undo: K10" or so
    wxString msg;
    msg.Printf(_("Undo: %s"), move_str.c_str());
    frame->appendComment(msg, true);

    return true;
}

void IGSConnection::OnAytTimer(wxTimerEvent& WXUNUSED(event))
{
    // Stop aytTimer if 1 hour idle
    if (!idleTimeStamp.IsValid())
    {
        wxLogDebug(_T("Invalid idle timestamp"));
        idleTimeStamp = wxDateTime::Now();
        return;
    }
    wxDateTime tmpStamp = idleTimeStamp;
    tmpStamp.Add(wxTimeSpan::Minutes(MAX_IDLE_TIME));

    if (!(tmpStamp.IsLaterThan(wxDateTime::Now())))
    {
        aytTimer.Stop();
        LOG_IGS(_T("Timer stopped, idle too long."));
        return;
    }

    block_idle = true;
    sendCommand(_T("ayt"));
    block_idle = false;
}

void IGSConnection::displayMessage(const wxString &msg, const wxString &title, int flags)
{
    if (parentFrame != NULL)
        wxMessageBox(msg, title, flags, parentFrame);
}

void IGSConnection::enterScoreMode(int id)
{
    MainFrame *frame = getMainFrameById(id);
    if (frame == NULL)
    {
        LOG_IGS(_T("Entering score mode but found no frame!"));
        return;
    }

    // No toggleScore() as IGS scoring is handled a bit different than the glGo-builtin scorer
    frame->enterIGSScoreMode();
    frame->appendComment(_("You can check your score with the score command, type 'done' when finished.\n"
                           "Click on a group of stones to mark it dead and remove it from the board."));
}

bool IGSConnection::distributeScoreRemove(int id, short x, short y, const wxString &msg)
{
    MainFrame *frame = getMainFrameById(id);
    if (frame == NULL)
        return false;

    frame->getBoard()->removeDeadStone(x, y);
    frame->appendComment(msg);
    return true;
}

void IGSConnection::distributeScoreDone(const wxString &opponent, const wxString &msg)
{
    int dummy;
    MainFrame *frame = getMainFrameByName(opponent, dummy);
    if (frame != NULL)
        frame->appendComment(msg);
}

void IGSConnection::undoScore(const wxString &msg)
{
    // No game id here. Possible problems in multiple games

    MainFrame *frame = NULL;

    // Only playing in one game. Good.
    if (played_games.GetCount() == 1)
        frame = played_games.Item(0)->frame;

    // Multiple games. Let's find one in scoring. If several are in score mode, we have a problem ;(
    else
    {
        for (int i=0, sz=played_games.GetCount(); i<sz; i++)
        {
            MainFrame *f = played_games.Item(i)->frame;
            if (f->isIGSScored())
            {
                frame = f;
                break;
            }
        }
    }

    if (frame == NULL)
    {
        LOG_IGS(_T("Got an undo-score command but did not find a scored game!"));
        return;
    }

    // Remove territory and dead stone marks
    frame->getBoard()->getBoardHandler()->removeAllMarks();
    frame->getBoard()->getBoardHandler()->removeDeadMarks();

    // Run a new score process with the old position
    frame->getBoard()->getBoardHandler()->score();

    frame->appendComment(msg);
}

void IGSConnection::parseStatus(const wxString &opp_name, int id, const wxArrayString &statusArray)
{
    MainFrame *frame = NULL;
    if (id != -1 && opp_name.empty())
        frame = getMainFrameById(id);
    else
    {
        int dummy;
        frame = getMainFrameByName(opp_name, dummy);
    }

    wxASSERT(frame != NULL);
    if (frame == NULL)
        return;

    Matrix matrix(statusArray.GetCount());
    matrix.createFromIGSStatus(statusArray);
#ifdef __WXDEBUG__
    matrix.debug();
#endif

    // Display territory
    frame->getBoard()->getBoardHandler()->displayTerritoryFromMatrix(&matrix);

    // Keep the territory marks but don't display the result.
    frame->getBoard()->getBoardHandler()->finishScore(false);
}

bool IGSConnection::playsGame(short id)
{
    for (int i=0, sz=played_games.GetCount(); i<sz; i++)
        if(played_games.Item(i)->id == id)
            return true;
    return false;
}
