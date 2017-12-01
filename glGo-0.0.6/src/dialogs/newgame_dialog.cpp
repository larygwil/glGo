/*
 * newgame_dialog.cpp
 *
 * $Id: newgame_dialog.cpp,v 1.3 2003/10/02 14:16:51 peter Exp $
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
#pragma implementation "newgame_dialog.h"
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
#include <wx/spinctrl.h>
#include <wx/valgen.h>
#include "newgame_dialog.h"
#include "validators.h"


NewGameDialog::NewGameDialog(wxWindow *parent, GameData *game_data)
{
    wxASSERT(game_data != NULL);
    data = game_data;

    SetExtraStyle(wxWS_EX_VALIDATE_RECURSIVELY);

    // Load dialog XML resource
    wxXmlResource::Get()->LoadDialog(this, parent, _T("newgame_dialog"));

    // Set validators and transfer data to dialog controls
    XRCCTRL(*this, _T("white"), wxTextCtrl)->SetValidator(wxGenericValidator(&data->whiteName));
    XRCCTRL(*this, _T("black"), wxTextCtrl)->SetValidator(wxGenericValidator(&data->blackName));
    XRCCTRL(*this, _T("size"), wxSpinCtrl)->SetValidator(BoardSizeValidator(&data->board_size));
    XRCCTRL(*this, _T("handicap"), wxSpinCtrl)->SetValidator(HandicapValidator(&data->handicap));
    XRCCTRL(*this, _T("komi"), wxTextCtrl)->SetValidator(KomiValidator(&data->komi));
}
