# 🍺 TavernKeeper Mod

A Minecraft mod that lets you run your own tavern! Mark areas, serve customers, and earn emeralds.

## Features ✨

Run a fully functional tavern in Minecraft:
- **Mark areas** with a special tool (Dining, Sleeping, Service)
- **Customers spawn** and come to your tawern to request food
- **Serve them** and get paid
- **Watch them eat** at your tables before leaving

## Mod content

### Setting Up Your Tavern
1. **Select Mode**: Shift + Scroll → "Mode: §eDining Area"
2. **Mark Area**: Right-click → Right-click
3. **Auto-saved**: "Saved Dining Area #3 (250 blocks)"
4. **Place Furniture**:
   - Dining: Place upside-down stairs (tables) + stairs facing tables (chairs)
   - Service: Place lecterns (queue points) - barrels are optional decoration
5. **Set Tavern Sign**: Hold cane → Right-click any sign → Designates tavern sign
6. **Toggle Open/Closed**: Right-click tavern sign (empty hand) → Controls customer spawning
7. **See Areas**: Hold cane → All areas visible (colored boxes)

### Running Your Tavern
1. **Make sure tavern is OPEN** - right-click tavern sign to toggle
2. **Customers spawn automatically** (~30 seconds intervals when OPEN)
3. **Customer walks to lectern** and shows food request (item above head)
4. **You serve them** - right-click customer while holding the requested food
5. **Customer pays** (gives you emerald) and walks to a chair
6. **Customer eats** then leaves and despawns
7. **Toggle CLOSED** when you need a break - no new customers will spawn

## Documentation

- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - Complete documentation (how to use, technical details)
- **[TODO.md](TODO.md)** - Future features and roadmap

## Development

**Requirements:** Minecraft 1.21.1 | NeoForge 21.1.215 | Java 21

```bash
make build    # Compile
make run      # Test in-game
```

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
