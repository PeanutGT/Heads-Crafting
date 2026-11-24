package io.github.peanutgt.headscrafting.skin;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Skin data written into a head's profile.
 *
 * @param uniqueId  profile id; a premium UUID for player heads, a stable id derived from the value for decorative heads
 * @param name      player name, or null for decorative heads
 * @param value     base64 "textures" property value
 * @param signature Mojang signature, or null when unsigned
 */
public record SkinTexture(UUID uniqueId, @Nullable String name, String value, @Nullable String signature) {
}
