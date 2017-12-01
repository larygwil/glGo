SUBDIRS = dialogs utils sgf sdl gtp igs ugf sound share
EXTRA_DIST = images rc

bin_PROGRAMS=glGo

glGo_SOURCES = \
	glGo.cpp glGo.h defines.h htmlhelp_context.h \
	sidebar.cpp sidebar.h \
	gamedata.cpp gamedata.h \
	stone.cpp stone.h stones.h \
	move.cpp move.h \
	game.cpp game.h \
	events.cpp events.h \
	board_eventhandler.cpp board_eventhandler.h \
	marks.cpp marks.h \
	clock.cpp clock.h \
	matrix.cpp matrix.h \
	boardhandler.cpp boardhandler.h \
	board.cpp board.h \
	glBoard.cpp glBoard.h \
	sdlboard.cpp sdlboard.h \
	mainframe.cpp mainframe.h

INCLUDES = -Idialogs -Iutils -Isgf -Isdl -Isound -Igtp -Iigs

glGo_DEPENDENCIES = $(glGo_OBJECTS) igs/libigs.a gtp/libgtp.a dialogs/libdialogs.a utils/libutils.a sgf/libsgf.a sdl/libsdlcore.a sound/libsound.a ugf/libugf.a

glGo_LDFLAGS = -Lutils -Lsgf -Lsdl -Lsound -Lgtp -Ligs -Ldialogs -Lugf -Wl,-rpath,$(pkglibdir)
glGo_LDADD = -lutils -lsgf -lsdlcore -lsound -lgtp -ligs -ldialogs -lugf -lSDL -lSDL_image -lSDL_ttf -lSDL_gfx -lplibfnt -lplibul -lzzip -lpython2.3

## Force static link to SDL_gfx, plib and zziplib and set dynamic linker flag to binary install path.
## Ignore this, this is only for my binary Linux release build.

## glGo_LDFLAGS = -Lutils -Lsgf -Lsdl -Lsound -Lgtp -Ligs -Ldialogs -Lugf -Wl,-rpath,/opt/glGo/lib
## glGo_LDADD = -lutils -lsgf -lsdlcore -lsound -lgtp -ligs -ldialogs -lugf -lSDL -lSDL_image -lSDL_ttf /usr/local/lib/libSDL_gfx.a /usr/local/lib/libplibfnt.a /usr/local/lib/libplibul.a /usr/local/lib/libzzip.a -lpython2.3

## wxGTK-xrc for static builds requires some reordering
## glGo: $(glGo_OBJECTS) $(glGo_DEPENDENCIES)
##	@rm -f glGo
##	$(CXXLD) -o $@ $(glGo_OBJECTS) $(glGo_LDFLAGS) $(glGo_LDADD) $(AM_CXXFLAGS) $(CXXFLAGS) $(LDFLAGS)  $(LIBS)

xrc:
	cd rc && wxrc -o ../share/resource.xrs *.xrc
i18n:
	mkdir -p share/de_DE share/nl_NL
	msgfmt -c -o share/de_DE/messages.mo ../translation/de.po
	msgfmt -c -o share/nl_NL/messages.mo ../translation/nl.po

i18nupdate:
	-wxrc -g -o rcmsg.cpp rc/*.xrc
	xgettext -C -k_ *.h *.cpp dialogs/*.h dialogs/*.cpp utils/*.h utils/*.cpp sgf/*.h sgf/*.cpp gtp/*.h gtp/*.cpp igs/*.h igs/*.cpp
	msgmerge -U ../translation/de.po messages.po
	msgmerge -U ../translation/nl.po messages.po

backup:
	tar czf glgo_src.tgz *.h *.cpp dialogs/*.h dialogs/*.cpp utils/*.h utils/*.cpp sgf/*.h sgf/*.cpp gtp/*.h gtp/*.cpp igs/*.h igs/*.cpp sdl/*.h sdl/*.c sound/*.h sound/*.c sound/*.cpp ugf/*h ugf/*.c ugf/*.cpp rc/*.xrc
	cp glgo_src.tgz ~/backups
	mv glgo_src.tgz ~

sfbackup:
	cd ~ && scp glgo_src.tgz pstrempel@ggo.sourceforge.net:/home/users/p/ps/pstrempel/backups
