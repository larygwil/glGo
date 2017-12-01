/*
 * games_table.h
 *
 * $Id: games_table.h,v 1.6 2003/10/02 14:18:40 peter Exp $
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
 * @ingroup igsgui
 *
 * This file contains the classes GamesGrid and GamesTable which both
 * form the table listing the games on IGS
 */

#ifndef GAMES_TABLE_H
#define GAMES_TABLE_H

#ifdef __GNUG__
#pragma interface "games_table.h"
#endif

#include <wx/grid.h>
#include "igs_game.h"

class IGSMainFrame;
class wxGauge;


// ------------------------------------------------------------------------
//                             Class GamesGrid
// ------------------------------------------------------------------------

/**
 * The grid embedded into the GamesTable.
 * @ingroup igsgui
 */
class GamesGrid : public wxGrid
{
public:
    /** Constructor. This creates the table layout. */
    GamesGrid(wxWindow *parent);
};


// ------------------------------------------------------------------------
//                             Class GamesTable
// ------------------------------------------------------------------------

/**
 * The table displaying the games list.
 * @ingroup igsgui
 */
class GamesTable : public wxFrame
{
public:
    /**
     * Constructor
     * @param parent Pointer to the IGSMainframe parent
     */
    GamesTable(IGSMainFrame *parent);

    /* Destructor */
    ~GamesTable();

    /** Callback for close event and the close button */
    void OnClose(wxCloseEvent& event);

    /** Callback for refresh button */
    void OnRefresh(wxCommandEvent& WXUNUSED(event)) { sendRefresh(); }

    /** Callback on double-click on a row. This will start observing the game. */
    void OnDblLeft(wxGridEvent& event);

    /**
     * Left click on a colum.
     * If the user clicks on the new column, we it will sort ascending. If he
     * clicks on the same column again, ascending/descending will be toggled.
     */
    void OnLabelLeft(wxGridEvent& event);

    /** Right click on a colum. This will always sort descending. */
    void OnLabelRight(wxGridEvent& event);

    /** Update the games list from the given list. Called after clicking Refresh. */
    void updateGamesList(const GamesList &games_list);

    /** Update the gauge while parsing the games list */
    void updateGauge(int value);

    /** Check if the table is empty */
    bool Empty() const { return !hasData; }

    /** Send "games" command */
    void sendRefresh();

private:
    void displayGamesList();

    IGSMainFrame *parentFrame;
    GamesGrid *grid;
    wxGauge *gauge;
    wxStaticText *total;
    GamesList table_games_list;
    int current_sorted_col;
    bool ascending_sort, hasData;

DECLARE_EVENT_TABLE()
};

#endif
