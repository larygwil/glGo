#! /usr/bin/python

#
# plotter.py
#
# $Id: plotter.py,v 1.2 2003/11/28 20:03:27 peter Exp $
#
# glGo, a prototype for a 3D Goban based on wxWindows, OpenGL and SDL.
# Copyright (c) 2003, Peter Strempel <pstrempel@gmx.de>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

import parser, re, datetime, __init__
from parser import Game, Rank

ALL_GAMES = 0
WHITE_GAMES = 1
BLACK_GAMES = 2

games = []

#
# Convert "03" to "2003" and "93" to "1993".
# Range is 1920 - 2019
#
def fix_year(year):
    if int(year) < 20:
        return "20" + year
    if int(year) < 100:
        return "19" + year
    return year

#
# Parse a date and return a tuple (year, month, day)
# Accepted formats are:
# * 2003-11-26
# * 2003/11/26
# * 26.11.2003
#
def parse_date(toParse):
    if not toParse:
        return None

    # Try 2003-11-26 or 2003/11/26
    rex = re.compile(r"([0-9]{2,4})[-/]([0-9]{1,2})[-/]([0-9]{1,2})")
    so = rex.match(toParse)
    if so and len(so.groups()) == 3:
        return ( fix_year(so.group(1)), so.group(2), so.group(3) )

    # Failed? Try 26.11.2003
    rex = re.compile(r"([0-9]{1,2})[.]([0-9]{1,2})[.]([0-9]{2,4})")
    so = rex.match(toParse)
    if so and len(so.groups()) == 3:
        return ( fix_year(so.group(3)), so.group(2), so.group(1) )

    if __init__.info_level >= 1:
        print 'Failed to parse date string "%s".' % toParse
    return None

#
# Create three lists of tuples ( DATE, RANK ) where DATE is a tuple (YEAR, MONTH, DAY)
# One list for games as white, one for games as black, one for all games. Each list is
# sorted by date.
# Returns: Tuple with these three lists: (ALL_GAMES, WHITE_GAMES, BLACK_GAMES)
#
def collect_games(name):
    if not name:
        return None

    games = parser.find_by_player(name)
    if not games:
        return None

    # Gather the players black/white games
    white_games = [ ( parse_date(g[0]['DT']), g[0]['WR'], g[0]['RE'] ) for g in games if g[0]['PW'] == name ]
    black_games = [ ( parse_date(g[0]['DT']), g[0]['BR'], g[0]['RE'] ) for g in games if g[0]['PB'] == name ]

    white_games.sort()
    black_games.sort()
    all_games = white_games + black_games
    all_games.sort()

    return ( all_games, white_games, black_games )

#
# Init the data for the given player.
# The purpose of this function is to avoid calling collect_games three times
# for each "which" paramater value (ALL_GAMES, WHITE_GAMES, BLACK_GAMES) in
# create_data(). Instead the games are only read once and stored in a global
# variable for reuse.
# Returns: 0 on success, -1 on failure
#
def init_data(name):
    global games
    games = collect_games(name)
    if not games:
        if __init__.info_level >= 1:
            print "init_data() failed for " + name
        return -1
    return 0

#
# Create the data for the plotting. This requires a previous call
# to init_data(name).
# Which can be ALL_GAMES, WHITE_GAMES or BLACK_GAMES
# Returns: List of tuples ( time, rank ) or None if an error occured
#
def create_data(which=0):
    if not games:
        if __init__.info_level >= 1:
            print "No games"
        return None

    # Create list of dates from one of the three game lists
    dates = [ datetime.date(int(g[0][0]), int(g[0][1]), int(g[0][2])) for g in games[which] if g[0] ]

    # Create list of time deltas
    deltas = [ (dates[i] - dates[i-1]).days for i in range(1, len(dates)) ]
    deltas.insert(0, 0)

    # Create list of ranks and convert a rank into an integer
    ranks = [ 54 - Rank(g[1]).rank for g in games[which] if g[0] ]

    # Now convert the list of deltas into absolute time intervals
    added_deltas = []
    diff = 0
    for d in deltas:
        diff += d
        added_deltas.append(diff)

    # Finally create a list of points. A point is a tuple of ( time_interval, rank )
    points = [ (added_deltas[i], r) for i, r in enumerate(ranks) ]
    points.sort()
    if __init__.info_level >= 1:
        print "Create data ok", which
    return points

#
# Create game statistics. This requires a previous call to init_data(name)
# Returns:
#   The tuple ( wins, losses, wins_white, losses_white, wins_black, losses_black )
#   or None if an error occured
#
def create_statistics():
    if not games:
        if __init__.info_level >= 1:
            print "No games"
        return None

    wins = 0
    losses = 0
    wins_white = 0
    losses_white = 0
    wins_black = 0
    losses_black = 0

    # Games as white
    for g in games[1]:
        if not g[2]:
            continue
        if g[2].startswith('W+'):
            wins += 1
            wins_white += 1
        elif g[2].startswith('B+'):
            losses += 1
            losses_white += 1

    # Games as black
    for g in games[2]:
        if not g[2]:
            continue
        if g[2].startswith('B+'):
            wins += 1
            wins_black += 1
        elif g[2].startswith('W+'):
            losses += 1
            losses_black += 1

    return ( wins, losses, wins_white, losses_white, wins_black, losses_black )

#
# Debug stuff
#
if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print 'Usage is: plotter <playername>'
        sys.exit()
    parser.init_gamelist()
    init_data(sys.argv[1])
    print create_data()
    print create_statistics()
