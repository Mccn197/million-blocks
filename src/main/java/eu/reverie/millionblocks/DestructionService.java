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
    private final Set<UUID> laserPriming = new HashSet<>();
    private final Map<UUID, String> lastInteraction = new HashMap<>();
    private final Map<UUID, Long> lastShots = new HashMap<>();
    private int activeMassJobs = 0;

    public DestructionService(MillionBlocksPlugin plugin) { this.plugin = plugin; }

    public int breakCube(Player player, Block center, int rx, int ry, int rz, int cap) {
        if (blocked(center.getWorld())) return 0;
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

    public void startLaser(Player player) { startLaser(player, false); }
    public void startMountedLaser(Player player) {
        if(!plugin.getCinematicService().isPilot(player))throw new IllegalArgumentException("Monte dans REDLINE d'abord");
        stopLaser(player); startLaser(player, true);
    }
    private void startLaser(Player player, boolean mounted) {
        if(blocked(player.getWorld())) {
            player.sendMessage("§cArmes désactivées dans ce monde.");return;
        }
        UUID id=player.getUniqueId();
        if(activeLasers.containsKey(id) || !laserPriming.add(id))return;
        final World initial=player.getWorld();
        final int range=Math.max(32,Math.min(256,plugin.getConfig().getInt("weapons.laser.range",220)));
        // Warm the trajectory's chunks asynchronously once. Do NOT load chunks synchronously
        // every pulse, and never destroy blocks beyond the chunks that are loaded.
        plugin.getAimService().resolve(player,range,aim -> {
            if(!laserPriming.remove(id))return; // released during charging
            if(!player.isOnline() || player.getWorld()!=initial || blocked(initial))return;
            if(!mounted && (plugin.getItemFactory().identify(player.getInventory().getItemInMainHand())!=ToolId.LASER
                || !player.isHandRaised()))return;
            int interval=Math.max(1,plugin.getConfig().getInt("weapons.laser.tick-interval",2));
            int width=Math.max(1,Math.min(7,plugin.getConfig().getInt("weapons.laser.width",5)));
            int radius=width/2;
            int budget=Math.max(64,Math.min(2000,plugin.getConfig().getInt("weapons.laser.max-blocks-per-pulse",500)));
            BukkitTask task=new BukkitRunnable() {
                int age=0;
                @Override public void run() {
                    age+=interval;
                    if(!player.isOnline() || player.getWorld()!=initial || blocked(initial)
                        || (mounted ? !plugin.getCinematicService().isPilot(player)
                                   : plugin.getItemFactory().identify(player.getInventory().getItemInMainHand())!=ToolId.LASER
                                   || (age>=8 && !player.isHandRaised()))) {
                        stopLaser(player); return;
                    }
                    Location eye=mounted?plugin.getCinematicService().emitter(player):player.getEyeLocation();
                    Vector dir=eye.getDirection().normalize();
                    Vector right=dir.clone().crossProduct(new Vector(0,1,0));
                    if(right.lengthSquared()<0.01)right=new Vector(1,0,0);
                    right.normalize();
                    Vector up=right.clone().crossProduct(dir).normalize();
                    int destroyed=0;
                    HashSet<Long> seen=new HashSet<>();
                    for(int dist=2;dist<=range;dist+=2){
                        Location point=eye.clone().add(dir.clone().multiply(dist));
                        if(!initial.isChunkLoaded(point.getBlockX()>>4,point.getBlockZ()>>4))break;
                        // beam visible in empty air, red like the reference video
                        initial.spawnParticle(Particle.DUST,point,4,.05,.05,.05,0,
                            new Particle.DustOptions(Color.fromRGB(255,24,35),1.5f));
                        if(dist%4==0)initial.spawnParticle(Particle.ELECTRIC_SPARK,point,1,.06,.06,.06,.02);
                        for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++){
                            if(destroyed>=budget)break;
                            Block block=point.clone().add(right.clone().multiply(a)).add(up.clone().multiply(b)).getBlock();
                            if(!initial.isChunkLoaded(block.getX()>>4,block.getZ()>>4))continue;
                            if(!seen.add(block.getBlockKey()) || !isBreakable(block))continue;
                            block.setType(Material.AIR,false);
                            destroyed++;
                        }
                        if(destroyed>=budget)break;
                    }
                    if(destroyed>0)plugin.addProgress(player,destroyed);
                    if(age%12<interval)initial.playSound(eye,Sound.ENTITY_WARDEN_SONIC_BOOM,.18f,1.9f);
                }
            }.runTaskTimer(plugin,0L,interval);
            activeLasers.put(id,task);
        });
        player.sendMessage("§cREDLINE : charge du rayon long portée... maintiens le clic droit.");
    }

    public void stopLaser(Player player) {
        laserPriming.remove(player.getUniqueId());
        BukkitTask task=activeLasers.remove(player.getUniqueId());
        if(task!=null)task.cancel();
    }
    public void stopAll() {
        for(BukkitTask task:activeLasers.values())task.cancel();
        activeLasers.clear();
        laserPriming.clear();
        for(World world:Bukkit.getWorlds())world.removePluginChunkTickets(plugin);
    }

    public void launchTnt(Player player) {
        if(blocked(player.getWorld())||!cooldown(player,"tnt",450L))return;
        plugin.getAimService().resolve(player,Math.min(256,plugin.getConfig().getInt("weapons.tnt.range",170)),aim-> {
            Location target=aim.location();
            Location start=player.getEyeLocation().clone();
            World world=start.getWorld();
            int fuse=Math.max(10,plugin.getConfig().getInt("weapons.tnt.impact-ticks",22));
            new BukkitRunnable(){
                int step=0;
                @Override public void run(){
                    if(!player.isOnline()||player.getWorld()!=world||blocked(world)){cancel();return;}
                    double a=Math.min(1.0,(double)step/fuse);
                    Location p=start.clone().add(target.toVector().subtract(start.toVector()).multiply(a));
                    p.add(0,1.5*Math.sin(a*Math.PI),0);
                    if(world.isChunkLoaded(p.getBlockX()>>4,p.getBlockZ()>>4))
                        world.spawnParticle(Particle.SMOKE,p,8,.20,.20,.20,.02);
                    step+=2;
                    if(step>=fuse){
                        world.spawnParticle(Particle.EXPLOSION,target,10,1.4,1.4,1.4,.05);
                        world.playSound(target,Sound.ENTITY_GENERIC_EXPLODE,1.4f,.85f);
                        scheduleSphere(player,target,Math.max(4,plugin.getConfig().getInt("weapons.tnt.radius",7)),
                            2100,Math.max(300,plugin.getConfig().getInt("weapons.mass-blocks-per-tick",1200)),false);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin,0L,2L);
            plugin.getCinematicService().animate(player,ToolId.TNT_LAUNCHER,8);
        });
    }

    public void deployCreeper(Player player) {
        if(blocked(player.getWorld())||!cooldown(player,"creeper",1200L))return;
        plugin.getAimService().resolve(player,
            Math.min(256,plugin.getConfig().getInt("weapons.creeper.range",170)),aim -> {
            Location target=aim.location().clone();
            if(target.getBlock().getType().isSolid())target.add(0,1,0);
            World world=target.getWorld();
            // The miner works in PEACEFUL: Creeper is visual only, never a dependency.
            org.bukkit.entity.Entity visual=null;
            try {
                if(world.getDifficulty()!=Difficulty.PEACEFUL){
                    Creeper creeper=world.spawn(target,Creeper.class);
                    creeper.setPowered(true);creeper.setAI(false);creeper.setSilent(true);
                    visual=creeper;
                } else {
                    org.bukkit.entity.ItemDisplay display=world.spawn(target,org.bukkit.entity.ItemDisplay.class);
                    display.setItemStack(new org.bukkit.inventory.ItemStack(Material.CREEPER_HEAD));
                    visual=display;
                }
            }catch(RuntimeException ex){plugin.getLogger().info("Visual Creeper unavailable: "+ex.getMessage());}
            final org.bukkit.entity.Entity cosmetic=visual;
            player.sendMessage("§aCreeper Miner verrouillé: §f"+Math.round(target.distance(player.getEyeLocation()))+" blocs §7("+
                (aim.blockHit()?"raycast":"projection terrain")+").");
            world.playSound(target,Sound.ENTITY_CREEPER_PRIMED,1.2f,1.0f);
            new BukkitRunnable(){
                int ticks=0;
                @Override public void run(){
                    ticks+=2;
                    if(blocked(world)){if(cosmetic!=null&&cosmetic.isValid())cosmetic.remove();cancel();return;}
                    world.spawnParticle(Particle.DUST,target.clone().add(0,1,0),10,.4,.5,.4,0,
                        new Particle.DustOptions(Color.fromRGB(64,235,55),1.4f));
                    if(ticks>=26){
                        if(cosmetic!=null && cosmetic.isValid())cosmetic.remove();
                        world.playSound(target,Sound.ENTITY_GENERIC_EXPLODE,1.8f,1.05f);
                        world.spawnParticle(Particle.EXPLOSION,target,15,2,2,2,0);
                        scheduleSphere(player,target,Math.max(3,plugin.getConfig().getInt("weapons.creeper.radius",6)),
                            2600,Math.max(300,plugin.getConfig().getInt("weapons.creeper.blocks-per-tick",800)),false);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin,0L,2L);
        });
    }

    public void drill(Player player) {
        if(blocked(player.getWorld()))return;
        plugin.getAimService().resolve(player,Math.min(256,plugin.getConfig().getInt("weapons.drill.range",120)), aim -> {
            plugin.getCinematicService().animate(player,ToolId.DRILL,30);
            Location origin=player.getEyeLocation();
            World world=origin.getWorld();
            Vector dir=origin.getDirection().normalize();
            int max=(int)Math.min(aim.distance(),origin.distance(aim.location()));
            for(int step=2;step<=max;step+=3){
                final int at=step;
                plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                    if(!player.isOnline()||player.getWorld()!=world||blocked(world))return;
                    Block center=origin.clone().add(dir.clone().multiply(at)).getBlock();
                    if(!world.isChunkLoaded(center.getX()>>4,center.getZ()>>4))return;
                    breakCube(player,center,1,1,1,27);
                    world.spawnParticle(Particle.BLOCK_CRUMBLE,center.getLocation().add(.5,.5,.5),
                        8,.5,.5,.5,Material.STONE.createBlockData());
                },(long)(step/3)*2L);
            }
        });
    }

    public void meteor(Player player) {
        if(blocked(player.getWorld())||!cooldown(player,"meteor",3000L))return;
        plugin.getAimService().resolve(player,
            Math.min(256,plugin.getConfig().getInt("weapons.meteor.range",210)),aim->{
            final Location target=aim.location();
            int radius=Math.max(6,Math.min(30,plugin.getConfig().getInt("weapons.meteor.radius",14)));
            long delay=Math.max(10,plugin.getConfig().getInt("weapons.meteor.charge-ticks",35));
            player.sendMessage("§6☄ Météore verrouillé : "+(aim.blockHit()?"raycast":"terrain distant")+" — impact !");
            new BukkitRunnable(){
                long t=0;
                @Override public void run(){
                    if(blocked(target.getWorld())){cancel();return;}
                    t+=2;
                    Location p=target.clone().add(0,Math.max(0,28.0*(1d-(double)t/delay)),0);
                    target.getWorld().spawnParticle(Particle.FLAME,p,35,1.1,1.1,1.1,.04);
                    target.getWorld().spawnParticle(Particle.END_ROD,p,7,.3,.3,.3,.01);
                    if(t>=delay){
                        target.getWorld().playSound(target,Sound.ENTITY_GENERIC_EXPLODE,3f,.6f);
                        target.getWorld().spawnParticle(Particle.EXPLOSION,target,25,5,3,5,0);
                        scheduleSphere(player,target,radius,Math.max(10000,plugin.getConfig().getInt("weapons.meteor.max-blocks",20000)),
                            Math.max(100,plugin.getConfig().getInt("weapons.mass-blocks-per-tick",1200)),true);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin,0L,2L);
        });
    }

    public int worldEater(Player player) {
        if(blocked(player.getWorld())||!cooldown(player,"world_eater",6000L))return 0;
        if(activeMassJobs>=Math.max(1,plugin.getConfig().getInt("weapons.max-concurrent-mass-jobs",2))){
            player.sendMessage("§cDestruction massive déjà en cours.");return 0;
        }
        plugin.getAimService().resolve(player,
            Math.min(256,plugin.getConfig().getInt("weapons.world-eater.range",240)),aim->{
            final Location target=aim.location();
            final int radius=Math.max(15,Math.min(45,plugin.getConfig().getInt("weapons.world-eater.radius",34)));
            final long delay=Math.max(15,plugin.getConfig().getInt("weapons.world-eater.charge-ticks",60));
            plugin.getCinematicService().animate(player,ToolId.WORLD_EATER,(int)delay);
            player.sendMessage("§5§lWORLD EATER §d— surcharge à §f"+Math.round(target.distance(player.getEyeLocation()))+" blocs !");
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
                        target.getWorld().playSound(target,Sound.ENTITY_GENERIC_EXPLODE,4f,.55f);
                        scheduleSphere(player,target,radius,Math.max(150000,plugin.getConfig().getInt("weapons.world-eater.max-blocks",200000)),
                            Math.max(250,plugin.getConfig().getInt("weapons.mass-blocks-per-tick",1200)),true);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin,0L,2L);
        });
        return 0;
    }

    public void recordInteraction(Player player,String action,boolean cancelled){
        lastInteraction.put(player.getUniqueId(),action+(cancelled?" (déjà annulé)":"")+" / "+System.currentTimeMillis());
    }

    public String diagnostics(Player player){
        ToolId held=plugin.getItemFactory().identify(player.getInventory().getItemInMainHand());
        World world=player.getWorld();
        Location target=player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(150));
        return "§eWeapon debug: §7world=§f"+world.getName()+
            " §7hub/blocked=§f"+blocked(world)+
            " §7held=§f"+(held==null?"not a MillionBlocks weapon":held.configKey())+
            " §7target=§f"+target.getBlockX()+","+target.getBlockY()+","+target.getBlockZ()+
            " §7distance=§f"+Math.round(target.distance(player.getEyeLocation()))+
            " §7chunkLoaded=§f"+world.isChunkLoaded(target.getBlockX()>>4,target.getBlockZ()>>4)+
            " §7lastClick=§f"+lastInteraction.getOrDefault(player.getUniqueId(),"aucun")+
            " §7holding=§f"+player.isHandRaised();
    }
    public void test(Player player,ToolId id){
        if(blocked(player.getWorld())){
            player.sendMessage("§cArmes désactivées dans ce hub. /mb world status pour vérifier les destinations.");
            return;
        }
        switch(id){
            case LASER -> {
                // Explicit short pulse for command debugging; no fake held mouse state.
                player.sendMessage("§eTest laser: utilise /mb give laser puis maintiens clic droit (un item V0.5 est insuffisant).");
            }
            case CREEPER_MINER ->deployCreeper(player);
            case TNT_LAUNCHER ->launchTnt(player);
            case DRILL ->drill(player);
            case METEOR ->meteor(player);
            case WORLD_EATER ->worldEater(player);
            default ->player.sendMessage("§ePioche et marteau s'activent au cassage de blocs.");
        }
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
                if(!owner.isOnline() || owner.getWorld()!=world || blocked(world) || ++elapsed>1800){finish();return;}
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
                if(budget>0)plugin.addProgress(owner,budget);
                if(index>=total || removed>=maxBlocks)finish();
            }
            private void finish() {
                activeMassJobs=Math.max(0,activeMassJobs-1);
                for(org.bukkit.Chunk chunk:ticketed)
                    world.removePluginChunkTicket(chunk.getX(),chunk.getZ(),plugin);
                if(owner.isOnline()){
                    // Each pulse already updates the score and HUD immediately.
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
