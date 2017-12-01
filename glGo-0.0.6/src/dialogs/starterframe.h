/*
 * starterframe.h
 *
 * $Id: starterframe.h,v 1.7 2003/10/14 15:43:14 peter Exp $
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

#ifndef STARTERFRAME_H
#define STARTERFRAME_H

#ifdef __GNUG__
#pragma interface "starterframe.h"
#endif

#ifdef __WXMSW__
#include <wx/taskbar.h>
class StarterIcon;
#endif


// -----------------------------------------------------------------------
//                          Class StarterFrame
// -----------------------------------------------------------------------

/**
 * The start frame. This frame opens when the application is started and is
 * the wxApp toplevel frame. It contains the buttons to open boards and
 * the IGS console.
 * On Windows this frame can be minimized to the system tray.
 * @ingroup userinterface
 */
class StarterFrame : public wxFrame
{
public:
    /** Constructor */
    StarterFrame();

    /** Destructor */
    ~StarterFrame();

    /** Callback for close events */
    void OnClose(wxCloseEvent &event);

    /** Callback for button: Play on IGS */
    void OnIGS(wxCommandEvent& WXUNUSED(event));

    /** Callback for button: Open a board */
    void OnNewBoard(wxCommandEvent& WXUNUSED(event));

    /** Callback for button: Load a game */
    void OnLoadGame(wxCommandEvent& WXUNUSED(event));

    /** Callback for button: Play GNU Go */
    void OnGTP(wxCommandEvent& WXUNUSED(event));

    /** Callback for button: Preferences */
    void OnPreferences(wxCommandEvent& WXUNUSED(event));

    /** Callback for button: Exit and close events */
    void OnExit(wxCommandEvent& WXUNUSED(event));

private:
    void autohideFrame();
#ifdef __WXMSW__
    StarterIcon *taskbarIcon;
#endif

DECLARE_EVENT_TABLE()
};


// -----------------------------------------------------------------------
//                          Class StarterIcon
// -----------------------------------------------------------------------

#ifdef __WXMSW__
/**
 * Icon used for system tray when the StarterFrame is minimized.
 * This is currently only used on Windows.
 * @todo Figure out how to use this with KDE/Gnome docks
 * @ingroup userinterface
 */
class StarterIcon : public wxTaskBarIcon
{
public:
    StarterIcon(StarterFrame *frame);
    ~StarterIcon();
    void OnTaskbarLeftDblClick(wxTaskBarIconEvent& event) { restoreFrame(); }
    void OnTaskBarRightClick(wxTaskBarIconEvent& event);
    void OnRestore(wxCommandEvent& WXUNUSED(event)) { restoreFrame(); }
    void OnBoard(wxCommandEvent& WXUNUSED(event));
    void OnBoardType3D(wxCommandEvent& event);
    void OnBoardType2D(wxCommandEvent& event);
    void OnExit(wxCommandEvent& WXUNUSED(event));

private:
    void restoreFrame();
    wxMenu *popup;
    StarterFrame *frame;

DECLARE_EVENT_TABLE()
};
#endif  // __WXMSW__

#endif
