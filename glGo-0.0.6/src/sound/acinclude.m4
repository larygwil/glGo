dnl ---------------------------------------------------------------------------
dnl Macros for glGo soundlibs build
dnl
dnl Peter Strempel <pstrempel@users.sourceforge.net>
dnl ---------------------------------------------------------------------------

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
