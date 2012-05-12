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
package net.boreeas.frozenircd.connection;

import java.util.logging.Level;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.connection.client.Client;
import net.boreeas.frozenircd.utils.SharedData;

/**
 *
 * @author Boreeas
 */
public class ConnectTimeoutDaemon extends Thread {
   
    private Client client;
    static int connectTimeoutMillis = Integer.parseInt(ConfigData.getFirstConfigOption(ConfigKey.CONNECT_TIMEOUT)) * 1000;
    
    public ConnectTimeoutDaemon(Client client) {

        super("GetHostname[" + client + "]");

        setDaemon(true);

        this.client = client;
    }
    
    @Override
    public void run() {

        try {
            sleep(connectTimeoutMillis);
        }
        catch (InterruptedException ex) {
            SharedData.logger.log(Level.WARNING, "Unable to sleep for CONNECT_TIMEOUT", ex);
        }

        if (!client.registrationCompleted()) {
            client.disconnect("Timeout: Registration timed out");
        }
    }
}
