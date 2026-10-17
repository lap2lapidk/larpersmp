package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EconomyCommands extends BaseCmd {

    public EconomyCommands(SMPCore plugin) {
        super(plugin);
    }

    @Override
    protected void run(CommandSender s, String cmd, String[] a) {
        switch (cmd) {
            case "balance" -> balance(s, a);
            case "pay" -> pay(s, a);
            case "baltop" -> baltop(s);
            case "eco" -> eco(s, a);
            default -> { }
        }
    }

    private void balance(CommandSender s, String[] a) {
        PlayerData d;
        if (a.length == 0) {
            Player p = player(s);
            if (p == null) return;
            d = plugin.data().get(p);
            Util.msg(s, "Your balance: &a" + Util.exact(d.money));
            return;
        }
        d = plugin.data().find(a[0]);
        if (d == null) {
            Util.msg(s, "&cUnknown player.");
            return;
        }
        Util.msg(s, "&f" + d.name + "&7's balance: &a" + Util.exact(d.money));
    }

    private void pay(CommandSender s, String[] a) {
        Player p = player(s);
        if (p == null) return;
        if (a.length < 2) {
            usage(s, "/pay <player> <amount>");
            return;
        }
        PlayerData to = plugin.data().find(a[0]);
        if (to == null) {
            Util.msg(s, "&cUnknown player.");
            return;
        }
        if (to.uuid.equals(p.getUniqueId())) {
            Util.msg(s, "&cYou can't pay yourself.");
            return;
        }
        double amount = Util.parseAmount(a[1]);
        if (amount <= 0) {
            Util.msg(s, "&cEnter a valid amount (examples: 500, 2.5k, 1m).");
            return;
        }
        if (!plugin.economy().withdraw(p.getUniqueId(), amount)) {
            Util.msg(s, "&cYou don't have enough money.");
            return;
        }
        plugin.economy().deposit(to.uuid, amount);
        Util.msg(s, "Paid &f" + to.name + " &a" + Util.exact(amount));
        Player online = Bukkit.getPlayer(to.uuid);
        if (online != null) Util.msg(online, "&f" + p.getName() + " &7paid you &a" + Util.exact(amount));
    }

    private void baltop(CommandSender s) {
        List<PlayerData> list = new ArrayList<>(plugin.data().all());
        list.sort(Comparator.comparingDouble((PlayerData d) -> d.money).reversed());
        Util.raw(s, "&8&m      &r &6&lRichest Players &8&m      ");
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            PlayerData d = list.get(i);
            Util.raw(s, "&e#" + (i + 1) + " &f" + d.name + " &8- &a" + Util.money(d.money));
        }
    }

    private void eco(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.ADMIN)) return;
        if (a.length < 3) {
            usage(s, "/eco <give|take|set> <player> <amount>");
            return;
        }
        PlayerData t = plugin.data().find(a[1]);
        if (t == null) {
            Util.msg(s, "&cUnknown player.");
            return;
        }
        double amount = Util.parseAmount(a[2]);
        String action = a[0].toLowerCase();
        boolean ok = action.equals("set") ? amount >= 0 : amount > 0;
        if (!ok) {
            Util.msg(s, "&cInvalid amount.");
            return;
        }
        switch (action) {
            case "give" -> plugin.economy().deposit(t.uuid, amount);
            case "take" -> plugin.economy().set(t.uuid, t.money - amount);
            case "set" -> plugin.economy().set(t.uuid, amount);
            default -> {
                usage(s, "/eco <give|take|set> <player> <amount>");
                return;
            }
        }
        Util.msg(s, t.name + "'s balance is now &a" + Util.exact(t.money));
    }

    @Override
    protected List<String> tab(CommandSender s, String cmd, String[] a) {
        if (cmd.equals("eco")) {
            if (a.length == 1) return filter(List.of("give", "take", "set"), a[0]);
            if (a.length == 2) return names(s, a[1]);
            return List.of();
        }
        return super.tab(s, cmd, a);
    }
}
