/*
 * shouts_frame.h
 *
 * $Id: shouts_frame.h,v 1.2 2003/10/02 14:18:40 peter Exp $
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

#ifndef SHOUTS_FRAME_H
#define SHOUTS_FRAME_H

#ifdef __GNUG__
#pragma interface "shouts_frame.h"
#endif


class IGSMainFrame;

/**
 * Frame for shout output.
 * @ingroup igsgui
 */
class ShoutsFrame : public wxFrame
{
public:
    /** Constructor */
    ShoutsFrame(IGSMainFrame *parent);

    /** Destructor */
    virtual ~ShoutsFrame();

    /** Callback for close event and Close button */
    void OnClose(wxCloseEvent& event);

    /** Callback for Clear button */
    void OnClear(wxCommandEvent& WXUNUSED(event));

    /** Callback for Fonts button */
    void OnFont(wxCommandEvent& WXUNUSED(event));

    /** Callback for input enter */
    void OnCommandEnter(wxCommandEvent& WXUNUSED(event));

    /** Received a shout, append to output textcontrol */
    void receiveShout(const wxString &txt);

    /** Focus input textcontrol */
    void SetInputFocus();

private:
    IGSMainFrame *parentFrame;
    wxTextCtrl *input, *output;

DECLARE_EVENT_TABLE()
};

#endif
