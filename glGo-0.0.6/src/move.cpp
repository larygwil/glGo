/*
 * move.cpp
 *
 * $Id: move.cpp,v 1.30 2003/10/31 22:03:43 peter Exp $
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

#ifdef __GNUG__
#pragma implementation "move.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include <wx/log.h>
#endif

#include "stones.h"
#include "move.h"
#include <algorithm>

// #define DEBUG_SAVE_MODE


Move::Move()
{
    played_stone = Stone();
    number = caps_white = caps_black = 0;
    checked = false;
    parent = brother = son = marker = NULL;
    comment = wxEmptyString;
}

Move::Move(const Stone &stone, const Stones &captures, unsigned short number,
           unsigned short caps_white, unsigned short caps_black)
    : played_stone(stone), captures(captures), number(number), caps_white(caps_white), caps_black(caps_black)
{
    parent = brother = son = marker = NULL;
    checked = false;
    comment = wxEmptyString;
}

Move::~Move()
{
#if 0
    wxLogDebug(_T("Move destructor.\n"
                  "Sizeof captures: %d, "
                  "Sizeof edited stones: %d, "
                  "Sizeof marks: %d"),
               captures.size(), editedStones.size(), marks.size());
#endif
    captures.clear();
    editedStones.clear();
    clearMarks();
}

void Move::clearMarks()
{
    // wxLogDebug(_T("clearMarks(): Have %d marks"), marks.size());
    while (!marks.empty())
    {
        Mark *m = marks.back();
        marks.pop_back();
        delete m;
    }
    marks.clear();
}

void Move::check(Stones &caps)
{
    captures.clear();
    captures = caps;
    checked = true;

    // Add captures
    if (getColor() == STONE_WHITE)
        caps_white += caps.size();
    else
        caps_black += caps.size();
    if (parent != NULL)
    {
        caps_white += parent->getCapsWhite();
        caps_black += parent->getCapsBlack();
    }
}

bool Move::hasMark(const Position &mark)
{
    if (marks.empty())
        return false;

    ConstMarksIterator it;
    for (it=marks.begin(); it!=marks.end(); ++it)
        if (mark.getX() == (*it)->getX() && mark.getY() == (*it)->getY())
            return true;
    return false;
}

const Mark* Move::getMark(const Position &mark)
{
    if (marks.empty())
        return NULL;

    ConstMarksIterator it;
    for (it=marks.begin(); it!=marks.end(); ++it)
        if (mark.getX() == (*it)->getX() && mark.getY() == (*it)->getY())
            return *it;
    return NULL;
}

bool Move::addMark(unsigned short x, unsigned short y, MarkType t, const wxString &txt)
{
    if (hasMark(Position(x, y)))
    {
        // wxLogDebug("Already have a mark at %u/%u", x, y);
        return false;
    }

    Mark *m = Mark::createMark(x, y, t, txt);

    wxASSERT(m != NULL);
    marks.push_back(m);
    return true;
}

void Move::addMark(Mark* mark)
{
    wxASSERT(mark != NULL);
    if (mark == NULL)
        return;
    marks.push_back(mark);
}

bool Move::removeMark(unsigned short x, unsigned short y, MarkType t)
{
    ConstMarksIterator it;
    for (it=marks.begin(); it!=marks.end(); ++it)
    {
        if (x == (*it)->getX() && y == (*it)->getY())
        {
            marks.remove(*it);
            delete *it;
            return true;
        }
    }
    return false;
}

unsigned short Move::getBrothers() const
{
    int counter = 0;
    Move *tmp;
    if (parent != NULL)
        tmp = parent->son;
    else
    {
        if (brother == NULL)
            return 0;
        else
        {
            tmp = brother;  // We cannot assign tmp = this; because of const !
            counter = 1;
        }
    }
    wxASSERT(tmp != NULL);

    while ((tmp = tmp->brother) != NULL)
        counter++;

    return counter;
}

unsigned short Move::getSons() const
{
    if (son == NULL)
        return 0;

    Move *tmp = son;
    int counter = 1;

    while ((tmp = tmp->brother) != NULL)
        counter++;

    return counter;
}

bool Move::hasEditedStone(const Stone &s) const
{
    if (editedStones.empty())
        return false;

    ConstStonesIterator result = find(editedStones.begin(), editedStones.end(), s);
    if (result == editedStones.end())
        return false;
    return true;
}

void Move::addStone(const Stone &stone)
{
    bool do_not_add = false;

    switch(stone.getColor())
    {
    case STONE_WHITE:
    {
        Stone s(stone.getX(), stone.getY(), STONE_REMOVED_WHITE);
        if (hasEditedStone(s))
            do_not_add = true;
        removeStone(s);
        break;
    }
    case STONE_BLACK:
    {
        Stone s(stone.getX(), stone.getY(), STONE_REMOVED_BLACK);
        if (hasEditedStone(s))
            do_not_add = true;
        removeStone(s);
        break;
    }
    case STONE_REMOVED_WHITE:
    {
        Stone s(stone.getX(), stone.getY(), STONE_WHITE);
        if (hasEditedStone(s))
            do_not_add = true;
        removeStone(s);
        break;
    }
    case STONE_REMOVED_BLACK:
    {
        Stone s(stone.getX(), stone.getY(), STONE_BLACK);
        if (hasEditedStone(s))
            do_not_add = true;
        removeStone(s);
        break;
    }
    default:
        break;
    }

    // The stone was added in this move? Then just remove it and don't add a REMOVED_XXX stone
    // This avoid something like AB[kk]AE[kk] and superflous AB/AW/AE
    if (!do_not_add && !hasEditedStone(stone))
        editedStones.push_back(stone);
}

wxString Move::saveMarks()
{
    wxString txt;
    wxString sSQ = wxEmptyString;
    wxString sCR = wxEmptyString;
    wxString sTR = wxEmptyString;
    wxString sMA = wxEmptyString;
    wxString sLB = wxEmptyString;
    wxString sTB = wxEmptyString;
    wxString sTW = wxEmptyString;

    ConstMarksIterator it;
    for (it=marks.begin(); it!=marks.end(); ++it)
    {
        switch ((*it)->getType())
        {
        case MARK_NONE:
            wxFAIL_MSG(_T("Cannot save MARK_NONE"));
            break;
        case MARK_SQUARE:
            if (sSQ.empty())
                sSQ = _T("SQ");
            sSQ += wxString::Format("[%c%c]", 'a' + (*it)->getX()-1, 'a' + (*it)->getY()-1);
            break;
        case MARK_CIRCLE:
            if (sCR.empty())
                sCR = _T("CR");
            sCR += wxString::Format("[%c%c]", 'a' + (*it)->getX()-1, 'a' + (*it)->getY()-1);
            break;
        case MARK_TRIANGLE:
            if (sTR.empty())
                sTR = _T("TR");
            sTR += wxString::Format("[%c%c]", 'a' + (*it)->getX()-1, 'a' + (*it)->getY()-1);
            break;
        case MARK_CROSS:
            if (sMA.empty())
                sMA = _T("MA");
            sMA += wxString::Format("[%c%c]", 'a' + (*it)->getX()-1, 'a' + (*it)->getY()-1);
            break;
        case MARK_TEXT:
            if (sLB.empty())
                sLB = _T("LB");
            sLB += wxString::Format("[%c%c:%s]", 'a' + (*it)->getX()-1, 'a' + (*it)->getY()-1, (*it)->getText().c_str());
            break;
        case MARK_TERR_WHITE:
            if (sTW.empty())
                sTW = _T("TW");
            sTW += wxString::Format("[%c%c]", 'a' + (*it)->getX()-1, 'a' + (*it)->getY()-1);
            break;
        case MARK_TERR_BLACK:
            if (sTB.empty())
                sTB = _T("TB");
            sTB += wxString::Format("[%c%c]", 'a' + (*it)->getX()-1, 'a' + (*it)->getY()-1);
            break;
        default:
            wxLogDebug(_T("Mark type %d not yet supported"), (*it)->getType());
        }
    }

#ifdef DEBUG_SAVE_MODE
    wxLogDebug("SAVEEDITEDMARKS %s %s %s %s %s %s %s",
               sSQ.c_str(), sCR.c_str(), sTR.c_str(), sMA.c_str(), sLB.c_str(), sTB.c_str(), sTW.c_str());
#endif
    return sSQ + sCR + sTR + sMA + sLB + sTB + sTW;
}

wxString Move::saveEditedStones()
{
    wxString sAB = wxEmptyString;
    wxString sAW = wxEmptyString;
    wxString sAE = wxEmptyString;

    ConstStonesIterator it;
    for (it=editedStones.begin(); it!=editedStones.end(); ++it)
    {
        switch (it->getColor())
        {
        case STONE_REMOVED_WHITE:
        case STONE_REMOVED_BLACK:
        case STONE_REMOVED:
            if (sAE.empty())
                sAE = _T("AE");
            sAE += wxString::Format("[%c%c]", 'a' + it->getX()-1, 'a' + it->getY()-1);
            break;
        case STONE_BLACK:
            if (sAB.empty())
                sAB = _T("AB");
            sAB += wxString::Format("[%c%c]", 'a' + it->getX()-1, 'a' + it->getY()-1);
            break;
        case STONE_WHITE:
            if (sAW.empty())
                sAW = _T("AW");
            sAW += wxString::Format("[%c%c]", 'a' + it->getX()-1, 'a' + it->getY()-1);
            break;
        default:
            break;
        }
    }

#ifdef DEBUG_SAVE_MODE
    wxLogDebug("SAVEEDITEDMOVES %s %s %s", sAE.c_str(), sAW.c_str(), sAB.c_str());
#endif
    return sAE + sAB + sAW;  // AE before AB/AW is important
}

wxString Move::MoveToSGF()
{
    wxString str = parent != NULL ? ";" : wxEmptyString;  // No ";" for root node

    if (getX() > 0 && getY() > 0)
        // Write something like 'B[aa]'
        str += wxString::Format("%s[%c%c]",
                                getColor() == STONE_BLACK ? "B" : "W",
                                'a' + getX()-1,
                                'a' + getY()-1);
    else if ((getX() == -1 && getY() == -1) ||
             (getX() == 20 && getY() == 20))
        // Pass: 'B[tt]'
        str += wxString::Format("%s[tt]",
                                getColor() == STONE_BLACK ? "B" : "W");

    // Save edited stones
    if (!editedStones.empty())
        str += saveEditedStones();

    // Save marks
    if (!marks.empty())
        str += saveMarks();

#if 0
    // Save time info, if we have any
    if (time > 0)
        str.append((stoneColor == STONE_BLACK ? "B" : "W") + "L[" + time + "]");
#endif

    // Add comment, if we have one
    if (!comment.empty())
    {
        // Work on copy of original comment
        wxString s = comment;

        // Replace "\" with "\\"
        s.Replace("\\", "\\\\");

        // Replace "]" with "\]"
        s.Replace("]", "\\]");

        // Replace ";" with "\;"
        s.Replace(";", "\\;");

        // Remove trailing "\n" and whitespaces if we have any
        s.Trim();

        str += "C[" + s + "]";
    }

#ifdef DEBUG_SAVE_MODE
    wxLogDebug("SAVE MOVE: %s", str.c_str());
#endif

    return str;
}

wxChar Move::getNextFreeLetter()
{
    // Nothing to do
    if (marks.empty())
        return 'A';

    // Set range A-Z, a-z to unoccupied
    bool occupied[52];
    int n;
    for (int i=0; i<52; i++)
        occupied[i] = false;

    // Loop through marks and set existing letters as occupied
    ConstMarksIterator it;
    for (it=marks.begin(); it!=marks.end(); ++it)
    {
        if ((*it)->getType() != MARK_TEXT)
            continue;

        wxString label = (*it)->getText();

        if (label.length() != 1)
        {
            // TODO...
            wxLogDebug("Label length != 1. Bad luck.");
            continue;
        }
        n = label.GetChar(0) - 'A';
        if (n >= 32)
            n -= 6;
        // This occurs if it's a number. This should not happen as we want
        // to handle that seperately, but as it's no error, continue silently
        if (n <0 || n > 51)
            continue;
        occupied[n] = true;
    }

    // Find smallest unoccupied
    n=0;
    while (occupied[n] && n < 51)
        n++;

    // Return letter in range A-Z, a-z
    return 'A' + (n >= 26 ? n + 6 : n);
}
