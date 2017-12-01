/*
 * autoupdater.h
 */

#ifndef AUTOUPDATER_H
#define AUTOUPDATER_H

#ifdef __GNUG__
#pragma interface "autoupdater.h"
#endif

#include "igs_player.h"

/**
 * Database keeping track of players on IGS.
 * Information about players is provided from IGS either via the
 * "toggle quiet false" messages and when the user refreshes the
 * player table. Players from this input are collected in this
 * storage class for various usage, like providing ranks for shouts,
 * tells and matches.
 * @ingroup igs
 */
class AutoUpdater
{
public:
    /** Constructor */
    AutoUpdater(IGSConnection *con);

    /** Destructor */
    ~AutoUpdater();

    /** Parse the "21 ..." line */
    void doParse(const wxString &toParse);

    /**
     * Gets a player object from the table if existing.
     * @param name Name of the player to search
     * @returns The IGSPlayer object or NULL if the name does not exist in the table
     */
    const IGSPlayer* getPlayer(const wxString &name) const;

    /** Update and merge the player list with the incoming one from player table. */
    void updatePlayerList(const PlayerList &player_list);

private:
    void addPlayer(const wxString &name, const wxString &rank);
    void removePlayer(const wxString &name);

    IGSConnection *connection;
    PlayerList playerList;
};

#endif
