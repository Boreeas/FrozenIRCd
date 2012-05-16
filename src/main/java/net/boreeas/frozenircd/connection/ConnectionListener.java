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
package net.boreeas.frozenircd.connection;

import net.boreeas.frozenircd.connection.client.Client;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import net.boreeas.frozenircd.Interruptable;
import net.boreeas.frozenircd.utils.SharedData;

/**
 *
 * @author Boreeas
 */
public class ConnectionListener extends Thread implements Interruptable {
    
    private ServerSocket serverSocket;
    private boolean useSSL = false;
    
    private volatile boolean interrupted = false;
    
    public ConnectionListener(String host, int port) throws IOException {
        
        this(host, port, false);
    }
    
    public ConnectionListener(String host, int port, boolean useSSL) throws IOException {
        
        super("ConnectionListener[" + host + ":" + port + "]");
        this.useSSL = useSSL;
        
        if (!useSSL) {
            
            serverSocket = new ServerSocket();
        } else {
     
            serverSocket = SSLServerSocketFactory.getDefault().createServerSocket();
        }
        
        try {
            serverSocket.bind(new InetSocketAddress(host, port));
        } catch (SocketException ex) {
            SharedData.logger.error(String.format("Unable to bind to address: %s - Attempting to bind to default ip", 
                                                  ex.getMessage()));
            
            serverSocket.bind(new InetSocketAddress(port));
        }
        
        serverSocket.setSoTimeout(1000);
        
        SharedData.logger.info(String.format("Binding to %s:%s successful", serverSocket.getInetAddress(), 
                                                                            serverSocket.getLocalPort()));
    }
    
    @Override
    public void run() {
        
        while (!interrupted) {
            
            try {
                final Socket socket = serverSocket.accept();
                
                final Client client = new Client((useSSL) ? (SSLSocket) socket : socket, useSSL);
                
                // Check for hostname
                new HostnameDaemon(client, socket).start();
                
                // Check for identd
                new IdentDaemon(client, socket).start();
                
                // Wait for connection timeout
                new ConnectTimeoutDaemon(client).start();
                
                client.start();
                SharedData.connectionPool.addConnection(client.getUUID(), client);
            } catch (SocketTimeoutException ex) {
                
                // Forget about it - this is only to prevent endless blocks
            } catch (IOException ex) {
                
                SharedData.logger.error("Unable to accept incoming connection on port " + serverSocket.getLocalPort(), ex);
            }
        }
    }
    
    public void requestInterrupt() {
        
        interrupted = true;
    }
}
