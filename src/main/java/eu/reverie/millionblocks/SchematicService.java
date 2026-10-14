package eu.reverie.millionblocks;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.io.*;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.zip.GZIPInputStream;

/** Imports Sponge/WorldEdit .schem v3 palettes, block states and block data.
 *  Changes are explicit and throttled on the primary thread; no file is automatically pasted.
 *  For complex block entities/biomes use FAWE instead (those are intentionally not imported).
 */
public final class SchematicService {
    private final MillionBlocksPlugin plugin;
    private BukkitTask job;
    private String status = "idle";
    public SchematicService(MillionBlocksPlugin plugin) {this.plugin=plugin;}
    public boolean busy() {return job!=null;}
    public String status(){return status;}
    public void cancel() {if(job!=null) {job.cancel();job=null;status="cancelled";}}
    public File file() {return new File(plugin.getDataFolder(),"import/RuinedColosseusV2.schem");}
    public void info(Player player) throws IOException {
        Map<String,Object> root = read(file());
        player.sendMessage("§eSchematic: §f"+root.get("Width")+"×"+root.get("Height")+"×"+root.get("Length")+" §7(Version "+root.get("Version")+")");
        player.sendMessage("§7Fichier: plugins/MillionBlocks/import/RuinedColosseusV2.schem");
        player.sendMessage("§7Paste sans air ni tile entities; WorldEdit conseillé si tu veux 100% fidélité.");
    }
    public void paste(Player player) throws IOException {
        if(busy()) throw new IllegalStateException("Travail en cours: "+status);
        World world = Bukkit.getWorld(plugin.getWorldService().hubName());
        if(world==null) throw new IllegalArgumentException("Hub non chargé.");
        Map<String,Object> data=read(file());
        int width = n(data,"Width"), height=n(data,"Height"),length=n(data,"Length");
        if(width<=0 || length<=0 || height<=0 || (long)width*length*height>2_000_000L) throw new IOException("Dimensions hors limite.");
        Object blocksObj = data.get("Blocks");
        if(!(blocksObj instanceof Map<?,?> blocks)) throw new IOException("NBT Blocks absent (schem v3 attendu)");
        Object paletteObj=blocks.get("Palette");
        if(!(paletteObj instanceof Map<?,?> palette)) throw new IOException("Palette absente");
        byte[] bytes=(byte[])blocks.get("Data");
        if(bytes==null) throw new IOException("BlockData absent");
        BlockData[] states = new BlockData[Math.max(1,palette.size())];
        for (var e:palette.entrySet()) {
            int id=((Number)e.getValue()).intValue();
            if(id<0 || id>=states.length) throw new IOException("Palette id invalide "+id);
            String raw=String.valueOf(e.getKey());
            if(raw.equals("minecraft:air") || raw.equals("minecraft:cave_air") || raw.equals("minecraft:void_air"))continue;
            try {states[id]=Bukkit.createBlockData(raw);}catch(IllegalArgumentException bad){
                String simple=raw.split("\\[")[0].replace("minecraft:","").toUpperCase(Locale.ROOT);
                Material m=Material.matchMaterial(simple);
                if(m!=null)states[id]=m.createBlockData();
            }
        }
        int volume=width*length*height;
        int[] ids=new int[volume];int off=0;
        for(int i=0;i<volume;i++) {
            int value=0,shift=0;
            while(true){if(off>=bytes.length)throw new IOException("Data NBT tronqué");int b=bytes[off++]&255;value|=(b&127)<<shift;if((b&128)==0)break;shift+=7;if(shift>28)throw new IOException("Varint invalide");}
            ids[i]=value;
        }
        int x0=plugin.getConfig().getInt("hub.import.center-x",0)-width/2;
        int y0=plugin.getConfig().getInt("hub.import.base-y",130);
        int z0=plugin.getConfig().getInt("hub.import.center-z",0)-length/2;
        if(y0<world.getMinHeight()+1 || y0+height>=world.getMaxHeight()-1) throw new IOException("Hauteur d'import impossible.");
        int budget=Math.max(200,Math.min(5000,plugin.getConfig().getInt("hub.blocks-per-tick",1200)));
        status="pasting "+volume+" cells";
        final int[] cursor={0},changed={0};
        job=Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int processed=0;
            while(cursor[0]<volume && processed++<budget){
                int idx=cursor[0]++;
                int id=ids[idx];if(id<0 || id>=states.length||states[id]==null)continue;
                int x=idx%width,z=(idx/width)%length,y=idx/(width*length);
                world.getBlockAt(x0+x,y0+y,z0+z).setBlockData(states[id],false);changed[0]++;
            }
            if(cursor[0]>=volume){cancel();status="paste complete ("+changed[0]+" blocks)";plugin.getWorldService().setAutoSpawn(world,plugin.getConfig().getInt("hub.import.center-x",0),plugin.getConfig().getInt("hub.import.center-z",0));player.sendMessage("§aImport terminé: §f"+changed[0]+" blocs. §eDéfinis le spawn avec /mb hub spawn set sur un sol sûr.");}
        },1,1);
        player.sendMessage("§eImport lancé: §f"+width+"×"+height+"×"+length+" §7(origine: "+x0+","+y0+","+z0+")");
    }
    /** Adds an underside to the floating colosseum. Never executes without the explicit command. */
    public void island(Player player,int radius) {
        if(busy())throw new IllegalStateException("Travail en cours: "+status);
        World world=Bukkit.getWorld(plugin.getWorldService().hubName());
        if(world==null)throw new IllegalArgumentException("Hub non chargé.");
        radius=Math.max(45,Math.min(110,radius));
        int floor=plugin.getConfig().getInt("hub.import.base-y",130)-1;
        int budget=Math.max(200,Math.min(5000,plugin.getConfig().getInt("hub.blocks-per-tick",1200)));
        int width=radius*2+1,volume=width*width*24, cx=plugin.getConfig().getInt("hub.import.center-x",0),cz=plugin.getConfig().getInt("hub.import.center-z",0);
        final int r=radius;final int[] pos={0},changed={0};
        status="island generation";
        job=Bukkit.getScheduler().runTaskTimer(plugin,()->{
            int processed=0;
            while(pos[0]<volume && processed++<budget){
                int idx=pos[0]++, x=idx%width-r, z=(idx/width)%width-r,depth=idx/(width*width);
                double dist=Math.sqrt(x*x+z*z),taper=r-depth*1.55 + Math.sin(x*.19)*1.8 + Math.cos(z*.17)*1.8;
                if(dist>taper || taper<1)continue;
                int y=floor-depth;
                if(!world.getBlockAt(cx+x,y,cz+z).getType().isAir())continue;
                Material material=depth==0?Material.MOSS_BLOCK:depth<4?Material.STONE:Material.DEEPSLATE;
                world.getBlockAt(cx+x,y,cz+z).setType(material,false);changed[0]++;
            }
            if(pos[0]>=volume){cancel();status="island complete ("+changed[0]+" blocks)";plugin.getWorldService().setAutoSpawn(world,cx,cz);player.sendMessage("§aÎle terminée: "+changed[0]+" blocs. Tu peux maintenant importer le .schem.");}
        },1,1);
        player.sendMessage("§eConstruction de l'île flottante (rayon "+radius+") — placer le schem ensuite.");
    }
    private static int n(Map<String,Object> map,String key){return ((Number)map.get(key)).intValue();}
    @SuppressWarnings("unchecked")
    private static Map<String,Object> read(File file) throws IOException {
        if(!file.isFile())throw new FileNotFoundException("Copie RuinedColosseusV2.schem dans plugins/MillionBlocks/import/");
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(file))))){
            if(in.readUnsignedByte()!=10)throw new IOException("NBT racine non-Compound");in.readUTF();
            Map<String,Object> root=(Map<String,Object>)readTag(in,10);
            Object wrapped=root.get("Schematic");return wrapped instanceof Map<?,?> m?(Map<String,Object>)m:root;
        }
    }
    private static Object readTag(DataInputStream in,int type) throws IOException {
        return switch(type){
            case 1 -> in.readByte();case 2 -> in.readShort();case 3 -> in.readInt();case 4 -> in.readLong();
            case 5 -> in.readFloat();case 6 -> in.readDouble();
            case 7 -> {int n=in.readInt();if(n<0||n>10_000_000)throw new IOException("NBT byte array too large");byte[] a=new byte[n];in.readFully(a);yield a;}
            case 8 -> in.readUTF();
            case 9 -> {int inner=in.readUnsignedByte(),count=in.readInt();if(count<0||count>2_000_000)throw new IOException("NBT list too large");List<Object> list=new ArrayList<>(Math.min(count,1024));for(int i=0;i<count;i++)list.add(readTag(in,inner));yield list;}
            case 10 -> {Map<String,Object> m=new LinkedHashMap<>();while(true){int tag=in.readUnsignedByte();if(tag==0)break;String name=in.readUTF();m.put(name,readTag(in,tag));}yield m;}
            case 11 -> {int n=in.readInt();if(n<0||n>2_000_000)throw new IOException("NBT int array too large");int[] a=new int[n];for(int i=0;i<n;i++)a[i]=in.readInt();yield a;}
            case 12 -> {int n=in.readInt();if(n<0||n>2_000_000)throw new IOException("NBT long array too large");long[] a=new long[n];for(int i=0;i<n;i++)a[i]=in.readLong();yield a;}
            default -> throw new IOException("NBT tag unsupported "+type);
        };
    }
}
