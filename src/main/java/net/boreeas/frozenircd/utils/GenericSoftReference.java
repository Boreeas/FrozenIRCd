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

import java.lang.ref.SoftReference;

/**
 * This class is a front end to Java's SoftReference, making it easier to use by using generics.
 * @author Boreeas
 */
public class GenericSoftReference<T> {
    
    /**
     * The actual soft reference storing the object.
     */
    private final SoftReference ref;
    
    /**
     * Creates a new soft reference storing the given object
     * @param reference The reference to store
     */
    public GenericSoftReference(final T reference) {
        
        ref = new SoftReference(reference);
    }
    
    /**
     * Returns the referenced element.
     * @return the referenced element
     */
    public T get() {
        
        return (T) ref.get();
    }
}
