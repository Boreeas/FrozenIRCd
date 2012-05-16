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
package net.boreeas.frozenircd;

import net.boreeas.frozenircd.connection.ConnectionListener;
import net.boreeas.frozenircd.connection.server.ServerLink;
import net.boreeas.frozenircd.utils.SharedData;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;

/**
 * Represents the Server.
 * @author Boreeas
 */
public enum Server {
    
    /**
     * The server instance.
     */
    INSTANCE;
    
    private Set<ConnectionListener> connectionListeners;
    
    
    /**
     * Singleton constructor.
     */
    private Server() {
        
        connectionListeners = new HashSet<>();
    }
    
    public void start() {
        
        linkServers();
        startListeners();
        startPingDaemon();
    }
    
    public void startListeners() {
        
        SharedData.logger.info("Starting listeners:");
        String host = ConfigData.getFirstConfigOption(ConfigKey.HOST);
        
        for (String port: ConfigData.getConfigOption(ConfigKey.PORTS)) {
            
            boolean useSSL = false;
            
            if (port.startsWith("+")) {
                
                // +[port] indicates an SSL port
                useSSL = true;
                port = port.substring(1);
            }
            
            if (!port.matches("[0-9]+")) {
                
                SharedData.logger.error(String.format("Invalid port entry: Not an integer: %s (Skipping)", port));
                continue;
            }
            
            try {
                
                SharedData.logger.info(String.format("Binding to {0}:{1} ({2})", host, port, (useSSL) ? "ssl" : "no ssl"));
                
                ConnectionListener connListener = new ConnectionListener(host, Integer.parseInt(port), useSSL);
                connListener.start();
                
                connectionListeners.add(connListener);
            } catch (IOException ex) {
                SharedData.logger.error(String.format("Unable to listen on port %s", port), ex);
            }
        }
    }

    public void close() {
        
        SharedData.logger.info("Initiating shutdown sequence");
        
        for (ConnectionListener listener: connectionListeners) {
            
            SharedData.logger.info("No longer accepting incoming connections");
            
            listener.requestInterrupt();
        }
        
        SharedData.logger.info("Disconnecting connected clients");
        SharedData.logger.info("Delinking servers");
        SharedData.connectionPool.disconnectAll();
        
        SharedData.logger.info("Spinning down");
    }
    
    private void startPingDaemon() {
        
        new PingDaemon().start();
    }

    /**
     * Establishes a link connection to all servers specified in the config
     */
    private void linkServers() {
        
        Set<String[]> servers = ConfigData.getULines();
        
        for (String[] data: servers) {
            
            try {
                String host = data[0];
                int port = (data[1].matches("[0-9]+")) ? Integer.parseInt(data[1]) : 6667;
                String password = data[2];

                ServerLink newLink = new ServerLink(host, port, password);
                SharedData.connectionPool.addConnection(newLink.getUUID(), newLink);
            } catch (ArrayIndexOutOfBoundsException oobe) {
                
                // We did not get enough arguments to complete the connection
                SharedData.logger.warn(String.format("Incorrect link entry format for entry %s: Accepted formats are <host>:<port>:<password> or <host>::<password>", Arrays.toString(data)));
                continue;
            } catch (IOException ioe) {
                
                SharedData.logger.error(String.format("Failed to connect to establish link %s: IOException", Arrays.toString(data)), ioe);
                continue;
            }       
        }
        
        SharedData.logger.info("All servers linked");
    }
}
