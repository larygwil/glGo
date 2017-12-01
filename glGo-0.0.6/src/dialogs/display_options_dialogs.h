/*
 * display_options_dialogs.h
 *
 * $Id: display_options_dialogs.h,v 1.3 2003/10/22 23:59:45 peter Exp $
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

#ifndef DISPLAY_OPTIONS_DIALOGS_H
#define DISPLAY_OPTIONS_DIALOGS_H

#ifdef __GNUG__
#pragma interface "display_options_dialogs.h"
#endif

#include "utils/utils.h"

class GLBoard;
class Board;

/**
 * @file
 *
 * This file contains the dialogs for board display configuration. As there
 * are different settings for OpenGL and SDL, there are two separate dialogs
 * for this task.
 * @ingroup userinterface
 */


// ------------------------------------------------------------------------
//                          Class OGLOptionsDialog
// ------------------------------------------------------------------------

/**
 * Dialog to configure the OpenGL display options.
 * The dialog is created from the XML resource display_options_dialog.xrc
 * @ingroup userinterface
 * @todo Implement background image checkbox
 */
class OGLOptionsDialog : public wxDialog
{
public:
    /**
     * Constructor.
     * @param parent Pointer to the parent window
     * @param board Pointer to the OpenGL board embedded in the parent window.
     */
    OGLOptionsDialog(wxWindow *parent, GLBoard *board);

    /**
     * Transfers values from child controls to data areas specified by their validators.
     * @return False if a transfer failed, else true.
     */
    bool TransferDataFromWindow();

    /** Callback for Help button. */
    void OnHelp(wxCommandEvent& WXUNUSED(event));

    /** Select background color. */
    void OnSetBackgroundColor(wxCommandEvent& WXUNUSED(event));

private:
    OGLConfig config;
    GLBoard *glBoard;
    // bool back_image;
    wxColour back_color;

DECLARE_EVENT_TABLE()
};


// ------------------------------------------------------------------------
//                          Class SDLOptionsDialog
// ------------------------------------------------------------------------

/**
 * Dialog to configure the SDL display options.
 * The dialog is created from the XML resource display_options_dialog.xrc
 * The user can set the background.
 * @ingroup userinterface
 */
class SDLOptionsDialog : public wxDialog
{
public:
    /**
     * Constructor.
     * @param parent Pointer to the parent window
     * @param board Pointer to the board embedded in the parent window.
     */
    SDLOptionsDialog(wxWindow *parent, Board *board);

    /** Initialize the dialog. Must be called after creation and before ShowModal() */
    virtual void initDialog();

    /**
     * Transfers values from child controls to data areas specified by their validators.
     * @return False if a transfer failed, else true.
     */
    virtual bool TransferDataFromWindow();

    /** Callback for Help button. */
    void OnHelp(wxCommandEvent& WXUNUSED(event));

    /** Select background color. */
    void OnSetBackgroundColor(wxCommandEvent& WXUNUSED(event));

private:
    wxWindow *parent;
    Board *board;
    bool back_image;
    wxColour back_color;
    bool scaled_font;

DECLARE_EVENT_TABLE()
};

#endif
