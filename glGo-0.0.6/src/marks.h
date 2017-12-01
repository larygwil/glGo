/*
 * marks.h
 *
 * $Id: marks.h,v 1.10 2003/11/04 17:04:04 peter Exp $
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

#ifndef MARKS_H
#define MARKS_H

#include "stone.h"

/** List of all known mark types */
enum MarkType
{
    MARK_NONE,
    MARK_CIRCLE,
    MARK_SQUARE,
    MARK_TRIANGLE,
    MARK_CROSS,
    MARK_TEXT,
    MARK_TERR_BLACK,
    MARK_TERR_WHITE
};

class Board;

/** Superclass for all Marks */
class Mark : public Position
{
public:
    Mark(unsigned short x, unsigned short y) : Position(x, y) { }
    Mark(const Mark &m) : Position(m.x, m.y) { }
    virtual ~Mark() { }

    /** Gets the type of this mark */
    virtual MarkType getType() const { return MARK_NONE; }

    /** Get text of letter marks */
    virtual wxString getText() const { return wxEmptyString; }

    /** Set text of letter marks */
    virtual void setText(const wxString& WXUNUSED(s)) { }

    /** Operator == */
    bool operator==(const Mark &m) const { return x == m.x && y == m.y; }

    /** Operator != */
    bool operator!=(const Mark &m) const { return x != m.x || y != m.y; }

    /** Operator < */
    bool operator<(const Mark &m) const { return x*100+y < m.x*100+m.y; }

    /** Operator < (with Position) */
    bool operator<(const Position &p) const { return x*100+y < p.getX()*100+p.getY(); }

    /** Operator == */
    bool operator==(const Position &p) const { return x == p.getX() && y == p.getY(); }

    /** Operator != */
    bool operator!=(const Position &p) const { return x != p.getX() || y != p.getY(); }

    /**
     * Create a Mark subclass object from given position and type.
     * For example, when calling with type MARK_CIRCLE, the returned pointer will be of class MarkCircle.
     * @return Pointer to the created Mark object
     */
    static Mark* createMark(unsigned short x, unsigned short y, MarkType t, const wxString &txt = wxEmptyString);
};

/** Circle mark */
class MarkCircle : public Mark
{
public:
    MarkCircle(unsigned short x, unsigned short y) : Mark(x, y){ }
    virtual ~MarkCircle() { }
    virtual MarkType getType() const { return MARK_CIRCLE; }
};

/** Square mark */
class MarkSquare : public Mark
{
public:
    MarkSquare(unsigned short x, unsigned short y) : Mark(x, y) { }
    virtual ~MarkSquare() { }
    virtual MarkType getType() const { return MARK_SQUARE; }
};

/** Triangle mark */
class MarkTriangle : public Mark
{
public:
    MarkTriangle(unsigned short x, unsigned short y) : Mark(x, y) { }
    virtual ~MarkTriangle() { }
    virtual MarkType getType() const { return MARK_TRIANGLE; }
};

/** Cross mark */
class MarkCross : public Mark
{
public:
    MarkCross(unsigned short x, unsigned short y) : Mark(x, y) { }
    virtual ~MarkCross() { }
    virtual MarkType getType() const { return MARK_CROSS; }
};

/** Text mark */
class MarkText : public Mark
{
public:
    MarkText(unsigned short x, unsigned short y, const wxString &txt) : Mark(x, y), txt(txt) { }
    virtual ~MarkText() { }
    virtual MarkType getType() const { return MARK_TEXT; }
    virtual wxString getText() const { return txt; }
    virtual void setText(const wxString &s) { txt = s; }
private:
    wxString txt;
};

/** White territory mark. Displayed as white cross */
class MarkTerrWhite : public Mark
{
public:
    MarkTerrWhite(unsigned short x, unsigned short y) : Mark(x, y) { }
    virtual ~MarkTerrWhite() { }
    virtual MarkType getType() const { return MARK_TERR_WHITE; }
};

/** Black territory mark. Displayed as black cross */
class MarkTerrBlack : public Mark
{
public:
    MarkTerrBlack(unsigned short x, unsigned short y) : Mark(x, y) { }
    virtual ~MarkTerrBlack() { }
    virtual MarkType getType() const { return MARK_TERR_BLACK; }
};

#endif
