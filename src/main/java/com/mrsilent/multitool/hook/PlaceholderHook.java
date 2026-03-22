package com.mrsilent.multitool.hook;

import com.mrsilent.multitool.MultiToolPlugin;
import com.mrsilent.multitool.util.NBTUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for MultiTool.
 *
 * <p>Registered identifier: {@code %multitool_<placeholder>%}
 *
 * <table>
 *   <caption>Available placeholders</caption>
 *   <tr><td>%multitool_id%</td>    <td>ID of the multitool in main hand</td></tr>
 *   <tr><td>%multitool_type%</td>  <td>Current mode/type of the held tool</td></tr>
 *   <tr><td>%multitool_owner%</td> <td>Name of the player who received the tool</td></tr>
 *   <tr><td>%multitool_has%</td>   <td>"true" / "false" — player holds a multitool</td></tr>
 * </table>
 */
public class PlaceholderHook extends PlaceholderExpansion {

    private final MultiToolPlugin plugin;

    public PlaceholderHook(MultiToolPlugin plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    //  PlaceholderExpansion contract
    // =========================================================================

    @Override
    public @NotNull String getIdentifier() { return "multitool"; }

    @Override
    public @NotNull String getAuthor() { return "MrSilent"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    /** Keep registered across plugin reloads. */
    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (!(offlinePlayer instanceof Player player)) return "";

        ItemStack held  = player.getInventory().getItemInMainHand();
        boolean isTool  = NBTUtil.isMultiTool(held);

        return switch (params.toLowerCase()) {
            case "id"    -> isTool ? orEmpty(NBTUtil.getToolId(held))   : "";
            case "type"  -> isTool ? orEmpty(NBTUtil.getToolType(held)) : "";
            case "owner" -> isTool ? orEmpty(NBTUtil.getOwner(held))    : "";
            case "has"   -> String.valueOf(isTool);
            default      -> null; // unknown placeholder → signal PAPI to skip
        };
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static String orEmpty(String s) { return s != null ? s : ""; }
}
