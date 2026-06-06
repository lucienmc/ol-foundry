# Changelog

All notable changes to Foundry are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project aims to follow
[Semantic Versioning](https://semver.org/).

## [1.0.8] - 2026-06-06

### Added

- A **Foundry Fuels** page in JEI, listing every fuel with its smelting-speed multiplier and burn
  time: coal/charcoal, magma cream, blaze rod, slag, lava, and any other furnace fuel at the base rate.
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
