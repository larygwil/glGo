/*
 *  Settings.java
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
package ggo.utils;

import ggo.Defines;
import ggo.igs.HostConfig;
import ggo.gtp.GTPConfig;
import ggo.utils.Utils;
import ggo.igs.chatter.IGSChatter;
import ggo.igs.BozoHandler;
import ggo.gui.Clock;
import java.io.*;
import java.util.*;
import java.security.*;
import java.awt.Dimension;
import java.awt.Point;

/**
 *  Application settings to save on the disk, using the Java properties.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.11 $, $Date: 2002/10/05 11:14:05 $
 */
public class Settings implements Defines {
    //{{{ private members
    private boolean showToolbar, showCoords, showVariationGhosts, showHorizontalComment, playClickSound,
            showCursor, showSidebar, showStatusbar, doRemDir, showSlider, serverDisabled, iGSshowShouts,
            antiSlip, viewComm, igsChatterShowToolbar, playChatSound, simpleSound, storeLocation, storeSize,
            fixWindowsFont, igsDisplayInfo, igsDisplayMoves, doAutoReply;
    private int locale, lookAndFeel, serverPort, iGSMatchMainTime, iGSMatchByoyomiTime, iGSChatterType,
            serifFontSize, sansSerifFontSize, monospacedFontSize, clickType, playClockSound,
            timeWarningPeriod, antiSlipDelay, autoawayTime, sidebarLayout;
    private String remDir, gnugoPath, gnugoArgs, themePack, autoawayMessage, upperRank, lowerRank, friends,
            bozos, autoReply;
    private Properties props;
    private static String propFile = null;
    private GTPConfig gtpConfig;
    private Dimension frameSize = null;
    private Vector hostConfigList;
    private int selectedHostConfig;
    //}}}

    //{{{ Settings() constructor
    /**  Constructor for the Settings object */
    public Settings() {
        hostConfigList = new Vector();
        props = new Properties();
        boolean err = false;

        try {
            propFile = System.getProperty("user.home") + "/.ggorc";
        } catch (AccessControlException e) {
            System.err.println("Failed to get property file: " + e);
            initDefaults(true);
            return;
        }

        try {
            FileInputStream in = new FileInputStream(propFile);
            props.load(in);
            in.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not load file '" + propFile + "'\n" + e);
            err = true;
        } catch (IOException e) {
            System.err.println("Could not load file '" + propFile + "'\n" + e);
            err = true;
        }

        if (!err)
            initSettings();
        else
            initDefaults(true);
    } //}}}

    //{{{ initSettings() method
    /**  Init the settings from properties */
    private void initSettings() {
        locale = Utils.convertStringToInt(props.getProperty("locale"));
        fixWindowsFont = new Boolean(props.getProperty("fixWindowsFont")).booleanValue();
        lookAndFeel = Utils.convertStringToInt(props.getProperty("lookAndFeel"));
        themePack = props.getProperty("themePack");
        showSidebar = new Boolean(props.getProperty("showSidebar")).booleanValue();
        sidebarLayout = Utils.convertStringToInt(props.getProperty("sidebarLayout"));
        showToolbar = new Boolean(props.getProperty("showToolbar")).booleanValue();
        showCoords = new Boolean(props.getProperty("showCoords")).booleanValue();
        showCursor = new Boolean(props.getProperty("showCursor")).booleanValue();
        showSlider = new Boolean(props.getProperty("showSlider")).booleanValue();
        showStatusbar = new Boolean(props.getProperty("showStatusbar")).booleanValue();
        showVariationGhosts = new Boolean(props.getProperty("showVariationGhosts")).booleanValue();
        showHorizontalComment = new Boolean(props.getProperty("showHorizontalComment")).booleanValue();
        playClickSound = new Boolean(props.getProperty("playClickSound")).booleanValue();
        playClockSound = Utils.convertStringToInt(props.getProperty("playClockSound"));
        if (props.getProperty("timeWarningPeriod") != null)
            timeWarningPeriod = Utils.convertStringToInt(props.getProperty("timeWarningPeriod"));
        else
            timeWarningPeriod = 30;
        doRemDir = new Boolean(props.getProperty("doRemDir")).booleanValue();
        remDir = props.getProperty("remDir");
        gnugoPath = props.getProperty("gnugoPath");
        gnugoArgs = props.getProperty("gnugoArgs");
        serverDisabled = new Boolean(props.getProperty("localServerDisabled")).booleanValue();
        serverPort = Utils.convertStringToInt(props.getProperty("localServerPort"));
        iGSshowShouts = new Boolean(props.getProperty("iGSshowShouts")).booleanValue();
        viewComm = new Boolean(props.getProperty("viewComm")).booleanValue();
        if (props.getProperty("iGSMatchMainTime") != null)
            iGSMatchMainTime = Utils.convertStringToInt(props.getProperty("iGSMatchMainTime"));
        else
            iGSMatchMainTime = 10;
        if (props.getProperty("iGSMatchByoyomiTime") != null)
            iGSMatchByoyomiTime = Utils.convertStringToInt(props.getProperty("iGSMatchByoyomiTime"));
        else
            iGSMatchByoyomiTime = 10;
        antiSlip = new Boolean(props.getProperty("antiSlip")).booleanValue();
        if (props.getProperty("antiSlipDelay") != null)
            antiSlipDelay = Utils.convertStringToInt(props.getProperty("antiSlipDelay"));
        else
            antiSlipDelay = 500;
        if (props.getProperty("clickType") != null)
            clickType = Utils.convertStringToInt(props.getProperty("clickType"));
        else
            clickType = CLICK_TYPE_SINGLECLICK;
        if (props.getProperty("frameSizeX") != null)
            frameSize = new Dimension(
                    Utils.convertStringToInt(props.getProperty("frameSizeX")),
                    Utils.convertStringToInt(props.getProperty("frameSizeY")));
        if (props.getProperty("serifFontSize") != null)
            serifFontSize = Utils.convertStringToInt(props.getProperty("serifFontSize"));
        else
            serifFontSize = 12;
        if (props.getProperty("sansSerifFontSize") != null)
            sansSerifFontSize = Utils.convertStringToInt(props.getProperty("sansSerifFontSize"));
        else
            sansSerifFontSize = 12;
        if (props.getProperty("monospacedFontSize") != null)
            monospacedFontSize = Utils.convertStringToInt(props.getProperty("monospacedFontSize"));
        else
            monospacedFontSize = 12;
        if (props.getProperty("iGSChatterType") != null)
            iGSChatterType = Utils.convertStringToInt(props.getProperty("iGSChatterType"));
        else
            iGSChatterType = IGSChatter.IGSCHATTER_DESKTOPPANE;
        if (props.getProperty("igsDisplayInfo") != null)
            igsDisplayInfo = new Boolean(props.getProperty("igsDisplayInfo")).booleanValue();
        else
            igsDisplayInfo = true;
        igsDisplayMoves = new Boolean(props.getProperty("igsDisplayMoves")).booleanValue();
        igsChatterShowToolbar = new Boolean(props.getProperty("igsChatterShowToolbar")).booleanValue();
        playChatSound = new Boolean(props.getProperty("playChatSound")).booleanValue();
        autoawayTime = Utils.convertStringToInt(props.getProperty("autoawayTime"));
        autoawayMessage = props.getProperty("autoawayMessage");
        simpleSound = new Boolean(props.getProperty("simpleSound")).booleanValue();
        upperRank = props.getProperty("upperRank");
        lowerRank = props.getProperty("lowerRank");
        storeLocation = new Boolean(props.getProperty("storeLocation")).booleanValue();
        storeSize = new Boolean(props.getProperty("storeSize")).booleanValue();
        doAutoReply = new Boolean(props.getProperty("doAutoReply")).booleanValue();
        autoReply = props.getProperty("autoReply");
        friends = props.getProperty("friends");
        bozos = props.getProperty("bozos");

        gtpConfig = new GTPConfig();
        gtpConfig.setSize(Utils.convertStringToInt(props.getProperty("GTP_SIZE")));
        gtpConfig.setBlack(Utils.convertStringToInt(props.getProperty("GTP_BLACK")));
        gtpConfig.setWhite(Utils.convertStringToInt(props.getProperty("GTP_WHITE")));
        gtpConfig.setHandicap(Utils.convertStringToInt(props.getProperty("GTP_HANDICAP")));
        gtpConfig.setKomi(Utils.convertStringToFloat(props.getProperty("GTP_KOMI")));
        gtpConfig.setLevel(Utils.convertStringToInt(props.getProperty("GTP_LEVEL")));

        loadHostConfigList();
    } //}}}

    //{{{ initDefaults() method
    /**
     *  If no properties file was found, init the settings with default values.
     *
     *@param  updateHost  Description of the Parameter
     */
    public void initDefaults(boolean updateHost) {
        lookAndFeel = LOOKANDFEEL_SYSTEM;
        themePack = "skinlf-themepack.xml";
        showSidebar = true;
        sidebarLayout = SIDEBAR_EAST;
        showToolbar = true;
        showCoords = true;
        showCursor = true;
        showSlider = true;
        showStatusbar = true;
        showVariationGhosts = true;
        showHorizontalComment = false;
        playClickSound = true;
        playClockSound = Clock.WARN_SOUND_ONCE;
        playChatSound = true;
        timeWarningPeriod = 30;
        doRemDir = true;
        remDir = "";
        gnugoArgs = "--mode gtp --quiet";
        serverDisabled = false;
        serverPort = 9999;
        antiSlip = false;
        antiSlipDelay = 500;
        clickType = CLICK_TYPE_SINGLECLICK;
        frameSize = new Dimension(0, 0);
        serifFontSize = sansSerifFontSize = monospacedFontSize = 12;
        iGSChatterType = IGSChatter.IGSCHATTER_DESKTOPPANE;
        igsChatterShowToolbar = true;
        viewComm = false;
        storeLocation = false;
        storeSize = false;
        igsDisplayInfo = true;
        igsDisplayMoves = false;
        // Dont update this on reset
        if (updateHost) {
            locale = -1; // System default
            fixWindowsFont = false;
            gnugoPath = "gnugo";
            iGSshowShouts = true;
            iGSMatchMainTime = 10;
            iGSMatchByoyomiTime = 10;
            autoawayTime = 0;
            autoawayMessage = null;
            simpleSound = true;
            upperRank = "9p";
            lowerRank = "NR";
            hostConfigList.add(new HostConfig("igs.joyjoy.net", 7777, "guest", ""));
            selectedHostConfig = 0;
            gtpConfig = new GTPConfig();
            doAutoReply = false;
            autoReply = null;
        }
    } //}}}

    //{{{ saveSettings() method
    /**  Save properties file to disc */
    public void saveSettings() {
        try {
            props.setProperty("locale", String.valueOf(locale));
            props.setProperty("fixWindowsFont", String.valueOf(fixWindowsFont));
            props.setProperty("lookAndFeel", String.valueOf(lookAndFeel));
            props.setProperty("themePack", themePack);
            props.setProperty("showSidebar", String.valueOf(showSidebar));
            props.setProperty("sidebarLayout", String.valueOf(sidebarLayout));
            props.setProperty("showToolbar", String.valueOf(showToolbar));
            props.setProperty("showCoords", String.valueOf(showCoords));
            props.setProperty("showCursor", String.valueOf(showCursor));
            props.setProperty("showSlider", String.valueOf(showSlider));
            props.setProperty("showStatusbar", String.valueOf(showStatusbar));
            props.setProperty("showVariationGhosts", String.valueOf(showVariationGhosts));
            props.setProperty("showHorizontalComment", String.valueOf(showHorizontalComment));
            props.setProperty("playClickSound", String.valueOf(playClickSound));
            props.setProperty("playClockSound", String.valueOf(playClockSound));
            props.setProperty("timeWarningPeriod", String.valueOf(timeWarningPeriod));
            props.setProperty("doRemDir", String.valueOf(doRemDir));
            props.setProperty("remDir", remDir);
            props.setProperty("upperRank", upperRank);
            props.setProperty("lowerRank", lowerRank);
            props.setProperty("gnugoPath", gnugoPath);
            props.setProperty("gnugoArgs", gnugoArgs != null ? gnugoArgs : "--mode gtp --quiet");
            props.setProperty("GTP_SIZE", String.valueOf(gtpConfig.getSize()));
            props.setProperty("GTP_BLACK", String.valueOf(gtpConfig.getBlack()));
            props.setProperty("GTP_WHITE", String.valueOf(gtpConfig.getWhite()));
            props.setProperty("GTP_HANDICAP", String.valueOf(gtpConfig.getHandicap()));
            props.setProperty("GTP_KOMI", String.valueOf(gtpConfig.getKomi()));
            props.setProperty("GTP_LEVEL", String.valueOf(gtpConfig.getLevel()));
            props.setProperty("localServerDisabled", String.valueOf(serverDisabled));
            props.setProperty("localServerPort", String.valueOf(serverPort));
            props.setProperty("iGSshowShouts", String.valueOf(iGSshowShouts));
            props.setProperty("iGSMatchMainTime", String.valueOf(iGSMatchMainTime));
            props.setProperty("iGSMatchByoyomiTime", String.valueOf(iGSMatchByoyomiTime));
            props.setProperty("antiSlip", String.valueOf(antiSlip));
            props.setProperty("antiSlipDelay", String.valueOf(antiSlipDelay));
            props.setProperty("clickType", String.valueOf(clickType));
            if (frameSize != null) {
                props.setProperty("frameSizeX", String.valueOf(frameSize.width));
                props.setProperty("frameSizeY", String.valueOf(frameSize.height));
            }
            props.setProperty("serifFontSize", String.valueOf(serifFontSize));
            props.setProperty("sansSerifFontSize", String.valueOf(sansSerifFontSize));
            props.setProperty("monospacedFontSize", String.valueOf(monospacedFontSize));
            props.setProperty("iGSChatterType", String.valueOf(iGSChatterType));
            props.setProperty("viewComm", String.valueOf(viewComm));
            props.setProperty("igsChatterShowToolbar", String.valueOf(igsChatterShowToolbar));
            props.setProperty("playChatSound", String.valueOf(playChatSound));
            props.setProperty("autoawayTime", String.valueOf(autoawayTime));
            props.setProperty("autoawayMessage", autoawayMessage == null ? "" : autoawayMessage);
            props.setProperty("simpleSound", String.valueOf(simpleSound));
            props.setProperty("storeLocation", String.valueOf(storeLocation));
            props.setProperty("storeSize", String.valueOf(storeSize));
            props.setProperty("igsDisplayInfo", String.valueOf(igsDisplayInfo));
            props.setProperty("igsDisplayMoves", String.valueOf(igsDisplayMoves));
            if (friends != null)
                props.setProperty("friends", friends);
            else
                props.setProperty("friends", "");
            if (bozos != null)
                props.setProperty("bozos", bozos);
            else
                props.setProperty("bozos", "");
            props.setProperty("doAutoReply", String.valueOf(doAutoReply));
            if (autoReply != null && autoReply.length() > 0)
                props.setProperty("autoReply", autoReply);
            saveHostConfigList();

            FileOutputStream out = new FileOutputStream(propFile);
            props.store(out, PACKAGE + "-" + VERSION);
            out.close();
        } catch (NullPointerException e) {
            System.err.println("Failed to save settings: " + e);
            e.printStackTrace();
            return;
        } catch (FileNotFoundException e) {
            System.err.println("Failed to save settings: " + e);
            return;
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e);
            return;
        }
    } //}}}

    //{{{ HostConfig methods

    //{{{ getHostConfigList() method
    /**
     *  Gets the hostConfigList attribute of the Settings object
     *
     *@return    The hostConfigList value
     */
    public Vector getHostConfigList() {
        return hostConfigList;
    } //}}}

    //{{{ getHostConfig(int) method
    /**
     *  Gets the hostConfig attribute of the Settings object
     *
     *@param  index  Description of the Parameter
     *@return        The hostConfig value
     */
    public HostConfig getHostConfig(int index) {
        if (hostConfigList.isEmpty()) {
            hostConfigList.add(new HostConfig(HostConfig.DEFAULT_HOST,
                    HostConfig.DEFAULT_PORT,
                    HostConfig.DEFAULT_NAME,
                    ""));
            selectedHostConfig = 0;
            index = 0;
        }
        try {
            return (HostConfig)hostConfigList.get(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Failed to find host config: " + e);
            return null;
        }
    } //}}}

    //{{{ getHostConfig(String) method
    /**
     *  Gets the hostConfig attribute of the Settings object
     *
     *@param  id  Description of the Parameter
     *@return     The hostConfig value
     */
    public HostConfig getHostConfig(String id) {
        for (Enumeration e = hostConfigList.elements(); e.hasMoreElements(); ) {
            HostConfig hostConfig = (HostConfig)e.nextElement();
            if (hostConfig.getID().equals(id))
                return hostConfig;
        }
        return null;
    } //}}}

    //{{{ getCurrentHostConfig() method
    /**
     *  Gets the currentHostConfig attribute of the Settings object
     *
     *@return    The currentHostConfig value
     */
    public HostConfig getCurrentHostConfig() {
        return getHostConfig(selectedHostConfig);
    } //}}}

    //{{{ setCurrentHostConfig() method
    /**
     *  Sets the currentHostConfig attribute of the Settings object
     *
     *@param  id  The new currentHostConfig value
     */
    public void setCurrentHostConfig(String id) {
        int i = 0;
        int n = -1;
        for (Enumeration e = hostConfigList.elements(); e.hasMoreElements(); i++) {
            HostConfig hostConfig = (HostConfig)e.nextElement();
            if (hostConfig.getID().equals(id)) {
                n = i;
                break;
            }
        }
        if (n != -1)
            selectedHostConfig = n;
        else
            System.err.println("Failed to find host config index for id " + id);
    } //}}}

    //{{{ loadHostConfigList() method
    /**  Load the host config settings */
    private void loadHostConfigList() {
        String toParse = props.getProperty("hostConfig");
        if (toParse == null || toParse.length() == 0 || toParse.indexOf("###") == -1) {
            System.err.println("Not host config found.");
            return;
        }

        try {
            int pos1 = toParse.indexOf("###");
            int oldPos1 = 0;
            String[] h;
            do {
                String s = toParse.substring(oldPos1, pos1);
                if (s.startsWith("###"))
                    s = s.substring(3, s.length());

                int pos2 = s.indexOf("#");
                int oldPos2 = 0;
                int i = 0;
                h = new String[5];
                do {
                    String ss = s.substring(oldPos2, pos2);
                    if (ss.startsWith("#"))
                        ss = ss.substring(1, ss.length());
                    h[i++] = ss;
                    oldPos2 = pos2;
                } while ((pos2 = s.indexOf("#", pos2 + 1)) != -1);
                hostConfigList.add(new HostConfig(h[0], h[1], Integer.parseInt(h[2]), h[3], h[4]));
                oldPos1 = pos1;
            } while ((pos1 = toParse.indexOf("###", pos1 + 1)) != -1);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse host config: " + e);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to parse host config: " + e);
        }
        selectedHostConfig = Utils.convertStringToInt(props.getProperty("selectedHostConfig"));
        if (selectedHostConfig < 0)
            selectedHostConfig = 0;
    } //}}}

    //{{{ saveHostConfigList() method
    /**  Save the host config settings */
    private void saveHostConfigList() {
        try {
            String s = "";
            for (Enumeration e = hostConfigList.elements(); e.hasMoreElements(); ) {
                HostConfig hostConfig = (HostConfig)e.nextElement();
                s +=
                        hostConfig.getID() + "#" +
                        hostConfig.getHost() + "#" +
                        String.valueOf(hostConfig.getPort()) + "#" +
                        hostConfig.getName() + "#" +
                        hostConfig.getPassword() + "#" +
                        (hostConfig.getEncoding() != null ? hostConfig.getEncoding() : "default") + "###";
            }
            props.setProperty("hostConfig", s);
            props.setProperty("selectedHostConfig", String.valueOf(selectedHostConfig));
        } catch (NumberFormatException e) {
            System.err.println("Failed to save host config list: " + e);
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Failed to save host config list: " + e);
        }
    } //}}}

    //}}}

    //{{{ getShowSidebar() method
    /**
     *  Gets the showSidebar attribute of the Settings object
     *
     *@return    The showSidebar value
     */
    public boolean getShowSidebar() {
        return showSidebar;
    } //}}}

    //{{{ setShowSidebar() method
    /**
     *  Sets the showSidebar attribute of the Settings object
     *
     *@param  s  The new showSidebar value
     */
    public void setShowSidebar(boolean s) {
        showSidebar = s;
    } //}}}

    //{{{ getSidebarLayout() method
    /**
     *  Gets the sidebarLayout attribute of the Settings object
     *
     *@return    The sidebarLayout value
     */
    public int getSidebarLayout() {
        return sidebarLayout;
    } //}}}

    //{{{ setSidebarLayout() method
    /**
     *  Sets the sidebarLayout attribute of the Settings object
     *
     *@param  i  The new sidebarLayout value
     */
    public void setSidebarLayout(int i) {
        sidebarLayout = i;
    } //}}}

    //{{{ getShowToolbar() method
    /**
     *  Gets the showToolbar attribute of the Settings object
     *
     *@return    The showToolbar value
     */
    public boolean getShowToolbar() {
        return showToolbar;
    } //}}}

    //{{{ setShowToolbar() method
    /**
     *  Sets the showToolbar attribute of the Settings object
     *
     *@param  s  The new showToolbar value
     */
    public void setShowToolbar(boolean s) {
        showToolbar = s;
    } //}}}

    //{{{ getShowCoords() method
    /**
     *  Gets the showCoords attribute of the Settings object
     *
     *@return    The showCoords value
     */
    public boolean getShowCoords() {
        return showCoords;
    } //}}}

    //{{{ setShowCoords() method
    /**
     *  Sets the showCoords attribute of the Settings object
     *
     *@param  s  The new showCoords value
     */
    public void setShowCoords(boolean s) {
        showCoords = s;
    } //}}}

    //{{{ getShowCursor() method
    /**
     *  Gets the showCursor attribute of the Settings object
     *
     *@return    The showCursor value
     */
    public boolean getShowCursor() {
        return showCursor;
    } //}}}

    //{{{ setShowCursor() method
    /**
     *  Sets the showCursor attribute of the Settings object
     *
     *@param  b  The new showCursor value
     */
    public void setShowCursor(boolean b) {
        showCursor = b;
    } //}}}

    //{{{ getShowSlider() method
    /**
     *  Gets the showSlider attribute of the Settings object
     *
     *@return    The showSlider value
     */
    public boolean getShowSlider() {
        return showSlider;
    } //}}}

    //{{{ setShowSlider() method
    /**
     *  Sets the showSlider attribute of the Settings object
     *
     *@param  b  The new showSlider value
     */
    public void setShowSlider(boolean b) {
        showSlider = b;
    } //}}}

    //{{{ getShowStatusbar() method
    /**
     *  Gets the showStatusbar attribute of the Settings object
     *
     *@return    The showStatusbar value
     */
    public boolean getShowStatusbar() {
        return showStatusbar;
    } //}}}

    //{{{ setShowStatusbar() method
    /**
     *  Sets the showStatusbar attribute of the Settings object
     *
     *@param  b  The new showStatusbar value
     */
    public void setShowStatusbar(boolean b) {
        showStatusbar = b;
    } //}}}

    //{{{ getShowVariationGhosts() method
    /**
     *  Gets the showVariationGhosts attribute of the Settings object
     *
     *@return    The showVariationGhosts value
     */
    public boolean getShowVariationGhosts() {
        return showVariationGhosts;
    } //}}}

    //{{{ setShowVariationGhosts() method
    /**
     *  Sets the showVariationGhosts attribute of the Settings object
     *
     *@param  s  The new showVariationGhosts value
     */
    public void setShowVariationGhosts(boolean s) {
        showVariationGhosts = s;
    } //}}}

    //{{{ getShowHorizontalComment() method
    /**
     *  Gets the showHorizontalComment attribute of the Settings object
     *
     *@return    The showHorizontalComment value
     */
    public boolean getShowHorizontalComment() {
        return showHorizontalComment;
    } //}}}

    //{{{ setShowHorizontalComment() method
    /**
     *  Sets the showHorizontalComment attribute of the Settings object
     *
     *@param  s  The new showHorizontalComment value
     */
    public void setShowHorizontalComment(boolean s) {
        showHorizontalComment = s;
    } //}}}

    //{{{ getLocale() method
    /**
     *  Gets the locale attribute of the Settings object
     *
     *@return    The locale value
     */
    public int getLocale() {
        return locale;
    } //}}}

    //{{{ setLocale() method
    /**
     *  Sets the locale attribute of the Settings object
     *
     *@param  i  The new locale value
     */
    public void setLocale(int i) {
        locale = i;
    } //}}}

    //{{{ getFixWindowsFont() method
    /**
     *  Gets the fixWindowsFont attribute of the Settings object
     *
     *@return    The fixWindowsFont value
     */
    public boolean getFixWindowsFont() {
        return fixWindowsFont;
    } //}}}

    //{{{ setFixWindowsFont() method
    /**
     *  Sets the fixWindowsFont attribute of the Settings object
     *
     *@param  b  The new fixWindowsFont value
     */
    public void setFixWindowsFont(boolean b) {
        fixWindowsFont = b;
    } //}}}

    //{{{ getLookAndFeel() method
    /**
     *  Gets the lookAndFeel attribute of the Settings object
     *
     *@return    The lookAndFeel value
     */
    public int getLookAndFeel() {
        return lookAndFeel;
    } //}}}

    //{{{ setLookAndFeel() method
    /**
     *  Sets the lookAndFeel attribute of the Settings object
     *
     *@param  l  The new lookAndFeel value
     */
    public void setLookAndFeel(int l) {
        lookAndFeel = l;
    } //}}}

    //{{{ getThemePack() method
    /**
     *  Gets the themePack attribute of the Settings object
     *
     *@return    The themePack value
     */
    public String getThemePack() {
        return themePack;
    } //}}}

    //{{{ setThemePack() method
    /**
     *  Sets the themePack attribute of the Settings object
     *
     *@param  t  The new themePack value
     */
    public void setThemePack(String t) {
        themePack = t;
    } //}}}

    //{{{ getPlayClickSound() method
    /**
     *  Gets the playClickSound attribute of the Settings object
     *
     *@return    The playClickSound value
     */
    public boolean getPlayClickSound() {
        return playClickSound;
    } //}}}

    //{{{ setPlayClickSound() method
    /**
     *  Sets the playClickSound attribute of the Settings object
     *
     *@param  s  The new playClickSound value
     */
    public void setPlayClickSound(boolean s) {
        playClickSound = s;
    } //}}}

    //{{{ getPlayClockSound() method
    /**
     *  Gets the playClockSound attribute of the Settings object
     *
     *@return    The playClockSound value
     */
    public int getPlayClockSound() {
        return playClockSound;
    } //}}}

    //{{{ setPlayClockSound() method
    /**
     *  Sets the playClockSound attribute of the Settings object
     *
     *@param  i  The new playClockSound value
     */
    public void setPlayClockSound(int i) {
        playClockSound = i;
    } //}}}

    //{{{ getTimeWarningPeriod() method
    /**
     *  Gets the timeWarningPeriod attribute of the Settings object
     *
     *@return    The timeWarningPeriod value
     */
    public int getTimeWarningPeriod() {
        return timeWarningPeriod;
    } //}}}

    //{{{ setTimeWarningPeriod() method
    /**
     *  Sets the timeWarningPeriod attribute of the Settings object
     *
     *@param  i  The new timeWarningPeriod value
     */
    public void setTimeWarningPeriod(int i) {
        timeWarningPeriod = i;
    } //}}}

    //{{{ getDoRemDir() method
    /**
     *  Gets the doRemDir attribute of the Settings object
     *
     *@return    The doRemDir value
     */
    public boolean getDoRemDir() {
        return doRemDir;
    } //}}}

    //{{{ setDoRemDir() method
    /**
     *  Sets the doRemDir attribute of the Settings object
     *
     *@param  b  The new doRemDir value
     */
    public void setDoRemDir(boolean b) {
        doRemDir = b;
    } //}}}

    //{{{ getRemDir() method
    /**
     *  Gets the remDir attribute of the Settings object
     *
     *@return    The remDir value
     */
    public String getRemDir() {
        if (doRemDir)
            return remDir;
        return "";
    } //}}}

    //{{{ setRemDir() method
    /**
     *  Sets the remDir attribute of the Settings object
     *
     *@param  remDir  The new remDir value
     */
    public void setRemDir(String remDir) {
        if (doRemDir && remDir != null && remDir.length() > 0) {
            this.remDir = remDir;
            saveSettings();
        }
    } //}}}

    //{{{ getGnugoPath() method
    /**
     *  Gets the gnugoPath attribute of the Settings object
     *
     *@return    The gnugoPath value
     */
    public String getGnugoPath() {
        return gnugoPath;
    } //}}}

    //{{{ setGnugoPath() method
    /**
     *  Sets the gnugoPath attribute of the Settings object
     *
     *@param  path  The new gnugoPath value
     */
    public void setGnugoPath(String path) {
        gnugoPath = path;
    } //}}}

    //{{{ getGnugoArgs() method
    /**
     *  Gets the gnugoArgs attribute of the Settings object
     *
     *@return    The gnugoArgs value
     */
    public String getGnugoArgs() {
        return gnugoArgs;
    } //}}}

    //{{{ setGnugoArgs() method
    /**
     *  Sets the gnugoArgs attribute of the Settings object
     *
     *@param  args  The new gnugoArgs value
     */
    public void setGnugoArgs(String args) {
        gnugoArgs = args;
    } //}}}

    //{{{ getGTPConfig() method
    /**
     *  Gets the gTPConfig attribute of the Settings object
     *
     *@return    The gTPConfig value
     */
    public GTPConfig getGTPConfig() {
        return gtpConfig;
    } //}}}

    //{{{ setGTPConfig() method
    /**
     *  Sets the gTPConfig attribute of the Settings object
     *
     *@param  gtpConfig  The new gTPConfig value
     */
    public void setGTPConfig(GTPConfig gtpConfig) {
        this.gtpConfig = gtpConfig;
    } //}}}

    //{{{ getServerDisabled() method
    /**
     *  Gets the serverDisabled attribute of the Settings object
     *
     *@return    The serverDisabled value
     */
    public boolean getServerDisabled() {
        return serverDisabled;
    } //}}}

    //{{{ setServerDisabled() method
    /**
     *  Sets the serverDisabled attribute of the Settings object
     *
     *@param  disabled  The new serverDisabled value
     */
    public void setServerDisabled(boolean disabled) {
        serverDisabled = disabled;
    } //}}}

    //{{{ getServerPort() method
    /**
     *  Gets the serverPort attribute of the Settings object
     *
     *@return    The serverPort value
     */
    public int getServerPort() {
        return serverPort;
    } //}}}

    //{{{ setServerPort() method
    /**
     *  Sets the serverPort attribute of the Settings object
     *
     *@param  port  The new serverPort value
     */
    public void setServerPort(int port) {
        serverPort = port;
    } //}}}

    //{{{ getIGSshowShouts() method
    /**
     *  Gets the iGSshowShouts attribute of the Settings object
     *
     *@return    The iGSshowShouts value
     */
    public boolean getIGSshowShouts() {
        return iGSshowShouts;
    } //}}}

    //{{{ setIGSshowShouts() method
    /**
     *  Sets the iGSshowShouts attribute of the Settings object
     *
     *@param  b  The new iGSshowShouts value
     */
    public void setIGSshowShouts(boolean b) {
        iGSshowShouts = b;
    } //}}}

    //{{{ getIGSMatchMainTime() method
    /**
     *  Gets the iGSMatchMainTime attribute of the Settings object
     *
     *@return    The iGSMatchMainTime value
     */
    public int getIGSMatchMainTime() {
        return iGSMatchMainTime;
    } //}}}

    //{{{ setIGSMatchMainTime() method
    /**
     *  Sets the iGSMatchMainTime attribute of the Settings object
     *
     *@param  t  The new iGSMatchMainTime value
     */
    public void setIGSMatchMainTime(int t) {
        iGSMatchMainTime = t;
    } //}}}

    //{{{ getIGSMatchByoyomiTime() method
    /**
     *  Gets the iGSMatchByoyomiTime attribute of the Settings object
     *
     *@return    The iGSMatchByoyomiTime value
     */
    public int getIGSMatchByoyomiTime() {
        return iGSMatchByoyomiTime;
    } //}}}

    //{{{ setIGSMatchByoyomiTime() method
    /**
     *  Sets the iGSMatchByoyomiTime attribute of the Settings object
     *
     *@param  t  The new iGSMatchByoyomiTime value
     */
    public void setIGSMatchByoyomiTime(int t) {
        iGSMatchByoyomiTime = t;
    } //}}}

    //{{{ getAntiSlip() method
    /**
     *  Gets the antiSlip attribute of the Settings object
     *
     *@return    The antiSlip value
     */
    public boolean getAntiSlip() {
        return antiSlip;
    } //}}}

    //{{{ setAntiSlip() method
    /**
     *  Sets the antiSlip attribute of the Settings object
     *
     *@param  b  The new antiSlip value
     */
    public void setAntiSlip(boolean b) {
        antiSlip = b;
    } //}}}

    //{{{ getAntiSlipDelay() method
    /**
     *  Gets the antiSlipDelay attribute of the Settings object
     *
     *@return    The antiSlipDelay value
     */
    public int getAntiSlipDelay() {
        return antiSlipDelay;
    } //}}}

    //{{{ setAntiSlipDelay() method
    /**
     *  Sets the antiSlipDelay attribute of the Settings object
     *
     *@param  i  The new antiSlipDelay value
     */
    public void setAntiSlipDelay(int i) {
        antiSlipDelay = i;
    } //}}}

    //{{{ getClickType() method
    /**
     *  Gets the clickType attribute of the Settings object
     *
     *@return    The clickType value
     */
    public int getClickType() {
        return clickType;
    } //}}}

    //{{{ setClickType() method
    /**
     *  Sets the clickType attribute of the Settings object
     *
     *@param  t  The new clickType value
     */
    public void setClickType(int t) {
        clickType = t;
    } //}}}

    //{{{ getFrameSize() method
    /**
     *  Gets the frameSize attribute of the Settings object
     *
     *@return    The frameSize value
     */
    public Dimension getFrameSize() {
        return frameSize;
    } //}}}

    //{{{ saveFrameSize() method
    /**
     *  Sets the frameSize attribute of the Settings object
     *
     *@param  dim  The new frameSize value
     */
    public void setFrameSize(Dimension dim) {
        frameSize = dim;
        saveSettings();
    } //}}}

    //{{{ getSerifFontSize() method
    /**
     *  Gets the serifFontSize attribute of the Settings object
     *
     *@return    The serifFontSize value
     */
    public int getSerifFontSize() {
        return serifFontSize;
    } //}}}

    //{{{ setSerifFontSize() method
    /**
     *  Sets the serifFontSize attribute of the Settings object
     *
     *@param  s  The new serifFontSize value
     */
    public void setSerifFontSize(int s) {
        serifFontSize = s;
    } //}}}

    //{{{ getSansSerifFontSize() method
    /**
     *  Gets the sansSerifFontSize attribute of the Settings object
     *
     *@return    The sansSerifFontSize value
     */
    public int getSansSerifFontSize() {
        return sansSerifFontSize;
    } //}}}

    //{{{ setSansSerifFontSize() method
    /**
     *  Sets the sansSerifFontSize attribute of the Settings object
     *
     *@param  s  The new sansSerifFontSize value
     */
    public void setSansSerifFontSize(int s) {
        sansSerifFontSize = s;
    } //}}}

    //{{{ getMonospacedFontSize() method
    /**
     *  Gets the monospacedFontSize attribute of the Settings object
     *
     *@return    The monospacedFontSize value
     */
    public int getMonospacedFontSize() {
        return monospacedFontSize;
    } //}}}

    //{{{ setMonospacedFontSize() method
    /**
     *  Sets the monospacedFontSize attribute of the Settings object
     *
     *@param  s  The new monospacedFontSize value
     */
    public void setMonospacedFontSize(int s) {
        monospacedFontSize = s;
    } //}}}

    //{{{ getIGSChatterType() method
    /**
     *  Gets the iGSChatterType attribute of the Settings object
     *
     *@return    The iGSChatterType value
     */
    public int getIGSChatterType() {
        return iGSChatterType;
    } //}}}

    //{{{ setIGSChatterType() method
    /**
     *  Sets the iGSChatterType attribute of the Settings object
     *
     *@param  t  The new iGSChatterType value
     */
    public void setIGSChatterType(int t) {
        iGSChatterType = t;
    } //}}}

    //{{{ getViewComm() method
    /**
     *  Gets the viewComm attribute of the Settings object
     *
     *@return    The viewComm value
     */
    public boolean getViewComm() {
        return viewComm;
    } //}}}

    //{{{ setViewComm() method
    /**
     *  Sets the viewComm attribute of the Settings object
     *
     *@param  b  The new viewComm value
     */
    public void setViewComm(boolean b) {
        viewComm = b;
    } //}}}

    //{{{ getIGSChatterShowToolbar() method
    /**
     *  Gets the iGSChatterShowToolbar attribute of the Settings object
     *
     *@return    The iGSChatterShowToolbar value
     */
    public boolean getIGSChatterShowToolbar() {
        return igsChatterShowToolbar;
    } //}}}

    //{{{ setIGSChatterShowToolbar() method
    /**
     *  Sets the iGSChatterShowToolbar attribute of the Settings object
     *
     *@param  b  The new iGSChatterShowToolbar value
     */
    public void setIGSChatterShowToolbar(boolean b) {
        igsChatterShowToolbar = b;
    } //}}}

    //{{{ getPlayChatSound() method
    /**
     *  Gets the playChatSound attribute of the Settings object
     *
     *@return    The playChatSound value
     */
    public boolean getPlayChatSound() {
        return playChatSound;
    } //}}}

    //{{{ setPlayChatSound() method
    /**
     *  Sets the playChatSound attribute of the Settings object
     *
     *@param  b  The new playChatSound value
     */
    public void setPlayChatSound(boolean b) {
        playChatSound = b;
    } //}}}

    //{{{ getAutoawayTime() method
    /**
     *  Gets the autoawayTime attribute of the Settings object
     *
     *@return    The autoawayTime value
     */
    public int getAutoawayTime() {
        return autoawayTime;
    } //}}}

    //{{{ setAutoawayTime() method
    /**
     *  Sets the autoawayTime attribute of the Settings object
     *
     *@param  t  The new autoawayTime value
     */
    public void setAutoawayTime(int t) {
        autoawayTime = t;
    } //}}}

    //{{{ getAutoawayMessage() method
    /**
     *  Gets the autoawayMessage attribute of the Settings object
     *
     *@return    The autoawayMessage value
     */
    public String getAutoawayMessage() {
        return autoawayMessage;
    } //}}}

    //{{{ setAutoawayMessage() method
    /**
     *  Sets the autoawayMessage attribute of the Settings object
     *
     *@param  msg  The new autoawayMessage value
     */
    public void setAutoawayMessage(String msg) {
        autoawayMessage = msg;
    } //}}}

    //{{{ getSimpleSound() method
    /**
     *  Gets the simpleSound attribute of the Settings object
     *
     *@return    The simpleSound value
     */
    public boolean getSimpleSound() {
        return simpleSound;
    } //}}}

    //{{{ setSimpleSound() method
    /**
     *  Sets the simpleSound attribute of the Settings object
     *
     *@param  b  The new simpleSound value
     */
    public void setSimpleSound(boolean b) {
        simpleSound = b;
    } //}}}

    //{{{ getUpperRank() method
    /**
     *  Gets the upperRank attribute of the Settings object
     *
     *@return    The upperRank value
     */
    public String getUpperRank() {
        return upperRank;
    } //}}}

    //{{{ setUpperRank() method
    /**
     *  Sets the upperRank attribute of the Settings object
     *
     *@param  rank  The new upperRank value
     */
    public void setUpperRank(String rank) {
        upperRank = rank;
    } //}}}

    //{{{ getLowerRank() method
    /**
     *  Gets the lowerRank attribute of the HostConfig object
     *
     *@return    The lowerRank value
     */
    public String getLowerRank() {
        return lowerRank;
    } //}}}

    //{{{ setLowerRank() method
    /**
     *  Sets the lowerRank attribute of the HostConfig object
     *
     *@param  rank  The new lowerRank value
     */
    public void setLowerRank(String rank) {
        lowerRank = rank;
    } //}}}

    //{{{ getIGSDisplayInfo() method
    /**
     *  Gets the iGSDisplayInfo attribute of the Settings object
     *
     *@return    The iGSDisplayInfo value
     */
    public boolean getIGSDisplayInfo() {
        return igsDisplayInfo;
    } //}}}

    //{{{ setIGSDisplayInfo() method
    /**
     *  Sets the iGSDisplayInfo attribute of the Settings object
     *
     *@param  b  The new iGSDisplayInfo value
     */
    public void setIGSDisplayInfo(boolean b) {
        igsDisplayInfo = b;
    } //}}}

    //{{{ getIGSDisplayMoves() method
    /**
     *  Gets the iGSDisplayMoves attribute of the Settings object
     *
     *@return    The iGSDisplayMoves value
     */
    public boolean getIGSDisplayMoves() {
        return igsDisplayMoves;
    } //}}}

    //{{{ setIGSDisplayMoves() method
    /**
     *  Sets the iGSDisplayMoves attribute of the Settings object
     *
     *@param  b  The new iGSDisplayMoves value
     */
    public void setIGSDisplayMoves(boolean b) {
        igsDisplayMoves = b;
    } //}}}

    //{{{ Bozo handling

    //{{{ getFriends() method
    /**
     *  Gets the friends attribute of the Settings object
     *
     *@return    The friends value
     */
    public String getFriends() {
        return friends;
    } //}}}

    //{{{ setFriends() method
    /**
     *  Sets the friends attribute of the Settings object
     *
     *@param  s  The new friends value
     */
    public void setFriends(String s) {
        friends = s;
    } //}}}

    //{{{ getBozos() method
    /**
     *  Gets the bozos attribute of the Settings object
     *
     *@return    The bozos value
     */
    public String getBozos() {
        return bozos;
    } //}}}

    //{{{ setBozos() method
    /**
     *  Sets the bozos attribute of the Settings object
     *
     *@param  s  The new bozos value
     */
    public void setBozos(String s) {
        bozos = s;
    } //}}}

    //{{{ doAutoReply() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public boolean doAutoReply() {
        return doAutoReply;
    } //}}}

    //{{{ setDoAutoReply() method
    /**
     *  Sets the doAutoReply attribute of the Settings object
     *
     *@param  b  The new doAutoReply value
     */
    public void setDoAutoReply(boolean b) {
        doAutoReply = b;
    } //}}}

    //{{{ getAutoReply() method
    /**
     *  Gets the autoReply attribute of the Settings object
     *
     *@return    The autoReply value
     */
    public String getAutoReply() {
        return autoReply;
    } //}}}

    //{{{ setAutoReply
    /**
     *  Sets the autoReply attribute of the Settings object
     *
     *@param  s  The new autoReply value
     */
    public void setAutoReply(String s) {
        autoReply = s;
    } //}}}

    //}}}

    //{{{ IGS Location & Size

    //{{{ getStoredLocation() method
    /**
     *  Gets the storedLocation attribute of the Settings object
     *
     *@param  name  Description of the Parameter
     *@return       The storedLocation value
     */
    public Point getStoredLocation(String name) {
        String s;
        if (storeLocation && (s = props.getProperty("loc_" + name)) != null) {
            try {
                final int x = Integer.parseInt(s.substring(0, s.indexOf("#")));
                final int y = Integer.parseInt(s.substring(s.indexOf("#") + 1, s.length()));
                return new Point(x, y);
            } catch (NumberFormatException e) {
                return null;
            } catch (StringIndexOutOfBoundsException e) {
                return null;
            }
        }
        return null;
    } //}}}

    //{{{ setStoredLocation() method
    /**
     *  Sets the storedLocation attribute of the Settings object
     *
     *@param  name  The new storedLocation value
     *@param  p     The new storedLocation value
     */
    public void setStoredLocation(String name, Point p) {
        props.setProperty("loc_" + name, p.x + "#" + p.y);
    } //}}}

    //{{{ getStoreLocation() method
    /**
     *  Gets the storeLocation attribute of the Settings object
     *
     *@return    The storeLocation value
     */
    public boolean getStoreLocation() {
        return storeLocation;
    } //}}}

    //{{{ setStoreLocation() method
    /**
     *  Sets the storeLocation attribute of the Settings object
     *
     *@param  b  The new storeLocation value
     */
    public void setStoreLocation(boolean b) {
        storeLocation = b;
    } //}}}

    //{{{ getStoredSize() method
    /**
     *  Gets the storedSize attribute of the Settings object
     *
     *@param  name  Description of the Parameter
     *@return       The storedSize value
     */
    public Dimension getStoredSize(String name) {
        String s;
        if (storeSize && (s = props.getProperty("size_" + name)) != null) {
            try {
                final int w = Integer.parseInt(s.substring(0, s.indexOf("#")));
                final int h = Integer.parseInt(s.substring(s.indexOf("#") + 1, s.length()));
                return new Dimension(w, h);
            } catch (NumberFormatException e) {
                return null;
            } catch (StringIndexOutOfBoundsException e) {
                return null;
            }
        }
        return null;
    } //}}}

    //{{{ setStoredSize() method
    /**
     *  Sets the storedSize attribute of the Settings object
     *
     *@param  name  The new storedSize value
     *@param  size  The new storedSize value
     */
    public void setStoredSize(String name, Dimension size) {
        props.setProperty("size_" + name, size.width + "#" + size.height);
    } //}}}

    //{{{ getStoreSize() method
    /**
     *  Gets the storeSize attribute of the Settings object
     *
     *@return    The storeSize value
     */
    public boolean getStoreSize() {
        return storeSize;
    } //}}}

    //{{{ setStoreSize() method
    /**
     *  Sets the storeSize attribute of the Settings object
     *
     *@param  b  The new storeSize value
     */
    public void setStoreSize(boolean b) {
        storeSize = b;
    } //}}}

    //}}}
}

