package io.github.peanutgt.headscrafting;

import io.github.peanutgt.headscrafting.skin.MojangSkinSource;
import io.github.peanutgt.headscrafting.skin.SkinCache;
import io.github.peanutgt.headscrafting.skin.SkinResolver;
import io.github.peanutgt.headscrafting.skin.SkinSource;
import io.github.peanutgt.headscrafting.skin.SkinsRestorerSkinSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeadsCraftingPlugin extends JavaPlugin {

    private static final long SAVE_EVERY_TICKS = 20L * 60 * 5;

    private ExecutorService lookups;
    private SkinCache cache;
    private BlankHead blankHead;
    private BlankHeadRecipe recipe;
    private volatile Settings settings;
    private volatile Texts texts;
    private volatile SkinResolver resolver;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lookups = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "HeadsCrafting-lookup");
            thread.setDaemon(true);
            return thread;
        });
        blankHead = new BlankHead(this);
        recipe = new BlankHeadRecipe(this);
        cache = new SkinCache(getDataFolder().toPath().resolve("cache.json"),
                java.time.Duration.ofDays(7), java.time.Duration.ofHours(1));
        try {
            cache.load();
        } catch (IOException | RuntimeException e) {
            getLogger().log(Level.WARNING, "Could not read cache.json; starting with an empty cache.", e);
        }

        reload();
        getServer().getPluginManager().registerEvents(new ConversionListener(this), this);
        getServer().getPluginManager().registerEvents(new RecipeUnlockListener(this), this);
        AdminCommand command = new AdminCommand(this);
        Objects.requireNonNull(getCommand("headscrafting")).setExecutor(command);
        Objects.requireNonNull(getCommand("headscrafting")).setTabCompleter(command);
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::saveCache, SAVE_EVERY_TICKS, SAVE_EVERY_TICKS);
    }

    @Override
    public void onDisable() {
        if (recipe != null) {
            recipe.unregister();
        }
        if (lookups != null) {
            lookups.shutdownNow();
            try {
                lookups.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        saveCache();
    }

    /** Reads config.yml again and rebuilds the recipe and the skin sources. Main thread only. */
    public void reload() {
        reloadConfig();
        Settings next = Settings.from(getConfig());
        settings = next;
        texts = new Texts(Objects.requireNonNull(getConfig().getConfigurationSection("messages"),
                "config.yml has no messages section"));
        cache.setLifetimes(next.cacheFor(), next.missCacheFor());

        List<SkinSource> sources = new ArrayList<>();
        if (next.useMojang()) {
            sources.add(new MojangSkinSource(next.timeout()));
        }
        if (next.useSkinsRestorer() && getServer().getPluginManager().isPluginEnabled("SkinsRestorer")) {
            sources.add(new SkinsRestorerSkinSource());
        }
        resolver = new SkinResolver(cache, sources, lookups, getLogger());
        getLogger().info("Skin sources: " + resolver.sourceIds());

        recipe.register(next, blankHead);
    }

    private void saveCache() {
        try {
            cache.save();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not save cache.json.", e);
        }
    }

    public Settings settings() {
        return settings;
    }

    public Texts texts() {
        return texts;
    }

    public SkinResolver resolver() {
        return resolver;
    }

    public BlankHead blankHead() {
        return blankHead;
    }

    public BlankHeadRecipe recipe() {
        return recipe;
    }
}
