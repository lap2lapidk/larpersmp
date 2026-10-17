package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Base class for every GUI in the plugin. */
public abstract class Menu implements InventoryHolder {
    protected final SMPCore plugin;
    protected final Inventory inv;

    protected Menu(SMPCore plugin, int size, String title) {
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, size, Util.c(title));
    }

    /** Called for clicks inside this menu's own (top) inventory. The click is already cancelled. */
    public abstract void click(InventoryClickEvent e);

    public void close(InventoryCloseEvent e) {}

    /** Menus that let players move items in and out (like the sell menu) return true. */
    public boolean allowMove() {
        return false;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void open(Player p) {
        p.openInventory(inv);
    }

    /** Opens another menu on the next tick (safe to call from inside a click handler). */
    protected void openLater(Player p, Menu next) {
        Bukkit.getScheduler().runTask(plugin, () -> next.open(p));
    }

    protected void border(int from, int to) {
        ItemStack pane = Util.item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = from; i < to; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, pane.clone());
        }
    }
}
