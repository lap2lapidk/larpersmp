package com.smpcore;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Spawn and warps, saved in plugins/SMPCore/warps.yml */
public final class LocationStore {
    private final SMPCore plugin;
    private final File file;
    private YamlConfiguration yaml;

    public LocationStore(SMPCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps.yml: " + e.getMessage());
        }
    }

    public Location spawn() {
        return yaml.getLocation("spawn");
    }

    public void setSpawn(Location l) {
        yaml.set("spawn", l);
        save();
    }

    public Location warp(String name) {
        return yaml.getLocation("warps." + name.toLowerCase(Locale.ROOT));
    }

    public void setWarp(String name, Location l) {
        yaml.set("warps." + name.toLowerCase(Locale.ROOT), l);
        save();
    }

    public boolean delWarp(String name) {
        String path = "warps." + name.toLowerCase(Locale.ROOT);
        if (!yaml.contains(path)) return false;
        yaml.set(path, null);
        save();
        return true;
    }

    public List<String> warpNames() {
        ConfigurationSection s = yaml.getConfigurationSection("warps");
        return s == null ? new ArrayList<>() : new ArrayList<>(s.getKeys(false));
    }
}
