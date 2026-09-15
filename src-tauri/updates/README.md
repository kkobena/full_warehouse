# `src-tauri/updates/` — relais de mise à jour des postes clients

Ce répertoire est embarqué comme ressource dans les **installeurs serveur**
(`tauri:build:bundled` et `tauri:build:bundled-jre`). Son contenu est ensuite déposé par
`NSIS_HOOK_POSTINSTALL` dans `%PROGRAMDATA%\PharmaSmart\updates\`, où le backend le publie
aux postes clients de l'officine via `/api/updates/**`.

## Ce qu'on y met

Rien manuellement. `npm run tauri:prepare-client-update` y copie l'exécutable **client**
produit par `npm run tauri:build`, renommé `pharmasmart-<version>.exe` — le numéro dans le
nom est ce qui permet au backend d'annoncer la version disponible.

Le catalogue ne conserve qu'une version : le script efface les artefacts précédents avant
de copier, pour que l'installeur serveur ne grossisse pas à chaque release.

## Pourquoi ce fichier existe

Il garantit que le répertoire n'est jamais vide, pour que la ressource déclarée dans les
configurations Tauri corresponde toujours à au moins un fichier — y compris quand aucune
mise à jour n'est publiée.

## Sans mise à jour publiée

C'est un état parfaitement normal, et c'est le comportement par défaut : l'installeur
serveur se construit, NSIS ne dépose aucun artefact, le backend répond `204 No Content` à
toute demande de mise à jour, et les postes clients restent mis à jour par dépôt manuel de
`pharmasmart.exe`.
