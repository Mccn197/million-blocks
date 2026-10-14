package eu.reverie.millionblocks;

import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class DestructionService {
    private final MillionBlocksPlugin plugin;
    private final Set<Material> protectedTypes = Set.of(Material.BEDROCK, Material.BARRIER,
        Material.END_PORTAL_FRAME, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
        Material.REPEATING_COMMAND_BLOCK);
    private final Map<UUID, BukkitTask> activeLasers = new HashMap<>();
    private final Map<UUID, Long> lastShots = new HashMap<>();
    private int activeMassJobs = 0;

    public DestructionService(MillionBlocksPlugin plugin) { this.plugin = plugin; }

    public int breakCube(Player player, Block center, int rx, int ry, int rz, int cap) {
        if (blocked(player.getWorld())) return 0;
        List<Block> targets = new ArrayList<>();
        World world = center.getWorld();
        for (int x=-rx; x<=rx; x++) for (int y=-ry; y<=ry; y++) for (int z=-rz; z<=rz; z++) {
            if (targets.size() >= cap) break;
            Block b = world.getBlockAt(center.getX()+x, center.getY()+y, center.getZ()+z);
            if (isBreakable(b)) targets.add(b);
        }
        return destroy(player, targets);
    }

    private boolean blocked(World world) {
        // No explosive/mining abilities inside any concept hub, even in edit mode.
        return plugin.getWorldService().isHub(world)
             || plugin.getWorldService().isProtectedHub(world)
             || world.getName().equalsIgnoreCase("mining_spawn")
             || world.getName().equalsIgnoreCase("mining_sky");
    }

    public boolean isBusy(Player p) { return activeLasers.containsKey(p.getUniqueId()); }

    public void laser(Player player) { startLaser(player); }

    public void startLaser(Player player) {
        if (blocked(player.getWorld()) || activeLasers.containsKey(player.getUniqueId())) return;
        UUID key = player.getUniqueId();
        World initial = player.getWorld();
        int interval = Math.max(1,plugin.getConfig().getInt("weapons.laser.tick-interval",2));
        int range = Math.max(16,Math.min(240,plugin.getConfig().getInt("weapons.laser.range",145)));
        int width = Math.max(1,Math.min(7,plugin.getConfig().getInt("weapons.laser.width",5)));
        int radius = width/2;
        int maxBlocksPerPulse = Math.max(64,Math.min(1200,plugin.getConfig().getInt("weapons.laser.max-blocks-per-pulse",350)));
        BukkitTask task = new BukkitRunnable() {
            int age=0;
            @Override public void run() {
                age+=interval;
                if (!player.isOnline() || player.getWorld()!=initial
                    || plugin.getItemFactory().identify(player.getInventory().getItemInMainHand())!=ToolId.LASER
                    || (age>5 && !player.isHandRaised()) || blocked(player.getWorld())) {
                    stopLaser(player);return;
                }
                Location eye=player.getEyeLocation();
                Vector dir=eye.getDirection().normalize();
                Vector side=dir.clone().crossProduct(new Vector(0,1,0));
                if(side.lengthSquared()<0.01) side=new Vector(1,0,0);
                side.normalize();
                Vector up=side.clone().crossProduct(dir).normalize();
                int destroyed=0;
                HashSet<Long> seen = new HashSet<>();
                for(int dist=2;dist<=range;dist+=2){
                    Location point=eye.clone().add(dir.clone().multiply(dist));
                    // A bright, always visible continuous beam (even when the ray hits no blocks).
                    initial.spawnParticle(Particle.END_ROD,point,2,0.10,0.10,0.10,0.0);
                    initial.spawnParticle(Particle.ELECTRIC_SPARK,point,1,0.07,0.07,0.07,0.02);
                    for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++) {
                        if(destroyed>=maxBlocksPerPulse)break;
                        Block block=point.clone().add(side.clone().multiply(a)).add(up.clone().multiply(b)).getBlock();
                        long hash=block.getBlockKey();
                        if(!seen.add(hash) || !isBreakable(block)) continue;
                        block.setType(Material.AIR,false);
                        destroyed++;
                    }
                    if(destroyed>=maxBlocksPerPulse) break;
                }
                if(destroyed>0) plugin.addProgress(player,destroyed);
                if(age%10<interval) initial.playSound(eye,Sound.ENTITY_WARDEN_SONIC_BOOM,0.28f,1.85f);
            }
        }.runTaskTimer(plugin,0L,interval);
        activeLasers.put(key,task);
    }

    public void stopLaser(Player player) {
        BukkitTask task=activeLasers.remove(player.getUniqueId());
        if(task!=null)task.cancel();
    }
    public void stopAll() {
        for(BukkitTask task:activeLasers.values())task.cancel();
        activeLasers.clear();
        for(World world:Bukkit.getWorlds())world.removePluginChunkTickets(plugin);
    }

    public void launchTnt(Player player) {
        if(blocked(player.getWorld()))return;
        if(!cooldown(player,"tnt",450L))return;
        Location origin=player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(1.2));
        TNTPrimed tnt=player.getWorld().spawn(origin,TNTPrimed.class);
        tnt.setFuseTicks(Math.max(12,plugin.getConfig().getInt("weapons.tnt.fuse-ticks",55)));
        tnt.setVelocity(player.getEyeLocation().getDirection().normalize()
             .multiply(Math.max(1.0,Math.min(5.0,plugin.getConfig().getDouble("weapons.tnt.velocity",2.8)))));
        tnt.setSource(player);
        player.playSound(player.getLocation(),Sound.ENTITY_ARROW_SHOOT,1.0f,0.75f);
    }

    public void deployCreeper(Player player) {
        if(blocked(player.getWorld()))return;
        if(!cooldown(player,"creeper",1200L))return;
        Location target=aim(player,Math.min(100,plugin.getConfig().getInt("weapons.creeper.range",65)));
        // The mining blast is plugin-controlled and therefore works even with mobGriefing=false.
        if(target.getBlock().getType().isSolid())target.add(0,1,0);
        Creeper creeper=player.getWorld().spawn(target,Creeper.class);
        creeper.setPowered(true);
        creeper.setAI(false);
        creeper.setSilent(true);
        final Location blast=target.clone();
        player.getWorld().playSound(blast,Sound.ENTITY_CREEPER_PRIMED,1.3f,1.0f);
        new BukkitRunnable() {
            int ticks=0;
            @Override public void run() {
                ticks+=2;
                if(!creeper.isValid() || blocked(blast.getWorld())) {if(creeper.isValid())creeper.remove();cancel();return;}
                blast.getWorld().spawnParticle(Particle.SMOKE,blast.clone().add(0,1.3,0),8,.4,.6,.4,.015);
                if(ticks>=26){
                    creeper.remove();
                    blast.getWorld().playSound(blast,Sound.ENTITY_GENERIC_EXPLODE,1.5f,1.15f);
                    blast.getWorld().spawnParticle(Particle.EXPLOSION,blast,12,1.8,1.8,1.8,0);
                    scheduleSphere(player,blast,Math.max(3,plugin.getConfig().getInt("weapons.creeper.radius",5)),
                            1000,Math.max(500,plugin.getConfig().getInt("weapons.creeper.blocks-per-tick",800)), false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin,0L,2L);
    }

    public void drill(Player player) {
        if(blocked(player.getWorld()))return;
        Location origin=player.getEyeLocation();
        Vector direction=origin.getDirection().normalize();
        int range=Math.min(120,plugin.getConfig().getInt("weapons.drill.range",45));
        for(int step=2;step<=range;step+=3){
            final int at=step;
            plugin.getServer().getScheduler().runTaskLater(plugin,()-> {
                if(blocked(origin.getWorld()))return;
                Block center=origin.clone().add(direction.clone().multiply(at)).getBlock();
                breakCube(player,center,1,1,1,27);
                origin.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE,center.getLocation().add(.5,.5,.5),
                    10,.7,.7,.7,Material.STONE.createBlockData());
            },(long)(step/3)*2L);
        }
    }

    public void meteor(Player player) {
        if(blocked(player.getWorld()))return;
        if(!cooldown(player,"meteor",3000L))return;
        int range=Math.min(240,plugin.getConfig().getInt("weapons.meteor.range",130));
        final Location target=aim(player,range);
        int radius=Math.max(6,Math.min(30,plugin.getConfig().getInt("weapons.meteor.radius",14)));
        long delay=Math.max(10,plugin.getConfig().getInt("weapons.meteor.charge-ticks",35));
        player.sendMessage("§6☄ Météore verrouillé — impact imminent !");
        new BukkitRunnable() {
            long t=0;
            @Override public void run() {
                if(blocked(target.getWorld())) {cancel();return;}
                t+=2;
                Location p=target.clone().add(0,Math.max(0,28.0*(1.0-(double)t/delay)),0);
                target.getWorld().spawnParticle(Particle.FLAME,p,35,1.1,1.1,1.1,.04);
                target.getWorld().spawnParticle(Particle.END_ROD,p,7,.3,.3,.3,.01);
                if(t>=delay){
                    target.getWorld().playSound(target,Sound.ENTITY_GENERIC_EXPLODE,3.0f,0.6f);
                    target.getWorld().spawnParticle(Particle.EXPLOSION,target,25,5,3,5,0);
                    scheduleSphere(player,target,radius,Math.max(10000,plugin.getConfig().getInt("weapons.meteor.max-blocks",20000)),
                        Math.max(100,plugin.getConfig().getInt("weapons.mass-blocks-per-tick",1200)),true);
                    cancel();
                }
            }
        }.runTaskTimer(plugin,0L,2L);
    }

    public int worldEater(Player player) {
        if(blocked(player.getWorld()))return 0;
        if(!cooldown(player,"world_eater",6000L))return 0;
        if(activeMassJobs>=Math.max(1,plugin.getConfig().getInt("weapons.max-concurrent-mass-jobs",2))){
            player.sendMessage("§cDestruction massive déjà en cours, attends la fin.");return 0;
        }
        int range=Math.min(256,plugin.getConfig().getInt("weapons.world-eater.range",155));
        final Location target=aim(player,range);
        final int radius=Math.max(15,Math.min(45,plugin.getConfig().getInt("weapons.world-eater.radius",34)));
        final long delay=Math.max(15,plugin.getConfig().getInt("weapons.world-eater.charge-ticks",60));
        player.sendMessage("§5§lWORLD EATER §d— surcharge... éloigne-toi !");
        new BukkitRunnable(){
            int t=0;
            @Override public void run(){
                t+=2;
                if(blocked(target.getWorld())){cancel();return;}
                double r=2+(double)t/delay*radius;
                for(int k=0;k<32;k++){
                    double theta=2*Math.PI*k/32;
                    Location p=target.clone().add(r*Math.cos(theta),Math.sin(t/5.0)*2,r*Math.sin(theta));
                    target.getWorld().spawnParticle(Particle.PORTAL,p,3,.4,.4,.4,.04);
                    target.getWorld().spawnParticle(Particle.END_ROD,p,1,0,0,0,0);
                }
                if(t%12==0)target.getWorld().playSound(target,Sound.BLOCK_BEACON_ACTIVATE,1.4f,.55f+(float)t/delay);
                if(t>=delay){
                    target.getWorld().spawnParticle(Particle.EXPLOSION,target,45,9,9,9,.06);
                    target.getWorld().playSound(target,Sound.ENTITY_GENERIC_EXPLODE,4.0f,.55f);
                    scheduleSphere(player,target,radius,Math.max(150000,plugin.getConfig().getInt("weapons.world-eater.max-blocks",200000)),
                          Math.max(250,plugin.getConfig().getInt("weapons.mass-blocks-per-tick",1200)),true);
                    cancel();
                }
            }
        }.runTaskTimer(plugin,0L,2L);
        return 0;
    }

    private Location aim(Player player,int range){
        RayTraceResult hit=player.rayTraceBlocks(range);
        return hit!=null && hit.getHitPosition()!=null ? hit.getHitPosition().toLocation(player.getWorld())
            : player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(range));
    }

    private boolean cooldown(Player player,String name,long millis){
        String key=player.getUniqueId()+":"+name;
        long now=System.currentTimeMillis();
        // combine player UUID with tool category while still using a UUID-keyed map via named hash
        UUID hash=UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Long prev=lastShots.get(hash);
        if(prev!=null && now-prev<millis) {player.sendMessage("§7Recharge : "+((millis-(now-prev)+999)/1000)+"s");return false;}
        lastShots.put(hash,now);return true;
    }

    // Main-thread, budget-limited generator: never update 150k blocks in one tick.
    private void scheduleSphere(Player owner, Location origin, int radius,int maxBlocks,int blockBudget,boolean burst) {
        World world=origin.getWorld();
        if(blocked(world))return;
        activeMassJobs++;
        List<CompletableFuture<org.bukkit.Chunk>> chunks = new ArrayList<>();
        int cx=origin.getBlockX(), cz=origin.getBlockZ();
        for(int x=(cx-radius)>>4; x<=((cx+radius)>>4); x++) {
            for(int z=(cz-radius)>>4; z<=((cz+radius)>>4); z++) {
                chunks.add(world.getChunkAtAsync(x,z,true));
            }
        }
        // Preload the finite impact zone asynchronously before modifying blocks.
        CompletableFuture.allOf(chunks.toArray(new CompletableFuture[0]))
            .whenComplete((ok, err) -> {
                if (!plugin.isEnabled()) return;
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (err != null || blocked(world)) {
                        activeMassJobs=Math.max(0,activeMassJobs-1);
                        owner.sendMessage("§cImpact interrompu : chargement de chunks ou monde protégé.");
                        return;
                    }
                    List<org.bukkit.Chunk> ticketed = new ArrayList<>();
                    for (CompletableFuture<org.bukkit.Chunk> future:chunks) {
                        org.bukkit.Chunk chunk=future.join();
                        world.addPluginChunkTicket(chunk.getX(),chunk.getZ(),plugin);
                        ticketed.add(chunk);
                    }
                    runSphereJob(owner,origin,radius,maxBlocks,blockBudget,burst,ticketed);
                });
            });
    }

    private void runSphereJob(Player owner, Location origin, int radius,int maxBlocks,int blockBudget,boolean burst,List<org.bukkit.Chunk> ticketed) {
        World world=origin.getWorld();
        final int diameter=2*radius+1;
        final long total=(long)diameter*diameter*diameter;
        final int cx=origin.getBlockX(),cy=origin.getBlockY(),cz=origin.getBlockZ();
        final long r2=(long)radius*radius;
        new BukkitRunnable(){
            long index=0;
            int removed=0;
            int elapsed=0;
            @Override public void run(){
                if(blocked(world) || ++elapsed>1800){finish();return;}
                int budget=0,scanned=0;
                while(index<total && removed<maxBlocks && budget<blockBudget && scanned<blockBudget*16){
                    int x=(int)(index%diameter)-radius;
                    int y=(int)((index/diameter)%diameter)-radius;
                    int z=(int)(index/(diameter*(long)diameter))-radius;
                    index++;scanned++;
                    if((long)x*x+(long)y*y+(long)z*z>r2)continue;
                    int yy=cy+y;
                    if(yy<world.getMinHeight()||yy>=world.getMaxHeight())continue;
                    int xx=cx+x,zz=cz+z;
                    // Skip far-away unloaded chunks: never generate huge regions during an explosion.
                    if(!world.isChunkLoaded(xx>>4,zz>>4))continue;
                    Block block=world.getBlockAt(xx,yy,zz);
                    if(!isBreakable(block))continue;
                    block.setType(Material.AIR,false);
                    removed++;budget++;
                }
                if(burst && elapsed%3==0)world.spawnParticle(Particle.END_ROD,origin,40,radius*.7,radius*.7,radius*.7,.03);
                if(index>=total || removed>=maxBlocks)finish();
            }
            private void finish() {
                activeMassJobs=Math.max(0,activeMassJobs-1);
                for(org.bukkit.Chunk chunk:ticketed)
                    world.removePluginChunkTicket(chunk.getX(),chunk.getZ(),plugin);
                if(owner.isOnline()){
                    if(removed>0)plugin.addProgress(owner,removed);
                    owner.sendMessage("§6Destruction massive terminée : §e"+ItemFactory.formatNumber(removed)+" §6blocs");
                }
                cancel();
            }
        }.runTaskTimer(plugin,1L,1L);
    }

    public int breakSphere(Player player, Location center,int radius,int cap){
        if(blocked(center.getWorld()))return 0;
        scheduleSphere(player,center,radius,cap,Math.max(300,plugin.getConfig().getInt("weapons.mass-blocks-per-tick",1200)),false);
        return 0;
    }
    private int destroy(Player player,List<Block> blocks){
        int count=0;
        for(Block block:blocks){if(isBreakable(block)){block.setType(Material.AIR,false);count++;}}
        if(count>0)plugin.addProgress(player,count);
        return count;
    }
    private boolean isBreakable(Block b){return !b.getType().isAir()&&!protectedTypes.contains(b.getType());}
}
