/*
 * htmlhelp_context.h
 *
 * $Id: htmlhelp_context.h,v 1.7 2003/11/24 14:38:19 peter Exp $
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

/**
 * @page htmlhelp Creating HTML Help
 *
 * The HTML Help manual is created with a DocBook processor from the file
 * glGo.xml. You can use whatever processor you like and which can handle
 * DocBook-XML. I use xmlto as it is reasonble fast and creates good
 * results. Saxon is another good processor. To produce HTML or HTMLHelp files
 * from glGo.xml you need to install DocBook which ships with all Linux
 * distributions. See the Makefiles in the htmlhelp directory for how the xmlto
 * tool is used. The Makefiles create a HTML version which is included into the
 * distribution as plain HTML files and a HTMLHelp version which is used to
 * create the files loaded from the %glGo Help viewer. Those files differ on
 * Windows and Linux. %glGo uses the built-in HTMLHelp mechanism in wxWindows
 * which uses Microsofts HTMLHelp on Windows and a generic implementation on
 * Linux. To create the Windows HTMLHelp files you need the HTML Workshop tool
 * from Microsoft and run it on Windows/Wine/whatever on the created glGo.hhp
 * file in the htmlhelp directory. The creates the glGo.chm file which can be
 * viewed as standalone or from the %glGo manual viewer on Windows. On Linux the
 * htmlhelp directory is zipped into an archive named glGo.htb, this file can be
 * loaded from wxWindows and displayed in the manual viewer (this would work on
 * Windows, too, but the Windows built-in HTMLHelp viewer just looks
 * better). See the Makefile in htmlhelp/ on how exactly to do this
 * step. glGo.chm is finally included in the Windows distribution, glGo.htb in
 * the Linux distribution.
 *
 * If you want to create the html files from glGo.xml on Windows, expect some
 * major pain. You will need to download DocBook and Saxon and adjust the paths
 * to your DTD files in glGo.xml, html.xsl and htmlhelp.xsl, then run Saxon with
 * the proper arguments (see Saxon documentation) from a console. I personally
 * use Linux only to create the help files.
 *
 * Please also see the file @ref htmlhelp_context for some caveats and possible
 * problems.
 */

/**
 * @file
 * @anchor htmlhelp_context
 * The MS HTML Help indices in this file *must* be the same as in
 * ../htmlhelp/context.h
 *
 * I assume Microsoft assumes one could include the context.h file
 * into the C++ sources. But one cannot include something with
 * a line "#define index 0" into C++ code! Great job, Redmond!
 *
 * So when context.h is updated in the htmlhelp directory, this file
 * must be updated, too.
 *
 * Even worse, for the Microsoft HTML Help it is required to give the
 * filename in context.h. Unfortnately the filename is generated
 * dynamic and automatically by DocBook. Great job, Redmond! They
 * probably expect everyone using their crappy HTMLHelp workshop to
 * write this docu or buy a commercial one.
 *
 * See @ref htmlhelp for an overview of the %glGo HTML Help.
 */

#ifndef HTMLHELP_CONTEXT_H
#define HTMLHELP_CONTEXT_H

#include "defines.h"

#ifdef USE_MSHTMLHELP

// Chapters for Microsoft HTML Help using integers
// These must be the same as in ../htmlhelp/context.h

#define HTMLHELP_CONTEXT_INDEX    1  ///< Index chapter
#define HTMLHELP_CONTEXT_GNUGO    2  ///< GNU Go chapter
#define HTMLHELP_CONTEXT_OPENGL   3  ///< OpenGL chapter
#define HTMLHELP_CONTEXT_SDL      4  ///< SDL chapter
#define HTMLHELP_CONTEXT_OPTIONS  5  ///< Options chapter
#define HTMLHELP_CONTEXT_PLAYERDB 6  ///< Playerdatabase chapter

#else // USE_MSHTMLHELP

// Chapters for Unix wxHTMLHelp using strings

#define HTMLHELP_CONTEXT_INDEX    _T("index.html")           ///< Index chapter
#define HTMLHELP_CONTEXT_GNUGO    _T("Playing with GNU Go")  ///< GNU Go chapter
#define HTMLHELP_CONTEXT_OPENGL   _T("OpenGL Information")   ///< OpenGL chapter
#define HTMLHELP_CONTEXT_SDL      _T("SDL Information")      ///< SDL chapter
#define HTMLHELP_CONTEXT_OPTIONS  _T("Options")              ///< Options chapter
#define HTMLHELP_CONTEXT_PLAYERDB _T("The player database")  ///< Playerdatabase chapter

#endif // !USE_MSHTMLHELP

#endif // HTMLHELP_CONTEXT_H
