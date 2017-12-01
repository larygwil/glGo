/*
 * boardhandler.cpp
 *
 * $Id: boardhandler.cpp,v 1.68 2003/11/24 15:58:45 peter Exp $
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

#ifdef __GNUG__
#pragma implementation "boardhandler.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/window.h"
#include "wx/log.h"
#include "wx/intl.h"
#endif

#include <wx/config.h>
#include <wx/filename.h>
#include "boardhandler.h"
#include "move.h"
#include "game.h"
#include "gamedata.h"
#include "board.h"
#include "gamedata.h"
#include "matrix.h"
#include "utils/utils.h"
#include "sgfparser.h"
#include "sgfwriter.h"
#include "ugf/ugfparser.h"
#include <algorithm>

// #define DEBUG_CAPS

BoardHandler:: BoardHandler(Board *board)
{
    wxASSERT(board != NULL);
    this->board = board;
    is_modified = is_updated = sgf_loading = have_modified_stone = false;
    game = new Game();

    wxConfig::Get()->Read(_T("Board/NumberMoves"), &number_moves, false);
    wxConfig::Get()->Read(_T("Board/MarkBrothers"), &mark_brothers, false);
    wxConfig::Get()->Read(_T("Board/MarkSons"), &mark_sons, false);
}

BoardHandler::~BoardHandler()
{
    stones.clear();
    ghosts.clear();
    delete game;
    clearMarks();
}

void BoardHandler::clearMarks()
{
    while (!marks.empty())
    {
        Mark *m = marks.back();
        marks.pop_back();
        delete m;
    }
    marks.clear();
}

void BoardHandler::newGame(GameData *data)
{
    is_modified = false;
    is_updated = true;
    stones.clear();
    ghosts.clear();
    clearMarks();
    lastMovePos = Position(0, 0);
    modified_stone = Stone();
    have_modified_stone = false;
    game->newGame(data);
    if (data->handicap > 1)
        setupHandicap(data->handicap);
    board->updateMainframe();
}

Color BoardHandler::getCurrentTurnColor() const
{
    return game->getCurrentTurnColor();
}

bool BoardHandler::hasStone(const Stone &stone)
{
    if (stones.empty())
        return false;

    ConstStonesIterator result = find(stones.begin(), stones.end(), stone);
    if (result == stones.end())
        return false;
    return true;
}

bool BoardHandler::hasPosition(const Position &pos)
{
    if (stones.empty())
        return false;

    ConstStonesIterator result = find(stones.begin(), stones.end(), pos);
    if (result == stones.end())
        return false;
    return true;
}

bool BoardHandler::hasGhost(const Position &pos)
{
    if (ghosts.empty())
        return false;

    ConstStonesIterator result = find(ghosts.begin(), ghosts.end(), pos);
    if (result == ghosts.end())
        return false;
    return true;
}

const Stone* BoardHandler::getStone(const Position &pos)
{
    if (stones.empty())
        return NULL;

    StonesIterator result = find(stones.begin(), stones.end(), pos);
    if (result == stones.end())
        return NULL;
    return &*result;
}

bool BoardHandler::addStone(const Stone &stone)
{
    // Make sure the stone is a valid board position
    if (stone.getX() < 1 || stone.getX() > static_cast<short>(board->getBoardSize()) ||
        stone.getY() < 1 || stone.getY() > static_cast<short>(board->getBoardSize()))
    {
#ifdef __WXDEBUG__
        if (stone.getX() > 0)
            wxLogDebug(wxString::Format(_T("Illegal stone: %d/%d"), stone.getX(), stone.getY()));
#endif
        return false;
    }

    // Make sure we do not have the stone yet
    if (hasPosition(stone))
    {
        // wxLogDebug(_T("Already have a stone at %d/%d"), stone.getX(), stone.getY());
        return false;
    }

    // Ok, add stone to the list and mark game as modified
    stones.push_back(stone);
    // wxLogDebug(_T("Added stone %d/%d %d"), stone.getX(), stone.getY(), stone.getColor());
    return true;
}

bool BoardHandler::removeStone(const Stone &stone)
{
    // Make sure the stone is a valid board position
    if (stone.getX() < 1 || stone.getX() > static_cast<short>(board->getBoardSize()) ||
        stone.getY() < 1 || stone.getY() > static_cast<short>(board->getBoardSize()))
    {
#ifdef __WXDEBUG__
        if (stone.getX() > 0)
            wxLogDebug(wxString::Format(_T("Illegal stone: %d/%d"), stone.getX(), stone.getY()));
#endif
        return false;
    }

    // Make sure we have a stone there
    if (!hasPosition(stone))
    {
        // wxLogDebug(_T("No stone at position %d/%d"), stone.getX(), stone.getY());
        return false;
    }

    stones.remove(stone);
    // wxLogDebug(_T("Removed stone %d/%d %d"), stone.getX(), stone.getY(), stone.getColor());
    return true;
}

bool BoardHandler::playMove(short x, short y, Color c)
{
    // wxLogDebug(_T("BoardHandler::playMove %d/%d"), x, y);

    // Block while loading SGF
    if (sgf_loading)
        return false;

    // Get correct stone color if not given as parameter
    if (c == STONE_UNDEFINED)
        c = getCurrentTurnColor();

    // Create stone
    Stone stone(x, y, c);

    bool have_caps = false;

    // Pass or empty move?
    if ((x == 0 && y == 0) || (x == -1 && y == -1) ||
        (x == static_cast<short>(board->getBoardSize()) + 1 &&
         y == static_cast<short>(board->getBoardSize()) + 1))
        game->addMove(stone, Stones());

    // Real move
    else
    {
        // Add stone to list
        if (!addStone(stone))
            return false;

        // Legal move? Captures?
        Stones captures;
        if (!checkMove(stone, captures))
            return false;

        // Create and add move
        game->addMove(stone, captures);
        have_caps = !captures.empty();
        captures.clear();
    }

    // Add last move mark
    lastMovePos = stone;

    // Set flags for OpenGL scissor test
    have_modified_stone = !have_caps;
    modified_stone = stone;

    // Create ghosts stones indiciating possible variations
    createGhosts();

    // Mark the board position as updated so the board will redrawn
    is_updated = true;
    is_modified = true;

    // Play stone or pass sound
    if (board->getPlayLocalSound())
        playSound((x == -1 && y == -1) || (x == static_cast<short>(board->getBoardSize()) + 1 &&
                                           y == static_cast<short>(board->getBoardSize()) + 1) ?
                  SOUND_PASS : SOUND_STONE);

    return true;
}

bool BoardHandler::playMoveIGS(short x, short y, Color c, const Stones &captures, unsigned short move_number, bool silent)
{
    // wxLogDebug(_T("BoardHandler::playMoveIGS %d/%d %d %d - %d (%d)"), x, y, c, silent, move_number, game->getCurrentNumber());

    // Does the move number fit?
    // Note: IGS move numbers start from 0, so we don't need to add 1 to the current move
    bool not_last = false;
    // wxLogDebug("move_number = %d, currentNumber = %d", move_number, game->getCurrentNumber());
    if (move_number > (game->getCurrentNumber() + (game->getGameData()->handicap >= 2 ? 1 : 0)))
    {
        // This avoids confusion with the "moves" command batch
        if (silent)
            return false;
        // We are  not at the last move
        not_last = true;
    }

    // Create stone
    Stone stone(x, y, c);

    // Pass or empty move?
    if ((x == 0 && y == 0) || (x == -1 && y == -1) ||
        (x == static_cast<short>(board->getBoardSize()) + 1 &&
         y == static_cast<short>(board->getBoardSize()) + 1))
        game->addMove(stone, Stones());

    // Real move
    else
    {
        // Add stone to list if we are at the last move
        if (!not_last)
        {
            if(!addStone(stone))
                return false;
            // Remove IGS captures
            ConstStonesIterator it;
            for (it=captures.begin(); it!=captures.end(); ++it)
                removeStone(*it);
        }

        // Create and add move. Force adding to last move.
        game->addMove(stone, captures, true, not_last);
    }

    // Create ghosts stones indiciating possible variations
    // No ghosts on IGS (at least until implemented observed teaching games again...)
    // createGhosts();

    // Skip this if we are not at the last move
    if (!not_last)
    {
        // Add last move mark
        lastMovePos = stone;

        // Set flags for OpenGL scissor test
        if (captures.empty() && !silent)
        {
            modified_stone = stone;
            have_modified_stone = true;
        }
        else
        {
            modified_stone = Stone();
            have_modified_stone = false;
        }

        // Mark the board position as updated so the board will redrawn
        // If the board is blocked, this will be ignored, but it is set here so
        // once the board unblocks, a required update will happen
        is_updated = true;
    }
    is_modified = true;

    // Unlike gGo/Java, we play the sound even if we are not at the last move
    if (!silent && board->getPlayLocalSound())
        // Play stone or pass sound
        playSound((x == -1 && y == -1) || (x == static_cast<short>(board->getBoardSize()) + 1 &&
                                           y == static_cast<short>(board->getBoardSize()) + 1) ?
                  SOUND_PASS : SOUND_STONE);

    return true;
}

void BoardHandler::playMoveSGF(short x, short y, Color c)
{
    // wxLogDebug(_T("BoardHandler::playMoveSGF %d/%d %d"), x, y, c);

    wxASSERT(sgf_loading);

    // Get correct stone color if not given as parameter
    if (c == STONE_UNDEFINED)
        c = getCurrentTurnColor();

    // Create stone
    Stone stone(x, y, c);

    // Pass or empty move?
    if ((x == 0 && y == 0) || (x == 20 && y == 20))
        game->addMove(stone, Stones());

    // Real move
    else
        // Create unchecked move when loading SGF.
        // The checks will be done later when moving through the game
        game->addMove(stone, Stones(), false);
}

bool BoardHandler::checkMove(const Stone &stone, Stones &captures)
{
    // Check for captures and remove them
    if (checkCaptures(stone, captures))
    {
        wxASSERT(!(captures.empty()));  // Probably superflous
        ConstStonesIterator it;
        for (it=captures.begin(); it!=captures.end(); ++it)
            removeStone(*it);
    }

    // Check if this is a legal move (necassary to remove captures first)
    if (!checkLegal(stone))
    {
        wxLogDebug(_T("*** ILLEGAL MOVE ***"));
        playSound(SOUND_BEEP);
        // Illegal move, restore captures and remove the stone again
        removeStone(stone);
        ConstStonesIterator it;
        for (it=captures.begin(); it!=captures.end(); ++it)
            addStone(*it);
        captures.clear();
        return false;
    }

    return true;
}

bool BoardHandler::checkLegal(const Stone &stone)
{
    Stones group;
    group.push_back(stone);
    bool res = checkPosition(&group);
#ifdef DEBUG_CAPS
    wxLogDebug(_T("checkLegal res = %d"), res);
#endif
    group.clear();
    return !res;
}

bool BoardHandler::checkCaptures(const Stone &stone, Stones &captures)
{
    Color c = reverseColor(stone.getColor());
    Stones *tmp = new Stones();

    // Check position to the east
    Stone s(stone.getX() + 1, stone.getY(), c);
    if (hasPosition(s) && !hasStone(Stone(s.getX(), s.getY(), stone.getColor())))
    {
        tmp->push_back(s);
        if (checkPosition(tmp))
        {
            captures.sort();
            tmp->sort();
            captures.merge(*tmp);
            captures.sort();
            captures.unique();
        }
        tmp->clear();
    }

    // ... and west
    s = Stone(stone.getX() - 1, stone.getY(), c);
    if (hasPosition(s) && !hasStone(Stone(s.getX(), s.getY(), stone.getColor())))
    {
        tmp->push_back(s);
        if (checkPosition(tmp))
        {
            captures.sort();
            tmp->sort();
            captures.merge(*tmp);
            captures.sort();
            captures.unique();
        }
        tmp->clear();
    }

    // ... and north
    s = Stone(stone.getX(), stone.getY() + 1, c);
    if (hasPosition(s) && !hasStone(Stone(s.getX(), s.getY(), stone.getColor())))
    {
        tmp->push_back(Stone(s));
        if (checkPosition(tmp))
        {
            captures.sort();
            tmp->sort();
            captures.merge(*tmp);
            captures.sort();
            captures.unique();
        }
        tmp->clear();
    }

    // ... and south
    s = Stone(stone.getX(), stone.getY() - 1, c);
    if (hasPosition(s) && !hasStone(Stone(s.getX(), s.getY(), stone.getColor())))
    {
        tmp->push_back(s);
        if (checkPosition(tmp))
        {
            captures.sort();
            tmp->sort();
            captures.merge(*tmp);
            captures.sort();
            captures.unique();
        }
    }

    tmp->clear();
    delete tmp;

#ifdef DEBUG_CAPS
    wxLogDebug(wxString::Format(_T("Size of captures: %d"), captures.size()));
#endif

    return captures.size() != 0;
}

bool BoardHandler::checkNeighbour(Stones *group, Stone *stone)
{
#ifdef DEBUG_CAPS
    wxLogDebug(wxString::Format(_T("checkNeighbour for %d/%d %d"), stone->getX(), stone->getY(), stone->getColor()));
#endif

    // Check if the stone is already in our group
    ConstStonesIterator it = find(group->begin(), group->end(), *stone);
    if (it != group->end())
    {
#ifdef DEBUG_CAPS
        wxLogDebug(_T("Already have stone - TRUE"));
#endif
        delete stone;
        return true;
    }

    // There is a stone of same color on the board, add it to the list
    if (hasStone(*stone))
    {
#ifdef DEBUG_CAPS
        wxLogDebug(_T("Have stone - TRUE"));
#endif
        group->push_back(*stone);
        return true;
    }

    // There is a stone, but different color, on the board. Ignore it.
    else if (hasPosition(*stone))
    {
#ifdef DEBUG_CAPS
        wxLogDebug(_T("Have position - TRUE"));
#endif
        delete stone;
        return true;
    }

    // No stone here
    else
    {
#ifdef DEBUG_CAPS
        wxLogDebug(_T("Nothing here - FALSE"));
#endif
        delete stone;
        return false;
    }
}

bool BoardHandler::checkPosition(Stones *group)
{
#ifdef DEBUG_CAPS
    wxLogDebug(wxString::Format(_T("Entering checkPosition() with size of group: %d"), group->size()));
#endif

    // Loop through group
    ConstStonesIterator it;
    for (it=group->begin(); it!=group->end(); ++it)
    {
#ifdef DEBUG_CAPS
        wxLogDebug(wxString::Format(_T("Checking %d/%d %d"), it->getX(), it->getY(), it->getColor()));
#endif

        // Make sure the stone is not off the board
        if (it->getX() < 1 || it->getX() > static_cast<short>(board->getBoardSize()) ||
            it->getY() < 1 || it->getY() > static_cast<short>(board->getBoardSize()))
        {
#ifdef DEBUG_CAPS
            wxLogDebug(_T("Out of board"));
#endif
            group->remove(*it);
            it--;
            continue;
        }

        Stone *s;

        // Check neighbour stone to the east
        if (it->getX() < static_cast<short>(board->getBoardSize()) &&
            !checkNeighbour(group, (s = new Stone(it->getX() + 1, it->getY(), it->getColor()))))
            return false;

        // ... and west
        if (it->getX() > 1 &&
            !checkNeighbour(group, (s = new Stone(it->getX() - 1, it->getY(), it->getColor()))))
            return false;

        // ... and north
        if (it->getY() < static_cast<short>(board->getBoardSize()) &&
            !checkNeighbour(group, (s = new Stone(it->getX(), it->getY() + 1, it->getColor()))))
            return false;

        // ... and south
        if (it->getY() > 1 &&
            !checkNeighbour(group, (s = new Stone(it->getX(), it->getY() - 1, it->getColor()))))
            return false;
    }

#ifdef DEBUG_CAPS
    wxLogDebug(wxString::Format(_T("Exiting checkPosition() with true. Size of group: %d"), group->size()));
#endif
    return true;
}

void BoardHandler::assembleGroup(Stones *group)
{
    // Loop through group
    ConstStonesIterator it;
    for (it=group->begin(); it!=group->end(); ++it)
    {
        // Make sure the stone is not off the board
        if (it->getX() < 1 || it->getX() > static_cast<short>(board->getBoardSize()) ||
            it->getY() < 1 || it->getY() > static_cast<short>(board->getBoardSize()))
        {
            group->remove(*it);
            it--;
            continue;
        }

        Stone *s;

        // Check neighbour stone to the east
        if (it->getX() < static_cast<short>(board->getBoardSize()))
            checkNeighbour(group, (s = new Stone(it->getX() + 1, it->getY(), it->getColor())));

        // ... and west
        if (it->getX() > 1)
            checkNeighbour(group, (s = new Stone(it->getX() - 1, it->getY(), it->getColor())));

        // ... and north
        if (it->getY() < static_cast<short>(board->getBoardSize()))
            checkNeighbour(group, (s = new Stone(it->getX(), it->getY() + 1, it->getColor())));

        // ... and south
        if (it->getY() > 1)
            checkNeighbour(group, (s = new Stone(it->getX(), it->getY() - 1, it->getColor())));
    }
}

bool BoardHandler::nextMove()
{
    // Block while loading SGF
    if (sgf_loading)
        return false;

    // Move forward one move in the game
    Move *m = game->next();

    // If NULL, there was no next move
    if (m == NULL)
        return false;

    // Add the stone of this move to the board
    addStone(m->getPlayedStone());

    // Check unchecked move
    bool have_caps = false;
    if (!m->isChecked())
    {
        Stones captures;
        if (!checkMove(m->getPlayedStone(), captures))
        {
            // This should not happen, except the SGF file is invalid.
            LOG_SGF(wxString::Format(_T("Invalid SGF move at %d/%d"), m->getX(), m->getY()));
            wxFAIL_MSG("Invalid SGF move");
        }
        m->check(captures);
        have_caps = !captures.empty();
        captures.clear();
    }
    else if (!m->getCaptures().empty())
    {
        // Remove captures, if any. If move was unchecked, this already happend in checkMove()
        ConstStonesIterator it;
        for (it=m->getCaptures().begin(); it!=m->getCaptures().end(); ++it)
            removeStone(*it);
        have_caps = true;
    }

    // Set flags for OpenGL scissor test. No scissoring if there are dead stones.
    have_modified_stone = !setupEditedStones(m, NULL) && !have_caps;
    modified_stone = m->getPlayedStone();

    // Mark board as updated
    is_updated = true;

    // Add last move mark
    lastMovePos = m->getPlayedStone();

    // Create ghosts stones indiciating possible variations
    createGhosts();

    return true;
}

bool BoardHandler::previousMove()
{
    // Store current move
    Move *current = game->getCurrentMove();

    // Move backward one move in the game
    Move *m = game->previous(sgf_loading);

    // If NULL, there was no previous move
    if (m == NULL)
        return false;

    // Skip rest, not needed for SGF loading
    if (sgf_loading)
        return true;

    // Remove the stone of current move from the board
    removeStone(current->getPlayedStone());

    // Add the new (previous) move, it might have been removed
    addStone(m->getPlayedStone());

    // Restore captures, if any
    if (!current->getCaptures().empty())
    {
        ConstStonesIterator it;
        for (it=current->getCaptures().begin(); it!=current->getCaptures().end(); ++it)
            addStone(*it);
    }

    // No scissoring when going backwards, too messy
    // But we set the modified_stone so the OpenGL board can update its to-be-removed stone
    // for the next scissor step
    modified_stone = m->getPlayedStone();
    have_modified_stone = false;

    // Add edited stones of this and remove those of the last move
    setupEditedStones(m, current);

    // Mark board as updated
    is_updated = true;

    // Add last move mark
    lastMovePos = m->getPlayedStone();

    // Create ghosts stones indiciating possible variations
    createGhosts();

    return true;
}

bool BoardHandler::firstMove(bool forceUpdate)
{
    // If we are already at first move, abort
    if (!forceUpdate && game->getCurrentNumber() == 0)
    {
        wxLogDebug(_T("Already at first move"));
        return false;
    }

    // Set game to first move
    Move *m = game->first();
    stones.clear();

    // Add edited stones of the new move, but keep the current edited stones on the board.
    // This will show the handicap stones, too.
    setupEditedStones(m, NULL);

    // No scissoring here
    modified_stone = Stone();
    have_modified_stone = false;

    is_updated = true;
    lastMovePos = Position(0, 0);
    createGhosts();
    return true;
}

bool BoardHandler::lastMove()
{
    // Block while loading SGF
    if (sgf_loading)
        return false;

    // Remember current move number
    unsigned short old = game->getCurrentNumber();

    // Loop to last move
    while (nextMove());

    // Unset scissor flag again which were set in nextMove(), but keep the modified_stone
    have_modified_stone = false;

    // If the move number changed, the board position was updated
    is_updated = old != game->getCurrentNumber();
    if (is_updated)
    {
        // Add last move mark
        Move *m = game->getCurrentMove();
        if (m != NULL)
            lastMovePos = m->getPlayedStone();
        else
            lastMovePos = Position(0, 0);

        // Create ghosts stones indiciating possible variations
        createGhosts();
    }
    return is_updated;
}

bool BoardHandler::checkDeadMarks()
{
    bool flag = false;
    if (!stones.empty() && !game->getCurrentMove()->getMarks().empty())
    {
        StonesIterator it;
        for (it=stones.begin(); it != stones.end(); ++it)
        {
            const Mark *mark = game->getCurrentMove()->getMark(*it);
            if (mark != NULL &&
                (mark->getType() == MARK_TERR_BLACK ||
                 mark->getType() == MARK_TERR_WHITE))
            {
                it->SetDead();
                flag = true;
            }
        }
    }
    return flag;
}

void BoardHandler::removeDeadMarks()
{
    if (!stones.empty())
    {
        StonesIterator it;
        for (it=stones.begin(); it != stones.end(); ++it)
            if (it->IsDead())
                it->SetDead(false);
    }
}

bool BoardHandler::setupEditedStones(Move *new_move, Move *old_move)
{
    wxASSERT(new_move != NULL);

    removeDeadMarks();

    Stones s;

    // Check stones of old move
    if (old_move != NULL)
    {
        s = old_move->getEditedStones();
        if (!s.empty())
        {
            ConstStonesIterator it;
            for (it=s.begin(); it != s.end(); ++it)
            {
                // Remove normal black/white stones
                if (it->getColor() == STONE_BLACK || it->getColor() == STONE_WHITE)
                {
                    // We need to take a pointer of the original stone which has a real color
                    const Stone *s = getStone(Position(it->getX(), it->getY()));
                    if (s != NULL)
                        removeStone(*s);
                }
                // Add removed black/white stones
                else if (it->getColor() == STONE_REMOVED_BLACK)
                    addStone(Stone(it->getX(), it->getY(), STONE_BLACK));
                else if (it->getColor() == STONE_REMOVED_WHITE)
                    addStone(Stone(it->getX(), it->getY(), STONE_WHITE));
                else if (it->getColor() == STONE_REMOVED)
                {
                    wxLogDebug(_T("Got STONE_REMOVED! Bad."));  // TODO: Remove
                }
            }
        }
    }

    // Check stones of new move
    s = new_move->getEditedStones();
    if (!s.empty())
    {
        StonesIterator it;
        for (it=s.begin(); it != s.end(); ++it)
        {
            // Add black/white stones
            if (it->getColor() == STONE_BLACK || it->getColor() == STONE_WHITE)
                addStone(*it);
            // Remove others
            else
            {
                const Stone *s = getStone(Position(it->getX(), it->getY()));
                if (s != NULL)
                {
                    removeStone(*s);
                    // Replace STONE_REMOVED (from SGF reading) and replace with STONE_REMOVED_XXX
                    if (it->getColor() == STONE_REMOVED)
                    {
                        new_move->removeStone(*it);
                        new_move->addStone(Stone(it->getX(), it->getY(),
                                                 s->getColor() == STONE_BLACK ? STONE_REMOVED_BLACK :
                                                 STONE_REMOVED_WHITE));
                    }
                }
            }
        }
    }

    return checkDeadMarks();
}

bool BoardHandler::nextVariation()
{
    // Move to next variation
    Move *removed = NULL;
    Move *move = game->nextVar(removed);

    // If NULL, there is no next variation
    if (move == NULL)
    {
        wxLogDebug(_T("No next variation"));
        return false;
    }

    // Setup the new stones
    bool res = setupVariation(move, removed);

    // Create ghosts stones indiciating possible variations
    createGhosts();

    // Scissor flags
    modified_stone = move->getPlayedStone();
    have_modified_stone = false;

    return res;
}

bool BoardHandler::previousVariation()
{
    // Move to previous variation
    Move *removed = NULL;
    Move *move = game->previousVar(removed);

    // If NULL, there is no previous variation
    if (move == NULL)
    {
        wxLogDebug(_T("No previous variation"));
        return false;
    }

    // Setup the new stones
    bool res = setupVariation(move, removed);

    // Create ghosts stones indiciating possible variations
    createGhosts();

    // Scissor flags
    modified_stone = move->getPlayedStone();
    have_modified_stone = false;

    return res;
}

bool BoardHandler::setupVariation(Move *move, Move *removed)
{
    // TODO
    // wxASSERT(move != NULL);
    // Lets see if removed can be NULL, no idea.
    wxASSERT(move != NULL && removed != NULL);

    removeDeadMarks();

    // The flag indicates if there has been any change to the board
    bool flag = false;

    if (removed == NULL)
        wxLogDebug(_T("*** Removed stone is NULL. Wired. *** "));  // TODO, needed ?

    // Remove last stone and restore captures
    if (removed != NULL /* TODO: Needed? */ && removeStone(removed->getPlayedStone()))
    {
        // Check unchecked move
        if (!removed->isChecked())
        {
            Stones captures;
            if (!checkMove(removed->getPlayedStone(), captures))
                return false;
            removed->check(captures);
            captures.clear();
        }

        // Restore captures, if any
        if (!removed->getCaptures().empty())
        {
            ConstStonesIterator it;
            for (it=removed->getCaptures().begin(); it!=removed->getCaptures().end(); ++it)
                addStone(*it);
        }
        flag = true;
    }

    // Add new branched stone and remove captures
    if (addStone(move->getPlayedStone()))
    {
        // Check unchecked move
        if (!move->isChecked())
        {
            Stones captures;
            if (!checkMove(move->getPlayedStone(), captures))
                return false;
            move->check(captures);
            captures.clear();
        }

        // Remove captures, if any
        if (!move->getCaptures().empty())
        {
            ConstStonesIterator it;
            for (it=move->getCaptures().begin(); it!=move->getCaptures().end(); ++it)
                removeStone(*it);
        }
        flag = true;
    }

    checkDeadMarks();

    if (flag)
        // Add last move mark
        lastMovePos = move->getPlayedStone();

    is_updated = flag;
    return flag;
}

const wxString& BoardHandler::getComment(bool at_last) const
{
    if (!at_last)
        return game->getCurrentMove()->getComment();
    return game->getLast()->getComment();
}

void BoardHandler::setComment(const wxString &comment, bool at_last)
{
    // wxLogDebug("BoardHandler::setComment: %s", comment.c_str());
    wxASSERT(game != NULL && game->getCurrentMove() != NULL);
    if (!at_last)
        game->getCurrentMove()->setComment(comment);
    else
        game->getLast()->setComment(comment);
    is_modified = true;
}

void BoardHandler::addEditStone(unsigned short x, unsigned short y, Color c)
{
    // Create and add stone to list
    Stone stone(x, y, c);
    if (c == STONE_BLACK || c == STONE_WHITE)  // Don't add undefined or removed stones
        addStone(stone);  // No checks here, 0/0 and 20/20 are valid values

    // Store stone in Move
    wxASSERT(game->getCurrentMove() != NULL);
    game->getCurrentMove()->addStone(stone);

    // Mark the board position as updated so the board will redrawn
    is_updated = true;
}

void BoardHandler::addEditStoneSGF(unsigned short x, unsigned short y, Color c)
{
    // Store stone in Move
    wxASSERT(game->getCurrentMove() != NULL);
    game->getCurrentMove()->addStone(Stone(x, y, c));
}

void BoardHandler::removeEditStone(unsigned short x, unsigned short y)
{
    // Remove stone from list. We need to get the real stone, else the color mismatches
    const Stone *s = getStone(Position(x, y));
    if (s != NULL)
        removeStone(*s);

    // Add REMOVED_XXX stone to Move
    wxASSERT(game->getCurrentMove() != NULL);
    game->getCurrentMove()->addStone(Stone(x, y,
                                           s == NULL ? STONE_REMOVED : s->getColor() == STONE_BLACK ?
                                           STONE_REMOVED_BLACK : STONE_REMOVED_WHITE));

    // Mark the board position as updated so the board will redrawn
    is_updated = true;
}

void BoardHandler::removeEditStoneSGF(unsigned short x, unsigned short y)
{
    // Add NONE stone to Move
    wxASSERT(game->getCurrentMove() != NULL);
    game->getCurrentMove()->addStone(Stone(x, y, STONE_REMOVED));  // TODO: Which color?
}

const Marks& BoardHandler::getMarks()
{
    // This is quite ugly, but Marks is a container storing _pointers_, not objects. So if
    // we just merge the boardhandler-marks and the move-marks, deleting the boardhandler_marks
    // will also delete the move-marks, but we want to keep the latter. However, as we operate
    // on pointers, this function is reasonable fast. Drawing the marks on the OpenGL board
    // is the slow-down factor, not copying some STL list pointers.
    tmp_marks.clear();
    ConstMarksIterator it;
    if (!marks.empty())
        for (it=marks.begin(); it!=marks.end(); ++it)
            tmp_marks.push_back(*it);
    if (!game->getCurrentMove()->getMarks().empty())
        for (it=game->getCurrentMove()->getMarks().begin(); it!=game->getCurrentMove()->getMarks().end(); ++it)
            tmp_marks.push_back(*it);
    return tmp_marks;
}

bool BoardHandler::addMark(unsigned short x, unsigned short y, MarkType t, wxString txt)
{
    wxASSERT(game->getCurrentMove() != NULL);

    // Get next free letter for the text label if no label text was already given
    if (t == MARK_TEXT && txt.empty())
        txt = game->getCurrentMove()->getNextFreeLetter();

    return game->getCurrentMove()->addMark(x, y, t, txt);
}

bool BoardHandler::removeMark(unsigned short x, unsigned short y, MarkType t)
{
    wxASSERT(game->getCurrentMove() != NULL);
    return game->getCurrentMove()->removeMark(x, y, t);
}

void BoardHandler::removeAllMarks()
{
    wxASSERT(game->getCurrentMove() != NULL);
    game->getCurrentMove()->clearMarks();
    clearMarks();
    is_modified = true;
    is_updated = true;
}

void BoardHandler::createGhosts()
{
    // Clear old storage
    ghosts.clear();

    Move *current = game->getCurrentMove();
    wxASSERT(current != NULL);
    Move *m;

    // Get first son
    if (current->parent != NULL)
        m = current->parent->son;
    else
        m = current;  // First move has no parent

    // Add all brothers to the ghosts list
    do
    {
        if (m->getPlayedStone() == current->getPlayedStone())
            continue;
        if (!hasPosition(m->getPlayedStone()))
            ghosts.push_back(Stone(m->getPlayedStone()));
    } while ((m = m->brother) != NULL);

    // Sort and remove double entries
    ghosts.sort();
    ghosts.unique();
}

bool BoardHandler::setupHandicap(unsigned short handicap, bool increase_move)
{
    wxLogDebug(_T("setupHandicap %d"), handicap);

    switch (board->getBoardSize())
    {
    case 19:  // 19x19
        switch (handicap)
        {
        case 9:
            addEditStone(10, 10, STONE_BLACK);
        case 8:
        case 7:
            if (handicap >= 8)
            {
                addEditStone(10,  4, STONE_BLACK);
                addEditStone(10, 16, STONE_BLACK);
            }
            else
                addEditStone(10, 10, STONE_BLACK);
        case 6:
        case 5:
            if (handicap >= 6)
            {
                addEditStone( 4, 10, STONE_BLACK);
                addEditStone(16, 10, STONE_BLACK);
            }
            else
                addEditStone(10, 10, STONE_BLACK);
        case 4:
            addEditStone(16, 16, STONE_BLACK);
        case 3:
            addEditStone( 4,  4, STONE_BLACK);
        case 2:
            addEditStone(16,  4, STONE_BLACK);
            addEditStone( 4, 16, STONE_BLACK);
            break;
        case 1:
            game->getGameData()->komi = 0.5f;
            break;
        default:
            board->displayMessageBox(wxString::Format(INVALID_HANDICAP_VALUE, handicap), _("Error"),
                                     wxOK | wxICON_ERROR);
            return false;
        }
        break;

    case 13:  // 13x13
        switch (handicap)
        {
        case 9:
            addEditStone(7, 7, STONE_BLACK);
        case 8:
        case 7:
            if (handicap >= 8)
            {
                addEditStone(7, 4, STONE_BLACK);
                addEditStone(7, 10, STONE_BLACK);
            }
            else
                addEditStone(7, 7, STONE_BLACK);
        case 6:
        case 5:
            if (handicap >= 6)
            {
                addEditStone(4, 7, STONE_BLACK);
                addEditStone(10, 7, STONE_BLACK);
            }
            else
                addEditStone(7, 7, STONE_BLACK);
        case 4:
            addEditStone(10, 10, STONE_BLACK);
        case 3:
            addEditStone(4, 4, STONE_BLACK);
        case 2:
            addEditStone(10, 4, STONE_BLACK);
            addEditStone(4, 10, STONE_BLACK);
            break;
        case 1:
            game->getGameData()->komi = 0.5f;
            break;
        default:
            board->displayMessageBox(wxString::Format(INVALID_HANDICAP_VALUE, handicap), _("Error"),
                                     wxOK | wxICON_ERROR);
            return false;
        }
        break;

    case 9:  // 9x9
        switch (handicap)
        {
        case 9:
            addEditStone(5, 5, STONE_BLACK);
        case 8:
        case 7:
            if (handicap >= 8)
            {
                addEditStone(5, 3, STONE_BLACK);
                addEditStone(5, 7, STONE_BLACK);
            }
            else
                addEditStone(5, 5, STONE_BLACK);
        case 6:
        case 5:
            if (handicap >= 6)
            {
                addEditStone(3, 5, STONE_BLACK);
                addEditStone(7, 5, STONE_BLACK);
            }
            else
                addEditStone(5, 5, STONE_BLACK);
        case 4:
            addEditStone(7, 7, STONE_BLACK);
        case 3:
            addEditStone(3, 3, STONE_BLACK);
        case 2:
            addEditStone(7, 3, STONE_BLACK);
            addEditStone(3, 7, STONE_BLACK);
            break;
        case 1:
            game->getGameData()->komi = 0.5f;
            break;
        default:
            board->displayMessageBox(wxString::Format(INVALID_HANDICAP_VALUE, handicap), _("Error"),
                                     wxOK | wxICON_ERROR);
            return false;
        }
        break;
    default:
        board->displayMessageBox(_("Handicap setup is not supported for this board size."), _("Error"),
                                 wxOK | wxICON_ERROR);
        return false;
    }

    game->setCurrentTurnColor(STONE_WHITE);

    // In IGS games the handicap setup is a real move
    if (increase_move)
        game->setCurrentNumber(1);

    is_updated = true;
    return true;
}

bool BoardHandler::loadGame(const wxString &filename, wxWindow *parent, bool is_tmp_filename)
{
    wxFileName fn(filename);
    wxString ext = fn.GetExt();

    // UGF/URI ?
    if (!ext.Cmp("ugf") || !ext.Cmp("ugi"))
        return loadGameUGF(filename, parent);

    // Create parser object
    SGFParser parser(this, parent);

    // Create default GameData object and init a new game
    GameData *data = new GameData();
    newGame(data);

    // Set flag we are now reading SGF to avoid updating the board
    sgf_loading = true;

    // Load SGF. Parser will fill the GameData fields
    bool res = parser.loadSGF(filename, data);

    // Loading ok? Goto first move and update mainframe
    if (res)
    {
        firstMove(true);
        board->updateMainframe();
        // Remember filename
        if (!is_tmp_filename)
            game->getGameData()->filename = filename;
    }
    // Failed? Tell user if there is an error message.
    else if (!parser.getSGFError().empty())
        board->displayMessageBox(parser.getSGFError(), _("Error loading SGF file"), wxOK | wxICON_ERROR);

    // Reset flag
    sgf_loading = false;

    // Mark board as updated but unaltered
    is_updated = true;
    is_modified = false;

    // Clear last move
    lastMovePos = Position(0, 0);

    return res;
}

bool BoardHandler::loadGameUGF(const wxString &filename, wxWindow *parent)
{
    GameData *data = new GameData();
    newGame(data);

    sgf_loading = true;

    bool res = loadUGF(filename, data, this);
    if (res)
    {
        wxLogDebug("UGF loaded from %s.", filename.c_str());
         firstMove(true);
         board->updateMainframe();
    }
    // TODO: Some error message if loading failed

    sgf_loading = false;
    is_updated = true;
    is_modified = false;
    lastMovePos = Position(0, 0);
    return res;
}

bool BoardHandler::saveGame(const wxString &filename, bool dont_remember)
{
    SGFWriter writer;

    bool res = writer.saveSGF(filename, game);

    if (res)
    {
        // Remember filename
        if (!dont_remember)
            game->getGameData()->filename = filename;

        is_modified = false;
    }

    return res;
}

void BoardHandler::deleteCurrentMove()
{
    // Store current move
    Move *current = game->getCurrentMove();

    // Move to previous move (without markers)
    Move *m = game->previous(true);

    // Remove the stone of current move from the board
    removeStone(current->getPlayedStone());

    // If NULL, there was no previous move and we are at the root node.
    if (m != NULL)
    {
        // Add the new (previous) move, it might have been removed
        addStone(m->getPlayedStone());

        // Restore captures, if any
        if (!current->getCaptures().empty())
        {
            ConstStonesIterator it;
            for (it=current->getCaptures().begin(); it!=current->getCaptures().end(); ++it)
                addStone(*it);
        }

        // Add edited stones of this and remove those of the last move
        setupEditedStones(m, current);

        // Add last move mark
        lastMovePos = m->getPlayedStone();

        is_modified = true;
        modified_stone = m->getPlayedStone();
    }
    else
    {
        lastMovePos = Position(0, 0);
        modified_stone = Stone();
    }

    // Finally delete the move. Relocate possible pointers to siblings.
    deleteNode(current);

    // No scissoring
    have_modified_stone = false;

    // Mark board as updated
    is_updated = true;

    // Create ghosts stones indiciating possible variations
    createGhosts();

    // Update GUI interface
    board->updateMainframe();
}

void BoardHandler::deleteNode(Move *move)
{
    if (move == NULL)
        move = game->getCurrentMove();
    wxASSERT(move != NULL);
    if (move == NULL)
        return;

    Move *remember = NULL;
    Move *remSon = NULL;

    // This is not the root node?
    if (move->parent != NULL)
    {
        remember = move->parent;

        // Remember son of parent if its not the move to be deleted.
        // Then check for the brothers and fix the pointer connections, if we
        // delete a node with brothers. (It gets ugly now...)
        // This is cut&paste from gGo/Java, it obviously does work, although this
        // code doesn't look like it. :*)
        if (remember->son == move)               // This son is our move to be deleted?
        {
            if (remember->son->brother != NULL)  // This son has a brother?
                remSon = remember->son->brother; // Reset pointer
        }
        else // No, the son is not our move
        {
            remSon = remember->son;
            Move *tmp = remSon;
            Move *oldTmp = tmp;

            do { // Loop through all brothers until we find our move
                if (tmp == move)
                {
                    if (move->brother != NULL)           // Our move has a brother?
                        oldTmp->brother = move->brother; // Then set the previous move brother
                    else                                 // to brother of our move
                        oldTmp->brother = NULL;          // No brother found.
                    break;
                }
                oldTmp = tmp;
            } while ((tmp = tmp->brother) != NULL);
        }
    }

    // We are deleting the root node. However, root might have siblings, too.
    else if (game->hasPrevBrother(move))
    {
        wxLogDebug("ROOT - HAS PREV");
        Move *dummy;
        remember = game->previousVar(dummy);
        wxASSERT(remember != NULL);
        if (move->brother != NULL)
            remember->brother = move->brother;
        else
            remember->brother = NULL;
    }
    else if (move->brother != NULL)
    {
        wxLogDebug("ROOT - HAS NEXT");
        Move *dummy;
        remember = game->nextVar(dummy);
        wxASSERT(dummy == move && remember != NULL);
        game->setRoot(remember);  // remember is now root.
    }
    else
    {
        wxLogDebug("ROOT - HAS NONE");
        game->reset();  // first and only move. We delete everything.
        stones.clear();
        ghosts.clear();
        return;
    }

    if (move->son != NULL)
        game->deleteTree(move->son); // Traverse-clear the tree after our move (to avoid brothers)
    game->setCurrentMove(remember);  // Set current move to previous move
    if (move->son == NULL)
        game->makeCurrentLast();     // Relocate last move pointer if necassary
    remember->son = remSon;          // Reset son pointer
    remember->marker = NULL;         // Forget marker
    delete move;                     // Finally delete the node
}

void BoardHandler::numberMoves()
{
    // Move from current upwards to root and set a number mark
    Move *m = game->getCurrentMove();
    wxASSERT(m != NULL);
    if (m == NULL || m->getNumber() == 0)
        return;

    do {
        // TODO: Use mark type number
        marks.push_back(Mark::createMark(m->getX(), m->getY(), MARK_TEXT, wxString::Format("%u", m->getNumber())));
    } while ((m = m->parent) != NULL && m->getNumber() != 0);

    // No last move marker with numbering
    lastMovePos = Position(0, 0);

    // Mark board modified
    is_updated = true;
    is_modified = true;
}

void BoardHandler::markVariations(bool sons)
{
    Move *m = game->getCurrentMove();
    wxASSERT(m != NULL);
    if (m == NULL)
        return;

    // Mark all sons of current move
    if (sons && m->son != NULL)
    {
        unsigned short i = 0;
        m = m->son;
        do {
            wxChar txt = 'A' + i;
            marks.push_back(Mark::createMark(m->getX(), m->getY(), MARK_TEXT, txt));
            i++;
        } while ((m = m->brother) != NULL);
    }
    // Mark all brothers of current move
    else if (!sons && m->parent != NULL)
    {
        Move *tmp = m->parent->son;
        if (tmp == NULL)
            return;

        unsigned short i = 1;
        do {
            if (tmp != m)
                // TODO: Use number marks for brothers
                marks.push_back(Mark::createMark(tmp->getX(), tmp->getY(), MARK_TEXT, wxString::Format("%u", i++)));
        } while ((tmp = tmp->brother) != NULL);
    }
    else
        // Nothing changed
        return;

    // Mark board modified
    is_updated = true;
    is_modified = true;
}

void BoardHandler::setEditParameter(EditParam param, bool value)
{
    switch (param)
    {
    case EDIT_PARAM_NUMBER_MOVES:
        number_moves = value;
        break;
    case EDIT_PARAM_MARK_BROTHERS:
        mark_brothers = value;
        break;
    case EDIT_PARAM_MARK_SONS:
        mark_sons = value;
        break;
    default:
        wxFAIL_MSG(_T("Invalid EditParam value."));
        return;
    }

    processEditParams();

    // Mark board modified
    is_updated = true;
    is_modified = true;
}

void BoardHandler::processEditParams(int params)
{
    clearMarks();

    if (number_moves && (params | EDIT_PARAM_NUMBER_MOVES || params | EDIT_PARAM_ALL))
        numberMoves();
    if (mark_brothers && (params | EDIT_PARAM_MARK_BROTHERS || params | EDIT_PARAM_ALL))
        markVariations(false);
    if (mark_sons && (params | EDIT_PARAM_MARK_SONS || params | EDIT_PARAM_ALL))
        markVariations(true);
}

void BoardHandler::displayTerritoryFromMatrix(Matrix *matrix)
{
    unsigned short sz=matrix->getSize();
    short val;
    for (unsigned short i=0; i<sz; i++)
        for (unsigned short j=0; j<sz; j++)
            if ((val = matrix->at(i, j)) != STONE_UNDEFINED)
                // Add territory marks to the temporary BoardHandler mark storage
                marks.push_back(Mark::createMark(i+1, j+1,
                                                 val == STONE_BLACK || val == MARK_TERR_BLACK ? MARK_TERR_BLACK : MARK_TERR_WHITE));
}

void BoardHandler::score()
{
    // Get rid of the last move marker (unlikely after passing, but possible)
    lastMovePos = Position(0, 0);

    // Remove all territory marks
    clearMarks();

    // Create a new matrix and copy the current position over.
    Matrix matrix(board->getBoardSize());
    StonesIterator it;
    for (it=stones.begin(); it!=stones.end(); ++it)
        matrix.set(it->getX() - 1, it->getY() - 1,
                   // Multiply dead stones with -1, stones alive in seki with MARK_SEKI (10)
                   it->getColor() * (it->IsDead() ? -1 : it->IsSeki() ? MARK_SEKI : 1));

    // Score it
    Matrix *mark_matrix = matrix.countScore();

    // Display the scoring result
    displayTerritoryFromMatrix(mark_matrix);
    delete mark_matrix;
    mark_matrix = NULL;

    // Count dead stones
    int dead_white = 0, dead_black = 0;
    ConstStonesIterator cit;
    for (cit=stones.begin(); cit!=stones.end(); ++cit)
    {
        if (cit->IsDead())
        {
            if (cit->getColor() == STONE_WHITE)
                dead_white ++;
            else if (cit->getColor() == STONE_BLACK)
                dead_black ++;
        }
    }

    // Add captures
    int caps_white = game->getCurrentMove()->getCapsWhite() + dead_black;
    int caps_black = game->getCurrentMove()->getCapsBlack() + dead_white;

    // Calculate final score
    float final_white = static_cast<float>(matrix.getTerrWhite() + caps_white) + game->getGameData()->komi;
    int final_black = matrix.getTerrBlack() + caps_black;
    score_result = final_white - static_cast<float>(final_black);

    // Display values in sidebar
    board->displayScoreResult(matrix.getTerrWhite(),
                              caps_white,
                              final_white,
                              matrix.getTerrBlack(),
                              caps_black,
                              final_black,
                              matrix.getDame());

    // Mark board modified
    is_updated = true;
    is_modified = true;
}

bool BoardHandler::markStoneDeadOrSeki(const Position &pos, bool dead)
{
    // wxLogDebug("markStoneDeadOrSeki: %d %d %d", pos.getX(), pos.getY(), dead);

    // We have any stones?
    if (stones.empty())
        return false;

    StonesIterator stone = find(stones.begin(), stones.end(), pos);
    if (stone == stones.end())
        return false;  // No stone where the user clicked

    // Create the group this stone belongs to.
    Stones group;
    group.push_back(*stone);
    assembleGroup(&group);

    // Mark all stones in group as dead
    StonesIterator it;
    for (it=group.begin(); it!=group.end(); ++it)
    {
        Stone *s = const_cast<Stone*>(getStone(*it));
        if (dead)
            s->SetDead(!s->IsDead());
        else
            s->SetSeki(!s->IsSeki());
    }
    group.clear();

    // Call scoring process so the territory marks are displayed
    score();

    return true;
}

void BoardHandler::finishScore(bool displayResult)
{
    // Unmark seki stones again, but keep dead marks
    if (!stones.empty())
    {
        StonesIterator it;
        for (it=stones.begin(); it != stones.end(); ++it)
            it->SetSeki(false);
    }

    // Move territory marks from temporary BoardHandler mark storage into Move mark storage
    // If this is an observed IGS game (dispalyResult == false), then we need to mark dead stones
    if (!marks.empty())
    {
        ConstMarksIterator it;
        for (it=marks.begin(); it!=marks.end(); ++it)
        {
            // Theoretically we can only have territory marks in the storage now.
            game->getCurrentMove()->addMark(*it);

            if (!displayResult && hasPosition(Position((*it)->getX(), (*it)->getY())))
                const_cast<Stone*>(getStone(Position((*it)->getX(), (*it)->getY())))->SetDead();
        }
        marks.clear();  // Not clearMarks() as the Mark pointer now belongs to Move
    }

    if (displayResult)
    {
        // Display result in comment textfield
        board->displayComment(
            wxString::Format(_("%s wins by %.1f points."),
                             score_result > 0.0f ? _("White") : _("Black"),
                             score_result > 0.0f ? score_result : -score_result));
    }

    // Set SGF result in GameData
    game->getGameData()->result =
        wxString::Format(_T("%s+%.1f"),
                         score_result > 0.0f ? _("W") : _("B"),
                         score_result > 0.0f ? score_result : -score_result);

    // Mark board modified
    is_updated = true;
    is_modified = true;
}
