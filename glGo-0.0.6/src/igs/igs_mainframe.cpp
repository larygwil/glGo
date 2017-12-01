/*
 * igs_mainframe.cpp
 *
 * $Id: igs_mainframe.cpp,v 1.46 2003/11/24 01:32:47 peter Exp $
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
#pragma implementation "igs_mainframe.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/fontdlg.h>
#include <wx/config.h>
#include <wx/xrc/xmlres.h>
#include "igs_mainframe.h"
#include "tell_frame.h"
#include "player_table.h"
#include "games_table.h"
#include "shouts_frame.h"
#include "igs_connection.h"
#include "account_dialog.h"
#include "playerinfo_dialog.h"
#include "match_dialog.h"
#include "autoupdater.h"
#include "glGo.h"
#include "htmlhelp_context.h"
#include "preferences_dialog.h"
#include "about_dialog.h"
#include "html_utils.h"
#include "sound.h"
#include "inputctrl.h"
#include "playerdb.h"
#include "playerdb_gui.h"

// Icons
#ifndef __WXMSW__
#include "images/32blue.xpm"
#endif

const wxString IGS_HOST = _T("igs.joyjoy.net");
const wxString IGS_PORT = _T("6969");

bool IGSMainFrame::is_open = false;


BEGIN_EVENT_TABLE(IGSMainFrame, wxFrame)
    EVT_CLOSE(IGSMainFrame::OnClose)
    EVT_MENU(wxID_CLOSE, IGSMainFrame::OnClose)
    EVT_TEXT_ENTER(XRCID(_T("input")), IGSMainFrame::OnCommandEnter)
    EVT_MENU(XRCID(_T("connect")), IGSMainFrame::OnConnect)
    EVT_MENU(XRCID(_T("disconnect")), IGSMainFrame::OnDisconnect)
    EVT_MENU(XRCID(_T("account_config")), IGSMainFrame::OnAccountConfig)
    EVT_MENU(XRCID(_T("preferences")), IGSMainFrame::OnPreferences)
    EVT_MENU(XRCID(_T("player_management")), IGSMainFrame::OnPlayerManagement)
    EVT_MENU(XRCID(_T("my_stats")), IGSMainFrame::OnMyStats)
    EVT_MENU(XRCID(_T("user_stats")), IGSMainFrame::OnUserStats)
    EVT_MENU(XRCID(_T("clear_output")), IGSMainFrame::OnClearOutput)
    EVT_MENU(XRCID(_T("output_font")), IGSMainFrame::OnOutputFont)
    EVT_MENU(XRCID(_T("new_tell")), IGSMainFrame::OnNewTell)
    EVT_MENU(XRCID(_T("toggle_players")), IGSMainFrame::OnTogglePlayers)
    EVT_MENU(XRCID(_T("toggle_games")), IGSMainFrame::OnToggleGames)
    EVT_MENU(XRCID(_T("toggle_shouts")), IGSMainFrame::OnToggleShouts)
    EVT_MENU(wxID_HELP, IGSMainFrame::OnHelp)
    EVT_MENU(wxID_ABOUT, IGSMainFrame::OnAbout)
    EVT_MENU(XRCID(_T("open_webpage")), IGSMainFrame::OnOpenWebpage)
    EVT_MENU(XRCID(_T("check_update")), IGSMainFrame::OnCheckUpdate)
    EVT_CUSTOM(EVT_IGS_COMM, ID_WINDOW_IGSMAINFRAME, IGSMainFrame::OnCommEvent)
END_EVENT_TABLE()


IGSMainFrame::IGSMainFrame(const wxPoint& pos, const wxSize& size)
    : wxFrame(NULL, ID_WINDOW_IGSMAINFRAME, _("IGS"), pos, size)
{
    igs_connection = NULL;
    playerdb_gui = NULL;
    is_open = true;
    manual_disconnect_flag = false;

    // Assign icon
    SetIcon(wxICON(blue32));

    // Create frame and menus from resource file
    wxXmlResource::Get()->LoadPanel(this, _T("igs_mainframe_panel"));
    input = new InputCtrl(this, -1);
    wxASSERT(input != NULL);
    wxXmlResource::Get()->AttachUnknownControl(_T("input"), input);
    SetMenuBar(wxXmlResource::Get()->LoadMenuBar(_T("igs_mainframe_menu")));

    output = XRCCTRL(*this, _T("output"), wxTextCtrl);
    wxASSERT(output != NULL);

    // Try loading the terminal font from config, if it was saved
    wxFont font;
    wxString fontInfoStr;
    if (wxConfig::Get()->Read(_T("IGS/ConsoleFont"), &fontInfoStr))
        font = wxFont(fontInfoStr);
    else
        font = wxFont(9, wxMODERN, wxNORMAL, wxNORMAL);  // Default font
    wxTextAttr style(*wxBLACK, wxNullColour, font);
    output->SetDefaultStyle(style);

    // Create a statusbar
    CreateStatusBar();

    input->SetFocus();

    // Create shouts frame
    shouts = new ShoutsFrame(this);
    int size_x, size_y, pos_x, pos_y;
    if (!(wxConfig::Get()->Read(_T("IGS/Frames/ShoutsSizeX"), &size_x) &&
          wxConfig::Get()->Read(_T("IGS/Frames/ShoutsSizeY"), &size_y)))
    {
        size_x = 380;
        size_y = 240;
    }
    if (!(wxConfig::Get()->Read(_T("IGS/Frames/ShoutsPosX"), &pos_x) &&
          wxConfig::Get()->Read(_T("IGS/Frames/ShoutsPosY"), &pos_y)))
    {
        pos_x = 10;
        pos_y = 460;
    }
    shouts->SetSize(pos_x, pos_y, size_x, size_y);

    // Create games frame
    games = new GamesTable(this);
    if (!(wxConfig::Get()->Read(_T("IGS/Frames/GamesSizeX"), &size_x) &&
          wxConfig::Get()->Read(_T("IGS/Frames/GamesSizeY"), &size_y)))
    {
        size_x = 500;
        size_y = 460;
    }
    if (!(wxConfig::Get()->Read(_T("IGS/Frames/GamesPosX"), &pos_x) &&
          wxConfig::Get()->Read(_T("IGS/Frames/GamesPosY"), &pos_y)))
    {
        pos_x = 510;
        pos_y = 270;
    }
    games->SetSize(pos_x, pos_y, size_x, size_y);

    // Create player frame
    players = new PlayerTable(this);
    if (!(wxConfig::Get()->Read(_T("IGS/Frames/PlayersSizeX"), &size_x) &&
          wxConfig::Get()->Read(_T("IGS/Frames/PlayersSizeY"), &size_y)))
    {
        size_x = 500;
        size_y = 460;
    }
    if (!(wxConfig::Get()->Read(_T("IGS/Frames/PlayersPosX"), &pos_x) &&
          wxConfig::Get()->Read(_T("IGS/Frames/PlayersPosY"), &pos_y)))
    {
        pos_x = 510;
        pos_y = 10;
    }
    players->SetSize(pos_x, pos_y, size_x, size_y);

    tellHandler = new TellHandler(this);

    // Try to init the python interpreter, import the playerdb.py module and load the database file.
    // The python libraries are located in <shared_dir>/pythonlib.zip, taking advantage of the new
    // zip import feature of Python 2.3
    have_python = PlayerDB_Init(wxString::Format("%spythonlib.zip", wxGetApp().GetSharedPath().c_str()).c_str()) != -1;
    if (have_python)
    {
        wxLogDebug("Python module loaded.");
        if (PlayerDB_LoadDB(NULL) < 0)
            wxLogDebug("Failed to load player database.");
    }
    else
        wxLogDebug("Failed to import python module.");
}

IGSMainFrame::~IGSMainFrame()
{
    wxLogDebug("~IGSMainframe()");

    // Save size and position
    int x, y;
    GetSize(&x, &y);
    if (x > 100 && y > 30)  // Minimized?
    {
        wxConfig::Get()->Write(_T("IGS/Frames/TerminalSizeX"), x);
        wxConfig::Get()->Write(_T("IGS/Frames/TerminalSizeY"), y);
    }
    GetPosition(&x, &y);
    if (x > 0 && y > 0)
    {
        wxConfig::Get()->Write(_T("IGS/Frames/TerminalPosX"), x);
        wxConfig::Get()->Write(_T("IGS/Frames/TerminalPosY"), y);
    }

    is_open = false;
    if (igs_connection != NULL)
        delete igs_connection;
    delete tellHandler;
    // frames are cleared by ~wxFrame() as long as they are children of this frame

    // Save PlayerDB
    if (have_python)
    {
        if (PlayerDB_SaveDB(NULL) < 0)
            wxLogDebug("Failed to save player database.");
        // Keep interpreter running, no reason to shut it down
        // PlayerDB_Quit();
    }
}

bool IGSMainFrame::isConnected() const
{
    return igs_connection != NULL && igs_connection->IsConnected();
}

void IGSMainFrame::notifyConnectionLost()
{
    output->AppendText(wxString(_("Disconnected from IGS-PandaNet")).Append(".\n"));
    SetStatusText(_("Disconnected from IGS-PandaNet"));

    // Don't show messagebox if the user manually disconnects
    if (!manual_disconnect_flag)
    {
        wxMessageBox(_("The connection has been closed."), _("Information"), wxOK | wxICON_INFORMATION, this);
        manual_disconnect_flag = false;
    }
}

void IGSMainFrame::OnClose(wxCloseEvent& event)
{
    if (isConnected())
    {
        if (event.CanVeto())
        {
            bool asked = false;
            // Be annoying to escapers. :*)
            if (igs_connection->IsPlaying())
            {
                if (wxMessageBox(_("You are currently playing in a game.\n"
                                   "Do you want to finish your game before exiting from IGS-PandaNet?"),
                                 _("Warning"), wxYES_NO | wxICON_WARNING) == wxYES)
                {
                    event.Veto();
                    return;
                }
                asked = true;
            }
            // No escaper. Ask politely. But don't ask twice.
            if (!asked && wxMessageBox(_("You are currently connected to a server.\n"
                                         "Do you really want to close your connection?"),
                                       _("Question"), wxYES | wxNO | wxICON_EXCLAMATION) == wxNO)
            {
                event.Veto();
                return;
            }
        }
        // We cannot veto, bad luck
        wxLogDebug("OnClose: Disconnecting...");
        igs_connection->disconnect();
    }

    // Not connected anymore, hopefully
    wxGetApp().notifyIGSMainframeClosed();
    Destroy();
}

void IGSMainFrame::OnConnect(wxCommandEvent& WXUNUSED(event))
{
    connect();
}

void IGSMainFrame::connect()
{
    if (isConnected())
    {
        wxLogDebug("Already connected");
        return;
    }

    if (igs_connection == NULL)
        igs_connection = new IGSConnection(this, output);
    if (!igs_connection->connect(IGS_HOST, IGS_PORT))
        wxMessageBox(wxString::Format(
                    _("Failed to connect to %s:%s.\n"
                      "Please check your network."), IGS_HOST.c_str(), IGS_PORT.c_str()),
                     _("Network problem"), wxOK | wxICON_WARNING);
    else
        SetStatusText(_("Connected to IGS-PandaNet"));
}

void IGSMainFrame::OnDisconnect(wxCommandEvent& WXUNUSED(event))
{
    if (!isConnected())
    {
        wxLogDebug("Not connected");
        return;
    }

    // Be annoying to escapers. :*)
    if (igs_connection->IsPlaying() &&
        wxMessageBox(_("You are currently playing in a game.\n"
                       "Do you want to finish your game before exiting from IGS-PandaNet?"),
                     _("Warning"), wxYES_NO | wxICON_WARNING) == wxYES)
        return;

#if 0
    igs_connection->disconnect();
    output->AppendText(wxString(_("Disconnected from IGS-PandaNet")).Append(".\n"));
    SetStatusText(_("Disconnected from IGS-PandaNet"));
#else
    // Rather do a clean exit
    manual_disconnect_flag = true;  // Don't show disconnect messagebox
    igs_connection->sendCommand(_T("exit"));
#endif
}

void IGSMainFrame::OnCommandEnter(wxCommandEvent& WXUNUSED(event))
{
    wxString cmd = input->GetValue();
    if (cmd.empty())
        return;
    if (isConnected())
        igs_connection->sendCommand(cmd);
    input->Clear();
    output->AppendText(cmd + "\n");
}

void IGSMainFrame::OnAccountConfig(wxCommandEvent& WXUNUSED(event))
{
    AccountDialog dlg(this);
    dlg.ShowModal();
}

void IGSMainFrame::OnPreferences(wxCommandEvent& WXUNUSED(event))
{
    PreferencesDialog dlg(this);
    if (dlg.ShowModal() == wxID_OK && dlg.languageChanged() != -1 &&
        wxGetApp().setLanguage(languageToLocale(dlg.languageChanged())))
    {
        SetStatusText(_("Changed language."));

        // Replace menubar
        wxMenuBar *oldBar = GetMenuBar();
        SetMenuBar(wxXmlResource::Get()->LoadMenuBar(_T("igs_mainframe_menu")));
        delete oldBar;
    }
}

void IGSMainFrame::OnPlayerManagement(wxCommandEvent& WXUNUSED(event))
{
    // Python was loaded?
    if (!have_python)
    {
        wxMessageBox(_("Python module failed to load, player management is not available."),
                     _("Error"), wxOK | wxICON_ERROR, this);
        return;
    }

    // Make sure only one such window is opened
    if (playerdb_gui == NULL)
        playerdb_gui = new PlayerDBGui(this);
    playerdb_gui->Show(true);
}

void IGSMainFrame::OnMyStats(wxCommandEvent& WXUNUSED(event))
{
    if (isConnected())
    {
        igs_connection->sendCommand(wxString::Format(_T("stats %s"), igs_connection->getLoginName().c_str()), IGS_SENDFLAG_STATS);
        igs_connection->sendCommand(wxString::Format(_T("stored %s"), igs_connection->getLoginName().c_str()));
    }
}

void IGSMainFrame::OnUserStats(wxCommandEvent& WXUNUSED(event))
{
    if (!isConnected())
        return;
    wxString name = wxGetTextFromUser(_("Enter username:"), _("Enter player name"), "", this);
    if (!name.empty())
    {
        igs_connection->sendCommand(wxString::Format(_T("stats %s"), name.c_str()), IGS_SENDFLAG_STATS);
        igs_connection->sendCommand(wxString::Format(_T("stored %s"), name.c_str()));
    }
}

void IGSMainFrame::OnClearOutput(wxCommandEvent& WXUNUSED(event))
{
    output->Clear();
}

void IGSMainFrame::OnOutputFont(wxCommandEvent& WXUNUSED(event))
{
    wxFont font = wxGetFontFromUser(this, output->GetDefaultStyle().GetFont());
    if (font.Ok())
    {
        wxTextAttr style(*wxBLACK, wxNullColour, font);
        output->SetStyle(0, output->GetLastPosition(), style);
        output->SetDefaultStyle(style);
        output->Refresh();

        // Save native font to config
        wxConfig::Get()->Write(_T("IGS/ConsoleFont"), font.GetNativeFontInfoDesc());
    }
}

void IGSMainFrame::OnHelp(wxCommandEvent& WXUNUSED(event))
{
    // TODO: Write IGS manual chapter
#ifdef USE_MSHTMLHELP
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_INDEX);
#else
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_INDEX);
#endif
}

void IGSMainFrame::OnAbout(wxCommandEvent& event)
{
    wxString about_msg = wxString::Format(ABOUT_TEXT, PACKAGE, VERSION, VENDOR);

    wxString oal_msg;
    if (wxGetApp().IsSoundEnabled())
    {
        char info[256];
        Sound_get_info(info);
        oal_msg.Printf("\n%s", info);
    }
    else
        oal_msg = _("\nSound is disabled.");

    AboutDialog dlg(this, about_msg, wxEmptyString, oal_msg);
    dlg.ShowModal();
}

void IGSMainFrame::OnTogglePlayers(wxCommandEvent& event)
{
    if (players == NULL)
        players = new PlayerTable(this);
    players->Show(event.IsChecked());
    if (players->Empty())
        players->sendRefresh();
}

void IGSMainFrame::OnToggleGames(wxCommandEvent& event)
{
    if (games == NULL)
        games = new GamesTable(this);
    games->Show(event.IsChecked());
    if (games->Empty())
        games->sendRefresh();
}

void IGSMainFrame::OnToggleShouts(wxCommandEvent& event)
{
    if (shouts == NULL)
        shouts = new ShoutsFrame(this);
    shouts->Show(event.IsChecked());
    if (event.IsChecked())
        shouts->SetInputFocus();
}

void IGSMainFrame::notifyPlayerTableMinimized()
{
    wxMenuItem *it = GetMenuBar()->FindItem(XRCID(_T("toggle_players")));
    if (it != NULL)
        it->Check(false);
}

void IGSMainFrame::notifyGamesTableMinimized()
{
    wxMenuItem *it = GetMenuBar()->FindItem(XRCID(_T("toggle_games")));
    if (it != NULL)
        it->Check(false);
}

void IGSMainFrame::notifyShoutsMinimized()
{
    wxMenuItem *it = GetMenuBar()->FindItem(XRCID(_T("toggle_shouts")));
    if (it != NULL)
        it->Check(false);
}

void IGSMainFrame::OnNewTell(wxCommandEvent& WXUNUSED(event))
{
    tellHandler->getOrCreateTellFrame();
}

void IGSMainFrame::adjustNameWithRank(wxString &name)
{
    if (igs_connection != NULL)
    {
        const IGSPlayer *player = igs_connection->getAutoUpdater()->getPlayer(name);
        if (player != NULL)
            name += " [" + player->rank + "]";
    }
}

void IGSMainFrame::OnCommEvent(EventIGSComm& event)
{
    switch (event.getType())
    {
    case IGS_COMM_TYPE_UNDEFINED:
        LOG_IGS(wxString::Format(_T("Received undefined comm event: %d"), event.getType()));
        wxFAIL_MSG("Undefined comm event");
        return;
    case IGS_COMM_TYPE_SHOUT:
    {
        // Try to get the rank from the table
        wxString name = event.getName();
        // Bozo?
        PlayerDB_CheckReloadDB();
        if (PlayerDB_GetPlayerStatus(name) == PLAYER_STATUS_BOZO)
        {
            LOG_IGS(wxString::Format("Blocking bozo shout from %s: %s", name.c_str(), event.getText().c_str()));
            break;
        }
        wxString txt = wxEmptyString;
        if (!name.empty())
        {
            adjustNameWithRank(name);
            txt.Printf("%s: %s\n", name.c_str(), event.getText().c_str());
        }
        else
            txt = event.getText() + "\n";
        if (shouts != NULL)
            shouts->receiveShout(txt);
    }
    break;
    case IGS_COMM_TYPE_TELL:
    {
        wxString name = event.getName();
        // Bozo?
        PlayerDB_CheckReloadDB();
        if (PlayerDB_GetPlayerStatus(name) == PLAYER_STATUS_BOZO)
        {
            LOG_IGS(wxString::Format("Blocking bozo tell from %s: %s", name.c_str(), event.getText().c_str()));
            break;
        }
        // Try to get the rank from the table
        adjustNameWithRank(name);
        if (tellHandler->receiveTell(event.getName(), event.getText(), name))
            playSound(SOUND_CHAT);
    }
    break;
    case IGS_COMM_TYPE_KIBITZ:
        // Bozo?
        PlayerDB_CheckReloadDB();
        if (PlayerDB_GetPlayerStatus(event.getName()) == PLAYER_STATUS_BOZO)
        {
            LOG_IGS(wxString::Format("Blocking bozo kibitz from %s: %s", event.getName().c_str(), event.getText().c_str()));
            break;
        }
        if (isConnected())
            igs_connection->distributeKibitz(event.getID(), event.getName(), event.getText());
        break;
    case IGS_COMM_TYPE_SAY:
        if (isConnected())
            igs_connection->distributeSay(event.getName(), event.getText());
        break;
    default:
        break;
    }
}

void IGSMainFrame::OnOpenWebpage(wxCommandEvent& WXUNUSED(event))
{
    ViewHTMLFile(glGoURL);
}

void IGSMainFrame::OnCheckUpdate(wxCommandEvent& WXUNUSED(event))
{
    checkUpdate(this);
}

void IGSMainFrame::updatePlayerList(const PlayerList &player_list)
{
    if (players != NULL)
    {
        players->updatePlayerList(player_list);
        igs_connection->getAutoUpdater()->updatePlayerList(player_list);
    }
}

void IGSMainFrame::updateGamesList(const GamesList &games_list)
{
    if (games != NULL)
        games->updateGamesList(games_list);
}

void IGSMainFrame::updateGauge(int value, bool g_or_p)
{
    if (g_or_p && games != NULL)
        games->updateGauge(value);
    else if (players != NULL)
        players->updateGauge(value);
}

void IGSMainFrame::openPlayerinfoDialog(const PlayerInfo &playerInfo)
{
    PlayerinfoDialog *dlg = new PlayerinfoDialog(this, playerInfo);
    dlg->Show();
    // Dialog is deleted when it is manually closed or when this
    // frame closes, as the dialog is a child of this frame
}

void IGSMainFrame::openMatchDialog(Match *match)
{
    wxASSERT(match != NULL);
    if (match == NULL)
        return;

    // Bozo?
    PlayerDB_CheckReloadDB();
    if (PlayerDB_GetPlayerStatus(match->opponent) == PLAYER_STATUS_BOZO)
    {
        LOG_IGS(wxString::Format("Blocking match request from %s", match->opponent.c_str()));
        return;
    }

    // Try to get opponent rank
    wxString rank = wxEmptyString;
    if (igs_connection != NULL)
    {
        const IGSPlayer *player = igs_connection->getAutoUpdater()->getPlayer(match->opponent);
        if (player != NULL)
            rank = player->rank;
    }

    MatchDialog *dlg = new MatchDialog(this, match, rank);
    dlg->Show();
    // Dialog is deleted... see above

    // Play sound?
    if (match->type == MATCH_TYPE_INCOMING)
    {
        bool b;
        wxConfig::Get()->Read(_T("IGS/MatchSound"), &b, true);
        if (b)
            playSound(SOUND_MATCH);
    }
}

void IGSMainFrame::distributeTellError(const wxString &msg)
{
    tellHandler->distributeTellError(msg);
}
