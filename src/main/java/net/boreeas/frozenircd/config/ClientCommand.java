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
package net.boreeas.frozenircd.config;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Boreeas
 */
public enum ClientCommand {
    
    PING            ("ping"),
    USER            ("user"),
    NICK            ("nick"),
    PASS            ("pass"),
    OPER            ("oper"),
    MODE            ("mode");
    
    
    private String command;
    private static Map<String, ClientCommand> commands;
    
    
    private ClientCommand(String command) {
        
        this.command = command;
        
        finalizeConstructor();
    }
    
    private void finalizeConstructor() {
        
        if (commands == null) {
            commands = new HashMap<String, ClientCommand>();
        }
        commands.put(command.toUpperCase(), this);
    }
    
    public String getCommand() {
        return command;
    }
    
    public static ClientCommand forName(String name) {
        
        return commands.get(name.toUpperCase());
    }
}
