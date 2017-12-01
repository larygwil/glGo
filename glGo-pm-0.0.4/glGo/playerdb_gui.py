#! /usr/bin/python

#
# playerdb_gui.py
#
# $Id: playerdb_gui.py,v 1.19 2003/11/29 14:00:51 peter Exp $
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


import wx, wxPython.xrc, playerdb, plotter, gamesgrid, os, images, cPickle, sys
from parser import *
from plot_canvas import RankPlotCanvas, PlotFrame, format_align
from __init__ import *
if USE_RESOURCE:
    import resource

config = None

def sort_fun(item1, item2):
    if item1 == item2:
        return 0
    elif item1 < item2:
        return -1
    return 1

# Sort by first three characters
def calc_item_data(item):
    if not len(item):
        return 0
    i = 0
    code = 0
    for c in item:
        code += ord(c) * 74**(3-i)
        i += 1
        if i > 2:
            break
    return code

class Popup(wx.Menu):
    ID_EDIT    = 201
    ID_REMOVE  = 202
    ID_GRAPH   = 203
    ID_STATUS  = 204
    ID_FRIEND  = 205
    ID_NEUTRAL = 206
    ID_BOZO    = 207

    def __init__(self, parent, name):
        wx.Menu.__init__(self)

        self.parent = parent
        self.name = name

        self.Append(self.ID_EDIT, 'Edit')
        self.Append(self.ID_REMOVE, 'Remove')
        self.Append(self.ID_GRAPH, 'Show graph')

        sub_menu = wx.Menu()
        sub_menu.AppendRadioItem(self.ID_FRIEND, 'Friend')
        sub_menu.AppendRadioItem(self.ID_NEUTRAL, 'Neutral')
        sub_menu.AppendRadioItem(self.ID_BOZO, 'Bozo')
        self.AppendMenu(self.ID_STATUS, 'Status', sub_menu)
        status = playerdb.get_player_status(name)
        if status == PLAYER_STATUS_FRIEND:
            pos = self.ID_FRIEND
        elif status == PLAYER_STATUS_BOZO:
            pos = self.ID_BOZO
        else:
            pos = self.ID_NEUTRAL
        it = sub_menu.FindItemById(pos)
        if it:
            it.Check()

        wx.EVT_MENU(self, self.ID_EDIT, self.OnEdit)
        wx.EVT_MENU(self, self.ID_REMOVE, self.OnRemove)
        wx.EVT_MENU(self, self.ID_GRAPH, self.OnGraph)
        wx.EVT_MENU(self, self.ID_FRIEND, self.OnFriend)
        wx.EVT_MENU(self, self.ID_NEUTRAL, self.OnNeutral)
        wx.EVT_MENU(self, self.ID_BOZO, self.OnBozo)

    def OnEdit(self, event):
        self.parent.OpenPlayerNotesDialog(self.name)

    def OnRemove(self, event):
        self.parent.OnRemoveFromAll(None)

    def OnGraph(self, event):
        self.parent.showGraph(self.name)

    def OnFriend(self, event):
        print "OnFriend"
        res = playerdb.add_player(self.name, PLAYER_STATUS_FRIEND)
        print res
        if res != 2:
            self.parent.init_all_list()

    def OnNeutral(self, event):
        res = playerdb.add_player(self.name, PLAYER_STATUS_NEUTRAL)
        if res != 2:
            self.parent.init_all_list()

    def OnBozo(self, event):
        res = playerdb.add_player(self.name, PLAYER_STATUS_BOZO)
        if res != 2:
            self.parent.init_all_list()

class Frame(wx.Frame):
    selected_all_item = None
    flag_categories = []

    def __init__(self, parent=None, id=-1, title='Player management',
                 pos=wx.DefaultPosition, size=wx.DefaultSize):
        wx.Frame.__init__(self, parent, id, title, pos, size)

        wx.InitAllImageHandlers()
        self.icon = images.getMondrianIcon()
        self.SetIcon(self.icon)

        # Load XRC resource
        if not USE_RESOURCE:
            self.res = wxPython.xrc.wxXmlResource('playerdb.xrc')
            panel = self.res.LoadPanel(self, 'playerdb_panel')
            menubar = self.res.LoadMenuBar('playerdb_menu')
        else:
            resource.InitXmlResource()
            panel = wxPython.xrc.wxXmlResource_Get().LoadPanel(self, 'playerdb_panel')
            menubar = wxPython.xrc.wxXmlResource_Get().LoadMenuBar('playerdb_menu')

        # Layout
        self.SetMenuBar(menubar)
        sizer = wx.BoxSizer(wx.VERTICAL)
        sizer.Add(panel, 1, wx.EXPAND)
        self.SetSizer(sizer)
        sizer.SetSizeHints(self)

        # Init lists
        self.friends_list = wxPython.xrc.XRCCTRL(self, 'friends_list', wx.ListBox)
        self.bozo_list = wxPython.xrc.XRCCTRL(self, 'bozo_list', wx.ListBox)
        self.all_list = wxPython.xrc.XRCCTRL(self, 'all_list', wx.ListCtrl)
        img_list = wx.ImageList(16, 16)
        img_list.Add(images.getSmilesBitmap())
        img_list.Add(images.getBozoBitmap())
        self.all_list.AssignImageList(img_list, wx.IMAGE_LIST_SMALL)
        self.flag_filter = 0
        self.init_lists()

        # Event table
        wx.EVT_BUTTON(self, wxPython.xrc.XRCID('add_friend'), self.OnAddFriend)
        wx.EVT_BUTTON(self, wxPython.xrc.XRCID('remove_friend'), self.OnRemoveFriend)
        wx.EVT_BUTTON(self, wxPython.xrc.XRCID('add_bozo'), self.OnAddBozo)
        wx.EVT_BUTTON(self, wxPython.xrc.XRCID('remove_bozo'), self.OnRemoveBozo)
        wx.EVT_BUTTON(self, wxPython.xrc.XRCID('all_add'), self.OnAddToAll)
        wx.EVT_BUTTON(self, wxPython.xrc.XRCID('all_remove'), self.OnRemoveFromAll)
        wx.EVT_BUTTON(self, wxPython.xrc.XRCID('all_edit'), self.OnAllEdit)
        wx.EVT_BUTTON(self, wx.ID_EXIT, self.OnClose)
        wx.EVT_MENU(self, wx.ID_EXIT, self.OnClose)
        wx.EVT_CLOSE(self, self.OnClose)
        wx.EVT_BUTTON(self, wxPython.xrc.XRCID('reload'), self.OnReload)
        wx.EVT_MENU(self, wxPython.xrc.XRCID('reload'), self.OnReload)
        wx.EVT_MENU(self, wxPython.xrc.XRCID('cleanup'), self.OnCleanupDB)
        wx.EVT_MENU(self, wxPython.xrc.XRCID('edit_flags'), self.OnEditFlags)
        wx.EVT_MENU(self, wxPython.xrc.XRCID('scan_games'), self.OnScanDir)
        wx.EVT_MENU(self, wxPython.xrc.XRCID('list_games'), self.OnListGames)
        wx.EVT_MENU(self, wxPython.xrc.XRCID('cleanup_games'), self.OnCleanupGames)
        wx.EVT_MENU(self, wxPython.xrc.XRCID('show_graph_of'), self.OnShowGraphOf)
        wx.EVT_MENU(self, wx.ID_ABOUT, self.OnAbout)
        wx.EVT_LISTBOX_DCLICK(self, wxPython.xrc.XRCID('friends_list'), self.OnFriendsDblClick)
        wx.EVT_LISTBOX_DCLICK(self, wxPython.xrc.XRCID('bozo_list'), self.OnBozosDblClick)
        wx.EVT_LIST_ITEM_ACTIVATED(self, wxPython.xrc.XRCID('all_list'), self.OnAllDblClick)
        wx.EVT_LIST_ITEM_SELECTED(self, wxPython.xrc.XRCID('all_list'), self.OnAllItemSelected)
        wx.EVT_LIST_ITEM_RIGHT_CLICK(self, wxPython.xrc.XRCID('all_list'), self.OnAllItemRightClick)
        wx.EVT_CHOICE(self, wxPython.xrc.XRCID('filter_flag'), self.OnFilterFlag)

        # Init and load custom flag categories
        for i in range(0, NUMBER_CUSTOM_FLAGS):
            self.flag_categories.append( ( 0, '') )
        self.load_edit_flags(load_config())
        self.filter_choice = wxPython.xrc.XRCCTRL(self, 'filter_flag', wx.Choice)
        self.init_filter_choice()

    def init_lists(self):
        self.init_friend_bozo_list()
        self.init_all_list()

    def init_friend_bozo_list(self):
        friends = playerdb.list_friends()
        bozos = playerdb.list_bozos()
        self.friends_list.Clear()
        self.bozo_list.Clear()
        self.friends_list.InsertItems(friends, 0)
        self.bozo_list.InsertItems(bozos, 0)

    def init_all_list(self):
        self.all_list.ClearAll()
        for p in playerdb.list_players():
            if self.flag_filter and not playerdb.get_player_flag(p, self.flag_filter - 1):
               continue
            if playerdb.get_player_status(p) == PLAYER_STATUS_FRIEND:
                img_ind = 0
            elif playerdb.get_player_status(p) == PLAYER_STATUS_BOZO:
                img_ind = 1
            else:
                img_ind = 2
            self.all_list.InsertImageStringItem(0, p, img_ind)
            self.all_list.SetItemData(0, calc_item_data(p))
        self.all_list.SortItems(sort_fun)

    def OnClose(self, event):
        playerdb.save_db()
        self.Destroy()

    def OnReload(self, event):
        if playerdb.check_db_reload():
            self.init_lists()

    def insert_player(self, name, list, img_index=0):
        got_it = False
        for i in range(list.GetCount()):
            if list.GetString(i) > name:
                list.InsertItems([name], i)
                got_it = True
                break
        if not got_it:
            list.Append(name)
        self.all_list.InsertImageStringItem(0, name, img_index)
        self.all_list.SetItemData(0, calc_item_data(name))
        self.all_list.SortItems(sort_fun)

    def remove_player(self, name, list):
        ind = list.FindString(name)
        if ind != -1:
            list.Delete(ind)
        self.all_list.DeleteItem(self.all_list.FindItem(0, name))

    def OnAddFriend(self, event):
        name = wx.GetTextFromUser(message='Enter username:', caption='Add friend', parent=self)
        if name:
            res = playerdb.add_player(name, PLAYER_STATUS_FRIEND)
            if res == 1:
                self.remove_player(name, self.bozo_list)
            if res == 0 or res == 1:
                self.insert_player(name, self.friends_list, 0)
            playerdb.save_db()

    def OnRemoveFriend(self, event):
        name = self.friends_list.GetStringSelection()
        if name:
            self.remove_player(name, self.friends_list)
            p = playerdb.get_player(name)
            if p:
                p.set_status(PLAYER_STATUS_NEUTRAL)
                self.all_list.InsertImageStringItem(0, name, 3)
                self.all_list.SetItemData(0, calc_item_data(name))
                self.all_list.SortItems(sort_fun)
            else:
                playerdb.remove_player(name)
            playerdb.save_db()
        else:
            wx.Bell()

    def OnAddBozo(self, event):
        name = wx.GetTextFromUser(message='Enter username:', caption='Add bozo', parent=self)
        if name:
            res = playerdb.add_player(name, PLAYER_STATUS_BOZO)
            if res == 1:
                self.remove_player(name, self.friends_list)
            if res == 0 or res == 1:
                self.insert_player(name, self.bozo_list, 1)
            playerdb.save_db()

    def OnRemoveBozo(self, event):
        name = self.bozo_list.GetStringSelection()
        if name:
            self.remove_player(name, self.bozo_list)
            p = playerdb.get_player(name)
            if p:
                p.set_status(PLAYER_STATUS_NEUTRAL)
                self.all_list.InsertImageStringItem(0, name, 3)
                self.all_list.SetItemData(0, calc_item_data(name))
                self.all_list.SortItems(sort_fun)
            else:
                playerdb.remove_player(name)
            playerdb.save_db()
        else:
            wx.Bell()

    def OnFriendsDblClick(self, event):
        name = self.friends_list.GetStringSelection()
        if name:
            self.OpenPlayerNotesDialog(name)

    def OnBozosDblClick(self, event):
        name = self.bozo_list.GetStringSelection()
        if name:
            self.OpenPlayerNotesDialog(name)

    def OnAllDblClick(self, event):
        name = event.GetText()
        if name:
            self.OpenPlayerNotesDialog(name)

    def OpenPlayerNotesDialog(self, name):
        p = playerdb.get_player(name)
        if not p:
            return

        if not USE_RESOURCE:
            dlg = self.res.LoadDialog(self, 'playerdb_player_dlg')
        else:
            dlg = wxPython.xrc.wxXmlResource_Get().LoadDialog(self, 'playerdb_player_dlg')
        grid = gamesgrid.GamesListGrid(dlg)
        if not USE_RESOURCE:
            self.res.AttachUnknownControl('games_grid', grid)
        else:
            wxPython.xrc.wxXmlResource_Get().AttachUnknownControl('games_grid', grid)
        dlg.SetTitle(name)

        # Set comment
        note_edit = wxPython.xrc.XRCCTRL(dlg, 'note_edit', wx.TextCtrl)
        note_edit.SetValue(playerdb.get_player_comment(name))

        # Set status
        status_box = wxPython.xrc.XRCCTRL(dlg, 'status', wx.RadioBox)
        # Remember current status
        status = p.get_status()
        # Radiobox has another order than the friend/bozo constants
        if status == PLAYER_STATUS_FRIEND:
            status_box.SetSelection(0)
        elif status == PLAYER_STATUS_BOZO:
            status_box.SetSelection(2)

        # Set custom flags
        cf_panel = wxPython.xrc.XRCCTRL(dlg, 'custom_flags_panel', wx.Panel)
        cf_cb = []
        box = wx.StaticBox(cf_panel, -1, 'Flags')
        sizer = wx.StaticBoxSizer(box, wx.VERTICAL)
        for i in range(0, NUMBER_CUSTOM_FLAGS):
            if self.flag_categories[i][0]:
                cb = wx.CheckBox(cf_panel, 500 + i, self.flag_categories[i][1])
                cf_cb.append((cb, i))
                cb.SetValue(p.get_custom_flag(i))
                if os.name == 'nt':
                    sizer.Add(cb, 0, wx.ALL, 5)
                else:
                    sizer.Add(cb, 0, 0)
        cf_panel.SetSizer(sizer)
        # I really hate those OS checks, but the GUI layout messes up otherwise
        if os.name == 'nt':
            sizer.SetSizeHints(cf_panel)

        # Load games list
        games = find_by_player(name)
        if games:
            games = sort_games(games, 1)  # Sort by white rank
            grid.display_games(games)

        # Create graph and statistics
        plot_canvas = RankPlotCanvas(name, dlg)
        if not USE_RESOURCE:
            self.res.AttachUnknownControl('plot_canvas', plot_canvas)
        else:
            wxPython.xrc.wxXmlResource_Get().AttachUnknownControl('plot_canvas', plot_canvas)
        wx.EVT_CHECKBOX(dlg, wxPython.xrc.XRCID('games_all'), plot_canvas.OnAllGames)
        wx.EVT_CHECKBOX(dlg, wxPython.xrc.XRCID('games_white'), plot_canvas.OnWhiteGames)
        wx.EVT_CHECKBOX(dlg, wxPython.xrc.XRCID('games_black'), plot_canvas.OnBlackGames)
        plot_canvas.display_graph()
        stats = plotter.create_statistics()
        if not stats:
            stats = (0, 0, 0, 0, 0, 0)
        wxPython.xrc.XRCCTRL(dlg, 'wins', wx.StaticText).SetLabel(format_align(stats[0]))
        wxPython.xrc.XRCCTRL(dlg, 'losses', wx.StaticText).SetLabel(format_align(stats[1]))
        wxPython.xrc.XRCCTRL(dlg, 'wins_white', wx.StaticText).SetLabel(format_align(stats[2]))
        wxPython.xrc.XRCCTRL(dlg, 'losses_white', wx.StaticText).SetLabel(format_align(stats[3]))
        wxPython.xrc.XRCCTRL(dlg, 'wins_black', wx.StaticText).SetLabel(format_align(stats[4]))
        wxPython.xrc.XRCCTRL(dlg, 'losses_black', wx.StaticText).SetLabel(format_align(stats[5]))

        dlg.SetSize(wx.Size(380, 320))
        if dlg.ShowModal() == wx.ID_OK:
            # Transfer comment
            playerdb.set_player_comment(name, note_edit.GetValue())

            # Status
            sel = status_box.GetSelection()
            if sel == 0:
                new_status = PLAYER_STATUS_FRIEND
            elif sel == 2:
                new_status = PLAYER_STATUS_BOZO
            else:
                new_status = PLAYER_STATUS_NEUTRAL
            if new_status != status:
                p.set_status(new_status)
                self.init_lists()

            # Transfer custom flags
            for (cb, i) in cf_cb:
                p.set_custom_flag(i, cb.IsChecked())
            del cf_cb

            playerdb.save_db()
            if self.flag_filter:
                self.init_all_list()

    def OnAddToAll(self, event):
        name = wx.GetTextFromUser(message='Enter username:', caption='Add player', parent=self)
        if name:
            if playerdb.get_player(name):
                return
            res = playerdb.add_player(name, PLAYER_STATUS_NEUTRAL)
            if res == 0:
                self.all_list.InsertImageStringItem(0, name, 2)
                self.all_list.SetItemData(0, calc_item_data(name))
                self.all_list.SortItems(sort_fun)
            if res == 0 or res == 1:
                self.init_friend_bozo_list()
            playerdb.save_db()

    def OnRemoveFromAll(self, event):
        if self.selected_all_item:
            self.all_list.DeleteItem(self.all_list.FindItem(0, self.selected_all_item))
            res = playerdb.remove_player(self.selected_all_item)
            if res == 0:
                self.selected_all_item = None
                self.init_friend_bozo_list()
                playerdb.save_db()
        else:
            wx.Bell()

    def OnAllEdit(self, event):
        if self.selected_all_item:
            self.OpenPlayerNotesDialog(self.selected_all_item)
        else:
            wx.Bell()

    def OnAllItemSelected(self, event):
        self.selected_all_item = event.GetText()

    def OnAllItemRightClick(self, event):
        self.PopupMenu(Popup(self, self.selected_all_item), event.GetPoint())

    def OnCleanupDB(self, event):
        res = playerdb.cleanup_db()
        if res > 0:
            self.init_lists()
            if res > 1:
                s = 'players'
            else:
                s = 'player'
            wx.MessageBox('Cleanup done: Removed %d %s.' % (res, s),
                          'Database cleanup', wx.OK | wx.ICON_INFORMATION, self)
        else:
            wx.MessageBox('Nothing to do.',
                          'Database cleanup', wx.OK | wx.ICON_INFORMATION, self)

    def OnAbout(self, event):
        wx.MessageBox('%s playermanager %s\n\n'
                      'A standalone GUI for the glGo player database.\n\n'
                      'Written by Peter Strempel\n'
                      'Copyright (c) 2003, Peter Strempel' % (packagename, version),
                      'About glGo playermanager',
                      wx.OK | wx.ICON_INFORMATION, self)

    def OnEditFlags(self, event):
        config = load_config()
        self.load_edit_flags(config)
        if not USE_RESOURCE:
            dlg = self.res.LoadDialog(self, 'playerdb_flags_dlg')
        else:
            dlg = wxPython.xrc.wxXmlResource_Get().LoadDialog(self, 'playerdb_flags_dlg')
        flags_cb = []
        flags_edit = []
        for i in range(1, NUMBER_CUSTOM_FLAGS+1):
            flags_cb.append(wxPython.xrc.XRCCTRL(dlg, 'flag_%d_cb' % i, wx.CheckBox))
            flags_edit.append(wxPython.xrc.XRCCTRL(dlg, 'flag_%d_edit' % i, wx.TextCtrl))
        for i in range(0, NUMBER_CUSTOM_FLAGS):
            flags_cb[i].SetValue(self.flag_categories[i][0])
            flags_edit[i].SetValue(self.flag_categories[i][1])
        if dlg.ShowModal() == wx.ID_OK:
            for i in range(0, NUMBER_CUSTOM_FLAGS):
                self.flag_categories[i] = ( flags_cb[i].IsChecked(), flags_edit[i].GetValue() )
            self.save_edit_flags(config)
        del flags_cb
        del flags_edit
        del config
        self.init_filter_choice()

    def init_filter_choice(self):
        self.filter_choice.Clear()
        self.filter_choice.Append('No filter')
        for i in range(0, NUMBER_CUSTOM_FLAGS):
            if self.flag_categories[i][0]:
                self.filter_choice.Append(self.flag_categories[i][1])
        self.filter_choice.SetSelection(0)
        self.flag_filter = 0
        self.init_all_list()

    def load_edit_flags(self, config):
        if not config:
            return
        for i in range(1, NUMBER_CUSTOM_FLAGS+1):
            active = config.ReadInt('PlayerDB/CustomFlags/Flag%dEnabled' % i)
            value = config.Read('PlayerDB/CustomFlags/Flag%dValue' % i)
            self.flag_categories[i-1] = ( active, value )

    def save_edit_flags(self, config):
        if not config:
            return
        for i in range(1, NUMBER_CUSTOM_FLAGS+1):
            config.WriteInt('PlayerDB/CustomFlags/Flag%dEnabled' % i, self.flag_categories[i-1][0])
            config.Write('PlayerDB/CustomFlags/Flag%dValue' % i, self.flag_categories[i-1][1])
        config.Flush()
        config = None

    def OnScanDir(self, event):
        dirname = wx.DirSelector(message='Select SGF directory')
        if not dirname:
            return
        added = create_index(dirname)
        wx.MessageBox('Added %d games.' % added,
                      'SGF index', wx.OK | wx.ICON_INFORMATION, self)

    def OnListGames(self, event):
        frame = gamesgrid.GamesListFrame(self, -1, 'Games index', size=wx.Size(400, 400))
        frame.Show()
        games = list_index()
        if games:
            games = sort_games(games, 1)  # Sort by white rank
            frame.display_games(games)

    def OnCleanupGames(self, event):
        removed = cleanup_games()
        wx.MessageBox('Removed %d games.' % removed,
                      'Index cleanup', wx.OK | wx.ICON_INFORMATION, self)

    def OnShowGraphOf(self, event):
        name = wx.GetTextFromUser(message='Enter username:', caption='Select player', parent=self)
        if name:
            self.showGraph(name)

    def showGraph(self, name):
        frame = PlotFrame(name, self, -1, name)
        frame.Centre()
        frame.Show()

    def OnFilterFlag(self, event):
        self.flag_filter = event.GetSelection()
        self.init_all_list()

class PlayerDBApp(wx.App):
    def OnInit(self):
        self.frame = Frame(size=wx.Size(400, 400))
        self.frame.Centre()
        self.frame.Show()
        self.SetTopWindow(self.frame)
        return True

def load_config():
    config_path = playerdb.get_config_path()
    if not os.path.exists(config_path):
        config_path = ''
    elif os.name == 'nt' and sys.getwindowsversion()[3] == 1:  # Win9x/ME
        config_path = os.path.join(config_path, 'glGo')
    fname = os.path.join(config_path, packagename + '.rc')
    if debug_level >= 1:
        print 'Load config from ' + fname
    if os.name == 'nt':
        # Must force wxFileConfig on Win32, by default it uses the registry
        config = wx.FileConfig(localFilename = fname,
                               style = wx.CONFIG_USE_LOCAL_FILE)
    else:
        config = wx.Config(localFilename = fname,
                           style = wx.CONFIG_USE_LOCAL_FILE)
    return config

def main():
    # Load player database and game index
    path = playerdb.get_config_path()
    playerdb.load_db(os.path.join(path, DEFAULT_DB_FILE))
    if os.name == 'nt' and sys.getwindowsversion()[3] == 1:  # Win9x/ME
        path = None
    init_gamelist(path)

    app = PlayerDBApp(redirect=False)
    app.MainLoop()

if __name__ == '__main__':
    main()
