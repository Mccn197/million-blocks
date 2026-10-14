# Snapshot Survival — hors ligne uniquement

`python3 scripts/world_snapshot.py` sauvegarde/restaure un dossier de monde existant.
Le script n'est jamais exécuté automatiquement, ni au démarrage du plugin, ni par une
commande en jeu. Aucun monde réel n'a été sauvegardé, supprimé ou restauré pendant ce travail.

## Conditions

- Serveur arrêté, tous les mondes déchargés, redémarrage automatique désactivé.
- Accès local Linux/Python 3 au dossier du serveur ; jamais sur une copie encore ouverte
  par le serveur. Le script refuse le verrou POSIX `session.lock` utilisé par Java et un
  processus Java dont le répertoire courant est celui du serveur.
- Maintenir cet arrêt pendant toute l'opération : le script ne peut pas interdire à un
  administrateur/gestionnaire externe de relancer le serveur au milieu d'une restauration.
- Sauvegarder le serveur complet via l'hébergeur en plus du snapshot du monde.
- Choisir les chemins explicitement ; vérifier le nom Survival actif avec `/mb survival status`
  avant l'arrêt. Les dimensions/répertoires supplémentaires doivent être sauvegardés aussi.
- Le dossier de snapshot et celui de secours doivent rester hors du monde, sans chevauchement.
  Le secours doit être sur le même système de fichiers pour le déplacement atomique.
- Si NitroServ ne fournit pas de shell/Python : utiliser ses sauvegardes à l'arrêt et exporter
  les dossiers de mondes. Ne pas présenter le script local comme une restauration distante.

## Sauvegarder la Survival vierge

Exemple sur un **serveur de test arrêté** ; adapter tous les chemins :

```bash
python3 scripts/world_snapshot.py save \
  --world /srv/test/survival \
  --snapshot /srv/backups/survival-pristine-v06 \
  --confirm OFFLINE:survival
python3 scripts/world_snapshot.py verify \
  --snapshot /srv/backups/survival-pristine-v06
```

`level.dat` et un `session.lock` régulier existant sont obligatoires. Le manifeste recense
SHA-256 de chaque fichier, excepté le verrou. Le contenu copié et la source sont revérifiés.
Une destination existante, un lien symbolique ou un monde verrouillé provoque un refus.

## Restaurer après les tests destructifs

Arrêter de nouveau le serveur et ses redémarrages automatiques. Faire d'abord une sauvegarde
hébergeur. **Seul l'administrateur autorisé doit exécuter la confirmation suivante.**

```bash
python3 scripts/world_snapshot.py restore \
  --world /srv/test/survival \
  --snapshot /srv/backups/survival-pristine-v06 \
  --rescue /srv/backups/survival-before-restore-v06 \
  --confirm OFFLINE:survival
```

Le snapshot est vérifié, copié dans un staging puis vérifié à nouveau. Le monde courant est
**déplacé entier** vers `rescue/world`, jamais effacé. Le staging devient le monde actif.
Le snapshot initial est conservé. Le secours reçoit aussi un manifeste SHA-256 et peut être
vérifié avec `verify`. Un échec laisse les données récupérables dans le staging actif ou le
secours ; consulter le message et les dossiers avant tout redémarrage. Aucun écrasement
silencieux d'un secours existant. La suppression de vieux snapshots/secours est toujours
une opération administrative séparée, absente du script.

Cette opération restaure le terrain et les données dans le dossier du monde. Elle ne réinitialise
pas `plugins/MillionBlocks/progress.yml`, les autres plugins ni les dossiers externes de joueurs.
Décider explicitement si leur état doit aussi être restauré pour une nouvelle prise vidéo.

Tests locaux : round-trip, conservation du monde détruit, altération de checksum, verrou
concurrent, liens symboliques, destination existante et confirmation manquante. Mondes factices
uniquement ; aucune validation sur une Survival réelle ni sur NitroServ.
