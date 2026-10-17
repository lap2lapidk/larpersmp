package com.smpcore;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class ShopMainMenu extends Menu {
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};

    private final Map<Integer, ShopManager.Category> bySlot = new HashMap<>();

    public ShopMainMenu(SMPCore plugin, Player p) {
        super(plugin, 45, "&6&lServer Shop");
        int i = 0;
        for (ShopManager.Category c : plugin.shop().categories()) {
            if (i >= SLOTS.length) break;
            inv.setItem(SLOTS[i], Util.item(c.icon(), c.title(),
                    "&7" + c.items().size() + " items", "", "&eClick to browse"));
            bySlot.put(SLOTS[i], c);
            i++;
        }
        inv.setItem(39, Util.item(Material.GOLD_INGOT, "&6Your balance",
                "&a" + Util.exact(plugin.economy().balance(p.getUniqueId())),
                "&b" + plugin.economy().shards(p.getUniqueId()) + " shards"));
        inv.setItem(37, Util.item(Material.SPAWNER, "&eSpawner Shop",
                "&7Buy mob spawners with", "&7money or shards.", "", "&eClick to browse"));
        inv.setItem(43, Util.item(Material.PRISMARINE_SHARD, "&bShard Shop",
                "&7Spend the shards you earn", "&7from playing and kills.", "", "&eClick to browse"));
        inv.setItem(41, Util.item(Material.EMERALD, "&aSell items",
                "&7Click to open the sell menu.", "&7Your tier sell bonus applies!"));
        border(0, 45);
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();
        if (slot == 37) {
            openLater(p, new SpawnerShopMenu(plugin, p));
            return;
        }
        if (slot == 43) {
            openLater(p, new ShardShopMenu(plugin, p));
            return;
        }
        if (slot == 41) {
            openLater(p, new SellMenu(plugin));
            return;
        }
        ShopManager.Category c = bySlot.get(slot);
        if (c != null) openLater(p, new ShopCategoryMenu(plugin, p, c, 0));
    }
}
