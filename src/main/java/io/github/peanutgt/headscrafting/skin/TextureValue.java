package io.github.peanutgt.headscrafting.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * A texture value pasted from a site such as minecraft-heads.com.
 * Accepted only when it decodes to a skin hosted on textures.minecraft.net.
 */
public final class TextureValue {

    private static final int MAX_LENGTH = 4096;
    private static final String SKIN_HOST = "textures.minecraft.net";

    private TextureValue() {
    }

    public static Optional<SkinTexture> parse(String raw) {
        String value = raw.replaceAll("\\s+", "");
        if (value.isEmpty() || value.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        try {
            String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String url = root.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            String host = URI.create(url).getHost();
            if (host == null || !host.toLowerCase(Locale.ROOT).equals(SKIN_HOST)) {
                return Optional.empty();
            }
            // The same value always gets the same id, so identical decorative heads stack.
            UUID id = UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
            return Optional.of(new SkinTexture(id, null, value, null));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
