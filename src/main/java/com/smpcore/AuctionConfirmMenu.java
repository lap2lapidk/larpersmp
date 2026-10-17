package com.smpcore;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class AuctionConfirmMenu extends Menu {
    private final AuctionManager.Listing listing;

    public AuctionConfirmMenu(SMPCore plugin, AuctionManager.Listing listing) {
        super(plugin, 27, "&6Confirm purchase");
        this.listing = listing;

        ItemStack shown = listing.item().clone();
        ItemMeta meta = shown.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) lore.addAll(meta.lore());
        lore.add(Util.c(" "));
        lore.add(Util.c("&7Price: &a" + Util.exact(listing.price())));
        lore.add(Util.c("&7Seller: &f" + listing.sellerName()));
        meta.lore(lore);
        shown.setItemMeta(meta);

        inv.setItem(11, Util.item(Material.LIME_STAINED_GLASS_PANE, "&a&lConfirm",
                "&7Buy for &a" + Util.exact(listing.price())));
        inv.setItem(13, shown);
        inv.setItem(15, Util.item(Material.RED_STAINED_GLASS_PANE, "&c&lCancel", "&7Go back"));
        border(0, 27);
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();
        if (slot == 11) {
            plugin.auction().buy(p, listing.id());
            Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory());
        } else if (slot == 15) {
            openLater(p, new AuctionMenu(plugin, p, 0, false));
        }
    }
}
