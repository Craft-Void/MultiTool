package com.mrsilent.multitool.listener;

import com.mrsilent.multitool.MultiToolPlugin;
import com.mrsilent.multitool.gui.MultiToolGUI;
import com.mrsilent.multitool.model.MultiToolDefinition;
import com.mrsilent.multitool.model.MultiToolFlag;
import com.mrsilent.multitool.util.NBTUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Handles all player interactions relevant to multitools.
 *
 * <p><b>Critical rule — MUST NOT break other plugins:</b>
 * This listener only cancels/overrides events when ALL of the following
 * conditions are met:
 * <ol>
 *   <li>The player is sneaking.</li>
 *   <li>The player is holding a recognised multitool.</li>
 *   <li>The action is a right-click (main or air).</li>
 * </ol>
 * In all other cases, events are passed through unchanged.
 */
public class PlayerInteractListener implements Listener {

    private final MultiToolPlugin plugin;

    public PlayerInteractListener(MultiToolPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────
    // GUI open — SNEAK + RIGHT CLICK
    // ─────────────────────────────────────────────────────────────────

    /**
     * Opens the multitool GUI on sneak + right-click.
     *
     * <p>Priority is LOW so other plugins can handle the event first.
     * We only intervene when this is definitively our event.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        // Only care about the main hand to avoid duplicate triggers
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        // ── Must be sneaking ─────────────────────────────────────────
        if (!player.isSneaking()) return;

        // ── Must be a right-click ────────────────────────────────────
        org.bukkit.event.block.Action action = event.getAction();
        boolean isRightClick =
                action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;
        if (!isRightClick) return;

        // ── Must be holding a multitool ───────────────────────────────
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!NBTUtil.isMultiTool(hand)) return;

        String id = NBTUtil.getToolId(hand);
        if (id == null) return;

        MultiToolDefinition def = plugin.getItemManager().getDefinition(id);
        if (def == null) return;

        // ── Merge flags ───────────────────────────────────────────────
        String currentTypeKey = NBTUtil.getToolType(hand);
        Set<MultiToolFlag> flags = NBTUtil.getFlags(hand);

        // If NO_PLUGIN_GUI_BLOCK is set, do not cancel other GUI interactions
        // (we still open our own GUI, but we don't consume the event)
        boolean noPluginGuiBlock = def.hasFlag(MultiToolFlag.NO_PLUGIN_GUI_BLOCK)
                || flags.contains(MultiToolFlag.NO_PLUGIN_GUI_BLOCK);

        // ── Permission check ──────────────────────────────────────────
        if (!player.hasPermission("multitool.use") && !player.hasPermission("multitool.use.*")) {
            // Check tool-specific permission
            if (!player.hasPermission("multitool.use." + id)) {
                player.sendMessage(plugin.getConfigManager().getSwitchTitle());
                return;
            }
        }

        // ── Open GUI — cancel the event ONLY if we're opening our GUI ─
        // (Don't cancel if NO_PLUGIN_GUI_BLOCK so other plugins can also handle)
        if (!noPluginGuiBlock) {
            event.setCancelled(true);
        }

        // Open GUI on next tick to avoid inventory conflicts
        org.bukkit.scheduler.BukkitRunnable openTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                MultiToolGUI gui = new MultiToolGUI(plugin, player, def, hand);
                gui.open();
            }
        };
        openTask.runTaskLater(plugin, 1L);
    }

    // ─────────────────────────────────────────────────────────────────
    // Block breaking — NO_BREAK flag
    // ─────────────────────────────────────────────────────────────────

    /**
     * Prevents block breaking if the held multitool has the {@code NO_BREAK} flag.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (!NBTUtil.isMultiTool(hand)) return;

        Set<MultiToolFlag> flags = NBTUtil.getFlags(hand);
        if (flags.contains(MultiToolFlag.NO_BREAK)) {
            event.setCancelled(true);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Block placing — NO_PLACE flag
    // ─────────────────────────────────────────────────────────────────

    /**
     * Prevents block placing if the held multitool has the {@code NO_PLACE} flag.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (!NBTUtil.isMultiTool(hand)) return;

        Set<MultiToolFlag> flags = NBTUtil.getFlags(hand);
        if (flags.contains(MultiToolFlag.NO_PLACE)) {
            event.setCancelled(true);
        }
    }
}
