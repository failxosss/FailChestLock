package cz.failxos.failchestlock.gui;

import cz.failxos.failchestlock.BlockPos;
import cz.failxos.failchestlock.Lock;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** State of one open lock menu. */
public final class LockMenu implements InventoryHolder {

    public enum Type { ENTER, SET, SCOPE, OWNER, TRUSTED, REMOVE }

    public final Type type;
    public final UUID viewer;
    public final BlockPos pos;
    public final Lock lock;

    /** SET: true = changing the code of an existing lock, false = locking for the first time. */
    public boolean change;
    /** SET (change): true = apply to all of the owner's chests and the default code. */
    public boolean all;
    /** SET: 1 = enter the code, 2 = repeat it. */
    public int stage = 1;
    public String firstCode;
    public final StringBuilder input = new StringBuilder();
    /** TRUSTED: the player in each slot. */
    public UUID[] listed = new UUID[0];
    /** The menu is done or switching to another one, further clicks are ignored. */
    public boolean finished;

    private Inventory inventory;

    public LockMenu(Type type, UUID viewer, BlockPos pos, Lock lock) {
        this.type = type;
        this.viewer = viewer;
        this.pos = pos;
        this.lock = lock;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
