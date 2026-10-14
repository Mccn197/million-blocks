# Plugin research / dependency decision

## Gameplay plugin: custom (recommended)
The challenge mechanics are too specific to compose cleanly from off-the-shelf plugins. Keeping them together gives deterministic filming behavior, one config, consistent counters, and debug commands.

## WorldEdit / FastAsyncWorldEdit: optional production dependency
Use it to build the shop/reward room, paste schematics, prepare giant test volumes, or restore sections during map production. Do not make the core challenge depend on it.

## Shopkeepers: optional, not required
The maintained Shopkeepers plugin currently advertises support for Minecraft 26.2. It is useful only if we choose physical persistent merchants. The purchase/unlock rules should still stay in MillionBlocks.

## Citizens / economy stacks: skip for V0
Citizens is unnecessary if the shop can be represented by a simple villager/display/interaction or GUI. Vault/economy plugins add complexity without helping the story unless Brice specifically wants a second currency.

## Resource pack: optional later
Not needed for technical validation. It becomes worthwhile if we want unique models for the drill, laser gun, hammer, etc. Keep tool identity in PersistentDataContainer so visuals can be swapped later without changing gameplay.

## Reset strategy
Two layers:
1. Plugin-level `/mb arena reset` for very fast destructive tests.
2. Folder-level copied filming world / WorldEdit schematic for full-scene reset.

This is deliberately redundant: a broken test scene must never cost filming time.
