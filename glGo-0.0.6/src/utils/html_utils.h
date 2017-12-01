/*
 * html_utils.h
 *
 * $Id: html_utils.h,v 1.4 2003/10/06 20:10:54 peter Exp $
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

/** @addtogroup utils
 * @{ */

/**
 * @file
 * This file contains some global utilities code for HTML
 * connections and opening URLs in the default browser.
 */

#ifndef HTML_UTILS_H
#define HTML_UTILS_H

/** URL of glGo webpage. */
extern const wxString glGoURL ;

/**
 * Check the glGo webpage if there is an update available.
 * This function tries to read the file from the URL
 * http://ggo.sourceforge.net/version.html
 * The file contains only one line with the latest release
 * version number.
 * If the connection fails, this function exits silently.
 * Otherwise a dialog is shown with information if an
 * update is available.
 * @param parent Parent window calling this function
 */
void checkUpdate(wxWindow *parent);

/**
 * Open an URL in the default browser.
 * @param url URL to open
 * @author Julian Smart
 */
void ViewHTMLFile(const wxString& url);

/* @} */

#endif
