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
public enum Stats {

    NETWORK_VISIBLE,
    NETWORK_INVISIBLE,
    NETWORK_SERVERS(1),
    OPERATORS,
    NETWORK_UNKNOWN,
    NETWORK_CHANNELS,
    LOCAL_USERS,
    LOCAL_LINKS;


    private int value = 0;

    private Stats() {}

    private Stats(int initval) {
        value = initval;
    }

    public void inc() {
        value++;
    }

    public void dec() {
        value--;
    }

    public int get() {
        return value;
    }
}
