package eu.reverie.millionblocks;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public final class MillionBlocksCommand implements TabExecutor {
    private final MillionBlocksPlugin plugin;
    public MillionBlocksCommand(MillionBlocksPlugin plugin){this.plugin=plugin;}
    private static final List<String> ROOT=List.of("help","info","laser","weapon","progress","give","world","survival","hub","portal","hud","showcase","victory","arena","reload");
    @Override public boolean onCommand(CommandSender sender,Command command,String alias,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Use /mb from an in-game OP account.");return true;}
        try{
            if(args.length==0||args[0].equalsIgnoreCase("help")){help(p);return true;}
            switch(args[0].toLowerCase(Locale.ROOT)){
                case "info" -> {p.sendMessage("§6MillionBlocks "+plugin.getDescription().getVersion()+" §7/ §e"+ItemFactory.formatNumber(plugin.getProgressService().get(p))+" / "+ItemFactory.formatNumber(plugin.getTargetBlocks()));
                    p.sendMessage("§7Hub: "+plugin.getWorldService().hubName()+" [locked="+plugin.getWorldService().isProtectedHub(org.bukkit.Bukkit.getWorld(plugin.getWorldService().hubName()))+"]");
                    p.sendMessage("§7Portails: "+plugin.getPortalService().count()+" | Showcase: "+plugin.getShowcaseService().ids().size()+" | Job: "+plugin.getSchematicService().status());}
                case "progress" -> progress(p,args);
                case "give" -> give(p,args);
                case "world" -> world(p,args);
                case "survival" -> survival(p,args);
                case "hub" -> hub(p,args);
                case "portal" -> portal(p,args);
                case "hud" -> hud(p,args);
                case "laser" -> laser(p,args);
                case "weapon" -> weapon(p,args);
                case "showcase" -> showcase(p,args);
                case "victory" -> plugin.triggerVictory(p,true);
                case "arena" -> {require(args,2,"/mb arena reset");int blocks=plugin.getArenaService().reset();p.sendMessage("§aArena reset: "+blocks);}
                case "reload" -> reload(p,args);
                default -> help(p);
            }
        }catch(Exception ex){p.sendMessage("§cErreur: "+ex.getMessage());plugin.getLogger().warning("/mb error: "+ex);}
        return true;
    }
    private void progress(Player p,String[] a){
        String sub=a.length>=2?a[1]:"get";
        switch(sub){case "get" -> {}case "set" ->{require(a,3,"/mb progress set <n>");plugin.setProgress(p,Long.parseLong(a[2]));}
            case "add" ->{require(a,3,"/mb progress add <n>");plugin.addProgress(p,Long.parseLong(a[2]));}case "reset" -> plugin.resetProgress(p);
            default -> throw new IllegalArgumentException("get|set|add|reset");}
        p.sendMessage("§eProgress: §f"+ItemFactory.formatNumber(plugin.getProgressService().get(p)));
    }
    private void give(Player p,String[] a){require(a,2,"/mb give <all|portal|tool>");
        if(a[1].equalsIgnoreCase("all")){
            int n=0;List<String> failures=new ArrayList<>();for(ToolId id:ToolId.playable()){
                try{giveStack(p,plugin.getItemFactory().create(id));n++;}catch(Exception ex){failures.add(id.configKey());plugin.getLogger().warning("give "+id+": "+ex);}}
            p.sendMessage("§aItems: "+n+"/"+ToolId.playable().size()+" §c"+String.join(", ",failures));return;
        }
        if(a[1].equalsIgnoreCase("portal"))giveStack(p,plugin.getItemFactory().createTravelPortal());
        else {ToolId id=ToolId.parse(a[1]);if(id==ToolId.SHOP)throw new IllegalArgumentException("Boutique par item supprimée.");giveStack(p,plugin.getItemFactory().create(id));}
        p.sendMessage("§aItem reçu.");
    }
    private void giveStack(Player p,ItemStack item){Map<Integer,ItemStack> overflow=p.getInventory().addItem(item);
        for(ItemStack remain:overflow.values())p.getWorld().dropItemNaturally(p.getLocation(),remain);
    }
    private void world(Player p,String[] a){
        WorldService service=plugin.getWorldService();
        String cmd=a.length>1?a[1].toLowerCase(Locale.ROOT):"status";
        switch(cmd){
            case "mining","hub" -> service.teleportHub(p);
            case "survival" -> service.teleportSurvival(p);
            case "status" ->p.sendMessage(service.destinations());
            case "set" ->{
                require(a,4,"/mb world set hub|survival here");
                if(!a[3].equalsIgnoreCase("here"))throw new IllegalArgumentException("Utilise 'here' depuis le bon monde.");
                if(!p.hasPermission("millionblocks.admin"))throw new IllegalArgumentException("Permission admin requise.");
                if(a[2].equalsIgnoreCase("hub"))service.selectHubHere(p);
                else if(a[2].equalsIgnoreCase("survival"))service.selectSurvivalHere(p);
                else throw new IllegalArgumentException("hub|survival");
                p.sendMessage("§aDestinations mises à jour: "+service.destinations());
            }
            default ->throw new IllegalArgumentException("status|mining|survival|set hub here|set survival here");
        }
    }
    private void survival(Player p,String[] a) {
        String op=a.length>=2?a[1]:"status";
        WorldService w=plugin.getWorldService();
        switch (op.toLowerCase(Locale.ROOT)) {
            case "status" -> p.sendMessage("§eSurvival active : §f"+w.survivalName()+" §7| §eCandidate : §f"
                        +w.candidateSurvivalName()+" §7| §eSeed candidate : §f"
                        +plugin.getConfig().getLong("world-system.survival-next.seed",5133991228066333L));
            case "prepare" -> {org.bukkit.World nw=w.prepareNewSurvival();
                p.sendMessage("§aNouvelle Survival préparée : "+nw.getName()+" §7(l'ancien monde est intact).");}
            case "use" -> {require(a,3,"/mb survival use new|old");
                if(!a[2].equalsIgnoreCase("new")&&!a[2].equalsIgnoreCase("old"))
                    throw new IllegalArgumentException("new|old");
                w.chooseSurvival(a[2].equalsIgnoreCase("new"));
                p.sendMessage("§aSurvival active : "+w.survivalName()+" — /mb world survival pour y aller.");}
            default -> throw new IllegalArgumentException("status|prepare|use new|old");
        }
    }
    private void hub(Player p,String[] a) throws java.io.IOException {require(a,2,"/mb hub lock|edit|sky|island|schem|spawn");WorldService w=plugin.getWorldService();
        switch(a[1].toLowerCase(Locale.ROOT)){
            case "lock" -> {String value=a.length>=3?a[2]:"status";
                if(value.equals("status")){p.sendMessage("§eHub lock = "+w.isProtectedHub(org.bukkit.Bukkit.getWorld(w.hubName())));return;}
                boolean locked=switch(value){case "on" ->true;case "off"->false;case "toggle"->!plugin.getConfig().getBoolean("world-system.mining-spawn.protected",true);
                    default ->throw new IllegalArgumentException("/mb hub lock on|off|toggle|status");};w.setLock(locked);p.sendMessage("§aHub lock = "+locked);
            }
            case "edit" -> {require(a,3,"/mb hub edit on|off");w.toggleEditor(p,a[2].equalsIgnoreCase("on"));p.sendMessage("§aBypass admin = "+w.isEditor(p));}
            case "sky" -> {require(a,3,"/mb hub sky create");if(!a[2].equalsIgnoreCase("create"))throw new IllegalArgumentException("/mb hub sky create");
                org.bukkit.World world=w.createSkyWorld();p.sendMessage("§aHub sky: "+world.getName()+" §7Pas de TP avant construction. /mb hub island create 55 confirm");}
            case "island" -> {require(a,5,"/mb hub island create <radius> confirm");if(!a[2].equalsIgnoreCase("create")||!a[4].equalsIgnoreCase("confirm"))throw new IllegalArgumentException("/mb hub island create 55 confirm");
                plugin.getSchematicService().island(p,Integer.parseInt(a[3]));}
            case "schem" -> {require(a,3,"/mb hub schem info|paste confirm|cancel");switch(a[2].toLowerCase(Locale.ROOT)){
                case "info" -> plugin.getSchematicService().info(p);
                case "paste" ->{require(a,4,"/mb hub schem paste confirm");if(!a[3].equalsIgnoreCase("confirm"))throw new IllegalArgumentException("Ajoute confirm pour coller des blocs dans le monde actif.");plugin.getSchematicService().paste(p);}
                case "cancel" ->{plugin.getSchematicService().cancel();p.sendMessage("§eJob annulé (blocs déjà placés conservés).");}
                default ->throw new IllegalArgumentException("info|paste confirm|cancel");}}
            case "spawn" ->{require(a,3,"/mb hub spawn set");if(!a[2].equalsIgnoreCase("set"))throw new IllegalArgumentException("/mb hub spawn set");w.setHubSpawn(p);p.sendMessage("§aSpawn du hub enregistré; aucun bloc modifié.");}
            case "rebuild" -> p.sendMessage("§cLa commande rebuild a été désactivée pour préserver tes constructions.");
            default ->throw new IllegalArgumentException("lock|edit|sky|island|schem|spawn");
        }
    }
    private void portal(Player p,String[] a){String op=a.length>=2?a[1]:"give";PortalService service=plugin.getPortalService();
        switch(op.toLowerCase(Locale.ROOT)){
            case "give" ->{giveStack(p,plugin.getItemFactory().createTravelPortal());
                 p.sendMessage("§aMiner Shop Portal V2 reçu. §7Clic droit sur le sol pour le poser, illimité.");}
            case "refresh" ->{int n=plugin.getItemFactory().refreshTravelPortals(p);
                 p.sendMessage("§aPortails V1 remplacés dans ton inventaire: "+n+" §7(Tu peux /mb portal give).");}
            case "inspect" ->p.sendMessage("§ePortail en main: §f"+
                  plugin.getItemFactory().portalVersion(p.getInventory().getItemInMainHand())+
                  " §7| Modèle: ItemDisplay custom / fallback industriel selon portal.use-custom-model");
            case "list" ->{p.sendMessage("§ePortails: "+service.count());service.list().stream().limit(30).forEach(p::sendMessage);}
            case "remove" ->{require(a,3,"/mb portal remove nearest [radius]|all confirm|<id>");String removed;
                if(a[2].equalsIgnoreCase("nearest"))removed=service.removeNearest(p,a.length>=4?Double.parseDouble(a[3]):8);
                else if(a[2].equalsIgnoreCase("all")){require(a,4,"/mb portal remove all confirm");if(!a[3].equalsIgnoreCase("confirm"))throw new IllegalArgumentException("Confirme la suppression totale.");p.sendMessage("§aPortails supprimés: "+service.clear());return;}
                else {removed=service.remove(a[2])?a[2]:null;}
                p.sendMessage(removed==null?"§cAucun portail trouvé":"§aPortail retiré: "+removed);
            }
            case "exit" ->{require(a,3,"/mb portal exit create");if(!a[2].equalsIgnoreCase("create")||!plugin.getWorldService().isHub(p.getWorld()))throw new IllegalArgumentException("Dans le hub: /mb portal exit create");service.placeExit(p,p.getTargetBlockExact(7));}
            case "reload" ->{service.load();p.sendMessage("§aPortails rechargés.");}
            default ->throw new IllegalArgumentException("give|refresh|inspect|list|remove|exit create|reload");
        }
    }
    private void weapon(Player p,String[] a) {
        require(a,2,"/mb weapon debug|test <weapon>");
        switch(a[1].toLowerCase(Locale.ROOT)){
            case "debug" ->p.sendMessage(plugin.getDestructionService().diagnostics(p));
            case "test" ->{
                require(a,3,"/mb weapon test creeper_miner|tnt_launcher|meteor|world_eater|drill|laser");
                plugin.getDestructionService().test(p,ToolId.parse(a[2]));
            }
            default ->throw new IllegalArgumentException("debug|test <weapon>");
        }
    }

    private void laser(Player p,String[] a) {
        require(a,2,"/mb laser place|mount|fire on|fire off|remove");
        switch(a[1].toLowerCase(Locale.ROOT)) {
            case "place" -> plugin.getCinematicService().placeMachine(p);
            case "mount" -> plugin.getCinematicService().mount(p);
            case "remove" -> plugin.getCinematicService().removeMachine(p);
            case "fire" -> {
                require(a,3,"/mb laser fire on|off");
                if(a[2].equalsIgnoreCase("on"))plugin.getDestructionService().startMountedLaser(p);
                else if(a[2].equalsIgnoreCase("off"))plugin.getDestructionService().stopLaser(p);
                else throw new IllegalArgumentException("on|off");
            }
            default -> throw new IllegalArgumentException("place|mount|fire on|fire off|remove");
        }
    }
    private void hud(Player p,String[] a){HudService h=plugin.getHudService();String op=a.length>=2?a[1]:"refresh";
        switch(op.toLowerCase(Locale.ROOT)){
            case "show" ->h.show(p);case "hide"->h.hide(p);case "refresh"->h.update(p);
            case "debug" ->{h.update(p);p.sendMessage(h.diagnostics(p));}
            case "mode" ->{require(a,3,"/mb hud mode betterhud|custom|bossbar|off");h.setMode(a[2]);}
            case "toggle" ->{if(a.length==2)p.sendMessage(h.toggle(p)?"§aHUD visible":"§7HUD masqué");else p.sendMessage("§e"+a[2]+" = "+h.toggleOption(a[2]));}
            case "pack" ->{require(a,3,"/mb hud pack on|off");plugin.getConfig().set("hud.use-resource-pack-glyphs",a[2].equalsIgnoreCase("on"));plugin.saveConfig();h.updateAll();}
            case "multiplier" ->{require(a,3,"/mb hud multiplier <n>");plugin.getConfig().set("hud.multiplier",parseMultiplier(a[2]));plugin.saveConfig();h.updateAll();}
            case "reload" ->h.reload();default ->throw new IllegalArgumentException("mode|show|hide|toggle|pack|multiplier|reload|debug");
        }
        if(!op.equals("toggle"))p.sendMessage("§aHUD: "+h.mode());
    }
    private void showcase(Player p,String[] a){require(a,2,"/mb showcase circle|line|place|move|remove|clear|list|reload|save|respawn|repair");
        ShowcaseService s=plugin.getShowcaseService();switch(a[1]){
            case "circle" ->p.sendMessage("§aCercle: "+s.createCircle(p,a.length>=3?Double.parseDouble(a[2]):8.0));
            case "line" ->p.sendMessage("§aLigne: "+s.createLine(p,a.length>=3?Double.parseDouble(a[2]):3.0));
            case "place" ->{require(a,3,"/mb showcase place <tool>");p.sendMessage("§aShowcase: "+s.place(p,ToolId.parse(a[2])));}
            case "size" ->{
                require(a,4,"/mb showcase size <id|nearest|all> <0.25..12>");
                double scale=Double.parseDouble(a[3]);
                List<String> resized=s.resize(a[2],p,scale);
                if(resized.isEmpty()){p.sendMessage("§cAucun showcase trouvé (vérifie le monde et l'ID).");break;}
                if(!s.save())p.sendMessage("§cTaille appliquée en jeu mais erreur de sauvegarde YAML.");
                else p.sendMessage("§aTaille §e×"+scale+" §aenregistrée pour "+resized.size()+" stand(s): §7"+resized);
            }
            case "move" ->{require(a,3,"/mb showcase move <id|nearest>");
                String moved=s.move(a[2],p);
                if(moved==null){p.sendMessage("§cAucun showcase à déplacer.");break;}
                boolean saved=s.save();
                p.sendMessage((saved?"§aDéplacé et sauvegardé: ":"§cDéplacé mais sauvegarde échouée: ")+s.fullDetails(moved));}
            case "remove" ->{require(a,3,"/mb showcase remove nearest [radius]|id");p.sendMessage("§aSupprimé: "+(a[2].equals("nearest")?s.removeNearest(p,a.length>=4?Double.parseDouble(a[3]):8.0):s.remove(a[2])));}
            case "clear" ->p.sendMessage("§aSupprimés: "+s.clear());
            case "list" ->{p.sendMessage("§eShowcases: "+s.ids().size()+" (dont "+s.deferredCount()+" en attente de monde)");s.describe().stream().limit(30).forEach(p::sendMessage);}
            case "diagnose" ->p.sendMessage(s.diagnostic());
            case "save" ->{
                boolean ok=s.save();
                if(!ok){p.sendMessage("§cÉchec sauvegarde showcases.yml — consulte la console.");break;}
                p.sendMessage("§a✔ Sauvegarde réussie : §f"+s.ids().size()+" showcases §7→ §bplugins/MillionBlocks/showcases.yml");
                p.sendMessage("§7Coordonnées enregistrées (monde, X/Y/Z et yaw) :");
                for(String id:s.ids())p.sendMessage(" §8• "+s.fullDetails(id));
            }
            case "reload" ->{s.reload();p.sendMessage("§aShowcases rechargés : "+s.ids().size());}
            case "respawn" ->{s.respawnAll();p.sendMessage("§aShowcases reconstruits : "+s.ids().size());}
            case "repair" ->p.sendMessage("§aShowcases réparés à proximité : "+s.repairNear(p));
            default ->throw new IllegalArgumentException("circle|line|place|remove|clear|list|reload|save|respawn");
        }
    }
    private void reload(Player p,String[] a){String op=a.length>=2?a[1]:"all";
        switch(op){case "all" ->plugin.reloadEverything();case "config"->{plugin.reloadConfig();plugin.refreshRuntimeConfig();plugin.getHudService().reload();}
            case "showcases"->plugin.getShowcaseService().reload();case "portals"->plugin.getPortalService().load();case "hud"->plugin.getHudService().reload();
            default ->throw new IllegalArgumentException("all|config|showcases|portals|hud");}p.sendMessage("§aReload "+op);}
    private int parseMultiplier(String raw) {
        int n=Integer.parseInt(raw); if(n!=1 && n!=2)throw new IllegalArgumentException("Multiplicateur: 1|2"); return n;
    }
    private void require(String[] a,int n,String usage){if(a.length<n)throw new IllegalArgumentException(usage);}
    private void help(Player p){
        p.sendMessage("§6§lMillionBlocks V0.6.2 §7— commandes OP / TAB");
        p.sendMessage("§e/mb hub sky create §7— crée le monde céleste indépendant");
        p.sendMessage("§e/mb hub island create 55 confirm §7— socle rocheux flottant");
        p.sendMessage("§e/mb hub schem info|paste confirm|cancel §7— ton .schem");
        p.sendMessage("§e/mb hub spawn set §7— marque l'arrivée en sécurité");
        p.sendMessage("§e/mb hub lock on|off|toggle|status §7— protection");
        p.sendMessage("§e/mb hub edit on|off §7— bypass personnel");
        p.sendMessage("§e/mb showcase circle 8 §7— musée en cercle");
        p.sendMessage("§e/mb portal give|refresh|inspect|list|remove|exit create|reload");
        p.sendMessage("§e/mb hud mode betterhud|custom|bossbar|off §7— HUD");
        p.sendMessage("§e/mb hud toggle goal|percent|multiplier|icon");
        p.sendMessage("§e/mb hud pack on|off §7— glyphes resource pack");
        p.sendMessage("§e/mb laser place|mount|fire on|fire off|remove §7— REDLINE pilotable");
        p.sendMessage("§e/mb weapon debug|test creeper_miner §7— test des tirs à distance");
        p.sendMessage("§e/mb hud debug §7— diagnostic visibilité HUD");
        p.sendMessage("§e/mb give all §7/ §e/mb progress set 990000 §7/ §e/mb victory");
        p.sendMessage("§e/mb world status|set hub here|set survival here §7— destinations des portails");
        p.sendMessage("§e/mb showcase size nearest|sc-001|all 5 §7— modèles géants");
        p.sendMessage("§e/mb world mining|survival §7/ §e/mb reload all");
        p.sendMessage("§e/mb survival prepare|status|use new|use old §7— nouvelle seed sans effacer l'ancienne");
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String label,String[] a){
        if(a.length==1)return filter(ROOT,a[0]);String root=a[0].toLowerCase(Locale.ROOT);
        if(a.length==2)return filter(switch(root){
            case "laser"->List.of("place","mount","fire","remove");case "progress"->List.of("get","set","add","reset");case "give"->giveSuggestions();case "world"->List.of("mining","survival","status","set");case "survival"->List.of("prepare","use","status");
            case "hub"->List.of("lock","edit","sky","island","schem","spawn");case "portal"->List.of("give","refresh","inspect","list","remove","exit","reload");case "weapon"->List.of("debug","test");
            case "hud"->List.of("mode","show","hide","toggle","pack","multiplier","reload","refresh","debug");
            case "showcase"->List.of("line","circle","place","move","size","remove","clear","list","save","reload","respawn","repair","diagnose");
            case "arena"->List.of("reset");case "reload"->List.of("all","config","showcases","portals","hud");default->List.of();},a[1]);
        if(a.length==3){return filter(switch(root){
            case "hub"-> switch(a[1]){case "lock"->List.of("on","off","toggle","status");case "edit"->List.of("on","off");case "sky"->List.of("create");case "island"->List.of("create");case "schem"->List.of("info","paste","cancel");case "spawn"->List.of("set");default->List.of();};
            case "portal"->switch(a[1]){case "remove"->List.of("nearest","all");case "exit"->List.of("create");default->List.of();};
            case "hud"->switch(a[1]){case "mode"->List.of("betterhud","custom","bossbar","off");case "toggle"->List.of("goal","percent","multiplier","icon");case "pack"->List.of("on","off");case "multiplier"->List.of("1","2");default->List.of();};
            case "showcase"->switch(a[1]){case "place"->tools();case "remove"->plugin.getShowcaseService().ids();case "size"-> {
                List<String> l=new ArrayList<>(List.of("nearest","all"));l.addAll(plugin.getShowcaseService().ids());yield l;
            }case "move"-> {List<String> l=new ArrayList<>(List.of("nearest"));l.addAll(plugin.getShowcaseService().ids());yield l;}case "circle"->List.of("6","8","10","12");case "line"->List.of("2","3","4");default->List.of();};
            case "laser"->List.of("on","off");case "weapon"->a[1].equals("test")?tools():List.of();
            case "world"->a[1].equals("set")?List.of("hub","survival"):List.of();
            case "survival"->List.of("new","old");case "give"->tools();case "progress"->List.of("1000","325000","800000","990000","1000000");default->List.of();},a[2]);}
        if(a.length==4){if(root.equals("world")&&a[1].equals("set"))return filter(List.of("here"),a[3]);
            if(root.equals("showcase")&&a[1].equals("size"))return filter(List.of("0.5","1","2","3","5","8","12"),a[3]);
            if(root.equals("hub")&&a[1].equals("schem")&&a[2].equals("paste"))return filter(List.of("confirm"),a[3]);
            if(root.equals("hub")&&a[1].equals("island"))return filter(List.of("55","60","70","90"),a[3]);
            if(root.equals("portal")&&a[1].equals("remove")&&a[2].equals("all"))return filter(List.of("confirm"),a[3]);}
        if(a.length==5&&root.equals("hub")&&a[1].equals("island"))return filter(List.of("confirm"),a[4]);
        return List.of();
    }
    private List<String> tools(){return ToolId.playable().stream().map(ToolId::configKey).toList();}
    private List<String> giveSuggestions(){List<String> result=new ArrayList<>(List.of("all","portal"));result.addAll(tools());return result;}
    private List<String> filter(List<String> list,String prefix){return list.stream().filter(x->x.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).toList();}
}
