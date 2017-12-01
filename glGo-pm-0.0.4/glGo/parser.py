#! /usr/bin/python

#
# parser.py
#
# $Id: parser.py,v 1.5 2003/11/29 07:31:37 peter Exp $
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


import os, fnmatch, time, cPickle, __init__
from string import join, find

header_tag_list = [ 'PW', 'WR', 'PB', 'BR', 'SZ', 'KM', 'RE', 'DT' ]
INDEX_FILENAME = 'index'

class Game:
    def __init__(self, filename, headers):
        self.filename = filename
        self.headers = headers

class GameList:
    def __init__(self):
        self.games = {}

    def add_game(self, game):
        if self.games.has_key(hash(game.filename)):
            return 0
        self.games[hash(game.filename)] = game
        return 1

    def remove_game(self, key):
        if self.games.has_key(key):
            del self.games[key]

    def load(self, config_path=None):
        if config_path:
            fname = os.path.join(config_path, INDEX_FILENAME)
            self.config_path = config_path
        else:
            fname = INDEX_FILENAME
            self.config_path = None
        try:
            file = open(fname, 'r')
        except IOError:
            if __init__.info_level >= 1:
                print 'Failed to open index.'
            return
        try:
            self.games = cPickle.load(file)
        except:
            if __init__.info_level >= 1:
                print 'Failed to load index.'
            file.close()
        if __init__.debug_level >= 1:
            print 'Loaded index from %s' % file.name

    def save(self, config_path=None):
        if config_path:
            fname = os.path.join(config_path, INDEX_FILENAME)
        elif self.config_path:
            fname = os.path.join(self.config_path, INDEX_FILENAME)
        else:
            fname = INDEX_FILENAME
        try:
            file = open(fname, 'w')
        except IOError:
            if __init__.info_level >= 1:
                print 'Failed to open index for writing.'
            return
        try:
            cPickle.dump(self.games, file)
        except:
            if __init__.info_level >= 1:
                print 'Failed to save index.'
        file.close()
        if __init__.debug_level >= 1:
            print 'Saved index to %s' % file.name

    def search_by_player(self, playername):
        results = []
        for g in self.games.values():
            if g.headers['PW'] == playername or g.headers['PB'] == playername:
                results.append( (g.headers, g.filename) )
        return results

    def list_games(self):
        results = []
        for g in self.games.values():
            results.append( (g.headers, g.filename) )
        return results

class Rank:
    def __init__(self, s):
        self.rank = self.convert_rank(s)

    def convert_rank(self, s):
        # For the meaning of the rank encoding see igs_rank.h file

        # No length -> unknown rank
        if not s:
            return 54

        # NR or NR*
        elif s.startswith('NR'):
            return 53

        # ??? or ???*
        elif s.startswith('???'):
            return 52

        # Kyus
        elif s.find('k') != -1:
            return 21 + int(s[0:s.find('k')])

        # Dans
        elif s.find('d') != -1:
            return 22 - int(s[0:s.find('d')])

        # Pros
        elif s.find('p') != -1:
            return 10 - int(s[0:s.find('p')])

        # Shit happened
        else:
            return 54

    def cmp(self, other):
        return cmp(self.rank, other.rank)

def convert_rank_to_string(rank):
    if rank == 54:
        return "";
    if rank == 53:
        return "NR"
    if rank == 52:
        return "???"
    if rank >= 22 and rank <= 51:
        return str(rank - 21) + "k"
    if rank >= 10 and rank <= 21:
        return str((rank - 22) * -1) + "d"
    if rank >= 0 and rank <= 9:
        return str((rank - 10) * -1) + "p"
    return "NR";

gamelist = GameList()
last_search = None

def loadFile(fileName):
    try:
        file = open(fileName, 'r')
        list = []
        for line in file.xreadlines( ):
            list.append(line)
        file.close()
        return join(list, '')
    except IOError:
        if __init__.info_level >= 1:
            print 'IOError: Failed to read from file ' + fileName
        return None
    except:
        if __init__.info_level >= 1:
            print 'Failed to read from file ' + fileName
        return None

def parseSGFHeader(toParse):
    header_tag_dict = { }
    for tag in header_tag_list:
        header_tag_dict[tag] = parseTag(toParse, tag)
    return header_tag_dict

def parseTag(toParse, tag):
    if tag == None or tag == '':
        return

    l = len(tag)
    pos1 = find(toParse, tag + '[')
    if pos1 == -1:
        return None
    pos2 = find(toParse, ']', pos1)
    return toParse[pos1+l+1:pos2]

def do_parse(filename):
    sgf = loadFile(filename)
    if not sgf:
        return 0
    game = Game(filename, parseSGFHeader(sgf))
    return gamelist.add_game(game)

def parse(filename):
    # Single file
    if not os.path.isdir(filename):
        if not fnmatch.fnmatch(filename, '*.sgf'):
            return
        return do_parse(filename)

    # Directory
    else:
        try:
            files = os.listdir(filename)
        except OSError:
            if __init__.info_level >= 1:
                print 'Failed to read from ' + filename
            return None
        counter = 0
        for f in files:
            fname = os.path.join(filename, f)
            # Recursive directory read
            if os.path.isdir(fname):
                counter += parse(fname)
                continue
            if not fnmatch.fnmatch(f, '*.sgf'):
                if __init__.debug_level >= 1:
                    print 'No sgf: ' + f
                continue
            counter += do_parse(fname)
        return counter

def find_by_player(playername):
    global last_search
    # Check if we cached this list and the cache is no older than 3 seconds
    if last_search and last_search[0] == playername and last_search[1] - time.time() < 3:
        if __init__.debug_level >= 1:
            print 'Using cached gameslist for %s' % playername
        return last_search[2]
    gl = gamelist.search_by_player(playername)
    # Remember last list, as when opening the player dialog this
    # is called twice in a row: For the games list and for the graph
    last_search = (playername, time.time(), gl)
    return gl

def create_index(directory):
    added = parse(directory)
    gamelist.save()
    return added

def list_index():
    return gamelist.list_games()

def init_gamelist(config_path=None):
    gamelist.load(config_path)

def rank_sort_fun(a, b):
    r1 = Rank(a[0])
    r2 = Rank(b[0])
    return r1.cmp(r2)

def sort_games(games, sort_by='PW', reverse=False):
    if type(sort_by) == int:
        sort_by = header_tag_list[sort_by]
    tmplist = [ (games[i][0][sort_by], i) for i in range(0, len(games)) ]
    if sort_by == 'WR' or sort_by == 'BR':
        # Ranks need a special sort fun
        sortfun = rank_sort_fun
    else:
        sortfun = None
    tmplist.sort(sortfun)
    if reverse:
        tmplist.reverse()
    games = [ games[e[1]] for e in tmplist ]
    del tmplist
    return games

def cleanup_games():
    counter = 0
    tmplist = gamelist.games
    for k, v in tmplist.items():
        if not os.path.exists(v.filename):
            gamelist.remove_game(k)
            if __init__.info_level >= 1:
                print 'Removing %s' % v.filename
            counter += 1
    if counter:
        gamelist.save()
    return counter


#
# Debug stuff following.
#
def main():
    import sys
    if len(sys.argv) < 2:
        print 'Usage is: parser <directory|filename>'
        sys.exit()
    init_gamelist()
    # Convert to normalized absolute path, else the resulting index
    # won't have the full paths to the SGF files
    filename = os.path.abspath(sys.argv[1])
    added = create_index(filename)
    print 'Added %d games.' % added
    gamelist.save()

if __name__ == '__main__':
    main()
