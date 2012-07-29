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
package net.boreeas.frozenircd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.boreeas.frozenircd.command.Command;
import net.boreeas.frozenircd.command.Mode;
import net.boreeas.frozenircd.command.Reply;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.connection.Connection;
import net.boreeas.frozenircd.utils.Filter;
import net.boreeas.frozenircd.connection.client.Client;
import net.boreeas.frozenircd.utils.PatternMatcher;
import net.boreeas.frozenircd.utils.SharedData;
import net.boreeas.frozenircd.utils.StringUtils;

/**
 *
 * @author Boreeas
 */
public class Channel implements Flagable {

    public static final String NO_TOPIC = "";

    private static final char DISPLAY_VOICE  = '+';
    private static final char DISPLAY_OP     = '@';


    /**
     * The name for the channel.
     */
    private final String name;

    /**
     * The current topic for the channel.
     */
    private String topic;

    /**
     * The time the topic was set
     */
    private long topicSetTime = System.currentTimeMillis();

    /**
     * The set channel modes.
     */
    private final Map<Character, String> channelmodes = new HashMap<>();

    private final Set<ModeListEntry> ops = new HashSet<>();
    private final Set<ModeListEntry> voiced = new HashSet<>();
    private final Set<ModeListEntry> muted = new HashSet<>();
    private final Set<ModeListEntry> banned = new HashSet<>();
    private final Set<ModeListEntry> invited = new HashSet<>();
    private final Set<ModeListEntry> excepted = new HashSet<>();

    /**
     * The clients that have currently joined the room.
     */
    private final Set<Client> clients = new HashSet<>();

    // Thread locks
    private final Object clientLock = new Object();
    private final Object modeLock = new Object();

    /**
     * Creates a new channel with given name.
     * @param name The name of the channel
     */
    public Channel(final String name) {

        this.name = name;
    }

    public void sendToAll(Reply reply, Object... args) {

        // Reserver first for client's nickname
        Object[] actualArgs = new Object[args.length + 1];
        System.arraycopy(args, 0, actualArgs, 1, args.length);

        synchronized (clientLock) {
            for (Client client: clients) {

                actualArgs[0] = client.getNickname();
                client.sendStandardFormat(reply.format(actualArgs));
            }
        }
    }

    /**
     * Sends a message to all clients, appearing to originate from the given client
     * @param client The client who sent the message
     * @param message The message to send
     */
    public void sendFromClient(final Client client, final String message) {

        sendFromClient(client, message, SharedData.passAllFilter);
    }

    /**
     * Send a message to all clients passing through the filter, appearing to originate from the given client
     * @param client The client who sent the message
     * @param message The message to send
     * @param filter The filter to determine which clients receive the message
     */
    public void sendFromClient(final Client client, final String message, final Filter<Connection> filter) {

        final String actualMessage = ":" + client.getDisplayHostmask() + " " + message;

        synchronized (clientLock) {
            for (final Client other: clients) {

                if (filter.pass(other)) {

                    other.send(actualMessage);
                }
            }
        }
    }

    public void joinChannel(final Client client) {

        if (isEmpty()) {
            op(client.getHostmask(), ConfigData.getFirstConfigOption(ConfigKey.HOST));
        }

        synchronized (clientLock) {
            clients.add(client);
        }

        sendFromClient(client, Command.JOIN.format(this.name));

        if (topic != null) {
            client.sendStandardFormat(Reply.RPL_TOPIC.format(client.getSafeNickname(), this.name, this.topic));
        }

        client.sendStandardFormat(Reply.RPL_NAMREPLY.format(client.getNickname(), '=', name, names()));
    }

    public int size() {
        return clients.size();
    }

    public void partChannel(final Client client, String reason) {

        sendFromClient(client, Command.PART.format(name, reason));

        removeUser(client);
    }

    public void kick(Client issuer, Client target, String reason) {

        sendFromClient(issuer, Command.KICK.format(name, target.getNickname(), reason));

        removeUser(target);
    }

    private void removeUser(Client target) {

        synchronized (clientLock) {
            clients.remove(target);
        }

        synchronized (this) {
            deop(target.getHostmask());
            devoice(target.getHostmask());
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {

        this.topic = topic;
    }

    public String getName() {

        return name;
    }

    public boolean isEmpty() {

        return clients.isEmpty();
    }

    @Override
    public String flags() {

        synchronized (modeLock) {
            return StringUtils.joinIterable(channelmodes.keySet(), "");
        }
    }

    @Override
    public void removeFlag(char flag) {

        synchronized (modeLock) {
            channelmodes.remove(flag);
        }
    }

    @Override
    public void addFlag(char flag, String param) {

        synchronized (modeLock) {
            channelmodes.put(flag, param);
        }
    }

    @Override
    public boolean hasFlag(char flag) {

        return channelmodes.containsKey(flag);
    }

    @Override
    public String flagParams() {

        StringBuilder builder = new StringBuilder();

        synchronized (modeLock) {
            for (String param: channelmodes.values()) {
                if (param != null) builder.append(param);
            }
        }

        return builder.toString();
    }

    @Override
    public String getParam(char flag) {

        return channelmodes.get(flag);
    }




    /**
     * Returns the reply to the NAMES request
     * @return the names of the people in the channel
     */
    public String names() {

        StringBuilder builder = new StringBuilder();

        synchronized (clientLock) {
            for (Client client: clients) {

                if (builder.length() > 0) builder.append(' ');

                if (isOp(client))          builder.append(DISPLAY_OP);
                else if (isVoiced(client)) builder.append(DISPLAY_VOICE);

                builder.append(client.getNickname());
            }
        }

        return builder.toString();
    }

    /**
     * Returns the reply to the NAMES request for users outside the channel
     * @return the names of the users without Mode.UMODE_INVISIBLE
     */
    public String visibleNames() {

        StringBuilder builder = new StringBuilder();

        synchronized (clientLock) {
            for (Client client: clients) {
                if (!client.hasFlag(Mode.UMODE_INVISIBLE)) {

                    if (builder.length() > 0) builder.append(' ');

                    if (isOp(client))          builder.append(DISPLAY_OP);
                    else if (isVoiced(client)) builder.append(DISPLAY_VOICE);

                    builder.append(client.getNickname());
                }
            }
        }

        return builder.toString();
    }



    public boolean isOp(Client client) {
        return checkAccess(ops, client.getHostmask());
    }

    public boolean isVoiced(Client client) {
        return checkAccess(voiced, client.getHostmask()) || isOp(client); // Op implies voice
    }

    public boolean isMuted(Client client) {
        return checkAccess(muted, client.getHostmask()) || isBanned(client); // Ban implies mute
    }

    public boolean isBanned(Client client) {
        return checkAccess(banned, client.getHostmask());
    }

    public boolean isInvited(Client client) {
        return checkAccess(invited, client.getNickname());
    }

    public boolean isExcepted(Client client) {
        return checkAccess(excepted, client.getHostmask());
    }

    private synchronized boolean checkAccess(Set<ModeListEntry> access, String mask) {

        for (ModeListEntry entry: access) {
            if (PatternMatcher.matchGlob(entry.entry, mask)) {
                return true;
            }
        }

        return false;
    }

    public synchronized void op(String mask, String issuerMask) {
        ops.add(new ModeListEntry(mask, issuerMask));
    }

    public synchronized void deop(String mask) {
        ops.remove(new ModeListEntry(mask, mask));
    }

    public synchronized void voice(String mask, String issuerMask) {
        voiced.add(new ModeListEntry(mask, issuerMask));
    }

    public synchronized void devoice(String mask) {
        voiced.remove(new ModeListEntry(mask, mask));
    }

    public synchronized void mute(String mask, String issuerMask) {
        muted.add(new ModeListEntry(mask, issuerMask));
    }

    public synchronized void unmute(String mask) {
        muted.remove(new ModeListEntry(mask, mask));
    }

    public synchronized void ban(String mask, String issuerMask) {
        banned.add(new ModeListEntry(mask, issuerMask));
    }

    public synchronized void unban(String mask) {
        banned.remove(new ModeListEntry(mask, mask));
    }

    public synchronized void invite(String nick, String issuerMask) {
        invited.add(new ModeListEntry(nick, issuerMask));
    }

    public synchronized void uninvite(String nick) {
        invited.remove(new ModeListEntry(nick, nick));
    }

    public synchronized void except(String mask, String issuerMask) {
        excepted.add(new ModeListEntry(mask, issuerMask));
    }

    public synchronized void unexcept(String mask) {
        invited.remove(new ModeListEntry(mask, mask));
    }

    public Set<String> operList() {
        return list(ops);
    }

    public Set<String> voiceList() {
        return list(voiced);
    }

    public Set<String> inviteList() {
        return list(invited);
    }

    public Set<String> muteList() {
        return list(muted);
    }

    public Set<String> banList() {
        return list(banned);
    }

    public Set<String> exceptList() {
        return list(excepted);
    }

    private synchronized Set<String> list(Set<ModeListEntry> accessSet) {

        Set<String> res = new HashSet<>();

        for (ModeListEntry entry: accessSet) {
            res.add(entry.format());
        }

        return res;
    }


    public static boolean isChanTypeSupported(char chantype) {
        return chantype == '#';
    }
}
