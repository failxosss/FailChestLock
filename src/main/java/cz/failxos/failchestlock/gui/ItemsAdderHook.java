package cz.failxos.failchestlock.gui;

import cz.failxos.failchestlock.FailChestLock;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Napojeni na ItemsAdder pres reflexi (zadna build zavislost, plugin funguje i bez ItemsAdderu).
 * Ikony se berou z config.yml: itemsadder.icons.<klic> = namespace:item_id
 */
public final class ItemsAdderHook {

    private final FailChestLock plugin;
    private Method getInstance;
    private Method getItemStack;
    private final Set<String> warned = new HashSet<>();

    public ItemsAdderHook(FailChestLock plugin) {
        this.plugin = plugin;
    }

    /** Vrati kopii ItemsAdder itemu pro dany klic ikony, nebo null (pak se pouzije vanilla item). */
    public ItemStack icon(String key) {
        if (!plugin.getConfig().getBoolean("itemsadder.enabled", true)) {
            return null;
        }
        String id = plugin.getConfig().getString("itemsadder.icons." + key, "");
        if (id == null || id.isBlank()) {
            return null;
        }
        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null || !itemsAdder.isEnabled()) {
            return null;
        }
        try {
            if (getInstance == null) {
                Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack", true,
                        itemsAdder.getClass().getClassLoader());
                getInstance = customStack.getMethod("getInstance", String.class);
                getItemStack = customStack.getMethod("getItemStack");
            }
            Object stack = getInstance.invoke(null, id.trim());
            if (stack == null) {
                warnOnce(key, "ItemsAdder item nenalezen: " + id + " (ikona " + key + ")");
                return null;
            }
            Object item = getItemStack.invoke(stack);
            if (item instanceof ItemStack result) {
                return result.clone();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            warnOnce(key, "Nepodarilo se nacist ItemsAdder ikonu " + key + ": " + e);
        }
        return null;
    }

    private void warnOnce(String key, String message) {
        if (warned.add(key)) {
            plugin.getLogger().warning(message);
        }
    }
}
