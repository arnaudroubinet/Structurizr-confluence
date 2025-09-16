# Guide d’utilisation de l’agent Copilot

Ce dépôt est configuré pour être utilisé avec un agent Copilot (mode autonome) et Copilot Chat (mode local).

## Règles d’exécution

- Mode autonome (agent):
  - Toujours exécuter `mvn -B --no-transfer-progress clean install` avant de rendre la main (à la fin de la session).
  - Utiliser le script `scripts/build.sh` quand il est présent pour garantir une exécution homogène.
  - En cas d’échec build/test, tenter jusqu’à 3 corrections ciblées puis rapporter l’erreur avec la sortie Maven pertinente.

- Mode local (chat):
  - Proposer d’exécuter `mvn clean install` avant de finaliser des changements significatifs. Si l’utilisateur n’exige pas l’exécution, poursuivre, mais noter que les vérifications locales n’ont pas été faites.

## Conventions de branches et PR

- Préfixe de branche: `copilot/`.
- Toujours ouvrir une Pull Request vers `main` en décrivant:
  - Le but.
  - Les fichiers modifiés.
  - Le résultat des tests (`mvn -B clean install`).

## Qualité et sécurité

- Respecter Java 21 (voir `pom.xml`).
- Ne pas introduire de dépendances non nécessaires. Utiliser les versions définies dans les propriétés du `pom.xml`.

## Commandes standard

- Build: `scripts/build.sh`
- Tests uniquement: `mvn -B --no-transfer-progress -DskipITs=false test`

## Environnements

- VS Code est configuré pour Java 21 via `.vscode/settings.json` et tâches dans `.vscode/tasks.json`.
