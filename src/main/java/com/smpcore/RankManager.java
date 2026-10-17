package com.smpcore;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Handles ranks (member / mod / admin), permission nodes and tab-list prefixes. */
public final class RankManager {
    private final SMPCore plugin;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public RankManager(SMPCore plugin) {
        this.plugin = plugin;
    }

    public Rank rankOf(CommandSender s) {
        if (!(s instanceof Player p)) return Rank.ADMIN;
        if (p.isOp()) return Rank.ADMIN;
        PlayerData d = plugin.data().byId(p.getUniqueId());
        return d == null ? Rank.MEMBER : d.rank;
    }

    /** True if the sender has at least this rank. Console and ops count as admin. */
    public boolean has(CommandSender s, Rank needed) {
        return rankOf(s).atLeast(needed);
    }

    /** Sends a "no permission" message and returns false when the sender lacks the rank. */
    public boolean require(CommandSender s, Rank needed) {
        if (has(s, needed)) return true;
        Util.msg(s, "&cYou need the " + needed.color() + needed.display() + " &crank to do that.");
        return false;
    }

    public void set(PlayerData d, Rank rank) {
        d.rank = rank;
        Player online = plugin.getServer().getPlayer(d.uuid);
        if (online != null) apply(online);
    }

    /** (Re)applies permission nodes and tab prefix for a player. */
    public void apply(Player p) {
        remove(p);
        PlayerData d = plugin.data().get(p);
        PermissionAttachment a = p.addAttachment(plugin);
        for (Rank r : Rank.values()) {
            if (!d.rank.atLeast(r)) continue;
            for (String node : plugin.getConfig().getStringList("rank-permissions." + r.name().toLowerCase())) {
                boolean deny = node.startsWith("-");
                a.setPermission(deny ? node.substring(1) : node, !deny);
            }
        }
        attachments.put(p.getUniqueId(), a);
        p.playerListName(Util.c(d.rank.prefix() + "&f" + p.getName()));
        p.updateCommands();
    }

    public void remove(Player p) {
        PermissionAttachment old = attachments.remove(p.getUniqueId());
        if (old != null) {
            try {
                p.removeAttachment(old);
            } catch (IllegalArgumentException ignored) {
                // attachment was already removed
            }
        }
    }
}
