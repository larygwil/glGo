/*
 * gtp_scorer.cpp
 *
 * $Id: gtp_scorer.cpp,v 1.3 2003/10/07 21:12:21 peter Exp $
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
#pragma implementation "gtp_scorer.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#include "wx/intl.h"
#include "wx/frame.h"
#endif

#include <wx/process.h>
#include "mainframe.h"
#include "gtp_events.h"
#include "gtp_scorer.h"

#define ID_GTP_SCORER_PROCESS 600


BEGIN_EVENT_TABLE(GTPScorer, wxEvtHandler)
    EVT_END_PROCESS(ID_GTP_SCORER_PROCESS, GTPScorer::OnProcessTerm)
END_EVENT_TABLE()


GTPScorer::GTPScorer(MainFrame *frame, const wxString &gtp_engine, const wxString &filename,
                     int move_number, bool is_temp_file)
    : frame(frame), gtp_engine(gtp_engine), filename(filename), move_number(move_number),
      is_temp_file(is_temp_file)
{
    proc = NULL;
    gtp_in = NULL;
    gtp_err = NULL;

    LOG_GTP(wxString::Format(_T("Scoring file %s at move %d using engine '%s'"),
                             filename.c_str(), move_number, gtp_engine.c_str()));

    // Assemble command like "gnugo --score estimate -L 123 --quiet -l game.sgf";
    wxString cmd = gtp_engine + _T(" --score estimate ") +
        (move_number != -1 ? "-L " + move_number : "") +
        _T(" --quiet -l ") + filename;

    // Create GTP process
    Connect(cmd);
}

bool GTPScorer::Connect(const wxString &cmd)
{
    // Create new process as child of this class, so this receives an event when
    // the process terminates.
    proc = new wxProcess(this, ID_GTP_SCORER_PROCESS);

    // Tell process we want stdin and stderr
    proc->Redirect();

    // Try to start process and execute gnugo command in GTP mode
    LOG_GTP(wxString::Format(_T("Trying to execute: %s"), cmd.c_str()));
    long pid = 0;
    {
        // wxLogNull logNo;  // Avoid messagebox, we handle error ourselves
        pid = wxExecute(cmd, wxEXEC_ASYNC, proc);
    }

    // Failed to create process. Most likely reason is, the command was not found.
    wxLogDebug(_T("PID = %d"), static_cast<int>(pid));
    if (pid == -1)
    {
        LOG_GTP(_T("Error: Failed to start process."));
        delete proc;
        gtp_in = NULL;
        gtp_err = NULL;
        return false;
    }

    // Can we grab stdin?
    if (!proc->IsInputOpened())
    {
        LOG_GTP(_T("Failed to take stdin from process."));
        delete proc;
        gtp_in = NULL;
        gtp_err = NULL;
        return false;
    }

    gtp_in = proc->GetInputStream();
    gtp_err = proc->GetErrorStream();

    // Allright, process up and running
    wxLogDebug(_T("Process running"));

    return true;
}

void GTPScorer::OnProcessTerm(wxCommandEvent& WXUNUSED(event))
{
    wxLogDebug("TERMINATE");

    // Read stdin from GTP engine
    const size_t buf_size = 128;
    char buf[buf_size];
    gtp_in->Read(buf, buf_size);

    wxString result;
    bool error_flag = false;

    // GTP stdin had any data?
    if (gtp_in->LastRead() > 0)
    {
        result = wxString(buf, gtp_in->LastRead());
        result.Trim();
        wxLogDebug("Result: %s", result.c_str());

        // This should not happen, 128 chars should be big enough
        if (!gtp_in->Eof())
            LOG_GTP(_T("Wired, GNU Go sent a longer string than expected!"));
    }
    else
    {
        LOG_GTP(_T("Error: No result input from GNU Go incoming."));
        error_flag = true;

        // So let's read GTP stderr and see what went wrong
        gtp_err->Read(buf, buf_size);
        if (gtp_err->LastRead() > 0)
        {
            result = wxString(buf, gtp_err->LastRead());
            result.Trim();
            wxLogDebug("Error: %s", result.c_str());
        }
        else
        {
            LOG_GTP(_T("Error: Even worse, GNU Go does not tell us why scoring failed."));
            result = _("GNU Go failed to score this game.");
        }
    }

    delete proc;
    proc = NULL;
    gtp_in = NULL;
    gtp_err = NULL;
    wxLogDebug(_T("Process deleted"));

    // Remove file *only* if it was created as temp file
    if (is_temp_file)
    {
        wxLogDebug("Removing file %s", filename.c_str());
        wxRemoveFile(filename);
    }

    // Display score
    // frame->showScore(result, error_flag);
    EventGTPScore event(result, error_flag, this);
    frame->AddPendingEvent(event);
}
