package io.github.peanutgt.headscrafting;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.peanutgt.headscrafting.skin.SkinTexture;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Builds heads whose profile already carries the textures, so placing or trading one
 * never triggers a profile lookup on the server.
 */
public final class HeadFactory {

    private HeadFactory() {
    }

    public static ItemStack create(SkinTexture texture, net.kyori.adventure.text.Component displayName, int amount) {
        // createProfileExact keeps our UUID and name as given instead of "fixing" them from the user cache.
        PlayerProfile profile = Bukkit.createProfileExact(texture.uniqueId(), texture.name());
        profile.setProperty(texture.signature() == null
                ? new ProfileProperty("textures", texture.value())
                : new ProfileProperty("textures", texture.value(), texture.signature()));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, amount);
        head.editMeta(SkullMeta.class, meta -> {
            meta.setPlayerProfile(profile);
            meta.displayName(displayName);
        });
        return head;
    }
}
