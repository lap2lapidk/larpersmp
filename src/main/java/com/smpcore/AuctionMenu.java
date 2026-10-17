package com.smpcore;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class AuctionMenu extends Menu {
    private static final int PER_PAGE = 45;

    private final int page;
    private final boolean byPrice;
    private final List<AuctionManager.Listing> view;

    public AuctionMenu(SMPCore plugin, Player p, int page, boolean byPrice) {
        super(plugin, 54, "&6&lAuction House &8- &7page " + (page + 1));
        this.page = page;
        this.byPrice = byPrice;
        this.view = plugin.auction().sorted(byPrice);

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < view.size(); i++) {
            inv.setItem(i, display(view.get(start + i), p));
        }
        if (page > 0) inv.setItem(45, Util.item(Material.ARROW, "&ePrevious page"));
        inv.setItem(47, Util.item(Material.HOPPER, "&eSort: " + (byPrice ? "&fLowest price" : "&fNewest first"),
                "&7Click to change sorting"));
        int waiting = plugin.auction().claimCount(p.getUniqueId());
        inv.setItem(49, Util.item(Material.CHEST, "&aClaim items",
                "&7Expired / returned items waiting: &f" + waiting, "", "&eClick to claim"));
        inv.setItem(51, Util.item(Material.GOLD_INGOT, "&6Your balance",
                "&a" + Util.exact(plugin.economy().balance(p.getUniqueId())),
                "", "&7Sell an item: &f/ah sell <price>"));
        if (start + PER_PAGE < view.size()) inv.setItem(53, Util.item(Material.ARROW, "&eNext page"));
        border(45, 54);
    }

    private ItemStack display(AuctionManager.Listing l, Player viewer) {
        ItemStack it = l.item().clone();
        ItemMeta meta = it.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) lore.addAll(meta.lore());
        lore.add(Util.c(" "));
        lore.add(Util.c("&7Price: &a" + Util.exact(l.price())));
        lore.add(Util.c("&7Seller: &f" + l.sellerName()));
        long left = Math.max(0, (l.expires() - System.currentTimeMillis()) / 1000);
        lore.add(Util.c("&7Expires in: &f" + Util.time(left)));
        lore.add(Util.c(" "));
        if (l.seller().equals(viewer.getUniqueId())) {
            lore.add(Util.c("&cShift-right-click to cancel"));
        } else {
            lore.add(Util.c("&eClick to buy"));
        }
        meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        if (slot == 45 && page > 0) {
            openLater(p, new AuctionMenu(plugin, p, page - 1, byPrice));
            return;
        }
        if (slot == 47) {
            openLater(p, new AuctionMenu(plugin, p, 0, !byPrice));
            return;
        }
        if (slot == 49) {
            int n = plugin.auction().claim(p);
            if (n == 0) Util.msg(p, "&cYou have nothing to claim.");
            else Util.msg(p, "Claimed &f" + n + " &7item stack(s).");
            openLater(p, new AuctionMenu(plugin, p, page, byPrice));
            return;
        }
        if (slot == 53 && (page + 1) * PER_PAGE < view.size()) {
            openLater(p, new AuctionMenu(plugin, p, page + 1, byPrice));
            return;
        }
        if (slot < 0 || slot >= PER_PAGE) return;
        int idx = page * PER_PAGE + slot;
        if (idx >= view.size()) return;
        AuctionManager.Listing l = view.get(idx);

        if (l.seller().equals(p.getUniqueId())) {
            if (e.getClick() == ClickType.SHIFT_RIGHT) {
                plugin.auction().cancel(p, l.id());
                openLater(p, new AuctionMenu(plugin, p, page, byPrice));
            }
            return;
        }
        openLater(p, new AuctionConfirmMenu(plugin, l));
    }
}
