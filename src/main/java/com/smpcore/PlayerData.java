package com.smpcore;

import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {
    public final UUID uuid;
    public String name;
    public double money;
    /** Second currency, earned by playing and killing. */
    public long shards;
    public Rank rank = Rank.MEMBER;
    public int kills;
    public int deaths;
    /** Highest progression tier index already rewarded. */
    public int tier;
    /** Playtime in seconds. */
    public long playtime;
    /** Epoch millis until which the player is muted (0 = not muted). */
    public long mutedUntil;
    public final Map<String, Location> homes = new LinkedHashMap<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public boolean isMuted() {
        return mutedUntil > System.currentTimeMillis();
    }
}
