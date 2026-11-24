package io.github.peanutgt.headscrafting.skin;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Finds a player's skin: the cache first, then each source in order (Mojang, then SkinsRestorer).
 * A name is remembered as missing only when every source answered and none had it;
 * a source that failed (network, rate limit) does not count as an answer.
 */
public final class SkinResolver {

    /** What a lookup ended with. */
    public sealed interface Result permits Found, NotFound, Failed {
    }

    public record Found(SkinTexture texture, String source) implements Result {
    }

    public record NotFound() implements Result {
    }

    public record Failed() implements Result {
    }

    private final SkinCache cache;
    private final List<SkinSource> sources;
    private final Executor executor;
    private final Logger logger;

    public SkinResolver(SkinCache cache, List<SkinSource> sources, Executor executor, Logger logger) {
        this.cache = cache;
        this.sources = List.copyOf(sources);
        this.executor = executor;
        this.logger = logger;
    }

    public List<String> sourceIds() {
        return sources.stream().map(SkinSource::id).toList();
    }

    /** Completes off the main thread; switch back before touching the world. */
    public CompletableFuture<Result> resolve(String playerName) {
        Optional<SkinCache.Hit> cached = cache.get(playerName);
        if (cached.isPresent()) {
            Result result = cached.get() instanceof SkinCache.Found found
                    ? new Found(found.texture(), "cache")
                    : new NotFound();
            return CompletableFuture.completedFuture(result);
        }
        return CompletableFuture.supplyAsync(() -> lookup(playerName), executor);
    }

    private Result lookup(String playerName) {
        boolean anyFailed = false;
        for (SkinSource source : sources) {
            try {
                Optional<SkinTexture> texture = source.lookup(playerName);
                if (texture.isPresent()) {
                    cache.putFound(playerName, texture.get());
                    return new Found(texture.get(), source.id());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Failed();
            } catch (Exception e) {
                anyFailed = true;
                logger.log(Level.WARNING, "Skin lookup for " + playerName + " via " + source.id() + " failed: " + e.getMessage());
            }
        }
        if (anyFailed) {
            return new Failed();
        }
        cache.putMiss(playerName);
        return new NotFound();
    }
}
