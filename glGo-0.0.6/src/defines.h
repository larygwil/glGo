/*
 * defines.h
 *
 * $Id: defines.h,v 1.66 2003/11/24 16:02:10 peter Exp $
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

/**
 * @file
 * This header file includes common defines, macros and enums used from most
 * classes of the application.
 */

#ifndef DEFINES_H
#define DEFINES_H

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

/*
 * General
 */

// If you don't have config.h from automake, define PACKAGE and VERSION here.
#ifndef PACKAGE
#define PACKAGE  _T("glGo")
#endif
#ifndef VERSION
#define VERSION  "0.0.6"
#endif
#define VENDOR   _T("Peter Strempel")

// Do we use MS HTML Help on Win32?
#ifdef __WXMSW__
#define USE_MSHTMLHELP
#endif


/*
 * Board disply
 */

// Board defaults
#define DEFAULT_BOARD_SIZE 19
#define DEFAULT_KOMI       6.5f
#define DEFAULT_HANDICAP   0

// Game data min/max values
#define BOARD_SIZE_MIN   9
#define BOARD_SIZE_MAX  19
#define HANDICAP_MIN     2
#define HANDICAP_MAX     9

/** Board display types. OpenGL (3D) or SDL (2D) */
enum BoardDisplayType
{
    DISPLAY_TYPE_SDL,
    DISPLAY_TYPE_OPENGL
};


/*
 * Game logic
 */

/** Stone color */
enum Color
{
    STONE_UNDEFINED,      ///< Unknown color. Used for empty nodes
    STONE_WHITE,          ///< White stone
    STONE_BLACK,          ///< Black stone
    STONE_REMOVED_WHITE,  ///< Removed white stone
    STONE_REMOVED_BLACK,  ///< Removed black stone
    STONE_REMOVED         ///< Removed stone, unknown color
};

/** %Game navigation */
enum NavigationDirection
{
    NAVIGATE_DIRECTION_INVALID,
    NAVIGATE_DIRECTION_NEXT_MOVE,
    NAVIGATE_DIRECTION_PREVIOUS_MOVE,
    NAVIGATE_DIRECTION_FIRST_MOVE,
    NAVIGATE_DIRECTION_LAST_MOVE,
    NAVIGATE_DIRECTION_NEXT_VARIATION,
    NAVIGATE_DIRECTION_PREVIOUS_VARIATION
};

/** Frame and other IDs */
enum
{
    ID_WINDOW_MAINFRAME = wxID_HIGHEST + 10,
    ID_WINDOW_BOARD,
    ID_WINDOW_SIDEBAR,
    ID_WINDOW_IGSMAINFRAME,
    ID_CLOCK_TIMER,
    ID_SOUND_TOGGLE
};

/** General View parameters */
enum ViewParam
{
    VIEW_SHOW_MARKS,
    VIEW_SHOW_COORDS,
    VIEW_SHOW_CURSOR,
    VIEW_USE_SCALED_FONT,
    VIEW_USE_BACKGROUND_IMAGE
};

/** SGF editor modes */
enum EditMode
{
    EDIT_MODE_NORMAL,
    EDIT_MODE_STONE,
    EDIT_MODE_MARK_SQUARE,
    EDIT_MODE_MARK_CIRCLE,
    EDIT_MODE_MARK_TRIANGLE,
    EDIT_MODE_MARK_CROSS,
    EDIT_MODE_MARK_TEXT,
    EDIT_MODE_MARK_NUMBER,
    EDIT_MODE_SCORE
};

/** Current game type of a MainFrame */
enum GameType
{
    GAME_TYPE_PLAY,         ///< Default mode
    GAME_TYPE_GTP,          ///< GTP mode
    GAME_TYPE_IGS_OBSERVE,  ///< IGS observed game
    GAME_TYPE_IGS_PLAY      ///< IGS played game
};


/*
 * Sound
 */

/** Sounds. They need to be loaded in exactly this order into the OAL/SDL buffers. */
enum Sound
{
    SOUND_STONE,
    SOUND_PASS,
    SOUND_CHAT,
    SOUND_BEEP,
    SOUND_MATCH,
    SOUND_TIMEWARN
};


/*
 * Logging
 */

/** User defined log levels */
enum
{
    LOG_GLOBAL = wxLOG_User,
    LOG_BOARD,
    LOG_OPENGL,
    LOG_SDL,
    LOG_SOUND,
    LOG_SGF,
    LOG_IGS,
    LOG_GTP
};

#define LOG(l,s)       wxLogGeneric(l, s)   ///< Write logs specifying the loglevel
#define LOG_GLOBAL(s)  LOG(LOG_GLOBAL, s)   ///< Write general logs
#define LOG_BOARD(s)   LOG(LOG_BOARD, s)    ///< Write board and game related logs
#define LOG_OPENGL(s)  LOG(LOG_OPENGL, s)   ///< Write OpenGL related logs
#define LOG_SDL(s)     LOG(LOG_SDL, s)      ///< Write SDL related logs
#define LOG_SOUND(s)   LOG(LOG_SOUND, s)    ///< Write OpenAL related logs
#define LOG_SGF(s)     LOG(LOG_SGF, s)      ///< Write SGF related logs
#define LOG_IGS(s)     LOG(LOG_IGS, s)      ///< Write IGS related logs
#define LOG_GTP(s)     LOG(LOG_GTP, s)      ///< Write GTP related logs


/*
 * Common strings
 */

#define ABOUT_TEXT _("\n%s %s\n\n" \
                     "A prototype for a 3D Go board based on OpenGL and wxWindows.\n\n" \
                     "Written by %s\n\n" \
                     "English translation by Peter Strempel")
#define NO_GTP_ERROR_MESSAGE _("This version does not support GTP.")

#endif
