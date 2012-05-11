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

/**
 * This enum represents all config keys that must be given. If neither any
 * configuration option nor any default value is associated with any of the keys
 * in this enum, an IncompleteConfigurationException must be thrown.
 * @author Boreeas
 */
public enum ConfigKey {
    
    HOST                ("hostname"),
    DESCRIPTION         ("description"),
    USING_PASS          ("using-pass"),
    PORTS               ("ports"),
    
    MIN_NICK_LENGTH     ("nick.length.min"),
    MAX_NICK_LENGTH     ("nick.length.max"),
    NICK_PATTERN        ("nick.pattern"),
    BLACKLISTED_NICKS   ("nick.blacklist"),
    
    TOKEN               ("token"),
    
    LINK_PASS           ("password.server"),
    SERVICE_PASS        ("password.service"),
    USER_PASS           ("password.user"),
    
    LOGGING_LEVEL       ("logging.level");
    
    
    
    private String key;
    
    private ConfigKey(String key) {
        
        this.key = key;
    }
    
    public String getKey() {
        
        return key;
    }
}
