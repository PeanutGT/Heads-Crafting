package io.github.peanutgt.headscrafting.skin;

import java.util.Optional;

/** One place a skin can come from. Called off the main thread. */
public interface SkinSource {

    String id();

    /**
     * @return the skin, or empty when this source has no skin for the name
     * @throws Exception when the source could not answer (network error, rate limit)
     */
    Optional<SkinTexture> lookup(String playerName) throws Exception;
}
