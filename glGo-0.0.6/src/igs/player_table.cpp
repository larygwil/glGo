/*
 * player_table.cpp
 *
 * $Id: player_table.cpp,v 1.23 2003/11/19 14:30:40 peter Exp $
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
#pragma implementation "player_table.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/frame.h>
#include <wx/button.h>
#include <wx/stattext.h>
#include <wx/log.h>
#include <wx/intl.h>
#include <wx/settings.h>
#include <wx/menu.h>
#endif

#include <wx/xrc/xmlres.h>
#include <wx/gauge.h>
#include <wx/config.h>
#include "player_table.h"
#include "igs_mainframe.h"
#include "igs_connection.h"
#include "tell_frame.h"
#include "utils/utils.h"
#include "playerdb.h"

// Icon
#ifndef __WXMSW__
#include "images/32red.xpm"
#endif

#define NUM_COLS 9


// ------------------------------------------------------------------------
//                             Class PlayerGrid
// ------------------------------------------------------------------------

PlayerGrid::PlayerGrid(wxWindow *parent)
    : wxGrid(parent, -1)
{
    CreateGrid(0, NUM_COLS, wxGrid::wxGridSelectRows);
    EnableEditing(0);
    SetColLabelValue(0, _("Flg"));
    SetColLabelValue(1, _("Name"));
    SetColLabelValue(2, _("Rank"));
    SetColLabelValue(3, _("Play"));
    SetColFormatNumber(3);
    SetColLabelValue(4, _("Obs"));
    SetColFormatNumber(4);
    SetColLabelValue(5, _("Win/Loss"));
    SetColFormatNumber(5);
    SetColLabelValue(6, _("Idle"));
    SetColFormatNumber(6);
    SetColLabelValue(7, _("Country"));
    SetColLabelValue(8, _("Info"));
    AutoSizeColumns();
    SetRowLabelSize(0);
    SetLabelFont(wxSystemSettings::GetFont(wxSYS_DEFAULT_GUI_FONT));
    SetSelectionMode(wxGrid::wxGridSelectRows);
}


// ------------------------------------------------------------------------
//                             Class PlayerTable
// ------------------------------------------------------------------------

BEGIN_EVENT_TABLE(PlayerTable, wxFrame)
    EVT_CLOSE(PlayerTable::OnClose)
    EVT_BUTTON(wxID_CLOSE, PlayerTable::OnClose)
    EVT_BUTTON(XRCID("refresh"), PlayerTable::OnRefresh)
    EVT_BUTTON(XRCID("available"), PlayerTable::OnAvailable)
    EVT_BUTTON(XRCID("friends"), PlayerTable::OnFriends)
    EVT_BUTTON(XRCID("info"), PlayerTable::OnInfo)
    EVT_BUTTON(XRCID("play"), PlayerTable::OnPlay)
    EVT_GRID_CELL_LEFT_DCLICK(PlayerTable::OnDblLeft)
    EVT_GRID_LABEL_LEFT_CLICK(PlayerTable::OnLabelLeft)
    EVT_GRID_LABEL_RIGHT_CLICK(PlayerTable::OnLabelRight)
    EVT_GRID_CELL_RIGHT_CLICK(PlayerTable::OnPopup)
    EVT_MENU(XRCID("popup_stats"), PlayerTable::OnPopupStats)
    EVT_MENU(XRCID("popup_match"), PlayerTable::OnPopupMatch)
    EVT_MENU(XRCID("popup_tell"), PlayerTable::OnPopupTell)
    EVT_MENU(XRCID("popup_friend"), PlayerTable::OnPopupFriend)
    EVT_MENU(XRCID("popup_neutral"), PlayerTable::OnPopupNeutral)
    EVT_MENU(XRCID("popup_bozo"), PlayerTable::OnPopupBozo)
END_EVENT_TABLE()


PlayerTable::PlayerTable(IGSMainFrame *parent)
    : parentFrame(parent)
{
    popup_name = wxEmptyString;
    popup_row = 0;

    // Load frame and popup from resource file
    if (!wxXmlResource::Get()->LoadFrame(this, static_cast<wxWindow*>(parent), _T("igs_player_frame")) ||
        (popup = wxXmlResource::Get()->LoadMenu(_T("player_popup"))) == NULL)
    {
        LOG_GLOBAL("Failed to load resources for player table.");
        return;
    }

    // Embed grid in unknown control placeholder
    grid = new PlayerGrid(this);
    wxXmlResource::Get()->AttachUnknownControl(_T("player_grid"), grid);

    // Grab pointer to gauge and total text
    gauge = XRCCTRL(*this, _T("gauge"), wxGauge);
    total = XRCCTRL(*this, _T("total"), wxStaticText);
    wxASSERT(gauge != NULL && total != NULL);

    // Overwrite the wrong XRC label text, which is needed for layout
    total->SetLabel(wxString::Format(_("Total: %d"), 0));

    // Apply saved upper/lower limits
    wxString s;
    if (wxConfig::Get()->Read(_T("IGS/Frames/PlayersUpper"), &s))
        XRCCTRL(*this, _T("upper_rank"), wxComboBox)->SetValue(s);
    if (wxConfig::Get()->Read(_T("IGS/Frames/PlayersLower"), &s))
        XRCCTRL(*this, _T("lower_rank"), wxComboBox)->SetValue(s);

    // Apply icon
    SetIcon(wxICON(red32));

    current_sorted_col = 2;
    ascending_sort = true;
    available = friends = hasData = false;
}

PlayerTable::~PlayerTable()
{
    wxLogDebug("~PlayerTable()");
    table_player_list.Empty();

    // Store position and size in config
    int x, y;
    GetSize(&x, &y);
    if (x > 100 && y > 30)  // Minimized?
    {
        wxConfig::Get()->Write(_T("IGS/Frames/PlayersSizeX"), x);
        wxConfig::Get()->Write(_T("IGS/Frames/PlayersSizeY"), y);
    }
    GetPosition(&x, &y);
    if (x > 0 && y > 0)
    {
        wxConfig::Get()->Write(_T("IGS/Frames/PlayersPosX"), x);
        wxConfig::Get()->Write(_T("IGS/Frames/PlayersPosY"), y);
    }
}

void PlayerTable::getRankSelection(wxString &lower, wxString &upper)
{
    upper = XRCCTRL(*this, _T("upper_rank"), wxComboBox)->GetValue();
    lower = XRCCTRL(*this, _T("lower_rank"), wxComboBox)->GetValue();

    // If empty or invalid, replace upper with "9p" and lower with "NR"
    IGSRank dummy(upper);
    if (dummy.getRank() == 54)  // 54 = Invalid rank
        upper = "9p";
    dummy = IGSRank(lower);
    if (dummy.getRank() == 54)
        lower = _T("NR");
}

void PlayerTable::sendRefresh()
{
    // Get rank limits
    wxString lower, upper;
    getRankSelection(lower, upper);

    if (parentFrame->isConnected())
    {
        wxString l = lower, u = upper;

        // Check if we want to show all friends ignoring the rank limits
        bool b;
        wxConfig::Get()->Read(_T("IGS/ShowAllFriends"), &b, false);
        if (b && friends)
        {
            l = "NR";
            u = "9p";
        }

        // Send "user <upper>-<lower>"
        parentFrame->getIGSConnection()->sendCommand(
            wxString::Format(_T("user %s-%s"), u.c_str(), l.c_str()), IGS_SENDFLAG_PLAYERS);

        // Start gauge
        gauge->SetValue(0);
        gauge->Show();
    }

    // Save limits in config
    wxConfig::Get()->Write(_T("IGS/Frames/PlayersUpper"), upper);
    wxConfig::Get()->Write(_T("IGS/Frames/PlayersLower"), lower);
}

void PlayerTable::OnClose(wxCloseEvent& event)
{
    if (!event.CanVeto())
    {
        parentFrame->notifyPlayerTableClosed();
        Destroy();
        return;
    }

    event.Veto();
    Show(false);
    parentFrame->notifyPlayerTableMinimized();
}

void PlayerTable::updatePlayerList(const PlayerList &player_list)
{
    // Copy to own list (wxObjArray does a deep copy)
    table_player_list.Empty();
    table_player_list = player_list;

    // Load playerstatus from PlayerDB. This is cached to avoid reloading over and over when sorting/filtering.
    PlayerDB_CheckReloadDB();
    for (int i=0, sz=table_player_list.GetCount(); i<sz; i++)
        table_player_list.Item(i).status = PlayerDB_GetPlayerStatus(table_player_list.Item(i).name);

    // Now display our own copy
    displayPlayerList();

    // Reset gauge and remember number of games for the next run
    gauge->Hide();
    gauge->SetRange(player_list.GetCount());
}

void PlayerTable::displayPlayerList()
{
    int sz = table_player_list.GetCount();
    if (!sz)
        return;

    // Sort array by current sort definition
    int (*fp)(IGSPlayer**, IGSPlayer**);

    switch (current_sorted_col)
    {
    case 1:
        fp = ascending_sort ? IGSPlayer_sort_Name_ascending : IGSPlayer_sort_Name_descending;
        break;
    case 2:
        fp = ascending_sort ? IGSPlayer_sort_Rank_ascending : IGSPlayer_sort_Rank_descending;
        break;
    case 7:
        fp = ascending_sort ? IGSPlayer_sort_Country_ascending : IGSPlayer_sort_Country_descending;
        break;
    default:
        playSound(SOUND_BEEP);
        LOG_IGS(_T("Sorting this column is not yet supported."));
        fp = NULL;
    }

    if (fp != NULL)
        table_player_list.Sort(fp);

    // Reduce the number of unavailable players. This is redundant, see below, but probably
    // still cheaper than copying the list. We cannot overwrite the existing list as the
    // user might disable the filter again, so we need to keep the old list.
    // Maybe a smarter solution can be done, but this is not performance critical, the user
    // won't click the Available button in milliseconds frequency.
    int sz2 = sz;
    if (available || friends)
        for (int i=0, row=0; i<sz; i++, row++)
            if ((available && (table_player_list.Item(i).play.Trim(false).Cmp("-") ||
                               table_player_list.Item(i).flags.Find("X") != -1)) ||
                (friends &&
                 table_player_list.Item(i).status != PLAYER_STATUS_FRIEND))
                sz2 --;

    // Redisplay grid
    grid->BeginBatch();
    grid->ClearGrid();
    if (grid->GetNumberRows() > 0)
        grid->DeleteRows(0, grid->GetNumberRows());
    grid->AppendRows(sz2);  // Total size minus filtered
    wxString type;

    for (int i=0, row=0; i<sz; i++, row++)
    {
        // Available and friends filter
        int status = table_player_list.Item(i).status;
        if ((available && (table_player_list.Item(i).play.Trim(false).Cmp("-") ||
                           table_player_list.Item(i).flags.Find("X") != -1)) ||
            (friends && status != PLAYER_STATUS_FRIEND))
        {
            row--;
            continue;
        }

        grid->SetCellValue(row, 0, table_player_list.Item(i).flags);
        grid->SetCellValue(row, 1, table_player_list.Item(i).name);
        grid->SetCellValue(row, 2, table_player_list.Item(i).rank);
        grid->SetCellValue(row, 3, table_player_list.Item(i).play);
        grid->SetCellValue(row, 4, table_player_list.Item(i).obs);
        grid->SetCellValue(row, 5, table_player_list.Item(i).win_loss);
        grid->SetCellValue(row, 6, table_player_list.Item(i).idle);
        grid->SetCellValue(row, 7, table_player_list.Item(i).country);
        grid->SetCellValue(row, 8, table_player_list.Item(i).info);

        // Draw friends in blue, bozos in red
        if (status != -1 && status != PLAYER_STATUS_NEUTRAL)
            for (int j=0; j<NUM_COLS; j++)
                grid->SetCellTextColour(row, j, status == PLAYER_STATUS_FRIEND ? *wxBLUE : *wxRED);
    }
    grid->AutoSizeColumns();
    grid->EndBatch();

    // Set total player label
    total->SetLabel(wxString::Format(_("Total: %d"), sz2));

    hasData = true;
}

void PlayerTable::updateGauge(int value)
{
    if (value <= gauge->GetRange())
        gauge->SetValue(value);
}

void PlayerTable::OnDblLeft(wxGridEvent& event)
{
    sendStats(grid->GetCellValue(event.GetRow(), 1));
}

void PlayerTable::sendStats(const wxString &name)
{
    if (parentFrame->isConnected() && !name.empty())
    {
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("stats %s"), name.c_str()), IGS_SENDFLAG_STATS);
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("stored %s"), name.c_str()));
    }
}

void PlayerTable::OnLabelLeft(wxGridEvent& event)
{
    if (current_sorted_col == event.GetCol())
        // Toggle ascending/descending if we re-click on the same column
        ascending_sort = !ascending_sort;
    else
        // If we click on a new column, select ascending
        ascending_sort = true;

    current_sorted_col = event.GetCol();

    displayPlayerList();
}

void PlayerTable::OnLabelRight(wxGridEvent& event)
{
    current_sorted_col = event.GetCol();

    // Sort always descending
    ascending_sort = false;

    displayPlayerList();
}

void PlayerTable::OnAvailable(wxCommandEvent& WXUNUSED(event))
{
    available = !available;

    // Switch button text. No toggle buttons in wxGTK
    wxButton *button = XRCCTRL(*this, _T("available"), wxButton);
    wxASSERT(button != NULL);
    if (button != NULL)
        button->SetLabel(available ? _("All") : _("Available"));

    displayPlayerList();
}

void PlayerTable::OnFriends(wxCommandEvent& WXUNUSED(event))
{
    friends = !friends;

    // Switch button text. No toggle buttons in wxGTK
    wxButton *button = XRCCTRL(*this, _T("friends"), wxButton);
    wxASSERT(button != NULL);
    if (button != NULL)
        button->SetLabel(friends ? _("All") : _("Friends"));

    // Check if we want to show all friends ignoring the rank limits
    bool b;
    wxConfig::Get()->Read(_T("IGS/ShowAllFriends"), &b, false);
    if (b)
    {
        // Get rank limits. We don't need to check further if the rank limit is "9p-NR" anyways.
        wxString lower, upper;
        getRankSelection(lower, upper);
        if (lower.Cmp("NR") && upper.Cmp("9p") && parentFrame->isConnected())
        {
            sendRefresh();
            return;
        }
    }
    displayPlayerList();
}

int PlayerTable::getSelectedRow()
{
    // Anything selected?
    if (!grid->IsSelection())
        return -1;

    // Get selected rows. For whatever reason wxGrid::GetSelectedRows() will always return
    // an empty array here, so we have to do it the hard way.
    int top = (grid->GetSelectionBlockTopLeft()).Item(0).GetRow();
    int bottom = (grid->GetSelectionBlockBottomRight()).Item(0).GetRow();
    // Not exactly one row selected?
    if (top != bottom)
        return -1;
    return top;
}

void PlayerTable::OnInfo(wxCommandEvent& WXUNUSED(event))
{
    if (!parentFrame->isConnected())
        return;

    int row = getSelectedRow();
    if (row == -1)
    {
        playSound(SOUND_BEEP);
        return;
    }

    sendStats(grid->GetCellValue(row, 1));
}

void PlayerTable::OnPlay(wxCommandEvent& WXUNUSED(event))
{
    // Matching makes little sense when not connected
    if (!parentFrame->isConnected())
        return;

    int row = getSelectedRow();
    if (row == -1)
    {
        playSound(SOUND_BEEP);
        return;
    }

    // Get opponent name from selected row
    wxString name = grid->GetCellValue(row, 1);

    // TODO: Calculate white/black and restore setup values
    // This is repetitive code
    wxString white = parentFrame->getIGSConnection()->getLoginName();
    wxString black = name;
    wxString opp_name = black;
    Color col = STONE_WHITE;
    int size = 19;
    int main_time = 1;
    int byo_time = 10;

    // Open outgoing match dialog
    parentFrame->openMatchDialog(new Match(white, black, opp_name, col, size, main_time, byo_time,
                                           MATCH_TYPE_OUTGOING));
}

IGSPlayer* PlayerTable::getPlayer(const wxString &name) const
{
    for (int i=0, sz=table_player_list.GetCount(); i<sz; i++)
        if (!name.Cmp(table_player_list.Item(i).name))
            return &(table_player_list.Item(i));
    return NULL;
}

void PlayerTable::OnPopup(wxGridEvent& event)
{
    wxASSERT(popup != NULL);
    popup_row = event.GetRow();
    popup_name = grid->GetCellValue(popup_row, 1);

    // Load status from PlayerDB
    PlayerDB_CheckReloadDB();
    int status = PlayerDB_GetPlayerStatus(popup_name);

    // Make sure the cached status is up to date
    IGSPlayer *p = getPlayer(popup_name);
    if (p != NULL)
        p->status = status;

    // Set radio menuitem
    wxMenuItem *it = NULL;
    switch (status)
    {
    case -1:
    case PLAYER_STATUS_NEUTRAL:
        it = popup->FindItem(XRCID("popup_neutral"));
        break;
    case PLAYER_STATUS_FRIEND:
        it = popup->FindItem(XRCID("popup_friend"));
        break;
    case PLAYER_STATUS_BOZO:
        it = popup->FindItem(XRCID("popup_bozo"));
        break;
    }
    if (it != NULL)
        it->Check(true);

    // Show the popup
    PopupMenu(popup, event.GetPosition());
}

void PlayerTable::OnPopupStats(wxCommandEvent& WXUNUSED(event))
{
    sendStats(popup_name);
}

void PlayerTable::OnPopupTell(wxCommandEvent& WXUNUSED(event))
{
    if (!parentFrame->isConnected() || popup_name.empty())
        return;

    wxString full_name = popup_name;
    parentFrame->adjustNameWithRank(full_name);
    parentFrame->getTellHandler()->getOrCreateTellFrame(popup_name, full_name);
}

void PlayerTable::OnPopupMatch(wxCommandEvent& WXUNUSED(event))
{
    if (!parentFrame->isConnected() || popup_name.empty())
        return;

    // TODO: Calculate white/black and restore setup values
    // This is repetitive code
    wxString white = parentFrame->getIGSConnection()->getLoginName();
    wxString black = popup_name;
    wxString opp_name = black;
    Color col = STONE_WHITE;
    int size = 19;
    int main_time = 1;
    int byo_time = 10;

    // Open outgoing match dialog
    parentFrame->openMatchDialog(new Match(white, black, opp_name, col, size, main_time, byo_time,
                                           MATCH_TYPE_OUTGOING));
}

void PlayerTable::updatePlayerStatus(const wxString &name, int status, wxColour *col)
{
    PlayerDB_CheckReloadDB();
    PlayerDB_AddPlayer(name.c_str(), status);
    IGSPlayer *p = getPlayer(name);
    if (p != NULL)
        p->status = status;
    for (int j=0; j<NUM_COLS; j++)
        grid->SetCellTextColour(popup_row, j, *col);
    PlayerDB_SaveDB(NULL);
}

void PlayerTable::OnPopupFriend(wxCommandEvent& WXUNUSED(event))
{
    updatePlayerStatus(popup_name, PLAYER_STATUS_FRIEND, wxBLUE);
}

void PlayerTable::OnPopupNeutral(wxCommandEvent& WXUNUSED(event))
{
    updatePlayerStatus(popup_name, PLAYER_STATUS_NEUTRAL, wxBLACK);
}

void PlayerTable::OnPopupBozo(wxCommandEvent& WXUNUSED(event))
{
    updatePlayerStatus(popup_name, PLAYER_STATUS_BOZO, wxRED);
}
