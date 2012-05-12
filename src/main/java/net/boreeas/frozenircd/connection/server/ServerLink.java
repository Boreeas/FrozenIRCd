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
package net.boreeas.frozenircd.connection.server;

import net.boreeas.frozenircd.utils.SharedData;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import net.boreeas.frozenircd.Interruptable;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.connection.Connection;

/**
 * This class represents a link to another IRC server.
 * @author Boreeas
 */
public class ServerLink extends Connection {
    
    private String host;
    
    /**
     * Opens a link to the specified server.
     * @param host The host of the server to connect to
     * @param port The port of the server to connect to
     * @throws IOException If an IOException occurred
     */
    public ServerLink(String host, int port, String password) throws IOException {
        
        SharedData.logger.log(Level.INFO, "Opening link to server at {0}:{1} with password {2}", new Object[]{host, port,
                password});
        
        this.host = host;
        
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
        send(String.format("PASS %1s %2s%3s IRC|%3s", password, SharedData.PROTOCOL_VERSION, SharedData.BUILD_IDENTIFIER));
        send(String.format("SERVER %s 1 %s : %s", ConfigData.getFirstConfigOption(ConfigKey.HOST), 
                                                          ConfigData.getFirstConfigOption(ConfigKey.TOKEN), 
                                                          ConfigData.getFirstConfigOption(ConfigKey.DESCRIPTION)));
    }

    

    @Override
    public final void send(String line) {
        
        try {
            SharedData.logger.log(Level.ALL, line);
            writer.write(line + "\r\n");
            writer.flush();
        } catch (IOException ioe) {
            
            requestInterrupt();    // An ioe indicates a closed stream
            SharedData.logger.log(Level.SEVERE, String.format("Unable to write output to %s, closing link", socket.getInetAddress().getHostName()), ioe);
        }
    }

    @Override
    public void onInput(String input) {
        
        // TODO Server Protocol
    }

    @Override
    public void onDisconnect() {
        
        SharedData.logger.log(Level.INFO, "Link to {0} closed.", this);
    }

    @Override
    public String getCommonName() {
        
        return host;
    }
}
