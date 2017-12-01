/*
 *  EditTools.java
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
import java.awt.event.*;
import ggo.Defines;
import ggo.gGo;

/**
 *  The panel showing the editing tool buttons, active in edit mode.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/21 12:39:55 $
 */
public class EditTools extends JPanel implements ItemListener, Defines {
    //{{{ private members
    private JToggleButton stoneToggleButton,
            squareToggleButton,
            triangleToggleButton,
            textToggleButton,
            circleToggleButton,
            crossToggleButton,
            numberToggleButton;
    private ButtonGroup toggleGroup;
    private int markType;
    //}}}

    //{{{ EditTools constructor
    /**  Constructor for the EditTools object */
    public EditTools() {
        initComponents();
        markType = MARK_STONE;
    } //}}}

    //{{{ getMarkType() method
    /**
     *  Gets the selected mark type
     *
     *@return    The mark type value
     */
    public int getMarkType() {
        return markType;
    } //}}}

    //{{{ setMarkType() method
    /**
     *  Sets the mark type
     *
     *@param  t  The new mark type value
     */
    public void setMarkType(int t) {
        markType = t;
    } //}}}

    //{{{ initComponents() method
    /**  Init GUI elements */
    private void initComponents() {
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints;

        toggleGroup = new ButtonGroup();

        // Create toggle buttons
        stoneToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/tools/stone.gif")), true);
        gridBagConstraints = new GridBagConstraints();
        add(stoneToggleButton, gridBagConstraints);
        toggleGroup.add(stoneToggleButton);
        stoneToggleButton.addItemListener(this);

        squareToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/tools/square.gif")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        add(squareToggleButton, gridBagConstraints);
        toggleGroup.add(squareToggleButton);
        squareToggleButton.addItemListener(this);

        triangleToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/tools/triangle.gif")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        add(triangleToggleButton, gridBagConstraints);
        toggleGroup.add(triangleToggleButton);
        triangleToggleButton.addItemListener(this);

        textToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/tools/text.gif")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        add(textToggleButton, gridBagConstraints);
        toggleGroup.add(textToggleButton);
        textToggleButton.addItemListener(this);

        circleToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/tools/circle.gif")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        add(circleToggleButton, gridBagConstraints);
        toggleGroup.add(circleToggleButton);
        circleToggleButton.addItemListener(this);

        crossToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/tools/cross.gif")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        add(crossToggleButton, gridBagConstraints);
        toggleGroup.add(crossToggleButton);
        crossToggleButton.addItemListener(this);

        numberToggleButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/tools/number.gif")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        add(numberToggleButton, gridBagConstraints);
        toggleGroup.add(numberToggleButton);
        numberToggleButton.addItemListener(this);
    } //}}}

    //{{{ itemStateChanged() method
    /**
     *  ItemListener method: Toggle button was pressed
     *
     *@param  e  ItemEvent
     */
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();

        if (e.getStateChange() == ItemEvent.SELECTED) {

            if (source == stoneToggleButton)
                markType = MARK_STONE;
            else if (source == squareToggleButton)
                markType = MARK_SQUARE;
            else if (source == triangleToggleButton)
                markType = MARK_TRIANGLE;
            else if (source == textToggleButton)
                markType = MARK_TEXT;
            else if (source == circleToggleButton)
                markType = MARK_CIRCLE;
            else if (source == crossToggleButton)
                markType = MARK_CROSS;
            else if (source == numberToggleButton)
                markType = MARK_NUMBER;
        }
    } //}}}
}

