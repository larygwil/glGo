/*
 * Board.h
 *
 * $Id: board.h,v 1.36 2003/11/22 17:16:10 peter Exp $
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

#ifndef BOARD_H
#define BOARD_H

#ifdef __GNUG__
#pragma interface "board.h"
#endif

#include "defines.h"
#include "clock.h"


// This define controls if played stones or move navigation use the thread-safe
// event mechanism (AddPendingEvent) or the unsafe mechanism (ProcessEvent)
// TODO: Can probably be removed
#define THREAD_SAFE


class MainFrame;
class BoardHandler;
class GameData;


/**
 * @defgroup boardinterface Board interface
 * @ingroup userinterface
 *
 * The board interface contains the OpenGL 3D and the SDL 2D boards.
 *
 * The goal is to allow a pluggable class to exchange 3D and 2D displays
 * without altering functions not associated with the pure display.
 * This is done by defining an abstract superclass Board and implementing
 * the interface in the subclasses GLBoard and SDLBoard, which can be
 * freely exchanged without changing the overall interface logic.
 *
 * The actual implementation of game logic, storage of stones, captures
 * etc. are done in the BoardHandler class, which is associated in Board.
 * Such implementation should not be found within this module to seperate
 * the code for document and view properly.
 *
 * @{
 */

/**
 * Abstract superclass for GLBoard and SDLBoard.
 * This class is an interface for the real 3D and 2D display implementations.
 * Currently some shared code is moved here to avoid redundance, but for the
 * sake of a clear design this class should be a real interface with only
 * pure virtual functions.
 */
class Board
{
public:
    /** Constructor */
    Board(MainFrame *parent);

    /** Destructor */
    virtual ~Board();


    // -------------------------------------------------------------------------
    //                Declarations of abstract virtual methods
    // -------------------------------------------------------------------------

    /**
     * Boardtype runtime information.
     * Check if this board instance is based on the OpenGL glBoard class or on
     * the SDLBoard class.
     * @return True if based on OpenGL, else false
     */
    virtual bool isOpenGLBoard() const = 0;

    /**
     * Initialize a new game.
     * This function calls setupGame to initialize the board visuals, and - unlike
     * setupGame - calls newGame in the BoardHandler.
     * @param data Pointer to the new GameData instance, passed over to Game class
     * @see setGame(GameData*)
     */
    virtual void newGame(GameData *data) = 0;

    /**
     * Setup the board for a new game.
     * This is similar to newGame(GameData*), but won't call the BoardHandler
     * anymore, so it can be used when loading SGF files.
     * @param data Pointer to the new GameData instance, passed over to Game class
     * @see newGame(GameData*)
     */
    virtual void setupGame(GameData *data) = 0;

    /**
     * Toggle view parameters.
     * This function must accept and handle all enums in ViewParam.
     * @param para ViewParam type, see defines.h
     * @param value Toggle parameter on or off
     */
    virtual void setViewParameter(int para, bool value) = 0;

    /**
     * Set or update the background color.
     * This must be implemented by all subclasses.
     * @param c New background color
     */
    virtual void updateBackgroundColor(wxColour c) = 0;

    // -------------------------------------------------------------------------
    //                      End abstract virtual methods
    // -------------------------------------------------------------------------

    /**
     * Gets the current board size.
     * @return The board size
     */
    unsigned short getBoardSize() const { return board_size; }

    /**
     * Gets the boardhandler associated with this board.
     * @return Pointer to the boardhandler
     */
    BoardHandler* getBoardHandler() const { return boardhandler; }

    /** Gets a pointer to the parent MainFrame which embeds this board. */
    MainFrame* getParentFrame() const { return parentFrame; }

    /** Sets the color for the current turn. Used for SGF editing. */
    void setTurn(Color c);

    /**
     * Check if game was modified and needs to be redrawn.
     * @return True if the game was modified, else false
     */
    bool isModified() const;

    /*
     * Set blocked status of this board.
     * If true, the board won't update until unblocked.
     * If false, the board will refresh.
     */
    void block(bool b) { blocked = b; if (!b) is_modified = true; }

    /**
     * Update the mainframe of this board.
     * An EventInterfaceUpdate is sent to the frame.
     * @param force_clock_update If true, force update of both clock labels
     */
    void updateMainframe(bool force_clock_update=false);

    /** Gets the current edit mode. */
    EditMode getEditMode() const { return editMode; }

    /** Sets the SGF editor mode. */
    void setEditMode(EditMode m) { editMode = m; }

    /**
     * Display a message to the user.
     * This is a convinience function so the boardhandler and other code that don't
     * have a pointer to the associated MainFrame can open a window-modal instead of
     * an app-modal messagebox.
     */
    void displayMessageBox(const wxString &message,
                           const wxString& caption = _("Message"),
                           int style = wxOK);

    /**
     * Load a SGF file.
     * @param filename Name of SGF file to load
     * @param is_tmp_filename True if loaded from temp file
     * @return True if loaded was successfull
     */
    bool loadGame(const wxString &filename, bool is_tmp_filename=false);

    /**
     * Save a game to SGF
     * @param filename Name of the SGF file to save to
     * @param dont_remember If true, do not remember the filename (used for "Save/SaveAs" mechanism)
     */
    bool saveGame(const wxString &filename, bool dont_remember=false);

    /** Update clock.
     * @param col Color of the player whose clock is to  be updated
     * @param time Time value. Can be absolute or byoyomi time, depending on stones value
     * @param stones Byoyomi stones. If -1, time is interpreted as absolute time.
     */
    void updateClock(Color col, int time, short stones);

    /** Gets the white clock */
    const Clock& getClockWhite() const { return clock_white; }

    /** Gets the black clock */
    const Clock& getClockBlack() const { return clock_black; }

    /** Init clocks with the given time */
    void InitClocks(int white_time, int black_time);

    /** Start clock of given color */
    void StartClock(int c);

    /** Stop clock of given color */
    void StopClock(int c);

    /** Tick clock of given color by one second */
    int TickClock(int c);

    /** Set byoyomi time in the clock of given color */
    void SetByoTime(int c, int byo);

    /**
     * Undo the current move. This also deletes the subtree. This is used
     * for IGS and GTP games as Undo and for the SGF editor as Delete feature.
     */
    void undoMove();

    /** Check if it is our turn in own games. Used for the cursor display. */
    bool mayMove();

    /** Set own color. Only used for own IGS games to keep track which color we play. */
    void setMyColor(Color c) { myColor = c; }

    /** Gets own color. Only used for IGS games. */
    Color getMyColor() const { return myColor; }

    /** Enter or leave score mode */
    bool toggleScore();

    /** Display score result in sidebar */
    void displayScoreResult(int terrWhite, int capsWhite, float finalWhite,
                            int terrBlack, int capsBlack, int finalBlack,
                            int dame);

    /** Display comment in sidebar */
    void displayComment(const wxString &txt);

    /** Remove a dead stone. Called from IGS when scoring own games. */
    void removeDeadStone(short x, short y);

    /** Check if sound output is disabled for this board only. */
    bool getPlayLocalSound() const;

protected:
    // -------------------------------------------------------------------------
    //      Declarations of implemented abstract virtual methods from Board
    // -------------------------------------------------------------------------

    /**
     * Navigate the game.
     * The subclass has to implmenet this using events.
     * @param direction Navigation direction, see defines.h
     */
    virtual void navigate(NavigationDirection direction) = 0;

    // -------------------------------------------------------------------------
    //                      End abstract virtual methods
    // -------------------------------------------------------------------------


    /**
     * Handle a mouse click on the board.
     * This *only* takes care for the SGF editing, not normal moves as those
     * are sent via the event mechanism. So the subclass has to overwrite
     * this function, take care of normal moves itself, and then call this
     * superclass function. Yes, a bit complex, but works well and avoids
     * redundant code. The problem is, this class has no eventhandler attached.
     * Depending on the current edit mode the proper action will be chosen.
     * @param x X position of the click
     * @param y Y position of the click
     * @param button Which mouse button was clicked. 0 = left, 1 = right
     */
    void handleMouseClick(int x, int y, int button);

    MainFrame *parentFrame;      ///< Pointer to the mainframe which embeds this board
    unsigned short board_size;   ///< Board size
    BoardHandler *boardhandler;  ///< Pointer to the boardhandler attached to this board
    EditMode editMode;           ///< Current editing mode, see defines.h
    bool is_modified;            ///< Flag if the board is modified and needs a redraw in OnIdle
    bool blocked;                ///< Flag if the board should not update itself for the moment
    bool show_coords;            ///< Flag if coordinates are enabled
    bool show_marks;             ///< Flag if marks are enabled
    bool show_cursor;            ///< Flag if ghost cursor is enabled
    Clock clock_white;           ///< White clock
    Clock clock_black;           ///< Black clock
    Color myColor;               ///< Own color. Only used for own IGS games.
    EditMode oldEditMode;        ///< Used to remember old edit mode when toggling score mode
};

/** @} */  // End of group

#endif
