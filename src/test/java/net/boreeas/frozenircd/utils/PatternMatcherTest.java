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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author malte
 */
public class PatternMatcherTest {
    
    public PatternMatcherTest() {
    }

    /**
     * Test of matchGlob method, of class PatternMatcher.
     */
    @Test
    public void testMatchLiteral() {
        
        assertTrue(PatternMatcher.matchGlob("foo", "foo"));
        assertTrue(PatternMatcher.matchGlob("FOO", "FOO"));
        assertTrue(PatternMatcher.matchGlob("Foo", "Foo"));
        
        assertTrue(PatternMatcher.matchGlob("FOO", "foo"));
        assertTrue(PatternMatcher.matchGlob("fOo", "FoO"));
        assertTrue(PatternMatcher.matchGlob("foo", "FOO"));
        
        assertTrue(PatternMatcher.matchGlob("With spaces", "WITH SPACES"));
        assertTrue(PatternMatcher.matchGlob("With_special!$&/W§$chars", "WITH_SPECIAL!$&/W§$chars"));
        
        assertFalse(PatternMatcher.matchGlob("Length mismatch", "Length  mismatch"));
    }
    
    @Test
    public void testMatchSingleWildcard() {
        
        assertTrue(PatternMatcher.matchGlob("f?o", "foo"));
        assertTrue(PatternMatcher.matchGlob("f?o", "flo"));
        assertTrue(PatternMatcher.matchGlob("f?o", "f!o"));
        assertFalse(PatternMatcher.matchGlob("f?o", "!oo"));
        assertTrue(PatternMatcher.matchGlob("f?o", "f?o"));
        
        assertTrue(PatternMatcher.matchGlob("???", "foo"));
        assertTrue(PatternMatcher.matchGlob("???", "bar"));
        assertFalse(PatternMatcher.matchGlob("???", "foobar"));
    }
    
    @Test
    public void testMatchMultiWildcard() {
        
        assertTrue(PatternMatcher.matchGlob("*", ""));
        assertTrue(PatternMatcher.matchGlob("*", "a"));
        assertTrue(PatternMatcher.matchGlob("*", "ab"));
        assertTrue(PatternMatcher.matchGlob("*", "abc"));
        
        assertTrue(PatternMatcher.matchGlob("*a", "a"));
        assertTrue(PatternMatcher.matchGlob("*a", "ba"));
        assertTrue(PatternMatcher.matchGlob("*a", "cba"));
        
        assertTrue(PatternMatcher.matchGlob("a*", "a"));
        assertTrue(PatternMatcher.matchGlob("a*", "ab"));
        assertTrue(PatternMatcher.matchGlob("a*", "abc"));
        
        assertTrue(PatternMatcher.matchGlob("a*d", "ad"));
        assertTrue(PatternMatcher.matchGlob("a*d", "abcd"));
        
        assertTrue(PatternMatcher.matchGlob("*a*", "a"));
        assertTrue(PatternMatcher.matchGlob("*a*", "dcba"));
        assertTrue(PatternMatcher.matchGlob("*a*", "abcd"));
        assertTrue(PatternMatcher.matchGlob("*a*", "dcbabcd"));
    }
    
    @Test
    public void testCombined() {
        
        assertTrue(PatternMatcher.matchGlob("?????????*", "123456789"));
        assertTrue(PatternMatcher.matchGlob("?????????*", "1234567890"));
        assertFalse(PatternMatcher.matchGlob("?????????*", "12345678"));
    }
}
