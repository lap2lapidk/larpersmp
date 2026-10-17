package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Member progression tiers, earned through playtime. */
public final class ProgressionManager {

    public record Tier(String name, int hours, double reward, int maxHomes, double sellBonus,
                       Material icon, String color) {
        public long seconds() { return hours * 3600L; }
    }

    private final SMPCore plugin;
    private final List<Tier> tiers = new ArrayList<>();

    public ProgressionManager(SMPCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        tiers.clear();
        for (Map<?, ?> m : plugin.getConfig().getMapList("progression.tiers")) {
            Material icon = Material.matchMaterial(str(m, "icon", "PAPER"));
            tiers.add(new Tier(
                    str(m, "name", "Tier"),
                    (int) num(m, "hours", 0),
                    num(m, "reward", 0),
                    (int) num(m, "max-homes", 2),
                    num(m, "sell-bonus", 0),
                    icon == null ? Material.PAPER : icon,
                    str(m, "color", "&f")));
        }
        tiers.sort((a, b) -> Integer.compare(a.hours(), b.hours()));
        if (tiers.isEmpty()) {
            tiers.add(new Tier("Member", 0, 0, 2, 0, Material.PAPER, "&7"));
        }
    }

    private static String str(Map<?, ?> m, String key, String def) {
        Object o = m.get(key);
        return o == null ? def : o.toString();
    }

    private static double num(Map<?, ?> m, String key, double def) {
        Object o = m.get(key);
        return o instanceof Number n ? n.doubleValue() : def;
    }

    public List<Tier> tiers() {
        return tiers;
    }

    public int indexFor(long playtimeSeconds) {
        int idx = 0;
        for (int i = 0; i < tiers.size(); i++) {
            if (playtimeSeconds >= tiers.get(i).seconds()) idx = i;
        }
        return idx;
    }

    public Tier tierOf(PlayerData d) {
        return tiers.get(indexFor(d.playtime));
    }

    public int maxHomes(Player p) {
        PlayerData d = plugin.data().get(p);
        int homes = tierOf(d).maxHomes();
        if (d.rank == Rank.MOD) homes += 2;
        if (d.rank == Rank.ADMIN) homes += 50;
        return homes;
    }

    public double sellMultiplier(Player p) {
        return 1.0 + tierOf(plugin.data().get(p)).sellBonus() / 100.0;
    }

    /** Pays out any newly reached tiers. */
    public void tick(Player p) {
        PlayerData d = plugin.data().get(p);
        int idx = indexFor(d.playtime);
        while (d.tier < idx) {
            d.tier++;
            Tier t = tiers.get(d.tier);
            if (t.reward() > 0) plugin.economy().deposit(p.getUniqueId(), t.reward());
            Bukkit.broadcast(Util.c("&6★ &f" + p.getName() + " &7reached " + t.color() + "&l" + t.name()
                    + " &7tier!"));
            Util.msg(p, "Tier reward: &a" + Util.exact(t.reward()) + " &7| Homes: &f" + t.maxHomes()
                    + " &7| Sell bonus: &a+" + (int) t.sellBonus() + "%");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }
}
