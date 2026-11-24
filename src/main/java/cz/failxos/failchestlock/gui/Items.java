package cz.failxos.failchestlock.gui;

import cz.failxos.failchestlock.FailChestLock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Vsechny itemy v menu. Poradi zdroju ikony:
 * 1) ItemsAdder item z configu (itemsadder.icons.*), 2) vanilla item / textura hlavy.
 */
public final class Items {

    private final FailChestLock plugin;
    private final ItemsAdderHook hook;

    public Items(FailChestLock plugin) {
        this.plugin = plugin;
        this.hook = new ItemsAdderHook(plugin);
    }

    private Component txt(String key, String... pairs) {
        return plugin.messages().item(key, pairs);
    }

    private static ItemStack decorate(ItemStack item, Component name, Component... lore) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        if (lore.length > 0) {
            meta.lore(List.of(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Ikona: ItemsAdder (pokud je nastaveny) nebo vanilla zaloha. */
    private ItemStack icon(String key, Material fallback, Component name, Component... lore) {
        ItemStack custom = hook.icon(key);
        if (custom != null) {
            return decorate(custom, name, lore);
        }
        return decorate(new ItemStack(fallback), name, lore);
    }

    private static ItemStack texturedHead(String hash, Component name, Component lore) {
        try {
            UUID id = UUID.nameUUIDFromBytes(("failchestlock:" + hash).getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createPlayerProfile(id, "FailChestLock");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create("https://textures.minecraft.net/texture/" + hash).toURL());
            profile.setTextures(textures);

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwnerProfile(profile);
            meta.displayName(name);
            meta.lore(List.of(lore));
            item.setItemMeta(meta);
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    public ItemStack filler() {
        return icon("filler", Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
    }

    public ItemStack digit(int d) {
        Component name = txt("item-digit", "%digit%", String.valueOf(d));
        Component lore = txt("item-digit-lore");

        ItemStack custom = hook.icon("digit-" + d);
        if (custom != null) {
            return decorate(custom, name, lore);
        }

        List<String> hashes = plugin.getConfig().getStringList("gui.digit-heads");
        String hash = d < hashes.size() ? hashes.get(d) : "";
        if (hash != null && !hash.isBlank()) {
            ItemStack head = texturedHead(hash.trim(), name, lore);
            if (head != null) {
                return head;
            }
        }
        return decorate(new ItemStack(Material.PAPER), name, lore);
    }

    public ItemStack del() {
        return icon("del", Material.RED_CONCRETE, txt("item-del"));
    }

    public ItemStack ok() {
        return icon("ok", Material.LIME_CONCRETE, txt("item-ok"));
    }

    public ItemStack codeFilled() {
        return icon("code-filled", Material.LIME_STAINED_GLASS_PANE, txt("item-code-filled"));
    }

    public ItemStack codeEmpty() {
        return icon("code-empty", Material.LIGHT_GRAY_STAINED_GLASS_PANE, txt("item-code-empty"));
    }

    public ItemStack changeCode() {
        return icon("change-code", Material.TRIPWIRE_HOOK, txt("item-change-code"), txt("item-change-code-lore"));
    }

    public ItemStack trustedButton(int count) {
        return icon("trusted", Material.PLAYER_HEAD, txt("item-trusted"), txt("item-trusted-lore"),
                txt("item-trusted-count", "%count%", String.valueOf(count)));
    }

    public ItemStack removeButton() {
        return icon("remove", Material.RED_CONCRETE, txt("item-remove"), txt("item-remove-lore"));
    }

    public ItemStack close() {
        return icon("close", Material.BARRIER, txt("item-close"));
    }

    public ItemStack back() {
        return icon("back", Material.ARROW, txt("item-back"));
    }

    public ItemStack yes() {
        return icon("yes", Material.LIME_CONCRETE, txt("item-yes"));
    }

    public ItemStack no() {
        return icon("no", Material.RED_CONCRETE, txt("item-no"));
    }

    public ItemStack playerHead(OfflinePlayer player, boolean trusted) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        String name = player.getName() != null ? player.getName() : "?";
        meta.displayName(Component.text(name, trusted ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(txt(trusted ? "item-trusted-on" : "item-trusted-off")));
        if (trusted) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }
}
