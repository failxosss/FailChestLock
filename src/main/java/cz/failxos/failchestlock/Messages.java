package cz.failxos.failchestlock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Loads texts from lang/&lt;language&gt;.yml (MiniMessage format). */
public final class Messages {

    public static final List<String> BUNDLED = List.of(
            "en", "zh", "hi", "es", "fr", "ar", "bn", "pt", "ru", "ur", "id",
            "de", "ja", "tr", "vi", "ko", "it", "te", "mr", "ta", "cs", "sk");

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final FailChestLock plugin;
    private YamlConfiguration selected = new YamlConfiguration();
    private YamlConfiguration fallback = new YamlConfiguration();

    public Messages(FailChestLock plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File dir = new File(plugin.getDataFolder(), "lang");
        for (String code : BUNDLED) {
            if (!new File(dir, code + ".yml").exists()) {
                plugin.saveResource("lang/" + code + ".yml", false);
            }
        }
        fallback = bundled("en");

        String code = plugin.getConfig().getString("language", "en").trim().toLowerCase(Locale.ROOT);
        File file = new File(dir, code + ".yml");
        if (!code.matches("[a-z0-9_-]+") || !file.isFile()) {
            plugin.getLogger().warning("Language '" + code + "' not found, using English.");
            selected = fallback;
        } else {
            selected = YamlConfiguration.loadConfiguration(file);
        }
    }

    private YamlConfiguration bundled(String code) {
        try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
            if (in == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new YamlConfiguration();
        }
    }

    public String raw(String key) {
        String value = selected.getString(key);
        if (value == null) {
            value = fallback.getString(key);
        }
        return value == null ? "<red>Missing text: " + key : value;
    }

    private static String apply(String text, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace(pairs[i], pairs[i + 1]);
        }
        return text;
    }

    /** Text without prefix (menu titles). */
    public Component component(String key, String... pairs) {
        return MM.deserialize(apply(raw(key), pairs));
    }

    /** Text for item names and lore (no italics). */
    public Component item(String key, String... pairs) {
        return MM.deserialize(apply(raw(key), pairs)).decoration(TextDecoration.ITALIC, false);
    }

    public void send(Player to, String key, String... pairs) {
        String text = raw(key);
        if (text.isBlank()) {
            return;
        }
        to.sendMessage(MM.deserialize(raw("prefix") + apply(text, pairs)));
    }
}
