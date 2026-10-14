# Million Blocks — Reverie Minecraft Concept

Technical prototype for a Minecraft video challenge inspired by the escalating block-destruction loop of “I Mined 1 BILLION Blocks in Minecraft!”.

## Goal
A normal survival world becomes progressively easier to destroy. The player mines toward a configurable target, unlocks increasingly spectacular tools, and is teleported to a reward room at victory.

## V0 technical scope
- Persistent mined-block counter per player.
- Protected inventory item for shop teleport / shop GUI.
- Configurable progress target and item thresholds.
- 8 destructive tools including a Mining Laser.
- Creeper/explosion debris animation with temporary falling blocks.
- Victory title + delayed reward-room teleport.
- Debug/admin commands to skip farming and replay scenes quickly.
- Resettable local test arena.
- Manual-only GitLab pipeline (`web` / `api`) matching Reverie concept conventions.

## Recommended external plugins
None required for gameplay.

Optional production helpers:
- FastAsyncWorldEdit / WorldEdit for building and resetting large filming sets.
- Shopkeepers only if we decide we specifically want persistent physical merchants. The main shop logic should remain ours.

## Debug command surface
`/mb help`

Planned / included command families:
- `/mb progress get|set|add|reset`
- `/mb give <item|all>`
- `/mb shop`
- `/mb victory`
- `/mb arena reset`
- `/mb reload`

See `docs/DECISIONS.md` for the short list to validate together.
