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
package net.boreeas.frozenircd.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import net.boreeas.frozenircd.command.ClientCommandParser;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.connection.ConnectionPool;
import net.boreeas.frozenircd.connection.client.Client;
import net.boreeas.frozenircd.connection.server.ServerInputHandler;
import net.boreeas.frozenircd.connection.server.ServerLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.boreeas.frozenircd.config.ConfigData.*;
import static net.boreeas.frozenircd.config.ConfigKey.*;

/**
 *
 * @author Boreeas
 */
public class SharedData {
    
    
    // Utility class empty constructor
    private SharedData() {}
    
    /**
     * The version of the protocol used for this implementation
     */
    public static final String PROTOCOL_VERSION = "0210";
    
    /**
     * The identifier for this build
     */
    public static final String BUILD_IDENTIFIER = "frozen001a";
    
    /**
     * The public logger object to be used by every class.
     */
    public static final Logger logger = LoggerFactory.getLogger("FrozenIRCd");
    
    /**
     * The handler that takes care of any input received from a server
     */
    public static final ServerInputHandler serverLinkInputHandler = new ServerInputHandler() {

        public void onInput(ServerLink link, String input) {

            logger.trace("[{0} ->] {1}", new Object[]{link, input});
        }

        public void onLink(ServerLink link) {

            logger.trace("Server {0} linked", link);
        }

        public void onDisconnect(ServerLink link) {

            logger.trace("Server {0} delinked", link);
        }
    };

    public static void onClientCommand(Client client, String command, String[] args) {

        command = command.trim();

        try {

            ClientCommandParser.parseClientCommand(command, client, args);
        } catch (Exception ex) {

            client.sendNotice(getFirstConfigOption(HOST), client.getSafeNickname(),
                              "Internal server error while processing command.");
            
            logger.error(String.format("Unhandled exception during command handling.\n"
                    + "\tCommand: %s\n"
                    + "\tWith args: %s\n"
                    + "\tIssued by:%s (%s)\n"
                    + "Caused by:", command, Arrays.toString(args), client.getMask(), client), ex);
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
    public static final boolean passwordNeeded = getFirstConfigOption(USING_PASS).equalsIgnoreCase("true");

    /**
     * The pattern that nicknames must adhere to
     */
    public static final Pattern nickPattern = Pattern.compile(
            String.format("%s{%s,%s}",
                          getConfigOption(NICK_PATTERN)[0],
                          getConfigOption(MIN_NICK_LENGTH)[0],
                          getConfigOption(MAX_NICK_LENGTH)[0]));

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

        oldChars[length] = '\0'; // Avoiding explicit bound check in while

        int newLen = -1;
        while (oldChars[++newLen] > ' '
                && oldChars[newLen] != ':'
                && oldChars[newLen] != ','); // Find first non-printable, all characters before that can remain as-is

        // If there are none it ends on the null char I appended
        for (int j = newLen; j < length; j++) {

            char ch = oldChars[j];

            if (ch > ' ' && ch != ':' && ch != ',') {

                oldChars[newLen] = ch; // The while avoids repeated overwriting here when newLen==j
                newLen++;
            }
        }

        return new String(oldChars, 0, newLen);
    }

    private static Set<Character> generateSettableUmodeSet() {

        Set<Character> set = new HashSet<Character>();

        set.add('i');

        return set;
    }

    private static Map<Character, String> generateUmodeMap() {

        Map<Character, String> map = new HashMap<Character, String>();

        map.put('o', "Designates a user as IRC Operator. This mode can only be set by use of the OPER command");
        map.put('i', "Designates a client as invisible, which makes you harder to find.");

        return map;
    }
}
