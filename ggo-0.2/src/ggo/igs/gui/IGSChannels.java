/*
 *  IGSChannels.java
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
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.MessageFormat;
import ggo.igs.*;
import ggo.utils.*;
import ggo.gui.*;
import ggo.gGo;

/**
 *  Frame for channel communication
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.7 $, $Date: 2002/09/21 12:39:56 $
 */
public class IGSChannels extends JFrame {
    //{{{ private members
    private JToggleButton toggleButton;
    private JTextArea outputArea;
    private JTextField inputField;
    private JTextField channelTextField;
    private JButton closeButton, enterLeaveButton, titleButton;
    private int currentChannel;
    //}}}

    //{{{ IGSChannels() constructor
    /**
     *Constructor for the IGSChannels object
     *
     *@param  toggleButton  Pointer to the Channels button in the IGSMainWindow
     */
    public IGSChannels(JToggleButton toggleButton) {
        super(gGo.getIGSResources().getString("Channels"));
        initComponents();

        new HistoryScroller(inputField);

        // Restore location
        Point p = gGo.getSettings().getStoredLocation(getClass().getName());
        if (p == null)
            setLocation(200, 40);
        else
            setLocation(p);

        // Restore size
        Dimension size = gGo.getSettings().getStoredSize(getClass().getName());
        if (size != null)
            setSize(size);

        this.toggleButton = toggleButton;
        currentChannel = -1;

        // Create an icon for the appliction
        Image icon = ImageHandler.loadImage("32purple.png");
        try {
            setIconImage(icon);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icon image.");
        }
    } //}}}

    //{{{ initComponents() method
    /**  Init GUI components */
    private void initComponents() {
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    setVisible(false);
                }
            });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(5, 5));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(480, 420));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        outputArea.setFont(new Font("Serif", 0, gGo.getSettings().getSerifFontSize()));
        scrollPane.setViewportView(outputArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(new Font("Sans Serif", 0, gGo.getSettings().getSansSerifFontSize()));
        inputField.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    inputFieldActionPerformed(evt);
                }
            });

        mainPanel.add(inputField, BorderLayout.SOUTH);

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        JPanel buttomPanel = new JPanel();
        buttomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton listButton = new JButton(gGo.getIGSResources().getString("List"));
        listButton.setToolTipText(gGo.getIGSResources().getString("list_tooltip"));
        listButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    IGSConnection.sendCommand("channel");
                }
            });
        buttomPanel.add(listButton);

        titleButton = new JButton(gGo.getIGSResources().getString("Title"));
        titleButton.setToolTipText(gGo.getIGSResources().getString("title_tooltip"));
        titleButton.setEnabled(false);
        titleButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (currentChannel != -1) {
                        String title = JOptionPane.showInputDialog(IGSChannels.this, gGo.getIGSResources().getString("Enter_new_title"));
                        if (title != null && title.length() > 0)
                            IGSConnection.sendCommand("channel " + currentChannel + " title " + title);
                    }
                }
            });
        buttomPanel.add(titleButton);

        channelTextField = new JTextField();
        channelTextField.setHorizontalAlignment(JTextField.TRAILING);
        channelTextField.setToolTipText(gGo.getIGSResources().getString("select_channel_tooltip"));
        channelTextField.setPreferredSize(new Dimension(40, 22));
        buttomPanel.add(channelTextField);

        enterLeaveButton = new JButton(gGo.getIGSResources().getString("Enter"));
        enterLeaveButton.setToolTipText(gGo.getIGSResources().getString("enter_channel_tooltip"));
        enterLeaveButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (currentChannel == -1) {
                        boolean err = false;
                        int n = -1;
                        try {
                            n = Integer.parseInt(channelTextField.getText());
                        } catch (NumberFormatException e) {
                            err = true;
                        }

                        if (n < 0 || n > 100)
                            err = true;

                        if (err) {
                            currentChannel = -1;
                            getToolkit().beep();
                            return;
                        }
                        IGSConnection.sendCommand("yell \\" + n);
                    }
                    else {
                        IGSConnection.sendCommand("yell \\-1");
                        leaveChannel();
                    }
                }
            });
        buttomPanel.add(enterLeaveButton);

        buttomPanel.add(new FontSizeSelector(outputArea));

        JButton clearButton = new JButton(gGo.getIGSResources().getString("Clear"));
        clearButton.setToolTipText(gGo.getIGSResources().getString("clear_output_tooltip"));
        clearButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    outputArea.setText("");
                }
            });
        buttomPanel.add(clearButton);

        closeButton = new JButton(gGo.getIGSResources().getString("Close"));
        closeButton.setToolTipText(gGo.getIGSResources().getString("close_window_tooltip"));
        closeButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    setVisible(false);
                }
            });
        buttomPanel.add(closeButton);

        getContentPane().add(buttomPanel, BorderLayout.SOUTH);

        pack();
    } //}}}

    //{{{ enterChannel() method
    /**
     *  Enter a channel
     *
     *@param  n  Channel id
     */
    private void enterChannel(int n) {
        currentChannel = n;
        enterLeaveButton.setText(gGo.getIGSResources().getString("Leave"));
        enterLeaveButton.setToolTipText(gGo.getIGSResources().getString("leave_channel_tooltip"));
        channelTextField.setEnabled(false);
        channelTextField.setText(String.valueOf(currentChannel));
        titleButton.setEnabled(true);
        setTitle(
                new MessageFormat(gGo.getIGSResources().getString("channel_number")).format(new Object[]{new Integer(currentChannel)}));

        System.err.println("Entered channel #" + currentChannel);
    } //}}}

    //{{{ leaveChannel() method
    /**  Leave the current channel */
    public void leaveChannel() {
        enterLeaveButton.setText(gGo.getIGSResources().getString("Enter"));
        enterLeaveButton.setToolTipText(gGo.getIGSResources().getString("enter_channel_tooltip"));
        channelTextField.setEnabled(true);
        channelTextField.setText("");
        titleButton.setEnabled(false);
        setTitle(gGo.getIGSResources().getString("Channels"));
        currentChannel = -1;

        append(gGo.getIGSResources().getString("Left_channel"));
    } //}}}

    //{{{ inputFieldActionPerformed() method
    /**
     *  Text was typed in the input textfield
     *
     *@param  evt  ActionEvent
     */
    private void inputFieldActionPerformed(ActionEvent evt) {
        String txt = inputField.getText();
        inputField.setText("");

        if (txt == null || txt.length() == 0)
            return;

        if (currentChannel == -1) {
            append(gGo.getIGSResources().getString("Not_in_a_channel."));
            return;
        }

        IGSConnection.sendCommand("yell " + txt);
        append(IGSConnection.getLoginName() + ": " + txt);
    } //}}}

    //{{{ append() method
    /**
     *  Append text to the output textarea and scroll it
     *
     *@param  txt  Text to append
     */
    public void append(String txt) {
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

    //{{{ checkVisible() method
    /**
     *  Check if frame is visible, if not, set it to visible and toggle the Channels button.
     */
    public void checkVisible() {
        if (!isVisible()) {
            setVisible(true);
            try {
                toggleButton.setSelected(true);
            } catch (NullPointerException e) {}
        }
        if (getState() != Frame.NORMAL)
            setState(Frame.NORMAL);
    } //}}}

    //{{{ setInputFocus() method
    /**  Set input focus to this the input textfield */
    public void setInputFocus() {
        // --- 1.3 ---
        if (!gGo.is13())
            inputField.requestFocusInWindow();
        else
            inputField.requestFocus();
    } //}}}

    //{{{ getCloseButton() method
    /**
     *  Gets the closeButton
     *
     *@return    The closeButton value
     */
    public JButton getCloseButton() {
        return closeButton;
    } //}}}

    //{{{ recieveYell() method
    /**
     *  Yell was recieved from IGS. Parse and display it
     *
     *@param  txt  IGS input text
     */
    public void recieveYell(String txt) {
        int n = -1;
        String yellTxt = null;

        checkVisible();

        try {
            if (txt.startsWith("Changing into channel ")) {
                n = Integer.parseInt(txt.substring(22, txt.indexOf(".", 22)));
                checkVisible();
                setInputFocus();
                enterChannel(n);
                append(txt);
                return;
            }
            else {
                int pos = txt.indexOf(":");
                n = Integer.parseInt(txt.substring(0, pos));
                yellTxt = txt.substring(pos + 1, txt.length()).trim();
            }
        } catch (StringIndexOutOfBoundsException e) {
            // Nevermind, this is no error
            append(txt);
            return;
        } catch (NumberFormatException e) {
            // Nevermind, this is no error
            append(txt);
            return;
        }

        if (n != -1 && n != currentChannel) {
            System.err.println("Strange, I am not in channel # " + n);
            enterChannel(n);
            return;
        }

        append(yellTxt);
    } //}}}

    //{{{ changeFontSize() method
    /**  Description of the Method */
    public void changeFontSize() {
        outputArea.setFont(new Font("Serif", 0, gGo.getSettings().getSerifFontSize()));
        inputField.setFont(new Font("Sans Serif", 0, gGo.getSettings().getSansSerifFontSize()));
    } //}}}
}

