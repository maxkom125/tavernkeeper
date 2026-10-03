# 🍺 TavernKeeper Mod

A Minecraft mod that lets you run your own tavern! Mark areas, serve customers, and earn coins!

## Features ✨

Run a fully functional tavern in Minecraft:
- **Mark areas** with a special tool (Dining, Sleeping, Service)
- **Customers spawn** and come to your tavern to request food or a shelter for the night
- **Serve them** and get paid
- **Watch them eat** at your tables or sleep in beds before leaving

## Mod content

### Setting Up Your Tavern
1. **Select Mode**: Shift + Scroll → "Mode: §eDining Area"
2. **Mark Area**: Right-click → Right-click
3. **Auto-saved**: "Saved Dining Area #3 (250 blocks)"
4. **Place Furniture**:
   - Dining: 
     - Vanilla: Upside-down stairs (tables) + stairs facing tables (chairs)
     - Or use furniture from supported mods (Macaw's Furniture, Another Furniture Mod)
   - Sleeping: Place beds for customers to sleep overnight
   - Service: Place lecterns (food orders) + reception desks (room bookings)
5. **Set Tavern Sign**: Hold cane → Right-click any sign → Designates tavern sign
6. **Toggle Open/Closed**: Right-click tavern sign (empty hand) → Controls customer spawning
7. **See Areas**: Hold cane → All areas visible (colored boxes)

### Running Your Tavern
1. **Make sure tavern is OPEN** - right-click tavern sign to toggle
2. **Customers spawn automatically** (~30 seconds intervals when OPEN)
3. **Customer walks to lectern** and shows food request (item above head)
4. **You serve them** - right-click customer while holding the requested food
5. **Customer pays** (gives you coins) and walks to a chair
6. **Customer eats** then leaves and despawns
7. **Toggle CLOSED** when you need a break - no new customers will spawn

### Custom Economy
- **5-tier coins**: Copper → Iron → Gold → Diamond → Netherite (100:1 conversion)
- **Wallet**: Auto-collects coins, auto-converts to higher tiers
- **Dynamic pricing**: better food = higher pay
- *Coming soon: Dish of the Week bonuses, demand tweaks*

### Progression System
- **Tavern Upgrades**: 3 levels with automatic unlocks (reputation + money)
- **Advancements**: Coin collection, reputation, and earnings milestones

## Documentation

- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - Complete documentation (how to use, technical details)
- **[TODO.md](TODO.md)** - Future features and roadmap

## Development

**Requirements:** Minecraft 1.21.1 | NeoForge 21.1.215 | Java 21

```bash
make build    # Compile
make run      # Test in-game
make test     # Run all GameTests headless (same as CI)
```

### Testing
Behaviour is tested with Minecraft's **GameTest** framework: each test runs on a real (headless) server,
in a real structure, with real customers walking, pathfinding and ticking their AI. Tests live in
`src/main/java/maxitoson/tavernkeeper/gametest/` and run in CI after every push.

- **Run:** `make test` (CI does the same). Or in a dev client (`make run`): `/test runall`,
  `/test runthis` (test you're looking at), `/test runfailed`. Note: tests reset the tavern of the world they run in.
- **Write a test:** a `public static void` method taking `GameTestHelper`, annotated
  `@GameTest(template = FLAT_15, batch = "unique_name")` in a class with `@GameTestHolder(TavernKeeperMod.MODID)`
  and `@PrefixGameTestTemplate(false)`. Positions are relative to the structure; the floor is `y=0`, stand at `y=1`.
- **Isolation (important):** the `Tavern` is shared by the whole level, and tests inside one batch run at the same time.
  So any test touching the tavern gets its **own batch** and starts with `TavernTestSupport.freshTavern(helper)`
  (new empty tavern, closed so no random customers spawn).
- **Build the scene in code** with `TavernTestSupport` helpers (`placeTableWithChair`, `placeBed`, `diningArea`, `spawnSeatSeeker`, `upgradeTo`, ...),
  on the flat templates (`FLAT_7`, `FLAT_15`). For a new template add it to `TestStructureProvider` and run `./gradlew runData`.
- **Assert outcomes, not timings:** prefer `helper.succeedWhen(...)` (retried every tick until it passes or times out) and
  `helper.startSequence().thenWaitUntil(...).thenExecute(...)` over fixed `runAfterDelay` checks.
  Keep `timeoutTicks` close to what the scenario needs.
- **Movement tests** should check *how* the customer got there, not just the final state: use `WalkTracker`
  (fails on getting stuck or big detours) and assert where the customer stood when it arrived.
- **Players:** `helper.makeMockPlayer(GameType.SURVIVAL)` gives a player to serve customers with.
- **Bug fixes:** add a test that fails without the fix (see the "Regression:" notes in existing tests).

### Architecture
Built using Domain-Driven Design (DDD) with clean layered architecture:
- **Tavern** (Aggregate Root) → **Managers** → **Spaces** → (**Areas** and **Furniture**)
- Interface Segregation Principle for clean dependencies
- Server-side logic with client-side visualization

## Links

- [NeoForge Docs](https://docs.neoforged.net/)
- [NeoForge Discord](https://discord.neoforged.net/)

## Licences

- [MIT License](./LICENSE.txt)
- [Mojang Mapping License](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md)
