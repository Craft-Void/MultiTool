package com.mrsilent.multitool.config;

import com.mrsilent.multitool.MultiToolPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * ConfigManager wraps config.yml access and caches all settings.
 * All fields are re-populated on {@link #reload()}.
 */
public class ConfigManager {

    private final MultiToolPlugin plugin;

    // ── Cached fields ──────────────────────────────────────────────────────────
    private String  itemsFolder;
    private boolean globalUnbreakable;

    // GUI defaults
    private int     guiRows;
    private String  guiTitle;
    private boolean guiIncludeBaseSlot;
    private String  guiOpenSound;
    private float   guiOpenVolume;
    private float   guiOpenPitch;
    private int     guiCooldownMs;

    // Switch defaults
    private String  switchTitle;
    private String  switchSubtitle;
    private int     switchFadeIn;
    private int     switchStay;
    private int     switchFadeOut;
    private String  switchSound;
    private float   switchVolume;
    private float   switchPitch;

    // Hooks
    private boolean papiEnabled;
    private boolean papiSafeMode;
    private boolean eeEnabled;
    private boolean eeLoreFallback;

    // Protection
    private boolean protectBlockPluginGuis;
    private boolean protectBlockDoubleChest;

    // Debug
    private boolean debug;

    // Messages
    private String msgPrefix;
    private String msgReloadSuccess;
    private String msgGiveSuccess;
    private String msgGiveUnknownTool;
    private String msgGiveUnknownPlayer;
    private String msgListHeader;
    private String msgListEntry;
    private String msgNoPermission;
    private String msgNoTypes;
    private String msgCooldown;
    private String msgExportSuccess;
    private String msgCreateSuccess;
    private String msgCreateExists;
    private String msgRequirePermission;
    private String msgNoSwitch;
    private String msgSneakRequired;

    // ═══════════════════════════════════════════════════════════════════════════════

    public ConfigManager(MultiToolPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Reads (or re-reads) all values from config.yml.
     */
    public void reload() {
        FileConfiguration cfg = plugin.getConfig();

        itemsFolder       = cfg.getString("items-folder", "items");
        globalUnbreakable = cfg.getBoolean("global-unbreakable", true);

        // GUI
        guiRows             = Math.max(1, Math.min(6, cfg.getInt("gui.rows", 3)));
        guiTitle            = cfg.getString("gui.title", "&8[&6MultiTool&8] &7Select Mode");
        guiIncludeBaseSlot  = cfg.getBoolean("gui.include-base-slot", true);
        guiOpenSound        = cfg.getString("gui.open-sound", "BLOCK_NOTE_BLOCK_PLING");
        guiOpenVolume       = (float) cfg.getDouble("gui.open-volume", 1.0);
        guiOpenPitch        = (float) cfg.getDouble("gui.open-pitch", 1.0);
        guiCooldownMs       = cfg.getInt("gui.cooldown-ms", 300);

        // Switch
        switchTitle    = cfg.getString("switch.title", "&6Mode Switched");
        switchSubtitle = cfg.getString("switch.subtitle", "&7Now using {type}");
        switchFadeIn   = cfg.getInt("switch.fade-in", 5);
        switchStay     = cfg.getInt("switch.stay", 25);
        switchFadeOut  = cfg.getInt("switch.fade-out", 5);
        switchSound    = cfg.getString("switch.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        switchVolume   = (float) cfg.getDouble("switch.volume", 1.0);
        switchPitch    = (float) cfg.getDouble("switch.pitch", 1.2);

        // PAPI
        papiEnabled  = cfg.getBoolean("papi.enable", true);
        papiSafeMode = cfg.getBoolean("papi.safe-mode", true);

        // EE
        eeEnabled      = cfg.getBoolean("excellentenchants.enable", true);
        eeLoreFallback = cfg.getBoolean("excellentenchants.lore-fallback", true);

        // Protection
        protectBlockPluginGuis  = cfg.getBoolean("protection.block-plugin-guis", false);
        protectBlockDoubleChest = cfg.getBoolean("protection.block-double-chest", false);

        // Debug
        debug = cfg.getBoolean("debug", false);

        // Messages
        msgPrefix             = cfg.getString("messages.prefix", "&8[&6MultiTool&8] ");
        msgReloadSuccess      = cfg.getString("messages.reload-success",  "&aReloaded. Loaded &e{count} &atool(s).");
        msgGiveSuccess        = cfg.getString("messages.give-success",    "&aGave &e{id} &ato &e{player}&a.");
        msgGiveUnknownTool    = cfg.getString("messages.give-unknown-tool","&cUnknown tool: &e{id}");
        msgGiveUnknownPlayer  = cfg.getString("messages.give-unknown-player","&cPlayer not found: &e{player}");
        msgListHeader         = cfg.getString("messages.list-header",     "&6Loaded MultiTools &7({count}):");
        msgListEntry          = cfg.getString("messages.list-entry",      "  &8- &e{id} &7({types} types)");
        msgNoPermission       = cfg.getString("messages.no-permission",   "&cYou lack permission.");
        msgNoTypes            = cfg.getString("messages.no-types",        "&cThis tool has no types.");
        msgCooldown           = cfg.getString("messages.cooldown",        "&cWait before opening again.");
        msgExportSuccess      = cfg.getString("messages.export-success",  "&aExported to &e{file}");
        msgCreateSuccess      = cfg.getString("messages.create-success",  "&aTemplate created: items/{id}.yml");
        msgCreateExists       = cfg.getString("messages.create-exists",   "&cTool &e{id} &calready exists.");
        msgRequirePermission  = cfg.getString("messages.require-permission","&cNeed: multitool.use.{id}.{type}");
        msgNoSwitch           = cfg.getString("messages.no-switch",       "&cThis mode cannot be selected.");
        msgSneakRequired      = cfg.getString("messages.sneak-required",  "&7Sneak + Right-Click to switch.");
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public String  getItemsFolder()           { return itemsFolder; }
    public boolean isGlobalUnbreakable()      { return globalUnbreakable; }

    public int     getGuiRows()               { return guiRows; }
    public String  getGuiTitle()              { return guiTitle; }
    public boolean isGuiIncludeBaseSlot()     { return guiIncludeBaseSlot; }
    public String  getGuiOpenSound()          { return guiOpenSound; }
    public float   getGuiOpenVolume()         { return guiOpenVolume; }
    public float   getGuiOpenPitch()          { return guiOpenPitch; }
    public int     getGuiCooldownMs()         { return guiCooldownMs; }

    public String  getSwitchTitle()           { return switchTitle; }
    public String  getSwitchSubtitle()        { return switchSubtitle; }
    public int     getSwitchFadeIn()          { return switchFadeIn; }
    public int     getSwitchStay()            { return switchStay; }
    public int     getSwitchFadeOut()         { return switchFadeOut; }
    public String  getSwitchSound()           { return switchSound; }
    public float   getSwitchVolume()          { return switchVolume; }
    public float   getSwitchPitch()           { return switchPitch; }

    public boolean isPAPIEnabled()            { return papiEnabled; }
    public boolean isPAPISafeMode()           { return papiSafeMode; }
    public boolean isEEEnabled()              { return eeEnabled; }
    public boolean isEELoreFallback()         { return eeLoreFallback; }

    public boolean isProtectBlockPluginGuis() { return protectBlockPluginGuis; }
    public boolean isProtectBlockDoubleChest(){ return protectBlockDoubleChest; }

    /** Alias for {@link #getSwitchVolume()} — used internally. */
    public float   getSwitchSoundVolume()     { return switchVolume; }
    /** Alias for {@link #getSwitchPitch()} — used internally. */
    public float   getSwitchSoundPitch()      { return switchPitch; }

    public boolean isDebug()                  { return debug; }

    public String getMsgPrefix()              { return msgPrefix; }
    public String getMsgReloadSuccess()       { return msgReloadSuccess; }
    public String getMsgGiveSuccess()         { return msgGiveSuccess; }
    public String getMsgGiveUnknownTool()     { return msgGiveUnknownTool; }
    public String getMsgGiveUnknownPlayer()   { return msgGiveUnknownPlayer; }
    public String getMsgListHeader()          { return msgListHeader; }
    public String getMsgListEntry()           { return msgListEntry; }
    public String getMsgNoPermission()        { return msgNoPermission; }
    public String getMsgNoTypes()             { return msgNoTypes; }
    public String getMsgCooldown()            { return msgCooldown; }
    public String getMsgExportSuccess()       { return msgExportSuccess; }
    public String getMsgCreateSuccess()       { return msgCreateSuccess; }
    public String getMsgCreateExists()        { return msgCreateExists; }
    public String getMsgRequirePermission()   { return msgRequirePermission; }
    public String getMsgNoSwitch()            { return msgNoSwitch; }
    public String getMsgSneakRequired()       { return msgSneakRequired; }
}
