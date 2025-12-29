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
- **Simple 1-tavern-per-world**: First player to create area becomes owner

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
├── TavernKeeperMod.java  # Registration only
├── items/          # MarkingCane, TavernItem, WalletItem
├── areas/          # Types, Renderer, Commands
│   └── client/     # ModeInputHandler
├── compat/         # Mod compatibility layer
│   └── furniture/  # Furniture mod compatibility
│       ├── FurnitureRecognizer.java          # Interface for furniture recognition
│       ├── FurnitureCompatRegistry.java      # Central registry
│       ├── VanillaFurnitureRecognizer.java   # Vanilla stairs support
│       └── MacawsFurnitureRecognizer.java    # Macaw's Furniture support
├── events/         # Event handlers (organized by domain)
│   ├── PlayerInteractionHandler.java    # Player clicks & interactions
│   ├── WorldUpdateHandler.java          # Block place/break, entity spawn
│   ├── TavernLifecycleHandler.java      # Tick, player join, commands
│   ├── TavernUpgradeHandler.java        # Upgrade notifications
│   └── AdvancementHandler.java          # Grant advancements (coins, reputation, money)
├── entities/       # CustomerEntity, AI behaviors
│   └── ai/
│       └── behavior/  # FindSeat, EatAtChair, Leave, etc.
├── tavern/         # Domain logic (DDD architecture)
│   ├── Tavern.java       # Aggregate root with result objects
│   ├── TavernCommand.java # Commands for stats/upgrade info
│   ├── managers/   # All managers organized by type
│   │   ├── domain/    # Domain managers (physical world interaction)
│   │   │   ├── DiningManager, ServiceManager, SleepingManager, CustomerManager
│   │   │   ├── BaseManager, TavernContext
│   │   └── system/    # System managers (meta-game state)
│   │       └── UpgradeManager, AdvancementManager, EconomyManager
│   ├── spaces/     # DiningSpace, ServiceSpace, SleepingSpace
│   ├── furniture/  # Chair, Table, ServiceLectern, ServiceBarrel, Bed
│   │   └── types/  # Furniture type enums (DiningFurnitureType, etc.)
│   ├── economy/    # FoodRequest, Price, CoinRegistry
│   └── upgrades/   # TavernUpgrade (enum), UpgradeDetails, UpgradeFormatter
├── client/         # CustomerEntityRenderer, FoodRequestLayer
└── network/        # NetworkHandler, SyncAreasPacket
```

### 7. **Furniture Recognition**
- **Real-time updates**: Detects furniture when placed/broken
- **Dining Areas**: 
  - Vanilla: Stairs (Chairs) + Upside-down Stairs (Tables)
  - **Macaw's Furniture**: Automatically recognizes chairs, tables, desks, counters, stools (soft dependency)
- **Sleeping Areas**: Beds (with reservation system)
- **Service Areas**: Lecterns (food service) + Reception Desks (sleeping service)
- **Furniture Limits**: Enforced by upgrade level
- **Optimized**: Only updates specific block position, no full rescans
- **Smart validation**: Chairs must face tables and have air block above to be valid
- **Reservation System**: Prevents multiple customers from targeting same chair/bed

### 8. **Customer System** ✨
- **Service Areas**: Lecterns (food ordering) and Reception Desks (sleeping requests)
- **Raid-Style Spawning**: Uses Minecraft's native spawning mechanics
  - Multi-phase spawn attempts (20 per cycle)
  - Circular positioning around tavern center
  - World surface height detection
  - Full spawn validation
- **Customer Lifecycle System**: Three journey types
  - **Dining Only**: Lectern → Food → Chair → Eat → Leave
  - **Sleeping Only**: Reception → Pay → Bed → Sleep → Leave (morning)
  - **Full Service**: Lectern → Food → Chair → Eat → Reception → Pay → Bed → Sleep → Leave
- **Smart AI**: State-based behavior system

### 9. **Tavern Open/Closed State** 🚪
- **Tavern Sign**: Designate any sign as your tavern sign
  - Hold Marking Cane + Right-click sign → Sets as tavern sign
  - Right-click tavern sign (empty hand) → Toggle OPEN/CLOSED
  - Sign automatically shows "OPEN" (green) or "CLOSED" (red)
- **Business Control**: Controls customer spawning
  - OPEN: Customers spawn normally (~30 seconds intervals)
  - CLOSED: No new customers spawn

### 10. **Economy System** 💰
- **Currency**: 5-tier coin system (100:1 conversion rate)
  - Copper → Iron → Gold → Diamond → Netherite
- **Wallet (Coin Purse)**:
  - Auto-collects coins on pickup from ground
  - Auto-converts 100 lower → 1 higher tier
  - Right-click coins to store, right-click empty to extract
  - Intercepts customer payments automatically
- **Payment**: Full coin breakdown (e.g., 232 copper → 2 Iron + 32 Copper)

### 11. **Tavern Upgrades** ⬆️
- **Upgrade Levels** with balanced progression
- **Automatic Upgrades**: System checks after every payment. Upgrade adjusts limits/multipliers and notifies players
- **Commands**: `/tavern upgrade` shows current level and next requirements

### 12. **Reputation System** ⭐
- Earn/lose reputation based on customer service
- Affects upgrade unlocks and future features

### 13. **Advancements** 🏆
- Coin collection (5 tiers), reputation milestones (6 levels), money earned
- Auto-granted during gameplay

### 14. **Tavern Commands** 🔧
- `/tavern stats` - View owner, status, and statistics
- `/tavern upgrade` - View current level and next requirements
- `/tavern adjust` - Manual adjustments (money, reputation) for testing

### 15. **Sleeping System** 🛏️
- **Reception Desk**: Custom block for sleeping service
- **Payment First**: Customers pay at reception before going to bed
- **Sleep Until Morning**: Customers wake up at dawn (6 AM game time)

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
Event Handlers (UI Layer) → Tavern (Service Layer) → Managers → Spaces → Furniture
```

**Key Principles:**
- **UI/Business Separation**: Event handlers route to Tavern, which returns result objects
- **Result Objects Pattern**: `CreationResult`, `DeletionResult` bridge business logic and UI
- **Interface Segregation**: Managers use `TavernContext` interface
- **Bottom-Up Validation**: Managers ask Tavern for permission/state
- **Top-Down Creation**: Tavern creates and owns managers
- **No Duplication**: Commands and items share logic through Tavern API
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
- Tavern statistics (reputation, money earned, customers served, upgrade level)

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

Nothing!

---

## 📊 Technical Reference

### Area Types & Colors

| Type | Color | Purpose | Required Furniture |
|------|-------|---------|-------------------|
| **Dining** | Yellow | Eating area | Tables + Chairs (facing tables) |
| **Sleeping** | Blue | Rest area | Beds |
| **Service** | Green | Reception | Lecterns (food) + Reception Desks (sleeping) |

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
