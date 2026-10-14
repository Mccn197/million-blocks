package eu.reverie.millionblocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Chunk;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShowcaseService {
    private final MillionBlocksPlugin plugin;
    private final File file;
    private final NamespacedKey showcaseIdKey;
    private final NamespacedKey showcaseToolKey;
    private final Map<String, ShowcaseDefinition> definitions = new LinkedHashMap<>();
    // Preserve YAML entries for worlds not loaded yet or newer/unknown item types.
    private final Map<String, Map<String, Object>> deferred = new LinkedHashMap<>();
    private final Map<String, DisplayBundle> spawned = new LinkedHashMap<>();
    private int nextId = 1;
    private BukkitTask animationTask;

    public ShowcaseService(MillionBlocksPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "showcases.yml");
        this.showcaseIdKey = new NamespacedKey(plugin, "showcase-id");
        this.showcaseToolKey = new NamespacedKey(plugin, "showcase-tool");
    }

    public void reload() {
        stopAnimation();
        removeSpawnedEntities();
        definitions.clear();
        deferred.clear();
        load();
        respawnAll();
        startAnimation();
    }

    public void shutdown() {
        stopAnimation();
        // Mutations already save immediately. Never overwrite disk data just because
        // no worlds or showcase definitions were available during plugin shutdown.
        removeSpawnedEntities();
    }

    public List<String> createLine(Player player, double spacing) {
        spacing = Math.max(1.0, Math.min(10.0, spacing));
        List<ToolId> tools = ToolId.playable();
        Location origin = player.getLocation().clone();
        Vector forward = flatDirection(origin).normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
        Location center = origin.clone().add(forward.clone().multiply(5.0));
        List<String> ids = new ArrayList<>();
        double offset = (tools.size() - 1) * spacing / 2.0;
        for (int i = 0; i < tools.size(); i++) {
            Location anchor = center.clone().add(right.clone().multiply(i * spacing - offset));
            anchor.setYaw(player.getLocation().getYaw() + 180f);
            ids.add(create(tools.get(i), anchor));
        }
        return ids;
    }

    public List<String> createCircle(Player player, double radius) {
        radius = Math.max(2.0, Math.min(20.0, radius));
        List<ToolId> tools = ToolId.playable();
        Location center = player.getLocation().clone();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < tools.size(); i++) {
            double angle = (Math.PI * 2.0 * i / tools.size()) - Math.PI / 2.0;
            Location anchor = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            anchor.setYaw(yawFacing(anchor, center));
            ids.add(create(tools.get(i), anchor));
        }
        return ids;
    }

    public String place(Player player, ToolId tool) {
        Location anchor;
        Block target = player.getTargetBlockExact(8);
        if (target != null) {
            anchor = target.getLocation().add(0.5, 1.0, 0.5);
        } else {
            Vector forward = flatDirection(player.getLocation()).normalize();
            anchor = player.getLocation().clone().add(forward.multiply(3.0));
        }
        anchor.setYaw(player.getLocation().getYaw() + 180f);
        return create(tool, anchor);
    }

    public String create(ToolId tool, Location anchor) {
        if (tool == ToolId.SHOP) throw new IllegalArgumentException("SHOP n'est pas un item de showcase.");
        if (anchor.getWorld() == null) throw new IllegalArgumentException("Monde invalide.");
        String id = String.format(Locale.ROOT, "sc-%03d", nextId++);
        ShowcaseDefinition def = new ShowcaseDefinition(id, tool, anchor.clone(), 1.0f);
        definitions.put(id, def);
        spawn(def);
        save();
        return id;
    }


    /** Moves a saved anchor and regenerates its three display entities. */
    public String move(String id, Player player) {
        if ("nearest".equalsIgnoreCase(id)) {
            ShowcaseDefinition closest = definitions.values().stream()
                .filter(def -> def.location().getWorld() == player.getWorld())
                .filter(def -> def.location().distanceSquared(player.getLocation()) < 15*15)
                .min(Comparator.comparingDouble(def -> def.location().distanceSquared(player.getLocation())))
                .orElse(null);
            if (closest == null) return null;
            id = closest.id();
        }
        ShowcaseDefinition current=definitions.get(id);
        if(current==null) return null;
        Location loc=player.getLocation().clone();
        Block target=player.getTargetBlockExact(10);
        Location anchor=target==null ? loc.clone() : target.getLocation().add(.5,1,.5);
        anchor.setYaw(loc.getYaw()+180f);
        definitions.put(id, new ShowcaseDefinition(id,current.tool(),anchor,current.size()));
        DisplayBundle old=spawned.remove(id);
        if(old!=null) old.remove();
        spawn(definitions.get(id));
        return id;
    }

    /** Repairs showcases whose entities were removed or disappeared due to chunk unload. */
    public int repairNear(Player player) {
        int count=0;
        World world=player.getWorld();
        for(ShowcaseDefinition def:definitions.values()) {
            if(def.location().getWorld()!=world) continue;
            if(def.location().distanceSquared(player.getLocation())>112*112)continue;
            if(ensure(def))count++;
        }
        return count;
    }

    public int repairChunk(Chunk chunk) {
        int count=0;
        for(ShowcaseDefinition def:definitions.values()){
            Location at=def.location();
            if(at.getWorld()==chunk.getWorld() &&
               (at.getBlockX()>>4)==chunk.getX() && (at.getBlockZ()>>4)==chunk.getZ()){
                if(ensure(def))count++;
            }
        }
        return count;
    }
    private boolean ensure(ShowcaseDefinition def) {
        DisplayBundle bundle=spawned.get(def.id());
        if(bundle!=null && bundle.item().isValid() && bundle.text().isValid()
             && bundle.interaction().isValid()) return false;
        if(bundle!=null)bundle.remove();
        spawned.remove(def.id());
        spawn(def);
        return true;
    }

    public String fullDetails(String id) {
        ShowcaseDefinition def=definitions.get(id);
        if(def==null)return "introuvable: "+id;
        Location l=def.location();
        return "§f"+id+" §7"+def.tool().configKey()+" §d×"+fmt(def.size())+" §8@ §b"+l.getWorld().getName()
            +" §e"+fmt(l.getX())+", "+fmt(l.getY())+", "+fmt(l.getZ())+" §7yaw="+fmt(l.getYaw());
    }
    public File file() { return file; }

    public boolean remove(String id) {
        ShowcaseDefinition removed = definitions.remove(id);
        DisplayBundle bundle = spawned.remove(id);
        if (bundle != null) bundle.remove();
        if (removed != null) save(definitions.isEmpty() && deferred.isEmpty());
        return removed != null;
    }

    public String removeNearest(Player player, double radius) {
        double maxSq = radius * radius;
        ShowcaseDefinition nearest = definitions.values().stream()
                .filter(def -> def.location().getWorld() == player.getWorld())
                .filter(def -> def.location().distanceSquared(player.getLocation()) <= maxSq)
                .min(Comparator.comparingDouble(def -> def.location().distanceSquared(player.getLocation())))
                .orElse(null);
        if (nearest == null) return null;
        remove(nearest.id());
        return nearest.id();
    }

    public int clear() {
        int count = definitions.size();
        definitions.clear();
        deferred.clear();
        removeSpawnedEntities();
        save(true);
        return count;
    }

    public void respawnAll() {
        removeSpawnedEntities();
        for (ShowcaseDefinition def : definitions.values()) spawn(def);
    }

    public List<String> describe() {
        List<String> result = new ArrayList<>();
        for (ShowcaseDefinition def : definitions.values()) {
            Location loc = def.location();
            result.add(def.id() + " = " + def.tool().configKey() + " @ " + loc.getWorld().getName() + " "
                    + fmt(loc.getX()) + " " + fmt(loc.getY()) + " " + fmt(loc.getZ())+" §d×"+fmt(def.size()));
        }
        return result;
    }

    public List<String> ids() {
        List<String> all=new ArrayList<>(definitions.keySet());
        all.addAll(deferred.keySet());
        return all;
    }

    public int deferredCount() { return deferred.size(); }

    public int resolveWorld(World world) {
        if (world == null) return 0;
        int resolved=0;
        for (String id: new ArrayList<>(deferred.keySet())) {
            Map<String,Object> raw=deferred.get(id);
            if (!world.getName().equals(String.valueOf(raw.get("world")))) continue;
            try {
                ToolId tool=ToolId.valueOf(String.valueOf(raw.get("tool")));
                Location loc=new Location(world,
                    num(raw.get("x")),num(raw.get("y")),num(raw.get("z")),
                    (float)num(raw.get("yaw")),(float)num(raw.get("pitch")));
                ShowcaseDefinition def=new ShowcaseDefinition(id,tool,loc,clampSize(raw.get("size")));
                definitions.put(id,def);
                deferred.remove(id);
                spawn(def);
                resolved++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Deferred showcase "+id+" still cannot load: "+ex.getMessage());
            }
        }
        return resolved;
    }
    private static float clampSize(Object value) {
        double n=value instanceof Number number ? number.doubleValue():Double.parseDouble(String.valueOf(value==null?1.0:value));
        if(!Double.isFinite(n))return 1f;
        return (float)Math.max(0.25,Math.min(12,n));
    }

    /** Resize the item model only; preserve its anchor, tool, and other stands. */
    public List<String> resize(String id, Player player, double scale) {
        if(!Double.isFinite(scale) || scale<0.25 || scale>12)
            throw new IllegalArgumentException("Taille: entre 0.25 et 12 (exemple: 5).");
        List<String> ids=new ArrayList<>();
        if("all".equalsIgnoreCase(id)){
            for(ShowcaseDefinition def:definitions.values())
                if(def.location().getWorld()==player.getWorld())ids.add(def.id());
        }else if("nearest".equalsIgnoreCase(id)){
            ShowcaseDefinition closest=definitions.values().stream()
                .filter(def->def.location().getWorld()==player.getWorld())
                .filter(def->def.location().distanceSquared(player.getLocation())<20*20)
                .min(Comparator.comparingDouble(def->def.location().distanceSquared(player.getLocation())))
                .orElse(null);
            if(closest!=null)ids.add(closest.id());
        }else if(definitions.containsKey(id)) ids.add(id);
        for(String key:ids){
            ShowcaseDefinition before=definitions.get(key);
            ShowcaseDefinition after=new ShowcaseDefinition(key,before.tool(),before.location(),(float)scale);
            definitions.put(key,after);
            DisplayBundle old=spawned.remove(key);
            if(old!=null)old.remove();
            spawn(after);
        }
        return ids;
    }

    private static double num(Object o) {
        if(o instanceof Number n)return n.doubleValue();
        return Double.parseDouble(String.valueOf(o));
    }

    public String diagnostic() {
        int onDisk=0;
        if(file.isFile()){
            ConfigurationSection root=YamlConfiguration.loadConfiguration(file).getConfigurationSection("showcases");
            if(root!=null)onDisk=root.getKeys(false).size();
        }
        return "§eShowcases: actifs=§f"+definitions.size()+" §edeferred=§f"+deferred.size()+
            " §esur disque=§f"+onDisk+" §7"+file.getAbsolutePath()+" ("+
            (file.isFile()?file.length()+" octets":"fichier absent")+")";
    }

    public ToolId identifyShowcaseTool(Entity entity) {
        if (entity == null) return null;
        String raw = entity.getPersistentDataContainer().get(showcaseToolKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try { return ToolId.valueOf(raw); } catch (IllegalArgumentException ignored) { return null; }
    }

    public boolean claim(Player player, Entity entity) {
        ToolId tool = identifyShowcaseTool(entity);
        if (tool == null) return false;
        if (!plugin.getConfig().getBoolean("showcase.click-to-claim", true)) return true;
        long required = plugin.getItemFactory().unlockAt(tool);
        long current = plugin.getProgressService().get(player);
        if (current < required) {
            player.sendMessage("§cIl te faut §f" + ItemFactory.formatNumber(required) + " §cblocs pour débloquer cet item. §7(" + ItemFactory.formatNumber(current) + "/" + ItemFactory.formatNumber(required) + ")");
            return true;
        }
        try {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(plugin.getItemFactory().create(tool));
            if (!overflow.isEmpty()) {
                for (ItemStack stack : overflow.values()) player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
            player.sendMessage("§aItem récupéré : §f" + tool.configKey());
        } catch (Throwable throwable) {
            plugin.getLogger().severe("Could not give showcase item " + tool + ": " + throwable);
            throwable.printStackTrace();
            player.sendMessage("§cErreur en créant " + tool.configKey() + ". Regarde la console.");
        }
        return true;
    }

    public boolean save() { return save(false); }

    private boolean save(boolean explicitClear) {
        YamlConfiguration yaml = new YamlConfiguration();
        // Entries for unloaded worlds MUST survive a re-save from a different world.
        for (Map.Entry<String,Map<String,Object>> entry: deferred.entrySet()) {
            for (Map.Entry<String,Object> field:entry.getValue().entrySet())
                yaml.set("showcases."+entry.getKey()+"."+field.getKey(),field.getValue());
        }
        for (ShowcaseDefinition def : definitions.values()) {
            String path = "showcases." + def.id();
            Location loc = def.location();
            yaml.set(path + ".tool", def.tool().name());
            yaml.set(path + ".world", loc.getWorld().getName());
            yaml.set(path + ".x", loc.getX());
            yaml.set(path + ".y", loc.getY());
            yaml.set(path + ".z", loc.getZ());
            yaml.set(path + ".yaw", loc.getYaw());
            yaml.set(path + ".pitch", loc.getPitch());
            yaml.set(path + ".size", def.size());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            if(!explicitClear && definitions.isEmpty() && deferred.isEmpty() && file.isFile()){
                ConfigurationSection existing=YamlConfiguration.loadConfiguration(file).getConfigurationSection("showcases");
                if((existing!=null && !existing.getKeys(false).isEmpty()) || (existing==null && file.length()>0)){
                    plugin.getLogger().severe("Refusing to erase all saved showcases. Use explicit /mb showcase clear if intended.");
                    return false;
                }
            }
            File tmp = new File(file.getParentFile(),file.getName()+".tmp");
            yaml.save(tmp);
            if(file.exists() && file.length()>0) {
                String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
                Files.copy(file.toPath(),new File(file.getParentFile(),file.getName()+".bak-"+stamp).toPath());
            }
            try {
                Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            } catch(java.nio.file.AtomicMoveNotSupportedException notAtomic) {
                Files.move(tmp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not safely save showcases.yml: " + ex.getMessage());
            return false;
        }
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("showcases");
        if (root == null) {
            if (file.length()>0) plugin.getLogger().warning("showcases.yml has no 'showcases:' root; it will NOT be overwritten.");
            return;
        }
        int max = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection section=yaml.getConfigurationSection("showcases."+id);
            if(section==null)continue;
            Map<String,Object> snapshot=new LinkedHashMap<>(section.getValues(false));
            if(id.startsWith("sc-")) {
                try{max=Math.max(max,Integer.parseInt(id.substring(3)));}catch(NumberFormatException ignored){}
            }
            String path = "showcases." + id;
            try {
                ToolId tool = ToolId.valueOf(yaml.getString(path + ".tool", "SUPER_PICKAXE"));
                World world = Bukkit.getWorld(yaml.getString(path + ".world", ""));
                if (world == null) {
                    deferred.put(id,snapshot);
                    plugin.getLogger().warning("Deferred showcase " + id + ": world not loaded. YAML definition preserved.");
                    continue;
                }
                Location loc = new Location(world,
                        yaml.getDouble(path + ".x"), yaml.getDouble(path + ".y"), yaml.getDouble(path + ".z"),
                        (float) yaml.getDouble(path + ".yaw"), (float) yaml.getDouble(path + ".pitch"));
                definitions.put(id, new ShowcaseDefinition(id, tool, loc, clampSize(yaml.getDouble(path + ".size", 1.0))));
                if (id.startsWith("sc-")) {
                    try { max = Math.max(max, Integer.parseInt(id.substring(3))); } catch (NumberFormatException ignored) { }
                }
            } catch (IllegalArgumentException ex) {
                deferred.put(id,snapshot);
                plugin.getLogger().warning("Deferred invalid showcase " + id + ": " + ex.getMessage()+". YAML definition preserved.");
            }
        }
        nextId = Math.max(1, max + 1);
    }

    private void spawn(ShowcaseDefinition def) {
        Location anchor = def.location().clone();
        World world = anchor.getWorld();
        if (world == null) return;
        Chunk chunk=anchor.getChunk();
        chunk.load();
        for(Entity prior:chunk.getEntities()){
            if(def.id().equals(prior.getPersistentDataContainer().get(showcaseIdKey,PersistentDataType.STRING)))
                prior.remove();
        }

        float size=def.size();
        double lift=Math.max(0,size-1.0)*0.45;
        Location itemLoc = anchor.clone().add(0, plugin.getConfig().getDouble("showcase.item-height", 1.30)+lift, 0);
        ItemDisplay item = world.spawn(itemLoc, ItemDisplay.class);
        item.setItemStack(plugin.getItemFactory().create(def.tool()));
        item.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        var transform=item.getTransformation();
        transform.getScale().set(size,size,size);
        item.setTransformation(transform);
        item.setBillboard(Display.Billboard.FIXED);
        item.setGlowing(plugin.getConfig().getBoolean("showcase.glowing", true));
        item.setViewRange((float) plugin.getConfig().getDouble("showcase.view-range", 48.0));
        tag(item, def);

        Location textLoc = anchor.clone().add(0, plugin.getConfig().getDouble("showcase.text-height", 2.35)+Math.max(0,size-1.0)*0.9, 0);
        TextDisplay text = world.spawn(textLoc, TextDisplay.class);
        long unlock = plugin.getItemFactory().unlockAt(def.tool());
        text.setText(plugin.getItemFactory().displayName(def.tool()) + "\n§7Déblocage: §e" + ItemFactory.formatNumber(unlock) + " blocs");
        text.setBillboard(Display.Billboard.CENTER);
        text.setSeeThrough(true);
        text.setShadowed(true);
        text.setViewRange((float) plugin.getConfig().getDouble("showcase.view-range", 48.0));
        tag(text, def);

        Location interactionLoc = anchor.clone().add(0, 1.25, 0);
        Interaction interaction = world.spawn(interactionLoc, Interaction.class);
        interaction.setInteractionWidth(Math.min(16f,Math.max(1.4f,size*1.2f)));
        interaction.setInteractionHeight(Math.min(20f,Math.max(2.6f,size*1.65f)));
        interaction.setResponsive(true);
        tag(interaction, def);

        spawned.put(def.id(), new DisplayBundle(item, text, interaction));
    }

    private void tag(Entity entity, ShowcaseDefinition def) {
        entity.getPersistentDataContainer().set(showcaseIdKey, PersistentDataType.STRING, def.id());
        entity.getPersistentDataContainer().set(showcaseToolKey, PersistentDataType.STRING, def.tool().name());
        entity.setPersistent(true);
    }

    private void removeSpawnedEntities() {
        // Never delete unknown tagged displays when YAML wasn't loaded; they might
        // be the user's only surviving evidence after a corrupt/missing data file.
        Set<String> known=new HashSet<>(definitions.keySet());
        known.addAll(spawned.keySet());
        for (DisplayBundle bundle : spawned.values()) bundle.remove();
        spawned.clear();
        if(known.isEmpty())return;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                String id=entity.getPersistentDataContainer().get(showcaseIdKey,PersistentDataType.STRING);
                if(id!=null && known.contains(id)) entity.remove();
            }
        }
    }

    private void startAnimation() {
        if (!plugin.getConfig().getBoolean("showcase.spin.enabled", true)) return;
        long interval = Math.max(1L, plugin.getConfig().getLong("showcase.spin.interval-ticks", 2L));
        float degrees = (float) plugin.getConfig().getDouble("showcase.spin.degrees-per-step", 4.0);
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (DisplayBundle bundle : spawned.values()) {
                if (!bundle.item().isValid()) continue;
                Location loc = bundle.item().getLocation();
                bundle.item().setRotation(loc.getYaw() + degrees, 0f);
            }
        }, interval, interval);
    }

    private void stopAnimation() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }

    private Vector flatDirection(Location location) {
        Vector vector = location.getDirection();
        vector.setY(0);
        if (vector.lengthSquared() < 0.0001) return new Vector(0, 0, 1);
        return vector;
    }

    private float yawFacing(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private String fmt(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private record ShowcaseDefinition(String id, ToolId tool, Location location, float size) { }

    private record DisplayBundle(ItemDisplay item, TextDisplay text, Interaction interaction) {
        void remove() {
            if (item != null && item.isValid()) item.remove();
            if (text != null && text.isValid()) text.remove();
            if (interaction != null && interaction.isValid()) interaction.remove();
        }
    }
}
