/*
 * sgfwriter.h
 *
 * $Id: sgfwriter.h,v 1.4 2003/10/04 19:06:05 peter Exp $
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

#ifndef SGFWRITER_H
#define SGFWRITER_H

#ifdef __GNUG__
#pragma interface "sgfwriter.h"
#endif

class BoardHandler;
class Game;
class GameData;
class Move;
class wxBufferedOutputStream;

/**
 * Class to save a game to SGF format.
 * This class will open an output stream to the given file and
 * traverse the game tree in pre-order, calling Move::MoveToSGF
 * in each node of the tree, creating the SGF output.
 * @ingroup sgf
 */
class SGFWriter
{
public:
    /** Constructor */
    SGFWriter();

    /**
     * Save a game to a SGF file.
     * This is the main entry function for this class.
     * Saving the SGF is called in the BoardHandler with something like this:
     * @code
     * SGFWriter writer();
     * bool res = writer.saveSGF(filename, game);
     * if (res)
     *     // Do something
     * @endcode
     * @param filename Filename of the to be saved SGF file
     * @param game Pointer to the Game object to be saved
     * @return True on success, else false
     */
    bool saveSGF(const wxString &filename, Game *game);

private:
    void writeGameHeader(GameData *data);
    void traverse(Move *m);

    wxBufferedOutputStream *ostream;
};

#endif
