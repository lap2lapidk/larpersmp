package com.smpcore;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Everything teleport-related in one place: spawn, warps, homes, TPA, RTP and /back. */
public final class TravelMenu extends Menu {
    private static final int SPAWN = 13, WARPS = 20, HOMES = 22, RTP = 24, TPA = 29, TPAHERE = 31, BACK = 33, HUB = 40;

    public TravelMenu(SMPCore plugin, Player p) {
        super(plugin, 45, "&6&lTravel");
        inv.setItem(SPAWN, Util.item(Material.RESPAWN_ANCHOR, "&a&lSpawn",
                "&7Teleport to the server spawn.", "", "&eClick to teleport"));
        inv.setItem(WARPS, Util.item(Material.ENDER_PEARL, "&bWarps",
                "&7" + plugin.locations().warpNames().size() + " available", "", "&eClick to browse"));
        int homes = plugin.data().get(p).homes.size();
        inv.setItem(HOMES, Util.item(Material.RED_BED, "&eHomes",
                "&7" + homes + "/" + plugin.progression().maxHomes(p) + " set", "", "&eClick to manage"));
        inv.setItem(RTP, Util.item(Material.GRASS_BLOCK, "&2Random Teleport",
                "&7Drop into the wilderness.", "", "&eClick to teleport"));
        inv.setItem(TPA, Util.item(Material.ENDER_EYE, "&dTPA",
                "&7Ask to teleport to a player.", "", "&eClick to choose a player"));
        inv.setItem(TPAHERE, Util.item(Material.PLAYER_HEAD, "&dTPA Here",
                "&7Ask a player to come to you.", "", "&eClick to choose a player"));
        inv.setItem(BACK, Util.item(Material.CLOCK, "&6Back",
                "&7Return to your last location.", "", "&eClick to teleport"));
        inv.setItem(HUB, Util.item(Material.NETHER_STAR, "&eMain menu"));
        border(0, 45);
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        switch (e.getSlot()) {
            case SPAWN -> {
                p.closeInventory();
                plugin.teleports().teleport(p, plugin.locations().spawn(), "spawn");
            }
            case WARPS -> openLater(p, new WarpMenu(plugin, p, 0));
            case HOMES -> openLater(p, new HomesMenu(plugin, p));
            case RTP -> {
                p.closeInventory();
                plugin.teleports().rtp(p);
            }
            case TPA -> openLater(p, new PlayerSelectMenu(plugin, p, false, 0));
            case TPAHERE -> openLater(p, new PlayerSelectMenu(plugin, p, true, 0));
            case BACK -> {
                p.closeInventory();
                var l = plugin.teleports().getBack(p);
                if (l == null) Util.msg(p, "&cYou have nowhere to go back to.");
                else plugin.teleports().teleport(p, l, "your previous location");
            }
            case HUB -> openLater(p, new MainMenu(plugin, p));
            default -> { }
        }
    }
}
