/*
 * logger.cpp
 *
 * $Id: logger.cpp,v 1.8 2003/10/02 14:32:32 peter Exp $
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

#ifdef __GNUG__
#pragma implementation "logger.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#include "wx/intl.h"
#endif

#include "logger.h"
#include "defines.h"

Logger::Logger(FILE *fp)
    : wxLogStderr(fp), logfile(fp)
{}

Logger::~Logger()
{
    // Close logfile
    if (logfile != NULL && fclose(logfile))
        wxLogDebug(_T("Failed to close logfile."));
}

void Logger::DoLog(wxLogLevel level, const wxChar *msg, time_t timestamp)
{
    switch (level)
    {
    case LOG_GLOBAL:
        DoLogString(wxString(_T("GLOBAL: ")) + msg, timestamp);
        break;
    case LOG_BOARD:
        DoLogString(wxString(_T("BOARD : ")) + msg, timestamp);
        break;
    case LOG_OPENGL:
        DoLogString(wxString(_T("OPENGL: ")) + msg, timestamp);
        break;
    case LOG_SDL:
        DoLogString(wxString(_T("SDL   : ")) + msg, timestamp);
        break;
    case LOG_SOUND:
        DoLogString(wxString(_T("SOUND : ")) + msg, timestamp);
        break;
    case LOG_SGF:
        DoLogString(wxString(_T("SGF   : ")) + msg, timestamp);
        break;
    case LOG_IGS:
        DoLogString(wxString(_T("IGS   : ")) + msg, timestamp);
        break;
    case LOG_GTP:
        DoLogString(wxString(_T("GTP   : ")) + msg, timestamp);
        break;
    default:
        wxLogStderr::DoLog(level, msg, timestamp);
    }
}
