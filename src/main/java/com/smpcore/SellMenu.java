package com.smpcore;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Put items in, close the menu, everything sellable is sold. The rest is handed back. */
public final class SellMenu extends Menu {

    public SellMenu(SMPCore plugin) {
        super(plugin, 54, "&a&lSell &8- &7put items in, then close");
    }

    @Override
    public boolean allowMove() {
        return true;
    }

    @Override
    public void click(InventoryClickEvent e) {
        // movement is allowed, nothing to handle
    }

    @Override
    public void close(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack it : inv.getContents()) {
            if (it != null && !it.getType().isAir()) items.add(it);
        }
        inv.clear();
        if (items.isEmpty()) return;
        plugin.shop().sellAndReturn(p, items);
    }
}
