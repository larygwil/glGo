/*
 * inputctrl.h
 *
 * $Id: inputctrl.h,v 1.1 2003/10/19 06:52:34 peter Exp $
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

#ifndef INPUTCTRL_H
#define INPUTCTRL_H

#ifdef __GNUG__
#pragma interface "inputctrl.h"
#endif


/**
 * A subclass of wxTextCtrl implementing a history buffer. The buffer, implemented as
 * wxStringArray, remembers each typed command. The user can scroll through the buffer
 * using the cursor up/down keys. Buffer size is limited to 20 entries.
 *
 * To embed the control into a frame created from XML resources, use something like this:
 *
 * @code
 * input = new InputCtrl(this, -1);
 * wxXmlResource::Get()->AttachUnknownControl(_T("input"), input);
 * @endcode
 *
 * @ingroup userinterface
 */
class InputCtrl : public wxTextCtrl
{
public:
    /** Constructor. Takes the same parameters as wxTextCtrl. */
    InputCtrl(wxWindow* parent, wxWindowID id, const wxString& value = "",
              const wxPoint& pos = wxDefaultPosition, const wxSize& size = wxDefaultSize,
              long style = 0, const wxValidator& validator = wxDefaultValidator,
              const wxString& name = wxTextCtrlNameStr);

    /** Destructor. Clears and frees the buffer. */
    virtual ~InputCtrl();

    /** Processes Return and adds the command to the buffer. */
    void OnEnter(wxCommandEvent& event);

    /** Processes chars and reacts to cursor up/down. */
    void OnChar(wxKeyEvent& event);

private:
    wxArrayString buffer;
    size_t counter;

DECLARE_EVENT_TABLE()
};

#endif
