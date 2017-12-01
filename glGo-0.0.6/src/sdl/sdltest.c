/*
 * sdltest.c
 *
 * $Id: sdltest.c,v 1.16 2003/11/04 17:06:31 peter Exp $
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

/**
 * @file
 * This file contains the code for a standalone SDL board for testing and
 * development purpose.
 * @ingroup sdl
 */

#include "sdl_core.h"

#define SIZE_X 600
#define SIZE_Y 600

#ifdef _WIN32
#define SHARE_DIR "share/data/"
#else
#define SHARE_DIR "../share/data/"
#endif

#ifndef DOXYGEN_SHOULD_SKIP_THIS
struct LastStone
{
    int x;
    int y;
    int color;
} lastStone;
#endif

int main(int argc, char *argv[])
{
    SDL_Event event;
    unsigned short boardsize = 19;
    int coords = 1;
    char sdl_info[256];
    Uint8 background[3];
    background[0] = 128;
    background[1] = 0;
    background[2] = 0;

    if (SDLInit(boardsize, coords, SIZE_X, SIZE_Y, SHARE_DIR, background) < 0)
        return 1;

    SDLInfo(sdl_info);
    printf("SDL video info:\n%s", sdl_info);

    SDL_WM_SetCaption("glGo SDL test", NULL);

    if (SDLDrawBoard() < 0)
        return 1;

    SDLSwapBuffers();

    while (1)
    {
        if (SDL_WaitEvent(&event) == 0)
        {
            printf("Error waiting for event.\n");
            break;
        }

        switch(event.type)
        {
        case SDL_MOUSEBUTTONDOWN:
        {
            int x, y;

            if(lastStone.x != 0 && lastStone.y != 0)
                SDLDrawStone(lastStone.x, lastStone.y, lastStone.color);

            // Transform mouse coordinates into board position (1-19) for 19x19
            if (SDLConvertCoordToPos(event.button.x, event.button.y, &x, &y) < 0)
            {
                // Oops, invalid
                printf("Invalid mouse position\n");
                break;
            }
            SDLDrawStone(x, y, event.button.button == 1 ? 2 : 1);
            SDLDrawMark(x, y, event.button.button == 1 ? 1 : 2, 1, NULL);
            SDLDrawMark(x+1, y+1, event.button.button == 1 ? 1 : 2, 5, NULL);
            SDLSwapBuffers();
            lastStone.x = x;
            lastStone.y = y;
            lastStone.color = event.button.button == 1 ? 2 : 1;
        }
        break;
        case SDL_VIDEORESIZE:
            SDLCleanup();
            if (SDLResize(event.resize.w, event.resize.h) < 0)
            {
                printf("Failed to process resize event.\n");
                SDL_Quit();
                return 0;
            }
            SDLDrawBoard();
            SDLSwapBuffers();
            break;
        case SDL_KEYDOWN:
            // printf("Key: %d\n", event.key.keysym.sym);
            switch (event.key.keysym.sym)
            {
            case SDLK_ESCAPE:
                SDLCleanup();
                SDL_Quit();
                return 0;
            case SDLK_SPACE:
                /* Toggle through 9x9, 13x13 and 19x19 */
                boardsize = boardsize == 19 ? 9 : boardsize == 9 ? 13 : 19;
                if (SDLChangeBoardSize(boardsize, -1) < 0)
                    break;
                lastStone.x = 0;
                lastStone.y = 0;
                SDLDrawBoard();
                SDLSwapBuffers();
                break;
            case 99:  // C
                // Toggle coordinates
                coords = coords != 0 ? 0 : 1;
                if (SDLChangeBoardSize(boardsize, coords) < 0)
                    break;
                lastStone.x = 0;
                lastStone.y = 0;
                SDLDrawBoard();
                SDLSwapBuffers();
                break;
            case 102:  // F
                SDLToggleFixedFont();
                lastStone.x = 0;
                lastStone.y = 0;
                SDLDrawBoard();
                SDLSwapBuffers();
                break;
            default:
                break;
            }
            break;
        case SDL_QUIT:
            SDLCleanup();
            SDL_Quit();
            return 0;
        }
    }

    SDLCleanup();
    SDL_Quit();
    return 0;
}
