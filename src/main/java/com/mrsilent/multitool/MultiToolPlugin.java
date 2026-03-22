package com.mrsilent.multitool;

import com.mrsilent.multitool.command.MultiToolCommand;
import com.mrsilent.multitool.config.ConfigManager;
import com.mrsilent.multitool.gui.MultiToolGUI;
import com.mrsilent.multitool.hook.ExcellentEnchantsHook;
import com.mrsilent.multitool.hook.PlaceholderHook;
import com.mrsilent.multitool.listener.InventoryClickListener;
import com.mrsilent.multitool.listener.PlayerInteractListener;
import com.mrsilent.multitool.manager.ItemManager;
import com.mrsilent.multitool.util.NBTUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MultiTool — main plugin class.
 *
 * <p>Entry point for all plugin systems: config, item loading, GUI, hooks,
 * listeners, and commands.
 *
 * <ul>
 *   <li>Package: {@code com.mrsilent.multitool}</li>
 *   <li>Target:  Paper 1.21.x, Java 17</li>
 *   <li>Author:  MrSilent</li>
 * </ul>
 */
public final class MultiToolPlugin extends JavaPlugin {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static MultiToolPlugin instance;

    // ── Core Managers ─────────────────────────────────────────────────────────
    private ConfigManager configManager;
    private ItemManager   itemManager;

    // ── Hooks ─────────────────────────────────────────────────────────────────
    private ExcellentEnchantsHook eeHook;

    // ═════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void onEnable() {
        instance = this;

        // ── Default resources ────────────────────────────────────────────────
        saveDefaultConfig();
        saveDefaultItems();

        // ── Init NBT keys ────────────────────────────────────────────────────
        NBTUtil.init(this);

        // ── Managers ─────────────────────────────────────────────────────────
        this.configManager = new ConfigManager(this);
        this.itemManager   = new ItemManager(this);
        this.itemManager.loadAll();

        // ── Hooks ────────────────────────────────────────────────────────────
        initHooks();

        // ── Listeners ────────────────────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);

        // ── Commands ─────────────────────────────────────────────────────────
        var cmd = getCommand("multitool");
        if (cmd != null) {
            MultiToolCommand handler = new MultiToolCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        } else {
            getLogger().severe("Command 'multitool' not found in plugin.yml!");
        }

        getLogger().info(
            "MultiTool v" + getDescription().getVersion() +
            " enabled — " + itemManager.getDefinitions().size() + " tool(s) loaded."
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("MultiTool disabled.");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Reload
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fully reloads config, items, and rebuilds held multitools for all
     * online players.
     */
    public void reload() {
        reloadConfig();
        configManager.reload();
        itemManager.loadAll();

        // Rebuild every online player's held tool to reflect updated definitions
        for (Player player : Bukkit.getOnlinePlayers()) {
            itemManager.rebuildHeldTool(player);
        }

        getLogger().info(
            "MultiTool reloaded — " + itemManager.getDefinitions().size() + " tool(s) loaded."
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════

    /** Saves the example item YAML the first time if the items folder is missing. */
    private void saveDefaultItems() {
        java.io.File itemsDir = new java.io.File(
                getDataFolder(),
                getConfig().getString("items-folder", "items"));
        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
            saveResource("items/excellent_multitool.yml", false);
        }
    }

    /** Initialises optional soft-dependency hooks. */
    private void initHooks() {
        // ExcellentEnchants — reflection only, zero compile dependency
        if (getServer().getPluginManager().isPluginEnabled("ExcellentEnchants")
                && configManager.isEEEnabled()) {
            this.eeHook = new ExcellentEnchantsHook(this);
            if (eeHook.isAvailable()) {
                getLogger().info("ExcellentEnchants hook active (method: "
                        + eeHook.getDetectedMethod() + ").");
            } else {
                getLogger().warning("ExcellentEnchants found but hook failed → using lore fallback.");
            }
        }

        // PlaceholderAPI
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")
                && configManager.isPAPIEnabled()) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Accessors
    // ═════════════════════════════════════════════════════════════════════════

    /** @return the singleton plugin instance */
    public static MultiToolPlugin getInstance() { return instance; }

    /** @return the {@link ConfigManager} */
    public ConfigManager getConfigManager() { return configManager; }

    /** @return the {@link ItemManager} */
    public ItemManager getItemManager() { return itemManager; }

    /** @return the ExcellentEnchants hook (may be null) */
    public ExcellentEnchantsHook getEEHook() { return eeHook; }

    /** @return true when debug logging is enabled */
    public boolean isDebug() { return configManager.isDebug(); }

    /**
     * Logs a debug message if debug mode is enabled.
     *
     * @param message the message to log
     */
    public void debug(String message) {
        if (isDebug()) getLogger().info("[DEBUG] " + message);
    }
}
