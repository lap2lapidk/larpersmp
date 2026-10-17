package com.smpcore;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SpawnerShopMenu extends Menu {
    private static final int MAX = 45;

    private final List<SpawnerManager.Entry> entries;

    public SpawnerShopMenu(SMPCore plugin, Player p) {
        super(plugin, 54, "&e&lSpawner Shop");
        entries = plugin.spawners().shop();
        if (entries.size() > MAX) {
            plugin.getLogger().warning("Spawner shop has more than " + MAX + " entries; extras are hidden.");
        }
        for (int i = 0; i < entries.size() && i < MAX; i++) {
            inv.setItem(i, display(entries.get(i)));
        }
        inv.setItem(45, Util.item(Material.ARROW, "&cBack", "&7Return to the shop"));
        refresh(p);
        border(45, 54);
    }

    private void refresh(Player p) {
        inv.setItem(49, Util.item(Material.GOLD_INGOT, "&6Your balance",
                "&a" + Util.exact(plugin.economy().balance(p.getUniqueId())),
                "&b" + plugin.economy().shards(p.getUniqueId()) + " shards"));
    }

    private ItemStack display(SpawnerManager.Entry en) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(en.price() > 0 ? "&7Money: &a" + Util.exact(en.price()) : "&7Money: &cnot sold");
        lore.add(en.shards() > 0 ? "&7Shards: &b" + en.shards() : "&7Shards: &cnot sold");
        lore.add(" ");
        lore.add("&eLeft-click &7buy with money");
        lore.add("&eRight-click &7buy with shards");
        return Util.item(SpawnerManager.iconFor(en.type()),
                "&e" + SpawnerManager.niceName(en.type()) + " Spawner", lore);
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();
        if (slot == 45) {
            openLater(p, new ShopMainMenu(plugin, p));
            return;
        }
        if (slot < 0 || slot >= MAX || slot >= entries.size()) return;
        SpawnerManager.Entry en = entries.get(slot);
        ClickType t = e.getClick();
        if (t == ClickType.LEFT || t == ClickType.SHIFT_LEFT) plugin.spawners().buyWithMoney(p, en);
        else if (t == ClickType.RIGHT || t == ClickType.SHIFT_RIGHT) plugin.spawners().buyWithShards(p, en);
        else return;
        refresh(p);
    }
}
