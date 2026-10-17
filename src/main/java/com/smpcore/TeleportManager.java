package com.smpcore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Warmup teleports, TPA requests, /back and /rtp. */
public final class TeleportManager {

    public record Request(UUID from, boolean here, long expires) {}

    private static final Set<Material> UNSAFE = EnumSet.of(
            Material.WATER, Material.LAVA, Material.MAGMA_BLOCK, Material.CACTUS, Material.FIRE,
            Material.SOUL_FIRE, Material.POWDER_SNOW, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.BUBBLE_COLUMN, Material.KELP,
            Material.SEAGRASS, Material.TALL_SEAGRASS);

    private final SMPCore plugin;
    private final Map<UUID, Request> requests = new HashMap<>();
    private final Map<UUID, Location> back = new HashMap<>();
    private final Map<UUID, Long> rtpCooldown = new HashMap<>();
    private final Set<UUID> pending = new HashSet<>();
    private final Random random = new Random();

    public TeleportManager(SMPCore plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- back

    public void setBack(Player p, Location l) {
        back.put(p.getUniqueId(), l.clone());
    }

    public Location getBack(Player p) {
        return back.get(p.getUniqueId());
    }

    // ------------------------------------------------------------ teleport

    /** Teleports with a warmup (cancelled if the player moves). Mods and admins skip the warmup. */
    public void teleport(Player p, Location dest, String label) {
        if (dest == null || dest.getWorld() == null) {
            Util.msg(p, "&cThat location is not available.");
            return;
        }
        if (pending.contains(p.getUniqueId())) {
            Util.msg(p, "&cYou are already teleporting.");
            return;
        }
        int warmup = plugin.getConfig().getInt("teleport.warmup-seconds", 3);
        if (warmup <= 0 || plugin.ranks().has(p, Rank.MOD)) {
            go(p, dest, label);
            return;
        }
        pending.add(p.getUniqueId());
        final Location start = p.getLocation();
        new BukkitRunnable() {
            int left = warmup;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    pending.remove(p.getUniqueId());
                    cancel();
                    return;
                }
                Location now = p.getLocation();
                if (now.getWorld() != start.getWorld() || now.distanceSquared(start) > 1.5) {
                    pending.remove(p.getUniqueId());
                    p.sendActionBar(Util.c("&cTeleport cancelled - you moved."));
                    cancel();
                    return;
                }
                if (left <= 0) {
                    pending.remove(p.getUniqueId());
                    cancel();
                    go(p, dest, label);
                    return;
                }
                p.sendActionBar(Util.c("&eTeleporting to &f" + label + " &ein &6" + left + "s &e- don't move!"));
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void go(Player p, Location dest, String label) {
        setBack(p, p.getLocation());
        p.teleportAsync(dest).thenAccept(ok -> {
            if (ok) {
                p.sendActionBar(Util.c("&aTeleported to &f" + label));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1f);
            }
        });
    }

    // ----------------------------------------------------------------- TPA

    public void sendRequest(Player from, Player to, boolean here) {
        long timeout = plugin.getConfig().getLong("teleport.tpa-timeout-seconds", 60) * 1000L;
        requests.put(to.getUniqueId(), new Request(from.getUniqueId(), here, System.currentTimeMillis() + timeout));
        Util.msg(from, "Request sent to &f" + to.getName() + "&7. It expires in "
                + (timeout / 1000) + "s.");
        Util.msg(to, "&f" + from.getName() + " &7wants " + (here ? "you to teleport to them." : "to teleport to you."));
        to.sendMessage(Util.c("   ")
                .append(Util.c("&a&l[ACCEPT]").clickEvent(ClickEvent.runCommand("/tpaccept")))
                .append(Component.text("  "))
                .append(Util.c("&c&l[DENY]").clickEvent(ClickEvent.runCommand("/tpadeny"))));
    }

    /** Returns the live request for this player, or null (and tells them why). */
    public Request takeRequest(Player target) {
        Request r = requests.remove(target.getUniqueId());
        if (r == null || r.expires() < System.currentTimeMillis()) {
            Util.msg(target, "&cYou have no pending teleport requests.");
            return null;
        }
        return r;
    }

    // ----------------------------------------------------------------- RTP

    public void rtp(Player p) {
        if (!plugin.ranks().has(p, Rank.MOD)) {
            long cd = plugin.getConfig().getLong("rtp.cooldown-seconds", 30) * 1000L;
            long last = rtpCooldown.getOrDefault(p.getUniqueId(), 0L);
            long wait = last + cd - System.currentTimeMillis();
            if (wait > 0) {
                Util.msg(p, "&cYou can use /rtp again in " + (wait / 1000 + 1) + "s.");
                return;
            }
        }
        World w = Bukkit.getWorld(plugin.getConfig().getString("rtp.world", "world"));
        if (w == null) w = p.getWorld();
        Util.msg(p, "Searching for a safe spot...");
        attempt(p, w, 0);
    }

    private void attempt(Player p, World w, int tries) {
        if (!p.isOnline()) return;
        if (tries >= 10) {
            Util.msg(p, "&cCouldn't find a safe location. Try again.");
            return;
        }
        int min = plugin.getConfig().getInt("rtp.min-radius", 500);
        int max = Math.max(min + 1, plugin.getConfig().getInt("rtp.max-radius", 5000));
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = min + random.nextDouble() * (max - min);
        int x = (int) (Math.cos(angle) * radius);
        int z = (int) (Math.sin(angle) * radius);

        w.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    int y = w.getHighestBlockYAt(x, z);
                    Block ground = w.getBlockAt(x, y, z);
                    Material m = ground.getType();
                    Location dest = new Location(w, x + 0.5, y + 1, z + 0.5);
                    boolean safe = m.isSolid() && !UNSAFE.contains(m) && !Tag.LEAVES.isTagged(m)
                            && w.getWorldBorder().isInside(dest);
                    if (!safe) {
                        attempt(p, w, tries + 1);
                        return;
                    }
                    rtpCooldown.put(p.getUniqueId(), System.currentTimeMillis());
                    teleport(p, dest, "a random location");
                }));
    }
}
