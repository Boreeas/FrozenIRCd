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

import java.util.logging.Logger;
import net.boreeas.frozenircd.config.Config;
import net.boreeas.frozenircd.config.SharedData;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

/**
 * Represents the Server.
 * @author Boreeas
 */
public final class Server {
    
    
    
    /**
     * The set of all servers linked to this server.
     */
    // TODO Move this to a pool maybe
    //private Set<ServerLink> linkedServers = new CopyOnWriteArraySet<ServerLink>();
    
    private Set<ConnectionListener> connectionListeners;
    
    /**
     * The server instance.
     */
    private static Server instance;
    
    
    /**
     * Singleton constructor.
     */
    private Server() {
        
        Config config = SharedData.getConfig();
        checkConfig(config);
        
        int port = Integer.parseInt(config.get("port")[0]);
        SharedData.logger.log(Level.INFO, "Binding to port {0}", port);
        
        connectionListeners = new HashSet<ConnectionListener>();
        
        linkServers();
        startListeners();
    }
    
    private void checkConfig(Config config) {
        
        if (config.get(SharedData.CONFIG_KEY_PORT) == null) 
            throw new IncompleteConfigurationException("Missing port");
        
        if (config.get(SharedData.CONFIG_KEY_HOST) == null)
            throw new IncompleteConfigurationException("Missing hostname");
            
        if (config.get(SharedData.CONFIG_KEY_TOKEN) == null) 
            throw new IncompleteConfigurationException("Missing token");
            
        if (config.get(SharedData.CONFIG_KEY_DESCRIPTION) == null)
            throw new IncompleteConfigurationException("Missing description");
        
    }
    
    public void startListeners() {
        
        for (String port: SharedData.getConfig().get(SharedData.CONFIG_KEY_PORT)) {
            
            boolean useSSL = false;
            if (port.startsWith("+")) {
                // +[port] indicates an SSL port
                useSSL = true;
                port = port.substring(1);
            }
            
            if (!port.matches("[0-9]+")) {
                SharedData.logger.log(Level.SEVERE, "Invalid port entry: Not an integer: {0} (Skipping)", port);
                continue;
            }
            
            try {
                ConnectionListener connListener = new ConnectionListener(Integer.parseInt(port), useSSL);
                connListener.start();
                connectionListeners.add(connListener);
            } catch (IOException ex) {
                SharedData.logger.log(Level.SEVERE, String.format("Unable to listen on port %s", port), ex);
            }
        }
    }

    public void close() {
        
        SharedData.logger.log(Level.INFO, "Initiating shutdown sequence");
        
        for (ConnectionListener listener: connectionListeners) {
            
            SharedData.logger.log(Level.INFO, "No longer accepting incoming connections");
            
            listener.requestInterrupt();
        }
        
        SharedData.logger.log(Level.INFO, "Disconnecting connected clients");
        SharedData.clientPool.disconnectAll();
        
        SharedData.logger.log(Level.INFO, "Delinking servers");
        SharedData.serverPool.disconnectAll();
        
        SharedData.logger.log(Level.INFO, "Spinning down");
    }
    
    /**
     * Returns the running server instance. Will create a new server if none exists.
     * @return The running server instance.
     */
    public static synchronized Server getServer() {
        
        if (instance == null) {
            
            instance = new Server();
        }
        
        return instance;
    }
    
    

    /**
     * Establishes a link connection to all servers specified in the config
     */
    private void linkServers() {
        
        String[] servers = SharedData.getConfig().get(SharedData.CONFIG_KEY_LINKS);
        
        if (servers == null) {
            
            return; //No servers specified
        }
        
        for (String link: servers) {
            
            String[] data = link.split(":");
            
            try {
                String host = data[0];
                int port = (data[2].matches("[0-9]+")) ? Integer.parseInt(data[1]) : 6667;
                String password = (data.length >= 3) ? data[2] : data[1];

                ServerLink newLink = new ServerLink(host, port, password);
                newLink.addHandler(SharedData.serverLinkInputHandler);
                SharedData.serverPool.addConnection(newLink.getUUID(), newLink);
            } catch (ArrayIndexOutOfBoundsException oobe) {
                
                // We did not get enough arguments to complete the connection
                SharedData.logger.warning(String.format("Incorrect link entry format for entry %s: Accepted formats are <host>:<port>:<password> or <host>::<password>", link));
                continue;
            } catch (IOException ioe) {
                
                SharedData.logger.log(Level.SEVERE, String.format("Failed to connect to establish link %s: IOException", link), ioe);
                continue;
            }       
        }
        
        SharedData.logger.info("All servers linked");
    }
}
