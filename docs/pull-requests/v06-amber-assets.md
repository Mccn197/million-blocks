Le HUD V0.5.1 utilisait l'ActionBar et ses templates BetterHud n'avaient aucune liaison au score.
Cette V0.6 ajoute les placeholders natifs BetterHud Paper et un HUD Amber à gauche avec
compteur live, fissuration, options et x1/x2 effectif. Elle ajoute neuf modèles éditables
plus détaillés, les définitions item modernes, un portail industriel, un poste REDLINE à
siège/tourelle et des effets Display Entities pour le foret, TNT et World Eater.

La sauvegarde Survival est un outil hors ligne : verrou POSIX, checksums, confirmation,
staging et conservation du monde remplacé. La progression des destructions massives est
publiée à chaque lot et les jobs s'arrêtent à la déconnexion/changement de monde.

Validation : compilation Java 25 contre les véritables sources officielles Paper API 26.2,
8 tests JUnit + 16 tests Python réussis, PNG/JSON/YAML/références contrôlés, génération
reproductible. La cible Maven normale reste Paper 26.2 build 121.

Limites : l'artefact exact build 121 est inaccessible (403 du proxy). Aucun serveur Paper
ni client Minecraft n'a été lancé ; BetterHud/NitroServ et le pack shader final restent
à valider. REDLINE est un poste fixe, sans conduite WASD. Voir la checklist et les résultats
V0.6 avant déploiement. Aucun déploiement/CI distant ni suppression de monde réel.

Base : `dev`. Branche : `feature/v06-amber-assets`, qui comprend l'import V0.5.1.
Revoir l'import d'abord ou utiliser cette PR comme livraison complète. Ne pas fusionner sur `main`.
