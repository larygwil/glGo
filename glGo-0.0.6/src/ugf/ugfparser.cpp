/*
 * ugfparser.cpp
 *
 * $Id: ugfparser.cpp,v 1.2 2003/11/24 01:59:46 peter Exp $
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

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#include "wx/intl.h"
#include "wx/app.h"
#endif

#ifndef STANDALONE
#include "boardhandler.h"
#include "gamedata.h"
#include "glGo.h"
#endif
#include "ugfparser.h"
#include "ugf.h"

#ifndef STANDALONE
static BoardHandler *boardhandler;
static GameData *data;
#endif

void doMove_cb(int x, int y, int col, int num, int time)
{
#ifndef STANDALONE
    if (boardhandler != NULL)
        boardhandler->playMoveSGF(x, y,
                                  col == 1 ? STONE_WHITE : col == 2 ? STONE_BLACK : STONE_UNDEFINED);
#else
    wxLogDebug("doMove_cb: %d/%d %d %d %d", x, y, col, num, time);
#endif
}

void doComment_cb(const char *comment)
{
#ifndef STANDALONE
    if (boardhandler != NULL)
        boardhandler->setComment(comment);
#else
    wxLogDebug("doComment_cb: %s", comment);
#endif
}

void doMark_cb(int x, int y, const char *text)
{
#ifndef STANDALONE
    if (boardhandler != NULL)
        boardhandler->addMark(x, y, MARK_TEXT, text);
#else
    wxLogDebug("doMark_cb: %d/%d %s", x, y, text);
#endif
}

void doHeader_cb(const char *key, const char *value)
{
#ifndef STANDALONE
    wxString wxkey = wxString(key);

    if (!wxkey.Cmp("Size"))
        data->board_size = wxAtoi(value);
    else if (!wxkey.Cmp("Hdcp"))
        data->handicap = wxAtoi(value);
    else if (!wxkey.Cmp("Komi"))
        data->komi = wxAtoi(value);  // TODO: BAD!
    else if (!wxkey.Cmp("PlayerW"))
        data->whiteName = value;
    else if (!wxkey.Cmp("PlayerB"))
        data->blackName = value;
    else if (!wxkey.Cmp("WhiteRank"))
        data->whiteRank = value;
    else if (!wxkey.Cmp("BlackRank"))
        data->blackRank = value;
    else if (!wxkey.Cmp("Title"))
        data->gameName = value;
    else if (!wxkey.Cmp("Place"))
        data->place = value;
    else if (!wxkey.Cmp("Copyright"))
        data->copyright = value;
    else if (!wxkey.Cmp("Date"))
        data->date = value;
    else if (!wxkey.Cmp("Winner"))
        data->result = value;
#else
    wxLogDebug("doHeader_cb: %s - %s", key, value);
#endif
}

#ifndef STANDALONE
bool loadUGF(const wxString &filename, GameData *gd, BoardHandler *bh)
{
    if (bh == NULL || gd == NULL)
        return -1;
    boardhandler = bh;
    data = gd;

    // Init callbacks for Python -> C
    ugf_set_callbacks(doMove_cb, doComment_cb, doMark_cb, doHeader_cb);

    if (UGF_Init(wxString::Format("%spythonlib.zip", wxGetApp().GetSharedPath().c_str()).c_str()) < 0)
    {
        wxPrintf("Failed to load UGF module.\n");
        return -1;
    }

    return UGF_Parse(filename.c_str()) == 0;
}
#endif
