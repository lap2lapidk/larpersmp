package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InfoCommands extends BaseCmd {
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();

    public InfoCommands(SMPCore plugin) {
        super(plugin);
    }

    @Override
    protected void run(CommandSender s, String cmd, String[] a) {
        switch (cmd) {
            case "progress" -> {
                Player p = player(s);
                if (p != null) new ProgressMenu(plugin, p).open(p);
            }
            case "stats" -> stats(s, a);
            case "msg" -> msg(s, a);
            case "reply" -> reply(s, a);
            case "smpcore" -> admin(s, a);
            default -> { }
        }
    }

    private void stats(CommandSender s, String[] a) {
        PlayerData d;
        if (a.length == 0) {
            Player p = player(s);
            if (p == null) return;
            d = plugin.data().get(p);
        } else {
            d = plugin.data().find(a[0]);
            if (d == null) {
                Util.msg(s, "&cUnknown player.");
                return;
            }
        }
        ProgressionManager.Tier tier = plugin.progression().tierOf(d);
        Util.raw(s, "&8&m      &r &6&l" + d.name + " &8&m      ");
        Util.raw(s, "&7Rank: " + d.rank.color() + d.rank.display() + "  &7Tier: " + tier.color() + tier.name());
        Util.raw(s, "&7Money: &a" + Util.exact(d.money) + "  &7Shards: &b" + d.shards);
        Util.raw(s, "&7Playtime: &f" + Util.time(d.playtime));
        Util.raw(s, "&7Kills: &f" + d.kills + "  &7Deaths: &f" + d.deaths);
        Util.raw(s, "&7Homes: &f" + d.homes.size());
    }

    private void msg(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length < 2) {
            usage(s, "/msg <player> <message>");
            return;
        }
        Player t = target(s, a[0]);
        if (t == null) return;
        send(p, t, String.join(" ", Arrays.copyOfRange(a, 1, a.length)));
    }

    private void reply(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length < 1) {
            usage(s, "/reply <message>");
            return;
        }
        UUID last = lastMessaged.get(p.getUniqueId());
        Player t = last == null ? null : Bukkit.getPlayer(last);
        if (t == null) {
            Util.msg(s, "&cNobody to reply to.");
            return;
        }
        send(p, t, String.join(" ", a));
    }

    private void send(Player from, Player to, String text) {
        if (plugin.data().get(from).isMuted()) {
            Util.msg(from, "&cYou are muted.");
            return;
        }
        Util.raw(from, "&8[&7You &8» &f" + to.getName() + "&8] &7" + text);
        Util.raw(to, "&8[&f" + from.getName() + " &8» &7You&8] &7" + text);
        lastMessaged.put(from.getUniqueId(), to.getUniqueId());
        lastMessaged.put(to.getUniqueId(), from.getUniqueId());
    }

    private void admin(CommandSender s, String[] a) {
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            if (!plugin.ranks().require(s, Rank.ADMIN)) return;
            plugin.reloadAll();
            Util.msg(s, "Config, shop and progression tiers reloaded.");
            return;
        }
        Util.msg(s, "SMPCore v" + plugin.getPluginMeta().getVersion() + " - /smpcore reload");
    }

    @Override
    protected List<String> tab(CommandSender s, String cmd, String[] a) {
        if (cmd.equals("smpcore") && a.length == 1) return filter(List.of("reload"), a[0]);
        if (cmd.equals("progress") || cmd.equals("reply")) return List.of();
        return super.tab(s, cmd, a);
    }
}
