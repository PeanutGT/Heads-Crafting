package io.github.peanutgt.headscrafting;

import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** /headscrafting reload | giveblank [amount] — giveblank lets admins test while the recipe is off. */
public final class AdminCommand implements TabExecutor {

    private final HeadsCraftingPlugin plugin;

    public AdminCommand(HeadsCraftingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("headscrafting.admin")) {
            plugin.texts().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reload();
                plugin.texts().send(sender, "reloaded");
            }
            case "giveblank" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                int amount = 1;
                if (args.length > 1) {
                    try {
                        amount = Math.max(1, Math.min(64, Integer.parseInt(args[1])));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                player.getInventory().addItem(plugin.blankHead().create(plugin.settings(), amount)).values()
                        .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
                plugin.texts().send(player, "gave-blank", Map.of("count", Integer.toString(amount)));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "giveblank").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
