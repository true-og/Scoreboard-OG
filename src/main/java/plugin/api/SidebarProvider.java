// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin.api;

import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

// Content another plugin wants on a player's sidebar instead of the network board.
// Both methods are polled on the main thread every ten ticks while the claim holds.
public interface SidebarProvider {

    Component title(Player player);

    // Top to bottom, at most 15 entries. Fewer entries shrink the sidebar.
    List<Component> lines(Player player);

}
