package io.github.peanutgt.headscrafting;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** config.yml, read once per load or reload. */
public record Settings(
        String blankName,
        List<String> blankLore,
        boolean recipeEnabled,
        int recipeAmount,
        List<String> recipeShape,
        Map<Character, String> recipeIngredients,
        int maxPerUse,
        boolean consumeBook,
        Duration cooldown,
        String playerHeadName,
        String customHeadName,
        boolean useMojang,
        boolean useSkinsRestorer,
        Duration cacheFor,
        Duration missCacheFor,
        Duration timeout) {

    public static Settings from(FileConfiguration config) {
        ConfigurationSection ingredientsSection = config.getConfigurationSection("recipe.ingredients");
        Map<Character, String> ingredients = new java.util.LinkedHashMap<>();
        if (ingredientsSection != null) {
            for (String key : ingredientsSection.getKeys(false)) {
                if (key.length() == 1) {
                    ingredients.put(key.charAt(0), ingredientsSection.getString(key, ""));
                }
            }
        }
        return new Settings(
                config.getString("blank-head.name", "&f空白頭顱"),
                config.getStringList("blank-head.lore"),
                config.getBoolean("recipe.enabled", false),
                clamp(config.getInt("recipe.amount", 1), 1, 64),
                config.getStringList("recipe.shape"),
                Map.copyOf(ingredients),
                clamp(config.getInt("conversion.max-per-use", 64), 1, 64),
                config.getBoolean("conversion.consume-book", true),
                Duration.ofSeconds(Math.max(0, config.getLong("conversion.cooldown-seconds", 5))),
                config.getString("player-head-name", "&e{name} 的頭"),
                config.getString("custom-head-name", "&e裝飾頭"),
                config.getBoolean("skin.mojang", true),
                config.getBoolean("skin.skinsrestorer", true),
                Duration.ofDays(Math.max(0, config.getLong("skin.cache-days", 7))),
                Duration.ofMinutes(Math.max(0, config.getLong("skin.miss-cache-minutes", 60))),
                Duration.ofSeconds(Math.max(1, config.getLong("skin.timeout-seconds", 8))));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
