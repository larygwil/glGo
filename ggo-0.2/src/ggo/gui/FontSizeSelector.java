/*
 *  FontSizeSelector.java
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
import java.awt.*;
import java.awt.event.*;
import ggo.utils.*;
import ggo.gGo;

/**
 *  Simple combobox, attached to an output JTextArea, controlling its font size. If the font
 *  size is changed, it is saved in the settings.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:55 $
 */
public class FontSizeSelector extends JPanel {
    //{{{ private members
    private JComboBox fontSizeComboBox;
    private JTextArea outputArea;
    private int currentFontSize;
    //}}}

    //{{{ FontSizeSelector() constructor
    /**
     *Constructor for the FontSizeSelector object
     *
     *@param  outputArea  The controlled JTextField
     */
    public FontSizeSelector(JTextArea outputArea) {
        this.outputArea = outputArea;

        currentFontSize = gGo.getSettings().getSerifFontSize();

        initComponents();

        // Add this size to the list if it does not exist
        int pos = -1;
        boolean found = false;
        String cval = String.valueOf(currentFontSize);
        for (int i = 0, sz = fontSizeComboBox.getItemCount(); i < sz; i++) {
            String s = (String)(fontSizeComboBox.getItemAt(i));
            if (s.equals(cval)) {
                pos = i;
                found = true;
                break;
            }
            else {
                try {
                    if (Integer.parseInt(s) < currentFontSize)
                        pos = i;
                } catch (NumberFormatException e) {}
            }
        }
        if (!found) {
            fontSizeComboBox.insertItemAt(String.valueOf(currentFontSize), pos + 1);
            fontSizeComboBox.setSelectedIndex(pos + 1);
        }
        else {
            if (pos == -1)
                pos = 1;
            fontSizeComboBox.setSelectedIndex(pos);
        }
    } //}}}

    //{{{ initComponents() method
    /**  Init GUI components */
    private void initComponents() {
        fontSizeComboBox = new JComboBox();

        fontSizeComboBox.setEditable(true);
        fontSizeComboBox.setModel(new DefaultComboBoxModel(new String[]{"8", "10", "12", "14", "16"}));
        fontSizeComboBox.setToolTipText(gGo.getIGSResources().getString("font_size_select_tooltip"));
        fontSizeComboBox.setPreferredSize(new Dimension(44, 20));
        fontSizeComboBox.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (evt.getActionCommand().equals("comboBoxChanged")) {
                        String s = (String)(fontSizeComboBox.getSelectedItem());
                        int size = 12;
                        try {
                            size = Integer.parseInt(s);

                        } catch (NumberFormatException e) {
                            getToolkit().beep();
                        }

                        if (size == currentFontSize)
                            return;

                        try {
                            outputArea.setFont(new Font("Serif", 0, size));
                        } catch (NullPointerException e) {}

                        currentFontSize = size;
                        gGo.getSettings().setSerifFontSize(size);
                        gGo.getSettings().saveSettings();
                    }
                }
            });

        add(fontSizeComboBox);
    } //}}}
}

