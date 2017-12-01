/*
 * sidebar.cpp
 *
 * $Id: sidebar.cpp,v 1.31 2003/11/02 07:52:57 peter Exp $
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
#pragma implementation "sidebar.h"
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
#include <wx/notebook.h>
#include "gamedata.h"
#include "events.h"
#include "igs_events.h"
#include "mainframe.h"
#include "board.h"
#include "sidebar.h"
#include "utils/utils.h"
#include "glGo.h"
#include "inputctrl.h"
#ifndef NOT_GTP
#include "gtp.h"
#endif

// Icons
#include "images/stone.xpm"
#include "images/black_icon.xpm"
#include "images/white_icon.xpm"
#include "images/square.xpm"
#include "images/circle.xpm"
#include "images/triangle.xpm"
#include "images/cross.xpm"
#include "images/text.xpm"
#include "images/number.xpm"


// ------------------------------------------------------------------------
//                            Class Sidebar
// ------------------------------------------------------------------------

BEGIN_EVENT_TABLE(Sidebar, wxSashLayoutWindow)
    EVT_BUTTON(XRCID("pass"), Sidebar::OnPass)
    EVT_BUTTON(XRCID("score"), Sidebar::OnScore)
    EVT_BUTTON(XRCID("stone_black_white"), Sidebar::OnStone)
    EVT_BUTTON(XRCID("stone_white"), Sidebar::OnStoneWhite)
    EVT_BUTTON(XRCID("stone_black"), Sidebar::OnStoneBlack)
    EVT_BUTTON(XRCID("square"), Sidebar::OnMarkSquare)
    EVT_BUTTON(XRCID("circle"), Sidebar::OnMarkCircle)
    EVT_BUTTON(XRCID("triangle"), Sidebar::OnMarkTriangle)
    EVT_BUTTON(XRCID("cross"), Sidebar::OnMarkCross)
    EVT_BUTTON(XRCID("text"), Sidebar::OnMarkText)
END_EVENT_TABLE()


Sidebar::Sidebar(MainFrame *parent, int id)
    : wxSashLayoutWindow(static_cast<wxWindow*>(parent), id), frame(parent)
{
    is_on = true;

    SetOrientation(wxLAYOUT_VERTICAL);

    // Initialize on right side
    SetAlignment(wxLAYOUT_RIGHT);
    SetSashVisible(wxSASH_LEFT, TRUE);

    is_right = true;
    clock_warn_flag = 0;
}

void Sidebar::initSidebar()
{
    wxLogDebug("Sidebar::initSidebar()");

    // Load XML resource file
    wxXmlResource::Get()->LoadPanel(this, _T("sidebar"));

    // Load xpm icons
    XRCCTRL(*this, "stone_black_white", wxBitmapButton)->SetBitmapLabel(wxBitmap(stone_xpm));
    XRCCTRL(*this, "stone_black", wxBitmapButton)->SetBitmapLabel(wxBitmap(black_icon_xpm));
    XRCCTRL(*this, "stone_white", wxBitmapButton)->SetBitmapLabel(wxBitmap(white_icon_xpm));
    XRCCTRL(*this, "square", wxBitmapButton)->SetBitmapLabel(wxBitmap(square_xpm));
    XRCCTRL(*this, "circle", wxBitmapButton)->SetBitmapLabel(wxBitmap(circle_xpm));
    XRCCTRL(*this, "triangle", wxBitmapButton)->SetBitmapLabel(wxBitmap(triangle_xpm));
    XRCCTRL(*this, "cross", wxBitmapButton)->SetBitmapLabel(wxBitmap(cross_xpm));
    XRCCTRL(*this, "text", wxBitmapButton)->SetBitmapLabel(wxBitmap(text_xpm));
    XRCCTRL(*this, "number", wxBitmapButton)->SetBitmapLabel(wxBitmap(number_xpm));

    // Save default background color
    buttonBackgroundColor =  XRCCTRL(*this, "stone_black_white", wxBitmapButton)->GetBackgroundColour();
    clockBackgroundColor = XRCCTRL(*this, "clock_white_panel", wxPanel)->GetBackgroundColour();

    // Mark black play button as selected
    XRCCTRL(*this, "stone_black", wxBitmapButton)->SetBackgroundColour(*wxRED);
    turn_stone_selected = true;

    // TODO
    // Disable unused number button for now
    XRCCTRL(*this, "number", wxBitmapButton)->Enable(false);

    clock_white = XRCCTRL(*this, "clock_white", wxStaticText);
    clock_black = XRCCTRL(*this, "clock_black", wxStaticText);
    caps_white = XRCCTRL(*this, "caps_white", wxStaticText);
    caps_black = XRCCTRL(*this, "caps_black", wxStaticText);
    wxASSERT(clock_white != NULL && clock_black != NULL &&
             caps_white != NULL && caps_black != NULL);

    clockBackgroundColor = XRCCTRL(*this, "clock_white_panel", wxPanel)->GetBackgroundColour();

    setScore(0, 0, 0, 0, 0, 0, 0);
}

void Sidebar::Swap()
{
    is_right = !is_right;

    if (is_right)
    {
        SetAlignment(wxLAYOUT_RIGHT);
        SetSashVisible(wxSASH_RIGHT, FALSE);
        SetSashVisible(wxSASH_LEFT, TRUE);
    }
    else
    {
        SetAlignment(wxLAYOUT_LEFT);
        SetSashVisible(wxSASH_LEFT, FALSE);
        SetSashVisible(wxSASH_RIGHT, TRUE);
    }
}

void Sidebar::Toggle()
{
    is_on = !is_on;
    Show(is_on);
}

void Sidebar::reset(bool edit_tools, bool text_editable)
{
    setTextareaText(wxEmptyString);

    resetAllButtons();

    // Mark black play button as selected
    wxBitmapButton *but = XRCCTRL(*this, "stone_black", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);
    turn_stone_selected = true;

    setCaptures(0, 0);
    setScore(0, 0, 0, 0, 0, 0, 0);

    enableEditTools(edit_tools);

    // Select first notebook panel
    XRCCTRL(*this, "notebook", wxNotebook)->SetSelection(0);

    // Enable or disable editable textctrl
    XRCCTRL(*this, "textarea", wxTextCtrl)->SetEditable(text_editable);

    clock_warn_flag = 0;
}

void Sidebar::enableEditTools(bool enable)
{
    // Enable or disable the Edit notebook panel
    wxPanel *panel = XRCCTRL(*this, "edit_panel", wxPanel);
    if (panel != NULL)
        panel->Enable(enable);
}

void Sidebar::setTextareaText(const wxString &txt)
{
    XRCCTRL(*this, "textarea", wxTextCtrl)->SetValue(txt);
}

void Sidebar::appendTextareaText(const wxString &txt)
{
    XRCCTRL(*this, "textarea", wxTextCtrl)->AppendText(txt);
}

const wxString& Sidebar::getTextareaText()
{
    wxString tmp = XRCCTRL(*this, "textarea", wxTextCtrl)->GetValue();
    return *(new wxString(tmp));
}

void Sidebar::setGameInfo(GameData *data)
{
    wxASSERT(data != NULL);

    // Assemble white name and rank
    wxString tmp = data->whiteName;
    if (!data->whiteRank.IsEmpty())
        tmp += " " + data->whiteRank;
    XRCCTRL(*this, "name_white", wxStaticText)->SetLabel(tmp);

    // Assemble black name and rank
    tmp = data->blackName;
    if (!data->blackRank.IsEmpty())
        tmp += " " + data->blackRank;
    XRCCTRL(*this, "name_black", wxStaticText)->SetLabel(tmp);

    // Others
    XRCCTRL(*this, "komi", wxStaticText)->SetLabel(wxString::Format("%.1f", data->komi));
    XRCCTRL(*this, "handicap", wxStaticText)->SetLabel(wxString::Format("%d", data->handicap));
    XRCCTRL(*this, "byoyomi", wxStaticText)->SetLabel(data->time);
    if (data->igs_type > 0)
    {
        wxString type = wxEmptyString;
        if (data->igs_type == 1)
            type = _("Rated");
        else if (data->igs_type == 2)
            type = _("Free");
        else if (data->igs_type == 3)
            type = _("Teach");
        XRCCTRL(*this, "type", wxStaticText)->SetLabel(type);
    }
}

void Sidebar::resetAllButtons()
{
    wxBitmapButton *but = XRCCTRL(*this, "stone_black_white", wxBitmapButton);
    // Make sure these buttons do exist
    if (but == NULL)
        return;
    but->SetBackgroundColour(buttonBackgroundColor);
    XRCCTRL(*this, "stone_black", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    XRCCTRL(*this, "stone_white", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    XRCCTRL(*this, "square", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    XRCCTRL(*this, "circle", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    XRCCTRL(*this, "triangle", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    XRCCTRL(*this, "cross", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    XRCCTRL(*this, "text", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    XRCCTRL(*this, "number", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
}

void Sidebar::setTurn(Color c)
{
    if (!turn_stone_selected ||
        XRCCTRL(*this, "stone_white", wxBitmapButton) == NULL)
        return;

    if (c == STONE_WHITE)
    {
        XRCCTRL(*this, "stone_white", wxBitmapButton)->SetBackgroundColour(*wxRED);
        XRCCTRL(*this, "stone_black", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    }
    else if (c == STONE_BLACK)
    {
        XRCCTRL(*this, "stone_black", wxBitmapButton)->SetBackgroundColour(*wxRED);
        XRCCTRL(*this, "stone_white", wxBitmapButton)->SetBackgroundColour(buttonBackgroundColor);
    }
}

void Sidebar::OnPass(wxCommandEvent& WXUNUSED(event))
{
    EventPlayMove evt(-1, -1);
    if (frame->getCurrentBoardWindow() != NULL)
        wxPostEvent(frame->getCurrentBoardWindow()->GetEventHandler(), evt);
}

void Sidebar::OnScore(wxCommandEvent& WXUNUSED(event))
{
    bool res = frame->getBoard()->toggleScore();
    if (res)
        // Select score notebook panel
        XRCCTRL(*this, "notebook", wxNotebook)->SetSelection(2);
    XRCCTRL(*this, "score", wxButton)->SetLabel(res ? _("Done") : _("Score"));
    enableEditTools(!res && frame->getGameType() == GAME_TYPE_PLAY);
}

void Sidebar::OnStoneWhite(wxCommandEvent& WXUNUSED(event))
{
    frame->getBoard()->setEditMode(EDIT_MODE_NORMAL);
    frame->getBoard()->setTurn(STONE_WHITE);

    resetAllButtons();
    wxBitmapButton *but = XRCCTRL(*this, "stone_white", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);

    turn_stone_selected = true;
}

void Sidebar::OnStoneBlack(wxCommandEvent& WXUNUSED(event))
{
    frame->getBoard()->setEditMode(EDIT_MODE_NORMAL);
    frame->getBoard()->setTurn(STONE_BLACK);

    resetAllButtons();
    wxBitmapButton *but = XRCCTRL(*this, "stone_black", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);

    turn_stone_selected = true;
}

void Sidebar::OnStone(wxCommandEvent& WXUNUSED(event))
{
    frame->getBoard()->setEditMode(EDIT_MODE_STONE);

    resetAllButtons();
    wxBitmapButton *but = XRCCTRL(*this, "stone_black_white", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);

    turn_stone_selected = false;
}

void Sidebar::OnMarkSquare(wxCommandEvent& WXUNUSED(event))
{
    frame->getBoard()->setEditMode(EDIT_MODE_MARK_SQUARE);

    resetAllButtons();
    wxBitmapButton *but = XRCCTRL(*this, "square", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);

    turn_stone_selected = false;
}

void Sidebar::OnMarkCircle(wxCommandEvent& WXUNUSED(event))
{
    frame->getBoard()->setEditMode(EDIT_MODE_MARK_CIRCLE);

    resetAllButtons();
    wxBitmapButton *but = XRCCTRL(*this, "circle", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);

    turn_stone_selected = false;
}

void Sidebar::OnMarkTriangle(wxCommandEvent& WXUNUSED(event))
{
    frame->getBoard()->setEditMode(EDIT_MODE_MARK_TRIANGLE);

    resetAllButtons();
    wxBitmapButton *but = XRCCTRL(*this, "triangle", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);

    turn_stone_selected = false;
}

void Sidebar::OnMarkCross(wxCommandEvent& WXUNUSED(event))
{
    frame->getBoard()->setEditMode(EDIT_MODE_MARK_CROSS);

    resetAllButtons();
    wxBitmapButton *but = XRCCTRL(*this, "cross", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);

    turn_stone_selected = false;
}

void Sidebar::OnMarkText(wxCommandEvent& WXUNUSED(event))
{
    frame->getBoard()->setEditMode(EDIT_MODE_MARK_TEXT);

    resetAllButtons();
    wxBitmapButton *but = XRCCTRL(*this, "text", wxBitmapButton);
    if (but != NULL)
        but->SetBackgroundColour(*wxRED);

    turn_stone_selected = false;
}

void Sidebar::updateClock(Color col, const wxString &time_str, bool warn)
{
    wxStaticText *clock = col == STONE_WHITE ? clock_white : clock_black;
    clock->SetLabel(time_str);

    // Blink with red background if warn flag is set
    if (warn)
    {
        wxLogDebug("col: %d", col);
        wxPanel *panel =
            col == STONE_WHITE ? XRCCTRL(*this, "clock_white_panel", wxPanel) :
            XRCCTRL(*this, "clock_black_panel", wxPanel);
        wxColour *c = (clock_warn_flag & col) ? &clockBackgroundColor : wxRED;
        clock->SetBackgroundColour(*c);
        panel->SetBackgroundColour(*c);
        panel->Refresh();
        clock_warn_flag ^= col;
    }
}

void Sidebar::resetTimeWarning()
{
    if (clock_warn_flag & STONE_WHITE)
    {
        clock_white->SetBackgroundColour(clockBackgroundColor);
        XRCCTRL(*this, "clock_white_panel", wxPanel)->SetBackgroundColour(clockBackgroundColor);
        XRCCTRL(*this, "clock_white_panel", wxPanel)->Refresh();
    }
    else if (clock_warn_flag & STONE_BLACK)
    {
        clock_black->SetBackgroundColour(clockBackgroundColor);
        XRCCTRL(*this, "clock_black_panel", wxPanel)->SetBackgroundColour(clockBackgroundColor);
        XRCCTRL(*this, "clock_black_panel", wxPanel)->Refresh();
    }
    clock_warn_flag = 0;
}

void Sidebar::setCaptures(unsigned short white, unsigned short black)
{
    caps_white->SetLabel(wxString::Format("%u", white));
    caps_black->SetLabel(wxString::Format("%u", black));
}

void Sidebar::setScore(int terrWhite, int capsWhite, float finalWhite,
			  	       int terrBlack, int capsBlack, int finalBlack,
					   int dame)
{
    XRCCTRL(*this, "score_terr_white", wxStaticText)->SetLabel(wxString::Format("%d", terrWhite));
    XRCCTRL(*this, "score_caps_white", wxStaticText)->SetLabel(wxString::Format("%d", capsWhite));
    if (finalWhite - static_cast<int>(finalWhite))
        XRCCTRL(*this, "score_final_white", wxStaticText)->SetLabel(wxString::Format("%.1f", finalWhite));
    else
        XRCCTRL(*this, "score_final_white", wxStaticText)->SetLabel(wxString::Format("%d", static_cast<int>(finalWhite)));
    XRCCTRL(*this, "score_terr_black", wxStaticText)->SetLabel(wxString::Format("%d", terrBlack));
    XRCCTRL(*this, "score_caps_black", wxStaticText)->SetLabel(wxString::Format("%d", capsBlack));
    XRCCTRL(*this, "score_final_black", wxStaticText)->SetLabel(wxString::Format("%d", finalBlack));
    XRCCTRL(*this, "score_dame", wxStaticText)->SetLabel(wxString::Format("%d", dame));
}


// ------------------------------------------------------------------------
//                            Class SidebarGTP
// ------------------------------------------------------------------------

BEGIN_EVENT_TABLE(SidebarGTP, Sidebar)
    EVT_BUTTON(XRCID("undo"), SidebarGTP::OnUndo)
END_EVENT_TABLE()


void SidebarGTP::initSidebar()
{
    wxLogDebug("SidebarGTP::initSidebar()");

    // Load XML resource file
    wxXmlResource::Get()->LoadPanel(this, _T("sidebar_gtp"));

    clock_white = XRCCTRL(*this, "clock_white", wxStaticText);
    clock_black = XRCCTRL(*this, "clock_black", wxStaticText);
    caps_white = XRCCTRL(*this, "caps_white", wxStaticText);
    caps_black = XRCCTRL(*this, "caps_black", wxStaticText);
    wxASSERT(clock_white != NULL && clock_black != NULL &&
             caps_white != NULL && caps_black != NULL);

    clockBackgroundColor = XRCCTRL(*this, "clock_white_panel", wxPanel)->GetBackgroundColour();
}

void SidebarGTP::OnUndo(wxCommandEvent& WXUNUSED(event))
{
#ifndef NOT_GTP

    // Our turn?
    if (!wxGetApp().getGTP()->mayMove())
    {
        playSound(SOUND_BEEP);
        return;
    }

    // Send two "undo" commands to GTP engine and tell board to undo two moves
    EventGTPCommand evt(_T("undo"), GTP_COMMAND_SEND);
    for (int i=0; i<2; i++)
    {
        wxPostEvent(wxGetApp().getGTP(), evt);
        frame->getBoard()->undoMove();
    }
#endif
}


// ------------------------------------------------------------------------
//                            Class SidebarObserve
// ------------------------------------------------------------------------

BEGIN_EVENT_TABLE(SidebarObserve, Sidebar)
    EVT_BUTTON(XRCID("edit"), SidebarObserve::OnEditGame)
    EVT_BUTTON(XRCID("observers"), SidebarObserve::OnObservers)
    EVT_TEXT_ENTER(XRCID("kibitz_input"), SidebarObserve::OnCommandEnter)
END_EVENT_TABLE()


    void SidebarObserve::initSidebar()
{
    wxLogDebug("SidebarObserve::initSidebar()");

    // Load XML resource file
    wxXmlResource::Get()->LoadPanel(this, _T("sidebar_observe"));
    input = new InputCtrl(this, -1);
    wxASSERT(input != NULL);
    wxXmlResource::Get()->AttachUnknownControl("kibitz_input", input);

    clock_white = XRCCTRL(*this, "clock_white", wxStaticText);
    clock_black = XRCCTRL(*this, "clock_black", wxStaticText);
    caps_white = XRCCTRL(*this, "caps_white", wxStaticText);
    caps_black = XRCCTRL(*this, "caps_black", wxStaticText);
    wxASSERT(clock_white != NULL && clock_black != NULL &&
             caps_white != NULL && caps_black != NULL);

    clockBackgroundColor = XRCCTRL(*this, "clock_white_panel", wxPanel)->GetBackgroundColour();
}

void SidebarObserve::OnEditGame(wxCommandEvent& WXUNUSED(event))
{
    // Save game to tmpFile
    wxString tmpFile = frame->saveTempFile();
    if (tmpFile.empty())
        return;

    // Open new board loading from tmpFile
    MainFrame *new_frame = wxGetApp().newMainFrame(GAME_TYPE_PLAY, tmpFile, true);
    if (new_frame == NULL)
        return;

    // Navigate to last move
    if (frame->getCurrentBoardWindow() != NULL)
    {
        EventNavigate evt(NAVIGATE_DIRECTION_LAST_MOVE);
        wxPostEvent(new_frame->getCurrentBoardWindow()->GetEventHandler(), evt);
    }

    // Delete tmpFile
    wxRemoveFile(tmpFile);
}

void SidebarObserve::OnObservers(wxCommandEvent& WXUNUSED(event))
{
    EventIGSCommand evt(wxString::Format(_T("all %d"), frame->getGameID()));
    wxPostEvent(frame->GetEventHandler(), evt);
}

void SidebarObserve::OnCommandEnter(wxCommandEvent& WXUNUSED(event))
{
    wxString cmd = input->GetValue();
    if (cmd.empty())
        return;
    input->Clear();

    EventIGSCommand evt(wxString::Format(_T("kibitz %d %s"), frame->getGameID(), cmd.c_str()));
    wxPostEvent(frame->GetEventHandler(), evt);

    // No feedback from IGS
    appendTextareaText(frame->getMyName() + ": " + cmd + "\n");
}


// ------------------------------------------------------------------------
//                            Class SidebarIGSPlay
// ------------------------------------------------------------------------

BEGIN_EVENT_TABLE(SidebarIGSPlay, Sidebar)
    EVT_BUTTON(XRCID("pass"), SidebarIGSPlay::OnPass)
    EVT_BUTTON(XRCID("resign"), SidebarIGSPlay::OnResign)
    EVT_BUTTON(XRCID("adjourn"), SidebarIGSPlay::OnAdjourn)
    EVT_BUTTON(XRCID("observers"), SidebarIGSPlay::OnObservers)
    EVT_BUTTON(XRCID("undo"), SidebarIGSPlay::OnUndo)
    EVT_TEXT_ENTER(XRCID("say_input"), SidebarIGSPlay::OnCommandEnter)
END_EVENT_TABLE()


void SidebarIGSPlay::initSidebar()
{
    wxLogDebug("SidebarIGSPlay::initSidebar()");

    // Load XML resource file
    wxXmlResource::Get()->LoadPanel(this, _T("sidebar_igsplay"));
    input = new InputCtrl(this, -1);
    wxASSERT(input != NULL);
    wxXmlResource::Get()->AttachUnknownControl("say_input", input);

    clock_white = XRCCTRL(*this, "clock_white", wxStaticText);
    clock_black = XRCCTRL(*this, "clock_black", wxStaticText);
    caps_white = XRCCTRL(*this, "caps_white", wxStaticText);
    caps_black = XRCCTRL(*this, "caps_black", wxStaticText);
    wxASSERT(clock_white != NULL && clock_black != NULL &&
             caps_white != NULL && caps_black != NULL);

    clockBackgroundColor = XRCCTRL(*this, "clock_white_panel", wxPanel)->GetBackgroundColour();

    scoreMode = false;
}

void SidebarIGSPlay::OnPass(wxCommandEvent& WXUNUSED(event))
{
    EventIGSCommand evt(wxString::Format(_T("%s %d"),
                                         // When scoring, "Pass" button turned into "Done"
                                         !scoreMode ? _T("pass") : _T("done"),
                                         frame->getGameID()));
    wxPostEvent(frame->GetEventHandler(), evt);

    if (scoreMode)
        frame->appendComment(_T("You have typed done."));  // No feedback from IGS for "done"
}

void SidebarIGSPlay::OnResign(wxCommandEvent& WXUNUSED(event))
{
    if (wxMessageBox(_("Do you really want to resign the game?"), _("Resign?"),
                     wxYES_NO | wxICON_EXCLAMATION, frame) == wxNO)
        return;

    EventIGSCommand evt(wxString::Format(_T("resign %d"), frame->getGameID()));
    wxPostEvent(frame->GetEventHandler(), evt);
}

void SidebarIGSPlay::OnAdjourn(wxCommandEvent& WXUNUSED(event))
{
    EventIGSCommand evt(wxString::Format(_T("adjourn %d"), frame->getGameID()));
    wxPostEvent(frame->GetEventHandler(), evt);
}

void SidebarIGSPlay::OnObservers(wxCommandEvent& WXUNUSED(event))
{
    EventIGSCommand evt(wxString::Format(_T("all %d"), frame->getGameID()));
    wxPostEvent(frame->GetEventHandler(), evt);
}

void SidebarIGSPlay::OnUndo(wxCommandEvent& WXUNUSED(event))
{
    EventIGSCommand evt(wxString::Format(_T("undo %d"), frame->getGameID()));
    wxPostEvent(frame->GetEventHandler(), evt);
}

void SidebarIGSPlay::OnCommandEnter(wxCommandEvent& WXUNUSED(event))
{
    wxString cmd = input->GetValue();
    if (cmd.empty())
        return;
    input->Clear();

    EventIGSCommand evt(wxString::Format(_T("say %s"), cmd.c_str()));
    wxPostEvent(frame->GetEventHandler(), evt);

    // No feedback from IGS
    appendTextareaText(frame->getMyName() + ": " + cmd + "\n");
}

void SidebarIGSPlay::enterScoreMode()
{
    // Select score notebook panel
    XRCCTRL(*this, "notebook", wxNotebook)->SetSelection(2);
    // Turn pass button into "Done"
    XRCCTRL(*this, "pass", wxButton)->SetLabel(_("Done"));
    scoreMode = true;
}
