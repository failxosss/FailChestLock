package cz.failxos.failchestlock.gui;

import cz.failxos.failchestlock.BlockPos;
import cz.failxos.failchestlock.Containers;
import cz.failxos.failchestlock.FailChestLock;
import cz.failxos.failchestlock.Lock;
import cz.failxos.failchestlock.LockManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vsechna menu zamku (otevirani + obsluha kliku). */
public final class Menus implements Listener {

    /** Sloty cisel 0-9 v 54-slotovem ciselniku. */
    private static final int[] DIGIT_SLOTS = {49, 21, 22, 23, 30, 31, 32, 39, 40, 41};
    private static final int DEL_SLOT = 48;
    private static final int OK_SLOT = 50;

    private final FailChestLock plugin;
    private final Items items;
    private final Map<UUID, LockMenu> open = new HashMap<>();

    public Menus(FailChestLock plugin) {
        this.plugin = plugin;
        this.items = new Items(plugin);
    }

    private LockManager manager() {
        return plugin.manager();
    }

    public void later(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private int maxLength() {
        return Math.max(4, Math.min(9, plugin.getConfig().getInt("code.max-length", 6)));
    }

    private int minLength() {
        return Math.max(1, Math.min(maxLength(), plugin.getConfig().getInt("code.min-length", 4)));
    }

    // ---------- zvuky ----------

    private static void click(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    private static void fail(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
    }

    private static void success(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }

    // ---------- otevirani menu ----------

    private void show(Player p, LockMenu menu) {
        p.openInventory(menu.getInventory());
        open.put(p.getUniqueId(), menu);
    }

    private Inventory create(LockMenu menu, int size, Component title) {
        Inventory inv = Bukkit.createInventory(menu, size, title);
        menu.setInventory(inv);
        ItemStack filler = items.filler();
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }
        return inv;
    }

    private void keypad(LockMenu menu, Component title) {
        Inventory inv = create(menu, 54, title);
        for (int d = 0; d < 10; d++) {
            inv.setItem(DIGIT_SLOTS[d], items.digit(d));
        }
        inv.setItem(DEL_SLOT, items.del());
        inv.setItem(OK_SLOT, items.ok());
        refreshDisplay(menu);
    }

    private void refreshDisplay(LockMenu menu) {
        int n = maxLength();
        int start = 9 + (9 - n) / 2;
        Inventory inv = menu.getInventory();
        for (int i = 0; i < n; i++) {
            inv.setItem(start + i, i < menu.input.length() ? items.codeFilled() : items.codeEmpty());
        }
    }

    public void openEnter(Player p, BlockPos pos, Lock lock) {
        long left = manager().lockoutRemaining(p.getUniqueId(), lock.id);
        if (left > 0) {
            fail(p);
            plugin.messages().send(p, "locked-out", "%time%", String.valueOf((left + 999L) / 1000L));
            return;
        }
        LockMenu menu = new LockMenu(LockMenu.Type.ENTER, p.getUniqueId(), pos, lock);
        keypad(menu, plugin.messages().component("title-enter"));
        show(p, menu);
    }

    public void openSet(Player p, BlockPos pos, Lock lock, boolean change, int stage, String first) {
        LockMenu menu = new LockMenu(LockMenu.Type.SET, p.getUniqueId(), pos, lock);
        menu.change = change;
        menu.stage = stage;
        menu.firstCode = first;
        keypad(menu, plugin.messages().component(stage == 1 ? "title-set" : "title-repeat"));
        show(p, menu);
    }

    public void openOwner(Player p, BlockPos pos, Lock lock) {
        LockMenu menu = new LockMenu(LockMenu.Type.OWNER, p.getUniqueId(), pos, lock);
        Inventory inv = create(menu, 27, plugin.messages().component("title-owner"));
        inv.setItem(11, items.changeCode());
        inv.setItem(13, items.trustedButton(lock.trusted.size()));
        inv.setItem(15, items.removeButton());
        inv.setItem(22, items.close());
        show(p, menu);
    }

    public void openTrusted(Player p, BlockPos pos, Lock lock) {
        LockMenu menu = new LockMenu(LockMenu.Type.TRUSTED, p.getUniqueId(), pos, lock);
        create(menu, 54, plugin.messages().component("title-trusted"));
        fillTrusted(menu);
        show(p, menu);
    }

    private void fillTrusted(LockMenu menu) {
        Inventory inv = menu.getInventory();
        Lock lock = menu.lock;
        ItemStack filler = items.filler();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, i >= 45 ? filler : null);
        }

        List<UUID> list = new ArrayList<>(lock.trusted);
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID id = online.getUniqueId();
            if (!id.equals(lock.owner) && !list.contains(id)) {
                list.add(id);
            }
        }
        menu.listed = new UUID[45];
        for (int i = 0; i < list.size() && i < 45; i++) {
            UUID id = list.get(i);
            menu.listed[i] = id;
            inv.setItem(i, items.playerHead(Bukkit.getOfflinePlayer(id), lock.trusted.contains(id)));
        }
        inv.setItem(49, items.back());
    }

    public void openRemove(Player p, BlockPos pos, Lock lock) {
        LockMenu menu = new LockMenu(LockMenu.Type.REMOVE, p.getUniqueId(), pos, lock);
        Inventory inv = create(menu, 27, plugin.messages().component("title-remove"));
        inv.setItem(11, items.yes());
        inv.setItem(15, items.no());
        show(p, menu);
    }

    private void openContainer(Player p, Block block) {
        if (block != null && block.getState() instanceof Container container) {
            p.openInventory(container.getInventory());
        } else {
            p.closeInventory();
        }
    }

    // ---------- udalosti ----------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        LockMenu menu = open.get(p.getUniqueId());
        if (menu == null || e.getView().getTopInventory() != menu.getInventory()) {
            return;
        }
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= menu.getInventory().getSize()) {
            return;
        }
        handle(p, menu, slot);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        LockMenu menu = open.get(p.getUniqueId());
        if (menu != null && e.getView().getTopInventory() == menu.getInventory()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) {
            return;
        }
        LockMenu menu = open.get(p.getUniqueId());
        if (menu == null || menu.getInventory() != e.getInventory()) {
            return;
        }
        open.remove(p.getUniqueId());
        if (menu.type == LockMenu.Type.SET && !menu.change && !menu.finished) {
            plugin.messages().send(p, "lock-skipped");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        open.remove(e.getPlayer().getUniqueId());
        manager().forget(e.getPlayer().getUniqueId());
    }

    // ---------- kliky ----------

    private void handle(Player p, LockMenu menu, int slot) {
        if (menu.finished) {
            return;
        }
        switch (menu.type) {
            case ENTER, SET -> keypadClick(p, menu, slot);
            case OWNER -> ownerClick(p, menu, slot);
            case TRUSTED -> trustedClick(p, menu, slot);
            case REMOVE -> removeClick(p, menu, slot);
        }
    }

    private boolean canManage(Player p, LockMenu menu) {
        return manager().exists(menu.lock)
                && (menu.lock.owner.equals(p.getUniqueId()) || p.hasPermission(FailChestLock.BYPASS));
    }

    private void keypadClick(Player p, LockMenu menu, int slot) {
        for (int d = 0; d < 10; d++) {
            if (DIGIT_SLOTS[d] == slot) {
                if (menu.input.length() < maxLength()) {
                    menu.input.append(d);
                    refreshDisplay(menu);
                    click(p);
                }
                return;
            }
        }
        if (slot == DEL_SLOT) {
            if (menu.input.length() > 0) {
                menu.input.setLength(menu.input.length() - 1);
                refreshDisplay(menu);
                click(p);
            }
            return;
        }
        if (slot == OK_SLOT) {
            if (menu.type == LockMenu.Type.ENTER) {
                submitEnter(p, menu);
            } else {
                submitSet(p, menu);
            }
        }
    }

    private void submitEnter(Player p, LockMenu menu) {
        Lock lock = menu.lock;
        if (!manager().exists(lock)) {
            menu.finished = true;
            later(() -> p.closeInventory());
            return;
        }
        if (menu.input.length() == 0) {
            return;
        }
        String code = menu.input.toString();
        if (manager().matches(lock, code)) {
            manager().clearFails(p.getUniqueId(), lock.id);
            manager().grantUnlock(p, lock);
            success(p);
            menu.finished = true;
            Block block = menu.pos.block();
            later(() -> openContainer(p, block));
            return;
        }

        int left = manager().registerFail(p.getUniqueId(), lock.id);
        menu.input.setLength(0);
        refreshDisplay(menu);
        fail(p);
        if (left <= 0) {
            long ms = manager().lockoutRemaining(p.getUniqueId(), lock.id);
            plugin.messages().send(p, "locked-out", "%time%", String.valueOf((ms + 999L) / 1000L));
            menu.finished = true;
            later(() -> p.closeInventory());
        } else {
            plugin.messages().send(p, "wrong-code", "%left%", String.valueOf(left));
        }
    }

    private void submitSet(Player p, LockMenu menu) {
        String code = menu.input.toString();
        int min = minLength();
        if (code.length() < min) {
            fail(p);
            plugin.messages().send(p, "code-too-short", "%min%", String.valueOf(min));
            return;
        }

        if (menu.stage == 1) {
            click(p);
            menu.finished = true;
            later(() -> openSet(p, menu.pos, menu.lock, menu.change, 2, code));
            return;
        }

        if (!code.equals(menu.firstCode)) {
            fail(p);
            plugin.messages().send(p, "code-mismatch");
            menu.finished = true;
            later(() -> openSet(p, menu.pos, menu.lock, menu.change, 1, null));
            return;
        }

        menu.finished = true;
        if (menu.change) {
            if (!manager().exists(menu.lock)) {
                later(() -> p.closeInventory());
                return;
            }
            manager().setCode(menu.lock, code);
            plugin.messages().send(p, "code-changed");
        } else {
            Block block = menu.pos.block();
            if (block == null || !manager().isLockable(block.getType()) || manager().get(block) != null) {
                later(() -> p.closeInventory());
                return;
            }
            manager().create(p.getUniqueId(), Containers.halves(block), code);
            plugin.messages().send(p, "code-set");
        }
        success(p);
        later(() -> p.closeInventory());
    }

    private void ownerClick(Player p, LockMenu menu, int slot) {
        if (!canManage(p, menu)) {
            menu.finished = true;
            later(() -> p.closeInventory());
            return;
        }
        switch (slot) {
            case 11 -> {
                click(p);
                menu.finished = true;
                later(() -> openSet(p, menu.pos, menu.lock, true, 1, null));
            }
            case 13 -> {
                click(p);
                menu.finished = true;
                later(() -> openTrusted(p, menu.pos, menu.lock));
            }
            case 15 -> {
                click(p);
                menu.finished = true;
                later(() -> openRemove(p, menu.pos, menu.lock));
            }
            case 22 -> {
                menu.finished = true;
                later(() -> p.closeInventory());
            }
            default -> {
            }
        }
    }

    private void trustedClick(Player p, LockMenu menu, int slot) {
        if (!canManage(p, menu)) {
            menu.finished = true;
            later(() -> p.closeInventory());
            return;
        }
        if (slot == 49) {
            click(p);
            menu.finished = true;
            later(() -> openOwner(p, menu.pos, menu.lock));
            return;
        }
        if (slot >= 0 && slot < menu.listed.length && menu.listed[slot] != null) {
            UUID id = menu.listed[slot];
            if (!menu.lock.trusted.remove(id)) {
                menu.lock.trusted.add(id);
            }
            manager().markDirty();
            click(p);
            fillTrusted(menu);
        }
    }

    private void removeClick(Player p, LockMenu menu, int slot) {
        if (!canManage(p, menu)) {
            menu.finished = true;
            later(() -> p.closeInventory());
            return;
        }
        if (slot == 11) {
            manager().remove(menu.lock);
            plugin.messages().send(p, "lock-removed");
            success(p);
            menu.finished = true;
            later(() -> p.closeInventory());
        } else if (slot == 15) {
            click(p);
            menu.finished = true;
            later(() -> openOwner(p, menu.pos, menu.lock));
        }
    }
}
