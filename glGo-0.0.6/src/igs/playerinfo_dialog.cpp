/*
 * playerinfo_dialog.cpp
 *
 * $Id: playerinfo_dialog.cpp,v 1.10 2003/11/17 13:41:24 peter Exp $
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
#pragma implementation "playerinfo_dialog.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/xrc/xmlres.h>
#include <wx/config.h>
#ifdef __WXMSW__
#include <wx/fileconf.h>
#endif
#include "playerinfo_dialog.h"
#include "igs_mainframe.h"
#include "igs_connection.h"
#include "tell_frame.h"
#include "playerdb.h"

#ifndef __WXMSW__
#include "images/32purple.xpm"
#endif

#ifdef __VISUALC__
// Workaround for idiotic MS compiler bug
#define for(x) if(1) for(x)
#endif


BEGIN_EVENT_TABLE(PlayerinfoDialog, wxFrame)
    EVT_BUTTON(XRCID("close"), PlayerinfoDialog::OnClose)
    EVT_BUTTON(XRCID("chat"), PlayerinfoDialog::OnChat)
    EVT_BUTTON(XRCID("match"), PlayerinfoDialog::OnMatch)
    EVT_RADIOBOX(XRCID("status"), PlayerinfoDialog::OnStatus)
END_EVENT_TABLE()


PlayerinfoDialog::PlayerinfoDialog(IGSMainFrame *parent, const PlayerInfo &playerInfo)
    : parentFrame(parent), info(playerInfo)
{
    // Load dialog XML resource
    if (!wxXmlResource::Get()->LoadFrame(this, parent, _T("playerinfo_dialog")))
    {
        LOG_GLOBAL("Failed to load resource for playerinfo dialog");
        return;
    }

    // Set player name as title
    SetTitle(info.name);

    // Get status from player database
    PlayerDB_CheckReloadDB();
    int status = PlayerDB_GetPlayerStatus(playerInfo.name.c_str());
    // The radiobox has another order than the PlayerStatus enum
    if (status == 0 || status == -1)  // Neutral or not found
        status = 1;
    else if (status == 1)             // Friend
        status = 0;

    // Get comment from database
    char *comment = PlayerDB_GetPlayerComment(info.name.c_str());
    wxString wx_comment = wxEmptyString;
    if (comment != NULL)
    {
        wx_comment = wxString(comment);
        free(comment);
    }

    // Fill GUI fields
    XRCCTRL(*this, "name", wxStaticText)->SetLabel(info.name);
    XRCCTRL(*this, "rating", wxStaticText)->SetLabel(info.rating);
    XRCCTRL(*this, "rank", wxStaticText)->SetLabel(info.rank);
    XRCCTRL(*this, "email", wxStaticText)->SetLabel(info.email);
    XRCCTRL(*this, "country", wxStaticText)->SetLabel(info.country);
    XRCCTRL(*this, "info", wxStaticText)->SetLabel(info.info);
    XRCCTRL(*this, "access", wxStaticText)->SetLabel(info.access);
    XRCCTRL(*this, "reg_date", wxStaticText)->SetLabel(info.reg_date);
    XRCCTRL(*this, "defaults", wxStaticText)->SetLabel(info.defaults);
    XRCCTRL(*this, "rated_games", wxStaticText)->SetLabel(wxString::Format("%d", info.rated_games));
    XRCCTRL(*this, "wins", wxStaticText)->SetLabel(wxString::Format("%d", info.wins));
    XRCCTRL(*this, "losses", wxStaticText)->SetLabel(wxString::Format("%d", info.losses));
    XRCCTRL(*this, "stored", wxStaticText)->SetLabel(wxString::Format("%d", info.stored));
    XRCCTRL(*this, "status", wxRadioBox)->SetSelection(status);
    XRCCTRL(*this, "comments", wxTextCtrl)->SetValue(wx_comment);
    // Not yet implemented: games and stored games tables, defaults, toggles

    // Load custom flags categories from config
    int flag_active[NUMBER_CUSTOM_FLAGS];
    wxString flag_value[NUMBER_CUSTOM_FLAGS];
    for (int i=0; i<NUMBER_CUSTOM_FLAGS; i++)
    {
        flag_active[i] = false;
        flag_value[i] = wxEmptyString;
    }

    // This is ugly. The config is cached by wx, so if the user edits the flags
    // in the playermanager without restarting glGo, the changes won't appear here.
    // So we cannot use wxConfig::Get() but must force a new loaded config object.
#ifdef __WXMSW__
    // Must force wxFileConfig on Win32, by default it uses the registry
    wxFileConfig *config = NULL;
    wxString configPath = wxGetHomeDir() + wxFileName::GetPathSeparator() + PACKAGE;
    if (wxDirExists(configPath))
        config = new wxFileConfig(wxEmptyString, wxEmptyString,
                                  configPath + wxFileName::GetPathSeparator() + PACKAGE + ".rc",
                                  wxEmptyString, wxCONFIG_USE_LOCAL_FILE);
#else
    wxConfig *config = NULL;
    wxString configPath = wxGetHomeDir() + wxFileName::GetPathSeparator() + "." + PACKAGE;
    if (wxDirExists(configPath))
        config = new wxConfig(wxEmptyString, wxEmptyString,
                              configPath + wxFileName::GetPathSeparator() + PACKAGE + ".rc",
                              wxEmptyString, wxCONFIG_USE_LOCAL_FILE);
#endif

    // Read the custom flags from config
    if (config != NULL)
    {
        for (int i=0; i<NUMBER_CUSTOM_FLAGS; i++)
        {
            config->Read(wxString::Format("PlayerDB/CustomFlags/Flag%dEnabled", i+1),
                         flag_active+i, false);
            config->Read(wxString::Format("PlayerDB/CustomFlags/Flag%dValue", i+1),
                         flag_value+i, wxEmptyString);
        }
    }

    // Create dynamic checkboxes and set custom flags
    wxPanel *cf_panel = XRCCTRL(*this, "custom_flags_panel", wxPanel);
    wxASSERT(cf_panel != NULL);
    wxStaticBox *box = new wxStaticBox(cf_panel, -1, _("Flags"));
    wxStaticBoxSizer *sizer =  new wxStaticBoxSizer(box, wxVERTICAL);
    for (int i=0; i<NUMBER_CUSTOM_FLAGS; i++)
    {
        if (flag_active[i])
        {
            wxCheckBox *cb = new wxCheckBox(cf_panel, 500 + i, flag_value[i]);
            cf_cb[i] = cb;
            cb->SetValue(PlayerDB_GetPlayerFlag(info.name, i) == 1);
#ifdef __WXMSW__
            sizer->Add(cb, 0, wxALL, 5);
#else
            sizer->Add(cb, 0, 0);
#endif
        }
        else
            cf_cb[i] = NULL;
    }
    cf_panel->SetSizer(sizer);
    sizer->SetSizeHints(cf_panel);

    SetIcon(wxICON(purple32));
}

void PlayerinfoDialog::OnChat(wxCommandEvent& WXUNUSED(event))
{
    wxASSERT(parentFrame != NULL && !info.name.empty());
    if (parentFrame == NULL || info.name.empty())
        return;

    parentFrame->getTellHandler()->getOrCreateTellFrame(info.name);
}

void PlayerinfoDialog::OnMatch(wxCommandEvent& WXUNUSED(event))
{
    wxASSERT(parentFrame != NULL && !info.name.empty());
    if (parentFrame == NULL || info.name.empty())
        return;

    // TODO: Calculate white/black and restore setup values
    // This is repetitive in player_table, so some better solution is required
    wxString white = parentFrame->getIGSConnection()->getLoginName();
    wxString black = info.name;
    wxString opp_name = black;
    Color col = STONE_WHITE;
    int size = 19;
    int main_time = 1;
    int byo_time = 10;

    // Open outgoing match dialog
    parentFrame->openMatchDialog(new Match(white, black, opp_name, col, size, main_time, byo_time,
                                           MATCH_TYPE_OUTGOING));
}

void PlayerinfoDialog::OnClose(wxCommandEvent& WXUNUSED(event))
{
    // Transfer comment and flags
    for (int i=0; i<NUMBER_CUSTOM_FLAGS; i++)
    {
        if (cf_cb[i] != NULL)
            PlayerDB_SetPlayerFlag(info.name, i, cf_cb[i]->IsChecked());
        cf_cb[i] = NULL;
    }
    PlayerDB_SetPlayerComment(info.name,
                              XRCCTRL(*this, "comments", wxTextCtrl)->GetValue().c_str());
    PlayerDB_SaveDB(NULL);

    Destroy();
}

void PlayerinfoDialog::OnStatus(wxCommandEvent& WXUNUSED(event))
{
    int sel = XRCCTRL(*this, _T("status"), wxRadioBox)->GetSelection();

    // The radiobox has another order than the PlayerStatus enum
    int status;
    switch (sel)
    {
    case 0:
        status = PLAYER_STATUS_FRIEND;
        break;
    case 2:
        status = PLAYER_STATUS_BOZO;
        break;
    default:
        status = PLAYER_STATUS_NEUTRAL;
    }
    PlayerDB_AddPlayer(info.name, status);
    PlayerDB_SaveDB(NULL);
}
