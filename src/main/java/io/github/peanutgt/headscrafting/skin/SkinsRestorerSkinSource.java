package io.github.peanutgt.headscrafting.skin;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.MojangSkinDataResult;
import net.skinsrestorer.api.property.SkinProperty;

/**
 * Falls back to SkinsRestorer. Only loaded when the SkinsRestorer plugin is enabled.
 * First the skin a player chose with /skin (looked up by the offline-mode UUID of the name),
 * then whatever SkinsRestorer has stored for the name.
 */
public final class SkinsRestorerSkinSource implements SkinSource {

    @Override
    public String id() {
        return "skinsrestorer";
    }

    @Override
    public Optional<SkinTexture> lookup(String playerName) throws Exception {
        SkinsRestorer api = SkinsRestorerProvider.get();

        UUID offlineId = offlineUuid(playerName);
        Optional<SkinProperty> chosen = api.getPlayerStorage().getSkinOfPlayer(offlineId);
        if (chosen.isPresent()) {
            return Optional.of(toTexture(offlineId, playerName, chosen.get()));
        }

        Optional<MojangSkinDataResult> stored = api.getSkinStorage().getPlayerSkin(playerName, true);
        return stored.map(result -> toTexture(result.getUniqueId(), playerName, result.getSkinProperty()));
    }

    private static SkinTexture toTexture(UUID id, String name, SkinProperty property) {
        return new SkinTexture(id, name, property.getValue(), property.getSignature());
    }

    /** The UUID an offline-mode server gives this name. */
    static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
}
