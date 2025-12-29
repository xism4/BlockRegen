package me.allync.blockregen.manager;

import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.manager.MultiplierManager.MultiplierProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final BlockRegen plugin;
    private final Map<UUID, Map<String, Integer>> playerLevelCache = new ConcurrentHashMap<>();
    private final File playerDataFolder;

    public PlayerManager(BlockRegen plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        Map<String, Integer> levels = new HashMap<>();

        if (playerFile.exists()) {
            FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
            ConfigurationSection levelSection = playerData.getConfigurationSection("multiplier-levels");
            if (levelSection != null) {
                for (String profileName : levelSection.getKeys(false)) {
                    levels.put(profileName.toLowerCase(), levelSection.getInt(profileName));
                }
            }
        }
        playerLevelCache.put(uuid, levels);
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerLevelCache.containsKey(uuid)) {
            return; // Tidak ada data untuk disimpan
        }
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        FileConfiguration playerData = new YamlConfiguration();
        Map<String, Integer> levels = playerLevelCache.get(uuid);
        if (levels != null && !levels.isEmpty()) {
            playerData.set("multiplier-levels", levels);
            try {
                playerData.save(playerFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save player data for " + player.getName());
                e.printStackTrace();
            }
        }
    }

    public void unloadPlayerData(Player player) {
        if (playerLevelCache.containsKey(player.getUniqueId())) {
            savePlayerData(player);
            playerLevelCache.remove(player.getUniqueId());
        }
    }

    public int getMultiplierLevel(Player player) {
        String profileName = plugin.getConfigManager().getMultiplierProfileForWorld(player.getWorld().getName());
        return getMultiplierLevel(player.getUniqueId(), profileName);
    }

    public int getMultiplierLevel(UUID uuid, String profileName) {
        return playerLevelCache.getOrDefault(uuid, new HashMap<>()).getOrDefault(profileName.toLowerCase(), 1);
    }

    public void setMultiplierLevel(Player player, int level) {
        String profileName = plugin.getConfigManager().getMultiplierProfileForWorld(player.getWorld().getName());
        setMultiplierLevel(player.getUniqueId(), profileName, level);
    }

    public void setMultiplierLevel(UUID uuid, String profileName, int level) {
        playerLevelCache.computeIfAbsent(uuid, k -> new HashMap<>()).put(profileName.toLowerCase(), level);
    }

    public double getMultiplierValue(Player player) {
        MultiplierProfile profile = plugin.getMultiplierManager().getProfileForPlayer(player);
        if (profile == null || !profile.isEnabled()) {
            return 1.0;
        }
        int level = getMultiplierLevel(player);
        return profile.getMultiplier(level);
    }
}
