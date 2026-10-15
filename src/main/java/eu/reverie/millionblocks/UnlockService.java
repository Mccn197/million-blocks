package eu.reverie.millionblocks;

import java.util.*;

public final class UnlockService {
    private final MillionBlocksPlugin plugin;
    public UnlockService(MillionBlocksPlugin plugin) { this.plugin = plugin; }
    public record Stage(ToolId item, long previous, long threshold, long count, int percentage) {
        public String id(){return item == null ? "complete" : item.configKey();}
        public String itemText(){return item == null ? "Tous les items débloqués" : pluginFreeName(item);}
        private static String pluginFreeName(ToolId id) {
            return switch(id) {
                case SUPER_PICKAXE -> "Super Pickaxe"; case MINING_HAMMER -> "Mining Hammer";
                case CREEPER_MINER -> "Creeper Miner"; case TNT_LAUNCHER -> "TNT Launcher";
                case DRILL -> "Mining Drill"; case LASER -> "Mining Laser";
                case METEOR -> "Meteor Strike"; case WORLD_EATER -> "World Eater";
                default -> "";
            };
        }
        public long currentStage(){return Math.max(0,Math.min(threshold-previous,count-previous));}
        public long stageSize(){return Math.max(1,threshold-previous);}
    }
    public Stage next(long count) {
        List<ToolId> ordered = new ArrayList<>(ToolId.playable());
        ordered.sort(Comparator.comparingLong(id -> plugin.getItemFactory().unlockAt(id)));
        long previous=0;
        for (ToolId id : ordered) {
            long threshold=plugin.getItemFactory().unlockAt(id);
            if (threshold > count) {
                long size=Math.max(1, threshold - previous);
                int percentage=(int)Math.max(0,Math.min(100,Math.floor(100d*(count-previous)/size)));
                return new Stage(id, previous, threshold, count, percentage);
            }
            previous=Math.max(previous,threshold);
        }
        return new Stage(null,previous,previous,count,100);
    }
    public List<ToolId> reached(long before, long after) {
        if(after<=before)return List.of();
        List<ToolId> found=new ArrayList<>();
        for(ToolId id: ToolId.playable()){
            long t=plugin.getItemFactory().unlockAt(id);
            if(t>before && t<=after)found.add(id);
        }
        found.sort(Comparator.comparingLong(id->plugin.getItemFactory().unlockAt(id)));
        return found;
    }
}
