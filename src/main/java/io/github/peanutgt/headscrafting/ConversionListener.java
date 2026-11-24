package io.github.peanutgt.headscrafting;

import io.github.peanutgt.headscrafting.skin.SkinResolver;
import io.github.peanutgt.headscrafting.skin.SkinTexture;
import io.github.peanutgt.headscrafting.skin.TextureValue;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.bukkit.Keyed;

/**
 * Hold blank heads in one hand and a book in the other, then press the swap-hands key (F):
 * a signed book's title names a player, a book and quill's first page holds a texture value.
 * The blank heads in that hand turn into the head, up to conversion.max-per-use at a time.
 */
public final class ConversionListener implements Listener {

    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private final HeadsCraftingPlugin plugin;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public ConversionListener(HeadsCraftingPlugin plugin) {
        this.plugin = plugin;
    }

    /** What the book asks for. */
    private sealed interface Request permits PlayerRequest, CustomRequest {
    }

    private record PlayerRequest(String name) implements Request {
    }

    private record CustomRequest(SkinTexture texture) implements Request {
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack main = inventory.getItemInMainHand();
        ItemStack off = inventory.getItemInOffHand();

        EquipmentSlot headsHand;
        if (plugin.blankHead().is(main) && isBook(off)) {
            headsHand = EquipmentSlot.HAND;
        } else if (plugin.blankHead().is(off) && isBook(main)) {
            headsHand = EquipmentSlot.OFF_HAND;
        } else {
            return;
        }
        event.setCancelled(true);

        Texts texts = plugin.texts();
        ItemStack book = headsHand == EquipmentSlot.HAND ? off : main;
        Optional<Request> request = readBook(player, book);
        if (request.isEmpty()) {
            return;
        }
        if (!checkPermission(player, request.get())) {
            texts.send(player, "no-permission");
            return;
        }
        if (pending.contains(player.getUniqueId())) {
            texts.send(player, "busy");
            return;
        }
        long now = System.currentTimeMillis();
        long readyAt = lastUse.getOrDefault(player.getUniqueId(), 0L) + plugin.settings().cooldown().toMillis();
        if (now < readyAt) {
            long seconds = Math.max(1, (readyAt - now + 999) / 1000);
            texts.send(player, "cooldown", Map.of("seconds", Long.toString(seconds)));
            return;
        }

        ItemStack headsSnapshot = (headsHand == EquipmentSlot.HAND ? main : off).clone();
        ItemStack bookSnapshot = book.clone();

        switch (request.get()) {
            case CustomRequest custom -> {
                lastUse.put(player.getUniqueId(), now);
                Component name = Texts.itemText(plugin.settings().customHeadName(), Map.of());
                finish(player, headsHand, headsSnapshot, bookSnapshot, custom.texture(), name, "done-custom", "");
            }
            case PlayerRequest playerRequest -> lookUpAndFinish(player, headsHand, headsSnapshot, bookSnapshot, playerRequest.name(), now);
        }
    }

    private void lookUpAndFinish(Player player, EquipmentSlot headsHand, ItemStack headsSnapshot, ItemStack bookSnapshot,
                                 String name, long now) {
        UUID playerId = player.getUniqueId();
        pending.add(playerId);
        lastUse.put(playerId, now);
        plugin.texts().send(player, "looking-up", Map.of("name", name));

        plugin.resolver().resolve(name).whenComplete((result, error) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    pending.remove(playerId);
                    if (!player.isOnline()) {
                        return;
                    }
                    if (error != null || result instanceof SkinResolver.Failed) {
                        plugin.texts().send(player, "lookup-failed");
                        return;
                    }
                    if (result instanceof SkinResolver.Found found) {
                        String shownName = found.texture().name() != null ? found.texture().name() : name;
                        Component display = Texts.itemText(plugin.settings().playerHeadName(), Map.of("name", shownName));
                        finish(player, headsHand, headsSnapshot, bookSnapshot, found.texture(), display, "done-player", shownName);
                    } else {
                        plugin.texts().send(player, "not-found", Map.of("name", name));
                    }
                }));
    }

    /** Runs on the main thread. Gives the heads only if both hands still hold what they held at the start. */
    private void finish(Player player, EquipmentSlot headsHand, ItemStack headsSnapshot, ItemStack bookSnapshot,
                        SkinTexture texture, Component displayName, String doneKey, String name) {
        PlayerInventory inventory = player.getInventory();
        EquipmentSlot bookHand = headsHand == EquipmentSlot.HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        ItemStack heads = inventory.getItem(headsHand);
        ItemStack book = inventory.getItem(bookHand);
        if (!heads.isSimilar(headsSnapshot) || !book.isSimilar(bookSnapshot)) {
            plugin.texts().send(player, "hands-changed");
            return;
        }

        Settings settings = plugin.settings();
        int count = Math.min(heads.getAmount(), settings.maxPerUse());
        ItemStack result = HeadFactory.create(texture, displayName, count);

        if (settings.consumeBook()) {
            book.setAmount(book.getAmount() - 1);
            inventory.setItem(bookHand, book.getAmount() > 0 ? book : null);
        }
        heads.setAmount(heads.getAmount() - count);
        if (heads.getAmount() <= 0) {
            inventory.setItem(headsHand, result);
        } else {
            inventory.setItem(headsHand, heads);
            inventory.addItem(result).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        plugin.texts().send(player, doneKey, Map.of("count", Integer.toString(count), "name", name));
    }

    private Optional<Request> readBook(Player player, ItemStack book) {
        Texts texts = plugin.texts();
        if (book.getType() == Material.WRITTEN_BOOK) {
            BookMeta meta = (BookMeta) book.getItemMeta();
            String title = meta.hasTitle() ? meta.getTitle().trim() : "";
            if (!PLAYER_NAME.matcher(title).matches()) {
                texts.send(player, "invalid-name");
                return Optional.empty();
            }
            return Optional.of(new PlayerRequest(title));
        }
        WritableBookMeta meta = (WritableBookMeta) book.getItemMeta();
        String firstPage = meta.getPageCount() > 0 ? meta.getPage(1) : "";
        Optional<SkinTexture> texture = TextureValue.parse(firstPage);
        if (texture.isEmpty()) {
            texts.send(player, "invalid-texture");
            return Optional.empty();
        }
        return Optional.of(new CustomRequest(texture.get()));
    }

    private static boolean checkPermission(Player player, Request request) {
        return switch (request) {
            case CustomRequest ignored -> player.hasPermission("headscrafting.custom");
            case PlayerRequest playerRequest -> playerRequest.name().equalsIgnoreCase(player.getName())
                    ? player.hasPermission("headscrafting.player")
                    : player.hasPermission("headscrafting.player.others");
        };
    }

    private static boolean isBook(ItemStack item) {
        return item.getType() == Material.WRITTEN_BOOK || item.getType() == Material.WRITABLE_BOOK;
    }

    /** No headscrafting.craft permission: the blank head recipe shows no result. */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getRecipe() instanceof Keyed keyed) || !keyed.getKey().equals(plugin.recipe().key())) {
            return;
        }
        if (!event.getView().getPlayer().hasPermission("headscrafting.craft")) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastUse.remove(event.getPlayer().getUniqueId());
    }
}
