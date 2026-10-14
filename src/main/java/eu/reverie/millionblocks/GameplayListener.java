package eu.reverie.millionblocks;

import org.bukkit.entity.Creeper;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.Event.Result;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;

public final class GameplayListener implements Listener {
    private final MillionBlocksPlugin plugin;

    public GameplayListener(MillionBlocksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("portal.auto-give-item", true)) plugin.getItemFactory().ensureTravelPortal(player);
        plugin.getHudService().show(player);
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
            () -> {if(player.isOnline()){plugin.getShowcaseService().repairNear(player);
                plugin.getPortalService().repairNear(player);}},10L);
    }

    /** Repair after teleport and chunk load; no manual reload needed on hub entry. */
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player=event.getPlayer();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
           () -> { if(player.isOnline()){plugin.getShowcaseService().repairNear(player);
                plugin.getPortalService().repairNear(player);} },10L);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(event.getTo()==null) return;
        Player player=event.getPlayer();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
            () -> { if(player.isOnline()){plugin.getShowcaseService().repairNear(player);
                plugin.getPortalService().repairNear(player);} },12L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        org.bukkit.Chunk chunk=event.getChunk();
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
            () -> {if(chunk.isLoaded()){plugin.getShowcaseService().repairChunk(chunk);
                plugin.getPortalService().repairChunk(chunk);}},5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDestructionService().stopLaser(event.getPlayer());
        plugin.getHudService().remove(event.getPlayer());
        plugin.getCinematicService().removeMachine(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getWorldService().isProtectedHub(player.getWorld())) {
            if (!plugin.getWorldService().canEdit(player)) event.setCancelled(true);
            return; // editing a hub never increments progress or triggers tool powers
        }
        ToolId tool = plugin.getItemFactory().identify(player.getInventory().getItemInMainHand());
        if (tool == ToolId.SUPER_PICKAXE) {
            event.setCancelled(true);
            plugin.getDestructionService().breakCube(player, event.getBlock(), 1, 1, 0, 9);
            return;
        }
        if (tool == ToolId.MINING_HAMMER) {
            event.setCancelled(true);
            plugin.getDestructionService().breakCube(player, event.getBlock(), 2, 2, 0, 25);
            return;
        }
        if (plugin.getConfig().getBoolean("challenge.count-natural-breaks", true)) plugin.addProgress(player, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (plugin.getItemFactory().isTravelPortal(event.getItem())) {
            event.setCancelled(true);
            if(event.getClickedBlock()!=null) plugin.getPortalService().place(player,event.getClickedBlock());
            else player.sendMessage("§eClique avec le portail sur un bloc de sol pour le poser.");
            return;
        }
        ToolId id = plugin.getItemFactory().identify(event.getItem());
        if (id == null) return;
        // Never fire destructive tools inside the hub, even with construction bypass enabled.
        if (plugin.getWorldService().isHub(player.getWorld()) && id != ToolId.SHOP) {
            event.setCancelled(true);
            player.sendMessage("§cLes armes sont désactivées dans le Mining Spawn.");
            return;
        }
        if (id == ToolId.LASER) {
            // Cancelling the use packet prevents a continuous-use item from entering its held state.
            event.setUseInteractedBlock(Result.DENY);
            event.setUseItemInHand(Result.ALLOW);
            plugin.getDestructionService().startLaser(player);
            return;
        }
        event.setCancelled(true);
        if (plugin.getWorldService().isProtectedHub(player.getWorld()) && id != ToolId.SHOP) {
            player.sendMessage("§cLes armes sont désactivées dans le Mining Spawn.");
            return;
        }
        switch (id) {
            case SHOP -> player.sendMessage("§7Boutique désormais physique : /mb world mining.");
            case CREEPER_MINER -> plugin.getDestructionService().deployCreeper(player);
            case TNT_LAUNCHER -> plugin.getDestructionService().launchTnt(player);
            case DRILL -> plugin.getDestructionService().drill(player);
            case LASER -> plugin.getDestructionService().startLaser(player);
            case METEOR -> plugin.getDestructionService().meteor(player);
            case WORLD_EATER -> plugin.getDestructionService().worldEater(player);
            default -> { }
        }
    }

    @EventHandler
    public void onStopUsing(PlayerStopUsingItemEvent event) {
        if (plugin.getItemFactory().identify(event.getItem()) == ToolId.LASER) {
            plugin.getDestructionService().stopLaser(event.getPlayer());
        }
    }

    @EventHandler public void onHeldChange(PlayerItemHeldEvent event) {
        plugin.getDestructionService().stopLaser(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onShowcaseInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (plugin.getPortalService().interact(event) || plugin.getShowcaseService().claim(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (plugin.getWorldService().isProtectedHub(event.getLocation().getWorld())) {
            event.blockList().clear();
            return;
        }
        if (event.getEntity() instanceof Creeper && !event.blockList().isEmpty()) {
            plugin.getDebrisService().burst(event.getLocation(), new ArrayList<>(event.blockList()));
        }
        if (!plugin.getConfig().getBoolean("challenge.count-explosion-breaks", true)) return;
        if (event.getEntity() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            plugin.addProgress(player, event.blockList().size());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (plugin.getItemFactory().isTravelPortal(stack)) {
            event.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.getWorldService().isProtectedHub(event.getPlayer().getWorld()) && !plugin.getWorldService().canEdit(event.getPlayer())) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent event) {
        if (plugin.getWorldService().isProtectedHub(event.getPlayer().getWorld()) && !plugin.getWorldService().canEdit(event.getPlayer()))event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onFill(PlayerBucketFillEvent event) {
        if (plugin.getWorldService().isProtectedHub(event.getPlayer().getWorld()) && !plugin.getWorldService().canEdit(event.getPlayer()))event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (plugin.getWorldService().isProtectedHub(event.getBlock().getWorld()))event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if(plugin.getWorldService().isProtectedHub(event.getBlock().getWorld()))event.blockList().clear();
    }
    @EventHandler(ignoreCancelled = true)
    public void onPortalHit(EntityDamageByEntityEvent event) {plugin.getPortalService().damage(event);}
}
