package com.smpcore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Util {
    public static final String PREFIX = "&6&lSMP &8» &7";
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Util() {}

    /** Colored (&-code) text to a component, with italics turned off (good for item names/lore). */
    public static Component c(String s) {
        return LEGACY.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    public static void msg(CommandSender to, String s) {
        to.sendMessage(c(PREFIX + s));
    }

    public static void raw(CommandSender to, String s) {
        to.sendMessage(c(s));
    }

    /** Compact money, e.g. $1.2K, $3.40M */
    public static String money(double v) {
        double a = Math.abs(v);
        if (a >= 1_000_000_000d) return String.format(Locale.US, "$%.2fB", v / 1_000_000_000d);
        if (a >= 1_000_000d) return String.format(Locale.US, "$%.2fM", v / 1_000_000d);
        if (a >= 10_000d) return String.format(Locale.US, "$%.1fK", v / 1_000d);
        return exact(v);
    }

    public static String exact(double v) {
        if (v == Math.floor(v)) return String.format(Locale.US, "$%,.0f", v);
        return String.format(Locale.US, "$%,.2f", v);
    }

    /** Parses 1500, 1.5k, 2m, 3b. Returns -1 if invalid. */
    public static double parseAmount(String s) {
        try {
            s = s.toLowerCase(Locale.US).replace(",", "").trim();
            if (s.isEmpty()) return -1;
            double mult = 1;
            char last = s.charAt(s.length() - 1);
            if (last == 'k') { mult = 1_000d; s = s.substring(0, s.length() - 1); }
            else if (last == 'm') { mult = 1_000_000d; s = s.substring(0, s.length() - 1); }
            else if (last == 'b') { mult = 1_000_000_000d; s = s.substring(0, s.length() - 1); }
            double v = Double.parseDouble(s) * mult;
            if (Double.isNaN(v) || Double.isInfinite(v)) return -1;
            return Math.floor(v * 100d) / 100d;
        } catch (Exception e) {
            return -1;
        }
    }

    public static String time(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m";
        return seconds + "s";
    }

    public static ItemStack item(Material m, String name, String... lore) {
        return item(m, name, List.of(lore));
    }

    public static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(c(name));
            List<Component> l = new ArrayList<>();
            for (String line : lore) l.add(c(line));
            meta.lore(l);
            it.setItemMeta(meta);
        }
        return it;
    }

    /** Gives an item, dropping anything that does not fit at the player's feet. */
    public static void give(Player p, ItemStack it) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(it);
        for (ItemStack rest : left.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), rest);
        }
    }
}
