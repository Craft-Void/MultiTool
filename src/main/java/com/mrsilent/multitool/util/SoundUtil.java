package com.mrsilent.multitool.util;

import com.mrsilent.multitool.MultiToolPlugin;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * Sound utility for MultiTool.
 *
 * Supports two input formats in config / YAML:
 *   1. Bukkit enum name  — e.g.  BLOCK_NOTE_BLOCK_PLING
 *   2. Minecraft key     — e.g.  block.note_block.pling  or  minecraft:block.note_block.pling
 *
 * Resolution order:
 *   a) Try Bukkit enum name via Sound.valueOf() — backward-compatible
 *   b) Try minecraft: namespace key via Registry.SOUNDS (Paper 1.21.x preferred API)
 *   c) Return null if no match found
 */
public final class SoundUtil {

    private SoundUtil() { /* utility */ }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Parse
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Parse a sound from a string identifier.
     *
     * @param name the sound name (Bukkit enum OR minecraft key)
     * @return the Sound, or null if not found
     */
    @Nullable
    public static Sound parseSound(String name) {
        if (name == null || name.isBlank()) return null;
        String trimmed = name.trim();

        // ── Try Bukkit enum name (e.g. BLOCK_NOTE_BLOCK_PLING) ───────────────
        try {
            return Sound.valueOf(trimmed.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException ignored) {}

        // ── Try Registry with the string as-is (e.g. block.note_block.pling) ─
        try {
            NamespacedKey nk;
            if (trimmed.contains(":")) {
                nk = NamespacedKey.fromString(trimmed.toLowerCase());
            } else {
                nk = NamespacedKey.minecraft(trimmed.toLowerCase());
            }
            if (nk != null) {
                Sound s = Registry.SOUNDS.get(nk);
                if (s != null) return s;
            }
        } catch (Exception ignored) {}

        // ── Try converting UPPER_SNAKE_CASE to dot.notation ──────────────────
        // e.g. BLOCK_AMETHYST_BLOCK_CHIME is NOT a simple underscore→dot mapping,
        // but we can try the all-lowercase version as a fallback heuristic:
        try {
            String dotted = trimmed.toLowerCase().replace('_', '.');
            NamespacedKey nk = NamespacedKey.minecraft(dotted);
            Sound s = Registry.SOUNDS.get(nk);
            if (s != null) return s;
        } catch (Exception ignored) {}

        MultiToolPlugin.getInstance().debug("[SoundUtil] Could not resolve sound: " + name);
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Play Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Play a sound for a player at their location.
     *
     * @param player   the player
     * @param soundStr the sound name string
     * @param volume   volume (default 1.0)
     * @param pitch    pitch  (default 1.0)
     */
    public static void play(Player player, String soundStr, float volume, float pitch) {
        if (player == null || soundStr == null || soundStr.isBlank()) return;
        Sound sound = parseSound(soundStr);
        if (sound == null) return;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Play at default volume/pitch.
     */
    public static void play(Player player, String soundStr) {
        play(player, soundStr, 1.0f, 1.0f);
    }

    /**
     * Play a sound at a specific location for all nearby players.
     */
    public static void playAt(Location loc, String soundStr, float volume, float pitch) {
        if (loc == null || soundStr == null || soundStr.isBlank()) return;
        Sound sound = parseSound(soundStr);
        if (sound == null) return;
        if (loc.getWorld() != null) {
            loc.getWorld().playSound(loc, sound, volume, pitch);
        }
    }
}
