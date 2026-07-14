package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link PluginLogger} backed by the plugin's java.util.logging logger.
 */
public final class JavaPluginLogger implements PluginLogger {

    private final Logger logger;

    public JavaPluginLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        this.logger.info(message);
    }

    @Override
    public void warn(String message) {
        this.logger.warning(message);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        this.logger.log(Level.WARNING, message, throwable);
    }

    @Override
    public void severe(String message) {
        this.logger.severe(message);
    }

    @Override
    public void severe(String message, Throwable throwable) {
        this.logger.log(Level.SEVERE, message, throwable);
    }
}
