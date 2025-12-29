package me.allync.blockregen.task;

import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.data.BlockData;
import me.allync.blockregen.manager.MiningManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

/**
 * A per-player task that manages the custom breaking timer, animations,
 * and completion of a fixed-duration block.
 */
public class PlayerMiningTask extends BukkitRunnable {

    private final BlockRegen plugin;
    private final MiningManager miningManager;
    private final Player player;
    private final Block block;
    private final BlockData data;
    private final String blockIdentifier;
    private final BlockState originalState;
    private final long startTime;
    private final long requiredTimeMs;
    private final Map<UUID, PlayerMiningTask> miningTasks;

    private int currentStage = -1; // Pelacak stage untuk anti-flicker

    public PlayerMiningTask(BlockRegen plugin, Player player, Block block, BlockData data, String blockIdentifier, Map<UUID, PlayerMiningTask> miningTasks) {
        this.plugin = plugin;
        this.miningManager = plugin.getMiningManager();
        this.player = player;
        this.block = block;
        this.data = data;
        this.blockIdentifier = blockIdentifier;
        this.originalState = block.getState();
        this.miningTasks = miningTasks;

        this.startTime = System.currentTimeMillis();
        this.requiredTimeMs = (long) (data.getBreakDuration() * 1000.0);
    }

    @Override
    public void run() {
        // 1. Check if task should be cancelled (Player offline)
        if (!player.isOnline()) {
            // Player offline, tidak bisa kirim debug
            cancelTask();
            return;
        }

        // 2. Check if player is still looking at the same block
        Block targetBlock = null;
        try {
            // Gunakan getTargetBlockExact, ini cepat dan kita butuh presisi.
            // Perlu dicek null jika pemain melihat ke udara.
            targetBlock = player.getTargetBlockExact(5);
        } catch (IllegalStateException e) {
            // Player sedang melihat ke udara
        }

        if (targetBlock == null || !targetBlock.getLocation().equals(block.getLocation())) {
            miningManager.debug(player, blockIdentifier, "&cCancelling mining (Target changed).");
            cancelTask();
            return;
        }

        // 3. Check if player is still in GameMode SURVIVAL
        if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL) {
            miningManager.debug(player, blockIdentifier, "&cCancelling mining (Gamemode changed).");
            cancelTask();
            return;
        }

        // 4. Player is still mining. Calculate progress.
        long elapsedTime = System.currentTimeMillis() - startTime;
        float progress = (float) elapsedTime / (float) requiredTimeMs;

        if (progress >= 1.0f) {
            // 5. Block is broken!
            miningManager.debug(player, blockIdentifier, "Block break finished.");

            // Kirim animasi hancur final (stage 10)
            player.sendBlockDamage(block.getLocation(), 1.0f);

            // Proses penghancuran (drops, sound, particles, regen)
            miningManager.processBlockBreak(player, block, data, originalState, blockIdentifier);

            // Bersihkan dan batalkan
            miningTasks.remove(player.getUniqueId());
            this.cancel(); // Hentikan task ini

        } else {
            // 6. Kirim animasi retak HANYA jika stage berubah (Anti-Flicker)

            int newStage = (int) (progress * 10.0f); // Hitung stage (0-9)

            if (newStage != this.currentStage) {
                // Hanya kirim packet jika stagenya berubah
                this.currentStage = newStage;
                player.sendBlockDamage(block.getLocation(), progress);
            }
        }
    }

    /**
     * Membatalkan task dan membersihkan.
     */
    public void cancelTask() {
        if (!this.isCancelled()) {
            this.cancel();
        }
        miningTasks.remove(player.getUniqueId());
        // Kirim packet untuk menghapus animasi retak
        if (player.isOnline()) {
            player.sendBlockDamage(block.getLocation(), -1.0f);
        }
    }

    /**
     * Mendapatkan lokasi blok yang sedang ditambang task ini.
     * @return Lokasi Blok
     */
    public Location getBlockLocation() {
        return this.block.getLocation();
    }
}