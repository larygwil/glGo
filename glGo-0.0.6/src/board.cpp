/*
 * Board.cpp
 *
 * $Id: board.cpp,v 1.41 2003/11/22 17:16:12 peter Exp $
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
#pragma implementation "board.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/wx.h>
#endif

#include <wx/config.h>
#include "boardhandler.h"
#include "board.h"
#include "game.h"
#include "move.h"
#include "mainframe.h"
#include "sidebar.h"
#include "glGo.h"
#ifndef NOT_GTP
#include "gtp.h"
#endif


Board::Board(MainFrame *parent)
    : parentFrame(parent)
{
    board_size = DEFAULT_BOARD_SIZE;
    editMode = oldEditMode = EDIT_MODE_NORMAL;
    is_modified = false;
    blocked = false;
    boardhandler = new BoardHandler(this);

    // Init view parameters from config
    wxConfig::Get()->Read(_T("Board/ShowCoords"), &show_coords, true);
    wxConfig::Get()->Read(_T("Board/ShowMarks"), &show_marks, true);
    wxConfig::Get()->Read(_T("Board/ShowCursor"), &show_cursor, true);
}

Board::~Board()
{
    delete boardhandler;
}

bool Board::isModified() const
{
    return boardhandler->isModified();
}

void Board::setTurn(Color c)
{
    boardhandler->getGame()->setCurrentTurnColor(c);
}

void Board::updateMainframe(bool force_clock_update)
{
    wxASSERT(parentFrame != NULL && boardhandler != NULL && boardhandler->getGame() != NULL &&
             boardhandler->getGame()->getCurrentMove() != NULL);

    Move *m = boardhandler->getGame()->getCurrentMove();
    if (m == NULL)
        return;

    // Assemble something like "(Black Q16)" or "(White Pass)" (x/y = 20/20)
    // or "" (x/y = 0/0 or color is undefined)
    wxString move_str;
    if ((m->getX() == 0 && m->getY() == 0) || (m->getColor() != STONE_BLACK && m->getColor() != STONE_WHITE))
        move_str = wxEmptyString;
    else
    {
        move_str = "   (";
        move_str += m->getColor() == STONE_BLACK ? _("Black") :
            m->getColor() == STONE_WHITE ? _("White") : wxEmptyString;
        move_str += " ";
        if ((m->getX() == -1 && m->getY() == -1) || (m->getX() == board_size+1 && m->getY() == board_size+1))
            move_str += _("Pass");
        else
            move_str += wxString::Format("%c%d",
                                         'A' + (m->getX() < 9 ? m->getX()-1 : m->getX()),
                                         board_size - m->getY() + 1);
        move_str += ")";
    }

    EventInterfaceUpdate event(m->getNumber(),
                               m->getBrothers(),
                               m->getSons(),
                               boardhandler->getGame()->getCurrentTurnColor(),
                               m->getCapsWhite(),
                               m->getCapsBlack(),
                               move_str,
                               boardhandler->getComment(),
                               force_clock_update);
    wxPostEvent(parentFrame, event);
}

bool Board::loadGame(const wxString &filename, bool is_tmp_filename)
{
    bool res = boardhandler->loadGame(filename, parentFrame, is_tmp_filename);
    if (res)
        setupGame(boardhandler->getGame()->getGameData());
    return res;
}

bool Board::saveGame(const wxString &filename, bool dont_remember)
{
    return boardhandler->saveGame(filename, dont_remember);
}

void Board::displayMessageBox(const wxString &message, const wxString &caption, int style)
{
    wxASSERT(parentFrame != NULL);
    wxMessageBox(message, caption, style, parentFrame);
}

void Board::handleMouseClick(int x, int y, int button)
{
    wxASSERT(editMode != EDIT_MODE_NORMAL);  // Has to be handled in subclass

    bool res = true;

    // Editing stones: Left button adds black, right adds white stone,
    // Any button on an occupied spot removes any stone
    if (editMode == EDIT_MODE_STONE)
    {
        // Occupied?
        if (!boardhandler->hasPosition(Position(x, y)))
        {
            // Left adds black stone
            if (!button)
                boardhandler->addEditStone(x, y, STONE_BLACK);
            else
                // Right adds white stone
                boardhandler->addEditStone(x, y, STONE_WHITE);
        }
        else
            // Remove stone
            boardhandler->removeEditStone(x, y);
    }

    // Editing marks: Left button adds, right removes marks
    else if (editMode != EDIT_MODE_SCORE)
    {
        MarkType t;
        switch (editMode)
        {
        case EDIT_MODE_MARK_SQUARE:
            t = MARK_SQUARE;
            break;
        case EDIT_MODE_MARK_CIRCLE:
            t = MARK_CIRCLE;
            break;
        case EDIT_MODE_MARK_TRIANGLE:
            t = MARK_TRIANGLE;
            break;
        case EDIT_MODE_MARK_CROSS:
            t = MARK_CROSS;
            break;
        case EDIT_MODE_MARK_TEXT:
            t = MARK_TEXT;
            break;
        default:
            wxFAIL_MSG(wxString::Format(_T("Edit mode %d not implemented"), editMode));
            return;
        }

        if (!button)  // Left mouse - add mark
            res = boardhandler->addMark(x, y, t);
        else          // Right mouse - remove mark
            res = boardhandler->removeMark(x, y, t);
    }

    // In score mode left button marks or unmarks stones dead, right button
    // marks or unmarks stones as alive in seki
    else
    {
        wxLogDebug("MARK DEAD/SEKI");
        res = boardhandler->markStoneDeadOrSeki(Position(x, y), !button);
    }

    if (res)
    {
        // Make sure board gets updated on next OnIdle
        is_modified = true;

        // Mark game as modified
        boardhandler->setModified();
        parentFrame->updateTitle();
    }
}

void Board::updateClock(Color col, int time, short stones)
{
    if (col == STONE_WHITE)
    {
        clock_white.setCurrentTime(time, stones);
        clock_white.Stop();
        clock_black.Start();
    }
    else if (col == STONE_BLACK)
    {
        clock_black.setCurrentTime(time, stones);
        clock_black.Stop();
        clock_white.Start();
    }
    else
    {
        LOG_BOARD("Board::updateClock::Invalid color");
        wxFAIL_MSG("Board::updateClock::Invalid color");
    }
}

void Board::undoMove()
{
    boardhandler->deleteCurrentMove();
}

void Board::InitClocks(int white_time, int black_time)
{
    clock_white.setCurrentTime(white_time);
    clock_black.setCurrentTime(black_time);
}

void Board::StartClock(int c)
{
    wxLogDebug("StartClock: %d", c);
    if (c & STONE_WHITE)
        clock_white.Start();
    if (c & STONE_BLACK)
        clock_black.Start();
}

void Board::StopClock(int c)
{
    wxLogDebug("StopClock: %d", c);
    if (c & STONE_WHITE)
        clock_white.Stop();
    if (c & STONE_BLACK)
        clock_black.Stop();
}

int Board::TickClock(int c)
{
    if (c & STONE_WHITE)
        return clock_white.Tick();
    if (c & STONE_BLACK)
        return clock_black.Tick();
    return 0;
}

void Board::SetByoTime(int c, int byo)
{
    if (c & STONE_WHITE)
        clock_white.setByoTime(byo * 60);
    if (c & STONE_BLACK)
        clock_black.setByoTime(byo * 60);
}

bool Board::mayMove()
{
    switch (parentFrame->getGameType())
    {
    case GAME_TYPE_PLAY:
        return true;
    case GAME_TYPE_IGS_OBSERVE:
        return false;
    case GAME_TYPE_IGS_PLAY:
        return boardhandler->getCurrentTurnColor() == myColor;
    case GAME_TYPE_GTP:
        return wxGetApp().getGTP()->mayMove();
    default:
        wxFAIL_MSG("Board::mayMove(): Invalid game type");
        return false;
    }
}

bool Board::toggleScore()
{
    is_modified = true;

    // Normal -> Score mode
    if (editMode != EDIT_MODE_SCORE)
    {
        oldEditMode = editMode;
        editMode = EDIT_MODE_SCORE;
        boardhandler->score();
        return true;
    }

    // Score -> Normal mode
    editMode = oldEditMode;
    boardhandler->finishScore();
    return false;
}

void Board::displayScoreResult(int terrWhite, int capsWhite, float finalWhite,
                               int terrBlack, int capsBlack, int finalBlack,
                               int dame)
{
    parentFrame->getSidebar()->setScore(terrWhite, capsWhite, finalWhite, terrBlack, capsBlack, finalBlack, dame);
}

void Board::displayComment(const wxString &txt)
{
    parentFrame->appendComment(txt);
}

void Board::removeDeadStone(short x, short y)
{
    if (boardhandler->markStoneDeadOrSeki(Position(x, y)))
        is_modified = true;
}

bool Board::getPlayLocalSound() const
{
    return parentFrame->getPlayLocalSound();
}
