/*
 * account_dialog.h
 *
 * $Id: account_dialog.h,v 1.2 2003/10/02 14:18:40 peter Exp $
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

#ifndef ACCOUNT_DIALOG_H
#define ACCOUNT_DIALOG_H

#ifdef __GNUG__
#pragma interface "account_dialog.h"
#endif

#include "utils/utils.h"


/**
 * Simple GUI dialog to setup the IGS account data
 * @ingroup igsgui
 */
class AccountDialog : public wxDialog
{
public:

    /** Constructor */
    AccountDialog(wxWindow *parent);

    /**
     * Transfers values from child controls to data areas specified by their validators.
     * @return False if a transfer failed, else true.
     */
    bool TransferDataFromWindow();

    /** Callback for Help button
     * @todo Write a account chapter and point the help viewer there. */
    void OnHelp(wxCommandEvent& WXUNUSED(event));

private:
    IGSSettings settings;

DECLARE_EVENT_TABLE()
};

#endif
