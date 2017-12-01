/*
 * playerdb_gui.h
 *
 * $Id: playerdb_gui.h,v 1.2 2003/11/12 17:24:56 peter Exp $
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

#ifndef PLAYERDB_GUI_H
#define PLAYERDB_GUI_H

#ifdef __GNUG__
#pragma interface "playerdb_gui.h"
#endif


/**
 * Player management dialog.
 * This frame offers the interface to edit the friends and bozo list. The database
 * is handled by the embedded Python module.
 * @ingroup igsgui
 */
class PlayerDBGui : public wxFrame
{
public:
    PlayerDBGui(IGSMainFrame *parent);
    ~PlayerDBGui();
    void OnClose(wxCloseEvent& WXUNUSED(event));
    void OnAddFriend(wxCommandEvent& WXUNUSED(event));
    void OnRemoveFriend(wxCommandEvent& WXUNUSED(event));
    void OnAddBozo(wxCommandEvent& WXUNUSED(event));
    void OnRemoveBozo(wxCommandEvent& WXUNUSED(event));
    void OnHelp(wxCommandEvent& WXUNUSED(event));

private:
    IGSMainFrame *parentFrame;
    wxListBox *friends_list, *bozo_list;

DECLARE_EVENT_TABLE()
};

#endif
