/*
 *  BoardHandler.java
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
import ggo.gui.SideBar;
import ggo.utils.*;
import ggo.utils.sound.SoundHandler;
import ggo.parser.*;
import ggo.gui.*;
import java.io.File;
import java.util.*;

/**
 *  Class to handle the positions on the board. Do everything concerning the
 *  whole board position that is not GUI related, this is done in the Board
 *  class. Thing related to single stones or groups of stones is done in the
 *  StoneHandler class.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.14 $, $Date: 2002/10/24 13:00:17 $
 */
public class BoardHandler implements Defines {
    //{{{ private members
    private Board board;
    private StoneHandler stoneHandler;
    private GameData gameData;
    private int currentMove, gameMode, capturesBlack, capturesWhite, caps_black, caps_white;
    private boolean markedDead, terrContinueFlag;
    private Tree tree;
    private Move lastValidMove;
    private int branchLength = 0;
    private final static int MARK_TERRITORY_VISITED = 99;
    private final static int MARK_TERRITORY_DONE = 999;
    private final static int MARK_SEKI = 10;
    private boolean locked;
    //}}}

    //{{{ BoardHandler constructor
    /**
     *  Constructor for the BoardHandler object
     *
     *@param  b  Pointer to the Board object this object is attached to
     */
    public BoardHandler(Board b) {
        board = b;

        // Create a StoneHandler object
        stoneHandler = new StoneHandler(this);

        // Create a variation tree
        tree = new Tree(board.getBoardSize());

        currentMove = 0;
        gameMode = MODE_NORMAL;
        // markType = markNone;
        capturesBlack = capturesWhite = 0;
        markedDead = false;
        lastValidMove = null;
        locked = false;

        gameData = new GameData();
    } //}}}

    //{{{ Monitoring

    //{{{ lock() method
    /**
     *  Lock the boardhandler. While locked, no other threads may
     *  access this instance.
     */
    synchronized void lock() {
        checkLocked();
        locked = true;
    } //}}}

    //{{{ unlock() method
    /**  Unlock the boardhandler instance */
    synchronized void unlock() {
        locked = false;
        this.notify();
    } //}}}

    //{{{ checkLocked() method
    /**  Check if a previous lock was set. If yet, wait on this instance as monitor. */
    private synchronized void checkLocked() {
        if (locked) {
            System.err.println("BoardHandler.checkLocked(): Waiting on monitor");
            try {
                this.wait();
            } catch (InterruptedException e) {}
        }
    } //}}}

    //}}}

    //{{{ Getter & Setter

    //{{{ getStoneHandler() method
    /**
     *  Gets the stoneHandler attribute of the BoardHandler object
     *
     *@return    The stoneHandler value
     */
    public StoneHandler getStoneHandler() {
        return stoneHandler;
    } //}}}

    //{{{ getBoard() method
    /**
     *  Gets the board attribute of the BoardHandler object
     *
     *@return    The board value
     */
    public Board getBoard() {
        return board;
    } //}}}

    //{{{ getTree() method
    /**
     *  Gets the tree attribute of the BoardHandler object
     *
     *@return    The tree value
     */
    public Tree getTree() {
        return tree;
    } //}}}

    //{{{ setTree() method
    /**
     *  Sets the tree attribute of the BoardHandler object
     *
     *@param  t  The new tree value
     */
    public void setTree(Tree t) {
        if (t != null)
            tree = t;
    } //}}}

    //{{{ getGameData() method
    /**
     *  Gets the gameData attribute of the BoardHandler object
     *
     *@return    The gameData value
     */
    public GameData getGameData() {
        return gameData;
    } //}}}

    //{{{ getGameMode() method
    /**
     *  Gets the gameMode attribute of the BoardHandler object
     *
     *@return    The gameMode value
     */
    public int getGameMode() {
        return gameMode;
    } //}}}

    //{{{ setGameMode() method
    /**
     *  Sets the gameMode attribute of the BoardHandler object
     *
     *@param  m  The new gameMode value
     */
    public void setGameMode(int m) {
        // Leaving score mode
        // TODO

        gameMode = m;

        if (m == MODE_SCORE) {
            capturesBlack = tree.getCurrent().getCapturesBlack();
            capturesWhite = tree.getCurrent().getCapturesWhite();
        }
    } //}}}

    //{{{ setGameModeSGF() method
    /**
     *  Sets the modeSGF attribute of the BoardHandler object
     *
     *@param  m  The new modeSGF value
     */
    public void setGameModeSGF(int m) {
        if (gameMode == MODE_NORMAL && m == MODE_EDIT && tree.getCurrent().getMoveNumber() > 0)
            tree.getCurrent().setMoveNumber(tree.getCurrent().getMoveNumber() - 1);
        gameMode = m;
    } //}}}

    //{{{ setMarkedDead() method
    /**
     *  Sets the markedDead attribute of the BoardHandler object
     *
     *@param  m  The new markedDead value
     */
    public void setMarkedDead(boolean m) {
        markedDead = m;
    } //}}}

    //{{{ getLastValidMove() method
    /**
     *  Gets the lastValidMove attribute of the BoardHandler object
     *
     *@return    The lastValidMove value
     */
    public Move getLastValidMove() {
        return lastValidMove;
    } //}}}
    //}}}

    //{{{ clearData() method
    /**  Clears all data of this class, preparing for a new empty game */
    public void clearData() {
        tree.init(board.getBoardSize());
        currentMove = 0;
        gameMode = MODE_NORMAL;
        // markType = markNone;
        stoneHandler.clearData();
        capturesBlack = capturesWhite = 0;
        markedDead = false;
        lastValidMove = null;
        try {
            board.getMainFrame().dontFireSlider = true;
            branchLength = 0;
            board.getMainFrame().getSlider().setMaximum(0);
            board.getMainFrame().getSliderMaxLabel().setText("0");
            board.getMainFrame().dontFireSlider = false;
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ initGame() method
    /**
     *  Init a new game
     *
     *@param  data        The GameData object with the game information
     *@param  noHandicap  Set to true if this is called when loading a game
     */
    public void initGame(GameData data, boolean noHandicap) {
        gameData = data;

        // We have handicap? Then add the necassary stones.
        // Dont do this when reading sgfs, as they add those stones
        // with AB.
        if (data.handicap > 0 && !noHandicap) {
            setHandicap(data.handicap);
            stoneHandler.checkAllPositions();
        }

        updateGUI();
    } //}}}

    //{{{ hasStone() method
    /**
     *  Check if a stone exists on the given position
     *
     *@param  x  X position
     *@param  y  Y position
     *@return    True if a stone exists, else false
     */
    public boolean hasStone(int x, int y) {
        return stoneHandler.hasStone(x, y);
    } //}}}

    //{{{ hasStoneInMatrixOfMove() method
    /**
     *  Check if a stone exists on the given position in a given move
     *
     *@param  x  X position
     *@param  y  Y position
     *@param  m  Move to check
     *@return    True if a stone exists, else false
     */
    private boolean hasStoneInMatrixOfMove(int x, int y, Move m) {
        if (x < 1 || x > board.getBoardSize() || y < 1 || y > board.getBoardSize())
            return false;
        try {
            int s = Math.abs((int)(m.getMatrix().at(x - 1, y - 1)));
            // System.err.println("hasStoneInMatrixOfLastMove: " + s);
            return s == STONE_BLACK || s == STONE_WHITE;
        } catch (NullPointerException e) {
            System.err.println("BoardHandler.hasStoneInMatrixOfLastMove(): " + e);
            return false;
        }
    } //}}}

    //{{{ getStoneColorAt() method
    /**
     *  Return the color of a stone at the given position
     *
     *@param  x  X position
     *@param  y  Y position
     *@return    Color of the stone at x/y
     */
    public int getStoneColorAt(int x, int y) {
        if (!hasStone(x, y))
            return STONE_NONE;
        return stoneHandler.getStone(x, y).getColor();
    } //}}}

    //{{{ addStone() method
    /**
     *  Adds a new stone to the board.
     *
     *@param  c  Color of the stone
     *@param  x  X position of the stone
     *@param  y  Y position of the stone
     *@return    True if successful, stone has to be drawn on the board. Else false.
     */
    public synchronized boolean addStone(int c, int x, int y) {
        // System.err.println("BoardHandler addStone: " + c + ", " + x + "/" + y);

        if (hasStone(x, y)) {
            if (gameMode == MODE_EDIT) {
                if (removeStone(x, y, true))
                    board.updateGraphics();
            }
            return false;
        }

        // We jump through variations and need to update the stones and groups?
        if ((tree.getCurrent() != null && lastValidMove != null &&
                tree.getCurrent() != lastValidMove) ||
                (gameMode == MODE_NORMAL &&
                tree.getCurrent().getGameMode() == MODE_EDIT) ||
                lastValidMove == null)
            stoneHandler.checkAllPositions();

        Stone stone = new Stone(c, x, y);
        if (stone == null) {
            System.err.println("Could not create stone object.");
            return false;
        }

        // Remember captures from move before adding the stone
        capturesBlack = tree.getCurrent().getCapturesBlack();
        capturesWhite = tree.getCurrent().getCapturesWhite();

        // Normal mode, increase move counter and add the stone as new node
        if (gameMode == MODE_NORMAL) {
            currentMove++;

            // Remove variation ghosts
            if (gGo.getSettings().getShowVariationGhosts())
                board.removeGhosts();

            addMove(stone.getColor(), stone.getX(), stone.getY(), true);

            // BOARD.UPDATELASTMOVE
            // Gone to Board. OK.
        }
        // Edit mode...
        else if (gameMode == MODE_EDIT) {
            // ...we are currently in a normal mode node, so add the edited version as variation,
            // but dont remove the marks.
            // If this is our root move in an -empty- tree, dont add a node, then we edit root.
            if (tree.getCurrent().getGameMode() == MODE_NORMAL &&  // Its normal mode?
            !(tree.getCurrent() == tree.getRoot() &&  // Its root?
            tree.count() == 1)) { // Its an empty tree?
                // Remove variation ghosts
                if (gGo.getSettings().getShowVariationGhosts())
                    board.removeGhosts();
                addMove(stone.getColor(), stone.getX(), stone.getY(), false);
            }
            // ...we are currently already in a node that was created in edit mode, so continue
            // editing this node and dont add a new variation.
            // If its root in an empty tree, and if its the first editing move, change move data. Else do nothing.
            else if (currentMove == 0 && tree.getCurrent().getGameMode() != MODE_EDIT) {
                tree.getCurrent().setGameMode(MODE_EDIT);
                editMove(stone.getColor(), stone.getX(), stone.getY());
            }
            lastValidMove = tree.getCurrent();
        }

        stoneHandler.toggleWorking(true);
        if (!stoneHandler.addStone(stone, gameMode == MODE_NORMAL, true)) {
            // Suicide move
            board.getToolkit().beep();
            deleteNode();
            stoneHandler.toggleWorking(false);
            return false;
        }
        stoneHandler.toggleWorking(false);

        updateCurrentMatrix(c, x, y);

        // Update captures
        tree.getCurrent().setCaptures(capturesBlack, capturesWhite);

        // Display data in GUI
        updateGUI();

        board.setModified(true);
        return true;
    } //}}}

    //{{{ addStoneSGF() method
    /**
     *  Adds a new stone to the board while reading SGF files
     *
     *@param  c         Color of the stone
     *@param  x         X position of the stone
     *@param  y         Y position of the stone
     *@param  new_node  Set to true if a new node is created.
     */
    public synchronized void addStoneSGF(int c, int x, int y, boolean new_node) {
        if (hasStone(x, y)) {
            // In edit mode, this overwrites an existing stone with another color.
            // This is different to the normal interface, when reading sgf files.
            if (gameMode == MODE_EDIT &&
                    tree.getCurrent() != null &&
                    tree.getCurrent().getMatrix().at(x - 1, y - 1) != c) {
                if (!stoneHandler.removeStone(x, y)) {
                    System.err.println("   *** BoardHandler.addStoneSGF() Failed to remove stone! *** ");
                }
            }
        }

        // Check all positions if this is a normal move and parent was an edit move,
        // or if this is move 1 in a handicap game
        if ((tree.getCurrent().parent != null &&
                gameMode == MODE_NORMAL &&
                tree.getCurrent().parent.getGameMode() == MODE_EDIT) ||
                (gameData.handicap > 0 && currentMove == 1))
            stoneHandler.checkAllPositions();

        Stone stone = new Stone(c, x, y);
        if (stone == null) {
            System.err.println("Could not create stone object.");
            return;
        }

        // Remember captures before adding the stone, was already copied in createMoveSGF
        capturesBlack = tree.getCurrent().getCapturesBlack();
        capturesWhite = tree.getCurrent().getCapturesWhite();

        if (gameMode == MODE_NORMAL) {
            currentMove++;
            // This is a hack to force the first  move to be #1. For example, sgf2misc starts with move 0.
            if (currentMove == 1) {
                tree.getCurrent().setMoveNumber(currentMove);
            }
        }

        if (new_node) {
            // Set move data
            editMove(stone.getColor(), stone.getX(), stone.getY());

            // Update move game mode
            if (gameMode != tree.getCurrent().getGameMode()) {
                tree.getCurrent().setGameMode(gameMode);
            }
        }

        // If we are in edit mode, dont check for captures (sgf defines)
        stoneHandler.toggleWorking(true);
        stoneHandler.addStone(stone, gameMode == MODE_NORMAL, false);
        stoneHandler.toggleWorking(false);

        updateCurrentMatrix(c, x, y);

        // Update captures
        tree.getCurrent().setCaptures(capturesBlack, capturesWhite);
    } //}}}

    //{{{ addStoneIGS() method
    /**
     *  Adds a new stone to the board for IGS games
     *
     *@param  c         Color of the stone
     *@param  x         X position of the stone
     *@param  y         Y position of the stone
     *@param  captures  The feature to be added to the StoneIGS attribute
     *@return           True if successful, stone has to be drawn on the board. Else false.
     */
    public synchronized boolean addStoneIGS(int c, int x, int y, ArrayList captures) {
        // MOVE DEBUG
        if (moveDebug)
            System.err.println("BoardHandler.addStoneIGS(): " +
                (c == STONE_BLACK ? "B" : "W") + " " + x + "/" + y);

        // Pass?
        if (x == -1 && y == -1) {
            // Remember captures from move before adding the stone
            capturesBlack = tree.getCurrent().getCapturesBlack();
            capturesWhite = tree.getCurrent().getCapturesWhite();

            currentMove++;

            addMoveIGS(c, 20, 20, captures);
            return false;
        }

        if (hasStoneInMatrixOfMove(x, y, lastValidMove != null ? lastValidMove : tree.getCurrent())) {
            System.err.println("Already a stone at " + x + "/" + y);
            // Usually sent from refresh or when starting to observe, so update
            // move info, in case refresh was used to fix a broken game.
            Move m = lastValidMove != null ? lastValidMove : tree.getCurrent();
            m.setColor(c);
            if (m == lastValidMove)
                updateMove(m, false);
            return false;
        }

        Stone stone = new Stone(c, x, y);
        if (stone == null) {
            System.err.println("Could not create stone object.");
            return false;
        }

        // Remember captures from move before adding the stone
        capturesBlack = tree.getCurrent().getCapturesBlack();
        capturesWhite = tree.getCurrent().getCapturesWhite();

        currentMove++;

        boolean toDraw = addMoveIGS(stone.getColor(), stone.getX(), stone.getY(), captures);
        stoneHandler.addStone(stone, false, toDraw);

        return toDraw;
    } //}}}

    //{{{ removeStone() method
    /**
     *  Remove a stone from the board
     *
     *@param  x        X position of the stone to remove
     *@param  y        Y position of the stone to remove
     *@param  addMove  If true, add a new node. False if called from sgf reading or IGS score mode
     *@return          True if success, false if the stone did not exist
     */
    public boolean removeStone(int x, int y, boolean addMove) {
        boolean res = stoneHandler.removeStone(x, y, addMove);

        if (res) {
            if (tree.getCurrent().getGameMode() == MODE_NORMAL && currentMove > 0) {
                if (addMove) // false, when reading sgf or IGS score mode
                    addMove(STONE_NONE, x, y);
                updateCurrentMatrix(STONE_ERASE, x, y);
            }
            else
                updateCurrentMatrix(STONE_ERASE, x, y);
        }

        board.checkGhostSprite(x, y);

        board.setModified(true);

        return res;
    } //}}}

    //{{{ addMove() methods
    /**
     *  Adds a move to the game tree
     *
     *@param  c           Move color
     *@param  x           X position of the move
     *@param  y           Y position of the move
     *@param  clearMarks  If true, marks will be removed from the move. Not done
     *                    when creating a new variation in edit mode
     */
    protected synchronized void addMove(int c, int x, int y, boolean clearMarks) {
        Matrix mat = tree.getCurrent().getMatrix();
        Move m = new Move(c, x, y, currentMove, gameMode, mat);

        if (tree.hasSon(m)) {
            System.err.println("*** HAVE THIS SON ALREADY! ***");
            return;
        }

        // Remove all marks from this new move. We don't do that when creating
        // a new variation in edit mode.
        if (clearMarks) {
            m.getMatrix().clearAllMarks();
            board.hideAllMarks();
        }

        if (tree.addSon(m) && gGo.getSettings().getShowVariationGhosts() &&
                tree.getNumBrothers() > 0)
            updateVariationGhosts(true);

        lastValidMove = m;
    }

    /**
     *  Adds a move to the game tree
     *
     *@param  c  Move color
     *@param  x  X position of the move
     *@param  y  Y position of the move
     */
    protected void addMove(int c, int x, int y) {
        addMove(c, x, y, true);
    } //}}}

    //{{{ addMoveIGS() method
    /**
     *  Adds an IGS move to the game tree
     *
     *@param  c         Stone color
     *@param  x         X position
     *@param  y         Y position
     *@param  captures  ArrayList of captures, sent from IGS
     *@return           True if the board has to be redrawn, else false
     */
    protected synchronized boolean addMoveIGS(int c, int x, int y, ArrayList captures) {
        // MOVE DEBUG
        if (moveDebug)
            System.err.println("BoardHandler.addMoveIGS(): " +
                (c == STONE_BLACK ? "B" : "W") + " " + x + "/" + y);

        int current = 0;
        Move old = null;
        boolean res = true;
        if (lastValidMove != null) {
            current = lastValidMove.getMoveNumber();
            old = tree.getCurrent();
            if (old != lastValidMove)
                res = false;
            tree.setCurrent(lastValidMove);
        }
        else {
            current = currentMove - 1;
            System.err.println("BoardHandler.addMoveIGS(): lastValidMove is null");
        }
        Matrix mat = tree.getCurrent().getMatrix();
        Move m = new Move(c, x, y, current + 1, MODE_NORMAL, mat);

        if (!tree.setSon(m))
            System.err.println("*** A error happened in BoardHandler.addMoveIGS()\n" +
                    "*** Already have a son of this move. Trying to ignore.");
        updateCurrentMatrix(c, x, y);

        if (captures != null && !captures.isEmpty()) {
            for (Iterator it = captures.iterator(); it.hasNext(); ) {
                Position cap = (Position)it.next();
                stoneHandler.removeStone(cap.x, cap.y, res);
                updateCurrentMatrix(STONE_NONE, cap.x, cap.y);
            }
            if (c == STONE_BLACK)
                capturesBlack += captures.size();
            else
                capturesWhite += captures.size();
        }

        // Update captures
        tree.getCurrent().setCaptures(capturesBlack, capturesWhite);

        lastValidMove = m;
        if (!res) {
            tree.setCurrent(old);
            try {
                branchLength++;
                board.getMainFrame().dontFireSlider = true;
                board.getMainFrame().getSlider().setMaximum(branchLength);
                board.getMainFrame().getSliderMaxLabel().setText(String.valueOf(branchLength));
                board.getMainFrame().dontFireSlider = false;
            } catch (NullPointerException e) {
                System.err.println("Problem updating the slider: " + e);
                e.printStackTrace();
            }
        }
        else
            // Display data in GUI
            updateGUI();

        return res;
    } //}}}

    //{{{ editMove() method
    /**
     *  Edit a move
     *
     *@param  c  Stone color
     *@param  x  X position
     *@param  y  Y position
     */
    protected void editMove(int c, int x, int y) {
        if ((x < 1 || x > board.getBoardSize() || y < 1 || y > board.getBoardSize()) && x != 20 && y != 20)
            return;

        Move m = tree.getCurrent();
        try {
            m.setX(x);
            m.setY(y);
            m.setColor(c);
        } catch (NullPointerException e) {
            System.err.println("BoardHandler.editMove: No current move! - " + e);
            gGo.exitApp(1);
        }
    } //}}}

    //{{{ createMoveSGF() method
    /**
     *  Create a new node, used from SGF parser
     *
     *@param  mode     Node mode: MODE_NORMAL or MODE_EDIT
     *@param  brother  If true, the new node is added as brother of the last, if false as son
     */
    public void createMoveSGF(int mode, boolean brother) {
        Move m;

        Matrix mat = tree.getCurrent().getMatrix();
        m = new Move(STONE_BLACK, -1, -1, tree.getCurrent().getMoveNumber() + 1, mode, mat);

        if (mode == MODE_NORMAL)
            m.getMatrix().clearAllMarks();

        if (!brother) {
            tree.addSon(m);
        }
        else {
            tree.addBrother(m);
        }

        // Copy captures from parent move, if existing
        if (m.parent != null)
            m.setCaptures(m.parent.getCapturesBlack(), m.parent.getCapturesWhite());
    } //}}}

    //{{{ doPass() method
    /**
     *  Do a passing move
     *
     *@param  sgf  Set to true if called from SGFParser
     */
    public void doPass(boolean sgf) {
        int c = getBlackTurn() ? STONE_BLACK : STONE_WHITE;
        currentMove++;

        if (!sgf && gGo.getSettings().getShowVariationGhosts())
            board.removeGhosts();

        if (!sgf) {
            addMove(c, 20, 20);
        }
        else { // Sgf reading

            if (tree.hasParent()) {
                c = tree.getCurrent().parent.getColor() == STONE_BLACK ? STONE_WHITE : STONE_BLACK;
            }
            else {
                c = STONE_BLACK;
            }

            editMove(c, 20, 20);
        }

        if (tree.hasParent()) {
            tree.getCurrent().setCaptures(tree.getCurrent().parent.getCapturesBlack(),
                    tree.getCurrent().parent.getCapturesWhite());
        }

        if (!sgf) {
            // Play sound
            SoundHandler.playPass();

            updateGUI(tree.getCurrent());

            board.updateLastMoveMark(20, 20);
            board.updateGraphics();
        }

        board.setModified(true);
    } //}}}

    //{{{ updateCurrentMatrix() method
    /**
     *  Updates the matrix of the current move
     *
     *@param  c  Move color
     *@param  x  X position of the move
     *@param  y  Y position of the move
     */
    protected void updateCurrentMatrix(int c, int x, int y) {
        // Passing?
        if (x == 20 && y == 20) {
            return;
        }

        if (x < 1 || x > board.getBoardSize() || y < 1 || y > board.getBoardSize()) {
            System.err.println("   *** BoardHandler.updateCurrentMatrix() - Invalid move given: " +
                    x + "/" + y + " at move " + tree.getCurrent().getMoveNumber() + " ***");
            return;
        }

        try {
            if (c == STONE_NONE) {
                tree.getCurrent().getMatrix().removeStone(x, y);
            }
            else if (c == STONE_ERASE) {
                tree.getCurrent().getMatrix().eraseStone(x, y);
            }
            else {
                tree.getCurrent().getMatrix().insertStone(x, y, c, gameMode);
            }
        } catch (NullPointerException e) {
            System.err.println("Failed to update matrix: " + e);
        }
    } //}}}

    //{{{ updateMove() method
    /**
     *  Update the current position after a navigation event (next move, next
     *  variation etc.) was performed
     *
     *@param  m               Current move
     *@param  completeUpdate  If true, the whole board is redrawn, if false only the necassary part
     */
    protected void updateMove(Move m, boolean completeUpdate) {
        if (m == null) {
            System.err.println("BoardHandler.updateMove - Move = null");
            return;
        }

        currentMove = m.getMoveNumber();

        updateGUI(m);

        // Get rid of the varation ghosts
        if (gGo.getSettings().getShowVariationGhosts())
            board.removeGhosts();

        // Get rid of all marks
        board.hideAllMarks();

        // Remove territory marks
        if (tree.getCurrent().isTerritoryMarked()) {
            tree.getCurrent().getMatrix().clearTerritoryMarks();
            tree.getCurrent().setTerritoryMarked(false);
            completeUpdate = true;
        }

        // Unshade dead stones
        if (markedDead) {
            stoneHandler.removeDeadMarks();
            markedDead = false;
            completeUpdate = true;
        }

        // Oops, something serious went wrong
        if (m.getMatrix() == null) {
            System.err.println("   *** Move returns NULL pointer for matrix! ***");
            gGo.exitApp(1);
        }

        // Synchronize the board with the current nodes matrix
        stoneHandler.updateAll(m.getMatrix(), completeUpdate);

        // Update the ghosts indicating variations
        if (tree.getNumBrothers() > 0 && gGo.getSettings().getShowVariationGhosts())
            updateVariationGhosts(completeUpdate);

        if (completeUpdate) {
            board.setPositionModified(true);
            board.repaint();
        }
        else {
            // Update last move mark. When doing complete update, paintComponent does this
            if (m.getGameMode() == MODE_NORMAL && m.getX() > 0 && m.getY() > 0)
                board.updateLastMoveMark(m.getX(), m.getY());
            board.updateGraphics();
        }
    } //}}}

    //{{{ updateGUI() methods
    /**
     *  Display move data and comment of the given move in the GUI
     *
     *@param  m  Move which data is to be displayed
     */
    private void updateGUI(Move m) {
        try {
            // Is this the application?
            if (board.getMainFrame() != null) {
                int sons = tree.getNumSons();
                int brothers = tree.getNumBrothers();
                MainFrame mf = board.getMainFrame();

                // Update statusbar move and navigation labels
                StatusBar sb = board.getMainFrame().getStatusBar();
                sb.setMove(m.getMoveNumber(), m.getColor(), m.getX(), m.getY(), board.getBoardSize());
                if (board.getPlayMode() == PLAY_MODE_EDIT) {
                    sb.setNavigation(sons, brothers);
                    // Update comment
                    mf.setCommentText(m.getComment());
                }
                else {
                    // In observed/played games display captures, not navigation
                    sb.setNavigation(capturesWhite, capturesBlack);
                }

                // Update sidebar
                if (m.getGameMode() == MODE_NORMAL)
                    mf.getSideBar().setMove(m.getMoveNumber(), m.getColor(), m.getX(), m.getY(), board.getBoardSize());
                else
                    mf.getSideBar().setMove(m.getMoveNumber(), m.getColor(), -1, -1, board.getBoardSize());

                if (m.getGameMode() != MODE_SCORE)
                    mf.getSideBar().setTurn(getBlackTurn());
                else
                    mf.getSideBar().setScoreTurn();

                if (board.getPlayMode() == PLAY_MODE_EDIT)
                    mf.getSideBar().setNavigation(sons, brothers);

                if (!m.isScored())
                    mf.getSideBar().setCaptures(m.getCapturesWhite(), m.getCapturesBlack());
                else
                    mf.getSideBar().setScore(m.getScoreWhite(), m.getScoreBlack());

                // Update slider
                mf.dontFireSlider = true;
                if (tree.getVarCreatedFlag())
                    recalcBranchLength();
                else if (branchLength < m.getMoveNumber())
                    branchLength = m.getMoveNumber();
                mf.getSlider().setMaximum(branchLength);
                mf.getSliderMaxLabel().setText(String.valueOf(branchLength));
                mf.getSlider().setValue(m.getMoveNumber());
                mf.dontFireSlider = false;
            }
            // No? Then it should be the applet
            else if (board.getApplet() != null) {
                board.getApplet().setMove(m.getMoveNumber());
                board.getApplet().setCaptures(m.getCapturesWhite(), m.getCapturesBlack());
                board.getApplet().setCommentText(m.getComment());
            }
        } catch (NullPointerException ex) {
            System.err.println("Failed to updateGUI: " + ex);
        }
    }

    /**  Display move data and comment of the current move in the GUI */
    public void updateGUI() {
        updateGUI(tree.getCurrent());
    } //}}}

    //{{{ updateVariationGhosts() method
    /**
     *  Update the variation ghosts
     *
     *@param  completeUpdate  If true, the complete board is redrawn, if false only the needed parts.
     */
    protected void updateVariationGhosts(boolean completeUpdate) {
        Move m;
        try {
            m = tree.getCurrent().parent.son;
        } catch (NullPointerException e) {
            return;
        }

        ArrayList ghosts = board.getGhosts();
        ghosts.clear();

        do {
            if (m == tree.getCurrent())
                continue;
            if (!hasStone(m.getX(), m.getY())) {
                Stone s = new Stone(m.getColor(), m.getX(), m.getY());
                if (!ghosts.contains(s))
                    ghosts.add(s);
            }
        } while ((m = m.brother) != null);

        if (!completeUpdate)
            board.addGhostSprites();
    } //}}}

    //{{{ Navigation

    //{{{ nextMove() method
    /**
     *  Go to next move
     *
     *@param  autoplay  Set to true if called from autoplay mode
     *@return           True of successful, else false (to stop autoplay)
     */
    protected boolean nextMove(boolean autoplay) {
        if (gameMode == MODE_SCORE)
            return false;

        checkLocked();

        Move m = tree.nextMove();
        if (m == null)
            return false;

        if (autoplay)
            SoundHandler.playClick();

        updateMove(m, false);
        return true;
    } //}}}

    //{{{ previousMove() method
    /**  Go to previous move */
    protected void previousMove() {
        if (gameMode == MODE_SCORE)
            return;

        checkLocked();

        Move m = tree.previousMove();
        if (m != null)
            updateMove(m, false);
    } //}}}

    //{{{ nextVariation() method
    /**  Go to next variation */
    protected void nextVariation() {
        if (gameMode == MODE_SCORE)
            return;

        Move m = tree.nextVariation();
        if (m == null)
            return;

        recalcBranchLength();
        updateMove(m, false);
    } //}}}

    //{{{ previousVariation() method
    /**  Go to previous variation */
    protected void previousVariation() {
        if (gameMode == MODE_SCORE)
            return;

        Move m = tree.previousVariation();
        if (m == null)
            return;

        recalcBranchLength();
        updateMove(m, false);
    } //}}}

    //{{{ gotoFirstMove() method
    /**  Go to first move */
    protected void gotoFirstMove() {
        if (gameMode == MODE_SCORE)
            return;

        checkLocked();

        Move m = null;
        try {
            // We need to set the markers. So go the tree upwards
            m = tree.getCurrent();

            // Ascent tree until root reached
            while (m.parent != null)
                m = tree.previousMove();
        } catch (NullPointerException e) {
            System.err.println("Failed to go to first move: " + e);
        }

        tree.setToFirstMove(); // Set move to root
        m = tree.getCurrent();
        recalcBranchLength();
        updateMove(m, !board.isEditable());
    } //}}}

    //{{{ gotoLastMove() method
    /**
     *  Go to last move
     *
     *@param  refresh  If true, repaint the board. Required for silent movement in IGS games.
     */
    public void gotoLastMove(boolean refresh) {
        if (gameMode == MODE_SCORE)
            return;

        checkLocked();

        Move m = null;
        try {
            m = tree.getCurrent();

            // Descent tree to last son of main variation
            while (m.son != null) {
                m = tree.nextMove();
            }
        } catch (NullPointerException e) {
            System.err.println("Failed to go to last move: " + e);
        }

        recalcBranchLength();
        if (m != null && refresh)
            updateMove(m, !board.isEditable());
        else if (m != null && !refresh) {
            lastValidMove = m;
            updateGUI(m);
        }
    } //}}}

    //{{{ gotoMainBranch() method
    /**  Go back to the main branch */
    public void gotoMainBranch() {
        if (gameMode == MODE_SCORE)
            return;

        checkLocked();

        if (tree.getCurrent().parent == null)
            return;

        Move m = tree.getCurrent();
        Move old = m;
        Move lastOddNode = null;

        if (m == null)
            return;

        while ((m = m.parent) != null) {
            if (tree.getNumSons(m) > 1 && old != m.son) // Remember a node when we came from a branch
                lastOddNode = m;
            m.marker = old;
            old = m;
        }

        if (lastOddNode == null)
            return;

        lastOddNode.marker = null; // Clear the marker, so we can proceed in the main branch
        tree.setCurrent(lastOddNode);
        recalcBranchLength();
        updateMove(lastOddNode, false);
    } //}}}

    //{{{ gotoVarStart() method
    /**  Go to the variation start */
    public void gotoVarStart() {
        if (gameMode == MODE_SCORE)
            return;

        if (tree.getCurrent().parent == null)
            return;

        Move tmp = tree.previousMove();
        Move m = null;

        if (tmp == null)
            return;

        // Go up tree until we find a node that has > 1 sons
        while ((m = tree.previousMove()) != null && tree.getNumSons() <= 1)
            tmp = m;
        // Remember move+1, as we set current to the
        // first move after the start of the variation
        if (m == null) { // No such node found, so we reached root.
            tmp = tree.getRoot();
            // For convinience, if we have Move 1, go there. Looks better.
            if (tmp.son != null)
                tmp = tree.nextMove();
        }

        // If found, set current to the first move inside the variation
        tree.setCurrent(tmp);
        recalcBranchLength();
        updateMove(tmp, false);
    } //}}}

    //{{{ gotoNextBranch() method
    /**  Go the the next branch */
    public void gotoNextBranch() {
        Move m = tree.getCurrent();
        Move remember = m; // Remember current in case we dont find a branch

        // We are already on a node with 2 or more sons?
        if (tree.getNumSons() > 1) {
            m = tree.nextMove();
            recalcBranchLength();
            updateMove(m, false);
            return;
        }

        // Descent tree to last son of main variation
        while (m.son != null && tree.getNumSons() <= 1)
            m = tree.nextMove();

        // if (m != null && !m.equals(remember))
        if (m != null && m != remember) {
            if (m.son != null)
                m = tree.nextMove();
            recalcBranchLength();
            updateMove(m, false);
        }
        else
            tree.setCurrent(remember);
    } //}}}

    //{{{ gotoMove() method
    /**
     *  Goto a given move
     *
     *@param  m       Move to navigate to
     *@param  recalc  If true, recalculate the length of the current branch (for slider)
     */
    private void gotoMove(Move m, boolean recalc) {
        if (m == null)
            return;

        tree.setCurrent(m);
        if (recalc)
            recalcBranchLength();
        updateMove(m, false);
    } //}}}

    //{{{ gotoNthMove() method
    /**
     *  Goto move n
     *
     *@param  n  n-th move to go to
     */
    public void gotoNthMove(int n) {
        if (gameMode == MODE_SCORE)
            return;

        checkLocked();

        if (n < 0)
            return;

        Move m = tree.getRoot();
        Move old = tree.getCurrent();
        if (m == null || old == null)
            return;

        while (true) {
            if (m.getMoveNumber() == n)
                break;
            if (m.son == null)
                break;
            m = m.son;
        }

        if (m != null && m != old)
            gotoMove(m, true);
    } //}}}

    //{{{ gotoNthMoveInVar() method
    /**
     *  Goto move n within the current branch
     *
     *@param  n  n-th move to go to
     */
    public void gotoNthMoveInVar(int n) {
        if (gameMode == MODE_SCORE)
            return;

        if (n < 0)
            return;

        checkLocked();

        Move m = tree.getCurrent();
        if (m == null)
            return;
        Move old = m;

        while (true) {
            if (m.getMoveNumber() == n)
                break;
            if ((n >= currentMove && m.son == null && m.marker == null) ||
                    (n < currentMove && m.parent == null))
                break;
            if (n > currentMove) {
                if (m.marker == null)
                    m = m.son;
                else
                    m = m.marker;
                m.parent.marker = m;
            }
            else {
                m.parent.marker = m;
                m = m.parent;
            }
        }

        if (m != null && m != old)
            gotoMove(m, false);
    } //}}}

    //{{{ findMoveByPos() method
    /**
     *  Find a move by its position in all nodes of the main branch
     *
     *@param  x  X position of the searched move
     *@param  y  Y position of the searched move
     */
    public void findMoveByPos(int x, int y) {
        Move m = tree.findMoveInMainBranch(x, y);

        if (m != null)
            gotoMove(m, true);
        else
            board.getToolkit().beep();
    } //}}}

    //{{{ findMoveByPosInVar() method
    /**
     *  Find a move by its position in all following nodes in the current branch
     *
     *@param  x  X position of the searched move
     *@param  y  Y position of the searched move
     */
    public void findMoveByPosInVar(int x, int y) {
        Move m = tree.findMoveInBranch(x, y);

        if (m != null)
            gotoMove(m, false);
        else
            board.getToolkit().beep();
    } //}}}

    //}}}

    //{{{ swapVariations() method
    /**
     *  Swap current node with previous brother
     *
     *@return    True if successful, else false
     */
    public boolean swapVariations() {
        if (gameMode == MODE_SCORE)
            return false;

        Move m = tree.getCurrent();
        Move parent = null;
        Move prev = null;
        Move next = null;
        Move newSon = null;

        if (!tree.hasPrevBrother()) {
            System.err.println("BoardHandler.swapVariations() - No prev brother. Aborting...");
            return false;
        }

        parent = m.parent;
        prev = tree.previousVariation();
        next = m.brother;

        // Check if our move has further prev-prev nodes. If yes, we don't change parents son
        if (!tree.hasPrevBrother(prev))
            newSon = m;
        else
            tree.previousVariation().brother = m;

        // Do the swap
        m.brother = prev;
        prev.brother = next;

        if (newSon != null) {
            if (parent != null)
                parent.son = m;
            else
                tree.setRoot(m);
        }

        tree.setCurrent(m);
        updateMove(m, false);

        return true;
    } //}}}

    //{{{ getBlackTurn() method
    /**
     *  Checks if it is blacks or whites turn
     *
     *@return    True if black turn, false if white turn
     */
    protected boolean getBlackTurn() {
        // First node
        if (currentMove == 0) {
            // Handicap, so white starts
            if (gameData.handicap >= 2)
                return false;
            return true;
        }

        try {
            // Normal mode
            if (tree.getCurrent().getGameMode() == MODE_NORMAL)
                return tree.getCurrent().getColor() == STONE_WHITE;
            // Edit mode. Return color of parent move.
            else if (tree.getCurrent().parent != null)
                return tree.getCurrent().parent.getColor() == STONE_WHITE;
        } catch (NullPointerException e) {}

        // Crap happened. 50% chance this is correct :)
        System.err.println("Oops, crap happened in BoardHandler.getBlackTurn() !");
        return true;
    } //}}}

    //{{{ loadFromString() method
    /**
     *  Load game from a given String with the sgf content
     *
     *@param  toParse  String with sgf content
     *@return          True if successful, else false
     */
    public boolean loadFromString(final String toParse) {
        if (!ParserFactory.createSGFParser().parseString(toParse, this))
            return false;

        prepareBoard();
        return true;
    } //}}}

    //{{{ loadGame() method
    /**
     *  Load a game file
     *
     *@param  fileName  Filename
     *@param  remName   If true, remember the filename
     *@return           True if successful, else false
     */
    public boolean loadGame(final String fileName, final boolean remName) {
        if (!ParserFactory.loadFile(fileName, this))
            return false;

        if (remName)
            gameData.fileName = fileName;
        prepareBoard();
        return true;
    } //}}}

    //{{{ saveGame(String) method
    /**
     *  Save game to file. Format is determined by extension. If no extension
     *  is given, SGF format is used.
     *
     *@param  fileName  Name of the to  be saved file
     *@return           True if successful, else false
     */
    public boolean saveGame(String fileName) {
        return WriterFactory.saveFile(fileName, this);
    } //}}}

    //{{{ saveGame(File) method
    /**
     *  Save game to file. Format is determined by extension. If no extension
     *  is given, SGF format is used.
     *
     *@param  file  File to be saved
     *@return       True if successful, else false
     */
    public boolean saveGame(File file) {
        return WriterFactory.saveFile(file, this);
    } //}}}

    //{{{ prepareBoard() method
    /**  Setup the board for a new game */
    private void prepareBoard() {
        // Clear up trash and reset the board to move 0
        stoneHandler.clearData();
        currentMove = 0;
        tree.setToFirstMove();
        if (tree.getCurrent() == null) {
            System.err.println("   *** Oops! Bad things happened reading the sgf file! ***");
            gGo.exitApp(1);
        }
        gameMode = MODE_NORMAL;
        board.setMarkType(MARK_STONE);
        updateMove(tree.getCurrent(), true);
        lastValidMove = null;
        try {
            branchLength = tree.mainBranchSize() - 1;
            board.getMainFrame().dontFireSlider = true;
            board.getMainFrame().getSlider().setMaximum(branchLength);
            board.getMainFrame().getSliderMaxLabel().setText(String.valueOf(branchLength));
            board.getMainFrame().dontFireSlider = false;
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ setCaptures() method
    /**
     *  Adds captures
     *
     *@param  c  Color of the player the captures are added to
     *@param  n  The added captures value
     */
    public void setCaptures(int c, int n) {
        if (c == STONE_BLACK)
            capturesBlack += n;
        else
            capturesWhite += n;
    } //}}}

    //{{{ editMark() methods
    /**
     *  Edit a mark at the given position
     *
     *@param  x  X position
     *@param  y  Y position
     *@param  t  Mark type
     */
    public void editMark(int x, int y, int t) {
        editMark(x, y, t, null);
    }

    /**
     *  Edit a mark at the given position
     *
     *@param  x    X position
     *@param  y    Y position
     *@param  t    Mark type
     *@param  txt  Mark text label (for number and letter marks)
     */
    public void editMark(int x, int y, int t, String txt) {
        if (t == MARK_NONE)
            tree.getCurrent().getMatrix().removeMark(x, y);
        else
            tree.getCurrent().getMatrix().insertMark(x, y, t);

        /*
         *  TODO
         *  if ((t == MARK_TEXT || t == MARK_NUMBER) && !txt.isNull() && !txt.isEmpty())
         *  tree.getCurrent().getMatrix().setMarkText(x, y, txt);
         */
        board.setModified(true);
    } //}}}

    //{{{ setHandicap() methods
    /**
     *  Setup handicap stones. Use default values.
     *
     *@param  handicap  Number of handicap stones.
     */
    protected void setHandicap(int handicap) {
        setGameMode(MODE_EDIT);
        boolean mod = board.isModified();

        switch (board.getBoardSize()) {
            //{{{ 19x19
            case 19:
                switch (handicap) {
                    case 13: // Hehe, this is nuts... :)
                        addStone(STONE_BLACK, 17, 17);
                    case 12:
                        addStone(STONE_BLACK, 3, 3);
                    case 11:
                        addStone(STONE_BLACK, 3, 17);
                    case 10:
                        addStone(STONE_BLACK, 17, 3);
                    case 9:
                        addStone(STONE_BLACK, 10, 10);
                    case 8:
                    case 7:
                        if (handicap >= 8) {
                            addStone(STONE_BLACK, 10, 4);
                            addStone(STONE_BLACK, 10, 16);
                        }
                        else
                            addStone(STONE_BLACK, 10, 10);
                    case 6:
                    case 5:
                        if (handicap >= 6) {
                            addStone(STONE_BLACK, 4, 10);
                            addStone(STONE_BLACK, 16, 10);
                        }
                        else
                            addStone(STONE_BLACK, 10, 10);
                    case 4:
                        addStone(STONE_BLACK, 16, 16);
                    case 3:
                        addStone(STONE_BLACK, 4, 4);
                    case 2:
                        addStone(STONE_BLACK, 16, 4);
                        addStone(STONE_BLACK, 4, 16);
                    case 1:
                        gameData.komi = 0.5f;
                        break;
                    default:
                        board.displayInvalidHandicapError(handicap);
                }
                break; //}}}
            //{{{ 13x13
            case 13:
                switch (handicap) {
                    case 9:
                        addStone(STONE_BLACK, 7, 7);
                    case 8:
                    case 7:
                        if (handicap >= 8) {
                            addStone(STONE_BLACK, 7, 4);
                            addStone(STONE_BLACK, 7, 10);
                        }
                        else
                            addStone(STONE_BLACK, 7, 7);
                    case 6:
                    case 5:
                        if (handicap >= 6) {
                            addStone(STONE_BLACK, 4, 7);
                            addStone(STONE_BLACK, 10, 7);
                        }
                        else
                            addStone(STONE_BLACK, 7, 7);
                    case 4:
                        addStone(STONE_BLACK, 10, 10);
                    case 3:
                        addStone(STONE_BLACK, 4, 4);
                    case 2:
                        addStone(STONE_BLACK, 10, 4);
                        addStone(STONE_BLACK, 4, 10);
                    case 1:
                        gameData.komi = 0.5f;
                        break;
                    default:
                        board.displayInvalidHandicapError(handicap);
                }
                break; //}}}
            //{{{ 9x9
            case 9:
                switch (handicap) {
                    case 9:
                        addStone(STONE_BLACK, 5, 5);
                    case 8:
                    case 7:
                        if (handicap >= 8) {
                            addStone(STONE_BLACK, 5, 3);
                            addStone(STONE_BLACK, 5, 7);
                        }
                        else
                            addStone(STONE_BLACK, 5, 5);
                    case 6:
                    case 5:
                        if (handicap >= 6) {
                            addStone(STONE_BLACK, 3, 5);
                            addStone(STONE_BLACK, 7, 5);
                        }
                        else
                            addStone(STONE_BLACK, 5, 5);
                    case 4:
                        addStone(STONE_BLACK, 7, 7);
                    case 3:
                        addStone(STONE_BLACK, 3, 3);
                    case 2:
                        addStone(STONE_BLACK, 7, 3);
                        addStone(STONE_BLACK, 3, 7);
                    case 1:
                        gameData.komi = 0.5f;
                        break;
                    default:
                        board.displayInvalidHandicapError(handicap);
                }
                break; //}}}
            default:
                board.displayHandicapNotSupportedError();
        }

        setGameMode(MODE_NORMAL);
        board.setModified(mod);
    }

    /**
     *  Setup handicap stones using given positions.
     *
     *@param  handicap  Array of Positions
     */
    public void setHandicap(ArrayList handicap) {
        setGameMode(MODE_EDIT);

        for (Iterator it = handicap.iterator(); it.hasNext(); ) {
            Position pos = (Position)it.next();
            addStone(STONE_BLACK, pos.x, pos.y);
        }

        setGameMode(MODE_NORMAL);
    } //}}}

    //{{{ deleteNode() method
    /**  Delete current node */
    public void deleteNode() {
        Move m = tree.getCurrent();
        Move remember = null;
        Move remSon = null;

        if (m == null) {
            System.err.println("No current move to delete.");
            return;
        }

        if (m.parent != null) {
            remember = m.parent;

            // Remember son of parent if its not the move to be deleted.
            // Then check for the brothers and fix the pointer connections, if we
            // delete a node with brothers. (It gets ugly now...)
            // YUCK! I hope this works.
            if (remember.son == m) { // This son is our move to be deleted?
                if (remember.son.brother != null) // This son has a brother?
                    remSon = remember.son.brother; // Reset pointer
            }
            else { // No, the son is not our move
                remSon = remember.son;
                Move tmp = remSon;
                Move oldTmp = tmp;

                do { // Loop through all brothers until we find our move
                    if (tmp == m) {
                        if (m.brother != null) // Our move has a brother?
                            oldTmp.brother = m.brother; // Then set the previous move brother
                        else // to brother of our move
                            oldTmp.brother = null; // No brother found.
                        break;
                    }
                    oldTmp = tmp;
                } while ((tmp = tmp.brother) != null);
            }
        }
        else if (tree.hasPrevBrother()) {
            remember = tree.previousVariation();
            if (m.brother != null)
                remember.brother = m.brother;
            else
                remember.brother = null;
        }
        else if (tree.hasNextBrother()) {
            remember = tree.nextVariation();
            // Urgs, remember is now root.
            tree.setRoot(remember);
        }
        else {
            // Oops, first and only move. We delete everything
            tree.init(board.getBoardSize());
            lastValidMove = null;
            stoneHandler.clearData();
            branchLength = 0;
            updateMove(tree.getCurrent(), true);
            return;
        }

        if (m.son != null)
            Tree.traverseClear(m.son); // Traverse the tree after our move (to avoid brothers)
        tree.setCurrent(remember); // Set current move to previous move
        remember.son = remSon; // Reset son pointer
        remember.marker = null; // Forget marker

        recalcBranchLength();
        updateMove(tree.getCurrent(), false);

        lastValidMove = null;
        board.setModified(true);
    } //}}}

    //{{{ silentDeleteNode() method
    /**
     *  Delete current node silently, used for IGS undoes
     *
     *@return                           True if we were somewhere else in the game, false if we were in the last move
     *@exception  NullPointerException  Delete node failed
     */
    public boolean silentDeleteNode() throws NullPointerException {
        Move c = tree.getCurrent();
        // This is not the last move?
        // Move to last move, silently delete, move back to current position
        if (c.son != null) {
            gotoLastMove(false);
            lastValidMove = tree.getCurrent().parent;
            branchLength = tree.getCurrent().parent.getMoveNumber();
            updateCurrentMatrix(
                    STONE_NONE,
                    tree.getCurrent().parent.son.getX(),
                    tree.getCurrent().parent.son.getY());
            tree.getCurrent().parent.son = null;
            tree.getCurrent().parent.marker = null;
            tree.setCurrent(c);
            updateGUI(c);
            return true;
        }
        // Was last move, simply delete it and repaint
        else {
            deleteNode();
            lastValidMove = tree.getCurrent();
            return false;
        }
    } //}}}

    //{{{ numberMoves() method
    /**  Mark all moves with their move number */
    public void numberMoves() {
        if (gameMode == MODE_SCORE)
            return;

        // Move from current upwards to root and set a number mark
        Move m = tree.getCurrent();
        if (m == null || m.getMoveNumber() == 0)
            return;

        do {
            board.addMark(m.getX(), m.getY(), MARK_NUMBER, m.getMoveNumber());
        } while ((m = m.parent) != null && m.getMoveNumber() != 0);
    } //}}}

    //{{{ markVariations() method
    /**
     *  Mark all brothers or sons of the current move
     *
     *@param  sons  If true, mark sons. If false, mark brothers.
     */
    public void markVariations(boolean sons) {
        if (gameMode == MODE_SCORE)
            return;

        Move m = tree.getCurrent();
        if (m == null)
            return;

        // Mark all sons of current move
        if (sons && m.son != null) {
            m = m.son;
            do {
                board.addMark(m.getX(), m.getY(), MARK_TEXT);
            } while ((m = m.brother) != null);
        }
        // Mark all brothers of current move
        else if (!sons && m.parent != null) {
            Move tmp = m.parent.son;
            if (tmp == null)
                return;

            do {
                if (tmp != m)
                    board.addMark(tmp.getX(), tmp.getY(), MARK_TEXT);
            } while ((tmp = tmp.brother) != null);
        }
    } //}}}

    //{{{ recalcBranchLength() method
    /**  Recalculate the length of the current branch. */
    private void recalcBranchLength() {
        branchLength = tree.getCurrent().getMoveNumber() + tree.getBranchLength();
    } //}}}

    //{{{ Scoring

    //{{{ countScore() method
    /**  Count the score. */
    public void countScore() {
        tree.getCurrent().getMatrix().clearTerritoryMarks();

        // Copy the current matrix
        Matrix m = new Matrix(tree.getCurrent().getMatrix());

        // Do some cleanups, we only need stones
        m.absMatrix();
        m.clearAllMarks();

        // Mark all dead stones in the matrix with negative number
        for (int i = 0; i < board.getBoardSize(); i++) {
            for (int j = 0; j < board.getBoardSize(); j++) {
                if (stoneHandler.hasStone(i + 1, j + 1)) {
                    if (stoneHandler.getStone(i + 1, j + 1).isDead())
                        m.set(i, j, m.at(i, j) * -1);
                    else if (stoneHandler.getStone(i + 1, j + 1).isSeki())
                        m.set(i, j, m.at(i, j) * MARK_SEKI);
                }
            }
        }

        int terrWhite = 0;
        int terrBlack = 0;
        int i = 0;
        int j = 0;

        while (true) {
            boolean found = false;

            for (i = 0; i < board.getBoardSize(); i++) {
                for (j = 0; j < board.getBoardSize(); j++) {
                    if (m.at(i, j) <= 0) {
                        found = true;
                        break;
                    }
                }
                if (found)
                    break;
            }

            if (!found)
                break;

            // Traverse the enclosed territory. Resulting color is in col afterwards
            int col = traverseTerritory(m, i, j, STONE_NONE);

            // Now turn the result into real territory or dame points
            for (i = 0; i < board.getBoardSize(); i++) {
                for (j = 0; j < board.getBoardSize(); j++) {
                    if (m.at(i, j) == MARK_TERRITORY_VISITED) {
                        if (col == STONE_BLACK) {
                            tree.getCurrent().getMatrix().removeMark(i + 1, j + 1);
                            tree.getCurrent().getMatrix().insertMark(i + 1, j + 1, MARK_TERR_BLACK);
                            terrBlack++;
                        }
                        else if (col == STONE_WHITE) {
                            tree.getCurrent().getMatrix().removeMark(i + 1, j + 1);
                            tree.getCurrent().getMatrix().insertMark(i + 1, j + 1, MARK_TERR_WHITE);
                            terrWhite++;
                        }
                        m.set(i, j, MARK_TERRITORY_DONE);
                    }
                }
            }
        }

        // Mark the move having territory marks
        tree.getCurrent().setTerritoryMarked(true);

        // Update game data
        gameData.scoreTerrWhite = terrWhite;
        gameData.scoreCapsWhite = capturesWhite + caps_white;
        gameData.scoreTerrBlack = terrBlack;
        gameData.scoreCapsBlack = capturesBlack + caps_black;

        // Update Interface
        try {
            board.getMainFrame().getSideBar().setResult(
                    terrWhite, capturesWhite + caps_white,
                    terrBlack, capturesBlack + caps_black,
                    gameData.komi);
            board.getMainFrame().getSideBar().setScoreTurn();
        } catch (NullPointerException e) {
            System.err.println("Failed to update score in GUI: " + e);
        }

        // Paint the territory on the board. Do a complete update.
        stoneHandler.updateAll(tree.getCurrent().getMatrix(), true);
        board.setPositionModified(true);
        board.repaint();
    } //}}}

    //{{{ traverseTerritory() method
    /**
     *  Recursive traverse of the territory, to count the score
     *
     *@param  m    Description of the Parameter
     *@param  x    Description of the Parameter
     *@param  y    Description of the Parameter
     *@param  col  Description of the Parameter
     *@return      Description of the Return Value
     */
    private int traverseTerritory(Matrix m, int x, int y, int col) {
        // Mark visited
        m.set(x, y, MARK_TERRITORY_VISITED);

        // North
        col = checkNeighbourTerritory(m, x, y - 1, col);
        if (terrContinueFlag)
            col = traverseTerritory(m, x, y - 1, col);

        // East
        col = checkNeighbourTerritory(m, x + 1, y, col);
        if (terrContinueFlag)
            col = traverseTerritory(m, x + 1, y, col);

        // South
        col = checkNeighbourTerritory(m, x, y + 1, col);
        if (terrContinueFlag)
            col = traverseTerritory(m, x, y + 1, col);

        // West
        col = checkNeighbourTerritory(m, x - 1, y, col);
        if (terrContinueFlag)
            col = traverseTerritory(m, x - 1, y, col);

        return col;
    } //}}}

    //{{{ checkNeighbourTerritory() method
    /**
     *  Description of the Method
     *
     *@param  m    Description of the Parameter
     *@param  x    Description of the Parameter
     *@param  y    Description of the Parameter
     *@param  col  Description of the Parameter
     *@return      Description of the Return Value
     */
    private int checkNeighbourTerritory(Matrix m, final int x, final int y, int col) {
        // Off the board? Dont continue
        if (x < 0 || x >= board.getBoardSize() || y < 0 || y >= board.getBoardSize()) {
            terrContinueFlag = false;
            return col;
        }

        // No stone? Continue
        if (m.at(x, y) <= 0) {
            terrContinueFlag = true;
            return col;
        }

        // A stone, but no color found yet? Then set this color and dont continue
        // The stone must not be marked as alive in seki.
        if (col == STONE_NONE && m.at(x, y) > 0 && m.at(x, y) < MARK_SEKI) {
            col = m.at(x, y);
            terrContinueFlag = false;
            return col;
        }

        // A stone, but wrong color? Set abort flag but continue to mark the rest of the dame points
        int tmpCol = STONE_NONE;
        if (col == STONE_BLACK)
            tmpCol = STONE_WHITE;
        else if (col == STONE_WHITE)
            tmpCol = STONE_BLACK;
        if ((tmpCol == STONE_BLACK || tmpCol == STONE_WHITE) && m.at(x, y) == tmpCol) {
            terrContinueFlag = false;
            return STONE_ERASE;
        }

        // A stone, correct color, or already visited, or seki. Dont continue
        terrContinueFlag = false;
        return col;
    } //}}}

    //{{{ markDeadStone() method
    /**
     *  Mark a stone and all attached stones as dead
     *
     *@param  x  X position
     *@param  y  Y position
     */
    public void markDeadStone(int x, int y) {
        DeadGroupData data = new DeadGroupData();
        data.caps = 0;
        data.col = STONE_NONE;
        data.dead = false;

        if (stoneHandler.removeDeadGroup(x, y, data)) {
            // Mark we have dead stones in this move
            if (data.dead)
                markedDead = true;

            // Add captures for opponent
            if (!data.dead)
                data.caps *= -1;
            setCaptures(data.col == STONE_BLACK ? STONE_WHITE : STONE_BLACK, data.caps);

            countScore();
        }
    } //}}}

    //{{{ enterScoreMode() method
    /**
     *  Enter score mode. Called when scoring is started by pressing the Score button.
     *
     *@param  caps_black  Number of black captures
     *@param  caps_white  Number of white captures
     */
    public void enterScoreMode(int caps_black, int caps_white) {
        capturesBlack = tree.getCurrent().getCapturesBlack();
        capturesWhite = tree.getCurrent().getCapturesWhite();
        this.caps_black = caps_black;
        this.caps_white = caps_white;
        tree.getCurrent().setScored(true);
    } //}}}

    //{{{ leaveScoreMode() method
    /**
     * Leave score mode. Called when scoring is aborted by pressing the Score button again.
     */
    public void leaveScoreMode() {
        // Remove territory marks
        if (tree.getCurrent().isTerritoryMarked()) {
            tree.getCurrent().getMatrix().clearTerritoryMarks();
            tree.getCurrent().setTerritoryMarked(false);
            tree.getCurrent().setScored(false);
        }

        // Unshade dead stones
        stoneHandler.removeDeadMarks();
        markedDead = false;

        updateMove(tree.getCurrent(), true);

        caps_black = caps_white = 0;
    } //}}}

    //}}}
}

