/*
 * game.h
 *
 * $Id: game.h,v 1.20 2003/11/24 15:58:27 peter Exp $
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

#ifndef GAME_H
#define GAME_H

#ifdef __GNUG__
#pragma interface "game.h"
#endif


class Move;
class GameData;

/**
 * This class represents a complete game. A game takes place within one board window and
 * contains a sequence or tree (if variations exist) of moves. This class is responsible
 * for storage of moves and captures.
 * @ingroup gamelogic
 */
class Game
{
public:
    /** Default constructor. Creates an empty Game object. */
    Game();

    /** Destructor */
    ~Game();

    /**
     * Create a new game. All stored moves are deleted.
     * @param data Pointer to the new GameData instance which replaces the current one
     */
    void newGame(GameData *data);

    /** Resets the game. This clears the complete tree and creates a new empty root node. */
    void reset();

    /**
     * Check if it is the turn of the black player.
     * @return True if blacks turn, false if whites turn
     */
    Color getCurrentTurnColor() const { return black_turn ? STONE_BLACK : STONE_WHITE; }

    /** Set the current turn color. Used for SGF editing and handicap setup. */
    void setCurrentTurnColor(Color c) { black_turn = c == STONE_BLACK; }

    /** Gets a pointer to the current move. */
    Move* getCurrentMove() { return current; }

    /** Sets the current move. */
    void setCurrentMove(Move *m) { current = m; }

    /** Gets a pointer to the root move. */
    Move* getRoot() { return root; }

    /** Sets the root move. Ugly, but relocating root is required for node deletion. */
    void setRoot(Move *m) { root = m; }

    /** Gets a pointer to the last move. */
    Move *getLast() { return last; }

    /** Point last to current move. Used for undo. */
    void makeCurrentLast() { last = current; }

    /** Gets the current move number. */
    unsigned short getCurrentNumber() const { return current_number; }

    /** Sets the current move number. */
    void setCurrentNumber(unsigned short n) { current_number = n; }

    /**
     * Gets a pointer to the GameData. This object contains the game information
     * like black, white player, board size, komi, handicap etc.
     */
    GameData* getGameData() const { return game_data; }

    /**
     * Replaces the current GameData object.
     * @param data Pointer to new GameData instance
     */
    void setGameData(GameData *data);

    /**
     * Add a new move. The move will be appended at the current move.
     * Variations are not yet supported.
     * @param stone Played stone
     * @param captures List of captures. Empty list of none.
     * @param check If true, this Move has already been checked.
     * @param force_last If true, force appending this move as last move, even if we
     *                   are currently in earlier moves (used for IGS observing)
     */
    void addMove(const Stone &stone, const Stones& captures, bool check=true, bool force_last=false);

    /**
     * Navigate forward. If no next move exists, this function returns NULL.
     * @return Pointer to the next move. Null if none exists.
     */
    Move* next();

    /**
     * Navigate backward. If no previous move exists, this function returns NULL.
     * @param nomarker If true, the marker pointers in nodes won't be set
     * @return Pointer to the previous move. Null if none exists.
     */
    Move* previous(bool nomarker=false);

    /**
     * Navigate to first move. This will return NULL for non-handicap games.
     * @return First move
     */
    Move* first();

    /**
     * Navigate to next variation, if any.
     * @param removed This reference to a pointer will store the removed Move
     * @return Next variation move or NULL if no next variation exists
     */
    Move* nextVar(Move *&removed);

    /**
     * Navigate to previous variation, if any.
     * @param removed This reference to a pointer will store the removed Move
     * @return Previous variation move or NULL if no previous variation exists
     */
    Move* previousVar(Move *&removed);

    /**
     * Cleanup tree and delete stored moves.
     * @param move If NULL, the complete tree is deleted. Otherwise the tree is
     *             traversed in pre-order beginning from this move. This can be
     *             used to delete a subtree from a node with all children.
     */
    void deleteTree(Move *move=NULL);

    /** Check if the given move has a previous brother. */
    bool hasPrevBrother(Move *m);

private:
    bool black_turn;
    Move *root, *current, *last;
    unsigned short current_number, total_number;
    GameData *game_data;
};

#endif
