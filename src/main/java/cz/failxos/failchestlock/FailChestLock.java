package cz.failxos.failchestlock;

import cz.failxos.failchestlock.gui.Menus;
import cz.failxos.failchestlock.listener.ProtectionListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FailChestLock extends JavaPlugin {

    public static final String BYPASS = "failchestlock.bypass";

    private Messages messages;
    private LockManager manager;
    private Menus menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);
        manager = new LockManager(this);
        manager.reloadSettings();
        manager.load();
        menus = new Menus(this);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(menus, this);
        pm.registerEvents(new ProtectionListener(this), this);

        getLogger().info("FailChestLock " + getPluginMeta().getVersion() + " by Failxos nacten.");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.save();
        }
    }

    public Messages messages() {
        return messages;
    }

    public LockManager manager() {
        return manager;
    }

    public Menus menus() {
        return menus;
    }
}
