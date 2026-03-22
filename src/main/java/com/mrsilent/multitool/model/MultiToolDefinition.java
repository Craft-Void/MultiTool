package com.mrsilent.multitool.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parsed representation of a multitool YAML file.
 *
 * <p>Holds all configuration for one multitool: its base material/name/lore,
 * its available {@link MultiToolType} modes, GUI overrides, flags, enchants, etc.
 *
 * <p>This is a pure data object — no Bukkit logic lives here.
 */
public class MultiToolDefinition {

    // ── Identity ──────────────────────────────────────────────────────────────

    /** Unique lower-case id, e.g. "excellent_multitool". */
    private String id;

    // ── Base Item ─────────────────────────────────────────────────────────────

    private Material     material    = Material.STONE;
    private String       displayName = "&fMultiTool";
    private List<String> lore        = new ArrayList<>();
    private boolean      unbreakable = false;

    // ── Enchants ──────────────────────────────────────────────────────────────

    private List<String> enchants          = new ArrayList<>();
    private List<String> excellentEnchants = new ArrayList<>();

    // ── Flags ─────────────────────────────────────────────────────────────────

    private Set<MultiToolFlag> flags = EnumSet.noneOf(MultiToolFlag.class);

    // ── Types / Modes ─────────────────────────────────────────────────────────

    /** Ordered map of typeKey → type (insertion order = YAML order). */
    private Map<String, MultiToolType> types = new LinkedHashMap<>();

    // ── GUI Overrides (null = use config.yml defaults) ────────────────────────

    private Integer guiRows;
    private String  guiTitle;
    private Boolean guiIncludeBaseSlot;
    private String  guiOpenSound;

    // ── Switch Sound ──────────────────────────────────────────────────────────

    private String switchSound;

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material m) { this.material = m; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String n) { this.displayName = n; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> l) { this.lore = l != null ? l : new ArrayList<>(); }

    public boolean isUnbreakable() { return unbreakable; }
    public void setUnbreakable(boolean u) { this.unbreakable = u; }

    public List<String> getEnchants() { return enchants; }
    public void setEnchants(List<String> e) { this.enchants = e != null ? e : new ArrayList<>(); }

    public List<String> getExcellentEnchants() { return excellentEnchants; }
    public void setExcellentEnchants(List<String> e) { this.excellentEnchants = e != null ? e : new ArrayList<>(); }

    public Set<MultiToolFlag> getFlags() { return flags; }
    public void setFlags(Set<MultiToolFlag> f) { this.flags = f != null ? f : EnumSet.noneOf(MultiToolFlag.class); }

    /** Convenience: check if this definition has a specific flag. */
    public boolean hasFlag(MultiToolFlag flag) { return flags.contains(flag); }

    public Map<String, MultiToolType> getTypes() { return types; }
    public void setTypes(Map<String, MultiToolType> t) { this.types = t != null ? t : new LinkedHashMap<>(); }

    /**
     * Look up a type by key (case-insensitive).
     *
     * @param key the type key
     * @return type, or null if not found
     */
    public MultiToolType getType(String key) {
        if (key == null) return null;
        MultiToolType direct = types.get(key);
        if (direct != null) return direct;
        // Case-insensitive fallback
        for (Map.Entry<String, MultiToolType> entry : types.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) return entry.getValue();
        }
        return null;
    }

    // ── GUI Overrides ─────────────────────────────────────────────────────────

    public Integer getGuiRows() { return guiRows; }
    public void setGuiRows(Integer r) { this.guiRows = r; }

    public String getGuiTitle() { return guiTitle; }
    public void setGuiTitle(String t) { this.guiTitle = t; }

    public Boolean getGuiIncludeBaseSlot() { return guiIncludeBaseSlot; }
    public void setGuiIncludeBaseSlot(Boolean b) { this.guiIncludeBaseSlot = b; }

    public String getGuiOpenSound() { return guiOpenSound; }
    public void setGuiOpenSound(String s) { this.guiOpenSound = s; }

    // ── Switch ────────────────────────────────────────────────────────────────

    public String getSwitchSound() { return switchSound; }
    public void setSwitchSound(String s) { this.switchSound = s; }
}
