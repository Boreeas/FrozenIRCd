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

import net.boreeas.frozenircd.config.Config;
import net.boreeas.frozenircd.config.SharedData;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

/**
 * This class represents a link to another IRC server.
 * @author Boreeas
 */
public class ServerLink extends Thread implements Interruptable, Connection {
    
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    
    private Set<InputHandler> handlers = new CopyOnWriteArraySet<InputHandler>();
    
    private volatile boolean interrupted = false;
    
    /**
     * Opens a link to the specified server.
     * @param host The host of the server to connect to
     * @param port The port of the server to connect to
     * @throws IOException If an IOException occurred
     */
    public ServerLink(String host, int port, String password) throws IOException {
        
        SharedData.logger.log(Level.INFO, "Opening link to server at {0}:{1} with password {2}", new Object[]{host, port,
                password});
        
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
        Config config = SharedData.getConfig();
        
        send(String.format("PASS %1s %2s%3s IRC|%3s", password, SharedData.PROTOCOL_VERSION, SharedData.BUILD_IDENTIFIER));
        send(String.format("SERVER %s 1 %s : %s", config.get(SharedData.CONFIG_KEY_HOST), 
                                                          config.get(SharedData.CONFIG_KEY_TOKEN), 
                                                          config.get(SharedData.CONFIG_KEY_DESCRIPTION)));
    }

    @Override
    public void run() {
        
        // For graceful termination
        while (!interrupted) {
            
            try {
                
                String input = reader.readLine();
                
                if (input == null) {
                    
                    // Null only if connection is closed
                    SharedData.logger.info(String.format("Link to %s closed", socket.getInetAddress().getHostName()));
                    break;
                }
                
                // We don't handle input ourself, but let input handler register to do so
                for (InputHandler handler: handlers) {    
                    handler.onInput(this, input);
                } 
                
            } catch (IOException ioe) {
                
                SharedData.logger.log(Level.SEVERE, String.format("Link to %s closed with Exception", socket.getInetAddress().getHostName()), ioe);
                break;
            }
            
            try {
                
                // This reduces load on the processor and enables other threads to finish their read/writes
                sleep(50);
            } catch (InterruptedException ie) {
                
                // Stop requested
                break;
            }
        }

        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException ioe) {
            
            SharedData.logger.log(Level.WARNING, String.format("IOException while closing streams to %s", socket.getInetAddress().getHostName()), ioe);
        }
    }
    
    /**
     * Adds an input handler to be notified if input is read.
     * @param handler The handler to register to be notified
     */
    public synchronized void addHandler(InputHandler handler) {
        
        handlers.add(handler);
    }

    @Override
    public void requestInterrupt() {
        
        interrupted = true;
    }

    @Override
    public final void send(String line) {
        
        try {
            SharedData.logger.log(Level.parse("0"), line);
            writer.write(line + "\r\n");
            writer.flush();
        } catch (IOException ioe) {
            
            requestInterrupt();    // An ioe indicates a closed stream
            SharedData.logger.log(Level.SEVERE, String.format("Unable to write output to %s, closing link", socket.getInetAddress().getHostName()), ioe);
        }
    }
}
