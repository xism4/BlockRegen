package me.allync.blockregen.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.data.BlockData;
import me.allync.blockregen.data.ToolRequirement;
import me.allync.blockregen.manager.MiningManager;
import me.allync.blockregen.task.PlayerMiningTask; // <-- IMPORT BARU
import me.allync.blockregen.util.SoundUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
// Hapus PotionEffect imports

import java.util.HashMap; // <-- IMPORT BARU
import java.util.Map;
import java.util.UUID;

/**
 * Listener for BlockDamageEvent to handle custom block breaking durations.
 * New Approach: Per-player task.
 */
public class BlockMiningListener implements Listener {

    private final BlockRegen plugin;
    private final MiningManager miningManager;

    // --- NEW MAP TO TRACK ACTIVE TASKS ---
    private final Map<UUID, PlayerMiningTask> activeMiningTasks = new HashMap<>();

    public BlockMiningListener(BlockRegen plugin) {
        this.plugin = plugin;
        this.miningManager = plugin.getMiningManager();
        // Old maps (playerBreakTime, playerMiningLocation, playerLastHitTime) are removed
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String blockIdentifier = miningManager.getBlockIdentifier(block);

        // --- 1. Initial Checks ---
        if (player.getGameMode() != GameMode.SURVIVAL) return; // Only survival
        if (!plugin.getConfigManager().allWorldsEnabled && !plugin.getConfigManager().enabledWorlds.contains(player.getWorld().getName())) return;

        if (BlockRegen.harvestFlowEnabled) {
            Material blockType = block.getType();
            if (blockType == Material.WHEAT || blockType == Material.CARROTS ||
                    blockType == Material.POTATOES || blockType == Material.BEETROOTS ||
                    blockType == Material.NETHER_WART || blockType == Material.COCOA) {
                return;
            }
        }

        // --- 2. Get BlockData & Check if this listener should handle it ---
        BlockData data = plugin.getBlockManager().getBlockData(blockIdentifier);
        if (data == null || !data.hasCustomBreakDuration() || !data.isFixedDuration()) {
            // BlockBreakListener will handle it.
            return;
        }

        // --- 5. Custom Breaking Logic ---

        // --- NEW MAIN LOGIC ---

        // Stop vanilla breaking
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();

        // Check if player is already mining
        PlayerMiningTask existingTask = activeMiningTasks.get(uuid);
        if (existingTask != null) {
            // If player hits a *different* block, cancel the old task
            if (!existingTask.getBlockLocation().equals(block.getLocation())) {
                miningManager.debug(player, blockIdentifier, "&cSwitching target block. Cancelling old task.");
                existingTask.cancelTask();
                // Continue to create a new task below
            } else {
                // Player is hitting the same block, task is already running.
                // Do nothing.
                return;
            }
        }

        // --- 3. WorldGuard Checks ---
        if (plugin.getConfigManager().worldGuardEnabled && plugin.getWorldGuardPlugin() != null) {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            boolean canBreak = query.testBuild(BukkitAdapter.adapt(block.getLocation()), localPlayer, Flags.BLOCK_BREAK);

            if (!canBreak) { // DENY
                if (!plugin.getConfigManager().worldGuardBreakRegenInDenyRegions) {
                    miningManager.debug(player, blockIdentifier, "WorldGuard check &cfailed&7 (block-break: DENY).");
                    return; // Respect DENY
                }
            } else { // ALLOW
                if (plugin.getConfigManager().worldGuardDisableOtherBreak) {
                    if (!plugin.getBlockManager().isRegenBlock(blockIdentifier)) {
                        if (!plugin.isPlayerBypassing(player)) {
                            miningManager.debug(player, blockIdentifier, "Block is not a regen block. &cCancelling event due to WG config.");
                            return;
                        }
                    }
                }
            }
        }

        // --- 4. Further Checks (Region, Regenerating, Tool) ---
        if (!plugin.getRegionManager().isLocationInRegion(block.getLocation())) {
            miningManager.debug(player, blockIdentifier, "Location is not within a BlockRegen region. &cIgnoring.");
            return;
        }

        if (plugin.getRegenManager().isRegenerating(block.getLocation())) {
            miningManager.debug(player, blockIdentifier, "Block is already regenerating. &cCancelling event.");
            return;
        }

        if (data.requiresTool()) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            boolean toolMatches = false;
            for (ToolRequirement requirement : data.getRequiredTools()) {
                if (requirement.matches(itemInHand)) {
                    toolMatches = true;
                    break;
                }
            }
            if (!toolMatches) {
                miningManager.debug(player, blockIdentifier, "&cTool requirement not met.");
                player.sendMessage(plugin.getConfigManager().wrongToolMessage);
                SoundUtil.playSound(block.getLocation(), plugin.getConfigManager().wrongToolSound, null);
                return;
            }
        }

        // --- 6. Create and Start New Task ---
        miningManager.debug(player, blockIdentifier, "Started mining. Total time: " + data.getBreakDuration() + "s");
        PlayerMiningTask newTask = new PlayerMiningTask(plugin, player, block, data, blockIdentifier, activeMiningTasks);
        newTask.runTaskTimer(plugin, 0L, 1L); // Run every 1 tick

        activeMiningTasks.put(uuid, newTask);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up data if player logs off
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerMiningTask existingTask = activeMiningTasks.get(uuid);
        if (existingTask != null) {
            existingTask.cancelTask();
        }
    }
}