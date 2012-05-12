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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import net.boreeas.frozenircd.connection.client.Client;

/**
 *
 * @author Boreeas
 */
public class IdentDaemon extends Thread {
    
    private Client client;
    private Socket socket;
    
    public IdentDaemon(Client client, Socket socket) {
        
        super("IdentHostname[" + client + "]");
        
        setDaemon(true);
        
        this.client = client;
        this.socket = socket;
    }
    
    @Override
    public void run() {

        client.sendNotice("AUTH", client.getSafeNickname(), "*** Checking Ident");
        try {
            Socket identdSocket = new Socket(socket.getInetAddress(), 113);
            identdSocket.setSoTimeout(10000);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(identdSocket.getOutputStream()));
            writer.write(String.format("%s, %s", socket.getPort(), socket.getLocalPort()));


            String id = parseIdentResponse(
                    new BufferedReader(
                    new InputStreamReader(
                    identdSocket.getInputStream())).readLine());

            if (id == null || id.length() == 0) {
                throw new IOException();    // No ident reponse
            }

            client.setIdentified(true);
            client.setUsername(id);

        }
        catch (SocketTimeoutException ex) {
            client.sendNotice("AUTH", client.getSafeNickname(), "*** Ident reponse timed out");
        }
        catch (IOException ex) {
            client.sendNotice("AUTH", client.getSafeNickname(), "*** No Ident reponse");
        }
    }

    public String parseIdentResponse(String response) {

        String[] fields = response.split(" ?: ?");
        if (fields.length < 4) {
            return null;
        }

        if (!fields[1].equalsIgnoreCase("USERID")) {
            return null;
        }

        return response.trim();
    }
}
