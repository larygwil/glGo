/*
 * gtp_setup_wizard.h
 *
 * $Id: gtp_setup_wizard.h,v 1.2 2003/10/02 14:17:33 peter Exp $
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

#ifndef GTP_SETUP_WIZARD
#define GTP_SETUP_WIZARD

#ifdef __GNUG__
#pragma interface "gtp_setup_wizard.h"
#endif

#include <wx/wizard.h>

/**
 * A wizard to let the user enter the path to GNU Go.
 * The user will be given advice what to do and guided through
 * the setup. Hopefully this is easy enough for novice users.\n
 *
 * Create and run this wizard like:
 *
 * @code
 * GTPSetupWizard *wizard = new GTPSetupWizard(this);
 * gtp_path = wizard->Run();
 * wizard->Destroy();
 * if (!gtp_path.empty())
 *     ...
 * @endcode
 *
 * The return value of Run() contains the gtp full binary path
 * or wxEmptyString if nothing was found.
 *
 * @ingroup gtp
 */
class GTPSetupWizard : public wxWizard
{
public:
    /** Constructor */
    GTPSetupWizard(wxWindow* parent);

    /**
     * Main entry point to this wizard.
     * This function will create the pages and call wxWizard::RunWizard()
     * @return String containing the GNU Go path, or wxEmptyString if nothing was found
     */
    wxString Run();

    void OnButton(wxCommandEvent& WXUNUSED(event));
    void OnTextInput(wxCommandEvent& event);
    void OnTextEnter(wxCommandEvent& WXUNUSED(event));
    void OnWizardPageChanging(wxWizardEvent& event);
    void OnWizardHelp(wxCommandEvent& WXUNUSED(event));

private:
    bool validateGTPBinary();

    wxWizardPageSimple *page2;
    wxString GTPBinary;
    wxTextCtrl *txtCtrl;

DECLARE_EVENT_TABLE()
};

#endif
