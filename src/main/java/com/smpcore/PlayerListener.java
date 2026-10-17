package com.smpcore;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public final class PlayerListener implements Listener {
    private final SMPCore plugin;

    public PlayerListener(SMPCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean first = !plugin.data().exists(p.getUniqueId());
        PlayerData d = plugin.data().get(p);

        if (first) {
            d.money = plugin.getConfig().getDouble("starting-balance", 1000);
            d.shards = plugin.getConfig().getLong("shards.starting", 0);
            giveStarterKit(p);
            Location spawn = plugin.locations().spawn();
            if (spawn != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> p.teleportAsync(spawn), 2L);
            }
        }

        plugin.setup(p);
        plugin.refreshVanish(p);

        if (plugin.isVanished(p.getUniqueId())) {
            e.joinMessage(null);
        } else if (first) {
            e.joinMessage(Util.c("&e★ &fWelcome &b" + p.getName() + " &fto the server for the first time!"));
        } else {
            e.joinMessage(Util.c("&8[&a+&8] " + d.rank.color() + p.getName()));
        }
    }

    private void giveStarterKit(Player p) {
        for (Map<?, ?> m : plugin.getConfig().getMapList("starter-kit")) {
            Material mat = Material.matchMaterial(String.valueOf(m.get("material")));
            if (mat == null) continue;
            int amount = m.get("amount") instanceof Number n ? n.intValue() : 1;
            Util.give(p, new ItemStack(mat, Math.max(1, amount)));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        PlayerData d = plugin.data().get(p);
        plugin.ranks().remove(p);
        plugin.scoreboards().remove(p);
        plugin.clearStaffChat(p);
        if (plugin.isVanished(p.getUniqueId())) {
            e.quitMessage(null);
        } else {
            e.quitMessage(Util.c("&8[&c-&8] " + d.rank.color() + p.getName()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        PlayerData d = plugin.data().get(p);

        if (d.isMuted()) {
            e.setCancelled(true);
            Util.msg(p, "&cYou are muted.");
            return;
        }
        if (plugin.inStaffChat(p)) {
            e.setCancelled(true);
            String text = PlainTextComponentSerializer.plainText().serialize(e.message());
            Bukkit.getScheduler().runTask(plugin, () -> plugin.staffChat(p, text));
            return;
        }
        final String prefix = d.rank.prefix();
        e.renderer(ChatRenderer.viewerUnaware((source, name, message) ->
                Util.c(prefix + "&f" + source.getName() + "&8: ")
                        .append(message.colorIfAbsent(NamedTextColor.WHITE))));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        plugin.data().get(victim).deaths++;
        plugin.teleports().setBack(victim, victim.getLocation());

        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            plugin.data().get(killer).kills++;
            double reward = plugin.getConfig().getDouble("kill-reward", 100);
            long shardReward = plugin.getConfig().getLong("shards.per-kill", 1);
            StringBuilder bar = new StringBuilder();
            if (reward > 0) {
                plugin.economy().deposit(killer.getUniqueId(), reward);
                bar.append("&a+").append(Util.exact(reward));
            }
            if (shardReward > 0) {
                plugin.economy().addShards(killer.getUniqueId(), shardReward);
                if (bar.length() > 0) bar.append(" &8| ");
                bar.append("&b+").append(shardReward).append(shardReward == 1 ? " shard" : " shards");
            }
            if (bar.length() > 0) {
                killer.sendActionBar(Util.c(bar + " &7for killing &f" + victim.getName()));
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (e.isBedSpawn() || e.isAnchorSpawn()) return;
        Location spawn = plugin.locations().spawn();
        if (spawn != null) e.setRespawnLocation(spawn);
    }
}
