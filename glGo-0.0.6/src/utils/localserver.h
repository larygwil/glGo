/*
 * localserver.h
 *
 * $Id: localserver.h,v 1.2 2003/11/22 12:00:02 peter Exp $
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

#ifndef LOCALSERVER_H
#define LOCALSERVER_H

#ifdef __GNUG__
#pragma interface "localserver.h"
#endif

#define DEFAULT_LOCALSERVER_PORT 9998

/**
 * SocketServer running on localhost:9998.
 * The server is listening for connections sending SGF filenames, used for
 * opening SGF files in a running glGo process to avoid opening a new instance
 * each time the user opens a SGF file from a webbrowser or filemanager.
 * On POSIX systems a Unix domain socket is used (much safer), on Windows
 * an Internet domain socket (insecure, but nothing better available on Windows).
 */
class LocalServer : public wxEvtHandler
{
public:
    /**
     * Constructor
     * @param port Port to use. Default is 9998. Only used for Windows.
     */
    LocalServer(int port = DEFAULT_LOCALSERVER_PORT);

    /** Destructor */
    ~LocalServer();

private:
    void OnSocketEvent(wxSocketEvent &event);

#ifndef __WXMSW__
    static const wxString tmpfilename;
#endif
#ifdef __VISUALC__
    #define buf_size 256
#else
    static const int buf_size = 256;
#endif
    char buf[buf_size];
    wxSocketServer *server;
    wxSocketBase *socket;

DECLARE_EVENT_TABLE()
};

#endif
