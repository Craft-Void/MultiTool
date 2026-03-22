package com.mrsilent.multitool.listener;

import com.mrsilent.multitool.MultiToolPlugin;
import com.mrsilent.multitool.gui.MultiToolGUI;
import com.mrsilent.multitool.model.MultiToolDefinition;
import com.mrsilent.multitool.model.MultiToolFlag;
import com.mrsilent.multitool.model.MultiToolType;
import com.mrsilent.multitool.util.NBTUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Handles clicks inside a {@link MultiToolGUI} inventory.
 *
 * <p><b>Safety first:</b> This listener only acts on inventories that are
 * held by our {@link MultiToolGUI} class. Any other inventory is entirely
 * ignored and passed through — this ensures zero interference with
 * ExcellentShop, ExcellentCrates, Auction GUI, chest GUIs, etc.
 */
public class InventoryClickListener implements Listener {

    private final MultiToolPlugin plugin;

    public InventoryClickListener(MultiToolPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────
    // Click handler
    // ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        // ── Only care about our GUI ───────────────────────────────────
        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof MultiToolGUI gui)) return;

        // ── Always cancel clicks inside our GUI ───────────────────────
        // This prevents item manipulation within the GUI itself.
        event.setCancelled(true);

        // ── Only respond to left/right clicks ─────────────────────────
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();

        // Clicks in the bottom inventory (player's own inventory) → ignore
        if (slot < 0 || slot >= topInv.getSize()) return;

        ItemStack clicked = topInv.getItem(slot);
        if (clicked == null || clicked.getType().isAir()) return;

        // ── Determine what was clicked ────────────────────────────────
        MultiToolDefinition def = gui.getDefinition();

        // Is this a multitool type button?
        String typeKey = NBTUtil.getToolType(clicked);

        if (typeKey == null || typeKey.equals("base")) {
            // Clicked the base button (slot 0 / include-base-slot)
            gui.switchToBase();
            return;
        }

        MultiToolType type = def.getType(typeKey);
        if (type == null) return;

        // ── Check flags ───────────────────────────────────────────────
        if (type.hasFlag(MultiToolFlag.NO_SWITCH)) return;

        if (type.hasFlag(MultiToolFlag.REQUIRE_PERMISSION)) {
            String perm = "multitool.use." + def.getId() + "." + typeKey;
            if (!player.hasPermission(perm)) {
                player.sendMessage(com.mrsilent.multitool.util.ColorUtil
                        .of("&cYou don't have permission to use this mode!"));
                return;
            }
        }

        // ── Execute switch ────────────────────────────────────────────
        gui.switchTo(typeKey);
    }

    // ─────────────────────────────────────────────────────────────────
    // Drag handler — prevent dragging items inside our GUI
    // ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (!(topInv.getHolder() instanceof MultiToolGUI)) return;

        // Cancel any drag that touches our GUI slots
        int topSize = topInv.getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
