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
package net.boreeas.frozenircd.command;

import net.boreeas.frozenircd.utils.ArrayUtils;
import net.boreeas.frozenircd.config.Reply;
import net.boreeas.frozenircd.connection.Connection;
import net.boreeas.frozenircd.utils.SharedData;
import net.boreeas.frozenircd.connection.client.Client;
import static net.boreeas.frozenircd.config.Reply.*;
import static net.boreeas.frozenircd.config.ConfigData.*;
import static net.boreeas.frozenircd.config.ConfigKey.*;

/**
 *
 * @author Boreeas
 */
public class ClientCommandParser {
    
    private static final String PING = "PING";
    private static final String PONG = "PONG";
    private static final String USER = "USER";
    private static final String NICK = "NICK";
    private static final String PASS = "PASS";
    private static final String OPER = "OPER";
    private static final String MODE = "MODE";
    private static final String QUIT = "QUIT";
    private static final String STOP = "STOP";
    
    
    public static void parseClientCommand(String command, Client client, String[] args) {
        
        command = command.toUpperCase();
        
        
        switch (command) {
            
            case (PING):
                onPingCommand(client, args);
                break;
            
            
            default:
                onUnknownCommand(client, command);
                break;
        }
    }
    
    
    
    private static void onPingCommand(Client client, String[] args) {

        if (args.length < 1) {

            client.sendStandardFormat(ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), PING, "<message>"));
            return;
        }

        client.sendStandardFormat(OTHER_PONG.format(getFirstConfigOption(HOST), args[0]));
    }
    
    private static void onPongCommand(Client client, String[] args) {

        if (args.length == 0) {

            client.send(ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), PONG, "<key>"));
            return;
        }

        client.updatePing(args[0]);
    }
    
    private static void onQuitCommand(Client client, String[] args) {

        String quitMessage = ( args.length == 0 ) ? client.getSafeNickname() : joinArray(args);

        client.disconnect(quitMessage);
    }
    
    private static void onUserCommand(Client client, String[] args) {

        if (SharedData.passwordNeeded && ( client.passGiven() == null )) {

            return; // Drop silently
        }

        String nickname = client.getSafeNickname();

        if (args.length < 4) {
            
            client.sendStandardFormat(ERR_NEEDMOREPARAMS.format(nickname, USER, "<username> <unused> <unused> :<realname>"));
            
        } else if (client.userGiven()) {
            
            client.sendStandardFormat(ERR_ALREADYREGISTERED.format(nickname));
            
        } else if (!isNameLegal(args[0])) {

            client.disconnect("Error: Illegal username");
            
        } else {

            if (!client.receivedIdentResponse()) {
                client.setUsername(args[0]);
            }
        
            client.setRealname(ArrayUtils.joinArray(args, 3));
            client.addFlag('i');
        }
    }
    
    private static void onNickCommand(Client client, String[] args) {

        if (SharedData.passwordNeeded && ( client.passGiven() == null )) {

            return; // Drop silently
        }

        String nickname = client.getSafeNickname();

        if (args.length == 0) {

            client.sendStandardFormat(ERR_NONICKNAMEGIVEN.format(nickname));
            
        } else if (!isNameLegal(args[0])) {

            client.sendStandardFormat(ERR_ERRONEUSNICKNAME.format(nickname, args[0], "Illegal character"));
            
        } else if (isNameBlacklisted(args[0])) {
            
            client.sendStandardFormat(ERR_ERRONEUSNICKNAME.format(nickname, args[0], "Illegal nickname"));
            
        } else if (isNameInUse(client, args[0])) {
            
            client.sendStandardFormat(Reply.ERR_NICKNAMEINUSE.format(nickname, args[0]));
            
        } else {
            
            client.setNickname(args[0]);
        }
    }
    
    
    
    private static void onUnknownCommand(Client client, String command) {
        
        client.sendStandardFormat(command);
    }
    
    
    
    private static boolean isNameBlacklisted(String name) {
        
        for (String nick : getConfigOption(BLACKLISTED_NICKS)) {
            
            if (nick.equalsIgnoreCase(name)) {
                return true;
            }
        }
        
        return false;
    }
    
    
    private static boolean isNameInUse(Client client, String name) {
        
        for (Connection conn : SharedData.connectionPool.getConnections()) {

            if (conn != client && SharedData.nicknamesEqual(name, conn.getCommonName())) {
                return true;
            }
        }
        
        return false;
    }
    
    
    private static boolean isNameLegal(String name) {
        
        return SharedData.nickPattern.matcher(name).matches();
    }
}
