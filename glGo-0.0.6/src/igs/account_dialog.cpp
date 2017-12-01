/*
 * account_dialog.cpp
 *
 * $Id: account_dialog.cpp,v 1.3 2003/10/02 14:18:40 peter Exp $
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
#pragma implementation "account_dialog.h"
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
#include "account_dialog.h"
#include "glGo.h"
#include "htmlhelp_context.h"


BEGIN_EVENT_TABLE(AccountDialog, wxDialog)
    EVT_BUTTON(wxID_HELP, AccountDialog::OnHelp)
END_EVENT_TABLE()


AccountDialog::AccountDialog(wxWindow *parent)
{
    SetExtraStyle(wxWS_EX_VALIDATE_RECURSIVELY);

    // Load dialog XML resource
    wxXmlResource::Get()->LoadDialog(this, parent, _T("igs_account_dialog"));

    loadIGSSettings(settings);

    // Set validators and transfer data to dialog controls
    XRCCTRL(*this, _T("loginname"), wxTextCtrl)->SetValidator(wxGenericValidator(&settings.loginname));
    XRCCTRL(*this, _T("password"), wxTextCtrl)->SetValidator(wxGenericValidator(&settings.password));
    XRCCTRL(*this, _T("autoconnect"), wxCheckBox)->SetValidator(wxGenericValidator(&settings.autoconnect));
}

bool AccountDialog::TransferDataFromWindow()
{
    if (!wxWindow::TransferDataFromWindow())
        return false;

    // Save data in config
    saveIGSSettings(settings);

    return true;
}

void AccountDialog::OnHelp(wxCommandEvent& WXUNUSED(event))
{
    // TODO: Write a account chapter and point the help viewer there
#ifdef USE_MSHTMLHELP
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_INDEX);
#else
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_INDEX);
#endif
}
