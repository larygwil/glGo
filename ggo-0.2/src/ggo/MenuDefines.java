/*
 * MenuDefines.java
 *
 *  gGo
 *  Copyright (C) 2002  Peter Strempel <pstrempel@t-online.de>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package ggo;

/**
 *  Menu constants for MainFrame and subclasses
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:55 $
 */
interface MenuDefines {
    final static int FILE_NEW_BOARD = 1;
    final static int FILE_NEW = 2;
    final static int FILE_OPEN = 4;
    final static int FILE_SAVE = 8;
    final static int FILE_SAVE_AS = 16;
    final static int FILE_CLOSE = 32;
    final static int FILE_CONNECT_IGS = 64;
    final static int FILE_CONNECT_GTP = 128;
    final static int FILE_EXIT = 256;

    final static int FILE_IMPORTEXPORT_IMPORT_SGF = 1;

    final static int EDIT_DELETE = 1;
    final static int EDIT_SWAP_VARIATIONS = 2;
    final static int EDIT_NUMBER_MOVES = 4;
    final static int EDIT_MARK_BROTHERS = 8;
    final static int EDIT_MARK_SONS = 16;
    final static int EDIT_IGS = 32;
    final static int EDIT_GAME = 64;
    final static int EDIT_GUESS_SCORE = 128;

    final static int SETTINGS_PREFERENCES = 1;
    final static int SETTINGS_GAME_INFO = 2;
    final static int SETTINGS_MEMORY_STATUS = 4;
    final static int SETTINGS_MEMORY_CLEANUP = 8;
    final static int SETTINGS_AUTOPLAY_DELAY = 16;

    final static int VIEW_CLEAR = 1;
    final static int VIEW_TOOLBAR = 2;
    final static int VIEW_STATUSBAR = 4;
    final static int VIEW_COORDS = 8;
    final static int VIEW_SIDEBAR = 16;
    final static int VIEW_CURSOR = 32;
    final static int VIEW_SLIDER = 64;
    final static int VIEW_VARIATIONS = 128;
    final static int VIEW_HORIZONTAL_COMMENT = 256;
    final static int VIEW_SAVE_SIZE = 512;
}
