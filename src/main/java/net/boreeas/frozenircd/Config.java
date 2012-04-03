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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 *
 * @author Boreeas
 */
public class Config {
    
    /**
     * The backend file.
     */
    private final File backend;
    
    /**
     * The fields/values of the config file.
     */
    private Map<String, String[]> fields;
    
    /**
     * Indicates whether the config has already been read from the disk.
     */
    private boolean read = false;
    
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
    public void load() throws FileNotFoundException, IOException, IllegalFormatException {
        
        if (read) {
            
            return; // We are done
        }
        
        BufferedReader reader = new BufferedReader(new FileReader(backend));
        
        String input = null;
        while ((input = reader.readLine().trim()) != null) {
            
            if (input.startsWith("#") || input.equals("")) {
                continue;   //Skip comments and empty lines
            }
            
            String[] parts = input.split(" ", 2);
            
            if (parts.length < 2) {
                
                throw new IllegalFormatException(String.format("Missing field.name or field.value: %s", input));
            }
        }
    }
}
