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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Boreeas
 */
public class ConnectionPool {
    
    private Map<String, Connection> pool = new HashMap<String, Connection>();
    
    public ConnectionPool() {
        
        
    }
    
    /**
     * Adds a connection to the pool.
     * @param identifier The unique identifier for the connection
     * @param connection The connection to add
     */
    public void addConnection(String identifier, Connection connection) {
        
        pool.put(identifier, connection);
    }
    
    /**
     * Removes the connection for the given identifier.
     * @param identifier The unique identifier for that connection
     * @return The removed connection, or <code>null</code> if none was removed
     */
    public Connection removeConnection(String identifier) {
        
        return pool.remove(identifier);
    }
    
    /**
     * Returns the connection for the given identifier
     * @param identifier The unique identifier for the connection
     * @return The associated connection
     */
    public Connection getConnection(String identifier) {
        
        return pool.get(identifier);
    }
    
    /**
     * Broadcasts a message to every attached connection except <code>source</code>
     * @param message The message to send
     * @param source The connection from which the message was received. If this is null, the message will be broadcasted to every connection
     */
    public void broadcast(String message, Connection source) {
        
        for (Entry<String, Connection> entry: pool.entrySet()) {
            
            //Do not send a message to the original target
            if (!entry.getValue().equals(source)) {
                
                entry.getValue().send(message);
            }
        }
    }
}
