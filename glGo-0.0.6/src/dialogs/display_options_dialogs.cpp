/*
 * display_options_dialogs.cpp
 *
 * $Id: display_options_dialogs.cpp,v 1.6 2003/11/04 15:03:10 peter Exp $
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
#pragma implementation "display_options_dialogs.h"
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
#include <wx/valgen.h>
#include <wx/colordlg.h>
#include <wx/config.h>
#include "display_options_dialogs.h"
#include "glBoard.h"
#include "glGo.h"
#include "htmlhelp_context.h"


// ------------------------------------------------------------------------
//                          Class OGLOptionsDialog
// ------------------------------------------------------------------------


BEGIN_EVENT_TABLE(OGLOptionsDialog, wxDialog)
    EVT_BUTTON(wxID_HELP, OGLOptionsDialog::OnHelp)
    EVT_BUTTON(XRCID(_T("background_color")), OGLOptionsDialog::OnSetBackgroundColor)
END_EVENT_TABLE()


OGLOptionsDialog::OGLOptionsDialog(wxWindow *parent, GLBoard *board)
{
    SetExtraStyle(wxWS_EX_VALIDATE_RECURSIVELY);

    wxASSERT(board != NULL);
    glBoard = board;

    // Read data from config file
    loadOGLConfig(config);
    // wxConfig::Get()->Write(_T("Board/OpenGL/BackImage"), images);
    back_color = readColorFromConfig(_T("Board/OpenGL/BackColor"));

    // Load dialog XML resource
    wxXmlResource::Get()->LoadDialog(this, parent, _T("ogl_options_dialog"));

    // Set validators and transfer data to dialog controls
    XRCCTRL(*this, _T("reflections"), wxCheckBox)->SetValidator(wxGenericValidator(&config.reflections));
    XRCCTRL(*this, _T("shadows"), wxCheckBox)->SetValidator(wxGenericValidator(&config.shadows));
    XRCCTRL(*this, _T("render_to_texture"), wxCheckBox)->SetValidator(wxGenericValidator(&config.render_to_texture));
    XRCCTRL(*this, _T("blur"), wxCheckBox)->SetValidator(wxGenericValidator(&config.blur));
    XRCCTRL(*this, _T("fast_rendering"), wxCheckBox)->SetValidator(wxGenericValidator(&config.fast_rendering));
    XRCCTRL(*this, _T("blending"), wxCheckBox)->SetValidator(wxGenericValidator(&config.blending));
    XRCCTRL(*this, _T("antialias_lines"), wxCheckBox)->SetValidator(wxGenericValidator(&config.antialias_lines));
    XRCCTRL(*this, _T("antialias_stones"), wxCheckBox)->SetValidator(wxGenericValidator(&config.antialias_stones));
    XRCCTRL(*this, _T("antialias_scene"), wxCheckBox)->SetValidator(wxGenericValidator(&config.antialias_scene));
    XRCCTRL(*this, _T("antialias_scene_quality"), wxRadioBox)->SetValidator(wxGenericValidator(&config.antialias_scene_quality));
    XRCCTRL(*this, _T("textures"), wxCheckBox)->SetValidator(wxGenericValidator(&config.textures));
    XRCCTRL(*this, _T("textures_quality"), wxRadioBox)->SetValidator(wxGenericValidator(&config.textures_quality));
    XRCCTRL(*this, _T("multitextures"), wxCheckBox)->SetValidator(wxGenericValidator(&config.multitextures));
    XRCCTRL(*this, _T("stone_quality"), wxRadioBox)->SetValidator(wxGenericValidator(&config.stone_quality));
    XRCCTRL(*this, _T("scissor"), wxCheckBox)->SetValidator(wxGenericValidator(&config.use_scissor));
    // XRCCTRL(*this, _T("background_image"), wxCheckBox)->SetValidator(wxGenericValidator(&back_image));
}

bool OGLOptionsDialog::TransferDataFromWindow()
{
    if (!wxWindow::TransferDataFromWindow())
        return false;

    // Update board. If the board rejects a setting, it will change it in config.
    glBoard->setOGLViewParameter(config);
    glBoard->updateBackgroundColor(back_color);

    // Save data in config
    saveOGLConfig(config);
    // wxConfig::Get()->Write(_T("Board/OpenGL/BackImage"), images);
    wxConfig::Get()->Write(_T("Board/OpenGL/BackColor"), wxString::Format("%u#%u#%u",
                                                                          back_color.Red(),
                                                                          back_color.Green(),
                                                                          back_color.Blue()));

    // Notify user about possible bad settings

    // Scissor and Render to texture is a bad idea
    if (config.use_scissor && config.render_to_texture)
        wxMessageBox(_("Using scissor test with render to texture enabled will produce display artifacts.\n"
                       "Please consider using only one of these two settings at the same time.\n"
                       "See OpenGL chapter in the manual for some more details on this."),
                     _("Information"), wxOK | wxICON_INFORMATION, this);

    // Antialias without blending is an even worse idea
    if (!config.blending && (config.antialias_lines || config.antialias_stones))
        wxMessageBox(_("Using antialiasing without blending enabled will not work well. Please consider\n"
                       "enabling blending if you want proper antialias effects.\n"
                       "See OpenGL chapter in the manual for some more details on this."),
                     _("Information"), wxOK | wxICON_INFORMATION, this);

    return true;
}

void OGLOptionsDialog::OnHelp(wxCommandEvent& WXUNUSED(event))
{
#ifdef USE_MSHTMLHELP
    wxLogDebug(_T("MSHTMLHELP"));
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_OPENGL);
#else
    wxLogDebug(_T("NO MSHTMLHELP"));
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_OPENGL);
#endif
}

void OGLOptionsDialog::OnSetBackgroundColor(wxCommandEvent& WXUNUSED(event))
{
    wxColour color = wxGetColourFromUser(this, back_color);

    // User hit Ok or Cancel?
    if (color.Ok())
        back_color = color;
}


// ------------------------------------------------------------------------
//                          Class SDLOptionsDialog
// ------------------------------------------------------------------------

BEGIN_EVENT_TABLE(SDLOptionsDialog, wxDialog)
    EVT_BUTTON(wxID_HELP, SDLOptionsDialog::OnHelp)
    EVT_BUTTON(XRCID(_T("background_color")), SDLOptionsDialog::OnSetBackgroundColor)
END_EVENT_TABLE()


SDLOptionsDialog::SDLOptionsDialog(wxWindow *parent, Board *board)
    : parent(parent), board(board)
{
    SetExtraStyle(wxWS_EX_VALIDATE_RECURSIVELY);
}

void SDLOptionsDialog::initDialog()
{
    // Load dialog XML resource
    wxXmlResource::Get()->LoadDialog(this, parent, _T("sdl_options_dialog"));

    // Read config
    wxConfig::Get()->Read(_T("Board/SDL/BackImage"), &back_image, true);
    wxConfig::Get()->Read(_T("Board/SDL/ScaledFont"), &scaled_font, true);
    back_color = readColorFromConfig(_T("Board/SDL/BackColor"));

    // Set validators and transfer data to dialog controls
    XRCCTRL(*this, _T("background_image"), wxCheckBox)->SetValidator(wxGenericValidator(&back_image));
    XRCCTRL(*this, _T("scaled_font"), wxCheckBox)->SetValidator(wxGenericValidator(&scaled_font));
}

bool SDLOptionsDialog::TransferDataFromWindow()
{
    if (!wxWindow::TransferDataFromWindow())
        return false;

    // Update board
    board->updateBackgroundColor(back_color);
    board->setViewParameter(VIEW_USE_SCALED_FONT, scaled_font);
    board->setViewParameter(VIEW_USE_BACKGROUND_IMAGE, back_image);

    // Save data in config
    wxConfig::Get()->Write(_T("Board/SDL/BackImage"), back_image);
    wxConfig::Get()->Write(_T("Board/SDL/ScaledFont"), scaled_font);
    wxConfig::Get()->Write(_T("Board/SDL/BackColor"), wxString::Format("%u#%u#%u",
                                                                       back_color.Red(),
                                                                       back_color.Green(),
                                                                       back_color.Blue()));
    wxConfig::Get()->Flush();

    return true;
}

void SDLOptionsDialog::OnSetBackgroundColor(wxCommandEvent& WXUNUSED(event))
{
    wxColour color = wxGetColourFromUser(this, back_color);

    // User hit Ok or Cancel?
    if (color.Ok())
        back_color = color;
}

void SDLOptionsDialog::OnHelp(wxCommandEvent& WXUNUSED(event))
{
#ifdef USE_MSHTMLHELP
    wxLogDebug(_T("MSHTMLHELP"));
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_SDL);
#else
    wxLogDebug(_T("NO MSHTMLHELP"));
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_SDL);
#endif
}
