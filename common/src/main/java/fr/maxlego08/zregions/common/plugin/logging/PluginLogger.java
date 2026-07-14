package fr.maxlego08.zregions.common.plugin.logging;

/**
 * Platform-agnostic logger (the common module never sees java.util.logging or slf4j directly).
 */
public interface PluginLogger {

    void info(String message);

    void warn(String message);

    void warn(String message, Throwable throwable);

    void severe(String message);

    void severe(String message, Throwable throwable);
}
