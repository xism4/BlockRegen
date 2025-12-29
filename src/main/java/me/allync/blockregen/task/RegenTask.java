package me.allync.blockregen.task;

import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.data.BlockData;
import me.allync.blockregen.manager.RegenManager;
import me.allync.blockregen.util.ParticleUtil;
import me.allync.blockregen.util.SoundUtil;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitRunnable;

public class RegenTask extends BukkitRunnable {
    private final BlockRegen plugin;

    private final RegenManager regenManager;

    private final BlockState originalState;

    private final String blockIdentifier;

    public RegenTask(BlockRegen plugin, RegenManager regenManager, BlockState originalState, String blockIdentifier) {
        this.plugin = plugin;
        this.regenManager = regenManager;
        this.originalState = originalState;
        this.blockIdentifier = blockIdentifier;
    }

    public void run() {
        this.originalState.update(true, false);
        this.regenManager.removeRegenerating(this.originalState.getLocation());
        BlockData data = this.plugin.getBlockManager().getBlockData(this.blockIdentifier);
        String sound = (data != null) ? data.getRegenSound() : null;
        SoundUtil.playSound(this.originalState.getLocation(), sound, (this.plugin.getConfigManager()).defaultRegenSound);
        if ((this.plugin.getConfigManager()).particlesEnabled && (this.plugin.getConfigManager()).particlesOnRegen) {
            String particle = (data != null && data.getRegenParticle() != null) ? data.getRegenParticle() : (this.plugin.getConfigManager()).defaultRegenParticle;
            ParticleUtil.spawnParticle(this.originalState.getLocation(), particle);
        }
    }
}
