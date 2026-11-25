package cz.failxos.failchestlock;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** One lock. A double chest is a single lock with two positions. */
public final class Lock {

    public final UUID id;
    public final UUID owner;
    public String salt;
    public String hash;
    /** Incremented when the code changes, so earlier unlocks stop working. */
    public int version;
    public final Set<UUID> trusted = new LinkedHashSet<>();
    public final Set<BlockPos> positions = new LinkedHashSet<>();

    public Lock(UUID id, UUID owner, String salt, String hash) {
        this.id = id;
        this.owner = owner;
        this.salt = salt;
        this.hash = hash;
    }
}
