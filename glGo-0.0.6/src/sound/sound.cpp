/*
 * sound.c
 *
 * $Id: sound.cpp,v 1.9 2003/10/23 00:33:56 peter Exp $
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
#include "sound.h"


// ------------------------------------------------------------------------
//                      Dynamic sound library loading
// ------------------------------------------------------------------------

#ifndef NO_DYNAMIC_LOADING

#include <plib/ul.h>

#define OAL_SOUND_LIB "libalsound"
#define SDL_SOUND_LIB "libsdlsound"

ulDynamicLibrary *handle;

// Function pointers to shared lib
int (*init_sound)(const char*);
int (*play_sound)(int);
void (*cleanup_sound)(void);
void (*get_sound_info)(char*);

int Sound_load_library(int sound_system)
{
#ifdef NO_OPENAL
    if (sound_system == SOUND_SYSTEM_OAL)
    {
        printf("OpenAL was disabled at compile time!\n");
        sound_system = SOUND_SYSTEM_SDL;
    }
#endif

    printf("Trying to load library for %s sound system.\n",
           (sound_system == SOUND_SYSTEM_OAL ? "OpenAL" :
            sound_system == SOUND_SYSTEM_SDL ? "SDL Mixer" : "UNKNOWN"));

    if (handle != NULL)
    {
        printf("Libray already opened.\n");
        return -1;
    }

    if (sound_system == SOUND_SYSTEM_OAL)
    {
        handle = new ulDynamicLibrary(OAL_SOUND_LIB);
        if (handle == NULL)
        {
            printf("Failed to dlopen library.\n");
            return -1;
        }

        init_sound = (int(*)(const char*))(handle->getFuncAddress("OALInitSound"));
        if (init_sound == NULL)
        {
            printf("Failed to get dlsym to OALInitSound.\n");
            return -1;
        }

        play_sound = (int(*)(int))(handle->getFuncAddress("OALPlaySound"));
        if (play_sound == NULL)
        {
            printf("Failed to get dlsym to OALPlaySound.\n");
            return -1;
        }

        cleanup_sound = (void(*)())(handle->getFuncAddress("OALCleanupSound"));
        if (cleanup_sound == NULL)
        {
            printf("Failed to get dlsym to OALCleanupSound.\n");
            return -1;
        }

        get_sound_info = (void(*)(char*))(handle->getFuncAddress("OALGetDeviceInfo"));
        if (get_sound_info == NULL)
        {
            printf("Failed to get dlsym to OALGetDeviceInfo\n");
            return -1;
        }
    }
    else if (sound_system == SOUND_SYSTEM_SDL)
    {
        handle = new ulDynamicLibrary(SDL_SOUND_LIB);
        if (handle == NULL)
        {
            printf("Failed to dlopen library.\n");
            return -1;
        }

        init_sound = (int(*)(const char*))(handle->getFuncAddress("SDLInitSound"));
        if (init_sound == NULL)
        {
            printf("Failed to get dlsym to SDLInitSound.\n");
            return -1;
        }

        play_sound = (int(*)(int))(handle->getFuncAddress("SDLPlaySound"));
        if (play_sound == NULL)
        {
            printf("Failed to get dlsym to SDLPlaySound.\n");
            return -1;
        }

        cleanup_sound = (void(*)())(handle->getFuncAddress("SDLCleanupSound"));
        if (cleanup_sound == NULL)
        {
            printf("Failed to get dlsym to SDLCleanupSound.\n");
            return -1;
        }

        get_sound_info = (void(*)(char*))(handle->getFuncAddress("SDLGetDeviceInfo"));
        if (get_sound_info == NULL)
        {
            printf("Failed to get dlsym to SDLGetDeviceInfo\n");
            return -1;
        }
    }

    return 0;
}

int Sound_init_library(const char *sharedPath)
{
    if (handle == NULL || init_sound == NULL || init_sound(sharedPath) < 0)
    {
        printf("Failed to init sound.\n");
        return -1;
    }
    return 0;
}

int Sound_play(int sound)
{
    if (handle == NULL || play_sound == NULL)
        return -1;
    return play_sound(sound);
}

void Sound_quit_library()
{
    if (handle == NULL)
        return;
    cleanup_sound();
    handle = NULL;
}

void Sound_get_info(char *info)
{
    if (handle == NULL)
        return;
    get_sound_info(info);
}

#else  // USE_DYNAMIC_LOADING


// ------------------------------------------------------------------------
//                        Static sound library loading
// ------------------------------------------------------------------------

#include "al_sound.h"
#include "sdl_sound.h"

static int _sound_system = SOUND_SYSTEM_OAL;

int Sound_load_library(int sound_system)
{
#ifdef NO_OPENAL
    if (sound_system == SOUND_SYSTEM_OAL)
    {
        printf("OpenAL was disabled at compile time!\n");
        sound_system = SOUND_SYSTEM_SDL;
    }
#endif
    _sound_system = sound_system;
    printf("Using %s sound system.\n",
           (sound_system == SOUND_SYSTEM_OAL ? "OpenAL" :
            sound_system == SOUND_SYSTEM_SDL ? "SDL Mixer" : "UNKNOWN"));
    return 0;
}

int Sound_init_library(const char *sharedPath)
{
    if (_sound_system == SOUND_SYSTEM_SDL)
        return SDLInitSound(sharedPath);
#ifndef NO_OPENAL
    else if (_sound_system == SOUND_SYSTEM_OAL)
        return OALInitSound(sharedPath);
#endif
    printf("Invalid sound system.\n");
    return -1;
}

int Sound_play(int sound)
{
    if (_sound_system == SOUND_SYSTEM_SDL)
        return SDLPlaySound(sound);
#ifndef NO_OPENAL
    else if (_sound_system == SOUND_SYSTEM_OAL)
        return OALPlaySound(sound);
#endif
    printf("Invalid sound system.\n");
    return -1;
}

void Sound_quit_library()
{
    if (_sound_system == SOUND_SYSTEM_SDL)
        SDLCleanupSound();
#ifndef NO_OPENAL
    else if (_sound_system == SOUND_SYSTEM_OAL)
        OALCleanupSound();
#endif
    else
        printf("Invalid sound system.\n");
}

void Sound_get_info(char *info)
{
    if (_sound_system == SOUND_SYSTEM_SDL)
        SDLGetDeviceInfo(info);
#ifndef NO_OPENAL
    else if (_sound_system == SOUND_SYSTEM_OAL)
        OALGetDeviceInfo(info);
#endif
    else
        printf("Invalid sound system.\n");
}

#endif // !USE_DYNAMIC_LOADING
