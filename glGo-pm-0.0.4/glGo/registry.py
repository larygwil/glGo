#
# registry.py
#
# $Id: registry.py,v 1.1 2003/11/22 11:47:18 peter Exp $
#
# glGo, a prototype for a 3D Goban based on wxWindows, OpenGL and SDL.
# Copyright (c) 2003, Peter Strempel <pstrempel@gmx.de>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

import os

#
# This reads the registry entry HKCU\Software\glGo\Location which
# contains the glGo install folder.
# Obviously, for Win32 only.
#
def get_glGo_install_path():
    if os.name != 'nt':
        return None

    import _winreg

    try:
        handle = _winreg.OpenKey(_winreg.HKEY_CURRENT_USER, "Software\\glGo")
        path, type = _winreg.QueryValueEx(handle, "Location")
        _winreg.CloseKey(handle)
    except:
        path = None
    return path

#
# Debug stuff
#
## if __name__ == '__main__':
##     print get_glGo_install_path()
