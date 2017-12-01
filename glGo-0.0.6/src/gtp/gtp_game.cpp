/*
 * gtp_game.cpp
 *
 * $Id: gtp_game.cpp,v 1.14 2003/10/21 08:45:58 peter Exp $
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
#pragma implementation "gtp_game.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/app.h>
#include <wx/log.h>
#include <wx/intl.h>
#endif

#include "gtp_eventhandler.h"
#include "gtp_game.h"
#include "gtp.h"
#include "stone.h"
#include "board.h"
#include "boardhandler.h"
#include "game.h"
#include "utils/utils.h"


GTPGame::GTPGame(GTP *const gtp, GTPConfig *conf)
    : gtp(gtp), gtp_config(conf)
{
    wxASSERT(gtp != NULL && gtp_config != NULL);
    wxLogDebug(_T("GTPGame constructor"));

    state = lastState = GTP_STATE_UNKNOWN;
    lastMoveWasPass = false;
    lastHumanX = lastHumanY = -1;
}

void GTPGame::sendGTPCommand(wxString command)
{
    EventGTPCommand evt(command, GTP_COMMAND_SEND);
    wxPostEvent(gtp, evt);
}

bool GTPGame::mayMove(Color color)
{
    if (color == STONE_UNDEFINED)
        color = gtp->getBoard()->getBoardHandler()->getCurrentTurnColor();

    if (state == GTP_STATE_SCORING)
        return false;
    if (state == GTP_STATE_DONE)
        return false;
    if (color == STONE_BLACK)
        return gtp_config->black == GTP_HUMAN;
    return gtp_config->white == GTP_HUMAN;
}

void GTPGame::initGame(GTPConfig *conf)
{
    // If a new GTPConfig object was passed, delete old and use new one
    if (conf != NULL)
    {
        if (gtp_config != NULL)
            delete gtp_config;
        gtp_config = conf;
    }

#if 0
    // Adjust "No time" settings
    int mainTime = 0;
    int byoTime = 0;
    int byoStones = -1;
    if (gtpConfig.getTimeSystem() != GTP_TIME_SYSTEM_NO_TIME) {
        mainTime = gtpConfig.getMainTime();
        byoTime = gtpConfig.getByoYomiTime();
        byoStones = gtpConfig.getByoYomiStones();
    }

    // Init clocks
    GTPConnection.getBoard().getClockWhite().init(mainTime, byoTime, byoStones);
    GTPConnection.getBoard().getClockBlack().init(mainTime, byoTime, byoStones);
    if (gtp_config->black == GTP_HUMAN)
        GTPConnection.getBoard().getClockBlack().setIsMyClock(true);
    if (gtp_config->white == GTP_HUMAN)
        GTPConnection.getBoard().getClockWhite().setIsMyClock(true);
    // TODO: Adjust clocks for resume. Needs SGF clock tags. Not supported yet.
#endif

    lastMoveWasPass = false;

    // Resumed game?
    if (!(gtp_config->resumeFileName).empty())
    {
        wxLogDebug(_T("GTP resume game: %s"), gtp_config->resumeFileName.c_str());

        // Keep a copy of the GTPConfig object, as loadGame will delete the current
        GTPConfig *old_config = new GTPConfig(*gtp_config);

        // Load game
        wxASSERT(gtp->getBoard() != NULL);
        if (gtp->getBoard()->loadGame(old_config->resumeFileName))
        {
            // Reallocate gtp_config pointer and copy data.
            gtp_config = new GTPConfig(*(gtp->getBoard()->getBoardHandler()->getGame()->getGameData()));
            gtp_config->black = old_config->black;
            gtp_config->white = old_config->white;
            gtp_config->level = old_config->level;
            gtp_config->gtp_path = old_config->gtp_path;
            gtp_config->resumeFileName = old_config->resumeFileName;
            delete old_config;
            // Replace GameData object in Game class
            gtp->getBoard()->getBoardHandler()->getGame()->setGameData(static_cast<GameData*>(gtp_config));

            // Move to last move. Not as event as GTPEventHandler blocks navigation
            gtp->getBoard()->getBoardHandler()->lastMove();

            // Tell GTP engine to load file
            state = GTP_STATE_RESUME_GAME;
            sendGTPCommand(wxString::Format(_T("loadsgf %s"), (gtp_config->resumeFileName).c_str()));
            // GTP will send which turn it is and call startGame(Color)

            return;
        }
        else
            // Loading failed, clear resumeFileName and let a new game start
            gtp_config->resumeFileName = wxEmptyString;
    }

    // Adjust "Computer" name to "<name> <version>", for example "GNU GO 3.4"
    state = GTP_STATE_SETUP_GAME_NAME;
    sendGTPCommand(_T("name"));
    sendGTPCommand(_T("version"));

    // New game (recheck so if loading above failed, we start the new game here)
    if ((gtp_config->resumeFileName).empty())
    {
        sendGTPCommand(wxString::Format(_T("boardsize %d"), gtp_config->board_size));
        sendGTPCommand(wxString::Format(_T("komi %f"), gtp_config->komi));
        sendGTPCommand(wxString::Format(_T("level %d"), gtp_config->level));

        if (gtp_config->handicap < 2)
            turn = STONE_BLACK;
        else
        {
            turn = STONE_WHITE;
            sendGTPCommand(wxString::Format(_T("fixed_handicap %d"), gtp_config->handicap));
        }
    }

}

void GTPGame::startGame(Color color)
{
    wxLogDebug(_T("GTPGame::startGame(Color): %d"), color);

    if (color != STONE_UNDEFINED)
        turn = color;

#if 0
    // TODO
    // Start clock
    if (turn == STONE_BLACK)
        GTPConnection.getBoard().getClockBlack().start();
    else
        GTPConnection.getBoard().getClockWhite().start();
#endif

    generateMove();
}

void GTPGame::generateMove()
{
    if (turn == STONE_WHITE)
    {
        state = GTP_STATE_MOVE_WHITE;

        if (gtp_config->white == GTP_COMPUTER)
            sendGTPCommand(_T("genmove_white"));
    }
    else
    {
        state = GTP_STATE_MOVE_BLACK;

        if (gtp_config->black == GTP_COMPUTER)
            sendGTPCommand(_T("genmove_black"));
    }
}

Color GTPGame::switchTurn()
{
    Color color = STONE_UNDEFINED;

    if (turn == STONE_BLACK)
    {
        color = STONE_BLACK;
        turn = STONE_WHITE;
        state = GTP_STATE_MOVE_BLACK;
#if 0
        GTPConnection.getBoard().getClockBlack().stop();
        if (!GTPConnection.getBoard().getClockBlack().playStone()) {
            finishGame();
            gGoDialog.showNonModalMessageDialog(
                GTPConnection.getBoard(),
                MessageFormat.format(gGo.getIGSResources().
                                     getString("out_of_time_message_observe"), new Object[]{"Black"}),
                gGo.getIGSResources().getString("Information"));
        }
        else
            GTPConnection.getBoard().getClockWhite().start();
#endif
    }
    else
    {
        color = STONE_WHITE;
        turn = STONE_BLACK;
        state = GTP_STATE_MOVE_WHITE;
#if 0
        GTPConnection.getBoard().getClockWhite().stop();
        if (!GTPConnection.getBoard().getClockWhite().playStone()) {
            finishGame();
            gGoDialog.showNonModalMessageDialog(
                GTPConnection.getBoard(),
                MessageFormat.format(gGo.getIGSResources().
                                     getString("out_of_time_message_observe"), new Object[]{"White"}),
                gGo.getIGSResources().getString("Information"));
        }
        else
            GTPConnection.getBoard().getClockBlack().start();
#endif
    }

    return color;
}

void GTPGame::receiveMoveFromGTP(short x, short y)
{
    wxLogDebug(_T("GTPGame::receiveMoveFromGTP(): %d/%d"), x, y);

    // Send move to board
    EventPlayGTPMove evt(x, y, switchTurn());
    wxPostEvent(gtp->getGTPEventhandler(), evt);

    if (!checkGameFinished(x, y))
        generateMove();
}

void GTPGame::receiveMoveFromHuman(short x, short y, Color color)
{
    wxLogDebug(_T("GTPGame::receiveMoveFromHuman(): %d/%d %d"), x, y, color);

    // Verify turn
    if (color != turn)
    {
        wxLogDebug(_T("Error: Turn mismatches!"));
        return;
    }

    // Remember the move, we are still waiting for confirmation from the GTP engine
    lastHumanX = x;
    lastHumanY = y;
    state = GTP_STATE_CONFIRMING_MOVE;

    // Send move to GTP
    wxString command;
    if (color == STONE_WHITE)
        command = _T("white ");
    else if (color == STONE_BLACK)
        command = _T("black ");
    if (x == -1 && y == -1)
        command += _T("pass");
    else
    {
        char cx = 'A' + (x < 9 ? x : x + 1) - 1;
        command += cx;
        command += wxString::Format("%d", gtp_config->board_size - y + 1);
    }
    sendGTPCommand(command);
}

void GTPGame::confirmMove(bool confirmed)
{
    wxLogDebug(_T("GTPGame::confirmMove %d"), confirmed);

    if (confirmed)
    {
        // Send move to board
        EventPlayMove evt(lastHumanX, lastHumanY, turn);
        evt.setOk(true);  // Set Ok flag so the GTPEventhandler can differ this from
                          // moves which come from the board as response to mouseclicks
        wxPostEvent(gtp->getGTPEventhandler(), evt);

        switchTurn();

        if (!checkGameFinished(lastHumanX, lastHumanY))
            generateMove();
    }
    // Move was illegal. Beep and abort
    else
        playSound(SOUND_BEEP);

    lastHumanX = lastHumanY = -1;
}

void GTPGame::recieveHandicapFromGTP(Position **positions)
{
    // Send handicap to board
    // TODO: Make two handicap setup functions: fixed and non fixed. For non-fixed
    // we use the parsed array.
    EventHandicapSetup evt(gtp_config->handicap, positions);
    wxPostEvent(gtp->getGTPEventhandler(), evt);

    state = GTP_STATE_MOVE_WHITE;
    startGame();

    // TODO: Well, we parse handicap, but don't use it but use our own fixed
    // position. Argh!
    // This *MUST* be deleted in the event receiver instead, else segfault !!!
    for (unsigned short i=0; i<gtp_config->handicap; i++)
        delete positions[i];
    delete[] positions;
}

bool GTPGame::checkGameFinished(short x, short y)
{
    if (state == GTP_STATE_DONE)
        return true;

    lastState = state;

    // Pass?
    if (x == -1 && y == -1)
    {
        if (lastMoveWasPass)
        {
            state = GTP_STATE_SCORING;
#if 0
            // TODO
            GTPConnection.getBoard().switchMode(MODE_SCORE);
            // Stop clocks
            GTPConnection.getBoard().getClockBlack().stop();
            GTPConnection.getBoard().getClockWhite().stop();
#endif
            return true;
        }
        lastMoveWasPass = true;
    }
    else
        lastMoveWasPass = false;
    return false;
}
