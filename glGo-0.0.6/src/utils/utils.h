/*
 * utils.h
 *
 * $Id: utils.h,v 1.28 2003/11/21 03:31:27 peter Exp $
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
 * @defgroup utils Utilities
 *
 * Global utilities used by various parts of the application.
 *
 * @{
 */

/**
 * @file
 * Global utility functions.
 */

#ifndef UTILS_H
#define UTILS_H

#include "defines.h"

/** OpenGL View configuration */
struct OGLConfig
{
    bool reflections, shadows, render_to_texture, blur, fast_rendering, blending,
        antialias_lines, antialias_stones, antialias_scene, textures, multitextures,
		use_scissor;
    int antialias_scene_quality, textures_quality, stone_quality;
};

/** Global application settings */
struct Settings
{
    int language, board_display_type, sound_system;
    bool autohide, global_sound, tooltips, localserver;
#ifdef __WXMSW__
    bool minimize_to_tray;
#endif
};

/** IGS settings */
struct IGSSettings
{
    wxString loginname, password;
    bool autoconnect, shouts_in_terminal, skip_guests, show_obs_msgbox, ayt_timer, chat_sound, match_sound, timewarn_sound,
        all_friends, autosave_own, autosave_observed;
    int timewarn_threshold;
};

/**
 * Get the reverse color.
 * STONE_BLACK -> STONE_WHITE and STONE_WHITE -> STONE_BLACK
 * @param c The color we want to reverse
 * @return The reversed color
 */
inline Color reverseColor(const Color &c)
{
    return ((c-1) % 2) ? STONE_WHITE : STONE_BLACK;
}

/**
 * Parse a move string in SGF format into integer coordinates.
 * For example, kk -> 10/10
 * @param s String to parse
 * @param x Reference which will store the x coord result
 * @param y Reference which will store the y coord result
 */
void parseSGFStringMove(const wxString &s, unsigned short &x, unsigned short &y);

/**
 * Parse a move string into integer coordinates.
 * Example: Q16 -> 4/16. This format is used by GTP and IGS.
 * @param s String to parse
 * @param board_size Size of this board, required to reverse y axis
 * @param x Reference which will store the x coord result
 * @param y Reference which will store the y coord result
 * @return True if parsing was ok, else if error occured
 */
bool parseStringMove(const wxString &s, unsigned short board_size, short &x, short &y);

/** Convert saved language into locale */
int languageToLocale(int lang);

/** Load OGL configuration. The default settings for OGL are defined here. */
void loadOGLConfig(OGLConfig &config);

/** Save OGL configuration. */
void saveOGLConfig(const OGLConfig &config);

/** Load application settings. Defaults are defined here. */
void loadSettings(Settings &s);

/** Save application settings. */
void saveSettings(const Settings &s);

/** Load IGS settings. Defaults are defined here. */
void loadIGSSettings(IGSSettings &s);

/** Save IGS settings. */
void saveIGSSettings(const IGSSettings &s);

/** Read a color from the config file with the given key. */
wxColour readColorFromConfig(const wxString &key);

/** Play the given sound. See defines.h for possible values. */
void playSound(Sound s);

/* @} */

#endif
