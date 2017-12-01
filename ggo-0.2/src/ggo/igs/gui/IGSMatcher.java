/*
 *  IGSMatcher.java
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
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import ggo.igs.*;
import ggo.*;
import ggo.utils.*;

/**
 *  Interface for game requests.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.10 $, $Date: 2002/10/22 11:40:35 $
 */
public class IGSMatcher extends JFrame {
    //{{{ private members
    private String player1, player2, challenger, challengerRank;
    private int size, mainTime, byoyomiTime, byoyomiStones;
    private boolean requesting, isAutomatch;
    private JComboBox whiteComboBox, blackComboBox;
    private JTextField sizeTextField, mainTimeTextField, byoyomiTextField;
    private static int matchCounter = 0;
    private static ResourceBundle igs_match_resources;
    //}}}

    //{{{ static constructor
    static {
        igs_match_resources = gGo.getIGSMatchResources();
    } //}}}

    //{{{ IGSMatcher() constructor
    /**
     *Constructor for the IGSMatcher object
     *
     *@param  requesting     True if we are sending out the request, false if we recieve it
     *@param  whiteName      Name of white player
     *@param  blackName      Name of black player
     *@param  size           Board size
     *@param  mainTime       Main time
     *@param  byoyomiTime    Byo yomi time
     *@param  byoyomiStones  Number of byo-yomi stones per period
     *@param  challenger     Name of the challenger
     *@param  isAutomatch    True if this is an automatch request, false for normal match
     */
    public IGSMatcher(boolean requesting, String whiteName, String blackName, int size,
            int mainTime, int byoyomiTime, int byoyomiStones, String challenger, boolean isAutomatch) {
        setTitle(requesting ? igs_match_resources.getString("Match_request") : igs_match_resources.getString("Match"));
        this.requesting = requesting;
        this.player1 = whiteName;
        this.player2 = blackName;
        this.size = size;
        this.mainTime = mainTime;
        this.byoyomiTime = byoyomiTime;
        this.byoyomiStones = byoyomiStones;
        this.challenger = challenger;
        this.challengerRank = null;
        this.isAutomatch = isAutomatch;

        // Try to get ranks from database
        IGSRank r1 = null;
        IGSRank r2 = null;
        if (IGSConnection.getAutoUpdater().hasPlayer(player1))
            r1 = IGSConnection.getAutoUpdater().getPlayer(player1).getRank();
        if (IGSConnection.getAutoUpdater().hasPlayer(player2))
            r2 = IGSConnection.getAutoUpdater().getPlayer(player2).getRank();

        // If we got the ranks, switch white-black if player2 is stronger than player1.
        // Only if we are requesting. Don't do this for incoming requests.
        if (r1 != null && r2 != null) {
            if (requesting && r1.compareTo(r2) > 0) {
                String tmp = player1;
                player1 = player2;
                player2 = tmp;
                IGSRank tmpR = r1;
                r1 = r2;
                r2 = tmpR;
            }

            if (player1.equals(challenger))
                challengerRank = r1.toString();
            else
                challengerRank = r2.toString();
        }

        initComponents();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
                (screenSize.width - getWidth()) / 2 + matchCounter * 20,
                (screenSize.height - getHeight()) / 2 + matchCounter * 20);
        matchCounter++;

        // Create an icon for the appliction
        Image icon = ImageHandler.loadImage("32emerald.png");
        try {
            setIconImage(icon);
        } catch (NullPointerException e) {
            System.err.println("Failed to load icon image.");
        }

        setVisible(true);
    } //}}}

    //{{{ initComponents() method
    /**  Init GUI components */
    private void initComponents() {
        JPanel topPanel = new JPanel();
        JLabel requestLabel = new JLabel();
        JLabel challengerLabel = new JLabel();
        JButton statsButton = new JButton();
        JPanel mainPanel = new JPanel();
        JPanel playerPanel = new JPanel();
        JLabel whiteLabel = new JLabel();
        JLabel blackLabel = new JLabel();
        whiteComboBox = new JComboBox();
        blackComboBox = new JComboBox();
        JPanel settingsPanel = new JPanel();
        JLabel sizeLabel = new JLabel();
        sizeTextField = new JTextField();
        JLabel mainTimeLabel = new JLabel();
        mainTimeTextField = new JTextField();
        JLabel byoyomiLabel = new JLabel();
        byoyomiTextField = new JTextField();
        JPanel buttonPanel = new JPanel();
        JButton acceptButton = new JButton();
        JButton declineButton = new JButton();

        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent evt) {
                    declineButtonActionPerformed(null);
                }
            });

        if (requesting) {
            requestLabel.setText(igs_match_resources.getString("requesting_match_with"));
        }
        else {
            if (isAutomatch)
                requestLabel.setText(igs_match_resources.getString("requested_automatch"));
            else
                requestLabel.setText(igs_match_resources.getString("requested_match"));
        }
        topPanel.add(requestLabel);

        challengerLabel.setForeground(Color.red);
        challengerLabel.setText(challenger + (challengerRank != null ? " " + challengerRank : ""));
        topPanel.add(challengerLabel);

        if (!requesting) {
            statsButton.setText(igs_match_resources.getString("stats_button"));
            statsButton.setToolTipText(igs_match_resources.getString("stats_button_tooltip"));
            statsButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        statsButtonActionPerformed(evt);
                    }
                });
            topPanel.add(statsButton);
        }

        getContentPane().add(topPanel, BorderLayout.NORTH);

        Insets insets = new Insets(5, 5, 5, 5);
        Dimension dim = new Dimension(40, 26);

        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(
                BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 5, 5, 5),
                BorderFactory.createBevelBorder(BevelBorder.RAISED)));
        playerPanel.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints2;

        whiteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        whiteLabel.setText(igs_match_resources.getString("White"));
        gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints2.insets = insets;
        playerPanel.add(whiteLabel, gridBagConstraints2);

        blackLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackLabel.setText(igs_match_resources.getString("Black"));
        gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.gridx = 1;
        gridBagConstraints2.gridy = 0;
        gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints2.insets = insets;
        playerPanel.add(blackLabel, gridBagConstraints2);

        whiteComboBox.setModel(new DefaultComboBoxModel(new String[]{player1, player2}));
        whiteComboBox.setEnabled(!isAutomatch);
        whiteComboBox.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    whiteComboBoxActionPerformed(evt);
                }
            });

        gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridy = 1;
        gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints2.insets = insets;
        playerPanel.add(whiteComboBox, gridBagConstraints2);

        blackComboBox.setModel(new DefaultComboBoxModel(new String[]{player2, player1}));
        blackComboBox.setEnabled(!isAutomatch);
        blackComboBox.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    blackComboBoxActionPerformed(evt);
                }
            });

        gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.gridx = 1;
        gridBagConstraints2.gridy = 1;
        gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints2.insets = insets;
        playerPanel.add(blackComboBox, gridBagConstraints2);

        mainPanel.add(playerPanel, BorderLayout.NORTH);

        settingsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints1;

        sizeLabel.setText(igs_match_resources.getString("Size") + ":");
        gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.insets = insets;
        settingsPanel.add(sizeLabel, gridBagConstraints1);

        sizeTextField.setHorizontalAlignment(JTextField.TRAILING);
        sizeTextField.setText(String.valueOf(size));
        sizeTextField.setPreferredSize(dim);
        sizeTextField.setEnabled(!isAutomatch);
        gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.insets = insets;
        settingsPanel.add(sizeTextField, gridBagConstraints1);

        mainTimeLabel.setText(igs_match_resources.getString("Main_time") + ":");
        gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.gridy = 1;
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.insets = insets;
        settingsPanel.add(mainTimeLabel, gridBagConstraints1);

        mainTimeTextField.setHorizontalAlignment(JTextField.TRAILING);
        mainTimeTextField.setText(String.valueOf(mainTime));
        mainTimeTextField.setPreferredSize(dim);
        mainTimeTextField.setEnabled(!isAutomatch);
        gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.gridx = 1;
        gridBagConstraints1.gridy = 1;
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.insets = insets;
        settingsPanel.add(mainTimeTextField, gridBagConstraints1);

        byoyomiLabel.setText(igs_match_resources.getString("Byo-yomi_time") + ":");
        gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.gridy = 2;
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.insets = insets;
        settingsPanel.add(byoyomiLabel, gridBagConstraints1);

        byoyomiTextField.setHorizontalAlignment(JTextField.TRAILING);
        byoyomiTextField.setText(String.valueOf(byoyomiTime));
        byoyomiTextField.setPreferredSize(dim);
        byoyomiTextField.setEnabled(!isAutomatch);
        gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.gridx = 1;
        gridBagConstraints1.gridy = 2;
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.insets = insets;
        settingsPanel.add(byoyomiTextField, gridBagConstraints1);

        if (isAutomatch) {
            JLabel byoyomiStonesLabel = new JLabel();
            byoyomiStonesLabel.setText(igs_match_resources.getString("Byo-yomi_stones") + ":");
            byoyomiStonesLabel.setEnabled(isAutomatch);
            gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = 0;
            gridBagConstraints1.gridy = 3;
            gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.insets = insets;
            settingsPanel.add(byoyomiStonesLabel, gridBagConstraints1);

            JTextField byoyomiStonesTextField = new JTextField();
            byoyomiStonesTextField.setHorizontalAlignment(JTextField.TRAILING);
            byoyomiStonesTextField.setText(String.valueOf(byoyomiStones));
            byoyomiStonesTextField.setPreferredSize(dim);
            byoyomiStonesTextField.setEnabled(false);
            gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = 1;
            gridBagConstraints1.gridy = 3;
            gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.insets = insets;
            settingsPanel.add(byoyomiStonesTextField, gridBagConstraints1);
        }

        mainPanel.add(settingsPanel, BorderLayout.CENTER);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 5));

        acceptButton.setText(requesting ? igs_match_resources.getString("Ok") : igs_match_resources.getString("Accept"));
        acceptButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    acceptButtonActionPerformed(evt);
                }
            });
        buttonPanel.add(acceptButton);

        declineButton.setText(requesting ? igs_match_resources.getString("Cancel") : igs_match_resources.getString("Decline"));
        declineButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    declineButtonActionPerformed(evt);
                }
            });
        buttonPanel.add(declineButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        pack();
    } //}}}

    //{{{ statsButtonActionPerformed() method
    /**
     *  Stats button was clicked
     *
     *@param  evt  ActionEvent
     */
    private void statsButtonActionPerformed(ActionEvent evt) {
        IGSConnection.sendCommand("stats " + challenger);
    } //}}}

    //{{{ declineButtonActionPerformed() method
    /**
     *  Decline button was clicked
     *
     *@param  evt  ActionEvent
     */
    private void declineButtonActionPerformed(ActionEvent evt) {
        if (!requesting)
            IGSConnection.sendCommand("decline " + challenger);
        closeDialog();
    } //}}}

    //{{{ acceptButtonActionPerformed() method
    /**
     *  Accept button was clicked
     *
     *@param  evt  ActionEvent
     */
    private void acceptButtonActionPerformed(ActionEvent evt) {
        if (!isAutomatch) {
            // match qgodev W 19 0 0
            IGSConnection.sendCommand(
                    "match " + challenger + " " +
                    (IGSConnection.getLoginName().equals(whiteComboBox.getSelectedItem()) ? "W" : "B") + " " +
                    sizeTextField.getText() + " " +
                    mainTimeTextField.getText() + " " +
                    byoyomiTextField.getText());

            // Save time settings when sending match
            if (requesting) {
                gGo.getSettings().setIGSMatchMainTime(Utils.convertStringToInt(mainTimeTextField.getText()));
                gGo.getSettings().setIGSMatchByoyomiTime(Utils.convertStringToInt(byoyomiTextField.getText()));
                gGo.getSettings().saveSettings();
            }
        }
        else {
            // automatch qgodev
            IGSConnection.sendCommand("automatch " + challenger);
            IGSReader.automatchFlag = true;
        }
        // Remember settings
        try {
            IGSMainWindow.getIGSConnection().getIGSReader().rememberMatchTime(
                    challenger,
                    new IGSTime(Integer.parseInt(mainTimeTextField.getText()),
                    Integer.parseInt(byoyomiTextField.getText()),
                    (mainTime == 0 && byoyomiTime == 0 ? -1 : byoyomiStones)));
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse time settings in match dialog: " + e);
        }
        closeDialog();
    } //}}}

    //{{{ blackComboBoxActionPerformed() method
    /**
     *  Black player combobox was changed
     *
     *@param  evt  ActionEvent
     */
    private void blackComboBoxActionPerformed(ActionEvent evt) {
        if (evt.getActionCommand().equals("comboBoxChanged"))
            whiteComboBox.setSelectedIndex(blackComboBox.getSelectedIndex());
    } //}}}

    //{{{ whiteComboBoxActionPerformed() method
    /**
     *  White player combobox was changed
     *
     *@param  evt  ActionEvent
     */
    private void whiteComboBoxActionPerformed(ActionEvent evt) {
        if (evt.getActionCommand().equals("comboBoxChanged"))
            blackComboBox.setSelectedIndex(whiteComboBox.getSelectedIndex());
    } //}}}

    //{{{ closeDialog() method
    /** Closes the dialog */
    private void closeDialog() {
        matchCounter--;
        setVisible(false);
        dispose();
    } //}}}
}

