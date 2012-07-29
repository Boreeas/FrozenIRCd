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

import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;

/**
 *
 * @author Boreeas
 */
public enum Command {

    //<editor-fold defaultstate="collapsed" desc="Commands">
    /**
     * Sent as a reply to a PING request.<br />
     * Parameters: Server hostname, ping argument
     */
    PONG                    ("PONG %s :%s", 2),

    /**
     * Sent as alive check.<br />
     * Parameters: ping argument
     */
    PING                    ("PING :%s", 1),

    /**
     * Sent as a reply when a client successfully joined a channel.<br />
     * Parameters: channel
     */
    JOIN                    ("JOIN %s", 1),

    /**
     * Sent as a reply when a client parts a channel.<br />
     * Parameters: channel, reason
     */
    PART                    ("PART %s :%s", 2),

    /**
     * Sent when a mode change occurs in a room.<br />
     * Parameters: channel, +/-, mode, mode param
     */
    MODE                    ("MODE %s %s%s %s", 4),

    /**
     * Sent when a client changes their nickname.<br />
     * Parameters: new nick
     */
    NICK                    ("NICK %s", 1),

    /**
     * Sent when a client is invited to a channel.<br />
     * Parameters: invited nick, channel
     */
    INVITE                  ("INVITE %s %s", 2),

    /**
     * Sent when a client disconnects.<br />
     * Parameters: reason
     */
    QUIT                    ("QUIT " + ConfigData.getFirstConfigOption(ConfigKey.HOST) + " :%s", 1),

    /**
     * Sent when a client is kicked from a room.<br />
     * Parameters: nick, channel, reason
     */
    KICK                    ("KICK %s %s :%s", 3),

    /**
     * Private message to a channel.
     * Parameters: channel, message
     */
    PRIVMSG                 ("PRIVMSG %s :%s", 2);
    //</editor-fold>


    private String message;
    private int numOfParams;

    private Command(String message, int numOfParams) {

        this.message = message;
        this.numOfParams = numOfParams;
    }

    public String getMessage() {
        return message;
    }

    public int getNumOfParams() {
        return numOfParams;
    }


    public String format(Object... args) {

        if (args.length < numOfParams) {

            throw new IllegalArgumentException(String.format("Not enough parameters to format reply '%s' (Needed: %s, Got: %s)", message, numOfParams, args.length));
        }

        return String.format(message, (Object[]) args);
    }
}
