/*
 * logger.h
 *
 * $Id: logger.h,v 1.3 2003/10/02 14:32:32 peter Exp $
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

#ifndef LOGGER_H
#define LOGGER_H

#ifdef __GNUG__
#pragma interface "logger.h"
#endif

/**
 * A customized logger subclass of wxLogStderr.
 * This class categorizes the log output done in the application using the
 * LOG_XXX macros (see defines.h) by overwriting wxLog::DoLog and prepending
 * the category name of the user loglevel to the output.
 * This class takes care to close the logfile in its destructor.
 * @ingroup utils
 */
class Logger : public wxLogStderr
{
public:

    /**
     * Creates a Logger target which sends its output to the given FILE or
     * to stderr if the FILE is NULL.
     * @param fp Pointer to the C stream. If NULL, output is sent to stderr
     */
    Logger(FILE *fp = NULL);

    /** Destructor. Takes care to close the file and delete the pointer. */
    virtual ~Logger();

protected:
    /**
     * Overwrites wxLog::DoLog adding some prepending text to the output depending
     * on the user loglevel.
     */
    virtual void DoLog(wxLogLevel level, const wxChar *msg, time_t timestamp);

private:
    FILE *logfile;  ///< C file stream, output logfile
};

#endif
