# FailChestLock

Zámky na chestky s kódem přes GUI – Paper **1.21.11**, Java 21, autor **Failxos**.
Plugin nemá žádné příkazy, vše je v menu.

## Build
- GitHub: nahraj složku na GitHub → záložka **Actions** → *Build* → stáhni artifact `FailChestLock` (jar).
- Lokálně: `mvn clean package` → `target/FailChestLock-1.0.0.jar`

## Ovládání
- **Položení chestky / sudu:** otevře se menu *Nastav kód* (kód zadáš dvakrát). Když menu zavřeš, chestka zůstane odemčená.
- **Cizí hráč:** pravý klik otevře číselník, po správném kódu se chestka otevře.
- **Majitel:** otevírá bez kódu. **Shift + pravý klik prázdnou rukou** otevře menu majitele (změna kódu, důvěryhodní hráči, odebrání zámku).
- **Zamknutí už stojící chestky:** Shift + pravý klik s tripwire hookem v ruce (`lock-tool` v configu).

## ItemsAdder
Volitelné. V `config.yml` v sekci `itemsadder.icons` můžeš ke každému tlačítku (číslice 0-9, smazat, potvrdit, změnit kód...) napsat `namespace:item_id` svého ItemsAdder itemu. Prázdné = vanilla item. Nastavení se projeví po restartu a ikony se čtou při otevření menu.

## Ochrany
Rozbít chestku smí jen majitel, hoppery z ní nic nevytáhnou, výbuchy ji nezničí a nejde ji připojit k cizí zamčené chestce.

## Permission
- `failchestlock.bypass` – otevře/rozbije jakoukoliv zamčenou chestku (defaultně nikdo, ani OP)

Data zámků jsou v `plugins/FailChestLock/locks.yml` (jen hash kódu, ne kód samotný). Vše ostatní v `config.yml`.
