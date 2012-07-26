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
import net.boreeas.frozenircd.utils.Filter;
import net.boreeas.frozenircd.utils.SharedData;

/**
 *
 * @author Boreeas
 */
public class ChannelPool {

    private static final Map<String, Channel> channels = new HashMap<>();

    public static Channel getChannel(String name) {

        return channels.get(SharedData.toLowerCase(name));
    }

    public synchronized static void addChannel(Channel channel) {

        channels.put(SharedData.toLowerCase(channel.getName()), channel);
    }

    public synchronized static void removeChannel(String name) {

        channels.remove(SharedData.toLowerCase(name));
    }


    public synchronized static Set<Channel> getChannels(Filter<Channel> filter) {

        Set<Channel> results = new HashSet<>();

        for (Channel chan: channels.values()) {

            if (filter.pass(chan)) {
                results.add(chan);
            }
        }

        return results;
    }
}
