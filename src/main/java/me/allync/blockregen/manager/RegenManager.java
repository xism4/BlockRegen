package me.allync.blockregen.manager;

import java.util.HashMap;
import java.util.Map;
import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.task.RegenTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RegenManager {
    private final BlockRegen plugin;

    private final Map<Location, BlockState> regeneratingBlocks = new HashMap<>();

    public RegenManager(BlockRegen plugin) {
        this.plugin = plugin;
    }

    public void startRegen(BlockState originalState, int delay, String blockIdentifier) {
        this.regeneratingBlocks.put(originalState.getLocation(), originalState);
        (new RegenTask(this.plugin, this, originalState, blockIdentifier)).runTaskLater((Plugin)this.plugin, delay * 20L);
    }

    public boolean isRegenerating(Location location) {
        return this.regeneratingBlocks.containsKey(location);
    }

    public void removeRegenerating(Location location) {
        this.regeneratingBlocks.remove(location);
    }

    public void handleShutdown() {
        this.plugin.getLogger().info("Server is shutting down. Regenerating all pending blocks immediately...");
        (new HashMap<>(this.regeneratingBlocks)).forEach((location, state) -> state.update(true, false));
        this.regeneratingBlocks.clear();
        this.plugin.getLogger().info("All pending blocks have been regenerated.");
    }

    /**
     * Sends the action bar countdown message.
     * Dipindahkan ke sini agar bisa diakses oleh MiningManager.
     */
    public void sendActionBarMessage(Player player, int delay) {
        if (plugin.getConfigManager().sendRegenCountdown) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(plugin.getConfigManager().regenCountdownMessage.replace("%time%", String.valueOf(delay))));
        }
    }
}