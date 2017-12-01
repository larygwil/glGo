/*
 * node.h
 *
 * $Id: node.h,v 1.9 2003/10/04 19:06:05 peter Exp $
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

#ifndef NODE_H
#define NODE_H

#ifdef __GNUG__
#pragma interface "node.h"
#endif


// -----------------------------------------------------------------------
//                             Class Property
// -----------------------------------------------------------------------

/**
 * A property contains one identifier and a list of values.
 * A property is part of a Node and defined as:
 * @verbatim Property = PropIdent PropValue { PropValue } @endverbatim
 * The Property class contains one string for PropIdent and an array of strings
 * for the PropValues.
 * Example: "B[aa]" has the PropIdent "B" and one value "aa".
 * @ingroup sgf
 * @see Node
 */
class Property
{
public:
    /**
     * Constructor.
     * @param id Identifier
     */
    Property(const wxString &id);

    /** Gets the PropIdent string. */
    const wxString& getIdent() const { return ident; }

    /** Gets the list of PropValue strings. */
    const wxArrayString& getValues() const { return values; }

    /** Adds one PropValue string to the list. */
    void addValue(wxString &value);

#ifdef __WXDEBUG__
    /** Print this property. For debugging only. */
    void printMe() const;
#endif

private:
    wxString ident;        ///< PropIdent
    wxArrayString values;  ///< PropValue array
};

WX_DEFINE_ARRAY(Property*, Properties);


// -----------------------------------------------------------------------
//                              Class Node
// -----------------------------------------------------------------------

/**
 * A SGF Node contains a list of properties.
 * A node is defined as:
 * @verbatim  Node = ";" { Property } @endverbatim
 * This class contains pointers to a son and a brother Node to model the SGF
 * tree. It contains an array of Property objects. The Node class is
 * responsible for parsing the SGF node text.
 * Example: ";B[aa]C[Great move!]" would be one node with two properties.
 * Some of the parsing is already done in this class, splitting the node text
 * into properties and preparing for the final parsing step.
 * @ingroup sgf
 * @see Property
 * @todo Sort property so moves (B/W) are in front of AB/AW in the list, else
 *       SGFParser::parseSGFProperty will do mess. Most SGF files dont do this, but
 *       you never know...
 */
class Node
{
public:
    /**
     * Constructor.
     * @param txt Complete SGF plaintext of this node, which will be parsed.
     */
    Node(const wxString &txt);

    /**
     * Destructor.
     * Takes care of deleting the properties array.
     */
    ~Node();

    /** Gets the list of properties belonging to this node. */
    const Properties& getProperties() const { return properties; }

#ifdef __WXDEBUG__
    /** Prints this Node. For debugging only. */
    void printMe() const;
#endif

    Node *brother;          ///< Pointer to the next brother node
    Node *son;              ///< Pointer to the son node
    unsigned int number;    ///< Current move number
    static size_t counter;  ///< Static counter, shows number of total nodes

private:
    bool parseNode(const wxString &node);
    void next_nonspace(const wxString &node, size_t &i);
    void findNextUnescapedValEnd(size_t &t, const wxString &node);

    Properties properties;
};

#endif
