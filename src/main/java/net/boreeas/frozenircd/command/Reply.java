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

import java.util.Date;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.utils.SharedData;

/**
 *
 * @author Boreeas
 */
public enum Reply {
    
    //<editor-fold defaultstate="collapsed" desc="RPL - Standard replies">
    /**
     * Sent when a client finishes the registration.<br />
     * Parameters: nick, hostmask
     */
    RPL_WELCOME             ("001 %s :Welcome to the FrozenIRCd Test Network, %s", 2),
    
    /**
     * Sent when a client finishes registration to inform them about the server they connect to.<br />
     * Parameters: nick, server host
     */
    RPL_YOURHOST            ("002 %s :Your host is " + ConfigData.getFirstConfigOption(ConfigKey.HOST) + 
                                     ", running FrozenIRCd " + SharedData.BUILD_IDENTIFIER, 1),
    
    /**
     * Sent when a client finishes registration to inform them about the date the server was created.<br />
     * Parameters: nick
     */
    RPL_CREATED             ("003 %s :This server is running since " + new Date(), 1),
    
    /**
     * Sent when a client finished registration to give them further information about the server.<br />
     * Parameters: nick
     */
    RPL_MYINFO              ("004 %s " + ConfigData.getFirstConfigOption(ConfigKey.HOST) + 
                                   " " + SharedData.BUILD_IDENTIFIER + 
                                   " " + Mode.UMODES + " " + Mode.CMODES, 1),
    
    /**
     * Sent when a client finished registration to give them further information about the server.<br />
     * Parameters: nick
     */
    RPL_ISUPPORT            ("005 %s", 1),
    
    /**
     * Sent to indicate a successful umode change.<br />
     * Parameters: nick, flag string
     */
    RPL_UMODEIS             ("221 %s :+%s", 2),
    
    /**
     * Sent as the first line of a LIST reply.<br />
     * Parameters: nick
     */
    RPL_LISTSTART           ("321 %s Channels :Users Name", 1),
    
    /**
     * Sent as channel info in a LIST reply.<br />
     * Parameters: nick, channel, number of users, topic
     */
    RPL_LIST                ("322 %s %s %s :%s", 4),
    
    /**
     * Sent as the last line of a LIST reply.<br />
     * Parameters: nick
     */
    RPL_LISTEND             ("323 %s :End of LIST", 1),
    
    /**
     * Sent to inform the client of the mode flags set for a channel.<br />
     * Parameters: nick, channel name, flags, flag params
     */
    RPL_CHANNELMODEIS       ("324 %s %s +%s %s", 4),
    
    /**
     * Sent on a TOPIC if no topic has been set.<br />
     * Parameters: nick, channel
     */
    RPL_NOTOPIC             ("331 %s %s: No topic has been set", 2),
    
    /**
     * Sent on a channel join or on a TOPIC.<br />
     * Parameters: nick, channel, topic
     */
    RPL_TOPIC               ("332 %s %s :%s", 3),
    
    /**
     * Sent on channel join or on a NAMES, containing the nicks of the people in the given channel.<br />
     * Parameters: nick, symbol, channel, space separated list of nicks
     */
    RPL_NAMREPLY            ("353 %s %s %s :%s", 4),
    
    /**
     * Sent to indicate the end of a NAMES listing.<br />
     * Parameters: nick, channel
     */
    RPL_ENDOFNAMES          ("366 %s %s: End of NAMES", 2),
    
    /**
     * Sent when a completes registration containing the MOTD of the server.<br />
     * Parameters: nick, (part of the) MOTD
     */
    RPL_MOTD                ("372 %s :- %s", 2),
    
    /**
     * Signifies the beginning of the MOTD.<br />
     * Parameters: nick
     */
    RPL_MOTDSTART           ("375 %s :- " + ConfigData.getFirstConfigOption(ConfigKey.HOST) + " Message of the Day -", 1),  
    
    /**
     * Signifies the end of the MOTD.<br />
     * Parameters: nick
     */
    RPL_ENDOFMOTD           ("376 %s :" + ConfigData.getFirstConfigOption(ConfigKey.HOST) + " End of MOTD", 1),
    
    /**
     * Sent to indicate a successfully executed OPER command.<br />
     * Parameters: nick
     */
    RPL_YOUREOPER           ("381 %s :Successfully set OPER status", 1),
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="ERR - Error messages">
    /**
     * Sent if an error occurred that can not be resolved right now.<br />
     * Parameters: nick, command
     */
    ERR_UNKNOWNERROR        ("400 %s %s: Internal server error while processing command.", 2),
    
    /**
     * Sent if a command targets a channel that does not exist.<br />
     * Parameters: nick, channel
     */
    ERR_NOSUCHCHANNEL       ("403 %s %s :No such channel", 2),
    
    /**
     * Sent if a command by a client was not recognized.<br />
     * Parameters: nick, command
     */
    ERR_UNKNOWNCOMMAND      ("421 %s %s :Unknown command", 2),
    
    /**
     * Sent whenever a command expects a nickname as a parameter but is not given any.<br />
     * Parameters: nick
     */
    ERR_NONICKNAMEGIVEN     ("431 %s :No nickname given", 1),
    
    /**
     * Sent when a NICK command fails for a general reason.<br />
     * Parameters: nick, target nick, reason
     */
    ERR_ERRONEUSNICKNAME    ("432 %s %s :Illegal nickname: %s", 3),
    
    /**
     * Sent when a NICK command fails because the target nick is already in use.<br />
     * Parameters: nick, target nick
     */
    ERR_NICKNAMEINUSE       ("433 %s :%s", 2),
    
    
    /**
     * Sent whenever a client attempts to use a command on a channel they are not a member of.<br />
     * Parameters: nick, channel
     */
    ERR_NOTONCHANNEL        ("442 %s :Need to be in channel: %s", 2),
    
    /**
     * Sent when a command expects more parameters than were given by the client.<br />
     * Parameters: nick, command, expected parameters
     */
    ERR_NEEDMOREPARAMS      ("461 %s %s :Not enough parameters. Parameters: %s", 3),
    
    /**
     * Sent when a client attempts to re-register a connection, i.e. sending one
     * of (USER, SERVICE, SERVER) after registering as a user.<br />
     * Parameters: nick
     */
    ERR_ALREADYREGISTERED   ("462 %s :You may not reregister", 1),
    
    /**
     * Sent when a client attempts to use an incorrect password.<br />
     * Parameters: nick
     */
    ERR_PASSWDMISMATCH      ("464 %s :Invalid password", 1),
    
    /**
     * Send when a client attempts to set an unknown mode flag.<br />
     * Parameters: nick, flag
     */
    ERR_UNKNOWNMODE         ("472 %s :Unknown mode flag %s", 2),
    
    /**
     * Sent when an OPER-command is used by a non-OPER.<br />
     * Parameters: nick
     */
    ERR_NOPRIVILEGES        ("481 %s :Oper status is needed", 1),
    
    /**
     * Sent when a user tries to execute a command without sufficient
     * channel access.<br />
     * Parameters: nick, channel
     */
    ERR_CHANOPRIVSNEEDED    ("482 %s %s :Need to be a channel operator", 2),
    
    /**
     * Sent when a client whose host does not match any o-line attempts to use the OPER command.<br />
     * Parameters: nick
     */
    ERR_NOOPERHOST          ("491 %s :Your host did not match any o-line", 1),
    
    /**
     * Sent when a client attempts to set a mode that the server does not know or support.<br />
     * Parameters: nick, unknown mode
     */
    ERR_UMODEUNKNOWNFLAG    ("501 %s :Unknown mode flag %s", 2),
    
    /**
     * Sent when a client attempts to set umodes for a client other than themselves.<br />
     * Parameters: nick
     */
    ERR_USERSDONTMATCH      ("502 %s :Can't set mode on other user", 1),
    
    /**
     * Sent when a client attempts to manually set a mode which can only be set by a service or the server.<br />
     * Parameters: nick, flag
     */
    ERR_CANTSET             ("999 %s :Can't set mode manually: %s", 2);
    //</editor-fold>
    
    
    
    
    private String message;
    private int numOfParams;
    
    private Reply(String message, int numOfParams) {
        
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
