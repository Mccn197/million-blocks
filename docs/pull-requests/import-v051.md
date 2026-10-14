Importe les sources V0.5.1 et les fichiers du handoff V0.6 dans une branche issue de `dev`.
Les prototypes et le JAR V0.5.1 sont conservés comme références, sans annoncer une V0.6 validée.

Validation : comparaison avec le contenu du ZIP, inspection du checkout et de la branche de base.
La CI GitLab importée n'accepte que les déclenchements manuels ; aucun workflow GitHub Actions
n'est ajouté. Aucun serveur distant n'est contacté et aucun déploiement n'est effectué.

Base : `dev`. Branche : `feature/import-v051`. Ne pas fusionner sur `main`.
