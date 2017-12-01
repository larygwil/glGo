SUBDIRS=src htmlhelp
EXTRA_DIST=Doxyfile footer.html README-SDL.txt README-Vorbis.txt

docdir = $(prefix)/share/doc/glGo
doc_DATA = AUTHORS ChangeLog COPYING LICENCE README README-SDL.txt README-Vorbis.txt

xrc:
	$(MAKE) xrc -C src

i18n:
	$(MAKE) i18n -C src

i18nupdate:
	$(MAKE) i18nupdate -C src

data:
	rm -f src/share/data.zip src/share/data.dat
	cd src/share && zip data.zip *.jpg *.png && zzxorcopy -8823 data.zip data.dat

installer:
	touch dummy
	fromdos License_install.txt
	fakeroot epm -s src/images/ggo.xpm glGo
	cat install-header linux-2.4-intel/$(PACKAGE)-$(VERSION)-linux-2.4-intel.tar.gz > $(PACKAGE)-$(VERSION).sh
	chmod +x $(PACKAGE)-$(VERSION).sh
	rm -f dummy

rpm:
	fakeroot epm -f rpm glGo
	cp linux-2.4-intel/$(PACKAGE)-$(VERSION)-linux-2.4-intel.rpm $(PACKAGE)-$(VERSION).rpm

deb:
	fakeroot epm -f deb glGo
	cp linux-2.4-intel/$(PACKAGE)-$(VERSION)-linux-2.4-intel.deb $(PACKAGE)-$(VERSION).deb

installerclean:
	rm -rf linux-2.4-intel
	rm -f $(PACKAGE)-$(VERSION).sh
	rm -f $(PACKAGE)-$(VERSION).rpm
	rm -f $(PACKAGE)-$(VERSION).deb

backup:
	cd .. && tar czf glGo.tgz glGo
	cp ../glGo.tgz ~/backups
	cp ../glGo.tgz ~/data/backups
	mv ../glGo.tgz ~

sfbackup:
	cd ~ && scp glGo.tgz pstrempel@ggo.sourceforge.net:/home/users/p/ps/pstrempel/backups

patch:
	rm -f patch patch.tgz glGo-$(VERSION)-Patch.sh
	-xdelta delta -9 /opt/glGo/bin/glGo src/glGo patch
	cp src/share/resource.xrs .
	tar czf patch.tgz patch resource.xrs ChangeLog
	cat patch-header patch.tgz > glGo-$(VERSION)-Patch.sh
	chmod +x glGo-$(VERSION)-Patch.sh

pythonlib:
	cd python/playerdb && python compile.py && zip pythonlib.zip glGo/*.pyc && mv pythonlib.zip ../../src/share
