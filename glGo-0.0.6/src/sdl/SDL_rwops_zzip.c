/*
 *      Copyright (c) 2001 Guido Draheim <guidod@gmx.de>
 *      Use freely under the restrictions of the ZLIB License
 *
 *      (this example uses errno which might not be multithreaded everywhere)
 */

#ifndef __VISUALC__
/* No POSIX with MSVC. Maybe with stlport, didn't try yet. */
#define USE_IO_EXT
#endif

#include "SDL_rwops_zzip.h"
#ifdef _WIN32
#define _zzip_ssize_t size_t
#endif
#include <zzip/zzip.h>
#ifdef USE_IO_EXT
#include <zzip/plugin.h>
#endif
#undef _zzip_read

#include <string.h> /* strchr */

#ifdef USE_IO_EXT

#if defined ZZIP_HAVE_UNISTD_H
#include <unistd.h>
#elif defined ZZIP_HAVE_IO_H
#include <io.h>
#else
#error need posix io for this example
#endif

#include <string.h> /* strchr */

/* This is Win32 specific */
#ifndef O_BINARY
#define O_BINARY 0
#endif

/* XOR key */
static int xor_value = 8823;
static struct zzip_plugin_io xor_handlers;

/* File extension .dat */
static zzip_strings_t xor_fileext[] = { ".dat", "", 0 };

#endif  // USE_IO_EXT

/* MSVC can not take a casted variable as an lvalue ! */
#define SDL_RWOPS_ZZIP_DATA(_context) \
             ((_context)->hidden.unknown.data1)
#define SDL_RWOPS_ZZIP_FILE(_context)  (ZZIP_FILE*) \
             ((_context)->hidden.unknown.data1)

static int _zzip_seek(SDL_RWops *context, int offset, int whence)
{
    return zzip_seek(SDL_RWOPS_ZZIP_FILE(context), offset, whence);
}

static int _zzip_read(SDL_RWops *context, void *ptr, int size, int maxnum)
{
    return zzip_read(SDL_RWOPS_ZZIP_FILE(context), ptr, size*maxnum);
}

static int _zzip_write(SDL_RWops *context, const void *ptr, int size, int num)
{
    return 0; /* ignored */
}

static int _zzip_close(SDL_RWops *context)
{
    if (! context) return 0; /* may be SDL_RWclose is called by atexit */

    zzip_close (SDL_RWOPS_ZZIP_FILE(context));
    SDL_FreeRW (context);
    return 0;
}

#ifdef USE_IO_EXT
static zzip_ssize_t xor_read (int f, void* p, zzip_size_t l)
{
    zzip_ssize_t r = read(f, p, l);
    zzip_ssize_t x; char* q; for (x=0, q=p; x < r; x++) q[x] ^= xor_value;
    return r;
}
#endif

SDL_RWops *SDL_RWFromZZIP(const char* file, const char* mode)
{
    register SDL_RWops* rwops;
    register ZZIP_FILE* zzip_file;

    if (! strchr (mode, 'r'))
	return SDL_RWFromFile(file, mode);

#ifndef USE_IO_EXT
    zzip_file = zzip_fopen (file, mode);
#else
    zzip_init_io (&xor_handlers, 0); xor_handlers.read = &xor_read;
    zzip_file = zzip_open_ext_io (file, O_RDONLY|O_BINARY, ZZIP_ALLOWREAL | ZZIP_CASELESS, xor_fileext, &xor_handlers);
#endif
    if (! zzip_file) return 0;

    rwops = SDL_AllocRW ();
    if (! rwops)
    {
#ifndef _WIN32
        errno=ENOMEM;
#endif
        zzip_close (zzip_file);
        return 0;
    }

    SDL_RWOPS_ZZIP_DATA(rwops) = zzip_file;
    rwops->read = _zzip_read;
    rwops->write = _zzip_write;
    rwops->seek = _zzip_seek;
    rwops->close = _zzip_close;
    return rwops;
}
