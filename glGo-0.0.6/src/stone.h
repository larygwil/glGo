/*
 * stone.h
 *
 * $Id: stone.h,v 1.16 2003/11/24 14:38:41 peter Exp $
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

#ifndef STONE_H
#define STONE_H

#ifdef __GNUG__
#pragma interface "stone.h"
#endif

#include "defines.h"


/**
 * Class representing a single board position.
 * @ingroup gamelogic
 */
class Position
{
public:
    /** Default constructor. Create a position at 0/0. */
    Position();

    /**
     * Copy constructor.
     * @param p Position to copy from
     */
    Position(const Position &p);

    /**
     * Parameter constructor.
     * @param x x position
     * @param y y position
     */
    Position(short x, short y);

    /** Destructor */
    virtual ~Position();

    /**
     * Gets the x position
     * @return x coordinate from 1 to 19 (or other board size)
     */
    short getX() const { return x; }

    /**
     * Gets the y position
     * @return y coordinate from 1 to 19 (or other board size)
     */
    short getY() const { return y; }

    /**
     * Operator ==. Compare this position with another.
     * @param p Position to compare with
     * @return True if equal, false if not equal.
     */
    bool operator==(const Position &p) const { return x == p.x && y == p.y; }

    /**
     * Operator !=. Compare this position with another.
     * @param p Position to compare with
     * @return True if not equal, false if equal.
     */
    bool operator!=(const Position &p) const { return x != p.x || y != p.y; }

    /**
     * operator <. Compare this position with another.
     * @param p Position to compare with
     * @return True if lesser, else false
     */
    bool operator<(const Position &p) const { return x*100+y < p.x*100+p.y; }

    /**
     * operator =. Assign another position to this instance.
     * @param p Position to assign to this instance
     * @return This position instance
     */
    Position& operator=(const Position &p);

    /**
     * Debug counter to check for memory leaks.
     * @todo Remove this later
     */
#ifdef __WXDEBUG__
    static int counter;
#endif

protected:
    short x, y;
};


/**
 * Class representing a single stone. Inherits from Position and adds a color attribute.
 * @ingroup gamelogic
 */
class Stone : public Position
{
public:
    /** Default constructor. Creates a stone at 0/0 of color NONE. */
    Stone() : Position(), color(STONE_UNDEFINED), isDead(false), isSeki(false) {}

    /**
     * Copy constructor
     * @param s Stone instance to copy from
     */
    Stone(const Stone &s) : Position(s.x, s.y), color(s.color), isDead(s.isDead), isSeki(s.isSeki) {}

    /**
     * Parameter constructor
     * @param x x position
     * @param y y position
     * @param color Stone color
     */
    Stone(short x, short y, Color color) : Position(x, y), color(color), isDead(false), isSeki(false) {}

    /** Destructor */
    virtual ~Stone() {}

    /**
     * Gets the stone color
     * @return Stone color
     */
    Color getColor() const { return color; }

    /**
     * Operator ==. Compare this stone instance with another.
     * @param s Stone instance to compare with
     * @return True if equal, false if not equal
     */
    bool operator==(const Stone &s) const { return x == s.x && y == s.y && color == s.color; }

    /**
     * Operator !=. Compare this stone instance with another.
     * @param s Stone instance to compare with
     * @return True if not equal, false if equal
     */
    bool operator!=(const Stone &s) const { return x != s.x || y != s.y || color != s.color; }

    /**
     * Operator !=. Compare this stone instance with a Position
     * @param p Position instance to compare with
     * @return True if equal, false if not equal
     */
    bool operator==(const Position &p) const { return x == p.getX() && y == p.getY(); }

    /**
     * Operator !=. Compare this stone instance with a Position
     * @param p Position instance to compare with
     * @return True if not equal, false if equal
     */
    bool operator!=(const Position &p) const { return x != p.getX() || y != p.getY(); }

    /**
     * Operator <. Compare this stone instance with another
     * @param s Stone instance to compare with
     * @return True if lesser, else false
     */
    bool operator<(const Stone &s) const { return x*100+y < s.x*100+s.y; }

    /**
     * Operator =. Assign another stone instance
     * @param s Stone instance to assign
     * @return This stone instance
     */
    Stone& operator=(const Stone &s);

    /** Check if the stone is marked as dead */
    bool IsDead() const { return isDead; }

    /** Mark or unmark the stone as dead */
    void SetDead(bool b=true) { isDead = b; }

    /** Check if the stone is marked as seki */
    bool IsSeki() const { return isSeki; }

    /** Mark or unmark the stone as seki */
    void SetSeki(bool b=true) { isSeki = b; }

private:
    Color color;
    bool isDead, isSeki;
};

#endif
