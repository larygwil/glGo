/*
 *  Writer.java
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
package ggo.parser;

import ggo.*;
import java.io.*;
import java.text.MessageFormat;
import javax.swing.JOptionPane;

/**
 *  Abstract superclass for Writer classes. Saving XML is not yet supported.
 *  is supported.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.4 $, $Date: 2002/10/17 19:03:34 $
 */
public abstract class Writer implements Defines {
    /**  BoardHandler object this Writer instance is attached to */
    protected BoardHandler boardHandler;

    //{{{ saveFile(String, BoardHandler) method
    /**
     *  Entry method, convert a game into a format like SGF, XML, UGF, and save it to a file
     *
     *@param  fileName      Filename of the file to save
     *@param  boardHandler  BoardHandler object this Writer instance is attached to
     *@return               True if successful, else false
     */
    public boolean saveFile(String fileName, BoardHandler boardHandler) {
        this.boardHandler = boardHandler;
        try {
            return doSaveFile(fileName, doWrite(), boardHandler.getGameData().charset);
        } catch (NullPointerException e) {
            System.err.println("Writer.saveFile() - Failed to save file: " + e);
            e.printStackTrace();
            return false;
        }
    } //}}}

    //{{{ saveFile(File, BoardHandler) method
    /**
     *  Entry method, convert a game into a format like SGF, XML, UGF, and save it to a file
     *
     *@param  file          File to save
     *@param  boardHandler  BoardHandler object this Writer instance is attached to
     *@return               True if successful, else false
     */
    public boolean saveFile(File file, BoardHandler boardHandler) {
        this.boardHandler = boardHandler;
        try {
            return doSaveFile(file, doWrite(), boardHandler.getGameData().charset);
        } catch (NullPointerException e) {
            System.err.println("Writer.saveFile() - Failed to save file: " + e);
            e.printStackTrace();
            return false;
        }
    } //}}}

    //{{{ doSaveFile(File, String, String) method
    /**
     *  Save a given string to a file.
     *
     *@param  file     File to save to
     *@param  txt      String to be written to the file
     *@param  charset  Charset to use for writing
     *@return          True if successful, else false
     */
    boolean doSaveFile(File file, String txt, String charset) {
        try {
            BufferedWriter f = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
            // --- 1.3 ---
            // Charset.forName(charset)));
                    charset));
            f.write(txt);
            f.close();
        } catch (IOException e) {
            System.err.println("Error saving file '" + file.getName() + "': " + e);
            displayError(
                    MessageFormat.format(gGo.getSGFResources().getString("saving_file_error"), new Object[]{file.getName()}));
            return false;
            // --- 1.3 ---
            /*
             *  } catch (UnsupportedEncodingException e) {
             *  System.err.println("Charset " + charset + " not supported: " + e);
             *  return false;
             */
        } catch (SecurityException e) {
            System.err.println("Error saving file '" + file.getName() + "': " + e);
            displayError(
                    MessageFormat.format(gGo.getSGFResources().getString("saving_file_error"), new Object[]{file.getName()}));
            return false;
        }
        return true;
    } //}}}

    //{{{ doSaveFile(String, String, String)
    /**
     *  Save a given string to a file.
     *
     *@param  fileName  Filename to save to
     *@param  txt       String to be written to the file
     *@param  charset   Charset to use for writing
     *@return           True if successful, else false
     */
    boolean doSaveFile(String fileName, String txt, String charset) {
        File f;
        try {
            f = new File(fileName);
            if (!f.createNewFile() && !f.canWrite()) {
                displayError(
                        MessageFormat.format(gGo.getSGFResources().getString("cannot_write_error"), new Object[]{fileName}));
                return false;
            }
        } catch (IOException e) {
            System.err.println("Error saving file '" + fileName + "': " + e);
            displayError(
                    MessageFormat.format(gGo.getSGFResources().getString("cannot_write_error"), new Object[]{fileName}));
            return false;
        } catch (SecurityException e) {
            System.err.println("Error saving file '" + fileName + "': " + e);
            displayError(
                    MessageFormat.format(gGo.getSGFResources().getString("cannot_write_error"), new Object[]{fileName}));
            return false;
        }
        return doSaveFile(f, txt, charset);
    } //}}}

    //{{{ displayError() method
    /**
     *  Display a messagebox with an error text
     *
     *@param  txt  Error text
     */
    private void displayError(String txt) {
        JOptionPane.showMessageDialog(
                boardHandler.getBoard().getMainFrame(),
                txt,
                MessageFormat.format(
                gGo.getSGFResources().getString("file_save_error_title"),
                new Object[]{gGo.getSGFResources().getString("SGF")}),
                JOptionPane.ERROR_MESSAGE);
    } //}}}

    //{{{ doWrite() method
    /**
     *  Subclasses have to implement this method, here the actual conversion of the game
     *  into a format like SGF, XML or UGF has to take place.
     *
     *@return    String containing the game in SGF etc. format
     */
    abstract String doWrite(); //}}}
}

