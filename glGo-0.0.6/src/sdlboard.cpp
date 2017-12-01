/*
 * sdlboard.cpp
 *
 * $Id: sdlboard.cpp,v 1.34 2003/11/06 04:35:02 peter Exp $
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
#pragma implementation "sdlboard.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/config.h>
#include <wx/image.h>
#include <SDL/SDL.h>
#include <SDL/SDL_image.h>
#include <SDL/SDL_ttf.h>
#include <SDL/SDL_rotozoom.h>
#include <SDL/SDL_gfxPrimitives.h>
#include "sdlboard.h"
#include "SDL_rwops_zzip.h"
#include "mainframe.h"
#include "boardhandler.h"
#include "glGo.h"
#include "gamedata.h"
#include "utils/utils.h"

#define ALPHA 240
#define NUMBER_WHITE_STONES 8
#define FONT_CACHE_SIZE 38

#define UPDATE_STATUS_UNINITIALIZED 0
#define UPDATE_STATUS_ALL 1
#define UPDATE_STATUS_HAVE_CACHE 2
#define UPDATE_STATUS_CURSOR 4

// Static variables
#if SDL_BYTEORDER == SDL_BIG_ENDIAN
static const Uint32 rmask = 0xff000000;
static const Uint32 gmask = 0x00ff0000;
static const Uint32 bmask = 0x0000ff00;
static const Uint32 amask = 0x000000ff;
static const Uint32 amask_transparent = 0x00000080;
#else
static const Uint32 rmask = 0x000000ff;
static const Uint32 gmask = 0x0000ff00;
static const Uint32 bmask = 0x00ff0000;
static const Uint32 amask = 0xff000000;
static const Uint32 amask_transparent = 0x80000000;
#endif
bool SDLBoard::video_init_flag = false;
SDL_Surface* SDLBoard::kaya_img = NULL;
SDL_Surface* SDLBoard::stone_black_img = NULL;
SDL_Surface** SDLBoard::stones_white_img = NULL;
SDL_Surface* SDLBoard::table_img = NULL;

#ifdef __VISUALC__
// Workaround for idiotic MS compiler bug
#define for(x) if(1) for(x)
#endif


BEGIN_EVENT_TABLE(SDLBoard, wxPanel)
    EVT_CALCULATE_LAYOUT(SDLBoard::OnCalculateLayout)
    EVT_SIZE(SDLBoard::OnSize)
    EVT_ERASE_BACKGROUND(SDLBoard::OnEraseBackground)
    EVT_PAINT(SDLBoard::OnPaint)
    EVT_IDLE(SDLBoard::OnIdle)
    EVT_LEFT_DOWN(SDLBoard::OnMouseLeftDown)
    EVT_RIGHT_DOWN(SDLBoard::OnMouseRightDown)
    EVT_CHAR(SDLBoard::OnChar)
END_EVENT_TABLE()


SDLBoard::SDLBoard(MainFrame *parent, int id)
    : wxPanel((wxWindow*)parent, id),
      Board(parent)
{
    screen = screen2 = kaya = stone_black = NULL;
    ttf_font = NULL;
    for (int i=0; i<FONT_CACHE_SIZE; i++)
        text[i] = NULL;
    stones_white = new SDL_Surface*[NUMBER_WHITE_STONES];
    for (int i=0; i<NUMBER_WHITE_STONES; i++)
        stones_white[i] = NULL;
    square_size = width = height = offset = offsetX = offsetY = board_pixel_size = -1;
    update_status = UPDATE_STATUS_UNINITIALIZED;
    cursor = old_cursor = Position(-1, -1);
    block_cursor = false;

    if (show_cursor)
        Connect(wxID_ANY, wxEVT_MOTION,
                (wxObjectEventFunction) (wxEventFunction) (wxMouseEventFunction) &SDLBoard::OnMouseMotion);

    // Init SDL video system and TTF fonts if not yet done
    if (!video_init_flag)
    {
        if(SDL_WasInit(SDL_INIT_VIDEO) == 0)
        {
            if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_NOPARACHUTE) < 0)
            {
                LOG_SDL("Failed to init SDL video");
                return;
            }
        }
        if (SDL_SetVideoMode(0, 0, 0, SDL_SWSURFACE) < 0)
            LOG_SDL("Failed to set SDL video mode");
        // Unsure if this is needed at all. If it fails, we try to ignore it.
        // On my Linux box this works without SDL_SetVideoMode, but according to the SDL
        // docs you need to call this before doing SDL video operations. No idea...
        else
            LOG_SDL("SDL video system initialized");

        if(TTF_Init() == -1)
        {
            LOG_SDL(wxString::Format("TTF_Init: %s\n", TTF_GetError()));
            return;
        }
    }
    video_init_flag = true;

    // Load unscaled images. These are shared among all instances of this class
    if (kaya_img == NULL)
    {
        // Kaya
        SDL_RWops *rwops = SDL_RWFromZZIP(wxGetApp().GetSharedPath() + _T("data/kaya.jpg"), "rb");
        if (rwops == NULL ||
            (kaya_img = IMG_Load_RW(rwops, 1)) == NULL)
            LOG_SDL(wxString::Format(_T("Failed to load image 'kaya.jpg': %s\n"), SDL_GetError()));

        // White stones
        stones_white_img = new SDL_Surface*[NUMBER_WHITE_STONES];
        for (int i=0; i<NUMBER_WHITE_STONES; i++)
        {
            rwops = SDL_RWFromZZIP(wxGetApp().GetSharedPath() + wxString::Format(_T("data/hyuga%d.png"), (i+1)), "rb");
            if (rwops == NULL ||
                (stones_white_img[i] = IMG_Load_RW(rwops, 1)) == NULL)
                LOG_SDL(wxString::Format(_T("Failed to load image 'hyuga%d.png': %s\n"), (i+1), SDL_GetError()));
        }

        // Black stone
        rwops = SDL_RWFromZZIP(wxGetApp().GetSharedPath() + _T("data/blk.png"), "rb");
        if (rwops == NULL ||
            (stone_black_img = IMG_Load_RW(rwops, 1)) == NULL)
            LOG_SDL(wxString::Format(_T("Failed to load image 'blk.png': %s\n"), SDL_GetError()));

        // Table
        rwops = SDL_RWFromZZIP(wxGetApp().GetSharedPath() + _T("data/table.jpg"), "rb");
        if (rwops == NULL ||
            (table_img = IMG_Load_RW(rwops, 1)) == NULL)
            LOG_SDL(wxString::Format(_T("Failed to load image 'table.jpg': %s\n"), SDL_GetError()));

        wxLogDebug("Images loaded");
    }

    // Create pseudo-random matrix for white stones
    whitestonematrix = new int*[board_size];
    for (int i=0; i<board_size; i++)
    {
        whitestonematrix[i] = new int[board_size];
        for (int j=0; j<board_size; j++)
            whitestonematrix[i][j] = rand() % NUMBER_WHITE_STONES;
    }

    // Get background color from config
    back_color = readColorFromConfig(_T("Board/SDL/BackColor"));
    wxConfig::Get()->Read(_T("Board/SDL/ScaledFont"), &scaled_font, true);
    wxConfig::Get()->Read(_T("Board/SDL/BackImage"), &background_image, true);
}

SDLBoard::~SDLBoard()
{
    wxLogDebug("~SDLBoard()");

    if (kaya != NULL)
        SDL_FreeSurface(kaya);
    for (int i=0; i<NUMBER_WHITE_STONES; i++)
    {
        if (stones_white[i] != NULL)
        {
            SDL_FreeSurface(stones_white[i]);
            stones_white[i] = NULL;
        }
    }
    delete[] stones_white;
    if (stone_black != NULL)
        SDL_FreeSurface(stone_black);
    if (screen != NULL)
        SDL_FreeSurface(screen);
    if (screen2 != NULL)
        SDL_FreeSurface(screen2);
    screen = screen2 = kaya = stone_black = NULL;
    stones_white = NULL;

    freeTTFFont();

    for (int i=0; i<board_size; i++)
        delete whitestonematrix[i];
    delete[] whitestonematrix;

    while (GetEventHandler() != this)
        PopEventHandler(true);
}

void SDLBoard::Cleanup()
{
    if (kaya_img != NULL)
    {
        SDL_FreeSurface(kaya_img);
        kaya_img = NULL;
    }
    if (stones_white_img != NULL)
    {
        for (int i=0; i<NUMBER_WHITE_STONES; i++)
        {
            if (stones_white_img[i] != NULL)
            {
                SDL_FreeSurface(stones_white_img[i]);
                stones_white_img[i] = NULL;
            }
        }
        delete[] stones_white_img;
    }
    if (stone_black_img != NULL)
        SDL_FreeSurface(stone_black_img);
    if (table_img != NULL)
        SDL_FreeSurface(table_img);
    kaya_img = stone_black_img = table_img = NULL;
    stones_white_img = NULL;

    TTF_Quit();
    SDL_Quit();
}

void SDLBoard::freeTTFFont()
{
    if (ttf_font != NULL)
    {
        TTF_CloseFont(ttf_font);
        ttf_font = NULL;
        for(int i=0; i<FONT_CACHE_SIZE; i++)
        {
            if(text[i] != NULL)
                SDL_FreeSurface(text[i]);
            text[i] = NULL;
        }
    }
}

wxString SDLBoard::getSDLInfo()
{
    char sdl_info[256];
    char drivername[32];
    if (SDL_VideoDriverName(drivername, 32) != NULL)
        sprintf(sdl_info, "Driver: %s\nDepth: %u\n",
                drivername,
                SDL_GetVideoInfo()->vfmt->BitsPerPixel);
    return wxString(sdl_info);
}

void SDLBoard::calculateSize()
{
    int margin = 5;  // Fixed margin
    GetClientSize(&width, &height);
    int w = width - margin * 2;
    int h = height - margin * 2;

    offset = w < h ? w * (show_coords?4:2) / 100 : h * (show_coords?4:2) / 100;

    square_size = w < h ? (w - 2*offset) / board_size : (h - 2*offset) / board_size;
    // Should not happen, but safe is safe.
    if (square_size == 0)
        square_size = 1;

    offset += square_size / 2;
    board_pixel_size = square_size * (board_size - 1);

    // Center the board in canvas
    offsetX = margin + (w - board_pixel_size) / 2;
    offsetY = margin + (h - board_pixel_size) / 2;

    // Rescale images

    // Free surfaces first
    if (kaya != NULL)
        SDL_FreeSurface(kaya);
    for (int i=0; i<NUMBER_WHITE_STONES; i++)
    {
        if (stones_white[i] != NULL)
        {
            SDL_FreeSurface(stones_white[i]);
            stones_white[i] = NULL;
        }
    }
    if (stone_black != NULL)
        SDL_FreeSurface(stone_black);
    kaya = stone_black = NULL;

    // Kaya
    int bsize = board_pixel_size + offset * 2;
    wxASSERT(kaya_img != NULL);
    kaya = zoomSurface(kaya_img,
                       bsize / (double)(kaya_img->w),
                       bsize / (double)(kaya_img->h),
                       SMOOTHING_ON);

    // White stones
    for (int i=0; i<NUMBER_WHITE_STONES; i++)
    {
        wxASSERT(stones_white_img[i] != NULL);
        stones_white[i] = zoomSurface(stones_white_img[i],
                                      (square_size+1)/(double)stones_white_img[i]->w,
                                      (square_size+1)/(double)stones_white_img[i]->h,
                                      SMOOTHING_ON);
    }

    // Black stone
    wxASSERT(stone_black_img != NULL);
    stone_black = zoomSurface(stone_black_img,
                              (square_size+1)/(double)stone_black_img->w,
                              (square_size+1)/(double)stone_black_img->h,
                              SMOOTHING_ON);

    // Prepare kaya board with grid, starpoints and coordinates
    drawBoard();

    // Create SDL_Surface which will be blitted into memory in OnPaint
    // The width and height must be multiples of 4 (this took me quite a while to figure out)
    if (screen != NULL)
        SDL_FreeSurface(screen);
    screen = SDL_CreateRGBSurface(SDL_SWSURFACE, width-width%4+4, height-height%4+4, 24,
                                  rmask, gmask, bmask, amask);
    if (screen == NULL)
    {
        LOG_SDL(_T("Critical error: Could not allocate memory for screen"));
        wxFAIL_MSG(_T("Critical error: Could not allocate memory for screen"));
        // Not much error handling to do. If this fails, we are doomed anyways.
        return;
    }
    if (screen2 != NULL)
        SDL_FreeSurface(screen2);
    screen2 = SDL_CreateRGBSurface(SDL_SWSURFACE, screen->w, screen->h, 24,
                                   rmask, gmask, bmask, amask);
    if (screen2 == NULL)
    {
        LOG_SDL(_T("Critical error: Could not allocate memory for screen2"));
        wxFAIL_MSG(_T("Critical error: Could not allocate memory for screen2"));
    }
    SDL_SetAlpha(screen, 0, 128);
    SDL_SetAlpha(screen2, 0, 128);

    // Load ttf font with size square_size/2
    // If an error occurs, switch to fixed font which should never fail
    freeTTFFont();
    if (scaled_font)
    {
        bool ttf_err = false;

        // zzip fails on this for some reason. Also we load this on each resize, so not using zzip is no bad idea.
        SDL_RWops *rwops = SDL_RWFromFile(wxGetApp().GetSharedPath() + _T("FreeSans.ttf"), "rb");
        if (rwops == NULL)
        {
            LOG_SDL(_T("Could not open file 'FreeSans.ttf'"));
            ttf_err = true;
        }
        else if ((ttf_font = TTF_OpenFontRW(rwops, 1, square_size/2)) == NULL)
        {
            LOG_SDL(wxString::Format(_T("TTF_OpenFont: %s\n"), TTF_GetError()));
            ttf_err = true;
        }
        wxLogDebug("Font loaded. Size = %d, Error = %d", square_size/2, ttf_err);

        // Cache letters
        if (!ttf_err)
        {
            SDL_Color fg = {0,0,0,255};
            char c[3];
            for (int i=0; i<FONT_CACHE_SIZE; i++)
            {
                if (i < 19)
                {
                    int j = board_size-i;
                    c[0] = j < 10 ? ' ' : '1';
                    c[1] = j == 10 ? '0' : '1' + (j-1) % 10;
                    c[2] = '\0';
                }
                else
                {
                    c[0] = 'A' + (i-19<8?i-19:i-18);
                    c[1] = '\0';
                }
                text[i] = TTF_RenderText_Blended(ttf_font, c, fg);
                if (text[i] == NULL)
                {
                    LOG_SDL(wxString::Format(_T("TTF_RenderText_Blended: %s\n"), TTF_GetError()));
                    scaled_font = false;
                    freeTTFFont();
                    break;
                }
                TTF_SizeText(ttf_font, c, &text_metrics[i][0], &text_metrics[i][1]);
            }
            wxLogDebug("Font cached");
        }
        else
        {
            scaled_font = false;
            freeTTFFont();
        }
    }

    update_status = UPDATE_STATUS_UNINITIALIZED;
}

void SDLBoard::OnEraseBackground(wxEraseEvent& WXUNUSED(event))
{
}

void SDLBoard::OnIdle(wxIdleEvent& WXUNUSED(event))
{
    // Check if boardhandler has a modified position.
    // This will allow thread-safe calls
    if (!blocked && boardhandler->checkUpdate())
    {
        block_cursor = true;  // Make sure no cursor update will intercept now
        Refresh(false);
        update_status = UPDATE_STATUS_ALL;
    }
    // We ignore blocked status here, the user was doing something so there should
    // be an effect in any case
    else if (is_modified)
    {
        Refresh(false);
    }
}

void SDLBoard::OnSize(wxSizeEvent& WXUNUSED(event))
{
    calculateSize();
    is_modified = true;
}

void SDLBoard::OnPaint(wxPaintEvent& WXUNUSED(event))
{
    wxPaintDC dc(this);
    wxMemoryDC mdc;

    if (screen == NULL || screen2 == NULL)
        calculateSize();

    if (!mayMove())
    {
        block_cursor = true;
        cursor = old_cursor = Position(-1, -1);
    }

    // Redraw stones and marks
    if (!(update_status & UPDATE_STATUS_CURSOR))
    {
        // Background
        if (background_image)
        {
            SDL_Rect r;
            for (int i=0; i<width; i+=table_img->w)
            {
                for (int j=0; j<height; j+=table_img->h)
                {
                    r.x = i;
                    r.y = j;
                    SDL_BlitSurface(table_img, NULL, screen, &r);
                }
            }
        }
        else
            SDL_FillRect(screen, NULL, SDL_MapRGB(screen->format, back_color.Red(), back_color.Green(), back_color.Blue()));

        // Blit kaya (includes grid and coordinates already) into screen
        SDL_Rect dest_rect;
        dest_rect.x = offsetX - offset;
        dest_rect.y = offsetY - offset;
        SDL_BlitSurface(kaya, NULL, screen, &dest_rect);

        // Loop through stones and draw them
        ConstStonesIterator it;
        for (it = boardhandler->getStones().begin(); it != boardhandler->getStones().end(); ++it)
            drawStone(it->getX(), it->getY(), it->getColor(),
                      it->IsDead() || it->IsSeki());  // Make dead/seki stones transparent

        // Draw ghosts
        if (!boardhandler->getGhosts().empty())
        {
            // Loop through stones and draw them with transparency enabled
            ConstStonesIterator it;
            for (it = boardhandler->getGhosts().begin(); it != boardhandler->getGhosts().end(); ++it)
                drawStone(it->getX(), it->getY(), it->getColor(), true);
        }

        // Draw marks
        if (show_marks)
        {
            // The mark storage of the current move...
            Color col;
            Marks marks = boardhandler->getMarks();
            if (!marks.empty())
            {
                ConstMarksIterator it;
                for (it = marks.begin(); it != marks.end(); ++it)
                {
                    // Get color of a possible stone at this position and use white for marks
                    // on black stones
                    const Stone *s = boardhandler->getStone(Position((*it)->getX(), (*it)->getY()));
                    if (s == NULL)
                        col = STONE_UNDEFINED;
                    else if (s->getColor() == STONE_WHITE)
                        col = STONE_BLACK;
                    else
                        col = STONE_WHITE;
                    drawMark((*it)->getX(), (*it)->getY(), col, (*it)->getType(), (*it)->getText());
                }
            }

            // ... and the last move marker
            short lx = boardhandler->getLastMovePos().getX();
            short ly = boardhandler->getLastMovePos().getY();
            if (lx != 0 && ly != 0)
            {
                const Stone *s = boardhandler->getStone(Position(lx, ly));
                if (s == NULL)
                    col = STONE_UNDEFINED;
                else if (s->getColor() == STONE_WHITE)
                    col = STONE_BLACK;
                else
                    col = STONE_WHITE;
                drawMark(lx, ly, col, MARK_CIRCLE);
            }
        }

        update_status = UPDATE_STATUS_ALL;
    }
    // Only draw the cursor ghost
    else if (show_cursor && !block_cursor && update_status & UPDATE_STATUS_CURSOR)
    {
        // If we have a cached surface, copy it to screen and use it. This avoids
        // redrawing the full board when only the cursor ghost moved.
        if (update_status & UPDATE_STATUS_HAVE_CACHE)
        {
            // wxLogDebug("old_cursor: %d %d", old_cursor.getX(), old_cursor.getY());
            // Only blit the rect where the old cursor was
            SDL_Rect rect;
            rect.x = offsetX + square_size * (old_cursor.getX()-1) - square_size/2 - 1;
            rect.y = offsetY + square_size * (old_cursor.getY()-1) - square_size/2 - 1;
            rect.w = square_size + 2;
            rect.h = square_size + 2;
            SDL_BlitSurface(screen2, &rect, screen, &rect);
        }
        // No cached surface yet, so we copy the current screen into the cache
        else
        {
            SDL_BlitSurface(screen, NULL, screen2, NULL);
            update_status |= UPDATE_STATUS_HAVE_CACHE;
        }
        if (show_cursor && cursor.getX() != -1 && cursor.getY() != -1 && !boardhandler->hasPosition(cursor))
            drawStone(cursor.getX(), cursor.getY(), boardhandler->getCurrentTurnColor(), true);
        update_status ^= UPDATE_STATUS_CURSOR;
        old_cursor = cursor;
    }
    // Crap happened
    else
    {
        wxLogDebug(_T("*** Invalid update status %d ***"), update_status);
        return;
    }

    // Copy the SDL surface into a wxBitmap.
    // I suppose this is the most performance-critical part. Maybe it can be improved, I don't know.
    // SDL blitting is very fast, so we operate as much as possible on the SDL_Surface and only
    // copy the complete screen into the window.
    // Most important is to avoid using wxWindows image rescaling which has poor quality.
    if (SDL_MUSTLOCK(screen))
        SDL_LockSurface(screen);
    wxBitmap bmp(wxImage(screen->w, screen->h, (unsigned char*)(screen->pixels), true));
    if (SDL_MUSTLOCK(screen))
        SDL_UnlockSurface(screen);
    mdc.SelectObject(bmp);

    // Blit bitmap into memory device context
    dc.Blit(0, 0, screen->w, screen->h, &mdc, 0, 0);

    block_cursor = is_modified = false;
}

void SDLBoard::drawStarPoint(int x, int y)
{
    int size = square_size / 10;
    if (size < 2)
        size = 2;

    filledCircleRGBA(kaya,
                     offset + square_size * (x - 1),
                     offset + square_size * (y - 1),
                     size,
                     0, 0, 0, 255);
    aacircleRGBA(kaya,
                 offset + square_size * (x - 1),
                 offset + square_size * (y - 1),
                 size,
                 0, 0, 0, 255);
}

void SDLBoard::drawBoard()
{
    if (kaya == NULL)
    {
        wxFAIL_MSG(_T("No board!"));
        return;
    }

    // Draw vertical lines
    for (int i=0; i<board_size; i++)
        lineRGBA(kaya,
                 offset + i*square_size, offset,
                 offset + i*square_size, offset + board_pixel_size,
                 0, 0, 0, 255);

    // Draw horizontal lines
    for (int i=0; i<board_size; i++)
        lineRGBA(kaya,
                 offset, offset + square_size * i,
                 offset + board_pixel_size, offset + square_size * i,
                 0, 0, 0, 255);

    // Starpoints
    if (board_size > 11)
    {
        drawStarPoint(4, 4);
        drawStarPoint(board_size - 3, 4);
        drawStarPoint(4, board_size - 3);
        drawStarPoint(board_size - 3, board_size - 3);
        if (board_size % 2 != 0)
        {
            drawStarPoint((board_size + 1) / 2, 4);
            drawStarPoint((board_size + 1) / 2, board_size - 3);
            drawStarPoint(4, (board_size + 1) / 2);
            drawStarPoint(board_size - 3, (board_size + 1) / 2);
            drawStarPoint((board_size + 1) / 2, (board_size + 1) / 2);
        }
    }
    else
    {
        drawStarPoint(3, 3);
        drawStarPoint(3, board_size - 2);
        drawStarPoint(board_size - 2, 3);
        drawStarPoint(board_size - 2, board_size - 2);
        if (board_size % 2 != 0)
            drawStarPoint((board_size + 1) / 2, (board_size + 1) / 2);
    }

    // Coordinates.
    // This uses either SDL_ttf true type fonts or the SDL_gfx 8x8 font
    if (show_coords)
    {
        // SDL_gfx
        if (!scaled_font)
        {
            for (int i=0; i<board_size; i++)
            {
                int j = board_size-i;
                char c[3];
                c[0] = j < 10 ? ' ' : '1';
                c[1] = j == 10 ? '0' : '1' + (j-1) % 10;
                c[2] = '\0';

                // Left
                stringRGBA(kaya,
                           offset/2 - (j<10?12:8),
                           offset + square_size * i - 3,
                           c, 0, 0, 0, 255);
                // Right
                stringRGBA(kaya,
                           board_pixel_size + offset*3/2 - (j<10?12:8),
                           offset + square_size * i - 3,
                           c, 0, 0, 0, 255);
                // Top
                characterRGBA(kaya,
                              offset + square_size * i - 3,
                              offset/2 - 4,
                              'A' + (i<8?i:i+1), 0, 0, 0, 255);
                // Bottom
                characterRGBA(kaya,
                              offset + square_size * i - 3,
                              board_pixel_size + offset*3/2 - 4,
                              'A' + (i<8?i:i+1), 0, 0, 0, 255);

            }
        }

        // SDL_ttf
        else if (text[0] != NULL)
        {
            SDL_Rect rect;

            for (int i=0; i<board_size; i++)
            {
                // Left
                rect.x = offset/2 - text_metrics[0][0]/2;
                rect.y = offset + square_size * i - text_metrics[i][1]/2;
                SDL_BlitSurface(text[i], NULL, kaya, &rect);

                // Right
                rect.x = board_pixel_size + offset*3/2 - text_metrics[0][0]/2;
                SDL_BlitSurface(text[i], NULL, kaya, &rect);

                // Top
                rect.x = offset + square_size * i - text_metrics[i+19][0]/2;
                rect.y = offset/2 - text_metrics[i+19][1]/2;
                SDL_BlitSurface(text[i+19], NULL, kaya, &rect);

                // Bottom
                rect.y = board_pixel_size + offset*3/2 - text_metrics[i+19][1]/2;
                SDL_BlitSurface(text[i+19], NULL, kaya, &rect);
            }
        }
    }
}

void SDLBoard::drawStone(int x, int y, Color color, bool transparent)
{
    if (screen == NULL)
    {
        wxFAIL_MSG(_T("No SDL screen!"));
        return;
    }

    SDL_Surface *stoneImg = NULL;
    Uint32 amask_old = 0;

    if (color == STONE_WHITE)
        stoneImg = stones_white[whitestonematrix[x-1][y-1]];
    else
        stoneImg = stone_black;

    wxASSERT(stoneImg != NULL);
    if (transparent)
    {
        // Set a transparent alpha mask if we want ghost stones and remember the old mask
        amask_old = stoneImg->format->Amask;
        stoneImg->format->Amask = amask_transparent;
    }

    SDL_Rect dest_rect;
    dest_rect.x = offsetX + square_size * (x-1) - square_size/2;
    dest_rect.y = offsetY + square_size * (y-1) - square_size/2;
    SDL_BlitSurface(stoneImg, NULL, screen, &dest_rect);

    if (transparent)
        stoneImg->format->Amask = amask_old;
}

void SDLBoard::drawMark(int x, int y, int color, int type, const wxString &txt)
{
    if (screen == NULL)
    {
        wxFAIL_MSG(_T("No SDL screen!"));
        return;
    }

    int c = 0;
    if (color == STONE_WHITE || type == MARK_TERR_WHITE)
        c = 255;
    int base_x = offsetX + square_size * (x-1);
    int base_y = offsetY + square_size * (y-1);

    // Shift marks over stones slightly up and left
    if (color != STONE_UNDEFINED && square_size%2 == 0)
    {
        base_x -= 1;
        base_y -= 1;
    }

    switch (type)
    {
    case MARK_CIRCLE:
        aacircleRGBA(screen, base_x, base_y, (short)(square_size * 0.30), c, c, c, ALPHA);
        break;
    case MARK_SQUARE:
    {
        Sint16 vx[4] = {
            base_x - square_size/4,
            base_x + square_size/4,
            base_x + square_size/4,
            base_x - square_size/4 };
        Sint16 vy[4] = {
            base_y - square_size/4,
            base_y - square_size/4,
            base_y + square_size/4,
            base_y + square_size/4 };
        aapolygonRGBA(screen, vx, vy, 4, c, c, c, ALPHA);
    }
    break;
    case MARK_TRIANGLE:
    {
        Sint16 vx[3] = {
            base_x - square_size/4,
            base_x + square_size/4,
            base_x };
        Sint16 vy[3] = {
            base_y - 1 + square_size/4,
            base_y - 1 + square_size/4,
            base_y - 1 - square_size/4 };
        aapolygonRGBA(screen, vx, vy, 3, c, c, c, ALPHA);
    }
    break;
    case MARK_CROSS:
        // Maybe draw territory using something else like small stone ghosts
    case MARK_TERR_WHITE:
    case MARK_TERR_BLACK:
        aalineRGBA(screen,
                   base_x - square_size/4, base_y - square_size/4,
                   base_x + square_size/4, base_y + square_size/4,
                   c, c, c, ALPHA);
        aalineRGBA(screen,
                   base_x - square_size/4, base_y + square_size/4,
                   base_x + square_size/4, base_y - square_size/4,
                   c, c, c, ALPHA);
        break;
    case MARK_TEXT:
    {
        char s[24] = "A";
        if (!txt.empty())
            strcpy(s, txt.Left(24).c_str());  // 24 chars should be enough

        if (!scaled_font)
        {
            // GFX 8x8 fonts. Fast but fixed size.
            stringRGBA(screen,
                       base_x - (strlen(s) == 1 ? 3 : 7),
                       base_y - 3,
                       s,
                       c, c, c, ALPHA);
        }
        else
        {
            // SDL_ttf truetype fonts
            wxASSERT(ttf_font != NULL);
            SDL_Color fg = {c, c, c, 255};
            SDL_Surface *fs = TTF_RenderText_Blended(ttf_font, s, fg);
            if (fs == NULL)
            {
                LOG_SDL(wxString::Format(_T("TTF_RenderText_Blended: %s\n"), TTF_GetError()));
                break;
            }
            int w, h;
            TTF_SizeText(ttf_font, s, &w, &h);
            SDL_Rect rect;
            rect.x = base_x - w/2;
            rect.y =  base_y - h/2;
            SDL_BlitSurface(fs, NULL, screen, &rect);
        }
    }
    break;
    default:
        break;
    }
}

void SDLBoard::OnCalculateLayout(wxCalculateLayoutEvent &event)
{
    // As the sidebar sash is processed first, we now take the
    // complete remaining space of the parent window.
    event.SetRect(wxRect(0, 0, 0, 0));
}

void SDLBoard::newGame(GameData *data)
{
    setupGame(data);
    boardhandler->newGame(data);
}

void SDLBoard::setupGame(GameData *data)
{
    wxASSERT(data != NULL);

    // Set new board size if changed.
    if (board_size != data->board_size)
    {
        // Recreate pseudo-random matrix for white stones
        if (whitestonematrix != NULL)
        {
            for (int i=0; i<board_size; i++)
                delete whitestonematrix[i];
            delete[] whitestonematrix;
        }
        board_size = data->board_size;
        whitestonematrix = new int*[board_size];
        for (int i=0; i<board_size; i++)
        {
            whitestonematrix[i] = new int[board_size];
            for (int j=0; j<board_size; j++)
                whitestonematrix[i][j] = rand() % NUMBER_WHITE_STONES;
        }

        calculateSize();
    }

    editMode = EDIT_MODE_NORMAL;
    is_modified = true;
}

void SDLBoard::setViewParameter(int para, bool value)
{
    switch (para)
    {
    case VIEW_SHOW_MARKS:
        show_marks = value;
        break;
    case VIEW_SHOW_COORDS:
        show_coords = value;
        calculateSize();
        break;
    case VIEW_SHOW_CURSOR:
        show_cursor = value;
        if (show_cursor)
            Connect(wxID_ANY, wxEVT_MOTION,
                    (wxObjectEventFunction) (wxEventFunction) (wxMouseEventFunction) &SDLBoard::OnMouseMotion);
        else
            Disconnect(wxID_ANY, wxEVT_MOTION,
                       (wxObjectEventFunction) (wxEventFunction) (wxMouseEventFunction) &SDLBoard::OnMouseMotion);
        cursor = Position(-1, -1);
        break;
    case VIEW_USE_SCALED_FONT:
        scaled_font = value;
        calculateSize();
        break;
    case VIEW_USE_BACKGROUND_IMAGE:
        background_image = value;
        calculateSize();
        break;
    default:
        wxFAIL_MSG(_T("Unknown parameter"));
        return;
    }

    is_modified = true;
}

void SDLBoard::navigate(NavigationDirection direction)
{
    // No navigation in edit mode
    if (editMode == EDIT_MODE_SCORE)
    {
        playSound(SOUND_BEEP);
        return;
    }

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

bool SDLBoard::convertCoordToPos(const wxCoord coordX, const wxCoord coordY, int &x, int &y)
{
    // Convert X coord
    x = coordX - offsetX + square_size / 2;
    if (x >= 0 && square_size > 0)
        x = x / square_size + 1;
    else
        return false;

    // Convert Y coord
    y = coordY - offsetY + square_size / 2;
    if (y >= 0 && square_size > 0)
        y = y / square_size + 1;
    else
        return false;

    // Off the board?
    if (x > static_cast<int>(board_size) || y > static_cast<int>(board_size))
        return false;

    // Ok, valid point
    return true;
}

void SDLBoard::OnMouseMotion(wxMouseEvent& e)
{
    // Check if a cursor display is allowed.
    if (update_status == UPDATE_STATUS_UNINITIALIZED ||
        !show_cursor || block_cursor || !mayMove())
        return;

    int x, y;
    if (!convertCoordToPos(e.GetX(), e.GetY(), x, y))
    {
        // Oops, invalid
        if (cursor.getX() != -1 && cursor.getY() != -1)
        {
            cursor = Position(-1, -1);
            is_modified = true;
        }
        return;
    }

    // Cursor still on the same spot
    if (x == cursor.getX() && y == cursor.getY())
        return;

    // A stone or ghost on this spot?
    if (boardhandler->hasPosition(Position(x, y)) ||
        boardhandler->hasGhost(Position(x, y)))
        return;

    cursor = Position(x, y);

    // Tell OnPaint to use the cached screen to avoid redrawing everything
    update_status = UPDATE_STATUS_CURSOR | (update_status & UPDATE_STATUS_HAVE_CACHE);
    is_modified = true;
}

void SDLBoard::OnMouseLeftDown(wxMouseEvent& e)
{
    handleMouseClick(e.GetX(), e.GetY(), 0);
}

void SDLBoard::OnMouseRightDown(wxMouseEvent& e)
{
    handleMouseClick(e.GetX(), e.GetY(), 1);
}

void SDLBoard::handleMouseClick(int x, int y, int button)
{
    // Transform mouse coordinates into board position (1-19) for 19x19
    int px, py;
    if (!convertCoordToPos(x, y, px, py))
    {
        // Oops, invalid
        return;
    }

    // Normal move: Left button plays, right button ignored
    if (editMode == EDIT_MODE_NORMAL)
    {
        if (button)  // Ignore right button
            return;
        EventPlayMove evt(px, py);
        GetEventHandler()->ProcessEvent(evt);
        if (evt.Ok())
            is_modified = true;
        return;
    }

    // Others are handled in Board::handleMouseClick()
    cursor = Position(-1, -1);
    Board::handleMouseClick(px, py, button);
}

void SDLBoard::OnChar(wxKeyEvent& e)
{
    int key = e.GetKeyCode();
    // wxLogDebug(wxString::Format("OnChar: %d", key));

    switch (key)
    {
    case WXK_RIGHT:
        navigate(NAVIGATE_DIRECTION_NEXT_MOVE);
        break;
    case WXK_LEFT:
        navigate(NAVIGATE_DIRECTION_PREVIOUS_MOVE);
        break;
    case WXK_UP:
        navigate(NAVIGATE_DIRECTION_NEXT_VARIATION);
        break;
    case WXK_DOWN:
        navigate(NAVIGATE_DIRECTION_PREVIOUS_VARIATION);
        break;
    case WXK_HOME:
        navigate(NAVIGATE_DIRECTION_FIRST_MOVE);
        break;
    case WXK_END:
        navigate(NAVIGATE_DIRECTION_LAST_MOVE);
        break;
    }

    e.Skip();
}

void SDLBoard::updateBackgroundColor(wxColour c)
{
    back_color = c;
    is_modified = true;
    Refresh(false);
}
