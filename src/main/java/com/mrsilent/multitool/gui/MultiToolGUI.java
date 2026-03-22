package com.mrsilent.multitool.gui;

import com.mrsilent.multitool.MultiToolPlugin;
import com.mrsilent.multitool.config.ConfigManager;
import com.mrsilent.multitool.model.MultiToolDefinition;
import com.mrsilent.multitool.model.MultiToolFlag;
import com.mrsilent.multitool.model.MultiToolType;
import com.mrsilent.multitool.util.ColorUtil;
import com.mrsilent.multitool.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI that displays the available modes of a multitool.
 *
 * <p>Implements {@link InventoryHolder} so the
 * {@link com.mrsilent.multitool.listener.InventoryClickListener} can
 * safely identify our inventory and handle clicks without interfering with
 * any other plugin's inventories.
 */
public class MultiToolGUI implements InventoryHolder {

    // ── Static cooldown tracker (player UUID → last open time ms) ──
    private static final Map<java.util.UUID, Long> COOLDOWNS = new HashMap<>();

    private final MultiToolPlugin plugin;
    private final Player player;
    private final MultiToolDefinition definition;
    private final ItemStack sourceTool;

    private Inventory inventory;

    public MultiToolGUI(MultiToolPlugin plugin,
                        Player player,
                        MultiToolDefinition definition,
                        ItemStack sourceTool) {
        this.plugin     = plugin;
        this.player     = player;
        this.definition = definition;
        this.sourceTool = sourceTool;
    }

    // ─────────────────────────────────────────────────────────────────
    // Open
    // ─────────────────────────────────────────────────────────────────

    /**
     * Builds and opens the GUI for the player.
     *
     * @return true if opened successfully, false if on cooldown
     */
    public boolean open() {
        // ── Cooldown check ───────────────────────────────────────────
        long cooldownMs = plugin.getConfigManager().getGuiCooldownMs();
        long now = System.currentTimeMillis();
        Long last = COOLDOWNS.get(player.getUniqueId());
        if (last != null && (now - last) < cooldownMs) return false;
        COOLDOWNS.put(player.getUniqueId(), now);

        // ── Resolve GUI settings ─────────────────────────────────────
        ConfigManager cfg = plugin.getConfigManager();

        int rows = (definition.getGuiRows() != null)
                ? definition.getGuiRows()
                : cfg.getGuiRows();
        rows = Math.max(1, Math.min(6, rows));

        String rawTitle = (definition.getGuiTitle() != null)
                ? definition.getGuiTitle()
                : cfg.getGuiTitle();
        Component title = ColorUtil.process(player, rawTitle, definition.getId(), "gui");

        // ── Create inventory ─────────────────────────────────────────
        inventory = Bukkit.createInventory(this, rows * 9, title);

        // ── Populate base slot if requested ──────────────────────────
        boolean includeBase = (definition.getGuiIncludeBaseSlot() != null)
                ? definition.getGuiIncludeBaseSlot()
                : cfg.isGuiIncludeBaseSlot();

        if (includeBase) {
            ItemStack baseBtn = ItemUtil.buildBase(plugin, definition, player);
            inventory.setItem(0, baseBtn);
        }

        // ── Populate type buttons ────────────────────────────────────
        for (MultiToolType type : definition.getTypes().values()) {
            int slot = type.getSlot();
            if (slot < 0 || slot >= inventory.getSize()) {
                plugin.debug("Type " + type.getKey() + " slot " + slot + " is out of bounds — auto-assigning.");
                slot = findNextEmptySlot(inventory);
            }
            if (slot < 0) continue; // Inventory is full

            // Check permission
            if (type.hasFlag(MultiToolFlag.REQUIRE_PERMISSION)) {
                String perm = "multitool.use." + definition.getId() + "." + type.getKey();
                if (!player.hasPermission(perm)) {
                    inventory.setItem(slot, buildLockedButton(type));
                    continue;
                }
            }

            // Check NO_SWITCH flag
            if (type.hasFlag(MultiToolFlag.NO_SWITCH)) {
                inventory.setItem(slot, buildLockedButton(type));
                continue;
            }

            ItemStack btn = buildTypeButton(type);
            inventory.setItem(slot, btn);
        }

        // ── Play open sound ───────────────────────────────────────────
        String soundKey = (definition.getGuiOpenSound() != null)
                ? definition.getGuiOpenSound()
                : cfg.getGuiOpenSound();
        playSound(player, soundKey, (float) cfg.getGuiOpenVolume(), (float) cfg.getGuiOpenPitch());

        // ── Open ─────────────────────────────────────────────────────
        player.openInventory(inventory);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────
    // Button builders
    // ─────────────────────────────────────────────────────────────────

    /**
     * Builds the clickable button for a type — a compact representation
     * showing the type's material, name, and lore.
     */
    private ItemStack buildTypeButton(MultiToolType type) {
        return ItemUtil.buildTyped(plugin, definition, type, player, 1, -1);
    }

    /**
     * Builds a "locked" placeholder for types the player cannot access.
     */
    private ItemStack buildLockedButton(MultiToolType type) {
        org.bukkit.inventory.ItemStack barrier = new org.bukkit.inventory.ItemStack(
                org.bukkit.Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta meta = barrier.getItemMeta();
        meta.displayName(ColorUtil.of("&c&l" + (type.getDisplayName() != null
                ? ColorUtil.strip(type.getDisplayName())
                : type.getKey())));
        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.of("&7&oYou don't have access to this mode."));
        meta.lore(lore);
        barrier.setItemMeta(meta);
        return barrier;
    }

    // ─────────────────────────────────────────────────────────────────
    // Switch (called from InventoryClickListener)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Switches the player's held item to the given type.
     * Called when a player clicks a type button in the GUI.
     *
     * @param typeKey the clicked type key
     */
    public void switchTo(String typeKey) {
        MultiToolType type = definition.getType(typeKey);
        if (type == null) return;

        // Preserve amount
        int amount = sourceTool.getAmount();
        // Preserve CMD
        int cmd = -1;
        if (sourceTool.hasItemMeta() && sourceTool.getItemMeta().hasCustomModelData()) {
            cmd = sourceTool.getItemMeta().getCustomModelData();
        }

        ItemStack newItem = ItemUtil.buildTyped(plugin, definition, type, player, amount, cmd);
        player.getInventory().setItemInMainHand(newItem);

        // ── Play switch sound ─────────────────────────────────────────
        String soundKey = resolveFirstNonNull(
                type.getSwitchSound(),
                definition.getSwitchSound(),
                plugin.getConfigManager().getSwitchSound());
        float vol = (float) plugin.getConfigManager().getSwitchSoundVolume();
        float pitch = (float) plugin.getConfigManager().getSwitchSoundPitch();
        playSound(player, soundKey, vol, pitch);

        // ── Show title ────────────────────────────────────────────────
        showSwitchTitle(type);

        // Close GUI
        player.closeInventory();
    }

    /**
     * Switches back to the base item when the base slot button is clicked.
     */
    public void switchToBase() {
        ItemStack base = ItemUtil.buildBase(plugin, definition, player);
        player.getInventory().setItemInMainHand(base);

        String soundKey = resolveFirstNonNull(
                definition.getSwitchSound(),
                plugin.getConfigManager().getSwitchSound());
        playSound(player, soundKey,
                (float) plugin.getConfigManager().getSwitchSoundVolume(),
                (float) plugin.getConfigManager().getSwitchSoundPitch());

        player.closeInventory();
    }

    // ─────────────────────────────────────────────────────────────────
    // Title
    // ─────────────────────────────────────────────────────────────────

    private void showSwitchTitle(MultiToolType type) {
        ConfigManager cfg = plugin.getConfigManager();

        String rawTitle = resolveFirstNonNull(
                type.getSwitchTitle(),
                cfg.getSwitchTitle());
        String rawSub = resolveFirstNonNull(
                type.getSwitchSubtitle(),
                cfg.getSwitchSubtitle());

        Component title    = ColorUtil.process(player, rawTitle,  definition.getId(), type.getKey());
        Component subtitle = ColorUtil.process(player, rawSub,    definition.getId(), type.getKey());

        net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(cfg.getSwitchFadeIn()  * 50L),
                java.time.Duration.ofMillis(cfg.getSwitchStay()    * 50L),
                java.time.Duration.ofMillis(cfg.getSwitchFadeOut() * 50L));

        player.showTitle(net.kyori.adventure.title.Title.title(title, subtitle, times));
    }

    // ─────────────────────────────────────────────────────────────────
    // Sound helper
    // ─────────────────────────────────────────────────────────────────

    /**
     * Plays a sound at the player's location using the Registry (not deprecated enum).
     *
     * @param player   target player
     * @param soundKey minecraft sound key (dots or underscores)
     * @param volume   volume
     * @param pitch    pitch
     */
    public static void playSound(Player player, String soundKey, float volume, float pitch) {
        if (soundKey == null || soundKey.isBlank()) return;
        try {
            // Normalise: "entity.experience_orb.pickup" or "ENTITY_EXPERIENCE_ORB_PICKUP"
            String normalized = soundKey.trim().toLowerCase().replace("_", ".");
            NamespacedKey key = NamespacedKey.minecraft(normalized);
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } else {
                // Try underscore format
                key = NamespacedKey.minecraft(soundKey.trim().toLowerCase());
                sound = Registry.SOUNDS.get(key);
                if (sound != null) player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────

    private static int findNextEmptySlot(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) return i;
        }
        return -1;
    }

    @SafeVarargs
    private static <T> T resolveFirstNonNull(T... values) {
        for (T v : values) if (v != null && !v.toString().isBlank()) return v;
        return null;
    }

    // ─────────────────────────────────────────────────────────────────
    // InventoryHolder
    // ─────────────────────────────────────────────────────────────────

    @Override
    public @NotNull Inventory getInventory() { return inventory; }

    // ─────────────────────────────────────────────────────────────────
    // Getters (used by InventoryClickListener)
    // ─────────────────────────────────────────────────────────────────

    public MultiToolDefinition getDefinition() { return definition; }
    public Player getPlayer()                   { return player; }
}
