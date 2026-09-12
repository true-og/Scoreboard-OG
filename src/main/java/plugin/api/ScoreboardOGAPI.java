// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package plugin.api;

import org.bukkit.entity.Player;

import plugin.ScoreboardOG;

// Lets a minigame take over a player's sidebar for the length of a match and hand
// it back afterwards. The network board returns on its own once the claim is released.
public final class ScoreboardOGAPI {

    private ScoreboardOGAPI() {

    }

    // True once Scoreboard-OG is enabled and its packet sidebar is usable.
    public static boolean isAvailable() {

        final ScoreboardOG plugin = ScoreboardOG.getInstance();
        return plugin != null && plugin.isEnabled() && plugin.isSidebarReady();

    }

    // Replaces the network board with the provider's content. A later claim by
    // any owner wins; the owner name is what release checks.
    public static void claim(Player player, String owner, SidebarProvider provider) {

        if (!isAvailable() || player == null || owner == null || provider == null)
            return;

        ScoreboardOG.getInstance().claimSidebar(player, owner, provider);

    }

    // Hands the sidebar back if this owner still holds it. Returns whether it did.
    public static boolean release(Player player, String owner) {

        if (!isAvailable() || player == null || owner == null)
            return false;

        return ScoreboardOG.getInstance().releaseSidebar(player, owner);

    }

    // Hands back every sidebar this owner holds; for a plugin's onDisable.
    public static void releaseAll(String owner) {

        if (!isAvailable() || owner == null)
            return;

        ScoreboardOG.getInstance().releaseAllSidebars(owner);

    }

    // The owner currently holding the player's sidebar, or null for the network
    // board.
    public static String getOwner(Player player) {

        if (!isAvailable() || player == null)
            return null;

        return ScoreboardOG.getInstance().getSidebarOwner(player);

    }

}
