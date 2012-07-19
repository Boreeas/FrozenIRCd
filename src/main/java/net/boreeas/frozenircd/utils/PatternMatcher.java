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
public class PatternMatcher {

    private static final char WILDCARD_ONE = '?';
    private static final char WILDCARD_MANY = '*';

    /**
     * Matches two strings.
     * @param pattern The glob pattern
     * @param text The string to match
     * @return 
     */
    public static boolean match(String pattern, String text) {

    	return matchCharacter(pattern, 0, text, 0);
    }

    /**
     * Handles glob matching for specific characters.<br />
     * Solution provided by Tony Edgecombe (http://stackoverflow.com/a/1728273)
     */
    private static boolean matchCharacter(String pattern, int patternIndex, String text, int textIndex) {
    	
        if (patternIndex >= pattern.length()) {
    		return false;
    	}

    	switch(pattern.charAt(patternIndex)) {
    		case WILDCARD_ONE:
    			// Match any character
    			if (textIndex >= text.length()) {
    				return false;
    			}
    			break;

    		case WILDCARD_MANY:
    			// * at the end of the pattern will match anything
    			if (patternIndex + 1 >= pattern.length() || textIndex >= text.length()) {
    				return true;
    			}

    			// Probe forward to see if we can get a match
    			while (textIndex < text.length()) {
    				if (matchCharacter(pattern, patternIndex + 1, text, textIndex)) {
    					return true;
    				}
    				textIndex++;
    			}

    			return false;

    		default:
    			if (textIndex >= text.length()) {
    				return false;
    			}

    			String textChar = text.substring(textIndex, textIndex + 1);
    			String patternChar = pattern.substring(patternIndex, patternIndex + 1);

    			// Note the match is case insensitive
    			if (textChar.compareToIgnoreCase(patternChar) != 0) {
    				return false;
    			}
    	}

    	// End of pattern and text?
    	if (patternIndex + 1 >= pattern.length() && textIndex + 1 >= text.length()) {
    		return true;
    	}

    	// Go on to match the next character in the pattern
    	return matchCharacter(pattern, patternIndex + 1, text, textIndex + 1);
    }
}
