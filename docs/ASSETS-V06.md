# Assets V0.6

Neuf sources `assets/blockbench/*.bbmodel`, atlas PNG 64×64 embarqués, cubes nommés,
UV par face et groupe racine. Les prototypes initiaux restent dans
`references/prototypes/blockbench/`. Les exports Java ont les mêmes cubes/UV, sans géométrie
réservée à ModelEngine. Les UV PNG sont convertis vers l'échelle 0–16 des modèles Minecraft.

| Asset | Détails ajoutés |
| --- | --- |
| Prismbreaker | facettes diamant, poignée segmentée, cellule et insert prismatique |
| Quakehammer | pistons d'impact, rail hazard, grip amorti et attaches |
| Detonation Buddy | roues blindées, moyeux, sac minier et yeux de statut |
| Blastcaster | bouche segmentée, refroidissement, rail, cartouche et pontet |
| Bore-X | foret étagé en six segments, ailettes moteur, rotor indépendant |
| REDLINE MK-I | optique segmentée, émetteur rouge, radiateurs, châssis/tourelle séparés |
| Starfall Beacon | stabilisateurs, supports dorés, anneau orbital et cœur |
| VOID ENGINE | confinement segmenté, cœur violet, ailettes et avertissements |
| Portail industriel | pistons, joints hazard, panneaux KEEP OUT/DANGER, balises et seuil |

Les PNG utilisent des matériaux originaux déterministes : acier brossé, métal sombre,
couleur d'énergie propre à chaque outil, bandes de danger et marquages pixel art. Le portail
possède un treillis d'énergie avec trous transparents. Les arrondis restent segmentés/cubiques.
`assets/previews/v06_geometry_contact_sheet.png` est un aperçu dérivé de la géométrie exportée,
pas une capture Minecraft ni une validation du rendu dans le jeu.

## Édition et export

Ouvrir les neuf `.bbmodel` dans Blockbench en format Java Block/Item. Les PNG sont intégrés
et également livrés séparément dans le pack. Les quatre rigs dans `assets/blockbench/animated/`
utilisent le format Free et des pistes éditables : montée du portail, rotation du rotor,
recul de Blastcaster, lévitation/charge de VOID ENGINE.
**Les animations Free ne s'exécutent pas dans un modèle item JSON vanilla.** Le plugin
recrée ses effets avec des Display Entities ; aucun runtime ModelEngine n'est requis.
Le rotor du rig Bore-X est séparé du boîtier pour l'édition de l'animation.

Définitions modernes : `assets/millionblocks/items/<id>.json`, sélectionnées par
`ItemMeta.setItemModel`. Pas d'override global de pioche vanilla ni de CustomModelData fragile.
Le pack fournit aussi les trois pièces dérivées `bore_x_rotor`, `redline_portable` et
`redline_chassis` ; le laser en inventaire utilise la pièce portable, la machine combine
châssis et tourelle. Tous les clients doivent accepter le pack pour voir ces modèles.

## Comportements implémentés, à tester en jeu

- Portail : ItemDisplay animé sur 18 ticks, particules vertes, texte/hitbox, TP existant.
  `portal.use-custom-model: false` rétablit des BlockDisplays industriels vanilla.
- REDLINE : `/mb laser place`, `/mb laser mount`, `/mb laser fire on|off`, `/mb laser remove`.
  Châssis fixe, siège, visée par le regard, tourelle distincte. Sortir avec accroupissement
  coupe le tir au prochain tick de contrôle. Pas de conduite WASD, ni sauvegarde de la machine
  temporaire après redémarrage. Une machine par administrateur ; nettoyage à sa déconnexion.
- Portable : `/mb give laser`, maintien clic droit, relâchement pour arrêter.
- Foret : prop de rotor tournant pendant le forage.
- TNT : prop avec translation de recul ; trajectoire/fusible existants conservés.
- World Eater : prop montant/tournant pendant la charge, effet sphérique existant conservé.

Le cadrage première personne, les pivots, le siège, la portée visuelle, les textures en jeu
et le coût graphique de plusieurs joueurs restent à vérifier. Ne pas confondre compilation,
validité JSON et qualité/compatibilité visuelle validée avec un client.

## Reproduction

```bash
/workspace/toolchain/asset-venv/bin/python scripts/build_assets.py
/workspace/toolchain/asset-venv/bin/python scripts/render_models.py
```

La génération utilise les prototypes archivés comme base et reproduit les mêmes fichiers
à l'octet près. Les éditions manuelles des fichiers générés doivent être reportées dans le
script ou protégées avant sa réexécution : il remplace ses sorties.
