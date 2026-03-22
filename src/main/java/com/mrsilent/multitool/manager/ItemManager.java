package com.mrsilent.multitool.manager;

import com.mrsilent.multitool.MultiToolPlugin;
import com.mrsilent.multitool.model.MultiToolDefinition;
import com.mrsilent.multitool.model.MultiToolFlag;
import com.mrsilent.multitool.model.MultiToolType;
import com.mrsilent.multitool.util.AttributeUtil;
import com.mrsilent.multitool.util.ItemUtil;
import com.mrsilent.multitool.util.NBTUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Manages loading, caching, and resolving of all multitool definitions.
 *
 * <p>Definitions are loaded from {@code plugins/MultiTool/<items-folder>/*.yml}.
 * Each file must contain a top-level {@code multitool:} section.
 */
public class ItemManager {

    private final MultiToolPlugin plugin;
    private final Logger log;

    /** id → definition */
    private final Map<String, MultiToolDefinition> definitions = new LinkedHashMap<>();

    public ItemManager(MultiToolPlugin plugin) {
        this.plugin = plugin;
        this.log    = plugin.getLogger();
    }

    // ─────────────────────────────────────────────────────────────────
    // Load / Reload
    // ─────────────────────────────────────────────────────────────────

    /**
     * Clears all definitions and reloads them from disk.
     */
    public void loadAll() {
        definitions.clear();

        String folderName = plugin.getConfigManager().getItemsFolder();
        File itemsDir = new File(plugin.getDataFolder(), folderName);

        // Copy default items if the folder doesn't exist yet
        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
            plugin.saveResource(folderName + "/excellent_multitool.yml", false);
        }

        File[] files = itemsDir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            log.warning("[MultiTool] No item YAML files found in: " + itemsDir.getPath());
            return;
        }

        int loaded = 0;
        for (File file : files) {
            try {
                MultiToolDefinition def = loadDefinition(file);
                if (def != null) {
                    definitions.put(def.getId(), def);
                    loaded++;
                    plugin.debug("Loaded multitool: " + def.getId() + " from " + file.getName());
                }
            } catch (Exception e) {
                log.severe("[MultiTool] Failed to load " + file.getName() + ": " + e.getMessage());
                if (plugin.getConfigManager().isDebug()) e.printStackTrace();
            }
        }
        log.info("[MultiTool] Loaded " + loaded + " tool definition(s).");
    }

    // ─────────────────────────────────────────────────────────────────
    // YAML → Model
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parses a single YAML file into a {@link MultiToolDefinition}.
     *
     * @param file the YAML file
     * @return parsed definition, or null on failure
     */
    private MultiToolDefinition loadDefinition(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("multitool");
        if (root == null) {
            log.warning("[MultiTool] File " + file.getName() + " has no 'multitool:' section — skipping.");
            return null;
        }

        MultiToolDefinition def = new MultiToolDefinition();

        // ── Basic fields ─────────────────────────────────────────────
        String id = root.getString("id");
        if (id == null || id.isBlank()) id = file.getName().replace(".yml", "");
        def.setId(id.toLowerCase().trim());

        String materialStr = root.getString("material", "STONE");
        Material material = Material.matchMaterial(materialStr.toUpperCase());
        def.setMaterial(material != null ? material : Material.STONE);

        def.setDisplayName(root.getString("display-name", "&fMultiTool"));
        def.setLore(root.getStringList("lore"));
        def.setEnchants(root.getStringList("enchants"));
        def.setExcellentEnchants(root.getStringList("excellent-enchants"));
        def.setUnbreakable(root.getBoolean("unbreakable",
                plugin.getConfigManager().isGlobalUnbreakable()));

        // ── Flags ────────────────────────────────────────────────────
        def.setFlags(parseFlags(root.getStringList("flags")));

        // ── GUI overrides ────────────────────────────────────────────
        ConfigurationSection guiSec = root.getConfigurationSection("gui");
        if (guiSec != null) {
            if (guiSec.contains("rows"))              def.setGuiRows(guiSec.getInt("rows"));
            if (guiSec.contains("title"))             def.setGuiTitle(guiSec.getString("title"));
            if (guiSec.contains("include-base-slot")) def.setGuiIncludeBaseSlot(guiSec.getBoolean("include-base-slot"));
            if (guiSec.contains("open-sound"))        def.setGuiOpenSound(guiSec.getString("open-sound"));
        }

        // ── Switch sound ─────────────────────────────────────────────
        def.setSwitchSound(root.getString("switch-sound"));

        // ── Types ────────────────────────────────────────────────────
        ConfigurationSection typesSec = root.getConfigurationSection("types");
        if (typesSec != null) {
            Map<String, MultiToolType> types = new LinkedHashMap<>();
            for (String typeKey : typesSec.getKeys(false)) {
                ConfigurationSection ts = typesSec.getConfigurationSection(typeKey);
                if (ts == null) continue;
                MultiToolType type = loadType(typeKey, ts, def);
                types.put(typeKey, type);
            }
            def.setTypes(types);
        }

        return def;
    }

    /**
     * Parses a single type section into a {@link MultiToolType}.
     */
    private MultiToolType loadType(String key,
                                   ConfigurationSection sec,
                                   MultiToolDefinition def) {
        MultiToolType type = new MultiToolType(key);

        // Material (fall back to definition material)
        String matStr = sec.getString("material");
        if (matStr != null) {
            Material mat = Material.matchMaterial(matStr.toUpperCase());
            type.setMaterial(mat != null ? mat : def.getMaterial());
        } else {
            type.setMaterial(def.getMaterial());
        }

        type.setDisplayName(sec.getString("name", sec.getString("display-name",
                "&f" + capitalize(key) + " Mode")));
        type.setLore(sec.getStringList("lore"));
        type.setEnchants(sec.getStringList("enchants"));
        type.setExcellentEnchants(sec.getStringList("excellent-enchants"));
        type.setSlot(sec.getInt("slot", 0));
        type.setSwitchSound(sec.getString("sound", sec.getString("switch-sound")));
        type.setSwitchTitle(sec.getString("switch-title"));
        type.setSwitchSubtitle(sec.getString("switch-subtitle"));

        // Attributes
        ConfigurationSection attrSec = sec.getConfigurationSection("attributes");
        type.setAttributes(AttributeUtil.parseSection(attrSec));

        // Flags — type flags do NOT inherit from definition; they're additive at apply-time
        type.setFlags(parseFlags(sec.getStringList("flags")));

        // NBT
        ConfigurationSection nbtSec = sec.getConfigurationSection("nbt");
        if (nbtSec != null) {
            Map<String, String> nbtMap = new HashMap<>();
            for (String nbtKey : nbtSec.getKeys(false)) {
                nbtMap.put(nbtKey, String.valueOf(nbtSec.get(nbtKey)));
            }
            type.setNbt(nbtMap);
        }

        return type;
    }

    // ─────────────────────────────────────────────────────────────────
    // Flag parsing
    // ─────────────────────────────────────────────────────────────────

    private Set<MultiToolFlag> parseFlags(List<String> flagStrings) {
        Set<MultiToolFlag> result = EnumSet.noneOf(MultiToolFlag.class);
        if (flagStrings == null) return result;
        for (String s : flagStrings) {
            MultiToolFlag f = MultiToolFlag.fromString(s);
            if (f != null) {
                result.add(f);
            } else {
                log.warning("[MultiTool] Unknown flag: " + s);
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // Rebuild (called on reload)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Examines a player's main-hand item. If it's a multitool, rebuilds it
     * in-place with updated definition data while preserving damage and
     * custom model data.
     *
     * @param player online player to check
     */
    public void rebuildHeldTool(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!NBTUtil.isMultiTool(hand)) return;

        String id   = NBTUtil.getToolId(hand);
        String type = NBTUtil.getToolType(hand);

        MultiToolDefinition def = definitions.get(id);
        if (def == null) return;

        MultiToolType typeObj = (type != null && !type.equals("base")) ? def.getType(type) : null;
        ItemStack rebuilt = ItemUtil.rebuild(plugin, hand, def, typeObj, player);

        player.getInventory().setItemInMainHand(rebuilt);
        plugin.debug("Rebuilt held tool for " + player.getName() + " → " + id + ":" + type);
    }

    // ─────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────

    public MultiToolDefinition getDefinition(String id) { return definitions.get(id); }
    public Collection<MultiToolDefinition> getDefinitions() { return definitions.values(); }
    public boolean isLoaded(String id) { return definitions.containsKey(id); }

    // ─────────────────────────────────────────────────────────────────
    // Util
    // ─────────────────────────────────────────────────────────────────

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
