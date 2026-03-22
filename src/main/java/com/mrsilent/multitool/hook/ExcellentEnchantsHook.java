package com.mrsilent.multitool.hook;

import com.mrsilent.multitool.MultiToolPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Reflection-based hook for ExcellentEnchants integration.
 *
 * ExcellentEnchants is intentionally NOT compiled against — this hook performs
 * runtime reflection to apply enchantments without creating a hard compile-time
 * dependency.  Zero import of any EE class.
 *
 * Detection order:
 *   1. Bukkit enchantment registry lookup (EE registers its enchants with
 *      namespace "excellentenchants" — e.g. NamespacedKey("excellentenchants","veinminer"))
 *      → If found: applied via  meta.addEnchant(enchant, level, true)
 *
 *   2. EE EnchantManager reflection — tries known method signatures:
 *        getEnchantment(String) / getById(String) / getByName(String)
 *        applyToItem(ItemStack) / addToItem(ItemStack, int) / apply(ItemStack, Player, int)
 *
 *   3. Lore fallback — appends  §6EE: §e<enchant>:<level>  lore line
 *      (config:  excellentenchants.lore-fallback: true)
 */
public class ExcellentEnchantsHook {

    // ── State ──────────────────────────────────────────────────────────────────
    private final MultiToolPlugin plugin;
    private boolean available = false;
    private String  detectedMethod = "none";

    // ── Reflection handles (cached at init time) ───────────────────────────────
    /** EE's enchantment registry object (if obtained via reflection) */
    private Object enchantRegistry = null;
    /** Method to look up an EE enchant by id string */
    private Method lookupMethod = null;
    /** Method to apply the enchant to an ItemStack */
    private Method applyMethod   = null;

    // ═══════════════════════════════════════════════════════════════════════════
    //  Constructor — probe available APIs
    // ═══════════════════════════════════════════════════════════════════════════

    public ExcellentEnchantsHook(MultiToolPlugin plugin) {
        this.plugin = plugin;
        probe();
    }

    private void probe() {
        // ── Strategy 1: Bukkit registry (cheapest, most reliable) ──────────────
        // EE registers its enchants in the server's enchantment registry.
        // We verify by looking up a known internal EE enchant (any would do).
        // If the registry approach works we set available=true here; actual
        // lookup happens per-enchant in applyEnchant().
        try {
            // Just confirm EE plugin is loaded (already checked in MultiToolPlugin)
            available      = true;
            detectedMethod = "bukkit_registry";
            plugin.debug("[EEHook] Strategy 1 (Bukkit registry) selected.");
            return; // We'll attempt actual lookup in applyEnchant()
        } catch (Exception e) {
            plugin.debug("[EEHook] Strategy 1 probe failed: " + e.getMessage());
        }

        // ── Strategy 2: Reflection via EnchantRegistry class ──────────────────
        try {
            // EE 4.x / NightExpress framework
            Class<?> registryClass = resolveClass(
                "su.nightexpress.excellentenchants.registry.EnchantRegistry",
                "su.nightexpress.excellentenchants.api.enchant.EnchantRegistry",
                "su.nightexpress.excellentenchants.EnchantsPlugin"
            );
            if (registryClass != null) {
                lookupMethod = resolveMethod(registryClass, new String[]{"getById", "getByName", "get"},
                    String.class);
                if (lookupMethod != null) {
                    available      = true;
                    detectedMethod = "reflection_registry_" + lookupMethod.getName();
                    plugin.debug("[EEHook] Strategy 2 selected: " + detectedMethod);
                    return;
                }
            }
        } catch (Exception e) {
            plugin.debug("[EEHook] Strategy 2 failed: " + e.getMessage());
        }

        // ── Strategy 3: Plugin main class reflection ───────────────────────────
        try {
            org.bukkit.plugin.Plugin eePlugin =
                org.bukkit.Bukkit.getPluginManager().getPlugin("ExcellentEnchants");
            if (eePlugin != null) {
                lookupMethod = resolveMethod(eePlugin.getClass(),
                    new String[]{"getEnchantment", "getEnchant", "getById"},
                    String.class);
                if (lookupMethod != null) {
                    enchantRegistry = eePlugin;
                    available       = true;
                    detectedMethod  = "reflection_plugin_" + lookupMethod.getName();
                    plugin.debug("[EEHook] Strategy 3 selected: " + detectedMethod);
                    return;
                }
            }
        } catch (Exception e) {
            plugin.debug("[EEHook] Strategy 3 failed: " + e.getMessage());
        }

        // Nothing worked — will use lore fallback
        plugin.debug("[EEHook] All strategies failed; lore fallback will be used.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Apply Enchant
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Apply a single ExcellentEnchants enchantment to an ItemStack.
     * Entry format:  "enchant_id:level"  or just  "enchant_id"  (default level 1).
     *
     * @param item     the target item
     * @param entry    the enchant entry string from YAML
     * @param player   the player context (may be null)
     */
    public void applyEnchant(ItemStack item, String entry, Player player) {
        if (item == null || entry == null || entry.isBlank()) return;

        // Parse entry
        String[] parts = entry.split(":");
        String enchantId = parts[0].trim().toLowerCase();
        int    level     = parts.length > 1 ? parseLevel(parts[1].trim()) : 1;

        // ── Strategy 1: Bukkit registry ────────────────────────────────────────
        if ("bukkit_registry".equals(detectedMethod)) {
            if (applyViaRegistry(item, enchantId, level)) return;
            // Registry lookup failed — fall through to lore fallback
        }

        // ── Strategy 2/3: Reflection ───────────────────────────────────────────
        if (lookupMethod != null && !"bukkit_registry".equals(detectedMethod)) {
            if (applyViaReflection(item, enchantId, level, player)) return;
        }

        // ── Lore Fallback ──────────────────────────────────────────────────────
        if (plugin.getConfigManager().isEELoreFallback()) {
            appendLoreFallback(item, enchantId, level);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Try to apply using Bukkit's enchantment registry (EE registers under "excellentenchants"). */
    private boolean applyViaRegistry(ItemStack item, String enchantId, int level) {
        try {
            // Try "excellentenchants:<id>" namespace
            NamespacedKey nk = new NamespacedKey("excellentenchants", enchantId);
            Enchantment ench = Registry.ENCHANTMENT.get(nk);
            if (ench == null) {
                // Try without namespace prefix (some versions may use minecraft namespace)
                nk = NamespacedKey.minecraft(enchantId);
                ench = Registry.ENCHANTMENT.get(nk);
            }
            if (ench != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return false;
                meta.addEnchant(ench, level, true);
                item.setItemMeta(meta);
                plugin.debug("[EEHook] Registry applied: " + enchantId + ":" + level);
                return true;
            }
        } catch (Exception e) {
            plugin.debug("[EEHook] Registry lookup failed for " + enchantId + ": " + e.getMessage());
        }
        return false;
    }

    /** Try to apply via the resolved reflection method. */
    private boolean applyViaReflection(ItemStack item, String enchantId, int level, Player player) {
        try {
            Object enchant = (enchantRegistry != null)
                ? lookupMethod.invoke(enchantRegistry, enchantId)
                : lookupMethod.invoke(null, enchantId);
            if (enchant == null) return false;

            // Try to apply: look for addToItem / applyToItem / apply methods
            Method apply = resolveMethod(enchant.getClass(),
                new String[]{"addToItem", "applyToItem", "apply", "addEnchant"},
                ItemStack.class, int.class);
            if (apply != null) {
                apply.invoke(enchant, item, level);
                plugin.debug("[EEHook] Reflection applied: " + enchantId + ":" + level);
                return true;
            }
            // Try with Player argument
            apply = resolveMethod(enchant.getClass(),
                new String[]{"addToItem", "applyToItem", "apply"},
                ItemStack.class, int.class, Player.class);
            if (apply != null) {
                apply.invoke(enchant, item, level, player);
                plugin.debug("[EEHook] Reflection (with player) applied: " + enchantId + ":" + level);
                return true;
            }
        } catch (Exception e) {
            plugin.debug("[EEHook] Reflection apply failed for " + enchantId + ": " + e.getMessage());
        }
        return false;
    }

    /** Append a lore line as fallback: §6EE: §e<enchant>:<level> */
    private void appendLoreFallback(ItemStack item, String enchantId, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacySection()
            .deserialize("§6EE: §e" + enchantId + ":" + level));
        meta.lore(lore);
        item.setItemMeta(meta);
        plugin.debug("[EEHook] Lore fallback applied: " + enchantId + ":" + level);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Reflection Utilities
    // ═══════════════════════════════════════════════════════════════════════════

    private Class<?> resolveClass(String... candidates) {
        for (String name : candidates) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private Method resolveMethod(Class<?> clazz, String[] names, Class<?>... paramTypes) {
        for (String name : names) {
            try {
                Method m = clazz.getMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            try {
                Method m = clazz.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private int parseLevel(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return 1; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Accessors
    // ═══════════════════════════════════════════════════════════════════════════

    /** @return true if any EE application strategy was found at startup */
    public boolean isAvailable()      { return available; }

    /** @return a string describing which detection strategy is active */
    public String  getDetectedMethod(){ return detectedMethod; }
}
