# MillionBlocks V0.6

Plugin Minecraft Java 26.2 / Java 25 : progression, armes, HUD Amber et objets originaux.
Développement sur `dev` et `feature/*`. Aucun changement ni fusion sur `main`.

**Livraison candidate compilée, validation en jeu encore nécessaire.** Le JAR a été compilé
contre les véritables sources officielles Paper 26.2. Le dépôt Maven Paper est bloqué dans
cet environnement : l'artefact exact `26.2.build.121-stable` n'a pas été validé.

- [Installation et checklist](docs/INSTALLATION-V06.md)
- [Résultats et limites](docs/VALIDATION-V06.md)
- [HUD BetterHud Paper](docs/BETTERHUD-AMBER.md)
- [Assets et animations](docs/ASSETS-V06.md)
- [Snapshot Survival hors ligne](docs/SURVIVAL-SNAPSHOT.md)
- [Compilation reproductible](docs/BUILD-V06.md)
- [Aperçu géométrique des neuf modèles](assets/previews/v06_geometry_contact_sheet.png)

## Fonctionnalités V0.6

- HUD BetterHud Paper fixe en haut à gauche, compteur live, x1/x2 effectif,
  objectif/pourcentage/multiplicateur/icône masquables, fissuration liée au minage.
- Intégration native de placeholders BetterHud, sans PlaceholderAPI ; BossBar de debug
  configurable si BetterHud ou sa configuration ne sont pas disponibles. Aucune sidebar.
- Neuf modèles Blockbench enrichis et éditables, atlas PNG originaux, modèles Java
  et définitions modernes `assets/millionblocks/items`. Trois pièces dérivées permettent
  le rotor, le laser portable et le châssis séparé de REDLINE.
- Portail industriel ItemDisplay montant du sol avec particules vertes, interaction
  de téléportation existante conservée, fallback industriel vanilla configurable.
- REDLINE montable et orientable avec siège, châssis fixe et tourelle séparée, tir rouge
  continu ; alternative portable. Poste fixe : aucun déplacement WASD du véhicule.
- Rotor de foret, recul TNT et charge du World Eater via Display Entities temporaires.
- Progression des destructions massives actualisée par lot pendant le minage.
- Outil de snapshot hors ligne avec SHA-256, refus d'un monde verrouillé,
  confirmation et conservation obligatoire du monde remplacé.

Les dossiers `references/` et `HANDOFF.md` conservent le contexte fourni ; leurs annonces
V0.5.1/prototypes ne remplacent pas les documents V0.6 ci-dessus.

## Tests locaux

```bash
scripts/bootstrap-tools.sh
python3 -m venv /workspace/toolchain/asset-venv
/workspace/toolchain/asset-venv/bin/python -m pip install -r requirements-dev.txt
scripts/validate.sh --source-api
```

`--source-api` est un secours explicite, pas une certification Paper build 121.
La compilation cible exacte reste `scripts/build.sh` après ouverture des accès Paper.
Aucun script ci-dessus ne déploie un serveur, ne pousse Git ou ne déclenche une CI distante.
