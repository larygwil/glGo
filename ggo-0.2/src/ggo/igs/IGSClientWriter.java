/*
 *  IGSClientWriter.java
 */
package ggo.igs;

import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.Date;
import java.text.DateFormat;
import ggo.gGo;
import ggo.utils.Settings;
import ggo.igs.gui.*;
import ggo.igs.chatter.*;

/**
 *  Wrapper class to organize output done to the client GUI elements
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/09/04 21:17:50 $
 */
public class IGSClientWriter {
    //{{{ private members
    private JTextArea out;
    private IGSChatter chatter;
    private IGSShouter shouter;
    private IGSChannels channels;
    //}}}

    //{{{ IGSClientWriter constructor
    /**
     *  Constructor for the IGSClientWriter object
     *
     *@param  out       Pointer to the output JTextArea
     *@param  chatter   Pointer to the IGSChatter
     *@param  shouter   Pointer to the IGSShouter
     *@param  channels  Pointer to the IGSChatter
     */
    public IGSClientWriter(JTextArea out, IGSChatter chatter, IGSShouter shouter, IGSChannels channels) {
        this.out = out;
        this.chatter = chatter;
        this.shouter = shouter;
        this.channels = channels;
    } //}}}

    //{{{ write() method
    /**
     *  Write text to the output JTextArea
     *
     *@param  str  String to write
     */
    public void write(String str) {
        out.append(str + "\n");

        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        out.scrollRectToVisible(out.modelToView(
                                out.getDocument().getLength()));
                    } catch (BadLocationException e) {
                        System.err.println("Failed to scroll: " + e);
                    }
                }
            });
    } //}}}

    //{{{ recieveChat() method
    /**
     *  Recieve a chat from server, forward it to the IGSChatter frame.
     *  This method checks for the users idle time or away setting and
     *  eventually returns an autoaway message to the sender. When
     *  autoaway is sent, a timestamp is added to the recieved text.
     *
     *@param  fromName  Player name the chat comes from
     *@param  txt       Chat text
     */
    public void recieveChat(String fromName, String txt) {
        if ((IGSConnection.isAway() ||
                (gGo.getSettings().getAutoawayTime() > 0 &&
                IGSConnection.getIdleTime() > gGo.getSettings().getAutoawayTime() * 60)) &&
                gGo.getSettings().getAutoawayMessage() != null &&
                gGo.getSettings().getAutoawayMessage().length() > 0) {
            IGSConnection.blockIdle(true);
            IGSConnection.sendCommand("tell " + fromName + " " + gGo.getSettings().getAutoawayMessage());
            IGSConnection.blockIdle(false);

            txt = "[" + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()) + "] " + txt;
        }

        chatter.recieveChat(fromName, txt);
    } //}}}

    //{{{ recieveChatError() method
    /**
     *  Recieve a chat error message from server, forward it to the IGSChatter frame
     *
     *@param  txt  Chat error message
     */
    public void recieveChatError(String txt) {
        chatter.recieveChatError(txt);
    } //}}}

    //{{{ recieveShout() method
    /**
     *  Recieve a shout from server, forward it to the IGSShouter frame
     *
     *@param  txt  Chat text
     */
    public void recieveShout(String txt) {
        shouter.append(txt);
    } //}}}

    //{{{ recieveYell() method
    /**
     *  Recieve a yell from server, forward it to the IGSChannels frame
     *
     *@param  txt  Yell text
     */
    public void recieveYell(String txt) {
        channels.recieveYell(txt);
    } //}}}

    //{{{ recieveChannelList() method
    /**
     *  Forward input from "channel" command to channel frame
     *
     *@param  txt  Recieved text
     */
    public void recieveChannelList(String txt) {
        channels.append(txt);
    } //}}}

    //{{{ sendBozoAutoReply() method
    /**
     *  Send automatic message to bozo if this feature is enabled
     *
     *@param  name  Bozo player name
     */
    public void sendBozoAutoReply(String name) {
        if (gGo.getSettings().doAutoReply()) {
            String s = gGo.getSettings().getAutoReply();
            if (s == null || s.length() == 0)
                s = gGo.getIGSBozoResources().getString("default_message");
            IGSConnection.sendCommand("tell " + name + " " + s);
        }
    } //}}}
}

