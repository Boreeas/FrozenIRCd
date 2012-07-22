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

/**
 *
 * @author Boreeas
 */
public interface Flagable {
    
    /**
     * Returns the mode string for this Flagable
     * @return The mode string for the Flagable
     */
    public String flags();
    
    /**
     * Returns the mode params string for this Flagable
     * @return The mode params string
     */
    public String flagParams();
    
    /**
     * Removes the given flag.
     * @param flag The flag to remove
     */
    public void removeFlag(char flag);
    
    /**
     * Sets the given flag.
     * @param flag The flag to set
     */
    public void addFlag(char flag, String params);
    
    /**
     * Tells whether this Flagable has a certain flag
     * @param flag The flag to check
     * @return <code>true</code> if the Flagable has the flag <code>flag</code>, false otherwise
     */
    public boolean hasFlag(char flag);
    
    /**
     * Returns the parameter set for the given flag.
     * Returns null if the flag takes no parameter
     * or the flag was not set.
     * @return The parameter for the flag.
     */
    public String getParam(char flag);
}
