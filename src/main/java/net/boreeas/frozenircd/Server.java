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

import java.util.Set;

/**
 * Represents the Server.
 * @author Boreeas
 */
public final class Server {
    
    /**
     * The set of all servers linked to this server.
     */
    private Set<Server> linkedServers;
    
    /**
     * The set of all clients currently attached to this server.
     */
    private Set<Client> attachedClients;
    
    /**
     * The server instance.
     */
    private static Server instance;
    
    /**
     * Singleton constructor.
     */
    private Server() {
        
        // Singleton
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
}
