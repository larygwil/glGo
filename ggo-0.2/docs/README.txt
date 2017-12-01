gGo

Requirements
------------

gGo requires JRE 1.3 or later.
Java 1.4 is strongly recommanded, as some features will be missing with 1.3
You can download it at http://java.sun.com

Running
-------

Linux: Either run the lib/gGo.jar file with: 'java -jar gGo.jar' or execute the bin/ggo script.
You might want to copy it to /usr/local/bin or /home/<user>/bin so your bash knows this command.

Windows: Use the gGo.exe starter, this will look up the path to your Java installation in the
registry and start the .jar file. The ggo.bat script does the same.
If anything goes wrong with the gGo.exe starter, have a look into C:\ggostarter.log.
As alternative you can doubleclick on the lib/gGo.jar file if your Windows knows to associate .jar
files with java. Or open a DOS console, change to the lib/ directory and type 'java -jar gGo.jar'.

The easiest way to install gGo is using Java Web Start. Please check out the gGo webpage at
http://ggo.sourceforge.net

If you upgrade to a newer version and gGo crashes on startup, delete the .ggorc file and try again.

Skins
-----

gGo can be skinned. It uses the Skin Look And Feel (http://www.l2fprod.com/) which allows
a Java application to be skinnable with KDE and Gtk themes. A couple of prepared themepacks are
available at http://javootoo.l2fprod.com/plaf/skinlf/index.php
The gGo installation includes the default themepack. To get more skins please visit javootoo or use
your KDE and Gtk skins. For further information please check the l2fprod webpage.
gGo will run and switch back to the default look and feel if no skins are found.

Compiling
---------

If you want to compile the sources yourself, you can use ant. Simple go to the sources
directory and run 'ant'. The resulting gGo.jar will be in the dist subdirectory.
Before compilation you must copy or link the lib/ directory into the sources base directory,
as the jar extensions are required for compilation.

Third-party software
--------------------
gGo uses third-party software. Please refer to the file AUTHORS.txt within this package for
further informations.
This product includes software developed by L2FProd.com (http://www.L2FProd.com/).


Copyight (c) 2002 by Peter Strempel <pstrempel@t-online.de>
