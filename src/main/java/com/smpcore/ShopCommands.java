package com.smpcore;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ShopCommands extends BaseCmd {

    public ShopCommands(SMPCore plugin) {
        super(plugin);
    }

    @Override
    protected void run(CommandSender s, String cmd, String[] a) {
        Player p = player(s);
        if (p == null) return;
        switch (cmd) {
            case "shop" -> new ShopMainMenu(plugin, p).open(p);
            case "sell" -> sell(p, a);
            case "ah" -> auction(p, a);
            default -> { }
        }
    }

    private void sell(Player p, String[] a) {
        if (a.length == 0) {
            new SellMenu(plugin).open(p);
            return;
        }
        switch (a[0].toLowerCase()) {
            case "hand" -> plugin.shop().sellHand(p);
            case "all" -> plugin.shop().sellAll(p);
            default -> usage(p, "/sell [hand|all]");
        }
    }

    private void auction(Player p, String[] a) {
        if (a.length == 0) {
            new AuctionMenu(plugin, p, 0, false).open(p);
            return;
        }
        switch (a[0].toLowerCase()) {
            case "claim" -> {
                int n = plugin.auction().claim(p);
                Util.msg(p, n == 0 ? "&cYou have nothing to claim." : "Claimed &f" + n + " &7item stack(s).");
            }
            case "sell" -> listItem(p, a);
            default -> usage(p, "/ah [sell <price>|claim]");
        }
    }

    private void listItem(Player p, String[] a) {
        if (a.length < 2) {
            usage(p, "/ah sell <price>");
            return;
        }
        double price = Util.parseAmount(a[1]);
        double min = plugin.getConfig().getDouble("auction.min-price", 1);
        double max = plugin.getConfig().getDouble("auction.max-price", 1_000_000_000);
        if (price < min || price > max) {
            Util.msg(p, "&cPrice must be between &f" + Util.exact(min) + " &cand &f" + Util.exact(max) + "&c.");
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            Util.msg(p, "&cHold the item you want to sell.");
            return;
        }
        int limit = plugin.getConfig().getInt("auction.max-listings", 10);
        if (plugin.auction().countBy(p.getUniqueId()) >= limit) {
            Util.msg(p, "&cYou can only have &f" + limit + " &clistings at once.");
            return;
        }
        ItemStack listed = hand.clone();
        p.getInventory().setItemInMainHand(null);
        plugin.auction().create(p, listed, price);
        double tax = plugin.getConfig().getDouble("auction.tax-percent", 5);
        Util.msg(p, "Listed &f" + listed.getAmount() + "x " + ShopManager.nice(listed.getType())
                + " &7for &a" + Util.exact(price) + "&7. A &f" + (int) tax + "% &7tax is taken when it sells.");
    }

    @Override
    protected List<String> tab(CommandSender s, String cmd, String[] a) {
        if (a.length == 1) {
            if (cmd.equals("sell")) return filter(List.of("hand", "all"), a[0]);
            if (cmd.equals("ah")) return filter(List.of("sell", "claim"), a[0]);
        }
        return List.of();
    }
}
