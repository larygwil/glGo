#
# commandline.py
#
# $Id: commandline.py,v 1.5 2003/11/14 19:49:00 peter Exp $
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


import getopt, sys, __init__
from playerdb import *

def usage():
    print """Usage: players.py [--db path/to/database] [ACTION]...

Arguments required for long options are also required for short options.

OPTIONS
  -d, --db                                use given database file instead of
                                          the default location
  --flags                                 show custom flags
                                          only applies to --list action

ACTIONS
  -h, --help                              display this help
  -c, --copyright                         display copyright information
  -v, --version                           display version
  -l, --list                              display version
  -f, --friends                           display friends
  -b, --bozos                             display bozos
  -a, --addfriend                         add given name as friend
  -o, --addbozo                           add given name as bozo
  -r, --remove                            remove the given name

If no action is given, list is used by default.

If an already existing player is added again to the database, the status
will be changed, or nothing happens if you add an existing friend as friend
again.

The default database location <glGo_config_path>/players.db.
The glGo config path is $HOME/.glGo on Linux and $HOME/glGo on Windows.

Examples

  players.py -a tweet
    Adds tweet as friend to the default database

  players.py -o Zotan
    Adds Zotan as bozo to the default database

  players.py -r malf
    Removed malf from the default database

  players.py --db /foo/bar/somefile.db -f
    Lists all friends using /foo/bar/somefile.db as database"""

def version():
    print __init__.packagename + ' playermanager ' + __init__.version
    print """Written by Peter Strempel

Copyright (c) 2003, Peter Strempel
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version."""

def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:],
                                   "hlfba:o:r:d:vc",
                                   ["help", "list", "friends", "bozos", "addfriend=", "addbozo=", "remove=",
                                    "db=", "version", "cleanup", "flags"])
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    # Default to -l
    if not opts:
        opts = [("-l", "")]
    # Add -l if only -d/--db is given
    elif len(opts) == 1 and (opts[0][0] == '-d' or opts[0][0] == '--db'):
        opts.append(("-l", ""))

    db = None
    show_flags = False

    # Eval opts and args
    # First run
    for o, a in opts:
        if o in ("-h", "--help"):
            usage()
            sys.exit(0)
        if o in ("-v", "--version"):
            version()
            sys.exit(0)
        if o in ("-c", "--cleanup"):
            if load_db(db) == -1:
                return
            res = cleanup_db()
            if res == 0:
                print 'Database cleanup done.'
            else:
                print 'Nothing to do.'
            sys.exit(0)
        if o in ("-d", "--db"):
            db = a
        if o in ("", "--flags"):
            show_flags = True

    # Second run
    for o, a in opts:
        if o in ("-l", "--list"):
            if load_db(db) == -1:
                return
            print_all(show_flags)
        if o in ("-f", "--friends"):
            if load_db(db) == -1:
                return
            print "Friends:"
            print join(list_friends(), '\n')
        if o in ("-b", "--bozos"):
            if load_db(db) == -1:
                return
            print "Bozos:"
            print join(list_bozos(), '\n')
        if o in ("-a", "--addfriend"):
            load_db(db)
            add_player(a, PLAYER_STATUS_FRIEND)
            save_db(db)
        if o in ("-o", "--addbozo"):
            load_db(db)
            add_player(a, PLAYER_STATUS_BOZO)
            save_db(db)
        if o in ("-r", "--remove"):
            load_db(db)
            remove_player(a)
            save_db(db)

def print_all(show_flags=False):
    for p in get_players():
        if p.get_status() == PLAYER_STATUS_FRIEND:
            s = 'Friend'
        elif p.get_status() == PLAYER_STATUS_BOZO:
            s = 'Bozo'
        else:
            s = ''
        if show_flags:
            print '%20s%12s     ' % (p.get_name(), s), p.get_custom_flags()
        else:
            print '%20s%12s' % (p.get_name(), s)
