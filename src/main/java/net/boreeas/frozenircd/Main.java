package net.boreeas.frozenircd;

import net.boreeas.frozenircd.config.ConfigData;
import net.boreeas.frozenircd.config.ConfigKey;
import net.boreeas.frozenircd.config.IncompleteConfigurationException;
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
        
        for (ConfigKey configkey: ConfigKey.values()) {
        
            try {
                ConfigData.getConfigOption(configkey);
            } catch (IncompleteConfigurationException ice) {
                SharedData.logger.error(String.format("Missing configuration: %s", ice.getMessage()));
                return;
            }
        }
        
        Server.INSTANCE.start();
    }
}
