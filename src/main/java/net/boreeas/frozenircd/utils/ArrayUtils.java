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

/**
 *
 * @author Boreeas
 */
public final class ArrayUtils {
    
    // Utility class private constuctor
    private ArrayUtils(){}
    
    /**
     * Appends a string to a string builder, prepending an empty space if the
     * buffer already contains text.
     * @param builder The builder used for storing the text
     * @param toAdd The text to add to the builder
     */
    public static void appendToBuffer(StringBuilder builder, String toAdd) {
        
        if (builder.length() > 0) {
            builder.append(" ");
        }
        
        builder.append(toAdd);
    }
    
    /**
     * Concats an array of strings into a single string, using a whitespace
     * character as separator.
     * @param array The array that should be joined
     * @return The string that has been created
     */
    public static String joinArray(String[] array) {
        
        return joinArray(array, 0);
    }
    
    /**
     * Concats an array of strings into a single string, starting with the
     * string at the specified index.
     * @param array The array that should be joined
     * @param firstIndex The index at which to start
     * @return The string that has been created.
     */
    public static String joinArray(String[] array, int firstIndex) {
        
        StringBuilder builder = new StringBuilder();
        
        for (int i = firstIndex; i < array.length; i++) {
            appendToBuffer(builder, array[i]);
        }
        
        return builder.toString();
    }
}
