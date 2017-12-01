/*
 *  SoundHandler.java
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

import ggo.gGo;
import ggo.gui.Clock;
import ggo.utils.sound.SmartClip;

/**
 *  Wrapper class for sound handling. Plays the click sound when placing a
 *  stone. Stone sound kindly made available from CGoban2 by William Shubert.
 *  Sound can be using either the SmartClip or SimpleClip class.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:56 $
 */
public final class SoundHandler {
    //{{{ private members
    private static SoundClip playSound, passSound, incomingChatSound, timeSound, matchRequestSound;
    //}}}

    //{{{ static constructor
    static {
        if (gGo.getSettings().getSimpleSound()) {
            playSound = new SimpleClip("/sounds/stone.wav");
            incomingChatSound = new SimpleClip("/sounds/chatIn.wav");
            passSound = new SimpleClip("/sounds/pass.wav");
            timeSound = new SimpleClip("/sounds/tictoc.wav");
            matchRequestSound = new SimpleClip("/sounds/matchrequest.wav");
        }
        else {
            playSound = new SmartClip("/sounds/stone.wav");
            incomingChatSound = new SmartClip("/sounds/chatIn.wav");
            passSound = new SmartClip("/sounds/pass.wav");
            timeSound = new SmartClip("/sounds/tictoc.wav");
            matchRequestSound = new SmartClip("/sounds/matchrequest.wav");
        }
    } //}}}

    //{{{ foo() method
    /**  Dummy method to trigger the static constructor to preload sounds. */
    public static void foo() { } //}}}

    //{{{ playClick() method
    /**  Play the sound when a stone is placed on the board */
    public static void playClick() {
        try {
            if (gGo.getSettings().getPlayClickSound())
                playSound.play();
        } catch (NullPointerException e) {
            System.err.println("Failed to play sound: " + e);
        }
    } //}}}

    //{{{ playPass() method
    /**  Play the sound when a turn was passed */
    public static void playPass() {
        try {
            if (gGo.getSettings().getPlayClickSound())
                passSound.play();
        } catch (NullPointerException e) {
            System.err.println("Failed to play sound: " + e);
        }
    } //}}}

    //{{{ playIncomingChat() method
    /**  Playing incoming chat sound */
    public static void playIncomingChat() {
        try {
            incomingChatSound.play();
        } catch (NullPointerException e) {
            System.err.println("Failed to play sound: " + e);
        }
    } //}}}

    public static void playMatchRequestSound() {
        try {
            matchRequestSound.play();
        } catch (NullPointerException e) {
            System.err.println("Failed to play sound: " + e);
        }
    }

    //{{{ startTimeWarning() method
    /**  Playing time warning sound */
    public static void startTimeWarning() {
        try {
            if (gGo.getSettings().getPlayClockSound() == Clock.WARN_SOUND_LOOP)
                timeSound.loop();
            else if (gGo.getSettings().getPlayClockSound() == Clock.WARN_SOUND_ONCE)
                timeSound.play();
        } catch (NullPointerException e) {
            System.err.println("Failed to play sound: " + e);
        }
    } //}}}

    //{{{ stopTimeWarning() method
    /**  Playing time warning sound */
    public static void stopTimeWarning() {
        try {
            if (timeSound.isLooping())
                timeSound.stop();
        } catch (NullPointerException e) {}
    } //}}}
}

