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

    public static final char NO_FLAG = '0';

    //<editor-fold defaultstate="collapsed" desc="umodes">
    public static final String UMODES = "ior";

    public static final char UMODE_INVISIBLE    = 'i';
    public static final char UMODE_OPER         = 'o';
    public static final char UMODE_REGISTERED   = 'r';
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="cmodes">
    public static final String CMODES = "is";

    public static final char CMODE_SECRET = 's';
    public static final char CMODE_INVITEONLY = 'i';
    //</editor-fold>


    public static int handleModeChange(char mode, Client user, Flagable target, boolean adding, String[] args, int argIndex) {

        if (target instanceof Client) {

            // Only opers can set modes for other users
            if (user != target && (!user.hasFlag(UMODE_OPER) || !SharedData.operCanSetModes)) {
                user.sendStandardFormat(Reply.ERR_USERSDONTMATCH.format(user.getNickname()));
                return argIndex;
            }

            if (mode == UMODE_OPER) {

                processUmodeOper(user, target, adding);
            } else if (mode == UMODE_INVISIBLE) {

                processUmodeInvisible(target, adding);
            }else if (mode == UMODE_REGISTERED) {

                processUmodeRegistered(user, target, adding);
            } else if (mode == NO_FLAG) {

                user.sendStandardFormat(Reply.RPL_UMODEIS.format(user.getNickname(), user.flags()));
            } else {

                user.sendStandardFormat(Reply.ERR_UMODEUNKNOWNFLAG.format(user.getNickname(), mode));
            }
        } else if (target instanceof Channel) {

            Channel chan = (Channel) target;

            // Only opers can set modes for channels they are not in
            if (!user.isInChannel(chan.getName()) && (!user.hasFlag(UMODE_OPER) || !SharedData.operCanSetModes)) {
                user.sendStandardFormat(Reply.ERR_NOTONCHANNEL.format(user.getNickname(), chan.getName()));
                return argIndex;
            }

            if (mode == CMODE_SECRET) {

                processCmodeSecret(user, chan, adding);
            } else if (mode == CMODE_INVITEONLY) {

                processCmodeInviteOnly(user, chan, adding);
            } else if (mode == NO_FLAG) {

                String reply = Reply.RPL_CHANNELMODEIS.format(user.getNickname(), chan.getName(),
                                                              chan.flags(), chan.flagParams());
                user.sendStandardFormat(Reply.RPL_CHANNELMODEIS.format(reply));
            } else {

                user.sendStandardFormat(Reply.ERR_UNKNOWNMODE.format(user.getNickname(), mode));
            }
        } else {

            SharedData.logger.error("Unhandled Mode Target '" + target + "' at:");

            for (StackTraceElement trace: Thread.currentThread().getStackTrace()) {
                SharedData.logger.error(trace.toString());
            }
        }

        return argIndex;
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
            target.addFlag(UMODE_INVISIBLE, null);
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




    // Cmodes
    private static void processCmodeSecret(Client user, Channel target, boolean adding) {

        defaultAddCmode(user, target, CMODE_SECRET, null, adding);
    }

    private static void processCmodeInviteOnly(Client user, Channel target, boolean adding) {

        defaultAddCmode(user, target, CMODE_INVITEONLY, null, adding);
    }


    private static void defaultAddCmode(Client user, Channel target, char flag, String param, boolean adding) {

        String reply;

        if (!user.isInChannel(target.getName())) {
            reply = Reply.ERR_NOTONCHANNEL.format(user.getNickname(), target.getName());
            user.sendStandardFormat(reply);
            return;
        }

        if (!target.isOp(user)) {
            reply = Reply.ERR_CHANOPRIVSNEEDED.format(user.getNickname(), target.getName());
            user.sendStandardFormat(reply);
            return;
        }

        if (adding) {
            target.addFlag(flag, param);
        } else {
            target.removeFlag(flag);
        }

        reply = Reply.RPL_CHANNELMODEIS.format(user.getNickname(), target.getName(),
                                                   target.flags(), target.flagParams());

        // TODO figure out mode format for channel notifications
    }
}
