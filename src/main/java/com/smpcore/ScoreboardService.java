package com.smpcore;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-player sidebar showing rank, tier, money, playtime and stats. */
public final class ScoreboardService {
    private static final int LINES = 10;

    private final SMPCore plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardService(SMPCore plugin) {
        this.plugin = plugin;
    }

    private static String entry(int i) {
        return "§" + Integer.toHexString(i);
    }

    public void show(Player p) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o = sb.registerNewObjective("smp", "dummy",
                Util.c(plugin.getConfig().getString("scoreboard-title", "&6&lSMP")));
        o.setDisplaySlot(DisplaySlot.SIDEBAR);
        o.numberFormat(NumberFormat.blank());
        for (int i = 0; i < LINES; i++) {
            o.getScore(entry(i)).setScore(LINES - i);
        }
        boards.put(p.getUniqueId(), sb);
        p.setScoreboard(sb);
        update(p);
    }

    public void remove(Player p) {
        boards.remove(p.getUniqueId());
    }

    public void clear() {
        boards.clear();
    }

    public void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) update(p);
    }

    public void update(Player p) {
        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null) return;
        Objective o = sb.getObjective("smp");
        if (o == null) return;

        PlayerData d = plugin.data().get(p);
        ProgressionManager.Tier tier = plugin.progression().tierOf(d);
        int online = (int) Bukkit.getOnlinePlayers().stream().filter(x -> !plugin.isVanished(x.getUniqueId())).count();

        List<String> lines = List.of(
                " ",
                "&7Rank: " + d.rank.color() + d.rank.display(),
                "&7Tier: " + tier.color() + tier.name(),
                "&7Money: &a" + Util.money(d.money),
                "&7Shards: &b" + d.shards,
                "&7Playtime: &f" + Util.time(d.playtime),
                "&7Kills: &f" + d.kills + " &7Deaths: &f" + d.deaths,
                "&7Online: &f" + online,
                "  ",
                plugin.getConfig().getString("scoreboard-footer", "&eplay.yourserver.net"));

        for (int i = 0; i < LINES; i++) {
            o.getScore(entry(i)).customName(Util.c(lines.get(i)));
        }
    }
}
