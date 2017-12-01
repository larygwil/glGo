/*
 *  Board.java
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

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.util.*;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.lang.reflect.*;
import ggo.utils.sound.SoundHandler;
import ggo.*;
import ggo.utils.*;
import ggo.gui.*;
import ggo.gui.marks.*;
import ggo.igs.*;
import ggo.igs.gui.IGSObserverFrame;

/**
 *  The panel showing the board
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.17 $, $Date: 2002/10/23 01:50:42 $
 */
public class Board extends JPanel implements Defines {
    //{{{ private members
    private int boardSize = 19;
    private int offset, square_size, board_pixel_size, offsetX, offsetY, oldW = -1, oldH = -1;
    private BoardHandler boardHandler;
    private MarkHandler markHandler;
    private MainFrame mainFrame;
    private gGoApplet applet;
    private boolean showCoords, positionModified, modified;
    private BoardListener boardListener;
    private BufferedImage boardImageWithStones = null, boardImageWithoutStones = null, activeImage = null, kaya = null;
    private int curPosX = -1, curPosY = -1, antiSlipPosX = -1, antiSlipPosY = -1;
    private long antiSlipTimeStamp;
    private ArrayList ghosts;
    private boolean isready = false, editable, checkMove = true, locked = false;
    private int playMode;
    private int[][] whitestonematrix;
    /**  Description of the Field */
    public Image scaledBlackStoneImg;
    /**  Description of the Field */
    public Image[] scaledWhiteStoneImg;
    private final static int stoneShift = 1;
    //}}}

    //{{{ Board() constructors
    /**
     *Constructor for the Board object
     *
     *@param  mf        MainFrame that embeds this board
     *@param  editable  True if the board is editable, else false for playing or observing games
     */
    public Board(MainFrame mf, boolean editable) {
        mainFrame = mf;
        applet = null;

        // Init image cache
        scaledBlackStoneImg = null;
        scaledWhiteStoneImg = new Image[ImageHandler.WHITE_IMAGES_NUMBER];

        // Init random whitestone matrix
        whitestonematrix = new int[36][36];
        for (int x = 0; x < 36; x++)
            for (int y = 0; y < 36; y++)
                whitestonematrix[x][y] = (int)(Math.random() * ImageHandler.WHITE_IMAGES_NUMBER);

        // Create a BoardHandler instance
        boardHandler = new BoardHandler(this);

        // Create a MarkHandler instance
        markHandler = new MarkHandler();

        // Restore saved board size
        int sizeX;
        int sizeY;
        if (gGo.getSettings().getFrameSize() != null &&
                gGo.getSettings().getFrameSize().width > 0 &&
                gGo.getSettings().getFrameSize().height > 0) {
            sizeX = gGo.getSettings().getFrameSize().width;
            sizeY = gGo.getSettings().getFrameSize().height;
            // Force a sqaure board
            if (sizeX < sizeY)
                sizeY = sizeX;
            else
                sizeX = sizeY;
        }
        else {
            sizeX = BOARD_DEFAULT_X;
            sizeY = BOARD_DEFAULT_Y;
        }

        // Check screen resolution
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (sizeX > screenSize.width - 150)
            sizeX = sizeY = screenSize.width - 150;
        if (sizeY > screenSize.height - 40)
            sizeX = sizeY = screenSize.height - 40;

        setPreferredSize(new Dimension(sizeX, sizeY));
        setMinimumSize(new Dimension(40, 40));
        setLayout(new BorderLayout());

        // Set play mode
        try {
            if (mainFrame.getClass().getName().equals("ggo.gtp.GTPMainFrame"))
                playMode = PLAY_MODE_GTP;
            else if (mainFrame.getClass().getName().equals("ggo.igs.gui.IGSObserverFrame"))
                playMode = PLAY_MODE_IGS_OBSERVE;
            else if (mainFrame.getClass().getName().equals("ggo.igs.gui.IGSPlayingFrame") ||
                    mainFrame.getClass().getName().equals("ggo.igs.gui.IGSTeachingFrame"))
                playMode = PLAY_MODE_IGS_PLAY;
            else
                playMode = PLAY_MODE_EDIT;
        } catch (NullPointerException e) {
            playMode = PLAY_MODE_EDIT;
        }

        boardListener = new BoardListener();
        addMouseListener(boardListener);
        addMouseMotionListener(boardListener);
        if (playMode != PLAY_MODE_GTP && playMode != PLAY_MODE_IGS_PLAY) {
            addKeyListener(boardListener);
            // --- 1.3 ---
            // addMouseWheelListener(boardListener);
            if (!gGo.is13()) {
                try {
                    Class c = Class.forName("ggo.BoardWheelListener");
                    Method initMethod = c.getMethod("init", new Class[]{this.getClass()});
                    initMethod.invoke(c.newInstance(), new Object[]{this});
                } catch (Exception e) {
                    System.err.println("Failed to add MouseWheelListener: " + e);
                }
            }
        }

        try {
            showCoords = gGo.getSettings().getShowCoords();
        } catch (NullPointerException e) {
            System.err.println("Could not load settings.");
            showCoords = true;
        }

        oldW = -1;
        oldH = -1;
        positionModified = true;
        modified = false;
        ghosts = new ArrayList();
        this.editable = editable;
    }

    /**
     *Constructor for the Board object
     *
     *@param  applet    Applet that embeds this board
     *@param  editable  Description of the Parameter
     */
    public Board(boolean editable, gGoApplet applet) {
        this(null, editable);
        this.applet = applet;
    }

    /**  Default constructor for the Board object */
    public Board() {
        this(null, true);
    } //}}}

    //{{{ Getter & Setter

    //{{{ lock() method
    /**  Lock the board (while loading) */
    public void lock() {
        locked = true;
    } //}}}

    //{{{ unlock() method
    /**  Unlock the board (after loading) */
    public void unlock() {
        locked = false;
    } //}}}

    //{{{ getBoardHandler() method
    /**
     *  Gets the boardHandler attribute of the Board object
     *
     *@return    The boardHandler value
     */
    public BoardHandler getBoardHandler() {
        return boardHandler;
    } //}}}

    //{{{ getBoardSize() method
    /**
     *  Gets the boardSize attribute of the Board object
     *
     *@return    The boardSize value
     */
    public int getBoardSize() {
        return boardSize;
    } //}}}

    //{{{ setShowCoords() method
    /**
     *  Sets the showCoords attribute of the Board object
     *
     *@param  s  The new showCoords value
     */
    public void setShowCoords(boolean s) {
        if (showCoords == s)
            return;
        showCoords = s;
        oldW = oldH = -1;
    } //}}}

    //{{{ getShowCoords() method
    /**
     *  Gets the showCoords attribute of the Board object
     *
     *@return    The showCoords value
     */
    public boolean getShowCoords() {
        return showCoords;
    } //}}}

    //{{{ getMainFrame() method
    /**
     *  Gets the mainFrame attribute of the Board object
     *
     *@return    The mainFrame value
     */
    public MainFrame getMainFrame() {
        return mainFrame;
    } //}}}

    //{{{ getApplet() method
    /**
     *  Gets the applet attribute of the Board object
     *
     *@return    The applet value
     */
    public gGoApplet getApplet() {
        return applet;
    } //}}}

    //{{{ setPositionModified() method
    /**
     *  Sets the positionModified attribute of the Board object
     *
     *@param  b  The new positionModified value
     */
    public void setPositionModified(boolean b) {
        positionModified = b;
    }
    //}}}

    //{{{ isModified() method
    /**
     *  Check if the board was modified. If true, the user should be prompted to
     *  save before closing or opening a new game
     *
     *@return    True if board was modified
     */
    public boolean isModified() {
        return modified;
    } //}}}

    //{{{ setModified() method
    /**
     *  Sets the modified attribute. If true, the user should be prompted to
     *  save before closing or opening a new game if this was set.
     *
     *@param  m  The new modified value
     */
    public void setModified(boolean m) {
        modified = m;
        try {
            mainFrame.updateCaption();
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ getMarkType() method
    /**
     *  Gets the markType attribute of the MainFrame object
     *
     *@return    The markType value
     */
    public int getMarkType() {
        int mt = MARK_STONE;
        try {
            mt = mainFrame.getMarkType();
        } catch (NullPointerException ex) {
            System.err.println("Failed to get mark type: " + ex);
        }
        return mt;
    } //}}}

    //{{{ setMarkType() method
    /**
     *  Sets the markType attribute of the Board object
     *
     *@param  t  The new markType value
     */
    public void setMarkType(int t) {
        try {
            mainFrame.setMarkType(t);
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ getMarkHandler() method
    /**
     *  Gets the markHandler attribute of the Board object
     *
     *@return    The markHandler value
     */
    public MarkHandler getMarkHandler() {
        return markHandler;
    } //}}}

    //{{{ getGhosts() method
    /**
     *  Gets the ghosts attribute of the Board object
     *
     *@return    The ghosts value
     */
    public ArrayList getGhosts() {
        return ghosts;
    } //}}}

    //{{{ isEditable() method
    /**
     *  Gets the editable attribute of the Board object
     *
     *@return    The editable value
     */
    public boolean isEditable() {
        return editable;
    } //}}}

    //{{{ getPlayMode() method
    /**
     *  Gets the playMode attribute of the Board object
     *
     *@return    The playMode value
     */
    public int getPlayMode() {
        return playMode;
    } //}}}

    //}}}

    //{{{ clearData() method
    /**  Clears all data of this class, preparing for a new empty game */
    public void clearData() {
        boardHandler.clearData();
        try {
            if (mainFrame.getClass().getName().equals("ggo.MainFrame") ||
                    mainFrame.getClass().getName().equals("ggo.gtp.GTPMainFrame")) {
                mainFrame.getSideBar().switchMode(MODE_NORMAL);
                mainFrame.getSideBar().setMarkType(MARK_STONE);
            }
        } catch (NullPointerException e) {}
        markHandler.clear();
        positionModified = true;
        modified = false;
        ghosts.clear();
    } //}}}

    //{{{ initGame() method
    /**
     *  Prepare the board for a new game and init the new gamedata
     *
     *@param  data  GameData for the new game
     *@param  sgf   Flag, set to true when reading a sgf file to prevent automatic handicap setup.
     */
    public void initGame(GameData data, boolean sgf) {
        if (data.size != boardSize)
            oldW = oldH = -1;
        boardSize = data.size;
        clearData();
        boardHandler.initGame(data, sgf);
        isready = false;
        try {
            mainFrame.updateCaption();
            mainFrame.setGameInfo(
                    data.playerWhite +
                    (data.rankWhite != null && data.rankWhite.length() > 0 ? " " + data.rankWhite : ""),
                    data.playerBlack +
                    (data.rankBlack != null && data.rankBlack.length() > 0 ? " " + data.rankBlack : ""),
                    data.handicap,
                    data.komi);
        } catch (NullPointerException e) {}
        repaint();
    } //}}}

    //{{{ calculateSize() method
    /**
     *  Calculate the board size variables
     *
     *@return    True if board was resized, else false
     */
    private synchronized boolean calculateSize() {
        Insets insets = getInsets();
        final int margin = 5; // Fixed margin
        final int w = getWidth() - margin * 2;
        final int h = getHeight() - margin * 2;

        // Dont recalculate if window was not resized
        if (w == oldW && h == oldH)
            return false;

        oldW = w;
        oldH = h;

        offset = (w < h ? w * 2 / 100 : h * 2 / 100);

        if (showCoords)
            offset += (int)(((Graphics2D)getGraphics()).getFontMetrics().getStringBounds("19", (Graphics2D)getGraphics()).getWidth());

        square_size = (w < h ? (w - 2 * offset) / boardSize : (h - 2 * offset) / boardSize);
        // Should not happen, but safe is safe.
        if (square_size == 0)
            square_size = 1;

        offset += square_size / 2;
        board_pixel_size = square_size * (boardSize - 1);

        // Center the board in canvas
        offsetX = margin + (w - board_pixel_size) / 2;
        offsetY = margin + (h - board_pixel_size) / 2;

        // System.err.println("Offset = " + offset + ", OffsetX = " + offsetX + ", OffsetY = " + offsetY +
        // ", w = " + w + ", h = " + h + "\nsquare_size = " + square_size + ", board_pixel_size = " + board_pixel_size);

        // Cache scaled stones
        scaledBlackStoneImg = ImageHandler.getScaledStoneBlackImage(square_size + stoneShift);
        for (int i = 0; i < ImageHandler.WHITE_IMAGES_NUMBER; i++)
            scaledWhiteStoneImg[i] = ImageHandler.getScaledStoneWhiteImage(square_size + stoneShift, i);

        // Preload the scaled images to avoid a delay when first drawn on this board
        BufferedImage dummy = (BufferedImage)createImage(100, 100);
        Graphics2D dummyBig = (Graphics2D)dummy.createGraphics();
        dummyBig.drawImage(scaledBlackStoneImg, 0, 0, this);
        for (int i = 0; i < ImageHandler.WHITE_IMAGES_NUMBER; i++)
            dummyBig.drawImage(scaledWhiteStoneImg[i], 0, 0, this);

        return true;
    } //}}}

    //{{{ hasVarGhost() methods
    /**
     *  Check if we have a variation ghost of a given color at the given position
     *
     *@param  c  Color to check
     *@param  x  X coordinate of the point to check
     *@param  y  Y coordinate of the point to check
     *@return    True if a var ghost exists, else false
     */
    private boolean hasVarGhost(int c, int x, int y) {
        return ghosts.contains(new Stone(c, x, y));
    }

    /**
     *  Check if we have a variation ghost at the given position
     *
     *@param  x  X coordinate of the point to check
     *@param  y  Y coordinate of the point to check
     *@return    True if a var ghost exists, else false
     */
    private boolean hasVarGhost(int x, int y) {
        Stone s;
        for (Iterator it = ghosts.iterator(); it.hasNext(); ) {
            s = (Stone)it.next();
            if (s.getX() == x && s.getY() == y)
                return true;
        }
        return false;
    } //}}}

    //{{{ doMove() method
    /**
     *  Do a move. Paint the stone on the board, play the click sound, and update the last move mark.
     *
     *@param  c          Color of the played stone
     *@param  x          X coordinate of the played stone
     *@param  y          Y coordinate of the played stone
     *@param  playSound  True if a sound should be played, else false
     */
    public synchronized void doMove(int c, int x, int y, boolean playSound) {
        // IGS move? If it is my turn, just forward the move to IGS, it gets drawn after the IGS command.
        if (playMode == PLAY_MODE_IGS_PLAY &&
                (((PlayingMainFrame)mainFrame).getPlayerColor() == c ||
                ((PlayingMainFrame)mainFrame).getPlayerColor() == -1) &&
                !boardHandler.hasStone(x, y)) {
            ((PlayingMainFrame)mainFrame).moveDone(c, x, y);
            return;
        }

        // Wait until repaint is done
        if (!isready) {
            System.err.println("Board.doMove(): Waiting on monitor");
            try {
                this.wait();
            } catch (InterruptedException e) {}
        }

        boardHandler.lock();

        if (boardHandler.addStone(c, x, y)) {
            addStoneSprite(null, c, x, y);
            addGhostSprites();
            updateLastMoveMark(x, y);
            updateGraphics();

            // Play sound
            if (boardHandler.getGameMode() == MODE_NORMAL && playSound)
                SoundHandler.playClick();

            // GTP move?
            if (playMode == PLAY_MODE_GTP && checkMove)
                ((PlayingMainFrame)mainFrame).moveDone(c, x, y);
        }

        boardHandler.unlock();
    } //}}}

    //{{{ doIGSMove() method
    /**
     *  Do a move coming from an IGS game. This is different as it already gives the captures.
     *  Paint the stone on the board, play the click sound, and update the last move mark.
     *
     *@param  c          Color of the played stone
     *@param  x          X coordinate of the played stone
     *@param  y          Y coordinate of the played stone
     *@param  captures   ArrayList with the captured stone positions
     *@param  playSound  True if a sound should be played, else false
     *@param  whiteTime  White time data
     *@param  blackTime  Black time data
     */
    public synchronized void doIGSMove(int c, int x, int y, ArrayList captures, boolean playSound,
            IGSTime whiteTime, IGSTime blackTime) {

        // MOVE DEBUG
        if (moveDebug) {
            int gameID;
            try {
                gameID = ((ggo.igs.gui.IGSObserverFrame)mainFrame).getGameID();
            } catch (ClassCastException ex1) {
                try {
                    gameID = ((ggo.igs.gui.IGSPlayingFrame)mainFrame).getGameID();
                } catch (ClassCastException ex2) {
                    gameID = -1;
                }
            }
            System.err.println("Board.doIGSMove(): " +
                    (c == STONE_BLACK ? "B" : "W") + " " + x + "/" + y +
                    " GameID = " + (gameID != -1 ? String.valueOf(gameID) : "FAILED"));
        }

        // Wait until repaint is done
        if (!isready) {
            System.err.println("Board.doMoveIGS(): Waiting on monitor");
            try {
                this.wait();
            } catch (InterruptedException e) {}
        }

        // Handicap setup
        if (x == -2) {
            System.err.println("Handicap: " + y);
            if ((boardHandler.getGameData().handicap >= 2 && playMode == PLAY_MODE_IGS_OBSERVE) ||
                    (boardHandler.getGameData().handicap == y && playMode == PLAY_MODE_IGS_PLAY)) {
                System.err.println("Already have handicap. Aborting.");
                return;
            }
            boardHandler.lock();
            boardHandler.setHandicap(y);
            boardHandler.getGameData().handicap = y;
            // boardHandler.getGameData().komi = 0.5f;
            boardHandler.updateGUI();
            setPositionModified(true);
            repaint();
            boardHandler.unlock();
            playSound = false;
        }
        // Normal move or pass
        else {
            boardHandler.lock();
            if (boardHandler.addStoneIGS(c, x, y, captures) &&
                    (playSound || gGo.getSettings().getIGSDisplayMoves())) {
                addStoneSprite(null, c, x, y);
                // addGhostSprites();  No variations on IGS
                updateLastMoveMark(x, y);
                updateGraphics();
            }
            // Remove last move mark when passing
            else if (x == -1 && y == -1) {
                updateLastMoveMark(20, 20);
                updateGraphics();
            }
            else
                playSound = false;
            boardHandler.unlock();
        }

        // Update and switch clocks
        try {
            mainFrame.getClockWhite().setCurrentTime(whiteTime);
            mainFrame.getClockBlack().setCurrentTime(blackTime);

            // Don't run clocks with "0 0" time setting
            if (!(whiteTime.getTime() == 0 && whiteTime.getStones() == -1 &&
                    blackTime.getTime() == 0 && blackTime.getStones() == -1)) {
                if (c == STONE_BLACK) {
                    mainFrame.getClockBlack().stop();
                    mainFrame.getClockWhite().start();
                }
                else {
                    mainFrame.getClockWhite().stop();
                    mainFrame.getClockBlack().start();
                }
            }
        } catch (NullPointerException e) {
            System.err.println("Failed to update clocks: " + e);
        } catch (ClassCastException e) {
            System.err.println("Failed to update clocks: " + e);
        }

        // Ugly hack, force full redraw after first move
        // I don't know why, but on Windoze the first move is occasionally not drawn
        if (playSound && boardHandler.getTree().getCurrent().getMoveNumber() == 1) {
            setPositionModified(true);
            repaint();
        }

        // Play sound
        if (playSound) {
            if (x == -1 && y == -1)
                SoundHandler.playPass();
            else
                SoundHandler.playClick();
        }
    } //}}}

    //{{{ doGTPMove() method
    /**
     *  Do a move coming from a GTP engine. Paint the stone on the board, play the click sound, and update the last move mark.
     *
     *@param  c  Color of the played stone
     *@param  x  X coordinate of the played stone
     *@param  y  Y coordinate of the played stone
     */
    public synchronized void doGTPMove(int c, int x, int y) {
        // System.err.println("Board.doGTPMove() " + x + "/" + y + (c == STONE_BLACK ? " B" : " W"));

        checkMove = false;
        if (x == -1 && y == -1)
            boardHandler.doPass(false);
        else
            doMove(c, x, y, true);
        checkMove = true;

        // Ugly hack, force full redraw after first move
        // I don't know why, but on Windoze the first move is occasionally not drawn
        if (boardHandler.getTree().getCurrent().getMoveNumber() == 1) {
            setPositionModified(true);
            repaint();
        }
    } //}}}

    //{{{ doPass() method
    /**  Pass a move */
    public void doPass() {
        if (playMode == PLAY_MODE_GTP || playMode == PLAY_MODE_IGS_PLAY) {
            if (!mayMove())
                return;

            ((PlayingMainFrame)mainFrame).moveDone(boardHandler.getBlackTurn() ? STONE_BLACK : STONE_WHITE, -1, -1);
            if (playMode == PLAY_MODE_IGS_PLAY)
                return; // Dont perform actual pass here
        }

        boardHandler.lock();
        boardHandler.doPass(false);
        boardHandler.unlock();
    } //}}}

    //{{{ doResign() method
    /**  Resign game */
    public void doResign() {
        if (playMode == PLAY_MODE_GTP || playMode == PLAY_MODE_IGS_PLAY)
            ((PlayingMainFrame)mainFrame).doResign(boardHandler.getBlackTurn() ? STONE_BLACK : STONE_WHITE);
    } //}}}

    //{{{ doIGSDone() method
    /**  Game finished, send done to IGS */
    public void doIGSDone() {
        if (playMode == PLAY_MODE_IGS_PLAY) {
            if (!mayMove())
                return;

            ((PlayingMainFrame)mainFrame).doDone();
        }
    } //}}}

    //{{{ undoMove() method
    /**  Undo a move */
    public void undoMove() {
        if (playMode == PLAY_MODE_GTP || playMode == PLAY_MODE_IGS_PLAY) {
            if (!mayMove())
                return;

            int counter = ((PlayingMainFrame)mainFrame).undoMove();

            // Do not undo yet in own IGS games. This will be done when IGS sends the undo notify.
            if (playMode != PLAY_MODE_IGS_PLAY) {
                for (int i = 0; i < counter; i++)
                    boardHandler.deleteNode();
            }
        }
    } //}}}

    //{{{ addMark() methods
    /**
     *  Draw a mark on the board and add it to the MarkHandler storage
     *
     *@param  x       X position of the mark to add
     *@param  y       Y position of the mark to add
     *@param  type    Mark type
     *@param  number  Used for numbering moves. If -1, this is ignored and a number is automatically created
     */
    public void addMark(int x, int y, int type, int number) {
        if (doAddMark(null, x, y, type, number))
            updateGraphics();
    }

    /**
     *  Draw a mark on the board and add it to the MarkHandler storage
     *
     *@param  x     X position of the mark to add
     *@param  y     Y position of the mark to add
     *@param  type  Mark type
     */
    public void addMark(int x, int y, int type) {
        addMark(x, y, type, -1);
    } //}}}

    //{{{ Drawing methos

    //{{{ paintComponent() method
    /**
     *  Paint the board
     *
     *@param  g  Graphics object
     */
    public synchronized void paintComponent(Graphics g) {
        // System.err.println("PAINT COMPONENT");
        isready = false;

        boolean resized = calculateSize();

        // Size has changed, or buffer images dont exist. Recreate everything.
        if (resized || boardImageWithStones == null || boardImageWithoutStones == null || kaya == null) {
            boardImageWithStones = (BufferedImage)createImage(getWidth(), getHeight());
            boardImageWithoutStones = (BufferedImage)createImage(getWidth(), getHeight());
            activeImage = (BufferedImage)createImage(getWidth(), getHeight());

            Graphics2D big = (Graphics2D)boardImageWithoutStones.createGraphics();
            big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // big.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // big.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            // big.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            // big.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            big.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawBackground(big);
            drawGatter(big);
            if (showCoords)
                drawCoordinates(big);

            positionModified = true;
            // System.err.println("RESIZED");
        }

        if (positionModified && !locked) {
            // System.err.println("POS MODIFIED");
            // Copy image_without_stones to image_with_stones
            Graphics2D big = (Graphics2D)boardImageWithStones.createGraphics();
            big.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // big.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // big.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            // big.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            // big.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            big.drawImage(boardImageWithoutStones, 0, 0, this);
            // Draw stones on image_with_stones
            boolean res = drawStones(big);
            // Draw marks on image_with_stones
            drawMarks(big);
            // Copy board to activeImage
            ((Graphics2D)activeImage.getGraphics()).drawImage(boardImageWithStones, 0, 0, this);

            positionModified = !res;
            // System.err.println("Pos modified now " + positionModified);
        }

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        // g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        // g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        // g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // Clear the background
        super.paintComponent(g2);

        // Render image on panel
        g2.drawImage(boardImageWithStones, 0, 0, this);

        if (!locked) {
            // Draw cursor ghost stone
            if (curPosX != -1 && curPosY != -1 &&
                    !boardHandler.hasStone(curPosX, curPosY))
                addGhostSprite(g2, boardHandler.getBlackTurn() ? STONE_BLACK : STONE_WHITE, curPosX, curPosY);

            // Draw variation ghosts
            if (gGo.getSettings().getShowVariationGhosts())
                addGhostSprites(g2);

            // Draw last move mark, if we have a last move in normal mode
            Move m = boardHandler.getTree().getCurrent();
            if (m.getX() >= 1 && m.getY() <= boardSize && m.getGameMode() == MODE_NORMAL)
                updateLastMoveMark(g2, m.getX(), m.getY());
        }

        isready = true;
        this.notify();
    } //}}}

    //{{{ getActiveGraphics() method
    /**
     *  Gets the Graphics object of the active image to draw on
     *
     *@return    The Graphics of the active image
     */
    public Graphics2D getActiveGraphics() {
        if (activeImage != null)
            return (Graphics2D)(activeImage.getGraphics());
        else
            return (Graphics2D)getGraphics();
    } //}}}

    //{{{ updateGraphics() method
    /**
     *  Finally draws the active image on the board. Called once when something happened on the board.
     */
    public synchronized void updateGraphics() {
        // Wait until repaint is done
        if (!isready) {
            System.err.println("Board.updateGraphics(): Waiting on monitor");
            try {
                this.wait();
            } catch (InterruptedException e) {}
        }

        ((Graphics2D)getGraphics()).drawImage(activeImage, 0, 0, this);
    } //}}}

    //{{{ addStoneSprites() method
    /**
     *  Paint a group of stones on the board. This way drawing several stones is
     *  done in one step.
     *
     *@param  stones  ArrayList containing the stone objects to draw
     */
    public synchronized void addStoneSprites(ArrayList stones) {
        /*
         *  Disabled for now, this seems to block occasionally when
         *  deiconifying the window.
         *  / Wait until repaint is done
         *  if (!isready) {
         *  System.err.println("Board.addStoneSprites(): Waiting on monitor");
         *  try {
         *  this.wait();
         *  } catch (InterruptedException e) {}
         *  }
         */
        Graphics2D g = (Graphics2D)boardImageWithStones.createGraphics();
        Stone s;
        for (Iterator it = stones.iterator(); it.hasNext(); ) {
            s = (Stone)it.next();
            // Check if stone is marked dead, then draw as ghost
            if (!s.isDead() &&
                    (!markHandler.hasMark(s.getX(), s.getY()) ||
                    (markHandler.getMark(s.getX(), s.getY()).getType() != MARK_TERR_BLACK &&
                    markHandler.getMark(s.getX(), s.getY()).getType() != MARK_TERR_WHITE)))
                addStoneSprite(g, s.getColor(), s.getX(), s.getY());
            else
                addGhostSprite(g, s.getColor(), s.getX(), s.getY(), false);
        }
        getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
    } //}}}

    //{{{ addStoneSprite() methods
    /**
     *  Paint a stone on the board
     *
     *@param  g          Graphics object
     *@param  col        Stone color
     *@param  x          X position on the board
     *@param  y          Y position on the board
     *@param  checkMark  The feature to be added to the StoneSprite attribute
     *@return            True if displaying the stone was successful, else false
     */
    public boolean addStoneSprite(Graphics2D g, int col, int x, int y, boolean checkMark) {
        // System.err.println("addStoneSprite at " + x + "/" + y + ", " + col);

        if (x < 1 || x > boardSize || y < 1 || y > boardSize || col == STONE_NONE) {
            System.err.println("NONE");
            return false;
        }

        Image stoneImg;
        if (col == STONE_BLACK)
            stoneImg = scaledBlackStoneImg;
        else
            stoneImg = scaledWhiteStoneImg[whitestonematrix[x][y]];

        boolean flag = false;
        boolean res = true;

        try {
            if (g == null) {
                g = (Graphics2D)boardImageWithStones.createGraphics();
                flag = true;
            }

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            // g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            if (!g.drawImage(stoneImg,
                    offsetX + square_size * (x - 1) - square_size / 2 + stoneShift,
                    offsetY + square_size * (y - 1) - square_size / 2 + stoneShift,
                    this)) {
                // System.err.println("Failed to display stone image.");
                positionModified = true;
                res = false;
            }
            if (checkMark && markHandler.hasMark(x, y))
                drawMark(g, markHandler.getMark(x, y));
            if (flag)
                getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
        } catch (NullPointerException e) {
            System.err.println("Failed to load stone image.");
            return false;
        }

        return res;
    }

    /**
     *  Paint a stone on the given graphics context
     *
     *@param  g    Graphics context to paint on
     *@param  col  Stone color
     *@param  x    X position
     *@param  y    Y position
     *@return      True if successful, else false
     */
    public boolean addStoneSprite(Graphics2D g, int col, int x, int y) {
        return addStoneSprite(g, col, x, y, true);
    } //}}}

    //{{{ removeStoneSprites() method
    /**
     *  Remove a list of stones
     *
     *@param  stones  Arraylist containing the stone objects to remove from the board
     */
    public void removeStoneSprites(ArrayList stones) {
        Graphics2D g = (Graphics2D)boardImageWithStones.createGraphics();
        Stone s;
        for (Iterator it = stones.iterator(); it.hasNext(); ) {
            s = (Stone)it.next();
            removeStoneSprite(g, s.getX(), s.getY());
        }
        // Called in addStoneSprites
        // getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
    } //}}}

    //{{{ removeStoneSprite() methods
    /**
     *  Remove a stone from the board
     *
     *@param  g                 Graphics context to paint on
     *@param  x                 X position
     *@param  y                 Y position
     *@param  redrawNeighbough  If true, redraw right and bottom neigbough white stones
     */
    public void removeStoneSprite(Graphics2D g, int x, int y, boolean redrawNeighbough) {
        // System.err.println("removeStoneSprite at " + x + "/" + y);

        if (x < 1 || x > boardSize || y < 1 || y > boardSize) {
            // System.err.println("removeStoneSprite - Invalid stone: " + x + ", " + y);
            return;
        }

        boolean flag = false;

        try {
            if (g == null) {
                g = (Graphics2D)boardImageWithStones.createGraphics();
                flag = true;
            }

            // Copy the necassary part of the empty image over
            int x1 = offsetX + square_size * (x - 1) - square_size / 2 + stoneShift;
            int y1 = offsetY + square_size * (y - 1) - square_size / 2 + stoneShift;
            boolean hasRightNeighbour = boardHandler.hasStone(x + 1, y);
            boolean hasBottomNeighbour = boardHandler.hasStone(x, y + 1);
            int x2 = x1 + square_size + (hasRightNeighbour ? 0 : stoneShift);
            int y2 = y1 + square_size + (hasBottomNeighbour ? 0 : stoneShift);
            g.drawImage(boardImageWithoutStones,
                    x1, y1, x2, y2,
                    x1, y1, x2, y2,
                    this);
            if (markHandler.hasMark(x, y))
                drawMark(g, markHandler.getMark(x, y));
            // Redraw right and left white stones because of overlap effect. Not needed for black stones.
            if (redrawNeighbough) {
                if (hasRightNeighbour && boardHandler.getStoneColorAt(x + 1, y) == STONE_WHITE) {
                    removeStoneSprite(g, x + 1, y, false);
                    addStoneSprite(g, STONE_WHITE, x + 1, y, false);
                }
                if (hasBottomNeighbour && boardHandler.getStoneColorAt(x, y + 1) == STONE_WHITE) {
                    removeStoneSprite(g, x, y + 1, false);
                    addStoneSprite(g, boardHandler.getStoneHandler().getStone(x, y + 1).getColor(), x, y + 1, false);
                }
            }
            if (flag)
                getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
        } catch (NullPointerException e) {
            System.err.println("Failed to remove stone sprite: " + e);
        }
    }

    /**
     *  Remove a stone from the board
     *
     *@param  g  Graphics context to paint on
     *@param  x  X position
     *@param  y  Y position
     */
    public void removeStoneSprite(Graphics2D g, int x, int y) {
        removeStoneSprite(g, x, y, true);
    } //}}}

    //{{{ addGhostSprites() methods
    /**
     *  Paint the variation ghosts on the given Graphics object
     *
     *@param  g  Graphics object to paint on
     */
    public void addGhostSprites(Graphics2D g) {
        if (!ghosts.isEmpty()) {
            Stone s;
            for (Iterator it = ghosts.iterator(); it.hasNext(); ) {
                s = (Stone)it.next();
                if (!boardHandler.hasStone(s.getX(), s.getY()))
                    addGhostSprite(g, s.getColor(), s.getX(), s.getY());
            }
        }
    }

    /**  Paint the variation ghosts on the default Graphics object */
    public void addGhostSprites() {
        if (!ghosts.isEmpty()) {
            addGhostSprites((Graphics2D)boardImageWithStones.createGraphics());
            getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
        }
    } //}}}

    //{{{ removeGhosts() method
    /**  Remove all variation ghosts */
    public void removeGhosts() {
        if (!ghosts.isEmpty()) {
            Graphics2D g = (Graphics2D)boardImageWithStones.createGraphics();
            Stone s;
            for (Iterator it = ghosts.iterator(); it.hasNext(); ) {
                s = (Stone)it.next();
                removeGhostSprite(g, s.getX(), s.getY(), true);
            }
            getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
            ghosts.clear();
        }
    } //}}}

    //{{{ addGhostSprite() methods
    /**
     *  Adds a feature to the GhostSprite attribute of the Board object
     *
     *@param  g          Graphics object
     *@param  col        Stone color
     *@param  x          X position on the board
     *@param  y          Y position on the board
     *@param  checkMark  The feature to be added to the GhostSprite attribute
     *@return            True if displaying the stone was successful, else false.
     */
    public boolean addGhostSprite(Graphics2D g, int col, int x, int y, boolean checkMark) {
        // System.err.println("addGhostSprite at " + x + "/" + y + ", " + (col == STONE_BLACK ? "B" : "W"));

        if (x < 1 || x > boardSize || y < 1 || y > boardSize) {
            // System.err.println("addGhostSprite - Invalid stone: " + x + ", " + y);
            return false;
        }

        if (g == null) {
            System.err.println("Error displaying ghost stone.");
            return false;
        }

        // Check if we have a mark. It must be redrawn as the underlying ghost color might have changed.
        boolean hasMark = false;
        if (checkMark) {
            hasMark = markHandler.hasMark(x, y);
            if (hasMark)
                removeMark(g, x, y, true);
        }

        // Render the stone with 50% transparency
        AlphaComposite ac =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (col == STONE_BLACK ? 0.4f : 0.6f));
        g.setComposite(ac);

        Image stoneImg;
        if (col == STONE_BLACK)
            stoneImg = ImageHandler.getStoneBlackImage();

        else
            stoneImg = ImageHandler.getStoneWhiteImage(whitestonematrix[x][y]);

        try {
            if (!g.drawImage(stoneImg,
                    offsetX + square_size * (x - 1) - square_size / 2 + stoneShift,
                    offsetY + square_size * (y - 1) - square_size / 2 + stoneShift,
                    square_size, square_size, this)) {
                System.err.println("Failed to display ghost image.");
                return false;
            }

            // Set back to normal transparency
            ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
            g.setComposite(ac);

            // Redraw mark if we had one
            if (hasMark)
                drawMark(g, markHandler.getMark(x, y));
        } catch (NullPointerException e) {
            System.err.println("Failed to load stone image.");
            return false;
        }
        return true;
    }

    /**
     *  Adds a feature to the GhostSprite attribute of the Board object
     *
     *@param  g    The feature to be added to the GhostSprite attribute
     *@param  col  The feature to be added to the GhostSprite attribute
     *@param  x    The feature to be added to the GhostSprite attribute
     *@param  y    The feature to be added to the GhostSprite attribute
     *@return      True if successful, else false
     */
    public boolean addGhostSprite(Graphics2D g, int col, int x, int y) {
        return addGhostSprite(g, col, x, y, true);
    } //}}}

    //{{{ checkGhostSprite() method
    /**
     *  Check if there is a ghost on the given position
     *
     *@param  x  X position
     *@param  y  Y position
     */
    public void checkGhostSprite(int x, int y) {
        if (hasVarGhost(STONE_BLACK, x, y)) {
            addGhostSprite((Graphics2D)boardImageWithStones.createGraphics(), STONE_BLACK, x, y);
            getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
        }
        else if (hasVarGhost(STONE_WHITE, x, y)) {
            addGhostSprite((Graphics2D)boardImageWithStones.createGraphics(), STONE_WHITE, x, y);
            getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
        }
    } //}}}

    //{{{ removeGhostSprite() method
    /**
     *  Remove a ghost from the board
     *
     *@param  g         Graphics context to paint on
     *@param  x         X position
     *@param  y         Y position
     *@param  varGhost  If true, paint on boardImageWithoutStones, else on boardImageWithStones
     */
    public void removeGhostSprite(Graphics2D g, int x, int y, boolean varGhost) {
        if (x < 1 || x > boardSize || y < 1 || y > boardSize) {
            // System.err.println("removeGhostSprite - Invalid stone: " + x + ", " + y);
            return;
        }

        boolean flag = false;

        try {
            if (g == null) {
                g = (Graphics2D)boardImageWithStones.createGraphics();
                flag = true;
            }

            // Copy the necassary part of the empty image or the image with stones over
            int x1 = offsetX + square_size * (x - 1) - square_size / 2 + stoneShift;
            int y1 = offsetY + square_size * (y - 1) - square_size / 2 + stoneShift;
            int x2 = x1 + square_size + (boardHandler.hasStone(x + 1, y) ? 0 : stoneShift);
            int y2 = y1 + square_size + (boardHandler.hasStone(x, y + 1) ? 0 : stoneShift);
            g.drawImage(varGhost ? boardImageWithoutStones : boardImageWithStones,
                    x1, y1, x2, y2,
                    x1, y1, x2, y2,
                    this);
            if (flag)
                getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
        } catch (NullPointerException e) {
            System.err.println("Failed to remove ghost sprite: " + e);
        }
    } //}}}

    //{{{ doAddMark() method
    /**
     *  Draw a mark on the board and add it to the MarkHandler storage
     *
     *@param  g       Graphics object
     *@param  x       X position of the mark to add
     *@param  y       Y position of the mark to add
     *@param  type    Mark type
     *@param  number  Used for numbering moves. If -1, this is ignored and a number is automatically created
     *@return         True if successful, else false
     */
    private boolean doAddMark(Graphics2D g, int x, int y, int type, int number) {
        if (markHandler.hasMark(x, y))
            return false;

        boolean flag = false;
        if (g == null) {
            g = (Graphics2D)boardImageWithStones.createGraphics();
            flag = true;
        }

        Mark mark = null;

        switch (type) {
            case MARK_SQUARE:
                mark = new MarkSquare(x, y);
                break;
            case MARK_CROSS:
                mark = new MarkCross(x, y);
                break;
            case MARK_TRIANGLE:
                mark = new MarkTriangle(x, y);
                break;
            case MARK_CIRCLE:
                mark = new MarkCircle(x, y);
                break;
            case MARK_TEXT:
            {
                String txt = markHandler.getNextLetter();
                mark = new MarkText(x, y, txt);
                if (txt.length() == 1) {
                    MarkText tmp = (MarkText)mark;
                    tmp.setCounter(markHandler.getStringCounter());
                }
                boardHandler.getTree().getCurrent().getMatrix().setMarkText(x, y, txt);
            }
                break;
            case MARK_NUMBER:
            {
                int n;
                if (number == -1)
                    n = markHandler.getNextNumber();
                else {
                    n = number;
                    markHandler.setNumberOccupied(n);
                }
                mark = new MarkNumber(x, y, n);
                MarkNumber tmp = (MarkNumber)mark;
                tmp.setCounter(n - 1);
                boardHandler.getTree().getCurrent().getMatrix().setMarkText(x, y, Integer.toString(n));
            }
                break;
            case MARK_TERR_BLACK:
                // System.err.println("ODD THING WAS CALLED");
                mark = new MarkTerr(x, y, STONE_BLACK);
                break;
            case MARK_TERR_WHITE:
                // System.err.println("ODD THING WAS CALLED");
                mark = new MarkTerr(x, y, STONE_WHITE);
                break;
        }

        if (drawMark(g, mark)) {
            markHandler.addMark(mark);
            // TODO boardHandler->editMark(x, y, t, txt);
            boardHandler.editMark(x, y, type);
        }

        if (flag)
            getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);

        return true;
    } //}}}

    //{{{ removeMark() methods
    /**
     *  Remove a mark from the board
     *
     *@param  g        Graphics object. If null, image_with_stones is used
     *@param  x        X position of the mark to remove
     *@param  y        Y position of the mark to remove
     *@param  noCheck  If true, ignore MarkHandler storage. Used for last-move-mark.
     *@return          True if successful, else false
     *@see             #removeMark(Graphics2D, int, int)
     */
    public boolean removeMark(Graphics2D g, int x, int y, boolean noCheck) {
        // System.err.println("Removing mark at " + x + "/" + y + ", noCheck = " + noCheck);

        if (!noCheck && !markHandler.hasMark(x, y))
            return false;

        boolean flag = false;

        if (!noCheck) {
            markHandler.removeMark(x, y);
            boardHandler.editMark(x, y, MARK_NONE);
        }

        try {
            if (g == null) {
                g = (Graphics2D)boardImageWithStones.createGraphics();
                flag = true;
            }

            // Copy the necassary part of the empty image over
            int x1 = offsetX + square_size * (x - 1) - square_size / 2 + stoneShift;
            int y1 = offsetY + square_size * (y - 1) - square_size / 2 + stoneShift;
            int x2 = x1 + square_size + (boardHandler.hasStone(x + 1, y) ? 0 : stoneShift);
            int y2 = y1 + square_size + (boardHandler.hasStone(x, y + 1) ? 0 : stoneShift);
            g.drawImage(boardImageWithoutStones,
                    x1, y1, x2, y2,
                    x1, y1, x2, y2,
                    this);
            // Check if we had a stone before
            if (boardHandler.hasStone(x, y)) {
                Stone s = boardHandler.getStoneHandler().getStone(x, y);
                addStoneSprite(g, s.getColor(), x, y, false);
            }
            if (flag)
                getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
        } catch (NullPointerException e) {
            System.err.println("Failed to remove mark: " + e);
        }
        return true;
    }

    /**
     *  Remove a mark from the board
     *
     *@param  g  Graphics object. If null, image_with_stones is used
     *@param  x  X position of the mark to remove
     *@param  y  Y position of the mark to remove
     *@return    True if successful, else false
     *@see       #removeMark(Graphics2D, int, int, boolean)
     */
    public boolean removeMark(Graphics2D g, int x, int y) {
        return removeMark(g, x, y, false);
    } //}}}

    //{{{ updateLastMoveMark() methods
    /**
     *  Update the last-move-mark
     *
     *@param  x  X position of the last move
     *@param  y  Y position of the last move
     *@see       #updateLastMoveMark(Graphics2D, int, int)
     */
    public void updateLastMoveMark(int x, int y) {
        updateLastMoveMark(getActiveGraphics(), x, y);
    }

    /**
     *  Update the last-move-mark
     *
     *@param  g  Graphics object to draw on
     *@param  x  X position of the last move
     *@param  y  Y position of the last move
     *@see       #updateLastMoveMark(int, int)
     */
    public void updateLastMoveMark(Graphics2D g, int x, int y) {
        // Passing
        if (x == 20 && y == 20) {
            Move m = boardHandler.getTree().getCurrent().parent;
            if (m != null)
                removeMark(g, m.getX(), m.getY(), true);
            return;
        }
        if (!markHandler.hasMark(x, y))
            drawMark(g, new MarkCircle(x, y));
    } //}}}

    //{{{ drawGatter() method
    /**
     *  Draw the grid
     *
     *@param  g  Graphics object
     */
    private void drawGatter(Graphics2D g) {
        // Draw vertical lines
        for (int i = 0; i < boardSize; i++)
            g.draw(new Line2D.Double(offsetX + square_size * i, offsetY,
                    offsetX + square_size * i, offsetY + board_pixel_size));

        // Draw horizontal lines
        for (int i = 0; i < boardSize; i++)
            g.draw(new Line2D.Double(offsetX, offsetY + square_size * i,
                    offsetX + board_pixel_size, offsetY + square_size * i));

        // Draw the little circles on the starpoints
        if (boardSize > 11) {
            drawStarPoint(g, 4, 4);
            drawStarPoint(g, boardSize - 3, 4);
            drawStarPoint(g, 4, boardSize - 3);
            drawStarPoint(g, boardSize - 3, boardSize - 3);
            if (boardSize % 2 != 0) {
                drawStarPoint(g, (boardSize + 1) / 2, 4);
                drawStarPoint(g, (boardSize + 1) / 2, boardSize - 3);
                drawStarPoint(g, 4, (boardSize + 1) / 2);
                drawStarPoint(g, boardSize - 3, (boardSize + 1) / 2);
                drawStarPoint(g, (boardSize + 1) / 2, (boardSize + 1) / 2);
            }
        }
        else {
            drawStarPoint(g, 3, 3);
            drawStarPoint(g, 3, boardSize - 2);
            drawStarPoint(g, boardSize - 2, 3);
            drawStarPoint(g, boardSize - 2, boardSize - 2);
            if (boardSize % 2 != 0)
                drawStarPoint(g, (boardSize + 1) / 2, (boardSize + 1) / 2);
        }
    } //}}}

    //{{{ drawStarPoint() method
    /**
     *  Paint a starpoint, called from drawGatter
     *
     *@param  x  X coordinate
     *@param  y  Y coordinate
     *@param  g  Graphics object
     */
    private void drawStarPoint(Graphics2D g, int x, int y) {
        int size = square_size / 5;

        if (size < 4)
            size = 4;

        g.fill(new Ellipse2D.Double((double)(offsetX + 0.5 + square_size * (x - 1)) - (double)size / 2.0,
                (double)(offsetY + 0.5 + square_size * (y - 1)) - (double)size / 2.0,
                size, size));
    } //}}}

    //{{{ drawBackground() method
    /**
     *  Draw the background with the green table and the wooden board
     *
     *@param  g  Graphics object
     */
    private void drawBackground(Graphics2D g) {
        //{{{ Table
        /*
         *  Table
         */
        Image tableImg = ImageHandler.getTableImage();
        try {
            int size = tableImg.getWidth(this);
            if (size == -1) {
                System.err.println("Could not determine table image size.");
                return;
            }

            // This is less elegant than the old texture code, but -much- faster
            for (int x = 0, w = getWidth(); x < w; x += size)
                for (int y = 0, h = getHeight(); y < h; y += size)
                    g.drawImage(tableImg, AffineTransform.getTranslateInstance(x, y), this);
        } catch (NullPointerException e) {
            System.err.println("Failed to draw the table background.");
        } //}}}

        //{{{ Board
        /*
         *  Board
         */
        try {
            int bsize = board_pixel_size + offset * 2;
            kaya = (BufferedImage)createImage(bsize, bsize);
            Graphics2D big = (Graphics2D)kaya.createGraphics();
            // big.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            // big.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int ksizeX = ImageHandler.getBoardImage().getWidth(this);
            int ksizeY = ImageHandler.getBoardImage().getHeight(this);
            big.drawImage(
                    ImageHandler.getBoardImage(),
                    AffineTransform.getScaleInstance(
                    (float)bsize / (float)(ksizeX),
                    (float)bsize / (float)(ksizeY)),
                    this);
            g.drawImage(kaya, null, offsetX - offset, offsetY - offset);
        } catch (NullPointerException e) {
            System.err.println("Failed to draw the kaya board: " + e);
        } //}}}
    } //}}}

    //{{{ drawStones() method
    /**
     *  Draw the stone sprites on the board, call from paintComponent
     *
     *@param  g  Graphics object
     *@return    Description of the Return Value
     */
    private boolean drawStones(Graphics2D g) {
        boolean res = true;

        try {
            Hashtable stones = boardHandler.getStoneHandler().getStones();

            if (stones == null || stones.isEmpty())
                return true;

            for (Enumeration e = stones.elements(); e.hasMoreElements(); ) {
                Stone s = (Stone)e.nextElement();
                // Check if stone is marked dead, then draw as ghost
                if (!s.isDead()) {
                    if (!addStoneSprite(g, s.getColor(), s.getX(), s.getY(), false))
                        res = false;
                }
                else {
                    if (!addGhostSprite(g, s.getColor(), s.getX(), s.getY(), false))
                        res = false;
                }
            }
        } catch (NullPointerException e) {
            System.err.println("Failed to draw stones: " + e);
            return false;
        }

        return res;
    } //}}}

    //{{{ drawCoordinates() method
    /**
     *  Draw the coordinates of the board
     *
     *@param  g  Graphics object
     */
    private void drawCoordinates(Graphics2D g) {
        final int off = boardSize > 9 ? 4 : 6;
        FontMetrics fm = g.getFontMetrics();

        // Draw vertical coordinates. Numbers
        for (int i = 0; i < boardSize; i++) {
            String s = Integer.toString(boardSize - i);
            int w = (int)(fm.getStringBounds(s, g).getWidth());
            int h = (int)(fm.getStringBounds(s, g).getHeight());

            // Left side
            g.drawString(s,
                    offsetX - offset / 2 - w / 2 - off,
                    offsetY + square_size * i + h / 3);

            // Right side
            g.drawString(s,
                    offsetX + board_pixel_size + offset / 2 - w / 2 + off,
                    offsetY + square_size * i + h / 3);
        }

        // Draw horizontal coordinates. Letters (Note: Skip 'i')
        for (int i = 0; i < boardSize; i++) {
            String s = String.valueOf((char)('A' + (i < 8 ? i : i + 1)));
            int w = (int)(fm.getStringBounds(s, g).getWidth());
            int h = (int)(fm.getStringBounds(s, g).getHeight());

            // Top side
            g.drawString(s,
                    offsetX + square_size * i - w / 2,
                    offsetY - offset / 2 + h / 3 - off);

            // Bottom side
            g.drawString(s,
                    offsetX + square_size * i - w / 2,
                    offsetY + offset / 2 + board_pixel_size + h / 3 + off);
        }
    } //}}}

    //{{{ drawMark() method
    /**
     *  Draw the mark on the given Graphics object.
     *
     *@param  g     Graphics object to paint on
     *@param  mark  The mark object to pain
     *@return       True of painted successful, else false
     */
    private boolean drawMark(Graphics2D g, Mark mark) {
        boolean overlay = false;
        boolean doShift = false;

        // Get mark color
        int color = STONE_BLACK;
        if (boardHandler.hasStone(mark.getX(), mark.getY())) {
            doShift = true;
            if (mark.getType() == MARK_TERR_BLACK || mark.getType() == MARK_TERR_WHITE) {
                boardHandler.getStoneHandler().getStone(mark.getX(), mark.getY()).setDead(true);
                boardHandler.setMarkedDead(true);
                boardHandler.getTree().getCurrent().setScored(true);
            }
            if (boardHandler.getStoneColorAt(mark.getX(), mark.getY()) == STONE_BLACK ||
                    hasVarGhost(STONE_BLACK, mark.getX(), mark.getY()))
                color = STONE_WHITE;
        }
        else {
            // Check if a variation ghost is underlaying and adjust color
            if (gGo.getSettings().getShowVariationGhosts()) {
                if (hasVarGhost(STONE_BLACK, mark.getX(), mark.getY()))
                    color = STONE_WHITE;
                else if (hasVarGhost(STONE_WHITE, mark.getX(), mark.getY()))
                    color = STONE_BLACK;
            }

            // No stone, no var ghost, and text or number mark: Use overlay
            if ((!gGo.getSettings().getShowVariationGhosts() ||
                    !hasVarGhost(mark.getX(), mark.getY())) &&
                    (mark.getType() == MARK_TEXT || mark.getType() == MARK_NUMBER))
                overlay = true;
        }

        try {
            Color oldColor = g.getColor();
            g.setColor(color == STONE_BLACK ? Color.black : Color.white);
            // If it is text or number and no stone is underlaying, clear background
            if (overlay) {
                try {
                    // Get the correct rectangle of the original image to overlay.
                    int x1 = square_size * (mark.getX() - 1) - (int)(square_size * 0.4);
                    int y1 = square_size * (mark.getY() - 1) - (int)(square_size * 0.4);
                    int x2 = x1 + (int)(square_size * 0.8);
                    int y2 = y1 + (int)(square_size * 0.8);
                    g.drawImage(kaya, offsetX + x1, offsetY + y1, offsetX + x2, offsetY + y2,
                            offset + x1, offset + y1, offset + x2, offset + y2,
                            this);
                } catch (NullPointerException e) {
                    System.err.println("Failed to overlay board: " + e);
                }
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
            // g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            // g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            mark.drawShape(g,
                    offsetX + square_size * (mark.getX() - 1) + (doShift ? stoneShift : 0),
                    offsetY + square_size * (mark.getY() - 1) + (doShift ? stoneShift : 0),
                    square_size + stoneShift, square_size + stoneShift);
            g.setColor(oldColor);
        } catch (NullPointerException e) {
            System.err.println("Failed to draw mark: " + e);
            return false;
        }

        return true;
    } //}}}

    //{{{ drawMarks() methods
    /**
     *  Draw all marks of the current markHandler on a given Graphics object.
     *
     *@param  g  Graphics object to draw on
     *@see       #drawMarks()
     */
    protected void drawMarks(Graphics2D g) {
        if (markHandler.isEmpty())
            return;

        for (Enumeration e = markHandler.elements(); e.hasMoreElements(); )
            drawMark(g, (Mark)e.nextElement());
    }

    /**
     *  Draw all marks of the current markHandler on the image_with_stones.
     * This is called from updateAll() in StoneHandler.
     *
     *@see    #drawMarks(Graphics2D)
     */
    protected void drawMarks() {
        drawMarks((Graphics2D)boardImageWithStones.createGraphics());
        getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
    } //}}}

    //{{{ hideAllMarks() method
    /**  Remove all marks from the board and clean the markHandler container */
    public void hideAllMarks() {
        if (markHandler.isEmpty())
            return;

        Graphics2D g = (Graphics2D)boardImageWithStones.createGraphics();
        for (Enumeration e = markHandler.elements(); e.hasMoreElements(); ) {
            Mark m = (Mark)e.nextElement();
            removeMark(g, m.getX(), m.getY(), true);
        }
        getActiveGraphics().drawImage(boardImageWithStones, 0, 0, this);
        markHandler.clear();
    } //}}}

    //}}}

    //{{{ openFromString() method
    /**
     *  Open game from a String with the sgf content and display the game
     *
     *@param  toParse  String with sgf content
     *@return          True if successful, else false
     */
    public boolean openFromString(String toParse) {
        clearData();

        if (!boardHandler.loadFromString(toParse))
            return false;

        setModified(false);
        return true;
    } //}}}

    //{{{ openSGF() method
    /**
     *  Open a sgf file and display the game
     *
     *@param  fileName  File name of the file ot open
     *@param  remName   If true, remember the filename
     *@return           True if successful, else false
     */
    public boolean openSGF(final String fileName, final boolean remName) {
        // Clean up everything
        clearData();

        // Load the sgf
        boolean result = boardHandler.loadGame(fileName, remName);
        setModified(false);
        return result;
    } //}}}

    //{{{ saveSGF(String) method
    /**
     *  Save game to SGF file
     *
     *@param  fileName  Name of the to  be saved file
     *@return           True if successful, else false
     */
    public boolean saveSGF(String fileName) {
        boolean res = boardHandler.saveGame(fileName);
        if (res) {
            setModified(false);
            boardHandler.getGameData().fileName = fileName;
        }
        return res;
    } //}}}

    //{{{ saveSGF(File) method
    /**
     *  Save game to SGF file
     *
     *@param  file  File to be saved
     *@return       True if successful, else false
     */
    public boolean saveSGF(File file) {
        boolean res = boardHandler.saveGame(file);
        if (res) {
            setModified(false);
            boardHandler.getGameData().fileName = file.getAbsolutePath();
        }
        return res;
    } //}}}

    //{{{ openEditBoard() method
    /**  Open a board to edit the game. Used from IGS observed games */
    public void openEditBoard() {
        File tmpFile;
        try {
            tmpFile = File.createTempFile("ggo", ".sgf");
        } catch (IOException e) {
            System.err.println("Failed to create temporary file: " + e);
            return;
        }
        boardHandler.saveGame(tmpFile);
        if (playMode == PLAY_MODE_IGS_PLAY || playMode == PLAY_MODE_IGS_OBSERVE ||
                playMode == PLAY_MODE_GTP) {
            MainFrame mf = mainFrame.getMirrorFrame();
            if (mf == null) {
                mf = gGo.openNewMainFrame(mainFrame);
                mainFrame.setMirrorFrame(mf);
            }
            mf.openSGF(tmpFile.getAbsolutePath(), false);
        }
        else
            gGo.openNewMainFrame(tmpFile.getAbsolutePath(), false);
    } //}}}

    //{{{ Scoring

    //{{{ countScore() method
    /**  Enter score mode and start scoring process. */
    public void countScore() {
        // Switch to score mode
        boardHandler.setGameMode(MODE_SCORE);

        // Count the dead stones and add them to the captures. This way we keep
        // existing scoring (Cgoban2) and don't need to mark the dead stones again.
        long res = boardHandler.getStoneHandler().updateDeadMarks();
        int caps_black = (int)(res / 1000);
        int caps_white = (int)res % 1000;
        // System.err.println("CAPS BLACK = " + caps_black + ", WHITE = " + caps_white);

        boardHandler.enterScoreMode(caps_black, caps_white);
        boardHandler.countScore();

        setModified(true);
    } //}}}

    //{{{ doCountDont() method
    /**  Finish scoring. Calculate, display and save result. */
    public void doCountDone() {
        float totalWhite = boardHandler.getGameData().scoreCapsWhite + boardHandler.getGameData().scoreTerrWhite +
                boardHandler.getGameData().komi;
        float totalBlack = boardHandler.getGameData().scoreCapsBlack + boardHandler.getGameData().scoreTerrBlack;
        float result = 0;
        String rs;
        NumberFormat nf = NumberFormat.getNumberInstance(gGo.getLocale());
        String s =
                gGo.getBoardResources().getString("White") + "\n" +
                nf.format(boardHandler.getGameData().scoreTerrWhite) + " + " +
                nf.format(boardHandler.getGameData().scoreCapsWhite) + " + " +
                nf.format(boardHandler.getGameData().komi) + " = " +
                nf.format(totalWhite) +
                "\n" + gGo.getBoardResources().getString("Black") + "\n" +
                nf.format(boardHandler.getGameData().scoreTerrBlack) + " + " +
                nf.format(boardHandler.getGameData().scoreCapsBlack) + " = " +
                nf.format(totalBlack) + "\n";

        if (totalBlack > totalWhite) {
            result = totalBlack - totalWhite;
            s += MessageFormat.format(
                    gGo.getBoardResources().getString("end_score_message"),
                    new Object[]{gGo.getBoardResources().getString("Black"), new Float(result)});
            rs = "B+" + nf.format(result);
        }
        else if (totalWhite > totalBlack) {
            result = totalWhite - totalBlack;
            s += MessageFormat.format(
                    gGo.getBoardResources().getString("end_score_message"),
                    new Object[]{gGo.getBoardResources().getString("White"), new Float(result)});
            rs = "W+" + nf.format(result);
        }
        else {
            rs = gGo.getBoardResources().getString("Jigo");
            s += rs;
        }

        // Show points in sidebar
        try {
            mainFrame.getSideBar().setScore(totalWhite, totalBlack);

            if (JOptionPane.showConfirmDialog(
                    mainFrame,
                    s + "\n\n" +
                    gGo.getBoardResources().getString("update_gameinfo_question"),
                    gGo.getGTPResources().getString("Game_over"),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                boardHandler.getGameData().result = rs;
        } catch (NullPointerException e) {}

        boardHandler.getTree().getCurrent().setTerritoryMarked(false);
        boardHandler.getTree().getCurrent().setScore(totalBlack, totalWhite);
    } //}}}

    //}}}

    //{{{ handleClick() method
    /**
     *  Handle a single or double click on the board
     *
     *@param  x          X position on the board
     *@param  y          Y position on the board
     *@param  modifiers  MouseEvent modifiers
     */
    private void handleClick(int x, int y, int modifiers) {
        // System.err.println("Single Click: " + x + " " + y);

        // Button gesture outside the board?
        if (x < 1 || x > boardSize || y < 1 || y > boardSize)
            return;

        // Ok, we are inside the board, and it was no gesture.

        switch (boardHandler.getGameMode()) {
            case MODE_NORMAL:
                // Left click
                if ((modifiers & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
                    // Shift: Find move in main branch
                    if ((modifiers & MouseEvent.SHIFT_MASK) == MouseEvent.SHIFT_MASK)
                        boardHandler.findMoveByPos(x, y);
                    else
                        doMove((boardHandler.getBlackTurn() ? STONE_BLACK : STONE_WHITE), x, y, true);
                }
                // Right click & shift: Find move in this branch
                else if ((modifiers & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK &&
                        (modifiers & MouseEvent.SHIFT_MASK) == MouseEvent.SHIFT_MASK)
                    boardHandler.findMoveByPosInVar(x, y);
                break;
            case MODE_EDIT:
                // Left click
                if ((modifiers & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
                    if (mainFrame.getMarkType() == MARK_STONE) {
                        if (boardHandler.addStone(STONE_BLACK, x, y)) {
                            addStoneSprite(null, STONE_BLACK, x, y);
                            updateGraphics();
                        }
                    }
                    // Shift click and text mark: Customize text
                    else if ((modifiers & MouseEvent.SHIFT_MASK) == MouseEvent.SHIFT_MASK &&
                            mainFrame.getMarkType() == MARK_TEXT) {
                        // Check for existing text mark and get old text
                        String oldText = null;
                        Mark mark = markHandler.getMark(x, y);
                        if (mark != null && mark.getType() == MARK_TEXT) {
                            oldText = ((MarkText)(mark)).getText();
                            markHandler.removeMark(x, y);
                        }
                        else if (mark != null)
                            return; // Has a mark, but no text mark.

                        // Get new text from dialog
                        String newText = JOptionPane.showInputDialog(
                                mainFrame,
                                gGo.getBoardResources().getString("enter_custom_mark_text"),
                                oldText);
                        if (newText == null || newText.length() == 0)
                            return;

                        // Set new mark, or replace existing mark text
                        markHandler.rememberCustomizedText(newText);
                        addMark(x, y, MARK_TEXT);
                    }
                    else {
                        // Set a mark
                        addMark(x, y, getMarkType());
                    }
                }
                // Right click
                else if ((modifiers & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK) {
                    if (mainFrame.getMarkType() == MARK_STONE) {
                        if (boardHandler.addStone(STONE_WHITE, x, y)) {
                            addStoneSprite(null, STONE_WHITE, x, y);
                            updateGraphics();
                        }
                    }
                    else {
                        // Remove the mark
                        if (removeMark(null, x, y)) {
                            checkGhostSprite(x, y);
                            updateGraphics();
                        }
                    }
                }
                break;
            case MODE_SCORE:
                // IGS game in score progress?
                if (playMode == PLAY_MODE_IGS_PLAY) {
                    ((PlayingMainFrame)mainFrame).moveDone(STONE_NONE, x, y);
                    return;
                }

                // Left click: Mark or unmark as dead
                if ((modifiers & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)
                    boardHandler.markDeadStone(x, y);
                break;
        }
    } //}}}

    //{{{ convertCoordsToPoint() method
    /**
     *  Convert a coordinate to a point on the board (1-19 on 19x19 boards)
     *
     *@param  c  Coordinate
     *@param  o  OffsetX or OffsetY
     *@return    Point
     */
    private int convertCoordsToPoint(int c, int o) {
        int p = c - o + square_size / 2;
        if (p >= 0 && square_size > 0)
            return p / square_size + 1;
        else
            return -1;
    } //}}}

    //{{{ mayMove() method
    /**
     *  Check if its our turn in GTP/IGS game mode
     *
     *@return    True if it is our turn, else false
     */
    private boolean mayMove() {
        if (playMode != PLAY_MODE_GTP && playMode != PLAY_MODE_IGS_PLAY)
            return true;

        return ((PlayingMainFrame)mainFrame).mayMove(boardHandler.getBlackTurn() ? STONE_BLACK : STONE_WHITE);
    } //}}}

    //{{{ displayHandicapNotSupportedError() method
    /**  Board size does not support handicap set. Display a messagebox */
    void displayHandicapNotSupportedError() {
        JOptionPane.showMessageDialog(
                mainFrame,
                gGo.getBoardResources().getString("handicap_boardsize_not_supported"),
                gGo.getBoardResources().getString("error"),
                JOptionPane.ERROR_MESSAGE);
    } //}}}

    //{{{ displayInvalidHandicapError() method
    /**
     *  This handicap value is not supported. Display a messagebox
     *
     *@param  handicap  Description of the Parameter
     */
    void displayInvalidHandicapError(int handicap) {
        JOptionPane.showMessageDialog(mainFrame,
                MessageFormat.format(
                gGo.getIGSResources().getString("invalid_handicap_request"),
                new Object[]{String.valueOf(handicap)}),
                gGo.getBoardResources().getString("error"),
                JOptionPane.ERROR_MESSAGE);
    } //}}}

    //{{{ class BoardListener
    /**
     *  Listener class for MouseEvents
     *
     *@author     Peter Strempel
     *@version    $Revision: 1.17 $, $Date: 2002/10/23 01:50:42 $
     */
    // --- 1.3 ---
    // class BoardListener implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    class BoardListener implements MouseListener, MouseMotionListener, KeyListener {
        //{{{ private members
        private int lastX = -1, lastY = -1;
        //}}}

        //{{{ MouseEvents

        //{{{ mouseClicked() method
        /**
         *  Mouse was clicked
         *
         *@param  e  Mouse event
         */
        public void mouseClicked(MouseEvent e) {
        } //}}}

        //{{{ mousePressed() method
        /**
         *  Mouse button was pressed The current position is remembered, and if
         *  the mouse button is released again, an event will be handled if the
         *  position is the same. This is done to allow the mouse being moved
         *  few pixels during the mouse button is down.
         *
         *@param  e  Mouse event
         */
        public void mousePressed(MouseEvent e) {
            if (!editable || !mayMove())
                return;

            lastX = convertCoordsToPoint(e.getX(), offsetX);
            lastY = convertCoordsToPoint(e.getY(), offsetY);
        } //}}}

        //{{{ mouseReleased() method
        /**
         *  Mouse button was released. The position is compared with the
         *  position remembered in mousePressed.
         *
         *@param  e  Mouse event
         */
        public void mouseReleased(MouseEvent e) {
            // Right click in IGS observe frames. Append coordinates in inputField
            if (playMode == PLAY_MODE_IGS_OBSERVE &&
                    (e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK &&
                    e.getClickCount() == 2) {
                try {
                    ((IGSObserverFrame)mainFrame).addInputTextfieldCoords(
                            convertCoordsToPoint(e.getX(), offsetX),
                            convertCoordsToPoint(e.getY(), offsetY),
                            boardSize);
                } catch (ClassCastException ex) {
                    ex.printStackTrace();
                }
                return;
            }

            if (!editable || !mayMove())
                return;

            // Check for single or doubleclick type, only in own games, not for the editor
            if (playMode != PLAY_MODE_EDIT &&
                    gGo.getSettings().getClickType() == CLICK_TYPE_DOUBLECLICK &&
                    e.getClickCount() < 2)
                return;

            int x = convertCoordsToPoint(e.getX(), offsetX);
            int y = convertCoordsToPoint(e.getY(), offsetY);

            // Check anti-slip. Not for the editor
            if (playMode != PLAY_MODE_EDIT && gGo.getSettings().getAntiSlip()) {
                long delay = System.currentTimeMillis() - antiSlipTimeStamp;
                if (x != antiSlipPosX || y != antiSlipPosY || delay < gGo.getSettings().getAntiSlipDelay()) {
                    getToolkit().beep();
                    System.err.println("Antislip check failed. Delay was " + delay +
                            " (set to " + gGo.getSettings().getAntiSlipDelay() + ")");
                    lastX = lastY = -1;
                    return;
                }
            }

            if (x == lastX && y == lastY && lastX != -1 && lastY != -1) {
                lastX = lastY = -1;
                handleClick(x, y, e.getModifiers());
            }
        } //}}}

        //{{{ mouseEntered() method
        /**
         *  Mouse entered the board
         *
         *@param  e  Mouse event
         */
        public void mouseEntered(MouseEvent e) {
        } //}}}

        //{{{ mouseExited() method
        /**
         *  Mouse exited the board
         *
         *@param  e  Mouse event
         */
        public void mouseExited(MouseEvent e) {
            removeGhostSprite((Graphics2D)getGraphics(), curPosX, curPosY, false);
            curPosX = -1;
            curPosY = -1;
        } //}}}

        //{{{ mouseDragged() method
        /**
         *  Mouse was dragged over the board
         *
         *@param  e  Mouse event
         */
        public void mouseDragged(MouseEvent e) {
        } //}}}

        //{{{ mouseMoved() method
        /**
         *  Mouse was moved on the board. Set the coordinates in the statusbar and show cursor
         *
         *@param  e  Mouse event
         */
        public void mouseMoved(MouseEvent e) {
            // --- 1.3 ---
            if (!gGo.is13())
                requestFocusInWindow();
            else
                requestFocus();

            int x = convertCoordsToPoint(e.getX(), offsetX);
            int y = convertCoordsToPoint(e.getY(), offsetY);

            if (x == curPosX && y == curPosY)
                return;

            // Remember position and timestamp for anti-slip system
            if (playMode != PLAY_MODE_EDIT && gGo.getSettings().getAntiSlip()) {
                antiSlipPosX = x;
                antiSlipPosY = y;
                antiSlipTimeStamp = System.currentTimeMillis();
            }

            if ((x < 0 || x > boardSize || y < 0 || y > boardSize) &&
                    curPosX != -1 && curPosY != -1 && !boardHandler.hasStone(curPosX, curPosY) &&
                    (!hasVarGhost(curPosX, curPosY) || !gGo.getSettings().getShowVariationGhosts())) {
                removeGhostSprite((Graphics2D)getGraphics(), curPosX, curPosY, false);
                curPosX = -1;
                curPosY = -1;
                return;
            }

            if (x > 0 && x <= boardSize && y > 0 && y <= boardSize) {
                if (curPosX != -1 && curPosY != -1 && !boardHandler.hasStone(curPosX, curPosY) &&
                        (!hasVarGhost(curPosX, curPosY) || !gGo.getSettings().getShowVariationGhosts()))
                    removeGhostSprite((Graphics2D)getGraphics(), curPosX, curPosY, false);

                // Update statusbar and sidebar coordinates label
                try {
                    mainFrame.getStatusBar().setCoords(x, y, boardSize);
                    mainFrame.getSideBar().setCoords(x, y, boardSize);
                } catch (NullPointerException ex) {}

                // Display the cursor ghost stone. Check settings, if its an IGSObserverFrame and
                // if we are in edit mode. Check if its our turn in a game
                if (gGo.getSettings().getShowCursor() && editable && mayMove() &&
                        boardHandler.getGameMode() != MODE_SCORE) {
                    curPosX = x;
                    curPosY = y;

                    if (!boardHandler.hasStone(x, y) &&
                            (!hasVarGhost(x, y) || !gGo.getSettings().getShowVariationGhosts()))
                        addGhostSprite((Graphics2D)getGraphics(), boardHandler.getBlackTurn() ? STONE_BLACK : STONE_WHITE, x, y);
                }
                else
                    curPosX = curPosY = -1;
            }
            else
                curPosX = curPosY = -1;
        } //}}}

        //}}}

        //{{{ KeyEvents
        /**
         *  KeyListener method. A key was pressed while the board has focus
         *
         *@param  e  Key event
         */
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    boardHandler.previousMove();
                    break;
                case KeyEvent.VK_RIGHT:
                    boardHandler.nextMove(false);
                    break;
                case KeyEvent.VK_UP:
                    if (editable)
                        boardHandler.previousVariation();
                    break;
                case KeyEvent.VK_DOWN:
                    if (editable)
                        boardHandler.nextVariation();
                    break;
                case KeyEvent.VK_HOME:
                    boardHandler.gotoFirstMove();
                    break;
                case KeyEvent.VK_END:
                    boardHandler.gotoLastMove(true);
                    break;
                case KeyEvent.VK_PAGE_UP:
                    if (editable)
                        boardHandler.gotoVarStart();
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    if (editable)
                        boardHandler.gotoNextBranch();
                    break;
                case KeyEvent.VK_INSERT:
                    if (editable)
                        boardHandler.gotoMainBranch();
                    break;
            }
        }

        /**
         *  KeyListener method. Empty
         *
         *@param  e  Key event
         */
        public void keyReleased(KeyEvent e) {
        }

        /**
         *  KeyListener method. Empty
         *
         *@param  e  Key event
         */
        public void keyTyped(KeyEvent e) {
        } //}}}
    } //}}}
}

