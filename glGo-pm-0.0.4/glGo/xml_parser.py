#! /usr/bin/python

#
# xml_parser.py
#
# $Id: xml_parser.py,v 1.1 2003/11/29 04:32:52 peter Exp $
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


import os, __init__
from xml.sax import make_parser, parse, SAXParseException
from xml.sax.handler import ContentHandler, EntityResolver
import xmlcallbacks

STONE_UNKNOWN = 0
STONE_WHITE   = 1
STONE_BLACK   = 2

# This is the MarkType enum from marks.h. The order is important.
mark_types = [ "none", "circle", "square", "triangle", "cross", "text", "terr_black", "terr_white" ]

variation_stack = []
in_var = 0

#
# Convert "K10" etc. to glGo coordinates
# Returns: A tuple (x, y) in glGo coordinate space
#
def convert_pos(s, size):
    x = ord(s[0]) - 65
    if ord(s[0]) < ord('J'):
        x += 1
    return ( x, size - int(s[1:]) + 1 )

#
# SAX2 ContentHandler for Jago XML files.
# The format is specified in go.dtd and explained on the Jago webpage.
# See http://www.rene-grothmann.de/jago/
#
class JagoContentHandler(ContentHandler):
    def startDocument(self):
        # print "startDocument"
        self.boardsize = 19  # In case no BoardSize element is given
        global variation_stack, in_var
        variation_stack = []
        in_var = 0
        self.content = ""
        self.comment = ""

    def endDocument(self):
        # print "endDocument"
        pass

    def startElement(self, name, attrs):
        # print "startElement", name

        # Move
        if name == "Black" or name == "White":
            if name == "Black":
                col = STONE_BLACK
            else:
                col = STONE_WHITE
            try:
                doMove( (convert_pos(attrs.getValue("at"), self.boardsize),
                         col,
                         int(attrs.getValue("number"))) )
            except KeyError:
                if __init__.info_level >= 1:
                    print "Invalid XML tag: %s" % name

        # Variation start
        elif name == "Variation":
            doVariation()

        # Mark
        elif name == "Mark":
            label = None
            try:
                # Text mark?
                label = attrs.getValue("label")
                type = "text"
            except KeyError:
                try:
                    # Territory mark?
                    type = attrs.getValue("territory")
                    type = "terr_" + type
                except KeyError:
                    # Others
                    try:
                        type = attrs.getValue("type")
                    except KeyError:
                        type = "cross"
            try:
                doMark(convert_pos(attrs.getValue("at"), self.boardsize), type, label)
            except KeyError:
                if __init__.info_level >= 1:
                    print "Invalid XML tag: %s" % name

        # Add
        elif name == "AddWhite" or name == "AddBlack":
            if name == "AddWhite":
                col = STONE_WHITE
            else:
                col = STONE_BLACK
            try:
                doAddStone(convert_pos(attrs.getValue("at"), self.boardsize), col)
            except KeyError:
                if __init__.info_level >= 1:
                    print "Invalid XML tag: %s" % name

        # Delete
        elif name == "Delete":
            try:
                doDeleteStone(convert_pos(attrs.getValue("at"), self.boardsize))
            except KeyError:
                if __init__.info_level >= 1:
                    print "Invalid XML tag: %s" % name

    def endElement(self, name):
        # print "endElement", name

        # Comment
        if name == "Comment":
            doComment(self.comment)
            self.comment = ""
        elif name == "P":
            self.comment += self.content

        # Variation end
        elif name == "Variation":
            doVariation(end=True)

        # Nodes end
        elif name == "Nodes":
            process_var(variation_stack)

        # Information elements (aka game header)
        elif name == "BoardSize":
            self.boardsize = int(self.content)
            doHeader(name, self.content)
        elif name == "BlackPlayer" or \
             name == "BlackRank" or \
             name == "WhitePlayer" or \
             name == "WhiteRank" or \
             name == "Komi" or \
             name == "Handicap" or \
             name == "Result" or \
             name == "Date" or \
             name == "Copyright":
            doHeader(name, self.content)

        self.content = ""

    def characters(self, content):
        # print "characters", content
        if self.content.strip():
            self.content += content
        else:
            self.content = content

#
# EntityResolver to locate the go.dtd file in the shared glGo directory
#
class JagoEntityResolver(EntityResolver):
    def __init__(self, shared_path):
        self.shared_path = shared_path

    def resolveEntity(self, publicId, systemId):
        if systemId != "go.dtd":
            return systemId
        dtd_file = os.path.join(self.shared_path, "go.dtd")
        if __init__.info_level >= 1:
            print "Loading DTD from:", dtd_file
        if not os.path.exists(dtd_file):
            if __init__.info_level >= 1:
                print "File does not exist!"
            return "go.dtd"
        return os.path.join(self.shared_path, "go.dtd")

def doHeader(key, value):
    # print "doHeader", key, value
    xmlcallbacks.xml_do_header(key, value)

#
# Move is a tuple of (pos, col, number)
#
def doMove(move):
    # print "doMove", move
    l = len(variation_stack)-1
    v = variation_stack
    for i in range(0, in_var):
        v = v[l]
        l = len(v)-1
    v.append(move)

def doVariation(end=False):
    # print "doVariation", end
    global in_var
    if not end:
        in_var += 1
        l = len(variation_stack)-1
        v = variation_stack
        for i in range(1, in_var):
            v = v[l]
            l = len(v)-1
        v.append([])
    else:
        in_var -= 1

def process_var(var):
    todo_stack = []
    counter = [0]

    # Walk through the stack and play first-level moves and
    # put second-level variations on the todo_stack
    for v in var:
        if type(v) == list:
            todo_stack.append(v)
            counter.append(0)
        else:
            xmlcallbacks.xml_do_move(v)
            counter[len(counter)-1] += 1

    # Now recursively process the variations in the todo_stack
    while todo_stack:
        t = todo_stack.pop()
        n = counter.pop()
        # Move backwards to the beginning of this variation
        xmlcallbacks.xml_do_navigate_previous(n+1)
        process_var(t)

    # Move backwards the number of total moves made
    xmlcallbacks.xml_do_navigate_previous(counter[len(counter)-1]-1)

def doComment(comment):
    # print "doComment:", comment
    xmlcallbacks.xml_do_comment(comment)

def doMark(pos, type, label=None):
    # print "doMark:", pos, type, label
    # Translate "cross" into '1', see the MarkType enum in marks.h
    try:
        type_i = mark_types.index(type)
    except IndexError:
        type_i = 1
    if not label:
        label = ""  # Cannot pass None
    xmlcallbacks.xml_do_mark(pos, type_i, label)

def doAddStone(pos, col):
    # print "doAddStone:", pos, col
    xmlcallbacks.xml_do_add_stone(pos, col)

def doDeleteStone(pos):
    # print "doDeleteStone:", pos
    xmlcallbacks.xml_do_delete_stone(pos)

#
# Main entry function to load a XML file.
# Params: filename - File to load
#         shared_path - glGo shared directory where the go.dtd file is found
# Returns: 0 on success, -1 on error
#
def parseXML(filename, shared_path):
    if not filename or not os.path.exists(filename):
        if __init__.info_level >= 1:
            print "No such file: %s" % filename
        return -1

    parser = make_parser()
    parser.setContentHandler(JagoContentHandler())
    parser.setEntityResolver(JagoEntityResolver(shared_path))
    try:
        parser.parse(filename)
    except SAXParseException, (msg, exception, locator):
        if __init__.info_level >= 1:
            print "Sax Exception:", msg, exception, locator
        return -1
    except IOError, e:
        if __init__.info_level >= 1:
            print "IOError:", e
        return -1
    return 0


#
# Debug stuff
#

def main():
    import sys
    if len(sys.argv) < 2:
        print "Usage is: xml_parser.py <filename>"
        sys.exit(0)
    parseXML(sys.argv[1], "../share")

if __name__ == '__main__':
    main()
