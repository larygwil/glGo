/*
 * sgfparser.h
 *
 * $Id: sgfparser.h,v 1.16 2003/10/04 19:06:05 peter Exp $
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

#ifndef SGFPARSER_H
#define SGFPARSER_H

#ifdef __GNUG__
#pragma interface "sgfparser.h"
#endif

#include "defines.h"


/**
 * @defgroup sgf SGF
 *
 * This module is reponsible for parsing, loading and saving SGF files.
 *
 * The SGF parsing process is split into three classes and basically two steps.
 * First the SGFParser class loads and reads the plaintext SGF and loops through
 * it, creating a tree of Nodes from the text. Each node does some preprocessing
 * to store each property identifier and value into a list of Property objects.
 * In a second step the tree of Nodes is processed in pre-order form and
 * each of the properties within a node parsed and the result sent to the
 * BoardHandler which requested loading the file.
 *
 * SGF writing is implemented by the relatively simple SGFWriter class which
 * traverses the game tree in pre-order calling Move::MoveToSGF() in each node
 * which will convert the node into SGF output.
 *
 * The parser aims to be fully FF[4] compatible and follows the SGF
 * specifications as defined at http://www.red-bean.com/sgf/
 *
 * @{
 */

class Property;
class Node;
class BoardHandler;
class GameData;
#if defined(wxUSE_PROGRESSDLG) && !defined(SGF_CONSOLE)
class wxProgressDialog;
#endif

/**
 * This class handles parsing and loading of a SGF file.
 * First the SGF text is loaded into memory and a tree of Node objects
 * is internally created while looping through the SGF text.
 * The parsing of the internal node text happens in the Node class.
 * In a second step the tree of Nodes is traversed in pre-order form,
 * the content of each Node will be sent to the BoardHandler which
 * requested loading the file.
 * @see Node, Property
 */
class SGFParser
{
public:
    /**
     * Constructor.
     * @param handler Pointer to the BoardHandler which requests loading a file.
     * @param parent Parent window calling this class, used for the ProgressDialog
     */
    SGFParser(BoardHandler *handler, wxWindow *parent);

    /**
     * Destructor.
     * Takes care the tree of Nodes is deleted.
     */
    ~SGFParser();

    /**
     * Load a SGF file.
     * This is the main entry function for this class to load a SGF file, send
     * the moves to the BoardHandler given in the constructor and store the
     * SGF header data in the GameData object.
     * Loading the SGF is called in the BoardHandler with something like this:
     * @code
     * SGFParser parser(this, parent);
     * bool res = parser.loadSGF(filename, new GameData());
     * if (res)
     *     // Do something
     * @endcode
     * @param filename SGF File to load
     * @param game_data Pointer to the GameData object in which the SGF header data is saved.
     * @return True of loading was successfull, else false
     */
    bool loadSGF(const wxString &filename, GameData *game_data);

    /**
     * Gets the error message from last loading process.
     * Empty if there was no error or the user aborted loading.
     * @return Error message or wxEmptyString
     */
    const wxString& getSGFError() const { return sgf_error; }

private:
    bool loadFile(const wxString &filename, wxString &sgf);
    bool parseSGF(const wxString &sgf, Node *&root);
    bool parseSGFTree(Node *root);
    bool parseSGFNode(Node *node);
    /** @todo In german locale this creates invalid komi values. */
    void parseSGFProperty(Property *prop, bool &move_added);
    bool parseMove(const wxString &value, Color color);
    void parseMark(const wxString &id, const wxString &value);
    void deleteTree(Node *root);
    void findNext(unsigned long &start, const wxString &sgf, char c, bool start_flag=false);
    bool checkCommentOpenClose(const wxString sgf, const unsigned long &start,
                               const unsigned long &next, unsigned long &last);
    void findNextUnescapedChar(char c, unsigned long &t, const wxString &sgf);
    unsigned long minpos(const unsigned long &a, const unsigned long &b, const unsigned long &c);

    wxString sgf_error;
    BoardHandler *bh;
    GameData *data;
    wxWindow *parent;
#if defined(wxUSE_PROGRESSDLG) && !defined(SGF_CONSOLE)
    wxProgressDialog *progDlg;
#endif
};

/** @} */  // End of group

#endif
