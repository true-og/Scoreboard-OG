// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import net.megavex.scoreboardlibrary.api.sidebar.SidebarManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.trueog.utilitiesog.UtilitiesOG;

public class ScoreboardOG extends JavaPlugin {

    private static ScoreboardOG instance;

    private FileConfiguration config;
    private final Map<UUID, Sidebar> boards = new HashMap<>();

    private final Map<UUID, BukkitTask> updateTasks = new HashMap<>();

    private ScoreboardLibrary scoreboardLibrary;

    private SidebarManager sidebarManager;

    private LuckPerms luckPerms;

    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacyAmpersand();

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

        scoreboardLibrary = ScoreboardLibrary.loadNative(this);
        sidebarManager = scoreboardLibrary.createSidebarManager();

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

    }

    @Override
    public void onDisable() {

        boards.values().forEach(Sidebar::close);
        boards.clear();

        updateTasks.values().forEach(BukkitTask::cancel);
        updateTasks.clear();

        if (sidebarManager != null) {

            sidebarManager.close();

        }

        if (scoreboardLibrary != null) {

            scoreboardLibrary.close();

        }

    }

    public FileConfiguration config() {

        return config;

    }

    public void openBoard(Player player) {

        final Sidebar board = sidebarManager.createSidebar();
        board.title(deserialize("&4♥ &a&lTrue&c&lOG&r&e Network &4♥"));
        board.lines(buildSidebarLines(player));
        board.addViewer(player);
        boards.put(player.getUniqueId(), board);

        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {

            if (!player.isOnline()) {

                closeBoard(player);
                return;

            }

            board.lines(buildSidebarLines(player));

        }, 10L, 10L);

        updateTasks.put(player.getUniqueId(), task);

    }

    public void closeBoard(Player player) {

        final Sidebar board = boards.remove(player.getUniqueId());
        final BukkitTask task = updateTasks.remove(player.getUniqueId());

        if (task != null) {

            task.cancel();

        }

        if (board != null) {

            board.removeViewer(player);
            board.close();

        }

    }

    private List<Component> buildSidebarLines(Player player) {

        final List<Component> lines = new ArrayList<>();

        // Fetch LuckPerms prefix with formatting.
        String lpPrefix = getLuckPermsPrefixLegacy(player);
        lpPrefix = stripLeadingReset(stripTrailingReset(lpPrefix));

        // Bleed formatting from prefix into name.
        final String youTail = joinSpace(lpPrefix, player.getName());
        final String youLine = "&aYou:&r " + youTail + "&r";

        // Independently formatted Union tag.
        final String unionTag = expandMM(player, "<placeholderapi_player:%simpleclans_clan_color_tag%>");
        final String unionLine = "&cUnion: &r" + unionTag + "&r";

        lines.add(deserialize("&c&m----&6&m----&e&m----&2&m----&9&m----&5&m----"));
        lines.add(deserialize(youLine));
        lines.add(deserialize(""));
        lines.add(deserialize("&bDiamonds: " + expandMM(player, "<diamondbankog_balance>")));
        lines.add(deserialize(""));
        lines.add(deserialize(unionLine));
        lines.add(deserialize(""));
        lines.add(deserialize("&2Kills: " + expandMM(player, "<placeholderapi_player:%statistic_player_kills%>")));
        lines.add(deserialize(""));
        lines.add(deserialize("&4Deaths: " + expandMM(player, "<placeholderapi_player:%statistic_deaths%>")));
        lines.add(deserialize(""));
        lines.add(deserialize("&4&m--&0&m--&4> &etrue-og.net &4<&0&m--&4&m--"));

        return lines;

    }

    private Component deserialize(String text) {

        return legacySerializer.deserialize(text);

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
    private static String expandMM(Player p, String miniMsg) {

        final String out = UtilitiesOG.trueogExpand(miniMsg, p).content();
        return out == null ? "" : out;

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

    // Ensure the prefix and name are spaced out.
    private static String joinSpace(String... parts) {

        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String part : parts) {

            if (part == null) {

                continue;

            }

            final String t = StringUtils.trim(part);
            if (StringUtils.isEmpty(t)) {

                continue;

            }

            if (!first) {

                sb.append(' ');

            }

            sb.append(t);
            first = false;

        }

        return sb.toString();

    }

}