/*
 *  Tree.java
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
package ggo;

import ggo.*;
import java.util.*;
import java.io.Serializable;

/**
 *  Tree storing all moves of one game.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/10/05 16:03:56 $
 */
public class Tree implements Defines, Serializable {
    //{{{ private members
    private Move root;
    private transient Move current;
    private transient boolean varCreatedFlag = false;
    //}}}

    //{{{ Tree constructor
    /**
     *  Constructor for the Tree object
     *
     *@param  size  Description of the Parameter
     */
    public Tree(int size) {
        root = new Move(size);
        current = root;
    } //}}}

    //{{{ Getter & Setter

    //{{{ getRoot() method
    /**
     *  Gets the root node
     *
     *@return    The root node
     */
    public Move getRoot() {
        return root;
    } //}}}

    //{{{ setRoot() method
    /**
     *  Sets the root node
     *
     *@param  r  The new root node
     */
    public void setRoot(Move r) {
        root = r;
    } //}}}

    //{{{ getCurrent() method
    /**
     *  Gets the current node
     *
     *@return    The current node
     */
    public Move getCurrent() {
        return current;
    } //}}}

    //{{{ setCurrent() method
    /**
     *  Sets the current node
     *
     *@param  c  The new current node
     */
    public void setCurrent(Move c) {
        current = c;
    } //}}}

    //}}}

    //{{{ init() method
    /**
     *  Init the tree for a given board size.
     *
     *@param  size  Board size
     */
    public void init(int size) {
        clear();
        root = new Move(size);
        current = root;
    } //}}}

    //{{{ clear() method
    /**  Clear tree, remove all nodes. */
    public void clear() {
        if (root == null)
            return;

        traverseClear(root);

        root = null;
        current = null;
    } //}}}

    //{{{ traverseClear() method
    /**
     *  Traverse tree and delete nodes
     *
     *@param  m  Starting node for traversing
     */
    public static void traverseClear(Move m) {
        if (m == null)
            return;

        Stack stack = new Stack();
        Stack trash = new Stack();

        Move t = null;

        // Traverse the tree and drop every node into stack trash
        stack.push(m);

        while (!stack.isEmpty()) {
            t = (Move)stack.pop();
            if (t != null) {
                trash.push(t);
                stack.push(t.brother);
                stack.push(t.son);
            }
        }

        // Clearing this stack deletes all moves. Smart, eh?
        trash.clear();
    } //}}}

    //{{{ count() method
    /**
     *  Returns the size of this tree, i.e. the number of all moves.
     *
     *@return    Size of the tree
     */
    public int count() {
        if (root == null)
            return 0;

        Stack stack = new Stack();
        int counter = 0;
        Move t = null;
        // Traverse the tree and count all moves
        stack.push(root);
        while (!stack.isEmpty()) {
            t = (Move)stack.pop();
            if (t != null) {
                counter++;
                stack.push(t.brother);
                stack.push(t.son);
            }
        }
        return counter;
    } //}}}

    //{{{ addBrother() method
    /**
     *  Add a move as brother of current node of this tree
     *
     *@param  node  Move to add as brother
     *@return       True if adding successful, else false
     */
    public boolean addBrother(Move node) {
        if (root == null) {
            System.err.println("Error: No root!");
            return false;
        }
        else {
            if (current == null) {
                System.err.println("Error: No current node!");
                return false;
            }

            Move tmp = current;

            // Find brother farest right
            while (tmp.brother != null)
                tmp = tmp.brother;

            tmp.brother = node;
            node.parent = current.parent;
        }

        current = node;

        return true;
    } //}}}

    //{{{ addSon() method
    /**
     *  Add a move as son to the current move of the tree
     *
     *@param  node  Move to add as son
     *@return       True - A son found, added as brother. False - No son found,
     *              added as first son
     */
    public boolean addSon(Move node) {
        if (current == null) {
            System.err.println("Error: No current node!");
            return false;
        }

        // current node has no son?
        if (current.son == null) {
            current.son = node;
            node.parent = current;
            current = node;
            return false;
        }
        // A son found. Add the new node as farest right brother of that son
        else {
            current = current.son;
            varCreatedFlag = true;
            if (!addBrother(node)) {
                System.err.println("Failed to add a brother.");
                return false;
            }
            current = node;
            return true;
        }
    } //}}}

    //{{{ setSon() method
    /**
     *  Set a move as the current nodes son. Overwrite existing son if needed.
     *
     *@param  node  The son node
     *@return       True if successful, false if no current node exists
     */
    public boolean setSon(Move node) {
        if (current == null) {
            System.err.println("Error: No current node!");
            return false;
        }

        // current node has no son?
        if (current.son != null)
            System.err.println("Oops, this node already has a son!");

        current.son = node;
        node.parent = current;
        current = node;
        return true;
    } //}}}

    //{{{ getNumBrothers() method
    /**
     *  Returns the number of brothers of the current move
     *
     *@return    Number of brothers
     */
    public int getNumBrothers() {
        if (current == null || current.parent == null)
            return 0;

        Move tmp = null;
        int counter = 0;
        try {
            tmp = current.parent.son;

            while ((tmp = tmp.brother) != null)
                counter++;

        } catch (NullPointerException e) {
            System.err.println("Tree.getNumBrothers(): " + e);
            return 0;
        }

        return counter;
    } //}}}

    //{{{ getNumSons() methods
    /**
     *  Returns the number of sons of the current move
     *
     *@return    Number of sons
     */
    public int getNumSons() {
        return getNumSons(null);
    }

    /**
     *  Returns the number of sons of the given move
     *
     *@param  m  Move to check
     *@return    Number of sons
     */
    public int getNumSons(Move m) {
        if (m == null) {
            if (current == null)
                return 0;

            m = current;
        }

        Move tmp = null;
        int counter = 1;

        try {
            tmp = m.son;

            if (tmp == null)
                return 0;

            while ((tmp = tmp.brother) != null)
                counter++;

        } catch (NullPointerException e) {
            System.err.println("Tree.getNumSons(): " + e);
        }

        return counter;
    } //}}}

    //{{{ Navigation

    //{{{ nextMove() method
    /**
     *  Set current node to next node
     *
     *@return    New current node
     */
    public Move nextMove() {
        if (root == null || current == null || current.son == null)
            return null;

        if (current.marker == null) // No marker, simply take the main son
            current = current.son;

        else
            current = current.marker;
        // Marker set, use this to go the remembered path in the tree

        current.parent.marker = current; // Parents remembers this move we went to
        return current;
    } //}}}

    //{{{ previousMove() method
    /**
     *  Set current node to previous node
     *
     *@return    New current node
     */
    public Move previousMove() {
        if (root == null || current == null || current.parent == null)
            return null;

        current.parent.marker = current; // Remember the son we came from
        current = current.parent; // Move up in the tree
        return current;
    } //}}}

    //{{{ nextVariation() method
    /**
     *  Set current node to next variation
     *
     *@return    New current node
     */
    public Move nextVariation() {
        if (root == null || current == null || current.brother == null)
            return null;

        current = current.brother;
        return current;
    } //}}}

    //{{{ previousVariation() method
    /**
     *  Set current node to previous variation
     *
     *@return    New current node
     */
    public Move previousVariation() {
        if (root == null || current == null)
            return null;

        Move tmp;

        Move old;

        if (current.parent == null) {
            if (current == root)
                return null;
            else
                tmp = root;

        }
        else
            tmp = current.parent.son;

        old = tmp;

        while ((tmp = tmp.brother) != null) {
            if (tmp == current)
                return current = old;
            old = tmp;
        }

        return null;
    } //}}}

    //}}}

    //{{{ hasParent() method
    /**
     *  Check if current node has a parent
     *
     *@return    True if current node has parent, else false
     */
    public boolean hasParent() {
        return (current != null && current.parent != null);
    } //}}}

    //{{{ hasSon() method
    /**
     *  Check if a given move has one or more sons
     *
     *@param  m  Move to check
     *@return    True if move has sons, else false
     */
    public boolean hasSon(Move m) {
        if (root == null || m == null || current == null || current.son == null)
            return false;

        Move tmp = current.son;

        do {
            if (m.equals(tmp)) {
                current = tmp;
                return true;
            }
        } while ((tmp = tmp.brother) != null);

        return false;
    } //}}}

    //{{{ hasNextBrother() method
    /**
     *  Check if the current move has a next brother
     *
     *@return    True if current move has a next brother, else false
     */
    public boolean hasNextBrother() {
        if (current == null || current.brother == null)
            return false;

        return current.brother != null;
    } //}}}

    //{{{ hasPrevBrother() methods
    /**
     *  Check if the current move has a previous brother
     *
     *@return    True if current move has a previous brother, else false
     */
    public boolean hasPrevBrother() {
        return hasPrevBrother(current);
    }

    /**
     *  Check if a given move has a previous brother
     *
     *@param  m  Move to check
     *@return    True if the given move has a previous brother, else false
     */
    public boolean hasPrevBrother(Move m) {
        if (root == null || m == null)
            return false;

        Move tmp;

        if (m.parent == null) {
            if (m == root)
                return false;
            else
                tmp = root;
        }
        else
            tmp = m.parent.son;

        return tmp != m;
    } //}}}

    //{{{ setToFirstMove() method
    /**  Sets the current move as root */
    public void setToFirstMove() {
        if (root == null) {
            System.err.println("Error: No root!");
            return;
        }
        current = root;
    } //}}}

    //{{{ mainBranchSize() method
    /**
     *  Calculate length of main branch
     *
     *@return    Length of main branch
     */
    public int mainBranchSize() {
        if (root == null || current == null || current.son == null)
            return 0;

        Move tmp = root;

        int counter = 1;
        while ((tmp = tmp.son) != null)
            counter++;

        return counter;
    } //}}}

    //{{{ getBranchLength() method
    /**
     *  Calculate length of current branch
     *
     *@return    Length of current branch
     */
    public int getBranchLength() {
        Move tmp = current;

        int counter = 0;
        // Go down the current branch, use the marker if possible to remember a previously used path
        while ((tmp = (tmp.marker != null ? tmp.marker : tmp.son)) != null)
            counter++;

        return counter;
    } //}}}

    //{{{ getVarCreatedFlag() method
    /**
     *  Gets the varCreatedFlag attribute of the Tree object
     *
     *@return    The varCreatedFlag value
     */
    public boolean getVarCreatedFlag() {
        boolean b = varCreatedFlag;
        varCreatedFlag = false;
        return b;
    } //}}}

    //{{{ Searching

    //{{{ findMoveInMainBranch() method
    /**
     *  Find a move in main branch
     *
     *@param  x  X position of the move
     *@param  y  Y position of the move
     *@return    Found move. Null if none was found.
     */
    public Move findMoveInMainBranch(int x, int y) {
        return findMove(root, x, y, false);
    } //}}}

    //{{{ findMoveInBranch() method
    /**
     *  Find a move in current branch branch
     *
     *@param  x  X position of the move
     *@param  y  Y position of the move
     *@return    Found move. Null if none was found.
     */
    public Move findMoveInBranch(int x, int y) {
        return findMove(current, x, y, true);
    } //}}}

    //{{{ findMove() method
    /**
     *  Find a move in the tree
     *
     *@param  start        Start node for search
     *@param  x            X position of the move
     *@param  y            Y position of the move
     *@param  checkmarker  If true, check navigation marks. If false, ignore them.
     *@return              Found move. Null if none was found.
     */
    private Move findMove(Move start, int x, int y, boolean checkmarker) {
        if (start == null)
            return null;

        Move t = start;

        do {
            if (t.getX() == x && t.getY() == y)
                return t;
            if (checkmarker && t.marker != null)
                t = t.marker;
            else
                t = t.son;
        } while (t != null);

        return null;
    } //}}}

    //}}}
}

