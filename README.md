# FailChestLock

Chest locks with a code for Paper **1.21.11** (Java 21), by **Failxos**.
The plugin has no commands, everything is done in menus.

## Build
- GitHub: push the project, open **Actions** -> *Build* and download the `FailChestLock` artifact (the jar).
- Locally: `mvn clean package` -> `target/FailChestLock-1.0.0.jar`

## How it works
- **Placing a chest or barrel:** the first time you get a menu to set a code (entered twice). Every chest you place afterwards is locked with the same code automatically.
- **Other players:** right-click opens a number pad, a correct code opens the chest. Too many wrong codes block the player for a while.
- **Owner:** opens the chest without a code. **Shift + right-click with an empty hand** opens the owner menu:
  - change the code (for this chest only, or for all your chests and the default code),
  - trusted players (they don't need the code),
  - remove the lock (affects only that chest unless `default-code.clear-on-remove` is on).
- **Locking an existing chest:** Shift + right-click with a tripwire hook in hand (`lock-tool`).
- **Protection:** only the owner can break a locked chest, hoppers can't move items, explosions don't destroy it, and it can't be joined to someone else's locked chest.

Anyone who knows your default code can open all chests that use it.

## Languages
Set `language` in `config.yml` (restart needed). Bundled: en, zh, hi, es, fr, ar, bn, pt, ru, ur, id, de, ja, tr, vi, ko, it, te, mr, ta, cs, sk.
Files are copied to `plugins/FailChestLock/lang/` so you can edit them or add your own `<code>.yml`. Missing keys fall back to English.

## ItemsAdder
Optional. In `itemsadder.icons` set `namespace:item_id` for any button. Empty = vanilla item. Icons are read when a menu opens.

## Permission
- `failchestlock.bypass` - open and break any locked chest (nobody by default, not even OPs)

Lock data is stored in `plugins/FailChestLock/locks.yml` (code hashes only, never the code itself).
