/*
 * game.cpp
 *
 * $Id: game.cpp,v 1.26 2003/10/14 15:39:49 peter Exp $
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
#pragma implementation "game.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/log.h>
#endif

#include "stones.h"
#include "move.h"
#include "game.h"
#include "gamedata.h"

Game::Game()
{
    game_data = NULL;
    root = current = last = NULL;
    // Create new game with a default GameData instance
    newGame(new GameData());
}

Game::~Game()
{
    deleteTree();
    delete game_data;
}

void Game::reset()
{
    deleteTree();
    black_turn = game_data->handicap < 2;
    root = new Move();
    current = last = root;
    current_number = total_number = 0;
}

void Game::newGame(GameData *data)
{
    wxASSERT(data != NULL);

    wxLogDebug(wxString::Format(_T("Komi: %.1f"), data->komi));

    // Delete old game_data object if we have new one
    if (game_data != NULL)
        delete game_data;

    // Set game_data pointer to new instance
    game_data = data;

    // Init game
    reset();
}

void Game::setGameData(GameData *data)
{
    wxASSERT(data != NULL);

    // Replace current game_data pointer with new pointer data
    if (game_data != NULL)
        delete game_data;
    game_data = data;
}

void Game::addMove(const Stone &stone, const Stones& captures, bool check, bool force_last)
{
    // Add captures if adding checked moves (not in SGF loading)
    unsigned short caps_white = 0;
    unsigned short caps_black = 0;
    if (check)
    {
        Move *m = !force_last ? current : last;
        caps_white = m->getCapsWhite();
        caps_black = m->getCapsBlack();
        if (stone.getColor() == STONE_WHITE)
            caps_white += captures.size();
        else
            caps_black += captures.size();
    }

    // Create move
    Move *move = new Move(stone, captures, !force_last ? current_number+1 : total_number+1,
                          caps_white, caps_black);
    if (check)
        move->check();

    // Set parent of the new move
    Move *tmp_current = !force_last ? current : last;
    move->parent = tmp_current;

    // Check if we need to start a variation.
    if (tmp_current->son)  // New branch
    {
        // Set new move as last brother of the current moves son
        Move *tmp = tmp_current->son;
        while (tmp->brother)
            tmp = tmp->brother;
        tmp->brother = move;
    }
    else  // Not a new branch
        // Add new move as son of current
        tmp_current->son = move;

    // Add new move as current if not forcing last
    if (!force_last)
    {
        current = move;
        black_turn = !black_turn;
        current_number ++;
    }
    // Update last and total in any case
    last = move;
    total_number ++;  // TODO: Reduce when IGS/GTP undo occurs
}

Move* Game::next()
{
    if (current->son == NULL)
    {
        wxLogDebug(_T("No next move"));
        return NULL;
    }

    if (current->marker == NULL)
        // No marker, simply take the main son
        current = current->son;
    else
        // Marker set, use this to go the remembered path in the tree
        current = current->marker;

    current->parent->marker = current;  // Parents remembers this move we went to
    current_number ++;
    black_turn = current->getPlayedStone().getColor() == STONE_WHITE;
    return current;
}

Move* Game::previous(bool nomarker)
{
    if (current_number == 0 || current == root)
    {
        wxLogDebug(_T("No previous move"));
        return NULL;
    }

    if (!nomarker)
        current->parent->marker = current;  // Remember the son we came from
    current = current->parent;              // Move up in the tree
    current_number --;
    if (current_number)
        black_turn = current->getPlayedStone().getColor() == STONE_WHITE;
    else
        black_turn = game_data->handicap < 2;
    return current;
}

Move* Game::first()
{
    current = root;
    current_number = 0;
    black_turn = game_data->handicap < 2;
    return current;
}

Move* Game::nextVar(Move *&removed)
{
    if (current->brother == NULL)
    {
        wxLogDebug(_T("No brother"));
        return NULL;
    }

    // Store removed stone
    removed = current;

    current = current->brother;
    return current;
}

Move* Game::previousVar(Move *&removed)
{
    if (current->parent == NULL)
    {
        wxLogDebug(_T("No parent"));
        return NULL;
    }

    if (current->parent->son == current)
    {
        wxLogDebug(_T("current->parent->son == current"));
        return NULL;
    }

    // Store removed stone
    removed = current;

    // Find previous brother
    Move *m = current->parent->son;
    while (m->brother && m->brother != current)
        m = m->brother;

    current = m;
    return current;
}

void Game::deleteTree(Move *move)
{
    if (move == NULL)
        move = root;

    list<Move*> stack;
    list<Move*> trash;
    Move *n = NULL;

    // Traverse the tree and drop every node into stack trash
    stack.push_back(move);

    int counter = 0;

    while (!stack.empty())
    {
        counter ++;

        n = stack.back();
        stack.pop_back();
        if (n != NULL)
        {
            trash.push_back(n);
            if (n->brother != NULL)
                stack.push_back(n->brother);
            if (n->son != NULL)
                stack.push_back(n->son);
        }
    }

    // Clear trash.
    list<Move*>::iterator it;
    for (it=trash.begin(); it!=trash.end(); ++it)
        delete *it;
    trash.clear();
}

bool Game::hasPrevBrother(Move *m)
{
    wxASSERT(m != NULL);
    if (m == NULL)
        return false;

    Move *tmp;
    if (m->parent == NULL)
    {
        if (m == root)
            return false;
        else
            tmp = root;
    }
    else
        tmp = m->parent->son;
    return tmp != m;
}
