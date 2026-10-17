package com.smpcore;

import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /spawnershop and /spawner give (admin) */
public final class SpawnerCommands extends BaseCmd {

    public SpawnerCommands(SMPCore plugin) {
        super(plugin);
    }

    @Override
    protected void run(CommandSender s, String cmd, String[] a) {
        if (cmd.equals("spawnershop")) {
            Player p = player(s);
            if (p != null) new SpawnerShopMenu(plugin, p).open(p);
            return;
        }
        // /spawner give <player> <mob> [amount]
        if (!plugin.ranks().require(s, Rank.ADMIN)) return;
        if (a.length < 3 || !a[0].equalsIgnoreCase("give")) {
            usage(s, "/spawner give <player> <mob> [amount]");
            return;
        }
        Player t = target(s, a[1]);
        if (t == null) return;
        EntityType type = SpawnerManager.parseType(a[2]);
        if (type == null) {
            Util.msg(s, "&cUnknown or unspawnable mob: &f" + a[2]);
            return;
        }
        int amount = 1;
        if (a.length > 3) {
            try {
                amount = Integer.parseInt(a[3]);
            } catch (NumberFormatException ex) {
                amount = -1;
            }
            if (amount < 1 || amount > 64) {
                Util.msg(s, "&cAmount must be between 1 and 64.");
                return;
            }
        }
        Util.give(t, plugin.spawners().create(type, amount));
        Util.msg(s, "Gave &f" + amount + "x " + SpawnerManager.niceName(type) + " Spawner &7to &f" + t.getName());
        if (!t.equals(s)) Util.msg(t, "You received &f" + amount + "x &e" + SpawnerManager.niceName(type) + " Spawner&7.");
    }

    @Override
    protected List<String> tab(CommandSender s, String cmd, String[] a) {
        if (cmd.equals("spawnershop")) return List.of();
        if (!plugin.ranks().has(s, Rank.ADMIN)) return List.of();
        if (a.length == 1) return filter(List.of("give"), a[0]);
        if (a.length == 2) return names(s, a[1]);
        if (a.length == 3) {
            List<String> all = new ArrayList<>();
            for (EntityType t : Registry.ENTITY_TYPE) {
                if (t.isSpawnable() && t.isAlive()) all.add(t.getKey().getKey());
            }
            return filter(all, a[2].toLowerCase(Locale.ROOT));
        }
        return List.of();
    }
}
