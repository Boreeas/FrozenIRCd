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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
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
    
    private Set<InputHandler> handlers = new CopyOnWriteArraySet<InputHandler>();
    
    private volatile boolean interrupted;
    
    public Client(Socket socket, boolean ssl) throws IOException {
        
        this.socket = socket;
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
                
            } catch (IOException ex) {
                SharedData.logger.log(Level.SEVERE, String.format("IOException while reading data from %s, closing connection.", socket.getInetAddress().getHostName()), ex);
                break;
            }
        }
        
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException ioe) {
            // Not much we can do here anyways
            SharedData.logger.log(Level.INFO, String.format("IOException while closing streams to %s", socket.getInetAddress().getHostName()), ioe);
        }
    }
    
    public void addHandler(InputHandler handler) {
        
        handlers.add(handler);
    }
    
    public void requestInterrupt() {
        
        this.interrupted = true;
    }

    public void send(String line) {
        
        try {
            
            SharedData.logger.log(Level.parse("0"), "[-> {0}] {1}", new Object[]{socket.getInetAddress(), line});
            
            writer.write(line + "\r\n");
            writer.flush();
        } catch (IOException ioe) {
            
            SharedData.logger.log(Level.SEVERE, String.format("Could not write to %s, closing connection", socket.getInetAddress()), ioe);
            requestInterrupt();
        }
    }
    
    public boolean isSSLConnection() {
        
        return ssl;
    }
    
}
