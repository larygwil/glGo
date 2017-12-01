/*
 *  GTPDefines.java
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
package ggo.gtp;

/**
 *  Defines for GTP code
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
interface GTPDefines {
    /**  State: Undefined */
    final static int STATE_UNKNOWN = 0;
    /**  State: White to play */
    final static int STATE_MOVE_WHITE = 1;
    /**  State: Black to play */
    final static int STATE_MOVE_BLACK = 2;
    /**  State: Game ended, scoring */
    final static int STATE_SCORING = 3;
    /** State: Game finished */
    final static int STATE_DONE = 4;
    /** State: Setup handicap */
    final static int STATE_SETUP_HANDICAP = 5;
    /** State: Resume game, wait for color to play */
    final static int STATE_RESUME_GAME = 6;
}

