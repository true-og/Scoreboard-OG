// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.ScoreboardManager;

public class Listeners implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {

        ScoreboardOG.getInstance().openBoard(event.getPlayer());

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {

        ScoreboardOG.getInstance().closeBoard(event.getPlayer());

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {

        final ScoreboardOG plugin = ScoreboardOG.getInstance();
        final Player player = event.getPlayer();

        // Minigames can replace the sidebar with a Bukkit scoreboard. Wait until
        // every world-change listener has finished, then reclaim the sidebar only
        // when no plugin still owns the player's Bukkit scoreboard.
        Bukkit.getScheduler().runTask(plugin, () -> {

            final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
            if (!player.isOnline() || scoreboardManager == null
                    || player.getScoreboard() != scoreboardManager.getMainScoreboard())
            {

                return;

            }

            plugin.openBoard(player);

        });

    }

}
