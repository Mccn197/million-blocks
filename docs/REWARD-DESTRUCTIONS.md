# Reward Destructions — MillionBlocks V0.7.1

Once a player reaches 1,000,000 blocks (configurable), the victory title appears.
After the configured delay, the plugin teleports the player to a separate,
plugin-owned reward world named reward_destructions.

Never change the reward-world name to an existing map. The creation code refuses
to overwrite a world that has no MillionBlocks ownership manifest.

## Features

- 20 individually labeled and destructible 16x16x16 sample chunks:
  stone, dirt, deepslate, coal, iron, gold, diamond, emerald, copper, redstone,
  lapis, ancient_debris, obsidian, bedrock, glass, diamond_block,
  netherite_block, tinted_glass, amethyst, end_stone.
- Void world, wide indestructible bedrock floor, central arrival platform.
- New worlds are created on first /mb reward prepare or after challenge victory.
- Build/reset operations are throttled (1250 block writes per tick).
- Mining in the reward world does not increment the main challenge counter.
- Bedrock remains normally indestructible, intentionally, as a contrast material.
- Each reset only rebuilds known cuboids; full reset also regenerates the floor.
- No deletion of world directories or player data.

## Commands (OP)

/mb reward status
/mb reward list
/mb reward prepare
/mb reward tp
/mb reward reset diamond confirm
/mb reward reset all confirm
/mb victory

## Safe installation

Install only MillionBlocks-0.7.1.jar into plugins/ with old JAR removed.
Preserve all plugins/MillionBlocks YAML files and showcase backups.
A new reward-destructions.yml manifest is generated in the plugin data folder.
The managed world folder is created at the root as reward_destructions/.
No new BetterHud or resource pack files are required.
