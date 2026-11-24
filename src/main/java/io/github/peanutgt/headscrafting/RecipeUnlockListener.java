package io.github.peanutgt.headscrafting;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Adds the blank head recipe to a player's recipe book when they join. */
public final class RecipeUnlockListener implements Listener {

    private final HeadsCraftingPlugin plugin;

    public RecipeUnlockListener(HeadsCraftingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.settings().recipeEnabled()
                || !plugin.getConfig().getBoolean("recipe.unlock-on-join", true)) {
            return;
        }
        if (!event.getPlayer().hasPermission("headscrafting.craft")) {
            return;
        }
        event.getPlayer().discoverRecipe(plugin.recipe().key());
    }
}
