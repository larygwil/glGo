/*
 * sgfwriter.cpp
 *
 * $Id: sgfwriter.cpp,v 1.9 2003/10/05 02:12:15 peter Exp $
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
#pragma implementation "sgfwriter.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/log.h>
#include <wx/intl.h>
#include <wx/msgdlg.h>
#endif

#include <wx/wfstream.h>
#include "sgfwriter.h"
#include "boardhandler.h"
#include "game.h"
#include "move.h"
#include "gamedata.h"


SGFWriter::SGFWriter()
{
    ostream = NULL;
}

bool SGFWriter::saveSGF(const wxString &filename, Game *game)
{
    Move *root = game->getRoot();
    wxASSERT(root != NULL);

    // This should actually never happen, but who knows.
    if (root == NULL)
    {
        wxMessageBox(_("Error saving SGF: The game has no root move!"),
                     _("SGF Error"), wxOK | wxICON_ERROR);
        return false;
    }

    // Create output file
    wxFileOutputStream fostream(filename);
    if (!fostream.Ok())
        return false;

    // Created buffered stream
    ostream = new wxBufferedOutputStream(fostream);
    wxASSERT(ostream != NULL);

    // Write game header to root node
    writeGameHeader(game->getGameData());

    // Traverse the tree recursive in pre-order
    traverse(root);
    ostream->Write("\n", 1);

    // Delete stream (this syncs the stream)
    delete ostream;
    ostream = NULL;

    return true;
}

void SGFWriter::writeGameHeader(GameData *data)
{
    wxString s;

    // We play Go, we use FF 4
    s = "(;GM[1]FF[4]";

    // Charset TODO
    // s += "CA[" + data.charset + "]" +

    // Application name : Version
    s += wxString::Format("AP[%s:%s]", PACKAGE , VERSION);

    // We show vars of current node
    s += "ST[1]\n";

    // Game Name
    if (!data->gameName.empty())
        s += _T("GN[") + data->gameName + "]";

    // Board size
    s += wxString::Format("\nSZ[%u]", data->board_size);

    // Handicap
    if (data->handicap >= 2)
        s += wxString::Format("HA[%u]", data->handicap);

    // Komi
    s += wxString::Format("KM[%.1f]\n", data->komi);

    // White name
    if (!data->whiteName.empty())
        s += _T("PW[") + data->whiteName + "]";

    // White rank
    if (!data->whiteRank.empty())
        s += _T("WR[") + data->whiteRank + "]";

    // Black name
    if (!data->blackName.empty())
        s += _T("PB[") + data->blackName + "]";

    // Black rank
    if (!data->blackRank.empty())
        s += _T("BR[") + data->blackRank + "]";

    // Result
    if (!data->result.empty())
        s += _T("RE[") + data->result + "]";

    // Time
    if (!data->time.empty())
        s += _T("TM[") + data->time + "]";

    // Date
    if (!data->date.empty())
        s += _T("DT[") + data->date + "]";

    // Place
    if (!data->place.empty())
        s += _T("PC[") + data->place + "]";

    // Copyright
    if (!data->copyright.empty())
        s += _T("CP[") + data->copyright + "]";

    s += "\n";

    ostream->Write(s.c_str(), s.Length());
}

void SGFWriter::traverse(Move *m)
{
    wxString s;
    if (m->parent != NULL)
        ostream->Write("\n(", 2);
    while (true)
    {
        s = m->MoveToSGF();
        ostream->Write(s.c_str(), s.Length());

        Move *tmp = m->son;
        if (tmp != NULL && tmp->brother != NULL)
        {
            do
            {
                traverse(tmp);
            } while ((tmp = tmp->brother) != NULL);
            break;
        }
        if ((m = m->son) != NULL)
            ostream->Write("\n", 1);
        else
            break;
    }
    ostream->Write(")", 1);
}
