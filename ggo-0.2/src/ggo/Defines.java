/*
 *  Defines.java
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

import java.util.Locale;

/**
 *  Global defines and constants
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.14 $, $Date: 2002/10/27 08:50:12 $
 */
public interface Defines {
    /**  Package name */
    public final static String PACKAGE = "gGo";
    /**  Package version */
    public final static String VERSION = "0.2";
    /** Supported locales */
    public final static Locale[] supportedLocales = {
            Locale.US,
            Locale.GERMANY,
            Locale.FRANCE,
            new Locale("es", "ES")
            };
    /** Path to JavaHelp */
    public final static String helpsetName = "jhelpset";
    /**  No stone */
    public final static int STONE_NONE = 0;
    /**  White stone */
    public final static int STONE_WHITE = 1;
    /**  Black stone */
    public final static int STONE_BLACK = 2;
    /**  Erased stone */
    public final static int STONE_ERASE = 3;
    /**  Normal play mode */
    public final static int MODE_NORMAL = 0;
    /**  Edit mode */
    public final static int MODE_EDIT = 1;
    /**  Score mode */
    public final static int MODE_SCORE = 2;
    /**  System default look and feel */
    public final static int LOOKANDFEEL_SYSTEM = 0;
    /**  Modified Java metal look and feel */
    public final static int LOOKANDFEEL_JAVA = 1;
    /**  Windows look and feel */
    public final static int LOOKANDFEEL_WINDOWS = 2;
    /**  Skin look and feel */
    public final static int LOOKANDFEEL_SKIN = 3;
    /**  Kunststoff look and feel */
    public final static int LOOKANDFEEL_KUNSTSTOFF = 4;
    /**  Metouia look and feel */
    public final static int LOOKANDFEEL_METOUIA = 5;
    /**  Motif look and feel */
    public final static int LOOKANDFEEL_MOTIF = 6;
    /**  Macintosh look and feel */
    public final static int LOOKANDFEEL_MAC = 7;
    /** No mark */
    public final static int MARK_NONE = 0;
    /**  Mark type square */
    public final static int MARK_SQUARE = 1;
    /**  Mark type circle */
    public final static int MARK_CIRCLE = 2;
    /**  Mark type triangle */
    public final static int MARK_TRIANGLE = 3;
    /**  Mark type cross */
    public final static int MARK_CROSS = 4;
    /**  Mark type text */
    public final static int MARK_TEXT = 5;
    /**  Mark type number */
    public final static int MARK_NUMBER = 6;
    /**  Mark type territory black */
    public final static int MARK_TERR_BLACK = 7;
    /**  Mark type territory white */
    public final static int MARK_TERR_WHITE = 8;
    /**  No mark type, but used for the GUI */
    public final static int MARK_STONE = 9;
    /**  Play mode: editing */
    public final static int PLAY_MODE_EDIT = 0;
    /**  Play mode: GTP playing */
    public final static int PLAY_MODE_GTP = 1;
    /**  Play mode: IGS playing */
    public final static int PLAY_MODE_IGS_PLAY = 2;
    /**  Play mode: IGS observing */
    public final static int PLAY_MODE_IGS_OBSERVE = 3;
    /**  GTP player is human */
    public final static int GTP_HUMAN = 0;
    /**  GTP player is computer */
    public final static int GTP_COMPUTER = 1;
    /**  Default horizontal board size */
    public final static int BOARD_DEFAULT_X = 500;
    /**  Default vertical board size */
    public final static int BOARD_DEFAULT_Y = 500;
    /**  Use single clicks to play a stone */
    public final static int CLICK_TYPE_SINGLECLICK = 0;
    /**  Use double clicks to play a stone */
    public final static int CLICK_TYPE_DOUBLECLICK = 1;
    /**  Sidebar layout east */
    public final static int SIDEBAR_EAST = 0;
    /**  Sidebar layout west */
    public final static int SIDEBAR_WEST = 1;
    /** gGo webpage URL */
    public final static String GGO_URL = "http://ggo.sourceforge.net";
    /** Debug output for IGS moves */
    public final static boolean moveDebug = false;
}

