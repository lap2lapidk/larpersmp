package com.smpcore;

import java.util.UUID;

public final class EconomyManager {
    private final DataManager data;

    public EconomyManager(DataManager data) {
        this.data = data;
    }

    public double balance(UUID id) {
        PlayerData d = data.byId(id);
        return d == null ? 0 : d.money;
    }

    public boolean has(UUID id, double amount) {
        return balance(id) >= amount;
    }

    public void deposit(UUID id, double amount) {
        PlayerData d = data.byId(id);
        if (d != null) d.money = round(d.money + amount);
    }

    public boolean withdraw(UUID id, double amount) {
        PlayerData d = data.byId(id);
        if (d == null || d.money < amount) return false;
        d.money = round(d.money - amount);
        return true;
    }

    public void set(UUID id, double amount) {
        PlayerData d = data.byId(id);
        if (d != null) d.money = round(Math.max(0, amount));
    }

    // ------------------------------------------------------------ shards

    public long shards(UUID id) {
        PlayerData d = data.byId(id);
        return d == null ? 0 : d.shards;
    }

    public void addShards(UUID id, long amount) {
        PlayerData d = data.byId(id);
        if (d != null) d.shards = Math.max(0, d.shards + amount);
    }

    public boolean takeShards(UUID id, long amount) {
        PlayerData d = data.byId(id);
        if (d == null || amount < 0 || d.shards < amount) return false;
        d.shards -= amount;
        return true;
    }

    public void setShards(UUID id, long amount) {
        PlayerData d = data.byId(id);
        if (d != null) d.shards = Math.max(0, amount);
    }

    private static double round(double v) {
        return Math.round(v * 100d) / 100d;
    }
}
