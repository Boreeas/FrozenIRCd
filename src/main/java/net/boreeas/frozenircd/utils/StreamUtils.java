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
package net.boreeas.frozenircd.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author Boreeas
 */
public class StreamUtils {
    
    // Utility class private constructor
    private StreamUtils() {}
    
    /**
     * Closes the reader if it isn't null. Any IOException is reported and dropped.
     * @param reader The reader to close
     */
    public static void safeClose(Reader reader) {
        
        // If the reader is null, do nothing
        if (reader != null) {
            
            try {
                reader.close();
            } catch (IOException ex) {
                
                // IOExceptions shouldn't happen here, issue a warning and drop it
                SharedData.logger.warn("Exception while closing stream reader", ex);
            }
        }
    }
    
    /**
     * Reads from the specified reader until the end of the stream is reached.
     * @param reader The reader to read from
     * @return The contents of the stream
     * @throws IOException If an IOException occurs while reading from the string
     */
    public static String readFully(BufferedReader reader) throws IOException {
        
        // Assert that we have a we have a reader to work with
        if (reader == null) {
            throw new IllegalArgumentException("reader can't be null");
        }
        
        String input = null;
        StringBuilder result = new StringBuilder();
        
        try {
            
            // Read until end of stream is reached
            while((input = reader.readLine()) != null) {
            
                result.append(input).append("\n");
            }
        } finally {
            
            // Close the stream to release resources
            safeClose(reader);
        }
        
        return result.toString();
    }
}
