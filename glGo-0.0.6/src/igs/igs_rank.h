/*
 * igs_rank.h
 *
 * $Id: igs_rank.h,v 1.4 2003/10/05 20:08:41 peter Exp $
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

#ifndef IGS_RANK_H
#define IGS_RANK_H

#ifdef __GNUG__
#pragma interface "igs_rank.h"
#endif

/**
 * Class representing an IGS rank. The rank is encoded with a numeric value,
 * reaching from 52 (NR) to 0 (10p).
 * Most functions are inline for performance improvement.
 *
 * @verbatim
                                Rank table
                                ----------

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
@endverbatim
 * @ingroup igs
 */
class IGSRank
{
public:
    /**
     * Constructor
     * @param rank Numeric value, see rank table of this file
     * @param mark True if the rank is marked with a "*"
     */
    IGSRank(unsigned short rank, bool mark) : rank(rank), mark(mark) {}

    /**
     * Constructor. Converts the given string into the internal value.
     * @param s Rank given as string, for example "2d*"
     */
    IGSRank(const wxString &s)
        {
            if (s.empty())
            {
                rank = 54;
                mark = false;
            }
            rank = convertRank(s);
            if (s.Last() == '*')
                mark = true;
            else
                mark = false;
        }

    /** Copy constructor */
    IGSRank(const IGSRank &r) : rank(r.rank), mark(r.mark) {}

    /**
     * Convert a rank represented as String to the internal numeric value.
     * @ param s Rank as String
     * @ return Internal rank value
     */
    unsigned short convertRank(const wxString &s) const;

    /** Returns the internal integer value of this rank. */
    unsigned short getRank() const { return rank; }

    /** Check if the rank is marked with a "*". */
    bool isMarked() const { return mark; }

    /** Operator ==. Compare rank with another. */
    bool operator==(const IGSRank &r) const { return rank == r.rank; }

    /** Operator ==. Compare rank with another. */
    bool operator!=(const IGSRank &r) const { return rank != r.rank; }

    /** Operator <. Compare rank with another. */
    bool operator<(const IGSRank &r) const { return rank < r.rank; }

    /** Operator >. Compare rank with another. */
    bool operator>(const IGSRank &r) const { return rank > r.rank; }

    /** Operator ==. Compare rank with another represented as string. */
    bool operator==(const wxString &s) const { return rank == convertRank(s); }

    /** Operator !=. Compare rank with another represented as string. */
    bool operator!=(const wxString &s) const { return rank != convertRank(s); }

    /** Operator <. Compare rank with another represented as string. */
    bool operator<(const wxString &s) const { return rank < convertRank(s); }

    /** Operator >. Compare rank with another represented as string. */
    bool operator>(const wxString &s) const { return rank > convertRank(s); }

    /**
     * Compare two ranks. This function is used by the table sorting.
     * return -1 if this rank is lesser, 0 if equal and 1 if greater than the given rank
     */
    int Cmp(const IGSRank &r) { return rank < r.rank ? -1 : rank > r.rank ? 1 : 0; }

private:
    unsigned short rank;    ///< Internal rank value
    bool mark;              ///< True if the rank is marked with "*"
};

#endif
