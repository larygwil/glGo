/*
 *  NewGameDialog.java
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
package ggo.dialogs;

import ggo.*;
import ggo.utils.Utils;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 *  Dialog for setting up a new game
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:55 $
 */
public class NewGameDialog extends JDialog implements ActionListener, Defines {
    //{{{ private members
    private JTextField playerWhiteTextField,
            playerBlackTextField,
            boardSizeTextField,
            komiTextField,
            handicapTextField;
    private boolean result = false;
    private KeyHandler keyHandler;
    //}}}

    //{{{ NewGameDialog constructor
    /**
     *  Creates new form NewGameDialog
     *
     *@param  parent  parent frame
     *@param  modal   set true to create a modal dialog
     */
    public NewGameDialog(Frame parent, boolean modal) {
        super(parent, gGo.getBoardResources().getString("New_Game"), modal);
        keyHandler = new KeyHandler();
        initComponents();
        setLocationRelativeTo(parent);
        setVisible(true);
    } //}}}

    //{{{ getResult() method
    /**
     *  Gets the result attribute of the NewGameDialog object
     *
     *@return    The result value
     */
    public boolean getResult() {
        return result;
    } //}}}

    //{{{ getWhiteName() method
    /**
     *  Gets the whiteName attribute of the NewGameDialog object
     *
     *@return    The whiteName value
     */
    public String getWhiteName() {
        return playerWhiteTextField.getText();
    } //}}}

    //{{{ getBlackName() method
    /**
     *  Gets the blackName attribute of the NewGameDialog object
     *
     *@return    The blackName value
     */
    public String getBlackName() {
        return playerBlackTextField.getText();
    } //}}}

    //{{{ getBoardSize() method
    /**
     *  Gets the boardSize attribute of the NewGameDialog object
     *
     *@return    The boardSize value
     */
    public int getBoardSize() {
        int s = Utils.convertStringToInt(boardSizeTextField.getText());
        if (s == -1)
            s = 19;

        else if (s < 4)
            s = 4;

        else if (s > 36)
            s = 36;

        return s;
    } //}}}

    //{{{ getKomi() method
    /**
     *  Gets the komi attribute of the NewGameDialog object
     *
     *@return    The komi value
     */
    public float getKomi() {
        return Utils.convertStringToFloat(komiTextField.getText());
    } //}}}

    //{{{ getHandicap() method
    /**
     *  Gets the handicap attribute of the NewGameDialog object
     *
     *@return    The handicap value
     */
    public int getHandicap() {
        return Utils.convertStringToInt(handicapTextField.getText());
    } //}}}

    //{{{ initComponents() method
    /**  Init the GUI elements */
    private void initComponents() {
        getContentPane().setLayout(new BorderLayout(5, 5));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(2, 2, 5, 5));

        JPanel playersPanel = new JPanel();
        playersPanel.setLayout(new GridLayout(2, 2, 15, 15));
        playersPanel.setBorder(new CompoundBorder(new EmptyBorder(new Insets(5, 10, 5, 10)),
                new BevelBorder(BevelBorder.RAISED)));

        JLabel playerWhiteLabel = new JLabel(gGo.getBoardResources().getString("White_player") + ":");
        playerWhiteLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        playersPanel.add(playerWhiteLabel);

        playerWhiteTextField = new JTextField(gGo.getBoardResources().getString("White"));
        playerWhiteTextField.addKeyListener(keyHandler);
        playersPanel.add(playerWhiteTextField);

        JLabel playerBlackLabel = new JLabel(gGo.getBoardResources().getString("Black_player") + ":");
        playerBlackLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        playersPanel.add(playerBlackLabel);

        playerBlackTextField = new JTextField(gGo.getBoardResources().getString("Black"));
        playerBlackTextField.addKeyListener(keyHandler);
        playersPanel.add(playerBlackTextField);

        inputPanel.add(playersPanel);

        JPanel dataPanel = new JPanel();
        dataPanel.setLayout(new GridLayout(3, 2, 5, 5));
        dataPanel.setBorder(new CompoundBorder(new EmptyBorder(new Insets(5, 10, 5, 10)),
                new BevelBorder(BevelBorder.RAISED)));

        JLabel boardSizeLabel = new JLabel(gGo.getBoardResources().getString("Board_size") + ":");
        boardSizeLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        dataPanel.add(boardSizeLabel);

        JPanel boardSizePanel = new JPanel();
        boardSizePanel.setLayout(new BorderLayout());

        JButton boardSizeLeftButton = new JButton();
        boardSizeLeftButton.setIcon(new ImageIcon(getClass().getResource("/images/left.gif")));
        boardSizeLeftButton.setMargin(new Insets(2, 2, 2, 2));
        boardSizeLeftButton.setActionCommand("boardSizeLeft");
        boardSizeLeftButton.addActionListener(this);
        boardSizePanel.add(boardSizeLeftButton, BorderLayout.WEST);

        boardSizeTextField = new JTextField("19");
        boardSizeTextField.setHorizontalAlignment(JTextField.CENTER);
        boardSizeTextField.setPreferredSize(new Dimension(40, 20));
        boardSizeTextField.addKeyListener(keyHandler);
        boardSizePanel.add(boardSizeTextField, BorderLayout.CENTER);

        JButton boardSizeRightButton = new JButton();
        boardSizeRightButton.setIcon(new ImageIcon(getClass().getResource("/images/right.gif")));
        boardSizeRightButton.setMargin(new Insets(2, 2, 2, 2));
        boardSizeRightButton.setActionCommand("boardSizeRight");
        boardSizeRightButton.addActionListener(this);
        boardSizePanel.add(boardSizeRightButton, BorderLayout.EAST);

        dataPanel.add(boardSizePanel);

        JLabel komiLabel = new JLabel(gGo.getBoardResources().getString("Komi") + ":");
        komiLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        dataPanel.add(komiLabel);

        komiTextField = new JTextField("5.5");
        komiTextField.addKeyListener(keyHandler);
        dataPanel.add(komiTextField);

        JLabel handicapLabel = new JLabel(gGo.getBoardResources().getString("Handicap") + ":");
        handicapLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        dataPanel.add(handicapLabel);

        handicapTextField = new JTextField("0");
        handicapTextField.addKeyListener(keyHandler);
        dataPanel.add(handicapTextField);

        inputPanel.add(dataPanel);

        getContentPane().add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton okButton = new JButton(gGo.getBoardResources().getString("Ok"));
        okButton.setActionCommand("Ok");
        okButton.addActionListener(this);
        buttonPanel.add(okButton);
        getRootPane().setDefaultButton(okButton);

        JButton cancelButton = new JButton(gGo.getBoardResources().getString("Cancel"));
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        buttonPanel.add(cancelButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
    } //}}}

    //{{{ boardSizeLeftButtonActionPerformed() method
    /**  Boardsize left button was clicked */
    private void boardSizeLeftButtonActionPerformed() {
        int number = Utils.convertStringToInt(boardSizeTextField.getText());
        if (number > 19)
            number = 19;

        else if (number > 13)
            number = 13;

        else if (number > 9)
            number = 9;

        boardSizeTextField.setText(String.valueOf(number));
    } //}}}

    //{{{ boardSizeRightButtonActionPerformed() method
    /**  Boardsize right button was clicked */
    private void boardSizeRightButtonActionPerformed() {
        int number = Utils.convertStringToInt(boardSizeTextField.getText());
        if (number < 9)
            number = 9;

        else if (number < 13)
            number = 13;

        else if (number < 19)
            number = 19;

        boardSizeTextField.setText(String.valueOf(number));
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  Handler for action evens
     *
     *@param  e  Action event
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Ok")) {
            result = true;
            setVisible(false);
        }
        else if (cmd.equals("Cancel")) {
            result = false;
            setVisible(false);
        }
        else if (cmd.equals("boardSizeLeft"))
            boardSizeLeftButtonActionPerformed();

        else if (cmd.equals("boardSizeRight"))
            boardSizeRightButtonActionPerformed();
    } //}}}

    //{{{ inner class Keyhandler
    /**
     *  Description of the Class
     *
     *@author     Peter Strempel
     *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:55 $
     */
    class KeyHandler extends KeyAdapter {
        /**
         *  Description of the Method
         *
         *@param  e  Description of the Parameter
         */
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                setVisible(false);
        }
    } //}}}
}

