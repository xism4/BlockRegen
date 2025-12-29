package me.allync.blockregen.data;

import org.bukkit.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomDrop {

    private final double chance;
    private final String amount;
    private final String material;
    private final String name;
    private final List<String> lore;
    private final List<String> commands;
    // --- UBAH: Ini sekarang satu objek, bukan map ---
    private final BlockData.FortuneData fortuneData;
    // --- AKHIR UBAHAN ---

    public CustomDrop(ConfigurationSection section) {
        this.chance = section.getDouble("chance", 100.0);
        this.amount = section.getString("amount", "1");
        this.material = section.getString("material");
        this.name = section.getString("name");
        this.lore = section.getStringList("lore");
        this.commands = section.getStringList("commands");

        // --- LOAD FORTUNE CONFIGURATION ---
        // Ini sekarang memuat sub-bagian 'fortune' sama seperti di BlockData
        ConfigurationSection fortuneSection = section.getConfigurationSection("fortune");
        if (fortuneSection != null) {
            this.fortuneData = new BlockData.FortuneData(fortuneSection);
        } else {
            this.fortuneData = null; // Tidak ada konfigurasi fortune
        }
        // --- END FORTUNE LOADING ---
    }

    public double getChance() {
        return chance;
    }

    public String getAmount() {
        return amount;
    }

    public String getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getCommands() {
        return commands;
    }

    // --- GETTER BARU UNTUK FORTUNE ---
    public boolean isFortuneEnabled() {
        return fortuneData != null && fortuneData.isEnabled();
    }

    public Map<Integer, Double> getFortuneMultipliers() {
        return (fortuneData != null) ? fortuneData.getMultipliers() : new HashMap<>();
    }
    // --- AKHIR GETTER BARU ---

    public boolean hasName() {
        return this.name != null && !this.name.isEmpty();
    }

    public boolean hasLore() {
        return this.lore != null && !this.lore.isEmpty();
    }

    public boolean hasCommands() {
        return this.commands != null && !this.commands.isEmpty();
    }
}