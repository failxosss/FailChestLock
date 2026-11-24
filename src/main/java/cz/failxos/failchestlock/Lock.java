package cz.failxos.failchestlock;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Jeden zamek. Dvojita chestka = jeden zamek se dvema pozicemi. */
public final class Lock {

    public final UUID id;
    public final UUID owner;
    public String salt;
    public String hash;
    /** Zvysuje se pri zmene kodu, aby zmizela drivejsi odemceni. */
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
