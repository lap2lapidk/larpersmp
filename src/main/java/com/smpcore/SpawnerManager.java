package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Spawner items, the spawner shop, silk-touch pickup and placement handling. */
public final class SpawnerManager implements Listener {

    public record Entry(EntityType type, double price, long shards) {}

    private final SMPCore plugin;
    private final NamespacedKey typeKey;

    public SpawnerManager(SMPCore plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "spawner_type");
    }

    // -------------------------------------------------------------- helpers

    /** Parses "zombie", "ZOMBIE" or "minecraft:zombie". Null if not a spawnable living mob. */
    public static EntityType parseType(String s) {
        if (s == null) return null;
        String k = s.toLowerCase(Locale.ROOT).trim();
        if (k.startsWith("minecraft:")) k = k.substring("minecraft:".length());
        try {
            EntityType t = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(k));
            if (t == null || !t.isSpawnable() || !t.isAlive()) return null;
            return t;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String niceName(EntityType t) {
        StringBuilder sb = new StringBuilder();
        for (String part : t.getKey().getKey().split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** Spawn egg for the mob, or a spawner block when that mob has no egg. */
    public static Material iconFor(EntityType t) {
        Material egg = Material.matchMaterial(t.getKey().getKey().toUpperCase(Locale.ROOT) + "_SPAWN_EGG");
        return egg == null ? Material.SPAWNER : egg;
    }

    public List<Entry> shop() {
        List<Entry> out = new ArrayList<>();
        for (Map<?, ?> m : plugin.getConfig().getMapList("spawners.shop")) {
            EntityType t = parseType(String.valueOf(m.get("type")));
            if (t == null) {
                plugin.getLogger().warning("Spawner shop: unknown or unspawnable mob '" + m.get("type") + "'");
                continue;
            }
            double price = m.get("price") instanceof Number n ? n.doubleValue() : 0;
            long shards = m.get("shards") instanceof Number n ? n.longValue() : 0;
            out.add(new Entry(t, price, shards));
        }
        return out;
    }

    // ---------------------------------------------------------------- items

    public ItemStack create(EntityType type, int amount) {
        ItemStack it = new ItemStack(Material.SPAWNER, Math.max(1, amount));
        if (it.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof CreatureSpawner cs) {
            cs.setSpawnedType(type);
            meta.setBlockState(cs);
            meta.displayName(Util.c("&e" + niceName(type) + " Spawner"));
            meta.lore(List.of(Util.c("&7Place it to spawn &f" + niceName(type) + "&7.")));
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.getKey().getKey());
            it.setItemMeta(meta);
        }
        return it;
    }

    /** The mob a spawner item will spawn, or null if it is not a (typed) spawner item. */
    public EntityType typeOf(ItemStack it) {
        if (it == null || it.getType() != Material.SPAWNER || !it.hasItemMeta()) return null;
        ItemMeta m = it.getItemMeta();
        String s = m.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (s != null) return parseType(s);
        if (m instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof CreatureSpawner cs) {
            return cs.getSpawnedType();
        }
        return null;
    }

    // ------------------------------------------------------------- buying

    public void buyWithMoney(Player p, Entry en) {
        if (en.price() <= 0) {
            Util.msg(p, "&cThat spawner can't be bought with money.");
            return;
        }
        if (!plugin.economy().withdraw(p.getUniqueId(), en.price())) {
            Util.msg(p, "&cYou can't afford that. Price: &f" + Util.exact(en.price()));
            return;
        }
        Util.give(p, create(en.type(), 1));
        Util.msg(p, "Bought a &e" + niceName(en.type()) + " Spawner &7for &c" + Util.exact(en.price()));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
    }

    public void buyWithShards(Player p, Entry en) {
        if (en.shards() <= 0) {
            Util.msg(p, "&cThat spawner can't be bought with shards.");
            return;
        }
        if (!plugin.economy().takeShards(p.getUniqueId(), en.shards())) {
            Util.msg(p, "&cYou need &b" + en.shards() + " shards &cfor that.");
            return;
        }
        Util.give(p, create(en.type(), 1));
        Util.msg(p, "Bought a &e" + niceName(en.type()) + " Spawner &7for &b" + en.shards() + " shards");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
    }

    // ------------------------------------------------------------ listeners

    /** Make sure a placed spawner keeps the mob type of the item it came from. */
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (b.getType() != Material.SPAWNER) return;
        EntityType type = typeOf(e.getItemInHand());
        if (type == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (b.getType() != Material.SPAWNER) return;
            if (b.getState() instanceof CreatureSpawner cs) {
                cs.setSpawnedType(type);
                cs.update(true, false);
            }
        });
    }

    /** Silk touch picks a spawner up (keeping its mob) instead of destroying it. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.SPAWNER) return;
        if (!plugin.getConfig().getBoolean("spawners.silk-touch-pickup", true)) return;
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.getType().isAir() || !tool.containsEnchantment(Enchantment.SILK_TOUCH)) return;
        if (!(b.getState() instanceof CreatureSpawner cs)) return;
        EntityType type = cs.getSpawnedType();
        if (type == null) return;
        e.setExpToDrop(0);
        e.setDropItems(false);
        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), create(type, 1));
    }
}
