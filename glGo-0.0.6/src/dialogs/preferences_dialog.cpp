/*
 * preferences_dialog.cpp
 *
 * $Id: preferences_dialog.cpp,v 1.21 2003/11/23 14:36:10 peter Exp $
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
#pragma implementation "preferences_dialog.h"
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
#include <wx/spinctrl.h>
#include <wx/tooltip.h>
#include <wx/config.h>
#include "preferences_dialog.h"
#include "glGo.h"
#include "sound.h"
#include "htmlhelp_context.h"


BEGIN_EVENT_TABLE(PreferencesDialog, wxDialog)
    EVT_BUTTON(wxID_HELP, PreferencesDialog::OnHelp)
    EVT_BUTTON(XRCID("select_autosave_own"), PreferencesDialog::OnSelectAutosaveOwn)
    EVT_BUTTON(XRCID("select_autosave_observed"), PreferencesDialog::OnSelectAutosaveObserved)
END_EVENT_TABLE()


PreferencesDialog::PreferencesDialog(wxWindow *parent)
{
    SetExtraStyle(wxWS_EX_VALIDATE_RECURSIVELY);

    // Load dialog XML resource
    wxXmlResource::Get()->LoadDialog(this, parent, _T("preferences_dialog"));

    loadSettings(settings);
    loadIGSSettings(igs_settings);

    old_language = settings.language;
    old_sound_system = settings.sound_system;

    // Set validators and transfer data to dialog controls
    XRCCTRL(*this, _T("language"), wxChoice)->SetValidator(wxGenericValidator(&settings.language));
    XRCCTRL(*this, _T("board_type"), wxChoice)->SetValidator(wxGenericValidator(&settings.board_display_type));
    XRCCTRL(*this, _T("autohide_startscreen"), wxCheckBox)->SetValidator(wxGenericValidator(&settings.autohide));
#ifdef __WXMSW__
    XRCCTRL(*this, _T("minimize_to_tray"), wxCheckBox)->SetValidator(wxGenericValidator(&settings.minimize_to_tray));
#endif
    XRCCTRL(*this, _T("enable_tooltips"), wxCheckBox)->SetValidator(wxGenericValidator(&settings.tooltips));
    XRCCTRL(*this, _T("localserver"), wxCheckBox)->SetValidator(wxGenericValidator(&settings.localserver));
    XRCCTRL(*this, _T("global_sound"), wxCheckBox)->SetValidator(wxGenericValidator(&settings.global_sound));
    XRCCTRL(*this, _T("sound_system"), wxChoice)->SetValidator(wxGenericValidator(&settings.sound_system));
    XRCCTRL(*this, _T("shouts_in_terminal"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.shouts_in_terminal));
    XRCCTRL(*this, _T("skip_guests"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.skip_guests));
    XRCCTRL(*this, _T("show_obs_msgbox"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.show_obs_msgbox));
    XRCCTRL(*this, _T("ayt_timer"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.ayt_timer));
    XRCCTRL(*this, _T("chat_sound"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.chat_sound));
    XRCCTRL(*this, _T("match_sound"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.match_sound));
    XRCCTRL(*this, _T("all_friends"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.all_friends));
    XRCCTRL(*this, _T("timewarn_sound"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.timewarn_sound));
    XRCCTRL(*this, _T("timewarn_threshold"), wxSpinCtrl)->SetValidator(wxGenericValidator(&igs_settings.timewarn_threshold));
    XRCCTRL(*this, _T("autosave_own"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.autosave_own));
    XRCCTRL(*this, _T("autosave_observed"), wxCheckBox)->SetValidator(wxGenericValidator(&igs_settings.autosave_observed));

    if (!wxGetApp().IsSoundEnabled())
    {
        XRCCTRL(*this, _T("global_sound"), wxCheckBox)->Enable(false);
        XRCCTRL(*this, _T("sound_system"), wxChoice)->Enable(false);
    }
}

bool PreferencesDialog::TransferDataFromWindow()
{
    if (!wxWindow::TransferDataFromWindow())
        return false;

    // Tooltips
    wxToolTip::Enable(settings.tooltips);

    // Notify user about language update
    if (settings.language != old_language)
    {
        wxMessageBox(_("You have changed the language.\n"
                       "Some settings might change, but you should restart\n"
                       "glGo to update everything properly."),
                     _("Information"), wxOK | wxICON_INFORMATION, this);
        old_language = settings.language;  // MainFrame will check this
    }
    else
        old_language = -1;

    // Try reloading sound system if it changed
    if (settings.sound_system != old_sound_system)
    {
        LOG_SOUND(_T("Sound system changed. Trying to reload."));
        Sound_quit_library();
        wxSleep(1);
        wxMessageBox(_("I have unloaded the old sound system.\n\n"
                       "I will wait some seconds and then try to reload sound. This might work or not."),
                     _("Information"), wxOK | wxICON_INFORMATION, this);
        if (!wxGetApp().loadSoundSystem(settings.sound_system))
        {
            LOG_SOUND(_T("Reload failed."));
            wxMessageBox(
                wxString::Format(
                    _("Failed to load %s sound system.\n\n"
                      "You might try to restart glGo, often changing the mixer device quickly does not work well.\n"
                      "I keep the new sound system in the preferences, but global sound is now disabled. Come back\n"
                      "here after a restart and enable sound again with the new system. If it then fails again, sound\n"
                      "might not be working properly with your hardware."),
                    (settings.sound_system == SOUND_SYSTEM_OAL ? _("OpenAL") :
                     settings.sound_system == SOUND_SYSTEM_SDL ? _("SDL Mixer") : _T("INVALID"))),
                _("Error"), wxOK | wxICON_ERROR, this);
            settings.global_sound = false;
            wxGetApp().DisableSound();
        }
        else
            LOG_SOUND(_T("Reloaded successfull."));
    }

    // Save data in config
    saveSettings(settings);
    saveIGSSettings(igs_settings);

    return true;
}

void PreferencesDialog::OnHelp(wxCommandEvent& WXUNUSED(event))
{
#ifdef USE_MSHTMLHELP
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_OPTIONS);
#else
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_OPTIONS);
#endif
}

void PreferencesDialog::OnSelectAutosaveOwn(wxCommandEvent& WXUNUSED(event))
{
    wxString old_dir;
    wxConfig::Get()->Read(_T("IGS/AutosaveOwnDir"), &old_dir, "");
    const wxString &dir = wxDirSelector(_("Choose a folder for own games"), old_dir);
    if (!dir.empty())
        wxConfig::Get()->Write(_T("IGS/AutosaveOwnDir"), dir);
}

void PreferencesDialog::OnSelectAutosaveObserved(wxCommandEvent& WXUNUSED(event))
{
    wxString old_dir;
    wxConfig::Get()->Read(_T("IGS/AutosaveObsDir"), &old_dir, "");
    const wxString &dir = wxDirSelector(_("Choose a folder for observed games"), old_dir);
    if (!dir.empty())
        wxConfig::Get()->Write(_T("IGS/AutosaveObsDir"), dir);
}
