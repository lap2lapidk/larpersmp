package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Player-to-player auction house. Listings and unclaimed items persist in auction.yml. */
public final class AuctionManager {

    public record Listing(String id, UUID seller, String sellerName, ItemStack item, double price, long expires) {}

    private final SMPCore plugin;
    private final File file;
    private final Map<String, Listing> listings = new LinkedHashMap<>();
    private final Map<UUID, List<ItemStack>> claims = new HashMap<>();

    public AuctionManager(SMPCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "auction.yml");
        load();
    }

    // ---------------------------------------------------------- persistence

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection ls = y.getConfigurationSection("listings");
        if (ls != null) {
            for (String id : ls.getKeys(false)) {
                ConfigurationSection s = ls.getConfigurationSection(id);
                if (s == null) continue;
                ItemStack item = s.getItemStack("item");
                String seller = s.getString("seller");
                if (item == null || seller == null) continue;
                try {
                    listings.put(id, new Listing(id, UUID.fromString(seller), s.getString("seller-name", "?"),
                            item, s.getDouble("price"), s.getLong("expires")));
                } catch (IllegalArgumentException ignored) {
                    // bad uuid, skip
                }
            }
        }
        ConfigurationSection cs = y.getConfigurationSection("claims");
        if (cs != null) {
            for (String key : cs.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    List<ItemStack> list = new ArrayList<>();
                    for (Object o : cs.getList(key, new ArrayList<>())) {
                        if (o instanceof ItemStack it) list.add(it);
                    }
                    if (!list.isEmpty()) claims.put(id, list);
                } catch (IllegalArgumentException ignored) {
                    // bad uuid, skip
                }
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Listing l : listings.values()) {
            String p = "listings." + l.id() + ".";
            y.set(p + "seller", l.seller().toString());
            y.set(p + "seller-name", l.sellerName());
            y.set(p + "item", l.item());
            y.set(p + "price", l.price());
            y.set(p + "expires", l.expires());
        }
        for (Map.Entry<UUID, List<ItemStack>> e : claims.entrySet()) {
            if (!e.getValue().isEmpty()) y.set("claims." + e.getKey(), e.getValue());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save auction.yml: " + ex.getMessage());
        }
    }

    // --------------------------------------------------------------- queries

    public List<Listing> sorted(boolean byPrice) {
        List<Listing> list = new ArrayList<>(listings.values());
        if (byPrice) list.sort(Comparator.comparingDouble(Listing::price));
        else list.sort(Comparator.comparingLong(Listing::expires).reversed());
        return list;
    }

    public int countBy(UUID seller) {
        int n = 0;
        for (Listing l : listings.values()) if (l.seller().equals(seller)) n++;
        return n;
    }

    public int claimCount(UUID id) {
        List<ItemStack> l = claims.get(id);
        return l == null ? 0 : l.size();
    }

    // --------------------------------------------------------------- actions

    public void create(Player p, ItemStack item, double price) {
        long hours = plugin.getConfig().getLong("auction.duration-hours", 48);
        String id = UUID.randomUUID().toString().substring(0, 8);
        listings.put(id, new Listing(id, p.getUniqueId(), p.getName(), item.clone(), price,
                System.currentTimeMillis() + hours * 3_600_000L));
        save();
    }

    public boolean buy(Player buyer, String id) {
        Listing l = listings.get(id);
        if (l == null) {
            Util.msg(buyer, "&cThat listing is no longer available.");
            return false;
        }
        if (l.seller().equals(buyer.getUniqueId())) {
            Util.msg(buyer, "&cYou can't buy your own listing.");
            return false;
        }
        if (!plugin.economy().has(buyer.getUniqueId(), l.price())) {
            Util.msg(buyer, "&cYou can't afford that. Price: &f" + Util.exact(l.price()));
            return false;
        }
        if (buyer.getInventory().firstEmpty() == -1) {
            Util.msg(buyer, "&cYou need a free inventory slot.");
            return false;
        }
        double tax = plugin.getConfig().getDouble("auction.tax-percent", 5);
        double payout = Math.round(l.price() * (1 - tax / 100.0) * 100d) / 100d;

        plugin.economy().withdraw(buyer.getUniqueId(), l.price());
        plugin.economy().deposit(l.seller(), payout);
        listings.remove(id);
        buyer.getInventory().addItem(l.item().clone());
        save();

        Util.msg(buyer, "Purchased &f" + ShopManager.nice(l.item().getType()) + " x" + l.item().getAmount()
                + " &7for &c" + Util.exact(l.price()));
        Player seller = Bukkit.getPlayer(l.seller());
        if (seller != null) {
            Util.msg(seller, "&f" + buyer.getName() + " &7bought your &f" + ShopManager.nice(l.item().getType())
                    + " &7- you received &a" + Util.exact(payout) + " &8(" + (int) tax + "% tax)");
        }
        return true;
    }

    public boolean cancel(Player p, String id) {
        Listing l = listings.get(id);
        if (l == null || !l.seller().equals(p.getUniqueId())) return false;
        listings.remove(id);
        Util.give(p, l.item().clone());
        save();
        Util.msg(p, "Listing cancelled and your item returned.");
        return true;
    }

    /** Moves expired listings to their owners' claim box. */
    public void expireCheck() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        Iterator<Listing> it = listings.values().iterator();
        while (it.hasNext()) {
            Listing l = it.next();
            if (l.expires() > now) continue;
            it.remove();
            claims.computeIfAbsent(l.seller(), k -> new ArrayList<>()).add(l.item().clone());
            changed = true;
            Player seller = Bukkit.getPlayer(l.seller());
            if (seller != null) {
                Util.msg(seller, "&eYour auction for &f" + ShopManager.nice(l.item().getType())
                        + " &eexpired. Use &f/ah claim &eto get it back.");
            }
        }
        if (changed) save();
    }

    /** Gives back every item waiting in the player's claim box. Returns how many stacks were given. */
    public int claim(Player p) {
        List<ItemStack> list = claims.remove(p.getUniqueId());
        if (list == null || list.isEmpty()) return 0;
        for (ItemStack it : list) Util.give(p, it);
        save();
        return list.size();
    }
}
