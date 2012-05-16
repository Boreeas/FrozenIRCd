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
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.UUID;
import net.boreeas.frozenircd.Interruptable;
import net.boreeas.frozenircd.config.Reply;
import net.boreeas.frozenircd.utils.SharedData;

/**
 *
 * @author Boreeas
 */
public abstract class Connection extends Thread implements Interruptable {
    
    protected boolean closed = false;
    private boolean interrupted = false;
    
    protected Socket socket;
    protected BufferedReader reader;
    protected BufferedWriter writer;
    
    protected UUID uuid = UUID.randomUUID();
    
    private String connectPassword;
    
    private long lastPingReplyTime = System.currentTimeMillis();
    private String lastPingText = null;
    
    public Connection() {
        
        super("Connection[]");
    }
    
    @Override
    public void run() {

        while (!interrupted) {
            try {
                
                String input = reader.readLine();
                if (input == null) {

                    break;  // Connection closed
                }
                
                onInput(input);

            }  catch (SocketTimeoutException ex) {
                continue;   //Prevent endless blocks
            } catch (IOException ex) {

                if (!closed) {
                    // Ignore IOExceptions on closed connections
                    SharedData.logger.error(String.format("IOException while reading data from %s, closing connection.", socket.getInetAddress().getHostName()), ex);
                }

                break;
            }

            try {
                sleep(50);
            }
            catch (InterruptedException ex) {
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
                SharedData.logger.info(String.format("IOException while closing streams to %s", socket.getInetAddress().getHostName()), ioe);
            }
        }
        
        SharedData.connectionPool.removeConnection(getUUID());
    }
    
    /**
     * Requests that this connection may be terminated. Once this method has been
     * called, it can not be undone. <br />
     * The time until the connection actually terminates is indeterminate.
     */
    public void requestInterrupt() {
        
        this.interrupted = true;
    }
    
    /**
     * Disconnects this connection.
     */
    public void disconnect() {
        
        disconnect("Connection closed");
    }
    
    /**
     * Disconnects this connection.
     * @param message The message for the disconnect
     */
    public void disconnect(String message) {
        
        SharedData.logger.info(String.format("Closing connection to {0} ({1})", this, message));
        
        closed = true;
        requestInterrupt();
        
        send(String.format("ERROR :Closing link: %s", message));
    }
    
    /**
     * Sets the password used for the connection.
     * @param connectPassword the password for the connection
     */
    public void setConnectPassword(String connectPassword) {
        
        this.connectPassword = connectPassword;
    }
    
    /**
     * Returns the password used for the connection.
     * @return the password for the connection, or <code>null</code> if no password was used
     */
    public String passGiven() {
        
        return connectPassword;
    } 
    
    /**
     * Returns the unique identifier for this connection
     * @return the unique identifier for this connection
     */
    public UUID getUUID() {
        
        return uuid;
    }
    
    public String getPingRequestText() {
        
        return lastPingText;
    }
    
    public long getLastPingTime() {
        
        return lastPingReplyTime;
    }
    
    public void updatePing(String key) {
        
        if (key.equals(lastPingText)) {
            lastPingReplyTime = System.currentTimeMillis();
        }
    }
    
    @Override
    public String toString() {
        
        return socket.getInetAddress().toString();
    }
    
    public void sendPingRequest(String request) {
        
        send(Reply.OTHER_PING.format(request));
        this.lastPingText = request;
    }
    
    /**
     * This method should be called when the server receives input from the connection
     * @param input The input receives
     */
    public abstract void onInput(String input);
    
    /**
     * This method should be called when the connection terminates.
     */
    public abstract void onDisconnect();
    
    /**
     * Sends a line to the receiver
     * @param line The message to send
     */
    public abstract void send(String line);   
    
    /**
     * Returns the common name for this connection. For servers, this should be
     * the hostname. For clients and services, this should be the nickname.<br />
     * This method must never return null.
     * @return The common name for this connection
     */
    public abstract String getCommonName();
}
