package me.allync.blockregen.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

public class SoundUtil {

    public static void playSound(Location location, String customSound, String defaultSound) {
        String soundString = (customSound != null && !customSound.isEmpty()) ? customSound : defaultSound;
        if (soundString == null || soundString.isEmpty()) return;

        String[] parts = soundString.split(":");
        try {
            Sound sound = Sound.valueOf(parts[0].toUpperCase());
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;

            World world = location.getWorld();
            if (world != null) {
                world.playSound(location, sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            // Invalid sound name or number format
            System.err.println("[BlockRegen] Invalid sound format: " + soundString);
        }
    }
}