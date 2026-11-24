package cz.failxos.failchestlock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final FailChestLock plugin;

    public Messages(FailChestLock plugin) {
        this.plugin = plugin;
    }

    public String raw(String key) {
        return plugin.getConfig().getString("messages." + key, "<red>Chybi zprava: " + key);
    }

    private static String apply(String text, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace(pairs[i], pairs[i + 1]);
        }
        return text;
    }

    /** Text bez prefixu (nadpisy menu). */
    public Component component(String key, String... pairs) {
        return MM.deserialize(apply(raw(key), pairs));
    }

    /** Text pro nazvy a lore itemu: bez kurzivy. */
    public Component item(String key, String... pairs) {
        return MM.deserialize(apply(raw(key), pairs)).decoration(TextDecoration.ITALIC, false);
    }

    public void send(Player to, String key, String... pairs) {
        if (raw(key).isBlank()) {
            return;
        }
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        to.sendMessage(MM.deserialize(prefix + apply(raw(key), pairs)));
    }
}
