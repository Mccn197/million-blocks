# Développement local V0.6

Voir [BUILD-V06.md](BUILD-V06.md) pour Java/Maven et les limites de la compilation source API.
Voir [INSTALLATION-V06.md](INSTALLATION-V06.md) pour la checklist complète.

Après accès aux téléchargements Paper et avec Java 25 actif, `scripts/setup-local.sh`
ou `scripts/setup-local.ps1` prépare exclusivement Paper **26.2 build 121**, vérifie son SHA-256,
refuse de remplacer un JAR existant et ne démarre rien. L'EULA n'est pas acceptée automatiquement.
Le téléchargement n'a pas été validé dans cette session (accès réseau bloqué) ; le script
PowerShell n'a pas été exécuté. Aucun autre build n'est choisi silencieusement.

Après compilation exacte et revue de l'EULA, installer manuellement le JAR dans ce serveur
local puis démarrer `local/server/start.sh`. Utiliser des copies de mondes et suivre la
checklist HUD/armes/snapshot. Les processus ne persistent pas après une restauration cloud.
