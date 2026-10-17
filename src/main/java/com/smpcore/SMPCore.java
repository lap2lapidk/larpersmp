package com.smpcore;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SMPCore extends JavaPlugin {
    private DataManager data;
    private EconomyManager economy;
    private RankManager ranks;
    private ProgressionManager progression;
    private LocationStore locations;
    private TeleportManager teleports;
    private ShopManager shop;
    private AuctionManager auction;
    private SpawnerManager spawners;
    private ScoreboardService scoreboards;

    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> staffChat = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        data = new DataManager(this);
        economy = new EconomyManager(data);
        ranks = new RankManager(this);
        progression = new ProgressionManager(this);
        locations = new LocationStore(this);
        teleports = new TeleportManager(this);
        shop = new ShopManager(this);
        auction = new AuctionManager(this);
        spawners = new SpawnerManager(this);
        scoreboards = new ScoreboardService(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(spawners, this);

        bind(new EconomyCommands(this), "balance", "pay", "baltop", "eco");
        bind(new TeleportCommands(this), "tpa", "tpahere", "tpaccept", "tpadeny", "sethome", "home", "delhome",
                "homes", "spawn", "setspawn", "warp", "warps", "setwarp", "delwarp", "rtp", "back");
        bind(new ShopCommands(this), "shop", "sell", "ah");
        bind(new StaffCommands(this), "rank", "mute", "unmute", "kick", "ban", "unban", "vanish", "sc",
                "invsee", "stp");
        bind(new InfoCommands(this), "progress", "stats", "msg", "reply", "smpcore");
        bind(new ShardCommands(this), "shards", "shardshop");
        bind(new SpawnerCommands(this), "spawnershop", "spawner");

        // every minute: playtime, tier rewards, expired auctions
        Bukkit.getScheduler().runTaskTimer(this, this::minuteTick, 20L * 60, 20L * 60);
        // autosave every 3 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            data.save();
            auction.save();
        }, 20L * 180, 20L * 180);
        // sidebar refresh
        Bukkit.getScheduler().runTaskTimer(this, scoreboards::updateAll, 40L, 40L);

        // /reload friendliness
        for (Player p : Bukkit.getOnlinePlayers()) setup(p);

        getLogger().info("SMPCore enabled.");
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
        if (auction != null) auction.save();
        if (scoreboards != null) scoreboards.clear();
    }

    private void bind(TabExecutor executor, String... names) {
        for (String n : names) {
            PluginCommand pc = getCommand(n);
            if (pc == null) {
                getLogger().warning("Command missing from plugin.yml: " + n);
                continue;
            }
            pc.setExecutor(executor);
            pc.setTabCompleter(executor);
        }
    }

    private void minuteTick() {
        int every = getConfig().getInt("shards.playtime-minutes", 10);
        long amount = getConfig().getLong("shards.playtime-amount", 1);
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = data.get(p);
            d.playtime += 60;
            if (every > 0 && amount > 0 && (d.playtime / 60) % every == 0) {
                d.shards += amount;
                p.sendActionBar(Util.c("&b+" + amount + " shard" + (amount == 1 ? "" : "s") + " &7for playing"));
            }
            progression.tick(p);
        }
        auction.expireCheck();
    }

    /** Prepares a player who just joined (or was online during a reload). */
    public void setup(Player p) {
        data.get(p);
        ranks.apply(p);
        scoreboards.show(p);
    }

    public void reloadAll() {
        reloadConfig();
        progression.reload();
        shop.reload();
        for (Player p : Bukkit.getOnlinePlayers()) ranks.apply(p);
    }

    // ---------------------------------------------------------- vanish / SC

    public boolean isVanished(UUID id) {
        return vanished.contains(id);
    }

    public boolean toggleVanish(Player p) {
        boolean nowVanished;
        if (vanished.remove(p.getUniqueId())) {
            nowVanished = false;
            for (Player other : Bukkit.getOnlinePlayers()) other.showPlayer(this, p);
        } else {
            nowVanished = true;
            vanished.add(p.getUniqueId());
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!ranks.has(other, Rank.MOD)) other.hidePlayer(this, p);
            }
        }
        return nowVanished;
    }

    /** Hides vanished staff from a (newly joined) viewer who is not staff. */
    public void refreshVanish(Player viewer) {
        if (ranks.has(viewer, Rank.MOD)) return;
        for (UUID id : vanished) {
            Player v = Bukkit.getPlayer(id);
            if (v != null) viewer.hidePlayer(this, v);
        }
    }

    public boolean inStaffChat(Player p) {
        return staffChat.contains(p.getUniqueId());
    }

    public boolean toggleStaffChat(Player p) {
        if (!staffChat.remove(p.getUniqueId())) {
            staffChat.add(p.getUniqueId());
            return true;
        }
        return false;
    }

    public void clearStaffChat(Player p) {
        staffChat.remove(p.getUniqueId());
    }

    public void staffChat(Player from, String message) {
        PlayerData d = data.get(from);
        String line = "&8[&cStaff&8] " + d.rank.prefix() + "&f" + from.getName() + "&8: &f" + message;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (ranks.has(p, Rank.MOD)) Util.raw(p, line);
        }
        getLogger().info("[StaffChat] " + from.getName() + ": " + message);
    }

    // -------------------------------------------------------------- getters

    public DataManager data() { return data; }
    public EconomyManager economy() { return economy; }
    public RankManager ranks() { return ranks; }
    public ProgressionManager progression() { return progression; }
    public LocationStore locations() { return locations; }
    public TeleportManager teleports() { return teleports; }
    public ShopManager shop() { return shop; }
    public AuctionManager auction() { return auction; }
    public SpawnerManager spawners() { return spawners; }
    public ScoreboardService scoreboards() { return scoreboards; }
}
