package me.allync.blockregen.util;

import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.manager.MultiplierManager;
import me.allync.blockregen.manager.MultiplierManager.MultiplierProfile;
import me.allync.blockregen.manager.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class MultiplierGUI {

    private final BlockRegen plugin;
    private final Player player;
    private final PlayerManager playerManager;
    private final MultiplierManager multiplierManager;

    public MultiplierGUI(BlockRegen plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerManager = plugin.getPlayerManager();
        this.multiplierManager = plugin.getMultiplierManager();
    }

    public void open() {
        Inventory gui = Bukkit.createInventory(null, multiplierManager.guiSize, multiplierManager.guiTitle);

        MultiplierProfile profile = multiplierManager.getProfileForPlayer(player);

        if (!profile.isEnabled()) {
            player.sendMessage(plugin.getConfigManager().prefix + "§cThe regen multiplier system is disabled in this world.");
            return;
        }

        if (multiplierManager.fillItemEnabled) {
            ItemStack fillItem = new ItemStack(multiplierManager.fillItemMaterial);
            ItemMeta fillMeta = fillItem.getItemMeta();
            if (fillMeta != null) {
                fillMeta.setDisplayName(multiplierManager.fillItemName);
                fillItem.setItemMeta(fillMeta);
            }
            for (int i = 0; i < multiplierManager.guiSize; i++) {
                gui.setItem(i, fillItem);
            }
        }

        int currentLevel = playerManager.getMultiplierLevel(player);

        // Info Item
        MultiplierManager.ConfigItem infoConfig = multiplierManager.getItem("info-item");
        if (infoConfig != null) {
            gui.setItem(infoConfig.slot, createItem(infoConfig, player, profile));
        }

        // Upgrade / Max Level Item
        if (currentLevel < profile.getMaxLevel()) {
            MultiplierManager.ConfigItem upgradeConfig = multiplierManager.getItem("upgrade-button");
            if (upgradeConfig != null) {
                gui.setItem(upgradeConfig.slot, createItem(upgradeConfig, player, profile));
            }
        } else {
            MultiplierManager.ConfigItem maxConfig = multiplierManager.getItem("max-level-item");
            if (maxConfig != null) {
                gui.setItem(maxConfig.slot, createItem(maxConfig, player, profile));
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createItem(MultiplierManager.ConfigItem configItem, Player p, MultiplierProfile profile) {
        ItemStack item = new ItemStack(configItem.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        int currentLevel = playerManager.getMultiplierLevel(p);
        int nextLevel = currentLevel + 1;
        double currentMultiplier = profile.getMultiplier(currentLevel);
        String cost = (currentLevel < profile.getMaxLevel()) ? multiplierManager.getFormattedCost(profile, nextLevel) : "N/A";

        meta.setDisplayName(replacePlaceholders(configItem.name, p, profile, currentLevel, currentMultiplier, cost, nextLevel));

        List<String> lore = configItem.lore.stream()
                .map(line -> replacePlaceholders(line, p, profile, currentLevel, currentMultiplier, cost, nextLevel))
                .collect(Collectors.toList());
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String replacePlaceholders(String text, Player p, MultiplierProfile profile, int level, double multiplier, String cost, int nextLevel) {
        return text.replace("%player_name%", p.getName())
                .replace("%world%", p.getWorld().getName())
                .replace("%profile%", profile.getName())
                .replace("%level%", String.valueOf(level))
                .replace("%multiplier%", String.valueOf(multiplier))
                .replace("%cost%", cost)
                .replace("%next_level%", String.valueOf(nextLevel));
    }
}
