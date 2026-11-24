package io.github.peanutgt.headscrafting.skin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidTest {

    @Test
    void offlineUuidMatchesTheServerUserCache() {
        // usercache.json on the 24h server lists GTTW_Peanut with this offline-mode UUID.
        assertEquals(UUID.fromString("30c1ffbc-84af-3c8b-8212-c70b64dec1be"),
                SkinsRestorerSkinSource.offlineUuid("GTTW_Peanut"));
    }

    @Test
    void compactMojangIdGetsDashes() {
        assertEquals(UUID.fromString("01b455bc-5273-42f4-8a10-9573212c3f2d"),
                MojangSkinSource.fromCompact("01b455bc527342f48a109573212c3f2d"));
    }
}
