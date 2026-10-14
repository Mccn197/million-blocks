package eu.reverie.millionblocks;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProgressService {
    private final MillionBlocksPlugin plugin;
    private final File file;
    private final Map<UUID, Long> progress = new HashMap<>();

    public ProgressService(MillionBlocksPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "progress.yml");
        load();
    }

    public long get(Player player) {
        return get(player.getUniqueId());
    }

    public long get(UUID uuid) {
        return progress.getOrDefault(uuid, 0L);
    }

    public long add(Player player, long amount) {
        return set(player, Math.max(0L, get(player) + amount));
    }

    public long set(Player player, long value) {
        progress.put(player.getUniqueId(), Math.max(0L, value));
        save();
        return get(player);
    }

    public void reset(Player player) {
        set(player, 0L);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                progress.put(UUID.fromString(key), yaml.getLong(key));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid UUID in progress.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        progress.forEach((uuid, value) -> yaml.set(uuid.toString(), value));
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
            }
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save progress.yml: " + exception.getMessage());
        }
    }
}
