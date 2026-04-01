// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import net.trueog.utilitiesog.UtilitiesOG;
import net.trueog.utilitiesog.utils.TextUtils;

public class ToggleScoreboardCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        final String toggleScoreboardPermission = "scoreboard.toggle";

        if (sender instanceof Player player) {

            if (player.hasPermission(toggleScoreboardPermission)) {

                final ScoreboardOG plugin = ScoreboardOG.getInstance();
                final YamlConfiguration preferences = plugin.getScoreboardPreferences();

                final boolean currentlyHidden = preferences.getBoolean(player.getUniqueId().toString());

                preferences.set(player.getUniqueId().toString(), !currentlyHidden);

                if (currentlyHidden) {

                    plugin.openBoard(player);

                    UtilitiesOG.trueogMessage(player,
                            "<#AAAAAA>[<#00AA00>Scoreboard<#AA0000>-OG<#AAAAAA>] <#55FF55>Scoreboard turned <#00AA00>ON<#55FF55>.");

                } else {

                    plugin.closeBoard(player);

                    UtilitiesOG.trueogMessage(player,
                            "<#AAAAAA>[<#00AA00>Scoreboard<#AA0000>-OG<#AAAAAA>] <#FFAA00>Scoreboard turned <#FF5555>OFF<#FFAA00>.");

                }

                try {

                    preferences.save(plugin.getScoreboardPreferencesFile());

                } catch (IOException error) {

                    throw new RuntimeException(error);

                }

            } else {

                TextUtils.permissionsErrorMessage(player, cmd.getName(), toggleScoreboardPermission);

            }

        } else {

            TextUtils.logToConsole(cmd.getName() + " " + toggleScoreboardPermission);

        }

        return true;

    }

}
