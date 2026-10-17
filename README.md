# SMPCore

A DonutSMP-style core plugin for **Paper 26.2** (Java 25): economy, shop, sell menu, auction house,
homes, TPA, RTP, scoreboard, and a **Member / Mod / Admin rank system with a progression map**.

## Build

```
mvn package
```

The jar is created at `target/SMPCore.jar`. Drop it in your server's `plugins/` folder and restart.
(Needs **JDK 25** and Maven 3.9+. The Paper API is pulled from `repo.papermc.io`.)

> Targets Paper 26.2 (tested target: build 129). `paper.api.version` in `pom.xml` is Paper's range `[26.2.build,)`;
> pin it to an exact `26.2.build.N-...` version if you want a fixed API.

## First-time setup

1. Start the server once so `plugins/SMPCore/config.yml` is generated.
2. Join, then run from the **console**: `rank <yourname> admin` (or `op` yourself - ops count as admins).
3. Stand where you want spawn and run `/setspawn`. Make warps with `/setwarp <name>`.

## Ranks

| Rank | Who | Can do |
|------|-----|--------|
| **Member** | everyone | economy, shop, /sell, /ah, homes, TPA, RTP, /progress |
| **Mod** | trusted members | everything above + `/mute /unmute /kick /vanish /sc /invsee /stp`, no teleport warmup/cooldowns, +2 homes |
| **Admin** | owner/co-owners | everything above + `/ban /unban /rank /eco /setspawn /setwarp /delwarp /smpcore reload`, full invsee edit, +50 homes |

Staff can only punish players of a lower rank than their own. Admins promote with
`/rank <player> <member|mod|admin>`. Extra vanilla permission nodes per rank (gamemode, give, etc.)
are set in `rank-permissions` in `config.yml`.

## Progression map (`/progress`)

* **Member tiers** - earned automatically through playtime: Newcomer -> Regular -> Veteran -> Elite -> Legend.
  Each tier pays a one-time reward, raises the home limit, and adds a bonus to everything you `/sell`.
  Tiers, hours, rewards and icons are all editable under `progression.tiers`.
* **Staff ladder** - the bottom row shows Member -> Mod -> Admin with what each rank unlocks and highlights yours.

## Commands

| Area | Commands |
|------|----------|
| Money | `/bal [player]` `/pay <player> <amt>` (supports 2.5k, 1m) `/baltop` `/eco give\|take\|set` |
| Shop | `/shop` (left click buy 1, shift-left buy 64, right sell 1, shift-right sell all) |
| Selling | `/sell` (GUI: put items in, close to sell) `/sell hand` `/sell all` |
| Auction | `/ah` `/ah sell <price>` `/ah claim` (5% tax, 48h listings, shift-right-click your listing to cancel) |
| Teleport | `/tpa /tpahere /tpaccept /tpadeny` `/rtp` `/back` `/spawn` `/warp /warps` |
| Homes | `/sethome [name]` `/home [name]` `/delhome <name>` `/homes` |
| Social | `/msg /reply` `/stats [player]` `/progress` |
| Staff | `/mute /unmute /kick /vanish /sc /invsee /stp` (mod) `/ban /unban /rank /eco` (admin) |

Other features: sidebar scoreboard (rank, tier, money, playtime, kills/deaths, online), rank prefixes in chat
and tab, starting balance + starter kit, kill rewards, respawn at spawn, join/quit messages.

## Shards

A second currency. Earned by playtime (default 1 every 10 min) and player kills (default 1); both are set under `shards:` in config.
`/shards [player]` `/shards top` `/shards pay <player> <amt>` `/shards give|take|set <player> <amt>` (admin) `/shardshop`.
The shard shop (`shards.shop`) sells items or runs console commands (`command:` with `%player%`).
Shards also show on the scoreboard, `/stats` and the `/shop` balance.

## Spawners

* `/spawnershop` (or the button in `/shop`): left-click buys with money, right-click buys with shards. Edit `spawners.shop` in config.
* Spawners keep their mob when placed; a **Silk Touch** tool picks a spawner up with its mob type intact (`spawners.silk-touch-pickup`).
* Admins: `/spawner give <player> <mob> [amount]`.

## Customizing

Everything is in `plugins/SMPCore/config.yml`: shop categories and prices, auction tax/duration/limits, RTP
radius and cooldown, teleport warmup, scoreboard text, starting balance and kit. Run `/smpcore reload` after editing.

Data files: `players.yml` (balances, ranks, homes, stats), `auction.yml`, `warps.yml`. Saved every 3 minutes
and on shutdown.

## Notes

* `/kick`, `/ban`, `/msg` also exist in vanilla. If another plugin wins the name, use `/smpcore:kick` etc.
* `/ban` and `/unban` call the vanilla ban list (`minecraft:ban` / `minecraft:pardon`).
* Not included (easy to add next): player orders, crates, clans/teams, bounties, spawner stacking.
