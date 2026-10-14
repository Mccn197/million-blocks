package eu.reverie.millionblocks;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.Vector;
import java.util.*;

/** Safe, non-destructive world management. Never overwrites a user's build on startup. */
public final class WorldService {
    private final MillionBlocksPlugin plugin;
    private final Map<UUID, Location> survivalReturns = new HashMap<>();
    private final Set<UUID> editors = new HashSet<>();
    public WorldService(MillionBlocksPlugin plugin) { this.plugin = plugin; }

    public void ensureWorlds() {
        if (!plugin.getConfig().getBoolean("world-system.enabled", true)) return;
        ensureHubWorld();
        if (Bukkit.getWorld(survivalName()) == null) {
            Bukkit.createWorld(new WorldCreator(survivalName())
                    .seed(plugin.getConfig().getLong("world-system.survival.seed", 42203442493L))
                    .generateStructures(true));
        }
    }
    public World ensureHubWorld() {
        String name = hubName();
        World world = Bukkit.getWorld(name);
        if (world == null) {
            WorldCreator creator = new WorldCreator(name).generateStructures(false);
            if (plugin.getConfig().getBoolean("world-system.mining-spawn.void-world", false)) {
                creator.generator(new ChunkGenerator() {});
            } else creator.type(WorldType.FLAT);
            world = Bukkit.createWorld(creator);
        }
        if (world != null) {
            world.setDifficulty(Difficulty.PEACEFUL);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setTime(6000);
            world.setStorm(false);
            // Do not call makeSafe(): doing so silently changes schematics at every restart.
        }
        return world;
    }
    public World createSkyWorld() {
        String newName = plugin.getConfig().getString("hub.sky-world-name", "mining_sky");
        if (newName.equalsIgnoreCase(survivalName())) throw new IllegalArgumentException("Le nom sky ne peut pas être celui du monde survival.");
        plugin.getConfig().set("world-system.mining-spawn.name", newName);
        plugin.getConfig().set("world-system.mining-spawn.void-world", true);
        plugin.getConfig().set("world-system.mining-spawn.spawn.x", 0.5);
        plugin.getConfig().set("world-system.mining-spawn.spawn.y", 152.0);
        plugin.getConfig().set("world-system.mining-spawn.spawn.z", 0.5);
        plugin.getConfig().set("locations.reward-room.enabled", false);
        plugin.saveConfig();
        return ensureHubWorld();
    }
    public World prepareNewSurvival() {
        String name = plugin.getConfig().getString("world-system.survival-next.name", "survival_v2");
        if (name.equalsIgnoreCase(hubName()) || name.equalsIgnoreCase("mining_sky")
                || name.equalsIgnoreCase("mining_spawn"))
            throw new IllegalArgumentException("Nom de monde interdit pour la Survival.");
        World w=Bukkit.getWorld(name);
        if (w==null) w=Bukkit.createWorld(new WorldCreator(name)
            .seed(plugin.getConfig().getLong("world-system.survival-next.seed",5133991228066333L))
            .generateStructures(true));
        if(w==null) throw new IllegalStateException("Impossible de créer "+name);
        return w;
    }
    public String candidateSurvivalName() {
        return plugin.getConfig().getString("world-system.survival-next.name","survival_v2");
    }
    public void chooseSurvival(boolean newWorld) {
        String name=newWorld?candidateSurvivalName():plugin.getConfig().getString("world-system.survival-original-name","survival");
        World w= newWorld?prepareNewSurvival():Bukkit.getWorld(name);
        if(w==null)w=Bukkit.createWorld(new WorldCreator(name).generateStructures(true));
        if(w==null)throw new IllegalStateException("Monde introuvable : "+name);
        plugin.getConfig().set("world-system.survival.name",name);
        plugin.saveConfig();
        survivalReturns.clear();
    }

    public void selectHubHere(Player player) {
        World here=player.getWorld();
        if(here.getName().equalsIgnoreCase(survivalName()))
            throw new IllegalArgumentException("Le hub ne peut pas être la Survival active.");
        // Select the existing world; never generate, paste or move any world data.
        plugin.getConfig().set("world-system.mining-spawn.name",here.getName());
        plugin.getConfig().set("hub.sky-world-name",here.getName());
        // The player's feet must be on a safe floor.
        Location destination=player.getLocation().clone();
        if(!isSafe(destination))throw new IllegalArgumentException("Position non sécurisée. Place-toi sur un sol solide (2 blocs d'air).");
        String path="world-system.mining-spawn.spawn.";
        plugin.getConfig().set(path+"x",destination.getX());
        plugin.getConfig().set(path+"y",destination.getY());
        plugin.getConfig().set(path+"z",destination.getZ());
        plugin.getConfig().set(path+"yaw",(double)destination.getYaw());
        plugin.getConfig().set(path+"pitch",(double)destination.getPitch());
        plugin.saveConfig();
    }
    public void selectSurvivalHere(Player player) {
        World here=player.getWorld();
        if(isHub(here)||here.getName().equalsIgnoreCase("mining_spawn")||here.getName().equalsIgnoreCase("mining_sky"))
            throw new IllegalArgumentException("Ce monde est un hub ; impossible de le choisir comme Survival.");
        plugin.getConfig().set("world-system.survival.name",here.getName());
        plugin.saveConfig();
        survivalReturns.clear();
        // Persist the preferred arrival on disk, not just in the current session.
        if(isSafe(player.getLocation())){
            Location loc=player.getLocation();
            String path="world-system.survival.spawn.";
            plugin.getConfig().set(path+"world",here.getName());
            plugin.getConfig().set(path+"x",loc.getX());
            plugin.getConfig().set(path+"y",loc.getY());
            plugin.getConfig().set(path+"z",loc.getZ());
            plugin.getConfig().set(path+"yaw",(double)loc.getYaw());
            plugin.getConfig().set(path+"pitch",(double)loc.getPitch());
            plugin.saveConfig();
            survivalReturns.put(player.getUniqueId(),loc.clone());
        }
    }
    public String destinations() {
        return "§eHub: §b"+hubName()+" §7("+((Bukkit.getWorld(hubName())!=null)?"chargé":"non chargé")+")"+
            " §eSurvival: §a"+survivalName()+" §7("+((Bukkit.getWorld(survivalName())!=null)?"chargé":"non chargé")+")";
    }

    public String hubName() { return plugin.getConfig().getString("world-system.mining-spawn.name", "mining_spawn"); }
    public String survivalName() { return plugin.getConfig().getString("world-system.survival.name", "survival"); }
    public boolean isHub(World world) { return world != null && world.getName().equalsIgnoreCase(hubName()); }
    public boolean isProtectedHub(World world) { return isHub(world) && plugin.getConfig().getBoolean("world-system.mining-spawn.protected", true); }
    public boolean canEdit(Player player) { return !isProtectedHub(player.getWorld()) || (player.hasPermission("millionblocks.admin") && editors.contains(player.getUniqueId())); }
    public boolean toggleEditor(Player player, boolean on) { if (on) editors.add(player.getUniqueId()); else editors.remove(player.getUniqueId()); return on; }
    public boolean isEditor(Player player) { return editors.contains(player.getUniqueId()); }
    public void setLock(boolean locked) { plugin.getConfig().set("world-system.mining-spawn.protected", locked); plugin.saveConfig(); }
    public void setAutoSpawn(World world,int x,int z) {
        int y=world.getHighestBlockYAt(x,z)+1;
        // Sky world: do not choose the void as a spawn location.
        if(y<=world.getMinHeight()+1) return;
        String path="world-system.mining-spawn.spawn.";
        plugin.getConfig().set(path+"x",x+.5);plugin.getConfig().set(path+"y",(double)y);plugin.getConfig().set(path+"z",z+.5);
        plugin.getConfig().set(path+"yaw",180.0);plugin.getConfig().set(path+"pitch",0.0);
        plugin.saveConfig();
        world.setSpawnLocation(x,y,z);
    }
    public void setHubSpawn(Player player) {
        if (!isHub(player.getWorld())) throw new IllegalArgumentException("Tu dois être dans le hub actif.");
        Location loc = player.getLocation();
        String path = "world-system.mining-spawn.spawn.";
        plugin.getConfig().set(path+"x", loc.getX());plugin.getConfig().set(path+"y", loc.getY());plugin.getConfig().set(path+"z", loc.getZ());
        plugin.getConfig().set(path+"yaw", (double)loc.getYaw()); plugin.getConfig().set(path+"pitch", (double)loc.getPitch());
        plugin.saveConfig();
        player.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    public Location hubSpawn(World world) {
        String path = "world-system.mining-spawn.spawn.";
        return new Location(world, plugin.getConfig().getDouble(path+"x", 0.5),plugin.getConfig().getDouble(path+"y", 81),
                plugin.getConfig().getDouble(path+"z", 6.5), (float)plugin.getConfig().getDouble(path+"yaw", 180),
                (float)plugin.getConfig().getDouble(path+"pitch", 0));
    }
    public void teleportToggle(Player player) { if (isHub(player.getWorld())) teleportSurvival(player); else teleportHub(player); }
    public void teleportHub(Player player) {
        World world = Bukkit.getWorld(hubName());
        if (world == null) {player.sendMessage("§cHub introuvable.");return;}
        Location spawn = hubSpawn(world);
        if (!isSafe(spawn)) { player.sendMessage("§cSpawn invalide dans §e"+hubName()+"§c. Sur la plateforme, tape /mb world set hub here.");return; }
        if (!isHub(player.getWorld()) && player.getWorld().getName().equalsIgnoreCase(survivalName()))
            survivalReturns.put(player.getUniqueId(),player.getLocation().clone());
        safeTeleport(player,spawn);
    }
    public void teleportSurvival(Player player) {
        World world = Bukkit.getWorld(survivalName());
        if (world == null) { player.sendMessage("§cSurvival introuvable."); return; }
        Location remembered = survivalReturns.get(player.getUniqueId());
        String path="world-system.survival.spawn.";
        Location saved=null;
        if(world.getName().equalsIgnoreCase(plugin.getConfig().getString(path+"world",""))){
            saved=new Location(world,plugin.getConfig().getDouble(path+"x"),
                plugin.getConfig().getDouble(path+"y"),plugin.getConfig().getDouble(path+"z"),
                (float)plugin.getConfig().getDouble(path+"yaw"),(float)plugin.getConfig().getDouble(path+"pitch"));
        }
        Location target = remembered != null && remembered.getWorld() == world && isSafe(remembered) ? remembered
                : saved!=null && isSafe(saved)?saved
                : new Location(world,0.5,Math.max(world.getMinHeight()+3,world.getHighestBlockYAt(0,0)+1),0.5);
        if(!isSafe(target)){player.sendMessage("§cPoint de retour Survival dangereux dans "+survivalName()+" ; TP annulé.");return;}
        safeTeleport(player,target);
    }
    public void teleportReward(Player player) {
        // Without an explicit reward destination, don't send players into the void.
        plugin.configuredLocation("locations.reward-room").ifPresentOrElse(loc -> {
            if (isSafe(loc)) safeTeleport(player,loc); else player.sendMessage("§cReward room non sécurisée; TP annulé.");
        },() -> player.sendMessage("§6Victoire ! Définis la reward room dans config.yml avant le tournage."));
    }
    private boolean isSafe(Location loc) {
        if (loc.getWorld() == null) return false;
        loc.getChunk().load();
        return loc.getBlock().getType().isAir() && loc.clone().add(0,1,0).getBlock().getType().isAir()
                && loc.clone().add(0,-1,0).getBlock().getType().isSolid();
    }
    private void safeTeleport(Player player,Location target) {
        player.setVelocity(new Vector(0,0,0));player.setFallDistance(0);
        player.teleport(target);player.setFallDistance(0);
    }
}
