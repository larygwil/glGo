/*
 * sound.h
 *
 * $Id: sound.h,v 1.7 2003/10/04 21:38:28 peter Exp $
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
 * @defgroup sound Sound system
 *
 * OpenAL and SDL_mixer interface
 *
 * %glGo offers two platform independant ways to access a sound device: OpenAL and SDL_mixer.
 * While OpenAL is generally the superior solution, SDL_mixer seems to give a better sound
 * quality on Linux systems which use the OSS kernel drivers. The goal of this interface is
 * to remove all dependencies to the various sound libraries from the %glGo main binary. There
 * are two shared libraries: libalsound.so/dll and libsdlsound.so/dll which act as bridge
 * to the OpenAL and SDL libraries. %glGo will call dlopen on runtime to dynamically access
 * the OpenAL or SDL interface, which relieves the binary from various dependencies and should
 * make life easier for Linux users.
 *
 * Once the sound system is opened, function pointers allow dynamic access to the sound
 * libraries, while the interface to the %glGo classes is identical. All %glGo needs to know
 * are the functions in the sound.h file.
 *
 * The dlopen functions are wrapped into a PLIB utilities which offers an easy platform
 * independant way for loading dynamic libraries and saves me a lot of platform checking #ifdef's.
 *
 * Define NO_DYNAMIC_LOADING to disable dynamic sound loading. glGo needs to be linked against
 * OpenAL and SDL_Mixer. Using OpenAL can be controlled at compile time with the NO_OPENAL define.
 *
 * @{
 */

/**
 * @file
 *
 * Headers for the interface to the OpenAL or SDL_mixer sound systems. OpenAL or SDL_mixer
 * are dynamically dlopen'ed during process runtime.
 */

#ifndef SOUND_H
#define SOUND_H

#define SOUND_SYSTEM_OAL 0  ///< OpenAL sound system
#define SOUND_SYSTEM_SDL 1  ///< SDL_mixer sound system

#ifdef __cplusplus
extern "C" {
#endif

    /**
     * Load sound library. This function will try to dlopen libalsound.so for the OpenAL system
     * and libsdlsound.so for the SDL_mixer system.
     * @param sound_system 0 For OpenAL, 1 for SDL_mixer. Other values are invalid.
     * @return 0 on success, -1 on error
     */
    int Sound_load_library(int sound_system);

    /**
     * Initialize sound system. This will call the appropriate function in the OpenAL or SDL library.
     * @param sharedPath Directory containing the sound files
     * @return 0 on success, -1 on error
     */
    int Sound_init_library(const char *sharedPath);

    /**
     * Play a sound file.
     * @param sound This is mapped to the Sound enum in defines.h
     * @return 0 on success, -1 on error
     */
    int Sound_play(int sound);

    /**
     * Shutdown sound system. This will call the appropriate function in the OpenAL and SDL library,
     * releasing the sound device and freeing the allocated buffers.
     */
    void Sound_quit_library();

    /** Get some information about the used sound device. Info should have a size of 256 or so. */
    void Sound_get_info(char *info);

#ifdef __cplusplus
}
#endif

/** @} */

#endif
