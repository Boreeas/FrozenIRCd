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

/**
 *
 * @author Boreeas
 */
public enum Reply {
    
    //<editor-fold defaultstate="collapsed" desc="RPL - Standard replies">
    /**
     * Sent to indicate a successful umode change.<br />
     * Parameters: nick, nick, flag string
     */
    RPL_UMODEIS             ("221 %s :Usermode for %s is +%s", 3),
    
    /**
     * Sent to indicate a successfully executed OPER command.<br />
     * Parameters: nick
     */
    RPL_YOUREOPER           ("381 %s :Successfully set OPER status", 1),
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="ERR - Error messages">
    /**
     * Sent if a command by a client was not recognized.<br />
     * Parameters: nick, command
     */
    ERR_UNKNOWNCOMMAND      ("421 %s :%s: Unknown command", 2),
    
    /**
     * Sent whenever a command expects a nickname as a parameter but is not given any.<br />
     * Parameters: nick
     */
    ERR_NONICKNAMEGIVEN     ("431 %s :No nickname given", 1),
    
    /**
     * Sent when a NICK command fails for a general reason.<br />
     * Parameters: nick, target nick, reason
     */
    ERR_ERRONEUSNICKNAME    ("432 %s :%s: Illegal nickname: %s", 3),
    
    /**
     * Sent when a NICK command fails because the target nick is already in use.<br />
     * Parameters: nick, target nick
     */
    ERR_NICKNAMEINUSE       ("433 %s :Nickname already in use: %s", 2),
    
    /**
     * Sent when a command expects more parameters than were given by the client.<br />
     * Parameters: nick, command, expected parameters
     */
    ERR_NEEDMOREPARAMS      ("461 %s :%s: Not enough parameters. Parameters: %s", 3),
    
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
     * Sent when an OPER-command is used by a non-OPER.<br />
     * Parameters: nick
     */
    ERR_NOTOPER             ("999 %s :Oper status is needed", 1),
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="OTHER - Not really numerics, but often used anyways">
    /**
     * Sent as a reply to a PING request.<br />
     * Parameters: Server hostname, ping argument
     */
    OTHER_PONG              ("PONG %s :%s", 2),
    
    /**
     * Sent as alive check.<br />
     * Parameters: ping argument
     */
    OTHER_PING              ("PING :%s", 1);
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
