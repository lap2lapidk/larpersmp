package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Pick an online player (heads) to send a /tpa or /tpahere request to. */
public final class PlayerSelectMenu extends Menu {
    private static final int PER_PAGE = 45;

    private final boolean here;
    private final int page;
    private final List<UUID> players = new ArrayList<>();

    public PlayerSelectMenu(SMPCore plugin, Player viewer, boolean here, int page) {
        super(plugin, 54, (here ? "&6&lTPA Here" : "&6&lTPA") + " &8- &7choose a player");
        this.here = here;
        this.page = page;

        boolean staff = plugin.ranks().has(viewer, Rank.MOD);
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.getUniqueId().equals(viewer.getUniqueId())) continue;
            if (plugin.isVanished(o.getUniqueId()) && !staff) continue;
            players.add(o.getUniqueId());
        }
        players.sort((a, b) -> {
            Player pa = Bukkit.getPlayer(a), pb = Bukkit.getPlayer(b);
            return pa.getName().compareToIgnoreCase(pb.getName());
        });

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < players.size(); i++) {
            Player o = Bukkit.getPlayer(players.get(start + i));
            if (o == null) continue;
            ItemStack head = Util.item(Material.PLAYER_HEAD, "&e" + o.getName(),
                    "&7Rank: " + plugin.data().get(o).rank.prefix(),
                    "",
                    here ? "&eClick to ask them to come to you" : "&eClick to ask to teleport to them");
            if (head.getItemMeta() instanceof SkullMeta sm) {
                sm.setOwningPlayer(o);
                head.setItemMeta(sm);
            }
            inv.setItem(i, head);
        }
        if (players.isEmpty()) {
            inv.setItem(22, Util.item(Material.BARRIER, "&cNobody else is online"));
        }
        if (page > 0) inv.setItem(45, Util.item(Material.ARROW, "&ePrevious page"));
        inv.setItem(47, Util.item(here ? Material.ENDER_PEARL : Material.ENDER_EYE,
                "&eMode: &f" + (here ? "TPA Here" : "TPA"),
                here ? "&7They teleport to you." : "&7You teleport to them.", "", "&eClick to switch"));
        inv.setItem(49, Util.item(Material.COMPASS, "&eBack to travel menu"));
        if (start + PER_PAGE < players.size()) inv.setItem(53, Util.item(Material.ARROW, "&eNext page"));
        border(45, 54);
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();
        if (slot == 45 && page > 0) {
            openLater(p, new PlayerSelectMenu(plugin, p, here, page - 1));
            return;
        }
        if (slot == 47) {
            openLater(p, new PlayerSelectMenu(plugin, p, !here, 0));
            return;
        }
        if (slot == 49) {
            openLater(p, new TravelMenu(plugin, p));
            return;
        }
        if (slot == 53 && (page + 1) * PER_PAGE < players.size()) {
            openLater(p, new PlayerSelectMenu(plugin, p, here, page + 1));
            return;
        }
        if (slot < 0 || slot >= PER_PAGE) return;
        int idx = page * PER_PAGE + slot;
        if (idx >= players.size()) return;
        Player target = Bukkit.getPlayer(players.get(idx));
        p.closeInventory();
        if (target == null) {
            Util.msg(p, "&cThat player went offline.");
            return;
        }
        plugin.teleports().sendRequest(p, target, here);
    }
}
