# Changelog

All notable changes to Foundry are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project aims to follow
[Semantic Versioning](https://semver.org/).

## [1.0.9] - 2026-06-06

### Added

- **Slag-brick dripstone accelerator** — place a Slag Bricks block between a lava source and a
  hanging pointed-dripstone tip above a cauldron to speed up lava filling. Lava passes through
  Slag Bricks to reach the dripstone below, and the drip rate is roughly 3× faster than vanilla.

## [1.0.8] - 2026-06-06

### Added

- All vanilla blasting recipes now work in the Foundry: iron, gold, copper ores and their raw forms,
  nether gold ore, nether quartz ore, and ancient debris.
- Ore blocks produce **Slag** as a byproduct (70% chance); raw ores have a lower chance (35%).
- **Slag reprocessing** — smelt Slag to recover an iron or gold nugget (70 / 30 split). With lava in
  the tank, you always get a second nugget roll.
- A **Foundry Fuels** page in JEI styled after the vanilla blast-furnace fuel tab: each entry shows
  an animated flame, the fuel item, how many items it smelts at base speed, and a second line for
  fuels with a speed bonus (coal ×1.5, magma cream ×2, blaze rod ×3).
- The JEI smelting recipe now shows the Foundry's actual GUI as its background, with an animated
  flame and a fuel slot that cycles through every valid furnace fuel.
- Click the smelting arrow in the Foundry screen to jump to its recipes in JEI.
- A recipe-transfer (+) button in JEI that loads a recipe's ingredient straight into the input slot.

### Changed

- Fuels moved out of the JEI smelting recipe onto their own dedicated page, so each recipe shows just
  its input and outputs.

## [1.0.7] - 2026-06-06

### Added

- The lava slot accepts any modded lava container, not just vanilla buckets — its lava is drained into
  the tank and the emptied container is returned to the slot.
- Right-clicking the Foundry with a lava-filled container — a vanilla bucket or a modded cell/tank —
  now fills the tank (previously vanilla buckets only).

## [1.0.6] - 2026-06-06

### Added

- Hoppers can now pull empty buckets out of the lava slot from the bottom.

### Changed

- A lit Foundry only billows the heavier smoke while its lava tank is fueled, and has a new lit top
  texture.

## [1.0.5] - 2026-06-06

### Added

- Translations for 10 languages: French, German, Spanish, Simplified Chinese, Russian, Brazilian
  Portuguese, Japanese, Korean, Italian, and Polish.

## [1.0.4] - 2026-06-05

### Added

- **Lava tapping** — sneak + right-click a Foundry with an empty bucket to draw one full bucket of
  lava back out of its tank, the inverse of filling it. Only fires when the tank holds at least a
  full bucket (1000 mB).

### Changed

- Filling or tapping the tank now keeps the swapped bucket in the **same hand slot** when you're
  holding a single bucket, instead of dropping the new bucket into a different inventory slot.
