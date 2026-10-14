package eu.reverie.millionblocks;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Placeable reusable portal; vanilla Display fallback without Oraxen/mandatory resource pack. */
public final class PortalService {
    private final MillionBlocksPlugin plugin;
    private final NamespacedKey portalId;
    private final File file;
    private final Map<String,Location> locations=new LinkedHashMap<>();
    private final Map<String,List<Entity>> spawned=new HashMap<>();
    private final Set<String> rising=new HashSet<>();
    private final Map<String,org.bukkit.scheduler.BukkitTask> animations=new HashMap<>();
    public PortalService(MillionBlocksPlugin plugin){this.plugin=plugin;this.portalId=new NamespacedKey(plugin,"portal-entity");this.file=new File(plugin.getDataFolder(),"portals.yml");}
    public void load(){ clearEntities();locations.clear();
        YamlConfiguration yaml=YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root=yaml.getConfigurationSection("portals");
        if(root!=null)for(String id:root.getKeys(false)){
            String p="portals."+id+".";World w=Bukkit.getWorld(yaml.getString(p+"world", ""));if(w==null)continue;
            locations.put(id,new Location(w,yaml.getDouble(p+"x"),yaml.getDouble(p+"y"),yaml.getDouble(p+"z"),
                    (float)yaml.getDouble(p+"yaw"),0));
        }
        for(var e:locations.entrySet()) spawn(e.getKey(),e.getValue(),false);
    }
    public void shutdown(){save();clearEntities();}
    public int repairNear(Player player){
        int fixed=0;
        for(var entry:locations.entrySet()){
            if(entry.getValue().getWorld()!=player.getWorld())continue;
            if(entry.getValue().distanceSquared(player.getLocation())>112*112)continue;
            if(repair(entry.getKey(),entry.getValue()))fixed++;
        }
        return fixed;
    }
    public int repairChunk(Chunk chunk){
        int fixed=0;
        for(var entry:locations.entrySet()){
            Location loc=entry.getValue();
            if(loc.getWorld()==chunk.getWorld() && (loc.getBlockX()>>4)==chunk.getX() &&
               (loc.getBlockZ()>>4)==chunk.getZ())
                if(repair(entry.getKey(),loc))fixed++;
        }
        return fixed;
    }
    private boolean repair(String id,Location loc){
        List<Entity> entities=spawned.get(id);
        if(entities!=null && entities.size()>=(plugin.getConfig().getBoolean("portal.use-custom-model",true)?3:8) && entities.stream().allMatch(Entity::isValid))return false;
        spawn(id,loc,false);
        return true;
    }
    public void save(){YamlConfiguration y=new YamlConfiguration();for(var e:locations.entrySet()){
        Location l=e.getValue();String p="portals."+e.getKey()+".";y.set(p+"world",l.getWorld().getName());
        y.set(p+"x",l.getX());y.set(p+"y",l.getY());y.set(p+"z",l.getZ());y.set(p+"yaw",l.getYaw());
    }try{y.save(file);}catch(IOException ex){plugin.getLogger().severe("Portal save error: "+ex);}}
    public int count(){return locations.size();}
    public List<String> list(){List<String> list=new ArrayList<>();locations.forEach((id,l)->list.add(id+" @ "+l.getWorld().getName()+" "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ()));return list;}
    public void place(Player player,Block clicked){place(player,clicked,false);}
    public void placeExit(Player player,Block clicked){place(player,clicked,true);}
    private void place(Player player,Block clicked,boolean allowHub){
        if(clicked==null || !clicked.getType().isSolid()){player.sendMessage("§cClique sur un sol solide.");return;}
        if(plugin.getWorldService().isHub(player.getWorld()) && !allowHub) {player.sendMessage("§cLes portails placés à la main sont pour la Survival. Utilise /mb portal exit create pour le retour.");return;}
        int limit=plugin.getConfig().getInt("portal.max-active",-1);
        if(limit>0&&locations.size()>=limit){player.sendMessage("§cLimite de portails atteinte ("+limit+"). /mb portal remove nearest");return;}
        Location base=clicked.getLocation().add(.5,1,.5);
        if(!base.getBlock().getType().isAir() || !base.clone().add(0,1,0).getBlock().getType().isAir()) {
            player.sendMessage("§cLe portail a besoin de deux blocs libres au-dessus du sol.");return;}
        base.setYaw(Math.round(player.getLocation().getYaw()/90f)*90f);
        String id=UUID.randomUUID().toString().substring(0,8);
        locations.put(id,base);spawn(id,base,true);save();
        player.sendMessage("§aPortail "+id+" placé. §7Clic droit: TP, clic gauche: retirer.");
    }
    public boolean interact(PlayerInteractAtEntityEvent e){
        if(e.getHand()!=EquipmentSlot.HAND)return false;
        String id=e.getRightClicked().getPersistentDataContainer().get(portalId,PersistentDataType.STRING);
        if(id==null)return false;
        e.setCancelled(true);
        if(!rising.contains(id)) { if(plugin.getWorldService().isHub(e.getPlayer().getWorld()))plugin.getWorldService().teleportSurvival(e.getPlayer());else plugin.getWorldService().teleportHub(e.getPlayer()); }
        return true;
    }
    public boolean damage(EntityDamageByEntityEvent e){
        String id=e.getEntity().getPersistentDataContainer().get(portalId,PersistentDataType.STRING);
        if(id==null)return false;
        e.setCancelled(true);
        if(e.getDamager() instanceof Player p){remove(id);p.sendMessage("§aPortail retiré.");}
        return true;
    }
    public String removeNearest(Player p,double radius){
        String best=null;double distance=radius*radius;
        for(var e:locations.entrySet()){
            if(e.getValue().getWorld()!=p.getWorld())continue;
            double d=e.getValue().distanceSquared(p.getLocation());
            if(d<distance){distance=d;best=e.getKey();}
        }if(best!=null)remove(best);return best;
    }
    public boolean remove(String id){Location loc=locations.remove(id);if(loc==null)return false;
        removeEntities(id);rising.remove(id);save();return true;
    }
    public int clear(){int n=locations.size();for(String id:new ArrayList<>(locations.keySet()))remove(id);return n;}
    private void clearEntities(){for(String id:new ArrayList<>(spawned.keySet()))removeEntities(id);}
    private void removeEntities(String id){
        var animation=animations.remove(id);if(animation!=null)animation.cancel();rising.remove(id);
        List<Entity> list=spawned.remove(id);if(list!=null)for(Entity entity:list)if(entity.isValid())entity.remove();}
    private void spawn(String id,Location base,boolean animate){
        removeEntities(id);
        World world=base.getWorld();if(world==null)return;
        Chunk chunk=base.getChunk();
        chunk.load();
        for(Entity old:chunk.getEntities()){
            if(id.equals(old.getPersistentDataContainer().get(portalId,PersistentDataType.STRING)))old.remove();
        }
        List<Entity> list=new ArrayList<>();spawned.put(id,list);
        // The furniture is a 3D arrangement of client-side block displays; the terrain is not modified.
        if(plugin.getConfig().getBoolean("portal.use-custom-model",true)) {
            ItemDisplay model=world.spawn(base.clone().add(0,1.4,0),ItemDisplay.class,ent->{
                ent.setItemStack(plugin.getItemFactory().createTravelPortal());
                ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                ent.setTransformation(new Transformation(new Vector3f(),new AxisAngle4f(),new Vector3f(3f),new AxisAngle4f()));
                ent.setTeleportDuration(1);ent.setRotation(base.getYaw(),0);
                ent.getPersistentDataContainer().set(portalId,PersistentDataType.STRING,id);
            });list.add(model);
        } else {
        piece(list,id,base,Material.IRON_BLOCK,-1.5,0,0,.22f,2.4f,.27f);
        piece(list,id,base,Material.IRON_BLOCK,1.27,0,0,.22f,2.4f,.27f);
        piece(list,id,base,Material.BLACK_CONCRETE,-1.5,2.3,0,3f,.32f,.3f);
        piece(list,id,base,Material.LIME_STAINED_GLASS,-1.20,.15,.07,2.48f,2.06f,.13f);
        piece(list,id,base,Material.LIME_STAINED_GLASS,-1.17,.20,.15,2.42f,1.95f,.045f);
        piece(list,id,base,Material.CRYING_OBSIDIAN,-1.25,.09,-.04,2.6f,.16f,.21f);
        }
        TextDisplay label=world.spawn(base.clone().add(0,2.78,.06),TextDisplay.class,ent->{
            ent.setText("§c§lKEEP OUT §7• §c§lDANGER");ent.setBillboard(Display.Billboard.CENTER);
            ent.setSeeThrough(false);ent.setShadowed(true);ent.getPersistentDataContainer().set(portalId,PersistentDataType.STRING,id);
        });list.add(label);
        Interaction hit=world.spawn(base.clone().add(0,1.25,0),Interaction.class,ent->{
            ent.setInteractionWidth(3f);ent.setInteractionHeight(2.7f);
            ent.setResponsive(true);ent.getPersistentDataContainer().set(portalId,PersistentDataType.STRING,id);
        });list.add(hit);
        if(animate){
            rising.add(id);
            final int[] tick={0};
            // Move only the rendered furniture, never the interaction anchor (available after rising).
            for(int i=0;i<list.size()-1;i++)list.get(i).teleport(list.get(i).getLocation().add(0,-1.8,0));
            final org.bukkit.scheduler.BukkitTask[] task={null};
            task[0]=Bukkit.getScheduler().runTaskTimer(plugin,()->{
                if(!locations.containsKey(id)) {task[0].cancel();return;}
                int t=++tick[0];double step=1.8/18.0;
                for(int i=0;i<list.size()-1;i++){
                    Entity part=list.get(i);if(part.isValid())part.teleport(part.getLocation().add(0,step,0));
                }
                if(t%3==0)world.spawnParticle(Particle.HAPPY_VILLAGER,base.clone().add(0,.3,0),8,1.25,.25,.18,.01);
                if(t>=18){rising.remove(id);animations.remove(id);task[0].cancel();}
            },1,1);
            animations.put(id,task[0]);
        }
    }
    private void piece(List<Entity> all,String id,Location base,Material type,double x,double y,double z,float sx,float sy,float sz){
        double yaw=Math.toRadians(base.getYaw());
        // Local X (portal width) rotates around the placed facing.
        Location where=base.clone().add(x*Math.cos(yaw)-z*Math.sin(yaw),y,x*Math.sin(yaw)+z*Math.cos(yaw));
        // Temporary vanilla display model; the future Blockbench/Oraxen model will replace it.
        BlockDisplay display=where.getWorld().spawn(where,BlockDisplay.class,ent->{
            ent.setBlock(type.createBlockData());ent.setRotation(base.getYaw(),0);ent.setTransformation(new Transformation(new Vector3f(0,0,0),new AxisAngle4f(),new Vector3f(sx,sy,sz),new AxisAngle4f()));
            ent.setViewRange(4);ent.setTeleportDuration(1);ent.getPersistentDataContainer().set(portalId,PersistentDataType.STRING,id);
        });all.add(display);
    }
}
