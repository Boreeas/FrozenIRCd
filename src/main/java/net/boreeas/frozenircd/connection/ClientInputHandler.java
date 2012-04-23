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
import net.boreeas.frozenircd.config.SharedData;

/**
 *
 * @author Boreeas
 */
public abstract class ClientInputHandler implements InputHandler {
    
    public abstract void onInput(Connection connection, String input);
    public abstract void onModeChange(Connection connection, String modeString);
    
    
    public void onConnect(Connection connection) {
        SharedData.logger.log(Level.ALL, "Client {0} connected", connection);
    }

    public void onDisconnect(Connection connection) {
        SharedData.logger.log(Level.ALL, "Client {0} disconnected", connection);
    }
}
