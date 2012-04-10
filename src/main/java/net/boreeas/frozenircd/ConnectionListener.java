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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import net.boreeas.frozenircd.config.SharedData;

/**
 *
 * @author Boreeas
 */
public class ConnectionListener extends Thread implements Interruptable {
    
    private ServerSocket serverSocket;
    private boolean useSSL = false;
    
    private volatile boolean interrupted = false;
    
    public ConnectionListener(int port) throws IOException {
        
        this(port, false);
    }
    
    public ConnectionListener(int port, boolean useSSL) throws IOException {
        
        SharedData.logger.log(Level.INFO, "Opening connection listener on port {0} ({1})", new Object[]{port,
                (useSSL) ? "ssl" : "no ssl"});
        
        this.useSSL = useSSL;
        
        if (!useSSL) {
            
            serverSocket = new ServerSocket(port);
        } else {
     
            serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(port);
        }
    }
    
    @Override
    public void run() {
        
        while (!interrupted) {
            
            try {
                
                Socket socket = serverSocket.accept();
                Client client = new Client((useSSL) ? (SSLSocket) socket : socket, useSSL);
                client.addHandler(SharedData.clientInputHandler);
                SharedData.clientPool.addConnection(client.toString(), client);
            } catch (IOException ex) {
                
                SharedData.logger.log(Level.SEVERE, "Unable to accept incoming connection on port " + serverSocket.getLocalPort(), ex);
            }
        }
    }
    
    public void requestInterrupt() {
        
        interrupted = true;
    }
}
