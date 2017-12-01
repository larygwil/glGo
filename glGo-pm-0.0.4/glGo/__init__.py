#
# glGo python package
#
# $Id: __init__.py,v 1.8 2003/11/30 22:34:11 peter Exp $
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


packagename = "glGo"
version     = "0.0.4"

PLAYER_STATUS_NEUTRAL = 0
PLAYER_STATUS_FRIEND  = 1
PLAYER_STATUS_BOZO    = 2

DEFAULT_DB_FILE = 'players.db'

NUMBER_CUSTOM_FLAGS = 5

debug_level = 0
info_level = 0

# Use resource.py file or .xrc files?
# True for release, False for development. The resource.py file is
# created using wxrc utility (see wxWindows/contrib)
USE_RESOURCE = True
