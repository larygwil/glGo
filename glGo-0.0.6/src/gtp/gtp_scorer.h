/*
 * gtp_scorer.h
 *
 * $Id: gtp_scorer.h,v 1.3 2003/10/02 14:17:33 peter Exp $
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

#ifndef GTP_SCORER_H
#define GTP_SCORER_H

#ifdef __GNUG__
#pragma interface "gtp_scorer.h"
#endif

class MainFrame;
class wxInputStream;

/**
 * This class calls a GNU Go process for score estimation.
 * The result is sent to the calling MainFrame using an EventGTPScore
 * @ingroup gtp
 */
class GTPScorer : public wxEvtHandler
{
public:
    /**
     * Constructor. Creating a GTPScorer object will call a GNU Go process and run
     * a score estimation on a (temporary) file. The result is sent back to the
     * MainFrame using an event.
     * @param frame Calling MainFrame
     * @param gtp_engine Name (and path) of the GTP program to use
     * @param filename File to run through the scorer
     * @param move_number Move used for scoring. If -1, the last move is scored
     * @param is_temp_file If this is true, the given file will be deleted
     */
    GTPScorer(MainFrame *frame, const wxString &gtp_engine, const wxString &filename,
              int move_number, bool is_temp_file = true);

private:
    bool Connect(const wxString &cmd);
    void OnProcessTerm(wxCommandEvent& WXUNUSED(event));

    MainFrame *frame;
    wxString gtp_engine, filename;
    int move_number;
    bool is_temp_file;
    wxProcess *proc;
    wxInputStream *gtp_in, *gtp_err;

DECLARE_EVENT_TABLE()
};

#endif
