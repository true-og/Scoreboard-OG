# Scoreboard-OG

TrueOG Network's custom Scoreboard plugin supporting dynamic MiniPlaceholders. Based on [scoreboard-library by MegavexNetwork](https://github.com/MegavexNetwork/scoreboard-library).

Current version: `1.2.0` ([changelog](CHANGELOG.md)).

Gradle Version: 8.14.3

Java Version: 17

Minecraft API: 1.19

API Jar: 1.19.4 Purpur

## API for other plugins

A minigame can take over a player's sidebar for the length of a match and hand it back afterwards. Compile against the jar, soft-depend on `Scoreboard-OG`, and only touch the API after checking the plugin is enabled.

```java
import plugin.api.ScoreboardOGAPI;
import plugin.api.SidebarProvider;

ScoreboardOGAPI.claim(player, "MyGame-OG", new SidebarProvider() {
    public Component title(Player player) { return Component.text("My Game"); }
    public List<Component> lines(Player player) { return List.of(Component.text("Players: 4")); }
});
// later
ScoreboardOGAPI.release(player, "MyGame-OG");
// on disable
ScoreboardOGAPI.releaseAll("MyGame-OG");
```

`claim` replaces the network board with the provider's content, polled on the main thread every ten ticks; up to 15 lines, fewer lines shrink the board. `release` hands the sidebar back only when the named owner still holds it, and the network board returns on its own. A player who hid the sidebar with `/togglescoreboard` sees neither board. A provider that throws loses its claim.

TheHerobrine-OG, BuildBattle-OG and Splegg-OG use this API and fall back to their own scoreboards when Scoreboard-OG is absent.

Scoreboard-OG is public domain software released under the terms of [The Unlicense](https://github.com/true-og/Template-OG/blob/main/LICENSE).
