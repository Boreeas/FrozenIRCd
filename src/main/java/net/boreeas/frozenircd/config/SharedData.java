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
package net.boreeas.frozenircd.config;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import net.boreeas.frozenircd.Connection;
import net.boreeas.frozenircd.ConnectionPool;
import net.boreeas.frozenircd.InputHandler;

/**
 *
 * @author Boreeas
 */
public class SharedData {
    
    public static final String PROTOCOL_VERSION = "0210";
    public static final String BUILD_IDENTIFIER = "frozen001a";
    
    public static final String CONFIG_KEY_HOST = "hostname";
    public static final String CONFIG_KEY_DESCRIPTION = "description";
    public static final String CONFIG_KEY_PASS = "pass";
    public static final String CONFIG_KEY_PORT = "port";
    
    public static final String CONFIG_KEY_LINKS = "links";
    public static final String CONFIG_KEY_TOKEN = "token";
    public static final String CONFIG_KEY_LINK_PASS = "linkpass";
    
    /**
     * The public logger object to be used by every class.
     */
    public static final Logger logger = Logger.getLogger("FrozenIRCd");
    
    /**
     * The handler that takes care of any input received from a server
     */
    public static final InputHandler serverLinkInputHandler = new InputHandler() {

        public void onInput(Connection connection, String input) {
            
            // TODO implement
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
    
    /**
     * The handler that takes care of client input
     */
    public static final InputHandler clientInputHandler = new InputHandler() {

        public void onInput(Connection connection, String input) {
            
            // TODO Implement
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
    
    /**
     * The pool of all linked servers
     */
    public static final ConnectionPool serverPool = new ConnectionPool();
    
    /**
     * The pool of all connected clients
     */
    public static final ConnectionPool clientPool = new ConnectionPool();
    
    
    /**
     * The general configuration settings
     */
    private static Config config;
    
    
    
    
    
    
    /**
     * Returns the general IRCd config. If an IOException occurs, or the file does not exists, it will return an
     * empty (or incomplete) config.
     * @return The general IRCd config
     */
    public static synchronized Config getConfig() {
        
        if (config == null) {
            
            File configFile = new File("./configs/config.conf");            
            config = new Config(configFile);
            
            try {

                if (!configFile.exists()) {

                    configFile.getParentFile().mkdir();
                    configFile.createNewFile();
                }
                
                config.load();
            } catch (IOException ex) {
                
                
            }
        }
        
        return config;
    }
}
