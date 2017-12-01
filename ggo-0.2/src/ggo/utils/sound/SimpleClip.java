/*
 *  SimpleClip.java
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
package ggo.utils.sound;

import java.applet.AudioClip;
import java.applet.Applet;
import java.net.URL;

/**
 *  This class wraps a java.applet.AudoClip to use the simple sound system
 *  which has less problems than the new SmartClip class.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:56 $
 */
public class SimpleClip implements SoundClip {
    private AudioClip clip;
    private boolean isLooping;

    /**
     *Constructor for the SimpleClip object
     *
     *@param  name  Name of the resource file to load
     */
    public SimpleClip(String name) {
        isLooping = false;

        URL url = SoundHandler.class.getResource(name);

        if (url == null) {
            System.err.println("Soundfile '" + name + "' not found.");
            clip = null;
        }
        else
            clip = Applet.newAudioClip(url);
    }

    /**  Play the sound clip */
    public void play() {
        clip.play();
        isLooping = false;
    }

    /**  Stop playing */
    public void stop() {
        clip.stop();
        isLooping = false;
    }

    /**  Loop the sound clip */
    public void loop() {
        clip.loop();
        isLooping = true;
    }

    /**
     *  Check if the clip is currently looping
     *
     *@return    True if looping, else false
     */
    public boolean isLooping() {
        return isLooping;
    }
}

