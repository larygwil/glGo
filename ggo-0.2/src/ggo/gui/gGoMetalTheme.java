/*
 *  gGoMetalTheme.java
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
package ggo.gui;

import javax.swing.plaf.metal.*;
import javax.swing.plaf.*;
import ggo.Defines;

/**
 *  Slightly adjusted theme for Metal look and feel. Some adjustments to fonts
 *  using less of the bold fonts.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class gGoMetalTheme extends DefaultMetalTheme implements Defines {
    private FontUIResource menuTextFont, controlTextFont;

    /**  Constructor for the gGoMetalTheme object */
    public gGoMetalTheme() {
        menuTextFont = new FontUIResource(super.getMenuTextFont().getName(),
                FontUIResource.PLAIN,
                super.getMenuTextFont().getSize());
        controlTextFont = new FontUIResource(super.getControlTextFont().getName(),
                FontUIResource.PLAIN,
                super.getControlTextFont().getSize());
    }

    /**
     *  Gets the name attribute of the gGoMetalTheme object
     *
     *@return    The name value
     */
    public String getName() {
        return "gGo";
    }

    /**
     *  Gets the menuTextFont attribute of the gGoMetalTheme object
     *
     *@return    The menuTextFont value
     */
    public FontUIResource getMenuTextFont() {
        return menuTextFont;
    }

    /**
     *  Gets the systemTextFont attribute of the gGoMetalTheme object
     *
     *@return    The systemTextFont value
     */
    public FontUIResource getControlTextFont() {
        return controlTextFont;
    }
}

