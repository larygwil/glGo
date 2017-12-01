/*
 * autoupdater.cpp
 */

#ifdef __GNUG__
#pragma implementation "autoupdater.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/log.h>
#endif

#include "igs_connection.h"
#include "autoupdater.h"

AutoUpdater::AutoUpdater(IGSConnection *con)
    : connection(con)
{
    wxASSERT(connection != NULL);
}

AutoUpdater::~AutoUpdater()
{
    wxLogDebug("~AutoUpdater()");

    playerList.Empty();
}

void AutoUpdater::doParse(const wxString &toParse)
{
    // wxLogDebug("AutoUpdater::doParse(): %s", toParse.c_str());

    // Zotan has disconnected
    if (toParse.Find("has disconnected") != -1)
    {
        removePlayer(toParse.Mid(1, toParse.Find(' ') - 1));
    }

    // Zotan [10k*] has connected.
    else if (toParse.Find("has connected.") != -1)
    {
        int pos = toParse.Find('[');
        if (pos == -1)
            return;
        addPlayer(toParse.Mid(1, pos - 2),
                  toParse.Mid(pos + 1, toParse.Find(']') - pos - 1).Trim());
    }
}

const IGSPlayer* AutoUpdater::getPlayer(const wxString &name) const
{
    for (int i=0, sz=playerList.GetCount(); i<sz; i++)
        if (!name.Cmp(playerList.Item(i).name))
            return &(playerList.Item(i));
    return NULL;
}

void AutoUpdater::addPlayer(const wxString &name, const wxString &rank)
{
    // wxLogDebug("addPlayer: <%s> <%s>", name.c_str(), rank.c_str());

    playerList.Add(IGSPlayer(name, rank));
}

void AutoUpdater::removePlayer(const wxString &name)
{
    // wxLogDebug("removePlayer: <%s>", name.c_str());

    const IGSPlayer *p = getPlayer(name);
    int ind = playerList.Index(*p);
    if (ind != wxNOT_FOUND)
        playerList.RemoveAt(ind);  // IGSPlayer object deleted by wxObjArray
}

void AutoUpdater::updatePlayerList(const PlayerList &player_list)
{
    for (int i=0, sz=player_list.GetCount(); i<sz; i++)
        if (getPlayer(player_list.Item(i).name) == NULL)
            playerList.Add(IGSPlayer(player_list.Item(i)));

    wxLogDebug("Size of AutoUpdater list: %d", playerList.GetCount());
}
