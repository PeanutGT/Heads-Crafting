package io.github.peanutgt.headscrafting.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Looks a name up on Mojang: name to premium UUID, then UUID to signed textures.
 * It never reads the server's usercache, so offline-mode UUIDs are never used.
 */
public final class MojangSkinSource implements SkinSource {

    private static final String PROFILE_BY_NAME = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private final HttpClient http;
    private final Duration timeout;

    public MojangSkinSource(Duration timeout) {
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String id() {
        return "mojang";
    }

    @Override
    public Optional<SkinTexture> lookup(String playerName) throws IOException, InterruptedException {
        Optional<JsonObject> profile = getJson(PROFILE_BY_NAME + playerName);
        if (profile.isEmpty()) {
            return Optional.empty();
        }
        String compactId = profile.get().get("id").getAsString();
        String exactName = profile.get().get("name").getAsString();

        Optional<JsonObject> session = getJson(SESSION_PROFILE + compactId + "?unsigned=false");
        if (session.isEmpty()) {
            return Optional.empty();
        }
        JsonArray properties = session.get().getAsJsonArray("properties");
        if (properties == null) {
            return Optional.empty();
        }
        for (JsonElement element : properties) {
            JsonObject property = element.getAsJsonObject();
            if ("textures".equals(property.get("name").getAsString())) {
                String value = property.get("value").getAsString();
                String signature = property.has("signature") ? property.get("signature").getAsString() : null;
                return Optional.of(new SkinTexture(fromCompact(compactId), exactName, value, signature));
            }
        }
        return Optional.empty();
    }

    /** @return the body, or empty for 204/404 (no such profile) */
    private Optional<JsonObject> getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 204 || status == 404) {
            return Optional.empty();
        }
        if (status != 200) {
            throw new IOException("Mojang answered HTTP " + status + " for " + url);
        }
        String body = response.body();
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(JsonParser.parseString(body).getAsJsonObject());
    }

    static UUID fromCompact(String compact) {
        String dashed = compact.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5");
        return UUID.fromString(dashed);
    }
}
