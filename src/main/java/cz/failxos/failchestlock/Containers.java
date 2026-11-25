package cz.failxos.failchestlock;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

public final class Containers {

    private Containers() {
    }

    /** Returns every half of a container (both halves of a double chest, otherwise just the block). */
    public static List<Block> halves(Block block) {
        List<Block> list = new ArrayList<>(2);
        BlockState state = block.getState();
        if (state instanceof Chest chest && chest.getInventory().getHolder() instanceof DoubleChest dc) {
            add(list, dc.getLeftSide());
            add(list, dc.getRightSide());
        }
        if (list.isEmpty()) {
            list.add(block);
        }
        return list;
    }

    private static void add(List<Block> list, InventoryHolder holder) {
        if (holder instanceof BlockState s) {
            list.add(s.getBlock());
        }
    }
}
