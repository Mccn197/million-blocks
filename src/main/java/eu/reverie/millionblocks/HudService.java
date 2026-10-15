package eu.reverie.millionblocks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

/** Persistent player HUD. Debug bossbar guarantees feedback even when actionbar is overwritten. */
public final class HudService {
    private final MillionBlocksPlugin plugin;
    private final Map<UUID,BossBar> bars = new HashMap<>();
    private final Set<UUID> hidden = new HashSet<>();
    private BukkitTask task;
    private final java.util.concurrent.ConcurrentMap<UUID,HudState> snapshots = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID,Long> minedAt = new HashMap<>();
    private final BetterHudBridge bridge;
    public HudState snapshot(UUID uuid) { return snapshots.getOrDefault(uuid, HudState.EMPTY); }
    public void mined(Player p) { minedAt.compute(p.getUniqueId(), (id, previous) -> HudState.animationStart(previous == null ? 0 : previous, System.nanoTime())); }
    public int multiplier() { return plugin.getConfig().getInt("hud.multiplier",1) == 2 ? 2 : 1; }
    public void connect() { bridge.connect(); }
    public void shutdown() { stop(); bridge.close(); snapshots.clear(); minedAt.clear(); }


    public HudService(MillionBlocksPlugin plugin) { this.plugin=plugin; this.bridge=new BetterHudBridge(plugin); }
    public void start() {
        stop();
        long ticks=Math.max(2,plugin.getConfig().getLong("hud.update-interval-ticks",10));
        task=Bukkit.getScheduler().runTaskTimer(plugin,this::updateAll,1,ticks);
    }
    public void reload(){start();}
    public void stop(){
        if(task!=null){task.cancel();task=null;}
        for(Player p:Bukkit.getOnlinePlayers())bridge.sync(p,false);
        for(BossBar b:bars.values())b.removeAll();
        bars.clear();
    }
    public boolean enabled(){return plugin.getConfig().getBoolean("hud.enabled",true);}
    private void enableGlobally(){
        if(!enabled()){
            plugin.getConfig().set("hud.enabled",true);
            plugin.saveConfig();
        }
    }
    public void show(Player p){
        enableGlobally();
        if(mode().equals("off")){
            plugin.getConfig().set("hud.mode","bossbar");
            plugin.saveConfig();
        }
        hidden.remove(p.getUniqueId());
        update(p);
    }
    public void hide(Player p){
        hidden.add(p.getUniqueId());
        remove(p);
        p.sendActionBar(Component.empty());
    }
    public boolean toggle(Player p){
        if(hidden.contains(p.getUniqueId())){show(p);return true;}
        hide(p);return false;
    }
    public void remove(Player p){
        bridge.sync(p,false);
        snapshots.remove(p.getUniqueId());
        BossBar b=bars.remove(p.getUniqueId());
        if(b!=null)b.removeAll();
    }
    public String mode(){return plugin.getConfig().getString("hud.mode","betterhud").toLowerCase(Locale.ROOT);}
    public boolean hidden(Player p){return hidden.contains(p.getUniqueId());}
    public void setMode(String name){
        name=name.toLowerCase(Locale.ROOT);
        if(!List.of("betterhud","custom","bossbar","off").contains(name))throw new IllegalArgumentException("Mode: betterhud|custom|bossbar|off");
        plugin.getConfig().set("hud.mode",name);
        if(!name.equals("off"))plugin.getConfig().set("hud.enabled",true);
        plugin.saveConfig();
        // Choosing a mode means 'show it now', not merely changing a hidden preference.
        hidden.clear();
        for(Player p:Bukkit.getOnlinePlayers())p.sendActionBar(Component.empty());
        reload();
        updateAll();
    }
    public boolean toggleOption(String key){
        if(!List.of("percent","goal","multiplier","icon").contains(key))throw new IllegalArgumentException("Option: percent|goal|multiplier|icon");
        String path="hud.show-"+key;
        boolean next=!plugin.getConfig().getBoolean(path,true);
        plugin.getConfig().set(path,next);
        plugin.saveConfig();
        updateAll();
        return next;
    }
    public String diagnostics(Player p){
        BossBar bar=bars.get(p.getUniqueId());
        return "§e"+bridge.diagnostics()+" §7| HUD enabled=§f"+enabled()+" §7mode=§f"+mode()+
            " §7hidden=§f"+hidden(p)+" §7task=§f"+(task!=null&&!task.isCancelled())+
            " §7bossbar=§f"+(bar!=null&&bar.getPlayers().contains(p)&&bar.isVisible())+
            " §7pack=§f"+plugin.getConfig().getBoolean("hud.use-resource-pack-glyphs",false)+
            " §7goal=§f"+plugin.getConfig().getBoolean("hud.show-goal",true)+
            " §7percent=§f"+plugin.getConfig().getBoolean("hud.show-percent",true);
    }
    public void updateAll(){for(Player player:Bukkit.getOnlinePlayers())update(player);}
    public void update(Player p){
        String m=mode();
        if(!enabled()||hidden(p)||m.equals("off")){
            remove(p);
            return;
        }
        long current=plugin.getProgressService().get(p), goal=plugin.getTargetBlocks();
        int percent=(int)Math.floor(Math.min(100d,100d*current/(double)goal));
        int mult=multiplier();
        boolean icon=plugin.getConfig().getBoolean("hud.show-icon",true);
        boolean showGoal=plugin.getConfig().getBoolean("hud.show-goal",true);
        boolean showPercent=plugin.getConfig().getBoolean("hud.show-percent",true);
        boolean showMult=plugin.getConfig().getBoolean("hud.show-multiplier",true);
        UnlockService.Stage stage=plugin.getUnlockService().next(current);
        snapshots.put(p.getUniqueId(),new HudState(current,goal,mult,true,showGoal,showPercent,showMult,icon,
            minedAt.getOrDefault(p.getUniqueId(),0L),stage.id(),stage.itemText(),stage.currentStage(),
            stage.stageSize(),stage.percentage()));
        boolean amber = bridge.sync(p,m.equals("betterhud"));
        if(m.equals("betterhud") && amber) {
            BossBar old=bars.remove(p.getUniqueId()); if(old!=null)old.removeAll();
            return;
        }
        String title=(icon?"§b⛏ ":"")+(showMult?"§6x"+mult+" §8│ ":"")+"§f"+ItemFactory.formatNumber(current)+
          (showGoal?" §8/ §f"+ItemFactory.formatNumber(goal):"")+(showPercent?" §e"+percent+"%":"");
        // During V0.4 tests custom is an ActionBar. Provide a configurable bossbar safety net
        // because chat and other plugins may obscure or replace the ActionBar packets.
        boolean barNeeded=(m.equals("betterhud") && plugin.getConfig().getBoolean("hud.custom-fallback-bossbar",true)) || m.equals("bossbar") || (m.equals("custom")&&plugin.getConfig().getBoolean("hud.custom-fallback-bossbar",true));
        if(barNeeded){
            BossBar bar=bars.computeIfAbsent(p.getUniqueId(),id->Bukkit.createBossBar("Million Blocks",BarColor.YELLOW,BarStyle.SEGMENTED_10));
            if(!bar.getPlayers().contains(p))bar.addPlayer(p);
            bar.setVisible(true);
            bar.setTitle(title);
            bar.setProgress(Math.max(0d,Math.min(1d,current/(double)goal)));
        }else { BossBar old=bars.remove(p.getUniqueId()); if(old!=null)old.removeAll(); }
        if(m.equals("custom")){
            boolean glyphs=plugin.getConfig().getBoolean("hud.use-resource-pack-glyphs",false);
            Component content=LegacyComponentSerializer.legacySection().deserialize(title);
            if(glyphs){
                Component sprite=Component.text("\uE100").font(net.kyori.adventure.key.Key.key("millionblocks:hud"));
                content=sprite.append(Component.space()).append(content);
            }
            p.sendActionBar(content);
        }else{
            p.sendActionBar(Component.empty());
        }
    }
}
