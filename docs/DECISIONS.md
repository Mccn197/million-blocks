# Decisions to validate together

## Already fixed
- Target stack: Paper 26.2 / build 121 stable, Java 25.
- Gameplay lives in our plugin; external plugins are map-production helpers only.
- Git convention: feature branch from `dev`, MR into `dev`, pipeline only on explicit request.
- Progress counter is permanent and never decreases when an item is unlocked/purchased.
- Laser is part of the main progression.
- Debug tooling is a first-class feature, not an afterthought.

## To decide with Brice / team
1. **Final target**: exactly 1,000,000 blocks, or a higher target for pacing?
2. **Number of destructive items**: recommended 8; possible 9th "Nuke" if pacing needs another beat.
3. **Shop presentation**:
   - physical room + clickable NPC/display (best on video),
   - inventory GUI only (fastest),
   - hybrid (recommended).
4. **Unlock model**:
   - milestones only (recommended),
   - separate currency earned per block,
   - actual spending of mined-block score (not recommended for audience clarity).
5. **Tool order / unlock thresholds**: tune after first local playtest.
6. **Destruction rules**: should player-built containers / reward room / shop be protected globally?
7. **Laser feel**: instant beam vs 1-2 second charge-up; recommended short charge-up for spectacle.
8. **Victory sequence duration**: immediate TP vs 2-4 second cinematic before reward room.
9. **Map reset scope**: only a designated test/mining arena vs full world template restore.
10. **Debris style**: only Creepers or every large explosive tool? Recommended every explosive tool with intensity scaling.

## Proposed 8-item progression
1. Super Pickaxe — 1,000
2. Mining Hammer — 5,000
3. Creeper Miner — 25,000
4. TNT Launcher — 75,000
5. Mining Drill — 175,000
6. Mining Laser — 325,000
7. Meteor Strike — 550,000
8. World Eater — 800,000
9. Victory — 1,000,000

These are tuning defaults, not design commitments.
