package me.allync.blockregen.data;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockData {

    private final Material replacedBlock;
    private final int regenDelay;
    private final List<ToolRequirement> requiredTools;
    private final boolean autoInventory;
    private final boolean naturalDrop;
    private final String breakSound;
    private final String regenSound;
    private final List<String> commands;
    private final Map<String, CustomDrop> customDrops;
    private final String breakParticle;
    private final String regenParticle;
    private final String expDropAmount;
    private final boolean autoPickupExp;
    private final Map<String, Integer> mmocoreExp;
    private final FortuneData fortuneData; // Diubah dari Map ke single object

    // --- BARU ---
    private final double breakDuration;
    private final boolean fixedDuration;
    // --- AKHIR BARU ---

    @SuppressWarnings("unchecked")
    public BlockData(ConfigurationSection section) {
        this.replacedBlock = Material.valueOf(section.getString("replaced-block", "STONE").toUpperCase());
        this.regenDelay = section.getInt("regen-delay", 5);
        this.requiredTools = new ArrayList<>();

        List<?> toolsList = section.getList("tools-required");
        if (toolsList != null) {
            for (Object toolObject : toolsList) {
                try {
                    String materialString = null;
                    Map<String, Object> detailsMap = null;

                    if (toolObject instanceof String) {
                        materialString = (String) toolObject;
                    } else if (toolObject instanceof Map) {
                        Map<String, Object> toolMap = (Map<String, Object>) toolObject;
                        if (toolMap.isEmpty()) continue;

                        materialString = toolMap.keySet().iterator().next();
                        detailsMap = (Map<String, Object>) toolMap.get(materialString);
                    }

                    if (materialString != null) {
                        String name = (detailsMap != null && detailsMap.containsKey("name")) ? (String) detailsMap.get("name") : null;
                        List<String> lore = (detailsMap != null && detailsMap.containsKey("lore")) ? (List<String>) detailsMap.get("lore") : null;

                        if (materialString.contains(":")) {
                            this.requiredTools.add(new ToolRequirement(materialString, name, lore));
                        } else {
                            Material material = Material.valueOf(materialString.toUpperCase());
                            this.requiredTools.add(new ToolRequirement(material, name, lore));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[BlockRegen] Gagal memuat salah satu tool requirement di blok '" + section.getName() + "'. Entri: " + toolObject + ". Error: " + e.getMessage());
                }
            }
        }

        this.autoInventory = section.getBoolean("drops.auto-inventory", false);
        this.naturalDrop = section.getBoolean("drops.natural-drop", true);
        this.breakSound = section.getString("sounds.break-sound");
        this.regenSound = section.getString("sounds.regen-sound");
        this.commands = section.getStringList("commands");
        this.customDrops = new HashMap<>();
        ConfigurationSection customDropsSection = section.getConfigurationSection("custom-drops");
        if (customDropsSection != null) {
            for (String key : customDropsSection.getKeys(false)) {
                ConfigurationSection dropSection = customDropsSection.getConfigurationSection(key);
                if (dropSection != null) {
                    this.customDrops.put(key, new CustomDrop(dropSection));
                }
            }
        }
        this.breakParticle = section.getString("particles.break-particle");
        this.regenParticle = section.getString("particles.regen-particle");

        if (section.isSet("exp-drop-amount")) {
            this.expDropAmount = section.getString("exp-drop-amount", "0");
        } else {
            this.expDropAmount = getVanillaExpDrop(section.getName());
        }

        this.autoPickupExp = section.getBoolean("auto-pickup-exp", false);

        this.mmocoreExp = new HashMap<>();
        ConfigurationSection mmocoreSection = section.getConfigurationSection("mmocore-exp");
        if (mmocoreSection != null) {
            for (String professionId : mmocoreSection.getKeys(false)) {
                int exp = mmocoreSection.getInt(professionId);
                this.mmocoreExp.put(professionId.toLowerCase(), exp);
            }
        }

        // --- PERUBAHAN ---
        // 'fortune' sekarang adalah satu objek, bukan map.
        ConfigurationSection fortuneSection = section.getConfigurationSection("fortune");
        if (fortuneSection != null) {
            this.fortuneData = new FortuneData(fortuneSection);
        } else {
            this.fortuneData = null; // Tidak ada konfigurasi fortune
        }
        // --- AKHIR PERUBAHAN ---

        // --- BARU ---
        this.breakDuration = section.getDouble("break-duration", 0.0);
        this.fixedDuration = section.getBoolean("fixed-duration", false);
        // --- AKHIR BARU ---
    }

    public static class FortuneData {
        private final boolean enabled;
        private final Map<Integer, Double> multipliers;

        public FortuneData(ConfigurationSection section) {
            this.enabled = section.getBoolean("enabled", false);
            this.multipliers = new HashMap<>();
            ConfigurationSection multSection = section.getConfigurationSection("multiplier");
            if (multSection != null) {
                for (String key : multSection.getKeys(false)) {
                    try {
                        this.multipliers.put(Integer.parseInt(key), multSection.getDouble(key));
                    } catch (NumberFormatException e) {
                        System.err.println("[BlockRegen] Invalid fortune level key: " + key);
                    }
                }
            }
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Map<Integer, Double> getMultipliers() {
            return multipliers;
        }
    }

    private String getVanillaExpDrop(String blockName) {
        return switch (blockName.toUpperCase()) {
            case "COAL_ORE", "DEEPSLATE_COAL_ORE" -> "0-2";
            case "LAPIS_ORE", "DEEPSLATE_LAPIS_ORE" -> "2-5";
            case "REDSTONE_ORE", "DEEPSLATE_REDSTONE_ORE" -> "1-5";
            case "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE" -> "3-7";
            case "EMERALD_ORE", "DEEPSLATE_EMERALD_ORE" -> "3-7";
            case "NETHER_QUARTZ_ORE" -> "2-5";
            case "NETHER_GOLD_ORE" -> "0-1";
            default -> "0";
        };
    }

    public Material getReplacedBlock() {
        return replacedBlock;
    }

    public int getRegenDelay() {
        return regenDelay;
    }

    public List<ToolRequirement> getRequiredTools() {
        return requiredTools;
    }

    public boolean requiresTool() {
        return !requiredTools.isEmpty();
    }

    public boolean isAutoInventory() {
        return autoInventory;
    }

    public boolean isNaturalDrop() {
        return naturalDrop;
    }

    public String getBreakSound() {
        return breakSound;
    }

    public String getRegenSound() {
        return regenSound;
    }

    public List<String> getCommands() {
        return commands;
    }

    public boolean hasCustomDrops() {
        return !customDrops.isEmpty();
    }

    public Map<String, CustomDrop> getCustomDrops() {
        return customDrops;
    }

    public String getBreakParticle() {
        return breakParticle;
    }

    public String getRegenParticle() {
        return regenParticle;
    }

    public String getExpDropAmount() {
        return expDropAmount;
    }

    public boolean isAutoPickupExp() {
        return autoPickupExp;
    }

    public Map<String, Integer> getMmocoreExp() {
        return mmocoreExp;
    }

    public boolean hasMmocoreExp() {
        return this.mmocoreExp != null && !this.mmocoreExp.isEmpty();
    }

    public FortuneData getFortuneData() {
        return fortuneData;
    }

    // --- GETTER BARU ---
    public double getBreakDuration() {
        return breakDuration;
    }

    public boolean isFixedDuration() {
        return fixedDuration;
    }

    public boolean hasCustomBreakDuration() {
        return this.breakDuration > 0.0;
    }
    // --- AKHIR GETTER BARU ---
}