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

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.boreeas.frozenircd.connection.client.Client;
import net.boreeas.frozenircd.connection.client.ClientCommandHandler;
import net.boreeas.frozenircd.connection.Connection;
import net.boreeas.frozenircd.connection.ConnectionPool;
import net.boreeas.frozenircd.connection.server.ServerCommandHandler;
import net.boreeas.frozenircd.connection.server.ServerInputHandler;
import net.boreeas.frozenircd.connection.server.ServerLink;
import net.boreeas.frozenircd.utils.HashUtils;

/**
 * This class provides a number of common variables and functions that are used
 * across the project
 * @author Boreeas
 */
public class SharedData {
     
    /**
     * The version of the protocol used for this implementation
     */
    public static final String PROTOCOL_VERSION = "0210";
    
    /**
     * The identifier for this build
     */
    public static final String BUILD_IDENTIFIER = "frozen001a";
    
    /**
     * All commands the server knows that can be executed by a client
     */
    private static Map<ClientCommand, ClientCommandHandler> clientCommands 
            = new EnumMap<ClientCommand, ClientCommandHandler>(ClientCommand.class);
    
    /**
     * All commands the server knows that can be executed by other servers
     */
    private static Map<String, ServerCommandHandler> serverCommands = new HashMap<String, ServerCommandHandler>();
    
    /**
     * The public logger object to be used by every class.
     */
    public static final Logger logger = Logger.getLogger("FrozenIRCd");
    
    /**
     * The handler that takes care of any input received from a server
     */
    public static final ServerInputHandler serverLinkInputHandler = new ServerInputHandler() {

        public void onInput(ServerLink link, String input) {
            
            logger.log(Level.FINEST, "[{0} ->] {1}", new Object[]{link, input});
        }

        public void onLink(ServerLink link) {
            
            logger.log(Level.FINEST, "Server {0} linked", link);
        }

        public void onDisconnect(ServerLink link) {
            
            logger.log(Level.FINEST, "Server {0} delinked", link);
        }
    };
    
    
    public static void onClientCommand(Client client, String command, String[] args) {
        
        try {
            
            ClientCommand cmd = ClientCommand.forName(command);
            
            if (clientCommands.containsKey(cmd)) {
                
                clientCommands.get(cmd).onCommand(client, args);
            } else {
                
                client.send(Reply.ERR_UNKNOWNCOMMAND.format(client.getSafeNickname(), command));
            }
        } catch (Exception ex) {
            
            logger.log(Level.SEVERE, String.format("Unhandled exception during command handling.\n"
                                                + "\tCommand: %s\n"
                                                + "\tWith args: %s\n"
                                                + "\tIssued by: %s\n"
                                                + "Caused by:", command, Arrays.toString(args), client), ex);
        }
    }
    
    
    /**
     * The pool of all connections
     */
    public static final ConnectionPool connectionPool = new ConnectionPool();
    
    
    /**
     * A set of all known umodes and their description.
     */
    public static final Map<Character, String> umodes = Collections.unmodifiableMap(generateUmodeMap());
    
    /**
     * A set of all umodes that can be set by a user
     */
    public static final Set<Character> settableUmodes = Collections.unmodifiableSet(generateSettableUmodeSet());
    
    /**
     * Determines whether a password is needed for a connection
     */
    public static final boolean passwordNeeded = ConfigData.getFirstConfigOption(ConfigKey.USING_PASS).equalsIgnoreCase("true");
    
    // Setup of some things
    static {
        String loglevel = ConfigData.getFirstConfigOption(ConfigKey.LOGGING_LEVEL);
        
        if (!loglevel.matches("\\-?[0-9]+")) {
            
            logger.setLevel(Level.parse("0"));
            logger.log(Level.WARNING, "Config option \"{0}\" did not match the required format (must be an integer) - Defaulting to 0", 
                                       ConfigKey.LOGGING_LEVEL.getKey());
        } else {
            
            logger.setLevel(Level.parse(loglevel));
            logger.log(Level.INFO, "Logging at level {0}", loglevel);
        }
        
        logger.log(Level.INFO, "Setting up client command map");
        fillClientCommandMap();
        logger.log(Level.INFO, "Setting up server command map");
        fillServerCommandMap();
    }
    
    /**
     * The pattern that nicknames must adhere to
     */
    public static final Pattern nickPattern 
            = Pattern.compile(
                String.format("%s{%s,%s}",
                    ConfigData.getConfigOption(ConfigKey.NICK_PATTERN)[0],
                    ConfigData.getConfigOption(ConfigKey.MIN_NICK_LENGTH)[0],
                    ConfigData.getConfigOption(ConfigKey.MAX_NICK_LENGTH)[0]));
   
    
    /**
     * Returns the lowercase version of <code>string</code> with the 
     * @param string
     * @return 
     */
    public static String toLowerCase(String string) {
        
        return string.toLowerCase().replace('[', '{').replace(']', '}').replace('\\', '|').replace('~', '^');
    }
    
    
    public static boolean nicknamesEqual(String nick1, String nick2) {
        
        
        if (nick1 == nick2) {
            return true;
        }
        
        if ((nick1 == null && nick2 != null) || (nick1 != null && nick2 == null)) {
            return false;
        }
        
        
        if (nick1.length() != nick2.length()) {
            return false;
        }
        
        return toLowerCase(nick1).equals(toLowerCase(nick2));
    }
    
    
    /**
     * Removes all non-printable characters from a string, as well as any : or ,
     * @param s The string to clean
     * @return The cleaned string
     * @author http://stackoverflow.com/a/7161653
     */
    public static String cleanString(String s) {
        
        int length = s.length();
        
        char[] oldChars = new char[length + 1];
        s.getChars(0, length, oldChars, 0);
        
        oldChars[length] = '\0';    // Avoiding explicit bound check in while
        
        int newLen = -1;
        while (oldChars[++newLen] > ' ' 
                && oldChars[newLen] != ':'
                && oldChars[newLen] != ',');   // Find first non-printable, all characters before that can remain as-is
        
        // If there are none it ends on the null char I appended
        for (int j = newLen; j < length; j++) {
            
            char ch = oldChars[j];
            
            if (ch > ' ' && ch != ':' && ch != ',') {
                
                oldChars[newLen] = ch;  // The while avoids repeated overwriting here when newLen==j
                newLen++;
            }
        }
        
        return new String(oldChars, 0, newLen);
    }
    
    
    private static Set<Character> generateSettableUmodeSet() {
        
        Set<Character> set = new HashSet<Character>();
        
        return set;
    }
    
    private static Map<Character, String> generateUmodeMap() {
        
        Map<Character, String> map = new HashMap<Character, String>();
        
        map.put('o', "Designates a user as IRC Operator. This mode can only be set by use of the OPER command");
        
        return map;
    }
    
    
    private static void fillClientCommandMap() {
        
        clientCommands.put(ClientCommand.PING, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args) {
                
                if (args.length < 1) {
                    
                    client.send(Reply.ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), ClientCommand.PING, "<message>"));
                    return;
                }
                
                client.sendWithoutPrefix(":PONG :" + args[0]);
            }
        });
        
        clientCommands.put(ClientCommand.USER, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args ) {
                                
                if (passwordNeeded && (client.passGiven() == null)) {
                    
                    return; // Drop silently
                }
                
                String nickname = client.getSafeNickname();
                
                if (args.length < 4) {
                    client.send(Reply.ERR_NEEDMOREPARAMS.format(nickname, ClientCommand.USER, "<username> <unused> <unused> :<realname>"));
                    return;
                }
                
                if (client.userGiven()) {
                    // ERR_ALREADYREGISTERED
                    client.send(Reply.ERR_ALREADYREGISTERED.format(nickname));
                }
                
                if (!client.receivedIdentResponse()) {
                    client.setUsername(args[0]);
                }
                client.setRealname(args[3]);
                client.addFlag('i');
            }
        });
        
        clientCommands.put(ClientCommand.NICK, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args) {
                
                if (passwordNeeded && (client.passGiven() == null)) {
                    
                    return; // Drop silently
                }
                
                String nickname = client.getSafeNickname();
                
                if (args.length == 0) {
                    
                    client.send(Reply.ERR_NONICKNAMEGIVEN.format(nickname));
                    return;
                }
                
                if (!nickPattern.matcher(args[0]).matches()) {
                    
                    client.send(Reply.ERR_ERRONEUSNICKNAME.format(nickname, args[0], "Illegal character"));
                    return;
                }
                  
                for (String nick : ConfigData.getConfigOption(ConfigKey.BLACKLISTED_NICKS)) {

                    if (nick.equalsIgnoreCase(args[0])) {

                        client.send(Reply.ERR_ERRONEUSNICKNAME.format(nickname, args[0], "Illegal nickname"));
                        return;
                    }
                }
                
                for (Connection conn: connectionPool.getConnections()) {
                    
                    if (conn != client && nicknamesEqual(args[0], conn.getCommonName())) {
                        
                        client.send(Reply.ERR_NICKNAMEINUSE.format(nickname, args[0]));
                        return;
                    }
                }
                
                client.setNickname(args[0]);
            }
        });
        
        clientCommands.put(ClientCommand.PASS, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args) {
                
                if (!passwordNeeded) {
                    return; // PASS ignored if none is required
                }
                
                if (client.passGiven() != null) {
                    
                    client.send(Reply.ERR_ALREADYREGISTERED.format(client.getSafeNickname()));
                    return;
                }
                
                if (args.length < 1) {
                    
                    client.send(Reply.ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), ClientCommand.PASS, "<password>"));
                    return;
                }
                
                String hash;
                try {
                    hash = HashUtils.SHA256(args[0]);
                }
                catch (NoSuchAlgorithmException ex) {
                    logger.log(Level.SEVERE, "Disconnecting user because password hash could not be calculated.", ex);
                    client.disconnect("Password could not be matched - hash algorithm defect.");
                    return;
                }
                
                if (hash.equals(ConfigData.getFirstConfigOption(ConfigKey.USER_PASS))) {
                    
                    client.sendNotice("***", "PASS accepted");
                    client.setPassGiven(true);
                } else {
                    
                    client.disconnect("Please specify the password using the PASS command");
                }
            }
        });
        
        clientCommands.put(ClientCommand.OPER, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args) {
                
                if (!client.registrationCompleted()) {
                    return; // Drop any commands before registration is complete
                }
                
                // Check for parameter completeness
                if (args.length < 2) {
                    
                    client.send(Reply.ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), ClientCommand.OPER, "<name> <password>"));
                    return;
                }
                
                // Check if the user's host matches any o-line
                if (!ConfigData.matchesOLine(client.getHostname())) {
                    
                    client.send(Reply.ERR_NOOPERHOST.format(client.getSafeNickname()));
                    return;
                }
                
                // Check password
                try {
                    if (!ConfigData.checkOperPassword(args[0], args[1])) {
                        
                        client.send(Reply.ERR_PASSWDMISMATCH.format(client.getSafeNickname()));
                        return;
                    }
                } catch (NoSuchAlgorithmException ex) {
                    
                    // Something went seriously wrong here
                    logger.log(Level.SEVERE, "Unable to generate password hash for OPER", ex);
                    client.sendNotice("***", "Unable to generate password hash");
                    return;
                } catch (IncompleteConfigurationException ex) {
                    
                    // The given name is not configured as oper
                    client.sendNotice("***", "No such oper: " + args[0]);
                    return;
                }
                
                client.send(Reply.RPL_YOUREOPER.format(client.getSafeNickname()));
                client.addFlag('o');
            }
        });
        
        clientCommands.put(ClientCommand.MODE, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args) {
                
                if (!client.registrationCompleted()) {
                    return;
                }
                
                if (args.length < 1) {
                    
                    client.send(Reply.ERR_NEEDMOREPARAMS.format(client.getSafeNickname(), ClientCommand.MODE, "<nick> [mode string]"));
                    return;
                }
                
                if (!nicknamesEqual(client.getSafeNickname(), args[0])) {
                    
                    //Don't allow to set other user's modes
                    client.send(Reply.ERR_USERSDONTMATCH.format(client.getSafeNickname()));
                    return;
                }
                
                if (args.length == 1) {
                    
                    client.send(Reply.RPL_UMODEIS.format(client.getSafeNickname(), client.getSafeNickname(), client.flags()));
                    return;
                }
                
                if (args[1].startsWith("-")) {
                    
                    if (args[1].length() < 2) {
                        return;
                    }
                    
                    client.removeFlags(args[1].substring(1));
                    return;
                }
                
                if (args[1].startsWith("+")) {
                    if (args[1].length() < 2) {
                        return;
                    }
                    
                    args[1] = args[1].substring(1);
                }
                
                client.addFlags(args[1]);
            }
        });
    }
    
    private static void fillServerCommandMap() {
        
        
    }
}
