/*
 * gtp_setup_dialog.h
 *
 * $Id: gtp_setup_dialog.h,v 1.5 2003/10/02 14:20:45 peter Exp $
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

#ifndef GTP_SETUP_DIALOG_H
#define GTP_SETUP_DIALOG_H

#ifdef __GNUG__
#pragma interface "gtp_setup_dialog.h"
#endif

#include "gtp_config.h"


/**
 * Dialog to create a new GTP game. The user is prompted for the game data like
 * which color the computer takes, board size, handicap, komi, player names etc.
 * The result is stored into the GTPConfig object passed in the constructor, so
 * the calling element can read the values from this pointer. The dialog will
 * only accept "Ok" when the input data is valid.
 * The dialog is created from the XML resource gtp_setup_dialog.xrc
 * @ingroup gtp
 */
class GTPSetupDialog : public wxDialog
{
public:

    /**
     * Constructor.
     * @param parent Parent Window
     * @param game_data Pointer to the GTPConfig object which stores the user input data
     */
    GTPSetupDialog(wxWindow *parent, GTPConfig *game_data);

    /**
     * Transfers values from child controls to data areas specified by their validators.
     * @return False if a transfer failed, else true.
     */
    bool TransferDataFromWindow();

    /** Callback for resume game. */
    void OnResumeGame(wxCommandEvent& WXUNUSED(event));

    /** Callback for Help button. */
    void OnHelp(wxCommandEvent& WXUNUSED(event));

private:
    GTPConfig *data;
    int white, black;

DECLARE_EVENT_TABLE()
};

#endif
