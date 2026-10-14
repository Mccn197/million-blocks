# MillionBlocks V0.6 — handoff (14 October 2026)

This is a **development starter**, not a live-ready Minecraft addon.

## Assets available
- 9 editable Blockbench `.bbmodel` prototypes + source textures.
- Amber HUD textures and BetterHud **Paper-plugin** example files.
- Minecraft resource-pack assets and model `.json` exports.
- Existing preview visuals for comparing styles.

## One crucial correction
The file `betterhud-2.1.1+mc26.2.jar` (around 46 KB) is **BetterHUD Fabric, a client-side mod**. It does not belong in Paper's `plugins/` folder. Use the **BetterHud** Bukkit/Paper server plugin by `toxicity188` from https://hangar.papermc.io/toxicity188/BetterHud/versions, selecting a Paper-26.2-compatible build such as `2.1.0-SNAPSHOT-448`. Keep a server backup first.

## Installation prerequisites
1. Server must have the actual **Paper BetterHud** plugin. Observe its startup log and default configuration files.
2. The `betterhud-plugin/plugins/BetterHud/` structure here is only a **template**. Confirm the exact layout against the installed version before merging.
3. MillionBlocks 0.5.1 does **not** yet register PAPI placeholders `millionblocks_count`, `millionblocks_multiplier`, `millionblocks_goal`, or `millionblocks_percent`. They must be implemented in a new MillionBlocks version before the HUD counter will display dynamically.
4. The Oraxen or ModelEngine integrations are **not active** in MillionBlocks 0.5.1. Blockbench models require proper packing and registration. The pilotable laser needs entity control logic.
5. When generating packs with BetterHud and Oraxen, merge their resources into **one** distributed pack and test with a clean client.

## Permissions/access
- No Blockbench account, computer remote-control, server admin password or paid-model licenses needed to **create** assets.
- For Git collaboration, create a dedicated `reverie-concepts/million-blocks` repo with `dev` branch and grant connector access. Do not modify ReverieMC.
- For direct NitroServ testing, connect **NitroServ** (not ReverieMC) to an approved server tool or provide exported `plugins/BetterHud/` and `plugins/MillionBlocks/config.yml` without secrets.
- No plugin replacement, destructive world action, CI pipeline, or resource-pack change should be performed without explicit deployment authorization.

## What still needs validation
- Pixel alignment for HUD at multiple GUI scales and screen sizes.
- Java 26.2 model format and animation rendering; load every `.bbmodel` in Blockbench.
- Exact 26.2-compatible versions of Oraxen/ModelEngine; consider own display-entity animations as fallback.
- Inventory item identifiers, ModelEngine mounting and laser shooting interactions.
