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
    private Set<ServerLink> linkedServers = new CopyOnWriteArraySet<ServerLink>();
    
    /**
     * The set of all clients currently attached to this server.
     */
    // TODO Move this to a pool maybe
    private Set<Client> attachedClients = new HashSet<Client>();
    
    private ServerSocket serverSocket;
    
    private volatile boolean interrupted = false;
    
    /**
     * The server instance.
     */
    private static Server instance;
    
    
    /**
     * Singleton constructor.
     */
    private Server() {
        
        Config config = SharedData.getConfig();
        
        if (config.get(SharedData.CONFIG_KEY_PORT) == null) {
            
            throw new IncompleteConfigurationException("Missing port");
        }
        
        int port = Integer.parseInt(config.get("port")[0]);
        SharedData.logger.log(Level.INFO, "Binding to port {0}", port);
        
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            SharedData.logger.log(Level.SEVERE, "Unable to create ServerSocket, unable to accept incoming connections", ioe);
        }
        
        
        if (config.get(SharedData.CONFIG_KEY_HOST) == null 
                || config.get(SharedData.CONFIG_KEY_TOKEN) == null 
                || config.get(SharedData.CONFIG_KEY_DESCRIPTION) == null) {
            
            throw new IncompleteConfigurationException("Missing hostname, token or description");
        }
        
        linkServers();
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
                (new ConnectionListener(Integer.parseInt(port), useSSL)).start();
            } catch (IOException ex) {
                SharedData.logger.log(Level.SEVERE, String.format("Unable to listen on port %s", port), ex);
            }
        }
    }

    public void close() {
        
        if (serverSocket != null) {
            
            try {
                serverSocket.close();
            } catch (IOException ioe) {
                SharedData.logger.log(Level.WARNING, "Unable to close server socket", ioe);
            }
        }
        
        for (ServerLink link: linkedServers) {
            
            link.requestInterrupt();
        }
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
                SharedData.serverPool.addConnection(host, newLink);
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
