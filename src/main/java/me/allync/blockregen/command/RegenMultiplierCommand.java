package me.allync.blockregen.command;

import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.manager.MultiplierManager;
import me.allync.blockregen.manager.PlayerManager;
import me.allync.blockregen.util.MultiplierGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RegenMultiplierCommand implements CommandExecutor, TabCompleter {

    private final BlockRegen plugin;
    private final PlayerManager playerManager;
    private final MultiplierManager multiplierManager;

    public RegenMultiplierCommand(BlockRegen plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.multiplierManager = plugin.getMultiplierManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!multiplierManager.isAnyProfileEnabled()) {
            sender.sendMessage(plugin.getConfigManager().prefix + "§cThe regen multiplier system is currently disabled.");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("blockregen.multiplier.use")) {
                player.sendMessage(plugin.getConfigManager().noPermissionMessage);
                return true;
            }
            new MultiplierGUI(plugin, player).open();
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("blockregen.multiplier.admin")) {
                sender.sendMessage(plugin.getConfigManager().noPermissionMessage);
                return true;
            }

            if (args.length != 4) {
                sender.sendMessage(plugin.getConfigManager().prefix + multiplierManager.adminUsageMessage);
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().prefix + multiplierManager.adminPlayerNotFoundMessage.replace("%player%", args[1]));
                return true;
            }

            String profileName = args[2];
            MultiplierManager.MultiplierProfile profile = multiplierManager.getProfile(profileName);
            if (profile == null || profile.getName().equals("default") && !profileName.equalsIgnoreCase("default")) {
                sender.sendMessage(plugin.getConfigManager().prefix + multiplierManager.adminProfileNotFoundMessage.replace("%profile%", profileName));
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().prefix + multiplierManager.adminInvalidLevelMessage.replace("%max_level%", String.valueOf(profile.getMaxLevel())));
                return true;
            }

            if (level < 1 || level > profile.getMaxLevel()) {
                sender.sendMessage(plugin.getConfigManager().prefix + multiplierManager.adminInvalidLevelMessage.replace("%max_level%", String.valueOf(profile.getMaxLevel())));
                return true;
            }

            playerManager.setMultiplierLevel(target.getUniqueId(), profileName, level);
            playerManager.savePlayerData(target);

            sender.sendMessage(plugin.getConfigManager().prefix + multiplierManager.adminSetSuccessMessage
                    .replace("%player%", target.getName())
                    .replace("%profile%", profile.getName())
                    .replace("%level%", String.valueOf(level)));

            target.sendMessage(plugin.getConfigManager().prefix + multiplierManager.adminSetNotifyMessage
                    .replace("%profile%", profile.getName())
                    .replace("%level%", String.valueOf(level)));

            return true;
        }

        sender.sendMessage(plugin.getConfigManager().prefix + "§cUnknown command. Use /rm or " + multiplierManager.adminUsageMessage);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("blockregen.multiplier.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Collections.singletonList("set"), new ArrayList<>());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return StringUtil.copyPartialMatches(args[2], multiplierManager.getProfileNames(), new ArrayList<>());
        }

        return Collections.emptyList();
    }
}
