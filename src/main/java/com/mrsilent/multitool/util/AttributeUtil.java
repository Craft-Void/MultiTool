package com.mrsilent.multitool.util;

import com.mrsilent.multitool.model.MultiToolType;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility for reading and applying Bukkit {@link Attribute} modifiers to item meta.
 *
 * <p><b>Paper 1.21.4 note:</b> {@code Attribute.values()} and {@code Attribute.valueOf()}
 * are deprecated in favour of the Registry API. This class uses
 * {@code Registry.ATTRIBUTE} for all lookups to avoid deprecation warnings and
 * ensure forward compatibility.
 *
 * <p>All applied modifiers use NamespacedKey namespace {@code "multitool"} so they
 * can be cleanly removed on type switches without touching other plugins' modifiers.
 */
public final class AttributeUtil {

    /** Namespace used for all modifier NamespacedKeys applied by this plugin. */
    private static final String NAMESPACE = "multitool";

    private AttributeUtil() {}

    // =========================================================================
    //  YAML → Model
    // =========================================================================

    /**
     * Parses an {@code attributes:} YAML section into a {@code Map<Attribute, Double>}.
     *
     * <pre>
     * attributes:
     *   GENERIC_ATTACK_DAMAGE: 8.0
     *   GENERIC_MOVEMENT_SPEED: 0.05
     * </pre>
     *
     * @param section the configuration section (may be null)
     * @return parsed map; never null, may be empty
     */
    public static Map<Attribute, Double> parseSection(ConfigurationSection section) {
        Map<Attribute, Double> result = new HashMap<>();
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            Attribute attr = parseAttribute(key, null);
            if (attr == null) continue;
            try {
                result.put(attr, section.getDouble(key));
            } catch (Exception ignored) {}
        }
        return result;
    }

    // =========================================================================
    //  Apply
    // =========================================================================

    /**
     * Applies all attributes from a {@link MultiToolType} to the given item meta.
     *
     * <p>Clears any previously-applied "multitool" modifiers first to prevent
     * stacking across type switches.
     *
     * @param meta       item meta to mutate
     * @param type       type whose attributes to apply
     * @param pluginName unused; kept for API symmetry
     */
    public static void applyAttributes(ItemMeta meta, MultiToolType type, String pluginName) {
        if (meta == null || type == null) return;
        clearOurModifiers(meta);

        Map<Attribute, Double> attrs = type.getAttributes();
        if (attrs == null || attrs.isEmpty()) return;

        for (Map.Entry<Attribute, Double> entry : attrs.entrySet()) {
            // Use the attribute's key().value() (e.g. "generic.attack_damage") for the modifier key
            String attrKeyValue = entry.getKey().key().value();
            NamespacedKey modKey = new NamespacedKey(NAMESPACE,
                    type.getKey() + "." + attrKeyValue);
            AttributeModifier modifier = new AttributeModifier(
                    modKey,
                    entry.getValue(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND);
            meta.addAttributeModifier(entry.getKey(), modifier);
        }
    }

    // =========================================================================
    //  Clear
    // =========================================================================

    /**
     * Removes all attribute modifiers whose NamespacedKey namespace equals
     * {@code "multitool"}, preventing modifier stacking on item rebuild.
     *
     * <p>Uses {@code Registry.ATTRIBUTE} to iterate, avoiding the deprecated
     * {@code Attribute.values()} call.
     *
     * @param meta item meta to mutate
     */
    public static void clearOurModifiers(ItemMeta meta) {
        if (meta == null) return;
        // Use Registry.ATTRIBUTE to iterate — avoids deprecated Attribute.values()
        for (Attribute attribute : Registry.ATTRIBUTE) {
            Collection<AttributeModifier> mods;
            try {
                mods = meta.getAttributeModifiers(attribute);
            } catch (Exception ignored) {
                continue;
            }
            if (mods == null || mods.isEmpty()) continue;
            for (AttributeModifier mod : new ArrayList<>(mods)) {
                if (NAMESPACE.equalsIgnoreCase(mod.getKey().getNamespace())) {
                    meta.removeAttributeModifier(attribute, mod);
                }
            }
        }
    }

    // =========================================================================
    //  Parsing helpers
    // =========================================================================

    /**
     * Resolves an {@link Attribute} from a YAML name string using the Registry.
     *
     * <p>Lookup order:
     * <ol>
     *   <li>Direct registry lookup via {@code minecraft:<lower_snake_name>}</li>
     *   <li>Shorthand alias map (e.g. {@code ATTACK_DAMAGE} → {@code generic.attack_damage})</li>
     * </ol>
     *
     * @param name   attribute name from YAML (e.g. {@code GENERIC_ATTACK_DAMAGE})
     * @param logger optional logger for unknown-attribute warnings (may be null)
     * @return matched Attribute, or {@code null} if unrecognised
     */
    public static Attribute parseAttribute(String name, Logger logger) {
        if (name == null || name.isBlank()) return null;

        // Convert UPPER_SNAKE to lower.dot (e.g. GENERIC_ATTACK_DAMAGE → generic.attack_damage)
        String dotForm = name.trim().toLowerCase().replace('_', '.');

        // Try direct registry lookup (minecraft key uses dots, e.g. "generic.attack_damage")
        NamespacedKey key = NamespacedKey.minecraft(dotForm);
        Attribute attr = Registry.ATTRIBUTE.get(key);
        if (attr != null) return attr;

        // Try underscore form just in case
        NamespacedKey keyUnderscore = NamespacedKey.minecraft(name.trim().toLowerCase());
        attr = Registry.ATTRIBUTE.get(keyUnderscore);
        if (attr != null) return attr;

        // Shorthand / alias resolution
        String resolved = resolveAlias(name.trim().toUpperCase());
        if (resolved != null) {
            attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(resolved));
            if (attr != null) return attr;
        }

        if (logger != null) {
            logger.warning("[MultiTool] Unknown attribute '" + name + "' — skipping.");
        }
        return null;
    }

    /**
     * Maps shorthand / legacy attribute names to their minecraft key dot-form.
     *
     * @param upper the UPPER_SNAKE input
     * @return the minecraft key value (dot-form), or null if no alias
     */
    private static String resolveAlias(String upper) {
        return switch (upper) {
            case "ATTACK_DAMAGE"              -> "generic.attack_damage";
            case "ATTACK_SPEED"               -> "generic.attack_speed";
            case "MOVEMENT_SPEED"             -> "generic.movement_speed";
            case "MAX_HEALTH"                 -> "generic.max_health";
            case "ARMOR"                      -> "generic.armor";
            case "ARMOR_TOUGHNESS"            -> "generic.armor_toughness";
            case "KNOCKBACK_RESISTANCE"       -> "generic.knockback_resistance";
            case "LUCK"                       -> "generic.luck";
            case "FOLLOW_RANGE"               -> "generic.follow_range";
            case "HORSE_JUMP_STRENGTH",
                 "JUMP_STRENGTH"              -> "generic.jump_strength";
            case "ATTACK_KNOCKBACK"           -> "generic.attack_knockback";
            case "FLYING_SPEED"               -> "generic.flying_speed";
            case "SCALE"                      -> "generic.scale";
            case "STEP_HEIGHT"                -> "generic.step_height";
            case "GRAVITY"                    -> "generic.gravity";
            case "SAFE_FALL_DISTANCE"         -> "generic.safe_fall_distance";
            case "FALL_DAMAGE_MULTIPLIER"     -> "generic.fall_damage_multiplier";
            case "BLOCK_BREAK_SPEED"          -> "generic.block_break_speed";
            case "MINING_EFFICIENCY"          -> "generic.mining_efficiency";
            case "MAX_ABSORPTION"             -> "generic.max_absorption";
            default                           -> null;
        };
    }
}
