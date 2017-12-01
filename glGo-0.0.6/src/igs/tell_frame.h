/*
 * tell_frame.h
 *
 * $Id: tell_frame.h,v 1.8 2003/10/22 02:54:50 peter Exp $
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

/**
 * @file
 * ingroup igsgui
 *
 * This file contains the TellFrame and TellHandler classes, which
 * represent the current chat subsystem. TellFrame is the user interface
 * while TellHandler creates and manages the different TellFrames and
 * distributes incoming tells to the right frame. The goal is to be
 * able to exchange the GUI to offer a more sophisticated interface
 * similar to gGo/Java.
 */

#ifndef TELL_FRAME_H
#define TELL_FRAME_H

#ifdef __GNUG__
#pragma interface "tell_frame.h"
#endif

#include <wx/hashmap.h>


class IGSMainFrame;
class TellHandler;


// ------------------------------------------------------------------------
//                          Class TellFrame
// ------------------------------------------------------------------------


/**
 * IGS tell frame. This is the user interface to read and input tells.
 * @ingroup igsgui
 */
class TellFrame : public wxFrame
{
public:
    TellFrame(TellHandler *handler, const wxString &target_player,
              const wxString &name_rank, IGSMainFrame *parent);
    ~TellFrame();
    bool appendText(const wxString &txt, bool no_target=false);
    void OnCommandEnter(wxCommandEvent& WXUNUSED(event));
    void OnClearButton(wxCommandEvent& WXUNUSED(event));
    void OnCloseButton(wxCommandEvent& WXUNUSED(event));
    void OnFont(wxCommandEvent& WXUNUSED(event));
    void OnSoundToggle(wxCommandEvent& WXUNUSED(event));
    void OnInfo(wxCommandEvent& WXUNUSED(event));
    void OnMatch(wxCommandEvent& WXUNUSED(event));

private:
    TellHandler *tellHandler;
    wxString target;
    wxTextCtrl *input, *output;
    bool sound_on;

DECLARE_EVENT_TABLE()
};



// ------------------------------------------------------------------------
//                          Class TellHandler
// ------------------------------------------------------------------------

/** Array of of currently open TellFrames */
WX_DECLARE_STRING_HASH_MAP(TellFrame*, TellFrames);

/**
 * Creates and manages TellFrames. The TellHandler also distributes incoming
 * tells to the right frame, in case none for that particular player is already
 * open, it creates one.
 * @ingroup igsgui
 */
class TellHandler
{
public:
    TellHandler(IGSMainFrame *parent);
    TellFrame* getOrCreateTellFrame(const wxString &player = wxEmptyString,
                                    const wxString &name_rank = wxEmptyString);
    void notifyTellClosed(const wxString &target_player);
    bool receiveTell(const wxString &name, const wxString &text,
					 const wxString &name_rank = wxEmptyString);
    void sendTell(const wxString &name, const wxString &text);
    void sendStats(const wxString &name);
    /** @todo This is repetitive */
    void sendMatch(const wxString &name);
    void distributeTellError(const wxString &msg);

private:
    IGSMainFrame *parentFrame;
    TellFrames tellFrames;
    wxString lastTarget;
};

#endif
