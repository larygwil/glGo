/*
 * sdl_sound.h
 *
 * $Id: sdl_sound.c,v 1.14 2003/10/22 22:38:15 peter Exp $
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

#include "string.h"
#include <SDL/SDL.h>
#include <SDL/SDL_mixer.h>

#include "sdl_sound.h"

static const char *STONE_FILE = "stone.ogg";
static const char *PASS_FILE  = "pass.ogg";
static const char *CHAT_FILE = "chatIn.ogg";
static const char *BEEP_FILE = "beep.ogg";
static const char *MATCH_FILE = "matchrequest.ogg";
static const char *TIMEWARN_FILE = "tictoc.ogg";

Mix_Chunk *stoneSound, *passSound, *chatSound, *beepSound, *matchSound, *timewarnSound;

Mix_Chunk* loadChunk(const char *filename)
{
    Mix_Chunk *chunk;
    printf("Loading file: %s\n", filename);
    chunk = Mix_LoadWAV(filename);
    if (chunk == NULL)
        printf("Failed to load sound file: %s\n", filename);
    return chunk;
}

SDLSOUNDAPI int SDLSOUNDAPIENTRY SDLInitSound(const char* sharedPath)
{
    char path[64];
    int len = strlen(sharedPath);

    if(SDL_WasInit(SDL_INIT_AUDIO) == 0)
    {
        if (SDL_Init(SDL_INIT_AUDIO | SDL_INIT_NOPARACHUTE) < 0)
        {
            printf("Failed to init SDL audio system.\n");
            return -1;
        }
    }

    if (Mix_OpenAudio(MIX_DEFAULT_FREQUENCY, MIX_DEFAULT_FORMAT, MIX_DEFAULT_CHANNELS, 4096) < 0)
    {
        printf("Failed to open audio device.\n");
        return -1;
    }

    // Load sound files
    strcpy(path, sharedPath);
    strcpy(path + len, STONE_FILE);
    stoneSound = loadChunk(path);
    strcpy(path + len, PASS_FILE);
    passSound = loadChunk(path);
    strcpy(path + len, CHAT_FILE);
    chatSound = loadChunk(path);
    strcpy(path + len, BEEP_FILE);
    beepSound = loadChunk(path);
    strcpy(path + len, MATCH_FILE);
    matchSound = loadChunk(path);
    strcpy(path + len, TIMEWARN_FILE);
    timewarnSound = loadChunk(path);

    printf("SDLInitSound Ok\n");

    return 0;
}

int SDLSOUNDAPIENTRY SDLPlaySound(int i)
{
    Mix_Chunk *chunk;

    switch (i)
    {
    case 0:
        chunk = stoneSound;
        break;
    case 1:
        chunk = passSound;
        break;
    case 2:
        chunk = chatSound;
        break;
    case 3:
        chunk = beepSound;
        break;
    case 4:
        chunk = matchSound;
        break;
    case 5:
        chunk = timewarnSound;
        break;
    default:
        printf("Invalid sound: %d\n", i);
        return -1;
    }

    if (chunk == NULL || Mix_PlayChannel(-1, chunk, 0) < 0)
    {
        printf("Failed to play sound %d.\n", i);
        return -1;
    }
    return 0;
}

SDLSOUNDAPI void SDLSOUNDAPIENTRY SDLCleanupSound()
{
    Mix_FreeChunk(stoneSound);
    Mix_FreeChunk(passSound);
    Mix_FreeChunk(chatSound);
    Mix_FreeChunk(beepSound);
    Mix_FreeChunk(matchSound);
    Mix_FreeChunk(timewarnSound);
    stoneSound = passSound = chatSound = NULL;
    Mix_CloseAudio();

    SDL_QuitSubSystem(SDL_INIT_AUDIO);

    printf("SDL Audio shutdown\n");
}


SDLSOUNDAPI void SDLSOUNDAPIENTRY SDLGetDeviceInfo(char *info)
{
    int numtimesopened, frequency, channels;
    Uint16 format;
    numtimesopened = Mix_QuerySpec(&frequency, &format, &channels);
    if(numtimesopened == 0)
    {
        sprintf(info, "An error occured:\n%s", Mix_GetError());
    }
    else
    {
        char *format_str = "Unknown";
        switch(format)
        {
        case AUDIO_U8: format_str = "U8"; break;
        case AUDIO_S8: format_str = "S8"; break;
        case AUDIO_U16LSB: format_str = "U16LSB"; break;
        case AUDIO_S16LSB: format_str = "S16LSB"; break;
        case AUDIO_U16MSB: format_str = "U16MSB"; break;
        case AUDIO_S16MSB: format_str = "S16MSB"; break;
        }
        sprintf(info,
                "SDL Mixer information:\n\nFrequency: %d Hz\nAudio format: %s\nChannels: %d",
                frequency, format_str, channels);
    }
}

// ------------------------------------------------------------------------
//                           Debugging code
// ------------------------------------------------------------------------

#ifdef STANDALONE
int main(int argc, char *argv[])
{
    int i;
    char sharedPath[64] = "../share/";
    char info[256];

    if (SDLInitSound(sharedPath) < 0)
    {
        printf("Failed to init sound.\n");
        return 1;
    }

    SDLGetDeviceInfo(info);
    printf("\n%s\n", info);

    for (i=0; i<6; i++)
    {
        SDLPlaySound(i);
        while(Mix_Playing(0))
            SDL_Delay(10);
        SDL_Delay(3000);
    }

    SDL_Quit();

    return 0;
}
#endif
