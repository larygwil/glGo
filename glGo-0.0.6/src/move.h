/*
 * move.h
 *
 * $Id: move.h,v 1.19 2003/10/31 22:03:43 peter Exp $
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

#ifndef MOVE_H
#define MOVE_H

#ifdef __GNUG__
#pragma interface "move.h"
#endif


/**
 * Class representing one move within a game. A move is defined by the mandatory
 * played stone, a move number, a possible list of captures done by playing this
 * stone and a possible list of edited stones.
 * It is important to understand the difference of the played stone and edited
 * stones. The played stones is mandatory, it is part of the move. Edited stones
 * are additional stones not related to the played move, they usually origin from
 * editing or SGF files via the AW/AB property.
 * A move can only have one played stone but zero, one or more edited stones.
 * To model a game tree, each move has a pointer to its parent, son and next brother.
 * @ingroup gamelogic
 */
class Move
{
public:
    /** Default constructor. Creates an an empty move with move number 0. */
    Move();

    /**
     * Parameter constructor. Creates a move with a given played stone.
     * @param stone Stone which was played in this move
     * @param captures List of captured stones, can be empty
     * @param number Move number
     * @param caps_white White captures
     * @param caps_black Black captures
     */
    Move(const Stone &stone, const Stones &captures, unsigned short number=0,
         unsigned short caps_white=0, unsigned short caps_black=0);

    /** Destructor */
    ~Move();

    /** Gets the stone which was played in this move. */
    const Stone& getPlayedStone() const { return played_stone; }

    /** Gets the move number. */
    unsigned short getNumber() const { return number; }

    /** Gets the x position of the played stone in this move. */
    short getX() const { return played_stone.getX(); }

    /** Gets the y position of the played stone in this move. */
    short getY() const { return played_stone.getY(); }

    /** Gets the color of the played stone in this move. */
    Color getColor() const { return played_stone.getColor(); }

    /** Gets the number of brothers of this move. */
    unsigned short getBrothers() const;

    /** Gets the number of sons of this move. */
    unsigned short getSons() const;

    /** Gets the list of captures done in this move. Can be empty. */
    const Stones& getCaptures() const { return captures; }

    /**
     * Gets the list of edited stones.
     * This stones are additional to the played stone (aka the "Move")
     * and come from editing or SGF files with AB/AW property.
     * @return List of stones. Can be empty.
     */
    const Stones& getEditedStones() const { return editedStones; }

    /** Check if the given Stone is present as edited Stone. */
    bool hasEditedStone(const Stone &s) const;

    /**
     * Adds a stone to the list of edited stones.
     * This is *NOT* like playing a move but only used for editing or
     * SGF loading (AB/AW property). The played stone aka "Move" is
     * given in the constructor and cannot be altered.
     * The SGF property AE will add a stone of color STONE_REMOVED_XXX.
     * @param stone Stone to add
     * @see removeStone(const Stone&)
     */
    void addStone(const Stone &stone);

    /**
     * Remove a stone from the list of edited stones.
     * @param stone Stone to remove
     * @see addStone(const Stone&)
     */
    void removeStone(const Stone &stone);

    /** Gets the comment text. */
    const wxString& getComment() const { return comment; }

    /** Sets the comment text. */
    void setComment(const wxString& str) { comment = str; }

    /** Is this move already checked? */
    bool isChecked() const { return checked; }

    /** Marks this move checked. */
    void check() { checked = true; }

    /** Marks this move checked with the given captures. */
    void check(Stones &caps);

    /** Gets a list of all marks of the current position. */
    const Marks& getMarks() const { return marks; }

    /**
     * Check if the move already has the given mark. It only checks
     * for the x/y coordinates, not the mark type, as two different
     * marks on the same location are invalid.
     * @return True if the position already has a mark, else false
     */
    bool hasMark(const Position &mark);

    /** Gets the mark at the given position, or NULL if no mark exists. */
    const Mark* getMark(const Position &mark);

    /** Adds a mark of type t at the given x/y coordinates. */
    bool addMark(unsigned short x, unsigned short y, MarkType t, const wxString &txt = wxEmptyString);

    /** Adds the given mark. */
    void addMark(Mark* mark);

    /** Removes a mark of type t at the given x/y coordinates. */
    bool removeMark(unsigned short x, unsigned short y, MarkType t);

    /** Removes all marks. */
    void clearMarks();

    /** Gets the number of white captures */
    unsigned short getCapsWhite() const { return caps_white; }

    /** Gets the number of black captures */
    unsigned short getCapsBlack() const { return caps_black; }

    /** Sets the number of white captures */
    void setCapsWhite(unsigned short caps) { caps_white = caps; }

    /** Sets the number of black captures */
    void setCapsBlack(unsigned short caps) { caps_black = caps; }

    /** Finds the next free textmark letter in the range of A-Z, a-z */
    wxChar getNextFreeLetter();

    /** Convert the move to a string in SGF format. */
    wxString MoveToSGF();

    Move *parent;   ///< Parent
    Move *brother;  ///< Next brother
    Move *son;      ///< First son
    Move *marker;   ///< Navigation marker pointing to the last used son

private:
    wxString saveMarks();
    wxString saveEditedStones();

    Stone played_stone;
    Stones editedStones;
    Stones captures;
    Marks marks;
    unsigned short number, caps_white, caps_black;
    wxString comment;
    bool checked;
};

inline void Move::removeStone(const Stone &stone)
{
    editedStones.remove(stone);
}

#endif
