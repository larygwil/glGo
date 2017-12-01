/*
 * boardhandler.h
 *
 * $Id: boardhandler.h,v 1.42 2003/11/24 01:32:34 peter Exp $
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

#ifndef BOARDHANDLER_H
#define BOARDHANDLER_H

#ifdef __GNUG__
#pragma interface "boardhandler.h"
#endif

#include "stones.h"

class Board;
class Game;
class GameData;
class Move;
class Matrix;


/**
 * @defgroup gamelogic Game logic
 *
 * This module contains the implementation for the essential game logic.
 *
 * This includes the position of stones within each move of the game, the
 * algorithms to navigate through the game, play new moves, check if moves
 * are legal and calculate and store captures.
 *
 * This code should be completely seperated from the actual display, to
 * properly implement a document-view architecture.
 *
 * @{
 */

/** General Edit parameters */
enum EditParam
{
    EDIT_PARAM_NUMBER_MOVES,
    EDIT_PARAM_MARK_BROTHERS,
    EDIT_PARAM_MARK_SONS,
    EDIT_PARAM_ALL
};

/**
 * This class is responsible for the storage of current stone positions.
 * It is associated with a Board and will care for adding moves and navigating
 * through the game. While a Game instance is associated with a BoardHandler which
 * stores the complete game, this class mainly cares for the current position only
 * and tells the Board which stones to display, calculates captures and checks if
 * played stones are legal. The communication between Board and BoardHandler is
 * done by events using the pluggable BoardEventhandler.
 */
class BoardHandler
{
public:
    /** Constructor */
    BoardHandler(Board *board);

    /** Destructor */
    ~BoardHandler();

    /** Create a new game. */
    void newGame(GameData *data);

    /** Checks if game is modified and has to be saved. */
    bool isModified() const { return is_modified; }

    /** Mark the game as modified. */
    void setModified() { is_modified = true; }

    /** Checks if the current position needs to be redrawn by the Board. */
    bool checkUpdate()
        { if (sgf_loading) return false; bool tmp = is_updated; is_updated = false; return tmp; }

    /**
     * Check if there is a single modified stone.
     * This is used for the OpenGL scissor test and returns true if there was only one added stone
     * without captures, so the board can apply its scissor function.
     * @return True if there was only a single modified stone without captures
     * @see getModifiedStone()
     */
    bool haveModifiedStone() const { return have_modified_stone; }

    /**
     * Gets the single modified stone or the default stone if there were captures.
     * @see haveModifiedStone()
     */
    const Stone& getModifiedStone() { have_modified_stone = false; return modified_stone; }

    /** Gets the current Game object. */
    Game* getGame() const { return game; }

    /** Gets a list of all stones of the current position. */
    const Stones& getStones() const { return stones; }

    /** Gets a list of all ghost stones of the current position. */
    const Stones& getGhosts() const { return ghosts; }

    /** Check if the given position is occupied with a ghost stone of any color. */
    bool hasGhost(const Position &pos);

    /** Gets the Stone at a given Position or NULL if no stone was found. */
    const Stone* getStone(const Position &pos);

    /** Check if the given position is occupied with a stone of any color. */
    bool hasPosition(const Position &pos);

    /** Gets a list of all marks of the current position. */
    const Marks& getMarks();

    /** Adds a mark of type t at the given x/y coordinates. The txt parameter is only used for SGF loading. */
    bool addMark(unsigned short x, unsigned short y, MarkType t, wxString txt = wxEmptyString);

    /** Removes a mark of type t at the given x/y coordinates. */
    bool removeMark(unsigned short x, unsigned short y, MarkType t);

    /** Removes all marks in the current move. */
    void removeAllMarks();

    /** Gets the position of the last move. */
    const Position& getLastMovePos() const { return lastMovePos; }

    /** Gets the color of the current turn. */
    Color getCurrentTurnColor() const;

    /** Gets the comment string of the current move. */
    const wxString& getComment(bool at_last=false) const;

    /** Sets a comment to the current move. */
    void setComment(const wxString &comment, bool at_last=false);

    /**
     * Add an editable stone.
     * This is not adding a move. There is also no legal or captures check
     * when adding a stone this way.
     * @param x X position
     * @param y Y position
     * @param c Stone color
     * @see removeEditStone(unsigned short, unsigned short)
     */
    void addEditStone(unsigned short x, unsigned short y, Color c);

    /**
     * Cut down version used for SGF loading, omitting some code.
     * @see addEditStone(unsigned short, unsigned short, Color)
     */
    void addEditStoneSGF(unsigned short x, unsigned short y, Color c);

    /**
     * Remove an editable stone. This is not deleting or undoing a move.
     * @param x X position
     * @param y Y position
     * @see addEditStone(unsigned short, unsigned short, Color)
     */
    void removeEditStone(unsigned short x, unsigned short y);

    /**
     * Cut down version used for SGF loading, omitting some code.
     * @see removeEditStone(unsigned short, unsigned short)
     */
    void removeEditStoneSGF(unsigned short x, unsigned short y);

    /**
     * Play a stone, creating a new move.
     * The move will be checked if it is legal and captures other stones.
     * Captures are stored in the Move object created by this function.
     * @param x X position of the played stone
     * @param y Y position of the played stone
     * @param c Stone color. If STONE_UNDEFINED, the proper alternating turn is used
     * @return True if the move is legal, else false
     */
    bool playMove(short x, short y, Color c=STONE_UNDEFINED);

    /**
     * Similar to playMove with several adjustments for IGS.
     * Especially glGo does not do its own captures calculation but uses the captures
     * list provided by IGS.
     */
    bool playMoveIGS(short x, short y, Color c, const Stones &captures, unsigned short move_number, bool silent=false);

    /**
     * Play a stone when loading SGF files, creating a new move.
     * This function will not check if the move is legal and not calculate
     * captures. The created Move will be marked as unchecked.
     * Those checks are done later when navigating through the game.
     * @param x X position of the played stone
     * @param y Y position of the played stone
     * @param c Stone color. If STONE_UNDEFINED, the proper alternating turn is used
     */
    void playMoveSGF(short x, short y, Color c);

    /**
     * Navigate to next move.
     * @return True if moved, false if there is no next move
     */
    bool nextMove();

    /**
     * Navigate to previous move.
     * @return True if moved, false if there is no previous move
     */
    bool previousMove();

    /**
     * Navigate to first move.
     * @param forceUpdate If true, force update of edited stoned. Required for SGF loading of handicap
     *                    games with zero moves (unlikely, but who knows...)
     * @return True if moved, false if already at first move
     */
    bool firstMove(bool forceUpdate = false);

    /**
     * Navigate to last move.
     * @return True if moved, false if already at last move
     */
    bool lastMove();

    /**
     * Navigate to next variation, if any.
     * @return True if moved, false if no next variation
     */
    bool nextVariation();

    /**
     * Navigate to previous variation, if any.
     * @return True if moved, false if no previous variation
     */
    bool previousVariation();

    /**
     * Setup fixed handicap stones.
     * Use the default position by japanese rules.
     * @param handicap Number of handicap stones. Must be 2-9.
     * @param increase_move Interpret handicap setup as a real move. Required for IGS games.
     * @todo Make fixed and non-fixed version
     */
    bool setupHandicap(unsigned short handicap, bool increase_move=false);

    /**
     * Load a SGF file.
     * @param filename SGF filename
     * @param parent Pointer to the MainFrame which embeds this board
     * @param is_tmp_filename True if loaded from temp file
     * @return True on success, false on failue
     */
    bool loadGame(const wxString &filename, wxWindow *parent, bool is_tmp_filename=false);

    /**
     * Save game to SGF.
     * @param filename SGF filename
     * @param dont_remember If true, don't remember the filename. Useful for temp files.
     * @return True on success, false on failue
     */
    bool saveGame(const wxString &filename, bool dont_remember=false);

    /** Delete current move and the subtree of children. */
    void deleteCurrentMove();

    /** Set editor parameters. This must implement all values of the EditParam enum. */
    void setEditParameter(EditParam param, bool value);

    /** Process the given editor parameters. */
    void processEditParams(int params=EDIT_PARAM_ALL);

    void score();
    void finishScore(bool displayResult=true);
    bool markStoneDeadOrSeki(const Position &pos, bool dead=true);
    void removeDeadMarks();
    void displayTerritoryFromMatrix(Matrix *matrix);

private:
    bool hasStone(const Stone &stone);
    bool addStone(const Stone &stone);
    bool removeStone(const Stone &stone);
    bool checkDeadMarks();
    bool setupEditedStones(Move *new_move, Move *old_move);
    bool setupVariation(Move *move, Move *removed);
    bool checkMove(const Stone &stone, Stones &captures);
    bool checkLegal(const Stone &stone);
    bool checkCaptures(const Stone &stone, Stones &captures);
    bool checkNeighbour(Stones *group, Stone *stone);
    bool checkPosition(Stones *group);
    void assembleGroup(Stones *group);
    void createGhosts();
    void deleteNode(Move *move=NULL);
    void clearMarks();
    /** @todo Use mark type number */
    void numberMoves();
    /** @todo Use number marks for brothers */
    void markVariations(bool sons=false);
    /** @todo Some error message if loading failed */
    bool loadGameUGF(const wxString &filename, wxWindow *parent);

    Stones stones, ghosts;
    Position lastMovePos;
    Board *board;
    Game *game;
    bool is_modified, is_updated, sgf_loading, number_moves, mark_brothers, mark_sons, have_modified_stone;
    Marks marks, tmp_marks;
    Stone modified_stone;
    float score_result;
};

/** @} */  // End of group

#endif
