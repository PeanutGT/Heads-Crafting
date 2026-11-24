package io.github.peanutgt.headscrafting.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

/**
 * Skins already looked up, by lower-case name, kept in plugins/HeadsCrafting/cache.json.
 * Names no source knows are remembered for a shorter time so they are not looked up again and again.
 */
public final class SkinCache {

    /** Result of a cache look: a skin, a remembered miss, or nothing known. */
    public sealed interface Hit permits Found, Missing {
    }

    public record Found(SkinTexture texture) implements Hit {
    }

    public record Missing() implements Hit {
    }

    private static final class Entry {
        @Nullable String uuid;
        @Nullable String name;
        @Nullable String value;
        @Nullable String signature;
        long time;
        boolean miss;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private volatile Duration foundFor;
    private volatile Duration missFor;

    public SkinCache(Path file, Duration foundFor, Duration missFor) {
        this.file = file;
        this.foundFor = foundFor;
        this.missFor = missFor;
    }

    public void setLifetimes(Duration foundFor, Duration missFor) {
        this.foundFor = foundFor;
        this.missFor = missFor;
    }

    public Optional<Hit> get(String name) {
        Entry entry = entries.get(key(name));
        if (entry == null) {
            return Optional.empty();
        }
        Duration lifetime = entry.miss ? missFor : foundFor;
        if (Instant.ofEpochMilli(entry.time).plus(lifetime).isBefore(Instant.now())) {
            entries.remove(key(name));
            return Optional.empty();
        }
        if (entry.miss || entry.uuid == null || entry.value == null) {
            return Optional.of(new Missing());
        }
        return Optional.of(new Found(new SkinTexture(UUID.fromString(entry.uuid), entry.name, entry.value, entry.signature)));
    }

    public void putFound(String name, SkinTexture texture) {
        Entry entry = new Entry();
        entry.uuid = texture.uniqueId().toString();
        entry.name = texture.name();
        entry.value = texture.value();
        entry.signature = texture.signature();
        entry.time = System.currentTimeMillis();
        entries.put(key(name), entry);
    }

    public void putMiss(String name) {
        Entry entry = new Entry();
        entry.time = System.currentTimeMillis();
        entry.miss = true;
        entries.put(key(name), entry);
    }

    public void load() throws IOException {
        entries.clear();
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Map<String, Entry> loaded = GSON.fromJson(reader, new TypeToken<HashMap<String, Entry>>() { }.getType());
            if (loaded != null) {
                entries.putAll(loaded);
            }
        }
    }

    /** Writes to a temporary file first, so a crash never leaves a half-written cache. */
    public synchronized void save() throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(new HashMap<>(entries), writer);
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
