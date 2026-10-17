package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Rank management and moderation. Mods get the moderation tools, admins get everything. */
public final class StaffCommands extends BaseCmd {

    public StaffCommands(SMPCore plugin) {
        super(plugin);
    }

    @Override
    protected void run(CommandSender s, String cmd, String[] a) {
        switch (cmd) {
            case "rank" -> rank(s, a);
            case "mute" -> mute(s, a);
            case "unmute" -> unmute(s, a);
            case "kick" -> kick(s, a);
            case "ban" -> ban(s, a);
            case "unban" -> unban(s, a);
            case "vanish" -> vanish(s);
            case "sc" -> staffChat(s, a);
            case "invsee" -> invsee(s, a);
            case "stp" -> stp(s, a);
            default -> { }
        }
    }

    // --------------------------------------------------------------- rank

    private void rank(CommandSender s, String[] a) {
        if (a.length == 0) {
            if (s instanceof Player p) {
                Rank r = plugin.data().get(p).rank;
                Util.msg(s, "Your rank: " + r.color() + r.display());
            } else {
                usage(s, "/rank <player> [member|mod|admin]");
            }
            return;
        }
        if (a[0].equalsIgnoreCase("list")) {
            Util.raw(s, "&8&m      &r &6&lStaff &8&m      ");
            boolean any = false;
            for (PlayerData d : plugin.data().all()) {
                if (d.rank == Rank.MEMBER) continue;
                any = true;
                Util.raw(s, d.rank.prefix() + "&f" + d.name);
            }
            if (!any) Util.raw(s, "&7No staff yet.");
            return;
        }
        PlayerData t = plugin.data().find(a[0]);
        if (t == null) {
            Util.msg(s, "&cUnknown player (they must have joined at least once).");
            return;
        }
        if (a.length == 1) {
            Util.msg(s, "&f" + t.name + " &7is " + t.rank.color() + t.rank.display());
            return;
        }
        if (!plugin.ranks().require(s, Rank.ADMIN)) return;
        Rank r = Rank.parse(a[1]);
        if (r == null) {
            usage(s, "/rank <player> <member|mod|admin>");
            return;
        }
        Rank old = t.rank;
        plugin.ranks().set(t, r);
        Util.msg(s, "&f" + t.name + " &7is now " + r.color() + r.display() + " &8(was " + old.display() + ")");
        Player online = Bukkit.getPlayer(t.uuid);
        if (online != null) {
            String verb = r.level() > old.level() ? "&a&lPROMOTED" : "&e&lRANK CHANGED";
            Util.msg(online, verb + " &7You are now " + r.color() + r.display() + "&7. Check &f/progress&7.");
        }
        plugin.getLogger().info(s.getName() + " set " + t.name + " to " + r.name());
    }

    // --------------------------------------------------------- moderation

    /** Staff can only punish players of a lower rank than themselves (console can do anything). */
    private boolean canPunish(CommandSender s, PlayerData target) {
        if (!(s instanceof Player)) return true;
        if (plugin.ranks().rankOf(s).level() > target.rank.level()) return true;
        Util.msg(s, "&cYou can't do that to someone of equal or higher rank.");
        return false;
    }

    private void mute(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.MOD)) return;
        if (a.length < 1) {
            usage(s, "/mute <player> [minutes]");
            return;
        }
        PlayerData t = plugin.data().find(a[0]);
        if (t == null) {
            Util.msg(s, "&cUnknown player.");
            return;
        }
        if (!canPunish(s, t)) return;
        long minutes = 0;
        if (a.length > 1) {
            try {
                minutes = Long.parseLong(a[1]);
            } catch (NumberFormatException ignored) {
                // no duration given
            }
        }
        long now = System.currentTimeMillis();
        t.mutedUntil = minutes > 0 ? now + minutes * 60_000L : now + 3650L * 86_400_000L;
        String len = minutes > 0 ? minutes + " minute(s)" : "permanently";
        Util.msg(s, "Muted &f" + t.name + " &7" + len + ".");
        Player online = Bukkit.getPlayer(t.uuid);
        if (online != null) Util.msg(online, "&cYou have been muted " + len + ".");
        notifyStaff(s, "muted " + t.name + " " + len);
    }

    private void unmute(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.MOD)) return;
        if (a.length < 1) {
            usage(s, "/unmute <player>");
            return;
        }
        PlayerData t = plugin.data().find(a[0]);
        if (t == null) {
            Util.msg(s, "&cUnknown player.");
            return;
        }
        t.mutedUntil = 0;
        Util.msg(s, "Unmuted &f" + t.name + "&7.");
        Player online = Bukkit.getPlayer(t.uuid);
        if (online != null) Util.msg(online, "&aYou have been unmuted.");
        notifyStaff(s, "unmuted " + t.name);
    }

    private void kick(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.MOD)) return;
        if (a.length < 1) {
            usage(s, "/kick <player> [reason]");
            return;
        }
        Player t = Bukkit.getPlayerExact(a[0]);
        if (t == null) {
            Util.msg(s, "&cThat player is not online.");
            return;
        }
        if (!canPunish(s, plugin.data().get(t))) return;
        String reason = a.length > 1 ? String.join(" ", Arrays.copyOfRange(a, 1, a.length)) : "Kicked by staff";
        t.kick(Util.c("&cYou were kicked\n&7" + reason));
        notifyStaff(s, "kicked " + t.getName() + " (" + reason + ")");
    }

    private void ban(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.ADMIN)) return;
        if (a.length < 1) {
            usage(s, "/ban <player> [reason]");
            return;
        }
        PlayerData t = plugin.data().find(a[0]);
        if (t != null && !canPunish(s, t)) return;
        String reason = a.length > 1 ? String.join(" ", Arrays.copyOfRange(a, 1, a.length)) : "Banned by an admin";
        // use the vanilla command explicitly so this does not recurse into our own /ban
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minecraft:ban " + a[0] + " " + reason);
        notifyStaff(s, "banned " + a[0] + " (" + reason + ")");
    }

    private void unban(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.ADMIN)) return;
        if (a.length < 1) {
            usage(s, "/unban <player>");
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "minecraft:pardon " + a[0]);
        notifyStaff(s, "unbanned " + a[0]);
    }

    private void notifyStaff(CommandSender by, String what) {
        String line = "&8[&cStaff&8] &f" + by.getName() + " &7" + what;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.ranks().has(p, Rank.MOD)) Util.raw(p, line);
        }
        plugin.getLogger().info(by.getName() + " " + what);
    }

    // ------------------------------------------------------------- utility

    private void vanish(CommandSender s) {
        if (!plugin.ranks().require(s, Rank.MOD)) return;
        Player p = player(s);
        if (p == null) return;
        boolean on = plugin.toggleVanish(p);
        Util.msg(p, on ? "You are now &avanished&7. Players can't see you." : "You are now &cvisible&7.");
    }

    private void staffChat(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.MOD)) return;
        Player p = player(s);
        if (p == null) return;
        if (a.length == 0) {
            boolean on = plugin.toggleStaffChat(p);
            Util.msg(p, "Staff chat mode " + (on ? "&aenabled" : "&cdisabled") + "&7.");
            return;
        }
        plugin.staffChat(p, String.join(" ", a));
    }

    private void invsee(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.MOD)) return;
        Player p = player(s);
        if (p == null) return;
        if (a.length < 1) {
            usage(s, "/invsee <player>");
            return;
        }
        Player t = target(s, a[0]);
        if (t == null) return;
        p.openInventory(t.getInventory());
        if (!plugin.ranks().has(p, Rank.ADMIN)) Util.msg(p, "Opened &f" + t.getName() + "&7's inventory (view only).");
    }

    private void stp(CommandSender s, String[] a) {
        if (!plugin.ranks().require(s, Rank.MOD)) return;
        Player p = player(s);
        if (p == null) return;
        if (a.length < 1) {
            usage(s, "/stp <player>");
            return;
        }
        Player t = target(s, a[0]);
        if (t == null) return;
        plugin.teleports().teleport(p, t.getLocation(), t.getName());
    }

    @Override
    protected List<String> tab(CommandSender s, String cmd, String[] a) {
        if (cmd.equals("rank")) {
            if (a.length == 1) {
                List<String> opts = new ArrayList<>(names(s, a[0]));
                opts.addAll(filter(List.of("list"), a[0]));
                return opts;
            }
            if (a.length == 2) return filter(List.of("member", "mod", "admin"), a[1]);
            return List.of();
        }
        if (cmd.equals("sc")) return List.of();
        return super.tab(s, cmd, a);
    }
}
