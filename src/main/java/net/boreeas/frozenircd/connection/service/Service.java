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
package net.boreeas.frozenircd.connection.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.config.Reply;
import net.boreeas.frozenircd.utils.SharedData;
import net.boreeas.frozenircd.connection.Connection;

/**
 *
 * @author Boreeas
 */
public class Service extends Connection {
    
    
    public final String nick;
    public final String visibility;
    public final String type;
    
    
    public Service(Socket socket, BufferedWriter writer, BufferedReader reader, String nick, String visibility, String type, UUID uuid) {
        
        this.socket = socket;
        this.writer = writer;
        this.reader = reader;
        
        this.nick = nick;
        this.visibility = visibility;
        this.type = type;
        
        this.uuid = uuid;
    }
    
    public void send(String line) {
        
        try {
            
            SharedData.logger.trace("[-> {0}] {1}", new Object[]{socket.getInetAddress(), line});
            
            writer.write(String.format(":%s %s\r\n", ConfigData.getFirstConfigOption(ConfigKey.HOST), line));
            writer.flush();
        } catch (IOException ioe) {
            
            SharedData.logger.error(String.format("Could not write to %s, closing connection", socket.getInetAddress()), ioe);
            disconnect(ioe.getMessage());
        }
    }

    @Override
    public void onInput(String input) {
        
        send(Reply.ERR_UNKNOWNCOMMAND.format(nick, "Unable to handle service input"));
    }

    @Override
    public void onDisconnect() {
        
        SharedData.logger.info("Service {0} at {1} disconnected.", new Object[]{nick, this});
    }

    @Override
    public String getCommonName() {
        
        return nick;
    }
    
}
