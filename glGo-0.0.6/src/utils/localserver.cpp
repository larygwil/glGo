/*
 * localserver.cpp
 *
 * $Id: localserver.cpp,v 1.3 2003/11/22 16:36:01 peter Exp $
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
#pragma implementation "localserver.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/app.h>
#include <wx/intl.h>
#include <wx/log.h>
#endif

#include <wx/socket.h>
#include "glGo.h"
#include "localserver.h"

enum
{
    SOCKET_ID = 1020
};

#ifndef __WXMSW__
const wxString LocalServer::tmpfilename = "/tmp/glGo_socket";
#endif


BEGIN_EVENT_TABLE(LocalServer, wxEvtHandler)
    EVT_SOCKET(SOCKET_ID, LocalServer::OnSocketEvent)
END_EVENT_TABLE()

LocalServer::LocalServer(int port)
{
#ifdef __WXMSW__
    wxIPV4address adr;
    adr.Hostname("localhost");
    adr.Service(port);
#else
    if (wxFileExists(tmpfilename) && !wxRemoveFile(tmpfilename))
        wxLogDebug("Failed to remove tmpfile.");
    wxUNIXaddress adr;
    adr.Filename(tmpfilename);
#endif

    server =  new wxSocketServer(adr, wxSOCKET_BLOCK);
    server->SetEventHandler(*this, SOCKET_ID);
    server->SetNotify(wxSOCKET_CONNECTION_FLAG | wxSOCKET_LOST_FLAG);
    server->Notify(true);
    if (!server->Ok())
    {
        LOG_GLOBAL("Failed to create SocketServer");
        server = NULL;
    }

    socket = NULL;
}

LocalServer::~LocalServer()
{
#ifndef __WXMSW__
    if (!wxRemoveFile(tmpfilename))
        wxLogDebug("Failed to remove tmpfile.");
#endif
}

void LocalServer::OnSocketEvent(wxSocketEvent &event)
{
    switch(event.GetSocketEvent())
    {
    case wxSOCKET_INPUT:
        if (socket == NULL)
            break;

        socket->Read(buf, buf_size);
        if (!socket->Error())
        {
            wxString fname = wxString(buf, socket->LastCount());
            LOG_GLOBAL(wxString::Format("Message from socket: %s", fname.c_str()));

            // Open board and load game if file exists
            if (wxFileExists(fname))
                wxGetApp().newMainFrame(GAME_TYPE_PLAY, fname);
        }
        else
            wxLogDebug("Error reading from socket");
        break;
    case wxSOCKET_CONNECTION:
        if (server == NULL)
            break;

        socket = server->Accept(false);
        if (socket == NULL)
        {
            wxLogDebug("Error accepting socket");
            break;
        }

        socket->SetEventHandler(*this, SOCKET_ID);
        socket->SetNotify(wxSOCKET_INPUT_FLAG);
        socket->Notify(true);
        break;

    case wxSOCKET_LOST:
        wxLogDebug("Connection closed by client");
        socket = NULL;
        break;

    default:
        break;
    }
}
