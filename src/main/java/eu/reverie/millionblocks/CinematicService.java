package eu.reverie.millionblocks;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.util.*;

/** Native Display Entities: no server terrain edits and no external model runtime. */
public final class CinematicService {
    private final MillionBlocksPlugin plugin;
    private final Set<ItemDisplay> props = new HashSet<>();
    private final Map<UUID,Machine> machines = new HashMap<>();
    private record Machine(ArmorStand seat, ItemDisplay body, ItemDisplay turret, Location emitter) { }
    public CinematicService(MillionBlocksPlugin plugin) { this.plugin=plugin; }

    public void animate(Player player, ToolId tool, int ticks) {
        if(props.size()>=16)return; // Keep cosmetic spam bounded independently of mining.
        Location origin=player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.25));
        ItemDisplay display=player.getWorld().spawn(origin,ItemDisplay.class,e->{
            var stack=plugin.getItemFactory().create(tool);
            if(tool==ToolId.DRILL){var meta=stack.getItemMeta();meta.setItemModel(new NamespacedKey("millionblocks","bore_x_rotor"));stack.setItemMeta(meta);}
            e.setItemStack(stack);
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            e.setPersistent(false); e.setTeleportDuration(1); e.setInterpolationDuration(1);
        });
        props.add(display);
        new BukkitRunnable(){
            int age=0;
            public void run(){
                if(++age>ticks || !display.isValid() || !player.isOnline() || player.getWorld()!=origin.getWorld()){
                    display.remove();props.remove(display);cancel();return;
                }
                double phase=(double)age/ticks;
                Vector forward=player.getEyeLocation().getDirection();
                Location at=tool==ToolId.WORLD_EATER?origin.clone().add(0,Math.sin(phase*Math.PI),0)
                    :player.getEyeLocation().add(forward.clone().multiply(1.25));
                if(tool==ToolId.TNT_LAUNCHER)at.subtract(forward.clone().multiply(.4*Math.sin(phase*Math.PI)));
                display.teleport(at);
                float rotation=tool==ToolId.DRILL?age*.65f:tool==ToolId.WORLD_EATER?age*.12f:0;
                float scale=tool==ToolId.WORLD_EATER?(float)(.6+.5*phase):.7f;
                display.setTransformation(new Transformation(new Vector3f(),new AxisAngle4f(rotation,0,0,1),new Vector3f(scale),new AxisAngle4f()));
            }
        }.runTaskTimer(plugin,1,1);
    }
    public void placeMachine(Player player) {
        if(plugin.getWorldService().isHub(player.getWorld()))throw new IllegalArgumentException("Machine désactivée dans le hub");
        org.bukkit.block.Block floor=player.getTargetBlockExact(6);
        if(floor==null || !floor.getType().isSolid())throw new IllegalArgumentException("Vise un sol solide à moins de 6 blocs");
        Location base=floor.getLocation().add(.5,1,.5);
        for(int y=0;y<3;y++)if(!base.clone().add(0,y,0).getBlock().getType().isAir())
            throw new IllegalArgumentException("Trois blocs libres nécessaires");
        removeMachine(player);
        base.setYaw(player.getLocation().getYaw());
        ArmorStand seat=base.getWorld().spawn(base,ArmorStand.class,e->{
            e.setVisible(false);e.setGravity(false);e.setInvulnerable(true);e.setSilent(true);
            e.setSmall(true);e.setCollidable(false);e.setPersistent(false);
        });
        ItemDisplay body=base.getWorld().spawn(base.clone().add(0,.8,0),ItemDisplay.class,e->{
            var stack=plugin.getItemFactory().create(ToolId.LASER);
            var meta=stack.getItemMeta();meta.setItemModel(new NamespacedKey("millionblocks","redline_chassis"));stack.setItemMeta(meta);
            e.setItemStack(stack);e.setPersistent(false);
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            e.setTransformation(new Transformation(new Vector3f(),new AxisAngle4f(),new Vector3f(2.3f),new AxisAngle4f()));
            e.setTeleportDuration(2);
        });
        ItemDisplay turret=base.getWorld().spawn(base.clone().add(0,.8,0),ItemDisplay.class,e->{
            e.setItemStack(plugin.getItemFactory().create(ToolId.LASER));e.setPersistent(false);
            e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            e.setTransformation(new Transformation(new Vector3f(),new AxisAngle4f(),new Vector3f(2.3f),new AxisAngle4f()));
            e.setTeleportDuration(2);
        });
        body.setRotation(base.getYaw(),0);turret.setRotation(base.getYaw(),0);
        Location emitter=base.clone().add(0,.9,0);
        machines.put(player.getUniqueId(),new Machine(seat,body,turret,emitter));
        player.sendMessage("§aREDLINE placée. /mb laser mount puis /mb laser fire on. Accroupis-toi pour quitter.");
    }
    public boolean isPilot(Player player){
        Machine m=machines.get(player.getUniqueId());
        return m!=null && m.seat.isValid() && m.body.isValid() && m.turret.isValid() && player.getVehicle()==m.seat;
    }
    public void mount(Player player){
        Machine m=machines.get(player.getUniqueId());
        if(m==null || !m.seat.isValid() || m.seat.getWorld()!=player.getWorld() || m.seat.getLocation().distanceSquared(player.getLocation())>64)
            throw new IllegalArgumentException("Place une machine ou rapproche-toi (8 blocs)");
        if(!m.seat.addPassenger(player))throw new IllegalArgumentException("Impossible de monter; quitte ton véhicule actuel");
    }
    public Location emitter(Player player){
        Machine m=machines.get(player.getUniqueId());
        if(!isPilot(player))throw new IllegalArgumentException("Monte dans ta machine d'abord");
        m.turret.setRotation(player.getLocation().getYaw(),player.getLocation().getPitch());
        return m.emitter.clone().add(player.getEyeLocation().getDirection().multiply(.95)).setDirection(player.getEyeLocation().getDirection());
    }
    public void removeMachine(Player player){
        plugin.getDestructionService().stopLaser(player);
        Machine m=machines.remove(player.getUniqueId());
        if(m!=null){m.seat.eject();m.seat.remove();m.body.remove();m.turret.remove();}
    }
    public void shutdown(){
        for(Machine m:machines.values()){m.seat.eject();m.seat.remove();m.body.remove();m.turret.remove();}
        machines.clear(); for(ItemDisplay p:props)p.remove(); props.clear();
    }
}
