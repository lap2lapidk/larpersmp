package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class TeleportCommands extends BaseCmd {

    public TeleportCommands(SMPCore plugin) {
        super(plugin);
    }

    @Override
    protected void run(CommandSender s, String cmd, String[] a) {
        Player p = player(s);
        if (p == null) return;
        switch (cmd) {
            case "tpa" -> request(p, a, false);
            case "tpahere" -> request(p, a, true);
            case "tpaccept" -> respond(p, true);
            case "tpadeny" -> respond(p, false);
            case "sethome" -> setHome(p, a);
            case "home" -> home(p, a);
            case "delhome" -> delHome(p, a);
            case "homes" -> homes(p);
            case "spawn" -> plugin.teleports().teleport(p, plugin.locations().spawn(), "spawn");
            case "setspawn" -> {
                if (!plugin.ranks().require(p, Rank.ADMIN)) return;
                plugin.locations().setSpawn(p.getLocation());
                Util.msg(p, "Spawn set to your location.");
            }
            case "warp" -> warp(p, a);
            case "warps" -> {
                List<String> names = plugin.locations().warpNames();
                Util.msg(p, names.isEmpty() ? "&cNo warps yet." : "Warps: &f" + String.join("&7, &f", names));
            }
            case "setwarp" -> setWarp(p, a);
            case "delwarp" -> delWarp(p, a);
            case "rtp" -> plugin.teleports().rtp(p);
            case "back" -> {
                Location l = plugin.teleports().getBack(p);
                if (l == null) Util.msg(p, "&cYou have nowhere to go back to.");
                else plugin.teleports().teleport(p, l, "your previous location");
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- TPA

    private void request(Player p, String[] a, boolean here) {
        if (a.length < 1) {
            usage(p, here ? "/tpahere <player>" : "/tpa <player>");
            return;
        }
        Player t = target(p, a[0]);
        if (t == null) return;
        if (t.getUniqueId().equals(p.getUniqueId())) {
            Util.msg(p, "&cYou can't teleport to yourself.");
            return;
        }
        plugin.teleports().sendRequest(p, t, here);
    }

    private void respond(Player p, boolean accept) {
        TeleportManager.Request r = plugin.teleports().takeRequest(p);
        if (r == null) return;
        Player from = Bukkit.getPlayer(r.from());
        if (from == null) {
            Util.msg(p, "&cThat player went offline.");
            return;
        }
        if (!accept) {
            Util.msg(p, "Request denied.");
            Util.msg(from, "&f" + p.getName() + " &cdenied your teleport request.");
            return;
        }
        Util.msg(p, "Request accepted.");
        Util.msg(from, "&f" + p.getName() + " &aaccepted your teleport request.");
        if (r.here()) plugin.teleports().teleport(p, from.getLocation(), from.getName());
        else plugin.teleports().teleport(from, p.getLocation(), p.getName());
    }

    // -------------------------------------------------------------- homes

    private static boolean validName(String n) {
        return n.matches("[a-z0-9_-]{1,16}");
    }

    private void setHome(Player p, String[] a) {
        String name = a.length > 0 ? a[0].toLowerCase() : "home";
        if (!validName(name)) {
            Util.msg(p, "&cHome names can use letters, numbers, - and _ (max 16).");
            return;
        }
        PlayerData d = plugin.data().get(p);
        int max = plugin.progression().maxHomes(p);
        if (!d.homes.containsKey(name) && d.homes.size() >= max) {
            Util.msg(p, "&cYou've reached your home limit (&f" + max + "&c). Rank up on &f/progress &cto unlock more.");
            return;
        }
        d.homes.put(name, p.getLocation().clone());
        Util.msg(p, "Home &f" + name + " &7set. (" + d.homes.size() + "/" + max + ")");
    }

    private void home(Player p, String[] a) {
        PlayerData d = plugin.data().get(p);
        if (d.homes.isEmpty()) {
            Util.msg(p, "&cYou don't have any homes. Use &f/sethome&c.");
            return;
        }
        String name;
        if (a.length > 0) name = a[0].toLowerCase();
        else if (d.homes.containsKey("home")) name = "home";
        else if (d.homes.size() == 1) name = d.homes.keySet().iterator().next();
        else {
            Util.msg(p, "Choose a home: &f" + String.join("&7, &f", d.homes.keySet()));
            return;
        }
        Location l = d.homes.get(name);
        if (l == null) {
            Util.msg(p, "&cNo home called &f" + name + "&c. Your homes: &f" + String.join(", ", d.homes.keySet()));
            return;
        }
        plugin.teleports().teleport(p, l, "home " + name);
    }

    private void delHome(Player p, String[] a) {
        if (a.length < 1) {
            usage(p, "/delhome <name>");
            return;
        }
        PlayerData d = plugin.data().get(p);
        if (d.homes.remove(a[0].toLowerCase()) == null) Util.msg(p, "&cNo such home.");
        else Util.msg(p, "Home deleted.");
    }

    private void homes(Player p) {
        PlayerData d = plugin.data().get(p);
        int max = plugin.progression().maxHomes(p);
        if (d.homes.isEmpty()) Util.msg(p, "No homes yet (0/" + max + "). Use &f/sethome&7.");
        else Util.msg(p, "Homes (" + d.homes.size() + "/" + max + "): &f" + String.join("&7, &f", d.homes.keySet()));
    }

    // -------------------------------------------------------------- warps

    private void warp(Player p, String[] a) {
        if (a.length < 1) {
            usage(p, "/warp <name>   (see /warps)");
            return;
        }
        Location l = plugin.locations().warp(a[0]);
        if (l == null) {
            Util.msg(p, "&cNo such warp.");
            return;
        }
        plugin.teleports().teleport(p, l, "warp " + a[0].toLowerCase());
    }

    private void setWarp(Player p, String[] a) {
        if (!plugin.ranks().require(p, Rank.ADMIN)) return;
        if (a.length < 1 || !validName(a[0].toLowerCase())) {
            usage(p, "/setwarp <name>");
            return;
        }
        plugin.locations().setWarp(a[0], p.getLocation());
        Util.msg(p, "Warp &f" + a[0].toLowerCase() + " &7created.");
    }

    private void delWarp(Player p, String[] a) {
        if (!plugin.ranks().require(p, Rank.ADMIN)) return;
        if (a.length < 1) {
            usage(p, "/delwarp <name>");
            return;
        }
        Util.msg(p, plugin.locations().delWarp(a[0]) ? "Warp deleted." : "&cNo such warp.");
    }

    @Override
    protected List<String> tab(CommandSender s, String cmd, String[] a) {
        if (a.length != 1) return List.of();
        switch (cmd) {
            case "tpa", "tpahere":
                return names(s, a[0]);
            case "home", "delhome":
                if (s instanceof Player p) return filter(plugin.data().get(p).homes.keySet(), a[0]);
                return List.of();
            case "warp", "delwarp":
                return filter(plugin.locations().warpNames(), a[0]);
            default:
                return List.of();
        }
    }
}
