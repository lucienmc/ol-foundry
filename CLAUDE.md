# Foundry Mod — Project Context for Claude

## Project Overview
Fabric mod for Minecraft 26.1.2, written in Kotlin.  
Mod ID: `foundry`. Main class: `dev.lucien.foundry.Foundry`.

Custom block: **Foundry** — a blast-furnace-like smelter (extends `AbstractFurnaceBlock`) with:
- Lava fluid tank (4-bucket / 4000 mB capacity) that **doubles** the active fuel's smelting speed
- Fuel-tiered smelting speed: coal/charcoal = 1.5×, blaze rod = 3× (lava doubles each)
- 3 output slots (primary result fills left-to-right)
- 1 byproduct slot (slag)
- 1 lava-bucket input slot (auto-consumes lava buckets into the tank each tick)
- Directional hopper access via `WorldlyContainer` (top = input, sides = fuel + lava, bottom = extract)

---

## Key Files

| File | Purpose |
|------|---------|
| `src/main/kotlin/.../block/FoundryBlock.kt` | `AbstractFurnaceBlock` subclass — LIT state, facing, interaction, particles |
| `src/main/kotlin/.../block/entity/FoundryBlockEntity.kt` | Server-side orchestration — tick, recipe, XP, serialization, `WorldlyContainer` rules |
| `src/main/kotlin/.../block/entity/FoundryState.kt` | Mutable smelting state + its serialization |
| `src/main/kotlin/.../block/entity/FoundryLavaTank.kt` | All fluid logic: storage, bucket consumption, drain, mB conversion, serialization |
| `src/main/kotlin/.../item/FoundryItem.kt` | `BlockItem` that restores stored lava on placement |
| `src/main/kotlin/.../item/LavaStorageComponent.kt` | Typed `DataComponentType` payload (lava mB carried by the item) |
| `src/main/kotlin/.../menu/FoundryMenu.kt` | Container menu — slot registration + ALL layout constants |
| `src/client/kotlin/.../screen/FoundryScreen.kt` | GUI rendering |
| `src/client/kotlin/.../FoundryClient.kt` | Client init — screen registration + lava tooltip (`ItemTooltipCallback`) |
| `src/client/kotlin/.../jei/FoundryRecipeCategory.kt` | JEI category — slots, fuel tooltips, byproduct display |
| `src/client/kotlin/.../jei/FoundryJeiPlugin.kt` | JEI plugin — reads recipes via `recipeAccess().synchronizedRecipes` |
| `src/main/resources/assets/foundry/textures/gui/container/foundry.png` | 256×256 palette-indexed GUI sheet |
| `assets/foundry/textures/gui/sprites/container/foundry/lit_progress.png` | Animated flame sprite |
| `assets/foundry/textures/gui/sprites/container/foundry/burn_progress.png` | Animated arrow sprite |
| `assets/foundry/textures/gui/sprites/container/foundry/lava_fill.png` | 16×128 animated lava fill (8×16×16 frames) |
| `assets/foundry/textures/gui/sprites/container/foundry/lava_fill.png.mcmeta` | Animation metadata |

---

## Block Entity Architecture

The block entity is split into three focused classes:

### `FoundryState` — smelting state
Owns the six mutable smelting fields and their serialization:
```kotlin
var fuelBurnTimeLeft: Int = 0
var maxFuelBurnTime: Int = 0
var smeltProgress: Int = 0
var smeltTotal: Int = DEFAULT_COOK_TIME   // 200 placeholder; set to cookingTime * PROGRESS_RESOLUTION when smelting
var fuelSpeed: Int = 0                     // per-tick progress of the burning fuel (see FoundryBlockEntity)
var storedXp: Float = 0f

val isBurning: Boolean get() = fuelBurnTimeLeft > 0

fun save(output: ValueOutput) { ... }
fun load(input: ValueInput) { ... }
```

### `FoundryLavaTank` — fluid logic
Owns everything fluid-related, including the mB↔droplet conversion (no magic `81`/`4000` elsewhere):
```kotlin
val storage: SingleVariantStorage<FluidVariant>   // exposed to pipes via ModBlockEntities
val hasLava: Boolean
val percent: Int   // 0–100, for ContainerData DATA_LAVA_PERCENT
val mb: Int        // 0–4000 mB, for ContainerData DATA_LAVA_MB

fun fillFromMb(mb: Int)                                    // set tank from a stored-item amount (clamped)
fun tryConsumeBucket(bucketSlot: ItemStack): ItemStack?   // returns empty bucket or null
fun drainForBoost()                                        // call once per boosted tick
fun save(output: ValueOutput)
fun load(input: ValueInput)

companion object {
    const val CAPACITY: Long = FluidConstants.BUCKET * 4
    const val DRAIN_PER_TICK: Long = FluidConstants.BUCKET / 1600
    const val DROPLETS_PER_MB: Long = FluidConstants.BUCKET / 1000   // = 81
    val CAPACITY_MB: Int = (CAPACITY / DROPLETS_PER_MB).toInt()      // = 4000 (used in tooltips)
}
```

### `FoundryBlockEntity` — orchestration
Holds `val lava = FoundryLavaTank { setChanged() }` and `val state = FoundryState()`.  
Implements `ImplementedContainer`, `WorldlyContainer`, `MenuProvider`.  
Owns: inventory, ContainerData, tick logic, recipe matching, XP, fuel-speed tiers, hopper rules.  
Note: constructor parameter is `blockState: BlockState` (not `state`) to avoid shadowing the `state` property.

**Fuel-speed model** (companion constants):
```kotlin
const val PROGRESS_RESOLUTION = 2     // smeltTotal = cookingTime * 2, keeps 1.5× integer
const val PROGRESS_DECAY = 2          // progress lost per tick when not burning
const val BASE_FUEL_SPEED = 3         // coal/charcoal → 1.5×
const val BLAZE_FUEL_SPEED = 6        // blaze rod    → 3×
const val LAVA_SPEED_MULTIPLIER = 2   // lava doubles the active fuel speed
fun fuelSpeedFor(stack): Int          // BLAZE_ROD → 6, else 3
```
`tryStartFuel` records `state.fuelSpeed = fuelSpeedFor(fuel)`; each smelting tick adds
`fuelSpeed * (lava ? 2 : 1)` to `smeltProgress`.

**Recipe matching is cross-side** — always via `level.recipeAccess().synchronizedRecipes`
(`getFirstMatch` / `getAllOfType`), never `getRecipeFor`/`getAllRecipesFor` (those don't exist
on the client `RecipeAccess`). `ModRecipes.init()` registers the serializer with
`RecipeSynchronization.synchronizeRecipeSerializer(...)` so the client has the recipes.

**Hopper access (`WorldlyContainer`)**:
```kotlin
getSlotsForFace: UP → INPUT_SLOT, DOWN → EXTRACT_SLOTS, else → FUEL_SLOT + LAVA_BUCKET_SLOT
canPlaceItemThroughFace: INPUT → isSmeltable; FUEL → not a lava bucket && not smeltable;
                         LAVA_BUCKET → is lava bucket
canTakeItemThroughFace:  index in EXTRACT_SLOTS
```

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

// Slot groups (single source of truth for face access + insertion loops)
val RESULT_SLOTS  = intArrayOf(OUTPUT_SLOT, OUTPUT_SLOT_2, OUTPUT_SLOT_3)
val EXTRACT_SLOTS = intArrayOf(OUTPUT_SLOT, OUTPUT_SLOT_2, OUTPUT_SLOT_3, BYPRODUCT_SLOT)

// ContainerData indices — shared with FoundryMenu's accessors (no magic 0..5)
const val DATA_SMELT_PROGRESS = 0
const val DATA_SMELT_TOTAL    = 1
const val DATA_FUEL_LEFT      = 2
const val DATA_FUEL_MAX       = 3
const val DATA_LAVA_PERCENT   = 4
const val DATA_LAVA_MB        = 5
const val DATA_COUNT          = 6
```

Insertion is unified through `insertInto(slotIndex, item)` (built on `canFitInSlot`); `addToResultSlots`
and the byproduct path both use it.

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

Indices are the `DATA_*` constants above — both the entity's `ContainerData` and `FoundryMenu`'s
accessors reference them, so they can't drift. Values sync as **shorts**, hence the `Short.MAX_VALUE`
caps on the fuel fields.

| Index | Const | Value | Source |
|-------|-------|-------|--------|
| 0 | `DATA_SMELT_PROGRESS` | `smeltProgress` | `state.smeltProgress` |
| 1 | `DATA_SMELT_TOTAL` | `smeltTotal` | `state.smeltTotal` |
| 2 | `DATA_FUEL_LEFT` | `fuelBurnTimeLeft` (capped) | `state.fuelBurnTimeLeft` |
| 3 | `DATA_FUEL_MAX` | `maxFuelBurnTime` (capped) | `state.maxFuelBurnTime` |
| 4 | `DATA_LAVA_PERCENT` | lava % (0–100) | `lava.percent` |
| 5 | `DATA_LAVA_MB` | lava in mB (0–4000) | `lava.mb` |

Indices 4 & 5 are server-derived (read-only on the client).

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
- **Smelting speed = fuel tier × lava**: coal/charcoal 1.5×, blaze rod 3×; lava in the tank doubles
  the active fuel speed (and drains `BUCKET/1600` per boosted tick). Tracked at `PROGRESS_RESOLUTION = 2`
  so the 1.5× stays integer. Fuel type also still affects burn *duration* (vanilla `fuelValues()`; slag = 800).
- **Byproduct (slag)**: `byproductChance` field — floor = guaranteed count, fraction = roll for +1 extra
  (supports `>1` for bulk recipes like raw-ore-blocks).
- **Extends `AbstractFurnaceBlock`**: gives FACING + LIT blockstate, GUI open via `openContainer`, and
  the recipe-book plumbing for free. `FoundryBlockEntity.serverTick` keeps `LIT` in sync with `isBurning`.
  `ModBlocks` still sets `.lightLevel { if (LIT) 13 else 0 }` (NOT automatic). `animateTick` adds the
  blast-furnace crackle, top smoke, and flame/smoke particles on the front face (offset by `FACING`).
  `blockstates/foundry.json` maps all 4 facings × `lit=false/true`; `lit=true` uses the `foundry_lit`
  model (overrides only the front-face texture), rotated by `y` per facing.
- **Stored lava survives item form**: `LavaStorageComponent` (typed `DataComponentType`, registered in
  `ModDataComponents`) carries mB. `FoundryItem.place` restores it via `lava.fillFromMb(...)`;
  creative break drops the item with the component (`playerWillDestroy`). Tooltip rendered client-side via
  `ItemTooltipCallback` in `FoundryClient` (NOT the deprecated `appendHoverText`).
- **Cross-side recipe access**: always `level.recipeAccess().synchronizedRecipes` (`getFirstMatch` /
  `getAllOfType`). `getRecipeFor`/`getAllRecipesFor` do NOT exist on the client `RecipeAccess` —
  do not use them. `isSmeltable(level, stack)` in the companion is the single source of truth
  (FoundryMenu delegates to it).
- **`blockState` constructor param**: Named `blockState` (not `state`) to avoid shadowing the `val state: FoundryState` property.
- **`level.server` on `ServerLevel`**: Non-null at runtime despite Java `@Nullable` on base `Level.getServer()`. Plain `.` call is correct; Sonar false-positive suppressed with `// NOSONAR` if needed.
- **JEI** (`FoundryJeiPlugin` / `FoundryRecipeCategory`): recipes read from
  `recipeAccess().synchronizedRecipes.getAllOfType(FOUNDRY_RECIPE_TYPE)` — single source of truth is JSON.
  Slag is added as an **output slot** so left-clicking it in JEI lists every byproduct recipe; the slot
  shows the guaranteed count with a `+x% chance for one more` tooltip. Category icon is the Foundry block.
