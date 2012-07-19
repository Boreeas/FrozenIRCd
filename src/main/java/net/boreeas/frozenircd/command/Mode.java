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

import net.boreeas.frozenircd.Channel;
import net.boreeas.frozenircd.Flagable;
import net.boreeas.frozenircd.connection.client.Client;
import net.boreeas.frozenircd.utils.SharedData;

/**
 *
 * @author Boreeas
 */
public class Mode {
    
    //<editor-fold defaultstate="collapsed" desc="umodes">
    public static final String UMODES = "ior";
    
    public static final char UMODE_INVISIBLE    = 'i';
    public static final char UMODE_OPER         = 'o';
    public static final char UMODE_REGISTERED   = 'r';
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="cmodes">
    public static final String CMODES = "";
    //</editor-fold>
    
    
    public static void handleModeChange(char mode, Client user, Flagable target, boolean adding, String[] args) {
        
        if (target instanceof Client) {
            
            // Only opers can set modes for other users
            if (user != target && (!user.hasFlag(UMODE_OPER) || !SharedData.operCanSetModes)) {
                user.sendStandardFormat(Reply.ERR_USERSDONTMATCH.format(user.getNickname()));
                return;
            }
            
            if (mode == UMODE_OPER)             processUmodeOper(user, target, adding);
            else if (mode == UMODE_INVISIBLE)   processUmodeInvisible(target, adding);
            else if (mode == UMODE_REGISTERED)  processUmodeRegistered(user, target, adding);
            
            else {
                user.sendStandardFormat(Reply.ERR_UMODEUNKNOWNFLAG.format(user.getNickname(), mode));
            }
        } else if (target instanceof Channel) {
            Channel chan = (Channel) target;
            
            // Only opers can set modes for channels they are not in
            if (!user.isInChannel(chan.getName()) && (!user.hasFlag(UMODE_OPER) || !SharedData.operCanSetModes)) {
                user.sendStandardFormat(Reply.ERR_NOTONCHANNEL.format(user.getNickname(), chan.getName()));
                return;
            }
            
            
        } else {
            
            SharedData.logger.error("Unhandled Mode Target '" + target + "' at:");
            
            for (StackTraceElement trace: Thread.currentThread().getStackTrace()) {
                SharedData.logger.error(trace.toString());
            }
        }
        
        
    }
    
    private static void processUmodeOper(Client user, Flagable target, boolean adding) {
        
        if (adding) {
            user.sendStandardFormat(Reply.ERR_CANTSET.format(user.getNickname(), UMODE_OPER));
            return;
        }
        
        target.removeFlag(UMODE_OPER);
    }
    
    private static void processUmodeInvisible(Flagable target, boolean adding) {
        
        if (adding) {
            target.addFlag(UMODE_INVISIBLE);
        } else {
            target.removeFlag(UMODE_INVISIBLE);
        }
    }
    
    private static void processUmodeRegistered(Client user, Flagable target, boolean adding) {
        
        if (adding) {
            user.sendStandardFormat(Reply.ERR_CANTSET.format(user.getNickname(), UMODE_REGISTERED));
            return;
        } else {
            target.removeFlag(UMODE_REGISTERED);
        }
    }
}