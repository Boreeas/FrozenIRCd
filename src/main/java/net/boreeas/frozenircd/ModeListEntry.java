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

import java.util.Objects;
import net.boreeas.frozenircd.utils.SharedData;

/**
 *
 * @author Boreeas
 */
public class ModeListEntry {

    public final String entry;
    private final String issuerMask;
    private final long time;

    public ModeListEntry(String entry, String issuerMask) {
        this.entry = SharedData.toLowerCase(entry);
        this.issuerMask = SharedData.toLowerCase(entry);
        this.time = System.currentTimeMillis();
    }

    public String format() {
        return entry + " " + issuerMask + " " + time;
    }

    public boolean equals(Object o) {

        if (o == null || !(o instanceof ModeListEntry)) {
            return false;
        }

        ModeListEntry other = (ModeListEntry) o;
        return other.entry.equals(entry);
    }

    public int hashCode() {
        return this.entry.hashCode();
    }

}
