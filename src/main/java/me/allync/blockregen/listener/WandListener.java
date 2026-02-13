package me.allync.blockregen.listener;

import me.allync.blockregen.BlockRegen;
import me.allync.blockregen.manager.RegionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {

    private final BlockRegen plugin;
    private final RegionManager regionManager;

    public WandListener(BlockRegen plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        //Do not loop like 3 times for the same
        if (event.getHand() != EquipmentSlot.HAND
                || !player.hasPermission("blockregen.admin")
                || itemInHand == null
                || itemInHand.getType() == Material.AIR
                || !itemInHand.hasItemMeta()
                || itemInHand.getType() != plugin.getConfigManager().wandMaterial
                || !itemInHand.getItemMeta().getDisplayName()
                .equals(plugin.getConfigManager().wandName)) {
            return;
        }

        event.setCancelled(true);

        Action action = event.getAction();
        Location location = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;
        if (location == null) return;

        if (action == Action.LEFT_CLICK_BLOCK) {
            regionManager.setPos1(player, location);
            player.sendMessage(plugin.getConfigManager().prefix + plugin.getConfigManager().pos1Message
                    .replace("%location%", formatLocation(location)));
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            regionManager.setPos2(player, location);
            player.sendMessage(plugin.getConfigManager().prefix + plugin.getConfigManager().pos2Message
                    .replace("%location%", formatLocation(location)));
        }
    }

    private String formatLocation(Location loc) {
        return String.format("X: %d, Y: %d, Z: %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
