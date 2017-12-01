/*
 *  ImageHandler.java
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
package ggo.utils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.*;
import ggo.*;

/**
 *  Global image handler, responsible for loading and providing the images.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/09/21 12:39:56 $
 */
public final class ImageHandler implements Defines {
    /**  Number of white stone images */
    public final static int WHITE_IMAGES_NUMBER = 8;

    //{{{ private members
    private static ImageIcon gGoImageIcon = null;
    private static Image tableImage = null;
    private static Image boardImage = null;
    private static Image stoneBlackImage = null;
    private static Image[] stoneWhiteImages;
    private static Hashtable cachedBlackImages;
    private static Hashtable[] cachedWhiteImages;
    //}}}

    //{{{ Static constructor
    static {
        // Preload stones
        stoneBlackImage = loadImage("blk.png");
        stoneWhiteImages = new Image[WHITE_IMAGES_NUMBER];
        stoneWhiteImages[0] = loadImage("hyuga1.png");
        stoneWhiteImages[1] = loadImage("hyuga2.png");
        stoneWhiteImages[2] = loadImage("hyuga3.png");
        stoneWhiteImages[3] = loadImage("hyuga4.png");
        stoneWhiteImages[4] = loadImage("hyuga5.png");
        stoneWhiteImages[5] = loadImage("hyuga6.png");
        stoneWhiteImages[6] = loadImage("hyuga7.png");
        stoneWhiteImages[7] = loadImage("hyuga8.png");

        cachedBlackImages = new Hashtable();
        cachedWhiteImages = new Hashtable[WHITE_IMAGES_NUMBER];
        for (int i = 0; i < WHITE_IMAGES_NUMBER; i++)
            cachedWhiteImages[i] = new Hashtable();
    } //}}}

    //{{{ getgGoImageIcon() method
    /**
     *  Get the gGo Image in ImageIcon format
     *
     *@return    ImageIcon
     */
    public static ImageIcon getgGoImageIcon() {
        if (gGoImageIcon != null)
            return gGoImageIcon;
        else
            return gGoImageIcon = loadImageIcon("ggo.gif");
    } //}}}

    //{{{ getgGoImage() method
    /**
     *  Get the gGo Image in Image format
     *
     *@return    Image
     */
    public static Image getgGoImage() {
        return getgGoImageIcon().getImage();
    } //}}}

    //{{{ getTableImage() method
    /**
     *  Gets the green table image
     *
     *@return    The tableImage Image
     */
    public static Image getTableImage() {
        if (tableImage != null)
            return tableImage;
        else
            return tableImage = loadImage("table.png");
    } //}}}

    //{{{ getBoardImage() method
    /**
     *  Gets the wooden board image
     *
     *@return    The boardImage Image
     */
    public static Image getBoardImage() {
        if (boardImage != null)
            return boardImage;
        else
            return boardImage = loadImage("kaya.jpg");
    } //}}}

    //{{{ getStoneBlackImage() method
    /**
     *  Gets the black stone image
     *
     *@return    The black stone image
     */
    public static Image getStoneBlackImage() {
        return stoneBlackImage;
    } //}}}

    //{{{ getScaledStoneBlackImage() method
    /**
     *  Gets a scaled version of the black stone image
     *
     *@param  factor  Scaling factor
     *@return         The scaled black stone image
     */
    public static Image getScaledStoneBlackImage(int factor) {
        Integer key = new Integer(factor);
        if (cachedBlackImages.containsKey(key)) {
            // System.err.println("Found black stone image of factor " + factor + " in second level cache");
            return (Image)cachedBlackImages.get(key);
        }
        else {
            // System.err.println("Rescaling and caching black stone image, factor = " + factor);
            Image tmp = getStoneBlackImage().getScaledInstance(factor, factor, Image.SCALE_SMOOTH);
            cachedBlackImages.put(key, tmp);
            return tmp;
        }
    } //}}}

    //{{{ getStoneWhiteImage() method
    /**
     *  Gets the n-th white stone image.
     *
     *@param  n  Number of white stone image to load
     *@return    The the n-th white stone image
     */
    public static Image getStoneWhiteImage(int n) {
        if (n < 0 || n >= WHITE_IMAGES_NUMBER) {
            n = (int)(Math.random() * WHITE_IMAGES_NUMBER);
            System.err.println("ImageHandler.getStoneWhiteImage() - Oops!");
        }
        return stoneWhiteImages[n];
    } //}}}

    //{{{ getScaledStoneWhiteImage() method
    /**
     *  Gets a scaled version of the n-th white stone image.
     *
     *@param  factor  Scaling factor
     *@param  n       Number of white stone image to load
     *@return         The scaled n-th white stone image
     */
    public static Image getScaledStoneWhiteImage(int factor, int n) {
        Integer key = new Integer(factor);
        if (cachedWhiteImages[n].containsKey(key)) {
            // System.err.println("Found white stone #" + n + " image of factor " + factor + " in second level cache");
            return (Image)cachedWhiteImages[n].get(key);
        }
        else {
            // System.err.println("Rescaling and caching white stone #" + n + " image, factor = " + factor);
            Image tmp = getStoneWhiteImage(n).getScaledInstance(factor, factor, Image.SCALE_SMOOTH);
            cachedWhiteImages[n].put(key, tmp);
            return tmp;
        }
    } //}}}

    //{{{ loadImage() method
    /**
     *  Load an Image
     *
     *@param  name  filename of the image to load
     *@return       The loaded image
     */
    public static Image loadImage(String name) {
        return loadImageIcon(name).getImage();
    } //}}}

    //{{{ loadImageIcon() method
    /**
     *  Load an ImageIcon
     *
     *@param  name  filename of the icon to load
     *@return       The loaded ImageIcon
     */
    private static ImageIcon loadImageIcon(String name) {
        URL url = ImageHandler.class.getResource("/images/" + name);

        if (url == null) {
            System.err.println("Image '" + name + "' not found.");
            return null;
        }

        ImageIcon icon = null;
        try {
            icon = new ImageIcon(url);
        } catch (NullPointerException e) {
            System.err.println("Failed to load image '" + name + "'.");
            return null;
        }

        if (icon == null)
            System.err.println("Failed to load image '" + name + "'.");

        return icon;
    } //}}}
}

