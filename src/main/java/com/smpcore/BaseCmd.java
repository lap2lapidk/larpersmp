package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Shared helpers for all command classes. */
public abstract class BaseCmd implements TabExecutor {
    protected final SMPCore plugin;

    protected BaseCmd(SMPCore plugin) {
        this.plugin = plugin;
    }

    protected abstract void run(CommandSender s, String cmd, String[] a);

    @Override
    public final boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        run(s, c.getName().toLowerCase(Locale.ROOT), a);
        return true;
    }

    /** Override for custom tab completion. Default: online player names for the first argument. */
    protected List<String> tab(CommandSender s, String cmd, String[] a) {
        return a.length == 1 ? names(s, a[0]) : List.of();
    }

    @Override
    public final List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> r = tab(s, c.getName().toLowerCase(Locale.ROOT), a);
        return r == null ? List.of() : r;
    }

    protected List<String> names(CommandSender s, String prefix) {
        List<String> out = new ArrayList<>();
        boolean staff = plugin.ranks().has(s, Rank.MOD);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.isVanished(p.getUniqueId()) && !staff) continue;
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) out.add(p.getName());
        }
        return out;
    }

    protected List<String> filter(Collection<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) out.add(o);
        }
        return out;
    }

    /** The sender as a player, or null after telling them off. */
    protected Player player(CommandSender s) {
        if (s instanceof Player p) return p;
        Util.msg(s, "&cOnly players can use that command.");
        return null;
    }

    /** Finds an online (non-vanished) player or complains. */
    protected Player target(CommandSender s, String name) {
        Player t = Bukkit.getPlayerExact(name);
        if (t == null || (plugin.isVanished(t.getUniqueId()) && !plugin.ranks().has(s, Rank.MOD))) {
            Util.msg(s, "&cThat player is not online.");
            return null;
        }
        return t;
    }

    protected void usage(CommandSender s, String usage) {
        Util.msg(s, "&cUsage: &f" + usage);
    }
}
