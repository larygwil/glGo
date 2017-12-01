/*
 * GLBoard.h
 *
 * $Id: glBoard.h,v 1.51 2003/10/21 08:46:13 peter Exp $
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

#ifndef GLBOARD_H
#define GLBOARD_H

#ifdef __GNUG__
#pragma interface "glBoard.h"
#endif

#include "board.h"
#include "marks.h"
#include <wx/glcanvas.h>
#include <wx/laywin.h>
#include "utils/utils.h"


// Mousegrid debugging. Can be removed. TODO
// #define MOUSEGRID_DEBUG

#define NO_ANIMATE


class Mark;
class fntRenderer;


/**
 * The OpenGL canvas. This class contains the OpenGL code to display a 3D goban
 * and stones. Ultimately, it should be possible to plug in another class instead
 * of this, if the user does not want to use OpenGL, and provide a simpler
 * 2D-only board by simply exchanging this class.
 * The code to handle the game logic and stone placement is found in BoardHandler
 * class, this class should *only* care for the display and keyboard/mouse input
 * (maybe input can be moved into another class later).
 * Some part of the code which is shared with the SDLBoard class is moved to the
 * common abstract superclass Board.
 * @ingroup boardinterface
 * @see ogl_helper.h OGLConfig
 */
class GLBoard: public wxGLCanvas, public Board
{
public:

    /** Constructor */
    GLBoard(MainFrame *parent,
            int id = -1,
            const wxPoint &pos = wxDefaultPosition,
            const wxSize &size = wxDefaultSize);

    /** Destructor */
    virtual ~GLBoard();


    // -------------------------------------------------------------------------
    //      Declarations of implemented abstract virtual methods from Board
    // -------------------------------------------------------------------------

    /**
     * Runtime type information. Implemented for Board. Returns true for this class.
     * @return Always true
     */
    virtual bool isOpenGLBoard() const { return true; }

    virtual void newGame(GameData *data);

    virtual void setupGame(GameData *data);

    virtual void setViewParameter(int para, bool value);

    virtual void updateBackgroundColor(wxColour c);

    // -------------------------------------------------------------------------
    //                      End implemented virtual methods
    // -------------------------------------------------------------------------

    /** Set OpenGL specific display parameters. */
    void setOGLViewParameter(OGLConfig &config);

    /**
     * Gets OpenGL Renderer, Version and Vendor info.
     * @return String with formatted OpenGL info
     */
    wxString getOpenGLInfo();

    /** Callback for Paint event. */
    void OnPaint(wxPaintEvent& WXUNUSED(event));

    /** Callback for Size event. */
    void OnSize(wxSizeEvent& event);

    /** Callback for CalculateLayout event. Called when the MainFrame sash is resized. */
    void OnCalculateLayout(wxCalculateLayoutEvent &event);

    /** Callback for Enter Window event. */
    void OnEnterWindow(wxMouseEvent& WXUNUSED(event));

    /** Callback for Erase Background event. */
    void OnEraseBackground(wxEraseEvent& WXUNUSED(event));

    /** Callback for Left mouse click */
    void OnMouseLeftDown(wxMouseEvent& e);

    /** Callback for Right mouse click */
    void OnMouseRightDown(wxMouseEvent& e);

    /** Callback for mouse move. */
    void OnMouseMotion(wxMouseEvent& e);

#ifndef NO_ANIMATE
    /** Timer callback. Used for the multitexture animation. */
    void OnTimer(wxTimerEvent& WXUNUSED(event));
#endif

    /**
     * Callback for Idle event.
     * This will check the boardhandler for position updates and if required redraw the scene.
     */
    void OnIdle(wxIdleEvent& WXUNUSED(event));

    /** Callback for Key Down event. */
    void OnKeyDown(wxKeyEvent& e);

    /** Callback for Key Up event. */
    void OnKeyUp(wxKeyEvent& e);

    /** Callback for Char event. */
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
    void recalculateBoard();
    void resetTransformations(bool force_defaults=false);
    void InitGL();
    void setupPerspective();
    void prepareSupersampleTexture();
    void createGobanList();
    void createStoneLists();
    /** Rendering. @todo Remove mousegrid */
    void Render();
    void selectPick(int x, int y, int &coordX, int &coordY);
    void drawAntialiasCircle(GLfloat radius, Color color);
    void drawStone(short x, short y, Color color);
    void drawGhostStone(short x, short y, Color c);
    void drawMark(const Mark *mark);
    void drawMark(short x, short y, MarkType t, const wxString& txt=wxEmptyString);
    void drawGrid();
    void drawStarpoint(short x, short y);
    void drawCoordinates();
    void drawTextMark(short x, short y, const wxString &txt);
    void drawTextureCube();
    void drawMouseGrid(bool display=false);
    void createTextures();
    void deleteTextures();
    void prepareTexture(const wxString &filename, GLuint id);
    void reconfigureTextureQuality();
    void handleMouseClick(int x, int y, int button);
    void setupScissor(short x, short y);

#ifdef __VISUALC__
    #define num_textures 4
#else
    static const size_t num_textures = 4;
#endif
    enum { TEXTURE_KAYA, TEXTURE_WHITE, TEXTURE_MARK_WHITE, TEXTURE_MARK_BLACK };
    GLuint *texture_ids;
    GLuint renderTex, board_list, starpoint_list, neutral_stone_list, white_stone_list,
        black_stone_list, circle_list, shadow_list, sphere_fragments;
    bool init_gl_flag, textures, reflections, line_antialias, stone_antialias, blend,
        antialias_scene, blur, fast_rendering, render_to_texture, renderTex_flag,
        scene_antialias_quality_high, texture_quality_high, multitextures, shadows,
        use_scissor, cursor_update, had_ghosts, block_cursor;
    unsigned short keydown_flag;
#ifndef NO_ANIMATE
    GLfloat angle;
    wxTimer *timer;
#endif
    GLfloat offset, board_pixel_size, square_size, r_x, r_y, r_z, s_x, s_y, s_z, fovy, stone_size;
#ifdef MOUSEGRID_DEBUG
    bool display_mouse_grid;
#endif
    fntRenderer *font_renderer;
    wxColour back_color;
    Stone lastStone;
    Position cursor;

DECLARE_EVENT_TABLE()
};

#endif
