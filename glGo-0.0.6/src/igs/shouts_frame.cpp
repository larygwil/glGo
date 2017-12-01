/*
 * shouts_frame.cpp
 *
 * $Id: shouts_frame.cpp,v 1.3 2003/10/19 06:50:59 peter Exp $
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
#pragma implementation "shouts_frame.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/config.h>
#include <wx/xrc/xmlres.h>
#include <wx/fontdlg.h>
#include "shouts_frame.h"
#include "igs_mainframe.h"
#include "igs_connection.h"
#include "inputctrl.h"

// Icons
#ifndef __WXMSW__
#include "images/32green.xpm"
#endif


BEGIN_EVENT_TABLE(ShoutsFrame, wxFrame)
    EVT_CLOSE(ShoutsFrame::OnClose)
    EVT_BUTTON(XRCID(_T("clear")), ShoutsFrame::OnClear)
    EVT_BUTTON(XRCID(_T("close")), ShoutsFrame::OnClose)
    EVT_BUTTON(XRCID(_T("font")), ShoutsFrame::OnFont)
    EVT_TEXT_ENTER(XRCID(_T("input")), ShoutsFrame::OnCommandEnter)
END_EVENT_TABLE()


ShoutsFrame::ShoutsFrame(IGSMainFrame *parent)
    : parentFrame(parent)
{
    wxASSERT(parent != NULL);

    // Load XRC resources
    wxXmlResource::Get()->LoadFrame(this, static_cast<wxWindow*>(parent), _T("igs_shouts_frame"));

    input = new InputCtrl(this, -1);
    wxASSERT(input != NULL);
    wxXmlResource::Get()->AttachUnknownControl(_T("input"), input);

    output = XRCCTRL(*this, _T("output"), wxTextCtrl);
    wxASSERT(output != NULL);

    // Restore output font
    wxFont font;
    wxString fontInfoStr;
    if (wxConfig::Get()->Read(_T("IGS/ShoutsFont"), &fontInfoStr))
        font = wxFont(fontInfoStr);
    else
        font = wxFont(9, wxMODERN, wxNORMAL, wxNORMAL);  // Default font
    wxTextAttr style = wxTextAttr(*wxBLACK, wxNullColour, font);
    output->SetDefaultStyle(style);

    input->SetFocus();

    // Assign icon
    SetIcon(wxICON(green32));
}

ShoutsFrame::~ShoutsFrame()
{
    parentFrame->notifyShoutsClosed();

    // Store position and size in config
    int x, y;
    GetSize(&x, &y);
    if (x > 100 && y > 30)  // Minimized?
    {
        wxConfig::Get()->Write(_T("IGS/Frames/ShoutsSizeX"), x);
        wxConfig::Get()->Write(_T("IGS/Frames/ShoutsSizeY"), y);
    }
    GetPosition(&x, &y);
    if (x > 0 && y > 0)
    {
        wxConfig::Get()->Write(_T("IGS/Frames/ShoutsPosX"), x);
        wxConfig::Get()->Write(_T("IGS/Frames/ShoutsPosY"), y);
    }
}

void ShoutsFrame::OnClose(wxCloseEvent& event)
{
    if (event.CanVeto())
    {
        event.Veto();
        parentFrame->notifyShoutsMinimized();
        Show(false);
        return;
    }

    // We cannot veto, bad luck
    parentFrame->notifyShoutsClosed();
    Destroy();
}

void ShoutsFrame::OnClear(wxCommandEvent& WXUNUSED(event))
{
    output->Clear();
}

void ShoutsFrame::OnFont(wxCommandEvent& WXUNUSED(event))
{
    wxFont font = wxGetFontFromUser(this, output->GetDefaultStyle().GetFont());
    if (font.Ok())
    {
        wxTextAttr style(*wxBLACK, wxNullColour, font);
        output->SetStyle(0, output->GetLastPosition(), style);
        output->SetDefaultStyle(style);
        output->Refresh();

        // Save native font to config
        wxConfig::Get()->Write(_T("IGS/ShoutsFont"), font.GetNativeFontInfoDesc());
    }
}

void ShoutsFrame::OnCommandEnter(wxCommandEvent& WXUNUSED(event))
{
    wxString cmd = input->GetValue();
    if (cmd.empty())
        return;
    input->Clear();

    if (parentFrame->isConnected())
    {
        parentFrame->getIGSConnection()->sendCommand(wxString::Format(_T("shout %s"), cmd.c_str()));
        receiveShout(
            wxString::Format("%s: %s\n", parentFrame->getIGSConnection()->getLoginName().c_str(), cmd.c_str()));
    }
}

void ShoutsFrame::receiveShout(const wxString &txt)
{
    output->AppendText(txt);
}

void ShoutsFrame::SetInputFocus()
{
    input->SetFocus();
}
