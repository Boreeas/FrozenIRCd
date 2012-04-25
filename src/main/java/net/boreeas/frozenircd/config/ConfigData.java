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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.boreeas.frozenircd.utils.HashUtils;

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
     * The "lines" configuration file
     */
    private static Config lines;
    
    /**
     * Oper passwords
     */
    private static Config opers;
    
    /**
     * Default settings for certain options
     */
    private final static Map<String, String[]> defaultOptions 
            = new HashMap<String, String[]>();
    
    /**
     * O-Lines parameter name
     */
    private final static String O_LINES = "o-lines";
    private static Set<String> olinesSet; 
    
    static {
        defaultOptions.put(ConfigKey.BLACKLISTED_NICKS.getKey(), 
                            new String[0]);
        defaultOptions.put(ConfigKey.LINKS.getKey(), 
                            new String[0]);
        defaultOptions.put(ConfigKey.DESCRIPTION.getKey(), 
                            new String[]{"An IRC server"});
        defaultOptions.put(ConfigKey.MAX_NICK_LENGTH.getKey(), 
                            new String[]{"8"});
        defaultOptions.put(ConfigKey.MIN_NICK_LENGTH.getKey(), 
                            new String[]{"0"});
        
        // The pattern nicks must adhere to
        putSingleDefaultOption(ConfigKey.NICK_PATTERN, 
                            // First letter - may not include numbers or a -
                            "["                 //Grab from the following
                                + "a-zA-Z"          //Alphabetical chars
                                + "_"               //Underscore
                                + "\\Q"             //Start escape sequence
                                    + "\\"              //Backslash
                                    + "[]"              //Square brakcets
                                    + "{}"              //Squiggly brackets
                                    + "^"               //Accent circonflexe
                                    + "|"               //Pipe
                                + "\\E"             //End escape sequence
                                + "`"               //This thingy
                            + "]"            
                
                            // Rest of the letters
                            + "["               //Grab from the following
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
                            + "]");            // End sequence
        putSingleDefaultOption(ConfigKey.USING_PASS, "false");
        putSingleDefaultOption(ConfigKey.PORTS, "6667");
        putSingleDefaultOption(ConfigKey.LOGGING_LEVEL, "0");
    }
    
    /**
     * Sets the default options to contain a single option for a given key
     * @param key The key to update
     * @param value The value to set
     */
    private static void putSingleDefaultOption(ConfigKey key, String value) {
        
        defaultOptions.put(key.getKey(), new String[]{value});
    }
    
    private static Config loadFile(String name) {
        
        File file = new File("./configs/" + name.toLowerCase() + ".conf");
        Config newConfig = new Config(file);
        
        try {
            
            if (!file.exists()) {
                
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            
            newConfig.load();
        } catch (IOException ex) {
            
            SharedData.logger.log(Level.SEVERE, name + " could not be loaded.", ex);
        }
        
        return newConfig;
    }
    
    /**
     * Returns the general IRCd config. If an IOException occurs, or the file does not exists, it will return an
     * empty (or incomplete) config.
     */
    private static synchronized void loadConfigFile() {
        
        if (config == null) {
            
            config = loadFile("Config");
            
            // Save on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread() {
            
                @Override
                public void run() {
                    
                    try {
                        config.save();
                    }
                    catch (IOException ex) {
                        
                        SharedData.logger.log(Level.SEVERE, "Could not save Config", ex);
                    }
                }
            });
        }
        
    }
    
    /**
     * Loads the lines from the file
     */
    private static synchronized void loadLinesFile() {
        
        if (lines == null) {
            
            lines = loadFile("Lines");
            
            
            // Lines will be kept in a seperate set for ease of access, therefore we
            // need to write it back when the system shuts down
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {

                    SharedData.logger.log(Level.INFO, "Rewriting lines to disk");

                    lines.set(O_LINES, olinesSet);
                    // TODO Add all the lines


                    try {
                        lines.save();
                    }
                    catch (IOException ex) {
                        SharedData.logger.log(Level.SEVERE, "Unable to save lines file", ex);
                    }
                }
            });
        }
        
    }
    
    /**
     * Loads the password hashs from the file
     */
    private static synchronized void loadOperPassFile() {
        
        if (opers == null) {
            
            opers = loadFile("Opers");
            
            // Save on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {

                    try {
                        opers.save();
                    }
                    catch (IOException ex) {
                        SharedData.logger.log(Level.SEVERE, "Opers file could not be saved.");
                    }
                }
            });
        }
        
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
            
            loadConfigFile();
        }
        
        String[] values = config.get(key);
        
        if (values == null) {
            SharedData.logger.log(Level.WARNING, "No value for key \"{0}\" found in config - checking defaults", key);
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
    
    /**
     * Returns the lines with a given name.
     * @param lineName The name for the set of lines
     * @return The lines
     */
    private static String[] getLines(String lineName) {
        
        if (lines == null) {
            
            loadLinesFile();
        }
        
        return lines.get(lineName);
    }
    
    /**
     * Returns all configured o-lines.
     * @return a set containing all configured o-lines
     */
    public static Set<String> getOLines() {
        
        if (olinesSet == null) {
            
            olinesSet = new HashSet<String>();
            String[] olines = getLines(O_LINES);
            
            if (!(olines == null)) {
                // If it's null, we return an empty set
                olinesSet.addAll(Arrays.asList(olines));
            }
        }
        
        return olinesSet;
    }
    
    /**
     * Matches a host against all lines in the line set.
     * @param lineSet The lines to check
     * @param host The host to check
     * @return <code>true</code> if the host matches any of the lines, <code>false</code> otherwise
     */
    private static boolean matches(Set<String> lineSet, String host) {
        
        for (String line: lineSet) {
            
            if (host.matches(line.replace(".", "\\.").replace('?', '.'))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the host matches any configured o-line.
     * @param host The host to check
     * @return <code>true</code> if a match is found, <code>false</code> otherwise
     */
    public static boolean matchesOLine(String host) {
        
        return matches(olinesSet, host);
    }
    
    public static boolean checkOperPassword(String name, String password) throws NoSuchAlgorithmException, IncompleteConfigurationException {
        
        if (opers == null) {
            
            loadOperPassFile();
        }
        
        try {
            return HashUtils.SHA256(password).equals(opers.get(name.toLowerCase())[0]);
        } catch (NullPointerException npe) {
            throw new IncompleteConfigurationException("No such oper");
        }
    }
}
