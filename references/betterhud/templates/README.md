## BetterHud Amber – integration template

Files follow the current documented BetterHud structure: image declaration → layout → HUD. They are **not plug-and-play yet**. You must install a confirmed 26.2-compatible BetterHud build, register the image files, and provide working PlaceholderAPI values from MillionBlocks (`millionblocks_count`, `millionblocks_goal`, `millionblocks_percent`, `millionblocks_multiplier`).

Place textures from `resourcepack/assets/millionblocks/textures/hud` (and the Amber panel/pickaxe from `assets/millionblocks/textures/font/`) in the exact BetterHud `assets` location required by your installed version; re-export with `/betterhud reload`. Then merge the BetterHud generated assets into the Oraxen pack as described in BetterHud docs. Do **not** serve multiple unrelated required packs to the client.

The crack sprites are 5 frames. Once the counter event is integrated, select `stone_crack_0..4` or animate via BetterHud if supported. This is a technical starter, not a live HUD.
