/*
 * sdl_core.h
 *
 * $Id: sdl_core.h,v 1.19 2003/10/31 22:16:08 peter Exp $
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
 * @defgroup sdl SDL 2D system
 *
 * Most SDL code now merged into the SDLBoard class after I figured out how to do multiple
 * SDL boards. libsdlcore now only contains the SDL_Rwops_ZZIP file.
 *
 * I keep the sdl_core.h/.c files for the sdltest program and for whatever future plans
 * might evolve. Maybe some day a standalone fullscreen boards, don't know yet.
 *
 * @see SDLBoard
 *
 * @{
 */

/** @file
 * THIS CODE IS NO LONGER USED IN THE MAIN %glGo APPLICATION !
 */

#ifndef SDL_CORE_H
#define SDL_CORE_H

#include <SDL/SDL.h>

#ifdef __cplusplus
extern "C" {
#endif

    /**
     * Init SDL videosystem.
     * @param boardsize Board size, 19 for 19x19 etc.
     * @param coords 1 to turn coordinates on, 0 off
     * @param w Window width
     * @param h Window height
     * @param sharedPath Directory with shared data files
     * @param background Pointer to an array with three entries giving the background RGB values
     * @return 0 on success, -1 if no videomode could be created
     */
    int SDLInit(unsigned short boardsize, int coords, int w, int h, const char* sharedPath, Uint8 *background);

    /** Resize SDL board */
    int SDLResize(int w, int h);

    /**
     * Change board size and/or coordinates display.
     * This will reload the images fitting for the new size.
     * @param s New board size
     * @param coords 1 to turn coordinates on, 0 off and -1 to leave unchanged
     */
    int SDLChangeBoardSize(int s, int coords);

    /**
     * Change the background color.
     * @param background Pointer to an array with three entries giving the background RGB values
     */
    void SDLSetBackgroundColor(Uint8 *background);

    /** Draw kaya board and grid */
    int SDLDrawBoard();

    /** Draw a stone */
    void SDLDrawStone(int x, int y, int color);

    /** Draw a mark */
    void SDLDrawMark(int x, int y, int color, int type, const char *txt);

    /** Swap double-buffered board */
    void SDLSwapBuffers();

    /**
     * Convert mouse to board coordinates.
     * @param coordX Mouse X coordinate
     * @param coordY Mouse Y coordinate
     * @param x This will contain the converted board X coordinate
     * @param y This will contain the converted board Y coordinate
     */
    int SDLConvertCoordToPos(const Uint16 coordX, const Uint16 coordY, int *x, int *y);

    /** Cleanup SDL system and free images. Should be called before SDLResize. */
    void SDLCleanup();

    /** Get SDL video info */
    void SDLInfo(char *buf);

    /** Toggles fixed/scaled font */
    void SDLToggleFixedFont();

#ifdef __cplusplus
}
#endif

/** @} */  // End of group

#endif
