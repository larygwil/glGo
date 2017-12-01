#! /usr/bin/python

#
# setup.py
#
# $Id: setup.py,v 1.6 2003/11/15 10:13:37 peter Exp $
#

from distutils.core import setup
import glGo.__init__
# import py2exe

setup(name="glGo-pm",
      version=glGo.__init__.version,
      description="glGo playermanager.",
      long_description="A standalone GUI and commandline interface for the glGo player database.",
      author="Peter Strempel",
      author_email="pstrempel@users.sourceforge.net",
      url="http://ggo.sourceforge.net",
      license="GPL",
      packages = [ 'glGo' ],
      scripts = [ 'players.py', 'players_gui.py' ]
      )
