/*
 * gtp_console.h
 *
 * $Id: gtp_console.h,v 1.7 2003/10/02 14:17:33 peter Exp $
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

#ifndef GTP_CONSOLE_H
#define GTP_CONSOLE_H

#ifdef __GNUG__
#pragma interface "gtp_console.h"
#endif

#include "gtp.h"


/**
 * A simple console frame to display the GTP communication.
 * This should not be opened directly by the framework but requested
 * with a call to GTP::OpenConsole()
 * @ingroup gtp
 */
class GTPConsole : public wxFrame
{
public:
    /** Constructor */
    GTPConsole(GTP *gtp, const wxString& title,
               const wxPoint& pos = wxDefaultPosition, const wxSize& size = wxDefaultSize,
               long style = wxDEFAULT_FRAME_STYLE);

    /** Callback for File-Close and close events */
    void OnClose(wxCloseEvent& event);

    /** Callback for Help-Manual */
    void OnHelp(wxCommandEvent& WXUNUSED(event));

    /** Callback for Help-About */
    void OnAbout(wxCommandEvent& WXUNUSED(event));

    /** Callback for input in the input command textcontrol */
    void OnInputCommand(wxCommandEvent& WXUNUSED(event));

    /** Callback when GTP output is received, display it in the output textcontrol */
    void OnOutputCommand(EventGTPCommand &event) { txtCtrl_Output->AppendText(event.getCommand() + _T('\n')); }

private:
    GTP *gtp;
    wxTextCtrl *txtCtrl_Input, *txtCtrl_Output;

DECLARE_EVENT_TABLE()
};

#endif
