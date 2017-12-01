/*
 * starterframe.cpp
 *
 * $Id: starterframe.cpp,v 1.17 2003/10/23 23:54:07 peter Exp $
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
#pragma implementation "starterframe.h"
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
#include <wx/config.h>
#include "starterframe.h"
#include "glGo.h"
#include "preferences_dialog.h"
#include "mainframe.h"

// Icons
#ifndef __WXMSW__
#include "images/100purple.xpm"
#include "images/100navy.xpm"
#include "images/100emerald.xpm"
#include "images/100red.xpm"
#include "images/100blue.xpm"
#include "images/100yellow.xpm"
#include "images/ggo.xpm"
#endif
#include "images/ggo_big.xpm"


// -----------------------------------------------------------------------
//                          Class StarterFrame
// -----------------------------------------------------------------------

BEGIN_EVENT_TABLE(StarterFrame, wxFrame)
    EVT_CLOSE(StarterFrame::OnClose)
    EVT_BUTTON(XRCID(_T("igs")), StarterFrame::OnIGS)
    EVT_BUTTON(XRCID(_T("board")), StarterFrame::OnNewBoard)
    EVT_BUTTON(XRCID(_T("load")), StarterFrame::OnLoadGame)
    EVT_BUTTON(XRCID(_T("gtp")), StarterFrame::OnGTP)
    EVT_BUTTON(XRCID(_T("preferences")), StarterFrame::OnPreferences)
    EVT_BUTTON(XRCID(_T("exit")), StarterFrame::OnExit)
END_EVENT_TABLE()


StarterFrame::StarterFrame()
{
    // Load frame from resource file
    wxXmlResource::Get()->LoadFrame(this, NULL, _T("starter_frame"));

    // Load images
    XRCCTRL(*this, _T("ggo_image"), wxStaticBitmap)->SetBitmap(wxBitmap(ggo_big_xpm));
    XRCCTRL(*this, _T("image1"), wxStaticBitmap)->SetBitmap(wxBITMAP(purple100));
    XRCCTRL(*this, _T("image2"), wxStaticBitmap)->SetBitmap(wxBITMAP(navy100));
    XRCCTRL(*this, _T("image3"), wxStaticBitmap)->SetBitmap(wxBITMAP(emerald100));
    XRCCTRL(*this, _T("image4"), wxStaticBitmap)->SetBitmap(wxBITMAP(red100));
    XRCCTRL(*this, _T("image5"), wxStaticBitmap)->SetBitmap(wxBITMAP(blue100));
    XRCCTRL(*this, _T("image6"), wxStaticBitmap)->SetBitmap(wxBITMAP(yellow100));
    XRCCTRL(*this, _T("app_version"), wxStaticText)->SetLabel(VERSION);

#ifdef __WXMSW__
    SetIcon(wxICON(aaa_ggo));
#else
    SetIcon(wxICON(ggo));
#endif

#ifdef __WXMSW__
    // Create taskbar icon. Only available on Windows
    taskbarIcon = new StarterIcon(this);
#endif
}

StarterFrame::~StarterFrame()
{
#ifdef __WXMSW__
    delete taskbarIcon;
#endif
}

void StarterFrame::autohideFrame()
{
    bool autohide;
    wxConfig::Get()->Read(_T("Misc/Autohide"), &autohide, false);
    if (autohide)
    {
#ifdef __WXMSW__
        bool use_tray;
        wxConfig::Get()->Read(_T("Misc/MinTray"), &use_tray, true);
        if (use_tray)
        {
            Hide();
            taskbarIcon->SetIcon(wxICON(mondrian));
        }
        else
            Iconize(true);
#else
        Iconize(true);
#endif
    }
}

void StarterFrame::OnIGS(wxCommandEvent& WXUNUSED(event))
{
#ifndef NO_IGS
    wxGetApp().openIGS();
    autohideFrame();
#else
    wxMessageBox(_("This build does not support the IGS client."),
                 _("Error"), wxOK | wxICON_ERROR);
#endif
}

void StarterFrame::OnNewBoard(wxCommandEvent& WXUNUSED(event))
{
    wxGetApp().newMainFrame();
    autohideFrame();
}

void StarterFrame::OnLoadGame(wxCommandEvent& WXUNUSED(event))
{
    wxString filename = MainFrame::selectLoadSGFFilename(this);
    if (filename.empty())
        return;

    wxGetApp().newMainFrame(GAME_TYPE_PLAY, filename);
    autohideFrame();
}

void StarterFrame::OnGTP(wxCommandEvent& WXUNUSED(event))
{
#ifndef NO_GTP
    if (wxGetApp().newMainFrame(GAME_TYPE_GTP) != NULL)
        autohideFrame();
#else
    wxMessageBox(NO_GTP_ERROR_MESSAGE, _("Error"), wxOK | wxICON_ERROR);
#endif
}

void StarterFrame::OnPreferences(wxCommandEvent& WXUNUSED(event))
{
    PreferencesDialog dlg(this);
    dlg.ShowModal();
}

void StarterFrame::OnExit(wxCommandEvent& WXUNUSED(event))
{
    if (wxGetApp().AttemptShutdown())
        Close(true);
}

void StarterFrame::OnClose(wxCloseEvent &event)
{
    if (!event.CanVeto())
    {
        Destroy();
        return;
    }

#ifndef __WXMSW__
    // On Linux this behaves the same as the Exit button
    if (wxGetApp().AttemptShutdown())
        Close(true);
#else
    // On Windows, veto and minimize to system tray instead if this option is enabled
    bool use_tray;
    wxConfig::Get()->Read(_T("Misc/MinTray"), &use_tray, true);
    if (use_tray)
    {
        event.Veto();
        Show(false);
        taskbarIcon->SetIcon(wxICON(mondrian));
    }
    // Exit app when not going to tray
    else if (wxGetApp().AttemptShutdown())
        Close(true);
    // This *should* never happen, but just in case we delete the frame
    else
        Destroy();
#endif
}


// -----------------------------------------------------------------------
//                          Class StarterIcon
// -----------------------------------------------------------------------

#ifdef __WXMSW__

BEGIN_EVENT_TABLE(StarterIcon, wxTaskBarIcon)
    EVT_TASKBAR_LEFT_DCLICK(StarterIcon::OnTaskbarLeftDblClick)
    EVT_TASKBAR_RIGHT_DOWN(StarterIcon::OnTaskBarRightClick)
    EVT_MENU(XRCID(_T("popup_restore")), StarterIcon::OnRestore)
    EVT_MENU(XRCID(_T("popup_board")), StarterIcon::OnBoard)
    EVT_MENU(XRCID(_T("board_3d")), StarterIcon::OnBoardType3D)
    EVT_MENU(XRCID(_T("board_2d")), StarterIcon::OnBoardType2D)
    EVT_MENU(XRCID(_T("popup_exit")), StarterIcon::OnExit)
END_EVENT_TABLE()

StarterIcon::StarterIcon(StarterFrame *frame)
    : frame(frame)
{
    // Load popup menu from resource file
    popup = wxXmlResource::Get()->LoadMenu(_T("starter_icon_popup"));
    // Apply bold text to Restore menuitem
    wxMenuItem *it = popup->FindItem(XRCID(_T("popup_restore")));
    if (it != NULL)
    {
        wxFont menuFont = wxSystemSettings::GetFont(wxSYS_DEFAULT_GUI_FONT);
        menuFont.SetWeight(wxBOLD);
        it->SetFont(menuFont);
        // Need to reset the text else the font change won't show effect
        it->SetText(_("Restore"));
    }
    // Read board type from config and preselect radiomenuitem
    int type;
    if (wxConfig::Get()->Read(_T("Board/Type"), &type))
    {
        wxMenuItem *it;
        if (type == DISPLAY_TYPE_OPENGL)
            it = popup->FindItem(XRCID(_T("board_3d")));
        else
            it = popup->FindItem(XRCID(_T("board_2d")));
        if (it != NULL)
            it->Check(true);
    }
}

StarterIcon::~StarterIcon()
{
    delete popup;
}

void StarterIcon::restoreFrame()
{
    frame->Show(true);
    frame->Raise();
    RemoveIcon();
}

void StarterIcon::OnTaskBarRightClick(wxTaskBarIconEvent& event)
{
    PopupMenu(popup);
}

void StarterIcon::OnBoard(wxCommandEvent& WXUNUSED(event))
{
    wxGetApp().newMainFrame();
}

void StarterIcon::OnBoardType3D(wxCommandEvent& event)
{
    wxMenuItem *it = popup->FindItem(XRCID(_T("board_3d")));
    if (it != NULL)
        it->Check(true);
    wxConfig::Get()->Write(_T("Board/Type"), DISPLAY_TYPE_OPENGL);
    wxLogDebug("Set to 3D board");
}

void StarterIcon::OnBoardType2D(wxCommandEvent& event)
{
    wxMenuItem *it = popup->FindItem(XRCID(_T("board_2d")));
    if (it != NULL)
        it->Check(true);
    wxConfig::Get()->Write(_T("Board/Type"), DISPLAY_TYPE_SDL);
    wxLogDebug("Set to 2D board");
}

void StarterIcon::OnExit(wxCommandEvent& WXUNUSED(event))
{
    // This is wired. If the frame is minimized, it won't destroy
    frame->Show(true);
    wxGetApp().AttemptShutdown();
}

#endif  // __WXMSW__
