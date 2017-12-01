/*
 * gtp_setup_wizard.cpp
 *
 * $Id: gtp_setup_wizard.cpp,v 1.8 2003/10/02 14:17:33 peter Exp $
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
#pragma implementation "gtp_setup_wizard.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/filename.h>
#include "gtp_setup_wizard.h"
#include "glGo.h"
#include "htmlhelp_context.h"

// XPM bitmap for non-Win32 builds
#if defined(__WXGTK__) || defined(__WXX11__) || defined(__WXMOTIF__) || defined(__WXMAC__)
#include "../images/wiztest.xpm"
#endif


enum
{
    ID_BUTTON,
    ID_TEXTCTRL
};

#ifdef __WXMSW__
    const wxString what = _T("gnugo.exe");
#else
    const wxString what = _T("gnugo");
#endif


BEGIN_EVENT_TABLE(GTPSetupWizard, wxWizard)
    EVT_BUTTON(ID_BUTTON, GTPSetupWizard::OnButton)
    EVT_TEXT(ID_TEXTCTRL, GTPSetupWizard::OnTextInput)
    EVT_TEXT_ENTER(ID_TEXTCTRL, GTPSetupWizard::OnTextEnter)
    EVT_WIZARD_PAGE_CHANGING(-1, GTPSetupWizard::OnWizardPageChanging)
    EVT_WIZARD_HELP(-1, GTPSetupWizard::OnWizardHelp)
END_EVENT_TABLE()


GTPSetupWizard::GTPSetupWizard(wxWindow* parent)
        : wxWizard(parent, -1, _("Locate the GNU Go executable"), wxBITMAP(wiztest))
{
    SetExtraStyle(wxWIZARD_EX_HELPBUTTON);
    GTPBinary = wxEmptyString;
}

wxString GTPSetupWizard::Run()
{
    // Page 1
    wxWizardPageSimple *page1 = new wxWizardPageSimple(this);
    wxBoxSizer *sizer1 = new wxBoxSizer(wxVERTICAL);
    sizer1->Add(
        new wxTextCtrl(page1, -1,
                       wxString::Format(
                           _("\nI could not find the GNU Go program.\n\n"
                             "If you don't have GNU Go yet, please download "
                             "it first from\n"
                             "http://www.gnu.org/software/gnugo/gnugo.html\n"
                             "or\n"
                             "http://panda-igs.joyjoy.net/java/gGo/gnugo.html\n\n"
                             "Extract the archive somewhere and then tell "
                             "me where you put the %s file.\n\n\n"
                             "Click \"Next\" to proceed."), what.c_str()),
                       wxDefaultPosition, wxDefaultSize,
                       wxTE_MULTILINE | wxTE_READONLY),
        0, wxALL | wxEXPAND, 5);
    page1->SetSizer(sizer1);

    // Page 2
    page2 = new wxWizardPageSimple(this);
    wxBoxSizer *sizer2 = new wxBoxSizer(wxVERTICAL);
    txtCtrl = new wxTextCtrl(page2, ID_TEXTCTRL, GTPBinary,
                             wxDefaultPosition, wxDefaultSize, wxTE_PROCESS_ENTER);
#ifdef __WXMSW__
    const wxString examplePath = _T("C:\\Program Files\\GNU Go");
    const wxString exampleFullPath = _T("C:\\Program Files\\GNU Go\\gnugo.exe");
#else
    const wxString examplePath = _T("/usr/local/games");
    const wxString exampleFullPath = _T("/usr/local/games/gnugo");
#endif
    sizer2->Add(
        new wxStaticText(page2, -1,
                         wxString::Format(
                             _("Please enter the path to GNU Go below.\n\n"
                               "For example, if you put \"%s\" into a\n"
                               "directory named \"%s\",\n"
                               "then enter: \"%s\""),
                             what.c_str(), examplePath.c_str(), exampleFullPath.c_str())),
        0, wxALL | wxEXPAND, 10);
    sizer2->Add(txtCtrl, 0, wxALL | wxEXPAND, 10);
    sizer2->Add(
        new wxStaticText(page2, -1,
                         _("Or click this button to find GNU Go on your harddisk:")),
        0, wxALL | wxEXPAND, 10);
    sizer2->Add(
        new wxButton(page2, ID_BUTTON, _("Find GNU Go"), wxDefaultPosition),
        0, wxALL | wxALIGN_CENTER, 10);
    sizer2->Add(
        new wxStaticText(page2, -1,
                         _("When done, click the \"Next\" button below to proceed.")),
        0, wxALL | wxEXPAND, 10);
    page2->SetSizer(sizer2);

    // Page 3
    wxWizardPageSimple *page3 = new wxWizardPageSimple(this);
    wxBoxSizer *sizer3 = new wxBoxSizer(wxVERTICAL);
    sizer3->Add(
        new wxTextCtrl(page3, -1,
                       _("\nAllright, looks we are set.\n\n"
                         "I will save the selected location, so the next time "
                         "I won't ask again, unless you move or delete the "
                         "GNU Go file.\n\n\n"
                         "Click \"Finish\" to start your GNU Go game.\n\n"
                         "Have fun!"),
                       wxDefaultPosition, wxDefaultSize,
                       wxTE_MULTILINE | wxTE_READONLY),
        0, wxALL | wxEXPAND, 5);
    page3->SetSizer(sizer3);

    // Connect pages
    wxWizardPageSimple::Chain(page1, page2);
    wxWizardPageSimple::Chain(page2, page3);

    // Run wizard
    if (RunWizard(page1))
        return GTPBinary;
    else
        return wxEmptyString;
}

void GTPSetupWizard::OnButton(wxCommandEvent& WXUNUSED(event))
{
    wxFileDialog dlg(this, _("Choose GNU Go binary"), "", "", _T("*.exe"));
    if(dlg.ShowModal() == wxID_OK)
        txtCtrl->SetValue(dlg.GetPath());
}

void GTPSetupWizard::OnTextInput(wxCommandEvent& event)
{
    GTPBinary = event.GetString();
}

void GTPSetupWizard::OnTextEnter(wxCommandEvent& WXUNUSED(event))
{
    if (GetCurrentPage() != NULL && GetCurrentPage() == page2)
        ShowPage(page2->GetNext());
}

void GTPSetupWizard::OnWizardPageChanging(wxWizardEvent& event)
{
    if (event.GetPage() == page2 && event.GetDirection() && !validateGTPBinary())
        event.Veto();
}

bool GTPSetupWizard::validateGTPBinary()
{
    if (GTPBinary.empty())
    {
        wxMessageBox(_("Please enter a filename."),
                     _("Information"), wxOK | wxICON_EXCLAMATION);
        return false;
    }
    if (!wxFileExists(GTPBinary))
    {
        wxMessageBox(wxString::Format(_("%s is not a valid file."), GTPBinary.c_str()),
                     _("Information"), wxOK | wxICON_EXCLAMATION);
        return false;
    }
    wxFileName fn(GTPBinary);
    wxString name = fn.GetName();
    if (name.Cmp(_T("gnugo")) &&
        wxMessageBox(wxString::Format(_("Umm... this file is not called \"%s\".\n"
                                        "Are you sure this is the right file?"), what.c_str()),
                     _("Confirm"),
                     wxYES_NO | wxICON_QUESTION,
                     this) == wxNO)
        return false;

    return true;
}

void GTPSetupWizard::OnWizardHelp(wxCommandEvent& WXUNUSED(event))
{
#ifdef USE_MSHTMLHELP
    wxLogDebug(_T("MSHTMLHELP"));
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_GNUGO);
#else
    wxLogDebug(_T("NO MSHTMLHELP"));
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_GNUGO);
#endif
}
