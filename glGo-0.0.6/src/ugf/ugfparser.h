/*
 * ugfparser.h
 *
 * $Id: ugfparser.h,v 1.2 2003/11/24 14:38:58 peter Exp $
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

#ifndef UGFPARSER_H
#define UGFPARSER_H

/**
 * @defgroup ugf UGF parser
 *
 * A parser for the UGF file format. UGF is a format used by the PandaEgg client
 * and the PandaNet mail magazine. Hardly any other client supports this
 * format. %glGo provides support to load UGF files. Support for saving is not
 * planned. The parser itself is written in Python and called from %glGo as
 * embedded Python. There are callback functions to let the Python script tell
 * the board about the read data, implementing an "extended embedded python"
 * mechanism. Python is much more suitable for this task than C++ and the speed
 * is good enough.
 *
 * @{
 */

#ifndef STANDALONE
/**
 * Main entry function to load a UGF file.
 * This function is called from a BoardHandler instance.
 * @param filename File to load
 * @param gd Pointer to the GameData instance which will receive the headers
 * @param bh Pointer to the calling BoardHandler which will receive the moves
 * @return true on success, false on failure
 */
bool loadUGF(const wxString &filename, GameData *gd, BoardHandler *bh);
#endif

/** @} */  // End of group

#endif
