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

import net.boreeas.frozenircd.Flagable;
import net.boreeas.frozenircd.Channel;
import net.boreeas.frozenircd.Server;
import net.boreeas.frozenircd.config.IncompleteConfigurationException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import net.boreeas.frozenircd.utils.HashUtils;
import net.boreeas.frozenircd.utils.StringUtils;
import net.boreeas.frozenircd.connection.Connection;
import net.boreeas.frozenircd.utils.SharedData;
import net.boreeas.frozenircd.connection.client.Client;
import static net.boreeas.frozenircd.command.Reply.*;
import static net.boreeas.frozenircd.config.ConfigData.*;
import static net.boreeas.frozenircd.config.ConfigKey.*;

/**
 *
 * @author Boreeas
 */
public class ClientCommandParser {
    
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String USER = "USER";
    public static final String NICK = "NICK";
    public static final String PASS = "PASS";
    public static final String OPER = "OPER";
    public static final String MODE = "MODE";
    public static final String QUIT = "QUIT";
    public static final String STOP = "STOP";
    public static final String JOIN = "JOIN";
    public static final String PART = "PART";
    public static final String TOPIC = "TOPIC";
    public static final String NAMES = "NAMES";
    public static final String PRIVMSG = "PRIVMSG";
    
    
    public static void parseClientCommand(String command, Client client, String[] args) {
        
        command = command.toUpperCase();
        
        
        switch (command) {
            
            case PING:
                onPingCommand(client, args);
                break;
                
            case PONG:
                onPongCommand(client, args);
                break;
                
            case MODE:
                onModeCommand(client, args);
                break;
                
            case NICK:
                onNickCommand(client, args);
                break;
                
            case OPER:
                onOperCommand(client, args);
                break;
                
            case PASS:
                onPassCommand(client, args);
                break;
                
            case QUIT:
                onQuitCommand(client, args);
                break;
                
            case STOP:
                onStopCommand(client, args);
                break;
                
            case USER:
                onUserCommand(client, args);
                break;
                
            case JOIN:
                onJoinCommand(client, args);
                break;
                
            case PART:
                onPartCommand(client, args);
                break;
                
            case TOPIC:
                onTopicCommand(client, args);
                break;
            
            case PRIVMSG:
                onPrivmsgCommand(client, args);
                break;
                
            case NAMES:
                onNamesCommand(client, args);
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

        client.sendStandardFormat(PONG.format(getFirstConfigOption(HOST), args[0]));
    }
    
    private static void onPongCommand(Client client, String[] args) {

        if (args.length == 0) {

            client.send(ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), PONG, "<key>"));
            return;
        }

        client.updatePing(args[0]);
    }
    
    private static void onQuitCommand(Client client, String[] args) {

        String quitMessage = ( args.length == 0 ) ? client.getSafeNickname() : StringUtils.joinArray(args);

        client.broadcastToChannels(Command.QUIT.format(quitMessage));
        
        client.disconnect(quitMessage);
    }
    
    private static void onUserCommand(Client client, String[] args) {

        if (SharedData.passwordNeeded && (client.passGiven() == null)) {

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

            if (!isNameLengthOK(args[0])) {
                args[0] = args[0].substring(0, SharedData.maxNickLength);
            }
            
            if (!client.receivedIdentResponse()) {
                client.setUsername(args[0]);
            }
        
            client.setRealname(StringUtils.joinArray(args, 3));
        }
        
        
        if (!client.rplWelcomeSent() && client.registrationCompleted()) {
            
            client.onRegistrationComplete();
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
            
            if (!isNameLengthOK(args[0])) {
                args[0] = args[0].substring(0, SharedData.maxNickLength);
            }
            
            
            client.broadcastToChannels("NICK :" + args[0]);
            client.setNickname(args[0]);
        }
        
        
        if (!client.rplWelcomeSent() && client.registrationCompleted()) {
            
            client.onRegistrationComplete();
        }
    }
    
    private static void onPassCommand(Client client, String[] args) {

        if (!SharedData.passwordNeeded) {
            return; // PASS ignored if none is required
        }

        if (client.passGiven() != null) {

            client.sendStandardFormat(Reply.ERR_ALREADYREGISTERED.format(client.getSafeNickname()));
            return;
        }

        if (args.length < 1) {

            client.sendStandardFormat(Reply.ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), PASS, "<password>"));
            return;
        }

        String hash;
        try {
            hash = HashUtils.SHA256(args[0]);
        } catch (NoSuchAlgorithmException ex) {
            SharedData.logger.error("Disconnecting user because password hash could not be calculated.", ex);
            client.disconnect("Password could not be matched - hash algorithm defect.");
            return;
        }

        if (hash.equals(getFirstConfigOption(USER_PASS))) {

            client.sendNotice(getFirstConfigOption(HOST), client.getSafeNickname(), "*** PASS accepted");
            client.setPassGiven(true);
        } else {

            client.disconnect("Please specify the password using the PASS command");
        }
    }
    
    private static void onOperCommand(Client client, String[] args) {

        if (!client.registrationCompleted()) {
            return; // Drop any commands before registration is complete
        }

        // Check for parameter completeness
        if (args.length < 2) {

            client.sendStandardFormat(Reply.ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), OPER, "<name> <password>"));
            return;
        }

        // Check if the user's host matches any o-line
        if (!matchesOLine(client.getHostname())) {

            client.sendStandardFormat(Reply.ERR_NOOPERHOST.format(client.getSafeNickname()));
            return;
        }

        // Check password
        try {
            if (!checkOperPassword(args[0], args[1])) {

                client.sendStandardFormat(Reply.ERR_PASSWDMISMATCH.format(client.getSafeNickname()));
                return;
            }
        } catch (NoSuchAlgorithmException ex) {

            // Something went seriously wrong here
            SharedData.logger.error("Unable to generate password hash for OPER", ex);
            client.sendNotice(getFirstConfigOption(HOST), client.getSafeNickname(), "Unable to generate password hash");
            return;
        } catch (IncompleteConfigurationException ex) {

            // The given name is not configured as oper
            client.sendNotice(getFirstConfigOption(HOST), client.getSafeNickname(), "No such oper: " + args[0]);
            return;
        }

        client.sendStandardFormat(Reply.RPL_YOUREOPER.format(client.getSafeNickname()));
        client.addFlag(Mode.UMODE_OPER, null);
    }
    
    private static void onModeCommand(Client client, String[] args) {

        if (!client.registrationCompleted()) {
            return;
        }

        if (args.length < 1 || args[0].length() < 1) {

            client.sendStandardFormat(Reply.ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), MODE, "<nick/chan> [mode string]"));
            return;
        }
        
        Flagable target = client;
        
        // Check if the target is a channel
        if (SharedData.isChanTypeSupported(args[0].charAt(0))) {
            
            target = SharedData.getChannel(args[0]);
            
            if (target == null) {
                client.sendStandardFormat(ERR_NOSUCHCHANNEL.format(client.getNickname(), args[0]));
                return;
            }
        }

        if (args.length == 1) {
            // Ignore mode requests for channels
            Mode.handleModeChange(Mode.NO_FLAG, client, target, true, args, 0);
            return;
        }

        boolean adding = true;  // Setting or removing umodes
        int argIndex = 2;       // Parameters to modes start at index 2
        
        for (char c: args[1].toCharArray()) {
            
            if (c == '+') {
                
                adding = true;
            } else if (c == '-') {
                
                adding = false;
            } else {
                
                argIndex = Mode.handleModeChange(c, client, client, adding, args, argIndex);
            }
        }
    }
    
    private static void onStopCommand(Client client, String[] args) {

        if (client.hasFlag(Mode.UMODE_OPER)) {

            String reason = "No reason given";
            if (args.length > 0) {
                reason = StringUtils.joinArray(args);
            }

            SharedData.connectionPool.notifyClients(String.format("Server shutting down (STOP command invoked by %s (%s) (Reason: %s))",
                    client.getCommonName(), client.getHostmask(), reason));

            Server.INSTANCE.close();
        } else {

            client.sendStandardFormat(Reply.ERR_NOPRIVILEGES.format(client.getSafeNickname()));
        }
    }
    
    private static void onJoinCommand(Client client, String[] args) {
        
        if (!client.registrationCompleted()) return;    // Drop
        
        if (args.length < 1) client.send(Reply.ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), 
                                                                         JOIN, 
                                                                         "<channel> [password]"));
        
        Channel channel = SharedData.getChannel(args[0]);
        
        if (channel == null) {
            channel = new Channel(SharedData.toLowerCase(args[0]));
            SharedData.addChannel(channel);
        }
        
        channel.joinChannel(client);
        client.addChannel(channel.getName());
    }
    
    private static void onPartCommand(Client client, String[] args) {
    
        if (!client.registrationCompleted()) {
            return; // Drop silently
        }
        
        if (args.length < 1) {
            
            client.sendStandardFormat(Reply.ERR_NEEDMOREPARAMS.format(client.getNickname(), PART, "<channel> [reason]"));
            return;
        }
        
        String channel = args[0];
        if (!client.isInChannel(channel)) {
            
            client.sendStandardFormat(Reply.ERR_NOTONCHANNEL.format(client.getNickname(), args[0]));
            return;
        }
        
        
        String reason = client.getNickname();
        if (args.length >= 2) {
            reason = StringUtils.joinArray(args, 1);
        }
        
        client.removeChannel(channel);
        SharedData.getChannel(channel).partChannel(client, reason);
    }
    
    private static void onTopicCommand(Client client, String[] args) {
        
        if (!client.registrationCompleted()) return;
        
        if (args.length < 1) {
            client.sendStandardFormat(ERR_NEEDMOREPARAMS.format(client.getNickname(), "<channel> [topic]"));
            return;
        }
        
        Channel chan = SharedData.getChannel(args[0]);
        
        if (args.length < 2) {
            
            String topic = chan.getTopic();
            
            if (topic == null) {
                client.sendStandardFormat(Reply.RPL_NOTOPIC.format(client.getNickname(), args[0]));
            } else {
                client.sendStandardFormat(Reply.RPL_TOPIC.format(client.getNickname(), args[0], topic));
            }
        } else if (!chan.isOp(client)) {
            
            client.sendStandardFormat(Reply.ERR_CHANOPRIVSNEEDED.format(client.getNickname(), args[0]));
            return;
        } else if (!client.isInChannel(args[0])) {
            
            client.sendStandardFormat(Reply.ERR_NOTONCHANNEL.format(client.getNickname(), args[0]));
            return;
        } else {
            
            String topic = args[1].trim();
            if (topic.isEmpty()) topic = null;
            
            chan.setTopic(topic);
            chan.sendFromClient(client, TOPIC + " " + args[0] + " :" + args[1]);
        }
    }
    
    private static void onPrivmsgCommand(Client client, String[] args) {
        
        if (!client.registrationCompleted()) return;
        
        if (args.length < 2 || args[1].isEmpty()) {
            client.sendStandardFormat(Reply.ERR_NEEDMOREPARAMS.format(client.getNickname(), "<target> <message>"));
        }
        
        a
    }
    
    private static void onNamesCommand(Client client, String[] args) {
        
        if (!client.registrationCompleted()) return;
        
        if (args.length == 0) {
            for (;;);
        } else {
            Channel chan = SharedData.getChannel(args[0]);
            if (chan == null) return;
            
            String names = (client.isInChannel(args[0])) ? chan.names() : chan.visibleNames();
            
            client.sendStandardFormat(Reply.RPL_NAMREPLY.format(client.getNickname(), '=', chan.getName(), names));
        }
    }
    
    
    private static void onUnknownCommand(Client client, String command) {
        
        client.sendStandardFormat(Reply.ERR_UNKNOWNCOMMAND.format(client.getSafeNickname(), command));
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

            if (conn != client && SharedData.stringsEqual(name, conn.getCommonName())) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean isNameLegal(String name) {
        
        return SharedData.nickPattern.matcher(name).matches();
    }
    
    private static boolean isNameLengthOK(String name) {
        
        return name.length() >= SharedData.minNickLength 
            && name.length() <= SharedData.maxNickLength;
    }
}
