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

import net.boreeas.frozenircd.config.Config;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import junit.framework.TestCase;

/**
 *
 * @author malte
 */
public class ConfigTest extends TestCase {

    public ConfigTest(String testName) {
        super(testName);
    }


    /**
     * Test of load method, of class Config.
     */
    public void testLoad() throws Exception {

        Config config = new Config(new File("this_file_does_not_exist"));

        try {
            config.load();
            assertTrue("Empty config file not loaded empty", config.isEmpty());
        } catch (IOException ioe) {
            fail("Test file not found: 'this_file_does_not_exist'");
        }

    }

    /**
     * Test of save method, of class Config.
     */
    public void testSave() throws Exception {

        new File("./this_file_will_be_created").delete();       // Remove previous test artifacts
        Config config = new Config(new File("./this_file_will_be_created"));
        config.save();

        config.set("x", "y");

        config.load();  //This shouldn't fail now
        assertTrue("Config not loaded empty", config.isEmpty());   // Reloaded from empty file on disk, so it should be empty

        config.set("x", "y");
        config.save();
        config.load();

        assertTrue(config.containsKey("x"));

    }

    public void testSet() throws Exception {

        Config config = new Config(new File("a"));
        config.set("AS", "d", "e", "f");

        assertTrue(config.containsKey("AS"));
    }

    public void testRemove() throws Exception {

        Config config = new Config(new File("a"));
        config.set("AS", "d", "e", "f");

        String[] values = config.remove("AS");
        assertFalse("Mapping still exists", config.containsKey("AS"));
        assertTrue("Not enough elements returned: " + values.length, values.length == 3);
        assertTrue("Elements in wrong order: " + Arrays.toString(values), values[0].equals("d") && values[1].equals("e") && values[2].equals("f"));
    }

    public void testGet() throws Exception {

        Config config = new Config(new File("a"));
        config.set("AS", "d", "e", "f");

        String[] values = config.get("AS");
        assertTrue("Mapping no longer exists", config.containsKey("AS"));
        assertTrue("Not enough elements returned: " + values.length, values.length == 3);
        assertTrue("Elements in wrong order: " + Arrays.toString(values), values[0].equals("d") && values[1].equals("e") && values[2].equals("f"));
    }

    public void testIsEmpty() throws Exception {

        Config config = new Config(new File("a"));

        assertTrue("Map not created empty", config.isEmpty());

        config.set("a", "b");

        assertFalse("Map empty after element added", config.isEmpty());

        config.remove("a");

        assertTrue("Map not empty after last element has been removed", config.isEmpty());
    }

    public void testContainsKey() throws Exception {

        Config config = new Config(new File("a"));

        config.set("a", "b");

        assertTrue("Map does not contain set key", config.containsKey("a"));
        assertFalse("Map contains unset key", config.containsKey("b"));

    }
}
