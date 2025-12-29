package me.allync.blockregen.data;

import org.bukkit.Location;
import org.bukkit.World;

public class Region {

    private final String name;
    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public Region(String name, Location pos1, Location pos2) {
        this.name = name;
        this.world = pos1.getWorld();
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return world;
    }

    public Location getMinPoint() {
        return new Location(world, minX, minY, minZ);
    }

    public Location getMaxPoint() {
        return new Location(world, maxX, maxY, maxZ);
    }

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().equals(this.world)) {
            return false;
        }
        return location.getBlockX() >= minX && location.getBlockX() <= maxX &&
               location.getBlockY() >= minY && location.getBlockY() <= maxY &&
               location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }
}
