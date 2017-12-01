/*
 *  IGSInput.java
 */
package ggo.igs;

import java.io.*;
import ggo.igs.*;

/**
 *  Thread reading the IGS input stream. The read data is forwarded to IGSReader,
 *  a subclass of FilterReader.
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.1.1.1 $, $Date: 2002/07/29 03:24:24 $
 */
public class IGSInput extends Thread {
    //{{{ private members
    private IGSReader in;
    //}}}

    //{{{ IGSInput constructor
    /**
     *Constructor for the IGSInput object
     *
     *@param  in  Pointer to IGSReader, a FilterReader subclass
     */
    public IGSInput(IGSReader in) {
        this.in = in;
    } //}}}

    //{{{ run() method
    /**  Main processing method for the IGSInput object */
    public void run() {
        final int off = 0;
        final int len = 80;
        char cbuf[] = new char[len];

        try {
            while (in.read(cbuf, off, len) > 0 && !isInterrupted())
                ;
        } catch (IOException e) {
            System.err.println("Failed to read from input stream: " + e);
        }
        IGSConnection.notifyConnectionLost();
    } //}}}
}

