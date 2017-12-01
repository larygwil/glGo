#! /usr/bin/python

#
# plot_canvas.py
#
# $Id: plot_canvas.py,v 1.4 2003/11/30 15:56:47 peter Exp $
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
from wxPython.lib import wxPlotCanvas
from wxPython.lib.wxPlotCanvas import *
import wxPython.xrc
import plotter
from parser import convert_rank_to_string
from __init__ import *
if USE_RESOURCE:
    import resource

ALL = 1
WHITE = 2
BLACK = 4

def format_align(i):
    try:
        return '%3s' % i
    except ValueError:
        return 'N/A'

class RankPlotCanvas(PlotCanvas):
    def __init__(self, name, parent):
        PlotCanvas.__init__(self, parent)
        if plotter.init_data(name) == 0:
            self.d1 = plotter.create_data(plotter.ALL_GAMES)
            self.d2 = plotter.create_data(plotter.WHITE_GAMES)
            self.d3 = plotter.create_data(plotter.BLACK_GAMES)
        else:
            self.d1 = None
            self.d2 = None
            self.d2 = None
        self.which_games = ALL

    #
    # Copy&Paste and slightly modified (see yticks) from wxPython 2.4.2
    #
    def draw(self, graphics, xaxis = None, yaxis = None, dc = None):
        if dc == None: dc = wx.wxClientDC(self)
        dc.BeginDrawing()
        dc.Clear()
        self.last_draw = (graphics, xaxis, yaxis)
        p1, p2 = graphics.boundingBox()
        xaxis = self._axisInterval(xaxis, p1[0], p2[0])
        yaxis = self._axisInterval(yaxis, p1[1], p2[1])
        text_width = [0., 0.]
        text_height = [0., 0.]
        if xaxis is not None:
            p1[0] = xaxis[0]
            p2[0] = xaxis[1]
            xticks = self._ticks(xaxis[0], xaxis[1])
            bb = dc.GetTextExtent(xticks[0][1])
            text_height[1] = bb[1]
            text_width[0] = 0.5*bb[0]
            bb = dc.GetTextExtent(xticks[-1][1])
            text_width[1] = 0.5*bb[0]
        else:
            xticks = None
        if yaxis is not None:
            p1[1] = yaxis[0]
            p2[1] = yaxis[1]
            yticks = self._ticks(yaxis[0], yaxis[1])
            yticks = self.convert_yticks_to_rank(yticks)
            for y in yticks:
                bb = dc.GetTextExtent(y[1])
                text_width[0] = max(text_width[0],bb[0])
            h = 0.5*bb[1]
            text_height[0] = h
            text_height[1] = max(text_height[1], h)
        else:
            yticks = None
        text1 = Numeric.array([text_width[0], -text_height[1]])
        text2 = Numeric.array([text_width[1], -text_height[0]])
        scale = (self.plotbox_size-text1-text2) / (p2-p1)
        try:
            shift = -p1*scale + self.plotbox_origin + text1
            self._drawAxes(dc, xaxis, yaxis, p1, p2,
                           scale, shift, xticks, yticks)
            graphics.scaleAndShift(scale, shift)
            graphics.draw(dc)
        except OverflowError:
            print "Problem drawing graph"
        dc.EndDrawing()

    def convert_yticks_to_rank(self, yticks):
        new_yticks = []
        for y in yticks:
            if y[1].endswith('.0'):
                new_yticks.append((y[0], convert_rank_to_string(54 - int(y[0]+0.5))))
            else:
                new_yticks.append((y[0], ""))
        have_rank = [ y[1] for y in new_yticks ]
        for i, y in enumerate(new_yticks):
            r = convert_rank_to_string(54 - int(y[0]+0.5))
            if not have_rank.count(r):
                try:
                    n = new_yticks[i+1]
                except IndexError:
                    new_yticks[i] = ( y[0], r )
                    continue
                if n[1] and n[1] != r:
                    new_yticks[i] = ( y[0], r )
        return new_yticks

    def init_objects(self, data):
        if not data:
            return None
        col = ("blue", "red", "black")
        all = []
        for i, d in enumerate(data):
            if not d:
                continue
            lines = PolyLine(d, color=col[i])
            marks = PolyMarker(d, color=col[i], marker='circle',size=1)
            all.append(lines)
            all.append(marks)
        return PlotGraphics(all)

    def display_graph(self, which=None):
        if not which:
            which = self.which_games
        if which & ALL:
            d1 = self.d1
        else:
            d1 = None
        if which & WHITE:
            d2 = self.d2
        else:
            d2 = None
        if which & BLACK:
            d3 = self.d3
        else:
            d3 = None
        objs = self.init_objects( (d1, d2, d3) )
        if objs:
            self.draw(objs, 'automatic', 'automatic')

    def OnAllGames(self, event):
        self.which_games ^= ALL
        self.display_graph(self.which_games)

    def OnWhiteGames(self, event):
        self.which_games ^= WHITE
        self.display_graph(self.which_games)

    def OnBlackGames(self, event):
        self.which_games ^= BLACK
        self.display_graph(self.which_games)

class PlotFrame(wxFrame):
   def __init__(self, name, parent, id, title, pos=wxDefaultPosition, size=wxDefaultSize):
       wxFrame.__init__(self, parent, id, title, pos, size)

       topPanel = wxPanel(self, -1)

       plot_canvas = RankPlotCanvas(name, topPanel)
       if not USE_RESOURCE:
           self.res = wxPython.xrc.wxXmlResource('plotter.xrc')
           panel = self.res.LoadPanel(topPanel, 'plot_panel')
           self.res.AttachUnknownControl('plot_canvas', plot_canvas)
       else:
           resource.InitXmlResource()
           panel = wxPython.xrc.wxXmlResource_Get().LoadPanel(topPanel, 'plot_panel')
           wxPython.xrc.wxXmlResource_Get().AttachUnknownControl('plot_canvas', plot_canvas)

       sizer = wxBoxSizer(wxVERTICAL)
       sizer.Add(panel, 1, wxEXPAND)
       sizer.Add(wxButton(topPanel, wxID_CLOSE, "Close"), 0, wxALL | wxALIGN_RIGHT, 5)
       topPanel.SetSizer(sizer)
       sizer.SetSizeHints(self)

       EVT_CHECKBOX(self, wxPython.xrc.XRCID('games_all'), plot_canvas.OnAllGames)
       EVT_CHECKBOX(self, wxPython.xrc.XRCID('games_white'), plot_canvas.OnWhiteGames)
       EVT_CHECKBOX(self, wxPython.xrc.XRCID('games_black'), plot_canvas.OnBlackGames)
       EVT_BUTTON(self, wxID_CLOSE, self.OnClose)

       stats = plotter.create_statistics()
       if not stats:
           stats = (0, 0, 0, 0, 0, 0)
       wxPython.xrc.XRCCTRL(self, 'wins', wxStaticText).SetLabel(format_align(stats[0]))
       wxPython.xrc.XRCCTRL(self, 'losses', wxStaticText).SetLabel(format_align(stats[1]))
       wxPython.xrc.XRCCTRL(self, 'wins_white', wxStaticText).SetLabel(format_align(stats[2]))
       wxPython.xrc.XRCCTRL(self, 'losses_white', wxStaticText).SetLabel(format_align(stats[3]))
       wxPython.xrc.XRCCTRL(self, 'wins_black', wxStaticText).SetLabel(format_align(stats[4]))
       wxPython.xrc.XRCCTRL(self, 'losses_black', wxStaticText).SetLabel(format_align(stats[5]))

       plot_canvas.display_graph()

   def OnClose(self, event):
       self.Destroy()


#
# Debug stuff following
#

## from parser import Game

## class PlotApp(wxApp):
##     def OnInit(self):
##         self.frame = PlotFrame(name, None, -1, "Plot Test")
##         self.frame.Centre()
##         self.frame.Show()
##         self.SetTopWindow(self.frame)
##         return True

## def main():
##     if len(sys.argv) < 2:
##         print 'Usage is: plot_canvas <playername>'
##         sys.exit()
##     global name
##     name = sys.argv[1]
##     import parser
##     parser.init_gamelist()
##     app = PlotApp(redirect=False)
##     app.MainLoop()

## if __name__ == '__main__':
##     main()
