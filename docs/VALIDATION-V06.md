# Validation V0.6 — 14 octobre 2026

## Résultats effectivement obtenus

| Contrôle | Résultat |
| --- | --- |
| Import V0.5.1 depuis le ZIP | branche `feature/import-v051` issue de `dev`, publiée |
| Java/Maven | Temurin 25.0.2+10 et Maven 3.9.11 ; archives vérifiées |
| Compilation API source officielle Paper 26.2 | réussie, sources complètes et générées |
| Compilation MillionBlocks V0.6 avec cette API | réussie, release Java 25, JAR produit |
| JUnit | 8 tests, 0 échec/erreur/skip |
| Python | 16 tests, 0 échec/skip |
| Génération des assets | 9 modèles principaux, 4 rigs, 12 définitions item ; identique à l'octet à la régénération |
| PNG/JSON/YAML et références | chargements, UV, géométrie, textures, bones/animations et types des conditions contrôlés |
| Snapshot | tests sur fichiers factices, y compris verrou POSIX concurrent et conservation du monde remplacé |
| Contrat BetterHud Paper | API/YAML examinés dans les sources 2.1.0/26.2 ; pas de validation du binaire NitroServ |
| Format pack Minecraft 26.2 | 88.0 ; recoupé avec les métadonnées de version `misode/mcmeta` tag `26.2-summary` |

## Ce qui reste non validé ou bloqué

- `mvn ...` avec l'artefact **exact** `26.2.build.121-stable` : refus 403 du proxy sur
  `repo.papermc.io`. Le POM conserve cette cible ; le secours ne prétend pas la remplacer.
- Serveur Paper build 121 lancé avec le plugin : **non exécuté**, téléchargement officiel
  du binaire/ressources non accessible dans le runtime actuel.
- Meilleure preuve de compatibilité BetterHud : version de NitroServ inconnue, aucun accès
  distant demandé/utilisé. Le pack shader HUD final doit être généré par cette version.
- Interface HUD, animation des displays, siège/tourelle, minage et fonctions conservées :
  implémentés et compilés, **non testés en jeu**. Les tests unitaires ne prouvent pas ces comportements.
- REDLINE : poste de tir fixe avec visée pilotée ; véhicule mobile/WASD non implémenté.
- Animations Blockbench Free : éditables, mais non exécutables par les modèles JSON vanilla.
- Snapshot d'une vraie Survival, restauration NitroServ et performance multi-joueurs : non exécutés.
- PR : `gh pr create --base dev` refusé par GitHub GraphQL ; accès Git en lecture/push fonctionnel.
  Les descriptions sont prêtes dans `docs/pull-requests/`. Aucun numéro de PR n'est inventé.

## Livrables

La release candidate est produite par `scripts/package_release.py` dans `dist/` :
JAR marqué `source-api`, ZIP items, bundle BetterHud, modèles/textures, archive complète
et sommes SHA-256. La provenance de compilation est inscrite dans le manifeste JAR et
`build-provenance.json`. Les logs pertinents sont dans `docs/validation/`.

Les domaines et instructions cloud sont sauvegardés au **brouillon** seulement. Revoir et
sauvegarder les paramètres puis publier l'environnement pour activer cette configuration.
La publication et la restauration dans une nouvelle tâche n'ont pas été testées.

Aucune modification/fusion sur `main`, aucun déploiement NitroServ, aucun accès ReverieMC,
aucun déclenchement de pipeline distant, aucune suppression de monde réel.
