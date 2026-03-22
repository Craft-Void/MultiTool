package com.mrsilent.multitool.util;

import com.mrsilent.multitool.MultiToolPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color and placeholder processing engine for MultiTool.
 *
 * <p>Supported input syntax:
 * <ul>
 *   <li>{@code &a}, {@code &b} … {@code &r}  — legacy Minecraft color codes</li>
 *   <li>{@code &#RRGGBB} or {@code #RRGGBB}  — 24-bit hex color</li>
 *   <li>{@code {player}}, {@code {player_name}}, {@code {id}}, {@code {type}}
 *       — internal plugin placeholders</li>
 *   <li>{@code %papi_placeholder%}            — PlaceholderAPI (when present)</li>
 * </ul>
 *
 * <p>Primary entry points used across the codebase:
 * <ul>
 *   <li>{@link #of(String)}  — simple color parse, returns Component</li>
 *   <li>{@link #process(Player, String, String, String)} — full parse with placeholders</li>
 *   <li>{@link #parse(String)} — alias for {@link #of(String)}</li>
 * </ul>
 */
public final class ColorUtil {

    private ColorUtil() {}

    /** Matches {@code &#RRGGBB} and {@code #RRGGBB}. */
    private static final Pattern HEX_PATTERN =
            Pattern.compile("(?:&#|#)([A-Fa-f0-9]{6})");

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    // =========================================================================
    //  Primary API — the two methods called most often
    // =========================================================================

    /**
     * Convert a raw legacy/hex color string to an Adventure {@link Component}.
     * This is the simple, no-placeholder version.
     *
     * @param raw string with {@code &} codes and/or {@code &#RRGGBB} hex
     * @return the Component (never null; returns {@link Component#empty()} for null input)
     */
    public static Component of(String raw) {
        if (raw == null) return Component.empty();
        return LEGACY.deserialize(convertHex(raw));
    }

    /**
     * Full processing pipeline: apply internal placeholders, PAPI placeholders,
     * hex conversion, then deserialize to Component.
     *
     * <p>Internal placeholder substitutions applied:
     * <ul>
     *   <li>{@code {player}} / {@code {player_name}} → player name (if player != null)</li>
     *   <li>{@code {id}}   → toolId</li>
     *   <li>{@code {type}} → typeKey</li>
     * </ul>
     *
     * @param player  player for PAPI / {player} substitution (may be null)
     * @param raw     raw string from YAML
     * @param toolId  multitool definition id
     * @param typeKey current mode/type key
     * @return processed Component
     */
    public static Component process(Player player, String raw, String toolId, String typeKey) {
        if (raw == null) return Component.empty();

        String text = raw;

        // 1. Internal {key} substitutions
        Map<String, String> placeholders = new HashMap<>();
        if (player  != null)  { placeholders.put("player", player.getName());
                                 placeholders.put("player_name", player.getName()); }
        if (toolId  != null)    placeholders.put("id",   toolId);
        if (typeKey != null)    placeholders.put("type", typeKey);

        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        }

        // 2. PAPI placeholders
        text = applyPAPI(text, player);

        // 3. Hex conversion + legacy deserialize
        return LEGACY.deserialize(convertHex(text));
    }

    // =========================================================================
    //  Aliases / overloads (for internal use and callers that use parse())
    // =========================================================================

    /** Alias for {@link #of(String)}. */
    public static Component parse(String raw) {
        return of(raw);
    }

    /**
     * Parse with an explicit placeholder map instead of toolId/typeKey strings.
     *
     * @param raw          raw string
     * @param player       player context (may be null)
     * @param placeholders map of {key} → value substitutions (may be null)
     * @return Component
     */
    public static Component parse(String raw, Player player, Map<String, String> placeholders) {
        if (raw == null) return Component.empty();
        String text = raw;

        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                text = text.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        text = applyPAPI(text, player);
        return LEGACY.deserialize(convertHex(text));
    }

    /** Parse a list of strings to a list of Components using {@link #process}. */
    public static List<Component> processList(Player player, List<String> lines,
                                               String toolId, String typeKey) {
        List<Component> result = new ArrayList<>();
        if (lines == null) return result;
        for (String line : lines) {
            result.add(process(player, line, toolId, typeKey));
        }
        return result;
    }

    /** Parse a list of strings to Components (no placeholders). */
    public static List<Component> parseList(List<String> lines) {
        List<Component> result = new ArrayList<>();
        if (lines == null) return result;
        for (String line : lines) result.add(of(line));
        return result;
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    /**
     * Strip all color / hex codes from a string, leaving plain text.
     *
     * @param raw the input string
     * @return the plain string
     */
    public static String strip(String raw) {
        if (raw == null) return "";
        String s = HEX_PATTERN.matcher(raw).replaceAll("");
        return s.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
    }

    /**
     * Convert a raw string to legacy § format (for non-Adventure APIs).
     *
     * @param raw the input string
     * @return string with § codes
     */
    public static String toLegacy(String raw) {
        if (raw == null) return "";
        return convertHex(raw).replace("&", "§").replace("§§", "&");
    }

    // =========================================================================
    //  Internal helpers
    // =========================================================================

    /**
     * Converts {@code &#RRGGBB} / {@code #RRGGBB} to legacy {@code §x§R§R§G§G§B§B} format.
     */
    static String convertHex(String text) {
        Matcher m = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder rep = new StringBuilder("§x");
            for (char c : hex.toCharArray()) rep.append('§').append(c);
            m.appendReplacement(sb, Matcher.quoteReplacement(rep.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Applies PlaceholderAPI placeholders.
     * Safe no-op if PAPI is absent, disabled, or throws an exception.
     */
    private static String applyPAPI(String text, Player player) {
        if (player == null) return text;
        MultiToolPlugin plugin = MultiToolPlugin.getInstance();
        if (plugin == null || !plugin.getConfigManager().isPAPIEnabled()) return text;
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return text;
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            if (!plugin.getConfigManager().isPAPISafeMode()) {
                plugin.getLogger().warning("[ColorUtil] PAPI error: " + e.getMessage());
            }
            return text;
        }
    }
}
