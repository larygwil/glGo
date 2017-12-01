/*
 * gtp_handler.h
 *
 * $Id: gtp_handler.h,v 1.4 2003/10/02 14:17:33 peter Exp $
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

#ifndef GTP_HANDLER_H
#define GTP_HANDLER_H

#ifdef __GNUG__
#pragma interface "gtp_handler.h"
#endif

#include <wx/thread.h>
#include <wx/txtstrm.h>

class GTP;
class wxProcess;

/**
 * GTPHandler thread
 * @ingroup gtp
 */
class GTPHandler : public wxThread
{
public:
    GTPHandler(wxProcess *process, long pid, GTP *gtp);
    virtual ExitCode Entry();
    void sendCommand(const wxString &cmd);
    void OnExit();
    void DetachProcess();
    void KillProcess(bool no_notify = false);

    /**
     * Check if the thread is currently shutting down.
     * If this is true, it is _unsafe_ to call Delete() on this object.
     * @return True if the thread is quitting, else false
     * @todo This is probably redundant, a call to wxThread::isAlive()
     *       should do, too.
     */
    bool isQuitting() { return !in_entry; }

private:
    wxProcess *proc;
    long pid;
    GTP *gtp;
    wxTextInputStream gtp_in;
    wxTextInputStream gtp_err;
    wxTextOutputStream gtp_out;
    bool in_entry;
};

#endif
