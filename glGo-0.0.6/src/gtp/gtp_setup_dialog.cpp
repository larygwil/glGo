/*
 * gtp_setup_dialog.cpp
 *
 * $Id: gtp_setup_dialog.cpp,v 1.11 2003/10/31 21:59:08 peter Exp $
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
#pragma implementation "gtp_setup_dialog.h"
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
#include <wx/config.h>
#include <wx/spinctrl.h>
#include <wx/valgen.h>
#include "gtp_setup_dialog.h"
#include "validators.h"
#include "glGo.h"
#include "htmlhelp_context.h"


BEGIN_EVENT_TABLE(GTPSetupDialog, wxDialog)
    EVT_BUTTON(XRCID(_T("resume_game")), GTPSetupDialog::OnResumeGame)
    EVT_BUTTON(XRCID(_T("help")), GTPSetupDialog::OnHelp)
END_EVENT_TABLE()


GTPSetupDialog::GTPSetupDialog(wxWindow *parent, GTPConfig *game_data)
{
    wxASSERT(game_data != NULL);
    data = game_data;

    SetExtraStyle(wxWS_EX_VALIDATE_RECURSIVELY);

    // Load data from config, if we have it
    int i = 0;
    if (wxConfig::Get()->Read(_T("GTP/White"), &i))
        data->white = i != 0 ? GTP_COMPUTER : GTP_HUMAN;
    if (wxConfig::Get()->Read(_T("GTP/Black"), &i))
        data->black = i != 0 ? GTP_COMPUTER : GTP_HUMAN;
    if (wxConfig::Get()->Read(_T("GTP/Size"), &i))
        data->board_size = i;
    if (wxConfig::Get()->Read(_T("GTP/Handicap"), &i))
        data->handicap = i;
    /* if (wxConfig::Get()->Read("GTP/Komi", &i))
        data->komi = i; */
    if (wxConfig::Get()->Read(_T("GTP/Level"), &i))
        data->level= i;

    // Load dialog XML resource
    wxXmlResource::Get()->LoadDialog(this, parent, _T("gtp_setup_dialog"));

    // Set validators and transfer data to dialog controls
    white = data->white;  // We cannot cast GTPPlayer* to int*
    black = data->black;
    XRCCTRL(*this, _T("white"), wxChoice)->SetValidator(wxGenericValidator(&white));
    XRCCTRL(*this, _T("black"), wxChoice)->SetValidator(wxGenericValidator(&black));
    XRCCTRL(*this, _T("size"), wxSpinCtrl)->SetValidator(BoardSizeValidator(&data->board_size));
    XRCCTRL(*this, _T("handicap"), wxSpinCtrl)->SetValidator(HandicapValidator(&data->handicap));
    XRCCTRL(*this, _T("komi"), wxTextCtrl)->SetValidator(KomiValidator(&data->komi));
    XRCCTRL(*this, _T("level"), wxSlider)->SetValidator(wxGenericValidator(&data->level));
}

bool GTPSetupDialog::TransferDataFromWindow()
{
    bool res = wxWindow::TransferDataFromWindow();

    // Transfer this in any case for game resume
    data->white = white != 0 ? GTP_COMPUTER : GTP_HUMAN;
    data->black = black != 0 ? GTP_COMPUTER : GTP_HUMAN;

    if (!res)
        return false;

    // Save current settings
    wxConfig::Get()->Write(_T("GTP/White"), data->white);
    wxConfig::Get()->Write(_T("GTP/Black"), data->black);
    wxConfig::Get()->Write(_T("GTP/Size"), data->board_size);
    wxConfig::Get()->Write(_T("GTP/Handicap"), data->handicap);
    // wxConfig::Get()->Write("GTP/Komi", data->komi);
    wxConfig::Get()->Write(_T("GTP/Level"), data->level);

    return true;
}

void GTPSetupDialog::OnResumeGame(wxCommandEvent& WXUNUSED(event))
{
    // Transfer game data. Ignore validation, as we reread the data from the SGF anyways
    TransferDataFromWindow();

    // Open a Fileselector dialog to get the filebane
    wxString lastDir;
#ifndef __WXMSW__
    // On Linux save and restore the last directory. The Windows filedialog does that already.
    wxConfig::Get()->Read(_T("SGF/LastSGFDir"), &lastDir);
#else
    lastDir = "";
#endif

    // Show dialog to select a file
    wxString filename =
        wxFileSelector(_("Load SGF game to resume playing"), lastDir, "", _T("sgf"),
                       _("SGF files (*.sgf)|*.sgf|MGT files (*.mgt)|*.mgt|All files (*)|*"),
                       wxOPEN | wxFILE_MUST_EXIST, this);

    // User hit Cancel in the dialog
    if (filename.empty())
        return;

#ifdef __WXMSW__
    // Windows whitespace crap
    if (filename.Find(" ") != -1)
    {
        wxMessageBox(_("The filename contains a space.\n\n"
                       "GNU Go will not be able to load this file. Please copy\n"
                       "the SGF file into a directory without spaces, like\n"
                       "\"C:\\Go\\mygame.sgf\" and try again!\n\n"
                       "Sorry, but there is nothing I can do about this."),
                     _("Warning"), wxOK | wxICON_WARNING, this);
        return;
    }
#endif

#ifndef __WXMSW__
    // Store directory in config
    wxFileName fn(filename);
    wxConfig::Get()->Write(_T("SGF/LastSGFDir"), fn.GetPath());
#endif

    // Store filename in GTPConfig, will be loaded later
    data->resumeFileName = filename;

    // Close this dialog. SGF will be loaded in GTPGame
    EndModal(wxID_OK);
}

void GTPSetupDialog::OnHelp(wxCommandEvent& WXUNUSED(event))
{
#ifdef USE_MSHTMLHELP
    wxLogDebug(_T("MSHTMLHELP"));
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_GNUGO);
#else
    wxLogDebug(_T("NO MSHTMLHELP"));
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_GNUGO);
#endif
}
