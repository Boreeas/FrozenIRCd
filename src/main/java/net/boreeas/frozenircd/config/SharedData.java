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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.boreeas.frozenircd.connection.client.Client;
import net.boreeas.frozenircd.connection.client.ClientInputHandler;
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
    private static Map<String, ClientCommandHandler> clientCommands = new HashMap<String, ClientCommandHandler>();
    
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
    
    /**
     * The handler that takes care of client input
     */
    public static final ClientInputHandler clientInputHandler = new ClientInputHandler() {

        public void onInput(Client connection, String input) {
            
            logger.log(Level.ALL, "[{0} ->] {1}", new Object[]{connection.toString(), input});
            
            String[] fields = input.split(" ", 2);
            
            if (clientCommands.containsKey(fields[0].toLowerCase())) {
                
                // For "x y :z" contains ["x y ", "z"]. For "" contains []. For "x y" contains ["x y"]
                String[] toLastArg = (fields.length > 1) ? fields[1].split(":") : new String[0];
                
                // For "x y :z" contains ["x", "y"]. For "" contains []. For "x y" contains ["x", "y"]
                String[] otherArgs = (toLastArg.length > 0) ? toLastArg[0].trim().split(" ") : new String[0];
                
                String[] argsTotal = new String[otherArgs.length + ((toLastArg.length > 1) ? 1 : 0)];
                
                System.arraycopy(otherArgs, 0, argsTotal, 0, otherArgs.length);
                if (toLastArg.length > 1) {
                    argsTotal[argsTotal.length - 1] = toLastArg[1];
                }
                
                clientCommands.get(fields[0].toLowerCase()).onCommand(connection, argsTotal, (fields.length > 1) ? fields[1] : "");
            } else {
                
                connection.send(String.format(ERR_UNKNOWNCOMMAND, '*', fields[0]));
            }
            
            
        }

        public void onModeChange(Client client, String modeString) {
            
            client.send(String.format(RPL_UMODEIS, client.getSafeNickname(), client.getSafeNickname(), modeString));
        }
        
        public void onConnect(Client connection) {
            SharedData.logger.log(Level.ALL, "Client {0} connected", connection);
        }

        public void onDisconnect(Client connection) {
            SharedData.logger.log(Level.ALL, "Client {0} disconnected", connection);
        }
    };
    
    /**
     * The pool of all linked servers
     */
    public static final ConnectionPool serverPool = new ConnectionPool();
    
    /**
     * The pool of all connected clients
     */
    public static final ConnectionPool clientPool = new ConnectionPool();
    
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
        
        clientCommands.put(CLIENT_COMMAND_PING, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args, String argsAsString) {
                
                client.sendWithoutPrefix(":PONG " + argsAsString);
            }
        });
        
        clientCommands.put(CLIENT_COMMAND_USER, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args, String argsAsString) {
                                
                if (passwordNeeded && !client.passGiven()) {
                    
                    return; // Drop silently
                }
                
                String nickname = client.getSafeNickname();
                
                if (args.length < 4) {
                    client.send(String.format(ERR_NEEDMOREPARAMS, nickname, CLIENT_COMMAND_USER, "<username> <unused> <unused> :<realname>"));
                    return;
                }
                
                if (client.userGiven()) {
                    // ERR_ALREADYREGISTERED
                    client.send(String.format(ERR_ALREADYREGISTERED, nickname));
                }
                
                if (!client.receivedIdentResponse()) {
                    client.setUsername(args[0]);
                }
                client.setRealname(args[3]);
                client.addFlag('i');
            }
        });
        
        clientCommands.put(CLIENT_COMMAND_NICK, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args, String argsAsString) {
                
                if (passwordNeeded && !client.passGiven()) {
                    
                    return; // Drop silently
                }
                
                String nickname = client.getSafeNickname();
                
                if (args.length == 0) {
                    
                    client.send(String.format(ERR_NONICKNAMEGIVEN, nickname));
                    return;
                }
                
                if (!nickPattern.matcher(args[0]).matches()) {
                    
                    client.send(String.format(ERR_ERRONEUSNICKNAME, nickname, args[0], "Illegal character"));
                    return;
                }
                  
                for (String nick : ConfigData.getConfigOption(ConfigKey.BLACKLISTED_NICKS)) {

                    if (nick.equalsIgnoreCase(args[0])) {

                        client.send(String.format(ERR_ERRONEUSNICKNAME, nickname, args[0], "Illegal nickname"));
                        return;
                    }
                }
                
                for (Connection conn: clientPool.getConnections()) {
                    
                    if (conn != client && nicknamesEqual(args[0], ((Client)conn).getSafeNickname())) {
                        
                        client.send(String.format(ERR_NICKNAMEINUSE, nickname, args[0]));
                        return;
                    }
                }
                
                client.setNickname(args[0]);
            }
        });
        
        clientCommands.put(CLIENT_COMMAND_PASS, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args, String argsAsString) {
                
                if (!passwordNeeded) {
                    return; // PASS ignored if none is required
                }
                
                if (client.passGiven()) {
                    
                    client.send(String.format(ERR_ALREADYREGISTERED, client.getSafeNickname()));
                    return;
                }
                
                if (args.length < 1) {
                    
                    client.send(String.format(ERR_NEEDMOREPARAMS, client.getSafeNickname(), CLIENT_COMMAND_PASS, "<password>"));
                    return;
                }
                
                String hash;
                try {
                    hash = HashUtils.SHA256(argsAsString);
                }
                catch (NoSuchAlgorithmException ex) {
                    logger.log(Level.SEVERE, "Disconnecting user because password hash could not be calculated.");
                    client.disconnect("Password could not be matched - hash algorithm defect.");
                    return;
                }
                
                if (hash.equals(ConfigData.getFirstConfigOption(ConfigKey.PASS))) {
                    
                    client.sendNotice("***", "PASS accepted");
                    client.setPassGiven(true);
                } else {
                    
                    client.disconnect("Please specify the password using the PASS command");
                }
            }
        });
        
        clientCommands.put(CLIENT_COMMAND_OPER, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args, String argsAsString) {
                
                if (!client.registrationCompleted()) {
                    return; // Drop any commands before registration is complete
                }
                
                // Check for parameter completeness
                if (args.length < 2) {
                    
                    client.send(String.format(ERR_NEEDMOREPARAMS, client.getSafeNickname(), CLIENT_COMMAND_OPER, "<name> <password>"));
                    return;
                }
                
                // Check if the user's host matches any o-line
                if (!ConfigData.matchesOLine(client.getHostname())) {
                    
                    client.send(String.format(ERR_NOOPERHOST, client.getSafeNickname()));
                    return;
                }
                
                // Check password
                try {
                    if (!ConfigData.checkOperPassword(args[0], args[1])) {
                        
                        client.send(String.format(ERR_PASSWDMISMATCH, client.getSafeNickname()));
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
                
                client.send(String.format(RPL_YOUREOPER, client.getSafeNickname()));
                client.addFlag('o');
            }
        });
        
        clientCommands.put(CLIENT_COMMAND_MODE, new ClientCommandHandler() {

            public void onCommand(Client client, String[] args, String argsAsString) {
                
                if (!client.registrationCompleted()) {
                    return;
                }
                
                if (args.length < 1) {
                    
                    client.send(String.format(ERR_NEEDMOREPARAMS, client.getSafeNickname(), CLIENT_COMMAND_MODE, "<nick> [mode string]"));
                    return;
                }
                
                if (!nicknamesEqual(client.getSafeNickname(), args[0])) {
                    
                    //Don't allow to set other user's modes
                    client.send(String.format(ERR_USERSDONTMATCH, client.getSafeNickname()));
                    return;
                }
                
                if (args.length == 1) {
                    
                    client.send(String.format(RPL_UMODEIS, client.getSafeNickname(), client.getSafeNickname(), client.flags()));
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
    
    
    private static final String CLIENT_COMMAND_PING = "ping";
    private static final String CLIENT_COMMAND_USER = "user";
    private static final String CLIENT_COMMAND_NICK = "nick";
    private static final String CLIENT_COMMAND_PASS = "pass";
    private static final String CLIENT_COMMAND_OPER = "oper";
    private static final String CLIENT_COMMAND_MODE = "mode";
    
    // Param 1 is always the nickname of the target, or * if none is given yet
    // Other parameters can be any information
    public static final String RPL_UMODEIS = "221 %s :Usermode for %s is %s";
    public static final String RPL_YOUREOPER = "381 %s :Successfully set OPER status";
    
    // Second parameter is usuall the cause of the error
    // Last parameter usually gives more help for the error
    public static final String ERR_UNKNOWNCOMMAND = "421 %s %s: Unknown command";
    public static final String ERR_NONICKNAMEGIVEN = "431 %s :No nickname given";
    public static final String ERR_ERRONEUSNICKNAME = "432 %s %s :Illegal nickname: %s";
    public static final String ERR_NICKNAMEINUSE = "433 %s :Nickname already in use: %s";
    public static final String ERR_NEEDMOREPARAMS = "461 %s %s :Not enough parameters. Parameters: %s";
    public static final String ERR_ALREADYREGISTERED = "462 %s :You may not reregister";
    public static final String ERR_PASSWDMISMATCH = "464 %s :Invalid password";
    public static final String ERR_NOOPERHOST = "491 %s :Your host did not match any o-line";
    public static final String ERR_UMODEUNKNOWNFLAG = "501 %s :Unknown mode flag %s";
    public static final String ERR_USERSDONTMATCH = "502 %s :Can't set mode on other user";
}
