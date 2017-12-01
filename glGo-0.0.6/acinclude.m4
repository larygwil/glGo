dnl ---------------------------------------------------------------------------
dnl Macros for glGo build
dnl
dnl Peter Strempel <pstrempel@users.sourceforge.net>
dnl ---------------------------------------------------------------------------

dnl ---------------------------------------------------------------------------
dnl AM_CHECK_STATIC
dnl
dnl adds support for --enable-static-wx
dnl This macro requires previously set WX_LIBS or WX_LIBS_STATIC. It will
dnl choose one of them depending on the enable configuration.
dnl Return value is found in WX_LIBS_CHECKED
dnl ---------------------------------------------------------------------------
AC_DEFUN(AM_CHECK_STATIC,
[
AC_ARG_ENABLE(
  static-wx,
  [  --enable-static-wx      Link static to wxWindows (default: dynamic)],
  echo "Preparing static build against wxWindows"
  WX_LIBS_CHECKED="/usr/lib/libwx_gtk_xrc-2.4.a $WX_LIBS_STATIC /usr/lib/libwx_gtk_gl-2.4.a -lGL -lGLU",
  dnl Ignore this, modifications for my Linux binary build system with self-compiled wxWindows
  dnl WX_LIBS_CHECKED="/usr/local/lib/libwx_gtk_xrc-2.4.a $WX_LIBS_STATIC /usr/local/lib/libwx_gtk_gl-2.4.a -lGL -lGLU",
  echo "Preparing dynamic build against wxWindows"
  WX_LIBS_CHECKED="$WX_LIBS -lwx_gtk_gl-2.4 -lwx_gtk_xrc-2.4 -lGL -lGLU"
  dnl Ignore, for my debug build.
  dnl WX_LIBS_CHECKED="$WX_LIBS -lwx_gtkd_gl-2.4 -lwx_gtkd_xrc-2.4 -lGL -lGLU"
)

AC_SUBST(WX_LIBS_CHECKED)
])

dnl ---------------------------------------------------------------------------
dnl AM_CHECK_OPENAL
dnl
dnl adds support for --disable-openal
dnl This will disable linkage against OpenAL.
dnl If OpenAL is not disabled, a check for the headers is performed.
dnl ---------------------------------------------------------------------------
AC_DEFUN(AM_CHECK_OPENAL,
[
AC_ARG_ENABLE(
  openal,
  [  --disable-openal        Disable OpenAL (default: enabled)],
  echo Building without OpenAL
  openal=false,
  [
    dnl OpenAL headers present?
    AC_CHECK_HEADERS(AL/al.h AL/alc.h AL/alut.h vorbis/vorbisfile.h,,
    AC_MSG_ERROR([
    OpenAL headers not found.
    Please install the OpenAL development package: http://www.openal.org
    or disable OpenAL with the --disable-openal switch.]))
  ]
  openal=true
)
AM_CONDITIONAL(OPENAL, test x$openal = xtrue)
])

dnl ---------------------------------------------------------------------------
dnl AC_TEST_REQUIRED_HEADERS
dnl
dnl A collection of AC_CHECK_HEADER checks required by glGo.
dnl I moved them here to keep the configure.in script clean.
dnl If a check fails, configure will abort with an error message.
dnl ---------------------------------------------------------------------------
AC_DEFUN(AC_TEST_REQUIRED_HEADERS,
[
dnl OpenGL
AC_CHECK_HEADERS(GL/gl.h GL/glu.h,,
AC_MSG_ERROR([
OpenGL headers not found. Please install the OpenGL or Mesa development package.]))

dnl OpenGL extensions
AC_CHECK_HEADERS(GL/glext.h,,
AC_MSG_ERROR([
OpenGL extensions header not found. Check http://www.opengl.org]),
[
#if HAVE_GL_GL_H
# include <GL/gl.h>
# endif
])

dnl SDL_image
AC_CHECK_HEADER(SDL/SDL_image.h,,
AC_MSG_ERROR([
SDL_image header not found.
Please install the SDL_image development package: http://www.libsdl.org]))

dnl SDL_ttf
AC_CHECK_HEADER(SDL/SDL_ttf.h,,
AC_MSG_ERROR([
SDL_ttf header not found.
Please install the SDL_ttf development package: http://www.libsdl.org]))

dnl SDL_gfx
AC_CHECK_HEADERS(SDL/SDL_rotozoom.h SDL/SDL_gfxPrimitives.h,,
AC_MSG_ERROR([
SDL_gfx headers not found.
Please install the SDL_gfx development package: http://www.libsdl.org]))

dnl SDL_mixer
AC_CHECK_HEADER(SDL/SDL_mixer.h,,
AC_MSG_ERROR([
SDL_mixer header not found.
Please install the SDL_mixer development package: http://www.libsdl.org]))

dnl PLIB
AC_LANG_PUSH(C++)
AC_CHECK_HEADERS(plib/ul.h plib/fnt.h,,
AC_MSG_ERROR([
PLIB headers not found.
Please install the PLIB development package: http://plib.sourceforge.net
plibfnt and plibul is sufficient. I don't need the complete PLIB suite.]))
AC_LANG_POP()

dnl zzip
AC_CHECK_HEADER(zzip/zzip.h,,
AC_MSG_ERROR([
ZZip header not found.
Please install the ZZipLib development package: http://zziplib.sourceforge.net/]))

dnl Python
AC_CHECK_HEADER(python2.3/Python.h,,
AC_MSG_ERROR([
Python 2.3 headers not found.
Please install the Python 2.3 development package: http://www.python.org/]))
])
