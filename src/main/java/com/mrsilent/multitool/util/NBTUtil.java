package com.mrsilent.multitool.util;

import com.mrsilent.multitool.model.MultiToolFlag;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Utility for reading and writing MultiTool data via the Bukkit
 * {@link PersistentDataContainer} (PDC) API.
 *
 * <p>PDC is built into Paper and does not require NBT-API, though
 * NBT-API is still listed as a soft-dependency for operator tooling.
 *
 * <p>All keys are namespaced under the plugin's namespace
 * ({@code multitool:...}).
 */
public final class NBTUtil {

    // ─── PDC Keys ────────────────────────────────────────────────────

    /** Stores the multitool definition id (e.g. "excellent_multitool"). */
    public static NamespacedKey KEY_ID;

    /** Stores the current type/mode key (e.g. "pickaxe"). */
    public static NamespacedKey KEY_TYPE;

    /** Stores a comma-joined list of active flags. */
    public static NamespacedKey KEY_FLAGS;

    /** Stores the original owner name at time of creation. */
    public static NamespacedKey KEY_OWNER;

    /**
     * Arbitrary user-defined NBT pairs from the YAML {@code nbt:} block.
     * Stored as separate keys under the namespace
     * {@code multitool.custom.<key>}.
     */
    private static Plugin plugin;
    private static java.util.Map<String, NamespacedKey> customKeyCache = new java.util.HashMap<>();

    private NBTUtil() {}

    // ─────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────

    /**
     * Must be called once from {@link com.mrsilent.multitool.MultiToolPlugin#onEnable()}.
     *
     * @param pl plugin instance
     */
    public static void init(Plugin pl) {
        plugin = pl;
        KEY_ID    = new NamespacedKey(pl, "id");
        KEY_TYPE  = new NamespacedKey(pl, "type");
        KEY_FLAGS = new NamespacedKey(pl, "flags");
        KEY_OWNER = new NamespacedKey(pl, "owner");
    }

    // ─────────────────────────────────────────────────────────────────
    // Write helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Writes the core multitool identity data onto an ItemMeta's PDC.
     *
     * @param meta  item meta (mutated in-place)
     * @param id    definition id
     * @param type  type/mode key
     * @param flags active flags
     * @param owner player name (may be null)
     */
    public static void writeCore(ItemMeta meta,
                                 String id,
                                 String type,
                                 Set<MultiToolFlag> flags,
                                 String owner) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ID,   PersistentDataType.STRING, id);
        pdc.set(KEY_TYPE, PersistentDataType.STRING, type);
        pdc.set(KEY_FLAGS, PersistentDataType.STRING, encodeFlagSet(flags));
        if (owner != null) pdc.set(KEY_OWNER, PersistentDataType.STRING, owner);
    }

    /**
     * Writes custom NBT key-value pairs from the YAML {@code nbt:} block.
     * Each key is stored as {@code multitool.custom.<key>}.
     *
     * @param meta    item meta (mutated in-place)
     * @param entries map of string→string pairs
     */
    public static void writeCustom(ItemMeta meta, java.util.Map<String, String> entries) {
        if (entries == null || entries.isEmpty()) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (java.util.Map.Entry<String, String> e : entries.entrySet()) {
            NamespacedKey key = getCustomKey(e.getKey());
            pdc.set(key, PersistentDataType.STRING, e.getValue());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Read helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the given item is a multitool.
     *
     * @param item item to check (may be null)
     * @return true if it has a multitool id in PDC
     */
    public static boolean isMultiTool(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(KEY_ID, PersistentDataType.STRING);
    }

    /**
     * Gets the multitool id stored in an item's PDC.
     *
     * @param item item
     * @return tool id, or null if not a multitool
     */
    public static String getToolId(ItemStack item) {
        if (!isMultiTool(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                   .get(KEY_ID, PersistentDataType.STRING);
    }

    /**
     * Gets the current type/mode key stored in an item's PDC.
     *
     * @param item item
     * @return type key, or null if not a multitool
     */
    public static String getToolType(ItemStack item) {
        if (!isMultiTool(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                   .get(KEY_TYPE, PersistentDataType.STRING);
    }

    /**
     * Gets the stored owner name from an item's PDC.
     *
     * @param item item
     * @return owner name, or null if not a multitool or no owner stored
     */
    public static String getOwner(ItemStack item) {
        if (!isMultiTool(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                   .get(KEY_OWNER, PersistentDataType.STRING);
    }

    /**
     * Reads the flag set from an item's PDC.
     *
     * @param item item
     * @return set of active flags (never null)
     */
    public static Set<MultiToolFlag> getFlags(ItemStack item) {
        if (!isMultiTool(item)) return EnumSet.noneOf(MultiToolFlag.class);
        String encoded = item.getItemMeta().getPersistentDataContainer()
                             .get(KEY_FLAGS, PersistentDataType.STRING);
        return decodeFlagSet(encoded);
    }

    // ─────────────────────────────────────────────────────────────────
    // Flag encoding
    // ─────────────────────────────────────────────────────────────────

    /** Encodes a flag set as a comma-separated string. */
    public static String encodeFlagSet(Set<MultiToolFlag> flags) {
        if (flags == null || flags.isEmpty()) return "";
        List<String> names = new ArrayList<>();
        for (MultiToolFlag f : flags) names.add(f.name());
        return String.join(",", names);
    }

    /** Decodes a comma-separated flag string back to a set. */
    public static Set<MultiToolFlag> decodeFlagSet(String encoded) {
        Set<MultiToolFlag> result = EnumSet.noneOf(MultiToolFlag.class);
        if (encoded == null || encoded.isBlank()) return result;
        for (String name : encoded.split(",")) {
            MultiToolFlag f = MultiToolFlag.fromString(name.trim());
            if (f != null) result.add(f);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // Custom key factory
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns (and caches) a NamespacedKey for a custom NBT field.
     * The key name is {@code custom.<field>}.
     *
     * @param field the custom field name (from YAML)
     * @return NamespacedKey
     */
    private static NamespacedKey getCustomKey(String field) {
        return customKeyCache.computeIfAbsent(
                "custom." + field.toLowerCase(),
                k -> new NamespacedKey(plugin, k));
    }
}
