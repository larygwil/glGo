noinst_LIBRARIES = libsdlcore.a
libsdlcore_a_SOURCES = \
	SDL_rwops_zzip.c SDL_rwops_zzip.h

EXTRA_PROGRAMS = sdltest
sdltest_SOURCES = \
	sdltest.c \
	sdl_core.c sdl_core.h \
	SDL_rwops_zzip.c SDL_rwops_zzip.h

LDFLAGS =
sdltest_LDADD = $(SDL_LIBS) -lSDL_image -lSDL_ttf -lSDL_gfx -lzzip

## Ignore this, for my Linux binary build only to force static link with SDL_gfx and zzip
## sdltest_LDADD = $(SDL_LIBS) -lSDL_image -lSDL_ttf /usr/local/lib/libSDL_gfx.a /usr/local/lib/libzzip.a

CLEANFILES = sdltest
