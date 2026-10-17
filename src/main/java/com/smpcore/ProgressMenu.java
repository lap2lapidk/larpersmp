package com.smpcore;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The progression map.
 * Top: member tiers (earned by playtime) laid out as a path.
 * Bottom: the staff ladder Member -> Mod -> Admin.
 */
public final class ProgressMenu extends Menu {
    /** Tier nodes snake across the map. The connector between two nodes sits at their midpoint slot. */
    private static final int[] NODES = {10, 12, 14, 16, 34, 32, 30, 28};

    public ProgressMenu(SMPCore plugin, Player p) {
        super(plugin, 54, "&6&lProgression Map");
        PlayerData d = plugin.data().get(p);
        List<ProgressionManager.Tier> tiers = plugin.progression().tiers();
        int current = plugin.progression().indexFor(d.playtime);

        // --- header
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(p);
        sm.displayName(Util.c(d.rank.color() + "&l" + p.getName()));
        sm.lore(List.of(
                Util.c("&7Rank: " + d.rank.color() + d.rank.display()),
                Util.c("&7Playtime: &f" + Util.time(d.playtime)),
                Util.c("&7Money: &a" + Util.exact(d.money)),
                Util.c("&7Kills: &f" + d.kills + "  &7Deaths: &f" + d.deaths)));
        head.setItemMeta(sm);
        inv.setItem(4, head);

        inv.setItem(9, Util.item(Material.OAK_SIGN, "&e&lMember Tiers",
                "&7Earned automatically through playtime.", "&7Each tier gives a reward, more homes", "&7and a /sell bonus."));
        inv.setItem(45, Util.item(Material.OAK_SIGN, "&e&lStaff Ladder",
                "&7Member &8» &aMod &8» &cAdmin", "&7Staff ranks are given by admins."));

        // --- member tier path
        int n = Math.min(tiers.size(), NODES.length);
        for (int i = 0; i < n; i++) {
            ProgressionManager.Tier t = tiers.get(i);
            boolean unlocked = i <= current;
            boolean isCurrent = i == current;

            List<String> lore = new ArrayList<>();
            lore.add("&7Playtime needed: &f" + t.hours() + "h");
            lore.add("&7Reward: &a" + Util.exact(t.reward()));
            lore.add("&7Max homes: &f" + t.maxHomes());
            lore.add("&7Sell bonus: &a+" + (int) t.sellBonus() + "%");
            lore.add(" ");
            if (isCurrent) {
                lore.add("&a&l✔ YOU ARE HERE");
                if (i + 1 < tiers.size()) {
                    long left = Math.max(0, tiers.get(i + 1).seconds() - d.playtime);
                    lore.add("&7Next tier in &f" + Util.time(left));
                } else {
                    lore.add("&6&lMAX TIER");
                }
            } else if (unlocked) {
                lore.add("&a✔ Unlocked");
            } else {
                lore.add("&c✘ Locked &8- &7" + Util.time(Math.max(0, t.seconds() - d.playtime)) + " to go");
            }
            inv.setItem(NODES[i], glow(Util.item(t.icon(), t.color() + "&l" + t.name(), lore), isCurrent));

            if (i > 0) {
                int slot = (NODES[i - 1] + NODES[i]) / 2;
                inv.setItem(slot, unlocked
                        ? Util.item(Material.LIME_STAINED_GLASS_PANE, "&aUnlocked")
                        : Util.item(Material.RED_STAINED_GLASS_PANE, "&cLocked"));
            }
        }

        // --- staff ladder
        ladder(47, Rank.MEMBER, Material.LEATHER_HELMET, d.rank);
        ladder(49, Rank.MOD, Material.IRON_HELMET, d.rank);
        ladder(51, Rank.ADMIN, Material.NETHERITE_HELMET, d.rank);
        inv.setItem(48, Util.item(Material.ARROW, "&8»"));
        inv.setItem(50, Util.item(Material.ARROW, "&8»"));

        border(0, 54);
    }

    private void ladder(int slot, Rank rank, Material icon, Rank mine) {
        List<String> lore = new ArrayList<>(plugin.getConfig()
                .getStringList("progression.staff-ladder." + rank.name().toLowerCase()));
        lore.add(" ");
        lore.add(mine == rank ? "&a&l✔ YOUR RANK" : (mine.atLeast(rank) ? "&a✔ Included" : "&7Not your rank"));
        inv.setItem(slot, glow(Util.item(icon, rank.color() + "&l" + rank.display(), lore), mine == rank));
    }

    private static ItemStack glow(ItemStack it, boolean on) {
        if (!on) return it;
        ItemMeta meta = it.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    @Override
    public void click(InventoryClickEvent e) {
        // display only
    }
}
