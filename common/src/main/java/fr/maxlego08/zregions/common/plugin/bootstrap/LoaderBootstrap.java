package fr.maxlego08.zregions.common.plugin.bootstrap;

/**
 * Minimal platform-neutral lifecycle contract (LuckPerms' LoaderBootstrap, MIT).
 * The platform's thin loader plugin delegates its own lifecycle to this.
 */
public interface LoaderBootstrap {

    void onLoad();

    default void onEnable() {
    }

    default void onDisable() {
    }
}
