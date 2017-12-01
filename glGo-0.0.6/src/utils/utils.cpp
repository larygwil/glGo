/*
 * utils.cpp
 *
 * $Id: utils.cpp,v 1.31 2003/11/21 03:31:27 peter Exp $
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

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/log.h>
#include <wx/intl.h>
#include <wx/colour.h>
#endif

#include "utils.h"
#include "sound/sound.h"
#include <wx/config.h>


void parseSGFStringMove(const wxString &s, unsigned short &x, unsigned short &y)
{
    // Convert characters all to lower case
    wxString tmp_s = s;
    tmp_s.MakeLower();

    // Assemble x/y coordinates from the move input (like "kk")
    x = tmp_s.GetChar(0) - 'a' + 1;
    y = tmp_s.GetChar(1) - 'a' + 1;
}

bool parseStringMove(const wxString &s, unsigned short board_size, short &x, short &y)
{
    // Convert characters all to lower case
    wxString tmp_s = s;
    tmp_s.MakeLower();

    char tmpA = 'a';
    char tmpJ = 'j';

    // Assemble x/y coordinates from the move input (like "Q16")
    x = tmp_s.GetChar(0) - tmpA + (tmp_s.GetChar(0) < tmpJ ? 1 : 0);
    long tmp_y;
    if (!tmp_s.Mid(1).ToLong(&tmp_y))
    {
        wxLogDebug(wxString::Format(_T("Failed to parse move %s"), s.c_str()));
        return false;
    }
    y = board_size - static_cast<int>(tmp_y) + 1;

    return true;
}

int languageToLocale(int lang)
{
    switch (lang)
    {
    case 0:
        return wxLANGUAGE_ENGLISH_US;
    case 1:
        return wxLANGUAGE_GERMAN;
    case 2:
        return wxLANGUAGE_DUTCH;
    default:
        return wxLANGUAGE_DEFAULT;
    }
}

void loadOGLConfig(OGLConfig &config)
{
    bool bool_value;
    int int_value;

    wxConfig::Get()->Read(_T("Board/OpenGL/Reflections"), &bool_value, true);
    config.reflections = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/Shadows"), &bool_value, true);
    config.shadows = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/RenderToTexture"), &bool_value, true);
    config.render_to_texture = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/Blur"), &bool_value, false);
    config.blur = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/FastRendering"), &bool_value, false);
    config.fast_rendering = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/Blending"), &bool_value, true);
    config.blending = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/AntialiasLines"), &bool_value, true);
    config.antialias_lines = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/AntialiasStones"), &bool_value, true);
    config.antialias_stones = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/AntialiasScene"), &bool_value, false);
    config.antialias_scene = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/AntialiasSceneQuality"), &int_value, 0);
    config.antialias_scene_quality = int_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/Textures"), &bool_value, true);
    config.textures = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/TexturesQuality"), &int_value, 1);
    config.textures_quality = int_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/Multitextures"), &bool_value, false);
    config.multitextures = bool_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/StoneQuality"), &int_value, true);
    config.stone_quality = int_value;
    wxConfig::Get()->Read(_T("Board/OpenGL/Scissor"), &bool_value, false);
    config.use_scissor = bool_value;
}

void saveOGLConfig(const OGLConfig &config)
{
    wxConfig::Get()->Write(_T("Board/OpenGL/Reflections"), config.reflections);
    wxConfig::Get()->Write(_T("Board/OpenGL/Shadows"), config.shadows);
    wxConfig::Get()->Write(_T("Board/OpenGL/RenderToTexture"), config.render_to_texture);
    wxConfig::Get()->Write(_T("Board/OpenGL/Blur"), config.blur);
    wxConfig::Get()->Write(_T("Board/OpenGL/FastRendering"), config.fast_rendering);
    wxConfig::Get()->Write(_T("Board/OpenGL/Blending"), config.blending);
    wxConfig::Get()->Write(_T("Board/OpenGL/AntialiasLines"), config.antialias_lines);
    wxConfig::Get()->Write(_T("Board/OpenGL/AntialiasStones"), config.antialias_stones);
    wxConfig::Get()->Write(_T("Board/OpenGL/AntialiasScene"), config.antialias_scene);
    wxConfig::Get()->Write(_T("Board/OpenGL/AntialiasSceneQuality"), config.antialias_scene_quality);
    wxConfig::Get()->Write(_T("Board/OpenGL/Textures"), config.textures);
    wxConfig::Get()->Write(_T("Board/OpenGL/TexturesQuality"), config.textures_quality);
    wxConfig::Get()->Write(_T("Board/OpenGL/Multitextures"), config.multitextures);
    wxConfig::Get()->Write(_T("Board/OpenGL/StoneQuality"), config.stone_quality);
    wxConfig::Get()->Write(_T("Board/OpenGL/Scissor"), config.use_scissor);

    wxConfig::Get()->Flush();
}

void loadSettings(Settings &s)
{
    int int_value;
    bool bool_value;

    wxConfig::Get()->Read(_T("Misc/Language"), &int_value, 0);
    s.language = int_value;
    wxConfig::Get()->Read(_T("Misc/Autohide"), &bool_value, false);
    s.autohide = bool_value;
#ifdef __WXMSW__
    wxConfig::Get()->Read(_T("Misc/MinTray"), &bool_value, false);
    s.minimize_to_tray = bool_value;
#endif
    wxConfig::Get()->Read(_T("Misc/Tooltips"), &bool_value, true);
    s.tooltips = bool_value;
    wxConfig::Get()->Read(_T("Misc/GlobalSound"), &bool_value, true);
    s.global_sound = bool_value;
    wxConfig::Get()->Read(_T("Misc/SoundSystem"), &int_value, 0);
    s.sound_system = int_value;
    wxConfig::Get()->Read(_T("Board/Type"), &int_value, 0);
    s.board_display_type = int_value;
    wxConfig::Get()->Read(_T("Misc/Localserver"), &bool_value, true);
    s.localserver = bool_value;
}

void saveSettings(const Settings &s)
{
    wxConfig::Get()->Write(_T("Misc/Language"), s.language);
    wxConfig::Get()->Write(_T("Misc/Autohide"), s.autohide);
#ifdef __WXMSW__
    wxConfig::Get()->Write(_T("Misc/MinTray"), s.minimize_to_tray);
#endif
    wxConfig::Get()->Write(_T("Misc/Tooltips"), s.tooltips);
    wxConfig::Get()->Write(_T("Misc/GlobalSound"), s.global_sound);
    wxConfig::Get()->Write(_T("Misc/SoundSystem"), s.sound_system);
    wxConfig::Get()->Write(_T("Board/Type"), s.board_display_type);
    wxConfig::Get()->Write(_T("Misc/Localserver"), s.localserver);

    wxConfig::Get()->Flush();
}

void loadIGSSettings(IGSSettings &s)
{
    wxString str_value;
    bool bool_value;
    int int_value;

    wxConfig::Get()->Read(_T("IGS/Loginname"), &str_value, _T("guest"));
    s.loginname = str_value;
    wxConfig::Get()->Read(_T("IGS/Password"), &str_value, "");
    s.password = str_value;
    wxConfig::Get()->Read(_T("IGS/Autoconnect"), &bool_value, false);
    s.autoconnect = bool_value;
    wxConfig::Get()->Read(_T("IGS/ShoutsInTerminal"), &bool_value, true);
    s.shouts_in_terminal = bool_value;
    wxConfig::Get()->Read(_T("IGS/SkipGuests"), &bool_value, true);
    s.skip_guests = bool_value;
    wxConfig::Get()->Read(_T("IGS/ShowObsMsgBox"), &bool_value, true);
    s.show_obs_msgbox = bool_value;
    wxConfig::Get()->Read(_T("IGS/AytTimer"), &bool_value, true);
    s.ayt_timer = bool_value;
    wxConfig::Get()->Read(_T("IGS/ChatSound"), &bool_value, true);
    s.chat_sound = bool_value;
    wxConfig::Get()->Read(_T("IGS/MatchSound"), &bool_value, true);
    s.match_sound = bool_value;
    wxConfig::Get()->Read(_T("IGS/TimeSound"), &bool_value, true);
    s.timewarn_sound = bool_value;
    wxConfig::Get()->Read(_T("IGS/TimeThreshold"), &int_value, 30);
    s.timewarn_threshold = int_value;
    wxConfig::Get()->Read(_T("IGS/ShowAllFriends"), &bool_value, false);
    s.all_friends = bool_value;
    wxConfig::Get()->Read(_T("IGS/AutosaveOwn"), &bool_value, false);
    s.autosave_own = bool_value;
    wxConfig::Get()->Read(_T("IGS/AutosaveObserved"), &bool_value, false);
    s.autosave_observed = bool_value;
}

void saveIGSSettings(const IGSSettings &s)
{
    wxConfig::Get()->Write(_T("IGS/Loginname"), s.loginname);
    wxConfig::Get()->Write(_T("IGS/Password"), s.password);
    wxConfig::Get()->Write(_T("IGS/Autoconnect"), s.autoconnect);
    wxConfig::Get()->Write(_T("IGS/ShoutsInTerminal"), s.shouts_in_terminal);
    wxConfig::Get()->Write(_T("IGS/SkipGuests"), s.skip_guests);
    wxConfig::Get()->Write(_T("IGS/ShowObsMsgBox"), s.show_obs_msgbox);
    wxConfig::Get()->Write(_T("IGS/AytTimer"), s.ayt_timer);
    wxConfig::Get()->Write(_T("IGS/ChatSound"), s.chat_sound);
    wxConfig::Get()->Write(_T("IGS/MatchSound"), s.match_sound);
    wxConfig::Get()->Write(_T("IGS/TimeSound"), s.timewarn_sound);
    wxConfig::Get()->Write(_T("IGS/TimeThreshold"), s.timewarn_threshold);
    wxConfig::Get()->Write(_T("IGS/ShowAllFriends"), s.all_friends);
    wxConfig::Get()->Write(_T("IGS/AutosaveOwn"), s.autosave_own);
    wxConfig::Get()->Write(_T("IGS/AutosaveObserved"), s.autosave_observed);

    wxConfig::Get()->Flush();
}

wxColour readColorFromConfig(const wxString &key)
{
    wxString s;

    // Try to read background color from config
    if (wxConfig::Get()->Read(key, &s))
    {
        // Create color object from string like "123#45#67"
        s += "#";  // Cheat for easy loop :=)
        size_t pos, old_pos=0, n=0;
        int c[3];
        while((pos = s.find("#", old_pos)) != wxString::npos)
        {
            c[n++] = wxAtoi(s.Mid(old_pos, pos));
            old_pos = pos+1;

            if (n>3)
            {
                // We read crap?
                LOG_GLOBAL(wxString::Format(_T("Read invalid background color: %s"), s.c_str()));
                return wxColour(128, 0, 0);
            }
        }
        return wxColor(c[0], c[1], c[2]);
    }

    // Default background color if we read nothing
    return wxColour(128, 0, 0);
}

void playSound(Sound s)
{
    // Sound globally disabled?
    bool global_sound;
    wxConfig::Get()->Read(_T("Misc/GlobalSound"), &global_sound, true);
    if (!global_sound)
        return;

    Sound_play(s);
}
