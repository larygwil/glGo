/*
 * player_table.h
 *
 * $Id: player_table.h,v 1.12 2003/11/19 14:30:40 peter Exp $
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
 * This file contains the classes PlayerGrid and PlayerTable which both
 * form the table listing the players on IGS
 */

#ifndef PLAYER_TABLE_H
#define PLAYER_TABLE_H

#ifdef __GNUG__
#pragma interface "player_table.h"
#endif

#include <wx/grid.h>
#include "igs_player.h"

class IGSMainFrame;
class wxGauge;


// ------------------------------------------------------------------------
//                             Class PlayerGrid
// ------------------------------------------------------------------------

/**
 * The grid embedded into the PlayerTable.
 * @ingroup igsgui
 */
class PlayerGrid : public wxGrid
{
public:
    /** Constructor. This creates the table layout. */
    PlayerGrid(wxWindow *parent);
};


// ------------------------------------------------------------------------
//                             Class PlayerTable
// ------------------------------------------------------------------------

/**
 * The table displaying the player list.
 * @ingroup igsgui
 */
class PlayerTable : public wxFrame
{
public:
    /**
     * Constructor
     * @param parent Pointer to the IGSMainframe parent
     */
    PlayerTable(IGSMainFrame *parent);

    /* Destructor */
    ~PlayerTable();

    /** Callback for close event and the close button */
    void OnClose(wxCloseEvent& event);

    /** Callback for refresh button. Refreshes the list. */
    void OnRefresh(wxCommandEvent& WXUNUSED(event)) { sendRefresh(); }

    /** Callback for available button. Toggle filter and redisplay list. */
    void OnAvailable(wxCommandEvent& WXUNUSED(event));

    /** Callback for friends button. Toggle filter and redisplay list. */
    void OnFriends(wxCommandEvent& WXUNUSED(event));

    /** Callback for info button. Does the same as a double-click on a row. */
    void OnInfo(wxCommandEvent& WXUNUSED(event));

    /** Callback for play button. Open a match dialog.
     * @todo Precalculate white/black according to rank. Restore saved match setup. And this is currently repetitive code. */
    void OnPlay(wxCommandEvent& WXUNUSED(event));

    /** Callback on double-click on a row. Sends "stats name" command. */
    void OnDblLeft(wxGridEvent& event);

    /** Right click on a row. Opens the popup menu. */
    void OnPopup(wxGridEvent& event);

    /**
     * Left click on a colum.
     * If the user clicks on the new column, we it will sort ascending. If he
     * clicks on the same column again, ascending/descending will be toggled.
     */
    void OnLabelLeft(wxGridEvent& event);

    /** Right click on a colum. This will always sort descending. */
    void OnLabelRight(wxGridEvent& event);

    /** Update the player list from the given list. Called after clicking Refresh. */
    void updatePlayerList(const PlayerList &player_list);

    /** Update the gauge while parsing the player list */
    void updateGauge(int value);

    /** Check if the table is empty */
    bool Empty() const { return !hasData; }

    /** Send "user <upper>-<lower>" */
    void sendRefresh();

    /**
     * Gets a player object from the table if existing.
     * @param name Name of the player to search
     * @returns The IGSPlayer object or NULL if the name does not exist in the table
     */
    IGSPlayer* getPlayer(const wxString &name) const;

    /** Popup menu - Stats */
    void OnPopupStats(wxCommandEvent& WXUNUSED(event));

    /** Popup menu - Tell */
    void OnPopupTell(wxCommandEvent& WXUNUSED(event));

    /** Popup menu - Match
     * @todo repetitive code here */
    void OnPopupMatch(wxCommandEvent& WXUNUSED(event));

    void OnPopupFriend(wxCommandEvent& WXUNUSED(event));
    void OnPopupNeutral(wxCommandEvent& WXUNUSED(event));
    void OnPopupBozo(wxCommandEvent& WXUNUSED(event));

private:
    void displayPlayerList();
    int getSelectedRow();
    void sendStats(const wxString &name);
    void updatePlayerStatus(const wxString &name, int status, wxColour *col);
    void getRankSelection(wxString &lower, wxString &upper);

    IGSMainFrame *parentFrame;
    PlayerGrid *grid;
    wxGauge *gauge;
    wxStaticText *total;
    PlayerList table_player_list;
    int current_sorted_col;
    bool ascending_sort, hasData, available, friends;
    wxMenu *popup;
    wxString popup_name;
    int popup_row;

DECLARE_EVENT_TABLE()
};

#endif
