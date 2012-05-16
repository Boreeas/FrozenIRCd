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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Boreeas
 */
public final class HashUtils {
    
    // Utility class final constructor
    private HashUtils() {}
    
    
    public static String SHA256(final String s) throws NoSuchAlgorithmException {

        MessageDigest algorithm = null;

        try {
            algorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException nsae) {
            SharedData.logger.error("Cannot find digest algorithm: SHA-256");
            throw nsae;
        }

        byte[] defaultBytes = s.getBytes();
        algorithm.reset();
        algorithm.update(defaultBytes);
        byte messageDigest[] = algorithm.digest();
        
        StringBuilder hexString = new StringBuilder();
        
        for (int i = 0; i < messageDigest.length; i++) {
            
            String hex = Integer.toHexString(0xFF & messageDigest[i]);
            if (hex.length() == 1) {
                
                hexString.append('0');
            }
            
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
}
