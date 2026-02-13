package me.allync.blockregen;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.allync.blockregen.command.BlockRegenCommand;
import me.allync.blockregen.command.RegenMultiplierCommand;
import me.allync.blockregen.listener.*;
import me.allync.blockregen.manager.*;
// Hapus import MiningMonitorTask
// import me.allync.blockregen.task.MiningMonitorTask;
import me.allync.blockregen.util.UpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
// Hapus import PotionEffectType
// import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BlockRegen extends JavaPlugin {

    private ConfigManager configManager;
    private BlockManager blockManager;
    private RegenManager regenManager;
    private RegionManager regionManager;
    private PlayerManager playerManager;
    private MultiplierManager multiplierManager;
    private MiningManager miningManager;
    // Hapus field MiningMonitorTask
    // private MiningMonitorTask miningMonitor;

    private WorldGuardPlugin worldGuardPlugin;
    private Economy economy = null;

    public static boolean mmoItemsEnabled;
    public static boolean itemsAdderEnabled;
    public static boolean harvestFlowEnabled;
    public static boolean mmocoreEnabled;
    public static boolean coinsEngineEnabled;

    private final Set<UUID> debuggingPlayers = new HashSet<>();
    private final Set<UUID> bypassPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        mmoItemsEnabled = getServer().getPluginManager().isPluginEnabled("MMOItems");
        if (mmoItemsEnabled) {
            getLogger().info("MMOItems found, integration enabled.");
            me.allync.blockregen.util.ItemUtil.setMmoItemsEnabled(true);
        }
        itemsAdderEnabled = getServer().getPluginManager().isPluginEnabled("ItemsAdder");
        if (itemsAdderEnabled) {
            getLogger().info("ItemsAdder found, integration enabled.");
        }
        harvestFlowEnabled = getServer().getPluginManager().isPluginEnabled("HarvestFlow");
        if (harvestFlowEnabled) {
            getLogger().info("HarvestFlow found, compatibility mode enabled.");
        }
        mmocoreEnabled = getServer().getPluginManager().isPluginEnabled("MMOCore");
        if (mmocoreEnabled) {
            getLogger().info("MMOCore found, integration enabled.");
        }

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        multiplierManager = new MultiplierManager(this);
        multiplierManager.load();

        if (multiplierManager.isAnyProfileEnabled()) {
            if (!setupEconomy()) {
                getLogger().info("Vault not found. Some multiplier profiles may not work unless they use CoinsEngine.");
            }
            setupCoinsEngine();
        }

        blockManager = new BlockManager(this);
        regenManager = new RegenManager(this);
        regionManager = new RegionManager(this);
        playerManager = new PlayerManager(this);
        miningManager = new MiningManager(this); // Inisialisasi MiningManager
        // Hapus inisialisasi MiningMonitorTask
        // miningMonitor = new MiningMonitorTask(this);

        blockManager.loadBlocks();
        regionManager.loadRegions();

        if (configManager.worldGuardEnabled) {
            Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
            if (plugin instanceof WorldGuardPlugin) {
                worldGuardPlugin = (WorldGuardPlugin) plugin;
                getLogger().info("WorldGuard integration enabled.");
            } else {
                getLogger().warning("WorldGuard not found or not a valid WorldGuard plugin. WorldGuard integration disabled.");
                configManager.worldGuardEnabled = false;
            }
        }
        if (configManager.checkForUpdates) {
            new UpdateChecker(this).check();
        }

        // Mendaftarkan Listener
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this, regionManager), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockMiningListener(this), this); // Daftarkan listener baru

        // Hapus task monitor
        // miningMonitor.runTaskTimer(this, 0L, 5L);

        // Mendaftarkan Command
        getCommand("blockregen").setExecutor(new BlockRegenCommand(this));
        getCommand("regenmultiplier").setExecutor(new RegenMultiplierCommand(this));

        getLogger().info("BlockRegen has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (regenManager != null) {
            regenManager.handleShutdown();
        }
        // Hapus semua logika onDisable yang lama
        // (PlayerQuitEvent di listener akan menangani pembersihan)
        getLogger().info("BlockRegen has been disabled.");
    }

    public void reloadPlugin() {
        configManager.loadConfig();
        multiplierManager.load();
        blockManager.loadBlocks();
        regionManager.loadRegions();

    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupCoinsEngine() {
        if (getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
            coinsEngineEnabled = true;
            getLogger().info("Successfully hooked into CoinsEngine.");
        } else {
            coinsEngineEnabled = false;
            getLogger().info("CoinsEngine not found, integration disabled.");
        }
    }


    public boolean isPlayerDebugging(Player player) {
        return debuggingPlayers.contains(player.getUniqueId());
    }

    public void toggleDebug(Player player) {
        UUID playerId = player.getUniqueId();
        if (debuggingPlayers.contains(playerId)) {
            debuggingPlayers.remove(playerId);
        } else {
            debuggingPlayers.add(playerId);
        }
    }

    public boolean isPlayerBypassing(Player player) {
        return bypassPlayers.contains(player.getUniqueId());
    }

    public void toggleBypass(Player player) {
        UUID playerId = player.getUniqueId();
        if (bypassPlayers.contains(playerId)) {
            bypassPlayers.remove(playerId);
        } else {
            bypassPlayers.add(playerId);
        }
    }

    // --- Getters ---
    public ConfigManager getConfigManager() { return configManager; }
    public BlockManager getBlockManager() { return blockManager; }
    public RegenManager getRegenManager() { return regenManager; }
    public RegionManager getRegionManager() { return regionManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public MultiplierManager getMultiplierManager() { return multiplierManager; }
    public MiningManager getMiningManager() { return miningManager; }
    // Hapus getter MiningMonitorTask
    // public MiningMonitorTask getMiningMonitor() { return miningMonitor; }
    public WorldGuardPlugin getWorldGuardPlugin() { return worldGuardPlugin; }
    public Economy getEconomy() { return economy; }
}