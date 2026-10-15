# MillionBlocks 0.6.2 — SAFE NitroServ verification

## Backups

Before copying anything, back up the full `plugins/MillionBlocks/` directory
(including `showcases.yml` and its .bak files) and both world folders.
Stop the server, replace only the JAR in `plugins/`, restart fully.
No world deletion, migration or generation is required.

## Portal destinations

1. Visit the built colosseum world `mining_sky` using an independent teleport
   command such as `/mv tp mining_sky` (Multiverse installed). Stand on safe floor.
2. Execute `/mb world set hub here` from the preferred arrival spot.
3. Visit the intended Survival world using `/mv tp survival_v2` or `/mv tp survival`
   (choose the actual world to film). Stand on safe floor.
4. Execute `/mb world set survival here`; then `/mb world status`.
5. Use the placed portal in each world. Return to the selected world and safe spawn.

These commands only change `plugins/MillionBlocks/config.yml` world names and hub
coordinates. No worlds are created or cleared.

## Showcase scale

- `/mb showcase size nearest 3`
- `/mb showcase size sc-001 5`
- `/mb showcase size all 2`
- `/mb showcase save`
- `/mb showcase reload` — verify sizes persist.

Scale range 0.25 to 12, default 1 for older YAML. Size is per showcase and
applies to ItemDisplay (with an adjusted floating height and interaction box).

## Remote weapons (destructive: use only a disposable world)

- `/mb weapon debug` — prints current world, hub protection, held tool ID,
  target coordinates and whether that chunk is loaded.
- `/mb give all` — REQUIRED for 0.6 custom items; old weapons can lack
  the right-click-in-air item component.
- Aim far away (up to configured range) and right click in Survival.
- `/mb weapon test creeper_miner` — verifies the server action even if
  the client item fails to emit PlayerInteractEvent.
- `/mb weapon test meteor`, `/mb weapon test world_eater` — destructive!
- Creeper Miner now triggers a scheduled plugin-controlled blast even if the
  visual Creeper entity despawns or `mobGriefing` is disabled.
- Distant targeting falls back to visible loaded terrain instead of
  triggering effects in unloaded sky.

## BetterHud left-edge clipping

Copy `plugins/BetterHud/huds/millionblocks-amber.yml` from the HUD patch zip.
In existing `plugins/BetterHud/config.yml`, **remove only unused demo HUDs**:

```yaml
default-hud: []
default-compass: []
```

Keep other HUDs if in use. Run `/hud reload`, rebuild/reapply BetterHud's final
resource pack if needed. The corrected HUD pixel offset is `(130,44)` relative
to top-left, instead of `(20,80)` that clipped the 255 px image.

## Limitations

Maven verification is not a live server test. Creeper and ray-tracing fixes
must be verified on NitroServ. Avoid testing with the VOD world or hub.
