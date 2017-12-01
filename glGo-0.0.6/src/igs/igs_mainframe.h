/*
 * igs_mainframe.h
 *
 * $Id: igs_mainframe.h,v 1.23 2003/11/09 11:39:15 peter Exp $
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

#ifndef IGS_MAINFRAME_H
#define IGS_MAINFRAME_H

#ifdef __GNUG__
#pragma interface "igs_mainframe.h"
#endif

#include "igs_events.h"
#include "igs_game.h"
#include "igs_player.h"


/**
 * @defgroup igsgui IGS GUI Interface
 * @ingroup igs
 *
 * The IGS GUI provides the interaction with the user and the other GUI elements
 * of the application
 *
 * @{
 */


class IGSConnection;
class TellHandler;
class PlayerTable;
class GamesTable;
class ShoutsFrame;
class PlayerDBGui;
struct Match;


/**
 * IGS console window. This is the main GUI class for the IGS user interface.
 */
class IGSMainFrame : public wxFrame
{
public:
    IGSMainFrame(const wxPoint& pos = wxDefaultPosition, const wxSize& size = wxDefaultSize);
    virtual ~IGSMainFrame();

    void connect();
    bool isConnected() const;
    IGSConnection* const getIGSConnection() const { return igs_connection; }
    wxTextCtrl* getOutput() { return output; }
    void notifyConnectionLost();
    void notifyPlayerTableClosed() { players = NULL; }
    void notifyPlayerTableMinimized();
    void notifyGamesTableClosed() { games = NULL; }
    void notifyGamesTableMinimized();
    void notifyShoutsClosed() { shouts = NULL; }
    void notifyShoutsMinimized();
    void notifyPlayerDBGUiClosed() { playerdb_gui = NULL; }
    void OnClose(wxCloseEvent& event);
    void OnConnect(wxCommandEvent& WXUNUSED(event));
    void OnDisconnect(wxCommandEvent& WXUNUSED(event));
    void OnCommandEnter(wxCommandEvent& WXUNUSED(event));
    void OnAccountConfig(wxCommandEvent& WXUNUSED(event));
    void OnPreferences(wxCommandEvent& WXUNUSED(event));
    void OnPlayerManagement(wxCommandEvent& WXUNUSED(event));
    void OnMyStats(wxCommandEvent& WXUNUSED(event));
    void OnUserStats(wxCommandEvent& WXUNUSED(event));
    void OnClearOutput(wxCommandEvent& WXUNUSED(event));
    void OnOutputFont(wxCommandEvent& WXUNUSED(event));
    /** @todo Write IGS manual chapter */
    void OnHelp(wxCommandEvent& WXUNUSED(event));
    void OnAbout(wxCommandEvent& event);
    void OnOpenWebpage(wxCommandEvent& WXUNUSED(event));
    void OnCheckUpdate(wxCommandEvent& WXUNUSED(event));
    void OnTogglePlayers(wxCommandEvent& event);
    void OnToggleGames(wxCommandEvent& event);
    void OnToggleShouts(wxCommandEvent& event);
    void OnNewTell(wxCommandEvent& WXUNUSED(event));
    void OnCommEvent(EventIGSComm& event);
    void updatePlayerList(const PlayerList &player_list);
    void updateGamesList(const GamesList &games_list);
    void updateGauge(int value, bool g_or_p=true);
    void adjustNameWithRank(wxString &name);
    void openPlayerinfoDialog(const PlayerInfo &playerInfo);
    void openMatchDialog(Match *match);
    TellHandler* getTellHandler() const { return tellHandler; }
    void distributeTellError(const wxString &msg);

    static bool is_open;  ///< Is this window created? @todo Somewhat ugly, use a better parent mechanism

private:
    IGSConnection *igs_connection;
    wxTextCtrl *input, *output;
    ShoutsFrame *shouts;
    PlayerTable *players;
    GamesTable *games;
    TellHandler *tellHandler;
    PlayerDBGui *playerdb_gui;
    bool manual_disconnect_flag, have_python;

DECLARE_EVENT_TABLE()
};

/** @} */  // End of group

#endif
