/*
 *  gGo.java
 *
 *  gGo
 *  Copyright (C) 2002  Peter Strempel <pstrempel@t-online.de>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package ggo;

import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.plaf.metal.*;
import java.util.*;
import ggo.*;
import ggo.utils.*;
import ggo.utils.sound.SoundHandler;
import ggo.gui.*;
import ggo.gtp.*;
import ggo.igs.gui.IGSMainWindow;
import javax.help.*;

/**
 *  Main application class
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.12 $, $Date: 2002/10/24 08:31:45 $
 */
public class gGo implements Defines {
    //{{{ private members
    private static Settings settings;
    private static StartUpFrame startUpFrame = null;
    // private static HelpViewer helpViewer;
    private static IGSMainWindow igsMainWindow = null;
    private static GTP gtp = null;
    private static Vector frames = null;
    private static Server server = null;
    private static boolean isApplet = false, is13 = false, noServer = false;
    private static HelpBroker helpBroker = null;
    private static Locale locale;
    private static ResourceBundle ggo_resources, board_resources, gtp_resources, sgf_resources,
            igs_resources, igs_player_resources, igs_match_resources, igs_bozo_resources,
            igs_message_resources, clipboard_dialog_resources;
    //}}}

    //{{{ static constructor
    static {
        // Load settings
        settings = new Settings();

        // Load locale TODO: Load from settings
        try {
            if (settings.getLocale() == -1)
                locale = Locale.getDefault();
            else
                locale = supportedLocales[settings.getLocale()];
        } catch (Exception e) {
            locale = Locale.getDefault();
        }
        // Load usually resource bundles. Others may be loaded later.
        ggo_resources = ResourceBundle.getBundle("ggo_resources", locale);
        board_resources = ResourceBundle.getBundle("board_resources", locale);

        // Preload sounds
        if (settings.getPlayClickSound())
            SoundHandler.foo();
    }
    //}}}

    //{{{ gGo() constructors
    /** Constructor for the gGo object */
    public gGo() {
        frames = new Vector();

        // Start server on localhost. Dont do this in the applet or if disabled in preferences
        if (!isApplet && !settings.getServerDisabled() && !noServer) {
            server = new Server(settings.getServerPort() != -1 ? settings.getServerPort() : 9999);
            if (server.init())
                server.start();
            else
                server = null;
        }

        // Load Help system
        try {
            ClassLoader cl = gGo.class.getClassLoader();
            URL url = HelpSet.findHelpSet(cl, helpsetName);
            HelpSet helpSet = new HelpSet(cl, url);
            helpBroker = helpSet.createHelpBroker();
        } catch (Exception e) {
            System.out.println("Failed to load Help system. Disabling...\n" + e);
            helpBroker = null;
        } catch (NoClassDefFoundError e) {
            System.out.println("Failed to load Help system. Disabling...\n" + e);
            helpBroker = null;
        }
    }

    /**
     *Constructor for the gGo object. This one is called if we run as applet
     *
     *@param  applet    True if this is called as applet.
     *@param  fileName  Description of the Parameter
     */
    public gGo(boolean applet, String fileName) {
        this();
        isApplet = applet;

        // TODO: Eventually make applet parameters to choose which window to open here
        openNewMainFrame(fileName, true);
    } //}}}

    //{{{ is13() method
    /**
     *  Check if we run on Java 1.3
     *
     *@return    True if 1.3, else false
     */
    public static boolean is13() {
        return is13;
    } //}}}

    //{{{ setIs13() method
    /**  Set 1.3 flag, called from the applet. */
    public static void setIs13() {
        is13 = true;
    } //}}}

    //{{{ getHelpBroker() method
    /**
     *  Gets the helpBroker attribute of the gGo class
     *
     *@return    The helpBroker value
     */
    public static HelpBroker getHelpBroker() {
        return helpBroker;
    } //}}}

    //{{{ hasStartUpFrame() method
    /**
     *  Check if we have a StartUpFrame. This is not the case if gGo was started with -edit option.
     *
     *@return    True if there is a StartUpFrame, else false
     */
    public static boolean hasStartUpFrame() {
        return startUpFrame != null;
    } //}}}

    //{{{ IGS Frame handling

    //{{{ openIGSWindow() method
    /**
     *  Open a new connection frame
     *
     *@return    Pointer to the opened frame object
     *@see       #closeIGSWindow()
     */
    public static IGSMainWindow openIGSWindow() {
        // Already have such a window. Bring it to the front.
        if (igsMainWindow != null) {
            // --- 1.3 ---
            if (!gGo.is13())
                igsMainWindow.requestFocusInWindow();
            else
                igsMainWindow.requestFocus();
            igsMainWindow.toFront();
        }
        else
            igsMainWindow = new IGSMainWindow();
        return igsMainWindow;
    } //}}}

    //{{{ closeIGSWindow() method
    /**
     *  Close the IGS window
     *
     *@see    #openIGSWindow()
     */
    public static void closeIGSWindow() {
        if (igsMainWindow == null) {
            System.err.println("Don't have an IGS window.");
            return;
        }
        igsMainWindow = null;
        System.err.println("Unregistered IGS window.");
    } //}}}

    //{{{ hasIGSConnection() method
    /**
     *  Check if currently a connection to a server exists.
     *
     *@return    True if a connection is open, else false.
     */
    public static boolean hasIGSConnection() {
        if (igsMainWindow == null)
            return false;
        return igsMainWindow.isConnected();
    } //}}}

    //{{{ hasIGSWindow() method
    /**
     *  Check if an IGS window is open
     *
     *@return    True if IGS window is open, else false
     */
    public static boolean hasIGSWindow() {
        return igsMainWindow != null;
    } //}}}

    //{{{ getIGSWindow() method
    /**
     *  Gets the iGSWindow attribute of the gGo class
     *
     *@return    The iGSWindow value
     */
    public static IGSMainWindow getIGSWindow() {
        return igsMainWindow;
    } //}}}

    //}}}

    //{{{ GTP Frame handling

    //{{{ openGTPWindow() method
    /**
     *  Description of the Method
     *
     *@param  parent  Description of the Parameter
     */
    public static void openGTPWindow(JFrame parent) {
        // Already have such a window. Bring it to the front.
        if (gtp != null) {
            // --- 1.3 ---
            if (!gGo.is13())
                gtp.getGTPMainFrame().requestFocusInWindow();
            else
                gtp.getGTPMainFrame().requestFocus();
            gtp.getGTPMainFrame().toFront();
        }
        else {
            GTPSetupDialog dlg = new GTPSetupDialog(parent, settings.getGTPConfig());
            dlg.setVisible(true);

            if (!dlg.getResult()) {
                if (parent == null)
                    gGo.exitApp(0);
                return;
            }

            GameData data = new GameData();
            data.playerBlack = dlg.getGTPConfig().getBlack() == GTP_COMPUTER ? getGTPResources().getString("Computer") : getGTPResources().getString("Human");
            data.playerWhite = dlg.getGTPConfig().getWhite() == GTP_COMPUTER ? getGTPResources().getString("Computer") : getGTPResources().getString("Human");
            data.size = dlg.getGTPConfig().getSize();
            data.komi = dlg.getGTPConfig().getKomi();
            data.handicap = dlg.getGTPConfig().getHandicap();

            gtp = new GTP(new String[]{settings.getGnugoPath(), settings.getGnugoArgs()},
                    dlg.getGTPConfig(), data);
            gtp.start();
        }
    } //}}}

    //{{{ closeGTPWindow() method
    /**  Description of the Method */
    public static void closeGTPWindow() {
        if (gtp == null) {
            System.err.println("Don't have a GTP window.");
            return;
        }
        gtp.close();
        gtp = null;
        System.err.println("Unregistered GTP window.");
    } //}}}

    //{{{ hasGTPWindow() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public static boolean hasGTPWindow() {
        return gtp != null;
    } //}}}

    //}}}

    //{{{ Frames handling

    //{{{ openNewMainFrame() methods
    /**
     *  Open a new MainFrame using a new thread.
     *
     *@return    Pointer to the opened MainFrame object
     *@see       #unregisterFrame(MainFrame)
     *@see       #getNumberOfOpenFrames()
     */
    public static MainFrame openNewMainFrame() {
        MainFrame mainFrame = new MainFrame();
        Thread t = new Thread(mainFrame);
        t.start();
        frames.addElement(mainFrame);
        return mainFrame;
    }

    /**
     *  Open a new MainFrame using a new thread and load a game in it.
     *
     *@param  fileName  Filename of the game to load
     *@param  remName   If true, remember the filename
     *@return           Pointer to the opened MainFrame object
     *@see              #unregisterFrame(MainFrame)
     *@see              #getNumberOfOpenFrames()
     */
    public static MainFrame openNewMainFrame(String fileName, boolean remName) {
        MainFrame mainFrame = openNewMainFrame();
        if (fileName != null)
            mainFrame.openSGF(fileName, remName);
        return mainFrame;
    }

    /**
     *  Description of the Method
     *
     *@param  synchFrame  Description of the Parameter
     *@return             Description of the Return Value
     */
    public static MainFrame openNewMainFrame(MainFrame synchFrame) {
        MainFrame mainFrame = new MainFrame(synchFrame);
        Thread t = new Thread(mainFrame);
        t.start();
        frames.addElement(mainFrame);
        return mainFrame;
    }
    //}}}

    //{{{ hasModifiedBoards() method
    /**
     *  Check if there are modified boards
     *
     *@return    True if there are modified board, prompt user on exit. False if
     *           no modified boards are registered.
     */
    public static boolean hasModifiedBoards() {
        if (frames.isEmpty())
            return false;

        for (Enumeration e = frames.elements(); e.hasMoreElements(); ) {
            MainFrame mf = (MainFrame)e.nextElement();
            if (mf.getBoard().isModified())
                return true;
        }

        return false;
    } //}}}

    //{{{ unregisterFrame() method
    /**
     *  Remove a MainFrame object.
     *
     *@param  mf  MainFrame object to remove
     *@see        #openNewMainFrame()
     *@see        #getNumberOfOpenFrames()
     */
    public static void unregisterFrame(MainFrame mf) {
        frames.remove(mf);
    } //}}}

    //{{{ getNumberOfOpenFrames() method
    /**
     *  Gets the number of open frames
     *
     *@return    The number of open frames
     *@see       #openNewMainFrame()
     *@see       #unregisterFrame(MainFrame)
     */
    public static int getNumberOfOpenFrames() {
        if (hasIGSWindow() || hasGTPWindow())
            return -1;
        return frames.size();
    } //}}}

    //{{{ getFirstMainFrame() method
    /**
     *  Gets the firstMainFrame attribute of the gGo class
     *
     *@return    The firstMainFrame value
     */
    public static MainFrame getFirstMainFrame() {
        try {
            return (MainFrame)frames.get(0);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    } //}}}

    //}}}

    //{{{ exitApp() method
    /**
     *  Description of the Method
     *
     *@param  returnCode  Description of the Parameter
     */
    public static void exitApp(int returnCode) {
        System.err.println("gGo.exitApp(" + returnCode + ")");
        if (server != null) {
            server.interrupt();
            server.closeAll();
        }
        // Don't call this if we use the applet.
        if (!isApplet)
            System.exit(returnCode);
    } //}}}

    //{{{ forceGarbageCollection() method
    /**
     *  Force the VM to perform a garbage collection
     *
     *@return    The amount of freed memory in KB
     */
    public static int forceGarbageCollection() {
        // Shamelessly taken from JEdit code.
        Runtime rt = Runtime.getRuntime();
        int before = (int)(rt.freeMemory() / 1024);
        System.gc();
        int after = (int)(rt.freeMemory() / 1024);
        int total = (int)(rt.totalMemory() / 1024);

        return after - before;
    } //}}}

    //{{{ getMemoryStatus() method
    /**
     *  Returns a string showing the current memory status. Used memory / Total
     *  memory
     *
     *@return    The memory Status
     */
    public static String getMemoryStatus() {
        // Shamelessly taken from JEdit code.
        Runtime runtime = Runtime.getRuntime();
        int freeMemory = (int)(runtime.freeMemory() / 1024);
        int totalMemory = (int)(runtime.totalMemory() / 1024);
        int usedMemory = (totalMemory - freeMemory);
        float fraction = ((float)usedMemory) / totalMemory;

        return (usedMemory / 1024) + board_resources.getString("MB") + "/" +
                (totalMemory / 1024) + board_resources.getString("MB");
    } //}}}

    //{{{ showAbout() method
    /**
     *  Show about dialog
     *
     *@param  frame  Parent frame
     */
    public static void showAbout(JFrame frame) {
        JOptionPane.showMessageDialog(frame,
                PACKAGE + " " + VERSION +
                "\n\n" + ggo_resources.getString("copyright") + " (c) 2002\nPeter Strempel <pstrempel@t-online.de>" +
                "\n\n" + ggo_resources.getString("language_author") +
                "\n\n" + ggo_resources.getString("running_on") + " " + System.getProperty("java.version"),
                ggo_resources.getString("about_title"),
                JOptionPane.INFORMATION_MESSAGE,
                ImageHandler.getgGoImageIcon());
    } //}}}

    //{{{ getSettings() method
    /**
     *  Get global settings
     *
     *@return    The settings value
     */
    public static Settings getSettings() {
        return settings;
    } //}}}

    //{{{ getGTP() method
    /**
     *  Gets the GTP object
     *
     *@return    The GTP object
     */
    public static GTP getGTP() {
        return gtp;
    } //}}}

    //{{{ I18n methods

    //{{{ getLocale() method
    /**
     *  Gets the locale attribute of the gGo class
     *
     *@return    The locale value
     */
    public static Locale getLocale() {
        return locale;
    } //}}}

    //{{{ getgGoResources() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public static ResourceBundle getgGoResources() {
        return ggo_resources;
    } //}}}

    //{{{ getBoardResources() method
    /**
     *  Gets the boardResources attribute of the gGo class
     *
     *@return    The boardResources value
     */
    public static ResourceBundle getBoardResources() {
        return board_resources;
    } //}}}

    //{{{ getGTPResources() method
    /**
     *  Gets the gTPResources attribute of the gGo class
     *
     *@return    The gTPResources value
     */
    public static ResourceBundle getGTPResources() {
        if (gtp_resources == null)
            gtp_resources = ResourceBundle.getBundle("gtp_resources", locale);
        return gtp_resources;
    } //}}}

    //{{{ getSGFResources() method
    /**
     *  Gets the sGFResources attribute of the gGo class
     *
     *@return    The sGFResources value
     */
    public static ResourceBundle getSGFResources() {
        if (sgf_resources == null)
            sgf_resources = ResourceBundle.getBundle("sgf_resources", locale);
        return sgf_resources;
    } //}}}

    //{{{ getIGSResources() method
    /**
     *  Gets the iGSResources attribute of the gGo class
     *
     *@return    The iGSResources value
     */
    public static ResourceBundle getIGSResources() {
        if (igs_resources == null)
            igs_resources = ResourceBundle.getBundle("igs_resources", locale);
        return igs_resources;
    } //}}}

    //{{{ getIGSPlayerResources() method
    /**
     *  Gets the iGSPlayerResources attribute of the gGo class
     *
     *@return    The iGSPlayerResources value
     */
    public static ResourceBundle getIGSPlayerResources() {
        if (igs_player_resources == null)
            igs_player_resources = ResourceBundle.getBundle("igs_player_resources", locale);
        return igs_player_resources;
    } //}}}

    //{{{ getIGSMatchResources() method
    /**
     *  Gets the iGSMatchResources attribute of the gGo class
     *
     *@return    The iGSMatchResources value
     */
    public static ResourceBundle getIGSMatchResources() {
        if (igs_match_resources == null)
            igs_match_resources = ResourceBundle.getBundle("igs_match_resources", locale);
        return igs_match_resources;
    } //}}}

    //{{{ getIGSBozoResources() method
    /**
     *  Gets the iGSBozoResources attribute of the gGo class
     *
     *@return    The iGSBozoResources value
     */
    public static ResourceBundle getIGSBozoResources() {
        if (igs_bozo_resources == null)
            igs_bozo_resources = ResourceBundle.getBundle("igs_bozo_resources", locale);
        return igs_bozo_resources;
    } //}}}

    //{{{ getIGSMessageResources() method
    /**
     *  Gets the iGSMessageResources attribute of the gGo class
     *
     *@return    The iGSMessageResources value
     */
    public static ResourceBundle getIGSMessageResources() {
        if (igs_message_resources == null)
            igs_message_resources = ResourceBundle.getBundle("igs_message_resources", locale);
        return igs_message_resources;
    } //}}}

    //{{{ getClipboardDialogResources() method
    /**
     *  Gets the clipboardDialogResources attribute of the gGo class
     *
     *@return    The clipboardDialogResources value
     */
    public static ResourceBundle getClipboardDialogResources() {
        if (clipboard_dialog_resources == null)
            clipboard_dialog_resources = ResourceBundle.getBundle("clipboard_dialog_resources", locale);
        return clipboard_dialog_resources;
    } //}}}

    //}}}

    //{{{ convertLookAndFeel() method
    /**
     *  Get the look and feel class name
     *
     *@param  i  Int value from dialog combobox
     *@return    String with LookAndFeel class name
     *@see       ggo.Defines
     */
    public static String convertLookAndFeel(int i) {
        switch (i) {
            case LOOKANDFEEL_JAVA:
                return UIManager.getCrossPlatformLookAndFeelClassName();
            case LOOKANDFEEL_WINDOWS:
                return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
            case LOOKANDFEEL_SKIN:
                return "com.l2fprod.gui.plaf.skin.SkinLookAndFeel";
            case LOOKANDFEEL_KUNSTSTOFF:
                return "com.incors.plaf.kunststoff.KunststoffLookAndFeel";
            case LOOKANDFEEL_MOTIF:
                return "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
            case LOOKANDFEEL_MAC:
                return "javax.swing.plaf.mac.MacLookAndFeel";
            case LOOKANDFEEL_METOUIA:
                return "net.sourceforge.mlf.metouia.MetouiaLookAndFeel";
            case LOOKANDFEEL_SYSTEM:
                return UIManager.getSystemLookAndFeelClassName();
        }
        return UIManager.getSystemLookAndFeelClassName();
    } //}}}

    //{{{ setTheme() method
    /**
     *  Sets the theme attribute of the gGo class
     *
     *@param  fileName  Filename of the themepack file
     *@return           True if successful, else false
     */
    public static boolean setTheme(String fileName) {
        System.err.println("Trying to apply theme: " + fileName);
        try {
            // Get default themepack from classpath
            if (fileName.equals("skinlf-themepack.xml")) {
                com.l2fprod.gui.plaf.skin.SkinLookAndFeel.setSkin(
                        com.l2fprod.gui.plaf.skin.SkinLookAndFeel.loadThemePackDefinition(gGo.class.getClassLoader().getResource(fileName)));
            }
            else if (fileName.endsWith(".xml")) {
                com.l2fprod.gui.plaf.skin.SkinLookAndFeel.setSkin(
                        com.l2fprod.gui.plaf.skin.SkinLookAndFeel.loadThemePackDefinition(com.l2fprod.gui.plaf.skin.SkinUtils.toURL(new File(fileName))));
            }
            else {
                com.l2fprod.gui.plaf.skin.SkinLookAndFeel.setSkin(
                        com.l2fprod.gui.plaf.skin.SkinLookAndFeel.loadThemePack(fileName));
            }
            com.l2fprod.gui.plaf.skin.SkinLookAndFeel.enable();
            // SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            System.err.println("Failed to set theme " + fileName + ": " + e);
            return false;
        } catch (NoClassDefFoundError e) {
            System.err.println("Failed to set theme " + fileName + ": " + e);
            return false;
        } catch (Error e) {
            System.err.println("Failed to set theme " + fileName + ": " + e);
            return false;
        }

        System.err.println("Theme applied");
        return true;
    } //}}}

    //{{{ initLookAndFeel() method
    /**
     *  Init LookAndFeel
     *
     *@param  skin  Skin name
     */
    public static void initLookAndFeel(String skin) {
        try {
            if (skin != null) {
                settings.setLookAndFeel(LOOKANDFEEL_SKIN);
                settings.setThemePack(skin);
            }
            if (settings.getLookAndFeel() == LOOKANDFEEL_SKIN) {
                String themeStr;
                if (settings.getThemePack() != null && settings.getThemePack().length() > 0)
                    themeStr = settings.getThemePack();
                else
                    themeStr = "skinlf-themepack.xml";
                if (!setTheme(themeStr)) {
                    gGo.getSettings().setLookAndFeel(LOOKANDFEEL_SYSTEM);
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Whoops, skinlf.jar or the themepack was not found.\n" + e);
            gGo.getSettings().setLookAndFeel(LOOKANDFEEL_SYSTEM);
            return;
        } catch (NoClassDefFoundError e) {
            System.err.println("Whoops, skinlf.jar or the themepack was not found.\n" + e);
            gGo.getSettings().setLookAndFeel(LOOKANDFEEL_SYSTEM);
            return;
        } catch (Error e) {
            System.err.println("Whoops, skinlf.jar or the themepack was not found.\n" + e);
            gGo.getSettings().setLookAndFeel(LOOKANDFEEL_SYSTEM);
            return;
        }

        try {
            String lnfName = convertLookAndFeel(settings.getLookAndFeel());
            // Apply own theme to Metal look and feel
            if (lnfName.equals("javax.swing.plaf.metal.MetalLookAndFeel"))
                MetalLookAndFeel.setCurrentTheme(new gGoMetalTheme());
            UIManager.setLookAndFeel(lnfName);

            // Quick and ugly... TODO
            if (UIManager.getLookAndFeel().getClass().getName().equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel") &&
                    settings.getFixWindowsFont()) {
                java.awt.Font f = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12);
                java.util.Enumeration keys = UIManager.getDefaults().keys();
                while (keys.hasMoreElements()) {
                    Object key = keys.nextElement();
                    Object value = UIManager.get(key);
                    if (value instanceof javax.swing.plaf.FontUIResource)
                        UIManager.put(key, f);
                }
                System.err.println("Japanese locale with Windows look and feel detected, Windows fonts adjusted");
            }
        } catch (Exception e) {
            System.err.println("Failed to set Look and Feel: " + e);
            gGo.getSettings().setLookAndFeel(LOOKANDFEEL_SYSTEM);
        }
    } //}}}

    //{{{ connect() method
    /**
     *  Try to connect to a running server to open a file
     *
     *@param  fileName  Name of file to open
     *@param  port      Port server is running on
     *@return           True if connection was established, else false
     */
    public static boolean connect(int port, String fileName) {
        System.err.println("Trying to connect to server...");

        Socket socket = null;
        try {
            socket = new Socket(InetAddress.getByName("127.0.0.1"), port);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // Write key
            // out.writeInt(123);

            // Write filename
            for (int i = 0, sz = fileName.length(); i < sz; i++)
                out.writeChar(fileName.charAt(i));
            out.writeChar('\0');

            out.close();
            socket.close();
        } catch (IOException e) {
            System.err.println("Failed to connect to server at port " + port + ": " + e);
            return false;
        }
        return true;
    } //}}}

    //{{{ usage() and version() methods
    /**  Print command line usage information. */
    public static void usage() {
        System.out.println("Usage: ggo [options] [file]" +
                "\n       -version:         Show version and exit." +
                "\n       -edit:            Start gGo in edit mode. You can give a filename to load a game." +
                "\n       -igs:             Start gGo in IGS Connection mode. A filename will be ignored." +
                "\n       -gtp:             Start gGo in gtp mode. A filename will be ignored." +
                "\n       -noserver:        Don't run the localhost server." +
                "\n       -skin <file>:     Use this themepack for the Skin look and leel." +
                "\n       -keepstderr:      Don't log to a file, print to stderr." +
                "\n       -logfile <file>:  Log output to this file instead to ~/.ggo.log" +
                "\n       -usage:           Show this message and exit.\n\n");
    }

    /**  Print current version */
    public static void version() {
        System.out.println(PACKAGE + " " + VERSION);
    } //}}}

    //{{{ main() method
    /**
     *  The main program for the gGo class
     *
     *@param  args  The command line arguments
     */
    public static void main(String[] args) {
        // Check Java version. Shamelessly copied from JEdit
        // --- 1.3 ---
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.compareTo("1.3") < 0) {
            System.out.println("You are running Java version " + javaVersion +
                    "\ngGo requires Java 1.3 or later.\n" +
                    "Please visit http://java.sun.com/ and download the latest Java version.");
            System.exit(0);
        }
        if (javaVersion.compareTo("1.4") < 0) {
            System.out.println("You are running Java version " + javaVersion +
                    "\ngGo will run, but some features will be missing." +
                    "\nIt is recommended to use Java 1.4." +
                    "\nPlease visit http://java.sun.com/ and download the latest Java version.");
            is13 = true;
        }

        boolean editMode = false;
        boolean igsMode = false;
        boolean gtpMode = false;
        boolean noRedirectErr = false;
        String skin = null;
        String logFile = null;
        noServer = false;

        //{{{ Parse command line arguments
        String fileName = null;
        if (args.length > 0) {
            boolean flag = false;

            for (int i = 0, sz = args.length; i < sz; i++) {
                if (args[i].startsWith("-") && !flag) {
                    if (args[i].equals("-usage") || args[i].equals("-help") || args[i].equals("-h")) {
                        version();
                        usage();
                        System.exit(0);
                    }
                    else if (args[i].equals("-version")) {
                        version();
                        System.exit(0);
                    }
                    else if (args[i].equals("-edit")) {
                        editMode = true;
                    }
                    else if (args[i].equals("-igs")) {
                        igsMode = true;
                    }
                    else if (args[i].equals("-gtp")) {
                        gtpMode = true;
                    }
                    else if (args[i].equals("-noserver")) {
                        noServer = true;
                    }
                    else if (args[i].equals("-keepstderr")) {
                        noRedirectErr = true;
                    }
                    else if (args[i].equals("-logfile")) {
                        if (noRedirectErr) {
                            System.out.println("Options both -keepstderr and -logfile makes no sense.");
                            usage();
                            System.exit(0);
                        }
                        try {
                            logFile = args[i + 1];
                            if (logFile.startsWith("-"))
                                throw new ArrayIndexOutOfBoundsException();
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println("Option -logfile requires argument.");
                            usage();
                            System.exit(0);
                        }
                        i++;
                    }
                    else if (args[i].equals("-skin")) {
                        try {
                            skin = args[i + 1];
                            if (skin.startsWith("-"))
                                throw new ArrayIndexOutOfBoundsException();
                        } catch (ArrayIndexOutOfBoundsException e) {
                            System.out.println("Option -skin requires argument.");
                            usage();
                            System.exit(0);
                        }
                        i++;
                    }
                    else {
                        System.out.println("Unknown option: " + args[i]);
                        usage();
                        System.exit(0);
                    }
                }
                else {
                    flag = true;
                    fileName = args[i];
                    break;
                }
            }
        } //}}}

        //{{{ Try connecting to a running gGo instance if a filename is given
        if (!igsMode && !gtpMode && !settings.getServerDisabled() && !noServer &&
                fileName != null && fileName.length() > 0 &&
                connect(settings.getServerPort() != -1 ? settings.getServerPort() : 9999, fileName))
            // Worked. Exit this VM.
            System.exit(0);
        //}}}

        //{{{ Redirect stderr
        if (!noRedirectErr) {
            try {
                if (logFile == null)
                    logFile = System.getProperty("user.home") + "/.ggo.log";
                File f = new File(logFile);
                if (f.createNewFile() || f.canWrite())
                    System.setErr(new PrintStream(new FileOutputStream(f)));
            } catch (IOException e) {
                System.err.println("Failed to init debug log: " + e);
            } catch (java.security.AccessControlException e) {
                System.err.println("Failed to init debug log: " + e);
            } catch (SecurityException e) {
                System.err.println("Failed to init debug log: " + e);
            }
        } //}}}

        // Print this to the logfile. Else it looks silly.
        System.err.println("Using locale: " + locale);

        // Set LookAndFeel and Theme
        initLookAndFeel(skin);
        gGo ggo = new gGo();
        UIManager.getLookAndFeelDefaults().put("ClassLoader", ggo.getClass().getClassLoader());

        if (editMode)
            openNewMainFrame(fileName, true);
        else if (igsMode)
            openIGSWindow();
        else if (gtpMode)
            openGTPWindow(null);
        else {
            startUpFrame = new StartUpFrame();
            startUpFrame.setVisible(true);
            if (fileName != null && fileName.length() > 0)
                openNewMainFrame(fileName, true);
        }
    } //}}}
}

