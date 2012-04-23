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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author Boreeas
 */
public class Config {
    
    /**
     * The separator of fields in the config file
     */
    public static final String FIELD_SEPARATOR = ";";
    
    /**
     * The backend file.
     */
    private final File backend;
    
    /**
     * The fields/values of the config file.
     */
    private Map<String, String[]> fields = new HashMap<String, String[]>();
    
    /**
     * Indicates whether the config has been modified
     */
    private boolean modified = true;
    
    
    /**
     * Creates a config with the given backend file.
     * @param file The file to use.
     */
    public Config(final File file) {
        
        this.backend = file;
    }
    
    /**
     * Loads the configuration file from the disk.
     * @throws FileNotFoundException If the backend file does not exist
     * @throws IOException If an IOException occurred while reading the file
     * @throws IllegalFormatException If any non-comment, non-empty line contains only a name or a value
     */
    public void load() throws IOException {
        
        //We are reloading from the disk, so remove everything to prevent old entries
        clear();
        
        if (!modified) {
            
            return; // We are done
        }
        
        BufferedReader reader = null;
        String input = null;
        
        try {
            
            reader = new BufferedReader(new FileReader(backend));
        } catch (FileNotFoundException nfe) {
            
            SharedData.logger.warning(String.format("Unable to read config input file: %s", backend));
            return;
        }
        
        while ((input = reader.readLine()) != null) {
            input = input.trim();
            
            if (input.startsWith("#") || input.equals("")) {
                continue;   //Skip comments and empty lines
            }
            
            String[] parts = input.split(" ?= ?", 2);
            
            if (parts.length < 2) {
                
                SharedData.logger.warning(String.format("Encountered incomplete line %s in config file %s, ignoring", input, backend));
                continue;   //Skip incomplete lines
            }
            
            String[] values = parts[1].trim().split(FIELD_SEPARATOR);    //TODO Split not by escaped separator
            
            for (int i = 0; i < values.length; i++) {
                
                values[i] = values[i].replace("\\" + FIELD_SEPARATOR, FIELD_SEPARATOR);  // Unescape field separators
            }
            
            parts[0] = parts[0].trim();
            
            if (fields.containsKey(parts[0])) {
                
                // Join old and new values
                String[] old = fields.get(parts[0]);
                String[] newArray = new String[old.length + values.length];
                System.arraycopy(old, 0, newArray, 0, old.length);
                System.arraycopy(values, 0, newArray, old.length, values.length);
                
                values = newArray;
            }
            
            fields.put(parts[0], values);
        }
        
        reader.close();
        
        modified = false;
    }
    
    /**
     * Saves the current configuration state back to the disk. This method tries to maintain the original file structure.
     * If no modifications have been made since the file has been saved or loaded the last time, this method returns immediately
     * @throws IOException If an IOException occurs
     */
    public void save() throws IOException {
        
        if (!modified) {
            
            return;
        }
        
        if (!backend.exists()) {
            
            backend.getParentFile().mkdirs();
            backend.createNewFile();
        }
        
        // Re-read from disk to maintain comments
        BufferedReader reader = new BufferedReader(new FileReader(backend));
        
        
        List<String> fileData = new ArrayList<String>();
        Set<String> savedKeys = new HashSet<String>();
        
        
        String line = null;
        while ((line = reader.readLine()) != null) {
            
            fileData.add(line.trim());
        }
        
        reader.close();
        
        
        // Update all keys in the file
        for (int i = 0; i < fileData.size(); i++) {
            
            if (fileData.get(i).startsWith("#") || fileData.get(i).equals("")) {
                continue; //Don't worry about comments
            }
            
            String key = fileData.get(i).split(" ?= ?")[0];
            
            if (fields.containsKey(key)) {
                
                fileData.set(i, String.format("%s = %s", key, toConfigString(fields.get(key))));
                savedKeys.add(key);
            }
        }
        
        //Add any not existing keys
        if (fields.size() > savedKeys.size()) {
            fileData.add("");
            fileData.add(String.format("# Saved on %s", new Date()));

            for (Entry<String, String[]> entry: fields.entrySet()) {

                if (!savedKeys.contains(entry.getKey())) {
                    fileData.add(String.format("%s = %s", entry.getKey(), toConfigString(entry.getValue())));
                }
            }
        }
        
        // Write everything back to the file
        BufferedWriter writer = new BufferedWriter(new FileWriter(backend));
        
        for (String out: fileData) {
            
            writer.write(String.format("%s%n", out));
        }
        
        writer.flush();
        writer.close();
        
        // File has been saved, so there is no difference between this and the data on the disk
        modified = false;
    }
    
    /**
     * Associates the given values with the given key.
     * @param key The key of the values
     * @param values The values
     */
    public void set(String key, Object... values) {
        
        String[] valueArray = new String[values.length];
        int index = 0;
        
        for (Object field: values) {
            
            valueArray[index++] = field.toString();
        }
        
        
        fields.put(key, valueArray);
        modified = true;
    }
    
    /**
     * Returns the values currently associated with the <code>key<code>, or null if none are associated.
     * @param key The key of the values
     * @return The values associated with <code>key</code>, or <code>null</code> if none are associated.
     */
    public String[] get(String key) {
        
        return fields.get(key);
    }
    
    /**
     * Removes the mapping of <code>key</code> from the map.
     * @param key The key to remove
     * @return The values removed, or <code>null</code> if the map did not include any mapping from <code>key</code>
     */
    public String[] remove(String key) {
        
        modified = true;
        return fields.remove(key);
        
    }
    
    /**
     * Empties the map.
     */
    public void clear() {
        
        modified = true;
        fields.clear();
    }
    
    /**
     * Tells whether the config contains no mappings.
     */
    public boolean isEmpty() {
        
        return fields.isEmpty();
    }
    
    /**
     * Tells whether this configuration contains target key.
     * @param key The key to check
     * @return <code>true</code> if there is such a key, <code>false</code> otherwise.
     */
    public boolean containsKey(String key) {
        
        return fields.containsKey(key);
    }
    
    /**
     * Converts an array of fields to a String that can be saved in the config file
     * @param fields The fields of the key saved to the file
     * @return A String with the fields separated by the FIELD_SEPARATOR, with separators in the each field escaped
     * @see #FIELD_SEPERATOR
     */
    private String toConfigString(String[] fields) {
        
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        
        
        for (String string: fields) {
            
            if (!first) {
                builder.append(";");
            } else {
                first = false;
            }
            
            builder.append(string.replace(FIELD_SEPARATOR, "\\" + FIELD_SEPARATOR)); // Escape field seperators
        }
        
        return builder.toString();
    }
}
