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
import net.boreeas.frozenircd.command.Reply;
import net.boreeas.frozenircd.connection.BroadcastFilter;
import net.boreeas.frozenircd.connection.client.Client;
import net.boreeas.frozenircd.utils.SharedData;
import net.boreeas.frozenircd.utils.StringUtils;

/**
 *
 * @author Boreeas
 */
public class Channel implements Flagable {
    
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
    
    private final Set<Client> ops = new HashSet<>();
    private final Set<Client> voices = new HashSet<>();
    private final Set<Client> muted = new HashSet<>();
    private final Set<Client> banned = new HashSet<>();
    
    /**
     * The clients that have currently joined the room.
     */
    private final Set<Client> clients = new HashSet<>();
    
    /**
     * Creates a new channel with given name.
     * @param name The name of the channel
     */
    public Channel(final String name) {
        
        this.name = name;
    }
    
    /**
     * Sends a reply to all clients, formatted with the given args.
     * @param reply The reply to send
     * @param args The args for the reply
     */
    public void sendToAll(final Reply reply, final Object... args) {
        
        final String formattedReply = reply.format(args);
        
        for (final Client client: clients) {
            
            client.sendStandardFormat(formattedReply);
        }
    }
    
    /**
     * Sends a message to all clients, appearing to originate from the given client
     * @param client The client who sent the message
     * @param message The message to send
     */
    public void sendFromClient(final Client client, final String message) {
        
        sendFromClient(client, message, SharedData.emptyBroadcastFilter);
    }
    
    /**
     * Send a message to all clients passing through the filter, appearing to originate from the given client
     * @param client The client who sent the message
     * @param message The message to send
     * @param filter The filter to determine which clients receive the message
     */
    public void sendFromClient(final Client client, final String message, final BroadcastFilter filter) {
        
        final String actualMessage = ":" + client.getDisplayHostmask() + " " + message;
        
        for (final Client other: clients) {
            
            if (filter.sendToConnection(other)) {
                
                other.send(actualMessage);
            }
        }
    }
    
    public void joinChannel(final Client client) {
        
        clients.add(client);
        sendFromClient(client, Command.JOIN.format(this.name));
        
        if (topic != null) {
            client.sendStandardFormat(Reply.RPL_TOPIC.format(client.getSafeNickname(), this.name, this.topic));
        }
        
        // TODO Check RPL_NAMREPLY format
        // client.sendStandardFormat(Reply.RPL_NAMREPLY.format);
    }
    
    public void partChannel(final Client client, String reason) {
     
        sendFromClient(client, Command.PART.format(this.name, reason));
        
        clients.remove(client);
    }
    
    public String getName() {
     
        return name;
    }

    @Override
    public String flags() {
        
        return StringUtils.joinIterable(channelmodes.keySet(), "");
    }

    @Override
    public void removeFlag(char flag) {
        
        channelmodes.remove(flag);
    }

    @Override
    public void addFlag(char flag, String param) {
        
        channelmodes.put(flag, param);
    }

    @Override
    public boolean hasFlag(char flag) {
        
        return channelmodes.containsKey(flag);
    }

    @Override
    public String flagParams() {
        
        StringBuilder builder = new StringBuilder();
        
        for (String param: channelmodes.values()) {
            if (param != null) builder.append(param);
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
        
        for (Client client: clients) {
            
            if (builder.length() > 0) builder.append(' ');
            
            if (isOp(client))     builder.append(DISPLAY_OP);
            if (isVoiced(client)) builder.append(DISPLAY_VOICE);
            
            builder.append(client.getNickname());
        }
        
        return builder.toString();
    }
    
            
            
    
    public boolean isOp(Client client) {
        return ops.contains(client);
    }
    
    public boolean isVoiced(Client client) {
        return voices.contains(client);
    }
    
    public boolean isMuted(Client client) {
        return muted.contains(client);
    }
    
    public boolean isBanned(Client client) {
        return banned.contains(client);
    }
    
    public void op(Client client) {
        ops.add(client);
    }
    
    public void deop(Client client) {
        ops.remove(client);
    }
    
    public void voice(Client client) {
        voices.add(client);
    }
    
    public void devoice(Client client) {
        voices.remove(client);
    }
    
    public void mute(Client client) {
        muted.add(client);
    }
    
    public void unmute(Client client) {
        muted.remove(client);
    }
    
    public void ban(Client client) {
        banned.add(client);
    }
    
    public void unban(Client client) {
        banned.remove(client);
    }
}
