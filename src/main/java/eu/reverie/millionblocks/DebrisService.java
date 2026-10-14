package eu.reverie.millionblocks;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class DebrisService {
    private final MillionBlocksPlugin plugin;

    public DebrisService(MillionBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    public void burst(Location origin, List<Block> blocks) {
        if (!plugin.getConfig().getBoolean("cinematics.debris.enabled", true)) return;
        int max = plugin.getConfig().getInt("cinematics.debris.max-per-explosion", 24);
        int lifetime = plugin.getConfig().getInt("cinematics.debris.lifetime-ticks", 30);
        double horizontal = plugin.getConfig().getDouble("cinematics.debris.velocity-horizontal", 0.55);
        double vertical = plugin.getConfig().getDouble("cinematics.debris.velocity-vertical", 0.65);

        Collections.shuffle(blocks);
        for (int i = 0; i < Math.min(max, blocks.size()); i++) {
            Block block = blocks.get(i);
            if (block.getType().isAir()) continue;
            FallingBlock debris = block.getWorld().spawnFallingBlock(block.getLocation().add(0.5, 0.5, 0.5), block.getBlockData());
            debris.setDropItem(false);
            debris.setCancelDrop(true);
            debris.setHurtEntities(false);
            Vector out = block.getLocation().toVector().subtract(origin.toVector());
            if (out.lengthSquared() < 0.001) out = new Vector(ThreadLocalRandom.current().nextDouble(-1,1), 0, ThreadLocalRandom.current().nextDouble(-1,1));
            out.normalize().multiply(horizontal * ThreadLocalRandom.current().nextDouble(0.55, 1.25));
            out.setY(vertical * ThreadLocalRandom.current().nextDouble(0.65, 1.35));
            debris.setVelocity(out);
            plugin.getServer().getScheduler().runTaskLater(plugin, debris::remove, lifetime);
        }
    }
}
