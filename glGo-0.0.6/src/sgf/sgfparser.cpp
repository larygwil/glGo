/*
 * sgfparser.cpp
 *
 * $Id: sgfparser.cpp,v 1.32 2003/10/31 22:00:35 peter Exp $
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
#pragma implementation "sgfparser.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#ifndef SGF_CONSOLE
#include "wx/log.h"
#include "wx/intl.h"
#else
#include "wx/wx.h"
#endif
#endif

#include <wx/textfile.h>
#include <wx/progdlg.h>
#include "sgfparser.h"
#include "node.h"
#include "utils/utils.h"
#include "boardhandler.h"
#include "gamedata.h"
#include "marks.h"

// Control what to print in console mode
#ifdef SGF_CONSOLE
#define DEBUG_NODES
#define DEBUG_TREE
#define DEBUG_SENDING
#endif

WX_DEFINE_ARRAY(Node*, NodeQueue);
WX_DEFINE_ARRAY(Node*, Queue);


SGFParser::SGFParser(BoardHandler *handler, wxWindow *parent)
    : parent(parent)
{
#ifndef SGF_CONSOLE
    wxASSERT(handler != NULL);
#endif
    bh = handler;
#if defined(wxUSE_PROGRESSDLG) && !defined(SGF_CONSOLE)
    progDlg = NULL;
#endif
    sgf_error = wxEmptyString;
}

SGFParser::~SGFParser()
{ }

bool SGFParser::loadSGF(const wxString &filename, GameData *game_data)
{
    wxASSERT(game_data != NULL);
    data = game_data;

    wxString sgf = wxEmptyString;
    if (!loadFile(filename, sgf))
    {
        LOG_SGF(wxString::Format(_T("Failed to load file: %s"), filename.c_str()));
        return false;
    }

    // Create SGF node tree from file
    Node *root = NULL;
    bool res = parseSGF(sgf, root);
    if (res)
        // Parse tree and send moves to boardhandler
        res = parseSGFTree(root);

    // Cleanup and exit
    deleteTree(root);

#if defined(wxUSE_PROGRESSDLG) && !defined(SGF_CONSOLE)
    progDlg->Destroy();
    progDlg = NULL;
#endif

    if (res)
        LOG_SGF(_T("File successfully loaded"));

    return res;
}

void SGFParser::deleteTree(Node *root)
{
    if (root == NULL)
        return;

    NodeQueue stack;
    NodeQueue trash;
    Node *n = NULL;

    // Traverse the tree and drop every node into stack trash
    stack.Add(root);

    while (!stack.IsEmpty())
    {
        size_t sz = stack.GetCount()-1;
        n = stack.Item(sz);
        stack.RemoveAt(sz);
        if (n != NULL)
        {
            trash.Add(n);
            if (n->brother != NULL)
                stack.Add(n->brother);
            if (n->son != NULL)
                stack.Add(n->son);
        }
    }

    // Clear trash
    WX_CLEAR_ARRAY(trash);
    trash.Clear();
}

bool SGFParser::loadFile(const wxString &filename, wxString &sgf)
{
    wxTextFile file(filename);

    if (!file.Exists() || !file.Open())
    {
        LOG_SGF(wxString::Format(_T("Failed to open file: %s"), filename.c_str()));
        sgf_error.Printf(_("Failed to open file: %s"), filename.c_str());
        return false;
    }

    // Append newline, a file might not end in a newline, then the last line
    // would not be read in. The physical file is not changed.
    file.AddLine("");

    if (file.Eof())
    {
        LOG_SGF(_T("Empty file"));
        sgf_error = _("Empty file.");
        return false;
    }

    wxString str;
    for (str = file.GetFirstLine(); !file.Eof(); str = file.GetNextLine())
        sgf += str + "\n";

    return true;
}

bool SGFParser::parseSGF(const wxString &sgf, Node *&root)
{
    Node::counter = 0;
    unsigned long nextNode = 0;
    findNext(nextNode, sgf, ';', true);
    if (nextNode == wxString::npos)
    {
        wxLogDebug("No nodes");
        return false;  // No nodes
    }

#if defined(wxUSE_PROGRESSDLG) && !defined(SGF_CONSOLE)
    // Create progress dialog
    wxASSERT(progDlg == NULL);
    progDlg = new wxProgressDialog (_("Loading"), _("Loading SGF file..."), 100, parent,
                                    wxPD_APP_MODAL | wxPD_CAN_ABORT | wxPD_AUTO_HIDE);
    unsigned long sgf_length = sgf.length();
    size_t prog_lock = 0;
#endif

    unsigned long nextVarStart = 0;
    unsigned long nextVarEnd = 0;
    findNext(nextVarStart, sgf, '(', true);
    findNext(nextVarEnd, sgf, ')');

#ifdef DEBUG_NODES
    wxPrintf("*** varstart = %u, varend = %u\n", nextVarStart, nextVarEnd);
#endif

    bool abort = false;
    root = new Node(wxEmptyString);
    Node *node = root;
    NodeQueue queue;
    queue.Add(root);

    while(true)
    {
        unsigned long pos = minpos(nextNode, nextVarStart, nextVarEnd);

#if defined(wxUSE_PROGRESSDLG) && !defined(SGF_CONSOLE)
        if ((++prog_lock % 10) == 0)  // Only update progressbar every 10th step
        {
            int value = pos * 50 / sgf_length;
            if (value > 100)
                value = 100;  // Avoid crashing the progress dialog
            if (!progDlg->Update(value))
                return false;
        }
#endif

        if (pos == wxString::npos)
            break;

        if (pos == nextNode)
        {
            findNext(nextNode, sgf, ';');
            Node *n = new Node(sgf.Mid(pos+1,
                                       minpos(nextNode, nextVarStart, nextVarEnd) - pos-1).Trim());
            if (node->son == NULL)
                node->son = n;
            else
            {
                Node *b = node->son;
                while (b->brother != NULL)
                    b = b->brother;
                b->brother = n;
            }
            node = n;

#ifdef DEBUG_NODES
            wxPrintf("  Node at %u, next at %u - %s\n", pos, nextNode,
                     (sgf.Mid(pos+1, minpos(nextNode, nextVarStart, nextVarEnd) - pos-1)).c_str());
#endif

            if (abort)
                nextNode = wxString::npos;
        }
        else if (pos == nextVarStart)
        {
            queue.Add(node);
            findNext(nextVarStart, sgf, '(');
#ifdef DEBUG_NODES
            wxPrintf("Var Start at %u, next at %u\n", pos, nextVarStart);
#endif
        }
        else if (pos == nextVarEnd)
        {
            size_t sz = queue.GetCount();
            node = queue.Item(sz-1);
            queue.RemoveAt(sz-1);
            findNext(nextVarEnd, sgf, ')');
#ifdef DEBUG_NODES
            wxPrintf("Var End at %u, next at %u\n", pos, nextVarEnd);
#endif
        }
    }

    return true;
}

void SGFParser::findNext(unsigned long &start, const wxString &sgf, char c, bool start_flag)
{
    // wxPrintf("Entering findNext: start = %u\n", start);
    unsigned long next, last=0;
    unsigned long s;

    while (true)
    {
        // wxPrintf("start = %u\n", start);
        next = sgf.find(c, start + (!start_flag?1:0));
        // wxPrintf("next = %u\n", next);

        s = start+1;
        if (next == wxString::npos || (next-start) <= 0 || checkCommentOpenClose(sgf, s, next, last))
            break;

        start = next;
    }
    start = next;
    // wxPrintf("returning: next = %u\n", next);
}

bool SGFParser::checkCommentOpenClose(const wxString sgf, const unsigned long &start,
                                      const unsigned long &next, unsigned long &last)
{
    // wxPrintf("CCOC: %u %u\n", start, next);
    // wxPrintf("### %s\n", sgf.Mid(start, next-start).c_str());

    unsigned long comment_start = start, pos = 0;
    while (true)
    {
        pos = sgf.find("[", comment_start);
        if (pos == wxString::npos || pos >= next)
            break;
        comment_start = pos+1;
    }
    comment_start --;
    // wxPrintf("comment_start = %u\n", comment_start);

    if (comment_start == 0 && last == 0)
        return true;  // No opening '[' found now or earlier
    last = comment_start != 0 ? comment_start : last;
    // wxPrintf("last = %u\n", last);

    unsigned long comment_node_end = last;
    while (true)
    {
        pos = comment_node_end;
        findNextUnescapedChar(']', pos, sgf);
        if (pos == wxString::npos || pos >= next)
            break;
        comment_node_end = pos+1;
    }
    // wxPrintf("comment_node_end = %u\n", comment_node_end);
    if (comment_node_end == last)
        return false;

    return true;
}

void SGFParser::findNextUnescapedChar(char c, unsigned long &t, const wxString &sgf)
{
    do
    {
        t = sgf.find(c, t+1);
        if (t == wxString::npos)
            break;
    } while (t > 0 && sgf.GetChar(t-1) == '\\');
}

unsigned long SGFParser::minpos(const unsigned long &a, const unsigned long &b, const unsigned long &c)
{
    if (a < b)
    {
        if (a < c)
            return a;
        return c;
    }
    else if (b < c)
        return b;
    return c;
}

bool SGFParser::parseSGFTree(Node *root)
{
    if (root == NULL || root->son == NULL)
    {
        LOG_SGF(_T("The SGF tree is empty."));
        sgf_error = _("The SGF tree is empty.");
        return false;
    }

    Node *node = root->son;
    unsigned int counter = 0;
    Queue queue;
#if defined(wxUSE_PROGRESSDLG) && !defined(SGF_CONSOLE)
    size_t prog_counter = 0, prog_lock = 0;
#endif

    while (node != NULL)
    {
        while (node != NULL)
        {
#if defined(wxUSE_PROGRESSDLG) && !defined(SGF_CONSOLE)
            prog_counter ++;
            if ((++prog_lock % 10) == 0)  // Only update progressbar every 10th step
            {
                int value = 50 + prog_counter * 50 / Node::counter;
                if (value > 100)
                    value = 100;  // Avoid crashing the progress dialog
                if (!progDlg->Update(value))
                    return false;
            }
#endif

            node->number = counter;
#ifdef DEBUG_TREE
            wxPrintf(_T("counter = %u\n"), counter);
#endif
            if (node->brother != NULL)
            {
                node->brother->number = counter;
                queue.Add(node->brother);
#ifdef DEBUG_TREE
                wxPrintf(_T("Pushed\n"));
#endif
            }
            if (parseSGFNode(node))
                counter += 1;
            node = node->son;
        }
        size_t sz = queue.GetCount();
        if (!sz)
            break;
        node = queue.Item(sz-1);
        queue.RemoveAt(sz-1);
        unsigned int num_back = counter - node->number;
#ifdef DEBUG_TREE
        wxPrintf("Popped, now %d moves back.\n", num_back);
#endif

#ifndef SGF_CONSOLE
        wxASSERT(bh != NULL);
        for (unsigned int i=0; i<num_back; i++)
            bh->previousMove();
#elif defined DEBUG_SENDING
        wxPrintf(_T("Sending to boardhandler: %u x previousMove()\n"), num_back);
#endif
        counter -= num_back;
    }

    return true;
}

bool SGFParser::parseSGFNode(Node *node)
{
    wxASSERT(node != NULL);
    bool move_added = node->number == 0;  // True for move 0, required for handicap setup

    if (!node->getProperties().IsEmpty())
        for (size_t i=0, sz = node->getProperties().GetCount(); i<sz; i++)
            parseSGFProperty(node->getProperties().Item(i), move_added);

    // wxLogDebug(_T("parseSGFNode: move_added = %d"), move_added);
    return move_added;
}

void SGFParser::parseSGFProperty(Property *prop, bool &move_added)
{
#if defined(__WXDEBUG__) && defined(DEBUG_NODES)
    prop->printMe();
#endif

    wxString id = prop->getIdent();

    /*
     * Black move
     */
    if (!id.Cmp(_T("B")))
    {
        for (size_t i=0, sz = prop->getValues().Count(); i<sz; i++)
            if (parseMove(prop->getValues().Item(i), STONE_BLACK))
                move_added = true;
    }

    /*
     * White move
     */
    else if (!id.Cmp(_T("W")))
    {
        for (size_t i=0, sz = prop->getValues().Count(); i<sz; i++)
            if (parseMove(prop->getValues().Item(i), STONE_WHITE))
                move_added = true;
    }

    /*
     * Comment
     */
    else if (!id.Cmp(_T("C")))
    {
        // C *should* have only one value. But who knows...
        for (size_t i=0, sz = prop->getValues().Count(); i<sz; i++)
        {
            wxString comment = prop->getValues().Item(i);
            comment.Replace("\\", "");  // Remove escape character
#ifndef SGF_CONSOLE
            wxASSERT(bh != NULL);
            bh->setComment(comment);
#elif defined DEBUG_SENDING
            wxPrintf(_T("Sending to boardhandler: setComment(\"%s\")\n"), comment.c_str());
#endif
        }
    }

    /*
     * AB, AW
     */
    else if (!id.Cmp(_T("AB")) || !id.Cmp(_T("AW")))
    {
        for (size_t i=0, sz = prop->getValues().Count(); i<sz; i++)
        {
            unsigned short x=0, y=0;
            parseSGFStringMove(prop->getValues().Item(i), x, y);
#ifndef SGF_CONSOLE
            wxASSERT(bh != NULL);
            // If this node had no new move yet, add an empty one first
            if (!move_added)
            {
                bh->playMoveSGF(0, 0, STONE_UNDEFINED);
                move_added = true;
            }
            bh->addEditStoneSGF(x, y, !id.Cmp(_T("AB")) ? STONE_BLACK : STONE_WHITE);
#elif defined DEBUG_SENDING
            if (!move_added)
            {
                wxPrintf(_T("Sending to boardhandler: bh->playMoveSGF(0, 0, STONE_UNDEFINED);\n"));
                move_added = true;
            }
            wxPrintf(_T("Sending to boardhandler: bh->addEditStoneSGF(%d, %d, %s)\n"),
                     x, y, (!id.Cmp(_T("AB")) ? _T("STONE_BLACK") : _T("STONE_WHITE")));
#endif
        }
    }

    /*
     * AE
     */
    else if (!id.Cmp(_T("AE")))
    {
        for (size_t i=0, sz = prop->getValues().Count(); i<sz; i++)
        {
            unsigned short x=0, y=0;
            parseSGFStringMove(prop->getValues().Item(i), x, y);
#ifndef SGF_CONSOLE
            wxASSERT(bh != NULL);
            // If this node had no new move yet, add an empty one first
            if (!move_added)
            {
                bh->playMoveSGF(0, 0, STONE_UNDEFINED);
                move_added = true;
            }
            bh->removeEditStoneSGF(x, y);
#elif defined DEBUG_SENDING
            if (!move_added)
            {
                wxPrintf(_T("                        bh->playMoveSGF(0, 0, STONE_UNDEFINED);"));
                move_added = true;
            }
            wxPrintf(_T("Sending to boardhandler: bh->removeEditStoneSGF(%d, %d)\n"), x, y);
#endif
        }
    }

    /*
     * CR, MA, SQ, CR, TW, TB
     */
    else if (!id.Cmp(_T("CR")) || !id.Cmp(_T("MA")) || !id.Cmp(_T("SQ")) || !id.Cmp(_T("TR")) ||
             !id.Cmp(_T("TW")) || !id.Cmp(_T("TB")))
    {
        for (size_t i=0, sz = prop->getValues().Count(); i<sz; i++)
            parseMark(id, prop->getValues().Item(i));
    }

    /*
     * LB
     */
    else if (!id.Cmp(_T("LB")))
    {
        for (size_t i=0, sz = prop->getValues().Count(); i<sz; i++)
        {
            unsigned short x=0, y=0;
            wxString item = prop->getValues().Item(i);
            parseSGFStringMove(item.Left(2), x, y);
            wxString txt = item.Mid(item.First(":")+1);
#ifndef SGF_CONSOLE
            wxASSERT(bh != NULL);
            bh->addMark(x, y, MARK_TEXT, txt);
#elif defined DEBUG_SENDING
            wxPrintf(_T("Sending to boardhandler: bh->addMark(%d, %d, MARK_TEXT, %s)\n"), x, y, txt.c_str());
#endif
        }
    }

    /*
     * L - This is not FF[4] but parsed as it is still quite commonly saved by other SGF editors (unfortunately)
     */
    else if (!id.Cmp(_T("L")))
    {
        for (size_t i=0, sz = prop->getValues().Count(); i<sz; i++)
        {
            // Cheat and append a ":" so the loop below works without hassle
            wxString item = prop->getValues().Item(i) + ":";
            size_t old_pos = 0;
            size_t pos = item.find(":", old_pos);
            do
            {
                wxString s = item.Mid(old_pos, pos-old_pos);
                wxASSERT(s.length() == 2);
                if (s.length() != 2)  // Should not happen. If, the SGF is invalid
                {
                    LOG_SGF(wxString::Format("Invalid SGF at: %s", item.c_str()));
                    break;
                }
                unsigned short x=0, y=0;
                parseSGFStringMove(s, x, y);
                wxChar txt = 'A' + i;
#ifndef SGF_CONSOLE
                wxASSERT(bh != NULL);
                bh->addMark(x, y, MARK_TEXT, txt);
#elif defined DEBUG_SENDING
                wxPrintf(_T("Sending to boardhandler: bh->addaArk(%d, %d, MARK_TEXT, %c)\n"), x, y, txt);
#endif
            } while ((pos = item.find(":", old_pos=pos+1)) != wxString::npos);
        }
    }

    /*
     * SZ
     */
    else if (!id.Cmp(_T("SZ")))
    {
        wxString bs = prop->getValues().Item(0);
        long tmp_l;
        if (!bs.ToLong(&tmp_l))
        {
            LOG_SGF(wxString::Format(_T("Invalid board size value: %s"), bs.c_str()));
            // Good luck...
            tmp_l = 19;
        }
        data->board_size = static_cast<unsigned int>(tmp_l);
    }

    /*
     * KM
     * todo: In german locale this creates invalid komi values.
     */
    else if (!id.Cmp(_T("KM")))
    {
        wxString komi = prop->getValues().Item(0);
        wxLogDebug(_T("Komi: %s"), komi.c_str());
        double tmp_d;
        if (!komi.ToDouble(&tmp_d))
        {
            LOG_SGF(wxString::Format(_T("Invalid komi value: %s"), komi.c_str()));
            // Good luck...
            tmp_d = 6.5;
        }
        data->komi = static_cast<float>(tmp_d);
    }

    /*
     * HA
     */
    else if (!id.Cmp(_T("HA")))
    {
        wxString handicap = prop->getValues().Item(0);
        wxLogDebug(_T("Handicap: %s"), handicap.c_str());
        long tmp_l;
        if (!handicap.ToLong(&tmp_l))
        {
            LOG_SGF(wxString::Format(_T("Invalid handicap value: %s"), handicap.c_str()));
            // Good luck...
            tmp_l = 0;
        }
        data->handicap = static_cast<unsigned int>(tmp_l);
    }

    /*
     * PW, PB, WR, BR
     */
    else if (!id.Cmp(_T("PW")))
    {
        data->whiteName = prop->getValues().Item(0);
    }
    else if (!id.Cmp(_T("PB")))
    {
        data->blackName = prop->getValues().Item(0);
    }
    else if (!id.Cmp(_T("WR")))
    {
        data->whiteRank = prop->getValues().Item(0);
    }
    else if (!id.Cmp(_T("BR")))
    {
        data->blackRank = prop->getValues().Item(0);
    }

    /*
     * Misc headers
     * GN, RE, CP, DT, PC, TM
     */
    else if (!id.Cmp(_T("GN")))
    {
        data->gameName = prop->getValues().Item(0);
    }
    else if (!id.Cmp(_T("RE")))
    {
        data->result = prop->getValues().Item(0);
    }
    else if (!id.Cmp(_T("CP")))
    {
        data->copyright = prop->getValues().Item(0);
    }
    else if (!id.Cmp(_T("DT")))
    {
        data->date = prop->getValues().Item(0);
    }
    else if (!id.Cmp(_T("PC")))
    {
        data->place = prop->getValues().Item(0);
    }
    else if (!id.Cmp(_T("TM")))
    {
        data->time = prop->getValues().Item(0);
    }

    /*
     * Unknown or not yet implemented
     */
#if defined(__WXDEBUG__) && defined(DEBUG_NODES) && 0
    else
    {
        wxLogDebug(_T("Unknown property or not yet implemented: %s"), id.c_str());
    }
#endif
}

bool SGFParser::parseMove(const wxString &value, Color color)
{
    // wxPrintf(_T("parseMove: %s %s\n"), (color == STONE_BLACK ? "B" : "W"), value.c_str());

    // Empty move, B[] occurs in Kogo
    if (value.empty())
    {
#ifndef SGF_CONSOLE
        wxASSERT(bh != NULL);
        bh->playMoveSGF(0, 0, STONE_UNDEFINED);
#elif defined DEBUG_SENDING
        wxPrintf(_T("Sending to boardhandler: playMoveSGF(0, 0, STONE_UNDEFINED)"));
#endif
        return true;
    }

    if (value.length() != 2)
    {
        LOG_SGF(wxString::Format(_T("Invalid point: %s"), value.c_str()));
        return false;
    }

    unsigned short x=0, y=0;
    parseSGFStringMove(value, x, y);

#ifndef SGF_CONSOLE
        wxASSERT(bh != NULL);
        bh->playMoveSGF(x, y, color);
#elif defined DEBUG_SENDING
        wxPrintf(_T("Sending to boardhandler: playMoveSGF(%d, %d, %d)\n"), x, y, color);
#endif

    return true;
}

void SGFParser::parseMark(const wxString &id, const wxString &value)
{
    if (id.empty() || value.empty())
        return;

    if (value.length() != 2)
    {
        LOG_SGF(wxString::Format(_T("Invalid point: %s"), value.c_str()));
        return;
    }

    // Get type
    MarkType t = MARK_NONE;
    if (!id.Cmp(_T("CR")))
        t = MARK_CIRCLE;
    else if(!id.Cmp(_T("TR")))
        t = MARK_TRIANGLE;
    else if (!id.Cmp(_T("SQ")))
        t = MARK_SQUARE;
    else if (!id.Cmp(_T("MA")))
        t = MARK_CROSS;
    else if (!id.Cmp(_T("TW")))
        t = MARK_TERR_WHITE;
    else if (!id.Cmp(_T("TB")))
        t = MARK_TERR_BLACK;
    else
    {
        LOG_SGF(wxString::Format(_T("Unknown mark type: %s"), id.c_str()));
        return;
    }

    // Get point
    unsigned short x=0, y=0;
    parseSGFStringMove(value, x, y);

#ifndef SGF_CONSOLE
    wxASSERT(bh != NULL);
    bh->addMark(x, y, t);
#elif defined DEBUG_SENDING
    wxPrintf(_T("Sending to boardhandler: addMark(%d, %d, %d)\n"), x, y, t);
#endif
}
