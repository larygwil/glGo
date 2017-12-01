/*
 *  GameInfoDialog.java
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

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import ggo.*;

/**
 *  Dialog for displaying and editing the game data
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:55 $
 */
public class GameInfoDialog extends JDialog implements ActionListener, Defines {
    //{{{ private members
    private JTextField whiteNameTextField,
            blackNameTextField,
            whiteRankTextField,
            blackRankTextField,
            komiTextField,
            handicapTextField,
            resultTextField,
            gameNameTextField,
            dateTextField,
            playedAtTextField,
            copyrightTextField;
    private GameData gameData;
    private KeyHandler keyHandler;
    private boolean result;
    //}}}

    //{{{ GameInfoDialog constructor
    /**
     *  Constructor for the GameInfoDialog object
     *
     *@param  parent  Description of the Parameter
     *@param  modal   Description of the Parameter
     *@param  data    Description of the Parameter
     */
    public GameInfoDialog(Frame parent, boolean modal, GameData data) {
        super(parent, gGo.getBoardResources().getString("Game_Information"), modal);
        keyHandler = new KeyHandler();
        initComponents();
        setLocationRelativeTo(parent);

        gameData = data;
        initSettings();

        setVisible(true);
    } //}}}

    //{{{ initComponents() method
    /**  Init the GUI elements */
    private void initComponents() {
        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Player panel
        JPanel playerPanel = new JPanel();
        playerPanel.setLayout(new GridBagLayout());
        playerPanel.setBorder(new CompoundBorder(new EmptyBorder(new Insets(2, 5, 2, 5)),
                new BevelBorder(BevelBorder.RAISED)));
        GridBagConstraints gridBagConstraints;

        JLabel whiteLabel = new JLabel(gGo.getBoardResources().getString("White"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 2;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        playerPanel.add(whiteLabel, gridBagConstraints);

        JLabel blackLabel = new JLabel(gGo.getBoardResources().getString("Black"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 2;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        playerPanel.add(blackLabel, gridBagConstraints);

        JLabel NameLabel = new JLabel(gGo.getBoardResources().getString("Name"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        playerPanel.add(NameLabel, gridBagConstraints);

        JLabel RankLabel = new JLabel(gGo.getBoardResources().getString("Rank"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        playerPanel.add(RankLabel, gridBagConstraints);

        whiteNameTextField = new JTextField(gGo.getBoardResources().getString("White"));
        whiteNameTextField.setPreferredSize(new Dimension(120, 25));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        whiteNameTextField.addKeyListener(keyHandler);
        playerPanel.add(whiteNameTextField, gridBagConstraints);

        blackNameTextField = new JTextField(gGo.getBoardResources().getString("Black"));
        blackNameTextField.setPreferredSize(new Dimension(120, 25));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        blackNameTextField.addKeyListener(keyHandler);
        playerPanel.add(blackNameTextField, gridBagConstraints);

        whiteRankTextField = new JTextField();
        whiteRankTextField.setPreferredSize(new Dimension(50, 25));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        whiteRankTextField.addKeyListener(keyHandler);
        playerPanel.add(whiteRankTextField, gridBagConstraints);

        blackRankTextField = new JTextField();
        blackRankTextField.setPreferredSize(new Dimension(50, 25));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        blackRankTextField.addKeyListener(keyHandler);
        playerPanel.add(blackRankTextField, gridBagConstraints);

        mainPanel.add(playerPanel);

        // Game panel
        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new GridLayout(7, 2, 5, 5));
        gamePanel.setBorder(new CompoundBorder(new EmptyBorder(new Insets(2, 5, 2, 5)),
                new BevelBorder(BevelBorder.RAISED)));

        JLabel komiLabel = new JLabel(gGo.getBoardResources().getString("Komi") + ":");
        komiLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        gamePanel.add(komiLabel);

        komiTextField = new JTextField();
        komiTextField.addKeyListener(keyHandler);
        gamePanel.add(komiTextField);

        JLabel handicapLabel = new JLabel(gGo.getBoardResources().getString("Handicap") + ":");
        handicapLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        gamePanel.add(handicapLabel);

        handicapTextField = new JTextField();
        handicapTextField.addKeyListener(keyHandler);
        gamePanel.add(handicapTextField);

        JLabel resultLabel = new JLabel(gGo.getBoardResources().getString("Result") + ":");
        resultLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        gamePanel.add(resultLabel);

        resultTextField = new JTextField();
        resultTextField.addKeyListener(keyHandler);
        gamePanel.add(resultTextField);

        JLabel gameNameLabel = new JLabel(gGo.getBoardResources().getString("Game_Name") + ":");
        gameNameLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        gamePanel.add(gameNameLabel);

        gameNameTextField = new JTextField();
        gameNameTextField.addKeyListener(keyHandler);
        gamePanel.add(gameNameTextField);

        JLabel dateLabel = new JLabel(gGo.getBoardResources().getString("Date") + ":");
        dateLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        gamePanel.add(dateLabel);

        dateTextField = new JTextField();
        dateTextField.addKeyListener(keyHandler);
        gamePanel.add(dateTextField);

        JLabel playedAtLabel = new JLabel(gGo.getBoardResources().getString("Played_at") + ":");
        playedAtLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        gamePanel.add(playedAtLabel);

        playedAtTextField = new JTextField();
        playedAtTextField.addKeyListener(keyHandler);
        gamePanel.add(playedAtTextField);

        JLabel copyrightLabel = new JLabel(gGo.getBoardResources().getString("Copyright") + ":");
        copyrightLabel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        gamePanel.add(copyrightLabel);

        copyrightTextField = new JTextField();
        copyrightTextField.addKeyListener(keyHandler);
        gamePanel.add(copyrightTextField);

        mainPanel.add(gamePanel);
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

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

    //{{{ initSettings() method
    /**  Fill GUI elements with data */
    private void initSettings() {
        if (gameData == null)
            return;

        whiteNameTextField.setText(gameData.playerWhite);
        blackNameTextField.setText(gameData.playerBlack);
        whiteRankTextField.setText(gameData.rankWhite);
        blackRankTextField.setText(gameData.rankBlack);
        komiTextField.setText(String.valueOf(gameData.komi));
        handicapTextField.setText(String.valueOf(gameData.handicap));
        resultTextField.setText(gameData.result);
        gameNameTextField.setText(gameData.gameName);
        dateTextField.setText(gameData.date);
        playedAtTextField.setText(gameData.place);
        copyrightTextField.setText(gameData.copyright);

        result = false;
    } //}}}

    //{{{ acceptChanges() method
    /**  Copy GUI element values to data */
    private void acceptChanges() {
        if (gameData == null)
            return;

        gameData.playerWhite = whiteNameTextField.getText();
        gameData.playerBlack = blackNameTextField.getText();
        gameData.rankWhite = whiteRankTextField.getText();
        gameData.rankBlack = blackRankTextField.getText();
        gameData.result = resultTextField.getText();
        gameData.gameName = gameNameTextField.getText();
        gameData.date = dateTextField.getText();
        gameData.place = playedAtTextField.getText();
        gameData.copyright = copyrightTextField.getText();
        try {
            gameData.komi = Float.parseFloat(komiTextField.getText());
        } catch (NumberFormatException e) {
            System.err.println("Failed to convert Komi value: " + e);
        }
        try {
            gameData.handicap = Integer.parseInt(handicapTextField.getText());
        } catch (NumberFormatException e) {
            System.err.println("Failed to convert Handicap value: " + e);
        }
    } //}}}

    //{{{ actionPerformed() method
    /**
     *  ActionListener method
     *
     *@param  e  ActionEvent
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (cmd.equals("Ok")) {
            acceptChanges();
            result = true;
            setVisible(false);
        }
        else if (cmd.equals("Cancel")) {
            result = false;
            setVisible(false);
        }

    } //}}}

    //{{{ hasResult() method
    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public boolean hasResult() {
        return result;
    } //}}}

    //{{{ inner class Keyhandler
    /**
     *  KeyAdapter subclas
     *
     *@author     Peter Strempel
     *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:55 $
     */
    class KeyHandler extends KeyAdapter {
        /**
         *  KeyListener method
         *
         *@param  e  KeyEvent
         */
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                setVisible(false);
        }
    } //}}}
}
