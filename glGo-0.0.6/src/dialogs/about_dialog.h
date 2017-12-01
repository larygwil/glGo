/*
 * about_dialog.h
 *
 * $Id: about_dialog.h,v 1.4 2003/10/02 14:16:34 peter Exp $
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

#ifndef ABOUT_DIALOG_H
#define ABOUT_DIALOG_H

#ifdef __GNUG__
#pragma interface "about_dialog.h"
#endif


/**
 * About dialog.
 * The dialog is created from the XML resource about_dialog.xrc
 * @ingroup userinterface
 */
class AboutDialog : public wxDialog
{
public:

    /**
     * Constructor.
     * @param parent Parent Window
     * @param about String for first tab: About glGo
     * @param ogl String for second tab: OpenGL
     * @param oal String for third tab: OpenAL
     */
    AboutDialog(wxWindow *parent,
                const wxString &about,
                const wxString &ogl,
                const wxString &oal);

    /** Callback for Update button. */
    void OnUpdate(wxCommandEvent& WXUNUSED(event));

    /** Callback for Webpage button. */
    void OnWebpage(wxCommandEvent& WXUNUSED(event));

private:

DECLARE_EVENT_TABLE()
};

#endif
