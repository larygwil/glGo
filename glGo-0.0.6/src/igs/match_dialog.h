/*
 * match_dialog.h
 *
 * $Id: match_dialog.h,v 1.3 2003/11/04 14:55:00 peter Exp $
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

#ifndef MATCH_DIALOG_H
#define MATCH_DIALOG_H

#ifdef __GNUG__
#pragma interface "match_dialog.h"
#endif


/**
 * The match dialog. There are two different resource files for this dialog depending
 * if it is an outgoing or incoming match request.
 * @ingroup igsgui
 */
class MatchDialog : public wxDialog
{
public:
    MatchDialog(IGSMainFrame *parent, Match *m, const wxString &opp_rank=wxEmptyString);
    ~MatchDialog();
    bool TransferDataFromWindow();
    void OnOK(wxCommandEvent& WXUNUSED(event));
    void OnCancel(wxCommandEvent& WXUNUSED(event));

private:
    void OnSwap(wxCommandEvent& WXUNUSED(event));
    void OnStats(wxCommandEvent& WXUNUSED(event));

    IGSMainFrame *parentFrame;
    Match *match;

DECLARE_EVENT_TABLE()
};

#endif
