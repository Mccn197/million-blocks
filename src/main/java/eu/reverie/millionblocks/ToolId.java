package eu.reverie.millionblocks;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum ToolId {
    SHOP,
    SUPER_PICKAXE,
    MINING_HAMMER,
    CREEPER_MINER,
    TNT_LAUNCHER,
    DRILL,
    LASER,
    METEOR,
    WORLD_EATER;

    public static ToolId parse(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("PICKAXE")) normalized = "SUPER_PICKAXE";
        if (normalized.equals("HAMMER")) normalized = "MINING_HAMMER";
        if (normalized.equals("CREEPER")) normalized = "CREEPER_MINER";
        if (normalized.equals("TNT")) normalized = "TNT_LAUNCHER";
        if (normalized.equals("EATER")) normalized = "WORLD_EATER";
        return valueOf(normalized);
    }

    public static List<ToolId> playable() {
        return Arrays.stream(values()).filter(id -> id != SHOP).toList();
    }

    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
