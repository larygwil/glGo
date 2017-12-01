/*
 * al_sound.h
 *
 * $Id: al_sound.h,v 1.8 2003/11/04 15:11:12 peter Exp $
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
 * OpenAL sound interface. This file can also be compiled as
 * standalone commandline binary for a simple OpenAL availability
 * testing. Specify -DSTANDALONE when compiling for this.
 */

#ifndef AL_SOUND_H
#define AL_SOUND_H

#ifdef _WIN32
#define OALAPI __declspec(dllexport)
#define OALAPIENTRY __cdecl
#else
#define OALAPI
#define OALAPIENTRY
#endif


#ifdef __cplusplus
extern "C" {
#endif

    /** Init OpenAL sound system. */
    OALAPI int OALAPIENTRY OALInitSound(const char *sharedPath);

    /** Play stone click sound. */
    OALAPI int OALAPIENTRY OALPlaySound(int s);

    /** Shutdown and cleanup sound system. */
    OALAPI void OALAPIENTRY OALCleanupSound();

    /** Get OpenAL information. */
    OALAPI void OALAPIENTRY OALGetDeviceInfo(char *info);

#ifdef __cplusplus
}
#endif

/* @} */

#endif
