/*
 *  IGSRank.java
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
package ggo.igs;

//{{{ Rank table
/*
    Pro   Value    Dan    Value    Kyu    Value    Other   Value
     1p     9       1d      21      1k      22      ???     52
     2p     8       2d      20      2k      23       NR     53
     3p     7       3d      19      3k      24   undef.     54
     4p     6       4d      18      4k      25
     5p     5       5d      17      5k      26
     6p     4       6d      16      6k      27
     7p     3       7d      15      7k      28
     8p     2       8d      14      8k      29
     9p     1       9d      13      9k      30
    10p     0      10d      12     10k      31
                   11d      11     11k      32
                   12d      10     12k      33
                                   13k      34
                                   14k      35
                                   15k      36
                                   16k      37
                                   17k      38
                                   18k      39
                                   19k      40
                                   20k      41
                                   21k      42
                                   22k      43
                                   23k      44
                                   24k      45
                                   25k      46
                                   26k      47
                                   27k      48
                                   28k      49
                                   29k      50
                                   30k      51
*/ //}}}

/**
 *  Class representing an IGS rank. The rank is encoded with an Integer, reaching from
 *  52 (NR) to 0 (10p)
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/25 11:18:37 $
 */
public class IGSRank {
    //{{{ private members
    /**  Internal rank value */
    protected int rank;
    private boolean mark = false;
    //}}}

    //{{{ Rank(int, boolean) constructor
    /**
     *Constructor for the IGSRank object
     *
     *@param  rank  Integer code for the rank. 52 (NR) - 0 (10p)
     *@param  mark  True if rank is marked with "*", else false
     */
    public IGSRank(int rank, boolean mark) {
        this.rank = rank;
        this.mark = mark;
    } //}}}

    //{{{ Rank(String) constructor
    /**
     *Constructor for the IGSRank object
     *
     *@param  s                          Complete rank string
     *@exception  NumberFormatException  Thrown if rank cannot be parsed. Calling method has to handle this.
     */
    public IGSRank(String s) throws NumberFormatException {
        rank = convertRank(s);
        if (s.endsWith("*"))
            mark = true;
    } //}}}

    //{{{ convertRank() method
    /**
     *  Convert a rank represented as String to the internal int value
     *
     *@param  s                          Rank as String
     *@return                            Internal rank value
     *@exception  NumberFormatException  Failed to convert the String
     */
    private int convertRank(String s) throws NumberFormatException {
        int pos = -1;

        if (s.length() == 0)
            return 54;
        else if (s.startsWith("NR"))
            return 53;
        else if (s.startsWith("???"))
            return 52;
        else if ((pos = s.indexOf("k")) != -1)
            return 21 + Integer.parseInt(s.substring(0, pos));
        else if ((pos = s.indexOf("d")) != -1)
            return 22 - Integer.parseInt(s.substring(0, pos));
        else if ((pos = s.indexOf("p")) != -1)
            return 10 - Integer.parseInt(s.substring(0, pos));
        else {
            // System.err.println("Throwing exception, failed to parse rank: " + s);
            throw new NumberFormatException();
        }
        // System.err.println("Final rank: " + rank + " " + (mark ? "*" : ""));
    } //}}}

    //{{{ toString() method
    /**
     *  Convert this rank to a string. For debugging.
     *
     *@return    Converted string
     */
    public String toString() {
        if (rank == 54)
            return "";
        if (rank == 53)
            return "NR" + (mark ? "*" : "");
        if (rank == 52)
            return "???" + (mark ? "*" : "");
        if (rank >= 22 && rank <= 51)
            return String.valueOf(rank - 21) + "k" + (mark ? "*" : "");
        if (rank >= 10 && rank <= 21)
            return String.valueOf((rank - 22) * -1) + "d" + (mark ? "*" : "");
        if (rank >= 0 && rank <= 9)
            return String.valueOf((rank - 10) * -1) + "p" + (mark ? "*" : "");
        else
            return "NR";
    } //}}}

    //{{{ getRank() method
    /**
     *  Returns the internal integer value of this rank
     *
     *@return    The integer rank value
     */
    public int getRank() {
        return rank;
    } //}}}

    //{{{ compareTo() methods
    /**
     *  Compare this rank with another IGSRank object. "Larger" means "stronger"
     *
     *@param  r  Rank to compare with
     *@return    -1 if this rank is small than the argument r, 0 if equal, 1 if larger
     *@see       #compareTo(String)
     *@see       #compareTo(Object)
     */
    public int compareTo(IGSRank r) {
        if (rank < r.rank)
            return -1;
        if (rank == r.rank)
            return 0;
        return 1;
    }

    /**
     *  Compare this rank with another rank given as String
     *
     *@param  s  Rank to compare with, represented as String
     *@return    -1 if this rank is small than the argument r, 0 if equal, 1 if larger
     *@see       #compareTo(IGSRank)
     *@see       #compareTo(Object)
     */
    public int compareTo(String s) {
        int r = convertRank(s);
        if (rank < r)
            return -1;
        if (rank == r)
            return 0;
        return 1;
    }

    /**
     *  Compare this rank with another rank given as Object. The argument will be casted to an IGSRank.
     *
     *@param  o  Rank to compare with. Will be casted to IGSRank
     *@return    -1 if this rank is small than the argument r, 0 if equal, 1 if larger
     *@see       #compareTo(IGSRank)
     *@see       #compareTo(String)
     */
    public int compareTo(Object o) {
        return compareTo((IGSRank)o);
    } //}}}
}

