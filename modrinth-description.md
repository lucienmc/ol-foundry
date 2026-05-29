# Foundry

Foundry adds one block: the Foundry, a smelter with a few mechanics the vanilla furnace doesn't have. It burns solid fuel like a furnace, but the smelting speed depends on which fuel you use, it has a lava tank that speeds things up, and most ore recipes leave behind slag that you can do something with.

Recipes are data-driven and show up in JEI.

## Fuel and lava

Smelting speed depends on the fuel:

- Coal / charcoal — 1.5×
- Blaze rod — 3×

The Foundry has a 4,000 mB (4 bucket) lava tank. While it holds lava, smelting runs twice as fast — so coal becomes 3× and a blaze rod 6× — and the tank drains slowly while it works.

You can fill the tank three ways: right-click with a lava bucket, drop buckets into the bucket slot (they're consumed automatically), or pipe lava in with a fluid-transfer mod. Lava left in the tank stays with the block if you break it and place it again.

## Slag

Smelting ores and metals produces slag as a byproduct. It has a few uses:

- Reprocess it in the Foundry to recover nuggets — mostly iron, sometimes gold, with one extra if there's lava in the tank
- Craft it into slag bricks
- Burn it as fuel
- Compost it

## Other behaviour

- The Foundry banks the experience from smelting and releases it when you take the output, the same way a furnace does.
- A few recipes give more when lava is present — for example, ancient debris can drop extra netherite scrap.
- Hoppers and pipes work per face: top inserts items to smelt, the sides take fuel and lava buckets, the bottom pulls out results and slag, and lava can be piped in from any side.

## Recipes

- Raw ores and raw ore blocks into ingots and metal blocks
- Ores (stone, deepslate, and nether variants) into ingots
- Cobblestone into stone, gravel into flint, sand into glass
- Ancient debris into netherite scrap
- Slag into nuggets

Everything is visible in JEI, including byproduct chances and cook times.

## Requirements

- Fabric Loader and Fabric API
- Minecraft 26.1.2
- JEI is optional, for browsing recipes in-game

Source and issues: https://github.com/lucien-n/Foundry — MIT licensed.
