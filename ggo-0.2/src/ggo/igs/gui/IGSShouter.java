/*
 *  IGSShouter.java
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
import ggo.gGo;
import ggo.utils.*;
import ggo.igs.*;
import ggo.gui.*;

/**
 *  Simple frame that embeds a JTextArea for output and a JTextField for input to
 *  display and enter IGS shouts.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.6 $, $Date: 2002/09/21 12:39:56 $
 */
public class IGSShouter extends JFrame {
    //{{{ private members
    private JToggleButton toggleButton;
    private JTextArea outputArea;
    private JTextField inputField;
    private JButton closeButton;
    //}}}

    //{{{ IGSShouter() constructor
    /**
     *Constructor for the IGSShouter object
     *
     *@param  toggleButton  Description of the Parameter
     */
    public IGSShouter(JToggleButton toggleButton) {
        super(gGo.getIGSResources().getString("Shouts"));
        initComponents();

        new HistoryScroller(inputField);

        // Restore location
        Point p = gGo.getSettings().getStoredLocation(getClass().getName());
        if (p == null)
            setLocation(30, 320);
        else
            setLocation(p);

        // Restore size
        Dimension size = gGo.getSettings().getStoredSize(getClass().getName());
        if (size != null)
            setSize(size);

        this.toggleButton = toggleButton;

        // Create an icon for the appliction
        Image icon = ImageHandler.loadImage("32green.png");
        try {
            setIconImage(icon);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icon image.");
        }
    } //}}}

    //{{{ initComponents() method
    /**  Init GUI elements */
    private void initComponents() {
        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    closeFrame();
                }
            });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(5, 5));

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(400, 180));

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

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(new FontSizeSelector(outputArea));

        JButton clearButton = new JButton();
        clearButton.setText(gGo.getIGSResources().getString("Clear"));
        clearButton.setToolTipText(gGo.getIGSResources().getString("clear_tooltip"));
        clearButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    outputArea.setText("");
                }
            });
        buttonPanel.add(clearButton);

        closeButton = new JButton();
        closeButton.setText(gGo.getIGSResources().getString("Close"));
        closeButton.setToolTipText(gGo.getIGSResources().getString("close_window_tooltip"));
        closeButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    closeFrame();
                }
            });
        buttonPanel.add(closeButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
    } //}}}

    //{{{ inputFieldActionPerformed() method
    /**
     *  Text was entered in the input line
     *
     *@param  evt  ActionEvent
     */
    private void inputFieldActionPerformed(ActionEvent evt) {
        String txt = inputField.getText();
        inputField.setText("");
        IGSConnection.sendCommand("shout " + txt);
        append(IGSConnection.getLoginName() + ": " + txt);
    } //}}}

    //{{{ closeFrameappend() method
    /** Close the frame */
    private void closeFrame() {
        setVisible(false);
    } //}}}

    //{{{ append() method
    /**
     *  Append text to the output textfield
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
    /**  Check if frame is visible, if not, raise it */
    public void checkVisible() {
        if (!isVisible()) {
            setVisible(true);
            try {
                toggleButton.setSelected(true);
            } catch (NullPointerException e) {}
        }
        setInputFocus();
    } //}}}

    //{{{ setInputFocus() method
    /**  Sets the inputFocus to the input command field */
    public void setInputFocus() {
        // --- 1.3 ---
        if (!gGo.is13())
            inputField.requestFocusInWindow();
        else
            inputField.requestFocus();
    } //}}}

    //{{{ getCloseButton() method
    /**
     *  Gets the closeButton attribute of the IGSShouter object
     *
     *@return    The closeButton value
     */
    public JButton getCloseButton() {
        return closeButton;
    } //}}}

    //{{{ changeFontSize() method
    /**  Change font size */
    public void changeFontSize() {
        outputArea.setFont(new Font("Serif", 0, gGo.getSettings().getSerifFontSize()));
        inputField.setFont(new Font("Sans Serif", 0, gGo.getSettings().getSansSerifFontSize()));
    } //}}}
}

