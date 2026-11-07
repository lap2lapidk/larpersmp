package com.smpcore;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** The /menu hub: one click to every other GUI in the plugin. */
public final class MainMenu extends Menu {
    private static final int TRAVEL = 11, SHOP = 12, SELL = 13, AH = 14, PROGRESS = 15,
            SHARDS = 21, SPAWNERS = 22, INFO = 23;

    public MainMenu(SMPCore plugin, Player p) {
        super(plugin, 36, "&6&lSMP Menu");
        inv.setItem(TRAVEL, Util.item(Material.ENDER_PEARL, "&bTravel",
                "&7Spawn, warps, homes, TPA, RTP", "", "&eClick to open"));
        inv.setItem(SHOP, Util.item(Material.EMERALD, "&aShop", "&7Buy and sell items.", "", "&eClick to open"));
        inv.setItem(SELL, Util.item(Material.HOPPER, "&aSell", "&7Sell items from your inventory.", "", "&eClick to open"));
        inv.setItem(AH, Util.item(Material.GOLD_INGOT, "&6Auction House", "&7Trade with other players.", "", "&eClick to open"));
        inv.setItem(PROGRESS, Util.item(Material.GOLDEN_PICKAXE, "&dProgression", "&7Tiers and ranks.", "", "&eClick to open"));
        inv.setItem(SHARDS, Util.item(Material.PRISMARINE_SHARD, "&bShard Shop", "&7Spend your shards.", "", "&eClick to open"));
        inv.setItem(SPAWNERS, Util.item(Material.SPAWNER, "&eSpawner Shop", "&7Money or shards.", "", "&eClick to open"));
        inv.setItem(INFO, Util.item(Material.PLAYER_HEAD, "&f" + p.getName(),
                "&7Balance: &a" + Util.exact(plugin.economy().balance(p.getUniqueId())),
                "&7Shards: &b" + plugin.economy().shards(p.getUniqueId()),
                "&7Rank: " + plugin.data().get(p).rank.prefix()));
        border(0, 36);
    }

    @Override
    public void click(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        switch (e.getSlot()) {
            case TRAVEL -> openLater(p, new TravelMenu(plugin, p));
            case SHOP -> openLater(p, new ShopMainMenu(plugin, p));
            case SELL -> openLater(p, new SellMenu(plugin));
            case AH -> openLater(p, new AuctionMenu(plugin, p, 0, false));
            case PROGRESS -> openLater(p, new ProgressMenu(plugin, p));
            case SHARDS -> openLater(p, new ShardShopMenu(plugin, p));
            case SPAWNERS -> openLater(p, new SpawnerShopMenu(plugin, p));
            default -> { }
        }
    }
}
