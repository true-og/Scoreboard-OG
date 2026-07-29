# Changelog

All notable changes to Scoreboard-OG are documented here.

## 1.1.0 - 2026-07-29

### Changes

- Restore the network sidebar after players return from minigames by refreshing
  it one tick after world transitions when no Bukkit scoreboard remains active.
- Add `/togglescoreboard` with persistent per-player visibility preferences.
- Add a dedicated aqua diamond-balance line.
- Correct the SimpleClans union, BattleTracker PvP kill, and BattleTracker PvP
  death placeholders, and migrate DiamondBank balances to MiniPlaceholder
  expansion.
- Upgrade the packet-based sidebar implementation to scoreboard-library 2.6.0
  and preserve its runtime adapters in shaded builds.
- Update the DiamondBank-OG and Utilities-OG integrations and remove the unused
  Chat-OG dependency.

## 1.0.0 - 2025-12-11

### Changes

- Release a player-specific TrueOG Network sidebar for the Purpur 1.19.4
  server target.
- Display each player's LuckPerms rank and colored name, SimpleClans union,
  diamond balance, kills, deaths, and the network address.
- Refresh dynamic sidebar values every ten ticks and manage boards across
  player joins, quits, plugin reloads, and shutdowns.
- Package the packet-backed scoreboard implementation and its required runtime
  adapter in the shaded plugin JAR.
