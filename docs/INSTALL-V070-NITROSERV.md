# MillionBlocks 0.7.0 — NitroServ test-only

## Sauvegarde
Ce batch NE CONTIENT AUCUN config.yml ni showcases.yml, portals.yml ou progress.yml à installer.
Arrête NitroServ et sauvegarde plugins/MillionBlocks/ et plugins/BetterHud/ avant toute modification.

## 1. JAR
- Arrête le serveur.
- Dans plugins/, déplace MillionBlocks-0.6.2.jar hors des plugins, ne laisse qu'un seul JAR.
- Copie MillionBlocks-0.7.0.jar dans plugins/.
- NE supprime PAS le dossier plugins/MillionBlocks/.

## 2. BetterHud
- Décompresse MillionBlocks-V07-BetterHud-PATCH.zip sur ton PC.
- Copie uniquement les dossiers contenus dans plugins/BetterHud/ dans le dossier BetterHud du serveur.
- Déplace vers une sauvegarde hors BetterHud les QUATRE anciennes configs millionblocks-amber.yml
  dans huds/, layouts/, images/ et texts/. Ces configs définissaient le même ID mb_amber.
- NE remplace PAS plugins/BetterHud/config.yml.
- Exécute /hud reload, puis active le resource pack généré dans plugins/BetterHud/build/.
- Vérifie que merge-other-folders inclut toujours MillionBlocks-items-0.6.0.zip.
- Pour les barres de démonstration, retire seulement test_hud de default-hud.
- Ajustement position: plugins/BetterHud/huds/millionblocks-v07.yml, pixel.x et pixel.y.

## 3. Test HUD
/mb hud mode betterhud
/mb hud show
/mb hud debug
/mb progress set 0
/mb progress add 999
/mb progress add 1
Après le titre Super Pickaxe, la mini-barre doit passer à Mining Hammer.
Autres paliers: 5000, 25000, 75000, 175000, 325000, 550000, 800000.
Ne touche pas au compte progression de la VOD.

## 4. Test raycast et armes
Déplace-toi dans la Survival TEST, jamais mining_sky.
/mb give all pour renouveler les items et leurs composants de clic-droit-dans-l'air.
/mb weapon debug avant et après un clic droit dans le vide:
vérifie held, holding et lastClick.
/mb weapon test creeper_miner lance directement la mécanique serveur.
Les tests meteor et world_eater sont destructifs.
Creeper: visual optionnel en Peaceful, mine contrôlée côté plugin.
Laser: maintiens clic droit 2-3 secondes, laisse charger les chunks et relâche.

## 5. Survival vierge et remise à zéro
/mb survival status
/mb survival prepare
/mb survival use new
/mb world survival
Le candidat reste séparé de l'ancien monde. Création seulement après commande admin.
Pour une restauration de monde après tests: arrêt complet du serveur obligatoire,
voir docs/SURVIVAL-SNAPSHOT.md et scripts/world_snapshot.py du dépôt GitHub.
Ne restaure jamais un monde actuellement chargé dans Paper.

## 6. Retour arrière
Arrête le serveur, remets l'ancien JAR et les quatre anciennes configs BetterHud.
Conserve les YAML des mondes, showcases, progressions et portails.

## Validation
Maven clean verify compile contre Paper 26.2 build 121 et Java 25.
11 tests Java passent. Les essais Minecraft NitroServ restent nécessaires.
