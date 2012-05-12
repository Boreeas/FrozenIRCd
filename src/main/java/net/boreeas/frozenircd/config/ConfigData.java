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

import net.boreeas.frozenircd.utils.SharedData;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
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
    public static final String CONFIG = "config";
    
    /**
     * The "lines" configuration file
     */
    private static Config lines;
    public static final String LINES = "lines";
    
    /**
     * Oper passwords
     */
    private static Config opers;
    public static final String OPERS = "opers";
    
    /**
     * Default settings for certain options
     */
    private final static Map<String, String[]> defaultOptions 
            = new HashMap<String, String[]>();
    
    /**
     * O-Lines (Oper hosts) parameter name
     */
    private final static String O_LINES = "olines";
    private static Set<String> olinesSet; 
    
    /**
     * U-Lines (Server link) parameter name
     */
    private final static String U_LINES = "ulines";
    private static Set<String[]> ulinesSet;
    
    static {
        defaultOptions.put(ConfigKey.BLACKLISTED_NICKS.getKey(), 
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
        putSingleDefaultOption(ConfigKey.USER_PASS, "");
        putSingleDefaultOption(ConfigKey.PING_FREQUENCY, "600");
        putSingleDefaultOption(ConfigKey.PING_TIMEOUT, "180");
        putSingleDefaultOption(ConfigKey.CONNECT_TIMEOUT, "60");
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
            
            config = loadFile(CONFIG);
            
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
            
            lines = loadFile(LINES);
            
            
            // Lines will be kept in a seperate set for ease of access, therefore we
            // need to write it back when the system shuts down
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {

                    SharedData.logger.log(Level.INFO, "Rewriting lines to disk");

                    lines.set(O_LINES, olinesSet.toArray());
                    lines.set(U_LINES, ulinesSet.toArray());
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
            
            opers = loadFile(OPERS);
            
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
            String error = String.format("Missing key %s (config option: %s)", key, key.getKey());
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
    
    public static Set<String[]> getULines() {
        
        if (ulinesSet == null) {
            
            ulinesSet = new HashSet<String[]>();
            String[] ulines = getLines(U_LINES);
            
            if (!(ulines == null)) {
                
                for (String uline: ulines) {
                    
                    String[] options = uline.split(":");
                    if (options.length < 2) {
                        SharedData.logger.log(Level.SEVERE, "Incorrect link entry format for entry \"{0}\": Accepted formats are <host>:<port>:<password>, <host>::<password> or <host>:<password>", uline);
                        continue;
                    } else if (options.length < 3) {
                        SharedData.logger.log(Level.WARNING, "Found link entry format <host>:<pass> in entry {0}, extending to \"{1}:6667:{2}\"", new Object[]{uline, options[0], options[1]});
                    } else if (options[1].equals("")) {
                        SharedData.logger.log(Level.WARNING, "Found link entry format <host>::<pass> in entry {0}, extending to \"{1}:6667:{2}\"", new Object[]{uline, options[0], options[2]});
                    }
                }
            }
        }
        
        return ulinesSet;
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
        
        return matches(getOLines(), host);
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
