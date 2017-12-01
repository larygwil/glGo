/*
 *  IGSSingleChatter.java
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
package ggo.igs.gui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import ggo.igs.*;
import ggo.igs.chatter.*;
import ggo.utils.*;
import ggo.utils.sound.SoundHandler;
import ggo.gui.*;
import ggo.gGo;

/**
 *  Single-window chat frame. This is the old version, because the desktoppane
 *  chat system was written. It's kept, maybe some people prefer it.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.9 $, $Date: 2002/09/22 09:21:37 $
 */
public class IGSSingleChatter extends JFrame implements IGSChatter {
    //{{{ private members
    private JTextArea outputArea;
    private JTextField inputField;
    private JButton closeButton;
    private JToggleButton toggleButton, soundToggleButton;
    private JComboBox playerSelector;
    private boolean playSound;
    //}}}

    //{{{ IGSSingleChatter() constructor
    /**
     *Constructor for the IGSSingleChatter object
     *
     *@param  toggleButton  Pointer to the "Chats" togglebutton in the IGSMainFrame toolbar
     */
    public IGSSingleChatter(JToggleButton toggleButton) {
        super(gGo.getIGSPlayerResources().getString("Chats"));
        initComponents();

        new HistoryScroller(inputField);

        playSound = gGo.getSettings().getPlayChatSound();
        soundToggleButton.setSelected(playSound);
        this.toggleButton = toggleButton;

        // Restore location
        Point p = gGo.getSettings().getStoredLocation(getClass().getName());
        if (p == null)
            setLocation(30, 30);
        else
            setLocation(p);

        // Restore size
        Dimension size = gGo.getSettings().getStoredSize(getClass().getName());
        if (size != null)
            setSize(size);

        // Create an icon for the appliction
        Image icon = ImageHandler.loadImage("32emerald.png");
        try {
            setIconImage(icon);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icon image.");
        }
    } //}}}

    //{{{ initComponents() method
    /**  Init GUI components */
    private void initComponents() {
        JPanel mainPanel = new JPanel();
        JScrollPane outputScrollPane = new JScrollPane();
        outputArea = new JTextArea();
        JPanel inputPanel = new JPanel();
        playerSelector = new JComboBox();
        inputField = new JTextField();
        JPanel buttonPanel = new JPanel();
        JButton statsButton = new JButton();
        soundToggleButton = new JToggleButton();
        JButton clearButton = new JButton();
        closeButton = new JButton();

        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    setVisible(false);
                }
            });

        mainPanel.setLayout(new BorderLayout(5, 5));

        outputScrollPane.setPreferredSize(new Dimension(400, 180));
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        outputArea.setFont(new Font("Serif", 0, gGo.getSettings().getSerifFontSize()));
        outputScrollPane.setViewportView(outputArea);
        mainPanel.add(outputScrollPane, BorderLayout.CENTER);

        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));

        playerSelector.setEditable(true);
        playerSelector.setModel(new DefaultComboBoxModel(new String[]{}));
        playerSelector.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (evt.getActionCommand().equals("comboBoxEdited"))
                        addName((String)(playerSelector.getSelectedItem()));
                }
            });
        inputPanel.add(playerSelector);

        inputField.setFont(new Font("Sans Serif", 0, gGo.getSettings().getSansSerifFontSize()));
        inputField.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    inputFieldActionPerformed(evt);
                }
            });
        inputPanel.add(inputField);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        statsButton.setIcon(new ImageIcon(getClass().getResource("/images/About16.gif")));
        statsButton.setToolTipText(gGo.getIGSPlayerResources().getString("user_info_tooltip"));
        statsButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (getTarget() != null && getTarget().length() > 0) {
                        IGSConnection.sendCommand("stats " + getTarget());
                    }
                }
            });
        buttonPanel.add(statsButton);
        buttonPanel.add(new FontSizeSelector(outputArea));

        soundToggleButton.setIcon(new ImageIcon(getClass().getResource("/images/Volume16.gif")));
        soundToggleButton.setToolTipText(gGo.getIGSPlayerResources().getString("toggle_sound_tooltip"));
        soundToggleButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (soundToggleButton.isSelected())
                        playSound = true;
                    else
                        playSound = false;
                    gGo.getSettings().setPlayChatSound(playSound);
                    gGo.getSettings().saveSettings();
                }
            });
        buttonPanel.add(soundToggleButton);

        clearButton.setText(gGo.getIGSPlayerResources().getString("Clear"));
        clearButton.setToolTipText(gGo.getIGSPlayerResources().getString("clear_tooltip"));
        clearButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    outputArea.setText("");
                    playerSelector.removeAllItems();
                }
            });
        buttonPanel.add(clearButton);

        closeButton.setText(gGo.getIGSPlayerResources().getString("Close"));
        closeButton.setToolTipText(gGo.getIGSPlayerResources().getString("close_window_tooltip"));
        closeButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    setVisible(false);
                }
            });
        buttonPanel.add(closeButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
    } //}}}

    //{{{ inputFieldActionPerformed() method
    /**
     *  Input was typed in the input field
     *
     *@param  evt  ActionEvent
     */
    private void inputFieldActionPerformed(ActionEvent evt) {
        String txt = inputField.getText();
        inputField.setText("");
        sendChat(getTarget(), txt, true);
    } //}}}

    //{{{ addName() method
    /**
     *  Add a playername to the playerselector combobox
     *
     *@param  name  Playername to add
     *@return       True if added, false if already existing
     */
    private boolean addName(String name) {
        for (int i = 0, sz = playerSelector.getItemCount(); i < sz; i++) {
            if (((String)(playerSelector.getItemAt(i))).equals(name))
                return false;
        }

        playerSelector.addItem(new String(name));
        return true;
    } //}}}

    //{{{ setName() method
    /**
     *  Set the current name in the playerselector combobox
     *
     *@param  name  Player name
     */
    public void setName(String name) {
        addName(name);
        playerSelector.setSelectedItem(name);
    } //}}}

    //{{{ getTarget() method
    /**
     *  Gets the currently selected tell target
     *
     *@return    Name of current player
     */
    private String getTarget() {
        return (String)(playerSelector.getSelectedItem());
    } //}}}

    //{{{ hasTarget() method
    /**
     *  Checks if the currently selected tell target equals the given player name
     *
     *@param  name  Player name to check
     *@return       True if given name is the currently selected target, else false
     */
    public boolean hasTarget(String name) {
        return name.equals(getTarget());
    } //}}}

    //{{{ notifyOnline() method
    /**
     *  Print notify online message
     *
     *@param  name  Player name who came online
     */
    public void notifyOnline(String name) {
        append(MessageFormat.format(gGo.getIGSPlayerResources().getString("notify_online"), new Object[] {name}));
    } //}}}

    //{{{ notifyOffline() method
    /**
     *  Print notify online message
     *
     *@param  name  Player name who went offline
     */
    public void notifyOffline(String name) {
        append(MessageFormat.format(gGo.getIGSPlayerResources().getString("notify_offline"), new Object[] {name}));
    } //}}}

    //{{{ sendChat() method
    /**
     *  Send a chat to a player
     *
     *@param  target  Target player name
     *@param  txt     Text to send
     *@param  sendIt  If true, actually send the tell command to IGS
     */
    public void sendChat(String target, String txt, boolean sendIt) {
        if (target == null || target.length() == 0 ||
                txt == null || txt.length() == 0)
            return;

        append("-> " + target + ": " + txt);
        addName(target);

        if (sendIt)
            IGSConnection.sendCommand("tell " + target + " " + txt);

        checkVisible();
        setInputFocus();
    } //}}}

    //{{{ recieveChat() method
    /**
     *  Called from outside if a tell was recieved
     *
     *@param  fromName  Player who sent tihs tell
     *@param  txt       Tell text
     */
    public void recieveChat(String fromName, String txt) {
        append(fromName + ": " + txt);
        addName(fromName);
        checkVisible();
        if (playSound)
            SoundHandler.playIncomingChat();
    } //}}}

    //{{{ recieveChatError() method
    /**
     *  Called from outside if a tell error was recieved
     *
     *@param  txt  Recieved error text
     */
    public void recieveChatError(String txt) {
        append(txt);
        checkVisible();
    } //}}}

    //{{{ checkVisible() method
    /**  Check if this frame is visible. If not, deiconify it. */
    public void checkVisible() {
        if (!isVisible()) {
            setVisible(true);
            try {
                toggleButton.setSelected(true);
            } catch (NullPointerException e) {}
        }

        if (getState() != Frame.NORMAL)
            setState(Frame.NORMAL);
        // toFront();
    } //}}}

    //{{{ setInputFocus() method
    /**  Set input focus to the input commandline */
    public void setInputFocus() {
        // --- 1.3 ---
        if (!gGo.is13())
            inputField.requestFocusInWindow();
        else
            inputField.requestFocus();
    } //}}}

    //{{{ append() method
    /**
     *  Append text to the output textfield
     *
     *@param  txt  Text to append
     */
    private void append(String txt) {
        outputArea.append(txt + "\n");

        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        outputArea.scrollRectToVisible(outputArea.modelToView(
                                outputArea.getDocument().getLength()));
                    } catch (BadLocationException e) {
                        System.err.println("Failed to scroll: " + e);
                    }
                }
            });
    } //}}}

    //{{{ getCloseButton() method
    /**
     *  Gets the closeButton attribute of the IGSSingleChatter object
     *
     *@return    The closeButton value
     */
    public JButton getCloseButton() {
        return closeButton;
    } //}}}

    //{{{ getCloseMenuItem() method
    /**
     *  Gets the closeMenuItem attribute of the IGSSingleChatter object
     *
     *@return    The closeMenuItem value
     */
    public JMenuItem getCloseMenuItem() {
        return null;
    } //}}}

    //{{{ updateLookAndFeel() method
    /**  Update Look and Feel */
    public void updateLookAndFeel() {
        SwingUtilities.updateComponentTreeUI(this);
        pack();
    } //}}}

    //{{{ changeFontSize() method
    /**  Change the font size of the output and input textfields */
    public void changeFontSize() {
        outputArea.setFont(new Font("Serif", 0, gGo.getSettings().getSerifFontSize()));
        inputField.setFont(new Font("Sans Serif", 0, gGo.getSettings().getSansSerifFontSize()));
    } //}}}
}

