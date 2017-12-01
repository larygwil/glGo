/*
 * glGo.h
 *
 * $Id: glGo.h,v 1.41 2003/11/21 02:26:21 peter Exp $
 */

/**
 * \mainpage %glGo
 *
 * \section intro Introduction
 *
 * %glGo, a prototype for a 3D Goban based on wxWindows, OpenGL and SDL.
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

#ifndef GLGO_H
#define GLGO_H

#ifdef __GNUG__
#pragma interface "glGo.h"
#endif

#include "defines.h"

#ifdef USE_MSHTMLHELP
// Include for wxBestHelpController on Win32
#include <wx/msw/helpbest.h>
#else
// Include for wxHTMLHelpController for non-Win32
#include <wx/html/helpctrl.h>
#endif

// Control defines to enable or disable parts of the application
// NO_GTP and NO_IGS should be set in the Makefile to avoid linking, too
// #define DO_DEBUG_DUMP   /* Trace memory leaks. */
// #define NO_GTP          /* Build without GTP */
// #define NO_IGS          /* Build without IGS client */
// #define NO_LOGFILE      /* Don't redirect log to file */


class StarterFrame;
class MainFrame;
#ifndef NO_IGS
class IGSMainFrame;
#endif
#ifndef NO_GTP
class GTP;
class GTPConfig;
#endif
class LocalServer;

WX_DEFINE_ARRAY(MainFrame*, MainFrames);

/**
 * Main application class.
 * This is the entry class to the whole application. The sort of "main" function of
 * a wxWindows application is OnInit(). The glGo class is responsible for reading
 * the resource files, redirecting the logfile, language support, commandline parsing,
 * loading of the HTML Help data, management of the board windows and starter frame
 * and a lot of other things. In other words, this is the center of the whole program.
 */
class glGo: public wxApp
{
public:
    /** Destructor */
    virtual ~glGo();

    /**
     * Main entry function for the application.
     */
    virtual bool OnInit();

    /**
     * Gets the absolute path to the shared data, including images and resources.
     * @return Absolute path to shared data files.
     */
    wxString GetSharedPath() { return sharedPath; }

    /**
     * Sets the application language. glGo should be restarted after this, else existing
     * GUI elements are not updated.
     * @param lang Language code, if omitted system default is used
     * @return True if the language was set successful, else false
     */
    bool setLanguage(int lang=wxLANGUAGE_DEFAULT);

#ifdef USE_MSHTMLHELP
    /** Gets the MS HTML Help helpcontroller, Win32 version only */
    wxBestHelpController* GetHelpController() const { return helpController; }
#else
    /** Gets the wx HTML Help controller, platform independant */
    wxHtmlHelpController* GetHelpController() const { return helpController; }
#endif

    /**
     * Try to shutdown the application. This checks if there is a server
     * connection or there are modified games, if yes, it prompts the user.
     * If nothing is modified or the user confirms, the program exits after
     * some cleanup.
     * @return True if application shuts down, false if vetoed
     */
    bool AttemptShutdown();

    /** Cleanup on exit */
    int OnExit();

    /** Kill GTP process and thread, if running */
    void KillGTP();

    /** Gets the GTP object. */
    GTP* getGTP() const { return gtp; }

    /** Connect to GTP engine. */
    bool connectGTP(MainFrame *frame
#ifndef NO_GTP
        ,GTPConfig *data
#endif
        );

    /** Open GTP console. */
    void openGTPConsole();

    /**
     * Open a new board.
     * @param game_type GameType of the board, see defines.h
     * @param filename If not empty, this SGF file will be loaded
     * @param is_tmp_filename True if this is loaded from a temp file
     * @return Pointer to the new created frame
     */
    MainFrame* newMainFrame(GameType game_type = GAME_TYPE_PLAY,
                            const wxString &filename = wxEmptyString,
                            bool is_tmp_filename=false);

    /** MainFrames have to notify this class when they close. */
    void notifyMainframeClosed(MainFrame *mf) { mainFrames.Remove(mf); }

    /** Open the IGS terminal frame */
    void openIGS();

#ifndef NO_IGS
    /** The IGSMainFrame has to notify this class when they close. */
    void notifyIGSMainframeClosed() { igs_frame = NULL; }
#endif

    bool IsSoundEnabled() const { return !no_sound; }

    void DisableSound() { no_sound = true; }

    bool loadSoundSystem(int sound_system);

private:
    bool parseCommandline(wxString &sgf_file, wxString &alternate_shared_dir,
                          int &sound_system, bool &sound_disabled);

    MainFrames mainFrames;
    StarterFrame *starterFrame;
    wxString sharedPath;
    wxLocale locale;
    bool no_sound;
    LocalServer *localserver;

#ifdef USE_MSHTMLHELP
    wxBestHelpController *helpController;  ///< Win32 MS HTML Help controller
#else
    wxHtmlHelpController *helpController;  ///< wx HTML Help controller
#endif

#ifndef NO_IGS
    IGSMainFrame *igs_frame;
#endif

#ifndef NO_GTP
    GTP *gtp;
#endif
};

DECLARE_APP(glGo)

#endif
