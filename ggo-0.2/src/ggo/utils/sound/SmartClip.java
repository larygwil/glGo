/*
 *  SmartClip.java
 *
 *  Copyright © 2002 William Shubert.
 *
 *  Used and slightly modified by Peter Strempel for gGo with William Shuberts permission.
 */
package ggo.utils.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;

/**
 * A "smart" audio clip that knows how to play itself and will turn off the
 * audio line when it is done.
 *
 *@author     William Shubert
 *@version    $Revision: 1.1.1.1 $, $Date: 2002/07/29 03:24:24 $
 */
public class SmartClip implements Runnable, SoundClip {

    private byte[] audioData;
    private boolean loopMe;
    private Thread myThread;

    /**
     * Create an audio clip. Creating the audio clip does not invoke any sound
     * code.
     *
     *@param  resourceName  The name of the audio clip resource. This name is
     *   relative to the jar file or class path that holds this
     *   <code>SmartClip</code> class.
     */
    public SmartClip(String resourceName) {
        ByteArrayOutputStream tempOut = new ByteArrayOutputStream();
        InputStream in = SmartClip.class.getResourceAsStream(resourceName);
        byte[] buf = new byte[1024];
        try {
            for (; ; ) {
                int lenRead = in.read(buf);
                if (lenRead == -1) {
                    break;
                }
                tempOut.write(buf, 0, lenRead);
            }
        } catch (NullPointerException excep) {
            System.err.println("Error loading sound clip: " + excep);
        } catch (IOException excep) {
            System.err.println("Error loading sound clip: " + excep);
        }
        audioData = tempOut.toByteArray();
    }

    /**
     * Play the audio clip. An audio clip will be created and destroyed here. If
     * this audio clip had earlier errors, then nothing will happen.
     */
    public void play() {
        loopMe = false;
        if (audioData != null) {
            new Thread(this).start();
        }
    }

    /**
     * Play the audio clip in a loop. An audio clip will be created and destroyed
     * here. If this audio clip had earlier errors, then nothing will happen.
     */
    public void loop() {
        loopMe = true;
        if (audioData != null) {
            myThread = new Thread(this);
            myThread.start();
        }
    }

    /**  Stop the audio clip when playing in a loop. */
    public void stop() {
        if (!isLooping())
            return;
        loopMe = false;
        try {
            myThread.interrupt();
        } catch (NullPointerException e) {}
    }

    /**
     *  Check if this audio clip is currently playing in a loop.
     *
     *@return    True if the clip i looping, else false
     */
    public boolean isLooping() {
        try {
            return loopMe && myThread.isAlive();
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Actually play the clip. You shouldn't call this directly; instead call the
     * <code>play()</code> function which creates a new thread and uses that to
     * call this function. This function will not return until the clip has
     * finished playing.
     *
     * <p>The function will actually sleep for a full second after the clip
     * finishes to prevent too much unnecessary opening and closing of the
     * audio device. I'm not sure if this is necessary, but it probably won't
     * hurt.
     */
    public void run() {
        Clip clip = null;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream
                    (new ByteArrayInputStream(audioData));
            clip = (Clip)AudioSystem.getLine(
                    new DataLine.Info(Clip.class, stream.getFormat()));
            clip.open(stream);
            long len = clip.getMicrosecondLength();
            clip.loop(loopMe ? Clip.LOOP_CONTINUOUSLY : 0);
            do {
                Thread.currentThread().sleep(len == AudioSystem.NOT_SPECIFIED ?
                        2000 :
                        (loopMe ? len : (len + 1001000) / 1000));
            } while (loopMe && !Thread.interrupted());
            clip.close();
        } catch (InterruptedException excep) {
            loopMe = false;
            if (clip != null)
                clip.close();
        } catch (Exception excep) {
            System.err.println("Error playing sound clip: " + excep);
            // audioData = null;
        }
    }
}

