/*
 *  GTPError.java
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
package ggo.gtp;

import ggo.Defines;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;
import ggo.gGo;
import ggo.gtp.*;

/**
 *  Parser class for the error output sent from a GTP engine. Additionally
 *  this class offers a GTP shell window for interactive GTP control.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class GTPError extends Thread implements ActionListener, Defines {
    //{{{ private members
    private BufferedReader bufIn;
    private static JFrame frame = null;
    private static JTextArea outputTextArea = null;
    private JTextField inputTextField;
    //}}}

    //{{{ GTPError() constructor
    /**
     *Constructor for the GTPError object
     *
     *@param  in  GTP InputStream object
     */
    public GTPError(InputStream in) {
        bufIn = new BufferedReader(new InputStreamReader(in));

        initGUI();
    } //}}}

    //{{{ initGUI() method
    /**  Init GUI elements of GTP shell */
    private void initGUI() {
        frame = new JFrame();
        frame.setTitle("GTP control");

        frame.getContentPane().setLayout(new BorderLayout(5, 5));

        JScrollPane outputScrollPane = new JScrollPane();
        outputScrollPane.setPreferredSize(new Dimension(400, 400));
        outputScrollPane.setAutoscrolls(true);
        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setFont(new Font("Monospaced", 0, 12));
        outputScrollPane.setViewportView(outputTextArea);
        frame.getContentPane().add(outputScrollPane, BorderLayout.CENTER);

        inputTextField = new JTextField();
        inputTextField.addActionListener(this);
        frame.getContentPane().add(inputTextField, BorderLayout.SOUTH);

        frame.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    gGo.getGTP().getGTPMainFrame().viewShowGTP.setSelected(false);
                }
            });

        frame.pack();
    } //}}}

    //{{{ appendOutput() method
    /**
     *  Append output to GTP shell
     *
     *@param  s  String to append
     */
    public static void appendOutput(String s) {
        if (outputTextArea == null)
            return;

        outputTextArea.append(s + "\n");

        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        outputTextArea.scrollRectToVisible(outputTextArea.modelToView(
                                outputTextArea.getDocument().getLength()));
                    } catch (BadLocationException e) {
                        System.err.println("Failed to scroll: " + e);
                    }
                }
            });
    } //}}}

    //{{{ run() method
    /**  Main processing method for the GTPError object */
    public void run() {
        String input;

        try {
            while ((input = bufIn.readLine()) != null && !isInterrupted())
                appendOutput(input);
        } catch (IOException e) {
            System.err.println("Failed to read from input stream: " + e);
        }

        frame.setVisible(false);
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    frame.dispose();
                }
            });
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  ActionListener method
     *
     *@param  e  ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        String command = inputTextField.getText();

        if (command.length() == 0)
            return;

        inputTextField.setText("");

        GTPConnection.sendCommand(command);
    } //}}}

    //{{{ toggleDebugWindow() method
    /**
     *  Toggle GTP shell on / off
     *
     *@param  on  GTP shell on or off
     */
    protected static void toggleDebugWindow(boolean on) {
        try {
            frame.setVisible(on);
        } catch (NullPointerException e) {}
    } //}}}
}

