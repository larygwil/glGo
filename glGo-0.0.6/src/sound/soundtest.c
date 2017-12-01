/*
 * soundtest.c
 *
 * $Id: soundtest.c,v 1.9 2003/10/22 22:38:15 peter Exp $
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
 * Simple console test application to run a dynamically loaded OpenAL and SDL Mixer
 * sound test.
 * @ingroup sound
 */

#include <stdio.h>
#ifdef _WIN32
#include <windows.h>
#else
#include <stdlib.h>
#include <unistd.h>
#endif
#include "sound.h"

/**
 * Main application method. This will parse the commandline parameters for "0" and "1" and
 * try to load and initialize the OpenAL (0) or SDL_mixer (1) sound system and then play
 * three test sounds and shutdown again.
 */
int main(int argc, char *argv[])
{
    int i, sound_system;
#ifdef _WIN32
    char sharedPath[64] = "share/";
#else
    char sharedPath[64] = "../share/";
#endif
    char info[256];

    if (argc != 2)
    {
        printf("Usage is:\nsoundtest 0 - Test OpenAL\nsoundtest 1 - Test SDL Mixer\n");
        sound_system = 0;
    }
    else
        sound_system = atoi(argv[1]);

    printf("\nTesting %s sound system\n\n", (sound_system ? "SDL Mixer" : "OpenAL"));

    if (Sound_load_library(sound_system) < 0)
        return 1;
    printf("Library loaded\n");

    if (Sound_init_library(sharedPath) < 0)
        return 1;
    printf("Sound initialized\n");

    Sound_get_info(info);
    printf("\n%s\n", info);

    for (i=0; i<6; i++)
    {
        printf("Playing sound %d\n", i);
        if (Sound_play(i) < 0)
            printf("Error playing sound %d\n", i);
#ifndef _WIN32
        sleep(3);
#else
        Sleep(3000);
#endif
    }

    Sound_quit_library();

    return 0;
}
