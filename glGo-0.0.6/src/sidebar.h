/*
 * sidebar.h
 *
 * $Id: sidebar.h,v 1.22 2003/11/02 07:52:44 peter Exp $
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

#ifndef SIDEBAR_H
#define SIDEBAR_H

#ifdef __GNUG__
#pragma interface "sidebar.h"
#endif

#include <wx/laywin.h>

class GameData;
class MainFrame;



// ------------------------------------------------------------------------
//                            Class Sidebar
// ------------------------------------------------------------------------

/**
 * The sidebar of the board window. This is a sash window associated with the
 * MainFrame. The sidebar can be switched off and moved between right and left.
 * The top third of the sidebar has a notebook with three tabs:
 * - %Game - Shows the clocks and player info
 * - Info - Shows game information
 * - Edit - Shows the sgf editing tools
 * @ingroup userinterface
 */
class Sidebar : public wxSashLayoutWindow
{
public:

    /** Constructor */
    Sidebar(MainFrame *parent, int id);

    /** Init the sidebar GUI elements. Should be overwritten by subclasses. */
    virtual void initSidebar();

    virtual GameType getSidebarType() const { return GAME_TYPE_PLAY; }

    /**
     * Check if the sidebar is on or off.
     * @return True if on, false if off
     */
    bool IsOn() const { return is_on; }

    /**
     * Check if the sidebar is located right or left.
     * @return True if right, false if left
     */
    bool IsRight() const { return is_right; }

    /** Toggle sidebar on-off. */
    void Toggle();

    /** Swap sidebar right-left. */
    void Swap();

    /**
     * Resets the sidebar on new games.
     * Clears comment textarea and resets edit buttons.
     * @param edit_tools If true, enable the edit tool panel, else disable it
     * @param text_editable Set kibitz/comment textfield editable or not
     */
    virtual void reset(bool edit_tools = true, bool text_editable=true);

    /** Enable or disable the edit tools panel. */
    void enableEditTools(bool enable = true);

    /** Set text in the kibitz/comment textarea. */
    void setTextareaText(const wxString &txt);

    /** Gets the text in the kibitz/comment textarea. */
    const wxString& getTextareaText();

    /** Append text to kibitz/comment textarea. */
    void appendTextareaText(const wxString &txt);

    /** Set and update game information. */
    void setGameInfo(GameData *data);

    /** Update the clock of the player of given color. */
    void updateClock(Color col, const wxString &time_str, bool warn=false);

    /** Set captures labels. */
    void setCaptures(unsigned short white, unsigned short black);

    /** Sets the color of the current turn, adjusting the edit buttons. */
    void setTurn(Color c);

    /** Callback for Pass button. */
    virtual void OnPass(wxCommandEvent& WXUNUSED(event));

    /** Callback for Score button. */
    void OnScore(wxCommandEvent& WXUNUSED(event));

    /** Callback for White stone button. */
    void OnStoneWhite(wxCommandEvent& WXUNUSED(event));

    /** Callback for Black stone button. */
    void OnStoneBlack(wxCommandEvent& WXUNUSED(event));

    /** Callback for edit stone button. */
    void OnStone(wxCommandEvent& WXUNUSED(event));

    /** Callback for square button. */
    void OnMarkSquare(wxCommandEvent& WXUNUSED(event));

    /** Callback for circle button. */
    void OnMarkCircle(wxCommandEvent& WXUNUSED(event));

    /** Callback for triangle button. */
    void OnMarkTriangle(wxCommandEvent& WXUNUSED(event));

    /** Callback for cross button. */
    void OnMarkCross(wxCommandEvent& WXUNUSED(event));

    /** Callback for text button. */
    void OnMarkText(wxCommandEvent& WXUNUSED(event));

    /** Resets the blinking clocks */
    void resetTimeWarning();

    /** Display score result */
    virtual void setScore(int terrWhite, int capsWhite, float finalWhite,
                          int terrBlack, int capsBlack, int finalBlack,
                          int dame);

protected:
    MainFrame *frame;
    bool is_right, is_on, turn_stone_selected;
    int clock_warn_flag;
    wxColour buttonBackgroundColor, clockBackgroundColor;
    wxStaticText *clock_white, *clock_black, *caps_white, *caps_black;

private:
    void resetAllButtons();

DECLARE_EVENT_TABLE()
};


// ------------------------------------------------------------------------
//                            Class SidebarGTP
// ------------------------------------------------------------------------

/**
 * Sidebar for GNU Go games
 * @ingroup userinterface
 */
class SidebarGTP : public Sidebar
{
public:
    /** Constructor */
    SidebarGTP(MainFrame *parent, int id) : Sidebar(parent, id) { }

    virtual GameType getSidebarType() const { return GAME_TYPE_GTP; }

    virtual void initSidebar();

    /** Callback for Undo button. */
    void OnUndo(wxCommandEvent& WXUNUSED(event));

    /** Dummy, no Score panel here */
    virtual void setScore(int, int, float, int, int, int, int) { }

private:
DECLARE_EVENT_TABLE()
};


// ------------------------------------------------------------------------
//                            Class SidebarObserve
// ------------------------------------------------------------------------

/**
 * Sidebar for observed IGS games
 * @ingroup userinterface
 */
class SidebarObserve : public Sidebar
{
public:
    /** Constructor */
    SidebarObserve(MainFrame *parent, int id) : Sidebar(parent, id) { }

    virtual GameType getSidebarType() const { return GAME_TYPE_IGS_OBSERVE; }

    /** Callback for Edit button. */
    void OnEditGame(wxCommandEvent& WXUNUSED(event));

    /** Callback for Observers button. */
    void OnObservers(wxCommandEvent& WXUNUSED(event));

    /** Callback for the kibitz textcontrol. */
    void OnCommandEnter(wxCommandEvent& WXUNUSED(event));

    virtual void initSidebar();

    /** Dummy, no Score panel here */
    virtual void setScore(int, int, float, int, int, int, int) { }

private:
    wxTextCtrl *input;

DECLARE_EVENT_TABLE()
};


// ------------------------------------------------------------------------
//                            Class SidebarIGSPlay
// ------------------------------------------------------------------------

/**
 * Sidebar for own played IGS games
 * @ingroup userinterface
 */
class SidebarIGSPlay : public Sidebar
{
public:
    /** Constructor */
    SidebarIGSPlay(MainFrame *parent, int id) : Sidebar(parent, id) { }

    virtual GameType getSidebarType() const { return GAME_TYPE_IGS_PLAY; }

    /** Callback for Pass button. */
    virtual void OnPass(wxCommandEvent& WXUNUSED(event));

    /** Callback for Resign button. */
    void OnResign(wxCommandEvent& WXUNUSED(event));

    /** Callback for Adjourn button. */
    void OnAdjourn(wxCommandEvent& WXUNUSED(event));

    /** Callback for Observers button. */
    void OnObservers(wxCommandEvent& WXUNUSED(event));

    /** Callback for Undo button. */
    void OnUndo(wxCommandEvent& WXUNUSED(event));

    /** Callback for the kibitz textcontrol. */
    void OnCommandEnter(wxCommandEvent& WXUNUSED(event));

    virtual void initSidebar();

    /** IGS switched to score mode after three passes. */
    void enterScoreMode();

    /** Check if this own IGS game is currently being scored. Ugly workaround. */
    bool isIGSScored() const { return scoreMode; }

private:
    wxTextCtrl *input;
    bool scoreMode;

DECLARE_EVENT_TABLE()
};

#endif
