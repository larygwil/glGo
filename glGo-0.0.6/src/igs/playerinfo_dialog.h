/*
 * playerinfo_dialog.h
 *
 * $Id: playerinfo_dialog.h,v 1.6 2003/11/17 12:48:14 peter Exp $
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

#ifndef PLAYERINFO_DIALOG_H
#define PLAYERINFO_DIALOG_H

#ifdef __GNUG__
#pragma interface "playerinfo_dialog.h"
#endif

#include "igs_player.h"

/**
 * Number of custom player flags.
 * This has to be the same value as in playerdb.glGo.__init__
 * @ingroup igsgui
 */
#define NUMBER_CUSTOM_FLAGS 5

class IGSMainFrame;

/**
 * Dialog displaying player information and statistics.
 * In fact this is a subclass of wxFrame. Being a wxDialog results in some
 * focus problems with several of these dialogs open, which is quite inconvinient.
 * @ingroup igsgui
 * @todo Implement the other two notebook panels like in gGo
 */
class PlayerinfoDialog : public wxFrame
{
public:
    PlayerinfoDialog(IGSMainFrame *parent, const PlayerInfo &playerInfo);
    void OnClose(wxCommandEvent& WXUNUSED(event));
    void OnChat(wxCommandEvent& WXUNUSED(event));
    /** @todo This is repetitive code */
    void OnMatch(wxCommandEvent& WXUNUSED(event));
    void OnStatus(wxCommandEvent& WXUNUSED(event));

private:
    IGSMainFrame *parentFrame;
    PlayerInfo info;
    wxCheckBox *cf_cb[NUMBER_CUSTOM_FLAGS];

DECLARE_EVENT_TABLE()
};

#endif
