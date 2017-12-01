EXTRA_DIST = glGo.xml html

htmldir = $(prefix)/share/doc/glGo
html_DATA = html

html: glGo.xml
	-xmlto html -o html -x html.xsl glGo.xml
	mkdir -p html/images
	cp images/*.jpg html/images

htmlhelp: glGo.xml context.h
	-xmlto htmlhelp -o htmlhelp -x htmlhelp.xsl glGo.xml
	mkdir -p htmlhelp/images
	cp images/*.jpg htmlhelp/images

glGo.htb: htmlhelp
	zip -j glGo.htb htmlhelp/*.html htmlhelp/*.hhp htmlhelp/*.hhc
	zip glGo.htb images/*.jpg
	cp glGo.htb ../src/share

clean:
	rm -rf html htmlhelp glGo.htb

install-exec-local:
	$(mkinstalldirs) $(htmldir)/html/images
