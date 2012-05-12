package net.boreeas.frozenircd;

import java.util.logging.Level;
import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.utils.SharedData;

/**
 * This class controls the startup of the IRCd.
 * @author Boreeas
 */
public final class Main {

    /**
     * No instantiation.
     */
    private Main() {
    }

    /**
     * Program entry point.
     * @param args The command line arguments
     */
    public static void main(final String[] args) {
        
        SharedData.logger.log(Level.INFO, "Checking configuration for completeness");
        for (ConfigKey configkey: ConfigKey.values()) {
            
            ConfigData.getConfigOption(configkey);
        }
        
        Server.INSTANCE.start();
    }
}
