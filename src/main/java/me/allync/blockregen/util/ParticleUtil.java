package me.allync.blockregen.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public class ParticleUtil {

    public static void spawnParticle(Location location, String particleString) {
        if (location == null || particleString == null || particleString.isEmpty()) {
            return;
        }

        String[] parts = particleString.split(":");
        if (parts.length < 1) {
            return;
        }

        Particle particle;
        try {
            particle = Particle.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid particle name
            return;
        }

        int count = 1;
        if (parts.length > 1) {
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
                // Use default count
            }
        }

        double offset = 0.0;
        if (parts.length > 2) {
            try {
                offset = Double.parseDouble(parts[2]);
            } catch (NumberFormatException ignored) {
                // Use default offset
            }
        }

        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(particle, location, count, offset, offset, offset);
        }
    }
}
