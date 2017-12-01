/*
 *  Normaltools.java
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
package ggo.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import ggo.gGo;
import ggo.gui.Clock;
import ggo.gui.PopupLabel;

/**
 *  The panel showing the captures active in normal game mode.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.5 $, $Date: 2002/10/23 01:50:53 $
 */
public class NormalTools extends JPanel {
    //{{{ private members
    private JLabel whiteLabel, blackLabel, whiteTitleLabel, blackTitleLabel,
            handicapLabel, komiLabel;
    private PopupLabel whitePlayerLabel, blackPlayerLabel;
    private Clock clockWhite, clockBlack;
    //}}}

    //{{{ NormalTools constructor
    /**  Constructor for the NormalTools object */
    public NormalTools(String superclassName) {
        initComponents(superclassName);
    } //}}}

    //{{{ initComponents() method
    /**  Init the GUI elements */
    private void initComponents(String superclassName) {
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        GridBagConstraints gridBagConstraints;
        Border emptyBorder = BorderFactory.createEmptyBorder(1, 3, 1, 3);
        Border emptyBorder2 = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        Insets insets = new Insets(1, 3, 1, 3);

        boolean popupFlag = superclassName.equals("ggo.igs.gui.IGSObserverFrame") ||
                            superclassName.equals("ggo.igs.gui.IGSPlayingFrame") ||
                            superclassName.equals("ggo.igs.gui.IGSTeachingFrame");

        //{{{ Handicap and Komi
        JPanel handicapKomiPanel = new JPanel();
        handicapKomiPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        handicapKomiPanel.setLayout(new BoxLayout(handicapKomiPanel, BoxLayout.X_AXIS));

        komiLabel = new JLabel(gGo.getBoardResources().getString("Komi") + " " + "5.5");
        komiLabel.setBorder(emptyBorder);
        handicapKomiPanel.add(komiLabel);

        handicapLabel = new JLabel(gGo.getBoardResources().getString("handicap_short") + " 0");
        handicapLabel.setBorder(emptyBorder);
        handicapKomiPanel.add(handicapLabel);
        //}}}

        //{{{ White panel
        JPanel whitePanel = new JPanel();
        whitePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        whitePanel.setLayout(new GridBagLayout());
        // --- 1.3 ---
        // This is somewhat strange, 1.3 messes this up
        if (!gGo.is13())
            whitePanel.setBorder(BorderFactory.createCompoundBorder(emptyBorder2, BorderFactory.createTitledBorder(gGo.getBoardResources().getString("White"))));
        else
            whitePanel.setBorder(BorderFactory.createTitledBorder(gGo.getBoardResources().getString("White")));

        // White name
        whitePlayerLabel = new PopupLabel(gGo.getBoardResources().getString("White"), popupFlag);
        whitePlayerLabel.setBorder(emptyBorder);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = insets;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        whitePanel.add(whitePlayerLabel, gridBagConstraints);

        // White clock
        clockWhite = new Clock();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = insets;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        whitePanel.add(clockWhite, gridBagConstraints);

        // White captures
        whiteTitleLabel = new JLabel(gGo.getBoardResources().getString("Captures"));
        whiteTitleLabel.setBorder(emptyBorder);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = insets;
        whitePanel.add(whiteTitleLabel, gridBagConstraints);

        whiteLabel = new JLabel("0");
        whiteLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        whiteLabel.setBorder(emptyBorder);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = insets;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        whitePanel.add(whiteLabel, gridBagConstraints);
        //}}}

        //{{{ Black panel
        JPanel blackPanel = new JPanel();
        blackPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        blackPanel.setLayout(new GridBagLayout());
        //
        // --- 1.3 ---
        // This is somewhat strange, 1.3 messes this up
        if (!gGo.is13())
            blackPanel.setBorder(BorderFactory.createCompoundBorder(emptyBorder2, BorderFactory.createTitledBorder(gGo.getBoardResources().getString("Black"))));
        else
            blackPanel.setBorder(BorderFactory.createTitledBorder(gGo.getBoardResources().getString("Black")));

        // Black name
        blackPlayerLabel = new PopupLabel(gGo.getBoardResources().getString("Black"), popupFlag);
        blackPlayerLabel.setBorder(emptyBorder);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = insets;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        blackPanel.add(blackPlayerLabel, gridBagConstraints);

        // Black clock
        clockBlack = new Clock();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = insets;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        blackPanel.add(clockBlack, gridBagConstraints);

        // Black captures
        blackTitleLabel = new JLabel(gGo.getBoardResources().getString("Captures"));
        blackTitleLabel.setBorder(emptyBorder);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = insets;
        blackPanel.add(blackTitleLabel, gridBagConstraints);

        blackLabel = new JLabel("0");
        blackLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        blackLabel.setBorder(emptyBorder);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = insets;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        blackPanel.add(blackLabel, gridBagConstraints);
        //}}}

        add(whitePanel);
        add(blackPanel);
        add(handicapKomiPanel);
    } //}}}

    //{{{ getClockBlack() method
    /**
     *  Gets the clockBlack attribute of the NormalTools object
     *
     *@return    The clockBlack value
     */
    public Clock getClockBlack() {
        return clockBlack;
    } //}}}

    //{{{ getClockWhite() method
    /**
     *  Gets the clockWhite attribute of the NormalTools object
     *
     *@return    The clockWhite value
     */
    public Clock getClockWhite() {
        return clockWhite;
    } //}}}

    //{{{ setCaptures() method
    /**
     *  Sets the captures
     *
     *@param  w  White captures
     *@param  b  Black captures
     */
    public void setCaptures(int w, int b) {
        whiteLabel.setText(String.valueOf(w));
        blackLabel.setText(String.valueOf(b));
        if (whiteTitleLabel.getText().equals(gGo.getBoardResources().getString("Points"))) {
            whiteTitleLabel.setText(gGo.getBoardResources().getString("Captures"));
            blackTitleLabel.setText(gGo.getBoardResources().getString("Captures"));
        }
    } //}}}

    //{{{ setScore() method
    /**
     *  Sets the score
     *
     *@param  w  White score
     *@param  b  Black score
     */
    public void setScore(float w, float b) {
        whiteLabel.setText(String.valueOf(w));
        blackLabel.setText(String.valueOf(b));
        whiteTitleLabel.setText(gGo.getBoardResources().getString("Points"));
        blackTitleLabel.setText(gGo.getBoardResources().getString("Points"));
    } //}}}

    //{{{ setGameInfo() method
    /**
     *  Sets the player names, handicap and komi
     *
     *@param  white     White name
     *@param  black     Black name
     *@param  handicap  Handicap
     *@param  komi      Komi
     */
    public void setGameInfo(final String white, final String black, final int handicap, final float komi) {
        whitePlayerLabel.setText(white);
        blackPlayerLabel.setText(black);
        handicapLabel.setText(gGo.getBoardResources().getString("handicap_short") + " " + String.valueOf(handicap));
        komiLabel.setText(gGo.getBoardResources().getString("Komi") + " " + String.valueOf(komi));
    } //}}}
}
