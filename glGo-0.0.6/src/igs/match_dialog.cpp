/*
 * match_dialog.cpp
 *
 * $Id: match_dialog.cpp,v 1.5 2003/11/04 14:54:56 peter Exp $
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
#pragma implementation "match_dialog.h"
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
#include <wx/valgen.h>
#include "igs_mainframe.h"
#include "igs_connection.h"
#include "match_dialog.h"
#include "utils/utils.h"


BEGIN_EVENT_TABLE(MatchDialog, wxDialog)
    EVT_BUTTON(wxID_OK, MatchDialog::OnOK)
    EVT_BUTTON(wxID_CANCEL, MatchDialog::OnCancel)
    EVT_BUTTON(XRCID("swap"), MatchDialog::OnSwap)
    EVT_BUTTON(XRCID("query_stats"), MatchDialog::OnStats)
END_EVENT_TABLE()


MatchDialog::MatchDialog(IGSMainFrame *parent, Match *m, const wxString &opp_rank)
    : parentFrame(parent), match(m)
{
    wxASSERT(match != NULL);

    SetExtraStyle(wxWS_EX_VALIDATE_RECURSIVELY);

    // Load dialog XML resource
    if (!wxXmlResource::Get()->LoadDialog(this, parent,
                                          match->type == MATCH_TYPE_INCOMING ? _T("match_dialog_in") : _T("match_dialog_out")))
    {
        LOG_GLOBAL(_T("Failed to load resource for match dialog"));
        return;
    }

    // Append rank to name if we got it
    wxString s = match->opponent;
    if (!opp_rank.empty())
        s += " " + opp_rank;

    // Fill GUI fields
    XRCCTRL(*this, "name", wxStaticText)->SetLabel(s);
    XRCCTRL(*this, "white", wxStaticText)->SetLabel(match->white);
    XRCCTRL(*this, "black", wxStaticText)->SetLabel(match->black);
    XRCCTRL(*this, "size", wxTextCtrl)->SetValidator(wxGenericValidator(&(match->size)));
    XRCCTRL(*this, "maintime", wxTextCtrl)->SetValidator(wxGenericValidator(&(match->main_time)));
    XRCCTRL(*this, "byotime", wxTextCtrl)->SetValidator(wxGenericValidator(&(match->byo_time)));
}

MatchDialog::~MatchDialog()
{
    wxLogDebug("~MatchDialog()");
    if (match != NULL)
    {
        delete match;
        match = NULL;
    }
}

bool MatchDialog::TransferDataFromWindow()
{
    if (!wxWindow::TransferDataFromWindow())
        return false;

    // Swap color if necessary
    if ((match->col == STONE_WHITE && !(match->white).Cmp(match->opponent)) ||
        (match->col == STONE_BLACK && !(match->black).Cmp(match->opponent)))
        match->col = reverseColor(match->col);

    return true;
}

void MatchDialog::OnOK(wxCommandEvent& WXUNUSED(event))
{
    if (Validate() && TransferDataFromWindow())
    {
        if (parentFrame->isConnected())
            // match <opponentname> [color] [board size] [time] [byoyomi minutes]
            parentFrame->getIGSConnection()->sendCommand(
                wxString::Format(_T("match %s %s %d %d %d"),
                                 (match->opponent).c_str(),
                                 (match->col == STONE_WHITE ? "W" : "B"),
                                 match->size,
                                 match->main_time,
                                 match->byo_time));
        SetReturnCode(wxID_OK);
        Destroy();
    }
}

void MatchDialog::OnCancel(wxCommandEvent& WXUNUSED(event))
{
    if (match->type == MATCH_TYPE_INCOMING && parentFrame->isConnected())
        parentFrame->getIGSConnection()->sendCommand(_T("decline ") + match->opponent);
    SetReturnCode(wxID_CANCEL);
    Destroy();
}

void MatchDialog::OnSwap(wxCommandEvent& WXUNUSED(event))
{
    wxString tmp = match->white;
    match->white = match->black;
    match->black = tmp;

    XRCCTRL(*this, "white", wxStaticText)->SetLabel(match->white);
    XRCCTRL(*this, "black", wxStaticText)->SetLabel(match->black);
}

void MatchDialog::OnStats(wxCommandEvent& WXUNUSED(event))
{
    if (parentFrame->isConnected())
    {
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("stats %s"), (match->opponent).c_str()), IGS_SENDFLAG_STATS);
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("stored %s"), (match->opponent).c_str()));
    }
}
