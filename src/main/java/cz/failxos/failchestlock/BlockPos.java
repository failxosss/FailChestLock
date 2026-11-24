package cz.failxos.failchestlock;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Arrays;

public record BlockPos(String world, int x, int y, int z) {

    public static BlockPos of(Block block) {
        return new BlockPos(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public Block block() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : w.getBlockAt(x, y, z);
    }

    public String serialize() {
        return world + ";" + x + ";" + y + ";" + z;
    }

    public static BlockPos parse(String text) {
        String[] parts = text.split(";");
        int n = parts.length;
        if (n < 4) {
            return null;
        }
        try {
            String world = String.join(";", Arrays.copyOfRange(parts, 0, n - 3));
            return new BlockPos(world, Integer.parseInt(parts[n - 3]),
                    Integer.parseInt(parts[n - 2]), Integer.parseInt(parts[n - 1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
