/*
 * sdlboard.h
 *
 * $Id: sdlboard.h,v 1.25 2003/11/24 14:38:25 peter Exp $
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

#ifndef SDL_BOARD_H
#define SDL_BOARD_H

#ifdef __GNUG__
#pragma interface "sdlboard.h"
#endif

#include "board.h"
#include "stone.h"
#include <wx/laywin.h>


struct SDL_Surface;
typedef struct _TTF_Font TTF_Font;


/**
 * A SDL 2D goban canvas.
 * This class contains the non-OpenGL 2D code base on SDL to display a goban and
 * stones. The code to handle the game logic and stone placement is found in BoardHandler
 * class, this class should *only* care for the display and keyboard/mouse input
 * (maybe input can be moved into another class later).
 * Some part of the code which is shared with the GLBoard class is moved to the
 * common abstract superclass Board.
 * This board draws the goban using the SDL library which offers a high quality 2D
 * display. Some additional third-party code (zziplib only at the moment) is bundled
 * into the sdlcore library and used by this class.
 * The unscaled images are shared among all instances of this class to save some
 * memory. The images are copied and the copy scaled when the window is resized.
 * @see sdl
 * @ingroup boardinterface sdl
 */
class SDLBoard : public wxPanel, public Board
{
public:
    /** Constructor */
    SDLBoard(MainFrame *parent, int id=-1);

    /** Destructor */
    ~SDLBoard();

    /**
     * Should be called from glGo::OnExit() to cleanup some shared SDL resources.
     * This also calls SDL_Quit(), so it should be called after shutting down sound.
     */
    static void Cleanup();

    /** Get SDL video info */
    wxString getSDLInfo();


    // -------------------------------------------------------------------------
    //      Declarations of implemented abstract virtual methods from Board
    // -------------------------------------------------------------------------

    /**
     * Runtime type information.
     * Implemented for Board. Returns false for this class.
     * @return Always false
     */
    virtual bool isOpenGLBoard() const { return false; }

    virtual void newGame(GameData *data);

    virtual void setupGame(GameData *data);

    virtual void setViewParameter(int para, bool value);

    virtual void updateBackgroundColor(wxColour c);

    // -------------------------------------------------------------------------
    //                      End implemented virtual methods
    // -------------------------------------------------------------------------

    /** Callback for CalculateLayout event. Called when the MainFrame sash is resized. */
    void OnCalculateLayout(wxCalculateLayoutEvent &event);

    /**
     * Callback for idle events.
     * This will check the boardhandler for position updates and if required redraw the scene.
     */
    void OnIdle(wxIdleEvent& WXUNUSED(event));

    /** Callback for erase events. Does nothing to avoid flickering. */
    void OnEraseBackground(wxEraseEvent& WXUNUSED(event));

    /** Callback for size event */
    void OnSize(wxSizeEvent& WXUNUSED(event));

    /** Callback for paint event. This will redraw the SDL board. */
    void OnPaint(wxPaintEvent& WXUNUSED(event));

    /** Callback for left mousebutton */
    void OnMouseLeftDown(wxMouseEvent& e);

    /** Callback for right mousebutton */
    void OnMouseRightDown(wxMouseEvent& e);

    /** Callback for mouse move. */
    void OnMouseMotion(wxMouseEvent& e);

    /** Callback for Char event */
    void OnChar(wxKeyEvent& e);


protected:
    // -------------------------------------------------------------------------
    //      Declarations of implemented abstract virtual methods from Board
    // -------------------------------------------------------------------------

    virtual void navigate(NavigationDirection direction);

    // -------------------------------------------------------------------------
    //                      End abstract virtual methods
    // -------------------------------------------------------------------------


private:
    void calculateSize();
    void drawBoard();
    void drawStarPoint(int x, int y);
    void drawStone(int x, int y, Color color, bool transparent=false);
    void drawMark(int x, int y, int color, int type, const wxString &txt=wxEmptyString);
    bool convertCoordToPos(const wxCoord coordX, const wxCoord coordY, int &x, int &y);
    void handleMouseClick(int x, int y, int button);
    void freeTTFFont();

    SDL_Surface *screen, *screen2, *kaya, *stone_black, **stones_white;
    static SDL_Surface *kaya_img, *stone_black_img, **stones_white_img, *table_img;
    int **whitestonematrix;
    int square_size, width, height, offset, offsetX, offsetY, board_pixel_size,
        update_status;
    static bool video_init_flag;
    wxColour back_color;
    bool scaled_font, block_cursor, background_image;
    Position cursor, old_cursor;
    TTF_Font *ttf_font;
    SDL_Surface *text[38];
    int text_metrics[38][2];

DECLARE_EVENT_TABLE()
};

#endif
