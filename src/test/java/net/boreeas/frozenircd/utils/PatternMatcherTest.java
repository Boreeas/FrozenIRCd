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
     * Test of match method, of class PatternMatcher.
     */
    @Test
    public void testMatchLiteral() {
        
        assertTrue(PatternMatcher.match("foo", "foo"));
        assertTrue(PatternMatcher.match("FOO", "FOO"));
        assertTrue(PatternMatcher.match("Foo", "Foo"));
        
        assertTrue(PatternMatcher.match("FOO", "foo"));
        assertTrue(PatternMatcher.match("fOo", "FoO"));
        assertTrue(PatternMatcher.match("foo", "FOO"));
        
        assertTrue(PatternMatcher.match("With spaces", "WITH SPACES"));
        assertTrue(PatternMatcher.match("With_special!$&/W§$chars", "WITH_SPECIAL!$&/W§$chars"));
        
        assertFalse(PatternMatcher.match("Length mismatch", "Length  mismatch"));
    }
    
    @Test
    public void testMatchSingleWildcard() {
        
        assertTrue(PatternMatcher.match("f?o", "foo"));
        assertTrue(PatternMatcher.match("f?o", "flo"));
        assertTrue(PatternMatcher.match("f?o", "f!o"));
        assertFalse(PatternMatcher.match("f?o", "!oo"));
        assertTrue(PatternMatcher.match("f?o", "f?o"));
        
        assertTrue(PatternMatcher.match("???", "foo"));
        assertTrue(PatternMatcher.match("???", "bar"));
        assertFalse(PatternMatcher.match("???", "foobar"));
    }
    
    @Test
    public void testMatchMultiWildcard() {
        
        assertTrue(PatternMatcher.match("*", ""));
        assertTrue(PatternMatcher.match("*", "a"));
        assertTrue(PatternMatcher.match("*", "ab"));
        assertTrue(PatternMatcher.match("*", "abc"));
        
        assertTrue(PatternMatcher.match("*a", "a"));
        assertTrue(PatternMatcher.match("*a", "ba"));
        assertTrue(PatternMatcher.match("*a", "cba"));
        
        assertTrue(PatternMatcher.match("a*", "a"));
        assertTrue(PatternMatcher.match("a*", "ab"));
        assertTrue(PatternMatcher.match("a*", "abc"));
        
        assertTrue(PatternMatcher.match("a*d", "ad"));
        assertTrue(PatternMatcher.match("a*d", "abcd"));
        
        assertTrue(PatternMatcher.match("*a*", "a"));
        assertTrue(PatternMatcher.match("*a*", "dcba"));
        assertTrue(PatternMatcher.match("*a*", "abcd"));
        assertTrue(PatternMatcher.match("*a*", "dcbabcd"));
    }
    
    @Test
    public void testCombined() {
        
        assertTrue(PatternMatcher.match("?????????*", "123456789"));
        assertTrue(PatternMatcher.match("?????????*", "1234567890"));
        assertFalse(PatternMatcher.match("?????????*", "12345678"));
    }
}
