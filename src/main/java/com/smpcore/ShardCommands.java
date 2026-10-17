package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** /shards and /shardshop */
public final class ShardCommands extends BaseCmd {
    private static final String USAGE = "/shards [player|top|pay|shop]";

    public ShardCommands(SMPCore plugin) {
        super(plugin);
    }

    @Override
    protected void run(CommandSender s, String cmd, String[] a) {
        if (cmd.equals("shardshop")) {
            openShop(s);
            return;
        }
        if (a.length == 0) {
            Player p = player(s);
            if (p != null) Util.msg(s, "Your shards: &b" + plugin.economy().shards(p.getUniqueId()));
            return;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "shop" -> openShop(s);
            case "top" -> top(s);
            case "pay" -> pay(s, a);
            case "give", "take", "set" -> admin(s, a);
            default -> {
                PlayerData d = plugin.data().find(a[0]);
                if (d == null) Util.msg(s, "&cUnknown player.");
                else Util.msg(s, "&f" + d.name + "&7's shards: &b" + d.shards);
            }
        }
    }

    private void openShop(CommandSender s) {
        Player p = player(s);
        if (p != null) new ShardShopMenu(plugin, p).open(p);
    }

    private void top(CommandSender s) {
        List<PlayerData> list = new ArrayList<>(plugin.data().all());
        list.sort(Comparator.comparingLong((PlayerData d) -> d.shards).reversed());
        Util.raw(s, "&8&m      &r &b&lTop Shards &8&m      ");
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            PlayerData d = list.get(i);
            Util.raw(s, "&e#" + (i + 1) + " &f" + d.name + " &8- &b" + d.shards);
        }
    }

    private void pay(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length < 3) {
            usage(s, "/shards pay <player> <amount>");
            return;
        }
        PlayerData to = plugin.data().find(a[1]);
        if (to == null) {
            Util.msg(s, "&cUnknown player.");
            return;
        }
        if (to.uuid.equals(p.getUniqueId())) {
            Util.msg(s, "&cYou can't pay yourself.");
            return;
        }
        long amount = amount(a[2]);
        if (amount <= 0) {
            Util.msg(s, "&cEnter a valid whole amount of shards.");
            return;
        }
        if (!plugin.economy().takeShards(p.getUniqueId(), amount)) {
            Util.msg(s, "&cYou don't have enough shards.");
            return;
        }
        plugin.economy().addShards(to.uuid, amount);
        Util.msg(s, "Paid &f" + to.name + " &b" + amount + " shards");
        Player online = Bukkit.getPlayer(to.uuid);
        if (online != null) Util.msg(online, "&f" + p.getName() + " &7paid you &b" + amount + " shards");
    }

    private void admin(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.ADMIN)) return;
        if (a.length < 3) {
            usage(s, "/shards <give|take|set> <player> <amount>");
            return;
        }
        PlayerData t = plugin.data().find(a[1]);
        if (t == null) {
            Util.msg(s, "&cUnknown player.");
            return;
        }
        long amount = amount(a[2]);
        String action = a[0].toLowerCase(Locale.ROOT);
        if (amount < 0 || (!action.equals("set") && amount == 0)) {
            Util.msg(s, "&cInvalid amount.");
            return;
        }
        switch (action) {
            case "give" -> plugin.economy().addShards(t.uuid, amount);
            case "take" -> plugin.economy().setShards(t.uuid, t.shards - amount);
            default -> plugin.economy().setShards(t.uuid, amount);
        }
        Util.msg(s, t.name + " now has &b" + t.shards + " shards");
    }

    /** Whole number of shards (supports 2k, 1m). -1 if invalid. */
    private static long amount(String s) {
        double v = Util.parseAmount(s);
        return v < 0 ? -1 : (long) Math.floor(v);
    }

    @Override
    protected List<String> tab(CommandSender s, String cmd, String[] a) {
        if (cmd.equals("shardshop")) return List.of();
        if (a.length == 1) {
            List<String> opts = new ArrayList<>(List.of("shop", "top", "pay"));
            if (plugin.ranks().has(s, Rank.ADMIN)) opts.addAll(List.of("give", "take", "set"));
            List<String> out = filter(opts, a[0]);
            out.addAll(names(s, a[0]));
            return out;
        }
        if (a.length == 2) {
            String sub = a[0].toLowerCase(Locale.ROOT);
            if (List.of("pay", "give", "take", "set").contains(sub)) return names(s, a[1]);
        }
        return List.of();
    }
}
