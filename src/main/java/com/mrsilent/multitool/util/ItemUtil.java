package com.mrsilent.multitool.util;

import com.mrsilent.multitool.MultiToolPlugin;
import com.mrsilent.multitool.hook.ExcellentEnchantsHook;
import com.mrsilent.multitool.model.MultiToolDefinition;
import com.mrsilent.multitool.model.MultiToolFlag;
import com.mrsilent.multitool.model.MultiToolType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Builds {@link ItemStack}s from {@link MultiToolDefinition} and
 * {@link MultiToolType} data.
 *
 * <p>This is the central factory for all item construction. It applies:
 * <ul>
 *   <li>Material</li>
 *   <li>Display name (Adventure Component)</li>
 *   <li>Lore (Adventure Component list)</li>
 *   <li>Vanilla enchantments</li>
 *   <li>ExcellentEnchants enchantments (via hook)</li>
 *   <li>Attribute modifiers</li>
 *   <li>PDC/NBT data</li>
 *   <li>Item flags (hide-attributes etc.)</li>
 *   <li>Unbreakable state</li>
 *   <li>Custom model data preservation</li>
 * </ul>
 */
public final class ItemUtil {

    private ItemUtil() {}

    // ─────────────────────────────────────────────────────────────────
    // Primary builders
    // ─────────────────────────────────────────────────────────────────

    /**
     * Builds the "base" item for a tool definition (no type selected).
     *
     * @param plugin plugin instance
     * @param def    definition
     * @param player optional player for placeholder resolution
     * @return built ItemStack
     */
    public static ItemStack buildBase(MultiToolPlugin plugin,
                                      MultiToolDefinition def,
                                      Player player) {
        ItemStack item = new ItemStack(def.getMaterial());
        applyMeta(plugin, item, def, null, player, 1, -1);
        return item;
    }

    /**
     * Builds a typed item (specific mode).
     *
     * @param plugin       plugin instance
     * @param def          definition
     * @param type         the target type
     * @param player       optional player
     * @param amount       stack amount
     * @param customModel  custom model data to preserve (-1 = ignore)
     * @return built ItemStack
     */
    public static ItemStack buildTyped(MultiToolPlugin plugin,
                                       MultiToolDefinition def,
                                       MultiToolType type,
                                       Player player,
                                       int amount,
                                       int customModel) {
        Material mat = (type.getMaterial() != null) ? type.getMaterial() : def.getMaterial();
        ItemStack item = new ItemStack(mat);
        applyMeta(plugin, item, def, type, player, amount, customModel);
        return item;
    }

    /**
     * Rebuilds an existing multitool in-place, preserving damage and
     * optionally preserving its custom model data.
     *
     * @param plugin   plugin instance
     * @param existing the existing item in the player's hand
     * @param def      definition (re-loaded from ItemManager)
     * @param type     current type (from PDC)
     * @param player   player
     * @return rebuilt ItemStack
     */
    public static ItemStack rebuild(MultiToolPlugin plugin,
                                    ItemStack existing,
                                    MultiToolDefinition def,
                                    MultiToolType type,
                                    Player player) {
        int amount = existing.getAmount();
        int cmd = -1;
        if (existing.hasItemMeta()) {
            ItemMeta m = existing.getItemMeta();
            if (m.hasCustomModelData()) cmd = m.getCustomModelData();
        }

        return (type != null)
                ? buildTyped(plugin, def, type, player, amount, cmd)
                : buildBase(plugin, def, player);
    }

    // ─────────────────────────────────────────────────────────────────
    // Meta application
    // ─────────────────────────────────────────────────────────────────

    /**
     * Applies all meta (name, lore, enchants, attributes, NBT…) to an item.
     *
     * @param plugin      plugin instance
     * @param item        item to mutate
     * @param def         tool definition
     * @param type        type (null = base item)
     * @param player      player for placeholders
     * @param amount      desired stack size
     * @param customModel custom model data to preserve (-1 = skip)
     */
    private static void applyMeta(MultiToolPlugin plugin,
                                  ItemStack item,
                                  MultiToolDefinition def,
                                  MultiToolType type,
                                  Player player,
                                  int amount,
                                  int customModel) {

        String id      = def.getId();
        String typeKey = (type != null) ? type.getKey() : "base";

        // ── Merge flags ──────────────────────────────────────────────
        Set<MultiToolFlag> mergedFlags = EnumSet.noneOf(MultiToolFlag.class);
        mergedFlags.addAll(def.getFlags());
        if (type != null) mergedFlags.addAll(type.getFlags());

        // ── Amount ───────────────────────────────────────────────────
        int finalAmount = amount;
        if (mergedFlags.contains(MultiToolFlag.PRESERVE_AMOUNT)) {
            finalAmount = Math.max(1, amount);
        } else if (mergedFlags.contains(MultiToolFlag.UNSTACKABLE)) {
            finalAmount = 1;
        }
        item.setAmount(finalAmount);

        // ── Build ItemMeta ───────────────────────────────────────────
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Display name
        String rawName = (type != null && type.getDisplayName() != null)
                ? type.getDisplayName()
                : def.getDisplayName();
        Component displayName = ColorUtil.process(player, rawName, id, typeKey);
        meta.displayName(displayName);

        // Lore: merge base lore + type lore
        List<Component> loreComponents = new ArrayList<>();
        for (String line : def.getLore()) {
            loreComponents.add(ColorUtil.process(player, line, id, typeKey));
        }
        if (type != null) {
            for (String line : type.getLore()) {
                loreComponents.add(ColorUtil.process(player, line, id, typeKey));
            }
        }
        meta.lore(loreComponents);

        // Unbreakable
        meta.setUnbreakable(def.isUnbreakable());

        // Custom model data
        if (customModel >= 0 && mergedFlags.contains(MultiToolFlag.KEEP_CUSTOM_MODEL_DATA)) {
            meta.setCustomModelData(customModel);
        }

        // Item flags
        if (mergedFlags.contains(MultiToolFlag.HIDE_ATTRIBUTES)) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        // Always hide the unbreakable tag
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        // Apply attributes
        if (type != null && !mergedFlags.contains(MultiToolFlag.NO_ENCHANTS)) {
            AttributeUtil.applyAttributes(meta, type, plugin.getName());
        }

        // Write PDC data
        NBTUtil.writeCore(meta, id, typeKey, mergedFlags,
                player != null ? player.getName() : null);
        if (type != null) NBTUtil.writeCustom(meta, type.getNbt());

        item.setItemMeta(meta);

        // ── Enchantments (after setItemMeta to avoid override) ───────
        if (!mergedFlags.contains(MultiToolFlag.NO_ENCHANTS)) {
            applyEnchants(plugin, item, def, type);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Enchantment helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Applies vanilla and EE enchantments from the definition + type.
     *
     * <p>Type enchants are merged on top of base enchants; type wins on
     * conflicts (higher level applied).
     */
    @SuppressWarnings("deprecation")
    private static void applyEnchants(MultiToolPlugin plugin,
                                      ItemStack item,
                                      MultiToolDefinition def,
                                      MultiToolType type) {
        // Remove all existing enchants first
        for (Enchantment e : item.getEnchantments().keySet()) {
            item.removeEnchantment(e);
        }

        // Apply base enchants
        applyVanillaEnchantList(item, def.getEnchants());

        // Apply type enchants (override base)
        if (type != null) applyVanillaEnchantList(item, type.getEnchants());

        // Apply EE enchants
        ExcellentEnchantsHook eeHook = plugin.getEEHook();
        if (eeHook != null && eeHook.isAvailable()) {
            applyEEEnchantList(plugin, item, def.getExcellentEnchants(), null);
            if (type != null) applyEEEnchantList(plugin, item, type.getExcellentEnchants(), null);
        } else {
            // EE not available — append lore fallback
            appendEELore(item, def.getExcellentEnchants());
            if (type != null) appendEELore(item, type.getExcellentEnchants());
        }
    }

    /**
     * Parses and applies a list of "enchant:level" vanilla enchantments.
     */
    @SuppressWarnings("deprecation")
    private static void applyVanillaEnchantList(ItemStack item, List<String> enchants) {
        if (enchants == null) return;
        for (String entry : enchants) {
            String[] parts = entry.split(":");
            if (parts.length < 1) continue;
            String enchName = parts[0].trim().toLowerCase();
            int level = (parts.length >= 2) ? parseIntSafe(parts[1], 1) : 1;

            Enchantment ench = Enchantment.getByName(enchName.toUpperCase());
            if (ench == null) {
                // Try minecraft key (Paper 1.21+)
                ench = org.bukkit.Registry.ENCHANTMENT.get(
                        org.bukkit.NamespacedKey.minecraft(enchName));
            }
            if (ench != null) {
                item.addUnsafeEnchantment(ench, level);
            }
        }
    }

    /**
     * Applies EE enchantments via the reflection hook.
     */
    private static void applyEEEnchantList(MultiToolPlugin plugin,
                                           ItemStack item,
                                           List<String> enchants,
                                           Player player) {
        if (enchants == null) return;
        ExcellentEnchantsHook hook = plugin.getEEHook();
        for (String entry : enchants) {
            String[] parts = entry.split(":");
            if (parts.length < 1) continue;
            String enchName = parts[0].trim();
            int level = (parts.length >= 2) ? parseIntSafe(parts[1], 1) : 1;
            hook.applyEnchant(item, enchName + ":" + level, player);
        }
    }

    /**
     * Fallback: appends EE enchant info as lore lines when EE API unavailable.
     */
    private static void appendEELore(ItemStack item, List<String> enchants) {
        if (enchants == null || enchants.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        for (String entry : enchants) {
            lore.add(ColorUtil.of("§6EE: §e" + entry));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    // ─────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
