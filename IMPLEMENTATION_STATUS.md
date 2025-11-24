# TavernKeeper Mod - Complete Documentation

> **Quick Links:** [README](README.md) · [TODO](TODO.md)

---

## ✅ Completed Features

### 1. **Marking Cane Interactions**
- **Shift + Scroll**: Change mode (Dining/Sleeping)
- **Right-click**: Set corners → Auto-saves on 2nd click
- **Left-click**: Clear selection OR mark area for deletion
- **Two-step delete**: Click area twice (red warning, cancel with right-click)
- Tooltip shows all controls

### 2. **Visual System**
- **Live preview**: Box follows cursor after 1st corner
- **Dynamic colors**: Box matches selected mode (Yellow/Blue)
- **All areas visible**: When holding cane, see all saved areas
- **Deletion warning**: Pending deletion shows as RED
- Uses Minecraft's built-in `hitResult` (clean!)

### 3. **Network Sync**
- **Server→Client**: All players see area changes instantly
- **On join**: Player receives all areas
- **On change**: Area creation/deletion syncs to all
- Uses native NeoForge networking with CustomPacketPayload

### 4. **Auto-Numbering**
- Ever-incrementing counters per type ("#1", "#2", "#3")
- Never reuses numbers (even after deletion)
- Persists in world NBT data

### 5. **Smart Deletion**
- Can't delete while actively selecting area
- Right-click cancels pending deletion
- Visual feedback (red box) before confirming
- Safer than immediate delete

### 6. **Package Structure**
```
tavernkeeper/
├── items/          # MarkingCane, TavernItem
├── areas/          # Types, Manager, Renderer, Commands
│   └── client/     # ModeInputHandler
├── entities/       # CustomerEntity, AI behaviors
│   └── ai/
│       └── behavior/  # FindSeat, EatAtChair, Leave, etc.
├── tavern/         # Domain logic (DDD architecture)
│   ├── managers/   # DiningManager, ServiceManager, CustomerManager
│   ├── spaces/     # DiningSpace, ServiceSpace, SleepingSpace
│   └── furniture/  # Chair, Table, Lectern, Barrel, Bed
├── client/         # CustomerEntityRenderer, FoodRequestLayer
└── network/        # NetworkHandler, SyncAreasPacket
```

### 7. **Furniture Recognition**
- **Real-time updates**: Detects furniture when placed/broken
- **Dining Areas**: Stairs (Chairs) + Upside-down Stairs (Tables)
- **Sleeping Areas**: Beds
- **Service Areas**: Lecterns (Queue points) + Barrels (recognized but not used yet)
- **Optimized**: Only updates specific block position, no full rescans
- **Smart validation**: Chairs must face tables to be valid

### 8. **Customer System** ✨
- **Service Areas**: Reception desks with lecterns (barrels optional decoration)
- **Raid-Style Spawning**: Uses Minecraft's native spawning mechanics
  - Multi-phase spawn attempts (20 per cycle)
  - Circular positioning around tavern center
  - World surface height detection
  - Full spawn validation
- **Customer AI Lifecycle**:
  1. Spawn near tavern
  2. Walk to lectern and queue
  3. Show food request (item above head + emerald in hand)
  4. Player serves requested food (right-click with food in hand)
  5. Customer pays (gives emerald)
  6. Walk to available chair
  7. Sit and eat food
  8. Leave and despawn

---

## 📖 How to Use

### Getting Started

**Step 1: Get the Marking Cane**
- Open creative inventory → "Tavern Keeper" tab
- Grab the Marking Cane

**Step 2: Mark Areas**
- Hold **Shift + Scroll** to change mode (Dining/Sleeping/Service)
- **Right-click** first corner → preview box appears
- **Right-click** second corner → auto-saves!

**Step 3: Place Furniture**
- **Dining Areas**: Upside-down stairs (tables) + stairs facing tables (chairs)
- **Service Areas**: Lecterns (required) + barrels (optional)
- **Sleeping Areas**: Beds

**Step 4: Delete Areas (if needed)**
- **Left-click** area → turns red
- **Left-click again** → deleted (or right-click to cancel)

### Setting Up Your Tavern
1. **Select Mode**: Shift + Scroll → "Mode: §eDining Area"
2. **Mark Area**: Right-click → Right-click
3. **Auto-saved**: "Saved Dining Area #3 (250 blocks)"
4. **Place Furniture**:
   - Dining: Place upside-down stairs (tables) + stairs facing tables (chairs)
   - Service: Place lecterns (queue points) - barrels are optional decoration
5. **See Areas**: Hold cane → All areas visible (colored boxes)
6. **Get food**: Have food items in your inventory (carrots, bread, etc.)

### Running Your Tavern
1. **Customers spawn automatically** (~30 seconds intervals)
2. **Customer walks to lectern** and shows food request (item above head)
3. **You serve them** - right-click customer while holding the requested food
4. **Customer pays** (gives you emerald) and walks to a chair
5. **Customer eats** then leaves and despawns
6. **Repeat!** New customers keep coming

### Example Workflow

```
1. Hold Shift + Scroll → "Mode: §eDining Area"

2. Right-click floor
   → "First position set"
   → Yellow preview box follows cursor

3. Right-click opposite corner to save

4. To delete: Left-click area twice
```

---

## 🏗️ Architecture & Design

### Clean Layered Design (DDD)

```
TavernKeeperMod
  ↓
Tavern (Aggregate Root)
  ↓
Managers (DiningManager, ServiceManager, CustomerManager)
  ↓
Spaces (DiningSpace, ServiceSpace, SleepingSpace)
  ↓
Furniture (Chair, Table, Lectern, Barrel, Bed)
```

**Key Principles:**
- **Interface Segregation**: Managers use `TavernContext` interface
- **Bottom-Up Validation**: Managers ask Tavern for permission/state
- **Top-Down Creation**: Tavern creates and owns managers
- **No Circular Dependencies**: Clean, maintainable code

### Spawn System Details

Uses exact logic from Minecraft's Raid system (`Raid.java` lines 686-706):
- Random circular spawning using angle math (cos/sin * 2π)
- 32-block radius from tavern center (random lectern)
- Random ±5 block offset
- World surface height detection via `Heightmap.Types.WORLD_SURFACE`
- Chunk loading validation (`level.isLoaded`)
- Entity ticking check (`level.isPositionEntityTicking`)
- Spawn placement validation (same as Ravager spawning)

### Persistence Strategy

**What Gets Saved:**
- Area definitions (positions, types, names)
- Area counters (auto-numbering)
- Customer manager settings (capacity, spawn intervals)

**What Doesn't Get Saved:**
- Customer entity UUIDs (entities persist themselves via Minecraft)
- Spawn cooldowns (reset to 0 on load = fresh spawn)
- Cached spawn positions (recalculated on demand)

**Why This Pattern:**
- Separates configuration (persistent) from runtime state (ephemeral)
- Prevents stale UUID tracking
- Simpler and more robust
- Entities already persist through Minecraft's chunk system

---

## 🐛 Known Issues

None! All major features implemented and working.

---

## 📊 Technical Reference

### Area Types & Colors

| Type | Color | Purpose | Required Furniture |
|------|-------|---------|-------------------|
| **Dining** | Yellow | Eating area | Tables + Chairs (facing tables) |
| **Sleeping** | Blue | Rest area | Beds |
| **Service** | Green | Reception | Lecterns (barrels optional) |

### Counter System
```java
// Per-type counters stored in NBT
DINING: counter = 5    // Next will be #6
SLEEPING: counter = 2  // Next will be #3
```

### Deletion Behavior
```
Create: Dining #1, #2, #3
Delete: Dining #2
Create new: Dining #4  (NOT #2, counter keeps incrementing)
```

### Mode State
- Stored per-player in memory
- Default mode: DINING
- Persists during session
- Reset on server restart

### Data Storage
- **Location**: `<world>/data/tavernkeeper_tavern.dat` and `tavernkeeper_areas.dat`
- **Format**: NBT (Named Binary Tags)
- **System**: Minecraft's `SavedData`
- **Auto-saves**: On area changes and world save

### Network Sync
- **Server→Client**: All players see changes instantly
- **On join**: Player receives all areas
- **Protocol**: NeoForge's `CustomPacketPayload`

---

## 📝 Notes

- Commands work as fallback/debug tool
- Primary workflow is GUI-based (Shift+Scroll, auto-save)
- Server-side logic only (no client-side cheating)
