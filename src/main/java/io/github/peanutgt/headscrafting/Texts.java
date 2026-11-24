package io.github.peanutgt.headscrafting;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

/** Text from config.yml, with &-color codes and {placeholders}. */
public final class Texts {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final ConfigurationSection messages;

    public Texts(ConfigurationSection messages) {
        this.messages = messages;
    }

    public void send(CommandSender to, String key, Map<String, String> placeholders) {
        String raw = messages.getString(key, key);
        to.sendMessage(format(raw, placeholders));
    }

    public void send(CommandSender to, String key) {
        send(to, key, Map.of());
    }

    /** An item name or lore line: not italic unless the text says so. */
    public static Component itemText(String raw, Map<String, String> placeholders) {
        return format(raw, placeholders).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static Component format(String raw, Map<String, String> placeholders) {
        String text = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return LEGACY.deserialize(text);
    }
}
