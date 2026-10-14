# Compilation et environnement cloud

Les tâches cloud sont déjà isolées : utiliser le checkout existant, ne pas créer de worktree
sauf demande explicite. Les outils et caches sont hors dépôt dans `/workspace/toolchain`.

## Outils

```bash
scripts/bootstrap-tools.sh
python3 -m venv /workspace/toolchain/asset-venv
/workspace/toolchain/asset-venv/bin/python -m pip install -r requirements-dev.txt
```

Le bootstrap utilise Temurin **25.0.2+10** et Maven **3.9.11**, téléchargés auprès de leurs
sources officielles avec vérification SHA-256/SHA-512. Le proxy existant est repris sans
imprimer ni enregistrer de credentials. Maven utilise le magasin de confiance Java du
système ; aucune désactivation TLS/signature/checksum.
Python 3.12+ est nécessaire au filtre d'extraction sécurisé des archives.

## Cible demandée : Paper 26.2 build 121

```bash
scripts/validate.sh
```

Le POM conserve `io.papermc.paper:paper-api:26.2.build.121-stable` en `provided`,
`maven.compiler.release=25`. Cette commande doit aboutir avant de déclarer la cible exacte
validée. Dans cette session, le proxy a répondu 403 au dépôt Paper ; l'échec s'est produit
avant la compilation. Ce n'est pas une erreur de sources Java démontrée.

## Secours explicitement utilisé pour le JAR candidat

```bash
scripts/build-source-api.sh
scripts/validate.sh --source-api
```

Le secours compile **les véritables sources complètes** de Paper API 26.2, y compris les
sources générées du dépôt officiel, avec Maven et Java 25. Ce ne sont ni stubs ni MockBukkit.
Les pins sont :

- Paper : `58e6e45d67c998909581f9079189ef51eb6f1267`, branche officielle `ver/26.2`.
- Brigadier : `9ba4f13c0fe82b07c08c2dc2d8043f075ffd0d98`, dépôt officiel Mojang.
- Dépendances du recipe Maven `scripts/source-api-pom.xml`, issues de Maven Central.
  BungeeCord Chat est la version publiée `1.21-R0.4` : ce n'est pas l'artefact
  `1.21-R0.2-deprecated+build.21` utilisé par le build upstream.

L'API locale a la version distincte `26.2-source-58e6e45`. Le build MillionBlocks passe
`-Dpaper.version=26.2-source-58e6e45`, sans modifier le POM cible. Aucune API n'est incluse
ou ombrée dans le JAR MillionBlocks. Ce procédé vérifie la compilation des 17 classes
contre une API réelle 26.2 ; il ne prouve pas la correspondance binaire exacte avec build 121,
ni le démarrage de Paper ou l'intégration BetterHud.

## Tests

- JUnit/Maven : compteur, options, limites, x2/saturation, cycle de fissuration,
  YAML du plugin via le parser Paper, mapping des items modernes.
- Python : PNG/JSON/YAML, ressources référencées, UV, géométrie, rigs, conditions BetterHud,
  pack format 88.0 et snapshot hors ligne.
- `git diff --check` : qualité du diff.

Pour régénérer les assets, voir [ASSETS-V06.md](ASSETS-V06.md).
Pour les tests avec client, voir [INSTALLATION-V06.md](INSTALLATION-V06.md).

## Accès restant à activer

Les domaines Paper, Adoptium, GitHub API et Minecraft ont été ajoutés au **brouillon**
de configuration cloud. Un enregistrement de brouillon ne les active pas dans cette machine.
Revoir/enregistrer les paramètres puis publier l'environnement pour une nouvelle version.
Le dépôt Git fonctionne déjà via l'authentification HTTPS de la plateforme.
La variable `GH_TOKEN` est présente, mais les requêtes GitHub API/GraphQL ont été refusées.
Ne pas ajouter un second token sur la seule base de cet échec : vérifier d'abord les accès
et la portée de la liaison existante. Aucune valeur de credential ne doit être mise en chat.
