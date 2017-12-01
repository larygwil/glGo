#!/usr/bin/python

#
# gamesgrid.py
#
# $Id: gamesgrid.py,v 1.6 2003/11/22 13:30:08 peter Exp $
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

from wxPython.wx import *
from wxPython.grid import *
from parser import header_tag_list, sort_games
from sgf_sender import open_sgf_file
import __init__, os

class GamesListGrid(wxGrid):
    def __init__(self, parent, id=-1):
        wxGrid.__init__(self, parent, id)

        self.CreateGrid(0, 8)  # must be size of header_tag_list
        self.EnableEditing(0)

        # the column order must be the same as header_tag_list, otherwise sort won't work
        self.SetColLabelValue(0, 'White')
        self.SetColLabelValue(1, 'Rank')
        self.SetColLabelValue(2, 'Black')
        self.SetColLabelValue(3, 'Rank')
        self.SetColLabelValue(4, 'Size')
        self.SetColLabelValue(5, 'Komi')
        self.SetColLabelValue(6, 'Result')
        self.SetColLabelValue(7, 'Date')
        self.AutoSizeColumns()
        self.SetRowLabelSize(0)
        self.SetSelectionMode(1)

        self.filenames = []

        EVT_GRID_CELL_LEFT_DCLICK(self, self.OnLeftDblClick)
        EVT_GRID_LABEL_LEFT_CLICK(self, self.OnLabelLeftClick)
        EVT_GRID_LABEL_RIGHT_CLICK(self, self.OnLabelRightClick)

    def display_games(self, games=None):
        if games:
            self.games = games
        self.ClearGrid()
        if self.GetNumberRows():
            self.DeleteRows(0, self.GetNumberRows())
        self.AppendRows(len(self.games))
        del self.filenames
        self.filenames = []
        row = 0
        for l in self.games:
            col = 0
            for tag in header_tag_list:
                if l[0][tag]:
                    self.SetCellValue(row, col, l[0][tag])
                col += 1
            row += 1
            self.filenames.append(l[1])

    def OnLeftDblClick(self, event):
        if __init__.info_level >= 1:
            print 'Opening SGF file %s.' % self.filenames[event.GetRow()]
        # Open SGF in glGo
        if os.name == 'nt':
            # Read glGo install folder from registry on Windows
            import registry
            dirname = registry.get_glGo_install_path()
        else:
            # Not needed on Linux, glGo is found in PATH
            dirname = None
        open_sgf_file(self.filenames[event.GetRow()], dirname=dirname, spawn=True)

    def OnLabelLeftClick(self, event):
        # Sort column ascending
        self.display_games(sort_games(self.games, event.GetCol()))

    def OnLabelRightClick(self, event):
        # Sort column descending
        self.display_games(sort_games(self.games, event.GetCol(), reverse=True))

class GamesListFrame(wxFrame):
    def __init__(self, parent, id, title, pos=wxDefaultPosition, size=wxDefaultSize):
        wxFrame.__init__(self, parent, id, title, pos, size)

        panel = wxPanel(self, -1)
        self.grid = GamesListGrid(panel, -1)
        flexsizer = wxFlexGridSizer(2)
        flexsizer.AddGrowableRow(0)
        flexsizer.AddGrowableCol(0)
        flexsizer.Add(self.grid, 1, wxEXPAND, 0)
        flexsizer.Add(wxButton(panel, wxID_CLOSE, 'Close'), 0, wxALL | wxALIGN_RIGHT, 5)
        panel.SetSizer(flexsizer)

        EVT_BUTTON(self, wxID_CLOSE, self.OnClose)

    def display_games(self, games):
        self.grid.display_games(games)

    def OnClose(self, event):
        self.Destroy()


#
# Debug stuff following
#

## from parser import *

## class TestApp(wxApp):
##     def OnInit(self):
##         self.frame = GamesListFrame(None, -1, 'Test', size=wxSize(400, 400))
##         self.frame.Centre()
##         self.frame.Show()
##         self.SetTopWindow(self.frame)
##         init_gamelist()
##         games = list_index()
##         if games:
##             self.frame.display_games(games)
##         return True

## def main():
##     app = TestApp(0)
##     app.MainLoop()

## if __name__ == '__main__':
##     main()
