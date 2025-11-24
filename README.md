# 🍺 TavernKeeper Mod

A Minecraft mod that lets you run your own tavern! Mark areas, serve customers, and earn emeralds.

## Features ✨

Run a fully functional tavern in Minecraft:
- **Mark areas** with a special tool (Dining, Sleeping, Service)
- **Customers spawn automatically** and request food
- **Serve them** and get paid
- **Watch them eat** at your tables before leaving

## Quick Start

1. Get the **Marking Cane** from creative inventory
2. Mark a **Service Area** (place lectern)
3. Mark a **Dining Area** (place tables and chairs - the most basic ones are stairs and upside-dwon stairs)
4. Keep food in your inventory
5. Customers arrive, you serve them (right click with food), profit! 💰

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
