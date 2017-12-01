/*
 *  UpdateChecker.java
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
import java.net.*;
import java.io.*;
import java.text.MessageFormat;
import ggo.Defines;
import ggo.gGo;

/**
 *  Simple class to check the latest gGo version by reading a text file from an URL
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.3 $, $Date: 2002/09/21 12:39:56 $
 */
public class UpdateChecker extends Thread {
    //{{{ private members
    private final static String url = "http://ggo.sourceforge.net/version.html";
    private JFrame parentFrame = null;
    //}}}

    //{{{ UpdateChecker() constructor
    /**
     *Constructor for the UpdateChecker object
     *
     *@param  parent  Parent frame, used for the messagebox
     */
    public UpdateChecker(JFrame parent) {
        parentFrame = parent;
    } //}}}

    //{{{ run() method
    /**  Try to connect to server, read version file, and display a messagebox */
    public void run() {
        String version = getVersion();
        String msg = gGo.getgGoResources().getString("failed_server");

        if (version.length() > 0) {
            if (version.equals(Defines.VERSION))
                msg = MessageFormat.format(
                        gGo.getgGoResources().getString("latest_version_message_1"),
                        new Object[]{version});
            else
                msg = MessageFormat.format(
                        gGo.getgGoResources().getString("latest_version_message_2"),
                        new Object[]{version});
        }

        JOptionPane.showMessageDialog(parentFrame,
                msg,
                gGo.getgGoResources().getString("Latest_version"),
                JOptionPane.INFORMATION_MESSAGE);
    } //}}}

    //{{{ getVersion() method
    /**
     *  Connect to server and recieve version.txt file.
     *
     *@return    Version string, for example: 0.0.6
     */
    private String getVersion() {
        System.err.println("Trying to read: " + url);

        String versionTxt = "";
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(new URL(url).openStream()));
            versionTxt = in.readLine();
            in.close();
        } catch (MalformedURLException e) {
            System.err.println("Failed to get update: " + e);
        } catch (IOException e) {
            System.err.println("Failed to get update: " + e);
        }

        return versionTxt;
    } //}}}
}

