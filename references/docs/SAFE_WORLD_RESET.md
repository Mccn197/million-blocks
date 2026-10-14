# Safe reversible Survival reset – NitroServ / Paper 26.2

**Never remove or overwrite a loaded Minecraft world folder.** A world with player/chunk data still loaded can be damaged if file operations race with saves.

## Set up the clean VOD template
1. Choose the preferred Survival seed and create the world. Visit village/sulfur to verify coordinates in 26.2.
2. Put the player in the mining hub, not the survival world, then **stop Paper completely** in NitroServ.
3. Download a complete ZIP of the survival world folder (e.g. `survival_v2/` including `level.dat`, `region/`, `entities/`, `poi/` and possible `DIM*` folders) to your PC. Name it `survival_vod_template_YYYY-MM-DD.zip`.
4. Keep a second copy outside NitroServ, plus your MillionBlocks configuration/backups separately.
5. Restart and use that same world as a free testing world.

## Reset to the VOD template
1. Broadcast and teleport **all players** to `mining_sky` (including admins). Verify nobody is in the target world.
2. Stop Paper. Download a separate last-minute backup of the destroyed world if you might want to recover scenes.
3. Rename the active world folder to `survival_v2_previous` or download and delete only that **specific** target folder after verification. Never change the server's global default `world` by mistake.
4. Extract/copy the clean `survival_vod_template` content back into `survival_v2/` with the correct directory nesting (e.g. `survival_v2/level.dat`, not nested twice).
5. Restart Paper. Teleport into Survival and check terrain, village, player position and plugin progress.

## Future plugin commands (DESIGN ONLY, NOT PRESENT IN V0.5.1)
- `/mb survival snapshot save <name>`: save world to immutable template, audit checksum.
- `/mb survival snapshot list`.
- `/mb survival reset prepare <name>`: admins only, backups, player evacuation.
- `/mb survival reset confirm <token>`: schedule offline restore on controlled reboot.
- `/mb survival snapshot restore-status`.

The reset operation needs a two-phase guarded workflow; a world cannot safely be overwritten while loaded. Player progress is stored by MillionBlocks separately and **is not automatically reset by restoring the world**. Add an explicit separate `/mb progress reset` during VOD preparation, if desired.

If on shared ReverieMC, take special care to isolate this to the concept server, not the existing Lobby/Skyblock worlds.
