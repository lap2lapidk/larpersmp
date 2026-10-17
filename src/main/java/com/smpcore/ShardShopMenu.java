package com.smpcore;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shop that is paid for with shards. Offers come from shards.shop in config.yml. */
public final class ShardShopMenu extends Menu {
    private static final int MAX = 45;

    public record Offer(Material material, int amount, String name, long price, String command) {}

    private final List<Offer> offers;

    public ShardShopMenu(SMPCore plugin, Player p) {
        super(plugin, 54, "&b&lShard Shop");
        offers = load(plugin);
        for (int i = 0; i < offers.size() && i < MAX; i++) {
            inv.setItem(i, display(offers.get(i)));
        }
        inv.setItem(45, Util.item(Material.ARROW, "&cBack", "&7Return to the shop"));
        refresh(p);
        border(45, 54);
    }

    private static List<Offer> load(SMPCore plugin) {
        List<Offer> out = new ArrayList<>();
        for (Map<?, ?> m : plugin.getConfig().getMapList("shards.shop")) {
            Material mat = Material.matchMaterial(String.valueOf(m.get("material")));
            if (mat == null || mat.isAir() || !mat.isItem()) {
                plugin.getLogger().warning("Shard shop: bad material '" + m.get("material") + "'");
                continue;
            }
            int amount = m.get("amount") instanceof Number n ? Math.max(1, n.intValue()) : 1;
            long price = m.get("price") instanceof Number n ? n.longValue() : 0;
            String name = m.get("name") == null ? null : String.valueOf(m.get("name"));
            String command = m.get("command") == null ? null : String.valueOf(m.get("command"));
            if (price <= 0) continue;
            out.add(new Offer(mat, amount, name, price, command));
        }
        return out;
    }

    private void refresh(Player p) {
        inv.setItem(49, Util.item(Material.PRISMARINE_SHARD, "&bYour shards",
                "&f" + plugin.economy().shards(p.getUniqueId())));
    }

    private ItemStack display(Offer o) {
        ItemStack it = new ItemStack(o.material(), Math.min(o.amount(), 64));
        ItemMeta meta = it.getItemMeta();
        if (o.name() != null) meta.displayName(Util.c(o.name()));
        List<Component> lore = new ArrayList<>();
        lore.add(Util.c(" "));
        lore.add(Util.c("&7Cost: &b" + o.price() + " shards"));
        if (o.command() == null && o.amount() > 1) lore.add(Util.c("&7You get: &f" + o.amount() + "x"));
        lore.add(Util.c(" "));
        lore.add(Util.c("&eClick to buy"));
        meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();
        if (slot == 45) {
            openLater(p, new ShopMainMenu(plugin, p));
            return;
        }
        if (slot < 0 || slot >= MAX || slot >= offers.size()) return;
        Offer o = offers.get(slot);
        if (!plugin.economy().takeShards(p.getUniqueId(), o.price())) {
            Util.msg(p, "&cYou need &b" + o.price() + " shards &cfor that.");
            return;
        }
        String label;
        if (o.command() != null && !o.command().isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), o.command().replace("%player%", p.getName()));
            label = o.name() != null ? o.name() : ShopManager.nice(o.material());
        } else {
            Util.give(p, new ItemStack(o.material(), o.amount()));
            label = o.amount() + "x " + ShopManager.nice(o.material());
        }
        Util.msg(p, "Bought &f" + label + " &7for &b" + o.price() + " shards");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
        refresh(p);
    }
}
