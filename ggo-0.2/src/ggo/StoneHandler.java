/*
 *  StoneHandler.java
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

import java.util.*;
import ggo.*;
import ggo.utils.*;
import ggo.gui.marks.*;

/**
 *  Class to handle calculations and data management for single stones or groups
 *  of stones on the board.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.5 $, $Date: 2002/10/17 19:04:41 $
 */
public class StoneHandler implements Defines {
    //{{{ private members
    private BoardHandler boardHandler;
    private Hashtable stones, stonesMarkedDead, stonesMarkedSeki;
    private ArrayList groups;
    private boolean workingOnNewMove;
    //}}}

    //{{{ StoneHandler constructor
    /**
     *  Constructor for the StoneHandler object
     *
     *@param  bh  Pointer to the BoardHandler object this object is attached to
     */
    public StoneHandler(BoardHandler bh) {
        boardHandler = bh;

        stones = new Hashtable(20);
        groups = new ArrayList();
        stonesMarkedDead = new Hashtable();
        stonesMarkedSeki = new Hashtable();
    } //}}}

    //{{{ getStones() method
    /**
     *  Returns the Hashtable with all stones
     *
     *@return    The stones hashtable
     */
    public Hashtable getStones() {
        return stones;
    } //}}}

    //{{{ getStone(int, int) method
    /**
     *  Gets a stone from the hashtable, identified by the coordinates
     *
     *@param  x  X position of the stone
     *@param  y  Y position of the stone
     *@return    The stone object
     */
    public Stone getStone(int x, int y) {
        return (Stone)stones.get(Utils.coordsToKey(x, y));
    } //}}}

    //{{{ toggleWorking() method
    /**
     *  Toggle liberty checking to working mode
     *
     *@param  w  Working mode on or off
     */
    public void toggleWorking(boolean w) {
        workingOnNewMove = w;
    } //}}}

    //{{{ clearData() method
    /**  Clears all data of this class, preparing for a new empty game */
    public void clearData() {
        stones.clear();
        groups.clear();
        stonesMarkedDead.clear();
        stonesMarkedSeki.clear();
    } //}}}

    //{{{ hasStone() method
    /**
     *  Check if the a stone exists on this position
     *
     *@param  x  X position of the stone
     *@param  y  Y position of the stone
     *@return    True if a stone was found, else false
     */
    public boolean hasStone(int x, int y) {
        return stones.containsKey(Utils.coordsToKey(x, y));
    } //}}}

    //{{{ addStone() method
    /**
     *  Adds a stone to the collection
     *
     *@param  stone    The stone to be added
     *@param  doCheck  If true, the position is checked for captures
     *@param  toDraw   If true, the stone graphics will be drawn on the board
     *@return          True if successful, else false
     */
    public boolean addStone(Stone stone, boolean doCheck, boolean toDraw) {
        stones.put(Utils.coordsToKey(stone.getX(), stone.getY()), stone);

        if (doCheck)
            return checkPosition(stone, toDraw);

        return true;
    } //}}}

    //{{{ removeStone() methods

    //{{{ removeStone(int, int, boolean) method
    /**
     *  Remove a stone from the hashtable. Does not remove the stone from the
     *  groups.
     *
     *@param  x       X position of the stone
     *@param  y       Y position of the stone
     *@param  toDraw  If true, the stone graphics will be removed from the board
     *@return         True if success, false if failure
     */
    public boolean removeStone(int x, int y, boolean toDraw) {
        if (!hasStone(x, y))
            return false;

        if (stones.remove(Utils.coordsToKey(x, y)) == null)
            return false;

        if (toDraw)
            boardHandler.getBoard().removeStoneSprite(null, x, y);

        return true;
    } //}}}

    //{{{ removeStone(int, int) method
    /**
     *  Remove a stone from the hashtable. Does not remove the stone from the
     *  groups. The stone graphics will be removed from the board.
     *
     *@param  x  X position of the stone
     *@param  y  Y position of the stone
     *@return    True if success, false if failure
     */
    public boolean removeStone(int x, int y) {
        return removeStone(x, y, true);
    } //}}}

    //{{{ removeStone(Stone) method
    /**
     *  Remove a stone from the hashtable. Does remove the stone from the
     *  groups.
     *
     *@param  stone   Stone to remove
     *@param  toDraw  If true, stone graphics will be removed from the board
     *@return         True if success, false if failure
     */
    public boolean removeStone(Stone stone, boolean toDraw) {
        if (!removeStone(stone.getX(), stone.getY(), toDraw))
            return false;

        // Look if one group has this stone, and remove it from the group.
        for (Iterator it = groups.iterator(); it.hasNext(); ) {
            Group g = (Group)it.next();
            if (g.containsStone(stone)) {
                g.remove(stone);
                break;
            }
        }

        return true;
    } //}}}

    //}}}

    //{{{ checkPosition() method
    /**
     *  Check the position of a stone for captures.
     *
     *@param  stone   Stone to check
     *@param  toDraw  If true, a removed group graphics will be erased from the
     *      board
     *@return         Returns false if it was a suicide move, else true
     */
    private boolean checkPosition(Stone stone, boolean toDraw) {
        return checkGroupLiberties(checkGroup(stone), stone, toDraw);
    } //}}}

    //{{{ checkGroup() method
    /**
     *  Description of the Method
     *
     *@param  stone  Description of the Parameter
     *@return        Description of the Return Value
     */
    private Group checkGroup(Stone stone) {
        Group active = null;

        // No groups existing? Create one.
        if (groups.isEmpty()) {
            Group g = assembleGroup(stone);
            groups.add(g);
            active = g;

            // System.err.println("No groups. Created one.");
        }
        // We already have one or more groups.
        else {
            boolean flag = false;
            Group tmp;

            // System.err.println("Already have some groups.");

            for (int i = 0; i < groups.size(); i++) {
                tmp = (Group)groups.get(i);

                // Check if the added stone is attached to an existing group.
                // If yes, update this group and replace the old one.
                // If the stone is attached to two groups, remove the second group.
                // This happens if the added stone connects two groups.
                if (tmp.isAttachedTo(stone)) {
                    // Group attached to stone
                    if (!flag) {
                        // System.err.println("Group attached to stone");
                        groups.remove(i);
                        active = assembleGroup(stone);
                        groups.add(i, active);
                        flag = true;
                    }
                    // Groups connected, remove one
                    else {
                        // System.err.println("Groups connected, remove one");
                        if (active != null && active == groups.get(i))
                            active = tmp;

                        groups.remove(i);
                        i--;
                    }
                }
            }

            // The added stone isnt attached to an existing group. Create a new group.
            if (!flag) {
                // System.err.println("The added stone isnt attached to an existing group. Create a new group.");
                Group g = assembleGroup(stone);
                groups.add(g);
                active = g;
            }
        }

        // System.err.println("Number of groups = " + groups.size());
        // System.err.println("ACTIVE " + active);

        return active;
    } //}}}

    //{{{ checkGroupLiberties() method
    /**
     *  Description of the Method
     *
     *@param  active  Description of the Parameter
     *@param  stone   Description of the Parameter
     *@param  toDraw  Description of the Parameter
     *@return         Description of the Return Value
     */
    private boolean checkGroupLiberties(Group active, Stone stone, boolean toDraw) {
        // Now we have to sort the active group as last in the groups ArrayList,
        // so if this one is out of liberties, we beep and abort the operation.
        // This prevents suicide moves.
        groups.remove(active);
        groups.add(active);

        // Check the liberties of every group. If a group has zero liberties, remove it.
        for (int i = 0; i < groups.size(); i++) {
            Group tmp = (Group)groups.get(i);

            tmp.setLiberties(countLiberties(tmp));

            // System.err.println("Group #" + i + " with " + tmp.getLiberties() + " liberties:");
            // System.err.println(tmp);

            // Oops, zero liberties
            if (tmp.getLiberties() == 0) {
                // Suicide move?
                if (tmp == active) {
                    if (active.size() == 1)
                        groups.remove(i);
                    removeStone(stone.getX(), stone.getY(), toDraw);
                    return false;
                }

                int stoneCounter = 0;

                // Erase the stones of this group from the stones table.
                for (Iterator it = tmp.iterator(); it.hasNext(); ) {
                    Stone s = (Stone)it.next();
                    if (workingOnNewMove)
                        boardHandler.updateCurrentMatrix(STONE_NONE, s.getX(), s.getY());

                    // Calling this method because no need to look through the groups here
                    removeStone(s.getX(), s.getY(), toDraw);
                    stoneCounter++;
                }

                // Remove the group from the groups list.
                // System.err.println("Oops, a group got killed. Removing killed group #" + i);

                if (tmp == active)
                    active = null;

                groups.remove(i);
                i--;

                // Tell the boardhandler about the captures
                boardHandler.setCaptures(stone.getColor(), stoneCounter);
            }
        }

        return true;
    } //}}}

    //{{{ assembleGroup() method
    /**
     *  Assemble a group of this stone and his neighbours
     *
     *@param  stone  Stone to check
     *@return        The assembled group
     */
    private Group assembleGroup(Stone stone) {
        // Oops
        if (stones.isEmpty()) {
            System.err.println("StoneHandler.assembleGroup - No stones on the board!");
            gGo.exitApp(1);
        }

        // System.err.println("assembleGroup: Checking stone " + stone);

        Group group = new Group();
        group.add(stone);

        int mark = 0;

        // Walk through the horizontal and vertical directions and assemble the
        // attached stones to this group.
        while (mark < group.size()) {
            stone = (Stone)group.get(mark);

            int stoneX = stone.getX();
            int stoneY = stone.getY();
            int col = stone.getColor();

            // North
            group = checkNeighbour(stoneX, stoneY - 1, col, group);

            // West
            group = checkNeighbour(stoneX - 1, stoneY, col, group);

            // South
            group = checkNeighbour(stoneX, stoneY + 1, col, group);

            // East
            group = checkNeighbour(stoneX + 1, stoneY, col, group);

            mark++;
        }

        return group;
    } //}}}

    //{{{ checkNeighbour() method
    /**
     *  Check if a stone at this position belongs to the group given as
     *  parameter. If yes, that stone is added.
     *
     *@param  x      X position to check
     *@param  y      Y position to check
     *@param  color  color of the group
     *@param  group  The group the stone has to be added to if it exists
     *@return        The group with or without the added stone
     */
    private Group checkNeighbour(int x, int y, int color, Group group) {
        Stone tmp = getStone(x, y);

        if (tmp != null && tmp.getColor() == color) {
            if (!group.contains(tmp)) {
                group.add(tmp);
                tmp.setChecked(true);
                // System.err.println("checkNeighbour: Added stone " + tmp);
            }
        }
        return group;
    } //}}}

    //{{{ countLiberties() method
    /**
     *  Count the liberties of a group
     *
     *@param  group  Group to count
     *@return        Number of liberties
     */
    private int countLiberties(Group group) {
        int liberties = 0;
        ArrayList libCounted = new ArrayList();

        // Walk through the horizontal and vertial directions, counting the
        // liberties of this group.
        for (int i = 0; i < group.size(); i++) {
            Stone tmp = (Stone)group.get(i);

            int x = tmp.getX();
            int y = tmp.getY();

            // North
            liberties = checkNeighbourLiberty(x, y - 1, libCounted, liberties);

            // West
            liberties = checkNeighbourLiberty(x - 1, y, libCounted, liberties);

            // South
            liberties = checkNeighbourLiberty(x, y + 1, libCounted, liberties);

            // East
            liberties = checkNeighbourLiberty(x + 1, y, libCounted, liberties);
        }
        return liberties;
    } //}}}

    //{{{ checkNeighbourLiberty() method
    /**
     *  Check if there is a liberty at this position
     *
     *@param  x           X position to check
     *@param  y           Y position to check
     *@param  libCounted  a vector with the already counted liberties, to
     *      prevent double-check
     *@param  liberties   liberties already counted
     *@return             new liberties value
     */
    private int checkNeighbourLiberty(int x, int y, ArrayList libCounted, int liberties) {
        if (x == 0 || y == 0)
            return liberties;

        if (x <= boardHandler.getBoard().getBoardSize() && y <= boardHandler.getBoard().getBoardSize() &&
                x >= 0 && y >= 0 &&
                !libCounted.contains(Utils.coordsToKey(x, y)) &&
                getStone(x, y) == null) {
            libCounted.add(Utils.coordsToKey(x, y));
            liberties++;
        }

        /*
         *  System.err.println("checkNeighbourLiberty at: " +
         *  x + "/" + y + ": " +
         *  liberties + " liberties, " +
         *  libCounted.size() + " counted");
         */
        return liberties;
    } //}}}

    //{{{ updateAll() methods
    /**
     *  Synchronize the matrix with the stonehandler data and update the canvas.
     *  This is usually called when navigating through the tree.
     *
     *@param  m               Matrix to synchronize with
     *@param  completeUpdate  If true, the stones hashtable will be redone, and
     *                        the board has to repaint everything. If false, this function will
     *                        draw or remove the stone graphics and the board has not to repaint.
     *@param  sgf             Set to true when reading from sgf, so mark drawing is skipped
     *@see                    #updateAll(Matrix, boolean)
     */
    public void updateAll(Matrix m, boolean completeUpdate, boolean sgf) {
        if (completeUpdate) {
            // Tabula rasa. We create the stone objects from scratch. Too much changed, better redraw all once.
            // Also called this way when parsing sgf files from SGFParser, to avoid drawing.
            stones.clear();
            groups.clear();
        }

        short data;
        int boardSize = boardHandler.getBoard().getBoardSize();
        Stone stone = null;

        ArrayList stonesToAdd = null;
        ArrayList stonesToRemove = null;
        ArrayList deadStones = null;
        if (!completeUpdate) {
            stonesToAdd = new ArrayList();
            stonesToRemove = new ArrayList();
            deadStones = new ArrayList();
        }
        MarkHandler mh = boardHandler.getBoard().getMarkHandler();
        mh.clear();

        //{{{ Loop through matrix
        for (int y = 1; y <= boardSize; y++) {
            for (int x = 1; x <= boardSize; x++) {
                // Extract the data for the stone from the matrix
                data = (short)Math.abs(m.at(x - 1, y - 1) % 10);
                switch (data) {
                    case STONE_BLACK:
                        if (completeUpdate) {
                            Stone s = new Stone(STONE_BLACK, x, y);
                            stones.put(Utils.coordsToKey(x, y), s);
                            checkGroup(s);
                        }
                        else {
                            if ((stone = getStone(x, y)) == null) {
                                Stone s = new Stone(STONE_BLACK, x, y);
                                stonesToAdd.add(s);
                                addStone(s, false, true);
                            }
                            else if (stone.getColor() == STONE_WHITE) {
                                stonesToRemove.add(stone);
                                stone.setColor(STONE_BLACK);
                                stonesToAdd.add(stone);
                            }
                        }
                        break;
                    case STONE_WHITE:
                        if (completeUpdate) {
                            Stone s = new Stone(STONE_WHITE, x, y);
                            stones.put(Utils.coordsToKey(x, y), s);
                            checkGroup(s);
                        }
                        else {
                            if ((stone = getStone(x, y)) == null) {
                                Stone s = new Stone(STONE_WHITE, x, y);
                                stonesToAdd.add(s);
                                addStone(s, false, true);
                            }
                            else if (stone.getColor() == STONE_BLACK) {
                                stonesToRemove.add(stone);
                                stone.setColor(STONE_WHITE);
                                stonesToAdd.add(stone);
                            }
                        }
                        break;
                    case STONE_NONE:
                    case STONE_ERASE:
                        if (!completeUpdate && hasStone(x, y)) {
                            Stone s = getStone(x, y);
                            stonesToRemove.add(s);
                            removeStone(s, false);
                        }
                        break;
                    default:
                        System.err.println("Bad matrix data <" + data + "> at " +
                                x + "/" + y + " in StoneHandler.updateAll !");
                }

                // Skip marks when reading sgf
                if (sgf)
                    continue;

                // Extract the mark data from the matrix
                data = (short)(Math.abs(m.at(x - 1, y - 1) / 10));
                switch (data) {
                    case MARK_SQUARE:
                        // System.err.println("Found a square mark at " + x + "/" + y);
                        mh.addMark(new MarkSquare(x, y));
                        break;
                    case MARK_CIRCLE:
                        // System.err.println("Found a circle mark at " + x + "/" + y);
                        mh.addMark(new MarkCircle(x, y));
                        break;
                    case MARK_TRIANGLE:
                        // System.err.println("Found a triangle mark at " + x + "/" + y);
                        mh.addMark(new MarkTriangle(x, y));
                        break;
                    case MARK_CROSS:
                        // System.err.println("Found a cross mark at " + x + "/" + y);
                        mh.addMark(new MarkCross(x, y));
                        break;
                    case MARK_TEXT:
                    {
                        // System.err.println("Found a text mark at " + x + "/" + y + ", txt = " + m.getMarkText(x, y));
                        String txt = m.getMarkText(x, y);

                        // No mark text found, create increasing letter
                        if (txt == null) {
                            txt = boardHandler.getBoard().getMarkHandler().getNextLetter();
                            m.setMarkText(x, y, txt);
                        }
                        mh.addMark(new MarkText(x, y, txt));
                    }
                        break;
                    case MARK_NUMBER:
                        // System.err.println("Found a number mark at " + x + "/" + y + "num = " + m.getMarkText(x, y));
                        mh.addMark(new MarkNumber(x, y, m.getMarkText(x, y)));
                        break;
                    case MARK_TERR_BLACK:
                        // System.err.println("Found a black territory mark at " + x + "/" + y);
                        mh.addMark(new MarkTerr(x, y, STONE_BLACK));
                        if (hasStone(x, y)) {
                            getStone(x, y).setDead(true);
                            if (!completeUpdate && !stonesToAdd.contains(getStone(x, y)))
                                deadStones.add(getStone(x, y));
                        }
                        break;
                    case MARK_TERR_WHITE:
                        // System.err.println("Found a white territory mark at " + x + "/" + y);
                        mh.addMark(new MarkTerr(x, y, STONE_WHITE));
                        if (hasStone(x, y)) {
                            getStone(x, y).setDead(true);
                            if (!completeUpdate && !stonesToAdd.contains(getStone(x, y)))
                                deadStones.add(getStone(x, y));
                        }
                        break;
                }
            }
        } //}}}

        // Restore dead and seki marks, if we have any
        if (boardHandler.getTree().getCurrent().isScored()) {
            if (completeUpdate && !stonesMarkedDead.isEmpty()) {
                for (Enumeration e = stonesMarkedDead.elements(); e.hasMoreElements(); ) {
                    Stone tmp = (Stone)e.nextElement();
                    // Some security checks for IGS score mode.
                    if (tmp == null)
                        continue;
                    if (hasStone(tmp.getX(), tmp.getY()))
                        getStone(tmp.getX(), tmp.getY()).setDead(true);
                    else
                        tmp.setDead(true);
                }
            }
            else if (!completeUpdate && deadStones != null && !deadStones.isEmpty()) {
                stonesToAdd.addAll(deadStones);
                boardHandler.getBoard().removeStoneSprites(deadStones);
            }

            if (completeUpdate && !stonesMarkedSeki.isEmpty()) {
                for (Enumeration e = stonesMarkedSeki.elements(); e.hasMoreElements(); ) {
                    Stone tmp = (Stone)e.nextElement();
                    if (tmp == null)
                        continue;
                    if (hasStone(tmp.getX(), tmp.getY()))
                        getStone(tmp.getX(), tmp.getY()).setSeki(true);
                    else
                        tmp.setSeki(true);
                }
            }
        }

        // Finally draw it on the board
        if (!completeUpdate) {
            // Draw stones and marks on board in one step. Not needed if we do complete update.
            boardHandler.getBoard().removeStoneSprites(stonesToRemove);
            boardHandler.getBoard().addStoneSprites(stonesToAdd);
            boardHandler.getBoard().drawMarks();
        }
    }

    /**
     *  Synchronize the matrix with the stonehandler data and update the canvas.
     *  This is usually called when navigating through the tree.
     *
     *@param  m               Matrix to synchronize with
     *@param  completeUpdate  If true, the stones hashtable will be redone, and
     *        the board has to repaint everything. If false, this function will
     *        draw or remove the stone graphics and the board has not to repaint.
     *@see                    #updateAll(Matrix, boolean, boolean)
     */
    public void updateAll(Matrix m, boolean completeUpdate) {
        updateAll(m, completeUpdate, false);
    } //}}}

    //{{{ checkAllPositions() method
    /**  Description of the Method */
    public void checkAllPositions() {
        // Traverse all stones and check their positions.
        // To help performance, we don't check already checked stones again.
        // Called when jumping through the variations.

        groups.clear();

        for (Enumeration e = stones.elements(); e.hasMoreElements(); ) {
            Stone s = (Stone)e.nextElement();
            s.setChecked(false);
        }

        for (Enumeration e = stones.elements(); e.hasMoreElements(); ) {
            Stone s = (Stone)e.nextElement();
            if (!s.isChecked())
                checkPosition(s, false);
        }
    } //}}}

    //{{{ Scoring

    //{{{ updateDeadMarks() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    protected long updateDeadMarks() {
        stonesMarkedDead.clear();
        stonesMarkedSeki.clear();

        int black = 0;
        int white = 0;
        for (Enumeration e = stones.elements(); e.hasMoreElements(); ) {
            Stone s = (Stone)e.nextElement();
            if (s.isDead()) {
                if (s.getColor() == STONE_BLACK)
                    white++;
                else
                    black++;
                stonesMarkedDead.put(Utils.coordsToKey(s.getX(), s.getY()), s);
            }
        }
        return black * 1000 + white;
    } //}}}

    //{{{ removeDeadGroup() method
    /**
     *  Description of the Method
     *
     *@param  x     Description of the Parameter
     *@param  y     Description of the Parameter
     *@param  data  Description of the Parameter
     *@return       Description of the Return Value
     */
    protected boolean removeDeadGroup(int x, int y, DeadGroupData data) {
        if (!hasStone(x, y))
            return false;

        Stone s = getStone(x, y);
        data.col = s.getColor();

        if (!s.isDead())
            data.dead = true;

        Group g = assembleGroup(s);

        data.caps = g.size();

        // Mark stones of this group as dead or alive again
        for (Iterator it = g.iterator(); it.hasNext(); ) {
            Stone tmp = (Stone)it.next();
            tmp.setDead(data.dead);
            if (data.dead)
                stonesMarkedDead.put(Utils.coordsToKey(tmp.getX(), tmp.getY()), tmp);
            else
                stonesMarkedDead.remove(Utils.coordsToKey(tmp.getX(), tmp.getY()));
        }

        return true;
    } //}}}

    //{{{ removeDeadMarks() method
    /**  Description of the Method */
    public void removeDeadMarks() {
        for (Enumeration e = stones.elements(); e.hasMoreElements(); ) {
            Stone s = (Stone)e.nextElement();
            if (s.isDead() || s.isSeki()) {
                s.setDead(false);
                s.setSeki(false);
            }
        }
        // Clear hashtables used for scoring
        stonesMarkedDead.clear();
        stonesMarkedSeki.clear();
    } //}}}

    //{{{ removeDeadGroupIGS() method
    /**
     *  Remove a dead stone and all attached stones. Called from IGS in score omde
     *
     *@param  x  X position
     *@param  y  Y position
     *@return    True if successful, else false
     */
    public DeadGroupData removeDeadGroupIGS(int x, int y) {
        if (!hasStone(x, y))
            return null;

        DeadGroupData data = new DeadGroupData();
        Stone s = getStone(x, y);
        data.col = s.getColor();
        Group g = assembleGroup(s);
        data.caps = g.size();

        for (Iterator it = g.iterator(); it.hasNext(); ) {
            Stone tmp = (Stone)it.next();
            tmp.setDead(true);
            stonesMarkedDead.put(Utils.coordsToKey(tmp.getX(), tmp.getY()), tmp);
        }

        return data;
    } //}}}

    //}}}
}

