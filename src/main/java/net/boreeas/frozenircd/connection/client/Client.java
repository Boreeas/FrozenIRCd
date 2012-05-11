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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.config.Reply;
import net.boreeas.frozenircd.config.SharedData;
import net.boreeas.frozenircd.connection.Connection;
import net.boreeas.frozenircd.connection.service.Service;
import net.boreeas.frozenircd.connection.service.ServiceCommandHandler;

/**
 *
 * @author Boreeas
 */
public class Client extends Connection {

    private boolean ssl;
    
    private boolean identdResponse = false;
    private boolean nickGiven = false;
    private boolean userGiven = false;
    private boolean passGiven = false;
    
    private Set<Character> flags = new HashSet<Character>();
    
    private String username;
    private String realname;
    private String nickname;
    private String hostname;
    
    
    public Client(Socket socket, boolean ssl) throws IOException {
        
        SharedData.logger.log(Level.INFO, "Client from {0} attached", socket);
        
        
        this.socket = socket;
        socket.setSoTimeout(1000);
        this.ssl = ssl;
        
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
    }

    public void send(String line) {
        
        try {
            
            SharedData.logger.log(Level.parse("0"), "[-> {0}] {1}", new Object[]{socket.getInetAddress(), line});
            
            writer.write(String.format(":%s %s\r\n", ConfigData.getFirstConfigOption(ConfigKey.HOST), line));
            writer.flush();
        } catch (IOException ioe) {
            
            SharedData.logger.log(Level.SEVERE, String.format("Could not write to %s, closing connection", socket.getInetAddress()), ioe);
            disconnect(ioe.getMessage());
        }
    }
    
    public void sendWithoutPrefix(String line) {
        
        try {
            
            SharedData.logger.log(Level.parse("0"), "[-> {0}] :{1}", new Object[]{socket.getInetAddress(), line});
            writer.write(String.format(":%s\r\n", line));
            writer.flush();
        } catch (IOException ioe) {
            
            SharedData.logger.log(Level.SEVERE, String.format("Could not write to %s, closing connection", socket.getInetAddress()), ioe);
            disconnect(ioe.getMessage());
        }
    }
    
    public void sendNotice(String sender, String message) {
        
        try {
            
            String toSend = String.format(":%s NOTICE %s :%s", ConfigData.getFirstConfigOption(ConfigKey.HOST), sender, message);
            SharedData.logger.log(Level.parse("0"), "[-> {0}] {1}", new Object[]{socket.getInetAddress(), toSend});
            writer.write(String.format("%s\r\n", toSend));
            writer.flush();
        } catch (IOException ioe) {
            
            SharedData.logger.log(Level.SEVERE, String.format("Could not write to %s, closing connection", socket.getInetAddress()), ioe);
            disconnect(ioe.getMessage());
        }
    }
    
    public void sendPrivateMessage(String sender, String message) {
        
        try {
            
            String toSend = String.format(":%s PRIVMSG %s :%s", ConfigData.getFirstConfigOption(ConfigKey.HOST), sender, message);
            SharedData.logger.log(Level.parse("0"), "[-> {0}] {1}", new Object[]{socket.getInetAddress(), toSend});
            writer.write(String.format("%s\r\n", toSend));
            writer.flush();
        } catch (IOException ioe) {
            
            SharedData.logger.log(Level.SEVERE, String.format("Could not write to %s, closing connection", socket.getInetAddress()), ioe);
            disconnect(ioe.getMessage());
        }
    }
    
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

    
    /**
     * Tells whether a client has a certain flag
     * @param flag The flag to check
     * @return <code>true</code> if the client has the flag <code>flag</code>, false otherwise
     */
    public boolean hasFlag(char flag) {
        
        return flags.contains(flag);
    }
    
    /**
     * Adds a mode flag for the client and notifies all input handlers of the mode change
     * @param flag The flag to add
     */
    public void addFlag(char flag) {
        
        if (!SharedData.settableUmodes.contains(flag)) {
            send(Reply.ERR_UMODEUNKNOWNFLAG.format(getSafeNickname(), flag));
            return;
        }
        
        flags.add(flag);
        
        onModeChange();
    }
    
    /**
     * Adds all chars in the string to the client
     * @param flagString 
     */
    public void addFlags(String flagString) {
        
        for (char flag: flagString.toCharArray()) {
            
            if (!SharedData.settableUmodes.contains(flag)) {
                
                send(Reply.ERR_UMODEUNKNOWNFLAG.format(getSafeNickname(), flag));
            } else {
            
                flags.add(flag);
            }
        }
        
        onModeChange();
    }
    
    /**
     * Removes a mode flag from the client and notifies all input handlers of the mode change
     * @param flag The flag to remove
     */
    public void removeFlag(char flag) {

        if (flag == 'r') {
            
            send(Reply.ERR_UMODEUNKNOWNFLAG.format(getSafeNickname(), "Cannot set umode -r"));
            return;
        }
        
        flags.remove(flag);
        
        onModeChange();
    }
    
    public void removeFlags(String flagString) {
        
        for (char flag: flagString.toCharArray()) {
            
            if (flag == 'r') {
                
                send(Reply.ERR_UMODEUNKNOWNFLAG.format(getSafeNickname(), "Cannot set umode -r"));
            } else {
            
                flags.remove(flag);
            }
        }
        
        onModeChange();
    }
    
    /**
     * Returns the mode string for the client
     * @return The mode string for the client
     */
    public String flags() {
        
        StringBuilder builder = new StringBuilder();
        
        for (Character character: flags) {
            
            builder.append(character);
        }
        
        return builder.toString();
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
    
    public String getMask() {
        return String.format("%s!%s@%s", nickname, username, hostname);
    }

    public void setNickname(String nickname) {
        this.nickname = SharedData.cleanString(nickname);
        nickGiven = true;
    }

    public void setRealname(String realname) {
        this.realname = SharedData.cleanString(realname);
    }

    public void setUsername(String username) {
        
        if (!identdResponse) {
            username = "~" + username;
        }
        
        this.username = SharedData.cleanString(username);
        userGiven = true;
    }
    
    public void setHostname(String hostname) {
        this.hostname = SharedData.cleanString(hostname);
    }
    
    public void setIdentified(boolean flag) {
        this.identdResponse = flag;
    }
    
    public void setPassGiven(boolean flag) {
        this.passGiven = true;
    }

    @Override
    public void onInput(String input) {
        
        SharedData.logger.log(Level.ALL, "[{0} ->] {1}", new Object[]{this, input});
        
        
        String[] fields = input.split(" ", 2);
        
        // For "x y :z" contains ["x y ", "z"]. For "" contains []. For "x y" contains ["x y"]
        String[] toLastArg = ( fields.length > 1 ) 
                                    ? fields[1].split(":") 
                                    : new String[0];

        // For "x y :z" contains ["x", "y"]. For "" contains []. For "x y" contains ["x", "y"]
        String[] otherArgs = ( toLastArg.length > 0 ) 
                                    ? toLastArg[0].trim().split(" ") 
                                    : new String[0];

        String[] argsTotal = new String[otherArgs.length + (( toLastArg.length > 1 ) ? 1 : 0 )];

        System.arraycopy(otherArgs, 0, argsTotal, 0, otherArgs.length);
        if (toLastArg.length > 1) {
            argsTotal[argsTotal.length - 1] = toLastArg[1];
        }
        
        SharedData.onClientCommand(this, fields[0], argsTotal);
    }

    @Override
    public void onDisconnect() {
        
        SharedData.logger.log(Level.ALL, "Client {0} disconnected", this);
    }

    @Override
    public String getCommonName() {
        return getSafeNickname();
    }
    
    public void onModeChange() {
        
        send(Reply.RPL_UMODEIS.format(getSafeNickname(), getSafeNickname(), flags()));
    }
    
    public Service toService(final ServiceCommandHandler handler, String newNick, String visibility, String type) {
        
        Service asService = new Service(socket, writer, reader, newNick, visibility, type, uuid);
        
        return asService;
    }
}
