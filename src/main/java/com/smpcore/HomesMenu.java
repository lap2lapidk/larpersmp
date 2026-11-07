package com.smpcore;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows every home slot the player owns. Click a bed to teleport, shift-right-click to delete,
 * click an empty slot to set a new home where you stand.
 */
public final class HomesMenu extends Menu {
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34};

    private final List<String> homeNames;
    private final int max;
    private final int shown;

    public HomesMenu(SMPCore plugin, Player p) {
        super(plugin, 45, "&6&lYour Homes");
        PlayerData d = plugin.data().get(p);
        this.homeNames = new ArrayList<>(d.homes.keySet());
        this.max = plugin.progression().maxHomes(p);
        this.shown = Math.min(Math.max(max, homeNames.size()), SLOTS.length);

        for (int i = 0; i < shown; i++) {
            if (i < homeNames.size()) {
                String n = homeNames.get(i);
                Location l = d.homes.get(n);
                inv.setItem(SLOTS[i], Util.item(Material.RED_BED, "&e" + n,
                        "&7World: &f" + (l != null && l.getWorld() != null ? l.getWorld().getName() : "?"),
                        l == null ? "" : "&7XYZ: &f" + l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ(),
                        "",
                        "&eClick to teleport",
                        "&cShift-right-click to delete"));
            } else {
                inv.setItem(SLOTS[i], Util.item(Material.GRAY_BED, "&7Empty home slot",
                        "&eClick to set a home here"));
            }
        }
        inv.setItem(40, Util.item(Material.COMPASS, "&eBack to travel menu"));
        inv.setItem(39, Util.item(Material.PAPER, "&6Homes: &f" + homeNames.size() + "&7/&f" + max,
                "&7Rank up on &f/progress", "&7to unlock more homes."));
        border(0, 45);
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();
        if (slot == 40) {
            openLater(p, new TravelMenu(plugin, p));
            return;
        }
        int idx = -1;
        for (int i = 0; i < shown; i++) if (SLOTS[i] == slot) idx = i;
        if (idx < 0) return;

        PlayerData d = plugin.data().get(p);
        if (idx < homeNames.size()) {
            String name = homeNames.get(idx);
            if (e.getClick() == ClickType.SHIFT_RIGHT) {
                d.homes.remove(name);
                Util.msg(p, "Home &f" + name + " &7deleted.");
                openLater(p, new HomesMenu(plugin, p));
                return;
            }
            Location l = d.homes.get(name);
            p.closeInventory();
            plugin.teleports().teleport(p, l, "home " + name);
            return;
        }
        // empty slot: set a home with the first free default name
        if (d.homes.size() >= max) {
            Util.msg(p, "&cYou've reached your home limit (&f" + max + "&c).");
            return;
        }
        String name = "home";
        int n = 2;
        while (d.homes.containsKey(name)) name = "home" + n++;
        d.homes.put(name, p.getLocation().clone());
        Util.msg(p, "Home &f" + name + " &7set. (" + d.homes.size() + "/" + max + ")");
        openLater(p, new HomesMenu(plugin, p));
    }
}
