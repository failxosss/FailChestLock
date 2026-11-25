package cz.failxos.failchestlock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LockManager {

    public record Unlock(long until, int version) {
    }

    /** Salted hash of a code (the code itself is never stored). */
    public record Credentials(String salt, String hash) {
    }

    private static final class Attempt {
        int fails;
        long lockedUntil;
    }

    private final FailChestLock plugin;
    private final File file;
    private final SecureRandom random = new SecureRandom();
    private final Map<BlockPos, Lock> byPos = new HashMap<>();
    private final Map<UUID, Lock> byId = new HashMap<>();
    private final Map<UUID, Credentials> defaults = new HashMap<>();
    private final Map<UUID, Map<UUID, Unlock>> unlocked = new HashMap<>();
    private final Map<String, Attempt> attempts = new HashMap<>();
    private Set<Material> lockable = EnumSet.noneOf(Material.class);
    private Material lockTool;
    private boolean saveQueued;

    public LockManager(FailChestLock plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locks.yml");
    }

    // ---------- settings ----------

    public void reloadSettings() {
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String name : plugin.getConfig().getStringList("lockable")) {
            Material m = Material.matchMaterial(name);
            if (m == null) {
                plugin.getLogger().warning("Unknown material in lockable: " + name);
            } else {
                set.add(m);
            }
        }
        lockable = set;

        String tool = plugin.getConfig().getString("lock-tool", "");
        lockTool = (tool == null || tool.isBlank()) ? null : Material.matchMaterial(tool);
    }

    public boolean isLockable(Material material) {
        return lockable.contains(material);
    }

    public Material lockTool() {
        return lockTool;
    }

    public boolean defaultCodeEnabled() {
        return plugin.getConfig().getBoolean("default-code.enabled", true);
    }

    // ---------- storage ----------

    public void load() {
        byPos.clear();
        byId.clear();
        defaults.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> m : yml.getMapList("locks")) {
            try {
                UUID id = UUID.fromString(String.valueOf(m.get("id")));
                UUID owner = UUID.fromString(String.valueOf(m.get("owner")));
                Lock lock = new Lock(id, owner, String.valueOf(m.get("salt")), String.valueOf(m.get("hash")));
                if (m.get("version") instanceof Number n) {
                    lock.version = n.intValue();
                }
                if (m.get("trusted") instanceof List<?> list) {
                    for (Object o : list) {
                        lock.trusted.add(UUID.fromString(String.valueOf(o)));
                    }
                }
                if (m.get("blocks") instanceof List<?> list) {
                    for (Object o : list) {
                        BlockPos pos = BlockPos.parse(String.valueOf(o));
                        if (pos != null) {
                            lock.positions.add(pos);
                        }
                    }
                }
                if (lock.positions.isEmpty()) {
                    continue;
                }
                byId.put(id, lock);
                for (BlockPos pos : lock.positions) {
                    byPos.put(pos, lock);
                }
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipped a damaged lock entry: " + ex.getMessage());
            }
        }

        ConfigurationSection section = yml.getConfigurationSection("defaults");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID player = UUID.fromString(key);
                    String salt = section.getString(key + ".salt");
                    String hash = section.getString(key + ".hash");
                    if (salt != null && hash != null) {
                        defaults.put(player, new Credentials(salt, hash));
                    }
                } catch (IllegalArgumentException ignored) {
                    // not a UUID, skip
                }
            }
        }
        plugin.getLogger().info("Loaded locks: " + byId.size());
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Lock lock : byId.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", lock.id.toString());
            m.put("owner", lock.owner.toString());
            m.put("salt", lock.salt);
            m.put("hash", lock.hash);
            m.put("version", lock.version);
            m.put("trusted", new ArrayList<>(lock.trusted.stream().map(UUID::toString).toList()));
            m.put("blocks", new ArrayList<>(lock.positions.stream().map(BlockPos::serialize).toList()));
            list.add(m);
        }
        yml.set("locks", list);
        for (Map.Entry<UUID, Credentials> entry : defaults.entrySet()) {
            yml.set("defaults." + entry.getKey() + ".salt", entry.getValue().salt());
            yml.set("defaults." + entry.getKey() + ".hash", entry.getValue().hash());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locks.yml: " + e.getMessage());
        }
    }

    public void markDirty() {
        if (saveQueued) {
            return;
        }
        if (!plugin.isEnabled()) {
            save();
            return;
        }
        saveQueued = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            saveQueued = false;
            save();
        });
    }

    // ---------- queries ----------

    public Lock get(Block block) {
        BlockPos pos = BlockPos.of(block);
        Lock lock = byPos.get(pos);
        if (lock != null && !isLockable(block.getType())) {
            dropPosition(pos, lock);
            return null;
        }
        return lock;
    }

    public Lock getAt(String world, int x, int y, int z) {
        return byPos.get(new BlockPos(world, x, y, z));
    }

    public boolean isLocked(Block block) {
        return byPos.containsKey(BlockPos.of(block));
    }

    public boolean exists(Lock lock) {
        return lock != null && byId.get(lock.id) == lock;
    }

    // ---------- changes ----------

    public Lock create(UUID owner, Collection<Block> blocks, String code) {
        String salt = newSalt();
        return createWith(owner, blocks, salt, hash(salt, code));
    }

    /** Locks the blocks with the player's default code, or returns null if the player has none. */
    public Lock createFromDefault(UUID owner, Collection<Block> blocks) {
        Credentials credentials = defaults.get(owner);
        if (credentials == null) {
            return null;
        }
        return createWith(owner, blocks, credentials.salt(), credentials.hash());
    }

    private Lock createWith(UUID owner, Collection<Block> blocks, String salt, String hash) {
        Lock lock = new Lock(UUID.randomUUID(), owner, salt, hash);
        for (Block block : blocks) {
            BlockPos pos = BlockPos.of(block);
            lock.positions.add(pos);
            byPos.put(pos, lock);
        }
        byId.put(lock.id, lock);
        markDirty();
        return lock;
    }

    public boolean hasDefault(UUID player) {
        return defaults.containsKey(player);
    }

    /** Remembers this lock's code as the player's default code (only if they have none yet). */
    public void rememberDefault(UUID player, Lock lock) {
        if (!defaults.containsKey(player)) {
            defaults.put(player, new Credentials(lock.salt, lock.hash));
            markDirty();
        }
    }

    public void clearDefault(UUID player) {
        if (defaults.remove(player) != null) {
            markDirty();
        }
    }

    public void extend(Lock lock, Block block) {
        BlockPos pos = BlockPos.of(block);
        lock.positions.add(pos);
        byPos.put(pos, lock);
        markDirty();
    }

    public void remove(Lock lock) {
        for (BlockPos pos : lock.positions) {
            byPos.remove(pos);
        }
        byId.remove(lock.id);
        markDirty();
    }

    /** A block is gone (broken): removes its position from the lock. */
    public void removePosition(Block block) {
        BlockPos pos = BlockPos.of(block);
        Lock lock = byPos.get(pos);
        if (lock != null) {
            dropPosition(pos, lock);
        }
    }

    private void dropPosition(BlockPos pos, Lock lock) {
        byPos.remove(pos);
        lock.positions.remove(pos);
        if (lock.positions.isEmpty()) {
            byId.remove(lock.id);
        }
        markDirty();
    }

    /** Changes the code of a single lock. */
    public void setCode(Lock lock, String code) {
        lock.salt = newSalt();
        lock.hash = hash(lock.salt, code);
        lock.version++;
        markDirty();
    }

    /** Changes the code of every lock owned by the player and makes it their default code. */
    public void setCodeAll(UUID owner, String code) {
        String salt = newSalt();
        String hash = hash(salt, code);
        for (Lock lock : byId.values()) {
            if (lock.owner.equals(owner)) {
                lock.salt = salt;
                lock.hash = hash;
                lock.version++;
            }
        }
        defaults.put(owner, new Credentials(salt, hash));
        markDirty();
    }

    public boolean matches(Lock lock, String code) {
        byte[] a = hash(lock.salt, code).getBytes(StandardCharsets.UTF_8);
        byte[] b = lock.hash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private String newSalt() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String hash(String salt, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest((salt + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- player unlocks ----------

    public boolean isUnlocked(Player player, Lock lock) {
        Map<UUID, Unlock> map = unlocked.get(player.getUniqueId());
        if (map == null) {
            return false;
        }
        Unlock u = map.get(lock.id);
        if (u == null) {
            return false;
        }
        if (u.until() < System.currentTimeMillis() || u.version() != lock.version) {
            map.remove(lock.id);
            return false;
        }
        return true;
    }

    public void grantUnlock(Player player, Lock lock) {
        long minutes = plugin.getConfig().getLong("unlock-minutes", 5L);
        long until = System.currentTimeMillis() + minutes * 60_000L;
        unlocked.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(lock.id, new Unlock(until, lock.version));
    }

    public void forget(UUID player) {
        unlocked.remove(player);
    }

    // ---------- wrong attempts ----------

    private static String key(UUID player, UUID lockId) {
        return player + ":" + lockId;
    }

    /** Remaining lockout time in ms (0 = not blocked). */
    public long lockoutRemaining(UUID player, UUID lockId) {
        String key = key(player, lockId);
        Attempt a = attempts.get(key);
        if (a == null || a.lockedUntil == 0L) {
            return 0L;
        }
        long left = a.lockedUntil - System.currentTimeMillis();
        if (left <= 0L) {
            attempts.remove(key);
            return 0L;
        }
        return left;
    }

    /** Counts a wrong attempt and returns the attempts left (0 = the player is blocked now). */
    public int registerFail(UUID player, UUID lockId) {
        int max = Math.max(1, plugin.getConfig().getInt("max-attempts", 3));
        Attempt a = attempts.computeIfAbsent(key(player, lockId), k -> new Attempt());
        a.fails++;
        if (a.fails >= max) {
            long seconds = Math.max(1L, plugin.getConfig().getLong("lockout-seconds", 60L));
            a.lockedUntil = System.currentTimeMillis() + seconds * 1000L;
            return 0;
        }
        return max - a.fails;
    }

    public void clearFails(UUID player, UUID lockId) {
        attempts.remove(key(player, lockId));
    }
}
