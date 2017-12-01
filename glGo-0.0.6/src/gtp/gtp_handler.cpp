/*
 * gtp_handler.cpp
 *
 * $Id: gtp_handler.cpp,v 1.7 2003/10/02 14:17:33 peter Exp $
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
#pragma implementation "gtp_handler.h"
#endif

// For compilers that support precompilation, includes "wx/wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/process.h>
#include "gtp_handler.h"
#include "gtp.h"

typedef void *ExitCode;

GTPHandler::GTPHandler(wxProcess *process, long pid, GTP *gtp)
    : proc(process),
      pid(pid),
      gtp(gtp),
      gtp_in(*process->GetInputStream()),
      gtp_err(*process->GetInputStream()),
      gtp_out(*process->GetOutputStream()),
      in_entry(false)
{ }

ExitCode GTPHandler::Entry()
{
    in_entry = true;
    wxLogDebug(_T("ENTRY"));
    wxString read;

    while (!TestDestroy() && proc != NULL && proc->IsInputOpened())
    {
        // Read next line from stdin stream of the process
        read = gtp_in.ReadLine();
        if (!read.IsEmpty())
        {
            // Create and send event to GTP about this incoming command
            EventGTPCommand evt(read, GTP_COMMAND_RECEIVE);
            wxPostEvent(gtp, evt);
        }
    }

    wxLogDebug(_T("LEAVING ENTRY"));
    in_entry = false;
    return 0;
}

void GTPHandler::sendCommand(const wxString &cmd)
{
    if (proc == NULL)
        return;
    wxLogDebug(wxString::Format(_T("Sending command: %s"), cmd.c_str()));
    gtp_out.WriteString(cmd + _T('\n'));
}

void GTPHandler::OnExit()
{
    wxLogDebug(_T("GTPHandler::OnExit()"));

    if (proc != NULL)
        DetachProcess();
    wxLogDebug(_T("Thread done"));

    if (gtp != NULL)
        gtp->notifyThreadClosed();
}

void GTPHandler::DetachProcess()
{
    wxLogDebug(_T("GTPHandler::DetachProcess()"));

    proc->Detach();
    proc = NULL;
    wxLogDebug(_T("Detached process"));
}

void GTPHandler::KillProcess(bool no_notify)
{
    wxLogDebug(_T("GTPHandler::KillProcess()"));

    if (proc != NULL)
    {
        if (no_notify)
            gtp = NULL;
        DetachProcess();
        wxProcess::Kill(pid, wxSIGKILL);
        wxLogDebug(_T("Sent SIGKILL"));
    }
}
