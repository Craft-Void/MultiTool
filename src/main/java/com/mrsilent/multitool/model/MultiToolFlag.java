package com.mrsilent.multitool.model;

/**
 * Flags that can be set on a {@link MultiToolDefinition} or {@link MultiToolType}.
 *
 * <p>Flags modify item and interaction behaviour. Definition-level flags apply to
 * all types; type-level flags override or extend them for a specific mode.
 */
public enum MultiToolFlag {

    // ── Permission / Access ───────────────────────────────────────────────────

    /** Player must have {@code multitool.use.<id>.<typeKey>} to switch to this mode. */
    REQUIRE_PERMISSION,

    // ── Switch Restrictions ───────────────────────────────────────────────────

    /** This mode cannot be switched to (greyed out in GUI). */
    NO_SWITCH,

    /** The multitool GUI can only be opened while sneaking. */
    ONLY_SNEAK_SWITCH,

    // ── Stack Behaviour ───────────────────────────────────────────────────────

    /** Always force the item amount to 1. */
    UNSTACKABLE,

    /** Preserve the existing stack amount when rebuilding the item. */
    PRESERVE_AMOUNT,

    // ── GUI / Plugin Interaction ──────────────────────────────────────────────

    /** Do not cancel the interact event when opening our GUI.
     *  Allows other plugin GUIs to also trigger on sneak+right-click. */
    NO_PLUGIN_GUI_BLOCK,

    /** Do not prevent opening double-chest inventories when holding this tool. */
    NO_DOUBLE_CHEST_PROTECT,

    // ── Display ───────────────────────────────────────────────────────────────

    /** Apply {@link org.bukkit.inventory.ItemFlag#HIDE_ATTRIBUTES} to the item. */
    HIDE_ATTRIBUTES,

    /** Do not apply any enchantments (vanilla or EE) to this item. */
    NO_ENCHANTS,

    /** When rebuilding the item, carry the existing CustomModelData value over. */
    KEEP_CUSTOM_MODEL_DATA,

    // ── World Interaction ─────────────────────────────────────────────────────

    /** Prevent block breaking while holding this tool. */
    NO_BREAK,

    /** Prevent block placing while holding this tool. */
    NO_PLACE;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Case-insensitive lookup; also accepts underscores instead of hyphens.
     *
     * @param s the string from YAML
     * @return matching flag, or {@code null} if none matched
     */
    public static MultiToolFlag fromString(String s) {
        if (s == null || s.isBlank()) return null;
        String normalized = s.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return MultiToolFlag.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
