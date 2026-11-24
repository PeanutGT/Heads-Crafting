package io.github.peanutgt.headscrafting;

import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

/** Registers the blank head recipe from config.yml; does nothing while recipe.enabled is false. */
public final class BlankHeadRecipe {

    private final NamespacedKey key;
    private final Logger logger;

    public BlankHeadRecipe(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "blank_head");
        this.logger = plugin.getLogger();
    }

    public NamespacedKey key() {
        return key;
    }

    public void register(Settings settings, BlankHead blankHead) {
        unregister();
        if (!settings.recipeEnabled()) {
            logger.info("Blank head recipe is off (recipe.enabled: false).");
            return;
        }
        if (settings.recipeShape().size() != 3 || settings.recipeShape().stream().anyMatch(row -> row.length() != 3)) {
            logger.warning("recipe.shape must be 3 rows of 3 characters; blank head recipe not registered.");
            return;
        }
        ShapedRecipe recipe = new ShapedRecipe(key, blankHead.create(settings, settings.recipeAmount()));
        recipe.shape(settings.recipeShape().toArray(String[]::new));
        for (Map.Entry<Character, String> entry : settings.recipeIngredients().entrySet()) {
            if (settings.recipeShape().stream().noneMatch(row -> row.indexOf(entry.getKey()) >= 0)) {
                continue;
            }
            Material material = Material.matchMaterial(entry.getValue());
            if (material == null || !material.isItem()) {
                logger.warning("Unknown item '" + entry.getValue() + "' in recipe.ingredients; blank head recipe not registered.");
                return;
            }
            recipe.setIngredient(entry.getKey(), material);
        }
        Bukkit.addRecipe(recipe);
        Bukkit.updateRecipes();
        logger.info("Blank head recipe registered.");
    }

    public void unregister() {
        if (Bukkit.removeRecipe(key)) {
            Bukkit.updateRecipes();
        }
    }
}
