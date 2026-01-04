package net.retrolilly.lillyFetch;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LillyFetch extends JavaPlugin {

    private CacheManager cacheManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cacheManager = new CacheManager();

        PluginCommand fetchCommand = getCommand("neofetch");
        if (fetchCommand != null) {
            fetchCommand.setExecutor(new LillyFetchCommand(this));
        }

        getLogger().info("LillyFetch enabled! Cache duration: " + getConfig().getInt("cache-duration", 60) + "s");
    }

    @Override
    public void onDisable() {
        if (cacheManager != null) {
            cacheManager.clear();
        }
        getLogger().info("LillyFetch has been disabled!");
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }
}
