package me.allync.blockregen.manager;

import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.data.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {

    private final BlockRegen plugin;
    private final File regionsFile;
    private FileConfiguration regionsConfig;

    private final List<Region> regions = new ArrayList<>();
    private final Map<UUID, Location> pos1Selections = new HashMap<>();
    private final Map<UUID, Location> pos2Selections = new HashMap<>();

    public RegionManager(BlockRegen plugin) {
        this.plugin = plugin;
        this.regionsFile = new File(plugin.getDataFolder(), "regions.yml");
        if (!regionsFile.exists()) {
            plugin.saveResource("regions.yml", false);
        }
        this.regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }

    public void loadRegions() {
        regions.clear();
        ConfigurationSection regionsSection = regionsConfig.getConfigurationSection("regions");
        if (regionsSection == null) return;

        for (String key : regionsSection.getKeys(false)) {
            World world = Bukkit.getWorld(regionsSection.getString(key + ".world"));
            if (world == null) {
                plugin.getLogger().warning("World not found for region '" + key + "'. Skipping...");
                continue;
            }
            Location pos1 = regionsSection.getVector(key + ".pos1").toLocation(world);
            Location pos2 = regionsSection.getVector(key + ".pos2").toLocation(world);
            regions.add(new Region(key, pos1, pos2));
        }
        plugin.getLogger().info("Loaded " + regions.size() + " regions.");
    }

    public void saveRegion(Player player, String name) throws IOException {
        Location pos1 = pos1Selections.get(player.getUniqueId());
        Location pos2 = pos2Selections.get(player.getUniqueId());

        if (pos1 == null || pos2 == null) {
            player.sendMessage(plugin.getConfigManager().prefix + "You must set both positions first.");
            return;
        }

        String path = "regions." + name;
        regionsConfig.set(path + ".world", pos1.getWorld().getName());
        regionsConfig.set(path + ".pos1", pos1.toVector());
        regionsConfig.set(path + ".pos2", pos2.toVector());
        regionsConfig.save(regionsFile);

        // Add to loaded regions and clear selections
        regions.add(new Region(name, pos1, pos2));
        pos1Selections.remove(player.getUniqueId());
        pos2Selections.remove(player.getUniqueId());
    }

    public boolean removeRegion(String name) throws IOException {
        Region regionToRemove = null;
        for (Region region : regions) {
            if (region.getName().equalsIgnoreCase(name)) {
                regionToRemove = region;
                break;
            }
        }

        if (regionToRemove != null) {
            regions.remove(regionToRemove);
            regionsConfig.set("regions." + name, null); // Remove from config
            regionsConfig.save(regionsFile);
            return true;
        }
        return false;
    }

    public boolean isLocationInRegion(Location location) {
        if (regions.isEmpty()) {
            return false; // If no regions are defined, deny regen everywhere
        }
        for (Region region : regions) {
            if (region.contains(location)) {
                return true;
            }
        }
        return false;
    }

    public void setPos1(Player player, Location location) {
        pos1Selections.put(player.getUniqueId(), location);
    }

    public void setPos2(Player player, Location location) {
        pos2Selections.put(player.getUniqueId(), location);
    }

    public Location getPos1(Player player) {
        return pos1Selections.get(player.getUniqueId());
    }

    public Location getPos2(Player player) {
        return pos2Selections.get(player.getUniqueId());
    }

    public List<String> getRegionNames() {
        List<String> names = new ArrayList<>();
        for (Region region : regions) {
            names.add(region.getName());
        }
        return names;
    }
}
