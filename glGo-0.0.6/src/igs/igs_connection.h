/*
 * igs_connection.h
 *
 * $Id: igs_connection.h,v 1.30 2003/11/24 15:58:11 peter Exp $
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

#ifndef IGS_CONNECTION_H
#define IGS_CONNECTION_H

#ifdef __GNUG__
#pragma interface "igs_connection.h"
#endif

#include <wx/socket.h>
#include <wx/timer.h>
#include <wx/datetime.h>
#include "igs_events.h"
#include "igs_game.h"


class IGSParser;
class IGSMainFrame;
class IGSEventhandler;
class IGSFrameEventhandler;
class MainFrame;
class AutoUpdater;


/**
 * @defgroup igs IGS
 *
 * The IGS module is responsible for the network connection to IGS, parsing
 * and handling the server commands and the complete GUI interface which
 * interacts as client with the user.
 *
 * @{
 */

/** Used for sendCommand flags */
enum IGSSendFlag
{
    IGS_SENDFLAG_NONE,
    IGS_SENDFLAG_PLAYERS,
    IGS_SENDFLAG_GAMES,
    IGS_SENDFLAG_STATS
};

/** Used to differ incoming and outgoing match requests. */
enum MatchType
{
    MATCH_TYPE_INCOMING,
    MATCH_TYPE_OUTGOING
};

/** An IGS match request. */
struct Match
{
    Match(const wxString &white, const wxString &black, const wxString &opp, Color col,
          int size, int main_time, int byo_time, MatchType type)
        : white(white), black(black), opponent(opp), col(col), size(size),
        main_time(main_time), byo_time(byo_time), type(type) {}
    wxString white, black, opponent;
    Color col;
    int size, main_time, byo_time;
    MatchType type;
};

/**
 * Simple struct defining an observed game by ID and a pointer to the
 * eventhandler currently plugged into the board window.
 */
struct ObservedGame
{
    short id;
    IGSEventhandler *eh;
    MainFrame *frame;
};

/**
 * Simple struct extending ObservedGame with black/white player names.
 * Used for handling of own games.
 */
struct PlayedGame : public ObservedGame
{
    wxString white, black;
};

/** Array containing all observed games */
WX_DEFINE_ARRAY(ObservedGame*, ObservedGames);

/** Array containing all played games */
WX_DEFINE_ARRAY(PlayedGame*, PlayedGames);


/**
 * Reading and writing to the network socket.
 * This class embeds a wxSocketClient object and is responsible for establishing and
 * managing the connection to the server. Data from the socket is read and written
 * from this class. Read data ia passed to IGSReader for further parsing.
 */
class IGSConnection : public wxEvtHandler
{
public:
    IGSConnection(IGSMainFrame *parent, wxTextCtrl *output);
    ~IGSConnection();
    IGSMainFrame* getIGSMainFrame() const { return parentFrame; }
    AutoUpdater* getAutoUpdater() const { return autoUpdater; }
    wxString getLoginName() const;
    wxString getPassword() const;
    bool IsConnected() const { return is_connected; }
    bool IsPlaying() const { return !played_games.IsEmpty(); }
    bool connect(const wxString &host, const wxString &port);
    void disconnect();
    void sendCommand(const wxString &cmd, IGSSendFlag flag=IGS_SENDFLAG_NONE);
    void OnSocketEvent(wxSocketEvent &event);
    void OnPlayIGSMove(EventPlayIGSMove &event);
    MainFrame* unobserveGame(int id, bool unplug);
    void updateGameData(const IGSGame game, const wxString &title);
    void distributeKibitz(int id, const wxString &name, const wxString &txt);
    void distributeSay(const wxString &name, const wxString &txt);
    void distributeObservers(int id, const wxString &txt);
    bool distributeGameEnd(int id, const wxString &result);
    /** @todo setMyColor will fail for teaching games */
    void startMatch(int id, const wxString &white, const wxString &black,
                    int white_time, int black_time, bool load_flag=false);
    void endMatch(const wxString &opp_name, int id=-1, const wxString &msg=wxEmptyString);
    bool distributeAdjournRequest(int id);
    bool distributeAdjournDecline(int id);
    void distributeAdjournOwn();
    void distributeAdjournObserved(int id);
    bool distributeUndo(int id, short x, short y, const wxString &move_str);
    void displayMessage(const wxString &msg, const wxString &title=wxEmptyString, int flags=wxOK | wxICON_INFORMATION);
    void enterScoreMode(int id);
    bool distributeScoreRemove(int id, short x, short y, const wxString &msg);
    void distributeScoreDone(const wxString &opponent, const wxString &msg);
    void undoScore(const wxString &msg);
    void parseStatus(const wxString &opp_name, int id, const wxArrayString &statusArray);
    bool playsGame(short id);
    void startObserve(short id);

private:
    void OnSendTimer(wxTimerEvent& WXUNUSED(event)) { doSend(); }
    void OnAytTimer(wxTimerEvent& WXUNUSED(event));
    void doSend();
    MainFrame* getMainFrameById(int id);
    MainFrame* getMainFrameByName(const wxString &name, int &pos);
    void unplugEventhandlers(MainFrame *frame);

    IGSMainFrame *parentFrame;
    wxTextCtrl *output;
    wxSocketClient *socket;
    IGSParser *parser;
    bool is_connected;
    ObservedGames observed_games;
    PlayedGames played_games;
    wxArrayString command_queue;
    wxTimer sendTimer, aytTimer;
    wxStopWatch sendWatch;
    wxDateTime idleTimeStamp;
    bool block_idle;
#ifdef __VISUALC__
    // This crap compiler doesn't know C++
    #define buf_size 256
    #define MAX_IDLE_TIME 60
#else
    static const unsigned int buf_size = 256; /** Socket read buffer size */
    static const long MAX_IDLE_TIME = 60; /** Max idle in minutes time until aytTimer stops */
#endif
    char buf[buf_size];
    AutoUpdater *autoUpdater;

DECLARE_EVENT_TABLE()
};

/** @} */  // End of group

#endif
