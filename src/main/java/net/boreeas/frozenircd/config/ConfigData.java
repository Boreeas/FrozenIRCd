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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Boreeas
 */
public class ConfigData {
    
    /**
     * The general configuration settings
     */
    private static Config config;
    
    /**
     * Default settings for certain options
     */
    private final static Map<String, String[]> defaultOptions 
            = new HashMap<String, String[]>();
    
    static {
        defaultOptions.put(ConfigKey.BLACKLISTED_NICKS.getKey(), 
                            new String[0]);
        defaultOptions.put(ConfigKey.LINKS.getKey(), 
                            new String[0]);
        defaultOptions.put(ConfigKey.DESCRIPTION.getKey(), 
                            new String[]{"An IRC server"});
        defaultOptions.put(ConfigKey.MAX_NICK_LENGTH.getKey(), 
                            new String[]{"9"});
        defaultOptions.put(ConfigKey.MIN_NICK_LENGTH.getKey(), 
                            new String[]{"1"});
        putSingleDefaultOption(ConfigKey.NICK_PATTERN, 
                            "["                 //Grab from the following
                                + "a-zA-Z0-9"       //Alphanumerical chars
                                + "_"               //Underscore
                                + "\\Q"             //Start escape sequence
                                    + "-"               //Minus
                                    + "\\"              //Backslash
                                    + "[]"              //Square brakcets
                                    + "{}"              //Squiggly brackets
                                    + "^"               //Accent circonflexe
                                    + "|"               //Pipe
                                + "\\E"             //End escape sequence
                                + "`"               //This thingy
                            + "]+");            //As many as possible
        putSingleDefaultOption(ConfigKey.USING_PASS, "false");
        putSingleDefaultOption(ConfigKey.PORTS, "6667");
        putSingleDefaultOption(ConfigKey.LOGGING_LEVEL, "0");
    }
    
    private static void putSingleDefaultOption(ConfigKey key, String value) {
        
        defaultOptions.put(key.getKey(), new String[]{value});
    }
    
    /**
     * Returns the general IRCd config. If an IOException occurs, or the file does not exists, it will return an
     * empty (or incomplete) config.
     * @return The general IRCd config
     */
    private static synchronized Config getConfig() {
        
        if (config == null) {
            
            File configFile = new File("./configs/config.conf");            
            config = new Config(configFile);
            
            try {

                if (!configFile.exists()) {

                    configFile.getParentFile().mkdir();
                    configFile.createNewFile();
                }
                
                config.load();
            } catch (IOException ex) {
                
                
            }
        }
        
        return config;
    }
    
    /**
     * Returns the values associated with <code>key</code> in the config. If
     * none is associated, the default option is returned, or <code>null</code>
     * if no default option exists.
     * @param key The key of the option
     * @return The values associated with key
     */
    public static String[] getConfigOption(String key) {
        
        if (config == null) {
            
            config = getConfig();
        }
        
        String[] values = config.get(key);
        
        if (values == null) {
            values = defaultOptions.get(key);
        }
        
        return values;
    }
    
    /**
     * Returns the configuration option associated with the given ConfigKey.
     * This method is guaranteed to return a non-null value. If no option is
     * associated with <code>key</code>, an error is thrown.
     * @param key The key of the option
     * @return The values associated with the option
     * @throws IncompleteConfigurationException If there is no value in the
     * configuration and no default option associated with this key.
     */
    public static String[] getConfigOption(ConfigKey key) {
        
        String[] values = getConfigOption(key.getKey());
        if (values == null) {
            String error = String.format("Missing key %s", key);
            throw new IncompleteConfigurationException(error);
        }
        
        return values;
    }
    
    public static String getFirstConfigOption(String key) {
        
        return getConfigOption(key)[0];
    }
    
    public static String getFirstConfigOption(ConfigKey key) {
        
        return getConfigOption(key)[0];
    }
}
