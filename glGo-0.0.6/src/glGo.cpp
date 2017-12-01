/*
 * glGo.cpp
 *
 * $Id: glGo.cpp,v 1.85 2003/11/21 15:08:31 peter Exp $
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
#pragma implementation "glGo.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#include <wx/image.h>
#include <wx/xrc/xmlres.h>
#include <wx/fs_zip.h>
#include <wx/filename.h>
#include <wx/cshelp.h>
#include <wx/config.h>
#include <wx/cmdline.h>
#include <wx/tooltip.h>
#ifndef __WXMSW__
#include <stdio_ext.h>
#else
#include <wx/fileconf.h>
#endif
#include "glGo.h"
#include "starterframe.h"
#include "preferences_dialog.h"
#include "mainframe.h"
#ifndef NO_IGS
#include "igs_mainframe.h"
#endif
#include "utils/utils.h"
#include "logger.h"
#include "sound.h"
#ifndef NO_GTP
#include "gtp/gtp.h"
#endif
#include "sdlboard.h"
#include "localserver.h"


// This is our main application class
IMPLEMENT_APP(glGo)

#ifndef __WXMSW__
// Global function declarations
wxString wxFindAppPath(const wxString& argv0);
wxString findSharedPath(const wxString &appDir);
#endif


// ------------------------------------------------------------------------------
//                                 Class glGo
// ------------------------------------------------------------------------------

glGo::~glGo()
{
    wxLogDebug(_T("glGo::~glGo()"));

#ifdef DO_DEBUG_DUMP
    wxDebugContext::Dump();
    wxDebugContext::PrintStatistics();
#endif

    // Release logchain if not yet done
    wxLog::SetActiveTarget(NULL);
}

// This is our application main entry function
bool glGo::OnInit()
{
    SetAppName(PACKAGE);
    SetVendorName(VENDOR);

    srand(time(0));

#ifndef NO_IGS
    igs_frame = NULL;
#endif

#ifndef NO_GTP
    gtp = NULL;
#endif

#ifdef DO_DEBUG_DUMP
    wxDebugContext::SetCheckpoint();
#endif


    /*
     * Parse commandline.
     */

    wxString sgf_file, alternate_shared_dir;
    int sound_system_cmd = -1;
    bool sound_disabled_cmd = false;
    if (!parseCommandline(sgf_file, alternate_shared_dir, sound_system_cmd, sound_disabled_cmd))
        return false;


    /*
     * Init config and relocate config base.
     */

    // Setup config path
#ifdef __WXMSW__
    // $HOME\glGo\ on Windows
    // %APPDATA$ is a bad idea because this folder is hidden by default and dumb users
    // won't find the logfile when I ask them for it.
    // Windows 9x does not know about $HOME, it will save into C:\Windows. I don't care.
    wxString configPath = wxGetHomeDir() + wxFileName::GetPathSeparator() + PACKAGE;
#else
    // $HOME/.glGo/ on Linux
    wxString configPath = wxGetHomeDir() + wxFileName::GetPathSeparator() + _T(".") + PACKAGE;
#endif
    // Make sure the directory exists, if not create it
    if (!wxDirExists(configPath))
    {
        wxLogDebug(_T("Creating config path: %s"), configPath.c_str());
        if (!wxMkdir(configPath, 0750))  // rwxr-x--- (ignored on non-Unix systems)
        {
            wxSafeShowMessage(_T("Error"),
                              wxString::Format(_T("Failed to create user configuration directory: %s"),
                                               configPath.c_str()));
            configPath = wxEmptyString;
        }
    }
    wxLogDebug(_T("Config path: %s"), configPath.c_str());

#ifdef __WXMSW__
    // Here you can select if you want to save settings in the registry or a file.
    // I came to the conclusion registry is a bad idea and use the fileconfig instead.
#if 0
    // Windows registry under HKCU\Software\glGo\Settings
    wxConfig *config = new wxConfig(wxEmptyString, wxEmptyString,
                                    wxString(PACKAGE) + _T("\\Settings"),
                                    wxEmptyString, wxCONFIG_USE_LOCAL_FILE);
#else
    // Windows file config in $HOME/glGo/glGo.rc
    // Explicit usage of wxFileConfig is required as wxConfig is typedef'ed to wxRegConfig on Win32
    wxFileConfig *config = new wxFileConfig(wxEmptyString, wxEmptyString,
                                            configPath + wxFileName::GetPathSeparator() + PACKAGE + _T(".rc"),
                                            wxEmptyString, wxCONFIG_USE_LOCAL_FILE);
#endif
#else
    // Linux config file in $HOME/.glGo/glGo.rc
    wxConfig *config = new wxConfig(wxEmptyString, wxEmptyString,
                                    configPath + wxFileName::GetPathSeparator() + PACKAGE + _T(".rc"),
                                    wxEmptyString, wxCONFIG_USE_LOCAL_FILE);
#endif

    wxConfigBase *oldconfig = wxConfig::Set(config);
    if (oldconfig != NULL)
        delete oldconfig;
    // New config object will be deleted in wxApp::OnExit()


    /*
     * Redirect logging to file.
     */
#ifndef NO_LOGFILE
    // Try creating logfile in append mode in $(configPath)/glGo.log
    FILE *log_file = fopen(wxString(configPath +
                                    wxFileName::GetPathSeparator() +
                                    PACKAGE + _T(".log")).c_str(), "a");
    if (log_file == NULL
#ifndef __WXMSW__
        || !__fwritable (log_file)
#endif
        )
    {
        wxLogDebug(_T("Failed to create logfile."));
        fclose(log_file);
        delete log_file;
        log_file = NULL;
    }
    else
        new wxLogChain(new Logger(log_file));
#endif
    // Now we can use the LOG_XXX macros

    wxString t_stamp;
    wxLog::TimeStamp(&t_stamp);
    LOG_GLOBAL(wxString::Format(_T("*** Starting new glGo %s session at %s ***"), VERSION, t_stamp.c_str()));


    /*
     * Check for shared directory
     */

    // Check if -s commandline option was given
    if (!alternate_shared_dir.empty())
        sharedPath = alternate_shared_dir;
    // Check if environment variable "GLGO_SHARED_PATH" is given
    else if (wxGetEnv(_T("GLGO_SHARED_PATH"), &alternate_shared_dir))
        sharedPath = alternate_shared_dir;
    // Nothing given, use defaults.
    else
    {
        // Get shared path for images and resources. Terminate if not found.
#ifdef __WXMSW__
        // Maybe read installpath from registry first.
        sharedPath = _T("share/");
#else
        // On Linux, find shared data path
        sharedPath = findSharedPath(wxFindAppPath(argv[0]));
#endif
    }
    LOG_GLOBAL(wxString::Format(_T("Shared path: %s"), sharedPath.c_str()));

    // Rough check if this shared directory makes some sense.
    if (!wxDirExists(sharedPath))
    {
        wxLogFatalError(_T("Shared data directory not found. Aborting. Try the -s commandline option."));
        return false;
    }
    if (!wxFileExists(sharedPath + _T("data.dat")))
    {
        wxLogFatalError(_T("Data file not found. Aborting. Try the -s commandline option."));
        return false;
    }


    /*
     * Init languages
     */

    // Set language to system default or to the saved setting
    int l;
    wxConfig::Get()->Read(_T("Misc/Language"), &l, wxLANGUAGE_DEFAULT);
    setLanguage(languageToLocale(l));


    /*
     * Init resources
     */

    // Install JPEG handler
    wxImage::AddHandler(new wxJPEGHandler);
#ifndef __WXMSW__
    // ... and XPM handler (Linux only)
    wxImage::AddHandler(new wxXPMHandler);
#endif
    // Initialize XML resource handlers
    wxFileSystem::AddHandler(new wxZipFSHandler);
    wxXmlResource::Get()->InitAllHandlers();

    // Load XML resource files
#ifdef __WXDEBUG__
    // For debug version, load each file individually from local directory
    // Unfortunately wxXMLResource::Load() returns true even if it fails to load the file...
    if (!wxFileExists(_T("rc/starter_frame.xrc")))
    {
        wxLogFatalError(_T("Failed to load resource files. Aborting. Try the -s commandline option."));
        return false;
    }
    wxXmlResource::Get()->Load(_T("rc/starter_frame.xrc"));
    wxXmlResource::Get()->Load(_T("rc/mainframe_menu.xrc"));
    wxXmlResource::Get()->Load(_T("rc/sidebar.xrc"));
    wxXmlResource::Get()->Load(_T("rc/newgame_dialog.xrc"));
    wxXmlResource::Get()->Load(_T("rc/gameinfo_dialog.xrc"));
    wxXmlResource::Get()->Load(_T("rc/gtp_setup_dialog.xrc"));
    wxXmlResource::Get()->Load(_T("rc/display_options_dialogs.xrc"));
    wxXmlResource::Get()->Load(_T("rc/about_dialog.xrc"));
    wxXmlResource::Get()->Load(_T("rc/preferences_dialog.xrc"));
    wxXmlResource::Get()->Load(_T("rc/igs_mainframe.xrc"));
    wxXmlResource::Get()->Load(_T("rc/igs_dialogs.xrc"));
    wxXmlResource::Get()->Load(_T("rc/igs_frames.xrc"));
    wxXmlResource::Get()->Load(_T("rc/playerdb.xrc"));
#else
    // For release, load the zipped resource file from shared data path
    if (!wxFileExists(sharedPath + _T("resource.xrs")) ||
        !wxXmlResource::Get()->Load(sharedPath + _T("resource.xrs")))
    {
        wxLogFatalError(_T("Failed to load file resource.xrs. Aborting. Try the -s commandline option."));
        return false;
    }
#endif


    /*
     * Initialize HTML Help system
     */

    // Create the help controller
#ifdef USE_MSHTMLHELP
    // Win32 MS HTML Help
    helpController = new wxBestHelpController();
    helpController->Initialize(wxString::Format(_T("docs/%s"), PACKAGE));
#else
    // wx HTML Help
    helpController = new wxHtmlHelpController();
    if (!configPath.empty())
        helpController->SetTempDir(configPath);
    helpController->AddBook(wxFileName(wxString::Format(_T("%s/%s.htb"), sharedPath.c_str(), PACKAGE)));
#endif

    // Create help provider
    wxHelpControllerHelpProvider *provider = new wxHelpControllerHelpProvider;
    wxHelpProvider::Set(provider);
    provider->SetHelpController(helpController);


    /*
     * Init sound system
     */
    if (!sound_disabled_cmd)
    {
        int sound_system;
        // Commandline option overwrites configuration
        if (sound_system_cmd == -1)
            wxConfig::Get()->Read(_T("Misc/SoundSystem"), &sound_system, SOUND_SYSTEM_OAL);
        else
        {
            sound_system = sound_system_cmd;
            wxConfig::Get()->Write(_T("Misc/SoundSystem"), sound_system_cmd);
        }

        // Try loading the shared library
        if (!loadSoundSystem(sound_system))
            wxSafeShowMessage(_("Error"),
                              wxString::Format(_("Failed to load %s sound system"),
                                               (sound_system == SOUND_SYSTEM_OAL ? _("OpenAL") :
                                                sound_system == SOUND_SYSTEM_SDL ? _("SDL Mixer") : "INVALID")));
    }
    else
    {
        LOG_SOUND(_T("Sound was disabled at commandline."));
        wxConfig::Get()->Write(_T("Misc/GlobalSound"), false);
        no_sound = true;
    }


    /*
     * Start localserver
     */

    bool use_localserver;
    wxConfig::Get()->Read(_T("Misc/Localserver"), &use_localserver, true);
    if (use_localserver)
        localserver = new LocalServer();
    else
        localserver = NULL;


    /*
     * Create the starter window
     */

    starterFrame = new StarterFrame();
    starterFrame->Show(true);
    SetTopWindow(starterFrame);


    /*
     * Open Board if sgf file parameter was given
     */

    if (!sgf_file.empty())
        newMainFrame(GAME_TYPE_PLAY, sgf_file);


    /*
     * Enable or disable tooltips
     */

    bool tooltips;
    wxConfig::Get()->Read(_T("Misc/Tooltips"), &tooltips, true);
    wxToolTip::Enable(tooltips);

    LOG_GLOBAL(_T("glGo::OnInit() exiting with success"));
    return true;
}

bool glGo::loadSoundSystem(int sound_system)
{
    LOG_SOUND(wxString::Format(_T("Trying to load %s sound system."),
                               (sound_system == SOUND_SYSTEM_OAL ? _("OpenAL") :
                                sound_system == SOUND_SYSTEM_SDL ? _("SDL Mixer") : _T("INVALID"))));
    if (Sound_load_library(sound_system) == 0)
    {
        LOG_SOUND(_T("Sound library loaded."));
        if (Sound_init_library(sharedPath.c_str()) == 0)
        {
            LOG_SOUND(_T("Sound system initialized.."));
        }
        else
        {
            LOG_SOUND(_T("Failed to initialize sound system."));
            wxConfig::Get()->Write(_T("Misc/GlobalSound"), false);
            no_sound = true;
            return false;
        }
        no_sound = false;
        return true;
    }

    LOG_SOUND(_T("Failed to load sound library."));
    wxConfig::Get()->Write(_T("Misc/GlobalSound"), false);
    no_sound = true;
    return false;
}

MainFrame* glGo::newMainFrame(GameType game_type, const wxString &filename, bool is_tmp_filename)
{
#ifdef NO_GTP
    if (game_type == GAME_TYPE_GTP)
    {
        wxMessageBox(NO_GTP_ERROR_MESSAGE, _("Error"), wxOK | wxICON_ERROR);
        return NULL;
    }
#endif


#ifdef __WXMSW__
    bool max;
    // Check maximized flag from config. Windows only
    wxConfig::Get()->Read(_T("Board/Maximized"), &max, false);
    wxLogDebug("Maximized: %d", max);
#endif

    wxSize size = wxDefaultSize;
    wxPoint pos = wxDefaultPosition;
#ifdef __WXMSW__
    if (!max)
#endif
    {
        // Read size from config file
        int x, y;
        if (wxConfig::Get()->Read(_T("Board/SizeX"), &x) &&
            wxConfig::Get()->Read(_T("Board/SizeY"), &y))
            size = wxSize(x, y);
        else
            size = wxSize(800, 620);

        // Cascade subsequent frames by 10 pixels per frame
        pos.x += mainFrames.GetCount() * 10;
        pos.y += mainFrames.GetCount() * 10;
    }

    // Use OpenGL or SDLBoard ?
    int type;  // We need an int to read from config, enum won't work
    wxConfig::Get()->Read(_T("Board/Type"), &type, 0);
    // Dreaded user might have messed in the config
    if (type < DISPLAY_TYPE_SDL || type > DISPLAY_TYPE_OPENGL)
        type = DISPLAY_TYPE_SDL;
    LOG_GLOBAL(wxString::Format(_T("Using board type %d"), type));

    // Create a new board window
    MainFrame *frame = new MainFrame(pos, size, game_type, type == 0 ? DISPLAY_TYPE_SDL : DISPLAY_TYPE_OPENGL);
    if (game_type == GAME_TYPE_GTP)
    {
        if (!frame->newGTPGame())
        {
            frame->Destroy();
            return NULL;
        }
    }

#ifdef __WXMSW__
    if (max)
    {
        frame->Maximize(true);
        wxLogDebug("Maximize!");
    }
#endif

    frame->Show(true);
    mainFrames.Add(frame);

    // Load SGF game if any given
    if (!filename.empty())
        frame->doLoad(filename, is_tmp_filename);
    return frame;
}

void glGo::openIGS()
{
#ifndef NO_IGS
    if (igs_frame != NULL && IGSMainFrame::is_open)
        igs_frame->Raise();
    else
    {
        // Read size and position from config file
        int x, y;
        wxSize size;
        wxPoint pos;
        if (wxConfig::Get()->Read(_T("IGS/Frames/TerminalSizeX"), &x) &&
            wxConfig::Get()->Read(_T("IGS/Frames/TerminalSizeY"), &y))
            size = wxSize(x, y);
        else
            size = wxSize(620, 500);
        if (wxConfig::Get()->Read(_T("IGS/Frames/TerminalPosX"), &x) &&
            wxConfig::Get()->Read(_T("IGS/Frames/TerminalPosY"), &y))
            pos = wxPoint(x, y);
        else
            pos = wxDefaultPosition;

        igs_frame = new IGSMainFrame(pos, size);
        igs_frame->Show(true);

        // Check for autoconnect
        bool autoconnect;
        wxConfig::Get()->Read(_T("IGS/Autoconnect"), &autoconnect, false);
        if (autoconnect)
            igs_frame->connect();
    }
#else
    wxMessageBox(_("This build does not support the IGS client."),
                 _("Error"), wxOK | wxICON_ERROR);
#endif
}

bool glGo::connectGTP(MainFrame *frame
#ifndef NO_GTP
    ,GTPConfig *data
#endif
    )
{
#ifndef NO_GTP
    wxASSERT(data != NULL);
    if (gtp == NULL)
        gtp = new GTP;
    if (frame->getCurrentBoardWindow() != NULL)
        return gtp->Connect(data, frame, frame->getBoard(), frame->getCurrentBoardWindow());
    else
        return false;
#else
    wxMessageBox(NO_GTP_ERROR_MESSAGE, _("Error"), wxOK | wxICON_ERROR);
    return false;
#endif
}

void glGo::openGTPConsole()
{
#ifndef NO_GTP
    if (gtp == NULL)
        gtp = new GTP;
    gtp->OpenConsole();
#else
    wxMessageBox(NO_GTP_ERROR_MESSAGE, _("Error"), wxOK | wxICON_ERROR);
#endif
}

bool glGo::AttemptShutdown()
{
    // Skip this in debug mode, too annoying
#ifndef __WXDEBUG__
    bool skip = false;

#ifndef NO_IGS
    // Connected to IGS?
    if (igs_frame != NULL && igs_frame->isConnected())
    {
        if (wxMessageBox(_("You are currently connected to a server.\n"
                         "Do you really want to close your connection?"),
                         _("Question"), wxYES | wxNO | wxICON_EXCLAMATION) == wxNO)
            return false;
        else
            skip = true;
    }
#endif

    // Don't ask again if we confirmed already above
    if (!skip)
    {
        // Is there a modified board?
        bool flag = false;
        for (int i=0, sz=mainFrames.GetCount(); i<sz; i++)
        {
            if (mainFrames.Item(i)->isModified())
            {
                flag = true;
                break;
            }
        }
        if (flag && wxMessageBox(_("At least one board is modified.\n"
                                   "If you exit the application now, all changes will be lost!\n"
                                   "Exit anyways?"), _("Question"), wxYES | wxNO | wxICON_EXCLAMATION) == wxNO)
            return false;
    }
#endif
    // Ok, either nothing is modified or the user really wants to shutdown. Do it.

    // Close GTP
    KillGTP();
#ifndef NO_IGS
    // Close IGS mainframe
    if (igs_frame != NULL)
    {
        igs_frame->Destroy();
        igs_frame = NULL;
    }
#endif
    // Close boards
    for (int i=0, sz=mainFrames.GetCount(); i<sz; i++)
        mainFrames.Item(i)->Destroy();
    starterFrame->Destroy();
    return true;
}

int glGo::OnExit()
{
    wxLogDebug("glGo::OnExit()");
    LOG_GLOBAL(_T("About to exit application..."));

    // Quit GTP if connection is open
    KillGTP();

    // Clear mainframes array
    mainFrames.Clear();

#ifndef NO_IGS
    // Close IGS mainframe
    if (igs_frame != NULL)
    {
        igs_frame->Destroy();
        igs_frame = NULL;
    }
#endif

    // Shutdown sound system
    LOG_SOUND(_T("Shutting down sound"));
    if (!no_sound)
        Sound_quit_library();

    // This will call SDL_Quit
    LOG_SDL(_T("Shutting down SDL"));
    SDLBoard::Cleanup();

    // Flush config
    wxConfig::Get()->Flush();

#ifndef __WXDEBUG__
    // Shutdown help system
    if (helpController != NULL)
    {
        // helpController->Quit();
        delete helpController;
    }
#endif

    // Kill localserver
    if (localserver != NULL)
        delete localserver;

    LOG_GLOBAL(_T("*** glGo session ending ***"));
    return 0;
}

void glGo::KillGTP()
{
#ifndef NO_GTP
    wxLogDebug(_T("glGo::KillGTP"));

    if (gtp != NULL)
    {
        delete gtp;
        gtp = NULL;
    }
#endif
}

bool glGo::setLanguage(int lang)
{
    wxLogDebug(_T("setLanguage: %d"), lang);

    // Init locale
#ifndef __BORLANDC__
    locale.Init(lang);
#else
    // This is a workaround for Borland C++ compiler which for whatever reason does not get
    // the wxLocale::Init(int) function right.

    // If default or unknown locale is given, try to find the system locale.
    if (lang == wxLANGUAGE_DEFAULT ||
        lang == wxLANGUAGE_UNKNOWN)
    {
        // System default detection does not work fully with Borland. We only get an integer
        // value, not the short name. So we have to translate that manually and look for
        // for the translated languages ourselves.
        switch (wxLocale::GetSystemLanguage())
        {
        case wxLANGUAGE_GERMAN:
        case wxLANGUAGE_GERMAN_AUSTRIAN:
        case wxLANGUAGE_GERMAN_LIECHTENSTEIN:
        case wxLANGUAGE_GERMAN_LUXEMBOURG:
        case wxLANGUAGE_GERMAN_SWISS:
            lang = wxLANGUAGE_GERMAN;
            break;
        default:
            lang = wxLANGUAGE_ENGLISH;
        }
    }

    switch (lang)
    {
    case wxLANGUAGE_GERMAN:
    case wxLANGUAGE_GERMAN_AUSTRIAN:
    case wxLANGUAGE_GERMAN_LIECHTENSTEIN:
    case wxLANGUAGE_GERMAN_LUXEMBOURG:
    case wxLANGUAGE_GERMAN_SWISS:
        locale.Init(_T("German"), _T("de_DE"));
        wxLogDebug(_T("Borland fix: INIT GERMAN"));
        break;
    case wxLANGUAGE_ENGLISH:
    case wxLANGUAGE_ENGLISH_US:
    case wxLANGUAGE_ENGLISH_UK:
    default:
        locale.Init(_T("English"), _T("en_US"));
        wxLogDebug(_T("Borland fix: INIT ENGLISH"));
        lang = wxLANGUAGE_ENGLISH;
    }
#endif  // !__BORLANDC__

    wxLogDebug(_T("Init locale Ok: %d"), locale.IsOk());
    if (locale.IsOk())
    {
        // Get canonical name, something like "en", "en_GB", "en_US"
        wxString canonical = locale.GetCanonicalName();

        LOG_GLOBAL(wxString::Format(_T("Initialized language %s (%d) - locale %s"),
                                    canonical.c_str(), lang, locale.GetLocale()));

        // Don't load a catalog for english
        if (!canonical.Cmp(_T("en")) ||
            !canonical.Cmp(_T("en_GB")) ||
            !canonical.Cmp(_T("en_US")))
            return true;

        // Search catalogs in shared path
        locale.AddCatalogLookupPathPrefix(sharedPath);

        // Load catalog
        if (locale.AddCatalog(_T("messages")))
        {
            wxLogDebug(_T("Loaded catalog"));
            return true;
        }

        // Loading failed
        LOG_GLOBAL(wxString::Format(_T("Failed to load catalog for %s from %s."),
                                    locale.GetLocale(),
                                    wxString(sharedPath + locale.GetCanonicalName()).c_str()));
        return false;
    }

    // Init failed
    LOG_GLOBAL(wxString::Format(_T("Failed to init locale. Current locale is %s."), locale.GetLocale()));
    return false;
}

bool glGo::parseCommandline(wxString &sgf_file, wxString &alternate_shared_dir,
                            int &sound_system, bool &sound_disabled)
{
    static const wxCmdLineEntryDesc cmdLineDesc[] =
        {
            { wxCMD_LINE_SWITCH, _T("h"), _T("help"), _T("Show this help message"),
              wxCMD_LINE_VAL_NONE, wxCMD_LINE_OPTION_HELP },
            { wxCMD_LINE_OPTION, _T("s"), _T("shared"), _T("Give shared data directory"),
              wxCMD_LINE_VAL_STRING, wxCMD_LINE_PARAM_OPTIONAL },
            { wxCMD_LINE_SWITCH, _T("n"), _T("nosound"), _T("Disable sound"),
              wxCMD_LINE_VAL_NONE, wxCMD_LINE_PARAM_OPTIONAL },
            { wxCMD_LINE_SWITCH, NULL, _T("openal"), _T("Select OpenAL sound system and enable sound"),
              wxCMD_LINE_VAL_NONE, wxCMD_LINE_PARAM_OPTIONAL },
            { wxCMD_LINE_SWITCH, NULL, _T("sdl"), _T("Select SDL mixer sound system and enable sound"),
              wxCMD_LINE_VAL_NONE, wxCMD_LINE_PARAM_OPTIONAL },
            { wxCMD_LINE_PARAM,  NULL, NULL, _T("SGF file"),
              wxCMD_LINE_VAL_STRING, wxCMD_LINE_PARAM_OPTIONAL },
            { wxCMD_LINE_SWITCH, _T("v"), _T("version"), _T("Display version and exit"),
              wxCMD_LINE_VAL_NONE, wxCMD_LINE_PARAM_OPTIONAL },
            { wxCMD_LINE_NONE }
        };

    wxCmdLineParser parser(cmdLineDesc, argc, argv);
    if (parser.Parse())
        return false;

    if (parser.Found("v"))
    {
        wxPrintf(_T("glGo %s\n"
                    "Written by Peter Strempel\n\n"
                    "Copyright (c) 2003, Peter Strempel\n"
                    "This program is free software; you can redistribute it and/or modify\n"
                    "it under the terms of the GNU General Public License as published by\n"
                    "the Free Software Foundation; either version 2 of the License, or\n"
                    "(at your option) any later version.\n"), VERSION);
        return false;
    }

    if (parser.Found("s", &alternate_shared_dir))
        wxPrintf(_T("Using shared data directory %s\n"), alternate_shared_dir.c_str());
    else
        alternate_shared_dir = wxEmptyString;

    if (parser.Found("n"))
    {
        wxPrintf(_T("Disabling sound.\n"));
        sound_disabled = true;
    }
    else
        sound_disabled = false;
    if (parser.Found(_T("openal")))
    {
        wxPrintf(_T("Using OpenAL sound system.\n"));
        sound_system = SOUND_SYSTEM_OAL;
    }
    else if (parser.Found(_T("sdl")))
    {
        wxPrintf(_T("Using SDL sound system.\n"));
        sound_system = SOUND_SYSTEM_SDL;
    }
    else
        sound_system = -1;

    if (parser.GetParamCount())
        sgf_file = parser.GetParam();
    else
        sgf_file = wxEmptyString;
    return true;
}


// ------------------------------------------------------------------------------
//                              Global functions
// ------------------------------------------------------------------------------

// This is not required on Windows
#ifndef __WXMSW__
/**
 * Helper method to find the directory of the executed application binary.
 * Taken from wxWindows documentation.
 * @param argv0 wxApp->argv[0]
 * @return Application path
 */
wxString wxFindAppPath(const wxString& argv0)
{
    wxString str;
    wxString cwd = wxGetCwd();

    if (wxIsAbsolutePath(argv0))
        return wxPathOnly(argv0);
    else
    {
        // Is it a relative path?
        wxString currentDir(cwd);
        if (currentDir.Last() != wxFILE_SEP_PATH)
            currentDir += wxFILE_SEP_PATH;
        str = currentDir + argv0;
        if (wxFileExists(str))
            return wxPathOnly(str);
    }
    // OK, it's neither an absolute path nor a relative path.
    // Search PATH.
    wxPathList pathList;
    pathList.AddEnvList(_T("PATH"));
    str = pathList.FindAbsoluteValidPath(argv0);
    if (!str.IsEmpty())
        return wxPathOnly(str);
    // Failed
    return wxEmptyString;
}

/**
 * Helper function to find the shared path where the images and resources are installed.
 * First a directory "share" in the appPath is checked. If this does not exist,
 * some predefines locations are checked. If nothing is found just "share" is returned.
 *
 * @param appDir application directory (for example /usr/local/bin)
 * @return Shared path (for example /usr/local/share/glGo or appDir/share)
 */
wxString findSharedPath(const wxString &appDir)
{
    // This should not happen, but safe is safe
    if (wxIsEmpty(appDir))
    {
        wxLogDebug(_T("Appdir was empty."));
        return _T("share/");
    }

    // Assemble list of likely Linux installation paths
    wxArrayString possiblePaths;
    possiblePaths.Add(appDir + wxFileName::GetPathSeparator() + _T("share"));
    possiblePaths.Add(_T("/opt/glGo/share"));
    possiblePaths.Add(_T("/usr/local/share/glGo"));
    possiblePaths.Add(_T("/usr/share/glGo"));
    possiblePaths.Add(_T("/opt/share"));
    possiblePaths.Add(wxGetHomeDir() + "/" + PACKAGE + _T("/share"));
    possiblePaths.Add(wxGetHomeDir() + _T("/share"));

    int n = possiblePaths.Count();

    // Loop through this list and check if the directory exists
    for (int i=0; i<n; i++)
    {
        if (wxDirExists(possiblePaths[i]))
            return possiblePaths[i] + wxFileName::GetPathSeparator();
    }

    return _T("share/");
}
#endif  // __WXMSW__
