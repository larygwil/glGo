/*
 * playerdb_gui.cpp
 *
 * $Id: playerdb_gui.cpp,v 1.5 2003/11/12 17:24:56 peter Exp $
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
#pragma implementation "playerdb_gui.h"
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
#include "igs_mainframe.h"
#include "glGo.h"
#include "htmlhelp_context.h"
#include "playerdb.h"
#include "playerdb_gui.h"

// Icons
#ifndef __WXMSW__
#include "images/32purple.xpm"
#endif

#ifdef __VISUALC__
// Workaround for idiotic MS compiler bug
#define for(x) if(1) for(x)
#endif


BEGIN_EVENT_TABLE(PlayerDBGui, wxFrame)
    EVT_CLOSE(PlayerDBGui::OnClose)
    EVT_BUTTON(wxID_OK, PlayerDBGui::OnClose)
    EVT_BUTTON(XRCID("add_friend"), PlayerDBGui::OnAddFriend)
    EVT_BUTTON(XRCID("remove_friend"), PlayerDBGui::OnRemoveFriend)
    EVT_BUTTON(XRCID("add_bozo"), PlayerDBGui::OnAddBozo)
    EVT_BUTTON(XRCID("remove_bozo"), PlayerDBGui::OnRemoveBozo)
    EVT_BUTTON(wxID_HELP, PlayerDBGui::OnHelp)
END_EVENT_TABLE()


PlayerDBGui::PlayerDBGui(IGSMainFrame *parent)
    : wxFrame(parent, -1, _("Player management")), parentFrame(parent)
{
    wxASSERT(parent != NULL);

    // Load XRC resources
    wxPanel *panel = wxXmlResource::Get()->LoadPanel(this, "playerdb_panel");

    // Layout panel
    wxBoxSizer *sizer = new wxBoxSizer(wxVERTICAL);
    sizer->Add(panel, 1, wxEXPAND);
    SetSizer(sizer);
    sizer->SetSizeHints(this);

    // Assign icon
    SetIcon(wxICON(purple32));

    // Centre frame
    Centre();

    // Get pointer to lists
    friends_list = XRCCTRL(*this, "friends_list", wxListBox);
    bozo_list = XRCCTRL(*this, "bozo_list", wxListBox);

    // Insert items from playerdb
    // The C Python interface returns an array of char*, we need to convert
    // this into an array of wxString and free the C array

    int size;
    char **list;

    // Friends
    PlayerDB_CheckReloadDB();
    list = PlayerDB_GetPlayerList(PLAYER_STATUS_FRIEND, &size);
    if (size > 0 && list != NULL)
    {
        wxString *friends = new wxString[size];
        for (int i=0; i<size; i++)
            friends[i] = wxString(list[i]);
        friends_list->InsertItems(size, friends, 0);
    }
    for (int i=0; i<size; i++)
        free(list[i]);
    free(list);

    // Bozos
    list = PlayerDB_GetPlayerList(PLAYER_STATUS_BOZO, &size);
    if (size > 0 && list != NULL)
    {
        wxString *bozos = new wxString[size];
        for (int i=0; i<size; i++)
            bozos[i] = wxString(list[i]);
        bozo_list->InsertItems(size, bozos, 0);
    }
    for (int i=0; i<size; i++)
        free(list[i]);
    free(list);
}

PlayerDBGui::~PlayerDBGui()
{
    // Notify IGSMainFrame
    parentFrame->notifyPlayerDBGUiClosed();
}

void PlayerDBGui::OnClose(wxCloseEvent& WXUNUSED(event))
{
    if (PlayerDB_SaveDB(NULL) < 0)
        wxLogDebug("Failed to save player database.");

    Destroy();
}

void PlayerDBGui::OnAddFriend(wxCommandEvent& WXUNUSED(event))
{
    wxString name = wxGetTextFromUser(_("Enter username:"), _("Add friend"), "", this);
    if (name.empty())
        return;
    int res = PlayerDB_AddPlayer(name, PLAYER_STATUS_FRIEND);
    if (res == 1)
    {
        int ind = bozo_list->FindString(name);
        if (ind != -1)
            bozo_list->Delete(ind);
    }
    if (res == 0 || res == 1)
        friends_list->Append(name);
}

void PlayerDBGui::OnRemoveFriend(wxCommandEvent& WXUNUSED(event))
{
    wxString name = friends_list->GetStringSelection();
    if (name.empty())
        return;
    friends_list->Delete(friends_list->GetSelection());
    PlayerDB_RemovePlayer(name);
}

void PlayerDBGui::OnAddBozo(wxCommandEvent& WXUNUSED(event))
{
    wxString name = wxGetTextFromUser(_("Enter username:"), _("Add bozo"), "", this);
    if (name.empty())
        return;
    int res = PlayerDB_AddPlayer(name, PLAYER_STATUS_BOZO);
    if (res == 1)
    {
        int ind = friends_list->FindString(name);
        if (ind != -1)
            friends_list->Delete(ind);
    }
    if (res == 0 || res == 1)
        bozo_list->Append(name);
}

void PlayerDBGui::OnRemoveBozo(wxCommandEvent& WXUNUSED(event))
{
    wxString name = bozo_list->GetStringSelection();
    if (name.empty())
        return;
    bozo_list->Delete(bozo_list->GetSelection());
    PlayerDB_RemovePlayer(name);
}

void PlayerDBGui::OnHelp(wxCommandEvent& WXUNUSED(event))
{
#ifdef USE_MSHTMLHELP
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_PLAYERDB);
#else
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_PLAYERDB);
#endif
}
