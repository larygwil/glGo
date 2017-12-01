/*
 * sdl_core.c
 *
 * $Id: sdl_core.c,v 1.28 2003/11/04 15:03:48 peter Exp $
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <SDL/SDL_image.h>
#include <SDL/SDL_ttf.h>
#include <SDL/SDL_gfxPrimitives.h>
#include <SDL/SDL_rotozoom.h>
#include "SDL_rwops_zzip.h"

#include "sdl_core.h"

#define STONE_UNDEFINED 0
#define STONE_WHITE     1
#define STONE_BLACK     2

#define MARK_NONE       0
#define MARK_CIRCLE     1
#define MARK_SQUARE     2
#define MARK_TRIANGLE   3
#define MARK_CROSS      4
#define MARK_TEXT       5

#define NUMBER_WHITE_STONES 8
#define MAX_BOARD_SIZE 19

#define ALPHA 220

static SDL_Surface *screen=NULL, *kaya=NULL, *stone_black=NULL, **stone_white=NULL;
static int offset, offsetX, offsetY, square_size, board_pixel_size;
static unsigned short board_size;
static int show_coords;
static char shared_path[64];
static int whitestonematrix[MAX_BOARD_SIZE][MAX_BOARD_SIZE];
static Uint8 back_color[3];
static TTF_Font *font=NULL;
static SDL_Surface *text[38];
static int text_metrics[38][2];
static int fixed_font = 0;

/** Recalculate board size layout */
void calculateSize()
{
    int margin = 5;  /* Fixed margin */
    int w = screen->w - margin * 2;
    int h = screen->h - margin * 2;

    offset = w < h ? w * (show_coords?4:2) / 100 : h * (show_coords?4:2) / 100;

    square_size = w < h ? (w - 2*offset) / board_size : (h - 2*offset) / board_size;
    /* Should not happen, but safe is safe. */
    if (square_size == 0)
        square_size = 1;

    offset += square_size / 2;
    board_pixel_size = square_size * (board_size - 1);

    /* Center the board in canvas */
    offsetX = margin + (w - board_pixel_size) / 2;
    offsetY = margin + (h - board_pixel_size) / 2;
}

/** Load font and cache letters */
int loadFont()
{
    SDL_Color fg = {0,0,0,255};
    SDL_RWops *rwops = NULL;
    char c[3], path[64];
    int i, len = strlen(shared_path)-5;

    if (font != NULL)
    {
        TTF_CloseFont(font);
        for(i=0; i<38; i++)
        {
            if(text[i] != NULL)
                SDL_FreeSurface(text[i]);
            text[i] = NULL;
        }
    }

    strcpy(path, shared_path);
    strcpy(path + len, "FreeSans.ttf");
    rwops = SDL_RWFromFile(path, "rb");
    if (rwops == NULL)
    {
        printf("Could not open file 'FreeSans.ttf'\n");
        return -1;
    }
    font = TTF_OpenFontRW(rwops, 1, square_size/2);
    if(font == NULL)
    {
        printf("TTF_OpenFont: %s\n", TTF_GetError());
        return -1;
    }

    for (i=0; i<38; i++)
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
        text[i] = TTF_RenderText_Blended(font, c, fg);
        if (!text[i])
        {
            printf("TTF_RenderText_Blended: %s\n", TTF_GetError());
            return -1;
        }
        TTF_SizeText(font, c, &text_metrics[i][0], &text_metrics[i][1]);
    }
    return 0;
}

/** Load and rescale the images */
int loadImages()
{
    double bsize;
    SDL_RWops *rwops;
    SDL_Surface *tmp;
    int len = strlen(shared_path);
    int i;
    char path[64];

    /* Kaya */
    strcpy(path, shared_path);
    strcpy(path + len, "kaya.jpg");
    rwops = SDL_RWFromZZIP(path, "rb");
    if (rwops == NULL)
    {
        printf("Failed to load image 'kaya.jpg': %s\n", SDL_GetError());
        return -1;
    }
    kaya = IMG_Load_RW(rwops, 1);
    if(kaya == NULL)
    {
        printf("Failed to load image 'kaya.jpg': %s\n", SDL_GetError());
        return -1;
    }
    bsize = board_pixel_size + offset * 2;
    kaya = zoomSurface(kaya,
                       bsize / (double)(kaya->w),
                       bsize / (double)(kaya->h),
                       SMOOTHING_ON);

    /*
     * Note:
     * Yes, we reload the stones from the zipped datafile each time the board is
     * resized. However, in real life the user will not constantly resize the board,
     * so we don't really need the unscaled images trashing the memory. Unzipping
     * is fast and the files should be cached by a real operating system.
     */

    /* White stones */
    stone_white = (SDL_Surface**)malloc(NUMBER_WHITE_STONES * sizeof(SDL_Surface*));
    if (stone_white == NULL)
    {
        printf("Out of memory.\n");
        abort();  /* Oops */
    }
    memset(stone_white, 0, sizeof(NUMBER_WHITE_STONES * sizeof(SDL_Surface*)));
    for (i=0; i<NUMBER_WHITE_STONES; i++)
    {
        char file[12];

        sprintf(file, "hyuga%d.png", i+1);
        strcpy(path + len, file);
        rwops = SDL_RWFromZZIP(path, "rb");
        if (rwops == NULL)
        {
            printf("Failed to load image: 'hyuga%d.png'\n", i+1);
            return -1;
        }
        tmp = IMG_Load_RW(rwops, 1);
        if(tmp == NULL)
        {
            printf("Failed to load image: 'hyuga%d.png'\n", i+1);
            return -1;
        }
        stone_white[i] = (SDL_Surface*)malloc(sizeof(SDL_Surface*));
        if (stone_white[i] == NULL)
        {
            printf("Out of memory.\n");
            abort();  /* Oops */
        }
        stone_white[i] = zoomSurface(tmp,
                                     square_size/(double)tmp->w,
                                     square_size/(double)tmp->h,
                                     SMOOTHING_ON);
        SDL_FreeSurface(tmp);
    }

    /* Black stone */
    strcpy(path + len, "blk.png");
    rwops = SDL_RWFromZZIP(path, "rb");
    if (rwops == NULL)
    {
        printf("Failed to load image 'blk.png'\n");
        return -1;
    }
    tmp = IMG_Load_RW(rwops, 1);
    if(tmp == NULL)
    {
        printf("Failed to load image 'blk.png'\n");
        return -1;
    }
    stone_black = zoomSurface(tmp,
                              square_size/(double)tmp->w,
                              square_size/(double)tmp->h,
                              SMOOTHING_ON);
    SDL_FreeSurface(tmp);

    return loadFont();
}

int SDLInit(unsigned short boardsize, int coords, int w, int h, const char* sharedPath, Uint8 *background)
{
    int x, y;

    if (sharedPath == NULL || !strlen(sharedPath))
    {
        printf("No shared path given.\n");
        return -1;
    }
    strcpy(shared_path, sharedPath);

    /* Init SDL. Video mode should already have been initialized in glGo::OnInit.
     * But better safe than sorry... */
    if(SDL_WasInit(SDL_INIT_VIDEO) == 0)
    {
        if(SDL_Init(SDL_INIT_VIDEO | SDL_INIT_NOPARACHUTE) < 0)
        {
            printf("Unable to init SDL: %s\n", SDL_GetError());
            return -1;
        }
    }

    /* Init ttf */
    if(TTF_Init() == -1)
    {
        printf("TTF_Init: %s\n", TTF_GetError());
        return -1;
    }

    /* Init random whitestone matrix */
    srand(time(0));
    for (x = 0; x < MAX_BOARD_SIZE; x++)
        for (y = 0; y < MAX_BOARD_SIZE; y++)
            whitestonematrix[x][y] = rand() % NUMBER_WHITE_STONES;

    board_size = boardsize;
    show_coords = coords;
    SDLSetBackgroundColor(background);
    return SDLResize(w, h);
}

void SDLSetBackgroundColor(Uint8 *background)
{
    back_color[0] = background[0];
    back_color[1] = background[1];
    back_color[2] = background[2];
}

int SDLResize(int w, int h)
{
    /* Try double-buffered hardware video mode */
    screen = SDL_SetVideoMode(w, h, 0, SDL_HWSURFACE | SDL_ANYFORMAT | SDL_DOUBLEBUF | SDL_RESIZABLE);
    if (screen == NULL)
    {
        printf("Unable to set double-buffered hardware video mode: %s\n", SDL_GetError());

        /* Try software video mode... */
        screen = SDL_SetVideoMode(w, h, 0, SDL_SWSURFACE | SDL_ANYFORMAT);
        if (screen == NULL)
        {
            printf("Unable to set single-buffered software video mode: %s\nBailing out...", SDL_GetError());
            return -1;
        }
    }

    calculateSize();

    return loadImages();
}

int SDLChangeBoardSize(int s, int coords)
{
    board_size = s;
    if (coords != -1)
        show_coords = coords;
    calculateSize();

    /* Free and reload images */
    if (kaya != NULL)
        SDL_FreeSurface(kaya);
    if (stone_white != NULL)
    {
        int i;
        for (i=0; i<NUMBER_WHITE_STONES; i++)
        {
            SDL_FreeSurface(stone_white[i]);
            stone_white[i] = NULL;
        }
        free(stone_white);
        stone_white = NULL;
    }
    if (stone_black != NULL)
        SDL_FreeSurface(stone_black);
    kaya = stone_black = NULL;

    return loadImages();
}

/** Draw a starpoint */
void drawStarPoint(int x, int y)
{
    int size = square_size / 10;
    if (size < 2)
        size = 2;

    filledCircleRGBA(screen,
                     offsetX + square_size * (x - 1),
                     offsetY + square_size * (y - 1),
                     size,
                     0, 0, 0, 255);
    aacircleRGBA(screen,
                 offsetX + square_size * (x - 1),
                 offsetY + square_size * (y - 1),
                 size,
                 0, 0, 0, 255);
}

int SDLDrawBoard()
{
    int i;
    SDL_Rect dest_rect;

    if (screen == NULL)
    {
        printf("No SDL screen!\n");
        return -1;
    }

    /* Set background color to red */
    if (SDL_FillRect(screen, NULL, SDL_MapRGB(screen->format, back_color[0], back_color[1], back_color[2])) == -1)
    {
        printf("Error setting background color: %s\n", SDL_GetError());
        return -1;
    }

    /* Draw kaya background */
    dest_rect.x = offsetX - offset;
    dest_rect.y = offsetY - offset;
    SDL_BlitSurface(kaya, NULL, screen, &dest_rect);

    /* Draw vertical lines */
    for (i=0; i<board_size; i++)
        lineRGBA(screen,
                 offsetX + i*square_size, offsetY,
                 offsetX + i*square_size, offsetY + board_pixel_size,
                 0, 0, 0, 255);
    /* Draw horizontal lines */
    for (i=0; i<board_size; i++)
        lineRGBA(screen,
                 offsetX, offsetY + square_size * i,
                 offsetX + board_pixel_size, offsetY + square_size * i,
                 0, 0, 0, 255);

    /* Starpoints */
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

    /* Coordinates */
    if (show_coords)
    {
        for (i=0; i<board_size; i++)
        {
            if (fixed_font)
            {
                int j;
                char c[3];

                j = board_size-i;
                c[0] = j < 10 ? ' ' : '1';
                c[1] = j == 10 ? '0' : '1' + (j-1) % 10;
                c[2] = '\0';

                /* Left */
                stringRGBA(screen,
                           offsetX - offset/2 - (j<10?12:8),
                           offsetY + square_size * i - 3,
                           c, 0, 0, 0, 255);
                /* Right */
                stringRGBA(screen,
                           offsetX + board_pixel_size + offset/2 - (j<10?12:8),
                           offsetY + square_size * i - 3,
                           c, 0, 0, 0, 255);
                /* Top */
                characterRGBA(screen,
                              offsetX + square_size * i - 3,
                              offsetY - offset/2 - 4,
                              'A' + (i<8?i:i+1), 0, 0, 0, 255);
                /* Bottom */
                characterRGBA(screen,
                              offsetX + square_size * i - 3,
                              offsetY + board_pixel_size + offset/2 - 4,
                              'A' + (i<8?i:i+1), 0, 0, 0, 255);
            }
            else
            {
                SDL_Rect rect;

                /* Left */
                rect.x = offsetX - offset/2 - text_metrics[0][0]/2;
                rect.y = offsetY + square_size * i - text_metrics[i][1]/2;
                SDL_BlitSurface(text[i], NULL, screen, &rect);

                /* Right */
                rect.x = offsetX + board_pixel_size + offset/2 - text_metrics[0][0]/2;
                SDL_BlitSurface(text[i], NULL, screen, &rect);

                /* Top */
                rect.x = offsetX + square_size * i - text_metrics[i+19][0]/2;
                rect.y = offsetY - offset/2 - text_metrics[i+19][1]/2;
                SDL_BlitSurface(text[i+19], NULL, screen, &rect);

                /* Bottom */
                rect.y = offsetY + board_pixel_size + offset/2 - text_metrics[i+19][1]/2;
                SDL_BlitSurface(text[i+19], NULL, screen, &rect);
            }
        }
    }

    return 0;
}

void SDLSwapBuffers()
{
    if (screen == NULL)
    {
        printf("No SDL screen!\n");
        return;
    }

    SDL_Flip(screen);
}

int SDLConvertCoordToPos(const Uint16 coordX, const Uint16 coordY, int *x, int *y)
{
    /* Convert X coord */
    *x = coordX - offsetX + square_size / 2;
    if (*x >= 0 && square_size > 0)
        *x = *x / square_size + 1;
    else
        return -1;

    /* Convert Y coord */
    *y = coordY - offsetY + square_size / 2;
    if (*y >= 0 && square_size > 0)
        *y = *y / square_size + 1;
    else
        return -1;

    /* Off the board? */
    if (*x > (int)board_size || *y > (int)board_size)
        return -1;

    /* Ok, valid point */
    return 0;
}

void SDLDrawStone(int x, int y, int color)
{
    SDL_Surface *stoneImg = NULL;
    SDL_Rect dest_rect;

    if (screen == NULL)
    {
        printf("No SDL screen!\n");
        return;
    }

    if (color == STONE_WHITE)
        stoneImg = stone_white[whitestonematrix[x-1][y-1]];
    else
        stoneImg = stone_black;

    dest_rect.x = offsetX + square_size * (x-1) - square_size/2;
    dest_rect.y = offsetY + square_size * (y-1) - square_size/2;
    SDL_BlitSurface(stoneImg, NULL, screen, &dest_rect);
}

void SDLDrawMark(int x, int y, int color, int type, const char *txt)
{
    int c = 0;
    int base_x, base_y;

    if (screen == NULL)
    {
        printf("No SDL screen!\n");
        return;
    }

    if (color == STONE_WHITE)
        c = 255;
    base_x = offsetX + square_size * (x-1);
    base_y = offsetY + square_size * (y-1);

    if (color != STONE_UNDEFINED && square_size%2 == 0)
    {
        base_x -= 1;
        base_y -= 1;
    }

    switch (type)
    {
    case MARK_CIRCLE:
        aacircleRGBA(screen, base_x, base_y, (short)(square_size * 0.3), c, c, c, ALPHA);
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
        if (txt != NULL && strlen(txt))
            strcpy(s, txt);
        if (fixed_font)
        {
            stringRGBA(screen,
                       base_x - 4,
                       base_y - 4,
                       s,
                       c, c, c, ALPHA);
        }
        else
        {
            SDL_Surface *fs;
            SDL_Rect rect;
            SDL_Color fg = {c, c, c, 255};
            int w, h;

            fs = TTF_RenderText_Blended(font, s, fg);
            if (fs == NULL)
            {
                printf("TTF_RenderText_Blended: %s\n", TTF_GetError());
                break;
            }
            TTF_SizeText(font, s, &w, &h);
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

void SDLCleanup()
{
    if (kaya != NULL)
        SDL_FreeSurface(kaya);
    if (stone_white != NULL)
    {
        int i;
        for (i=0; i<NUMBER_WHITE_STONES; i++)
        {
            SDL_FreeSurface(stone_white[i]);
            stone_white[i] = NULL;
        }
        free(stone_white);
        stone_white = NULL;
    }
    if (stone_black != NULL)
        SDL_FreeSurface(stone_black);
    if (screen != NULL)
        SDL_FreeSurface(screen);
    kaya = stone_black = screen = NULL;
}

void SDLInfo(char *buf)
{
    char drivername[32];
    if (SDL_VideoDriverName(drivername, 32) != NULL)
        sprintf(buf, "Driver: %s\nDepth: %u\n",
                drivername,
                SDL_GetVideoInfo()->vfmt->BitsPerPixel);
}

void SDLToggleFixedFont()
{
    fixed_font = !fixed_font;
}
