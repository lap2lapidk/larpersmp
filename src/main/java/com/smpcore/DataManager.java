package com.smpcore;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stores every player's data in plugins/SMPCore/players.yml */
public final class DataManager {
    private final SMPCore plugin;
    private final File file;
    private final Map<UUID, PlayerData> players = new HashMap<>();

    public DataManager(SMPCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = y.getConfigurationSection("players");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            PlayerData d = new PlayerData(id, s.getString("name", "unknown"));
            d.money = s.getDouble("money");
            d.shards = s.getLong("shards");
            Rank r = Rank.parse(s.getString("rank", "member"));
            d.rank = r == null ? Rank.MEMBER : r;
            d.kills = s.getInt("kills");
            d.deaths = s.getInt("deaths");
            d.tier = s.getInt("tier");
            d.playtime = s.getLong("playtime");
            d.mutedUntil = s.getLong("muted-until");
            ConfigurationSection h = s.getConfigurationSection("homes");
            if (h != null) {
                for (String n : h.getKeys(false)) {
                    Location l = h.getLocation(n);
                    if (l != null) d.homes.put(n, l);
                }
            }
            players.put(id, d);
        }
        plugin.getLogger().info("Loaded " + players.size() + " player profiles.");
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (PlayerData d : players.values()) {
            String p = "players." + d.uuid + ".";
            y.set(p + "name", d.name);
            y.set(p + "money", d.money);
            y.set(p + "shards", d.shards);
            y.set(p + "rank", d.rank.name());
            y.set(p + "kills", d.kills);
            y.set(p + "deaths", d.deaths);
            y.set(p + "tier", d.tier);
            y.set(p + "playtime", d.playtime);
            y.set(p + "muted-until", d.mutedUntil);
            for (Map.Entry<String, Location> e : d.homes.entrySet()) {
                y.set(p + "homes." + e.getKey(), e.getValue());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save players.yml: " + ex.getMessage());
        }
    }

    public boolean exists(UUID id) {
        return players.containsKey(id);
    }

    /** Existing profile or null. */
    public PlayerData byId(UUID id) {
        return players.get(id);
    }

    /** Gets (or creates) the profile of an online player. */
    public PlayerData get(Player p) {
        PlayerData d = players.computeIfAbsent(p.getUniqueId(), id -> new PlayerData(id, p.getName()));
        d.name = p.getName();
        return d;
    }

    /** Case-insensitive lookup of any player that has ever joined. */
    public PlayerData find(String name) {
        for (PlayerData d : players.values()) {
            if (d.name.equalsIgnoreCase(name)) return d;
        }
        return null;
    }

    public Collection<PlayerData> all() {
        return players.values();
    }
}
