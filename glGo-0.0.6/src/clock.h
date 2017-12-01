/*
 * clock.h
 *
 * $Id: clock.h,v 1.7 2003/10/31 22:02:02 peter Exp $
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

#ifndef CLOCK_H
#define CLOCK_H

#ifdef __GNUG__
#pragma interface "clock.h"
#endif

#define NO_BYO -999

/**
 * A simple class representing the logic of a clock. Only the IGS byo-yomi time
 * system is supported. This class is neither responsible for the GUI nor the
 * timer which updates the GUI. The GUI handling is done by the sidebar which
 * contains the clock wxStaticText labels, the timer is handled by the MainFrame.
 */
class Clock
{
public:
    /** Default constructor */
    Clock();

    /**
     * Constructors. Sets initial time. This is the same like calling the default
     * constructor and then setCurrentTime(int, int)
     * @param time Time in seconds. This can be either absolute or byoyomi time.
     * @param stones Number of remaining byoyomi stones. If -1, time will be interpreted as absolute.
     */
    Clock(int time, short stones);

    /**
     * Sets the clock to given time.
     * @param time Time in seconds. This can be either absolute or byoyomi time.
     * @param stones Number of remaining byoyomi stones. If -1, time will be interpreted as absolute.
     */
    void setCurrentTime(int time, short stones=-1);

    /** Gets the current time value. May be absolute or byo-yomi. */
    int getTime() const { return time; }

    /** Gets the current number of byoyomi stones */
    short getStones() const { return stones; }

    /** Format time values into a string */
    wxString format() const;

    /** Format time values into a string. Same as format() but can be called as static function. */
    static wxString Format(int t, short s=-1);

    /** Check is the clock is currently running. */
    bool IsRunning() const { return running; }

    /** Start the clock */
    void Start() { running = true; }

    /** Stop the clock */
    void Stop() { running = false; }

    /**
     * Do a tick and reduce the current time by seconds (usually 1 second). This is called by
     * the clock timer in the MainFrame once per second if the clock is running.
     * Also a check for the transition from absolute time into byoyomi periods is done here.
     * @param seconds Number of seconds to reduce from time.
     * @return New time value when in byoyomi period, else NO_BYO
     */
    int Tick(int seconds=1);

    /** Sets the byoyomi time. */
    void setByoTime(int byo) { byotime = byo; }

private:
    static wxString formatTime(int t);
    static wxString formatTimePart(int t);

    int time, byotime;
    short stones;
    bool running, in_byo;
#ifdef __VISUALC__
    // Crap compiler
    #define DEFAULT_BYO_STONES 25
#else
    static const short DEFAULT_BYO_STONES = 25;
#endif
};

#endif
