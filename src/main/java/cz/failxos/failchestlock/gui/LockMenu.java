package cz.failxos.failchestlock.gui;

import cz.failxos.failchestlock.BlockPos;
import cz.failxos.failchestlock.Lock;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/** Stav jednoho otevreneho menu zamku. */
public final class LockMenu implements InventoryHolder {

    public enum Type { ENTER, SET, OWNER, TRUSTED, REMOVE }

    public final Type type;
    public final UUID viewer;
    public final BlockPos pos;
    public final Lock lock;

    /** SET: true = zmena kodu existujiciho zamku, false = prvni zamknuti. */
    public boolean change;
    /** SET: 1 = zadani kodu, 2 = zopakovani. */
    public int stage = 1;
    public String firstCode;
    public final StringBuilder input = new StringBuilder();
    /** TRUSTED: hrac na kazdem slotu. */
    public UUID[] listed = new UUID[0];
    /** Menu je hotove / prechazi jinam - dalsi kliky se ignoruji. */
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
