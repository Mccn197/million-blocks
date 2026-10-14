package eu.reverie.millionblocks;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

/** Optional native BetterHud Paper integration; no PlaceholderAPI, Oraxen or ModelEngine. */
public final class BetterHudBridge {
    private final MillionBlocksPlugin plugin;
    private Object api;
    private ClassLoader loader;
    private String status = "BetterHud absent";
    private final Map<UUID,Object> attached = new HashMap<>();
    private final java.util.concurrent.atomic.AtomicReference<Function<UUID,HudState>> states = new java.util.concurrent.atomic.AtomicReference<>(uuid -> HudState.EMPTY);

    public BetterHudBridge(MillionBlocksPlugin plugin) { this.plugin = plugin; }
    private Class<?> type(String suffix) throws ClassNotFoundException {
        return Class.forName("kr.toxicity.hud.api." + suffix, true, loader);
    }
    private Object call(String owner, Object object, String method, Class<?>[] params, Object... args)
            throws ReflectiveOperationException {
        return type(owner).getMethod(method, params).invoke(object, args);
    }
    private Object manager(String name) throws ReflectiveOperationException {
        return call("BetterHud", api, "get" + name + "Manager", new Class<?>[0]);
    }
    public void connect() {
        Plugin better = plugin.getServer().getPluginManager().getPlugin("BetterHud");
        if (better == null || !better.isEnabled()) return;
        try {
            loader = better.getClass().getClassLoader();
            api = type("BetterHudAPI").getMethod("inst").invoke(null);
            states.set(plugin.getHudService()::snapshot);
            Object placeholders = manager("Placeholder");
            register(placeholders, "String", "millionblocks_count", s -> s.countText());
            register(placeholders, "String", "millionblocks_goal", s -> s.goalText());
            register(placeholders, "String", "millionblocks_percent", s -> s.percentText());
            register(placeholders, "String", "millionblocks_multiplier", s -> s.multiplierText());
            register(placeholders, "Boolean", "millionblocks_visible", HudState::visible);
            register(placeholders, "Boolean", "millionblocks_icon", HudState::showIcon);
            register(placeholders, "Number", "millionblocks_crack", s -> s.crack(System.nanoTime()));
            status = "API connectée (BetterHud " + better.getDescription().getVersion() + ")";
            plugin.getLogger().info(status + "; charger les fichiers Amber puis /hud reload.");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            api = null;
            states.set(uuid -> HudState.EMPTY);
            status = "API incompatible: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
            plugin.getLogger().warning(status);
        }
    }
    private void register(Object manager, String kind, String name, Function<HudState,Object> value)
            throws ReflectiveOperationException {
        Object container = call("manager.PlaceholderManager", manager, "get" + kind + "Container", new Class<?>[0]);
        Class<?> placeholder = type("placeholder.HudPlaceholder");
        Method uuidMethod = type("player.HudPlayer").getMethod("uuid");
        var stateProvider = states;
        Object proxy = Proxy.newProxyInstance(loader, new Class<?>[]{placeholder}, (p, m, a) -> switch (m.getName()) {
            case "getRequiredArgsLength" -> 0;
            case "invoke" -> (Function<Object,Object>) hp -> {
                try {
                    UUID uuid = (UUID) uuidMethod.invoke(hp);
                    return value.apply(stateProvider.get().apply(uuid));
                } catch (ReflectiveOperationException ex) { return value.apply(HudState.EMPTY); }
            };
            case "toString" -> "MillionBlocks:" + name;
            case "hashCode" -> System.identityHashCode(p);
            case "equals" -> p == a[0];
            default -> throw new UnsupportedOperationException(m.getName());
        });
        call("placeholder.PlaceholderContainer", container, "addPlaceholder", new Class<?>[]{String.class, placeholder}, name, proxy);
    }
    public boolean sync(Player player, boolean visible) {
        if (api == null) return false;
        try {
            Object hp = call("manager.PlayerManager", manager("Player"), "getHudPlayer", new Class<?>[]{UUID.class}, player.getUniqueId());
            if (hp == null) { status = "API connectée, joueur BetterHud en attente"; return false; }
            Object hud = call("manager.HudManager", manager("Hud"), "getHud", new Class<?>[]{String.class}, "mb_amber");
            Object previous = attached.get(player.getUniqueId());
            if (previous != null && (previous != hud || !visible)) {
                call("configuration.HudObject", previous, "remove", new Class<?>[]{type("player.HudPlayer")}, hp);
                attached.remove(player.getUniqueId());
            }
            if (hud == null) { status = "API connectée, mb_amber absent: installer YAML puis /hud reload"; return false; }
            if (visible) {
                call("configuration.HudObject", hud, "add", new Class<?>[]{type("player.HudPlayer")}, hp);
                attached.put(player.getUniqueId(), hud);
            }
            status = "mb_amber chargé, API connectée";
            return true;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            status = "Erreur liaison: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
            return false;
        }
    }
    public void close() {
        for (Player p : plugin.getServer().getOnlinePlayers()) sync(p, false);
        attached.clear();
        states.set(uuid -> HudState.EMPTY);
        api = null;
    }
    public String diagnostics() { return status; }
}
