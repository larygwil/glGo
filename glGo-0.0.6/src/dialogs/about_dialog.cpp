/*
 * about_dialog.cpp
 *
 * $Id: about_dialog.cpp,v 1.8 2003/10/06 20:10:49 peter Exp $
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
#pragma implementation "about_dialog.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/xrc/xmlres.h>
#include <wx/notebook.h>
#include "about_dialog.h"
#include "html_utils.h"
#include "images/ggo.xpm"


BEGIN_EVENT_TABLE(AboutDialog, wxDialog)
    EVT_BUTTON(XRCID(_T("check_update")), AboutDialog::OnUpdate)
    EVT_BUTTON(XRCID(_T("open_webpage")), AboutDialog::OnWebpage)
END_EVENT_TABLE()


AboutDialog::AboutDialog(wxWindow *parent,
                         const wxString &about,
                         const wxString &ogl,
                         const wxString &oal)
{
    // Load dialog XML resource
    wxXmlResource::Get()->LoadDialog(this, parent, _T("about_dialog"));

    // Init the three text fields
    XRCCTRL(*this, _T("about_glgo_text"), wxStaticText)->SetLabel(about);
    if (!ogl.empty())
        XRCCTRL(*this, _T("about_opengl_text"), wxTextCtrl)->SetValue(ogl);
    else
        XRCCTRL(*this, _T("notebook"), wxNotebook)->RemovePage(1);
    XRCCTRL(*this, _T("about_openal_text"), wxTextCtrl)->SetValue(oal);

    // Load gGo icon
    XRCCTRL(*this, _T("ggo_image"), wxStaticBitmap)->SetBitmap(wxBitmap(ggo_xpm));
}

void AboutDialog::OnUpdate(wxCommandEvent& WXUNUSED(event))
{
    checkUpdate(this);
}

void AboutDialog::OnWebpage(wxCommandEvent& WXUNUSED(event))
{
    ViewHTMLFile(glGoURL);
}
