package eu.reverie.millionblocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class MillionBlocksPlugin extends JavaPlugin {
    private final Set<UUID> victoryTriggered = new HashSet<>();
    private ProgressService progressService;
    private ItemFactory itemFactory;
    private DestructionService destructionService;
    private DebrisService debrisService;
    private ArenaService arenaService;
    private WorldService worldService;
    private HudService hudService;
    private RemoteAimService aimService;
    private UnlockService unlockService;
    private ShowcaseService showcaseService;
    private PortalService portalService;
    private SchematicService schematicService;
    private String shopTitle;
    private CinematicService cinematicService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.progressService = new ProgressService(this);
        this.itemFactory = new ItemFactory(this);
        this.destructionService = new DestructionService(this);
        this.cinematicService = new CinematicService(this);
        this.debrisService = new DebrisService(this);
        this.arenaService = new ArenaService(this);
        this.worldService = new WorldService(this);
        this.aimService = new RemoteAimService(this);
        this.unlockService = new UnlockService(this);
        this.hudService = new HudService(this);
        this.showcaseService = new ShowcaseService(this);
        this.portalService = new PortalService(this);
        this.schematicService = new SchematicService(this);

        this.worldService.ensureWorlds();
        refreshRuntimeConfig();
        this.showcaseService.reload();
        this.portalService.load();
        this.hudService.connect();
        this.hudService.start();

        getServer().getPluginManager().registerEvents(new GameplayListener(this), this);
        MillionBlocksCommand command = new MillionBlocksCommand(this);
        if (getCommand("millionblocks") != null) {
            getCommand("millionblocks").setExecutor(command);
            getCommand("millionblocks").setTabCompleter(command);
        }
        getLogger().info("MillionBlocks enabled. Target=" + getTargetBlocks());
    }

    @Override
    public void onDisable() {
        if (destructionService != null) destructionService.stopAll();
        if (hudService != null) hudService.shutdown();
        if (cinematicService != null) cinematicService.shutdown();
        if (showcaseService != null) showcaseService.shutdown();
        if (portalService != null) portalService.shutdown();
        if (schematicService != null) schematicService.cancel();
        if (progressService != null) progressService.save();
    }

    public void reloadEverything() {
        reloadConfig();
        refreshRuntimeConfig();
        if (showcaseService != null) showcaseService.reload();
        if (portalService != null) portalService.load();
        if (hudService != null) {hudService.connect();hudService.reload();}
    }

    public void refreshRuntimeConfig() {
        this.shopTitle = legacy(getConfig().getString("shop.title", "<dark_gray>Block Destroyer Shop</dark_gray>"));
    }

    public void addProgress(Player player, long amount) {
        long before = progressService.get(player);
        long after = progressService.set(player, HudState.addSaturated(before, amount, hudService.multiplier()));
        if (amount > 0 && hudService != null) hudService.mined(player);
        if (hudService != null) hudService.update(player);
        var unlocked = unlockService.reached(before, after);
        if (!unlocked.isEmpty()) {
            ToolId newest=unlocked.get(unlocked.size()-1);
            player.sendTitle("§6§lNOUVEL ITEM DÉBLOQUÉ !", "§e"+ItemFactory.cleanDisplayName(newest), 10, 65, 20);
            player.sendMessage("§a✓ Débloqué : §e"+ItemFactory.cleanDisplayName(newest)+" §7— récupère-le au musée !");
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
        }
        long target = getTargetBlocks();
        if (before < target && after >= target) triggerVictory(player, false);
    }

    public void setProgress(Player player, long value) {
        progressService.set(player, value);
        if (hudService != null) hudService.update(player);
    }

    public void resetProgress(Player player) {
        progressService.reset(player);
        victoryTriggered.remove(player.getUniqueId());
        if (hudService != null) hudService.update(player);
    }

    public long getTargetBlocks() {
        return Math.max(1L, getConfig().getLong("challenge.target-blocks", 1_000_000L));
    }

    public void triggerVictory(Player player, boolean forced) {
        if (!forced && !victoryTriggered.add(player.getUniqueId())) return;
        if (forced) victoryTriggered.add(player.getUniqueId());
        player.sendTitle(
                legacy(getConfig().getString("victory.title", "<gold><bold>1,000,000 BLOCKS MINED!</bold></gold>")),
                legacy(getConfig().getString("victory.subtitle", "<yellow>Challenge complete</yellow>")),
                10, 70, 20
        );
        long delay = getConfig().getLong("victory.teleport-delay-ticks", 60L);
        getServer().getScheduler().runTaskLater(this, () -> {
            java.util.Optional<Location> configured = configuredLocation("locations.reward-room");
            if (configured.isPresent()) player.teleport(configured.get());
            else if (worldService != null) worldService.teleportReward(player);
        }, Math.max(0L, delay));
    }

    public void openShop(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, shopTitle);
        int slot = 9;
        for (ToolId id : ToolId.playable()) {
            inventory.setItem(slot++, itemFactory.create(id));
        }
        player.openInventory(inventory);
    }

    public boolean isShopTitle(String title) {
        return title != null && title.equals(shopTitle);
    }

    public java.util.Optional<Location> configuredLocation(String path) {
        if (!getConfig().getBoolean(path + ".enabled", false)) return java.util.Optional.empty();
        World world = Bukkit.getWorld(getConfig().getString(path + ".world", "world"));
        if (world == null) return java.util.Optional.empty();
        return java.util.Optional.of(new Location(world,
                getConfig().getDouble(path + ".x"),
                getConfig().getDouble(path + ".y"),
                getConfig().getDouble(path + ".z"),
                (float) getConfig().getDouble(path + ".yaw"),
                (float) getConfig().getDouble(path + ".pitch")));
    }

    public String legacy(String input) {
        if (input == null) return "";
        String out = input;
        String[][] tags = {
                {"black", "0"}, {"dark_blue", "1"}, {"dark_green", "2"}, {"dark_aqua", "3"},
                {"dark_red", "4"}, {"dark_purple", "5"}, {"gold", "6"}, {"gray", "7"},
                {"dark_gray", "8"}, {"blue", "9"}, {"green", "a"}, {"aqua", "b"},
                {"red", "c"}, {"light_purple", "d"}, {"yellow", "e"}, {"white", "f"},
                {"obfuscated", "k"}, {"bold", "l"}, {"strikethrough", "m"}, {"underlined", "n"},
                {"italic", "o"}, {"reset", "r"}
        };
        for (String[] tag : tags) {
            out = out.replace("<" + tag[0] + ">", "§" + tag[1]);
            out = out.replace("<" + tag[0].toUpperCase(Locale.ROOT) + ">", "§" + tag[1]);
        }
        out = out.replaceAll("</[^>]+>", "§r");
        out = out.replaceAll("<[^>]+>", "");
        return out;
    }

    public ProgressService getProgressService() { return progressService; }
    public ItemFactory getItemFactory() { return itemFactory; }
    public DestructionService getDestructionService() { return destructionService; }
    public DebrisService getDebrisService() { return debrisService; }
    public ArenaService getArenaService() { return arenaService; }
    public WorldService getWorldService() { return worldService; }
    public RemoteAimService getAimService() { return aimService; }
    public UnlockService getUnlockService() { return unlockService; }
    public HudService getHudService() { return hudService; }
    public ShowcaseService getShowcaseService() { return showcaseService; }
    public PortalService getPortalService() { return portalService; }
    public CinematicService getCinematicService() { return cinematicService; }
    public SchematicService getSchematicService() { return schematicService; }
}
