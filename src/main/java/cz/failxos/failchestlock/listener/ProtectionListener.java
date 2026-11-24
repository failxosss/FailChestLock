package cz.failxos.failchestlock.listener;

import cz.failxos.failchestlock.BlockPos;
import cz.failxos.failchestlock.Containers;
import cz.failxos.failchestlock.FailChestLock;
import cz.failxos.failchestlock.Lock;
import cz.failxos.failchestlock.LockManager;
import cz.failxos.failchestlock.gui.Menus;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Otevirani zamcenych chestek, ochrana pred rozbitim, vybuchy a hoppery. */
public final class ProtectionListener implements Listener {

    private final FailChestLock plugin;

    public ProtectionListener(FailChestLock plugin) {
        this.plugin = plugin;
    }

    private LockManager manager() {
        return plugin.manager();
    }

    private Menus menus() {
        return plugin.menus();
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    // ---------- otevreni ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block == null || !manager().isLockable(block.getType())) {
            return;
        }
        Player p = e.getPlayer();
        boolean mainHand = e.getHand() == EquipmentSlot.HAND;
        Lock lock = manager().get(block);
        BlockPos pos = BlockPos.of(block);

        if (lock == null) {
            // zamknuti uz stojici chestky nastrojem (Shift + pravy klik)
            Material tool = manager().lockTool();
            ItemStack item = e.getItem();
            if (mainHand && p.isSneaking() && tool != null && item != null && item.getType() == tool) {
                e.setCancelled(true);
                menus().later(() -> {
                    if (p.isOnline() && manager().get(pos.block()) == null) {
                        menus().openSet(p, pos, null, false, 1, null);
                    }
                });
            }
            return;
        }

        if (p.hasPermission(FailChestLock.BYPASS)) {
            return;
        }

        if (lock.owner.equals(p.getUniqueId())) {
            if (mainHand && p.isSneaking() && isEmpty(e.getItem())) {
                e.setCancelled(true);
                menus().later(() -> {
                    if (p.isOnline() && manager().exists(lock)) {
                        menus().openOwner(p, pos, lock);
                    }
                });
            }
            return;
        }

        if (lock.trusted.contains(p.getUniqueId()) || manager().isUnlocked(p, lock)) {
            return;
        }

        e.setCancelled(true);
        if (mainHand) {
            menus().later(() -> {
                if (p.isOnline() && manager().exists(lock)) {
                    menus().openEnter(p, pos, lock);
                }
            });
        }
    }

    // ---------- pokladani ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block placed = e.getBlockPlaced();
        if (!manager().isLockable(placed.getType())) {
            return;
        }
        Player p = e.getPlayer();
        boolean bypass = p.hasPermission(FailChestLock.BYPASS);

        Lock joined = null;
        for (Block other : Containers.halves(placed)) {
            if (other.equals(placed)) {
                continue;
            }
            Lock lock = manager().get(other);
            if (lock == null) {
                continue;
            }
            if (!lock.owner.equals(p.getUniqueId()) && !bypass) {
                e.setCancelled(true);
                plugin.messages().send(p, "place-denied");
                return;
            }
            joined = lock;
        }

        if (joined != null) {
            manager().extend(joined, placed);
            return;
        }

        if (plugin.getConfig().getBoolean("auto-prompt-on-place", true)) {
            BlockPos pos = BlockPos.of(placed);
            menus().later(() -> {
                Block block = pos.block();
                if (p.isOnline() && block != null && manager().isLockable(block.getType())
                        && manager().get(block) == null) {
                    menus().openSet(p, pos, null, false, 1, null);
                }
            });
        }
    }

    // ---------- rozbijeni ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Lock lock = manager().get(e.getBlock());
        if (lock == null) {
            return;
        }
        Player p = e.getPlayer();
        if (lock.owner.equals(p.getUniqueId()) || p.hasPermission(FailChestLock.BYPASS)) {
            return;
        }
        e.setCancelled(true);
        plugin.messages().send(p, "break-denied");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakDone(BlockBreakEvent e) {
        manager().removePosition(e.getBlock());
    }

    // ---------- vybuchy a ostatni zniceni ----------

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (plugin.getConfig().getBoolean("protect.explosions", true)) {
            e.blockList().removeIf(b -> manager().isLocked(b));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (plugin.getConfig().getBoolean("protect.explosions", true)) {
            e.blockList().removeIf(b -> manager().isLocked(b));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (plugin.getConfig().getBoolean("protect.explosions", true) && manager().isLocked(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    // ---------- hoppery ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent e) {
        if (!plugin.getConfig().getBoolean("protect.hoppers", true)) {
            return;
        }
        if (isLockedInventory(e.getSource()) || isLockedInventory(e.getDestination())) {
            e.setCancelled(true);
        }
    }

    private boolean isLockedInventory(Inventory inventory) {
        Location loc = inventory.getLocation();
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        return manager().getAt(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) != null;
    }
}
