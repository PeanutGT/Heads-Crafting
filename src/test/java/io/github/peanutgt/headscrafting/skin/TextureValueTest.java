package io.github.peanutgt.headscrafting.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class TextureValueTest {

    private static String encode(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void acceptsMinecraftSkinUrl() {
        String value = encode("http://textures.minecraft.net/texture/38f46c1c49b7a0a499b46cc86411cf7c");
        var texture = TextureValue.parse(value);
        assertTrue(texture.isPresent());
        assertEquals(value, texture.get().value());
        assertNull(texture.get().signature());
    }

    @Test
    void ignoresWhitespaceAndLineBreaksFromBookPages() {
        String value = encode("http://textures.minecraft.net/texture/abc");
        String pasted = value.substring(0, 10) + "\n " + value.substring(10);
        assertEquals(value, TextureValue.parse(pasted).orElseThrow().value());
    }

    @Test
    void sameValueGivesSameIdSoHeadsStack() {
        String value = encode("http://textures.minecraft.net/texture/abc");
        assertEquals(TextureValue.parse(value).orElseThrow().uniqueId(), TextureValue.parse(value).orElseThrow().uniqueId());
    }

    @Test
    void rejectsOtherHosts() {
        assertFalse(TextureValue.parse(encode("http://example.com/texture/abc")).isPresent());
        assertFalse(TextureValue.parse(encode("http://textures.minecraft.net.example.com/x")).isPresent());
    }

    @Test
    void rejectsGarbage() {
        assertFalse(TextureValue.parse("").isPresent());
        assertFalse(TextureValue.parse("not base64 !!!").isPresent());
        assertFalse(TextureValue.parse(Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8))).isPresent());
    }
}
