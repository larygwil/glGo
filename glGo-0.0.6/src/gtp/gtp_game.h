/*
 * gtp_game.h
 *
 * $Id: gtp_game.h,v 1.11 2003/10/19 04:47:34 peter Exp $
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

#ifndef GTP_GAME_H
#define GTP_GAME_H

#ifdef __GNUG__
#pragma interface "gtp_game.h"
#endif

#include "defines.h"
#include "gtp_config.h"


/**
 * %GTP engine state.
 * These contants are used for the definition of the state of the %GTP engine,
 * which is somewhat a cheap version of a deterministic final automate.
 * @ingroup gtp
 */
enum GTPState
{
    GTP_STATE_UNKNOWN,               ///< State: Undefined
    GTP_STATE_SETUP_GAME_NAME,       ///< State: Setting up new game, sent "name"
    GTP_STATE_SETUP_GAME_VERSION,    ///< State: Setting up new game, sent "version"
    GTP_STATE_SETUP_GAME_BOARDSIZE,  ///< State: Setting up new game, sent "boardsize"
    GTP_STATE_SETUP_GAME_KOMI,       ///< State: Setting up new game, sent "komi"
    GTP_STATE_SETUP_GAME_LEVEL,      ///< State: Setting up new game, sent "level"
    GTP_STATE_SETUP_GAME_HANDICAP,   ///< State: Setting up new game, sent "fixed_handicap"
    GTP_STATE_MOVE_WHITE,            ///< State: White to play
    GTP_STATE_MOVE_BLACK,            ///< State: Black to play
    GTP_STATE_CONFIRMING_MOVE,       ///< State: Waiting for human move confirmation from %GTP engine
    GTP_STATE_SCORING,               ///< State: Game ended, scoring
    GTP_STATE_DONE,                  ///< State: Game finished
    GTP_STATE_RESUME_GAME            ///< State: Resume game, wait for color to play
};


class GTP;
class GameData;
class Position;

/**
 * GTPGame
 * @ingroup gtp
 */
class GTPGame
{
public:
    /** Constructor */
    GTPGame(GTP *const gtp, GTPConfig *conf);

    /** Gets the current state of the %GTP game and parsing logic. */
    GTPState getState() const { return state; }

    /** Sets the current state. */
    void setState(GTPState s) { state = s; }

    /** Gets a pointer to the GTPConfig object. */
    GTPConfig* getGTPConfig() const { return gtp_config; }

    /**
     * Check if it is the turn of the given player color.
     * @param color Color to check. If STONE_UNDEFINED is given, the check uses the color of the current turn.
     * @return True if we may move, else false
     */
    bool mayMove(Color color=STONE_UNDEFINED);

    /**
     * Init and start a new game.
     * @param conf Pointer to the GTPConfig configuration object
     */
    void initGame(GTPConfig *conf = NULL);

    /**
     * Start the game.
     * For resumed games the turn has to be given, for non-resumed games color must be STONE_UNDEFINED.
     * @param color Player who has the first turn.
     *              Only used for resumed games, otherwise leave empty or set to STONE_UNDEFINED.
     */
    void startGame(Color color = STONE_UNDEFINED);

    /**
     * The %GTP engine has done a move. Forward it to the board and generate next move.
     * @param x Move X position
     * @param y Move Y position
     */
    void receiveMoveFromGTP(short x, short y);

    /**
     * Human player has done a move. Forward it to the %GTP engine and generate next move.
     * @param  color  Color of the played move
     * @param  x      X coordinate of the move
     * @param  y      Y coordinate of the move
     */
    void receiveMoveFromHuman(short x, short y, Color color);

    /**
     * When a human move was sent to the %GTP engine, this method is called from
     * GTPInput for move validation.
     * @param confirmed  True if the move was legal, paint it on the board and generate
     *                   next turn. False if move was illegal.
     */
    void confirmMove(bool confirmed);

    /**
     *  %GTP engine sent handicap stones.
     * @param positions Array of Positions
     * @todo We parse handicap, but don't use it but use our own fixed positions. Argh!
     */
    void recieveHandicapFromGTP(Position **positions);

private:
    /**
     * Send command to %GTP engine. An event is posted, the GTP class will take
     * care of the rest.
     * @param command Command to send
     */
    void sendGTPCommand(wxString command);

    /**  Tell %GTP engine to generate a move. Color is calculated automatically. */
    void generateMove();

    /**
     * Switch turns after a move was made
     * @return Color of the turn after the switch
     */
    Color switchTurn();

    /**
     * Check if the game has finished by passing twice in a row.
     * If the game has ended, the interface switched to scoring.
     * @param  x X coordinate of last move. -1 if pass
     * @param  y Y coordinate of last move. -1 if pass
     * @return True if game has ended, else false
     */
    bool checkGameFinished(short x, short y);

    GTP *const gtp;
    GTPConfig *gtp_config;
    GTPState state, lastState;
    Color turn;
    short lastHumanX, lastHumanY;
    bool lastMoveWasPass;
};

#endif
