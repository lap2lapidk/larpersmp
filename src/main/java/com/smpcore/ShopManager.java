package com.smpcore;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server shop (config driven) plus the selling logic used by /sell. */
public final class ShopManager {

    public record ShopItem(Material material, double buy, double sell) {}

    public record Category(String id, String title, Material icon, List<ShopItem> items) {}

    private final SMPCore plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<Material, Double> sellPrices = new EnumMap<>(Material.class);

    public ShopManager(SMPCore plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        categories.clear();
        sellPrices.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shop.categories");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            Material icon = Material.matchMaterial(s.getString("icon", "CHEST"));
            List<ShopItem> items = new ArrayList<>();
            for (Map<?, ?> m : s.getMapList("items")) {
                Material mat = Material.matchMaterial(String.valueOf(m.get("material")));
                if (mat == null) {
                    plugin.getLogger().warning("Shop: unknown material '" + m.get("material") + "' in " + id);
                    continue;
                }
                double buy = m.get("buy") instanceof Number n ? n.doubleValue() : 0;
                double sell = m.get("sell") instanceof Number n ? n.doubleValue() : 0;
                items.add(new ShopItem(mat, buy, sell));
                if (sell > 0) sellPrices.put(mat, sell);
            }
            categories.put(id, new Category(id, s.getString("title", id),
                    icon == null ? Material.CHEST : icon, items));
        }
    }

    public Collection<Category> categories() {
        return categories.values();
    }

    public boolean sellable(ItemStack it) {
        return it != null && !it.getType().isAir() && sellPrices.containsKey(it.getType()) && !it.hasItemMeta();
    }

    // ------------------------------------------------------------------ buy

    public void buy(Player p, ShopItem si, int amount) {
        if (si.buy() <= 0) {
            Util.msg(p, "&cThat item is not for sale.");
            return;
        }
        double cost = si.buy() * amount;
        if (!plugin.economy().has(p.getUniqueId(), si.buy())) {
            Util.msg(p, "&cYou can't afford that. Price: &f" + Util.exact(si.buy()));
            return;
        }
        // Don't take more than the player can afford
        int affordable = (int) Math.min(amount, Math.floor(plugin.economy().balance(p.getUniqueId()) / si.buy()));
        if (affordable < amount) {
            amount = affordable;
            cost = si.buy() * amount;
        }
        Map<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(si.material(), amount));
        int leftover = 0;
        for (ItemStack it : left.values()) leftover += it.getAmount();
        int given = amount - leftover;
        if (given <= 0) {
            Util.msg(p, "&cYour inventory is full.");
            return;
        }
        double charge = si.buy() * given;
        plugin.economy().withdraw(p.getUniqueId(), charge);
        Util.msg(p, "Bought &f" + given + "x " + nice(si.material()) + " &7for &c" + Util.exact(charge));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.2f);
    }

    // ----------------------------------------------------------------- sell

    /** Sells up to {@code amount} plain items of this type from the player's inventory. */
    public void sell(Player p, ShopItem si, int amount) {
        if (si.sell() <= 0) {
            Util.msg(p, "&cThat item can't be sold.");
            return;
        }
        PlayerInventory inv = p.getInventory();
        int remaining = amount;
        int sold = 0;
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType() != si.material() || it.hasItemMeta()) continue;
            int take = Math.min(remaining, it.getAmount());
            if (take >= it.getAmount()) inv.setItem(i, null);
            else it.setAmount(it.getAmount() - take);
            remaining -= take;
            sold += take;
        }
        if (sold == 0) {
            Util.msg(p, "&cYou don't have any " + nice(si.material()) + " to sell.");
            return;
        }
        double total = round(si.sell() * sold * plugin.progression().sellMultiplier(p));
        plugin.economy().deposit(p.getUniqueId(), total);
        Util.msg(p, "Sold &f" + sold + "x " + nice(si.material()) + " &7for &a" + Util.exact(total));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.6f);
    }

    /** Sells everything sellable in the list, hands back the rest. Returns the money earned. */
    public double sellAndReturn(Player p, List<ItemStack> items) {
        double mult = plugin.progression().sellMultiplier(p);
        double total = 0;
        int count = 0;
        List<ItemStack> unsold = new ArrayList<>();
        for (ItemStack it : items) {
            if (it == null || it.getType().isAir()) continue;
            if (sellable(it)) {
                total += sellPrices.get(it.getType()) * it.getAmount() * mult;
                count += it.getAmount();
            } else {
                unsold.add(it);
            }
        }
        total = round(total);
        if (total > 0) {
            plugin.economy().deposit(p.getUniqueId(), total);
            int bonus = (int) Math.round((mult - 1) * 100);
            Util.msg(p, "Sold &f" + count + " &7items for &a" + Util.exact(total)
                    + (bonus > 0 ? " &8(&a+" + bonus + "% tier bonus&8)" : ""));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
        } else if (!unsold.isEmpty()) {
            Util.msg(p, "&cNone of those items can be sold.");
        }
        for (ItemStack u : unsold) Util.give(p, u);
        return total;
    }

    public void sellHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!sellable(hand)) {
            Util.msg(p, "&cYou can't sell the item in your hand.");
            return;
        }
        p.getInventory().setItemInMainHand(null);
        sellAndReturn(p, List.of(hand));
    }

    public void sellAll(Player p) {
        PlayerInventory inv = p.getInventory();
        List<ItemStack> toSell = new ArrayList<>();
        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (sellable(contents[i])) {
                toSell.add(contents[i]);
                inv.setItem(i, null);
            }
        }
        if (toSell.isEmpty()) {
            Util.msg(p, "&cYou have nothing sellable.");
            return;
        }
        sellAndReturn(p, toSell);
    }

    private static double round(double v) {
        return Math.round(v * 100d) / 100d;
    }

    public static String nice(Material m) {
        String[] parts = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String s : parts) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        }
        return sb.toString();
    }
}
