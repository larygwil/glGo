/*
 * newgame_dialog.h
 *
 * $Id: newgame_dialog.h,v 1.3 2003/10/02 14:16:51 peter Exp $
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

#ifndef NEWGAME_DIALOG_H
#define NEWGAME_DIALOG_H

#ifdef __GNUG__
#pragma interface "newgame_dialog.h"
#endif

#include "gamedata.h"


/**
 * Dialog to create a new game. The user is prompted for the game data like
 * board size, handicap, komi, player names etc. The result is stored into
 * the GameData object passed in the constructor, so the calling element can
 * read the values from this pointer. The dialog will only accept "Ok" when
 * the input data is valid.
 * The dialog is created from the XML resource newgame_dialog.xrc
 * @ingroup userinterface
 */
class NewGameDialog : public wxDialog
{
public:

    /**
     * Constructor.
     * @param parent Parent Window
     * @param game_data Pointer to the GameData object which stores the user input data
     */
    NewGameDialog(wxWindow *parent, GameData *game_data);

private:
    GameData *data;
};

#endif
