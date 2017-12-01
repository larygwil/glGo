/*
 * html_utils.cpp
 *
 * $Id: html_utils.cpp,v 1.8 2003/10/07 05:22:54 peter Exp $
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
#include "wx/wx.h"
#endif

#include <wx/xrc/xmlres.h>
#include <wx/url.h>
#include <wx/txtstrm.h>
#include "html_utils.h"
#include "defines.h"

#ifndef __WXMSW__
#include <wx/mimetype.h>
#elif defined(__MINGW32__)
#include <windows.h>
#endif


/** URL to glGo webpage. Must ends with a "/" */
extern const wxString glGoURL = _T("http://ggo.sourceforge.net/");

void checkUpdate(wxWindow *parent)
{
    wxASSERT(parent != NULL);
    const wxString updateURL = glGoURL + _T("version.html");

    wxURL url(updateURL);
    wxInputStream *in_stream = url.GetInputStream();
    if (in_stream == NULL || url.GetError() != wxURL_NOERR)
    {
        wxMessageBox(wxString::Format(_("Failed to connect to %s"), updateURL.c_str()),
                     _("Connection problem"), wxOK | wxICON_EXCLAMATION, parent);
        if (in_stream != NULL)
            delete in_stream;
        return;
    }

    LOG_GLOBAL(wxString::Format(_T("Connected to %s"), updateURL.c_str()));

    if (in_stream->Eof())
    {
        wxMessageBox(_("No data available from server."), _("Connection problem"), wxOK | wxICON_EXCLAMATION);
        if (in_stream != NULL)
            delete in_stream;
        return;
    }

    wxTextInputStream input(*in_stream);
    wxString version = input.ReadLine();

    LOG_GLOBAL(wxString::Format(_T("Read: %s"), version.c_str()));

    if (version.empty())
    {
        wxMessageBox(_("No data available from server."), _("Connection problem"), wxOK | wxICON_EXCLAMATION);
        if (in_stream != NULL)
            delete in_stream;
        return;
    }

    if (!version.Cmp(VERSION))
    {
        wxMessageBox(wxString::Format(_("You are running the latest version %s\n"
                                        "There is currently no update available."), version.c_str()),
                     _("Checking for update"),
                     wxOK | wxICON_INFORMATION, parent);
    }
    else
    {
        wxDialog *dlg = wxXmlResource::Get()->LoadDialog(parent, _T("update_checker"));
        wxASSERT(dlg != NULL);
        if (dlg == NULL)
            return;
        if (dlg->ShowModal() == wxID_OK)
            ViewHTMLFile(glGoURL);
    }

    if (in_stream != NULL)
        delete in_stream;
}

void ViewHTMLFile(const wxString& url)
{
#ifdef __WXMSW__
    HKEY hKey;
    TCHAR szCmdName[1024];
    DWORD dwType, dw = sizeof(szCmdName);
    LONG lRes;
    lRes = RegOpenKey(HKEY_CLASSES_ROOT, _T("htmlfile\\shell\\open\\command"), &hKey);
    if(lRes == ERROR_SUCCESS && RegQueryValueEx(hKey,(LPTSTR)NULL, NULL,
                                                &dwType, (LPBYTE)szCmdName, &dw) == ERROR_SUCCESS)
    {
        strcat(szCmdName, (const char*) url);
        PROCESS_INFORMATION  piProcInfo;
        STARTUPINFO          siStartInfo;
        memset(&siStartInfo, 0, sizeof(STARTUPINFO));
        siStartInfo.cb = sizeof(STARTUPINFO);
        CreateProcess(NULL, szCmdName, NULL, NULL, FALSE, NULL, NULL,
                      NULL, &siStartInfo, &piProcInfo );
    }
    if(lRes == ERROR_SUCCESS)
        RegCloseKey(hKey);
#else
    wxFileType *ft = wxTheMimeTypesManager->GetFileTypeFromExtension(_T("html"));
    if ( !ft )
    {
        wxMessageBox(_("Impossible to determine the file type for extension html.\n"
                       "Please edit your MIME types."),
                     _("Problem"), wxOK | wxICON_EXCLAMATION);
        return ;
    }

    wxString cmd;
    bool ok = ft->GetOpenCommand(&cmd,
                                 wxFileType::MessageParameters(url, _T("")));
    delete ft;

    if (!ok)
    {
        // TODO: some kind of configuration dialog here.
        wxMessageBox(_("Could not determine the command for running the browser."),
                     _("Browsing problem"), wxOK|wxICON_EXCLAMATION);
        return ;
    }

    ok = (wxExecute(cmd, FALSE) != 0);
#endif
}
