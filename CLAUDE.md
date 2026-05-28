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
| `src/main/kotlin/.../block/entity/FoundryBlockEntity.kt` | Server-side logic, fluid tank, inventory, tick |
| `src/main/kotlin/.../menu/FoundryMenu.kt` | Container menu — slot registration + ALL layout constants |
| `src/client/kotlin/.../screen/FoundryScreen.kt` | GUI rendering |
| `src/main/resources/assets/foundry/textures/gui/container/foundry.png` | 256×256 palette-indexed GUI sheet |
| `assets/foundry/textures/gui/sprites/container/foundry/lit_progress.png` | Animated flame sprite (14×14 currently, needs frames) |
| `assets/foundry/textures/gui/sprites/container/foundry/burn_progress.png` | Animated arrow sprite |
| `assets/foundry/textures/gui/sprites/container/foundry/lava_fill.png` | 16×128 animated lava fill (8×16×16 frames) |
| `assets/foundry/textures/gui/sprites/container/foundry/lava_fill.png.mcmeta` | Animation metadata |

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

val LAVA_CAPACITY: Long = FluidConstants.BUCKET * 4
val LAVA_DRAIN_PER_TICK: Long = FluidConstants.BUCKET / 1600
const val DEFAULT_COOK_TIME = 200
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
```

---

## ContainerData Slots (synced to client)

| Index | Value |
|-------|-------|
| 0 | `smeltProgress` |
| 1 | `smeltTotal` |
| 2 | `fuelBurnTimeLeft` (capped at Short.MAX_VALUE) |
| 3 | `maxFuelBurnTime` (capped at Short.MAX_VALUE) |
| 4 | lava % (0–100) |
| 5 | lava in mB (0–4000) |

---

## GUI Rendering Notes (`FoundryScreen`)

- `extractBackground()` renders: foundry.png sheet → animated flame → animated arrow → lava fill
- **Lava fill** is tiled vertically using `renderLavaTile()` with the `lava_fill` sprite (16-px frame height)
- **Sprites** are all mod-namespaced (NOT vanilla):
  ```kotlin
  LIT_PROGRESS_SPRITE  = foundry:container/foundry/lit_progress
  BURN_PROGRESS_SPRITE = foundry:container/foundry/burn_progress
  LAVA_FILL            = foundry:container/foundry/lava_fill
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
- Container area rows 3–81, cols 3–173 — all static GUI elements are baked here

---

## Pending Tasks

### 1. Bake static elements into foundry.png (Python script needed)
Elements to draw directly onto foundry.png so they show without the machine being active:
- **Lava gauge border**: rect at `(BAR_X=152, BAR_Y=7)`, size `16×50`; border=palette 4, interior=dark (palette 0 or new dark entry)
- **Flame outline** (the "off" state background): rect at `(FLAME_X=26, FLAME_Y=36)`, size `13×14`; color=palette 5
- **Arrow outline** (the "off" state background): rect at `(ARROW_X=50, ARROW_Y=35)`, size `22×15`; color=palette 5

### 2. Output slots → array + loop in `FoundryMenu.kt`
Currently 3 manual `addSlot` calls. Refactor to:
```kotlin
companion object {
    val OUTPUT_SLOTS = listOf(
        Triple(FoundryBlockEntity.OUTPUT_SLOT,   OUTPUT1_X, OUTPUT1_Y),
        Triple(FoundryBlockEntity.OUTPUT_SLOT_2, OUTPUT2_X, OUTPUT2_Y),
        Triple(FoundryBlockEntity.OUTPUT_SLOT_3, OUTPUT3_X, OUTPUT3_Y),
    )
    // All slot positions for the Screen's outline-rendering loop
    val ALL_SLOT_POSITIONS = listOf(
        Pair(INPUT_X, INPUT_Y), Pair(FUEL_X, FUEL_Y),
        Pair(OUTPUT1_X, OUTPUT1_Y), Pair(OUTPUT2_X, OUTPUT2_Y), Pair(OUTPUT3_X, OUTPUT3_Y),
        Pair(BYPRODUCT_X, BYPRODUCT_Y), Pair(LAVA_BUCKET_X, LAVA_BUCKET_Y),
    )
}
```
Then replace the 3 `addSlot` blocks with:
```kotlin
for ((slotIdx, x, y) in OUTPUT_SLOTS) {
    addSlot(object : Slot(container, slotIdx, x + 1, y + 1) {
        override fun mayPlace(stack: ItemStack) = false
        override fun onTake(player: Player, stack: ItemStack) {
            if (!player.level().isClientSide) foundry?.popExperience(player.level() as ServerLevel)
            super.onTake(player, stack)
        }
    })
}
```

### 3. Slot outline rendering loop in `FoundryScreen.kt`
Add to `extractBackground()` (after blitting foundry.png, before animated sprites):
```kotlin
val SLOT_SPRITE = Identifier.fromNamespaceAndPath("minecraft", "container/slot")
for ((sx, sy) in FoundryMenu.ALL_SLOT_POSITIONS) {
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + sx, yo + sy, 18, 18)
}
```
Also **remove** the stray self-referential import:
```kotlin
// DELETE this line:
import dev.lucien.foundry.screen.FoundryScreen.Companion.LAVA_FILL
```

### 4. Verify/improve sprite PNGs
`lit_progress.png` (14×14) and `burn_progress.png` exist but may need to be proper animated sprites.  
Check if `.mcmeta` files are needed for them.

---

## Design Decisions

- **No vanilla texture borrowing**: All static GUI elements owned by this mod's `foundry.png`. Not dependent on positional offsets in `blast_furnace.png` or similar.
- **F3+T only reloads textures**: Kotlin code changes require full `./gradlew runClient` rebuild.
- **`addSlot` offset convention**: Menu constants = outer 18×18 corner; slots registered at `+1` for inner 16×16.
- **Lava boost**: 4× speed when lava present (drains `BUCKET/1600` per tick); 2× without lava.
- **Byproduct (slag)**: `byproductChance` field — floor = guaranteed count, fraction = roll for +1 extra.
