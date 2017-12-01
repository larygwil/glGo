/*
 * tell_frame.cpp
 *
 * $Id: tell_frame.cpp,v 1.14 2003/11/04 14:55:53 peter Exp $
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
#pragma implementation "tell_frame.h"
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
#include <wx/fontdlg.h>
#include <wx/config.h>
#include "tell_frame.h"
#include "igs_mainframe.h"
#include "igs_connection.h"
#include "inputctrl.h"

// Icons
#include "images/volume.xpm"
#include "images/volume_off.xpm"
#ifndef __WXMSW__
#include "images/32emerald.xpm"
#endif


// ------------------------------------------------------------------------
//                          Class TellFrame
// ------------------------------------------------------------------------

BEGIN_EVENT_TABLE(TellFrame, wxFrame)
    EVT_BUTTON(XRCID("clear"), TellFrame::OnClearButton)
    EVT_BUTTON(wxID_CLOSE, TellFrame::OnCloseButton)
    EVT_BUTTON(XRCID("sound"), TellFrame::OnSoundToggle)
    EVT_BUTTON(XRCID("font"), TellFrame::OnFont)
    EVT_BUTTON(XRCID("info"), TellFrame::OnInfo)
    EVT_BUTTON(XRCID("match"), TellFrame::OnMatch)
    EVT_TEXT_ENTER(XRCID("input"), TellFrame::OnCommandEnter)
END_EVENT_TABLE()


TellFrame::TellFrame(TellHandler *handler, const wxString &target_player,
                     const wxString &name_rank, IGSMainFrame *parent)
    : tellHandler(handler), target(target_player)
{
    // Load frame from resource file
    wxXmlResource::Get()->LoadFrame(this, static_cast<wxWindow*>(parent), _T("igs_tell_frame"));

    input = new InputCtrl(this, -1);
    wxASSERT(input != NULL);
    wxXmlResource::Get()->AttachUnknownControl(_T("input"), input);

    output = XRCCTRL(*this, _T("output"), wxTextCtrl);
    wxASSERT(output != NULL);

    // Set volume image
    wxConfig::Get()->Read(_T("IGS/ChatSound"), &sound_on, true);
    if (sound_on)
        XRCCTRL(*this, _T("sound"), wxBitmapButton)->SetBitmapLabel(wxBitmap(volume_xpm));
    else
        XRCCTRL(*this, _T("sound"), wxBitmapButton)->SetBitmapLabel(wxBitmap(volume_off_xpm));

    // Show target player in textfield
    XRCCTRL(*this, _T("target_player"), wxStaticText)->SetLabel(name_rank);

    // Try loading the terminal font from config, if it was saved
    wxFont font;
    wxString fontInfoStr;
    if (wxConfig::Get()->Read(_T("IGS/ChatFont"), &fontInfoStr))
        font = wxFont(fontInfoStr);
    else
        font = wxFont(9, wxMODERN, wxNORMAL, wxNORMAL);  // Default font
    wxTextAttr style(*wxBLACK, wxNullColour, font);
    output->SetDefaultStyle(style);

    SetTitle(name_rank);
    SetIcon(wxICON(emerald32));

    SetSize(420, 340);  // TODO: Restore

    input->SetFocus();
}

TellFrame::~TellFrame()
{
    wxLogDebug("~TellFrame()");
    // Unregister
    if (IGSMainFrame::is_open)
        tellHandler->notifyTellClosed(target);
}

void TellFrame::OnCommandEnter(wxCommandEvent& WXUNUSED(event))
{
    wxString cmd = input->GetValue();
    if (cmd.empty())
        return;
    input->Clear();

    tellHandler->sendTell(target, cmd);
}

void TellFrame::OnFont(wxCommandEvent& WXUNUSED(event))
{
    wxFont font = wxGetFontFromUser(this, output->GetDefaultStyle().GetFont());
    if (font.Ok())
    {
        wxTextAttr style(*wxBLACK, wxNullColour, font);
        output->SetStyle(0, output->GetLastPosition(), style);
        output->SetDefaultStyle(style);
        output->Refresh();

        // Save native font to config
        wxConfig::Get()->Write(_T("IGS/ChatFont"), font.GetNativeFontInfoDesc());
    }
}

void TellFrame::OnClearButton(wxCommandEvent& WXUNUSED(event))
{
    output->Clear();
}

void TellFrame::OnCloseButton(wxCommandEvent& WXUNUSED(event))
{
    Destroy();
}

bool TellFrame::appendText(const wxString &txt, bool no_target)
{
    if (!txt.empty())
    {
        if (!no_target)
            output->AppendText(wxString::Format("%s: %s\n", target.c_str(), txt.c_str()));
        else
            output->AppendText(txt);
    }
    return sound_on;
}

void TellFrame::OnSoundToggle(wxCommandEvent& WXUNUSED(event))
{
    sound_on = !sound_on;

    if (sound_on)
        XRCCTRL(*this, _T("sound"), wxBitmapButton)->SetBitmapLabel(wxBitmap(volume_xpm));
    else
        XRCCTRL(*this, _T("sound"), wxBitmapButton)->SetBitmapLabel(wxBitmap(volume_off_xpm));

#ifdef __WXMSW__
    // For whatever reason on Windows the button image does not update until the focus is lost
    input->SetFocus();
#endif
}

void TellFrame::OnInfo(wxCommandEvent& WXUNUSED(event))
{
    tellHandler->sendStats(target);
}

void TellFrame::OnMatch(wxCommandEvent& WXUNUSED(event))
{
    tellHandler->sendMatch(target);
}


// ------------------------------------------------------------------------
//                          Class TellHandler
// ------------------------------------------------------------------------

TellHandler::TellHandler(IGSMainFrame *parent)
    : parentFrame(parent)
{
    lastTarget = wxEmptyString;
}

TellFrame* TellHandler::getOrCreateTellFrame(const wxString &player, const wxString &name_rank)
{
    wxString target, full_name=wxEmptyString;
    if (player.empty())
    {
        target = wxGetTextFromUser(
            _("Please enter the name of the player to chat with"),
            _("Enter player name"), "", parentFrame);

        if (target.empty())
            return NULL;

        // Try to append the rank if we know it
        full_name = target;
        parentFrame->adjustNameWithRank(full_name);
    }
    else
    {
        target = player;
        full_name = name_rank;
    }

    // Check if this frame already exists
    if (!tellFrames.empty())
    {
        if (tellFrames.find(target) != tellFrames.end())
        {
            TellFrame *tf = tellFrames[target];
            tf->Raise();
            return tf;
        }
    }

    // None found, create a new frame
    if (full_name.empty())
        full_name = target;
    TellFrame *tf = new TellFrame(this, target, full_name, parentFrame);
    tellFrames[target] = tf;
    tf->Show();
    return tf;
}

void TellHandler::notifyTellClosed(const wxString &target_player)
{
    if (tellFrames.find(target_player) == tellFrames.end())
    {
        wxFAIL_MSG(_T("Trying to erase non-existant tell frame"));
        return;
    }
    tellFrames.erase(target_player);
}

bool TellHandler::receiveTell(const wxString &name, const wxString &text, const wxString &name_rank)
{
    return getOrCreateTellFrame(name, name_rank)->appendText(text);
}

void TellHandler::sendTell(const wxString &name, const wxString &text)
{
    if (name.empty() || text.empty())
        return;

    if (parentFrame->isConnected())
    {
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("tell %s %s"), name.c_str(), text.c_str()));
        getOrCreateTellFrame(name)->appendText(
            wxString::Format("%s: %s\n", parentFrame->getIGSConnection()->getLoginName().c_str(), text.c_str()),
            true);
        // Remember last target in case we get the "Cannot find recipient." error
        lastTarget = name;
    }
}

void TellHandler::sendStats(const wxString &name)
{
    if (parentFrame->isConnected() && !name.empty())
    {
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("stats %s"), name.c_str()), IGS_SENDFLAG_STATS);
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("stored %s"), name.c_str()));
    }
}

void TellHandler::sendMatch(const wxString &name)
{
    if (parentFrame->isConnected() && !name.empty())
    {
        // TODO: Calculate white/black and restore setup values
        // This is repetitive with player_table
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
}

void TellHandler::distributeTellError(const wxString &msg)
{
    if (lastTarget.empty())
        return;

    getOrCreateTellFrame(lastTarget)->appendText(msg, true);
}
