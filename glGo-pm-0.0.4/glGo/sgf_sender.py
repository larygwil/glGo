#! /usr/bin/python

#
# open_sgf.py
#
# $Id: sgf_sender.py,v 1.6 2003/11/25 15:36:35 peter Exp $
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


import socket, os, sys
from __init__ import info_level

# Default port
GLGO_PORT = 9998

# Default tmpfile for Unix domain sockets
GLGO_TMPFILE = "/tmp/glGo_socket"

def send_via_socket(filename):
    if os.name == 'posix':
        # Use Unix domain socket on POSIX, much safer
        try:
            s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            s.connect(GLGO_TMPFILE)
        except socket.error, (errno, strerror):
            if info_level >= 1:
                print 'Failed to connect to glGo:', strerror
            return -1
    else:
        # Use Internet domain socket on Windows. Worse, but I can't help it
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect(("localhost", GLGO_PORT))
        except socket.error, (errno, strerror):
            if info_level >= 1:
                print 'Failed to connect to glGo:', strerror
            return -1

    # Send SGF filename and close socket again
    s.send(filename)
    s.close()
    return 0

def open_sgf_file(filename, dirname=None, spawn=False):
    if not filename:
        if info_level >= 1:
            print 'No filename given, aborting.'
        return -1

    # Convert to normalized absolute path, helpful for commandline usage
    filename = os.path.abspath(filename)

    if not os.path.exists(filename):
        if info_level >= 1:
            print 'File "%s" does not exist, aborting.' % filename
        return -1

    # First try opening a socket to a running glGo
    res = send_via_socket(filename)

    # Failed? Start new glGo process as 'glGo <filename>'
    if res == -1:
        try:
            if not spawn:
                if os.name == 'nt':
                    exec_fname = './glGo'
                    # We need to cd to the glGo directory on Windows
                    if not os.path.exists('./glGo.exe'):
                        if not dirname:
                            dirname = os.path.dirname(sys.argv[0])
                        os.chdir(dirname)
                    # Now wrap filename into " if the dreaded user opens a SGF from something
                    # like C:\Documents and Settings\foobar\My Go Games\
                    filename = '"%s"' % filename
                else:
                    # Why are things so much easier on Linux? :)
                    exec_fname = 'glGo'
                # Run glGo and replace current process
                os.execlp(exec_fname, 'glGo', filename)
            else:
                # Run glGo in new process
                if os.name == 'posix':
                    # On Posix glGo is looked up in PATH
                    os.spawnlp(os.P_NOWAIT, 'glGo', 'glGo', filename)
                elif os.name == 'nt':
                    # spawnlp does not exist on sucky-OS
                    olddir = os.getcwd()
                    os.chdir(dirname)
                    os.spawnl(os.P_NOWAIT, os.path.join(dirname, 'glGo'), 'glGo', filename)
                    os.chdir(olddir)
                else:
                    # Unsupported OS. We are screwed. :)
                    pass
        except OSError, (errno, strerror):
            if info_level >= 1:
                print 'Failed to start glGo process:', strerror
            return -1

    return 0

def main():
    try:
        open_sgf_file(sys.argv[1])
    except IndexError:
        print 'Usage is: open_sgf <filename>'

if __name__ == '__main__':
    main()
