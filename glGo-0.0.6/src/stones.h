/*
 * stones.h
 *
 * $Id: stones.h,v 1.2 2003/10/02 14:16:21 peter Exp $
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

/**
 * @file
 * Some common typedefs for the stones and marks STL lists.
 * This is a seperate file so other headers can include them exclusively.
 */

#ifndef STONES_H
#define STONES_H

#include "stone.h"
#include "marks.h"

#include <list>
using namespace std;

typedef list<Stone> Stones;
typedef list<Stone>::iterator StonesIterator;
typedef list<Stone>::const_iterator ConstStonesIterator;
typedef list<Mark*> Marks;
typedef list<Mark*>::iterator MarksIterator;
typedef list<Mark*>::const_iterator ConstMarksIterator;

#endif
