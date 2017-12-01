/*
 *  HostConfig.java
 */
package ggo.igs;

import java.io.UnsupportedEncodingException;
import ggo.gGo;

/**
 *  Configuration for the IGS host
 *
 *@author     Peter Strempel
 *@version    $Revision: 1.2 $, $Date: 2002/08/13 09:05:38 $
 */
public class HostConfig {
    /**  Default host */
    public final static String DEFAULT_HOST = "igs.joyjoy.net";
    /**  Default port */
    public final static int DEFAULT_PORT = 7777;
    /**  Default account name */
    public final static String DEFAULT_NAME = "guest";

    //{{{ private members
    private String id, host, name, password, encoding;
    private int port;
    //}}}

    //{{{ HostConfig constructors
    /**
     *Constructor for the HostConfig object
     *
     *@param  host      Host name
     *@param  port      Port number
     *@param  name      Account name
     *@param  password  Account password
     */
    public HostConfig(String host, int port, String name, String password) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.password = password;
        encoding = null;
        id = "Default";
    }

    /**
     *Constructor for the HostConfig object
     *
     *@param  id        Description of the Parameter
     *@param  host      Description of the Parameter
     *@param  port      Description of the Parameter
     *@param  name      Description of the Parameter
     *@param  password  Description of the Parameter
     */
    public HostConfig(String id, String host, int port, String name, String password) {
        this(host, port, name, password);
        this.id = id;
    } //}}}

    //{{{ getHost() method
    /**
     *  Gets the host attribute of the HostConfig object
     *
     *@return    The host value
     */
    public String getHost() {
        return host;
    } //}}}

    //{{{ setHost() method
    /**
     *  Sets the host attribute of the HostConfig object
     *
     *@param  host  The new host value
     */
    public void setHost(String host) {
        this.host = host;
    } //}}}

    //{{{ getPort() method
    /**
     *  Gets the port attribute of the HostConfig object
     *
     *@return    The port value
     */
    public int getPort() {
        return port;
    } //}}}

    //{{{ setPort() method
    /**
     *  Sets the port attribute of the HostConfig object
     *
     *@param  port  The new port value
     */
    public void setPort(int port) {
        this.port = port;
    } //}}}

    //{{{ getName() method
    /**
     *  Gets the name attribute of the HostConfig object
     *
     *@return    The name value
     */
    public String getName() {
        return name;
    } //}}}

    //{{{ setName() method
    /**
     *  Sets the name attribute of the HostConfig object
     *
     *@param  name  The new name value
     */
    public void setName(String name) {
        this.name = name;
    } //}}}

    //{{{ getPassword() method
    /**
     *  Gets the password attribute of the HostConfig object
     *
     *@return    The password value
     */
    public String getPassword() {
        return password;
    } //}}}

    //{{{ setPassword() method
    /**
     *  Sets the password attribute of the HostConfig object
     *
     *@param  password  The new password value
     */
    public void setPassword(String password) {
        this.password = password;
    } //}}}

    //{{{ getEncoding() method
    /**
     *  Gets the encoding attribute of the HostConfig object
     *
     *@return    The encoding value
     */
    public String getEncoding() {
        return encoding;
    } //}}}

    //{{{ setEncoding() method
    /**
     *  Sets the encoding attribute of the HostConfig object. Validate encoding.
     *  If invalid or empty, use system encoding.
     *
     *@param  en  The new encoding value
     *@return     True if encoding was valid, else false
     */
    public boolean setEncoding(String en) {
        // Empty string, use system default
        if (en == null || en.length() == 0 ||
                en.equals(gGo.getIGSResources().getString("system_default_encoding"))) {
            encoding = null;
            return true;
        }

        // Check for valid encoding. If invalid, use system default
        // --- 1.3 ---
        try {
            en.getBytes(en);
        } catch (UnsupportedEncodingException e) {
            encoding = null;
            return false;
        }

        encoding = en;
        return true;
    } //}}}

    /**
     *  Gets the configuration id attribute of the HostConfig object
     *
     *@return    The ID value
     */
    public String getID() {
        return id;
    }

    //{{{ toString() method
    /**
     *  Convert data of this class to a String. For debugging
     *
     *@return    Converted String
     */
    public String toString() {
        return "ID = " + id +
                "\nHost:      " + host + ":" + port +
                "\nName:      " + name +
                "\nPassword:  " + password +
                "\nEncoding:  " + encoding;
    } //}}}
}

