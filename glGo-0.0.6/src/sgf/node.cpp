/*
 * node.cpp
 *
 * $Id: node.cpp,v 1.12 2003/10/04 19:06:05 peter Exp $
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
#pragma implementation "node.h"
#endif

// For compilers that support precompilation, includes "wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/log.h"
#include "wx/intl.h"
#endif

#include "defines.h"
#include "node.h"


// -----------------------------------------------------------------------
//                             Class Property
// -----------------------------------------------------------------------

Property::Property(const wxString &id)
{
    ident = id;
}

void Property::addValue(wxString &value)
{
    values.Add(value);
}

#ifdef __WXDEBUG__
void Property::printMe() const
{
    wxPrintf("%s\n", ident.c_str());
    for (size_t i=0, sz = values.Count(); i<sz; i++)
        wxPrintf("    %s\n", values[i].c_str());
}
#endif


// -----------------------------------------------------------------------
//                              Class Node
// -----------------------------------------------------------------------

size_t Node::counter = 0;

Node::Node(const wxString &txt)
{
    brother = NULL;
    son = NULL;
    number = 0;
    counter ++;

    // wxPrintf(_T("Node: %s\n"), txt.c_str());

    if (txt.empty())
        return;

    if (!parseNode(txt))
    {
        LOG_SGF(wxString::Format(_T("Invalid SGF property: \"%s\""), txt.c_str()));
        WX_CLEAR_ARRAY(properties);
        properties.Clear();
    }
}

Node::~Node()
{
    WX_CLEAR_ARRAY(properties);
    properties.Clear();
}

bool Node::parseNode(const wxString &node)
{
    size_t l = node.length();
    size_t propStart = 0;
    size_t valStart = node.find("[");
    size_t valEnd = 0;
    findNextUnescapedValEnd(valEnd, node);
    if (valStart == wxString::npos || valEnd == wxString::npos)
    {
        wxLogDebug(_T("No values"));
        return false;
    }
    bool newProp = true;
    Property *property = NULL;

    while (true)
    {
        // wxPrintf("valStart = %d, valEnd = %d\n", valStart, valEnd);

        if (newProp)
        {
            wxString propIdent = node.Mid(propStart, valStart-propStart);
            // wxPrintf("propIdent = %s\n", propIdent.c_str());
            property = new Property(propIdent);
            newProp = false;
        }

        wxString propVal = node.Mid(valStart+1, valEnd-valStart-1);
        // wxPrintf("propVal = %s\n", propVal.c_str());
        wxASSERT(property != NULL);
        property->addValue(propVal);

        if (valEnd >= l-1)
        {
            wxASSERT(property != NULL);
            properties.Add(property);
            return true;
        }

        valStart = node.find("[", valEnd+1);
        // wxPrintf("valStart: %u\n", valStart);
        if (valStart == wxString::npos)
        {
            LOG_SGF(wxString::Format(_T("Property value start missing in %s"), node.c_str()));
            return false;
        }
        if (valStart != valEnd + 1)
        {
            newProp = true;
            propStart = valEnd + 1;
            next_nonspace(node, propStart);
            wxASSERT(property != NULL);
            properties.Add(property);
        }

        size_t newValEnd = valEnd;
        findNextUnescapedValEnd(newValEnd, node);
        if (newValEnd == wxString::npos)
        {
            LOG_SGF(wxString::Format(_T("Property value end missing in %s"), node.c_str()));
            return false;
        }
        valEnd = newValEnd;
    }
}

void Node::next_nonspace(const wxString &node, size_t &i)
{
    while (node.GetChar(i) == ' ' || node.GetChar(i) == '\n' || node.GetChar(i) == '\t')
        i++;
}

void Node::findNextUnescapedValEnd(size_t &t, const wxString &node)
{
    do
    {
        t = node.find("]", t+1);
        if (t == wxString::npos)
            break;
    } while (t > 0 && node.GetChar(t-1) == '\\');
}

#ifdef __WXDEBUG__
void Node::printMe() const
{
    if (!properties.IsEmpty())
        for (size_t i=0, sz = properties.GetCount(); i<sz; i++)
            properties.Item(i)->printMe();
}
#endif
