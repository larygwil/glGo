/*
 * mainframe.h
 *
 * $Id: mainframe.h,v 1.73 2003/11/22 17:16:24 peter Exp $
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

#ifndef MAINFRAME_H
#define MAINFRAME_H

#ifdef __GNUG__
#pragma interface "mainframe.h"
#endif

#include <wx/laywin.h>
#include <wx/timer.h>
#include "defines.h"
#include "events.h"
#ifndef NO_GTP
#include "gtp_events.h"
#endif


class Board;
class GLBoard;
class SDLBoard;
class Sidebar;


/**
 * @defgroup userinterface User interface
 *
 * This module contains the code for the user interface. The classes are responsible
 * for processing the user input from menus, dialogs and keystrokes providing visible
 * feedback of the results.
 *
 * The probably most important part is the Board interface displaying goban and stones.
 *
 * @{
 */

/**
 * Main frame for the board window. This frame embeds the OpenGL board canvas
 * and is responsible for the user interface, like menus, toolbar etc.
 */
class MainFrame: public wxFrame
{
public:
    /** Constructor */
    MainFrame(const wxPoint& pos=wxDefaultPosition,
              const wxSize& size=wxDefaultSize,
              GameType game_type=GAME_TYPE_PLAY,
              BoardDisplayType displayType=DISPLAY_TYPE_SDL);

    /** Destructor */
    virtual ~MainFrame();

    /** Init the SDL board. Must be called after the frame was realized. */
    void InitSDLBoard();

    /** Gets a pointer to the current board. */
    Board* getBoard() { return board; }

    /** Gets a pointer to the sidebar. */
    Sidebar* getSidebar() { return sidebar; }

    /** Gets a cast pointer to either the currently used GLBoard or SDLBoard. */
    wxWindow* getCurrentBoardWindow();

    /** Gets the current GameType of this frame. */
    const GameType& getGameType() const { return game_type; }

    /** Sets the GameType of this frame. */
    void setGameType(GameType t);

    /**
     * Update window title from game information.
     * This will set a title like: "Zotan 8k vs. tgmouse 10k"
     * or if game name is given: "Kogo's Joseki Dictionary"
     */
    void updateTitle();

    /** Callback for Close event. This is also used for the File-Close menuitem. */
    void OnClose(wxCloseEvent& event);

    /** Callback for Size event */
    void OnSize(wxSizeEvent& WXUNUSED(event));

    /** Callback for SashDrag event */
    void OnSashDrag(wxSashEvent& event);

    /** Callback for Interface update calls from BoardEventHandler. */
    void OnInterfaceUpdate(EventInterfaceUpdate& event);

    /** Check if the board associated with this mainframe is modified. */
    bool isModified() const;

    /** Force sidebar data display update with the current GameData. */
    void updateSidebar();

    /**
     * Start a new GTP game.
     * @return True if the game will start successfully, else false
     */
    bool newGTPGame();

    /** Select a filename to load a SGF game */
    static wxString selectLoadSGFFilename(wxWindow *win);

    /** Save the game under the given filename */
    bool doSave(wxString filename);

    /** Creates a default filename like Zotan8k-tgmouse9k */
    wxString createDefaultFilename();

    /**
     * Callback for File-New game
     * Check if board is modified, if yes let the user confirm to proceed.
     */
    void OnNew(wxCommandEvent& WXUNUSED(event));

    /**
     * Callback for File-Load
     * Check if board is modified, if yes let the user confirm to proceed.
     * If ok, ask the user for the filename and load the SGF file.
     */
    void OnLoad(wxCommandEvent& WXUNUSED(event));

    /** Callback for File-Save */
    void OnSave(wxCommandEvent& WXUNUSED(event));

    /** Callback for File-Save As */
    void OnSaveAs(wxCommandEvent& WXUNUSED(event));

    /** Callback for Settings-Game info */
    void OnGameInfo(wxCommandEvent& WXUNUSED(event));

    /** Callback for Settings - Toggle sound */
    void OnToggleSound(wxCommandEvent& event);

    /** Callback for Toolbar Toggle sound button */
    void OnToggleSound_Toolbar(wxCommandEvent& WXUNUSED(event));

    /** Callback for Settings-Preferences */
    void OnPreferences(wxCommandEvent& WXUNUSED(event));

    /** Callback for View-Display options */
    void OnDisplayOptions(wxCommandEvent& WXUNUSED(event));

    /** Callback for View-Show marks */
    void OnShowMarks(wxCommandEvent& event);

    /** Callback for View-Show coordinates */
    void OnShowCoords(wxCommandEvent& event);

    /** Callback for View-Show cursor */
    void OnShowCursor(wxCommandEvent& event);

    /** Callback for View-Clear text */
    void OnClearOutput(wxCommandEvent& WXUNUSED(event));

    /** Callback for View-Toolbar */
    void OnToolbar(wxCommandEvent& event);

    /** Callback for View-Statusbar */
    void OnStatusbar(wxCommandEvent& event);

    /** Callback for View-Fullscreen */
    void OnFullscreen(wxCommandEvent& event);

    /** Callback for View-Sidebar */
    void OnSidebar(wxCommandEvent& WXUNUSED(event));

    /** Callback for View-Swap sidebar */
    void OnSwapSidebar(wxCommandEvent& WXUNUSED(event));

    /** Callback for GTP-Connect */
    void OnGTP(wxCommandEvent& WXUNUSED(event));

    /** Callback for GTP-Console */
    void OnGTPConsole(wxCommandEvent& WXUNUSED(event));

    /** Callback for GTP-Close */
    void OnGTPClose(wxCommandEvent& WXUNUSED(event));

    /** Callback for GTP-Guess score */
    void OnGTPScore(wxCommandEvent& WXUNUSED(event));

    /** Callback for Help-Manual */
    void OnHelp(wxCommandEvent& WXUNUSED(event));

    /** Callback for Help-About */
    void OnAbout(wxCommandEvent& WXUNUSED(event));

    /**
     * Callback for Help-glGo on the web.
     * Opens the glGo webpage (defined in glGoURL) in the default browser.
     */
    void OnOpenWebpage(wxCommandEvent& WXUNUSED(event));

    /**
     * Callback for Help-Check update.
     * This function will call the checkUpdate() function and display
     * the resulting message in a messagebox.
     */
    void OnCheckUpdate(wxCommandEvent& WXUNUSED(event));

    /** Callback for Navigation-First move */
    void OnFirstMove(wxCommandEvent& WXUNUSED(event));

    /** Callback for Navigation-Previous move */
    void OnPreviousMove(wxCommandEvent& WXUNUSED(event));

    /** Callback for Navigation-Next move */
    void OnNextMove(wxCommandEvent& WXUNUSED(event));

    /** Callback for Navigation-Last move */
    void OnLastMove(wxCommandEvent& WXUNUSED(event));

    /** Callback for Navigation-Previous variation */
    void OnPreviousVariation(wxCommandEvent& WXUNUSED(event));

    /** Callback for Navigation-Next variation */
    void OnNextVariation(wxCommandEvent& WXUNUSED(event));

    /** Callback for Edit-Delete move */
    void OnDeleteMove(wxCommandEvent& WXUNUSED(event));

    /** Callback for Edit-Remove marks */
    void OnRemoveMarks(wxCommandEvent& WXUNUSED(event));

    /** Callback for Edit-Number moves */
    void OnNumberMoves(wxCommandEvent& event);

    /** Callback for Edit-Mark brothers */
    void OnMarkBrothers(wxCommandEvent& event);

    /** Callback for Edit-Mark sons */
    void OnMarkSons(wxCommandEvent& event);

    /** Update the game data. This is called when a new IGS game starts. */
    void updateGameData(const wxString &white_name, const wxString &white_rank,
                        const wxString &black_name, const wxString &black_rank,
                        int size, int handicap, float komi, int byo, short type,
                        const wxString &title, int id);

    /** Load a SGF file */
    void doLoad(const wxString &filename, bool is_tmp_filename=false);

    /** Append a text to the textarea. */
    void appendComment(const wxString &txt, bool at_last=false);

    /** Sets the game result for this game. Called from IGS module. */
    void setGameResult(const wxString &txt);

    /** Show score returned from GNU Go in a messagebox. */
    void OnShowScore(
#ifndef NO_GTP
        EventGTPScore &event
#endif
        );

    /** Gets ID of current game. Only relevant for IGS games. */
    int getGameID() const { return game_id; }

    /** Sets our own name. Only relevant for IGS games. */
    void setMyName(const wxString &name) { myName = name; }

    /**
     * Get our own name. Only relevant for IGS games, used for say/kibitz feedback.
     * @return Our login name or wxEmptyString if this is not and IGS game
     */
    const wxString& getMyName() const { return myName; }

    /**
     * Save current game to temp file.
     * @return Filename of temp file, wxEmptyString if tmpfile creation failed.
     */
    wxString saveTempFile();

    /**
     * Callback for click timer.
     * Decreases the clock by one second and updates the wxStaticText label in the sidebar.
     */
    void OnClockTimer(wxTimerEvent& WXUNUSED(event));

    /** Init clocks with the given time */
    void InitClocks(int white_time, int black_time);

    /**
     * Starts a clock of given color and the clockTimer
     * @param col Color of the player whose clock to stop.
     */
    void StartClock(int col);

    /**
     * Stops a clock.
     * @param col Color of the player whose clock to stop
     * @param stop_timer If true, stop the timer completely, no further clock updates will be called
     */
    void StopClock(int col, bool stop_timer=false);

    /** Own IGS game enters score mode after three passes. */
    void enterIGSScoreMode();

    /** Check if this own IGS game is currently being scored. Ugly workaround. */
    bool isIGSScored();

    /** Check if sound output is disabled for this board only. */
    bool getPlayLocalSound() const { return play_local_sound; }

private:
    void Init();
    void createSidebar();
    void RefreshLayout();
    bool checkModified();
    void reconfigureStatusbar();

    Board *board;
    GLBoard *glBoard;
    SDLBoard *sdlBoard;
    Sidebar *sidebar;
    wxToolBar *toolBar;
    GameType game_type;
    int game_id;
    wxString myName;
    wxTimer clockTimer;
    bool timewarn_flag, play_local_sound;

DECLARE_EVENT_TABLE()
};

/** @} */  // End of group

#endif
