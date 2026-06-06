# Changelog

All notable changes to Foundry are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project aims to follow
[Semantic Versioning](https://semver.org/).

## [1.0.6] - 2026-06-06

### Added
- Hoppers can now pull empty buckets out of the lava slot from the bottom.
- The lava slot accepts any modded lava container, not just vanilla buckets — its lava is drained into
  the tank and the emptied container is returned to the slot.

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
