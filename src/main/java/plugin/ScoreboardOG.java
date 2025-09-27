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

import me.tigerhix.lib.scoreboard.ScoreboardLib;
import me.tigerhix.lib.scoreboard.common.EntryBuilder;
import me.tigerhix.lib.scoreboard.type.Entry;
import me.tigerhix.lib.scoreboard.type.Scoreboard;
import me.tigerhix.lib.scoreboard.type.ScoreboardHandler;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.trueog.utilitiesog.UtilitiesOG;

public class ScoreboardOG extends JavaPlugin {

    private static ScoreboardOG instance;

    private FileConfiguration config;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    private LuckPerms luckPerms;

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
        ScoreboardLib.setPluginInstance(this);

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

        boards.values().forEach(Scoreboard::deactivate);
        boards.clear();

    }

    public FileConfiguration config() {

        return config;

    }

    public void openBoard(Player player) {

        final Scoreboard board = ScoreboardLib.createScoreboard(player).setHandler(new ScoreboardHandler() {

            @Override
            public String getTitle(Player p) {

                return "&4♥ &a&lTrue&c&lOG&r&e Network &4♥";

            }

            @Override
            public java.util.List<Entry> getEntries(Player p) {

                // Fetch LuckPerms prefix with formatting.
                String lpPrefix = getLuckPermsPrefixLegacy(p);
                lpPrefix = stripLeadingReset(stripTrailingReset(lpPrefix));

                // Bleed formatting from prefix into name.
                final String youTail = joinSpace(lpPrefix, p.getName());
                final String youLine = "&aYou:&r " + youTail + "&r";

                // Independently formatted Union tag.
                final String unionTag = expandMM(p, "<placeholderapi_player:%simpleclans_clan_color_tag%>");
                final String unionLine = "&cUnion: &r" + unionTag + "&r";

                return new EntryBuilder().next("&c&m----&6&m----&e&m----&2&m----&9&m----&5&m----").next(youLine).blank()
                        .next("&bDiamonds: " + expandMM(p, "<diamondbankog_balance>")).blank().next(unionLine).blank()
                        .next("&2Kills: " + expandMM(p, "<placeholderapi_player:%statistic_player_kills%>")).blank()
                        .next("&4Deaths: " + expandMM(p, "<placeholderapi_player:%statistic_deaths%>")).blank()
                        .next("&4&m--&0&m--&4> &etrue-og.net &4<&0&m--&4&m--").build();

            }

        }).setUpdateInterval(10L);
        board.activate();
        boards.put(player.getUniqueId(), board);

    }

    public void closeBoard(Player player) {

        final Scoreboard board = boards.remove(player.getUniqueId());
        if (board != null) {

            board.deactivate();

        }

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