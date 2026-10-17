package com.smpcore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public final class MenuListener implements Listener {
    private final SMPCore plugin;

    public MenuListener(SMPCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof Menu m) {
            if (m.allowMove()) return;
            e.setCancelled(true);
            if (e.getClickedInventory() == top) m.click(e);
            return;
        }
        // /invsee: mods may look, only admins may edit
        if (top.getHolder() instanceof Player target
                && e.getWhoClicked() instanceof Player viewer
                && !target.getUniqueId().equals(viewer.getUniqueId())
                && !plugin.ranks().has(viewer, Rank.ADMIN)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof Menu m && !m.allowMove()) {
            e.setCancelled(true);
            return;
        }
        if (top.getHolder() instanceof Player target
                && e.getWhoClicked() instanceof Player viewer
                && !target.getUniqueId().equals(viewer.getUniqueId())
                && !plugin.ranks().has(viewer, Rank.ADMIN)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu m) {
            m.close(e);
        }
    }
}
