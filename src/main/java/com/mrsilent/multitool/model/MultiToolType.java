package com.mrsilent.multitool.model;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single mode/type of a {@link MultiToolDefinition}.
 *
 * <p>For example, an "excellent_multitool" definition might have types:
 * {@code pickaxe}, {@code sword}, {@code axe}, etc.
 *
 * <p>Type values override or extend the parent definition values when building items.
 */
public class MultiToolType {

    // ── Identity ──────────────────────────────────────────────────────────────

    /** Unique key within the definition, e.g. "pickaxe". */
    private final String key;

    // ── Item Appearance ───────────────────────────────────────────────────────

    private Material material;
    private String   displayName;
    private List<String> lore = new ArrayList<>();

    // ── Enchantments ──────────────────────────────────────────────────────────

    private List<String> enchants          = new ArrayList<>();
    private List<String> excellentEnchants = new ArrayList<>();

    // ── GUI ───────────────────────────────────────────────────────────────────

    /** Slot index in the GUI inventory (0-based). */
    private int slot = 0;

    // ── Behaviour ─────────────────────────────────────────────────────────────

    private Set<MultiToolFlag>      flags      = EnumSet.noneOf(MultiToolFlag.class);
    private Map<Attribute, Double>  attributes = new HashMap<>();
    private Map<String, String>     nbt        = new HashMap<>();

    // ── Switch ────────────────────────────────────────────────────────────────

    private String switchSound;
    private String switchTitle;
    private String switchSubtitle;

    // ─────────────────────────────────────────────────────────────────────────

    public MultiToolType(String key) {
        this.key = key;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String  getKey()         { return key; }

    public Material getMaterial()   { return material; }
    public void setMaterial(Material m) { this.material = m; }

    public String getDisplayName()  { return displayName; }
    public void setDisplayName(String n) { this.displayName = n; }

    public List<String> getLore()   { return lore; }
    public void setLore(List<String> l) { this.lore = l != null ? l : new ArrayList<>(); }

    public List<String> getEnchants() { return enchants; }
    public void setEnchants(List<String> e) { this.enchants = e != null ? e : new ArrayList<>(); }

    public List<String> getExcellentEnchants() { return excellentEnchants; }
    public void setExcellentEnchants(List<String> e) { this.excellentEnchants = e != null ? e : new ArrayList<>(); }

    public int  getSlot()           { return slot; }
    public void setSlot(int s)      { this.slot = s; }

    public Set<MultiToolFlag> getFlags() { return flags; }
    public void setFlags(Set<MultiToolFlag> f) { this.flags = f != null ? f : EnumSet.noneOf(MultiToolFlag.class); }

    /** Convenience: check if this type has a specific flag. */
    public boolean hasFlag(MultiToolFlag flag) { return flags.contains(flag); }

    public Map<Attribute, Double> getAttributes() { return attributes; }
    public void setAttributes(Map<Attribute, Double> a) { this.attributes = a != null ? a : new HashMap<>(); }

    public Map<String, String> getNbt() { return nbt; }
    public void setNbt(Map<String, String> n) { this.nbt = n != null ? n : new HashMap<>(); }

    public String getSwitchSound()    { return switchSound; }
    public void setSwitchSound(String s)   { this.switchSound = s; }

    public String getSwitchTitle()    { return switchTitle; }
    public void setSwitchTitle(String t)   { this.switchTitle = t; }

    public String getSwitchSubtitle() { return switchSubtitle; }
    public void setSwitchSubtitle(String s) { this.switchSubtitle = s; }
}
