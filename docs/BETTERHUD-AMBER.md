# HUD Amber — BetterHud Paper

## Contrat

MillionBlocks enregistre directement sept placeholders via l'API publique BetterHud :

| Placeholder | Type | Valeur |
| --- | --- | --- |
| `millionblocks_count` | String | compteur formaté avec espaces |
| `millionblocks_goal` | String | `/ objectif`, ou chaîne vide |
| `millionblocks_percent` | String | pourcentage plafonné à 100 %, ou vide |
| `millionblocks_multiplier` | String | `x1`/`x2`, ou vide |
| `millionblocks_visible` | Boolean | état individuel/global du HUD |
| `millionblocks_icon` | Boolean | visibilité pioche/bloc |
| `millionblocks_crack` | Number | 0 au repos ; étapes 1–4 sur 600 ms |

Les snapshots sont immuables et publiés dans une ConcurrentMap : le renderer BetterHud
lit les valeurs sans appeler l'API Bukkit ni lire la configuration depuis son thread.
La progression provoque un rafraîchissement immédiat ; le timer traite reload/options.
Un minage continu laisse la fissuration avancer, plutôt que de bloquer l'étape 1.

L'API, le format des layouts et des conditions ont été examinés dans les sources BetterHud
2.1.0 pour 26.2 au commit `d5843a0` (6 juillet 2026), et dans leur version actuelle.
Ce contrôle de contrat n'est pas un test du plugin binaire installé sur NitroServ.
L'adaptateur échoue proprement et expose son diagnostic si l'API diffère.
Il n'ajoute ni PlaceholderAPI, ni ModelEngine, ni Oraxen.

## Installation manuelle sur un serveur de test

1. Relever `/version BetterHud` et `/version` ; conserver ces résultats.
2. Sauvegarder la configuration BetterHud et son resource pack existant.
3. Copier **le contenu** de `assets/betterhud-plugin/plugins/BetterHud/` dans
   `plugins/BetterHud/`, sans écraser d'autres fichiers/configurations.
4. Installer MillionBlocks puis démarrer. BetterHud est en `softdepend` : il démarre avant
   MillionBlocks. Son premier chargement peut signaler des placeholders manquants.
5. Après l'activation de MillionBlocks, exécuter `/hud reload` (alias `/bh reload`).
   Cela reconstruit les layouts et le pack généré de BetterHud avec les placeholders présents.
6. Fusionner le pack items MillionBlocks avec **le pack généré par cette version BetterHud**,
   garder ses shaders/fonts, puis distribuer le pack final aux clients selon la configuration
   de BetterHud. Il faut éviter deux systèmes indépendants qui écrasent leurs packs.
7. `/mb hud mode betterhud`, puis `/mb hud debug` : résultat attendu
   `mb_amber chargé, API connectée`, sans BossBar de fallback visible.

Les YAML ont `default: false` : MillionBlocks ajoute/retire seulement son HUD, sans masquer
les autres HUD BetterHud. Position `gui: x=0,y=0`, décalage pixels `(20,80)` ; modifier le YAML
puis `/hud reload` pour choisir une autre hauteur à gauche. Aucun scoreboard/sidebar.

**Le ZIP items fourni est complet pour ses objets et glyphes statiques.** Le pack shader HUD
final doit être généré et fusionné par BetterHud sur la version effectivement utilisée.
La présence des sprites/font dans un ZIP ne constitue pas à elle seule un HUD dynamique.
Aucun pack généré BetterHud compatible avec NitroServ n'a été validé ici.

## Commandes

```text
/mb hud mode betterhud|bossbar|custom|off
/mb hud show|hide|refresh|debug|reload
/mb hud toggle goal|percent|multiplier|icon
/mb hud multiplier 1
/mb hud multiplier 2
/mb progress set 999
/mb progress add 1
/mb reload config
```

`custom` conserve l'ancien mode ActionBar. `bossbar` sert au diagnostic.
`hud.custom-fallback-bossbar: false` masque le fallback en mode betterhud si l'API manque.
`challenge.target-blocks` définit l'objectif ; `hud.show-*` définit les options initiales.
Le x2 double les nouveaux gains, pas l'historique ; `/mb progress set` demeure une valeur absolue.
`/mb hud reload` recharge seulement le service MillionBlocks. `/hud reload` recharge les
YAML/images de BetterHud ; les deux commandes sont volontairement distinctes.
Les items déjà distribués en V0.5.1 gardent leurs anciennes métadonnées : réémettre avec
`/mb give all` et `/mb portal give` pour tester leurs modèles V0.6.

## Fusion reproductible du pack généré

Une fois le pack BetterHud réellement généré sur la version cible :

```bash
python3 scripts/merge_resourcepacks.py \
  --betterhud /chemin/BetterHud-generated.zip \
  --items dist/MillionBlocks-items-0.6.0.zip \
  --output /chemin/MillionBlocks-BetterHud-final.zip
```

L'outil conserve les shaders/overlays et le `pack.mcmeta` de BetterHud, exige la compatibilité
format 88, refuse tout conflit de ressource différent, les ZIP à chemins dangereux et un
fichier de sortie existant. Il ne résout pas automatiquement les conflits de fonts/shaders
avec d'autres plugins. Ses tests utilisent des packs factices ; aucun pack généré NitroServ
n'a été disponible pendant cette session.
