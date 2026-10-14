# Installation et checklist V0.6

**Aucun déploiement n'a été effectué.** Commencer sur un serveur de test indépendant de
NitroServ et de ReverieMC, avec copies de mondes. Le JAR livré est un candidat compilé
contre les sources API 26.2, pas un build certifié contre l'artefact Paper build 121.

## Préparation

- [ ] Activer l'accès au dépôt Paper et réussir `scripts/validate.sh` sans `--source-api`.
- [ ] Installer Paper **26.2 build 121**, Java **25** et noter leurs versions exactes.
- [ ] Relever la version de BetterHud **Paper** et vérifier qu'elle supporte 26.2.
- [ ] Faire une sauvegarde complète des mondes, données joueurs et dossiers de plugins.
- [ ] Préparer le snapshot Survival vierge hors ligne suivant `SURVIVAL-SNAPSHOT.md`.
- [ ] Vérifier la seed et le nom Survival actif ; ne jamais réutiliser par erreur une Survival
      de production pour les essais destructifs.
- [ ] Conserver `progress.yml`, `showcases.yml`, `portals.yml` et `config.yml` existants.

## Installation manuelle

1. Arrêter le serveur de test. Copier le JAR dans `plugins/`, retirer seulement l'ancien JAR
   MillionBlocks après l'avoir archivé. Ne pas effacer le dossier de données du plugin.
2. Ajouter/revoir les valeurs V0.6 du `config.yml` livré. Un ancien `hud.mode: custom` conserve
   l'ActionBar ; choisir `betterhud` explicitement. `portal.use-custom-model` active le modèle
   custom ; `false` sert de fallback vanilla. Définir `hud.multiplier` à 1 ou 2.
3. Copier les fichiers BetterHud fournis, en préservant les autres configs. Démarrer le
   serveur puis suivre `BETTERHUD-AMBER.md` pour `/hud reload` et la génération/fusion du pack.
4. Charger/faire accepter le pack final au client. Aucun envoi automatique de pack n'est
   ajouté au plugin MillionBlocks : garder le mode de distribution existant de BetterHud.
5. Réémettre les items : `/mb give all`, `/mb portal give`. Les anciens items ne reçoivent
   pas automatiquement leur `item_model` V0.6 ; les réémettre ne remet pas les scores à zéro.

## HUD — priorité de validation

- [ ] `/mb hud mode betterhud` ; `/mb hud debug` indique `mb_amber chargé, API connectée`.
- [ ] Pioche diamant, bloc et panneau Amber fixes à gauche, aucune sidebar.
- [ ] `/mb progress set 999`, casser un bloc : compteur 1 000 en x1.
- [ ] `/mb hud multiplier 2`, casser un bloc : +2 ; repasser à 1.
- [ ] Fissuration 0→1→2→3→4→0, y compris sous minage continu/laser.
- [ ] `/mb hud toggle goal|percent|multiplier|icon` masque/réaffiche chaque option.
- [ ] Objectif changé dans `challenge.target-blocks`, puis `/mb reload config` : objectif et %
      mis à jour. Tester aussi un compteur dépassant l'objectif.
- [ ] `/mb hud hide` et `show` ; déconnexion/reconnexion ; `/hud reload` puis `/mb hud refresh`.
- [ ] Tests de lisibilité GUI scale 2/3/4, résolutions 1280×720 et 1920×1080, pack refusé.
- [ ] BetterHud absent ou `mb_amber` manquant : debug utile, fallback BossBar contrôlable.

## Objets, animations et fonctions conservées

- [ ] Les huit armes et le portail ont leur modèle en inventaire, main et showcase.
- [ ] Ouvrir/éditer les neuf `.bbmodel` et les quatre rigs sans référence texture manquante.
- [ ] Portail dans Survival : montée, particules vertes, texte, clic droit TP, clic gauche retrait.
- [ ] Retour hub/Survival, rechargement des portails, chunk unload/reload et redémarrage.
- [ ] Protection du hub, édition admin, téléportations, import schématique et showcases.
- [ ] Minage naturel, 3×3/5×5, Creeper Miner, TNT, foret, laser, météore et World Eater.
- [ ] Progression live pendant les destructions massives ; arrêt à la déconnexion/au changement
      de monde ; cap/budget de blocs ; aucun tir dans le hub.
- [ ] `/mb laser place`, `mount`, `fire on`, visée, `fire off`, descente, `remove`.
      Le poste REDLINE est fixe ; le pilotage porte sur la visée, pas sur le déplacement.
- [ ] Alternative portable : maintien/relâchement clic droit, changement de slot et monde.
- [ ] Rotor, recul TNT et charge World Eater : pivots, cadence et nettoyage des props.
- [ ] Victoire et TP récompense au franchissement du seuil.
- [ ] Mesurer TPS/CPU avec plusieurs lasers ; les effets visuels ne constituent pas une mesure
      de performance. Réduire portée/budget si nécessaire.

## Après les essais destructifs

- [ ] Arrêter complètement le serveur de test et désactiver le redémarrage automatique.
- [ ] Faire une sauvegarde hébergeur puis confirmer explicitement la restauration hors ligne.
- [ ] Vérifier le secours et la Survival restaurée ; choisir séparément le sort du score et
      des données externes au monde. Ne jamais supprimer un monde chargé.
- [ ] Documenter les versions et observations avant toute décision de déploiement NitroServ.

Les cases restent volontairement ouvertes : aucune session avec client Minecraft ni aucun
accès au serveur NitroServ/ReverieMC n'a été réalisé. Aucun pipeline distant n'a été lancé.
