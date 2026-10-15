package eu.reverie.millionblocks;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Accurate long-range targeting with bounded asynchronous chunk preloading.
 * No block mutation and no sync chunk generation from the player's click.
 * Aiming at the sky chooses terrain at the far end of the ray instead of thin air.
 */
public final class RemoteAimService {
    private final MillionBlocksPlugin plugin;
    public RemoteAimService(MillionBlocksPlugin plugin) { this.plugin = plugin; }

    public record Aim(Location location, boolean blockHit, int distance, int chunks) {}

    public void resolve(Player player, int requestedRange, Consumer<Aim> success) {
        final World world = player.getWorld();
        final Location eye = player.getEyeLocation().clone();
        final Vector direction = eye.getDirection().normalize();
        final int range = Math.max(8, Math.min(256, requestedRange));
        final LinkedHashSet<Long> route = new LinkedHashSet<>();
        // Intersected chunks at 4-block steps, bounded to a reasonable amount.
        for (int d = 0; d <= range; d += 4) {
            Location at = eye.clone().add(direction.clone().multiply(d));
            int cx = at.getBlockX() >> 4, cz = at.getBlockZ() >> 4;
            route.add(((long)cx << 32) | (cz & 0xffffffffL));
        }
        int cap = Math.max(8, Math.min(32, plugin.getConfig().getInt("weapons.raycast.max-chunks", 24)));
        if (route.size() > cap) {
            player.sendMessage("§ePortée limitée : trop de chunks sur la trajectoire.");
            return;
        }
        List<CompletableFuture<org.bukkit.Chunk>> futures = new ArrayList<>();
        for (long key : route) {
            int cx = (int)(key >> 32), cz = (int)key;
            futures.add(world.getChunkAtAsync(cx, cz, true));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((ignored, failure) -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (failure != null || !player.isOnline() || player.getWorld() != world) {
                    if (player.isOnline()) player.sendMessage("§cImpossible de charger la trajectoire de tir.");
                    return;
                }
                RayTraceResult ray = world.rayTraceBlocks(eye, direction, range, FluidCollisionMode.NEVER, true);
                Location target;
                boolean hit = ray != null && ray.getHitBlock() != null;
                if (hit) target = ray.getHitPosition().toLocation(world);
                else {
                    Location end = eye.clone().add(direction.clone().multiply(range));
                    int surface = world.getHighestBlockYAt(end.getBlockX(), end.getBlockZ());
                    double y = Math.min(end.getY(), surface + 1.0);
                    if (y < world.getMinHeight()+1) y = surface+1.0;
                    target = new Location(world, end.getX(), Math.min(world.getMaxHeight()-2, y), end.getZ());
                }
                success.accept(new Aim(target, hit, range, route.size()));
            });
        });
    }
}
