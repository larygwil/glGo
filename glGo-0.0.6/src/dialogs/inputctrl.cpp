/*
 * inputctrl.cpp
 *
 * $Id: inputctrl.cpp,v 1.1 2003/10/19 06:52:34 peter Exp $
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
#pragma implementation "inputctrl.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/textctrl.h>
#endif

#include "inputctrl.h"


/** Size of history buffer: 20 lines */
#define MAX_BUFFER_SIZE 20


BEGIN_EVENT_TABLE(InputCtrl, wxTextCtrl)
    EVT_CHAR(InputCtrl::OnChar)
    EVT_TEXT_ENTER(-1, InputCtrl::OnEnter)
END_EVENT_TABLE()


InputCtrl::InputCtrl(wxWindow* parent, wxWindowID id, const wxString& value,
                     const wxPoint& pos, const wxSize& size,
                     long style, const wxValidator& validator, const wxString& name)
    : wxTextCtrl(parent, id, value, pos, size, style | wxTE_PROCESS_ENTER, validator, name), counter(0)
{
}

InputCtrl::~InputCtrl()
{
    buffer.Clear();
}

void InputCtrl::OnChar(wxKeyEvent& event)
{
    switch(event.GetKeyCode())
    {
    case WXK_UP:
        if (buffer.GetCount() == 0)
            break;
        if (counter > 0)
            counter--;
        if (counter >= buffer.GetCount())
            counter = buffer.GetCount() - 1;
        SetValue(buffer.Item(counter));
        break;
    case WXK_DOWN:
        if (buffer.GetCount() == 0)
            break;
        counter++;
        if (counter >= buffer.GetCount())
        {
            counter = buffer.GetCount();
            Clear();
            break;
        }
        SetValue(buffer.Item(counter));
        break;
    default:
        break;
    }

    event.Skip();
}

void InputCtrl::OnEnter(wxCommandEvent& event)
{
    wxString txt = GetValue();
    if (buffer.GetCount() == 0 ||
        txt.Cmp(buffer.Item(buffer.GetCount() - 1)))  // Ignore if last command identical
    {
        // Limit buffer to 20 entries
        if (buffer.GetCount() >= MAX_BUFFER_SIZE)
            buffer.RemoveAt(0);
        buffer.Add(txt);
    }
    counter = buffer.GetCount();

    event.Skip();
}
