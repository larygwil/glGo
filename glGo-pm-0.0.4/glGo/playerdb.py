#! /usr/bin/python

#
# playerdb.py
#
# $Id: playerdb.py,v 1.13 2003/11/28 20:02:04 peter Exp $
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


import cPickle, time, os.path, sys
from string import join
from __init__ import *

players = []
db_filename = None
last_loaded = 0

class Player:
    def __init__(self, name, status=PLAYER_STATUS_NEUTRAL):
        self.name = name
        self.status = status
        self.comment = ''
        self.custom_flags = []
        for i in range(0, NUMBER_CUSTOM_FLAGS):
            self.custom_flags.append(False)

    def __getstate__(self):
        d = self.__dict__
        try:
            d['custom_flags'] = self.custom_flags
        except AttributeError:
            pass
        return d

    def __setstate__(self, data):
        self.__dict__ = data

    def get_name(self):
        return self.name

    def get_status(self):
        return self.status

    def set_status(self, status):
        self.status = status

    def get_comment(self):
        try:
            return self.comment
        except AttributeError:
            return ''

    def set_comment(self, txt):
        self.comment = txt

    def get_custom_flags(self):
        try:
            return self.custom_flags
        except AttributeError:
            return None

    def get_custom_flag(self, n):
        try:
            return self.custom_flags[n]
        except AttributeError:
            return False
        except IndexError:
            return False

    def set_custom_flag(self, n, b):
        if b:
            real_bool = True
        else:
            real_bool = False
        try:
            self.custom_flags[n] = real_bool
        except AttributeError:
            self.custom_flags = []
            for i in range(0, NUMBER_CUSTOM_FLAGS):
                self.custom_flags.append(False)
            self.custom_flags[n] = real_bool
        except IndexError:
            if info_level >= 1:
                print 'IndexError in set_custom_flag'
            pass

    def has_data(self):
        if self.status != PLAYER_STATUS_NEUTRAL or self.comment:
            return True
        try:
            for f in self.custom_flags:
                if f:
                    return True
        except AttributeError:
            return False
        return False

def get_config_path():
    # Linux: $HOME/.glGo
    if os.name == 'posix':
        return os.path.expanduser('~/.glGo')
    # Windows
    elif os.name == 'nt':
        # Windows NT/2K/XP: $HOME\glGo
        if sys.getwindowsversion()[3] == 2:
            return os.path.expanduser('~\glGo')
        # Windows 9x/ME: <glGo-path>
        else:
            import registry
            return registry.get_glGo_install_path()
        # Others (unsupported)
    else:
        return ''

def find_database():
    db_dir = get_config_path()
    # Use current directory if glGo config path does not exist
    if not os.path.exists(db_dir):
        db_dir = ''
    return os.path.join(db_dir, DEFAULT_DB_FILE)

def load_db(filename=None):
    global players
    if not filename:
        filename = find_database()
    try:
        file = open(filename, 'r')
    except IOError:
        if info_level >= 1:
            print 'Failed to open file "%s" for reading.' % filename
        return -1
    if debug_level >= 1:
        print 'Opened db file %s.' % filename
    try:
        players = cPickle.load(file)
    except:
        if info_level >= 1:
            print 'Failed to load database.'
        file.close()
        return -1
    file.close()
    global db_filename, last_loaded
    db_filename = filename
    last_loaded = int(time.time())
    if info_level >= 1:
        print 'Loaded database from file "%s".' % filename
    return 0

def save_db(filename=None):
    if not filename:
        if db_filename:
            filename = db_filename
        else:
            filename = find_database()
    try:
        file = open(filename, 'w')
    except IOError:
        if info_level >= 1:
            print 'Failed to open file "%s" for writing.' % filename
        return -1
    try:
        cPickle.dump(players, file)
    except:
        if info_level >= 1:
            print 'Failed to dump database.'
        file.close()
        return -1
    file.close()
    global last_loaded
    last_loaded = int(time.time())
    if info_level >= 1:
        print 'Saved database to file "%s".' % filename
    return 0

def check_db_reload():
    if not db_filename:
        return 0
    if os.stat(db_filename)[8] > last_loaded:  # mtime
        load_db(db_filename)
        return 1
    return 0

def get_players():
    return players

# Add a player or set status for an existing player
# Returns:
# 0 - Player does not exist, added
# 1 - Player exists, status changed to either friend or neutral
# 2 - Player exists, status not changed
# 3 - Player exists, status changed to neutral
def add_player(name, status=PLAYER_STATUS_NEUTRAL):
    p = get_player(name)
    if p:
        if p.get_status() != status:
            if debug_level >= 1:
                print 'Already have %s, set to %d' % (name, status)
            p.set_status(status)
            if status != PLAYER_STATUS_NEUTRAL:
                return 1
            return 3
        return 2

    p = Player(name, status)
    players.append(p)
    if info_level >= 1:
        print 'Added %s as %d' % (name, status)
    return 0

# Removes a player
# Returns:
#  0 - Player removed
# -1 - Player not found, nothing changed
def remove_player(player):
    if not player:
        return
    if type(player) == str:
        player = get_player(player)
        if not player:
            if debug_level >= 1:
                print 'Dont have that player'
            return -1
    if info_level >= 1:
        print 'Removing %s' % player.get_name()
    try:
        players.remove(player)
    except ValueError:
        if debug_level >= 1:
            print 'Dont have that player'
            return -1
    return 0

def get_player(name):
    for p in players:
        if p.get_name() == name:
            return p
    return None

def get_player_status(name):
    p = get_player(name)
    if not p:
        return -1
    return p.get_status()

def list_players(status=None):
    l = []
    for p in players:
        if not status or p.get_status() == status:
            l.append(p.get_name())
    return l

def list_friends():
    l = list_players(PLAYER_STATUS_FRIEND)
    l.sort()
    return l

def list_bozos():
    l = list_players(PLAYER_STATUS_BOZO)
    l.sort()
    return l

def set_player_comment(name, comment):
    p = get_player(name)
    if not p:
        # Create a new player if the comment is not empty
        if not comment:
            return
        p = Player(name)
        players.append(p)
    p.set_comment(comment)

def get_player_comment(name):
    p = get_player(name)
    if not p:
        return ''
    return p.get_comment()

# Gets a custom flag of a player
# Returns:
#  -1 - Player not found
# 0/1 - Flag
def get_player_flag(name, flag):
    p = get_player(name)
    if not p:
        return -1
    return p.get_custom_flag(flag)

def set_player_flag(name, flag, value):
    p = get_player(name)
    if not p:
        # Create a new player if the flag is True
        if not value:
            return
        p = Player(name)
        players.append(p)
    p.set_custom_flag(flag, value)

# Cleanup database: Remove neutral players without data
# Returns:
# -1   - Empty database
#  0   - Nothing to cleanup
#  1.. - Number of removed entries
def cleanup_db():
    global players
    if not players:
        return -1
    old_size = len(players)
    players = [p for p in players if p.has_data()]
    new_size = len(players)
    if new_size != old_size:
        save_db()
        return old_size - new_size
    return 0

if __name__ == '__main__':
    from commandline import main
    main()
