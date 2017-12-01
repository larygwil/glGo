/*
 *  Clock.java
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import ggo.*;
import ggo.utils.sound.SoundHandler;
import ggo.utils.Utils;
import ggo.igs.IGSTime;

/**
 *  Clock label, displayed in the sidebar. Right now this clock only supports IGS style time systems.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class Clock extends JLabel implements Defines {
    //{{{ public members
    /**  Never play sound */
    public final static int WARN_SOUND_NEVER = 0;
    /**  Play warning sound once */
    public final static int WARN_SOUND_ONCE = 1;
    /**  Play warning sound in loop */
    public final static int WARN_SOUND_LOOP = 2;
    //}}}

    //{{{ private members
    private Timer timer;
    private int absoluteTime, currentAbsoluteTime;
    private int stonesPerPeriod;
    private int stonesLeftInPeriod;
    private int periodTime, currentPeriodTime;
    private boolean isIGSClock, alternateColor, blinked, isMyClock;
    private Color backgroundColor, foregroundColor;
    //}}}

    //{{{ Clock() constructor
    /**Constructor for the Clock object */
    public Clock() {
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)));

        isMyClock = blinked = alternateColor = isIGSClock = false;
        backgroundColor = getBackground();
        foregroundColor = getForeground();
        init(0, 0, -1);

        timer = new Timer(1000,
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (currentAbsoluteTime == 0)
                        currentPeriodTime--;
                    else
                        currentAbsoluteTime--;

                    // Needed for IGS clocks
                    if (isIGSClock && stonesLeftInPeriod == -1 && stonesPerPeriod != -1)
                        stonesLeftInPeriod = stonesPerPeriod;

                    updateClock();
                }
            });
    } //}}}

    //{{{ Clock(int, int, int) constructor
    /**
     *Constructor for the Clock object
     *
     *@param  absoluteTime     Main time
     *@param  periodTime       Time for one byo-yomi period
     *@param  stonesPerPeriod  Stones per byo-yomi period
     */
    public Clock(int absoluteTime, int periodTime, int stonesPerPeriod) {
        this();
        init(absoluteTime, periodTime, stonesPerPeriod);
    } //}}}

    //{{{ isIGSClock() method
    /**
     *  Gets the iGSClock attribute of the Clock object
     *
     *@return    The iGSClock value
     */
    public boolean isIGSClock() {
        return isIGSClock;
    } //}}}

    //{{{ init(int, int, int) method
    /**
     *  Init the clock with starting values
     *
     *@param  absoluteTime     Main time
     *@param  periodTime       Time for one byo-yomi period
     *@param  stonesPerPeriod  Stones per byo-yomi period
     */
    public void init(int absoluteTime, int periodTime, int stonesPerPeriod) {
        this.currentAbsoluteTime = this.absoluteTime = absoluteTime;
        this.currentPeriodTime = this.periodTime = periodTime;
        this.stonesLeftInPeriod = this.stonesPerPeriod = stonesPerPeriod;
        blinked = alternateColor = false;
        updateClock();
    } //}}}

    //{{{ init(IGSTime) method
    /**
     *  Init the clock with starting values. This method is used for IGS games
     *
     *@param  time  IGSTime object with the time value.
     */
    public void init(IGSTime time) {
        isIGSClock = true;
        currentAbsoluteTime = absoluteTime = time.getTime();
        if (time.getTime() == 0 && time.getStones() == -1)
            stonesPerPeriod = -1; // IGS "0 -1"
        else
            stonesPerPeriod = time.getStones();
        if (time.getInitByoTime() != -1 && time.getInitByoTime() != -99)
            currentPeriodTime = periodTime = time.getInitByoTime();
        else
            periodTime = -1;
        blinked = alternateColor = false;
        updateClock();

        // System.err.println("Clock.init(ISGTime) " + time + "\n" + this);
    } //}}}

    //{{{ setCurrentTime(int, int, int) method
    /**
     *  Set the current time
     *
     *@param  absolut  Absolut time
     *@param  period   Time of byo-yomi period
     *@param  stones   Number of stones per byo-yomi period
     */
    public void setCurrentTime(int absolut, int period, int stones) {
        currentAbsoluteTime = absolut;
        currentPeriodTime = period;
        stonesLeftInPeriod = stones;
    } //}}}

    //{{{ setCurrentTime(IGSTime) method
    /**
     *  Set the current time. This method is used for IGS games
     *
     *@param  time  Time data packed in an IGSTime object
     */
    public void setCurrentTime(IGSTime time) {
        currentAbsoluteTime = time.getStones() == -1 ? time.getTime() : 0;
        currentPeriodTime = time.getStones() == -1 ? periodTime : time.getTime();
        stonesLeftInPeriod = time.getStones();
        // Switch from absolute to byo-yomi
        if (stonesPerPeriod == -1 && stonesLeftInPeriod > 0)
            stonesPerPeriod = stonesLeftInPeriod + 1;
        updateClock();
        reset();
    } //}}}

    //{{{ setIsMyClock() method
    /**
     *  Sets the isMyClock attribute of the Clock object
     *
     *@param  b  The new isMyClock value
     */
    public void setIsMyClock(boolean b) {
        isMyClock = b;
    } //}}}

    //{{{ assembleTimeString() method
    /**
     *  Assemble the string of the current time data, shown on the label
     *
     *@return    String showing the current clock state
     */
    private String assembleTimeString() {
        if (currentAbsoluteTime > 0 || stonesPerPeriod == -1)
            return (currentAbsoluteTime < 0 ? "-" : "") +
                    Utils.formatTime(Math.abs(currentAbsoluteTime) / 60) + ":" + Utils.formatTime(Math.abs(currentAbsoluteTime) % 60);
        // Ugly workaround for IGS clocks. We are still in absolute time, but don't know the byo-yomi period time yet
        // We -might- have gotten the byo-yomi time before, then switch. But unfortunately this is not guaranteed.
        else if (isIGSClock && currentAbsoluteTime == 0 && stonesLeftInPeriod == -1 && periodTime == -1) {
            return (currentPeriodTime < 0 ? "-" : "") +
                    Utils.formatTime(Math.abs(currentPeriodTime) / 60) + ":" + Utils.formatTime(Math.abs(currentPeriodTime) % 60);
        }
        else
            return (currentPeriodTime < 0 ? "-" : "") + Utils.formatTime(Math.abs(currentPeriodTime) / 60) + ":" +
                    Utils.formatTime(Math.abs(currentPeriodTime) % 60) + " (" + stonesLeftInPeriod + ")";
    } //}}}

    //{{{ updateClock() method
    /**  Redraw the clock label */
    public void updateClock() {
        setText(assembleTimeString());

        // Blink when in byo-yomi <= 30 seconds
        if (currentAbsoluteTime == 0 && stonesLeftInPeriod != -1 &&
                currentPeriodTime <= gGo.getSettings().getTimeWarningPeriod() && isMyClock)
            blink();
    } //}}}

    //{{{ blink() method
    /**  Alternate the background and foreground colors to show a blinking effect */
    private void blink() {
        alternateColor = !alternateColor;
        setBackground(alternateColor ? Color.darkGray : backgroundColor);
        setForeground(alternateColor ? Color.red : foregroundColor);

        if (!blinked) {
            SoundHandler.startTimeWarning();
            blinked = true;
        }
    } //}}}

    //{{{ start() method
    /**  Start the clock */
    public void start() {
        timer.start();
        blinked = false;
    } //}}}

    //{{{ stop() method
    /**  Stop the clock */
    public void stop() {
        timer.stop();
        reset();
    } //}}}

    //{{{ reset() method
    /**  Reset the clock background */
    private void reset() {
        setForeground(foregroundColor);
        setBackground(backgroundColor);
        alternateColor = false;
        SoundHandler.stopTimeWarning();
    } //}}}

    //{{{ playStone() method
    /**
     *  Play a stone, used for GTP games. This method checks if the player has ran out of time, having a negative
     *  time value.
     *
     *@return    False if the player ran out of time. True if he has time left and can play on.
     */
    public boolean playStone() {
        if (currentAbsoluteTime == 0) {
            stonesLeftInPeriod--;
            int tmp = currentPeriodTime;
            if (stonesLeftInPeriod == 0) {
                stonesLeftInPeriod = stonesPerPeriod;
                tmp = periodTime;
            }
            if (currentPeriodTime <= 0) {
                updateClock();
                reset();
                return false;
            }
            currentPeriodTime = tmp;
            updateClock();
        }
        reset();
        return true;
    } //}}}

    //{{{ toString() method
    /**
     *  Convert the clock data into a String. For debugging.
     *
     *@return    Converted String
     */
    public String toString() {
        return "[" + currentAbsoluteTime + "/" + absoluteTime + "  " +
                currentPeriodTime + "/" + periodTime + "  " +
                stonesLeftInPeriod + "/" + stonesPerPeriod + "]";
    } //}}}
}

