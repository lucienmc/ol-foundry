# Foundry Mod — Project Context for Claude

## Project Overview
Fabric mod for Minecraft 26.1.2, written in Kotlin.  
Mod ID: `foundry`. Main class: `dev.lucien.foundry.Foundry`.

Custom block: **Foundry** — a blast-furnace-like smelter with:
- Lava fluid tank (4-bucket capacity) that boosts smelting speed 4× (vs 2× without lava)
- 3 output slots (primary result fills left-to-right)
- 1 byproduct slot (slag)
- 1 lava-bucket input slot (auto-consumes lava buckets into the tank each tick)

---

## Key Files

| File | Purpose |
|------|---------|
| `src/main/kotlin/.../block/entity/FoundryBlockEntity.kt` | Server-side orchestration — tick, recipe, XP, serialization |
| `src/main/kotlin/.../block/entity/FoundryState.kt` | Mutable smelting state + its serialization |
| `src/main/kotlin/.../block/entity/FoundryLavaTank.kt` | All fluid logic: storage, bucket consumption, drain, serialization |
| `src/main/kotlin/.../menu/FoundryMenu.kt` | Container menu — slot registration + ALL layout constants |
| `src/client/kotlin/.../screen/FoundryScreen.kt` | GUI rendering |
| `src/main/resources/assets/foundry/textures/gui/container/foundry.png` | 256×256 palette-indexed GUI sheet |
| `assets/foundry/textures/gui/sprites/container/foundry/lit_progress.png` | Animated flame sprite |
| `assets/foundry/textures/gui/sprites/container/foundry/burn_progress.png` | Animated arrow sprite |
| `assets/foundry/textures/gui/sprites/container/foundry/lava_fill.png` | 16×128 animated lava fill (8×16×16 frames) |
| `assets/foundry/textures/gui/sprites/container/foundry/lava_fill.png.mcmeta` | Animation metadata |

---

## Block Entity Architecture

The block entity is split into three focused classes:

### `FoundryState` — smelting state
Owns the five mutable smelting fields and their serialization:
```kotlin
var fuelBurnTimeLeft: Int = 0
var maxFuelBurnTime: Int = 0
var smeltProgress: Int = 0
var smeltTotal: Int = DEFAULT_COOK_TIME   // 200
var storedXp: Float = 0f

val isBurning: Boolean get() = fuelBurnTimeLeft > 0

fun save(output: ValueOutput) { ... }
fun load(input: ValueInput) { ... }
```

### `FoundryLavaTank` — fluid logic
Owns everything fluid-related:
```kotlin
val storage: SingleVariantStorage<FluidVariant>   // exposed to pipes via ModBlockEntities
val hasLava: Boolean
val percent: Int   // 0–100, for ContainerData slot 4
val mb: Int        // 0–4000 mB, for ContainerData slot 5

fun tryConsumeBucket(bucketSlot: ItemStack): ItemStack?   // returns empty bucket or null
fun drainForBoost()                                        // call once per boosted tick
fun save(output: ValueOutput)
fun load(input: ValueInput)

companion object {
    val CAPACITY: Long = FluidConstants.BUCKET * 4
    val DRAIN_PER_TICK: Long = FluidConstants.BUCKET / 1600
}
```

### `FoundryBlockEntity` — orchestration
Holds `val lava = FoundryLavaTank { setChanged() }` and `val state = FoundryState()`.  
Owns: inventory, ContainerData, tick logic, recipe matching, XP, MenuProvider boilerplate.  
Note: constructor parameter is `blockState: BlockState` (not `state`) to avoid shadowing the `state` property.

Pipe access wired in `ModBlockEntities`:
```kotlin
FluidStorage.SIDED.registerForBlockEntity({ entity, _ -> entity.lava.storage }, FOUNDRY)
```

---

## Inventory Slot Constants (`FoundryBlockEntity.Companion`)

```kotlin
const val INPUT_SLOT        = 0
const val FUEL_SLOT         = 1
const val OUTPUT_SLOT       = 2   // primary result
const val OUTPUT_SLOT_2     = 3   // bonus / overflow
const val OUTPUT_SLOT_3     = 4   // overflow
const val BYPRODUCT_SLOT    = 5   // slag
const val LAVA_BUCKET_SLOT  = 6
const val INVENTORY_SIZE    = 7
```

---

## GUI Layout Constants (`FoundryMenu.Companion`) — Single Source of Truth

All coordinates are **top-left of the 18×18 slot background** (GUI-relative).  
`addSlot` uses `CONSTANT + 1` for the inner 16×16 item area.  
**The Screen reads these same constants** — change once, both slot hitbox and visual outline update.

```kotlin
// Slots
const val INPUT_X = 25;        const val INPUT_Y = 16
const val FUEL_X = 25;         const val FUEL_Y = 52
const val OUTPUT1_X = 80;      const val OUTPUT1_Y = 24
const val OUTPUT2_X = 98;      const val OUTPUT2_Y = 24
const val OUTPUT3_X = 116;     const val OUTPUT3_Y = 24
const val BYPRODUCT_X = 98;    const val BYPRODUCT_Y = 44
const val LAVA_BUCKET_X = 151; const val LAVA_BUCKET_Y = 59
const val PLAYER_INV_X = 8;    const val PLAYER_INV_Y = 84

// Animated flame indicator
const val FLAME_X = 26;  const val FLAME_Y = 36
const val FLAME_W = 13;  const val FLAME_H = 14

// Animated progress arrow
const val ARROW_X = 50;  const val ARROW_Y = 35
const val ARROW_W = 22;  const val ARROW_H = 15

// Lava gauge border (interior = BAR_X+1, BAR_Y+1, BAR_W-2, BAR_H-2)
const val BAR_X = 152;  const val BAR_Y = 7
const val BAR_W = 16;   const val BAR_H = 50

// Slot iteration lists (used by Screen's outline loop and Menu's output loop)
val OUTPUT_SLOTS: List<Triple<Int, Int, Int>>   // (slotIndex, x, y)
val ALL_SLOT_POSITIONS: List<Pair<Int, Int>>     // (x, y) for every slot
```

---

## ContainerData Slots (synced to client)

| Index | Value | Source |
|-------|-------|--------|
| 0 | `smeltProgress` | `state.smeltProgress` |
| 1 | `smeltTotal` | `state.smeltTotal` |
| 2 | `fuelBurnTimeLeft` (capped at Short.MAX_VALUE) | `state.fuelBurnTimeLeft` |
| 3 | `maxFuelBurnTime` (capped at Short.MAX_VALUE) | `state.maxFuelBurnTime` |
| 4 | lava % (0–100) | `lava.percent` |
| 5 | lava in mB (0–4000) | `lava.mb` |

---

## GUI Rendering Notes (`FoundryScreen`)

- `extractBackground()` renders: foundry.png sheet → slot outlines → animated flame → animated arrow → lava fill
- **Slot outlines** rendered via loop over `FoundryMenu.ALL_SLOT_POSITIONS` using vanilla `minecraft:container/slot` sprite
- **Lava fill** tiled vertically using `renderLavaTile()` with the `lava_fill` sprite (16-px frame height)
- **Sprites** are all mod-namespaced:
  ```kotlin
  LIT_PROGRESS_SPRITE  = foundry:container/foundry/lit_progress
  BURN_PROGRESS_SPRITE = foundry:container/foundry/burn_progress
  LAVA_FILL            = foundry:container/foundry/lava_fill
  SLOT_SPRITE          = minecraft:container/slot
  ```
- `extractRenderState()` shows lava mB tooltip when hovering the gauge

---

## foundry.png Texture

- 256×256, **palette-indexed PNG** (mode=P, color_type=3)
- Palette (indices 0–5):
  - 0 = black `#000000`
  - 1 = white `#FFFFFF`
  - 2 = bg gray `#C6C6C6` (main background fill)
  - 3 = `#555555`
  - 4 = `#373737` (dark border)
  - 5 = `#8B8B8B` (slot gray / outlines)

---

## Design Decisions

- **No vanilla texture borrowing**: All static GUI elements owned by this mod's `foundry.png`.
- **F3+T only reloads textures**: Kotlin code changes require full `./gradlew runClient` rebuild.
- **`addSlot` offset convention**: Menu constants = outer 18×18 corner; slots registered at `+1` for inner 16×16.
- **Lava boost**: 4× speed when lava present (drains `BUCKET/1600` per tick); 2× without lava.
- **Byproduct (slag)**: `byproductChance` field — floor = guaranteed count, fraction = roll for +1 extra.
- **`blockState` constructor param**: Named `blockState` (not `state`) to avoid shadowing the `val state: FoundryState` property.
- **`level.server` on `ServerLevel`**: Non-null at runtime despite Java `@Nullable` on base `Level.getServer()`. Plain `.` call is correct; Sonar false-positive suppressed with `// NOSONAR` if needed.
- **JEI recipe source**: Reads directly from `RecipeManager.getAllRecipesFor(ModRecipes.FOUNDRY_RECIPE_TYPE)` — single source of truth is JSON, no hardcoded mirror list.
