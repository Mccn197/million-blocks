package eu.reverie.millionblocks;

import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Dedicated reward playground: owns ONLY a new, explicitly named void world
 * and a reproducible set of 16x16x16 material samples. No world deletion.
 */
public final class RewardDestructionsService {
    private static final int Y_FLOOR = 72;
    private static final int Y_BLOCKS = 73;
    private static final int SIZE = 16;
    private static final int PER_TICK_CAP = 1250;
    private final MillionBlocksPlugin plugin;
    private final File file;
    private final NamespacedKey labelTag;
    private final Map<String, Material> materials = new LinkedHashMap<>();
    private final Deque<Placement> queue = new ArrayDeque<>();
    private final Set<UUID> waiting = new HashSet<>();
    private BukkitTask task;
    private World world;
    private String currentJob = "idle";
    private long changed = 0;

    private record Placement(int x,int y,int z,Material material) {}
    private record Plot(int x,int z,String id,Material material) {}

    public RewardDestructionsService(MillionBlocksPlugin plugin) {
        this.plugin=plugin;
        this.file=new File(plugin.getDataFolder(),"reward-destructions.yml");
        this.labelTag=new NamespacedKey(plugin,"reward-label");
        register("stone",Material.STONE);
        register("dirt",Material.DIRT);
        register("deepslate",Material.DEEPSLATE);
        register("coal",Material.COAL_ORE);
        register("iron",Material.IRON_ORE);
        register("gold",Material.GOLD_ORE);
        register("diamond",Material.DIAMOND_ORE);
        register("emerald",Material.EMERALD_ORE);
        register("copper",Material.COPPER_ORE);
        register("redstone",Material.REDSTONE_ORE);
        register("lapis",Material.LAPIS_ORE);
        register("ancient_debris",Material.ANCIENT_DEBRIS);
        register("obsidian",Material.OBSIDIAN);
        register("bedrock",Material.BEDROCK);
        register("glass",Material.GLASS);
        register("diamond_block",Material.DIAMOND_BLOCK);
        register("netherite_block",Material.NETHERITE_BLOCK);
        register("tinted_glass",Material.TINTED_GLASS);
        register("amethyst",Material.AMETHYST_BLOCK);
        register("end_stone",Material.END_STONE);
    }
    private void register(String id,Material material){materials.put(id,material);}
    public String worldName() { return plugin.getConfig().getString("reward-world.name","reward_destructions"); }
    public boolean isReward(World other) { return other!=null && other.getName().equalsIgnoreCase(worldName()); }
    public boolean isBusy() { return task!=null && !task.isCancelled(); }
    public String status(){
        var y=YamlConfiguration.loadConfiguration(file);
        return "§6Reward: §f"+worldName()+" §7| prêt=§f"+y.getBoolean("built",false)+
            " §7| job=§f"+currentJob+" §7| restants=§f"+queue.size();
    }
    public Set<String> materialIds(){return Collections.unmodifiableSet(materials.keySet());}
    private void validateWorldName(){
        String name=worldName();
        if(name.isBlank() || name.contains("/") || name.contains("\\") || name.contains(".."))
            throw new IllegalArgumentException("Nom du monde reward invalide.");
        if(name.equalsIgnoreCase(plugin.getWorldService().hubName()) ||
           name.equalsIgnoreCase(plugin.getWorldService().survivalName()) ||
           name.equalsIgnoreCase(plugin.getWorldService().candidateSurvivalName()) ||
           name.equalsIgnoreCase("mining_sky") || name.equalsIgnoreCase("mining_spawn"))
            throw new IllegalArgumentException("Le monde reward doit avoir un nom distinct du hub et de la Survival.");
    }
    public void prepare(){
        if(!plugin.getConfig().getBoolean("reward-world.enabled",true))
            throw new IllegalStateException("reward-world.enabled est désactivé.");
        validateWorldName();
        if(isBusy())return;
        YamlConfiguration meta=YamlConfiguration.loadConfiguration(file);
        String name=worldName();
        File folder=new File(Bukkit.getWorldContainer(),name);
        if(folder.exists() && !meta.getBoolean("managed",false))
            throw new IllegalStateException("Ce monde existe déjà et n'appartient pas à MillionBlocks : pas de modification.");
        if(meta.getBoolean("managed",false) && !name.equals(meta.getString("world")))
            throw new IllegalStateException("Le manifeste reward appartient à un autre monde.");
        world=Bukkit.getWorld(name);
        if(world==null) world=Bukkit.createWorld(new WorldCreator(name)
            .environment(World.Environment.NORMAL).generateStructures(false)
            .generator(new ChunkGenerator(){}));
        if(world==null)throw new IllegalStateException("Impossible de charger "+name);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(6000);
        world.setStorm(false);
        world.setSpawnLocation(0,Y_FLOOR+2,-25);
        meta.set("managed",true);
        meta.set("world",name);
        if(!file.getParentFile().exists())file.getParentFile().mkdirs();
        try {meta.save(file);}catch(IOException ex){throw new IllegalStateException("Impossible d'écrire le manifeste",ex);}
        if(!meta.getBoolean("built",false))beginBuild(null,true);
    }
    public void teleport(Player player){
        if(!player.isOnline())return;
        waiting.add(player.getUniqueId());
        try{
            prepare();
            if(!isBusy())teleportWaiting();
        }catch(RuntimeException ex){
            waiting.remove(player.getUniqueId());
            player.sendMessage("§cReward Destructions indisponible : "+ex.getMessage());
            plugin.getLogger().warning("Reward teleport: "+ex);
        }
    }
    private Location spawn(){
        return new Location(world,0.5,Y_FLOOR+2,-25.5,0,10);
    }
    private void teleportWaiting(){
        if(world==null)return;
        for(UUID id:waiting){
            Player player=Bukkit.getPlayer(id);
            if(player==null || !player.isOnline())continue;
            player.teleport(spawn());
            player.sendMessage("§6§lREWARD DESTRUCTIONS §7— teste tes armes sur les 20 chunks de matériaux !");
            player.sendMessage("§7Pour reconstruire : §e/mb reward reset all confirm §7(OP uniquement).");
        }
        waiting.clear();
    }
    public void reset(String id){
        if(isBusy())throw new IllegalStateException("Construction déjà en cours.");
        YamlConfiguration meta=YamlConfiguration.loadConfiguration(file);
        if(!meta.getBoolean("managed",false) || !meta.getBoolean("built",false))
            throw new IllegalStateException("Prépare d'abord l'arène : /mb reward prepare.");
        if(world==null)world=Bukkit.getWorld(worldName());
        if(world==null)throw new IllegalStateException("Monde reward déchargé.");
        if(!id.equals("all") && !materials.containsKey(id))
            throw new IllegalArgumentException("Matériau inconnu : "+id);
        for(Player p:world.getPlayers()){
            boolean inside=id.equals("all");
            for(Plot plot:plots()){
                if(!id.equals("all") && !plot.id().equals(id))continue;
                Location l=p.getLocation();
                if(l.getBlockX()>=plot.x()-1 && l.getBlockX()<plot.x()+SIZE+1 &&
                   l.getBlockZ()>=plot.z()-1 && l.getBlockZ()<plot.z()+SIZE+1)
                    inside=true;
            }
            if(inside)p.teleport(spawn());
        }
        beginBuild(id,id.equals("all"));
    }
    private List<Plot> plots(){
        List<Plot> list=new ArrayList<>();
        int i=0;
        for(var entry:materials.entrySet()){
            int col=i%4, row=i/4;
            list.add(new Plot(-72+col*48,28+row*40,entry.getKey(),entry.getValue()));
            i++;
        }
        return list;
    }
    private void beginBuild(String only, boolean full){
        if(isBusy())throw new IllegalStateException("Job reward déjà en cours");
        queue.clear();changed=0;
        if(full){
            // Stable, indestructible path and spawn floor. Generated only inside reward world.
            for(int x=-98;x<=98;x++)for(int z=-37;z<=205;z++) {
                queue.addLast(new Placement(x,Y_FLOOR,z,Material.BEDROCK));
            }
            for(int x=-9;x<=9;x++)for(int z=-33;z<=-18;z++)
                queue.addLast(new Placement(x,Y_FLOOR+1,z,Material.POLISHED_ANDESITE));
        }
        for(Plot plot:plots()){
            if(only!=null && !only.equals("all") && !only.equals(plot.id()))continue;
            for(int x=plot.x();x<plot.x()+SIZE;x++)
                for(int z=plot.z();z<plot.z()+SIZE;z++)
                    for(int y=Y_BLOCKS;y<Y_BLOCKS+SIZE;y++)
                        queue.addLast(new Placement(x,y,z,plot.material()));
        }
        currentJob=only==null?"initial construction":("reset "+only);
        if(full) {
            YamlConfiguration meta=YamlConfiguration.loadConfiguration(file);
            meta.set("built",false);
            try{meta.save(file);}catch(IOException ex){throw new IllegalStateException("Manifest reward inaccessible",ex);}
        }
        task=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            int limit=Math.min(PER_TICK_CAP,queue.size());
            for(int i=0;i<limit;i++){
                Placement p=queue.removeFirst();
                world.getBlockAt(p.x(),p.y(),p.z()).setType(p.material(),false);
                changed++;
            }
            if(queue.isEmpty()){
                task.cancel();task=null;
                currentJob="idle";
                for(Plot plot:plots()){
                    if(full || only==null || only.equals("all") || plot.id().equals(only)) label(plot);
                }
                YamlConfiguration meta=YamlConfiguration.loadConfiguration(file);
                meta.set("managed",true);
                meta.set("world",worldName());
                if(full)meta.set("built",true);
                meta.set("last-block-count",changed);
                try{meta.save(file);}catch(IOException ex){plugin.getLogger().severe("Reward manifest save: "+ex);}
                teleportWaiting();
            }
        },1L,1L);
    }
    private void label(Plot plot){
        Location location=new Location(world,plot.x()+8.0,Y_BLOCKS+SIZE+2.2,plot.z()+8.0);
        for(TextDisplay display:world.getNearbyEntitiesByType(TextDisplay.class,location,3,3,3)){
            if(plot.id().equals(display.getPersistentDataContainer().get(labelTag,PersistentDataType.STRING)))
                display.remove();
        }
        world.spawn(location,TextDisplay.class,display->{
            display.setText("§6§l"+plot.id().toUpperCase(Locale.ROOT)+" §7• 16×16×16");
            display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.getPersistentDataContainer().set(labelTag,PersistentDataType.STRING,plot.id());
        });
    }
    public void shutdown(){
        if(task!=null){task.cancel();task=null;}
        queue.clear();waiting.clear();
    }
}
