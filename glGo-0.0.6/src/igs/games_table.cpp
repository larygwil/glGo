/*
 * games_table.cpp
 *
 * $Id: games_table.cpp,v 1.16 2003/10/21 15:31:06 peter Exp $
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
#pragma implementation "games_table.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/frame.h>
#include <wx/stattext.h>
#include <wx/log.h>
#include <wx/intl.h>
#include <wx/settings.h>
#endif

#include <wx/xrc/xmlres.h>
#include <wx/gauge.h>
#include <wx/config.h>
#include "games_table.h"
#include "igs_mainframe.h"
#include "igs_connection.h"
#include "utils/utils.h"

// Icon
#ifndef __WXMSW__
#include "images/32yellow.xpm"
#endif


// ------------------------------------------------------------------------
//                             Class GamesGrid
// ------------------------------------------------------------------------

GamesGrid::GamesGrid(wxWindow *parent)
    : wxGrid(parent, -1)
{
    CreateGrid(0, 12, wxGrid::wxGridSelectRows);
    EnableEditing(0);
    SetColLabelValue(0, _("Id"));
    SetColFormatNumber(0);
    SetColLabelValue(1, _("White"));
    SetColLabelValue(2, _("Rank"));
    SetColLabelValue(3, _("Black"));
    SetColLabelValue(4, _("Rank"));
    SetColLabelValue(5, _("Moves"));
    SetColFormatNumber(5);
    SetColLabelValue(6, _("Size"));
    SetColFormatNumber(6);
    SetColLabelValue(7, "H");
    SetColFormatNumber(7);
    SetColLabelValue(8, _("Komi"));
    SetColFormatFloat(8, -1, 1);
    SetColLabelValue(9, _("Byo"));
    SetColFormatNumber(9);
    SetColLabelValue(10, _("Type"));
    SetColLabelValue(11, _("Obs"));
    SetColFormatNumber(11);
    AutoSizeColumns();
    SetRowLabelSize(0);
    SetLabelFont(wxSystemSettings::GetFont(wxSYS_DEFAULT_GUI_FONT));
    SetSelectionMode(wxGrid::wxGridSelectRows);
}


// ------------------------------------------------------------------------
//                             Class GamesTable
// ------------------------------------------------------------------------

BEGIN_EVENT_TABLE(GamesTable, wxFrame)
    EVT_CLOSE(GamesTable::OnClose)
    EVT_BUTTON(wxID_CLOSE, GamesTable::OnClose)
    EVT_BUTTON(XRCID(_T("refresh")), GamesTable::OnRefresh)
    EVT_GRID_CELL_LEFT_DCLICK(GamesTable::OnDblLeft)
    EVT_GRID_LABEL_LEFT_CLICK(GamesTable::OnLabelLeft)
    EVT_GRID_LABEL_RIGHT_CLICK(GamesTable::OnLabelRight)
END_EVENT_TABLE()


GamesTable::GamesTable(IGSMainFrame *parent)
    : parentFrame(parent)
{
    // Load frame from resource file
    wxXmlResource::Get()->LoadFrame(this, static_cast<wxWindow*>(parent), _T("igs_games_frame"));

    // Embed grid in unknown control placeholder
    grid = new GamesGrid(this);
    wxXmlResource::Get()->AttachUnknownControl(_T("games_grid"), grid);

    // Grab pointer to gauge and total text
    gauge = XRCCTRL(*this, _T("gauge"), wxGauge);
    total = XRCCTRL(*this, _T("total"), wxStaticText);
    wxASSERT(gauge != NULL && total != NULL);

    // Overwrite the wrong XRC label text, which is needed for layout
    total->SetLabel(wxString::Format(_("Total: %d"), 0));

    // Apply icon
    SetIcon(wxICON(yellow32));

    current_sorted_col = 2;
    ascending_sort = true;
    hasData = false;
}

GamesTable::~GamesTable()
{
    wxLogDebug(_T("~GamesTable()"));
    table_games_list.Empty();

    // Store position and size in config
    int x, y;
    GetSize(&x, &y);
    if (x > 100 && y > 30)  // Minimized?
    {
        wxConfig::Get()->Write(_T("IGS/Frames/GamesSizeX"), x);
        wxConfig::Get()->Write(_T("IGS/Frames/GamesSizeY"), y);
    }
    GetPosition(&x, &y);
    if (x > 0 && y > 0)
    {
        wxConfig::Get()->Write(_T("IGS/Frames/GamesPosX"), x);
        wxConfig::Get()->Write(_T("IGS/Frames/GamesPosY"), y);
    }
}

void GamesTable::sendRefresh()
{
    if (!parentFrame->isConnected())
        return;

    parentFrame->getIGSConnection()->sendCommand(_T("games"), IGS_SENDFLAG_GAMES);

    // Start gauge
    gauge->SetValue(0);
    gauge->Show();
}

void GamesTable::OnClose(wxCloseEvent& event)
{
    if (!event.CanVeto())
    {
        parentFrame->notifyGamesTableClosed();
        Destroy();
        return;
    }

    event.Veto();
    Show(false);
    parentFrame->notifyGamesTableMinimized();
}

void GamesTable::updateGamesList(const GamesList &games_list)
{
    // Copy to own list (wxObjArray does a deep copy)
    table_games_list.Empty();
    table_games_list = games_list;

    // Now display our own copy
    displayGamesList();

    // Reset gauge and remember number of games for the next run
    gauge->Hide();
    gauge->SetRange(games_list.GetCount());
}

void GamesTable::displayGamesList()
{
    int sz = table_games_list.GetCount();
    if (!sz)
        return;

    // Sort array by current sort definition
    int (*fp)(IGSGame**, IGSGame**);

    switch (current_sorted_col)
    {
    case 0:
        fp = ascending_sort ? IGSGame_sort_ID_ascending : IGSGame_sort_ID_descending;
        break;
    case 1:
        fp = ascending_sort ? IGSGame_sort_WhiteName_ascending : IGSGame_sort_WhiteName_descending;
        break;
    case 2:
        fp = ascending_sort ? IGSGame_sort_WhiteRank_ascending : IGSGame_sort_WhiteRank_descending;
        break;
    case 3:
        fp = ascending_sort ? IGSGame_sort_BlackName_ascending : IGSGame_sort_BlackName_descending;
        break;
    case 4:
        fp = ascending_sort ? IGSGame_sort_BlackRank_ascending : IGSGame_sort_BlackRank_descending;
        break;
    case 5:
        fp = ascending_sort ? IGSGame_sort_Moves_ascending : IGSGame_sort_Moves_descending;
        break;
    case 6:
        fp = ascending_sort ? IGSGame_sort_Size_ascending : IGSGame_sort_Size_descending;
        break;
    case 7:
        fp = ascending_sort ? IGSGame_sort_Handicap_ascending : IGSGame_sort_Handicap_descending;
        break;
    case 8:
        fp = ascending_sort ? IGSGame_sort_Komi_ascending : IGSGame_sort_Komi_descending;
        break;
    case 9:
        fp = ascending_sort ? IGSGame_sort_Byo_ascending : IGSGame_sort_Byo_descending;
        break;
    case 11:
        fp = ascending_sort ? IGSGame_sort_Observers_descending : IGSGame_sort_Observers_ascending;
        break;
    default:
        playSound(SOUND_BEEP);
        LOG_IGS(_T("Sorting this column is not yet supported."));
        return;
    }

    table_games_list.Sort(fp);

    // Redisplay grid
    grid->BeginBatch();
    grid->ClearGrid();
    if (grid->GetNumberRows() > 0)
        grid->DeleteRows(0, grid->GetNumberRows());
    grid->AppendRows(sz);
    wxString type;

    for (int row=0; row<sz; row++)
    {
        grid->SetCellValue(row, 0, wxString::Format("%d", table_games_list.Item(row).id));
        grid->SetCellValue(row, 1, table_games_list.Item(row).white_name);
        grid->SetCellValue(row, 2, table_games_list.Item(row).white_rank);
        grid->SetCellValue(row, 3, table_games_list.Item(row).black_name);
        grid->SetCellValue(row, 4, table_games_list.Item(row).black_rank);
        grid->SetCellValue(row, 5, wxString::Format("%d", table_games_list.Item(row).moves));
        grid->SetCellValue(row, 6, wxString::Format("%d", table_games_list.Item(row).size));
        grid->SetCellValue(row, 7, wxString::Format("%d", table_games_list.Item(row).handicap));
        grid->SetCellValue(row, 8, wxString::Format("%.1f", table_games_list.Item(row).komi));
        grid->SetCellValue(row, 9, wxString::Format("%d", table_games_list.Item(row).byo));
        if (table_games_list.Item(row).type == IGS_GAME_TYPE_RATED)
            type = _("Rated");
        else if (table_games_list.Item(row).type == IGS_GAME_TYPE_FREE)
            type = _("Free");
        else if (table_games_list.Item(row).type == IGS_GAME_TYPE_TEACH)
            type = _("Teach");
        else
            type = wxEmptyString;
        grid->SetCellValue(row, 10, type);
        grid->SetCellValue(row, 11, wxString::Format("%d", table_games_list.Item(row).observers));
    }
    grid->AutoSizeColumns();
    grid->EndBatch();

    // Set total games label
    total->SetLabel(wxString::Format(_("Total: %d"), sz));

    hasData = true;
}

void GamesTable::updateGauge(int value)
{
    if (value <= gauge->GetRange())
        gauge->SetValue(value);
}

void GamesTable::OnDblLeft(wxGridEvent& event)
{
    int row = wxAtoi(grid->GetCellValue(event.GetRow(), 0));
    if (parentFrame->isConnected())
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("observe %d"), row));
}

void GamesTable::OnLabelLeft(wxGridEvent& event)
{
    if (current_sorted_col == event.GetCol())
        // Toggle ascending/descending if we re-click on the same column
        ascending_sort = !ascending_sort;
    else
        // If we click on a new column, select ascending
        ascending_sort = true;

    current_sorted_col = event.GetCol();

    displayGamesList();
}

void GamesTable::OnLabelRight(wxGridEvent& event)
{
    current_sorted_col = event.GetCol();

    // Sort always descending
    ascending_sort = false;

    displayGamesList();
}
