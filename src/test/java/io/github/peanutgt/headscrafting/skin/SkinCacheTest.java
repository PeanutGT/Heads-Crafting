package io.github.peanutgt.headscrafting.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SkinCacheTest {

    @TempDir
    Path dir;

    @Test
    void foundSkinSurvivesSaveAndLoad() throws Exception {
        Path file = dir.resolve("cache.json");
        SkinTexture texture = new SkinTexture(UUID.randomUUID(), "GTTW_Peanut", "value", "sig");
        SkinCache cache = new SkinCache(file, Duration.ofDays(7), Duration.ofHours(1));
        cache.putFound("GTTW_Peanut", texture);
        cache.save();

        SkinCache reloaded = new SkinCache(file, Duration.ofDays(7), Duration.ofHours(1));
        reloaded.load();
        var hit = reloaded.get("gttw_peanut").orElseThrow();
        assertEquals(texture, assertInstanceOf(SkinCache.Found.class, hit).texture());
    }

    @Test
    void missIsRememberedAndExpires() {
        SkinCache cache = new SkinCache(dir.resolve("c.json"), Duration.ofDays(7), Duration.ofHours(1));
        cache.putMiss("coralsea");
        assertInstanceOf(SkinCache.Missing.class, cache.get("coralsea").orElseThrow());

        cache.setLifetimes(Duration.ofDays(7), Duration.ZERO.minusSeconds(1));
        assertTrue(cache.get("coralsea").isEmpty());
    }
}
