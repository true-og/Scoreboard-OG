// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.trueog.utilitiesog.UtilitiesOG;

public class ScoreboardOG extends JavaPlugin {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final Component EMPTY_LINE = Component.empty();
    private static final Component SIDEBAR_TITLE = legacyText("&4♥ &a&lTrue&c&lOG&r&e Network &4♥");
    private static final Component FOOTER_LINE = legacyText("&etrue-og.net");

    private static ScoreboardOG instance;

    private FileConfiguration config;
    private final Map<UUID, PlayerSidebar> sidebars = new HashMap<>();

    private LuckPerms luckPerms;
    private ScoreboardLibrary scoreboardLibrary;
    private int updateTaskId = -1;

    private File scoreboardPreferencesFile;
    private YamlConfiguration scoreboardPreferences;

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

        this.scoreboardPreferencesFile = new File(getDataFolder(), "scoreboardPreferences.yml");
        if (!scoreboardPreferencesFile.exists()) {

            try {

                getDataFolder().mkdirs();
                scoreboardPreferencesFile.createNewFile();

            } catch (IOException error) {

                throw new RuntimeException(error);

            }

        }

        this.scoreboardPreferences = YamlConfiguration.loadConfiguration(scoreboardPreferencesFile);

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
        getCommand("togglescoreboard").setExecutor(new ToggleScoreboardCommand());
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

    public YamlConfiguration getScoreboardPreferences() {

        return scoreboardPreferences;

    }

    public File getScoreboardPreferencesFile() {

        return scoreboardPreferencesFile;

    }

    public boolean isScoreboardHidden(Player player) {

        return scoreboardPreferences.getBoolean(player.getUniqueId().toString());

    }

    public void openBoard(Player player) {

        if (scoreboardLibrary == null) {

            return;

        }

        if (isScoreboardHidden(player)) {

            return;

        }

        closeBoard(player);

        final PlayerSidebar sidebar = new PlayerSidebar(player);
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

        final String colorCodes = extractLeadingColorCodes(lpPrefix);
        if (StringUtils.isEmpty(colorCodes)) {

            return legacyText(p.getName());

        }

        return legacyText(colorCodes + p.getName());

    }

    private Component createUnionLine(Player p) {

        final Component label = legacyText("&cUnion: &r");
        final Component unionTag = expandText(p, "%simpleclans_union_color_tag%");
        return label.append(unionTag);

    }

    private Component createDiamondsLine(Player p) {

        final Component label = legacyText("&bDiamonds: ");
        final Component value = expandText(p, "<diamondbankog_balance>");
        return label.append(value);

    }

    private Component createKillsLine(Player p) {

        final Component label = legacyText("&2Kills: &r");
        final Component value = expandText(p, "%bt_pvp_kills%");
        return label.append(value);

    }

    private Component createDeathsLine(Player p) {

        final Component label = legacyText("&4Deaths: &r");
        final Component value = expandText(p, "%bt_pve_deaths%");
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

    // Return expanded text or null depending on if the String is populated.
    private static Component expandText(Player p, String text) {

        if (text == null || StringUtils.isEmpty(text)) {

            return Component.empty();

        }

        // TODO: Remove in 1.20. Expand PlaceholderAPI Placeholders First.
        String expandedText = text;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {

            expandedText = PlaceholderAPI.setPlaceholders(p, expandedText);

        }

        // Expand MiniPlaceholders and format the message.
        final Component out = UtilitiesOG.trueogExpand(expandedText, p);

        // Pass on the message.
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

        private PlayerSidebar(Player player) {

            this.player = player;
            this.sidebar = scoreboardLibrary.createSidebar();
            this.sidebar.addPlayer(player);

        }

        private void tick() {

            sidebar.title(SIDEBAR_TITLE);

            setLine(0, EMPTY_LINE);
            setLine(1, createRankLine(player));
            setLine(2, createYouLine(player));
            setLine(3, EMPTY_LINE);
            setLine(4, createDiamondsLine(player));
            setLine(5, EMPTY_LINE);
            setLine(6, createUnionLine(player));
            setLine(7, EMPTY_LINE);
            setLine(8, createKillsLine(player));
            setLine(9, EMPTY_LINE);
            setLine(10, createDeathsLine(player));
            setLine(11, EMPTY_LINE);
            setLine(12, FOOTER_LINE);

        }

        private void close() {

            sidebar.close();

        }

        private void setLine(int index, Component component) {

            sidebar.line(index, component);

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
