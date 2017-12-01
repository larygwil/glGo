/*
 * preferences_dialog.h
 *
 * $Id: preferences_dialog.h,v 1.8 2003/11/19 14:29:23 peter Exp $
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

#ifndef PREFERENCES_DIALOG_H
#define PREFERENCES_DIALOG_H

#ifdef __GNUG__
#pragma interface "preferences_dialog.h"
#endif

#include "utils/utils.h"


/**
 * Main application settings dialog.
 * @ingroup userinterface
 */
class PreferencesDialog : public wxDialog
{
public:

    /** Constructor */
    PreferencesDialog(wxWindow *parent);

    /**
     * Transfers values from child controls to data areas specified by their validators.
     * @return False if a transfer failed, else true.
     */
    bool TransferDataFromWindow();

    /** Callback for Help button */
    void OnHelp(wxCommandEvent& WXUNUSED(event));

    /** Callback for autosave-own-directory */
    void OnSelectAutosaveOwn(wxCommandEvent& WXUNUSED(event));

    /** Callback for autosave-observed-directory */
    void OnSelectAutosaveObserved(wxCommandEvent& WXUNUSED(event));

    /**
     * Check if the language was changed. Called from Mainframe when this dialog exits with wxID_OK.
     * @return Returns the content of settings.language or -1 if language was not changed
     */
    int languageChanged() const { return old_language; }

private:
    Settings settings;
    IGSSettings igs_settings;
    int old_language, old_sound_system;

DECLARE_EVENT_TABLE()
};

#endif
