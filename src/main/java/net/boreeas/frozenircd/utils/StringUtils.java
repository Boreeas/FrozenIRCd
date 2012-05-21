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
public final class StringUtils {
    
    // Utility class private constuctor
    private StringUtils(){}
    
    /**
     * Appends an object to a string builder, prepending an empty space if the
     * buffer already contains text.
     * @param builder The builder used for storing the text
     * @param toAdd The object to add to the builder
     */
    public static void appendToBuffer(StringBuilder builder, Object toAdd) {
        
        if (builder.length() > 0) {
            builder.append(" ");
        }
        
        builder.append(toAdd);
    }
    
    public static void appendToBuffer(StringBuilder builder, Object toAdd, String separator) {
        
        if (builder.length() > 0) {
            builder.append(separator);
        }
        
        builder.append(toAdd);
    }
    
    /**
     * Concats an array of objects into a single string, using a whitespace
     * character as separator.
     * @param array The array that should be joined
     * @return The string that has been created
     */
    public static String joinArray(Object[] array) {
        
        return joinArray(array, 0, " ");
    }
    
    public static String joinArray(Object[] array, String separator) {
        
        return joinArray(array, 0, separator);
    }
    
    /**
     * Concats an array of objects into a single string, starting with the
     * object at the specified index.
     * @param array The array that should be joined
     * @param firstIndex The index at which to start
     * @return The string that has been created.
     */
    public static String joinArray(Object[] array, int firstIndex) {
        
        return joinArray(array, firstIndex, " ");
    }
    
    public static String joinArray(Object[] array, int firstIndex, String separator) {
        
        StringBuilder builder = new StringBuilder();
        
        for (int i = firstIndex; i < array.length; i++) {
            appendToBuffer(builder, array[i], separator);
        }
        
        return builder.toString();
    }
    
    public static String joinIterable(Iterable iterable) {
        
        return joinIterable(iterable, " ");
    }
    
    public static String joinIterable(Iterable iterable, String separator) {
        
        StringBuilder builder = new StringBuilder();
        
        for (Object part: iterable) {
            appendToBuffer(builder, part, separator);
        }
        
        return builder.toString();
    }
}
