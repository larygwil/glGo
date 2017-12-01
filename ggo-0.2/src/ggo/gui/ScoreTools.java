/*
 *  ScoreTools.java
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

/**
 *  The panel showing the labels used in score mode.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class ScoreTools extends JPanel {
    //{{{ private members
    private JLabel whiteTerrLabel, whiteCapsLabel, whiteTotalLabel;
    private JLabel blackTerrLabel, blackCapsLabel, blackTotalLabel;
    //}}}

    //{{{ ScoreTools constructor
    /**Constructor for the ScoreTools object */
    public ScoreTools() {
        initComponents();
    } //}}}

    //{{{ initComponents() method
    /**  Init the GUI elements */
    private void initComponents() {
        setBorder(new BevelBorder(BevelBorder.RAISED));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        //{{{ White
        JPanel panelWhite = new JPanel();
        panelWhite.setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints;
        // --- 1.3 ---
        // This is somewhat strange, 1.3 messes this up
        if (!gGo.is13())
            panelWhite.setBorder(new CompoundBorder(new EmptyBorder(new Insets(6, 6, 6, 6)), new TitledBorder(gGo.getBoardResources().getString("White"))));
        else
            panelWhite.setBorder(new TitledBorder(gGo.getBoardResources().getString("White")));

        // Territory
        JLabel label1 = new JLabel(gGo.getBoardResources().getString("territory") + ":");
        label1.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelWhite.add(label1, gridBagConstraints);

        whiteTerrLabel = new JLabel("0");
        whiteTerrLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        whiteTerrLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelWhite.add(whiteTerrLabel, gridBagConstraints);

        // Captures
        JLabel label2 = new JLabel(gGo.getBoardResources().getString("caps") + ":");
        label2.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelWhite.add(label2, gridBagConstraints);

        whiteCapsLabel = new JLabel("0");
        whiteCapsLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        whiteCapsLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelWhite.add(whiteCapsLabel, gridBagConstraints);

        // Total
        JLabel label3 = new JLabel(gGo.getBoardResources().getString("Total") + ":");
        label3.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelWhite.add(label3, gridBagConstraints);

        whiteTotalLabel = new JLabel("0");
        whiteTotalLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        whiteTotalLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelWhite.add(whiteTotalLabel, gridBagConstraints);
        //}}}

        //{{{ Black
        JPanel panelBlack = new JPanel();
        panelBlack.setLayout(new GridBagLayout());
        // --- 1.3 ---
        if (!gGo.is13())
            panelBlack.setBorder(new CompoundBorder(new EmptyBorder(new Insets(6, 6, 6, 6)), new TitledBorder(gGo.getBoardResources().getString("Black"))));
        else
            panelBlack.setBorder(new TitledBorder(gGo.getBoardResources().getString("Black")));

        // Territory
        JLabel label4 = new JLabel(gGo.getBoardResources().getString("territory") + ":");
        label4.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelBlack.add(label4, gridBagConstraints);

        blackTerrLabel = new JLabel("0");
        blackTerrLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        blackTerrLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelBlack.add(blackTerrLabel, gridBagConstraints);

        // Captures
        JLabel label5 = new JLabel(gGo.getBoardResources().getString("caps") + ":");
        label5.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelBlack.add(label5, gridBagConstraints);

        blackCapsLabel = new JLabel("0");
        blackCapsLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        blackCapsLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelBlack.add(blackCapsLabel, gridBagConstraints);

        // Total
        JLabel label6 = new JLabel(gGo.getBoardResources().getString("Total") + ":");
        label6.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelBlack.add(label6, gridBagConstraints);

        blackTotalLabel = new JLabel("0");
        blackTotalLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        blackTotalLabel.setBorder(new EmptyBorder(new Insets(1, 1, 1, 1)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(1, 1, 1, 1);
        panelBlack.add(blackTotalLabel, gridBagConstraints);
        //}}}

        add(panelWhite);
        add(panelBlack);
    } //}}}

    //{{{ setResult() method
    /**
     *  Sets the result when scoring is done
     *
     *@param  terrW  White territory
     *@param  capsW  White captures
     *@param  terrB  Black territory
     *@param  capsB  Black captures
     *@param  komi   Komi
     */
    protected void setResult(int terrW, int capsW, int terrB, int capsB, float komi) {
        whiteTerrLabel.setText(String.valueOf(terrW));
        whiteCapsLabel.setText(String.valueOf(capsW));
        whiteTotalLabel.setText(String.valueOf(terrW + capsW + komi));
        blackTerrLabel.setText(String.valueOf(terrB));
        blackCapsLabel.setText(String.valueOf(capsB));
        blackTotalLabel.setText(String.valueOf(terrB + capsB));
    } //}}}

}

