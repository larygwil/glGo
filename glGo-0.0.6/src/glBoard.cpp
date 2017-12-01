/*
 * GLBoard.cpp
 *
 * $Id: glBoard.cpp,v 1.94 2003/10/31 22:09:06 peter Exp $
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

#ifdef __GNUG__
#pragma implementation "glBoard.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <GL/glu.h>
#include <GL/glext.h>
#include <wx/image.h>
#include <wx/config.h>
#include <math.h>
#include <plib/fnt.h>
#include "glBoard.h"
#include "ogl_helper.h"
#include "glGo.h"
#include "boardhandler.h"
#include "gamedata.h"
#include "mainframe.h"
#include "events.h"
#include <SDL/SDL_image.h>
#include "SDL_rwops_zzip.h"

#if !wxUSE_GLCANVAS
#error Please set wxUSE_GLCANVAS to 1 in setup.h.
#endif

// Extensions
#ifndef NO_MULTITEXTURES

// Supported by compiler?
#if !defined(GL_ARB_multitexture) || !defined(GL_ARB_multisample)
#error No ARB multitexture extension available
#endif

#ifdef _WIN32
extern PFNGLACTIVETEXTUREARBPROC glActiveTextureARB;
extern PFNGLMULTITEXCOORD2FARBPROC glMultiTexCoord2fARB;
#endif

#endif  // NO_MULTITEXTURES


// ---------------------------------------------------------------------------
//                            Defines and Globals
// ---------------------------------------------------------------------------

// Basic display values
#define BASE 1.0f
#define BOARD_HEIGHT 10.0f
#define SUPERSAMPLE_TEX_SIZE 1024
#define DEFAULT_FOVY 20.0f
#define DEFAULT_ZOOM 6.0f
#define SPHERE_FRAGMENTS_HIGH 32
#define SPHERE_FRAGMENTS_LOW 16

// Transformation factors
const float rotate_unit = 1.0f;
const float shift_unit  = 0.05f;
const float zoom_unit   = 0.05f;
const float fovy_unit   = 1.0f;

// Light
const GLfloat LightAmbient[4]        = {  0.8f, 0.8f, 0.8f, 1.0f };
const GLfloat LightDiffuse[4]        = {  1.0f, 1.0f, 1.0f, 1.0f };
const GLfloat LightSpecular[4]       = {  1.0f, 1.0f, 1.0f, 1.0f };
const GLfloat LightPosition[4]       = { -3.0f, 3.0f, 6.0f, 0.0f };

// Material
const GLfloat BlackLightAmbientDiffuse[4] = { 0.1f, 0.1f, 0.1f, 1.0f };
const GLfloat BlackLightSpecular[4]       = { 0.4f, 0.4f, 0.4f, 1.0f };
const GLfloat BlackLightShininess[4]      = { 80.0f };
const GLfloat WhiteLightAmbientDiffuse[4] = { 0.8f, 0.8f, 0.8f, 1.0f };
const GLfloat WhiteLightSpecular[4]       = { 0.9f, 0.9f, 0.9f, 1.0f };
const GLfloat WhiteLightShininess[4]      = { 40.0 };

// How to blend the marker on the black stone
const GLfloat black_marker_blend_rgba[4] = { 1.0f, 1.0f, 1.0f, 1.0f };

// Shadow
static GLfloat floorShadow[4][4];

// Texture fonts
#define COORDS_FONT "coords_font.txf"

#ifndef NO_ANIMATE
// ID and delay for the multitexture timer in milliseconds
const int MULITEXTURE_ANIMATION_ID    = -1;
const int MULITEXTURE_ANIMATION_DELAY = 100;
#endif

const GLuint EMPTY_LIST = 999;


// ---------------------------------------------------------------------------
//                               Class GLBoard
// ---------------------------------------------------------------------------

BEGIN_EVENT_TABLE(GLBoard, wxGLCanvas)
    EVT_CALCULATE_LAYOUT(GLBoard::OnCalculateLayout)
    EVT_PAINT(GLBoard::OnPaint)
    EVT_SIZE(GLBoard::OnSize)
    EVT_IDLE(GLBoard::OnIdle)
    EVT_ERASE_BACKGROUND(GLBoard::OnEraseBackground)
    EVT_ENTER_WINDOW(GLBoard::OnEnterWindow)
    EVT_KEY_DOWN(GLBoard::OnKeyDown)
    EVT_KEY_UP(GLBoard::OnKeyUp)
    EVT_CHAR(GLBoard::OnChar)
    EVT_LEFT_DOWN(GLBoard::OnMouseLeftDown)
    EVT_RIGHT_DOWN(GLBoard::OnMouseRightDown)
#ifndef NO_ANIMATE
    EVT_TIMER(MULITEXTURE_ANIMATION_ID, GLBoard::OnTimer)
#endif
END_EVENT_TABLE()


GLBoard::GLBoard(MainFrame *parent,
                 int id,
                 const wxPoint &pos,
                 const wxSize &size)
    : wxGLCanvas((wxWindow*)parent, id, pos, size),
      Board(parent)
{
    texture_ids = NULL;

    // Initialize default view parameters.
    init_gl_flag = blur = antialias_scene = renderTex_flag = scene_antialias_quality_high =
        fast_rendering = multitextures = use_scissor = cursor_update = had_ghosts = block_cursor = false;
    textures = reflections = shadows = line_antialias = stone_antialias = blend = texture_quality_high =
        render_to_texture = true;
    sphere_fragments = SPHERE_FRAGMENTS_HIGH;
    cursor = Position(-1, -1);

    if (show_cursor)
        Connect(wxID_ANY, wxEVT_MOTION,
                (wxObjectEventFunction) (wxEventFunction) (wxMouseEventFunction) &GLBoard::OnMouseMotion);

#ifndef NO_ANIMATE
    angle = 0.0f;
    timer = new wxTimer(this, MULITEXTURE_ANIMATION_ID);
#endif
    keydown_flag = 0;
    shadow_list = circle_list = EMPTY_LIST;
    resetTransformations();
    calculateSize();
#ifdef MOUSEGRID_DEBUG
    srand(42);  // For mousegrid debugging. TODO: Can be removed with mousegrid drawing
    display_mouse_grid = false;
#endif

    // Create floor plane and shadow matrix
    GLfloat floorPlane[4];
    GLfloat floorVertices[3][3] = {
        { -1.0f, 0.0f, 0.0f },
        {  1.0f, 0.0f, 0.0f },
        {  1.0f, 1.0f, 0.0f } };
    findPlane(floorPlane, floorVertices[0], floorVertices[1], floorVertices[2]);
    shadowMatrix(floorShadow, floorPlane, LightPosition);

    font_renderer = NULL;

    // Get background color from config
    // TODO: Background image?
    back_color = readColorFromConfig(_T("Board/OpenGL/BackColor"));
}

GLBoard::~GLBoard()
{
#ifndef NO_ANIMATE
    timer->Stop();
    delete timer;
#endif
    glDeleteLists(starpoint_list, 1);
    glDeleteLists(board_list, 1);
    glDeleteLists(neutral_stone_list, 3);
    if (circle_list != EMPTY_LIST)
        glDeleteLists(circle_list, 1);
    if (shadow_list != EMPTY_LIST)
        glDeleteLists(shadow_list, 1);

    deleteTextures();

    if (font_renderer != NULL)
        delete font_renderer;

    while (GetEventHandler() != this)
        PopEventHandler(true);

    // Save view state to config
    wxConfig::Get()->Write(_T("Board/OpenGL/RX"), r_x);
    wxConfig::Get()->Write(_T("Board/OpenGL/RY"), r_y);
    wxConfig::Get()->Write(_T("Board/OpenGL/RZ"), r_z);
    wxConfig::Get()->Write(_T("Board/OpenGL/SX"), s_x);
    wxConfig::Get()->Write(_T("Board/OpenGL/SY"), s_y);
    wxConfig::Get()->Write(_T("Board/OpenGL/SZ"), s_z);
    wxConfig::Get()->Write(_T("Board/OpenGL/Fovy"), fovy);
}

wxString GLBoard::getOpenGLInfo()
{
    return wxString::Format(_("Renderer: %s\n"
                              "Version: %s\n"
                              "Vendor: %s"),
                            glGetString(GL_RENDERER),
                            glGetString(GL_VERSION),
                            glGetString(GL_VENDOR));
}

void GLBoard::OnCalculateLayout(wxCalculateLayoutEvent &event)
{
    // As the sidebar sash is processed first, we now take the
    // complete remaining space of the parent window.
    event.SetRect(wxRect(0, 0, 0, 0));
}

void GLBoard::newGame(GameData *data)
{
    setupGame(data);
    boardhandler->newGame(data);
}

void GLBoard::setupGame(GameData *data)
{
    wxASSERT(data != NULL);

    // Set new board size if changed.
    if (board_size != data->board_size)
    {
        board_size = data->board_size;
        recalculateBoard();
    }

    lastStone = Stone();
    editMode = EDIT_MODE_NORMAL;
    is_modified = true;
}

void GLBoard::recalculateBoard()
{
    // Recalculate board scene data
    calculateSize();

    // Recreate lists unless this board was just opened
    if (init_gl_flag)
    {
        glDeleteLists(starpoint_list, 1);
        glDeleteLists(board_list, 1);
        glDeleteLists(neutral_stone_list, 3);
        if (circle_list != EMPTY_LIST)
        {
            glDeleteLists(circle_list, 1);
            circle_list = EMPTY_LIST;
        }
        if (shadow_list != EMPTY_LIST)
        {
            glDeleteLists(shadow_list, 1);
            shadow_list = EMPTY_LIST;
        }
        createGobanList();
        createStoneLists();
    }
}

void GLBoard::resetTransformations(bool force_defaults)
{
    if (!force_defaults)
    {
        // Try to read these values from config
        double tmp;
        wxConfig::Get()->Read(_T("Board/OpenGL/RX"), &tmp, 0.0);
        r_x = static_cast<GLfloat>(tmp);
        wxConfig::Get()->Read(_T("Board/OpenGL/RY"), &tmp, 0.0);
        r_y = static_cast<GLfloat>(tmp);
        wxConfig::Get()->Read(_T("Board/OpenGL/RZ"), &tmp, 0.0);
        r_z = static_cast<GLfloat>(tmp);
        wxConfig::Get()->Read(_T("Board/OpenGL/SX"), &tmp, 0.0);
        s_x = static_cast<GLfloat>(tmp);
        wxConfig::Get()->Read(_T("Board/OpenGL/SY"), &tmp, 0.0);
        s_y = static_cast<GLfloat>(tmp);
        wxConfig::Get()->Read(_T("Board/OpenGL/SZ"), &tmp, -BASE*DEFAULT_ZOOM);
        s_z = static_cast<GLfloat>(tmp);
        wxConfig::Get()->Read(_T("Board/OpenGL/Fovy"), &tmp, DEFAULT_FOVY);
        fovy = static_cast<GLfloat>(tmp);
    }
    else
    {
        r_x = 0.0f;
        r_y = 0.0f;
        r_z = 0.0f;
        s_x = 0.0f;
        s_y = 0.0f;
        s_z = -BASE*DEFAULT_ZOOM;
        fovy = DEFAULT_FOVY;
    }
}

void GLBoard::calculateSize()
{
    offset = BASE / (show_coords ? 8.0f : 10.0f);
    board_pixel_size = BASE * 2.0f - offset * 2.0f;
    wxASSERT(board_size != 0);
    square_size = board_pixel_size / (board_size - 1);
    stone_size = square_size / 2.0f * 0.95f;
}

void GLBoard::OnKeyDown(wxKeyEvent& e)
{
    int key = e.GetKeyCode();
    // wxLogDebug(wxString::Format("OnKeyDown: %d", key));

    // Numpad is not caught in OnChar

    switch (key)
    {
    case 392:  // NUMPAD PLUS
        s_z += zoom_unit;
        is_modified = true;
        if (fast_rendering && keydown_flag < 2)
            keydown_flag ++;
        break;
    case 394:  // NUMPAD MINUS
        s_z -= zoom_unit;
        is_modified = true;
        if (fast_rendering && keydown_flag < 2)
            keydown_flag ++;
        break;
    case 43:
    case 45:
    case WXK_RIGHT:
    case WXK_LEFT:
    case WXK_UP:
    case WXK_DOWN:
        if (fast_rendering && keydown_flag < 2)
            keydown_flag ++;
    default:
        e.Skip();
    }
}

void GLBoard::OnKeyUp(wxKeyEvent& e)
{
    switch (e.GetKeyCode())
    {
    case WXK_RIGHT:
    case WXK_LEFT:
    case WXK_UP:
    case WXK_DOWN:
    case 43:
    case 45:
    case 392:
    case 394:
        // Redraw scene if we were rotating while having a key pressed down
        // doing simple rendering
        if (keydown_flag > 1)
            is_modified = true;
        keydown_flag = 0;
    default:
        e.Skip();
    }
}

void GLBoard::OnChar(wxKeyEvent& e)
{
    int key = e.GetKeyCode();
    // wxLogDebug(wxString::Format("OnChar: %d", key));

    switch (key)
    {
    case WXK_RIGHT:
        if (e.ShiftDown())
        {
            s_x += shift_unit;
            is_modified = true;
        }
        else if (e.ControlDown())
        {
            r_z += rotate_unit;
            is_modified = true;
        }
        else if (e.AltDown())
        {
            r_y += rotate_unit;
            is_modified = true;
        }
        else
            navigate(NAVIGATE_DIRECTION_NEXT_MOVE);
        break;
    case WXK_LEFT:
        if (e.ShiftDown())
        {
            s_x -= shift_unit;
            is_modified = true;
        }
        else if (e.ControlDown())
        {
            r_z -= rotate_unit;
            is_modified = true;
        }
        else if (e.AltDown())
        {
            r_y -= rotate_unit;
            is_modified = true;
        }
        else
            navigate(NAVIGATE_DIRECTION_PREVIOUS_MOVE);
        break;
    case WXK_UP:
        if (e.ShiftDown())
        {
            s_y += shift_unit;
            is_modified = true;
        }
        else if (e.ControlDown())
        {
            r_x += rotate_unit;
            is_modified = true;
        }
        else
            navigate(NAVIGATE_DIRECTION_NEXT_VARIATION);
        break;
    case WXK_DOWN:
        if (e.ShiftDown())
        {
            s_y -= shift_unit;
            is_modified = true;
        }
        else if (e.ControlDown())
        {
            r_x -= rotate_unit;
            is_modified = true;
        }
        else
            navigate(NAVIGATE_DIRECTION_PREVIOUS_VARIATION);
        break;
    case 43:  // PLUS
        fovy += fovy_unit;
        is_modified = true;
        break;
    case 45:  // MINUS
        fovy -= fovy_unit;
        is_modified = true;
        break;
    case WXK_BACK:
        resetTransformations(true);
        is_modified = true;
        break;
    case 114:  // r
        // Reload textures. More convinient than restarting the application
        deleteTextures();
        createTextures();
        is_modified = true;
        break;
#ifdef MOUSEGRID_DEBUG
    case 109:  // m
        display_mouse_grid = !display_mouse_grid;
        is_modified = true;
        break;
#endif
    case WXK_HOME:
        navigate(NAVIGATE_DIRECTION_FIRST_MOVE);
        break;
    case WXK_END:
        navigate(NAVIGATE_DIRECTION_LAST_MOVE);
        break;
    }

    e.Skip();
}

void GLBoard::navigate(NavigationDirection direction)
{
    EventNavigate evt(direction);

#ifdef THREAD_SAFE
    // Thread safe, OnIdle will care for the update
    GetEventHandler()->AddPendingEvent(evt);
#else
    // Not thread safe. Should be ok here though
    GetEventHandler()->ProcessEvent(evt);

    if (evt.Ok())
        is_modified = true;
#endif
}

void GLBoard::OnMouseLeftDown(wxMouseEvent& e)
{
    int x, y;
    selectPick(e.GetX(), e.GetY(), x, y);
    handleMouseClick(x, y, 0);
}

void GLBoard::OnMouseRightDown(wxMouseEvent& e)
{
    int x, y;
    selectPick(e.GetX(), e.GetY(), x, y);
    handleMouseClick(x, y, 1);
}

#ifndef NO_ANIMATE
void GLBoard::OnTimer(wxTimerEvent& WXUNUSED(event))
{
    wxASSERT(multitextures);
    angle += 1.5f;
    is_modified = true;
    Refresh(false);
}
#endif

void GLBoard::OnIdle(wxIdleEvent& WXUNUSED(event))
{
    // Check if boardhandler has a modified position.
    // This will allow thread-safe calls
    if (!blocked && boardhandler->checkUpdate())
    {
        is_modified = true;
        block_cursor = true;  // Make sure no cursor update will intercept now
        cursor_update = false;
        Refresh(false);
    }
    // This happens when the board was rotated, OpenGL parameters were changed etc.
    // We ignore blocked status here, the user was doing something so there should
    // be an effect in any case
    else if (is_modified)
    {
        Refresh(false);
    }
}

void GLBoard::OnPaint(wxPaintEvent& WXUNUSED(event))
{
    Render();
}

void GLBoard::OnEnterWindow(wxMouseEvent& WXUNUSED(event))
{
    wxWindow::SetFocus();
}

void GLBoard::OnEraseBackground(wxEraseEvent& WXUNUSED(event))
{
    // Do nothing, to avoid flashing.
}

void GLBoard::OnSize(wxSizeEvent& event)
{
    // this is also necessary to update the context on some platforms
    wxGLCanvas::OnSize(event);

    is_modified = true;
    if (init_gl_flag)
        setupPerspective();
}

void GLBoard::InitGL()
{
    SetCurrent();

    LOG_OPENGL(wxString::Format(_T("Initialzing OpenGL on %s (%s) %s"),
                                glGetString(GL_RENDERER),
                                glGetString(GL_VERSION),
                                glGetString(GL_VENDOR)));

    // Common
    glShadeModel(GL_SMOOTH);
    glClearColor(static_cast<float>(back_color.Red()) / 255.0f,
                 static_cast<float>(back_color.Green()) / 255.0f,
                 static_cast<float>(back_color.Blue()) / 255.0f,
                 0.0f);
    glClearDepth(1.0f);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    glPolygonOffset(-2.0f, -1.0f);
    glEnable(GL_NORMALIZE);

    // Load images and create texture objects
    createTextures();
    LOG_OPENGL(_T("Textures created"));

    // Sphere mapping
    glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_SPHERE_MAP);
    glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_SPHERE_MAP);

    // Blending
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    // Hints
    glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
    glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
    glHint(GL_POLYGON_SMOOTH_HINT, GL_DONT_CARE);

    // Light
    glLightfv(GL_LIGHT0, GL_AMBIENT, LightAmbient);
    glLightfv(GL_LIGHT0, GL_DIFFUSE, LightDiffuse);
    glLightfv(GL_LIGHT0, GL_SPECULAR, LightSpecular);
    glLightfv(GL_LIGHT0, GL_POSITION, LightPosition);
    glEnable(GL_LIGHT0);

    // Initialize extensions
    // Multisample
    if (supportsExtension(_T("GL_ARB_multisample")))
    {
        LOG_OPENGL(_T("Multisample supported"));
        glEnable(GL_MULTISAMPLE_ARB);
    }
    else
        LOG_OPENGL(_T("Multisample not supported"));

    // Multitexture
#ifndef NO_MULTITEXTURES
    if (multitextures && !supportsExtension(_T("GL_ARB_multitexture")))
    {
        LOG_OPENGL(_T("Disabling multitextures, not supported"));
        multitextures = false;
    }
    else
    {
        LOG_OPENGL(_T("Multitextures supported"));
#ifdef _WIN32
        // Create function pointers on Windows
        if (initWin32Extensions())
        {
            wxASSERT(glActiveTextureARB != NULL && glMultiTexCoord2fARB != NULL);
            LOG_OPENGL(_T("Windows extension pointers initialized"));
        }
        else
        {
            LOG_OPENGL(_T("Failed to initialize Windows extension pointers."));
            multitextures = false;
        }
#endif // _WIN32
    }
#else  // !NO_MULTITEXTURES
    LOG_OPENGL(_T("Multitextures not supported by this build"));
#endif // !NO_MULTITEXTURES

    // Create PLIB font renderer. Must be created before createGobanList()
    font_renderer = new fntRenderer();

    // Create display lists
    createGobanList();
    createStoneLists();

    // Perspective and viewport
    setupPerspective();

    init_gl_flag = true;

    LOG_OPENGL(_T("OpenGL successfully initialized."));
}

void GLBoard::setupPerspective()
{
    // set GL viewport (not called by wxGLCanvas::OnSize on all platforms...)
    int w, h;
    GetClientSize(&w, &h);
#ifndef __WXMOTIF__
    if (GetContext())
#endif
    {
        SetCurrent();
        glViewport(0, 0, (GLint)w, (GLint)h);
    }
}

void GLBoard::prepareSupersampleTexture()
{
    GLint *data = new GLint[SUPERSAMPLE_TEX_SIZE * SUPERSAMPLE_TEX_SIZE * 3];
    glGenTextures(1, &renderTex);
    glBindTexture(GL_TEXTURE_2D, renderTex);
    glTexImage2D(GL_TEXTURE_2D, 0, 3, SUPERSAMPLE_TEX_SIZE, SUPERSAMPLE_TEX_SIZE,
                 0, GL_RGB, GL_UNSIGNED_BYTE, data);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
    delete data;
    renderTex_flag = true;
}

void GLBoard::createGobanList()
{
    wxLogDebug(_T("Creating Goban display list"));

    // Starpoint
    GLUquadric *quadric = gluNewQuadric();
    wxASSERT(quadric != NULL);
    gluQuadricDrawStyle(quadric, GLU_FILL);
    gluQuadricNormals(quadric, GLU_SMOOTH);
    starpoint_list = glGenLists(1);
    glNewList(starpoint_list, GL_COMPILE);
    glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
    gluDisk(quadric, 0.0, square_size/8.0, 16, 4);
    glEndList();
    gluDeleteQuadric(quadric);

    // Goban + Grid
    board_list = glGenLists(1);
    glNewList(board_list, GL_COMPILE);
    glPushMatrix();
    drawTextureCube();
    glTranslatef(0.0f, 0.0f, BASE/BOARD_HEIGHT + 0.001f);
    glDisable(GL_LIGHTING);
    drawGrid();
    // Draw coordinates in the display list with scissoring does not work
    if (show_coords && !use_scissor)
        drawCoordinates();
    glPopMatrix();
    glEndList();
}

void GLBoard::createStoneLists()
{
    wxLogDebug(_T("Creating stones display lists with %d fragments"), sphere_fragments);

    // Neutral, colorless stone. Used for shadows and ghosts
    neutral_stone_list = glGenLists(3);
    glNewList(neutral_stone_list, GL_COMPILE);
    glScalef(1.0f, 1.0f, 0.4f);
    glTranslatef(0.0f, 0.0f, stone_size);
    drawSphere(stone_size, sphere_fragments, multitextures);
    glEndList();

    // Black stone
    black_stone_list = neutral_stone_list + 1;
    glNewList(black_stone_list, GL_COMPILE);
    glMaterialfv(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, BlackLightAmbientDiffuse);
    glMaterialfv(GL_FRONT, GL_SPECULAR, BlackLightSpecular);
    glMaterialfv(GL_FRONT, GL_SHININESS, BlackLightShininess);
    glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
    glPushMatrix();
    glScalef(1.0f, 1.0f, 0.4f);
    glTranslatef(0.0f, 0.0f, stone_size);
    glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
    drawSphere(stone_size, sphere_fragments, multitextures);
    glPopMatrix();
    glEndList();

    // White stone
    white_stone_list = black_stone_list + 1;
    glNewList(white_stone_list, GL_COMPILE);
    glMaterialfv(GL_FRONT, GL_AMBIENT_AND_DIFFUSE, WhiteLightAmbientDiffuse);
    glMaterialfv(GL_FRONT, GL_SPECULAR, WhiteLightSpecular);
    glMaterialfv(GL_FRONT, GL_SHININESS, WhiteLightShininess);
    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    glPushMatrix();
    glScalef(1.0f, 1.0f, 0.4f);
    glTranslatef(0.0f, 0.0f, stone_size);
    glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
    drawSphere(stone_size, sphere_fragments, multitextures);
    glPopMatrix();
    glEndList();
}

void GLBoard::Render()
{
    wxPaintDC dc(this);

#ifndef __WXMOTIF__
    if (!GetContext())
    {
        wxFAIL_MSG(_T("*** No context in Render !"));
        return;
    }
#endif

    SetCurrent();

    if (!init_gl_flag)
    {
        InitGL();
        // Force instant redraw when render to texture is enabled,
        // else we get artifacts on startup
        if (render_to_texture)
        {
            is_modified = true;
            Refresh(false);
            return;
        }
    }

    // Draw ghost cursor
    if (!mayMove())
    {
        block_cursor = true;
        cursor = Position(-1, -1);
    }

    GLint viewport[4];
    glGetIntegerv (GL_VIEWPORT, viewport);

#if 0
    wxLogDebug("is_modified = %d, use_scissor = %d, cursor_update = %d, block_cursor = %d",
               is_modified, use_scissor, cursor_update, block_cursor);
    wxLogDebug("Last stone: %d/%d %d", lastStone.getX(), lastStone.getY(), lastStone.getColor());
    wxLogDebug("Cursor: %d/%d", cursor.getX(), cursor.getY());
#endif

    // If the board was modified or render_to_texture is disabled, redraw the scene
    if (is_modified || !render_to_texture)
    {
        // Configure blending, reflections, line antialias
        if (blend)
            glEnable(GL_BLEND);
        else
            glDisable(GL_BLEND);
        if (reflections)
            glEnable(GL_LIGHTING);
        else
            glDisable(GL_LIGHTING);
        if (line_antialias)
            glEnable(GL_LINE_SMOOTH);
        else
            glDisable(GL_LINE_SMOOTH);

        // Setup scene antialias
        bool do_antialias = false;
        int acsize = 2;
        const GLfloat *pjitter = j2;
        if (scene_antialias_quality_high)
        {
            acsize = 8;
            pjitter = j8;
        }

        // Check if we can do scene antialiasing.
        // We don't if a key is kept down while rotating.
        if (antialias_scene && keydown_flag <= 1)
        {
            do_antialias = true;
            glClear(GL_ACCUM_BUFFER_BIT);
        }

        // Setup projection matrix
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluPerspective(fovy,(GLfloat)viewport[2]/(GLfloat)viewport[3],0.1f,100.0f);

        // Check if we can do scissoring
        Stone modified;
        if (use_scissor && boardhandler->haveModifiedStone() && boardhandler->getGhosts().empty() && !had_ghosts)
            modified = boardhandler->getModifiedStone();
        else
            had_ghosts = false;

        // Jitter loop, only called once when scene antialias is disabled
        for (int jitter = do_antialias ? 0 : acsize-1; jitter < acsize; jitter++)
        {
            if ((!cursor_update || !use_scissor) && modified.getColor() == STONE_UNDEFINED)
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            if (do_antialias)
                accPerspective(fovy, (GLdouble) viewport[2]/(GLdouble) viewport[3], 0.1f, 100.0f,
                               pjitter[jitter*2], pjitter[jitter*2+1], 0.0f, 0.0f, 1.0f);

            // Setup modelview matrix
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
            glTranslatef(s_x, s_y, s_z);
            glRotatef(r_x, 1.0f, 0.0f, 0.0f);
            glRotatef(r_y, 0.0f, 1.0f, 0.0f);
            glRotatef(r_z, 0.0f, 0.0f, 1.0f);

            // Draw goban
            if (modified.getColor() == STONE_UNDEFINED)
            {
                glCallList(board_list);
                if (show_coords && use_scissor && !cursor_update)
                {
                    // Draw coordinates outside of board display list with scissoring
                    glPushMatrix();
                    glTranslatef(0.0f, 0.0f, BASE/BOARD_HEIGHT + 0.001f);
                    drawCoordinates();
                    glPopMatrix();
                }
            }

#ifdef MOUSEGRID_DEBUG
            // Mouse grid. Debugging only
            // TODO: This can be removed later
            if (display_mouse_grid)
            {
                glPushMatrix();
                drawMouseGrid(true);
                if (blend) glEnable(GL_BLEND);
                if (line_antialias) glEnable(GL_LINE_SMOOTH);
                glPopMatrix();
            }
#endif

            // Turn on light again for the stones
            if (!reflections && glIsEnabled(GL_LIGHTING))
                glDisable(GL_LIGHTING);
            else if (reflections && !glIsEnabled(GL_LIGHTING))
                glEnable(GL_LIGHTING);

            if (!cursor_update || !use_scissor)
            {
                // Draw stones
                if (modified.getColor() != STONE_UNDEFINED)
                {
                    // Scissored version. This is called if only a single stone was added.
                    // wxLogDebug("Scissored stone");
                    setupScissor(modified.getX(), modified.getY());
                    glDisable(GL_TEXTURE_2D);
                    drawStone(modified.getX(), modified.getY(), modified.getColor());
                    if (lastStone.getColor() != STONE_UNDEFINED)
                    {
                        setupScissor(lastStone.getX(), lastStone.getY());
                        bool shadows_old = shadows;
                        shadows = false;
                        drawStone(lastStone.getX(), lastStone.getY(), lastStone.getColor());
                        shadows = shadows_old;
                    }
                    glDisable(GL_SCISSOR_TEST);
                    lastStone = modified;
                }
                else
                {
                    // Non-scissored version. This is called when there were captures.
                    // wxLogDebug("Non scissored stone");
                    if (!boardhandler->getStones().empty())
                    {
                        // Fast rendering while rotating, disable line antialias
                        if (keydown_flag > 1 && glIsEnabled(GL_LINE_SMOOTH))
                            glDisable(GL_LINE_SMOOTH);

                        // Textures are enabled dynamically in drawStone() when needed
                        glDisable(GL_TEXTURE_2D);

                        // Loop through stones and draw them
                        ConstStonesIterator it;
                        for (it = boardhandler->getStones().begin(); it != boardhandler->getStones().end(); ++it)
                        {
                            if (!it->IsDead() && !it->IsSeki())
                                drawStone(it->getX(), it->getY(), it->getColor());
                            else
                            {
                                // Make dead/seki stones transparent
                                if (glIsEnabled(GL_LIGHTING))
                                    glDisable(GL_LIGHTING);
                                if (!glIsEnabled(GL_BLEND))
                                    glEnable(GL_BLEND);
                                glDepthMask(GL_FALSE);
                                drawGhostStone(it->getX(), it->getY(), it->getColor());
                                glDepthMask(GL_TRUE);
                                if (reflections)
                                    glEnable(GL_LIGHTING);
                                if (!blend)
                                    glDisable(GL_BLEND);
                                // Lots of state changes, this is slow. But dead stones
                                // don't occur often
                            }
                        }

                        // Switch on again what was disabled above for fast rendering
                        if (keydown_flag > 1)
                            glEnable(GL_LINE_SMOOTH);
                    }
                    lastStone = boardhandler->getModifiedStone();
                }
            }

            if (glIsEnabled(GL_LIGHTING))
                glDisable(GL_LIGHTING);

            // Draw ghosts
            if (!boardhandler->getGhosts().empty())
            {
                // Ghosts without blending makes no sense
                if (!glIsEnabled(GL_BLEND))
                    glEnable(GL_BLEND);

                // Make depth buffer read-only for better blending effect
                glDepthMask(GL_FALSE);

                // Loop through stones and draw them
                ConstStonesIterator it;
                for (it = boardhandler->getGhosts().begin(); it != boardhandler->getGhosts().end(); ++it)
                    drawGhostStone(it->getX(), it->getY(), it->getColor());

                glDepthMask(GL_TRUE);
                if (!blend)
                    glDisable(GL_BLEND);

                had_ghosts = true;
            }

            // Draw marks
            if (show_marks)
            {
                // The mark storage of the current move...
                Marks marks = boardhandler->getMarks();
                if (!marks.empty())
                {
                    ConstMarksIterator it;
                    for (it = marks.begin(); it != marks.end(); ++it)
                        drawMark(*it);
                }

                // ... and the last move marker
#ifndef NO_MULTITEXTURES
                if (!multitextures && (!cursor_update || !use_scissor))
#endif
                {
                    short lx = boardhandler->getLastMovePos().getX();
                    short ly = boardhandler->getLastMovePos().getY();
                    if (lx != 0 && ly != 0)
                        drawMark(lx, ly, MARK_CIRCLE);
                }
            }

            if (show_cursor && !block_cursor && cursor.getX() != -1 && cursor.getY() != -1 && !boardhandler->hasPosition(cursor))
            {
                if (!glIsEnabled(GL_BLEND))
                    glEnable(GL_BLEND);
                glDepthMask(GL_FALSE);
                if (cursor_update && use_scissor)
                    setupScissor(cursor.getX(), cursor.getY());
                drawGhostStone(cursor.getX(), cursor.getY(), boardhandler->getCurrentTurnColor());
                if (cursor_update && use_scissor)
                    glDisable(GL_SCISSOR_TEST);
                glDepthMask(GL_TRUE);
                if (!blend)
                    glDisable(GL_BLEND);
                cursor_update = false;
            }

            if (reflections)
                glEnable(GL_LIGHTING);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            if (do_antialias)
                glAccum(GL_ACCUM, 1.0f/acsize);
        }

        if (do_antialias)
            glAccum (GL_RETURN, 1.0);

        glFlush();
    }

    if (render_to_texture)
    {
        // Copy scene into texture
        glEnable(GL_TEXTURE_2D);
        // Prepare texture if not yet done
        if (!renderTex_flag)
            prepareSupersampleTexture();
        glBindTexture(GL_TEXTURE_2D, renderTex);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_DECAL);
        if (is_modified)
        {
            glReadBuffer(GL_BACK);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, viewport[2], viewport[3]);
        }

        // Render texture in color buffer
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluOrtho2D(0, 100, 0, 100);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_LIGHTING);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GLfloat xMax = (GLfloat)viewport[2] / (GLfloat)SUPERSAMPLE_TEX_SIZE;
        GLfloat yMax = (GLfloat)viewport[3] / (GLfloat)SUPERSAMPLE_TEX_SIZE;

        // If blur is enabled, jitter the texture
        for (int jitter = blur ? 0 : 7; jitter < 8; jitter++)
        {
            glPushMatrix();
            if (blur)
                glTranslatef(j8[jitter*2] * 100.0f / (GLfloat)viewport[2],
                             j8[jitter*2+1] * 100.0f / (GLfloat)viewport[3],
                             0.0f);
            glBegin(GL_QUADS);
            glTexCoord2f(0.0f, 0.0f); glVertex3f(0.0f, 0.0f, 0.0f);
            glTexCoord2f(0.0f, yMax); glVertex3f(0.0f, 100.0f, 0.0f);
            glTexCoord2f(xMax, yMax); glVertex3f(100.0f, 100.0f, 0.0f);
            glTexCoord2f(xMax, 0.0f); glVertex3f(100.0f, 0.0f, 0.0f);
            glEnd();
            glPopMatrix();
        }
        glEnable(GL_DEPTH_TEST);
        glFlush();
    }

    // All done, swap back and front buffer
    SwapBuffers();

    block_cursor = is_modified = false;
}

void GLBoard::selectPick(int x, int y, int &coordX, int &coordY)
{
    const int BUFSIZE = 32;
    GLuint selectBuf[BUFSIZE];
    GLint hits_buffer;
    GLint viewport[4];

    // Get viewport
    glGetIntegerv (GL_VIEWPORT, viewport);

    // Prepare select mode and names list
    glSelectBuffer(BUFSIZE, selectBuf);
    glRenderMode(GL_SELECT);
    glInitNames();
    glPushName(0);

    // Setup projection matrix
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    gluPickMatrix (x, viewport[3]-y, 1.0, 1.0, viewport);
    gluPerspective(fovy,(viewport[2]*1.0)/(viewport[3]*1.0),0.1f,100.0f);

    // Setup modelview matrix
    glMatrixMode(GL_MODELVIEW);

    // Draw the mouse grid
    glClear(GL_COLOR_BUFFER_BIT);
    drawMouseGrid();
    glFlush();

    // Get the mouse hits
    hits_buffer = glRenderMode(GL_RENDER);

    // Process hits
    GLuint *ptr = (GLuint*)selectBuf;
    for (int i=0; i<hits_buffer; i++)
    {
        ptr += 3;  // Skip z1, z2
        coordX = *ptr/100;
        coordY = *ptr - coordX*100;
        coordX += 1;
        coordY = board_size - coordY;
        return;  // Avoid adding multiple stones
    }

    // No hits
    coordX = coordY = 0;
}

void GLBoard::drawAntialiasCircle(GLfloat radius, Color color)
{
    // Get circle color
    if (color == STONE_BLACK)
        glColor4f(0.0f, 0.0f, 0.0f, 0.6f);
    else if (color == STONE_WHITE)
        glColor4f(1.0f, 1.0f, 1.0f, 0.6f);

    // If we have line antialias disabled, we need to turn it on for this
    if (!line_antialias && !glIsEnabled(GL_LINE_SMOOTH))
        glEnable(GL_LINE_SMOOTH);

    // If the circle has not been stored in a list, create it
    if (circle_list == EMPTY_LIST)
    {
        // Prepare list
        circle_list = glGenLists(1);
        glNewList(circle_list, GL_COMPILE);
        glLineWidth(1.0f);
        glDepthMask(GL_FALSE);
        drawCircle(radius);
        glLineWidth(1.0f);
        glDepthMask(GL_TRUE);
        glEndList();
    }

    // Call list and draw circle
    glCallList(circle_list);

    if (!line_antialias)
        glDisable(GL_LINE_SMOOTH);
}

void GLBoard::drawStone(short x, short y, Color color)
{
    bool lastMoveFlag =
        show_marks && textures && multitextures &&
        boardhandler->getLastMovePos().getX() == x &&
        boardhandler->getLastMovePos().getY() == y;

    y = board_size - y + 1;
    glPushMatrix();
    glTranslatef(-BASE + offset + (x-1)*square_size,
                 -BASE + offset + (y-1)*square_size,
                 BASE/BOARD_HEIGHT);

    // Black stone
    if (color == STONE_BLACK)
    {
        if (lastMoveFlag)
        {
            // Setup texture with the marker
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, texture_ids[TEXTURE_MARK_BLACK]);
#ifndef NO_ANIMATE
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();
            glRotatef(angle, 0.0f, 0.0f, 1.0f);
            glMatrixMode(GL_MODELVIEW);
#endif
            glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_BLEND);
            glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, black_marker_blend_rgba);
        }

        glCallList(black_stone_list);
    }

    // White stone
    else
    {
        if (textures)
        {
            // Setup first texture unit with the white stone grain
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, texture_ids[TEXTURE_WHITE]);
            // Enable sphere mapping
            glEnable(GL_TEXTURE_GEN_S);
            glEnable(GL_TEXTURE_GEN_T);
            glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

#ifndef NO_MULTITEXTURES
            if (lastMoveFlag)
            {
                // Setup second texture unit with the marker
                glActiveTextureARB(GL_TEXTURE1_ARB);
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, texture_ids[TEXTURE_MARK_WHITE]);
#ifndef NO_ANIMATE
                glMatrixMode(GL_TEXTURE);
                glLoadIdentity();
                glRotatef(angle, 0.0f, 0.0f, 1.0f);
                glMatrixMode(GL_MODELVIEW);
#endif
                glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
            }
#endif
        }

        glCallList(white_stone_list);

#ifndef NO_MULTITEXTURES
        // Disable second texture unit
        if (lastMoveFlag)
        {
            glDisable(GL_TEXTURE_2D);
            glActiveTextureARB(GL_TEXTURE0_ARB);
        }
#endif

        // Disable sphere mapping
        if (textures)
        {
            glDisable(GL_TEXTURE_GEN_S);
            glDisable(GL_TEXTURE_GEN_T);
        }
    }

    // No light and textures for shadows and circles
    glDisable(GL_LIGHTING);
    glDisable(GL_TEXTURE_2D);

    // Create shadows
    if (shadows)
    {
        glPushMatrix();
        glEnable(GL_POLYGON_OFFSET_FILL);  // Use polygon offset to avoid artifacts
        if (shadow_list == EMPTY_LIST)
        {
            wxLogDebug(_T("Creating shadow list."));
            shadow_list = glGenLists(1);
            glNewList(shadow_list, GL_COMPILE);
            glMultMatrixf((GLfloat*)floorShadow); // Project the shadow
            glColor4f(0.0f, 0.0f, 0.0f, 0.2f);    // 20% black
            glCallList(neutral_stone_list);
            glEndList();
        }
        glCallList(shadow_list);
        if (stone_antialias)
            drawAntialiasCircle(stone_size, STONE_UNDEFINED);  // Antialias shadow
        glDisable(GL_POLYGON_OFFSET_FILL);
        glPopMatrix();
    }

    // Draw am antialiased circle around the stone
    // Don't draw while rotating in fast rendering mode
    if (stone_antialias && keydown_flag <= 1)
    {
        glTranslatef(0.0f, 0.0f, stone_size*0.4f);
        drawAntialiasCircle(sphere_fragments == SPHERE_FRAGMENTS_LOW ? stone_size * 0.95f : stone_size,
                            color);
    }

    glPopMatrix();

    // Reset light
    if (reflections)
        glEnable(GL_LIGHTING);
}

void GLBoard::drawGhostStone(short x, short y, Color c)
{
    if ((x == 0 && y == 0) || (x == -1 && y == -1) ||
        (x == static_cast<short>(board_size)+1 && y == static_cast<short>(board_size)+1))
    {
        x = board_size+1;
        y = board_size;
    }

    y = board_size - y + 1;
    glPushMatrix();
    glTranslatef(-BASE + offset + (x-1)*square_size,
                 -BASE + offset + (y-1)*square_size,
                 BASE/BOARD_HEIGHT);

    if (c == STONE_BLACK)
        glColor4f(0.0f, 0.0f, 0.0f, 0.2f);  // 20% for black
    else
        glColor4f(1.0f, 1.0f, 1.0f, 0.3f);  // 30% for white

    glCallList(neutral_stone_list);

    glPopMatrix();
}

void GLBoard::drawMark(const Mark *mark)
{
    drawMark(mark->getX(), mark->getY(), mark->getType(), mark->getText());
}

void GLBoard::drawMark(short x, short y, MarkType t, const wxString& txt)
{
    // wxLogDebug(_T("GLBoard::drawMark %d/%d %d <%s>"), x, y, t, txt.c_str());

    // Off the board?
    if (x < 1 || x > board_size || y < 1 || y > board_size)
        return;

    glPushMatrix();

    // Get color of a possible stone at this position and use white for marks
    // on black stones. Blend marks with 80% alpha (not texts)
    const Stone *s = boardhandler->getStone(Position(x, y));
    if ((s == NULL || s->getColor() == STONE_WHITE) && t != MARK_TERR_WHITE)
        glColor4f(0.0f, 0.0f, 0.0f, t != MARK_TEXT ? 0.8f : 1.0f);
    else
        glColor4f(1.0f, 1.0f, 1.0f, t != MARK_TEXT ? 0.8f : 1.0f);

    glTranslatef(-BASE + offset + (x-1)*square_size,
                 -BASE + offset + (board_size - y)*square_size,
                 BASE/BOARD_HEIGHT +
                 (s != NULL ? stone_size*0.4f : 0.0f));  // Only move upwards when there is a stone

    glLineWidth(1.4f);
    glDisable(GL_DEPTH_TEST);

    switch(t)
    {
    case MARK_NONE:
        wxFAIL_MSG(_T("Trying to draw NONE mark"));
        break;
    case MARK_CIRCLE:
        drawCircle(stone_size*0.6f);
        break;
    case MARK_SQUARE:
        drawRect(stone_size*0.6f);
        break;
    case MARK_TRIANGLE:
        drawTriangle(stone_size*0.6f);
        break;
    case MARK_CROSS:
    // Maybe draw territory using something else like small stone ghosts
    case MARK_TERR_WHITE:
    case MARK_TERR_BLACK:
        drawCross(stone_size*0.6f);
        break;
    case MARK_TEXT:
        if (txt.empty())
            // TODO: Use some better default for the old L SGF tag
            drawTextMark(x, y, "A");
        else
            drawTextMark(x, y, txt);
        break;
    }

    glEnable(GL_DEPTH_TEST);
    glLineWidth(1.0f);
    glPopMatrix();
}

void GLBoard::drawGrid()
{
    glColor4f(0.0f, 0.0f, 0.0f, 1.0f);

    // Draw starpoints
    if (board_size > 11)
    {
        drawStarpoint(4, 4);
        drawStarpoint(board_size - 3, 4);
        drawStarpoint(4, board_size - 3);
        drawStarpoint(board_size - 3, board_size - 3);
        if (board_size % 2 != 0)
        {
            drawStarpoint((board_size + 1) / 2, 4);
            drawStarpoint((board_size + 1) / 2, board_size - 3);
            drawStarpoint(4, (board_size + 1) / 2);
            drawStarpoint(board_size - 3, (board_size + 1) / 2);
            drawStarpoint((board_size + 1) / 2, (board_size + 1) / 2);
        }
    }
    else
    {
        drawStarpoint(3, 3);
        drawStarpoint(3, board_size - 2);
        drawStarpoint(board_size - 2, 3);
        drawStarpoint(board_size - 2, board_size - 2);
        if (board_size % 2 != 0)
            drawStarpoint((board_size + 1) / 2, (board_size + 1) / 2);
    }

    // Draw the grid
    glDepthMask(GL_FALSE);
    glBegin(GL_LINES);
    unsigned short i;
    for (i=0; i<board_size; i++)
    {
        glColor4f(0.0f, 0.0f, 0.0f, 1.0f); glVertex3f( -BASE + offset + i*square_size,  BASE - offset,  0.0f);
        glColor4f(0.0f, 0.0f, 0.0f, 1.0f); glVertex3f( -BASE + offset + i*square_size, -BASE + offset,  0.0f);
    }
    for (i=0; i<board_size; i++)
    {
        glColor4f(0.0f, 0.0f, 0.0f, 1.0f); glVertex3f( -BASE + offset,  -BASE + offset + i*square_size,  0.0f);
        glColor4f(0.0f, 0.0f, 0.0f, 1.0f); glVertex3f(  BASE - offset,  -BASE + offset + i*square_size,  0.0f);
    }
    glEnd();
    glDepthMask(GL_TRUE);
}

void GLBoard::drawStarpoint(short x, short y)
{
    glPushMatrix();
    glTranslatef(-BASE + offset + (x-1)*square_size,
                 -BASE + offset + (y-1)*square_size,
                 0.0f);
    glCallList(starpoint_list);
    glPopMatrix();
}

void GLBoard::drawCoordinates()
{
    wxASSERT(font_renderer != NULL);
    if (font_renderer == NULL)
        return;

    // Somehow when loading the font in InitGL it won't display right.
    // But as this drawing happens in a display list, the overhead can be ignored
    fntTexFont font(wxGetApp().GetSharedPath() + COORDS_FONT);
    font.setGap(font.getGap()*2/3);
    font_renderer->setFont(&font);

    // Adjust font point size
    float font_size = square_size * 0.5;
    font_renderer->setPointSize(font_size);

    // Get bbox for 'A'
    float left, right, bottom, top;
    font.getBBox("A", font_size, font_renderer->getSlant(), &left, &right, &bottom, &top);

    int j;
    char c1[3];
    char c2[2];
    for (int i=0; i<board_size; i++)
    {
        j = i+1;

        c1[0] = j < 10 ? ' ' : '1';
        c1[1] = j == 10 ? '0' : '1' + (j-1) % 10;
        c1[2] = '\0';

        c2[0] = 'A' + (i<8?i:i+1);
        c2[1] = '\0';

        font_renderer->begin();

        // Left
        font_renderer->start2f(-BASE + (j<10?(right-left)*0.2f:(right-left)*0.25f),
                               -BASE + offset + i*square_size - (top-bottom)/2.0f);
        font_renderer->puts(c1);

        // Right
        font_renderer->start2f( BASE + - offset + (j<10?(right-left)*0.2f:(right-left)*0.25f),
                                -BASE + offset + i*square_size - (top-bottom)/2.0f);
        font_renderer->puts(c1);

        // Top
        font_renderer->start2f(-BASE + offset + i*square_size - (right-left)/2.0f,
                               BASE - (top-bottom)*1.5f);
        font_renderer->puts(c2);

        // Bottom
        font_renderer->start2f(-BASE + offset + i*square_size - (right-left)/2.0f,
                               -BASE + (top-bottom)/2.0f);
        font_renderer->puts(c2);

        font_renderer->end();
    }

    // Was enabled in plib font
    glDisable(GL_TEXTURE_2D);
}

void GLBoard::drawTextMark(short x, short y, const wxString &txt)
{
    wxASSERT(font_renderer != NULL);
    if (font_renderer == NULL)
        return;

    fntTexFont font(wxGetApp().GetSharedPath() + COORDS_FONT);
    font.setGap(font.getGap()*2/3);
    font_renderer->setFont(&font);

    // Adjust font point size
    float font_size = square_size * 0.9f;
    font_renderer->setPointSize(font_size);

    // Get bbox for text
    float left, right, bottom, top;
    font.getBBox(txt, font_size, font_renderer->getSlant(), &left, &right, &bottom, &top);

    font_renderer->begin();
    font_renderer->start2f(-(right-left)*0.53f,
                           -(top-bottom)*0.53f);
    font_renderer->puts(txt);
    font_renderer->end();

    // Was enabled in plib font
    glDisable(GL_TEXTURE_2D);
}

void GLBoard::drawTextureCube()
{
    // Enable and bind texture
    glEnable(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, texture_ids[TEXTURE_KAYA]);
    glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_DECAL);

    glBegin(GL_QUADS);
    // Front face
    glNormal3f(0.0f, 0.0f, 1.0f);
    glTexCoord2f(0.0f, 1.0f); glVertex3f( -BASE, -BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 1.0f); glVertex3f(  BASE, -BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 0.0f); glVertex3f(  BASE,  BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 0.0f); glVertex3f( -BASE,  BASE,  BASE/BOARD_HEIGHT);

    // Back face
    glNormal3f(0.0f, 0.0f, -1.0f);
    glTexCoord2f(1.0f, 1.0f); glVertex3f( -BASE, -BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 0.0f); glVertex3f( -BASE,  BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 0.0f); glVertex3f(  BASE,  BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 1.0f); glVertex3f(  BASE, -BASE, -BASE/BOARD_HEIGHT);

    // Top face
    glNormal3f(0.0f, 1.0f, 0.0f);
    glTexCoord2f(0.0f, 1.0f); glVertex3f( -BASE,  BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 0.0f); glVertex3f( -BASE,  BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 0.0f); glVertex3f(  BASE,  BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 1.0f); glVertex3f(  BASE,  BASE, -BASE/BOARD_HEIGHT);

    // Bottom face
    glNormal3f(0.0f, -1.0f, 0.0f);
    glTexCoord2f(1.0f, 1.0f); glVertex3f( -BASE, -BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 1.0f); glVertex3f(  BASE, -BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 0.0f); glVertex3f(  BASE, -BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 0.0f); glVertex3f( -BASE, -BASE,  BASE/BOARD_HEIGHT);

    // Right face
    glNormal3f(1.0f, 0.0f, 0.0f);
    glTexCoord2f(1.0f, 0.0f); glVertex3f(  BASE, -BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 1.0f); glVertex3f(  BASE,  BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 1.0f); glVertex3f(  BASE,  BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 0.0f); glVertex3f(  BASE, -BASE,  BASE/BOARD_HEIGHT);

    // Left face
    glNormal3f(-1.0f, 0.0f, 0.0f);
    glTexCoord2f(0.0f, 0.0f); glVertex3f( -BASE, -BASE, -BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 0.0f); glVertex3f( -BASE, -BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(1.0f, 1.0f); glVertex3f( -BASE,  BASE,  BASE/BOARD_HEIGHT);
    glTexCoord2f(0.0f, 1.0f); glVertex3f( -BASE,  BASE, -BASE/BOARD_HEIGHT);
    glEnd();

    glDisable(GL_TEXTURE_2D);
}

void GLBoard::drawMouseGrid(bool display)
{
    glLoadIdentity();
    glDisable(GL_TEXTURE_2D);
    glDisable(GL_LIGHTING);
    glDisable(GL_BLEND);
    glDisable(GL_LINE_SMOOTH);
    glDisable(GL_DEPTH_TEST);
    float z = s_z;
    if (display)
        z += 0.1f;
    glTranslatef(s_x, s_y, z);
    glRotatef(r_x, 1.0f, 0.0f, 0.0f);
    glRotatef(r_y, 0.0f, 1.0f, 0.0f);
    glRotatef(r_z, 0.0f, 0.0f, 1.0f);
    for (unsigned short i=0; i<board_size; i++)
    {
        for (unsigned short j=0; j<board_size; j++)
        {
            if (display)
                glColor4f(rand()%10/10.0f, rand()%10/10.0f, rand()%10/10.0f, 1.0f);
            glLoadName(i*100 + j);
            glBegin(GL_QUADS);
            glVertex3f(-BASE+offset + i*square_size - square_size/2,
                       -BASE+offset + j*square_size - square_size/2,
                       BASE/BOARD_HEIGHT);
            glVertex3f(-BASE+offset + i*square_size + square_size/2,
                       -BASE+offset + j*square_size - square_size/2,
                       BASE/BOARD_HEIGHT);
            glVertex3f(-BASE+offset + i*square_size + square_size/2,
                       -BASE+offset + j*square_size + square_size/2,
                       BASE/BOARD_HEIGHT);
            glVertex3f(-BASE+offset + i*square_size - square_size/2,
                       -BASE+offset + j*square_size + square_size/2,
                       BASE/BOARD_HEIGHT);
            glEnd();
        }
    }
    glEnable(GL_DEPTH_TEST);
}

void GLBoard::createTextures()
{
    wxLogDebug(_T("Creating textures..."));

    texture_ids = new GLuint[num_textures];
    glGenTextures(num_textures, texture_ids);

    prepareTexture(wxGetApp().GetSharedPath() + _T("data/kaya.jpg"),       texture_ids[TEXTURE_KAYA]);
    prepareTexture(wxGetApp().GetSharedPath() + _T("data/white_tex.jpg"),  texture_ids[TEXTURE_WHITE]);
    prepareTexture(wxGetApp().GetSharedPath() + _T("data/mark_white.jpg"), texture_ids[TEXTURE_MARK_WHITE]);
    prepareTexture(wxGetApp().GetSharedPath() + _T("data/mark_black.jpg"), texture_ids[TEXTURE_MARK_BLACK]);
}

void GLBoard::deleteTextures()
{
    if (texture_ids != NULL)
    {
        wxLogDebug(_T("Deleting textures..."));
        glDeleteTextures(num_textures, texture_ids);
        delete[] texture_ids;
    }
}

void GLBoard::prepareTexture(const wxString &filename, GLuint id)
{
    SDL_RWops *rwops = SDL_RWFromZZIP(filename, _T("rb"));
    if (rwops == NULL)
    {
        LOG_OPENGL(wxString::Format(_T("Failed to load rwops: %s"), filename.c_str()));
        return;
    }
    SDL_Surface *s = IMG_Load_RW(rwops, 1);
    if (s == NULL)
    {
        LOG_OPENGL(wxString::Format(_T("Failed to load rwops: %s"), filename.c_str()));
        return;
    }

    // Bind image to texture
    glBindTexture(GL_TEXTURE_2D, id);

    // Configure texture
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexImage2D(GL_TEXTURE_2D, 0, 3, s->w, s->h, 0, GL_RGB, GL_UNSIGNED_BYTE, static_cast<unsigned char*>(s->pixels));
    if (texture_quality_high)
    {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
    }
    else
    {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    }

    // Create Mipmaps
    gluBuild2DMipmaps(GL_TEXTURE_2D, 3, s->w, s->h, GL_RGB, GL_UNSIGNED_BYTE, static_cast<unsigned char*>(s->pixels));

    // Cleanup
    SDL_FreeSurface(s);
}

void GLBoard::reconfigureTextureQuality()
{
    for (size_t i=0; i<num_textures; i++)
    {
        glBindTexture(GL_TEXTURE_2D, texture_ids[i]);

        if (texture_quality_high)
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        }
        else
        {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        }
    }
}

void GLBoard::handleMouseClick(int x, int y, int button)
{
    // Validate point
    if (x < 1 || x > static_cast<int>(board_size) ||
        y < 1 || y > static_cast<int>(board_size))
        return;

    // Normal move: Left button plays, right button ignored
    if (editMode == EDIT_MODE_NORMAL)
    {
        if (button)  // Ignore right button
            return;
        EventPlayMove evt(x, y);
        GetEventHandler()->ProcessEvent(evt);
        if (evt.Ok())
            is_modified = true;
        return;
    }

    // Others are handled in Board::handleMouseClick()
    cursor = Position(-1, -1);
    Board::handleMouseClick(x, y, button);
}

void GLBoard::setViewParameter(int para, bool value)
{
    switch (para)
    {
    case VIEW_SHOW_MARKS:
        show_marks = value;
        break;
    case VIEW_SHOW_COORDS:
        show_coords = value;
        if (init_gl_flag)
            recalculateBoard();
        break;
    case VIEW_SHOW_CURSOR:
        show_cursor = value;
        if (show_cursor)
            Connect(wxID_ANY, wxEVT_MOTION,
                    (wxObjectEventFunction) (wxEventFunction) (wxMouseEventFunction) &GLBoard::OnMouseMotion);
        else
            Disconnect(wxID_ANY, wxEVT_MOTION,
                       (wxObjectEventFunction) (wxEventFunction) (wxMouseEventFunction) &GLBoard::OnMouseMotion);
        cursor = Position(-1, -1);
        break;
    case VIEW_USE_SCALED_FONT:
        // Unused
        break;
    case VIEW_USE_BACKGROUND_IMAGE:
        // Not yet implemented
        break;
    default:
        wxFAIL_MSG(_T("Unknown parameter"));
        return;
    }

    is_modified = true;
}

void GLBoard::setOGLViewParameter(OGLConfig &config)
{
    reflections = config.reflections;
    shadows = config.shadows;
    render_to_texture = config.render_to_texture;
    blur = config.blur;
    fast_rendering = config.fast_rendering;
    blend = config.blending;
    line_antialias = config.antialias_lines;
    stone_antialias = config.antialias_stones;
    antialias_scene = config.antialias_scene;
    scene_antialias_quality_high = config.antialias_scene_quality != 0;
    if (textures != config.textures)
    {
        textures = config.textures;
        if (init_gl_flag)
        {
            glDeleteLists(neutral_stone_list, 3);
            createStoneLists();
        }
    }
    if (texture_quality_high != (config.textures_quality != 0))
    {
        texture_quality_high = config.textures_quality != 0;
        if (init_gl_flag)
            reconfigureTextureQuality();
    }
    if (multitextures != config.multitextures)
    {
#ifndef NO_MULTITEXTURES
        if (config.multitextures && init_gl_flag && !supportsExtension(_T("GL_ARB_multitexture")))
        {
            wxMessageBox(_("Multitextures are not supported by your graphiccard.\n"
                           "Are you running in software mode?\n"
                           "Try the glview tool at http://www.realtech-vr.com/glview/"),
                         _("Information"), wxOK | wxICON_INFORMATION, parentFrame);
            config.multitextures = multitextures = false;
        }
        else
        {
            multitextures = config.multitextures;
            if (init_gl_flag)
            {
                glDeleteLists(neutral_stone_list, 3);
                createStoneLists();
            }
        }
#ifndef NO_ANIMATE
        wxASSERT(timer != NULL);
        if (value)
            timer->Start(MULITEXTURE_ANIMATION_DELAY);
        else
            timer->Stop();
#endif
#else
        wxMessageBox(_("Multitextures are not supported by this build."),
                     _("Information"), wxOK | wxICON_INFORMATION, parentFrame);
        config.multitextures = multitextures = false;
#endif
    }
    if ((sphere_fragments == SPHERE_FRAGMENTS_HIGH) != (config.stone_quality != 0))
    {
        sphere_fragments = config.stone_quality ? SPHERE_FRAGMENTS_HIGH : SPHERE_FRAGMENTS_LOW;
        if (init_gl_flag)
        {
            glDeleteLists(neutral_stone_list, 3);
            createStoneLists();
        }
    }
    if (use_scissor != config.use_scissor)
    {
        use_scissor = config.use_scissor;
        if (init_gl_flag)
        {
            glDeleteLists(board_list, 1);
            createGobanList();
        }
    }

    is_modified = true;
}

void GLBoard::updateBackgroundColor(wxColour c)
{
    // Convert wxColour into glColor
    glClearColor(static_cast<float>(c.Red()) / 255.0f,
                 static_cast<float>(c.Green()) / 255.0f,
                 static_cast<float>(c.Blue()) / 255.0f,
                 0.0f);

    back_color = c;
    is_modified = true;
}

void GLBoard::setupScissor(short x, short y)
{
    // Setup projection matrix
    GLint viewport[4];
    glGetIntegerv (GL_VIEWPORT, viewport);
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();
    gluPerspective(fovy,(viewport[2]*1.0f)/(viewport[3]*1.0f),0.1f,100.0f);

    // Setup modelview matrix
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();
    glTranslatef(s_x, s_y, s_z);
    glRotatef(r_x, 1.0, 0.0, 0.0);
    glRotatef(r_y, 0.0, 1.0, 0.0);
    glRotatef(r_z, 0.0, 0.0, 1.0);

    y = board_size - y + 1;
    glTranslatef(-BASE + offset + (x-1)*square_size,
                 -BASE + offset + (y-1)*square_size,
                 BASE/BOARD_HEIGHT);

    GLdouble modelMatrix[16], projMatrix[16];
    glGetDoublev(GL_MODELVIEW_MATRIX, modelMatrix);
    glGetDoublev(GL_PROJECTION_MATRIX, projMatrix);
    GLfloat objx1 = -square_size;
    GLfloat objy1 = -square_size;
    GLfloat objx2 = -square_size;
    GLfloat objy2 =  square_size;
    GLfloat objx3 =  square_size;
    GLfloat objy3 = -square_size;
    GLfloat objx4 =  square_size;
    GLfloat objy4 =  square_size;
    GLfloat z = stone_size * 0.4f;
    GLdouble winx1, winy1, winz1, winx2, winy2, winz2, winx3, winy3, winz3, winx4, winy4, winz4;

    gluProject(objx1, objy1, z, modelMatrix, projMatrix, viewport, &winx1, &winy1, &winz1);
    gluProject(objx2, objy2, z, modelMatrix, projMatrix, viewport, &winx2, &winy2, &winz2);
    gluProject(objx3, objy3, z, modelMatrix, projMatrix, viewport, &winx3, &winy3, &winz3);
    gluProject(objx4, objy4, z, modelMatrix, projMatrix, viewport, &winx4, &winy4, &winz4);

    GLdouble winx_min = min(min(winx1, winx2), min(winx3, winx4));
    GLdouble winy_min = min(min(winy1, winy2), min(winy3, winy4));
    GLfloat winx_max = max(max(winx1, winx2), max(winx3, winx4));
    GLfloat winy_max = max(max(winy1, winy2), max(winy3, winy4));

    GLfloat width = winx_max - winx_min;
    GLfloat height = winy_max - winy_min;

    glEnable(GL_SCISSOR_TEST);
    glScissor(static_cast<int>(winx_min), static_cast<int>(winy_min),
              static_cast<int>(width), static_cast<int>(height));

    glPopMatrix();
    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
}

void GLBoard::OnMouseMotion(wxMouseEvent& e)
{
    // Check if a cursor display is allowed.
    if (!init_gl_flag || !show_cursor || block_cursor || !mayMove())
        return;

    int x, y;
    selectPick(e.GetX(), e.GetY(), x, y);

    // Off the board?
    if (x < 1 || x > static_cast<int>(board_size) ||
        y < 1 || y > static_cast<int>(board_size))
    {
        // Oops, invalid
        if (cursor.getX() != -1 && cursor.getY() != -1)
        {
            cursor = Position(-1, -1);
            is_modified = true;
        }
        return;
    }

    // Cursor still on the same spot?
    if (x == cursor.getX() && y == cursor.getY())
        return;

    // A stone or ghost on this spot?
    if (boardhandler->hasPosition(Position(x, y)) ||
        boardhandler->hasGhost(Position(x, y)))
        return;

    cursor = Position(x, y);

    // Tell Render() to use scissoring
    cursor_update = true;
    is_modified = true;
}
