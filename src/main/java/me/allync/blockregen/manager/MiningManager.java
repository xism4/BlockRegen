package me.allync.blockregen.manager;

import dev.lone.itemsadder.api.CustomBlock;
import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.data.BlockData;
import me.allync.blockregen.data.CustomDrop;
import me.allync.blockregen.util.ItemUtil;
import me.allync.blockregen.util.ParticleUtil;
import me.allync.blockregen.util.SoundUtil;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.EXPSource;
import net.Indyuce.mmocore.experience.PlayerProfessions;
import net.Indyuce.mmocore.experience.Profession;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
// Hapus ConcurrentHashMap
// import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Manages the logic for block breaking (drops, sounds, particles, regen)
 * and tracks players actively mining custom-duration blocks.
 */
public class MiningManager {

    private final BlockRegen plugin;
    private final Logger logger;

    // --- HAPUS SEMUA MAP LAMA ---
    // private final Map<UUID, Long> playerBreakTime = new ConcurrentHashMap<>();
    // private final Map<UUID, Location> playerMiningLocation = new ConcurrentHashMap<>();
    // private final Map<UUID, Long> playerLastHitTime = new ConcurrentHashMap<>();
    // --- AKHIR PENGHAPUSAN ---

    public MiningManager(BlockRegen plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Processes the actual breaking of a regen block.
     * This is called by BlockBreakListener (for normal breaks)
     * or BlockMiningListener (for custom duration breaks).
     */
    public void processBlockBreak(Player player, Block block, BlockData data, BlockState originalState, String blockIdentifier) {
        // 1. Handle Drops (Custom & Natural)
        handleAllDrops(player, block, data, blockIdentifier);

        // 2. Execute Commands
        executeCommands(player, data);

        // 3. Handle Vanilla Experience
        String expAmountStr = data.getExpDropAmount();
        int expToDrop = 0;
        if (expAmountStr != null && !expAmountStr.isEmpty()) {
            expToDrop = parseAmount(expAmountStr);
        }

        if (expToDrop > 0) {
            if (data.isAutoPickupExp()) {
                player.giveExp(expToDrop);
            } else {
                // Drop orb at block location
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);

                final int finalExpToDrop = expToDrop;
                block.getWorld().spawn(loc, ExperienceOrb.class, orb -> {
                    orb.setExperience(finalExpToDrop);
                });
            }
        }

        // 4. Handle MMOCore Experience
        handleMMOCoreExp(player, block, data, blockIdentifier);

        // 5. Play Sound & Spawn Particles
        SoundUtil.playSound(block.getLocation(), data.getBreakSound(), plugin.getConfigManager().defaultBreakSound);
        if (plugin.getConfigManager().particlesEnabled && plugin.getConfigManager().particlesOnBreak) {
            String particle = data.getBreakParticle() != null ? data.getBreakParticle() : plugin.getConfigManager().defaultBreakParticle;
            ParticleUtil.spawnParticle(block.getLocation(), particle);
        }

        // 6. Schedule Regeneration
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            block.setType(data.getReplacedBlock());
            plugin.getRegenManager().startRegen(originalState, data.getRegenDelay(), blockIdentifier);
        });

        // 7. Send Action Bar Countdown
        if (plugin.getConfigManager().sendRegenCountdown) {
            plugin.getRegenManager().sendActionBarMessage(player, data.getRegenDelay());
        }
    }

    private void handleAllDrops(Player player, Block block, BlockData data, String blockIdentifier) {
        List<ItemStack> finalDrops = new ArrayList<>();
        ItemStack tool = player.getInventory().getItemInMainHand();
        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);

        double regenMultiplier = 1.0;
        if (plugin.getMultiplierManager().isAnyProfileEnabled()) {
            regenMultiplier = plugin.getPlayerManager().getMultiplierValue(player);
        }

        // Handle Custom Drops
        if (data.hasCustomDrops()) {
            for (CustomDrop customDrop : data.getCustomDrops().values()) {
                if (ThreadLocalRandom.current().nextDouble(100.0) < customDrop.getChance()) {
                    ItemStack item = ItemUtil.createItemStack(customDrop);
                    if (item != null) {
                        int amount = parseAmount(customDrop.getAmount());

                        // Apply custom drop fortune
                        if (customDrop.isFortuneEnabled() && fortuneLevel > 0) {
                            Map<Integer, Double> multipliers = customDrop.getFortuneMultipliers();
                            if (multipliers.containsKey(fortuneLevel)) {
                                double multiplier = multipliers.get(fortuneLevel);
                                amount = (int) Math.round(amount * multiplier);
                                debug(player, blockIdentifier,"Applied Custom Drop Fortune " + fortuneLevel + " multiplier of " + multiplier + ". New amount: " + amount);
                            }
                        }

                        // Apply regen multiplier
                        if (regenMultiplier > 1.0) {
                            amount = (int) Math.round(amount * regenMultiplier);
                            debug(player, blockIdentifier,"Applied Regen Multiplier of x" + regenMultiplier + ". New amount: " + amount);
                        }

                        if (amount > 0) {
                            item.setAmount(amount);
                            finalDrops.add(item);
                        }
                    }
                    if (customDrop.hasCommands()) {
                        executeCommandList(player, customDrop.getCommands());
                    }
                }
            }
        }

        // Handle Natural Drops
        if (data.isNaturalDrop()) {
            Collection<ItemStack> naturalDrops = block.getDrops(tool);
            // Apply vanilla fortune config
            BlockData.FortuneData fortuneData = data.getFortuneData();

            for (ItemStack drop : naturalDrops) {
                int amount = drop.getAmount();

                // Apply vanilla fortune multiplier from config
                if (fortuneData != null && fortuneData.isEnabled() && fortuneLevel > 0) {
                    Map<Integer, Double> multipliers = fortuneData.getMultipliers();
                    if (multipliers.containsKey(fortuneLevel)) {
                        double multiplier = multipliers.get(fortuneLevel);
                        amount = (int) Math.round(amount * multiplier);
                        debug(player, blockIdentifier,"Applied Vanilla Fortune " + fortuneLevel + " multiplier of " + multiplier + ". New amount: " + amount);
                    }
                }

                // Apply regen multiplier
                if (regenMultiplier > 1.0) {
                    amount = (int) Math.round(amount * regenMultiplier);
                    debug(player, blockIdentifier,"Applied Regen Multiplier of x" + regenMultiplier + " to natural drop. New amount: " + amount);
                }
                if (amount > 0) {
                    ItemStack finalDrop = drop.clone();
                    finalDrop.setAmount(amount);
                    finalDrops.add(finalDrop);
                }
            }
        }

        if (finalDrops.isEmpty()) {
            return;
        }

        // Give items to player
        if (data.isAutoInventory()) {
            PlayerInventory inventory = player.getInventory();
            HashMap<Integer, ItemStack> remaining = inventory.addItem(finalDrops.toArray(new ItemStack[0]));
            if (!remaining.isEmpty()) {
                remaining.values().forEach(i -> block.getWorld().dropItemNaturally(block.getLocation(), i));
            }
        } else {
            finalDrops.forEach(i -> block.getWorld().dropItemNaturally(block.getLocation(), i));
        }
    }


    private void executeCommands(Player player, BlockData data) {
        if (data.getCommands() == null || data.getCommands().isEmpty()) return;
        executeCommandList(player, data.getCommands());
    }

    private void executeCommandList(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        for (String command : commands) {
            String formattedCmd = command.replace("%player%", player.getName());
            if (formattedCmd.startsWith("[CONSOLE]")) {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), formattedCmd.substring(9).trim());
            } else if (formattedCmd.startsWith("[PLAYER]")) {
                player.performCommand(formattedCmd.substring(8).trim());
            } else if (formattedCmd.startsWith("[OP]")) {
                boolean wasOp = player.isOp();
                try {
                    if (!wasOp) player.setOp(true);
                    player.performCommand(formattedCmd.substring(4).trim());
                } finally {
                    if (!wasOp) player.setOp(false);
                }
            } else {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), formattedCmd);
            }
        }
    }

    private void handleMMOCoreExp(Player player, Block block, BlockData data, String blockIdentifier) {
        if (!BlockRegen.mmocoreEnabled || !plugin.getConfigManager().mmocoreEnabled) {
            return;
        }
        if (!data.hasMmocoreExp()) {
            return;
        }

        debug(player, blockIdentifier, "Handling MMOCore EXP drops...");

        PlayerData playerData = PlayerData.get(player);
        PlayerProfessions professions = playerData.getCollectionSkills();

        for (Map.Entry<String, Integer> entry : data.getMmocoreExp().entrySet()) {
            String professionId = entry.getKey();
            int expAmount = entry.getValue();

            Profession profession = MMOCore.plugin.professionManager.get(professionId);

            if (profession != null) {
                professions.giveExperience(profession, expAmount, EXPSource.SOURCE,
                        block.getLocation().add(
                                plugin.getConfigManager().mmocoreHologramOffsetX,
                                plugin.getConfigManager().mmocoreHologramOffsetY,
                                plugin.getConfigManager().mmocoreHologramOffsetZ),
                        true);
                debug(player, blockIdentifier, "&aGave &f" + expAmount + " &aEXP to profession &f" + professionId);
            } else {
                debug(player, blockIdentifier, "&cCould not find profession with ID '&f" + professionId + "&c' for player " + player.getName());
            }
        }
    }

    private int parseAmount(String amountStr) {
        if (amountStr.contains("-")) {
            String[] parts = amountStr.split("-");
            try {
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                if (min >= max) return min;
                return ThreadLocalRandom.current().nextInt(min, max + 1);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                return 1;
            }
        } else {
            try {
                return Integer.parseInt(amountStr);
            } catch (NumberFormatException e) {
                return 1;
            }
        }
    }

    /**
     * Gets the unique identifier for a block (Vanilla material or ItemsAdder ID).
     */
    public String getBlockIdentifier(Block block) {
        if (BlockRegen.itemsAdderEnabled) {
            CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
            if (customBlock != null) {
                return customBlock.getNamespacedID();
            }
        }
        return block.getType().name();
    }

    /**
     * Sends a debug message to a player if they have debugging enabled.
     */
    public void debug(Player player, String blockIdentifier, String message) {
        if (plugin.isPlayerDebugging(player)) {
            player.sendMessage(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&',
                    "&8[&eBlockRegen&8-&6Debug&8] (&b" + blockIdentifier + "&8) &7" + message));
        }
    }

    // --- HAPUS SEMUA GETTER LAMA ---
    // public Map<UUID, Long> getPlayerBreakTime() { ... }
    // public Map<UUID, Location> getPlayerMiningLocation() { ... }
    // public Map<UUID, Long> getPlayerLastHitTime() { ... }
    // public void clearPlayerData(UUID uuid) { ... }
    // --- AKHIR PENGHAPUSAN ---
}