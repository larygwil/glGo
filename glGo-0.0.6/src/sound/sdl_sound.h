/*
 * sdl_sound.h
 *
 * $Id: sdl_sound.h,v 1.5 2003/10/13 18:04:08 peter Exp $
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

/** @addtogroup sound
 * @{ */

/**
 * @file
 *
 * SDL_mixer sound interface. This file can also be compiled as
 * standalone commandline binary for a simple SDL_mixer availability
 * testing. Specify -DSTANDALONE when compiling for this.
 *
 */

#ifndef SDL_SOUND_H
#define SDL_SOUND_H

#ifdef _WIN32
#define SDLSOUNDAPI __declspec(dllexport)
#define SDLSOUNDAPIENTRY __cdecl
#else
#define SDLSOUNDAPI
#define SDLSOUNDAPIENTRY
#endif


#ifdef __cplusplus
extern "C" {
#endif

    /** Initialize the SDL Audio system. */
    SDLSOUNDAPI int SDLSOUNDAPIENTRY SDLInitSound(const char* sharedPath);

    /** Play a sound. */
    SDLSOUNDAPI int SDLSOUNDAPIENTRY SDLPlaySound(int i);

    /** Shutdown SDL Audio system. */
    SDLSOUNDAPI void SDLSOUNDAPIENTRY SDLCleanupSound();

    /** Get SDL Mixer information. */
    SDLSOUNDAPI void SDLSOUNDAPIENTRY SDLGetDeviceInfo(char *info);

#ifdef __cplusplus
}
#endif

/* @} */

#endif
