# Foundry Mod — Project Context for Claude

## Project Overview
Fabric mod for Minecraft 26.1.2, written in Kotlin. Mod ID `foundry`; main class `dev.lucien.foundry.Foundry`.

**Foundry** — a blast-furnace-like smelter (`AbstractFurnaceBlock` subclass):
- Lava tank (default 4 buckets / 4000 mB) that **doubles** the active fuel's smelting speed and drains slowly while boosting.
- Fuel-tiered speed (defaults): any fuel 1×, coal/charcoal 1.5×, magma cream 2×, blaze rod 3×.
- 3 output slots (primary fills left-to-right) + 1 byproduct (slag) slot + 1 lava-bucket input slot.
- Directional hopper/pipe access (`WorldlyContainer`): top = input, sides = fuel + lava, bottom = extract.
- All speed multipliers, tank capacity, and slag/magma-cream burn times are configurable.

---

## Conventions (apply to every change)

Before finishing any code change, run through this checklist:

1. **Update `CHANGELOG.md`.** Add an entry under the current unreleased version for any user-facing
   change (new behavior, recipe, item, interaction, balance tweak, bugfix). Use the existing
   [Keep a Changelog](https://keepachangelog.com/) sections (`Added` / `Changed` / `Fixed` /
   `Removed`). Skip only for pure internal refactors with no observable effect.
2. **Keep `CLAUDE.md` in sync.** When the architecture, public APIs, config fields, or design
   decisions change, update the relevant section here in the same change. Document the *why* and the
   pitfalls — don't mirror constants/signatures that already live in code (that just drifts).
3. **No useless comments.** Don't add comments that merely restate well-named code
   (`// shrink the stack` over `stack.shrink(1)`). Comments earn their place by explaining *why* —
   a non-obvious decision, a pitfall, or a cross-version gotcha. Delete stale or redundant ones as
   you touch surrounding code.
4. **`CHANGELOG.md` is the release notes — not the PR body.** The release workflow publishes the
   `## [<mod_version>]` section of `CHANGELOG.md` verbatim as the Modrinth changelog, so keep that
   section clean and player-facing (no file/class names or internal jargon). PR descriptions are for
   **reviewers** — explain what changed and why, infra detail welcome.
5. **Bump `mod_version` in `gradle.properties`** on any PR that ships a user-facing change (patch for
   fixes/small features), and give that version its own `CHANGELOG.md` section. Merging into `main`
   pushes to the release workflow (`.github/workflows/release.yml`), which tags `v<mod_version>`,
   creates a GitHub release, and publishes to Modrinth **only if that version isn't already released**
   — so a merge without a version bump is a safe no-op.

---

## Key Files

| File | Purpose |
|------|---------|
| `block/FoundryBlock.kt` | `AbstractFurnaceBlock` subclass — LIT state, facing, interaction, particles |
| `block/entity/FoundryBlockEntity.kt` | Server-side orchestration — tick, recipe, XP, serialization, `WorldlyContainer` + slot/`DATA_*` constants |
| `block/entity/FoundryState.kt` | Mutable smelting state + its serialization |
| `block/entity/FoundryLavaTank.kt` | All fluid logic: storage, bucket consumption, drain, mB conversion, serialization |
| `config/FoundryConfig.kt` | `data class` of tunable values + `sanitized()` validation/defaults |
| `config/FoundryConfigManager.kt` | Gson load/save of `config/foundry.json`; holds the live `config` singleton |
| `config/client/FoundryConfigScreen.kt` | Builds the YACL options screen (loaded only when YACL is present) |
| `config/client/FoundryModMenu.kt` | `ModMenuApi` entrypoint — opens the YACL screen, no-op without YACL |
| `item/FoundryItem.kt` | `BlockItem` that restores stored lava on placement |
| `item/LavaStorageComponent.kt` | Typed `DataComponentType` payload (lava mB carried by the item) |
| `menu/FoundryMenu.kt` | Container menu — slot registration + ALL GUI layout constants |
| `screen/FoundryScreen.kt` | GUI rendering |
| `FoundryClient.kt` | Client init — screen registration + lava tooltip (`ItemTooltipCallback`) |
| `jei/FoundryRecipeCategory.kt` / `jei/FoundryJeiPlugin.kt` | JEI category + plugin (reads `recipeAccess().synchronizedRecipes`) |
| `assets/foundry/textures/gui/...` | `foundry.png` GUI sheet + animated `container/foundry/` sprites (flame, arrow, lava fill) |
| `mixin/PointedDripstoneBlockMixin.java` | Dripstone accelerator — Java (not Kotlin; see Mixin pitfall below) |

---

## Block Entity Architecture

Split into three classes. Their constants and method signatures are the single source of truth in
code — read them there rather than duplicating here.

- **`FoundryState`** — the mutable smelting fields (burn time, progress, `fuelSpeed`, `storedXp`) plus
  `save`/`load`. `smeltTotal = cookingTime * PROGRESS_RESOLUTION` while smelting.
- **`FoundryLavaTank`** — all fluid logic: the `SingleVariantStorage<FluidVariant>` exposed to pipes,
  bucket fill/consume, boost drain, and mB↔droplet conversion (`DROPLETS_PER_MB = 81`,
  `DRAIN_PER_TICK = BUCKET/1600`). Capacity is read live from config (so it's a `get()`, not a `const`).
  - **Pitfall:** the property MUST be named `capacityDroplets`, not `capacity`. Inside the
    `SingleVariantStorage` anonymous object an unqualified `capacity` resolves to the inherited
    `getCapacity()` synthetic, so `getCapacity(variant) = capacity` recurses forever (StackOverflowError).
- **`FoundryBlockEntity`** — orchestration: inventory, `ContainerData`, tick, recipe matching, XP,
  fuel-speed tiers, hopper rules. Implements `ImplementedContainer`, `WorldlyContainer`, `MenuProvider`.
  - Constructor param is `blockState` (not `state`) to avoid shadowing the `state: FoundryState` property.
  - Slot indices and `DATA_*` ContainerData indices live in its companion; `FoundryMenu` references the
    same `DATA_*` constants so they can't drift. ContainerData syncs as **shorts** (hence `Short.MAX_VALUE`
    caps on the fuel fields); lava percent/mB are server-derived (read-only client-side).
  - **Fuel-speed model:** tracked at `PROGRESS_RESOLUTION = 2` so 1.5× stays integer. Each smelting tick
    adds `fuelSpeed` (or `round(fuelSpeed * config.lavaSpeedMultiplier)` while lava is present).
    `getFuelBurnTime` accepts any vanilla fuel (`fuelValues()`) plus slag/magma cream (configured times).

**Cross-side recipe access:** always `level.recipeAccess().synchronizedRecipes` (`getFirstMatch` /
`getAllOfType`). `getRecipeFor`/`getAllRecipesFor` do **not** exist on the client `RecipeAccess`.
`ModRecipes.init()` registers the serializer via `RecipeSynchronization.synchronizeRecipeSerializer(...)`.
`isSmeltable(level, stack)` in the companion is the single source of truth (FoundryMenu delegates to it).

**`level.server` on `ServerLevel`** is non-null at runtime despite the `@Nullable` on base
`Level.getServer()` — a plain `.` call is correct (`// NOSONAR` if Sonar flags it).

---

## GUI

- **Layout constants** (slot, flame, arrow, lava-gauge coordinates) live in `FoundryMenu.Companion` and
  are the single source of truth — `FoundryScreen` reads the same constants, so slot hitboxes and visual
  outlines never diverge. Constants are the outer 18×18 slot corner; `addSlot` registers at `+1` for the
  inner 16×16 item area.
- `FoundryScreen.extractBackground()` draws: `foundry.png` sheet → slot outlines (vanilla
  `minecraft:container/slot`) → animated flame → animated arrow → vertically tiled lava fill. All mod
  sprites are namespaced `foundry:container/foundry/...`.
- `foundry.png` is a 256×256 **palette-indexed** (mode P) sheet owned entirely by this mod — no vanilla
  texture borrowing.
- F3+T reloads textures only; Kotlin changes need a full `./gradlew runClient`.

---

## Design Decisions

- **Smelting speed = fuel tier × lava**, every value read live from `FoundryConfig`. Lava in the tank
  doubles the active fuel speed and drains `BUCKET/1600` per boosted tick. Fuel type also affects burn
  *duration* (vanilla `fuelValues()`; slag/magma cream aren't vanilla fuels and use configured times).
- **Byproduct (slag):** `byproductChance` — integer floor = guaranteed count, fraction = roll for +1
  extra (supports `>1` for bulk recipes like raw-ore-blocks).
- **Weighted result pools:** `FoundryRecipe.resultPool` (optional). When non-empty, each produced item is
  a weighted pick (`rollResult(random)`) and `result` is just the viewer-facing representative. Used by
  **slag reprocessing** (iron 70 / gold 30 nugget; lava grants one extra roll via `bonusResultChance` +
  `bonusRequiresLava`). Pooled recipes require an *empty* result slot. JEI shows the pool as a cycling
  output with per-entry odds.
- **Extends `AbstractFurnaceBlock`:** free FACING + LIT blockstate, GUI open, recipe-book plumbing.
  `serverTick` keeps LIT in sync with `isBurning`; `ModBlocks` sets `.lightLevel { if (LIT) 13 else 0 }`
  (not automatic). `animateTick` adds front-face particles offset by FACING. `blockstates/foundry.json`
  maps all 4 facings × `lit`; `lit=true` uses the `foundry_lit` model (front-face texture only).
- **Stored lava survives item form:** `LavaStorageComponent` (typed `DataComponentType`) carries mB.
  `FoundryItem.place` restores it; creative break drops the item with the component (`playerWillDestroy`).
  Tooltip rendered via `ItemTooltipCallback` in `FoundryClient` (not the deprecated `appendHoverText`).
- **JEI** (`FoundryJeiPlugin` / `FoundryRecipeCategory`): recipes read from
  `synchronizedRecipes.getAllOfType(...)` — JSON is the single source of truth. Slag is an output slot, so
  left-clicking it lists every byproduct recipe. Fuel tooltips read the live `FoundryConfig` multipliers.
- **Slag bricks dripstone accelerator:** `PointedDripstoneBlockMixin.java` injects into two methods of
  `PointedDripstoneBlock`. `canDripThrough` (private static) is overridden to return `true` for slag
  bricks so the upward fluid search isn't blocked. `randomTick` fires two extra `maybeTransferFluid`
  calls when a downward tip (`THICKNESS == TIP`, `TIP_DIRECTION == DOWN`) has slag bricks at `pos.above()`,
  giving ~3× fill rate. **Must be Java:** Kotlin `companion object` emits a non-private static `Companion`
  field that Mixin rejects when merging into the target class. Any future mixin targeting a private static
  method needs the same treatment.

---

## Configuration

`config/foundry.json`, loaded once in `Foundry.onInitialize()` via `FoundryConfigManager.load()` (client
and server). Persisted with **Gson** (bundled with Minecraft — this mod intentionally does **not** ship
`fabric-language-kotlin`, so `kotlinx.serialization` is avoided).

- `FoundryConfig` is an immutable `data class`. `sanitized()` clamps every field and treats `0`/missing
  (Gson bypasses Kotlin defaults via Unsafe allocation) as "use default", so a partial/stale file heals
  on load instead of crashing.
- `FoundryConfigManager.config` is the live singleton, read each tick; edits apply next tick (no reload).
  Not network-synced — on a dedicated server the server's file is authoritative.
- Tunable fields: per-fuel speed multipliers (base / coal / magma cream / blaze rod), the lava bonus
  multiplier, lava tank capacity (buckets), and slag / magma-cream burn times.
- **In-game screen is optional:** YACL builds it, ModMenu launches it — both `compileOnly` + `localRuntime`
  (dev-only), listed under `suggests`, never `depends`. `FoundryConfigScreen` is only class-loaded when
  YACL is present; `FoundryModMenu` guards on `isModLoaded("yet_another_config_lib_v3")` and shows no
  button otherwise. Repos: `maven.terraformersmc.com/releases` (ModMenu), `maven.isxander.dev/releases` (YACL).
