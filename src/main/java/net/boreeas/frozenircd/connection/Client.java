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
package net.boreeas.frozenircd.connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import net.boreeas.frozenircd.Interruptable;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.config.SharedData;

/**
 *
 * @author Boreeas
 */
public class Client extends Thread implements Interruptable, Connection {

    private Socket socket;
    private boolean ssl;
    
    private BufferedReader reader;
    private BufferedWriter writer;
    
    private Set<ClientInputHandler> handlers = new CopyOnWriteArraySet<ClientInputHandler>();
    
    private volatile boolean interrupted;
    private boolean closed;
    
    private Set<Character> flags = new HashSet<Character>();
    private String username;
    private String realname;
    private String nickname;
    private String hostname;
    private boolean identdResponse = false;
    private final UUID uuid = UUID.randomUUID();
    
    public Client(Socket socket, boolean ssl) throws IOException {
        
        SharedData.logger.log(Level.INFO, "Client from {0} attached", socket);
        
        
        this.socket = socket;
        socket.setSoTimeout(1000);
        this.ssl = ssl;
        
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
    }

    @Override
    public void run() {
        
        while (!interrupted) {
            try {
                String input = reader.readLine();
                if (input == null) {
                    
                    break;  // Connection closed
                }
                
                for (InputHandler handler: handlers) {
                    
                    handler.onInput(this, input);
                }
                
            } catch (SocketTimeoutException ex) {
                continue;   //Prevent endless blocks
            } catch (IOException ex) {
                
                if (!closed) {
                    // Ignore IOExceptions on closed connections
                    SharedData.logger.log(Level.SEVERE, String.format("IOException while reading data from %s, closing connection.", socket.getInetAddress().getHostName()), ex);
                }
                
                break;
            }
            
            try {
                sleep(50);
            } catch (InterruptedException ex) {
                requestInterrupt();
            }
        }
        
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException ioe) {
            // Not much we can do here anyways
            if (!closed) {
                // Ignore IOExceptions on closed connections
                SharedData.logger.log(Level.INFO, String.format("IOException while closing streams to %s", socket.getInetAddress().getHostName()), ioe);
            }
        }
    }
    
    public void addHandler(ClientInputHandler handler) {
        
        handlers.add(handler);
    }
    
    public void requestInterrupt() {
        
        this.interrupted = true;
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
    
    public boolean isSSLConnection() {
        
        return ssl;
    }
    
    /**
     * Disconnects this client.
     */
    @Override
    public void disconnect() {
        
        disconnect("Connection closed");
    }
    
    /**
     * Disconnects this client.
     * @param message The message for the disconnect
     */
    @Override
    public void disconnect(String message) {
        
        send(String.format("QUIT :%s", message));
        requestInterrupt();
        closed = true;
        
        for (ClientInputHandler handler: handlers) {
            handler.onDisconnect(this);
        }
        
        SharedData.serverPool.removeConnection(uuid);
    }
    
    @Override
    public String toString() {
        
        return socket.getInetAddress().toString();
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
        
        flags.add(flag);
        for (ClientInputHandler handler: handlers) {
            
            handler.onModeChange(this, flags());
        }
    }
    
    /**
     * Removes a mode flag from the client and notifies all input handlers of the mode change
     * @param flag The flag to remove
     */
    public void removeFlag(char flag) {
        
        flags.remove(flag);
        
        for (ClientInputHandler handler: handlers) {
            
            handler.onModeChange(this, flags());
        }
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
    
    public UUID getUUID() {
        
        return uuid;
    }
    
    public String getRealname() {
        return realname;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public String getMask() {
        return String.format("%s!%s@%s", nickname, username, hostname);
    }

    public void setNickname(String nickname) {
        this.nickname = SharedData.cleanString(nickname);
    }

    public void setRealname(String realname) {
        this.realname = SharedData.cleanString(realname);
    }

    public void setUsername(String username) {
        
        if (!identdResponse) {
            username = "~" + username;
        }
        
        this.username = SharedData.cleanString(username);
    }
    
    public void setHostname(String hostname) {
        this.hostname = SharedData.cleanString(hostname);
    }
    
    public void setIdentified(boolean flag) {
        this.identdResponse = flag;
    }
    
    
    
}
