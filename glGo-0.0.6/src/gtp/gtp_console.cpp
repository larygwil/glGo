/*
 * gtp_console.cpp
 *
 * $Id: gtp_console.cpp,v 1.13 2003/10/02 14:17:33 peter Exp $
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
#pragma implementation "gtp_console.h"
#endif

// For compilers that support precompilation, includes "wx/wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include "gtp_console.h"
#include "glGo.h"
#include "htmlhelp_context.h"


BEGIN_EVENT_TABLE(GTPConsole, wxFrame)
    EVT_CLOSE(GTPConsole::OnClose)
    EVT_MENU(ID_CLOSE,  GTPConsole::OnClose)
    EVT_MENU(wxID_HELP, GTPConsole::OnHelp)
    EVT_MENU(wxID_ABOUT, GTPConsole::OnAbout)
    EVT_TEXT_ENTER(ID_TEXTINPUT, GTPConsole::OnInputCommand)
    EVT_CUSTOM(EVT_GTP_COMMAND, -1, GTPConsole::OnOutputCommand)
END_EVENT_TABLE()


GTPConsole::GTPConsole(GTP *gtp, const wxString& title,
                       const wxPoint& pos, const wxSize& size, long style)
    : wxFrame(NULL, -1, title, pos, size, style), gtp(gtp)
{
    wxASSERT(gtp != NULL);

    // set the frame icon
    // SetIcon(wxICON(mondrian));

    // Create a menubar
    wxMenu *menuFile = new wxMenu;
    menuFile->Append(ID_CLOSE, wxString(_("&Close")) + wxString(_T("\tCtrl-W")));
    wxMenu *helpMenu = new wxMenu;
    helpMenu->Append(wxID_HELP, wxString(_("&Manual")) + wxString(_T("\tF1")));
    helpMenu->Append(wxID_ABOUT, _("&About"));
    wxMenuBar *menuBar = new wxMenuBar();
    menuBar->Append(menuFile, _("&File"));
    menuBar->Append(helpMenu, _("&Help"));
    SetMenuBar(menuBar);

    // Create a panel with the input and output textcontrols
    wxPanel *panel = new wxPanel(this, ID_PANEL);
    txtCtrl_Output = new wxTextCtrl(panel, ID_TEXTOUTPUT, wxEmptyString,
                                    wxDefaultPosition, wxDefaultSize,
                                    wxTE_MULTILINE | wxTE_READONLY);
    txtCtrl_Input  = new wxTextCtrl(panel, ID_TEXTINPUT, wxEmptyString,
                                    wxDefaultPosition, wxDefaultSize, wxTE_PROCESS_ENTER);
    // Frame layout
    wxSizer *sizer = new wxBoxSizer(wxVERTICAL);
    sizer->Add(txtCtrl_Output, 1, wxEXPAND | wxALL, 1);
    sizer->SetItemMinSize(txtCtrl_Output, 300, 300);
    sizer->Add(txtCtrl_Input, 0, wxEXPAND | wxALL, 1);
    panel->SetSizer(sizer);
    sizer->Fit(this);

    // Give input field focus
    txtCtrl_Input->SetFocus();
}

void GTPConsole::OnClose(wxCloseEvent& event)
{
    wxLogDebug(_T("OnClose"));

    // Hide it instead of closing, so the output is shown
    if (event.CanVeto())
    {
        wxLogDebug("Hiding console");
        event.Veto();
        Hide();
    }
    else
    {
        wxLogDebug("Destroying console");
        gtp->notifyConsoleClosed();
        Destroy();
    }
}

void GTPConsole::OnHelp(wxCommandEvent& WXUNUSED(event))
{
#ifdef USE_MSHTMLHELP
    wxLogDebug(_T("MSHTMLHELP"));
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_GNUGO);
#else
    wxLogDebug(_T("NO MSHTMLHELP"));
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_GNUGO);
#endif
}

void GTPConsole::OnAbout(wxCommandEvent& WXUNUSED(event))
{
    wxString msg;
    if (!gtp->isConnected())
        msg = _("Not connected to a GTP engine.");
    else
        msg.Printf(_("Connected to %s %s"),
                   (gtp->getGTPName()).c_str(),
                   (gtp->getGTPVersion()).c_str());

    wxMessageBox(msg, _T("About GTP"), wxOK | wxICON_INFORMATION, this);
}

void GTPConsole::OnInputCommand(wxCommandEvent& WXUNUSED(event))
{
    wxLogDebug(wxString::Format(_T("INPUT: %s"), (txtCtrl_Input->GetValue()).c_str()));

    wxString cmd = txtCtrl_Input->GetValue();

    // Ignore if input field was empty and the user just hit return
    if (cmd.IsEmpty())
        return;

    // Notify GTP about the command, so it gets sent to the GTP engine
    wxASSERT(gtp != NULL);
    EventGTPCommand evt(cmd, GTP_COMMAND_SEND);
    wxPostEvent(gtp, evt);

    txtCtrl_Input->Clear();

    // Append to output
    txtCtrl_Output->AppendText(cmd + _T('\n'));
}
