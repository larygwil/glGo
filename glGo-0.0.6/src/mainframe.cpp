/*
 * mainframe.cpp
 *
 * $Id: mainframe.cpp,v 1.123 2003/11/24 14:39:51 peter Exp $
 *
 * glGo, a prototype for a 3D Goban based on wxWindows, OpenGL and SDL.
 * Copyright (c) 2003, Peter Strempel <pstrempel@gmx.de>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#ifdef __GNUG__
#pragma implementation "mainframe.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/xrc/xmlres.h>
#include <wx/config.h>
#include <wx/artprov.h>
#ifdef __WXMSW__
#include <wx/image.h>
#endif
#include "mainframe.h"
#include "glGo.h"
#include "glBoard.h"
#include "sdlboard.h"
#include "boardhandler.h"
#include "game.h"
#include "gamedata.h"
#include "newgame_dialog.h"
#include "gameinfo_dialog.h"
#include "display_options_dialogs.h"
#include "preferences_dialog.h"
#include "about_dialog.h"
#include "htmlhelp_context.h"
#include "utils.h"
#include "html_utils.h"
#include "board_eventhandler.h"
#include "sidebar.h"
#include "sound.h"

#ifndef NO_GTP
#include "gtp/gtp_setup_dialog.h"
#include "gtp/gtp_setup_wizard.h"
#include "gtp/gtp_scorer.h"
#endif

#ifdef __WXDEBUG__
/** DEBUG: For stone counter in OnNew */
#include "stone.h"
#endif

// Icons
#ifndef __WXMSW__
#include "images/32navy.xpm"
#endif
#include "images/first.xpm"
#include "images/last.xpm"
#include "images/save.xpm"
#include "images/delete.xpm"
#include "images/info.xpm"
#include "images/volume16.xpm"


BEGIN_EVENT_TABLE(MainFrame, wxFrame)
    EVT_CLOSE(MainFrame::OnClose)
    EVT_SIZE(MainFrame::OnSize)
    EVT_CUSTOM(EVT_INTERFACE_UPDATE, ID_WINDOW_MAINFRAME, MainFrame::OnInterfaceUpdate)
#ifndef NO_GTP
    EVT_CUSTOM(EVT_GTP_SCORE, ID_WINDOW_MAINFRAME, MainFrame::OnShowScore)
#endif
    EVT_SASH_DRAGGED_RANGE(ID_WINDOW_SIDEBAR, ID_WINDOW_SIDEBAR, MainFrame::OnSashDrag)
    EVT_MENU(wxID_NEW, MainFrame::OnNew)
    EVT_MENU(wxID_OPEN, MainFrame::OnLoad)
    EVT_MENU(wxID_SAVE, MainFrame::OnSave)
    EVT_MENU(wxID_SAVEAS, MainFrame::OnSaveAs)
    EVT_MENU(wxID_CLOSE, MainFrame::OnClose)
    EVT_MENU(XRCID("delete_move"), MainFrame::OnDeleteMove)
    EVT_MENU(XRCID("remove_marks"), MainFrame::OnRemoveMarks)
    EVT_MENU(XRCID("number_moves"), MainFrame::OnNumberMoves)
    EVT_MENU(XRCID("mark_brothers"), MainFrame::OnMarkBrothers)
    EVT_MENU(XRCID("mark_sons"), MainFrame::OnMarkSons)
    EVT_MENU(XRCID("gameinfo"), MainFrame::OnGameInfo)
    EVT_MENU(XRCID("preferences"), MainFrame::OnPreferences)
    EVT_MENU(XRCID("display_options"), MainFrame::OnDisplayOptions)
    EVT_MENU(XRCID("show_marks"), MainFrame::OnShowMarks)
    EVT_MENU(XRCID("show_coords"), MainFrame::OnShowCoords)
    EVT_MENU(XRCID("show_cursor"), MainFrame::OnShowCursor)
    EVT_MENU(XRCID("clear_output"), MainFrame::OnClearOutput)
    EVT_MENU(XRCID("statusbar"), MainFrame::OnStatusbar)
    EVT_MENU(XRCID("toolbar"), MainFrame::OnToolbar)
    EVT_MENU(XRCID("fullscreen"), MainFrame::OnFullscreen)
    EVT_MENU(XRCID("sidebar"), MainFrame::OnSidebar)
    EVT_MENU(XRCID("swap_sidebar"), MainFrame::OnSwapSidebar)
    EVT_MENU(XRCID("toggle_sound"), MainFrame::OnToggleSound)
    EVT_MENU(XRCID("gtp_connect"), MainFrame::OnGTP)
    EVT_MENU(XRCID("gtp_console"), MainFrame::OnGTPConsole)
    EVT_MENU(XRCID("gtp_close"), MainFrame::OnGTPClose)
    EVT_MENU(XRCID("gtp_score"), MainFrame::OnGTPScore)
    EVT_MENU(wxID_HELP, MainFrame::OnHelp)
    EVT_MENU(wxID_ABOUT, MainFrame::OnAbout)
    EVT_MENU(XRCID("open_webpage"), MainFrame::OnOpenWebpage)
    EVT_MENU(XRCID("check_update"), MainFrame::OnCheckUpdate)
    EVT_MENU(NAVIGATE_DIRECTION_FIRST_MOVE, MainFrame::OnFirstMove)
    EVT_MENU(NAVIGATE_DIRECTION_NEXT_MOVE, MainFrame::OnNextMove)
    EVT_MENU(NAVIGATE_DIRECTION_PREVIOUS_MOVE, MainFrame::OnPreviousMove)
    EVT_MENU(NAVIGATE_DIRECTION_LAST_MOVE, MainFrame::OnLastMove)
    EVT_MENU(NAVIGATE_DIRECTION_NEXT_VARIATION, MainFrame::OnNextVariation)
    EVT_MENU(NAVIGATE_DIRECTION_PREVIOUS_VARIATION, MainFrame::OnPreviousVariation)
    EVT_MENU(wxID_CLEAR, MainFrame::OnDeleteMove)
    EVT_MENU(wxID_SETUP, MainFrame::OnGameInfo)
    EVT_MENU(ID_SOUND_TOGGLE, MainFrame::OnToggleSound_Toolbar)
    EVT_TIMER(ID_CLOCK_TIMER, MainFrame::OnClockTimer)
END_EVENT_TABLE()


MainFrame::MainFrame(const wxPoint& pos, const wxSize& size, GameType game_type, BoardDisplayType displayType)
    : wxFrame(NULL, ID_WINDOW_MAINFRAME, PACKAGE, pos, size), game_type(game_type)
{
    game_id = 0;
    myName = wxEmptyString;
    timewarn_flag = false;
    play_local_sound = true;

    wxLogDebug("GAME TYPE: %d", game_type);

    // Assign icon
    SetIcon(wxICON(navy32));

    // Create statusbar
    wxStatusBar *sb = CreateStatusBar(4);
    wxASSERT(sb != NULL);
    int sb_widths[4] = { -2, -1, -1, -1 };
    sb->SetStatusWidths(4, sb_widths);

    // Create the sidebar sash
    createSidebar();

    // Create GLBoard or SDLBoard. This is currently only possible in the constructor and very ugly.
    // But virtual baseclasses are even more ugly in wxWindows (ehem, not possible, to be exact)
    switch (displayType)
    {
    case DISPLAY_TYPE_OPENGL:
        // Create OpenGL board
        glBoard = new GLBoard(this, ID_WINDOW_BOARD);
        board = glBoard;
        glBoard->SetFocus();
        sdlBoard = NULL;
        // Attach custom eventhandler
        // Here we can later attach different sorts of eventhandlers for different board
        // types, like editing, GTP, IGS play, IGS observe etc.
        glBoard->PushEventHandler(new BoardEventhandler(board));
        {
            OGLConfig config;
            loadOGLConfig(config);
            glBoard->setOGLViewParameter(config);
        }
        break;
    case DISPLAY_TYPE_SDL:
        // Create SDL board
        sdlBoard = new SDLBoard(this, ID_WINDOW_BOARD);
        board = sdlBoard;
        sdlBoard->SetFocus();
        glBoard = NULL;
        // Attach custom eventhandler
        sdlBoard->PushEventHandler(new BoardEventhandler(board));
        break;
    default:
        wxFAIL_MSG(_T("Invalid board displaytype"));
        LOG_GLOBAL(_T("Invalid board display type given!"));
        return;
    }

    // Create menus from resource file. This has to happen after the board was created.
    SetMenuBar(wxXmlResource::Get()->LoadMenuBar(_T("mainframe_menu")));

    // Init menus and create toolbar
    Init();

    // Reconfigure layout if this is not a default board
    if (game_type != GAME_TYPE_PLAY)
        setGameType(game_type);

    // Set the layout
    RefreshLayout();

    // Update sidebar game information
    sidebar->setGameInfo(board->getBoardHandler()->getGame()->getGameData());

    // Set frame title
    updateTitle();

    // Attach clock timer
    clockTimer.SetOwner(this, ID_CLOCK_TIMER);
}

MainFrame::~MainFrame()
{
    clockTimer.Stop();

    if (glBoard != NULL)
    {
        glBoard->Destroy();
        glBoard = NULL;
    }
    if (sdlBoard != NULL)
    {
        sdlBoard->Destroy();
        sdlBoard = NULL;
    }
    board = NULL;

    while (GetEventHandler() != this)
        PopEventHandler(true);

    wxLogDebug("~Mainframe()");
}

void MainFrame::Init()
{
    // Read config and init checkbox menuitems
    bool value;
    wxMenuItem *it;

    // Number moves
    if (wxConfig::Get()->Read(_T("Board/NumberMoves"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("number_moves"));
        if (it != NULL)
            it->Check(value);
    }

    // Mark brothers
    if (wxConfig::Get()->Read(_T("Board/MarkBrothers"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("mark_brothers"));
        if (it != NULL)
            it->Check(value);
    }

    // Mark sons
    if (wxConfig::Get()->Read(_T("Board/MarkSons"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("mark_sons"));
        if (it != NULL)
            it->Check(value);
    }

    // Show marks
    if (wxConfig::Get()->Read(_T("Board/ShowMarks"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("show_marks"));
        if (it != NULL)
            it->Check(value);
    }

    // Show coords
    if (wxConfig::Get()->Read(_T("Board/ShowCoords"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("show_coords"));
        if (it != NULL)
            it->Check(value);
    }

    // Show cursor
    if (wxConfig::Get()->Read(_T("Board/ShowCursor"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("show_cursor"));
        if (it != NULL)
            it->Check(value);
    }

    // Toggle sidebar
    if (wxConfig::Get()->Read(_T("Board/Sidebar"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("sidebar"));
        if (it != NULL)
            it->Check(value);
        if (!value)
            sidebar->Toggle();
    }

    // Swap sidebar
    if (wxConfig::Get()->Read(_T("Board/SwapSidebar"), &value))
    {
        if (value)
            sidebar->Swap();
    }

    // Statusbar
    if (wxConfig::Get()->Read(_T("Board/Statusbar"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("statusbar"));
        if (it != NULL)
            it->Check(value);
        if (!value)
            GetStatusBar()->Hide();
    }

    // Create toolbar
    toolBar = CreateToolBar(wxTB_HORIZONTAL | wxTB_3DBUTTONS | wxTB_FLAT);
    toolBar->AddTool(wxID_NEW, _("New"),
                     wxArtProvider::GetBitmap(wxART_NORMAL_FILE, wxART_TOOLBAR),
                     _("New game"));
    toolBar->AddTool(wxID_OPEN, _("Open"),
                     wxArtProvider::GetBitmap(wxART_FILE_OPEN, wxART_TOOLBAR),
                     _("Open SGF"));
    toolBar->AddTool(wxID_SAVE, _("Save"), wxBitmap(save_xpm), _("Save file"));
    toolBar->AddSeparator();
    toolBar->AddTool(NAVIGATE_DIRECTION_FIRST_MOVE, _("First"), wxBitmap(first_xpm), _("First move"));
    toolBar->AddTool(NAVIGATE_DIRECTION_PREVIOUS_MOVE, _("Back"),
                     wxArtProvider::GetBitmap(wxART_GO_BACK, wxART_TOOLBAR),
                     _("Previous move"));
    toolBar->AddTool(NAVIGATE_DIRECTION_NEXT_MOVE, _("Next"),
                     wxArtProvider::GetBitmap(wxART_GO_FORWARD, wxART_TOOLBAR),
                     _("Next move"));
    toolBar->AddTool(NAVIGATE_DIRECTION_LAST_MOVE, _("Last"), wxBitmap(last_xpm), _("Last move"));
    toolBar->AddTool(NAVIGATE_DIRECTION_NEXT_VARIATION, _("Next variation"),
                     wxArtProvider::GetBitmap(wxART_GO_UP, wxART_TOOLBAR),
                     _("Next variation"));
    toolBar->AddTool(NAVIGATE_DIRECTION_PREVIOUS_VARIATION, _("Previous variation"),
                     wxArtProvider::GetBitmap(wxART_GO_DOWN, wxART_TOOLBAR),
                     _("Previous variation"));
    toolBar->AddSeparator();
    toolBar->AddTool(wxID_CLEAR, _("Delete"), wxBitmap(delete_xpm), _("Delete move"));
    toolBar->AddTool(wxID_SETUP, _("Info"), wxBitmap(info_xpm), _("Game information"));
    toolBar->AddTool(ID_SOUND_TOGGLE, _("Sound"), wxBitmap(volume16_xpm), _("Toggle sound"));
    toolBar->Realize();

    if (wxConfig::Get()->Read(_T("Board/Toolbar"), &value))
    {
        it = GetMenuBar()->FindItem(XRCID("toolbar"));
        if (it != NULL)
            it->Check(value);
        if (!value)
            toolBar->Hide();
    }
}

void MainFrame::createSidebar()
{
    wxLogDebug("create sidebar: GAME TYPE  %d", game_type);

    switch (game_type)
    {
    case GAME_TYPE_GTP:
        sidebar =  new SidebarGTP(this, ID_WINDOW_SIDEBAR);
        break;
    case GAME_TYPE_IGS_OBSERVE:
        sidebar =  new SidebarObserve(this, ID_WINDOW_SIDEBAR);
        break;
    case GAME_TYPE_IGS_PLAY:
        sidebar =  new SidebarIGSPlay(this, ID_WINDOW_SIDEBAR);
        break;
    case GAME_TYPE_PLAY:
    default:
        sidebar = new Sidebar(this, ID_WINDOW_SIDEBAR);
    }
    sidebar->initSidebar();
    int sb_w;
    if (!wxConfig::Get()->Read(_T("Board/SidebarWidth"), &sb_w))
    {
        // Sidebar takes 30% of the frame width by default
        int w, h;
        GetClientSize(&w, &h);
        sb_w = static_cast<int>(w*0.25f);
    }
    sidebar->SetDefaultSize(wxSize(sb_w, 1000));
    sidebar->SetAlignment(wxLAYOUT_RIGHT);
    sidebar->SetSashVisible(wxSASH_LEFT, TRUE);
}

void MainFrame::updateTitle()
{
    GameData *data = board->getBoardHandler()->getGame()->getGameData();
    wxASSERT(data != NULL);

    // example: Zotan 8k vs. tgmouse 10k
    // or if game name is given: Kogo's Joseki Dictionary

    // Prepend '*' marker for modified games (not IGS/GTP games)
    wxString title = wxEmptyString;
    if (game_type == GAME_TYPE_PLAY && board->isModified())
        title = "* ";

    // If there is a game title, use it
    if (!data->gameName.empty())
    {
        SetTitle(title + data->gameName);
        return;
    }

    // White name and rank
    wxString white;
    if (!data->whiteName.empty())
        white = data->whiteName;
    else
        white = _("White");
    if (!data->whiteRank.empty())
        white += " " + data->whiteRank;

    // Black name and rank
    wxString black;
    if (!data->blackName.empty())
        black = data->blackName;
    else
        black = _("Black");
    if (!data->blackRank.empty())
        black += " " + data->blackRank;

    // Prepend game ID for IGS games
    if (game_type == GAME_TYPE_IGS_OBSERVE ||
        game_type == GAME_TYPE_IGS_PLAY)
        title = wxString::Format(_("Game %d - "), game_id);
    title += wxString::Format(_("%s vs. %s"), white.c_str(), black.c_str());
    SetTitle(title);
}

wxWindow* MainFrame::getCurrentBoardWindow()
{
    // Ugly. But virtual baseclasses in wxWindows is even more mess.
    if (board == NULL)
        return NULL;
    return board->isOpenGLBoard() ?
        static_cast<wxWindow*>(glBoard) :
        static_cast<wxWindow*>(sdlBoard);
}

void MainFrame::RefreshLayout()
{
    if (getCurrentBoardWindow() == NULL)
        return;
    wxLayoutAlgorithm layout;
    layout.LayoutFrame(this, getCurrentBoardWindow());
}

void MainFrame::OnClose(wxCloseEvent& event)
{
    wxLogDebug(_T("MainFrame::OnClose"));

    // Proceed and close if this close event is forced or if the
    // modified check passes
    if (!event.CanVeto() || checkModified())
    {
        // Save size or maximized state (not when fullscreen)
        if (!IsFullScreen())
        {
#ifdef __WXMSW__
            bool max = IsMaximized();
            wxConfig::Get()->Write(_T("Board/Maximized"), max);
            if (!max)
#endif
            {
                int x, y;
                GetSize(&x, &y);
                if (x > 100 && y > 30)  // Minimized?
                {
                    wxConfig::Get()->Write(_T("Board/SizeX"), x);
                    wxConfig::Get()->Write(_T("Board/SizeY"), y);
                }
            }
        }

        // Save sidebar width
        int w, h;
        sidebar->GetClientSize(&w, &h);
        if (w > 50)
            wxConfig::Get()->Write(_T("Board/SidebarWidth"), w);

        wxGetApp().notifyMainframeClosed(this);
        if (game_type == GAME_TYPE_GTP)
            wxGetApp().KillGTP();

        Destroy();
    }

    // Don't close the window
    else
        event.Veto();
}

void MainFrame::OnSize(wxSizeEvent& WXUNUSED(event))
{
    RefreshLayout();
}

void MainFrame::OnSashDrag(wxSashEvent& event)
{
    if (event.GetDragStatus() == wxSASH_STATUS_OUT_OF_RANGE)
        return;

    switch (event.GetId())
    {
    case ID_WINDOW_SIDEBAR:
        sidebar->SetDefaultSize(wxSize(event.GetDragRect().width, 1000));
        break;
    }

    RefreshLayout();
}

void MainFrame::appendComment(const wxString &txt, bool at_last)
{
    sidebar->appendTextareaText(txt + "\n");
    wxString current_comment = board->getBoardHandler()->getComment(at_last);
    if (!current_comment.empty())
        current_comment += "\n";
    current_comment += txt;
    board->getBoardHandler()->setComment(current_comment, at_last);
}

void MainFrame::OnInterfaceUpdate(EventInterfaceUpdate& event)
{
    // Update sidebar
    updateSidebar();
    if (game_type == GAME_TYPE_PLAY)
        sidebar->setTurn(event.getToPlay());
    if (game_type != GAME_TYPE_IGS_OBSERVE && game_type != GAME_TYPE_IGS_PLAY)
        sidebar->setTextareaText(event.getComment());
    else
    {
        // Update clocks in IGS games if force flag is given.
        // Otherwise the timer takes care of this.
        // Don't start clocks if IGS teaching games and with 0/-1 settings.
        // TODO: Somewhen implement time tags parsing from SGF files
        if (event.getForceClockUpdate() &&
            board->getBoardHandler()->getGame()->getGameData()->igs_type != 3)  // 3 = IGS Teach
        {
            // Update both clocks and make sure the clock and timer are running
            sidebar->updateClock(STONE_WHITE, board->getClockWhite().format());
            sidebar->updateClock(STONE_BLACK, board->getClockBlack().format());
            StartClock(event.getToPlay());
        }
        timewarn_flag = false;
        sidebar->resetTimeWarning();
    }
    sidebar->setCaptures(event.getCapsWhite(), event.getCapsBlack());

    // Update frame title
    updateTitle();

    // Update statusbar
    switch (game_type)
    {
    case GAME_TYPE_PLAY:
        SetStatusText(wxString::Format(_("Move: %u%s"),
                                       event.getMoveNumber(),
                                       event.getMoveStr().c_str()), 1);
        SetStatusText(wxString::Format(_("%s to play"),
                                       event.getToPlay() == STONE_BLACK ? _("Black") : _("White")), 2);
        SetStatusText(wxString::Format(_("Brothers: %u   Sons: %u"),
                                       event.getBrothers(),
                                       event.getSons()), 3);
        break;
    case GAME_TYPE_GTP:
    case GAME_TYPE_IGS_OBSERVE:
    case GAME_TYPE_IGS_PLAY:
        SetStatusText(wxString::Format(_("Move: %u%s"),
                                       event.getMoveNumber(),
                                       event.getMoveStr().c_str()), 1);
        SetStatusText(wxString::Format(_("%s to play"),
                                       event.getToPlay() == STONE_BLACK ? _("Black") : _("White")), 2);
    }
}

bool MainFrame::isModified() const
{
    return board->isModified();
}

bool MainFrame::checkModified()
{
// Skip this in debug mode, too annoying
#ifndef __WXDEBUG__
    if (isModified() && game_type == GAME_TYPE_PLAY)
    {
        switch (wxMessageBox(_("The game has been modified.\n"
                               "Do you want to save your changes?"),
                             _("Question"),
                             wxYES_NO | wxCANCEL | wxICON_QUESTION,
                             this))
        {
        case wxYES:
            // Try saving
            if (doSave(board->getBoardHandler()->getGame()->getGameData()->filename))
                return true;  // Save ok, proceed
            return false;     // Save failed, abort
        case wxNO:
            return true;      // Don't save and proceed
        case wxCANCEL:
            return false;     // Don't save and abort
        }
    }
#endif

    return true;  // Board is not modified, proceed
}

void MainFrame::updateSidebar()
{
    sidebar->setGameInfo(board->getBoardHandler()->getGame()->getGameData());
}

void MainFrame::OnNew(wxCommandEvent& WXUNUSED(event))
{
#ifndef NO_GTP
    // In GTP mode start a new GTP game
    if (game_type == GAME_TYPE_GTP)
    {
        newGTPGame();
        return;
    }
#endif

#ifdef __WXDEBUG__
    /* See and remove include "stone.h" above */
    wxLogDebug(wxString::Format(_T("OLD: Counter pos/stones: %d"), Position::counter));
#endif

    if (checkModified())  // Ask user to save game
    {
        GameData *data = new GameData();
        NewGameDialog dlg(this, data);
        if (dlg.ShowModal() == wxID_OK)
        {
            board->newGame(data);
            sidebar->reset();
            sidebar->setGameInfo(data);
            SetStatusText(_("New game created."));
        }
        else
            delete data;
    }

#ifdef __WXDEBUG__
    wxLogDebug(wxString::Format(_T("NEW: Counter pos/stones: %d"), Position::counter));
#endif
}

wxString MainFrame::selectLoadSGFFilename(wxWindow *win)
{
    wxString lastDir;
#ifndef __WXMSW__
    // On Linux save and restore the last directory. The Windows filedialog does that already.
    wxConfig::Get()->Read(_T("SGF/LastSGFDir"), &lastDir);
#else
    lastDir = "";
#endif

    // Show dialog to select a file
    wxString filename =
        wxFileSelector(
            _("Load SGF file"), lastDir, "", _T("sgf"),
            _("SGF files (*.sgf)|*.sgf|UGF files(*.ugf;*.ugi)|*.ugf;*.ugi|MGT files (*.mgt)|*.mgt|All files (*)|*"),
            wxOPEN | wxFILE_MUST_EXIST, win);

    // User hit Cancel in the dialog
    if (filename.empty())
        return wxEmptyString;

#ifndef __WXMSW__
    // Store directory in config
    wxFileName fn(filename);
    wxConfig::Get()->Write(_T("SGF/LastSGFDir"), fn.GetPath());
#endif

    return filename;
}

void MainFrame::OnLoad(wxCommandEvent& WXUNUSED(event))
{
    // Ask user to save current game if modified
    if (!checkModified())
        return;

    // Select a filename and load the game
    doLoad(selectLoadSGFFilename(this));
}

void MainFrame::doLoad(const wxString &filename, bool is_tmp_filename)
{
    if (!filename.empty() && board->loadGame(filename, is_tmp_filename))
    {
        SetStatusText(wxString::Format(_("File %s loaded."), filename.c_str()));
        sidebar->reset();
    }
}

wxString MainFrame::createDefaultFilename()
{
    // Create a default filename like Zotan8k-tgmouse9k
    // Remove IGS '*' from rank and playername

    GameData *data = board->getBoardHandler()->getGame()->getGameData();

    wxString pw = data->whiteName;
    if (!pw.empty() && pw.GetChar(pw.length() - 1) == '*')
        pw = pw.Left(pw.length() - 1);

    wxString pb = data->blackName;
    if (!pb.empty() && pb.GetChar(pb.length() - 1) == '*')
        pb = pb.Left(pb.length() - 1);

    wxString rw = data->whiteRank;
    if (!rw.empty() && rw.GetChar(rw.length() - 1) == '*')
        rw = rw.Left(rw.length() - 1);

    wxString rb = data->blackRank;
    if (!rb.empty() && rb.GetChar(rb.length() - 1) == '*')
        rb = rb.Left(rb.length() - 1);

    return pw + rw + "-" + pb + rb;
}

void MainFrame::OnSave(wxCommandEvent& WXUNUSED(event))
{
    doSave(board->getBoardHandler()->getGame()->getGameData()->filename);
}

void MainFrame::OnSaveAs(wxCommandEvent& WXUNUSED(event))
{
    // We need to ask for a filename in any case
    doSave(wxEmptyString);
}

bool MainFrame::doSave(wxString filename)
{
    // We need to open the file selector dialog?
    if (filename.empty())
    {
        wxString lastDir;
#ifndef __WXMSW__
        // On Linux save and restore the last directory. The Windows filedialog does that already.
        wxConfig::Get()->Read(_T("SGF/LastSGFDir"), &lastDir);
#else
        lastDir = "";
#endif

        // Create a default name from player names and ranks
        filename = createDefaultFilename();

        // Show dialog to select a file
        filename =
            wxFileSelector(_("Save SGF file"), lastDir, filename, _T("sgf"),
                           _("SGF files (*.sgf)|*.sgf|All files (*)|*"),
                           wxSAVE | wxOVERWRITE_PROMPT, this);

        // User hit Cancel in the dialog
        if (filename.empty())
            return false;

#ifndef __WXMSW__
        // Store directory in config
        wxFileName fn(filename);
        wxConfig::Get()->Write(_T("SGF/LastSGFDir"), fn.GetPath());
#endif
    }

    // Try saving the game
    if (board->saveGame(filename))
    {
        SetStatusText(wxString::Format(_("File %s saved."), filename.c_str()));
        updateTitle();
        return true;
    }
    return false;
}

void MainFrame::OnDeleteMove(wxCommandEvent& WXUNUSED(event))
{
    // No delete in IGS/GTP games
    if (game_type == GAME_TYPE_PLAY)
        board->undoMove();
}

void MainFrame::OnRemoveMarks(wxCommandEvent& WXUNUSED(event))
{
    board->getBoardHandler()->removeAllMarks();
}

void MainFrame::OnNumberMoves(wxCommandEvent& event)
{
    board->getBoardHandler()->setEditParameter(EDIT_PARAM_NUMBER_MOVES, event.IsChecked());
    wxConfig::Get()->Write(_T("Board/NumberMoves"), event.IsChecked());
}

void MainFrame::OnMarkBrothers(wxCommandEvent& event)
{
    board->getBoardHandler()->setEditParameter(EDIT_PARAM_MARK_BROTHERS, event.IsChecked());
    wxConfig::Get()->Write(_T("Board/MarkBrothers"), event.IsChecked());
}

void MainFrame::OnMarkSons(wxCommandEvent& event)
{
    board->getBoardHandler()->setEditParameter(EDIT_PARAM_MARK_SONS, event.IsChecked());
    wxConfig::Get()->Write(_T("Board/MarkSons"), event.IsChecked());
}

void MainFrame::OnGameInfo(wxCommandEvent& WXUNUSED(event))
{
    const GameData *currentData = board->getBoardHandler()->getGame()->getGameData();
    GameData *newData = new GameData(*currentData);
    GameInfoDialog dlg(this, newData);
    if (dlg.ShowModal() == wxID_OK)
    {
        if (game_type != GAME_TYPE_PLAY)
            // Trash changed data in GTP and IGS games
            delete newData;
        else
        {
            board->getBoardHandler()->getGame()->setGameData(newData);
            sidebar->setGameInfo(newData);
            updateTitle();
        }
    }
    else
        delete newData;
}

void MainFrame::OnToggleSound(wxCommandEvent& event)
{
    play_local_sound = event.IsChecked();
}

void MainFrame::OnToggleSound_Toolbar(wxCommandEvent& WXUNUSED(event))
{
    wxMenuItem *it = GetMenuBar()->FindItem(XRCID("toggle_sound"));
    if (it == NULL)
        return;
    play_local_sound = !it->IsChecked();
    it->Check(play_local_sound);
}

void MainFrame::OnPreferences(wxCommandEvent& WXUNUSED(event))
{
    PreferencesDialog dlg(this);
    if (dlg.ShowModal() == wxID_OK && dlg.languageChanged() != -1 &&
        wxGetApp().setLanguage(languageToLocale(dlg.languageChanged())))
    {
        SetStatusText(_("Changed language."));

        // Replace menubar
        wxMenuBar *oldBar = GetMenuBar();
        SetMenuBar(wxXmlResource::Get()->LoadMenuBar(_T("mainframe_menu")));
        delete oldBar;
    }
}

void MainFrame::OnDisplayOptions(wxCommandEvent& WXUNUSED(event))
{
    if (glBoard != NULL)
    {
        OGLOptionsDialog dlg(this, glBoard);
        dlg.ShowModal();
    }
    else if (sdlBoard != NULL)
    {
        SDLOptionsDialog dlg(this, board);
        dlg.initDialog();
        dlg.ShowModal();
    }
}

void MainFrame::OnShowMarks(wxCommandEvent& event)
{
    board->setViewParameter(VIEW_SHOW_MARKS, event.IsChecked());
    wxConfig::Get()->Write(_T("Board/ShowMarks"), event.IsChecked());
}

void MainFrame::OnShowCoords(wxCommandEvent& event)
{
    board->setViewParameter(VIEW_SHOW_COORDS, event.IsChecked());
    wxConfig::Get()->Write(_T("Board/ShowCoords"), event.IsChecked());
}

void MainFrame::OnShowCursor(wxCommandEvent& event)
{
    board->setViewParameter(VIEW_SHOW_CURSOR, event.IsChecked());
    wxConfig::Get()->Write(_T("Board/ShowCursor"), event.IsChecked());
}

void MainFrame::OnClearOutput(wxCommandEvent& WXUNUSED(event))
{
    sidebar->setTextareaText(wxEmptyString);
}

void MainFrame::OnToolbar(wxCommandEvent& event)
{
    GetToolBar()->Show(event.IsChecked());
    RefreshLayout();
    SendSizeEvent();
    wxConfig::Get()->Write(_T("Board/Toolbar"), event.IsChecked());
}

void MainFrame::OnStatusbar(wxCommandEvent& event)
{
    GetStatusBar()->Show(event.IsChecked());
    RefreshLayout();
    SendSizeEvent();
    wxConfig::Get()->Write(_T("Board/Statusbar"), event.IsChecked());
}

void MainFrame::OnFullscreen(wxCommandEvent& event)
{
    ShowFullScreen(event.IsChecked(), wxFULLSCREEN_NOMENUBAR | wxFULLSCREEN_NOBORDER | wxFULLSCREEN_NOCAPTION);
    RefreshLayout();
}

void MainFrame::OnSidebar(wxCommandEvent& WXUNUSED(event))
{
    sidebar->Toggle();
    RefreshLayout();
    wxConfig::Get()->Write(_T("Board/Sidebar"), sidebar->IsOn());
}

void MainFrame::OnSwapSidebar(wxCommandEvent& WXUNUSED(event))
{
    sidebar->Swap();
    RefreshLayout();
    wxConfig::Get()->Write(_T("Board/SwapSidebar"), !sidebar->IsRight());
}

void MainFrame::OnGTP(wxCommandEvent& WXUNUSED(event))
{
    newGTPGame();
}

bool MainFrame::newGTPGame()
{
#ifndef NO_GTP
    // No GTP in IGS games
    if (game_type == GAME_TYPE_IGS_OBSERVE ||
        game_type == GAME_TYPE_IGS_PLAY)
        return false;

    // First check if we can reuse the board
    if (checkModified())
    {
        GTPConfig *data = new GTPConfig();
        // Try to read GNU Go path from config
        wxString tmp_s;
        if (wxConfig::Get()->Read(_T("GTP/GnuGoPath"), &tmp_s))
            data->gtp_path = tmp_s;
        GTPSetupDialog dlg(this, data);
        if (dlg.ShowModal() == wxID_OK)
        {
            wxASSERT(data != NULL);
            board->newGame(static_cast<GameData*>(data));
            sidebar->setGameInfo(static_cast<GameData*>(data));
            while(!wxGetApp().connectGTP(this, data))
            {
                // Failed to find GNU Go. Run wizard.
                GTPSetupWizard *wizard = new GTPSetupWizard(this);
                wxString gtpPath = wizard->Run();
                wizard->Destroy();
                if (gtpPath.empty())  // Nothing found
                    return false;
                // Save GNU Go path
                wxConfig::Get()->Write(_T("GTP/GnuGoPath"), gtpPath);
                // Try again
                data->gtp_path = gtpPath;
            }
            setGameType(GAME_TYPE_GTP);
            return true;
        }
        else
        {
            delete data;
            return false;
        }
    }
#else
    wxMessageBox(NO_GTP_ERROR_MESSAGE, _("Error"), wxOK | wxICON_ERROR, this);
#endif
    return false;
}

void MainFrame::OnGTPConsole(wxCommandEvent& WXUNUSED(event))
{
#ifndef NO_GTP
    // No GTP in IGS games
    if (game_type == GAME_TYPE_IGS_OBSERVE ||
        game_type == GAME_TYPE_IGS_PLAY)
        return;
    wxGetApp().openGTPConsole();
#else
    wxMessageBox(NO_GTP_ERROR_MESSAGE, _("Error"), wxOK | wxICON_ERROR, this);
#endif
}

void MainFrame::OnGTPClose(wxCommandEvent& WXUNUSED(event))
{
#ifndef NO_GTP
    // No GTP in IGS games
    if (game_type == GAME_TYPE_IGS_OBSERVE ||
        game_type == GAME_TYPE_IGS_PLAY)
        return;
    wxGetApp().KillGTP();
    sidebar->enableEditTools();
    setGameType(GAME_TYPE_PLAY);
#else
    wxMessageBox(NO_GTP_ERROR_MESSAGE, _("Error"), wxOK | wxICON_ERROR, this);
#endif
}

void MainFrame::OnGTPScore(wxCommandEvent& WXUNUSED(event))
{
#ifndef NO_GTP
    // Save game to temp file
    wxString tmpFile = saveTempFile();
    if (tmpFile.empty())
        return;

    // Try to read GNU Go path from config
    wxString gnugo_bin ;
    wxConfig::Get()->Read(_T("GTP/GnuGoPath"), &gnugo_bin, _T("gnugo"));

    // Call scorer at current move + 1
    SetStatusText(_("Called GNU Go score estimation."));
    new GTPScorer(this, gnugo_bin, tmpFile, board->getBoardHandler()->getGame()->getCurrentNumber() + 1);
#else
    wxMessageBox(NO_GTP_ERROR_MESSAGE, _("Error"), wxOK | wxICON_ERROR, this);
#endif
}

void MainFrame::OnHelp(wxCommandEvent& WXUNUSED(event))
{
#ifdef USE_MSHTMLHELP
    wxGetApp().GetHelpController()->DisplaySection(HTMLHELP_CONTEXT_INDEX);
#else
    wxGetApp().GetHelpController()->Display(HTMLHELP_CONTEXT_INDEX);
#endif
}

void MainFrame::OnAbout(wxCommandEvent& WXUNUSED(event))
{
    wxString about_msg = wxString::Format(ABOUT_TEXT, PACKAGE, VERSION, VENDOR);

    wxString ogl_msg;
    if (board->isOpenGLBoard())
        ogl_msg = _("\nOpenGL Information:") + wxString::Format(_T("\n\n%s"), (glBoard->getOpenGLInfo()).c_str());
    else if (sdlBoard != NULL)
        ogl_msg = _("\nSDL Information:") + wxString::Format(_T("\n\n%s"), (sdlBoard->getSDLInfo()).c_str());
    else
        ogl_msg = _("\nThis is a native wxWindows GDI board.");

    wxString oal_msg;
    if (wxGetApp().IsSoundEnabled())
    {
        char info[256];
        Sound_get_info(info);
        oal_msg.Printf("\n%s", info);
    }
    else
        oal_msg = _("\nSound is disabled.");

    AboutDialog dlg(this, about_msg, ogl_msg, oal_msg);
    dlg.ShowModal();
}

void MainFrame::OnOpenWebpage(wxCommandEvent& WXUNUSED(event))
{
    ViewHTMLFile(glGoURL);
}

void MainFrame::OnCheckUpdate(wxCommandEvent& WXUNUSED(event))
{
    checkUpdate(this);
}

void MainFrame::OnFirstMove(wxCommandEvent& WXUNUSED(event))
{
    if (getCurrentBoardWindow() == NULL)
        return;
    EventNavigate evt(NAVIGATE_DIRECTION_FIRST_MOVE);
    getCurrentBoardWindow()->GetEventHandler()->AddPendingEvent(evt);
}

void MainFrame::OnPreviousMove(wxCommandEvent& WXUNUSED(event))
{
    if (getCurrentBoardWindow() == NULL)
        return;
    EventNavigate evt(NAVIGATE_DIRECTION_PREVIOUS_MOVE);
    getCurrentBoardWindow()->GetEventHandler()->AddPendingEvent(evt);
}

void MainFrame::OnNextMove(wxCommandEvent& WXUNUSED(event))
{
    if (getCurrentBoardWindow() == NULL)
        return;
    EventNavigate evt(NAVIGATE_DIRECTION_NEXT_MOVE);
    getCurrentBoardWindow()->GetEventHandler()->AddPendingEvent(evt);
}

void MainFrame::OnLastMove(wxCommandEvent& WXUNUSED(event))
{
    if (getCurrentBoardWindow() == NULL)
        return;
    EventNavigate evt(NAVIGATE_DIRECTION_LAST_MOVE);
    getCurrentBoardWindow()->GetEventHandler()->AddPendingEvent(evt);
}

void MainFrame::OnPreviousVariation(wxCommandEvent& WXUNUSED(event))
{
    if (getCurrentBoardWindow() == NULL)
        return;
    EventNavigate evt(NAVIGATE_DIRECTION_PREVIOUS_VARIATION);
    getCurrentBoardWindow()->GetEventHandler()->AddPendingEvent(evt);
}

void MainFrame::OnNextVariation(wxCommandEvent& WXUNUSED(event))
{
    if (getCurrentBoardWindow() == NULL)
        return;
    EventNavigate evt(NAVIGATE_DIRECTION_NEXT_VARIATION);
    getCurrentBoardWindow()->GetEventHandler()->AddPendingEvent(evt);
}

void MainFrame::setGameType(GameType t)
{
    wxLogDebug(_T("MainFrame::setGameType() %d"), t);

    game_type = t;

    // Recreate sidebar if needed
    if (sidebar->getSidebarType() != game_type)
    {
        sidebar->Destroy();
        createSidebar();
        RefreshLayout();
        updateSidebar();
    }

    switch (game_type)
    {
    case GAME_TYPE_PLAY:
    {
        board->setEditMode(EDIT_MODE_NORMAL);
        wxMenuItem *it = GetMenuBar()->FindItem(wxID_NEW);
        if (it != NULL && !it->IsEnabled())
            it->Enable(true);
        it = GetMenuBar()->FindItem(wxID_OPEN);
        if (it != NULL && !it->IsEnabled())
            it->Enable(true);
        it = GetMenuBar()->FindItem(XRCID("delete_move"));
        if (it != NULL && !it->IsEnabled())
            it->Enable(true);
        toolBar->EnableTool(wxID_NEW, true);
        toolBar->EnableTool(wxID_OPEN, true);
        toolBar->EnableTool(wxID_CLEAR, true);
        sidebar->reset();
    }
    break;
    case GAME_TYPE_IGS_OBSERVE:
    case GAME_TYPE_IGS_PLAY:
    {
        // Disable "New"
        wxMenuItem *it = GetMenuBar()->FindItem(wxID_NEW);
        if (it != NULL && it->IsEnabled())
            it->Enable(false);
        it = GetMenuBar()->FindItem(XRCID("delete_move"));
        if (it != NULL && it->IsEnabled())
            it->Enable(false);
        toolBar->EnableTool(wxID_NEW, false);
        toolBar->EnableTool(wxID_CLEAR, false);
    }
    case GAME_TYPE_GTP:
    {
        // Disable "Open"
        toolBar->EnableTool(wxID_OPEN, false);
        wxMenuItem *it = GetMenuBar()->FindItem(wxID_OPEN);
        if (it != NULL && it->IsEnabled())
            it->Enable(false);
        board->setEditMode(EDIT_MODE_NORMAL);
        sidebar->reset(false, false);
    }
    break;
    }

    reconfigureStatusbar();
}

void MainFrame::reconfigureStatusbar()
{
    wxStatusBar *sb = GetStatusBar();
    wxASSERT(sb != NULL);

    switch (game_type)
    {
    case GAME_TYPE_PLAY:
    {
        sb->SetFieldsCount(4);
        int sb_widths[4] = { -2, -1, -1, -1 };
        sb->SetStatusWidths(4, sb_widths);
    }
    break;
    case GAME_TYPE_GTP:
    case GAME_TYPE_IGS_OBSERVE:
    case GAME_TYPE_IGS_PLAY:
    {
        sb->SetFieldsCount(3);
        int sb_widths[3] = { -2, -1, -1 };
        sb->SetStatusWidths(3, sb_widths);
    }
    break;
    }
}

void MainFrame::updateGameData(const wxString &white_name, const wxString &white_rank,
                               const wxString &black_name, const wxString &black_rank,
                               int size, int handicap, float komi, int byo, short type,
                               const wxString &title, int id)
{
    GameData *data = board->getBoardHandler()->getGame()->getGameData();

    // Copy values into current GameData
    data->whiteName = white_name;
    data->whiteRank = white_rank;
    data->blackName = black_name;
    data->blackRank = black_rank;
    data->board_size = size;   // TODO: This wont change the display !
    data->handicap = handicap;
    data->komi = komi;
    data->time = wxString::Format("%d:00", byo);
    data->igs_type = type;
    data->gameName = title;

    // Add place, copyright and date tags
    data->place = _("IGS-PandaNet");
    data->copyright = wxString::Format(_T("Copyright (c) PANDANET Inc. %d"), wxDateTime::GetCurrentYear());
    data->date = wxDateTime::Now().FormatDate();

    // Update sidebar and title
    updateSidebar();
    updateTitle();
    if (game_type != GAME_TYPE_IGS_PLAY)
    {
        sidebar->updateClock(STONE_WHITE, Clock::Format(byo));
        sidebar->updateClock(STONE_BLACK, Clock::Format(byo));
    }
    // Tell clocks about the byoyomi time
    board->SetByoTime(STONE_WHITE | STONE_BLACK, byo);

    // Remember game id
    game_id = id;
}

void MainFrame::OnShowScore(
#ifndef NO_GTP
    EventGTPScore &event
#endif
    )
{
#ifndef NO_GTP
    // Display result or error message
    wxString msg = wxEmptyString;
    if (!event.getErrorFlag())
        msg = _("GNU Go thinks: ");
    msg += event.getResult();

    SetStatusText(msg);
    wxMessageBox(msg, _("GNU Go score estimator"),
                 wxOK | (event.getErrorFlag() ? wxICON_ERROR : wxICON_INFORMATION), this);

    // Delete scorer, if given
    if (event.getScorer() != NULL)
        delete event.getScorer();
#else
    wxFAIL_MSG(_T("OnShowScore called in non-GTP build."));
#endif
}

wxString MainFrame::saveTempFile()
{
    wxString tmpFile = wxFileName::CreateTempFileName(PACKAGE);
    if (tmpFile.empty())
    {
        LOG_GLOBAL(_T("Error: Failed to create temporary file."));
        return wxEmptyString;
    }
    if (!board->saveGame(tmpFile, true))
    {
        LOG_GLOBAL(wxString::Format(_T("Error: Failed to save game to temporary file: %s"), tmpFile.c_str()));
        return wxEmptyString;
    }
    return tmpFile;
}

void MainFrame::OnClockTimer(wxTimerEvent& WXUNUSED(event))
{
    int time = 0;
    if (board->getClockWhite().IsRunning())
        time = board->TickClock(STONE_WHITE);
    else if (board->getClockBlack().IsRunning())
        time = board->TickClock(STONE_BLACK);

    // Check for time warning
    int warn = 0;
    if (game_type == GAME_TYPE_IGS_PLAY)
    {
        int time_warning_level;
        wxConfig::Get()->Read(_T("IGS/TimeThreshold"), &time_warning_level, 30);
        if (time_warning_level > 0 && time < time_warning_level && time != NO_BYO)
        {
            if (board->getClockWhite().IsRunning() &&
                board->getMyColor() == STONE_WHITE &&
                board->getBoardHandler()->getCurrentTurnColor() == STONE_WHITE)
                warn |= STONE_WHITE;
            else if (board->getClockBlack().IsRunning() &&
                     board->getMyColor() == STONE_BLACK &&
                     board->getBoardHandler()->getCurrentTurnColor() == STONE_BLACK)
                warn |= STONE_BLACK;

            // Play sound
            bool play_warn_sound;
            wxConfig::Get()->Read(_T("IGS/TimeSound"), &play_warn_sound, true);
            if (warn > 0 && !timewarn_flag && play_warn_sound)
            {
                playSound(SOUND_TIMEWARN);
                timewarn_flag = true;
            }
        }
    }

    sidebar->updateClock(STONE_WHITE, board->getClockWhite().format(), (warn & STONE_WHITE) != 0);
    sidebar->updateClock(STONE_BLACK, board->getClockBlack().format(), (warn & STONE_BLACK) != 0);
}

void MainFrame::InitClocks(int white_time, int black_time)
{
    board->InitClocks(white_time, black_time);
    sidebar->updateClock(STONE_WHITE, Clock::Format(white_time));
    sidebar->updateClock(STONE_BLACK, Clock::Format(black_time));
    if (white_time != 0 && black_time != 0)
        StartClock(STONE_BLACK);
}

void MainFrame::StartClock(int col)
{
    board->StartClock(col);

    if (!clockTimer.IsRunning())
        clockTimer.Start(1000);

    wxLogDebug("Started %s clock", (col == STONE_WHITE ? "white" : "black"));
}

void MainFrame::StopClock(int col, bool stop_timer)
{
    board->StopClock(col);

    if (stop_timer)
        clockTimer.Stop();

    wxLogDebug("Stopped %s clock", (col == STONE_WHITE ? "white" : "black"));
}

void MainFrame::setGameResult(const wxString &txt)
{
    if (!txt.empty())
        board->getBoardHandler()->getGame()->getGameData()->result = txt;
}

void MainFrame::enterIGSScoreMode()
{
    wxASSERT(game_type == GAME_TYPE_IGS_PLAY);
    if (game_type != GAME_TYPE_IGS_PLAY)
        return;

    // Switch sidebar to score mode
    static_cast<SidebarIGSPlay*>(sidebar)->enterScoreMode();

    // Stop clocks and timer
    StopClock(STONE_WHITE | STONE_BLACK, true);
}

bool MainFrame::isIGSScored()
{
    if (game_type != GAME_TYPE_IGS_PLAY)
        return false;  // No own IGS game
    return static_cast<SidebarIGSPlay*>(sidebar)->isIGSScored();
}
