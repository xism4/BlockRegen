package me.allync.blockregen.command;

import me.allync.blockregen.BlockRegen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlockRegenCommand implements CommandExecutor, TabCompleter {

    private final BlockRegen plugin;

    public BlockRegenCommand(BlockRegen plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("blockregen.admin")) {
            sender.sendMessage(plugin.getConfigManager().noPermissionMessage);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(plugin.getConfigManager().helpHeader);
            sender.sendMessage(plugin.getConfigManager().helpTitle);
            plugin.getConfigManager().helpCommands.forEach(sender::sendMessage);
            sender.sendMessage(plugin.getConfigManager().helpFooter);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getConfigManager().reloadMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().prefix + "This command can only be run by a player.");
                return true;
            }
            Player player = (Player) sender;
            plugin.toggleDebug(player);
            boolean isDebugging = plugin.isPlayerDebugging(player);
            player.sendMessage(plugin.getConfigManager().prefix + "Your personal debug mode has been " + (isDebugging ? "§aenabled" : "§cdisabled") + "§f.");
            return true;
        }

        // --- BLOK KODE BARU UNTUK PERINTAH BYPASS ---
        if (args[0].equalsIgnoreCase("bypass")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().prefix + "This command can only be run by a player.");
                return true;
            }
            Player player = (Player) sender;
            plugin.toggleBypass(player);
            boolean isBypassing = plugin.isPlayerBypassing(player);
            player.sendMessage(plugin.getConfigManager().prefix + "Bypass mode for breaking non-regen blocks has been " + (isBypassing ? "§aenabled" : "§cdisabled") + "§f.");
            return true;
        }
        // --- AKHIR BLOK KODE BARU ---

        if (args[0].equalsIgnoreCase("wand")) {
            // ... (kode wand tetap sama) ...
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().prefix + "This command can only be run by a player.");
                return true;
            }
            Player player = (Player) sender;
            ItemStack wand = new ItemStack(plugin.getConfigManager().wandMaterial);
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getConfigManager().wandName);
                meta.setLore(Collections.singletonList("§7Left-click to set position 1"));
                wand.setItemMeta(meta);
            }
            player.getInventory().addItem(wand);
            player.sendMessage(plugin.getConfigManager().wandReceiveMessage);
            return true;
        }

        if (args[0].equalsIgnoreCase("save")) {
            // ... (kode save tetap sama) ...
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().prefix + "This command can only be run by a player.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(plugin.getConfigManager().prefix + "Usage: /br save <name>");
                return true;
            }
            Player player = (Player) sender;
            String regionName = args[1];
            try {
                plugin.getRegionManager().saveRegion(player, regionName);
                sender.sendMessage(plugin.getConfigManager().regionSaveMessage.replace("%region%", regionName));
            } catch (IOException e) {
                sender.sendMessage(plugin.getConfigManager().prefix + "§cCould not save region to file. Check console for errors.");
                e.printStackTrace();
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            // ... (kode remove tetap sama) ...
            if (args.length < 2) {
                sender.sendMessage(plugin.getConfigManager().prefix + "Usage: /br remove <name>");
                return true;
            }
            String regionName = args[1];
            try {
                if (plugin.getRegionManager().removeRegion(regionName)) {
                    sender.sendMessage(plugin.getConfigManager().regionRemovedMessage.replace("%region%", regionName));
                } else {
                    sender.sendMessage(plugin.getConfigManager().regionNotFoundMessage.replace("%region%", regionName));
                }
            } catch (IOException e) {
                sender.sendMessage(plugin.getConfigManager().prefix + "§cCould not remove region from file. Check console for errors.");
                e.printStackTrace();
            }
            return true;
        }

        sender.sendMessage(plugin.getConfigManager().prefix + "Unknown command. Use /br help for a list of commands.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest main subcommands
            completions.add("reload");
            completions.add("wand");
            completions.add("save");
            completions.add("remove");
            completions.add("debug");
            completions.add("bypass"); // <-- TAMBAHAN TAB-COMPLETION
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("save") || args[0].equalsIgnoreCase("remove")) {
                // Suggest existing region names for save/remove
                completions.addAll(plugin.getRegionManager().getRegionNames());
            }
        }

        // Filter suggestions based on what the player has typed
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}