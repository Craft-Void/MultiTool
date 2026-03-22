package com.mrsilent.multitool.command;

import com.mrsilent.multitool.MultiToolPlugin;
import com.mrsilent.multitool.model.MultiToolDefinition;
import com.mrsilent.multitool.model.MultiToolType;
import com.mrsilent.multitool.util.ColorUtil;
import com.mrsilent.multitool.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Executor for all {@code /multitool} (alias: /mt /mtool) sub-commands.
 *
 * <p>Sub-commands:
 * <ul>
 *   <li>{@code give <id> [player]}        — Give a multitool to a player</li>
 *   <li>{@code list}                       — List loaded tool IDs</li>
 *   <li>{@code reload}                     — Reload config + items</li>
 *   <li>{@code create <id>}                — Create a YAML template</li>
 *   <li>{@code export excellentshop <id>}  — Export to ExcellentShop format</li>
 *   <li>{@code export excellentcrates <id>}— Export to ExcellentCrates format</li>
 * </ul>
 */
public class MultiToolCommand implements CommandExecutor, TabCompleter {

    private final MultiToolPlugin plugin;

    public MultiToolCommand(MultiToolPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────
    // Execution
    // ─────────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give"   -> cmdGive(sender, args);
            case "list"   -> cmdList(sender);
            case "reload" -> cmdReload(sender);
            case "create" -> cmdCreate(sender, args);
            case "export" -> cmdExport(sender, args);
            default       -> sendHelp(sender);
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────
    // /multitool give <id> [player]
    // ─────────────────────────────────────────────────────────────────

    private void cmdGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("multitool.give")) {
            sender.sendMessage(ColorUtil.of("&cNo permission."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.of("&eUsage: /multitool give <id> [player]"));
            return;
        }
        String id = args[1].toLowerCase();
        MultiToolDefinition def = plugin.getItemManager().getDefinition(id);
        if (def == null) {
            sender.sendMessage(ColorUtil.of("&cUnknown multitool: &e" + id));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ColorUtil.of("&cPlayer not found: &e" + args[2]));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ColorUtil.of("&cSpecify a player when running from console."));
            return;
        }

        ItemStack item = ItemUtil.buildBase(plugin, def, target);
        target.getInventory().addItem(item);
        target.sendMessage(ColorUtil.of("&aYou received &e" + ColorUtil.strip(def.getDisplayName()) + "&a."));
        if (!target.equals(sender)) {
            sender.sendMessage(ColorUtil.of("&aGave &e" + ColorUtil.strip(def.getDisplayName())
                    + " &ato &f" + target.getName() + "&a."));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // /multitool list
    // ─────────────────────────────────────────────────────────────────

    private void cmdList(CommandSender sender) {
        if (!sender.hasPermission("multitool.list")) {
            sender.sendMessage(ColorUtil.of("&cNo permission."));
            return;
        }
        sender.sendMessage(ColorUtil.of("&e&lLoaded MultiTools &7(" + plugin.getItemManager().getDefinitions().size() + "):"));
        for (MultiToolDefinition def : plugin.getItemManager().getDefinitions()) {
            sender.sendMessage(ColorUtil.of(
                    "  &7- &f" + def.getId()
                    + " &8(&7" + def.getTypes().size() + " modes&8)"
                    + " &7[" + def.getMaterial().name() + "]"));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // /multitool reload
    // ─────────────────────────────────────────────────────────────────

    private void cmdReload(CommandSender sender) {
        if (!sender.hasPermission("multitool.reload")) {
            sender.sendMessage(ColorUtil.of("&cNo permission."));
            return;
        }
        plugin.reload();
        sender.sendMessage(ColorUtil.of("&aMultiTool reloaded successfully."));
    }

    // ─────────────────────────────────────────────────────────────────
    // /multitool create <id>
    // ─────────────────────────────────────────────────────────────────

    private void cmdCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("multitool.create")) {
            sender.sendMessage(ColorUtil.of("&cNo permission."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.of("&eUsage: /multitool create <id>"));
            return;
        }
        String id = args[1].toLowerCase().trim();
        File itemsDir = new File(plugin.getDataFolder(),
                plugin.getConfigManager().getItemsFolder());
        itemsDir.mkdirs();
        File target = new File(itemsDir, id + ".yml");
        if (target.exists()) {
            sender.sendMessage(ColorUtil.of("&cFile already exists: &e" + target.getName()));
            return;
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(target))) {
            pw.println(generateTemplate(id));
            sender.sendMessage(ColorUtil.of("&aTemplate created: &e" + target.getPath()));
        } catch (IOException e) {
            sender.sendMessage(ColorUtil.of("&cFailed to create file: &e" + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // /multitool export excellentshop|excellentcrates <id>
    // ─────────────────────────────────────────────────────────────────

    private void cmdExport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("multitool.export")) {
            sender.sendMessage(ColorUtil.of("&cNo permission."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.of("&eUsage: /multitool export <excellentshop|excellentcrates> <id>"));
            return;
        }
        String format = args[1].toLowerCase();
        String id     = args[2].toLowerCase();
        MultiToolDefinition def = plugin.getItemManager().getDefinition(id);
        if (def == null) {
            sender.sendMessage(ColorUtil.of("&cUnknown multitool: &e" + id));
            return;
        }

        File exportDir = new File(plugin.getDataFolder(), "exports");
        exportDir.mkdirs();

        switch (format) {
            case "excellentshop" -> exportExcellentShop(sender, def, exportDir);
            case "excellentcrates" -> exportExcellentCrates(sender, def, exportDir);
            default -> sender.sendMessage(ColorUtil.of("&cUnknown format: &e" + format));
        }
    }

    /** Exports as an ExcellentShop product entry. */
    private void exportExcellentShop(CommandSender sender,
                                     MultiToolDefinition def,
                                     File exportDir) {
        File out = new File(exportDir, "excellentshop_" + def.getId() + ".yml");
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("# ExcellentShop export for: " + def.getId());
            pw.println("products:");
            pw.println("  " + def.getId() + ":");
            pw.println("    item:");
            pw.println("      material: " + def.getMaterial().name());
            pw.println("      display-name: '" + def.getDisplayName() + "'");
            pw.println("    buy-price: 1000.0");
            pw.println("    sell-price: 500.0");
            sender.sendMessage(ColorUtil.of("&aExported to: &e" + out.getPath()));
        } catch (IOException e) {
            sender.sendMessage(ColorUtil.of("&cExport failed: &e" + e.getMessage()));
        }
    }

    /** Exports as an ExcellentCrates reward entry. */
    private void exportExcellentCrates(CommandSender sender,
                                       MultiToolDefinition def,
                                       File exportDir) {
        File out = new File(exportDir, "excellentcrates_" + def.getId() + ".yml");
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("# ExcellentCrates export for: " + def.getId());
            pw.println("rewards:");
            pw.println("  " + def.getId() + ":");
            pw.println("    weight: 1.0");
            pw.println("    commands:");
            pw.println("      - 'multitool give " + def.getId() + " {player}'");
            pw.println("    preview:");
            pw.println("      material: " + def.getMaterial().name());
            pw.println("      name: '" + def.getDisplayName() + "'");
            sender.sendMessage(ColorUtil.of("&aExported to: &e" + out.getPath()));
        } catch (IOException e) {
            sender.sendMessage(ColorUtil.of("&cExport failed: &e" + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Template generator
    // ─────────────────────────────────────────────────────────────────

    private static String generateTemplate(String id) {
        return """
# ═══════════════════════════════════════════════════════════════
# MultiTool Definition — %id%
# Generated by /multitool create
# ═══════════════════════════════════════════════════════════════
multitool:
  # Unique identifier — used in commands and NBT
  id: "%id%"

  # Base material (shown before any mode is selected)
  material: DIAMOND_SWORD

  # Display name — supports & codes and &#RRGGBB hex
  display-name: "&6&l%id_cap% MultiTool"

  # Item unbreakable (overrides global-unbreakable in config.yml)
  unbreakable: true

  # Base lore (appended to ALL types)
  lore:
    - "&8A custom multitool."
    - ""
    - "&eSNEAK + RIGHT CLICK &7to switch modes"
    - "&7Current mode: &f{type}"

  # Base vanilla enchants (format: enchant:level)
  enchants:
    - "unbreaking:3"
    - "mending:1"

  # Base ExcellentEnchants (format: enchant:level)
  excellent-enchants: []

  # Tool-level flags (see MultiToolFlag enum for all options)
  flags:
    - "UNSTACKABLE"
    - "ONLY_SNEAK_SWITCH"

  # GUI overrides (leave out to use config.yml defaults)
  gui:
    rows: 2
    title: "&8&l%id_cap% Modes"
    include-base-slot: false
    open-sound: "block.amethyst_block.chime"

  # Default switch sound (overridable per-type)
  switch-sound: "entity.experience_orb.pickup"

  # ─── Types/Modes ───────────────────────────────────────────
  types:

    pickaxe:
      name: "&bPickaxe Mode"
      material: NETHERITE_PICKAXE
      slot: 10
      lore:
        - "&7Break blocks fast."
      enchants:
        - "efficiency:5"
        - "fortune:3"
        - "unbreaking:3"
        - "mending:1"
      excellent-enchants: []
      attributes:
        GENERIC_ATTACK_DAMAGE: 5.0
        GENERIC_ATTACK_SPEED: 1.4
        GENERIC_MOVEMENT_SPEED: 0.05
      flags:
        - "UNSTACKABLE"
        - "HIDE_ATTRIBUTES"
      nbt:
        mode: "pickaxe"
      switch-title: "&bPickaxe Mode"
      switch-subtitle: "&7Mining Activated"
      sound: "block.amethyst_block.resonate"

    sword:
      name: "&cSword Mode"
      material: NETHERITE_SWORD
      slot: 12
      lore:
        - "&7Fight enemies."
      enchants:
        - "sharpness:5"
        - "looting:3"
        - "unbreaking:3"
        - "mending:1"
      excellent-enchants: []
      attributes:
        GENERIC_ATTACK_DAMAGE: 12.0
        GENERIC_ATTACK_SPEED: 1.8
      flags:
        - "UNSTACKABLE"
        - "HIDE_ATTRIBUTES"
      nbt:
        mode: "sword"
      switch-title: "&cSword Mode"
      switch-subtitle: "&7Combat Activated"
      sound: "entity.player.attack.sweep"

    axe:
      name: "&aAxe Mode"
      material: NETHERITE_AXE
      slot: 14
      lore:
        - "&7Fell trees."
      enchants:
        - "efficiency:5"
        - "fortune:3"
        - "unbreaking:3"
        - "mending:1"
      excellent-enchants: []
      attributes:
        GENERIC_ATTACK_DAMAGE: 10.0
        GENERIC_ATTACK_SPEED: 0.9
      flags:
        - "UNSTACKABLE"
        - "HIDE_ATTRIBUTES"
      nbt:
        mode: "axe"
      switch-title: "&aAxe Mode"
      switch-subtitle: "&7Lumberjack Activated"
      sound: "block.wood.break"
""".replace("%id%", id).replace("%id_cap%", capitalize(id));
    }

    // ─────────────────────────────────────────────────────────────────
    // Help
    // ─────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.of("&6&lMultiTool &7Commands:"));
        sender.sendMessage(ColorUtil.of("  &e/mt give <id> [player] &8— &7Give a multitool"));
        sender.sendMessage(ColorUtil.of("  &e/mt list               &8— &7List loaded tools"));
        sender.sendMessage(ColorUtil.of("  &e/mt reload             &8— &7Reload config & items"));
        sender.sendMessage(ColorUtil.of("  &e/mt create <id>        &8— &7Generate YAML template"));
        sender.sendMessage(ColorUtil.of("  &e/mt export <format> <id> &8— &7Export to other plugins"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Tab Completion
    // ─────────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
                                      String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "list", "reload", "create", "export"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give", "create" ->
                        plugin.getItemManager().getDefinitions().forEach(
                                d -> completions.add(d.getId()));
                case "export" ->
                        completions.addAll(Arrays.asList("excellentshop", "excellentcrates"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args[0].equalsIgnoreCase("export")) {
                plugin.getItemManager().getDefinitions().forEach(
                        d -> completions.add(d.getId()));
            }
        }

        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────
    // Util
    // ─────────────────────────────────────────────────────────────────

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
