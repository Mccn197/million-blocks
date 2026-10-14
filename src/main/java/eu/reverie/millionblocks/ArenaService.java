package eu.reverie.millionblocks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

public final class ArenaService {
    private final MillionBlocksPlugin plugin;

    public ArenaService(MillionBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public int reset() {
        if (!plugin.getConfig().getBoolean("arena.enabled", false)) return -1;
        World world = Bukkit.getWorld(plugin.getConfig().getString("arena.world", "world"));
        if (world == null) return -2;
        int minX = Math.min(plugin.getConfig().getInt("arena.min.x"), plugin.getConfig().getInt("arena.max.x"));
        int minY = Math.min(plugin.getConfig().getInt("arena.min.y"), plugin.getConfig().getInt("arena.max.y"));
        int minZ = Math.min(plugin.getConfig().getInt("arena.min.z"), plugin.getConfig().getInt("arena.max.z"));
        int maxX = Math.max(plugin.getConfig().getInt("arena.min.x"), plugin.getConfig().getInt("arena.max.x"));
        int maxY = Math.max(plugin.getConfig().getInt("arena.min.y"), plugin.getConfig().getInt("arena.max.y"));
        int maxZ = Math.max(plugin.getConfig().getInt("arena.min.z"), plugin.getConfig().getInt("arena.max.z"));
        long volume = (long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);
        int cap = plugin.getConfig().getInt("arena.max-reset-blocks", 250000);
        if (volume > cap) return -3;
        Material material = Material.matchMaterial(plugin.getConfig().getString("arena.reset-material", "STONE"));
        if (material == null) material = Material.STONE;
        int count=0;
        for (int x=minX; x<=maxX; x++) for (int y=minY; y<=maxY; y++) for (int z=minZ; z<=maxZ; z++) {
            world.getBlockAt(x,y,z).setType(material, false);
            count++;
        }
        return count;
    }
}
