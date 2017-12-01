/*
 *  DynamicFileFilter.java
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

import javax.swing.filechooser.FileFilter;
import java.text.MessageFormat;
import java.io.File;
import ggo.utils.*;
import ggo.gGo;

/**
 *  Abstract FileFilter subclass for the various file formats used in gGo.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1 $, $Date: 2002/10/08 14:47:03 $
 */
public abstract class DynamicFileFilter extends FileFilter {

    /**
     *  Get list of possible extensions, like sgf, ugf, ugi, xml etc.
     *
     *@return    The extensions array
     */
    protected abstract String[] getExtensions();

    /**
     *  Gets the description of the file format, like SGF, XML, UGF etc.
     *
     *@return    The format description
     */
    protected abstract String getFormatDescriptor();

    /**
     *  checks if a file is accepted by the filter
     *
     *@param  f  File to check
     *@return    True if accepted, else false
     */
    public boolean accept(File f) {
        if (f.isDirectory())
            return true;

        String extension = Utils.getExtension(f);
        String[] extensions = getExtensions();
        if (extension != null)
            for (int i = 0, sz = extensions.length; i < sz; i++)
                if (extensions[i].equals(extension))
                    return true;
        return false;
    }

    /**
     *  Gets the description of this file filter
     *
     *@return    Description string
     */
    public String getDescription() {
        return MessageFormat.format(
                gGo.getSGFResources().getString("file_descriptor"),
                new Object[]{getFormatDescriptor()});
    }
}

