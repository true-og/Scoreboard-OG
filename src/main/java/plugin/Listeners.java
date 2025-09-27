// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Listeners implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {

        ScoreboardOG.getInstance().openBoard(event.getPlayer());

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {

        ScoreboardOG.getInstance().closeBoard(event.getPlayer());

    }

}
