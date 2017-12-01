/*
 *  gGoApplet.java
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
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.security.*;
import ggo.*;
import ggo.dialogs.GameInfoDialog;

/**
 *  <p>Main class for the applet, embedded or full application.</p>
 *  <p>If the applet is embedded, only a board will be shown inside the browser window.
 *  If the applet is used as full application (the default), this class serves as starter
 *  for the real application, handled in the gGo class.</p>
 *  <p>The applet can be controlled by giving parameters in the HTLM code, like:<br>
 *  &lt;param name="boardonly" value="true"&gt;</p>
 *  <p>The following parameters are supported:</p>
 *  <table border="1" cellpadding="5">
 *  <tr><th>Name</th><th>Description</th><th>Value</th><th>Default</th></tr>
 *  <tr><td><b>boardonly</b></td><td>Show the full application or an embedded board</td><td>true|false</td><td>false</td></tr>
 *  <tr><td><b>boardSize</b></td><td>Board size</td><td>4-36</td><td>19</td></tr>
 *  <tr><td><b>fileName</b></td><td>File to load, given as URL or URI</td><td>String</td><td>none</td></tr>
 *  <tr><td><b>showComment</b></td><td>Show the comment field (true) or keep it minimized (false)<br>
 *  Ignored if boardonly is not true</td><td>true|false</td><td>false</td></tr>
 *  <tr><td><b>hideReset</b></td><td>Hide the reset button<br>Ignored if boardonly is not true</td><td>true|false</td><td>false</td></tr>
 *  <tr><td><b>hideAutoplay</b></td><td>Hide the autoplay button<br>
 *  Ignored if boardonly is not true</td><td>true|false</td><td>false</td></tr>
 *  <tr><td><b>noEdit</b></td><td>Don't allow the user to edit the game<br>
 *  Ignored if boardonly is not true</td><td>true|false</td><td>false</td></tr></table>
 *  <p> The applet offers three functions to be called from JavaScript:
 *  reset(), reset(int boardSize) and load(String fileName)</p>
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 *@see        <a href="http://www.igoweb.org/~zotan/applethelp.html">gGo applet developer manual</a>
 */
public class gGoApplet extends JApplet implements Defines, DocumentListener {
    //{{{ private members
    private Board board = null;
    private final static String httpStr = "http://";
    private final String fileStr = "file://";
    private JLabel moveLabel, capsLabel;
    private JTextArea commentEdit;
    private JSplitPane splitPane;
    private int boardSize = 19;
    private boolean showComment = false, hideReset = false, hideAutoplay = false, noEdit = false;
    //}}}

    //{{{ init() method
    /**  init method of the applet */
    public void init() {
        // Check version
        String javaVersion = System.getProperty("java.version");
        System.err.println("Running on " + javaVersion);
        if (javaVersion.compareTo("1.4") < 0) {
            gGo.setIs13();
        }

        // Set LookAndFeel and Theme
        gGo.initLookAndFeel(null);
        UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());

        // Read filename from HTML parameter
        boolean url = false;
        String fileName = getParameter("fileName");
        System.err.println("fileName parameter: " + fileName);

        if (fileName != null) {
            if (fileName.length() == 0 || fileName.equals("null"))
                fileName = null;
            else {
                if (fileName.startsWith(httpStr))
                    url = true;
                else if (fileName.startsWith(fileStr))
                    fileName = fileName.substring(fileStr.length(), fileName.length()).trim();
            }
        }

        // Check boardonly HTML paramteter
        if (getParameter("boardonly") != null && getParameter("boardonly").equals("true")) {
            // Read showComment HTML paramter
            if (getParameter("showComment") != null && getParameter("showComment").equals("true"))
                showComment = true;

            // Read hideReset HTML paramter
            if (getParameter("hideReset") != null && getParameter("hideReset").equals("true"))
                hideReset = true;

            // Read hideAutoplay HTML paramter
            if (getParameter("hideAutoplay") != null && getParameter("hideAutoplay").equals("true"))
                hideAutoplay = true;

            // Read noEdit HTML paramter
            if (getParameter("noEdit") != null && getParameter("noEdit").equals("true"))
                noEdit = true;

            initGUI();

            // Open file if one was given in fileName HTML parameter
            if (fileName != null) {
                if (!url)
                    board.openSGF(fileName, false);
                else
                    loadURL(fileName);
            }
        }
        else {
            gGo ggo= new gGo(true, url ? null : fileName);
            board = ggo.getFirstMainFrame().getBoard();
            if (url)
                loadURL(fileName);
        }

        // Read boardSize HTML parameter. Ignored when fileName parameter was given.
        if ((fileName == null || fileName.length() == 0) &&
                getParameter("boardSize") != null) {
            try {
                boardSize = Integer.parseInt(getParameter("boardSize"));
                reset(boardSize);
            } catch (NumberFormatException e) {
                System.err.println("Failed to convert boardSize parameter: " + e);
                boardSize = 19;
            }
        }
    } //}}}

    //{{{ start() method
    /**
     *  Applet starts up. Show or hide comment field according to showComment parameter
     */
    public void start() {
        if (showComment) {
            try {
                splitPane.setDividerLocation((int)(board.getSize().height * 0.75));
            } catch (NullPointerException e) {}
        }
    } //}}}

    //{{{ initGUI() method
    /**  Init GUI elements for the boardonly applet */
    private void initGUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        // Create the board
        board = new Board(!noEdit, this);

        // Create the Comment Edit field
        commentEdit = new JTextArea();
        commentEdit.setLineWrap(true);
        commentEdit.setEditable(false);
        commentEdit.setWrapStyleWord(true);
        commentEdit.getDocument().addDocumentListener(this);
        JScrollPane commentScrollPane = new JScrollPane(commentEdit);
        commentScrollPane.setPreferredSize(new Dimension(100, 60));
        commentScrollPane.setMinimumSize(new Dimension(0, 0));

        // Create the SplitPane
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                board, commentScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(2000);

        panel.add(splitPane, BorderLayout.CENTER);

        // Create the toolbar
        try {
            panel.add(initToolsPanel(), BorderLayout.EAST);
        } catch (NullPointerException e) {}

        getContentPane().add(panel);
    } //}}}

    //{{{ initToolsPanel() method
    /**
     *  Init the panel with the navigation buttons
     *
     *@return    The tool panel
     */
    private JPanel initToolsPanel() {
        JPanel tools = new JPanel();
        int cols = 14;
        if (hideAutoplay)
            cols--;
        if (hideReset)
            cols--;
        tools.setLayout(new GridLayout(cols, 1));
        tools.setBackground(Color.orange);

        try {
            JButton twoLeftButton = new JButton(new ImageIcon(getClass().getResource("/images/2leftarrow.gif")));
            twoLeftButton.setBorderPainted(false);
            twoLeftButton.setBackground(Color.orange);
            twoLeftButton.setToolTipText("First move (Home)");
            twoLeftButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().gotoFirstMove();
                    }
                });
            tools.add(twoLeftButton);

            JButton leftButton = new JButton(new ImageIcon(getClass().getResource("/images/1leftarrow.gif")));
            leftButton.setBorderPainted(false);
            leftButton.setBackground(Color.orange);
            leftButton.setToolTipText("Previous move (Left)");
            leftButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().previousMove();
                    }
                });
            tools.add(leftButton);

            JButton rightButton = new JButton(new ImageIcon(getClass().getResource("/images/1rightarrow.gif")));
            rightButton.setBorderPainted(false);
            rightButton.setBackground(Color.orange);
            rightButton.setToolTipText("Next move (Right)");
            rightButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().nextMove(false);
                    }
                });
            tools.add(rightButton);

            JButton twoRightButton = new JButton(new ImageIcon(getClass().getResource("/images/2rightarrow.gif")));
            twoRightButton.setBorderPainted(false);
            twoRightButton.setBackground(Color.orange);
            twoRightButton.setToolTipText("Last move (End)");
            twoRightButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().gotoLastMove(true);
                    }
                });
            tools.add(twoRightButton);

            JButton mainBranchButton = new JButton(new ImageIcon(getClass().getResource("/images/start.gif")));
            mainBranchButton.setBorderPainted(false);
            mainBranchButton.setBackground(Color.orange);
            mainBranchButton.setToolTipText("Main branch (Ins)");
            mainBranchButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().gotoMainBranch();
                    }
                });
            tools.add(mainBranchButton);

            JButton varStartButton = new JButton(new ImageIcon(getClass().getResource("/images/top.gif")));
            varStartButton.setBorderPainted(false);
            varStartButton.setBackground(Color.orange);
            varStartButton.setToolTipText("Variation start (PgUp)");
            varStartButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().gotoVarStart();
                    }
                });
            tools.add(varStartButton);

            JButton upButton = new JButton(new ImageIcon(getClass().getResource("/images/up.gif")));
            upButton.setBorderPainted(false);
            upButton.setBackground(Color.orange);
            upButton.setToolTipText("Previous variation (Up)");
            upButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().previousVariation();
                    }
                });
            tools.add(upButton);

            JButton downButton = new JButton(new ImageIcon(getClass().getResource("/images/down.gif")));
            downButton.setBorderPainted(false);
            downButton.setBackground(Color.orange);
            downButton.setToolTipText("Next variation (Down)");
            downButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().nextVariation();
                    }
                });
            tools.add(downButton);

            JButton nextBranchButton = new JButton(new ImageIcon(getClass().getResource("/images/bottom.gif")));
            nextBranchButton.setBorderPainted(false);
            nextBranchButton.setBackground(Color.orange);
            nextBranchButton.setToolTipText("Next branch (PgDown)");
            nextBranchButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        board.getBoardHandler().gotoNextBranch();
                    }
                });
            tools.add(nextBranchButton);

            if (!hideAutoplay) {
                JToggleButton autoplayButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/Play16.gif")));
                autoplayButton.setBorderPainted(false);
                autoplayButton.setBackground(Color.orange);
                autoplayButton.setToolTipText("Start/Stop autoplay");
                autoplayButton.addItemListener(
                    new ItemListener() {
                        javax.swing.Timer autoplayTimer = null;

                        public void itemStateChanged(ItemEvent e) {
                            final Object source = e.getItemSelectable();
                            if (e.getStateChange() == ItemEvent.SELECTED) {
                                // Use default delay of 1 second here
                                autoplayTimer = new javax.swing.Timer(1000,
                                    new ActionListener() {
                                        public void actionPerformed(ActionEvent evt) {
                                            if (!board.getBoardHandler().nextMove(true) || !isActive())
                                                ((JToggleButton)source).setSelected(false);
                                        }
                                    });
                                autoplayTimer.start();
                            }
                            else if (e.getStateChange() == ItemEvent.DESELECTED) {
                                autoplayTimer.stop();
                                autoplayTimer = null;
                            }
                        }
                    });
                tools.add(autoplayButton);
            }

            if (!hideReset) {
                JButton resetButton = new JButton(new ImageIcon(getClass().getResource("/images/New16.gif")));
                resetButton.setBorderPainted(false);
                resetButton.setBackground(Color.orange);
                resetButton.setToolTipText("Reset board");
                resetButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            reset(boardSize);
                        }
                    });
                tools.add(resetButton);
            }

            JButton gameInfoButton = new JButton(new ImageIcon(getClass().getResource("/images/Information16.gif")));
            gameInfoButton.setBorderPainted(false);
            gameInfoButton.setBackground(Color.orange);
            gameInfoButton.setToolTipText("Display game information");
            gameInfoButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        GameInfoDialog dlg = new GameInfoDialog(null, true, board.getBoardHandler().getGameData());
                    }
                });
            tools.add(gameInfoButton);

            moveLabel = new JLabel("0", SwingConstants.CENTER);
            moveLabel.setToolTipText("Move number");
            tools.add(moveLabel);

            capsLabel = new JLabel("0/0", SwingConstants.CENTER);
            capsLabel.setToolTipText("Captures white/black");
            tools.add(capsLabel);

        } catch (NullPointerException e) {
            System.err.println("Failed to load icons for tools: " + e);
            return null;
        }
        return tools;
    } //}}}

    //{{{ setMove() method
    /**
     *  Set the move number in the boardonly applet
     *
     *@param  number  The move number
     */
    protected void setMove(int number) {
        try {
            moveLabel.setText(String.valueOf(number));
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ setCaptures() method
    /**
     *  Sets captures in the boardonly applet
     *
     *@param  capsW  Captures white
     *@param  capsB  Captures black
     */
    protected void setCaptures(int capsW, int capsB) {
        try {
            capsLabel.setText(String.valueOf(capsW) + "/" + String.valueOf(capsB));
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ setCommentText() method
    /**
     *  Write text in the comment field
     *
     *@param  txt  Text to write
     */
    protected void setCommentText(String txt) {
        try {
            commentEdit.setText(txt);
        } catch (NullPointerException e) {}
    } //}}}

    //{{{ reset() method
    /**  Interface for Javascript. Reset the board. */
    public void reset() {
        board.initGame(new GameData(), false);
    } //}}}

    //{{{ reset(int) method
    /**
     *  Interface for Javascript. Reset the board with a given board size
     *
     *@param  boardSize  Board size
     */
    public void reset(int boardSize) {
        board.initGame(new GameData(boardSize), false);
    } //}}}

    //{{{ load() method
    /**
     *  Interface for JavaScript.
     *  Load a sgf file, either given as local file, URI or URL.
     *
     *@param  fileName  local filename, URI or URL
     */
    public void load(String fileName) {
        if (fileName == null || fileName.length() == 0)
            return;

        if (fileName.startsWith(httpStr))
            loadURL(fileName);
        else if (fileName.startsWith(fileStr))
            loadLocalFile(fileName.substring(fileStr.length(), fileName.length()).trim());
        else
            loadLocalFile(fileName);
    } //}}}

    //{{{ loadLocalFile() method
    /**
     *  Load a sgf file given as URL from local disc
     *
     *@param  fileName  Filename pointing to the sgf file
     */
    private void loadLocalFile(final String fileName) {
        if (board == null)
            return;

        // Ugly ugly... but else a AccessControlException is thrown
        AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    board.openSGF(fileName, false);
                    return null;
                }
            });
    } //}}}

    //{{{ loadURL() method
    /**
     *  Load a sgf file given as URL from a remote http host
     *
     *@param  fileName  URL pointing to the sgf file
     */
    private void loadURL(final String fileName) {
        if (board == null)
            return;

        URL url = null;
        try {
            url = new URL(fileName);
        } catch (MalformedURLException e) {
            System.err.println("Bad URL: " + e);
            return;
        }

        final URL finalURL = url;
        final String s[] = {null};

        // Ugly ugly... but else a AccessControlException is thrown
        AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    s[0] = parseURL(finalURL);
                    return null;
                }
            });

        if (s[0] != null && s[0].length() > 0)
            board.openFromString(s[0]);
    } //}}}

    //{{{ parseURL() method
    /**
     *  Parse a remote sgf file from a http host
     *
     *@param  url  URL of sgf file to load
     *@return      String with the sgf content
     */
    private String parseURL(URL url) {
        StringBuffer s = new StringBuffer();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;

            while ((line = in.readLine()) != null)
                s.append(line + "\n");

            in.close();
        } catch (IOException e) {
            System.err.println("Failed to read from URL: " + e);
            return null;
        }

        return s.toString();
    } //}}}

    //{{{ DocumentListener methods()
    /**
     *  Gives notification that the comment edit field has changed.
     *
     *@param  e  The document event
     */
    public void insertUpdate(DocumentEvent e) {
        try {
            board.getBoardHandler().getTree().getCurrent().setComment(commentEdit.getText());
        } catch (NullPointerException ex) {}
    }

    /**
     *  Gives notification that the comment edit field has changed.
     *
     *@param  e  The document event
     */
    public void removeUpdate(DocumentEvent e) {
        try {
            board.getBoardHandler().getTree().getCurrent().setComment(commentEdit.getText());
        } catch (NullPointerException ex) {}
    }

    /**
     *  Gives notification that the comment edit field has changed. - Not used.
     *
     *@param  e  The document event
     */
    public void changedUpdate(DocumentEvent e) { }
    //}}}
}

