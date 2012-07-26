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

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;
import net.boreeas.frozenircd.command.ClientCommandParser;
import net.boreeas.frozenircd.command.Reply;
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

    // TODO remove this - This belongs in ClientCommandParser
    public static void onClientCommand(Client client, String command, String[] args) {

        command = command.trim();

        try {

            ClientCommandParser.parseClientCommand(command, client, args);
        } catch (Exception ex) {

            client.sendStandardFormat(Reply.ERR_UNKNOWNERROR.format(client.getSafeNickname(), command));

            logger.error(String.format("Unhandled exception during command handling.\n"
                    + "\tCommand: %s\n"
                    + "\tWith args: %s\n"
                    + "\tIssued by:%s (%s)\n"
                    + "Caused by:", command, Arrays.toString(args), client.getHostmask(), client), ex);
        }
    }



    /**
     * Determines whether a password is needed for a connection
     */
    public static final boolean passwordNeeded = getFirstConfigOption(USING_PASS).equalsIgnoreCase("true");

    /**
     * Determines whether opers can set modes for other users and modes for channels they are not part of.
     */
    public static final boolean operCanSetModes = getFirstConfigOption(OPER_CANSETMODE).equalsIgnoreCase("true");

    /**
     * Determines whether opers can message channels they are not in
     */
    public static final boolean operCanMsgChan = getFirstConfigOption(OPER_CANMSGCHAN).equalsIgnoreCase("true");

    /**
     * The pattern that nicknames must adhere to
     */
    public static final Pattern nickPattern = Pattern.compile(getConfigOption(NICK_PATTERN)[0]);

    /**
     * The minimum length of nicknames
     */
    public static final int minNickLength = Integer.parseInt(getFirstConfigOption(MIN_NICK_LENGTH));

    /**
     * The maximum length of nicknames
     */
    public static final int maxNickLength = Integer.parseInt(getFirstConfigOption(MAX_NICK_LENGTH));

    /**
     * This filter returns true for all connections
     */
    public static final Filter passAllFilter = new Filter() {

        @Override
        public boolean pass(Object o) {
            return true;
        }
    };

    /**
     * The motd sent to a connected client after the registration has been sent
     */
    public static final String motd = readMOTD();

    /**
     * Returns the lowercase version of <code>string</code> with the
     * @param string
     * @return
     */
    public static String toLowerCase(String string) {

        return string.toLowerCase().replace('[', '{').replace(']', '}').replace('\\', '|').replace('~', '^');
    }

    public static boolean stringsEqual(String first, String second) {


        if (first == second) {
            return true;
        }

        if ((first == null && second != null) || (first != null && second == null)) {
            return false;
        }


        if (first.length() != second.length()) {
            return false;
        }

        return toLowerCase(first).equals(toLowerCase(second));
    }

    /**
     * Removes all non-printable characters from a string, as well as any : or ,<br />
     * Solution provided by ratchet freak (http://stackoverflow.com/a/7161653)
     * @param s The string to clean
     * @return The cleaned string
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




    /**
     * Returns the content of the MOTD file, or <code>null</code> if no MOTD file is present
     * @return The content of the MOTD file
     */
    private static String readMOTD() {

        File motdFile = new File("./configs/motd");

        if (!motdFile.exists()) {

            logger.warn("No MOTD file found.");
            return null;
        }

        try {

            return StreamUtils.readFully(new BufferedReader(new InputStreamReader(new FileInputStream(motdFile))));
        } catch (IOException ex) {

            logger.error("Error while reading from MOTD file", ex);
            return null;
        }
    }
}
