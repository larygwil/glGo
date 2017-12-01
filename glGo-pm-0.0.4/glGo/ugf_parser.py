#! /usr/bin/python

#
# ugf_parser.py
#
# $Id: ugf_parser.py,v 1.4 2003/11/27 10:18:20 peter Exp $
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


import re, os, string, parser, ugf, __init__

STONE_UNDEFINED = 0
STONE_WHITE     = 1
STONE_BLACK     = 2

boardsize = 19
comments = []
marks = []

def getSection(toParse, start, end, testEOF=False):
    # Find first occurance of the elements in tuple end
    pos = len(toParse)
    endrex = end[0]
    for e in end:
        p = toParse.find(e[2:len(e)-2])
        if p != -1 and p < pos:
            pos = p
            endrex = e
    if __init__.info_level >= 1:
        print "Endres = " + endrex

    if testEOF:
        rs = r"%s(\n.*)+(%s|$)" % (start, endrex)
    else:
        rs = r"%s(\n.*)+(%s)" % (start, endrex)
    if __init__.info_level >= 1:
        print "rs = " + rs

    rex = re.compile(rs)
    so = rex.search(toParse)
    if not so:
        if __init__.info_level >= 1:
            print "No section between %s and %s" % (start, end)
        return None
    return so.group()

def parse(ugf):
    # Header section
    toParse = getSection(ugf, "\[Header\]", ("\[Data\]", "\[Remote\]"))
    if not toParse:
        return -1
    rex = re.compile(r"(([a-zA-Z]+)=(.*\n))")
    headers = rex.findall(toParse)
    for h in headers:
        parseHeader(h[1], h[2].strip())

    # Figure section
    # We parse this before the Data section to collect comments and marks.
    toParse = getSection(ugf, "\[Figure\]", ("",), testEOF=True)
    if toParse:  # I guess it's no error if figure is missing. No idea.
        rex = re.compile(r"(.Text,[0-9]+\n)(((?![.]E).*\n)*)(.EndText)")
        texts = rex.findall(toParse)
        for t in texts:
            parseFigure(t[0], t[1])

    # Data section
    toParse = getSection(ugf, "\[Data\]", ("\[Figure\]",), testEOF=True)
    if not toParse:
        return -1
    rex = re.compile(r"([A-Z]{2,2}),([BW][12]),([0-9]+),([0-9])+")
    moves = rex.findall(toParse)
    if not moves:
        if __init__.info_level >= 1:
            print 'No moves'
        return -1
    for m in moves:
        parseMove(m)

    # We missed comments or marks?
    if comments and __init__.info_level >= 1:
        print "Missed some comments!"
    if marks and __init__.info_level >= 1:
        print "Missed some marks!"

    return 0

def parseHeader(key, value):
    # print "Header: %s  -  %s" % (key, value)

    if key == "Size":
        boardsize = int(value)
        if __init__.debug_level >= 1:
            print "Set boardsize to %d" % boardsize
    elif key == "Date":
        # First entry in date is enough
        pos = value.find(",")
        if pos != -1:
            value = value[0:pos]
    elif key == "Hdcp":
        pos = value.find(",")
        if pos != -1:
            try:
                handi = value[0:pos]
                komi = value[pos+1:]
                # Cheat a seperate key, we cannot send sequences
                ugf.ugf_do_header("Komi", komi)
            except:
                return
    elif key == "PlayerW" or key == "PlayerB":
        pos1 = value.find(",")
        if pos1 != -1:
            name = value[0:pos1]
            pos2 = value.find(",", pos1+1)
            if pos2 != -1:
                rank = value[pos1+1:pos2]
                if key == "PlayerW":
                    which = "WhiteRank"
                else:
                    which = "BlackRank"
                ugf.ugf_do_header(which, rank)  # Cheat again
            value = name
    elif key == "Winner":
        result = ""
        pos1 = value.find(",")
        if pos1 != -1:
            result = value[0:pos1] + "+"
            pos2 = value.find(",", pos1+1)
            pos3 = value.find(".", pos1+1)
            pos2 = max(pos2, pos3)
            if pos2 == -1:
                reason = value[pos1+1:]
                if reason == "C":
                    result += "R"
                elif reason == "T":
                    result += "T"
            else:
                result += value[pos1+1:]
        if __init__.debug_level >= 1:
            print "Result: " + result
        value = result
    elif key == "Title":
        value = value.strip(",")

    # Security check against buffer overflow. These embedded images
    # in PandaNet magazine files are shocking. :)
    if len(value) > 64:
        value = value[0:64]
        print "Cutting value length"
    ugf.ugf_do_header(key, value)

def parseMove(move):
    # Position, Color, Move number, Thinking time
    if move[0] == 'YA' or move[0] == 'YB':
        pos = (0, 0)  # Pass
    else:
        # glGo coordinate scheme is different than UGF coords
        pos = (ord(move[0][0]) - 64, boardsize - (ord(move[0][1]) - 65))
    if move[1][0] == 'W':
        col = STONE_WHITE
    elif move[1][0] == 'B':
        col = STONE_BLACK
    else:
        col = STONE_NONE
    move_num = int(move[2])
    doMove( (pos, col, move_num, int(move[3])) )

    # Now check if we have comments or marks in this move, those were
    # previously parsed and are now in the comments and marks lists.
    while comments and comments[0][0] == move_num:
        c = comments.pop(0)
        doComment(c[1])
    while marks and marks[0][0] == move_num:
        m = marks.pop(0)
        doMark(m[1][0], m[1][1])

def parseFigure(figure, toParse):
    # Get move number
    rex = re.compile(r"[.]Text,([0-9]+).*")
    so = rex.match(figure)
    if not so or len(so.groups()) != 1:
        if __init__.info_level >= 1:
            print "Failed to parse figure: %s" % figure
        return
    move_num = int(so.group(1))

    # Parse comment
    rex = re.compile(r"((?![.][#]).*\n)*")
    so = rex.search(toParse)
    if so:
        comments.append((move_num, so.group().strip()))

    # Parse marks
    rex = re.compile(r"([.][#],[0-9]+,[0-9]+,[A-Z]+)+")
    so = rex.search(toParse)
    marks = rex.findall(toParse)
    for m in marks:
        parseMark(m[3:], move_num)

def parseMark(mark, move_num):
    if not mark:
        return

    rex = re.compile(r"([0-9]+),([0-9]+),([A-Z]+)")
    try:
        m = rex.findall(mark)[0]
        marks.append((move_num, ((int(m[0]), int(m[1])), m[2])))
    except IndexError:
        if __init__.info_level >= 1:
            print "Failed to parse mark: %s" % mark
        return

def doMove(move):
    if __init__.debug_level >= 1:
        print "Move:", move
    ugf.ugf_do_move(move)

def doComment(comment):
    if __init__.debug_level >= 1:
        print "Comment:", comment
    ugf.ugf_do_comment(comment)

def doMark(pos, text):
    if __init__.debug_level >= 1:
        print "Mark:", pos, text
    ugf.ugf_do_mark(pos, text)

def parseUGF(filename):
    if not filename or not os.path.exists(filename):
        if __init__.info_level >= 1:
            print "No such file: %s" % filename
        return -1

    # Clear lists if we reused the interpreter
    global comments, marks
    comments = []
    marks = []

    ugf = parser.loadFile(filename)
    if not ugf:
        if __init__.info_level >= 1:
            print "No file content"
        return -1

    # Get rid of Windows carriage crap
    ugf = string.replace(ugf, "\r\n", "\n")
    return parse(ugf)


#
# Debug stuff
#

def main():
    # Enable debugging
    __init__.debug_level = 1
    __init__.info_level = 1

    import sys
    if len(sys.argv) < 2:
        print "Usage is: ugf_parser.py <filename>"
        sys.exit(0)
    parseUGF(sys.argv[1])

if __name__ == '__main__':
    main()

