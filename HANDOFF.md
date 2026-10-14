# MillionBlocks V0.6 — WORK / Codex handoff

This handoff ZIP gathers inputs from the user's existing MillionBlocks Minecraft project.
It is NOT a completed V0.6, a production-ready resource pack, or a validated BetterHud integration.

## What's inside

- `plugin/` — full Minecraft Paper 26.2 / Java 25 plugin source **V0.5.1** (Maven `pom.xml`, Java, config and scripts).
- `assets/blockbench/` — nine original **editable prototype** `.bbmodel` files (8 upgrades + portal).
- `assets/resourcepack/` — **prototype** pack textures, fonts, item-model JSON files.
- `assets/betterhud-plugin/` — **draft BetterHud Paper templates** and image assets; inspect API/version before installing.
- `assets/previews/` — visual previews of HUD Amber, laser, portal and storyboard GIFs.
- `references/` — art direction, safe-world reset procedure, select Oraxen templates, original schematic, last known 0.5.1 built JAR for comparison.

## Target goals

1. Provide a truly functional HUD Amber anchored at the top left, diamond Minecraft pickaxe + cracked block + dark rectangular numeric counter as in Bionic video. Live count from MillionBlocks, optional goal / percentage / multiplier, debug BossBar fallback.
2. Improve all eight Blockbench prototype weapons and industrial KEEP OUT/DANGER portal. Prioritize fully editable source and original assets, not screenshots alone.
3. Design/implement animated industrial portal emerging from ground and pilotable REDLINE MK-I laser if technically viable; preserve handheld fallback.
4. Maintain existing game behavior. Validate Paper 26.2/Java 25 and BetterHud Paper integration.
5. Preserve safe reset/snapshot workflow for Survival. NEVER delete or overwrite existing worlds.

## GitHub

Repo: `Mccn197/million-blocks` — initialized with a README on `main` and a `dev` branch.
**DO NOT PUSH DIRECTLY TO `main` OR MERGE INTO `main`.** Work from `dev` into feature branches with PRs back to `dev`.
The existing source has NOT YET been imported in GitHub. Import from `plugin/` as a first step on a feature branch.
Do not automatically deploy to NitroServ or touch ReverieMC. Do not trigger remote CI pipelines without user consent.

## Important distinctions

- Minecraft Java *resource packs* do not run Java plugin logic; a texture/glyph pack alone cannot make a working dynamic HUD.
- BetterHud should be the **Paper server plugin**, not Fabric client mod. Confirm actual installed version and config format.
- Oraxen + ModelEngine were seen on another server (ReverieMC), NOT automatically available on NitroServ.
- Native static Minecraft model JSON differs from Blockbench rigged animations; complex animation may need Display Entities or ModelEngine.
- User screenshots in previous chat may not be attached here; HUD design and storyboard previews are in `assets/previews/`.
- Do not assume 0.5.1 is bug-free. It was compiled and was used for live gameplay but V0.6 requires its own tests.
