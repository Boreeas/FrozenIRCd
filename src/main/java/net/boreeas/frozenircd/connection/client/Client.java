/*
 * Copyright 2012 Malte Schuetze.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.boreeas.frozenircd.connection.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.boreeas.frozenircd.Channel;
import net.boreeas.frozenircd.Flagable;
import net.boreeas.frozenircd.command.Mode;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.command.Reply;
import net.boreeas.frozenircd.utils.SharedData;
import net.boreeas.frozenircd.connection.Connection;
import net.boreeas.frozenircd.utils.StringUtils;

/**
 *
 * @author Boreeas
 */
public class Client extends Connection implements Flagable {

    private boolean ssl;
    
    private boolean identdResponse = false;
    private boolean nickGiven = false;
    private boolean userGiven = false;
    private boolean passGiven = false;
    private boolean welcomeSent = false; 
            
    private Map<Character, String> flags = new HashMap<>();
    
    private String username;
    private String realname;
    private String nickname;
    private String hostname;
    
    private Set<String> channels = new HashSet<>();
    
    
    public Client(Socket socket, boolean ssl) throws IOException {
        
        SharedData.logger.info(String.format("Client from %s attached", socket));
        
        
        this.socket = socket;
        socket.setSoTimeout(1000);
        this.ssl = ssl;
        
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
    }

    
    // Communications
    
    /**
     * Sends a message to the client
     * @param line The message to send
     */
    public void send(String line) {
        
        try {
            
            SharedData.logger.debug("[← " + this + "] " + line);
            writer.write(String.format("%s\r\n", line));
            writer.flush();
        } catch (IOException ioe) {
            
            if (closed) {
                return;
            }
            
            SharedData.logger.error(String.format("Could not write to %s, closing connection", socket.getInetAddress()), ioe);            
            disconnect(ioe.getMessage());
        }
    }
    
    /**
     * Sends the text in the standard format, that is <code>:< serverhostname > < line >\r\n</code>
     * @param The line to send
     */
    public void sendStandardFormat(String line) {
        
        send(String.format(":%s %s", ConfigData.getFirstConfigOption(ConfigKey.HOST), line));
    }
    
    /**
     * Sends a notice to the user.
     * @param senderHostmask The hostmask of the sender
     * @param receiver The receiver of the notice
     * @param message The message to send
     */
    public void sendNotice(String senderHostmask, String receiver, String message) {
        
        send(String.format(":%s NOTICE %s :%s", senderHostmask, receiver, message));
    }
    
    /**
     * Sends a private message to the user
     * @param senderHostmask The hostmask of the sender
     * @param receiver The receiver of the notice
     * @param message The message to send
     */
    public void sendPrivateMessage(String senderHostmask, String receiver, String message) {
        
        send(String.format(":%s PRIVMSG %s :%s", senderHostmask, receiver, message));
    }
    
    /**
     * Kills the connection to the user. Equivalent to
     * calling <code>disconnect("Killed: " + reason)</code>.
     * @param reason The reason why the connection was killed
     */
    public void kill(String reason) {
        
        disconnect("Killed: " + reason);
    }
    
    
    /**
     * Tells whether this is an ssl connection or not.
     * @return <code>true</code> if this connection uses ssl, <code>false</code> otherwise.
     */
    public boolean isSSLConnection() {
        
        return ssl;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public boolean userGiven() {
        
        return userGiven;
    }
    
    public boolean nickGiven() {
        
        return nickGiven;
    }
    
    public boolean receivedIdentResponse() {
    
        return identdResponse;
    }
    
    public boolean registrationCompleted() {
        
        return nickGiven && userGiven 
                && ((ConfigData.getFirstConfigOption(ConfigKey.USING_PASS).equalsIgnoreCase("true")) ? passGiven : true);
    }
    
    public boolean rplWelcomeSent() {
        
        return welcomeSent;
    }
    
    
    
    public String getRealname() {
        return realname;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    /**
     * Returns the nickname for this client, or <code>*</code> if no nickname
     * has been given.
     * @return The nickname, or *
     */
    public String getSafeNickname() {
        
        return (nickname == null) ? "*" : nickname;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getHostmask() {
        return nickname + "!" + username + "@" + hostname;
    }
    
    public String getDisplayHostmask() {
       
        String hostmask = nickname + "!";
        if (!receivedIdentResponse()) hostmask += '~';
        
        return hostmask + username + "@" + hostname;
    }
    
    /**
     * Sets the nickname of this user to <code>nickname</code>.
     * @param nickname The new nickname
     */
    public void setNickname(String nickname) {
        
        this.nickname = SharedData.cleanString(nickname);
        nickGiven = true;
    }

    /**
     * Sets the real name of this user to <code>realname</code>.
     * @param realname The real name
     */
    public void setRealname(String realname) {
        this.realname = SharedData.cleanString(realname);
    }

    /**
     * Sets the username of this user to <code>username</code>.
     * @param username The username
     */
    public void setUsername(String username) {
                
        this.username = SharedData.cleanString(username);
        userGiven = true;
    }
    
    /**
     * Sets the host name of this user to <code>hostname</code>.
     * @param hostname The host name
     */
    public void setHostname(String hostname) {
        this.hostname = SharedData.cleanString(hostname);
    }
    
    /**
     * Toggles the flag that marks if this user replied to the ident request.
     * @param flag Marks if the user replied or not.
     */
    public void setIdentResponseReceived(boolean flag) {
        this.identdResponse = flag;
    }
    
    public void setPassGiven(boolean flag) {
        this.passGiven = true;
    }
    
    @Override
    public String getCommonName() {
        return getSafeNickname();
    }
    
    
    
    // Event hooks
    @Override
    public void onInput(String input) {
        
        SharedData.logger.debug(String.format("[→ " + this + "] " + input));
        
        
        String[] fields = input.split(" ", 2);
        String[] args;
        
        if (fields.length >= 2 && fields[1].trim().startsWith(":")) {
            args = new String[] { fields[1].trim().substring(1) };
        } else {

            // For "x y :z" yields ["x y ", "z"]. For "" yields []. For "x y" yields ["x y"]
            String[] toLastArg = ( fields.length > 1 ) 
                                        ? fields[1].split(":") 
                                        : new String[0];

            // For "x y :z" yields ["x", "y"]. For "" yields []. For "x y" yields ["x", "y"]
            String[] otherArgs = ( toLastArg.length > 0 ) 
                                        ? toLastArg[0].trim().split(" ") 
                                        : new String[0];

            args = new String[otherArgs.length + (( toLastArg.length > 1 ) ? 1 : 0 )];

            System.arraycopy(otherArgs, 0, args, 0, otherArgs.length);
            if (toLastArg.length > 1) {
                args[args.length - 1] = toLastArg[1];
            }
        }
    
        SharedData.onClientCommand(this, fields[0], args);
    }

    @Override
    public void onDisconnect() {
        
        SharedData.logger.trace(String.format("Client %s disconnected", this));
    }

    public void onModeChange() {
        
        sendStandardFormat(Reply.RPL_UMODEIS.format(getSafeNickname(), flags()));
    }
    
    public void onRegistrationComplete() {
        
        welcomeSent = true;
        
        addFlag(Mode.UMODE_INVISIBLE, null);
        
        sendStandardFormat(Reply.RPL_WELCOME.format(nickname, getHostmask()));
        sendStandardFormat(Reply.RPL_YOURHOST.format(nickname));
        sendStandardFormat(Reply.RPL_CREATED.format(nickname));
        sendStandardFormat(Reply.RPL_MYINFO.format(nickname));
        sendStandardFormat(Reply.RPL_ISUPPORT.format(nickname));
        
        sendStandardFormat(Reply.RPL_MOTDSTART.format(nickname));
        
        if (SharedData.motd != null) {
            
            for (String part: SharedData.motd.split("\n")) {
                sendStandardFormat(Reply.RPL_MOTD.format(nickname, part));
            }
        }
        
        sendStandardFormat(Reply.RPL_ENDOFMOTD.format(nickname));
    }
    
    
    
    
    // Channel operations
    
    /**
     * Adds a channel to this user's channel list. Also call 
     * the joinChannel method of the Channel class to make
     * this change visible.
     * @param channel The name of the channel to add
     */
    public void addChannel(String channel) {
        
        channels.add(channel);
    }
    
    /**
     * Removes a channel from this user's channel list. Also
     * call the partChannel method of the Channel class to
     * make this change visible.
     * @param channel The name of the channel to remove
     */
    public void removeChannel(String channel) {
        
        channels.remove(SharedData.toLowerCase(channel));
    }
    
    /**
     * Tells whether a client is currently in a channel.
     * @param channel The name of the channel to check
     */
    public boolean isInChannel(String channel) {
        
        return channels.contains(SharedData.toLowerCase(channel));
    }
    
    /**
     * Broadcasts a message to all channels this user is in.
     * @param message The message to send.
     */
    public void broadcastToChannels(String message) {
        
        for (String channel: channels) {
            
            Channel chan = SharedData.getChannel(channel);
            chan.sendFromClient(this, message);
        }
    }
    
    
    // Flag interface
    
    
    public boolean hasFlag(char flag) {
        
        return flags.containsKey(flag);
    }
    
    
    public void addFlag(char flag, String mode) {
                
        flags.put(flag, mode);
        onModeChange();
    }
    
    
    public void removeFlag(char flag) {

        flags.remove(flag);
        onModeChange();
    }
    
    
    public String flags() {
        
        return StringUtils.joinIterable(flags.keySet(), "");
    }
    
    public String flagParams() {
        
        StringBuilder builder = new StringBuilder();
        
        for (String param: flags.values()) {
            if (param != null) {
                builder.append(" ").append(param);
            }
        }
        
        return builder.toString();
    }
    
    public String getParam(char flag) {
        
        return flags.get(flag);
    }
}
