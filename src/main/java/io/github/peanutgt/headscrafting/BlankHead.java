package io.github.peanutgt.headscrafting;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/** The blank head: a player head carrying the headscrafting:blank tag. Only tagged heads convert. */
public final class BlankHead {

    private final NamespacedKey tag;

    public BlankHead(Plugin plugin) {
        this.tag = new NamespacedKey(plugin, "blank");
    }

    public ItemStack create(Settings settings, int amount) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, amount);
        item.editMeta(meta -> {
            meta.displayName(Texts.itemText(settings.blankName(), Map.of()));
            meta.lore(settings.blankLore().stream().map(line -> Texts.itemText(line, Map.of())).toList());
            meta.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public boolean is(@Nullable ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(tag, PersistentDataType.BYTE);
    }
}
