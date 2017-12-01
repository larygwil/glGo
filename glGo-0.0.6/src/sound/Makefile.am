noinst_LIBRARIES = libsound.a
if OPENAL
SLIBS_AL = libalsound.la
else
SLIBS_AL =
endif
SLIBS_SDL = libsdlsound.la
pkglib_LTLIBRARIES = $(SLIBS_AL) $(SLIBS_SDL)
EXTRA_PROGRAMS = soundtest

libsound_a_SOURCES = sound.cpp sound.h
if OPENAL
libalsound_la_SOURCES = al_sound.c al_sound.h
endif
libsdlsound_la_SOURCES = sdl_sound.c sdl_sound.h
soundtest_SOURCES = soundtest.c

LDFLAGS =
if OPENAL
libalsound_la_LIBADD = -lopenal
endif
libalsound_la_LDFLAGS = -version-info 0:4:0
libsdlsound_la_LIBADD = -lSDL_mixer
libsdlsound_la_LDFLAGS = -version-info 0:2:0

## Ignore this, for my Linux binary build only
## soundtest_LDFLAGS = -Wl,-rpath,/opt/glGo/lib
soundtest_LDADD = libsound.a -ldl -lplibul -lstdc++

if OPENAL
INCLUDES =
else
INCLUDES = -DNO_OPENAL
endif

CLEANFILES = soundtest
