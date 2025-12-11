// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import net.trueog.utilitiesog.UtilitiesOG;

public class ScoreboardOG extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private static ScoreboardOG instance;

    private FileConfiguration config;
    private final Map<UUID, PlayerSidebar> sidebars = new HashMap<>();

    private LuckPerms luckPerms;
    private ScoreboardLibrary scoreboardLibrary;
    private int updateTaskId = -1;

    public static ScoreboardOG getInstance() {

        return instance;

    }

    @Override
    public void onLoad() {

        instance = this;

    }

    @Override
    public void onEnable() {

        saveDefaultConfig();
        this.config = getConfig();

        try {

            this.scoreboardLibrary = ScoreboardLibrary.loadScoreboardLibrary(this);

        } catch (Exception e) {

            this.scoreboardLibrary = null;
            getLogger().warning("No scoreboard packet adapter available, disabling scoreboards.");

        }

        // Hook LuckPerms API.
        final RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager()
                .getRegistration(LuckPerms.class);
        if (provider != null) {

            this.luckPerms = provider.getProvider();

        } else {

            getLogger().severe("LuckPerms not found – prefixes will be empty.");

        }

        getServer().getPluginManager().registerEvents(new Listeners(), this);
        Bukkit.getOnlinePlayers().forEach(this::openBoard);

        this.updateTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
                () -> Bukkit.getOnlinePlayers().forEach((Player online) ->
                {

                    final PlayerSidebar sidebar = sidebars.get(online.getUniqueId());
                    if (sidebar != null) {

                        sidebar.tick();

                    }

                }), 0L, 10L);

    }

    @Override
    public void onDisable() {

        if (updateTaskId != -1) {

            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;

        }

        sidebars.values().forEach(PlayerSidebar::close);
        sidebars.clear();

        if (scoreboardLibrary != null) {

            scoreboardLibrary.close();

        }

    }

    public FileConfiguration config() {

        return config;

    }

    public void openBoard(Player player) {

        if (scoreboardLibrary == null) {

            return;

        }

        closeBoard(player);

        PlayerSidebar sidebar = new PlayerSidebar(player);
        sidebars.put(player.getUniqueId(), sidebar);

    }

    public void closeBoard(Player player) {

        final PlayerSidebar sidebar = sidebars.remove(player.getUniqueId());
        if (sidebar != null) {

            sidebar.close();

        }

    }

    private Component createRankLine(Player p) {

        String lpPrefix = getLuckPermsPrefixLegacy(p);
        lpPrefix = stripLeadingReset(stripTrailingReset(lpPrefix));

        return legacyText(lpPrefix);

    }

    private Component createYouLine(Player p) {

        String lpPrefix = getLuckPermsPrefixLegacy(p);
        lpPrefix = stripLeadingReset(stripTrailingReset(lpPrefix));

        String colorCodes = extractLeadingColorCodes(lpPrefix);

        if (colorCodes.isEmpty()) {

            return legacyText(p.getName());

        }

        return legacyText(colorCodes + p.getName());

    }

    private Component createUnionLine(Player p) {

        final Component label = legacyText("&cUnion: &r");
        final Component unionTag = expandMM(p, "<placeholderapi_player:%simpleclans_clan_color_tag%>");
        return label.append(unionTag);

    }

    private Component createDiamondsLine(Player p) {

        final Component label = legacyText("&bDiamonds: ");
        final Component value = expandMM(p, "<diamondbankog_balance>");
        return label.append(value);

    }

    private Component createKillsLine(Player p) {

        final Component label = legacyText("&2Kills: &r");
        final Component value = expandMM(p, "<placeholderapi_player:%statistic_player_kills%>");
        return label.append(value);

    }

    private Component createDeathsLine(Player p) {

        final Component label = legacyText("&4Deaths: &r");
        final Component value = expandMM(p, "<placeholderapi_player:%statistic_deaths%>");
        return label.append(value);

    }

    private static Component legacyText(String input) {

        if (input == null || StringUtils.isEmpty(input)) {

            return Component.empty();

        }

        return LEGACY_SERIALIZER.deserialize(input);

    }

    // Resolve the player's LuckPerms prefix as legacy color codes with any
    // serializing.
    private String getLuckPermsPrefixLegacy(Player p) {

        // If there is no LuckPerms, return empty handed.
        if (luckPerms == null) {

            return "";

        }

        // If there is no user, return empty handed.
        final User user = luckPerms.getUserManager().getUser(p.getUniqueId());
        if (user == null) {

            return "";

        }

        // If a player has no prefix in LuckPerms, return empty handed.
        final CachedMetaData meta = user.getCachedData().getMetaData();
        final String prefix = meta.getPrefix();
        if (prefix == null || StringUtils.isEmpty(prefix)) {

            return "";

        }

        // Normalize to legacy color codes for ScoreboardLib pipeline.
        // Pass on the prefix colors.
        return StringUtils.trim(prefix.replace('§', '&'));

    }

    /// Return expanded text or null depending on if the String is populated.
    private static Component expandMM(Player p, String miniMsg) {

        final Component out = UtilitiesOG.trueogExpand(miniMsg, p);
        return out == null ? Component.empty() : out;

    }

    // Strip trailing resets.
    private static String stripTrailingReset(String s) {

        if (s == null || StringUtils.isEmpty(s)) {

            return "";

        }

        return s.replaceAll("(?i)(?:\\s*(?:<reset>|[&§]r))+$", "");

    }

    // Strip leading resets.
    private static String stripLeadingReset(String s) {

        if (s == null || StringUtils.isEmpty(s)) {

            return "";

        }

        return s.replaceFirst("(?i)^(?:\\s*(?:<reset>|[&§]r))+", "");

    }

    private final class PlayerSidebar {

        private final Player player;
        private final Sidebar sidebar;
        private final ComponentSidebarLayout layout;

        private PlayerSidebar(Player player) {

            this.player = player;
            this.sidebar = scoreboardLibrary.createSidebar();

            final SidebarComponent titleComponent = SidebarComponent
                    .staticLine(legacyText("&4♥ &a&lTrue&c&lOG&r&e Network &4♥"));

            final SidebarComponent lines = SidebarComponent.builder().addBlankLine()
                    .addDynamicLine(() -> createRankLine(this.player)).addDynamicLine(() -> createYouLine(this.player))
                    .addBlankLine().addDynamicLine(() -> createDiamondsLine(this.player)).addBlankLine()
                    .addDynamicLine(() -> createUnionLine(this.player)).addBlankLine()
                    .addDynamicLine(() -> createKillsLine(this.player)).addBlankLine()
                    .addDynamicLine(() -> createDeathsLine(this.player)).addBlankLine()
                    .addStaticLine(legacyText("&etrue-og.net")).build();

            this.layout = new ComponentSidebarLayout(titleComponent, lines);
            this.sidebar.addPlayer(player);

        }

        private void tick() {

            layout.apply(sidebar);

        }

        private void close() {

            sidebar.close();

        }

    }

    private String extractLeadingColorCodes(String input) {

        if (input == null || input.length() < 2) {

            return "";

        }

        final StringBuilder out = new StringBuilder();

        for (int i = 0; i < input.length() - 1; i++) {

            final char c = input.charAt(i);
            if (c != '&' && c != '§') {

                break;

            }

            final char code = input.charAt(i + 1);
            out.append(c).append(code);

            // Skip the code character.
            i++;

        }

        return out.toString();

    }

}