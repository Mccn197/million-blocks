# MillionBlocks — Direction artistique V1 (propositions à valider)

**Cible :** Paper Java 26.2, huit armes/upgrades + portail, hub musée, resource pack personnalisé, pas de mod client imposé. **État :** document de préproduction + pack HUD glyphes prototype, *pas* huit modèles Blockbench finis, et aucune intégration plugin déployée.

## 1. Concept transversal : REVERIE MINING LAB

Les armes racontent une escalade : **outil amélioré → outil lourd → engin industriel → arme expérimentale → catastrophe cosmique**. Même style low-poly pixel art 16/32 px, base métal gris graphite, accent cyan pour le minage, jaune industriel pour les explosifs et rouge/rouge vif pour les faisceaux. De fines marques de danger « KEEP OUT » peuvent orner la machine du laser, mais pas reprendre l'asset exact de Bionic.

Échelle cible des objets tenus : silhouette lisible en vue FPS et à 2–4 blocs en third-person, ~20–60 cubes selon complexité ; prévisualisation propre en inventaire ; UV bien alignés ; tous les modèles originaux. Le rayon rouge et les VFX ne sont **pas peints dans le modèle** : ils sont émis par MillionBlocks pour que l'effet soit orienté vers la cible et suive le gameplay réel.

## 2. Les huit modèles à concevoir

### MB01 — SUPER PICKAXE · « Prismbreaker »
- **Silhouette :** pioche diamant high-tech, deux ailettes de cristal turquoise inversées, coeur central hexagonal, manche renforcé de métal foncé et laiton.
- **Signature visuelle :** cristaux qui semblent pulsés par l'énergie du cœur ; attache arrière articulée.
- **Animations :** idle pulsation 1.4 s (texture animée du cristal), swing *crit* 0.25 s, poussière de pierre au contact.
- **Difficulté :** faible (35–40 cubes + 32×32 texture). Un seul item Java model suffit pour V1.
- **Plan de démonstration :** courte séquence minage accélérée depuis première personne.

### MB02 — MINING HAMMER · « Quakehammer »
- **Silhouette :** tête rectangulaire massive en obsidienne usinée, double vérin hydraulique, incrustations orange.
- **Signature :** mâchoires latérales en blocs séparés et cabochons de sécurité jaune/noir.
- **Animations :** compression de vérins avant coup (via modèles échangés ou Display séparés), impact caméra + anneau de poussière et `block_crack`.
- **Difficulté :** faible/moyenne (40–55 cubes, quelques états de modèle).
- **Plan :** un clic, cercle 3×3/5×5 dans la paroi.

### MB03 — CREEPER MINER · « Detonation Buddy »
- **Silhouette :** capsule verte mécanique, tête de creeper carrée stylisée, pattes magnétiques, grandes diodes rouges sur les côtés.
- **Signature :** petit compagnon minier drôle mais très dangereux : yeux passant du vert à l'orange/rouge en charge.
- **Animations :** capsule lancée (arc), atterrissage avec 2 rebonds, déploiement des pattes, clignotement 0.7 s, explosion concentrique.
- **Difficulté :** moyenne (item + Display/entity compagnon ; il faut associer le bon PDC et gérer la suppression).
- **Plan :** lancer le Creeper au sommet d'une falaise et assister à une explosion visible de loin.

### MB04 — TNT LAUNCHER · « Blastcaster »
- **Silhouette :** arme lourde rouge/orange, canon tronconique octogonal, chargeur rotatif à 3–4 canisters TNT, viseur trapézoïdal, poignée arrière.
- **Signature :** tube de propulsion cuivre/acier, affichage d'un témoin de charge.
- **Animations :** chargeur pivotant après tir, recul du canon et projectile TNT à traînée de braises.
- **Difficulté :** moyenne (~60 cubes) ; recoil 2 états ou animation Display, projectile vanilla/custom.
- **Plan :** bombe traversant un ravin à 70 blocs.

### MB05 — MINING DRILL · « Bore-X »
- **Silhouette :** foreuse portative double prise, longue mèche conique à 6 segments anguleux autour d'un axe, engrenages cuivre/noir, réservoir cyan.
- **Signature :** hélice tronçonnée à cubes inclinés pour donner un volume arrondi sans textures lisses.
- **Animations :** rotation de mèche (si Display rig serveur ; sinon 4–8 états de modèle en boucle), étincelles et nuages de gravats.
- **Difficulté :** moyenne/élevée ; visuellement fort mais le rig en main exige une concession technique.
- **Plan :** caméra sur le côté montrant un tunnel se forer automatiquement.

### MB06 — MINING LASER · « REDLINE MK-I » — **pièce vedette**
- **Choix préféré :** mini-excavatrice laser **pilotable** comme sur ta capture. Châssis de chenilles anguleuses, cockpit ouvert à arceau, deux bobines d'énergie rouge, bras de canon articulé, lentille circulaire rouge et bouclier frontal.
- **Alternative secondaire :** le même canon en version portative (test/dev), afin de conserver un usage pratique pendant la mise au point.
- **Animations :** `spawn` (déploiement ~20 ticks), `charge` (anneaux de lentille + montée en fréquence ~15 ticks), `fire` (faisceau rouge continu + spirales de pixels rouges projetées dans la direction regardée), `stop` (retrait de la lentille + vapeur), `despawn`.
- **Architecture :** entité siège + groupe de `ItemDisplay`/`BlockDisplay` pivotants pour machine ; dégâts laser restent dans MillionBlocks (raycast). Contrôle joueur en montée à prototyper ; **ce n'est pas un véhicule mobile prêt**.
- **Performance :** un faisceau visuel principal + points de rayons espacés et système particules client, avec plafond global par tick.
- **Difficulté :** élevée ; prototype prioritaire de rig animé après approbation.
- **Plan :** montée sur la machine, charge visuelle, traverse une montagne à 100+ blocs, traveling latéral du faisceau rouge.

### MB07 — METEOR STRIKE · « Starfall Beacon »
- **Silhouette en main :** déclencheur/staff compact, médaillon à anneaux flottants et fragments de météorite orange/rouge autour d'une étoile centrale.
- **Animations :** ciblage au sol (4 anneaux orange clignotants), météore au loin descendant avec trail rouge/orange, flash d'impact, onde circulaire (poussière), explosion du terrain.
- **Difficulté :** moyenne (item statique + VFX dynamiques) ; animation des anneaux via entités d'affichage ou positions calculées.
- **Plan :** contre-plongée depuis le sol sur le projectile tombant du ciel.

### MB08 — WORLD EATER · « VOID ENGINE » — **upgrade finale**
- **Silhouette :** manipulateur gravitationnel à 4 branches d'obsidienne poli/noir-violet, un cœur d'améthyste enfermé dans un double anneau hexagonal, glyphes dangereux.
- **Animations :** `target` 0.5 s (cercle sombre au sol), `charge` 3 s (anneaux violets qui grossissent et effets d'aspiration de petits fragments), `detonate` (flash blanc/violet + détonation étendue), `aftershock` (onde circulaire + blocs retirés progressivement).
- **Difficulté :** élevée côté VFX, moyenne pour l'item.
- **Plan :** wide shot avant/après, cratère colossal. Adapter le rayon au budget TPS : impossible de poser 150 000 destructions en un seul tick.

## 3. Portail de boutique · « KEEP OUT / DANGER »

- Arceau industriel bois usiné + plaque danger inclinée + cœur vert acide avec texture défilante, modèle original.
- État `inventory` : petit jeton/boîtier portable avec un symbole portail ; pose illimitée dans monde Survival uniquement.
- État `deploy` : éléments sortant du sol, `0 → 25 → 60 → 90 → 100 %` sur ~1.0–1.5 s, flash vert et poussière. La porte ne devient interactive qu'à la fin.
- État `idle` : membrane verte ondulante, petites particules jaunes, variation subtile de luminosité.
- État `remove` : anneau se contracte, bris de quelques particules, suppression sans drop supplémentaire.
- Simple version faisable en vanilla Display + pack. Animation de membrane via PNG MCMeta ou modèles composés ; aucune obligation d'ajouter ModelEngine.
- Penser à orientation yaw et hitbox de portail de 2×2 blocs, contrôle collisions pour ne pas boucher les couloirs.

## 4. HUD « Mineral Counter »

**Disposition souhaitée :** à gauche sous la zone webcam, rectangle graphite bord gris, emblème de bloc stone/bedrock, super pioche turquoise, petit `xN` or en haut, **compteur dynamique en chiffres blancs**. Version Reverie : filets cyan discrets, sans reprendre au pixel près le HUD de Bionic. Exemple `⛏ x2` + `▣ 1 852 080`.

- Par défaut le **compteur seul** ; goal et % sont facultatifs et désactivables séparément.
- Position X/Y relative à l'écran, échelle et marges réglables ; UI scale compatible à tester dans deux résolutions.
- Modes `hidden`, `hud`, `bossbar_debug` ; un seul moteur visuel maître pour éviter les doublons.
- Mise à jour maximum 5 à 10 fois/seconde (pas chaque bloc à l'écran), affichage interpolé si nécessaire, aucune duplication d'ActionBar.
- Effet `+54 345` ponctuel en vert près du HUD lors des destructions massives.

**Architecture conseillée :** BetterHud (build explicitement compatible 26.2) pour coordonnées/texte dynamiques, intégré au plugin par API/placeholders. Oraxen gère les modèles/registries et éventuellement un pack fusionné. Attention : Oraxen seul n'assure pas un placement libre garanti du HUD à gauche. Faire un seul pack distribué et vérifier collisions de shaders / atlas / polices (BetterHud a changé de shaders selon les versions Minecraft).

**Fichiers présents dans ce kit :** `hud_preview_cyan.png`, `hud_preview_amber.png` et `MillionBlocks-HUD-proto-26.2.zip`. Le ZIP est un **resource pack valide de démonstration de glyphes** (format 88.0) mais **n'affiche pas encore automatiquement un HUD à gauche**. Test glyphes avec :

`/tellraw @s {"text":"\\uE100 \\uE101 \\uE102","font":"millionblocks:hud"}`

## 5. Intégration technique

| Composant | Responsabilité | Dépendance |
|---|---|---|
| `MillionBlocks` | gameplay, items IDs, permissions, triggers, progression, raycasts, rate limiting, sauvegardes | Paper 26.2 |
| `Blockbench` | modèles sources `.bbmodel`, UV, textures, poses de démonstration | outil hors serveur |
| `Oraxen` (optionnel mais pratique) | déclaration des items et meubles, référence des `item_model`, pack assemblé | compatible Paper, installation à faire |
| `BetterHud` (recommandé) | HUD à gauche, texte dynamique, animation `+blocs`, coordonnées à l'écran | build Paper 26.2 à valider |
| `Display Entities` | animation du portail, foreuse et machine pilotable sans mod client | Paper |

**Important :** une animation osseuse native GeckoLib `.animation.json` n'est pas lue directement par un resource pack Java vanilla ou Oraxen. Pour les rigs complexes, il faut un moteur compatible (ModelEngine ou autre) ou une logique d'animation côté plugin via plusieurs Displays. Pour la V1 nous privilégions les Displays natifs afin de réduire les dépendances et maîtriser les effets.

### Structure cible

```text
millionblocks-art/
  blockbench/{prismbreaker,quakehammer,creeper_pod,blastcaster,bore_x,redline_mk1,starfall,void_engine,shop_portal}.bbmodel
  textures/items/<id>.png
  textures/hud/{pickaxe,block,panel}.png
  vfx/{redline_beam,meteor,void_engine}/
  config/oraxen/items.yml
  config/betterhud/millionblocks-hud.yml
  minecraft-pack/assets/millionblocks/{models,textures,font}/
  tests/screenshots/
```

## 6. Ordre conseillé après validation

1. **Hud POC** : installer BetterHud compatible 26.2, exposer `%millionblocks_count%`, multiplier et goal au moteur. Tester sous GUI scales 2/3/4 et en 1080p/1440p. (Ne pas désactiver l'ancien HUD de secours tant que le nouveau n'affiche pas le compteur.)
2. **Un modèle léger** : Prismbreaker en `.bbmodel`, export item JSON, texture 32×32, modèle en main + showcase, pipeline pack validé.
3. **Prototype d'animation** : portail original en Display, montée de sol + interaction et persistance chunk, portails V1 migrés.
4. **Armes portables** : marteau, Creeper Pod, TNT Launcher, Bore-X, Beacon.
5. **Laser REDLINE MK-I pilotable** : créer siège, 5–8 parties animables, charge/faisceau et usage du gameplay V0.5.
6. **VOID ENGINE** : VFX progressifs / garde-fous de performances (taille, chunks, temps, sons, TPS) puis répétition cinématique.
7. **Intégration repo Git** : `feature/art-pipeline-v1` depuis `dev` puis merge vers `dev` ; ne pas déclencher le pipeline sans demande explicite.

### Tests de réception
- Une pioche custom est visible en inventaire, en main, en vitrine sans effet « missing texture ».
- Portail apparaît/retire/se recharge après retour au hub, et aucun duplicata après reconnexion.
- HUD suit nombre de blocs en moins de 0.3 s et `goal/percent` sont indépendamment toggleables.
- Laser visuel suit la visée et stoppe dès le relâchement ; les particules ne créent pas de ralentissement majeur.
- World Eater fonctionne de loin sans supprimer le hub et avec un plafond de blocs/tick.
- Aucune dépendance à un mod client (Axiom uniquement pour build, pas pour tournage).

## Références consultées (inspiration et contraintes techniques)

- https://mcmodels.net/products/10549/animated-officer-set
- https://mcmodels.net/products/11371/animated-holy-weapon-pack
- https://mcmodels.net/products/13524/animated-portals
- https://mcmodels.net/products/14999/portal-animated-decorations-volume-2
- https://docs.oraxen.com/configuration/items
- https://docs.oraxen.com/configuration/glyphs
- https://docs.oraxen.com/developers/api
- https://hangar.papermc.io/toxicity188/BetterHud/versions/
- https://feedback.minecraft.net/hc/en-us/articles/46690753273997-Minecraft-Java-Edition-26-2

Ces liens servent à apprendre les techniques et à comparer le rendu, **pas à copier des modèles payants**. La disponibilité/licence des assets tiers et compatibilité précise des plugins doivent être vérifiées avant déploiement.
