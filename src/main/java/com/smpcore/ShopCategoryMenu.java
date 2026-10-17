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

public final class ShopCategoryMenu extends Menu {
    private static final int PER_PAGE = 45;

    private final ShopManager.Category category;
    private final int page;

    public ShopCategoryMenu(SMPCore plugin, Player p, ShopManager.Category category, int page) {
        super(plugin, 54, category.title() + " &8- &7page " + (page + 1));
        this.category = category;
        this.page = page;

        List<ShopManager.ShopItem> items = category.items();
        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && start + i < items.size(); i++) {
            inv.setItem(i, display(items.get(start + i)));
        }
        inv.setItem(45, Util.item(Material.ARROW, "&cBack", "&7Return to the categories"));
        if (page > 0) inv.setItem(48, Util.item(Material.PAPER, "&ePrevious page"));
        if (start + PER_PAGE < items.size()) inv.setItem(50, Util.item(Material.PAPER, "&eNext page"));
        refreshBalance(p);
        border(45, 54);
    }

    private void refreshBalance(Player p) {
        inv.setItem(49, Util.item(Material.GOLD_INGOT, "&6Your balance",
                "&a" + Util.exact(plugin.economy().balance(p.getUniqueId()))));
    }

    private ItemStack display(ShopManager.ShopItem si) {
        ItemStack it = new ItemStack(si.material());
        ItemMeta meta = it.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Util.c(" "));
        lore.add(Util.c(si.buy() > 0 ? "&7Buy: &a" + Util.exact(si.buy()) : "&7Buy: &cnot sold"));
        lore.add(Util.c(si.sell() > 0 ? "&7Sell: &6" + Util.exact(si.sell()) : "&7Sell: &ccannot sell"));
        lore.add(Util.c(" "));
        lore.add(Util.c("&eLeft-click &7buy 1  &8|  &eShift-left &7buy 64"));
        lore.add(Util.c("&eRight-click &7sell 1  &8|  &eShift-right &7sell all"));
        meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        int slot = e.getSlot();

        if (slot == 45) {
            openLater(p, new ShopMainMenu(plugin, p));
            return;
        }
        if (slot == 48 && page > 0) {
            openLater(p, new ShopCategoryMenu(plugin, p, category, page - 1));
            return;
        }
        if (slot == 50 && (page + 1) * PER_PAGE < category.items().size()) {
            openLater(p, new ShopCategoryMenu(plugin, p, category, page + 1));
            return;
        }
        if (slot < 0 || slot >= PER_PAGE) return;
        int idx = page * PER_PAGE + slot;
        if (idx >= category.items().size()) return;
        ShopManager.ShopItem si = category.items().get(idx);

        ClickType t = e.getClick();
        switch (t) {
            case LEFT -> plugin.shop().buy(p, si, 1);
            case SHIFT_LEFT -> plugin.shop().buy(p, si, 64);
            case RIGHT -> plugin.shop().sell(p, si, 1);
            case SHIFT_RIGHT -> plugin.shop().sell(p, si, Integer.MAX_VALUE);
            default -> { return; }
        }
        refreshBalance(p);
    }
}
