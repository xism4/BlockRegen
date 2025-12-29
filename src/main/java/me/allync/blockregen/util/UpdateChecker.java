package me.allync.blockregen.util;

import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final BlockRegen plugin;
    private final int RESOURCE_ID = 128152; // REPLACE WITH YOUR SPIGOTMC RESOURCE ID

    public UpdateChecker(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public void check() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String latestVersion = reader.readLine();
                    reader.close();

                    String currentVersion = plugin.getDescription().getVersion();

                    if (latestVersion != null && !latestVersion.isEmpty() && !latestVersion.equals(currentVersion)) {
                        String downloadLink = "https://www.spigotmc.org/resources/" + RESOURCE_ID + "/";
                        String updateMessage = plugin.getConfigManager().updateNotifyMessage
                                .replace("%latest_version%", latestVersion)
                                .replace("%current_version%", currentVersion)
                                .replace("%download_link%", downloadLink);

                        plugin.getLogger().info(updateMessage);
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.hasPermission("blockregen.admin")) {
                                p.sendMessage(updateMessage);
                            }
                        }
                    } else {
                        plugin.getLogger().info(ChatColor.GREEN + "[BlockRegen] You are running the latest version.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning(ChatColor.RED + "[BlockRegen] Failed to check for updates: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}
