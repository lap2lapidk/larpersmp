package com.smpcore;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/** Paginated list of every warp. Click to teleport; admins can shift-right-click to delete. */
public final class WarpMenu extends Menu {
    private static final int PER_PAGE = 45;

    private final int page;
    private final List<String> names;
    private final boolean admin;

    public WarpMenu(SMPCore plugin, Player p, int page) {
        super(plugin, 54, "&6&lWarps &8- &7page " + (page + 1));
        this.page = page;
        this.admin = plugin.ranks().has(p, Rank.ADMIN);
        this.names = plugin.locations().warpNames();
        names.sort(String.CASE_INSENSITIVE_ORDER);

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < names.size(); i++) {
            String n = names.get(start + i);
            if (admin) {
                inv.setItem(i, Util.item(icon(n), "&e" + n, "&7Click to teleport", "", "&cShift-right-click to delete"));
            } else {
                inv.setItem(i, Util.item(icon(n), "&e" + n, "&7Click to teleport"));
            }
        }
        if (names.isEmpty()) {
            inv.setItem(22, Util.item(Material.BARRIER, "&cNo warps yet",
                    admin ? "&7Create one with &f/setwarp <name>" : "&7An admin hasn't created any."));
        }
        if (page > 0) inv.setItem(45, Util.item(Material.ARROW, "&ePrevious page"));
        inv.setItem(49, Util.item(Material.COMPASS, "&eBack to travel menu"));
        if (start + PER_PAGE < names.size()) inv.setItem(53, Util.item(Material.ARROW, "&eNext page"));
        border(45, 54);
    }

    /** Warps can be given an icon in config.yml under menus.warp-icons.<name>. */
    private Material icon(String warpName) {
        String m = plugin.getConfig().getString("menus.warp-icons." + warpName.toLowerCase(Locale.ROOT));
        if (m != null) {
            Material mat = Material.matchMaterial(m);
            if (mat != null) return mat;
        }
        Material def = Material.matchMaterial(plugin.getConfig().getString("menus.default-warp-icon", "ENDER_PEARL"));
        return def != null ? def : Material.ENDER_PEARL;
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();
        if (slot == 45 && page > 0) {
            openLater(p, new WarpMenu(plugin, p, page - 1));
            return;
        }
        if (slot == 49) {
            openLater(p, new TravelMenu(plugin, p));
            return;
        }
        if (slot == 53 && (page + 1) * PER_PAGE < names.size()) {
            openLater(p, new WarpMenu(plugin, p, page + 1));
            return;
        }
        if (slot < 0 || slot >= PER_PAGE) return;
        int idx = page * PER_PAGE + slot;
        if (idx >= names.size()) return;
        String name = names.get(idx);

        if (admin && e.getClick() == ClickType.SHIFT_RIGHT) {
            plugin.locations().delWarp(name);
            Util.msg(p, "Warp &f" + name + " &7deleted.");
            openLater(p, new WarpMenu(plugin, p, page));
            return;
        }
        p.closeInventory();
        plugin.teleports().teleport(p, plugin.locations().warp(name), "warp " + name);
    }
}
