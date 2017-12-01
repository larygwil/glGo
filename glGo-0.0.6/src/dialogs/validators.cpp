/*
 * validators.cpp
 *
 * $Id: validators.cpp,v 1.4 2003/10/02 14:16:51 peter Exp $
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
#pragma implementation "validators.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/spinctrl.h>
#include "defines.h"
#include "gamedata.h"
#include "validators.h"

// No bell on Linux, PC speaker is too annoying
#ifdef __WXMSW__
#define USE_VAL_BELL
#endif


//------------------------------------------------------------------------
//                       Class BoardSizeValidator
//------------------------------------------------------------------------

BEGIN_EVENT_TABLE(BoardSizeValidator, wxValidator)
    EVT_CHAR(BoardSizeValidator::OnChar)
END_EVENT_TABLE()

bool BoardSizeValidator::TransferToWindow()
{
    if (m_validatorWindow == NULL)
        return false;

    if (m_validatorWindow->IsKindOf(CLASSINFO(wxSpinCtrl)) && board_size != NULL)
    {
        ((wxSpinCtrl*)m_validatorWindow)->SetValue(*board_size);
        return true;
    }
    return false;
}

bool BoardSizeValidator::TransferFromWindow()
{
    if (m_validatorWindow == NULL)
        return false;

    if (m_validatorWindow->IsKindOf(CLASSINFO(wxSpinCtrl)) && board_size != NULL)
    {
        *board_size = ((wxSpinCtrl*)m_validatorWindow)->GetValue();
        return true;
    }
    return false;
}

bool BoardSizeValidator::Validate(wxWindow* parent)
{
    if (m_validatorWindow == NULL)
        return false;

    int val=0;
    if (m_validatorWindow->IsKindOf(CLASSINFO(wxSpinCtrl)))
        val = ((wxSpinCtrl*)m_validatorWindow)->GetValue();

    if (val < BOARD_SIZE_MIN || val > BOARD_SIZE_MAX)
    {
        wxLogError(wxString::Format(_("Invalid board size.\n"
                                      "Please enter a board size from %d to %d."),
                                    BOARD_SIZE_MIN, BOARD_SIZE_MAX));
        return false;
    }
    return true;
}

void BoardSizeValidator::OnChar(wxKeyEvent& event)
{
    if (m_validatorWindow == NULL)
        return;

    int keyCode = event.KeyCode();
    if (!(keyCode < WXK_SPACE || keyCode == WXK_DELETE || keyCode > WXK_START) &&
        !wxIsdigit(keyCode))
    {
#ifdef USE_VAL_BELL
        if (!wxValidator::IsSilent())
            wxBell();
#endif
        return;
    }
    event.Skip();
}


//------------------------------------------------------------------------
//                       Class HandicapValidator
//------------------------------------------------------------------------

BEGIN_EVENT_TABLE(HandicapValidator, wxValidator)
    EVT_CHAR(HandicapValidator::OnChar)
END_EVENT_TABLE()

bool HandicapValidator::TransferToWindow()
{
    if (m_validatorWindow == NULL)
        return false;

    if (m_validatorWindow->IsKindOf(CLASSINFO(wxSpinCtrl)) && handicap != NULL)
    {
        ((wxSpinCtrl*) m_validatorWindow)->SetValue(*handicap);
        return true;
    }
    else if (m_validatorWindow->IsKindOf(CLASSINFO(wxTextCtrl)) && handicap != NULL)
    {
        ((wxTextCtrl*) m_validatorWindow)->SetValue(wxString::Format(wxT("%d"), *handicap));
        return true;
    }
    return false;
}

bool HandicapValidator::TransferFromWindow()
{
    if (m_validatorWindow == NULL)
        return false;

    if (m_validatorWindow->IsKindOf(CLASSINFO(wxSpinCtrl)) && handicap != NULL)
    {
        *handicap = ((wxSpinCtrl*) m_validatorWindow)->GetValue();
        return true;
    }
    else if (m_validatorWindow->IsKindOf(CLASSINFO(wxTextCtrl)) && handicap != NULL)
    {
      *handicap = wxAtoi(((wxTextCtrl*) m_validatorWindow)->GetValue());
      return true;
    }
    return false;
}

bool HandicapValidator::Validate(wxWindow* parent)
{
    if (m_validatorWindow == NULL)
        return false;

    int val=0;

    if (m_validatorWindow->IsKindOf(CLASSINFO(wxSpinCtrl)))
        val = ((wxSpinCtrl*) m_validatorWindow)->GetValue();
    else if (m_validatorWindow->IsKindOf(CLASSINFO(wxTextCtrl)))
        val = wxAtoi(((wxTextCtrl*) m_validatorWindow)->GetValue());

    if (val && (val < HANDICAP_MIN || val > HANDICAP_MAX))
    {
        wxLogError(INVALID_HANDICAP_VALUE, val);
        return false;
    }
    return true;
}

void HandicapValidator::OnChar(wxKeyEvent& event)
{
    if (m_validatorWindow == NULL)
        return;

    int keyCode = event.KeyCode();
    if (!(keyCode < WXK_SPACE || keyCode == WXK_DELETE || keyCode > WXK_START) &&
        !wxIsdigit(keyCode))
    {
#ifdef USE_VAL_BELL
        if (!wxValidator::IsSilent())
            wxBell();
#endif
        return;
    }
    event.Skip();
}


//------------------------------------------------------------------------
//                         Class KomiValidator
//------------------------------------------------------------------------

BEGIN_EVENT_TABLE(KomiValidator, wxValidator)
    EVT_CHAR(KomiValidator::OnChar)
END_EVENT_TABLE()

bool KomiValidator::TransferToWindow()
{
    if (m_validatorWindow == NULL)
        return false;

    if (m_validatorWindow->IsKindOf(CLASSINFO(wxSpinCtrl)) && komi != NULL)
    {
        ((wxSpinCtrl*) m_validatorWindow)->SetValue(static_cast<int>(*komi));
        return true;
    }
    else if (m_validatorWindow->IsKindOf(CLASSINFO(wxTextCtrl)) && komi != NULL)
    {
        ((wxTextCtrl*) m_validatorWindow)->SetValue(wxString::Format("%.1f", *komi));
        return true;
    }
    return false;
}

bool KomiValidator::TransferFromWindow()
{
    if (m_validatorWindow == NULL)
        return false;

    if (m_validatorWindow->IsKindOf(CLASSINFO(wxSpinCtrl)) && komi != NULL)
    {
        *komi = ((wxSpinCtrl*) m_validatorWindow)->GetValue();
        return true;
    }
    else if (m_validatorWindow->IsKindOf(CLASSINFO(wxTextCtrl)) && komi != NULL)
    {
        wxString str = ((wxTextCtrl*) m_validatorWindow)->GetValue();
        if (str.empty())
            *komi = DEFAULT_KOMI;
        else
        {
            double tmp_dbl;
            if (!str.ToDouble(&tmp_dbl))
            {
                wxLogError(_("Invalid komi value: %s"), str.c_str());
                *komi = DEFAULT_KOMI;
                return false;
            }
            *komi = static_cast<float>(tmp_dbl);
        }
        return true;
    }
    return false;
}

void KomiValidator::OnChar(wxKeyEvent& event)
{
    if (m_validatorWindow == NULL)
        return;

    int keyCode = event.KeyCode();
    if (!(keyCode < WXK_SPACE || keyCode == WXK_DELETE || keyCode > WXK_START) &&
        !wxIsdigit(keyCode) && keyCode != '.' && keyCode != ',' && keyCode != '-')
    {
#ifdef USE_VAL_BELL
        if (!wxValidator::IsSilent())
            wxBell();
#endif
        return;
    }
    event.Skip();
}
