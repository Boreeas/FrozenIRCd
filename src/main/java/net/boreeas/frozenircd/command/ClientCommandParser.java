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

import com.sun.tracing.dtrace.ArgsAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import net.boreeas.frozenircd.Channel;
import net.boreeas.frozenircd.ChannelPool;
import net.boreeas.frozenircd.Flagable;
import net.boreeas.frozenircd.Server;
import net.boreeas.frozenircd.config.IncompleteConfigurationException;
import net.boreeas.frozenircd.connection.Connection;
import net.boreeas.frozenircd.connection.ConnectionPool;
import net.boreeas.frozenircd.connection.client.Client;
import net.boreeas.frozenircd.utils.Filter;
import net.boreeas.frozenircd.utils.HashUtils;
import net.boreeas.frozenircd.utils.PatternMatcher;
import net.boreeas.frozenircd.utils.SharedData;
import net.boreeas.frozenircd.utils.StringUtils;

import org.ietf.jgss.ChannelBinding;
import sun.awt.GlobalCursorManager;
import static net.boreeas.frozenircd.command.Reply.*;
import static net.boreeas.frozenircd.config.ConfigData.*;
import static net.boreeas.frozenircd.config.ConfigKey.*;

/**
 *
 * @author Boreeas
 */
public class ClientCommandParser {

    private ClientCommandParser() {}

    private static final String DIE = "DIE";
    private static final String PING = "PING";
    private static final String PONG = "PONG";
    private static final String USER = "USER";
    private static final String NICK = "NICK";
    private static final String PASS = "PASS";
    private static final String OPER = "OPER";
    private static final String MODE = "MODE";
    private static final String QUIT = "QUIT";
    private static final String STOP = "STOP";
    private static final String JOIN = "JOIN";
    private static final String PART = "PART";
    private static final String LIST = "LIST";
    private static final String KICK = "KICK";
    private static final String TOPIC = "TOPIC";
    private static final String NAMES = "NAMES";
    private static final String INVITE = "INVITE";
    private static final String PRIVMSG = "PRIVMSG";


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

            case DIE:
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

            case LIST:
                onListCommand(client, args);
                break;

            case INVITE:
                onInviteCommand(client, args);
                break;

            case KICK:
                onKickCommand(client, args);
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

        client.sendStandardFormat("PONG :" + args[0]);
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


            client.broadcastToChannels(Command.NICK.format(args[0]));
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
        if (Channel.isChanTypeSupported(args[0].charAt(0))) {

            target = ChannelPool.getChannel(args[0]);

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

                argIndex = Mode.handleModeChange(c, client, target, adding, args, argIndex);
            }
        }
    }

    private static void onStopCommand(Client client, String[] args) {

        if (client.hasFlag(Mode.UMODE_OPER)) {

            String reason = "No reason given";
            if (args.length > 0) {
                reason = StringUtils.joinArray(args);
            }

            String stop = "Server shutting down (" + STOP + " command invoked by " + client.getNickname()
                        + "(" + client.getHostmask() + ") (Reason: " + reason + ")";
            ConnectionPool.ALL.notifyClients(stop);

            Server.INSTANCE.close();
        } else {

            client.sendStandardFormat(Reply.ERR_NOPRIVILEGES.format(client.getSafeNickname()));
        }
    }

    @SuppressWarnings("unchecked")
    private static void onJoinCommand(Client client, String[] args) {

        if (!client.registrationCompleted()) {
            return;
        }    // Drop

        if (args.length < 1) {
            client.sendStandardFormat(ERR_NEEDMOREPARAMS.format(client.getNickname(),
                                                                JOIN, "<channel> [password]"));
            return;
        }

        boolean partAll = false;

        for (String chan: args[0].split(",")) {

            if (chan.equals("0")) {
                partAll = true;
            } else {
                joinSingleChannel(client, chan);
            }
        }

        if (partAll) {
            for (String channel: client.getChannels(SharedData.passAllFilter)) {
                partSingleChannel(client, channel, client.getNickname());
            }
        }
    }

    private static void onPartCommand(Client client, String[] args) {

        if (!client.registrationCompleted()) {
            return; // Drop silently
        }

        if (args.length < 1) {

            client.sendStandardFormat(Reply.ERR_NEEDMOREPARAMS.format(client.getNickname(), PART, "<channel> [reason]"));
            return;
        }

        String reason = client.getNickname();
        if (args.length >= 2) {
            reason = args[1];
        }

        for (String chanName: args[0].split(",")) {
            partSingleChannel(client, chanName, reason);
        }
    }

    private static void onTopicCommand(Client client, String[] args) {

        if (!client.registrationCompleted()) {
            return;
        }

        if (args.length < 1) {
            client.sendStandardFormat(ERR_NEEDMOREPARAMS.format(client.getNickname(),
                                                                TOPIC, "<channel> [topic]"));
            return;
        }

        Channel chan = ChannelPool.getChannel(args[0]);

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
            if (topic.isEmpty()) {
                topic = null;
            }

            chan.setTopic(topic);
            chan.sendFromClient(client, TOPIC + " " + args[0] + " :" + args[1]);
        }
    }

    private static void onPrivmsgCommand(Client client, final String[] args) {

        if (!client.registrationCompleted()) {
            return;
        }

        if (args.length < 2 || args[1].isEmpty()) {
            client.sendStandardFormat(Reply.ERR_NEEDMOREPARAMS.format(client.getNickname(),
                                                                      PRIVMSG, "<target> <message>"));
            return;
        }

        if (Channel.isChanTypeSupported(args[0].charAt(0))) {
            privmsgChannel(client, args[0], args[1]);
        } else {
            privmsgConnections(client, args[0], args[1]);
        }
    }

    private static void onNamesCommand(Client client, String[] args) {

        if (!client.registrationCompleted()) {
            return;
        }

        if (args.length == 0) {

            @SuppressWarnings("unchecked")
            Set<Channel> results = ChannelPool.getChannels(SharedData.passAllFilter);
            for (Channel chan: results) {

                String names = (client.isInChannel(chan.getName())) ? chan.names() : chan.visibleNames();
                if (names.isEmpty()) continue;

                client.sendStandardFormat(Reply.RPL_NAMREPLY.format(client.getNickname(), '=', chan.getName(), names));
            }

        } else {

            for (String name: args[0].split(",")) {
                Channel chan = ChannelPool.getChannel(name);
                if (chan == null) continue;

                String names = (client.isInChannel(args[0])) ? chan.names() : chan.visibleNames();
                if (names.isEmpty()) continue;

                client.sendStandardFormat(Reply.RPL_NAMREPLY.format(client.getNickname(), '=', chan.getName(), names));
                client.sendStandardFormat(Reply.RPL_ENDOFNAMES.format(client.getNickname(), chan.getName()));
            }
        }
    }

    private static void onListCommand(final Client client, String[] args) {

        if (!client.registrationCompleted()) {
            return;
        }

        Filter<Channel> chanFilter;

        if (args.length == 0) {

            chanFilter = new Filter<Channel>() {

                @Override
                public boolean pass(Channel chan) {
                    return !chan.hasFlag(Mode.CMODE_SECRET) || client.isInChannel(chan.getName());
                }
            };
        } else {

            final String[] channels = args[0].split(",");

            chanFilter = new Filter<Channel>() {

                @Override
                public boolean pass(Channel chan) {

                    if (chan.hasFlag(Mode.CMODE_SECRET) && !client.isInChannel(chan.getName())) {
                        return false;
                    }

                    for (String name: channels) {
                        if (PatternMatcher.matchGlob(name, chan.getName())) {
                            return true;
                        }
                    }

                    return false;
                }
            };
        }

        client.sendStandardFormat(Reply.RPL_LISTSTART.format(client.getNickname()));

        for (Channel chan: ChannelPool.getChannels(chanFilter)) {

            String reply = Reply.RPL_LIST.format(client.getNickname(), chan.getName(), chan.size(), chan.getTopic());
            client.sendStandardFormat(reply);
        }

        client.sendStandardFormat(Reply.RPL_LISTEND.format(client.getNickname()));
    }

    private static void onInviteCommand(Client client, final String[] args) {

        if (!client.registrationCompleted()) {
            return;
        }

        if (args.length < 2) {
            String reply = ERR_NEEDMOREPARAMS.format(client.getNickname(), INVITE, "<user> <channel>");
            client.sendStandardFormat(reply);
            return;
        }

        final String channel = args[1];
        final String nick = args[0];

        Channel chan = ChannelPool.getChannel(channel);
        Set<Connection> targets = ConnectionPool.ALL.getConnections(new Filter<Connection>() {

            @Override
            public boolean pass(Connection instance) {
                return SharedData.stringsEqual(nick, instance.getCommonName());
            }
        });

        if (targets.isEmpty() || !(targets.toArray()[0] instanceof Client)) {
            client.sendStandardFormat(ERR_NOSUCHNICK.format(client.getNickname(), nick));
            return;
        }

        Client target = (Client) targets.toArray()[0];

        if (chan != null) {

            if (chan.hasFlag(Mode.CMODE_INVITEONLY) && !chan.isOp(client)) {
                client.sendStandardFormat(ERR_CHANOPRIVSNEEDED.format(client.getNickname(), channel));
                return;
            }

            if (!client.isInChannel(chan.getName())) {
                client.sendStandardFormat(ERR_NOTONCHANNEL.format(client.getNickname(), channel));
                return;
            }

            chan.invite(target.getNickname(), client.getDisplayHostmask());
        }

        if (target.hasFlag(Mode.UMODE_AWAY)) {
            String awaymsg = target.getParam(Mode.UMODE_AWAY);
            String awayInfo = RPL_AWAY.format(client.getNickname(), nick, awaymsg);
            client.sendStandardFormat(awayInfo);
        }

        client.sendStandardFormat(RPL_INVITING.format(client.getNickname(), nick, channel));
        target.sendFromUser(client, Command.INVITE.format(nick + " " + channel));
    }

    private static void onKickCommand(Client client, String[] args) {

        if (args.length < 2) {

            String reply = ERR_NEEDMOREPARAMS.format(client.getNickname(), KICK, "<channel> <user> [reason]");
            client.sendStandardFormat(reply);
            return;
        }

        String[] channels = args[0].split(",");
        String[] targets = args[1].split(",");

        if (channels.length == 1) {

            for (String target: targets) {

                String reason = (args.length >= 3) ? args[2] : target;
                kickSingleUser(client, target, channels[0], reason);
            }
        } else if (channels.length == targets.length) {

            for (int i = 0; i < targets.length; i++) {

                String reason = (args.length >= 3) ? args[2] : targets[i];
                kickSingleUser(client, targets[i], channels[i], reason);
            }
        } else {

            String reply = ERR_NEEDMOREPARAMS.format(client.getNickname(), KICK,
                                                     "Need exactly one or as many channels as targets");
            client.sendStandardFormat(reply);
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

    @SuppressWarnings("unchecked")
    private static boolean isNameInUse(Client client, String name) {

        for (Connection conn : ConnectionPool.ALL.getConnections(SharedData.passAllFilter)) {

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


    private static void joinSingleChannel(Client client, String chanName) {

        Channel channel = ChannelPool.getChannel(chanName);

        if (channel == null) {
            channel = new Channel(SharedData.toLowerCase(chanName));
            ChannelPool.addChannel(channel);
        }

        if (channel.isBanned(client)) {
            client.sendStandardFormat(ERR_BANNEDFROMCHAN.format(client.getNickname(), channel.getName()));
            return;
        }

        if (channel.hasFlag(Mode.CMODE_INVITEONLY) && !channel.isInvited(client)) {
            client.sendStandardFormat(ERR_INVITEONLYCHAN.format(client.getNickname(), channel.getName()));
            return;
        }

        channel.joinChannel(client);
        client.addChannel(channel.getName());
    }

    private static void partSingleChannel(Client client, String chanName, String reason) {

        if (!client.isInChannel(chanName)) {

            client.sendStandardFormat(Reply.ERR_NOTONCHANNEL.format(client.getNickname(), chanName));
            return;
        }

        client.removeChannel(chanName);
        ChannelPool.getChannel(chanName).partChannel(client, reason);
    }

    private static void kickSingleUser(Client client, final String nick, String channel, String reason) {

        Channel chan = ChannelPool.getChannel(channel);

        Set<Connection> targets = ConnectionPool.ALL.getConnections(new Filter<Connection>() {

            @Override
            public boolean pass(Connection instance) {
                return SharedData.stringsEqual(nick, instance.getCommonName());
            }
        });


        if (chan == null) {
            client.sendStandardFormat(ERR_NOSUCHCHANNEL.format(client.getNickname(), channel));
            return;
        }

        if (targets.isEmpty() || !(targets.toArray()[0] instanceof Client)) {
            // RFC2812 demands this instead of ERR_NOSUCHNICKNAME
            client.sendStandardFormat(ERR_USERNOTINCHANNEL.format(client.getNickname(), nick, channel));
            return;
        }

        Client target = (Client) targets.toArray()[0];

        if (!client.isInChannel(channel)) {
            client.sendStandardFormat(ERR_NOTONCHANNEL.format(client.getNickname(), channel));
            return;
        }

        if (!target.isInChannel(channel)) {
            client.sendStandardFormat(ERR_USERNOTINCHANNEL.format(client.getNickname(), nick, channel));
            return;
        }

        if (!chan.isOp(client)) {
            client.sendStandardFormat(ERR_CHANOPRIVSNEEDED.format(client.getNickname(), channel));
            return;
        }

        chan.kick(client, target, reason);
        target.removeChannel(channel);
    }

    private static void privmsgChannel(final Client client, String chanName, String message) {

        Channel target = ChannelPool.getChannel(chanName);

        if (target == null) {
            client.sendStandardFormat(ERR_NOSUCHCHANNEL.format(client.getNickname(), chanName));
            return;
        }

        if (!client.isInChannel(chanName)
            || (target.hasFlag(Mode.CMODE_MODERATED) && !target.isVoiced(client))
            || (target.isMuted(client) && !target.isVoiced(client))) {

            client.sendStandardFormat(ERR_CANNOTSENDTOCHANNEL.format(client.getNickname(), chanName));
            return;
        }

        target.sendFromClient(client, Command.PRIVMSG.format(chanName, message), new Filter<Connection>() {

            @Override
            public boolean pass(Connection instance) {
                return instance != client;
            }
        });
    }

    private static void privmsgConnections(Client client, final String targetName, String message) {

        Set<Connection> targets = ConnectionPool.ALL.getConnections(new Filter<Connection>() {

            @Override
            public boolean pass(Connection instance) {
                return PatternMatcher.matchGlob(targetName, instance.getCommonName());
            }
        });

        if (targets.size() > 1) {
            client.sendStandardFormat(ERR_TOOMANYTARGETS.format(client.getNickname(), targetName));
            return;
        }

        boolean foundMatch = false;
        for (Connection conn: targets) {

            if (conn instanceof Client) {

                foundMatch = true;
                ((Client) conn).sendStandardFormat(Command.PRIVMSG.format(targetName, message));
            }
        }

        if (!foundMatch) {
            client.sendStandardFormat(ERR_NOSUCHNICK.format(client.getNickname(), targetName));
        }
    }
}
