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
    
    /**
     * The access list for the channel.
     */
    private final Map<String, Set<Character>> accessList = new HashMap<>();
    
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
}
