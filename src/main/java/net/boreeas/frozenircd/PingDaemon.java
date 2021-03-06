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

import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.connection.Connection;
import net.boreeas.frozenircd.connection.ConnectionPool;
import net.boreeas.frozenircd.utils.SharedData;

/**
 *
 * @author Boreeas
 */
public class PingDaemon extends Thread {

    public PingDaemon() {

        super("PingDaemon");
        setDaemon(true);
    }

    @Override
    public void run() {

        int pingFreqMillis = 1000 * Integer.parseInt(ConfigData.getFirstConfigOption(ConfigKey.PING_FREQUENCY));
        int pingTimeoutMillis = 1000 * Integer.parseInt(ConfigData.getFirstConfigOption(ConfigKey.PING_TIMEOUT));

        int timeSinceLastPingMillis = pingFreqMillis;

        while (true) {

            for (Connection connection : ConnectionPool.ALL.getConnections(SharedData.passAllFilter)) {

                // Disconnect if ping request sent
                if (connection.getPingRequestText() != null
                    && timeDiff(connection) > pingTimeoutMillis) {

                    connection.disconnect("Ping Timeout: " + timeDiff(connection)/1000 + " seconds");

                }

                // Send a ping request
                if (timeSinceLastPingMillis >= pingFreqMillis) {

                    connection.sendPingRequest(ConfigData.getFirstConfigOption(ConfigKey.HOST));
                }
            }

            // Reset time
            if (timeSinceLastPingMillis >= pingFreqMillis) {

                timeSinceLastPingMillis = 0;
            }

            // Wait for next
            try {

                sleep(pingTimeoutMillis);
                timeSinceLastPingMillis += pingTimeoutMillis;
            }
            catch (InterruptedException ex) {
                SharedData.logger.warn("Unable to sleep in PING daemon", ex);
            }
        }
    }

    private long timeDiff(Connection conn) {

        return System.currentTimeMillis() - conn.getLastPingTime();
    }
}
