package eu.reverie.millionblocks;

import org.bukkit.Material;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ItemFactory {
    private final MillionBlocksPlugin plugin;
    private final NamespacedKey toolKey;
    private final NamespacedKey travelKey;

    public ItemFactory(MillionBlocksPlugin plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "tool-id");
        this.travelKey = new NamespacedKey(plugin, "travel-item");
    }

    public ItemStack create(ToolId id) {
        if (id == ToolId.SHOP) {
            ItemStack stack = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.legacy("<gold><bold>Boutique</bold></gold>"));
                meta.setLore(List.of(plugin.legacy("<gray>Clic droit pour ouvrir la boutique.</gray>")));
                meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, id.name());
                stack.setItemMeta(meta);
            }
            return stack;
        }

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("items." + id.configKey());
        String materialName = section == null ? "STONE" : section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName == null ? "STONE" : materialName);
        if (material == null || material.isAir()) material = Material.STONE;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String configuredName = section == null ? id.name() : section.getString("name", id.name());
            meta.setDisplayName(plugin.legacy(configuredName));
            List<String> lore = new ArrayList<>();
            if (section != null) {
                for (String line : section.getStringList("lore")) lore.add(plugin.legacy(line));
            }
            long unlock = unlockAt(id);
            if (unlock > 0) lore.add(plugin.legacy("<dark_gray>Déblocage : <yellow>" + formatNumber(unlock) + " blocs</yellow></dark_gray>"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, id.name());
            stack.setItemMeta(meta);
        }
        // Tell the vanilla client these custom weapons are usable even when aiming into AIR.
        // For LASER this also provides a real hold/release signal (Paper PlayerStopUsingItemEvent).
        if (id == ToolId.LASER || id == ToolId.CREEPER_MINER || id == ToolId.TNT_LAUNCHER
            || id == ToolId.DRILL || id == ToolId.METEOR || id == ToolId.WORLD_EATER) {
            stack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(3600.0f)
                .animation(ItemUseAnimation.NONE)
                .hasConsumeParticles(false));
        }
        return stack;
    }

    public ToolId identify(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try { return ToolId.valueOf(raw); } catch (IllegalArgumentException ex) { return null; }
    }

    public void ensureShopItem(Player player) {
        int slot = Math.max(0, Math.min(8, plugin.getConfig().getInt("challenge.shop-item-slot", 8)));
        ItemStack current = player.getInventory().getItem(slot);
        if (identify(current) != ToolId.SHOP) player.getInventory().setItem(slot, create(ToolId.SHOP));
    }

    public ItemStack createTravelPortal() {
        String materialName = plugin.getConfig().getString("world-system.portal-item.material", "ENDER_EYE");
        Material material = Material.matchMaterial(materialName == null ? "ENDER_EYE" : materialName);
        if (material == null || material.isAir()) material = Material.ENDER_EYE;
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.legacy("<green><bold>Miner Shop Portal</bold></green> <dark_gray>•</dark_gray> <aqua>V2</aqua>"));
            meta.setLore(List.of(
                plugin.legacy("<gray>Clic droit sur le sol : construire un portail animé.</gray>"),
                plugin.legacy("<gray>Clic droit sur le portail : voyager au hub.</gray>"),
                plugin.legacy("<gray>Clic gauche sur le portail : le retirer.</gray>"),
                plugin.legacy("<yellow>Objet illimité : pas consommé à la pose.</yellow>")));
            meta.getPersistentDataContainer().set(travelKey, PersistentDataType.STRING, "WORLD_PORTAL_V2");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public boolean isTravelPortal(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        if(meta==null)return false;
        String tag=meta.getPersistentDataContainer().get(travelKey,PersistentDataType.STRING);
        return "WORLD_PORTAL".equals(tag)||"WORLD_PORTAL_V2".equals(tag);
    }

    public String portalVersion(ItemStack stack) {
        if(!isTravelPortal(stack))return "AUCUN";
        String v=stack.getItemMeta().getPersistentDataContainer().get(travelKey,PersistentDataType.STRING);
        return "WORLD_PORTAL_V2".equals(v)?"V2 (portail posé, animé)":"V1 legacy (mise à jour nécessaire)";
    }

    /** Automatically convert old inventory markers to the present furniture item. */
    public int refreshTravelPortals(Player player) {
        int replaced=0;
        for(int slot=0;slot<player.getInventory().getSize();slot++){
            ItemStack stack=player.getInventory().getItem(slot);
            if(isTravelPortal(stack) && !portalVersion(stack).startsWith("V2")){
                player.getInventory().setItem(slot,createTravelPortal());
                replaced++;
            }
        }
        return replaced;
    }

    public void ensureTravelPortal(Player player) {
        refreshTravelPortals(player);
        for (ItemStack stack: player.getInventory().getContents()) if (isTravelPortal(stack)) return;
        int slot = Math.max(0, Math.min(8, plugin.getConfig().getInt("world-system.portal-item.slot", 7)));
        if (player.getInventory().getItem(slot) == null || player.getInventory().getItem(slot).getType().isAir())
            player.getInventory().setItem(slot, createTravelPortal());
        else player.getInventory().addItem(createTravelPortal());
    }

    public long unlockAt(ToolId id) {
        if (id == ToolId.SHOP) return 0L;
        return Math.max(0L, plugin.getConfig().getLong("items." + id.configKey() + ".unlock-at", 0L));
    }

    public String displayName(ToolId id) {
        if (id == ToolId.SHOP) return "Boutique";
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("items." + id.configKey());
        return plugin.legacy(section == null ? id.name() : section.getString("name", id.name()));
    }

    public static String formatNumber(long value) {
        return String.format(java.util.Locale.US, "%,d", value).replace(',', ' ');
    }
}
