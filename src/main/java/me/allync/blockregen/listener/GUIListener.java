package me.allync.blockregen.listener;

import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;
import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.manager.MultiplierManager;
import me.allync.blockregen.manager.MultiplierManager.MultiplierProfile;
import me.allync.blockregen.manager.PlayerManager;
import me.allync.blockregen.util.MultiplierGUI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final BlockRegen plugin;
    private final PlayerManager playerManager;
    private final MultiplierManager multiplierManager;
    private final Economy economy;

    public GUIListener(BlockRegen plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.multiplierManager = plugin.getMultiplierManager();
        this.economy = plugin.getEconomy();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(multiplierManager.guiTitle)) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        MultiplierManager.ConfigItem upgradeButton = multiplierManager.getItem("upgrade-button");
        if (upgradeButton == null) return;

        if (event.getSlot() == upgradeButton.slot) {
            handleUpgrade(player);
        }
    }

    private void handleUpgrade(Player player) {
        MultiplierProfile profile = multiplierManager.getProfileForPlayer(player);
        if (!profile.isEnabled()) {
            player.closeInventory();
            return;
        }

        int currentLevel = playerManager.getMultiplierLevel(player);
        if (currentLevel >= profile.getMaxLevel()) {
            player.closeInventory();
            return;
        }

        int nextLevel = currentLevel + 1;
        double cost = profile.getCost(nextLevel);

        if (cost <= 0) {
            player.closeInventory();
            return;
        }

        String balanceType = profile.getBalanceType();
        boolean transactionSuccess = false;

        if ("Vault".equalsIgnoreCase(balanceType)) {
            if (economy == null) {
                player.sendMessage(ChatColor.RED + "Vault economy is not available on the server.");
                player.closeInventory();
                return;
            }
            if (economy.getBalance(player) < cost) {
                player.sendMessage(multiplierManager.notEnoughMoneyMessage.replace("%cost%", multiplierManager.getFormattedCost(profile, nextLevel)));
                player.closeInventory();
                return;
            }
            transactionSuccess = economy.withdrawPlayer(player, cost).transactionSuccess();

        } else if ("Coin".equalsIgnoreCase(balanceType)) {
            // PERUBAHAN BESAR: Menggunakan API statis, sama seperti di NightMarket
            if (!BlockRegen.coinsEngineEnabled) {
                player.sendMessage(ChatColor.RED + "CoinsEngine is not available on the server.");
                player.closeInventory();
                return;
            }

            String currencyName = profile.getCoinEngineType();
            // Ambil objek Currency menggunakan metode API statis
            Currency currency = CoinsEngineAPI.getCurrency(currencyName);

            if (currency == null) {
                player.sendMessage(ChatColor.RED + "The currency '" + currencyName + "' does not exist in CoinsEngine.");
                plugin.getLogger().warning("Invalid currency '" + currencyName + "' configured for multiplier profile '" + profile.getName() + "'.");
                player.closeInventory();
                return;
            }

            // Periksa saldo menggunakan metode API statis
            if (CoinsEngineAPI.getBalance(player, currency) < cost) {
                player.sendMessage(multiplierManager.notEnoughMoneyMessage.replace("%cost%", multiplierManager.getFormattedCost(profile, nextLevel)));
                player.closeInventory();
                return;
            }

            // Kurangi saldo menggunakan metode API statis
            CoinsEngineAPI.removeBalance(player, currency, cost);
            transactionSuccess = true; // Asumsikan berhasil jika tidak ada error

        } else {
            player.sendMessage(ChatColor.RED + "Unsupported balance type in multiplier config: " + balanceType);
            player.closeInventory();
            return;
        }

        if (transactionSuccess) {
            playerManager.setMultiplierLevel(player, nextLevel);
            playerManager.savePlayerData(player);
            player.sendMessage(multiplierManager.successMessage.replace("%level%", String.valueOf(nextLevel)));
            new MultiplierGUI(plugin, player).open();
        } else {
            player.sendMessage(ChatColor.RED + "An error occurred during the transaction.");
            player.closeInventory();
        }
    }
}

