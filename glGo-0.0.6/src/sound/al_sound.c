/*
 * al_sound.c
 *
 * $Id: al_sound.c,v 1.16 2003/11/04 15:11:00 peter Exp $
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
#include <string.h>

/*
 * Let OpenAL decode Ogg? Only available on Linux and Mac.
 * This still has some unresolved problems, disabled now.
 */
#if 0 && !defined(_WIN32)
#define OAL_DECODES_VORBIS
#error Expect problems!
#endif

#if defined(STANDALONE) && !defined(_WIN32)
#include <unistd.h>
#endif

#ifdef _WIN32
#include <windows.h>
#include <mmsystem.h>
#endif

#include <AL/al.h>
#include <AL/alc.h>
#ifdef OAL_DECODES_VORBIS
#include <AL/alexttypes.h>
#endif
#include "al_sound.h"

#ifndef OAL_DECODES_VORBIS
#include <vorbis/vorbisfile.h>
#endif
static const char *STONE_FILE = "stone.ogg";
static const char *PASS_FILE  = "pass.ogg";
static const char *CHAT_FILE = "chatIn.ogg";
static const char *BEEP_FILE = "beep.ogg";
static const char *MATCH_FILE = "matchrequest.ogg";
static const char *TIMEWARN_FILE = "tictoc.ogg";

/* Global variables */
#define NUM_BUFFERS 6
#define NUM_SOURCES 1
ALuint g_Buffers[NUM_BUFFERS];
ALuint g_Source[NUM_SOURCES];

/* Function prototypes */
int initAL(const char *sharedPath);
int initAL();
int loadSoundFile(const char *filename, int buffer);
int createSource();
ALvoid DisplayALError(const char *szText, ALint errorCode);
#ifndef _WIN32
static void *alut_context_id = NULL;
#endif

OALAPI int OALAPIENTRY OALInitSound(const char *sharedPath)
{
    if (initAL(sharedPath) < 0)
        return -1;
    return createSource();
}

int initAL(const char *sharedPath)
{
    ALint error;
    char path[64];
    int len = strlen(sharedPath);

    /* Initialize OpenAL */
#ifdef _WIN32
    ALCcontext *Context;
    ALCdevice *Device;

    /* Open device */
    Device = alcOpenDevice("DirectSound3D");
    if(Device == NULL)
    {
        printf("Invalid device.\n");
        return -1;
    }
    /* Create contex */
    Context = alcCreateContext(Device, NULL);
    if(Context == NULL)
    {
        printf("Invalid device.\n");
        return -1;
    }
    /* Set active context */
    alcMakeContextCurrent(Context);
#else
    ALCdevice *dev;

    dev = alcOpenDevice(NULL);
    if(dev == NULL)
    {
        printf("Invalid device.\n");
        return -1;
    }
    alut_context_id = alcCreateContext(dev, NULL);
    if(alut_context_id == NULL)
    {
        printf("Invalid device.\n");
        return -1;
    }
    alcMakeContextCurrent(alut_context_id);
#endif

    /* Clear error code */
    alGetError();

    /* Generate Buffers */
    alGenBuffers(NUM_BUFFERS, g_Buffers);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alGenBuffers :", error);
        return -1;
    }

    /* Load sound files */
    strcpy(path, sharedPath);
    strcpy(path + len, STONE_FILE);
    if (loadSoundFile(path, 0) < 0)
        return -1;
    strcpy(path + len, PASS_FILE);
    if (loadSoundFile(path, 1) < 0)
        return -1;
    strcpy(path + len, CHAT_FILE);
    if (loadSoundFile(path, 2) < 0)
        return -1;
    strcpy(path + len, BEEP_FILE);
    if (loadSoundFile(path, 3) < 0)
        return -1;
    strcpy(path + len, MATCH_FILE);
    if (loadSoundFile(path, 4) < 0)
        return -1;
    strcpy(path + len, TIMEWARN_FILE);
    if (loadSoundFile(path, 5) < 0)
        return -1;

    printf("initSound Ok\n");
    return 0;
}

int loadSoundFile(const char *filename, int buffer)
{
    FILE *file;
#ifndef OAL_DECODES_VORBIS
    OggVorbis_File of;
    vorbis_info *vorbisInfo;
    int current = 0;
#endif
    ALenum format;
    ALsizei size, freq;
#ifdef __VISUALC__
    /* Crap compiler */
#define buf_size 4096*24
#else
    /* Good compiler */
    const int buf_size = 4096*24;
#endif
    char buf[buf_size];
    ALint error;

    printf("Loading file: %s\n", filename);

    /* Open file */
    if ((file = fopen(filename, "rb")) == NULL)
    {
        printf("Failed to open file %s.\n", filename);
        alDeleteBuffers(NUM_BUFFERS, g_Buffers);
        return -1;
    }

#ifdef OAL_DECODES_VORBIS
    /*
     * Let OpenAL decode the Vorbis buffer. Simply read the file
     * and pass the compressed data into the OAL buffer.
     */
    size = fread (buf, 1, buf_size, file);
    if (size <= 0)
    {
        printf("Error reading from file %s\n", filename);
        return -1;
    }
    fclose(file);
    freq = 1;  /* This gets ignored */
    format = AL_FORMAT_VORBIS_EXT;
#else
    /*
     * Read and decompress the Ogg data ourselves and pass the raw
     * data to the OAL buffer. This requires linking against ogg,
     * vorbis and vorbisfile libs.
     * This is required on Windows, as the Win32 OpenAL implementation
     * does not have the Vorbis decoding extension.
     */

    /* Open ogg stream */
    if (ov_open(file, &of, NULL, 0) < 0)
    {
        printf("Failed to load file %s\n", filename);
        fclose(file);
        alDeleteBuffers(NUM_BUFFERS, g_Buffers);
        return -1;
    }

    /* Get format and frequency */
    vorbisInfo = ov_info(&of, -1);
    if(vorbisInfo->channels == 1)
        format = AL_FORMAT_MONO16;
    else
        format = AL_FORMAT_STEREO16;
    freq = vorbisInfo->rate;

    /* Read data */
    size = 0;
    while(size < buf_size)
    {
        long ret=ov_read(&of,buf+size,buf_size-size,0,2,1,&current);
        if (ret > 0)
            size += ret;
        else if (ret == 0)  /* EOF */
            break;
        else if (ret < 0)
        {
            printf("Error in ogg stream.\n");
            ov_clear(&of);  /* Release stream before quitting */
            alDeleteBuffers(NUM_BUFFERS, g_Buffers);
            return -1;
        }
    }
#endif

    /* Copy data into AL Buffer */
    alBufferData(g_Buffers[buffer],format,buf,size,freq);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alBufferData buffer: ", error);
        alDeleteBuffers(NUM_BUFFERS, g_Buffers);
        return -1;
    }

    /* Release stream */
#ifndef OAL_DECODES_VORBIS
    ov_clear(&of);
#endif
    return 0;
}

int createSource()
{
    ALint error;

    ALfloat source0Pos[] = { 0.0, 0.0, 0.0 };
    ALfloat source0Vel[] = { 0.0, 0.0, 0.0};

    alGenSources(1,g_Source);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alGenSources : ", error);
        return -1;
    }

    alSourcefv(g_Source[0], AL_POSITION, source0Pos);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alSourcefv AL_POSITION : ", error);
        return -1;
    }

    alSourcefv(g_Source[0], AL_VELOCITY, source0Vel);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alSourcefv 0 AL_VELOCITY : ", error);
        return -1;
    }

    alSourcei(g_Source[0], AL_LOOPING, AL_FALSE);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alSourcei 0 AL_LOOPING true: ", error);
        return -1;
    }

    printf("createSource Ok\n");
    return 0;
}

OALAPI int OALAPIENTRY OALPlaySound(int s)
{
    ALint error;

    /* Stop source */
    alSourceStop(g_Source[0]);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alSourceStop 0 : ", error);
        return -1;
    }

    /* Attach buffer s to source */
    alSourcei(g_Source[0], AL_BUFFER, g_Buffers[s]);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alSourcei AL_BUFFER 0 : ", error);
        return -1;
    }

    /* Play */
    alSourcePlay(g_Source[0]);
    if ((error = alGetError()) != AL_FALSE)
    {
        DisplayALError("alSourcePlay 0 : ", error);
        return -1;
    }

    return 0;
}

void exitSound()
{
#ifdef _WIN32
    ALCcontext *Context;
    ALCdevice *Device;

    Context=alcGetCurrentContext();
    Device=alcGetContextsDevice(Context);
    alcMakeContextCurrent(NULL);
    alcDestroyContext(Context);
    alcCloseDevice(Device);
#else
    if(alut_context_id == NULL)
    {
        printf("Invalid context.\n");
        return;
    }
    alcDestroyContext(alut_context_id);
#endif

    printf("OpenAL shutdown\n");
}

OALAPI void OALAPIENTRY OALCleanupSound()
{
    /* Delete source and buffer */
    alDeleteSources(NUM_SOURCES, g_Source);
    alDeleteBuffers(NUM_BUFFERS, g_Buffers);

    /* Finito. */
    exitSound();
}

/* Display AL Error message */
ALvoid DisplayALError(const char *szText, ALint errorcode)
{
    printf("%s%s\n", szText, alGetString(errorcode));
}

OALAPI void OALAPIENTRY OALGetDeviceInfo(char *info)
{
    sprintf(info,
            "OpenAL sound information:\n\nDevice: %s\n"
            "Vendor: %s\n"
            "Version: %s\n"
            "Renderer: %s",
            alcGetString(alcGetContextsDevice(alcGetCurrentContext()),
                         ALC_DEFAULT_DEVICE_SPECIFIER),
            alGetString(AL_VENDOR),
            alGetString(AL_VERSION),
            alGetString(AL_RENDERER));
}


/* ------------------------------------------------------------------------
 *                           Debugging code
 * ------------------------------------------------------------------------ */

#ifdef STANDALONE
int main()
{
    int i;
    char sharedPath[64] = "../share/";
    char info[256];

    if (OALInitSound(sharedPath) < 0)
        return -1;

    OALGetDeviceInfo(info);
    printf("\n%s\nExtensions:\n%s\n", info, alGetString(AL_EXTENSIONS));

    for (i=0; i<NUM_BUFFERS; i++)
    {
        printf("Playing buffer %d\n", i);
        if (OALPlaySound(i) < 0)
            printf("Error playing buffer %d\n", i);
        printf("Sleeping a moment...\n");
#ifdef _WIN32
        Sleep(3000);
#else
        sleep(3);
#endif
    }

    OALCleanupSound();

    return 0;
}
#endif
