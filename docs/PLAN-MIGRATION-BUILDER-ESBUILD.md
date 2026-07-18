# PLAN — Migration Builder Angular : Webpack → esbuild (`@angular/build:application`)

> Statut : **Proposition initiale**
> Contexte : Angular 22 déprécie `@angular-devkit/build-angular:browser` (webpack) au profit de `@angular/build:application` (esbuild). Le projet utilise `@angular-builders/custom-webpack:browser`, qui étend le builder deprecated, d'où le warning au build.

---

## 1. Contexte

### 1.1 Le warning

```
The "@angular-devkit/build-angular:browser" builder is deprecated as part of
Angular's Webpack support deprecation. Use "@angular/build:application" instead.
For more information, see https://angular.dev/tools/cli/build-system-migration.
```

- Émis à chaque build (dev, prod, tauri, electron).
- Ne bloque rien pour l'instant. Le builder webpack **fonctionne encore** sur Angular 22.x.
- Angular publie le calendrier de retrait progressif : deprecation annoncée en v22, retrait effectif attendu Angular 23/24 (mi/fin 2026).

### 1.2 Situation actuelle

`angular.json` :

```json
"architect": {
  "build": {
    "builder": "@angular-builders/custom-webpack:browser",
    "options": {
      "customWebpackConfig": { "path": "./webpack/webpack.custom.js" },
      …
    }
  },
  "serve": {
    "builder": "@angular-builders/custom-webpack:dev-server",
    …
  }
}
```

`@angular-builders/custom-webpack@22.0.1` — dernière version disponible, aligne le peer sur Angular 22.

### 1.3 Objectifs

| Objectif | Métrique | Cible |
|---|---|---|
| Retirer le warning au build | Occurrences du warning | 0 |
| Passer sur `@angular/build:application` | `angular.json` → builder | `@angular/build:application` |
| Retirer `@angular-builders/custom-webpack` | `package.json` | Retiré |
| Retirer `webpack` explicite | `package.json` | Retiré |
| Gain vitesse build dev | `webapp:build:dev` cold | −40 à −70 % |
| Gain vitesse build prod | `webapp:build:prod` cold | −20 à −40 % |
| Constantes globales inchangées | `I18N_HASH`, `__VERSION__`, `SERVER_API_URL` accessibles | Fonctionnel |
| Fusion i18n JSON `fr` + `en` | Fichiers `i18n/fr.json` et `i18n/en.json` générés | Fonctionnel |

### 1.4 Non-objectifs

- Passer les tests Jest au nouveau `@angular/build:unit-test` (Vitest / Karma-less). Hors scope.
- Toucher au bundler Tauri (Rust côté). La config `angular.json > tauri` reste, seul le builder change.
- Refonte de l'organisation des i18n JSON (les 59 fichiers `fr/*.json` et 42 `en/*.json` restent tels quels).

---

## 2. Inventaire — ce que fait le webpack custom actuel

Cinq responsabilités dans `webpack/webpack.custom.js` :

| # | Rôle | Détails |
|---|---|---|
| 1 | **Fusion i18n JSON** | `MergeJsonWebpackPlugin` : concatène `pharmaSmart-app/src/main/webapp/i18n/fr/*.json` → `<output>/i18n/fr.json` (idem pour `en`). Consommé par `@ngx-translate/http-loader` |
| 2 | **Hash i18n** | `folder-hash` sur le dossier i18n → variable `I18N_HASH` (sert au cache-busting côté runtime) |
| 3 | **Constantes globales** | `DefinePlugin` : `I18N_HASH`, `__VERSION__` (depuis `package.json`), `SERVER_API_URL` (empty au build) |
| 4 | **Notifier dev** | `WebpackNotifierPlugin` : toast desktop à chaque build dev |
| 5 | **Analyseur bundle prod** | `BundleAnalyzerPlugin` : `stats.html` à côté du bundle |

Plus, côté `devServer` :
- Proxy vers backend Spring (`localhost:9080`) sur `/api`, `/services`, `/management`, `/v3/api-docs`, `/h2-console`, `/health`
- HMR + liveReload activés

### 2.1 Consommateurs des constantes

- **116 fichiers** référencent `I18N_HASH`, `__VERSION__` ou `SERVER_API_URL` (services API, config, environments)
- Déclarés dans `pharmaSmart-app/src/main/webapp/declarations.d.ts` comme `declare const`

---

## 3. Table de portage — mapping webpack → esbuild

`@angular/build:application` utilise esbuild ; les plugins webpack ne fonctionnent pas. Chaque responsabilité est portée différemment :

| Webpack (actuel) | esbuild / `@angular/build:application` | Difficulté |
|---|---|---|
| `MergeJsonWebpackPlugin` | Script Node **pre-build** dans `scripts/build-i18n.js` qui écrit `pharmaSmart-app/src/main/webapp/i18n/{fr,en}.json`, référencés ensuite comme `assets` | 🟠 Moyenne |
| `folder-hash` + `DefinePlugin` `I18N_HASH` | Même script pre-build : calcule le hash, l'écrit dans un fichier `.i18n-hash.ts` (module ambient) importé par les consommateurs, OU passé via `define` du builder | 🟢 Facile |
| `DefinePlugin` `__VERSION__` | Option native `define` du builder | 🟢 Facile |
| `DefinePlugin` `SERVER_API_URL` | Option native `define` (statique) OU env-var au build | 🟢 Facile |
| `WebpackNotifierPlugin` | Retiré. Optionnel : script `chokidar` externe si nostalgique | 🟢 Trivial |
| `BundleAnalyzerPlugin` | Post-build : `esbuild-visualizer` sur les métafiles ; ou `source-map-explorer` sur les `.js.map` | 🟠 Moyenne |
| Proxy dev (`proxy.conf.js`) | Option native `proxyConfig` — accepte un JSON ou un JS avec la même API | 🟢 Facile |
| HMR / liveReload | Défaut activé | 🟢 Trivial |
| `allowedCommonJsDependencies` | Option native identique | 🟢 Facile |
| `customWebpackConfig.path` | ❌ Supprimé | — |

---

## 4. Portage détaillé, étape par étape

### 4.1 Étape 1 — Script de génération i18n

Créer `scripts/build-i18n.js` :

```js
#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const { hashElement } = require('folder-hash');

const ROOT = path.resolve(__dirname, '..', 'pharmaSmart-app', 'src', 'main', 'webapp', 'i18n');
const LANGS = ['fr', 'en'];

async function main() {
  const hash = (await hashElement(ROOT, {
    algo: 'md5',
    encoding: 'hex',
    files: { include: ['*.json'] },
  })).hash;

  for (const lang of LANGS) {
    const dir = path.join(ROOT, lang);
    if (!fs.existsSync(dir)) continue;
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.json'));
    const merged = {};
    for (const f of files) {
      const key = path.basename(f, '.json');
      merged[key] = JSON.parse(fs.readFileSync(path.join(dir, f), 'utf8'));
    }
    fs.writeFileSync(path.join(ROOT, `${lang}.json`), JSON.stringify(merged));
  }

  // Emit hash file for the builder to pick up via `define`
  fs.writeFileSync(
    path.join(__dirname, '..', 'target', '.i18n-hash.json'),
    JSON.stringify({ hash })
  );
  console.log(`i18n merged (${LANGS.join(', ')}) — hash=${hash}`);
}

main().catch(err => { console.error(err); process.exit(1); });
```

Ajouter dans `.gitignore` : `pharmaSmart-app/src/main/webapp/i18n/fr.json` et `en.json` (générés).

Ajouter dans `package.json` scripts :

```json
"scripts": {
  "build:i18n": "node scripts/build-i18n.js",
  "prewebapp:build:dev": "npm run build:i18n",
  "prewebapp:build:prod": "npm run build:i18n",
  "prewebapp:build:tauri": "npm run build:i18n",
  "prestart": "npm run build:i18n"
}
```

### 4.2 Étape 2 — Assets i18n

Dans `angular.json > options.assets`, ajouter les JSON générés :

```json
"assets": [
  "pharmaSmart-app/src/main/webapp/content",
  "pharmaSmart-app/src/main/webapp/favicon.ico",
  "pharmaSmart-app/src/main/webapp/manifest.webapp",
  "pharmaSmart-app/src/main/webapp/robots.txt",
  {
    "glob": "*.json",
    "input": "pharmaSmart-app/src/main/webapp/i18n",
    "output": "i18n"
  }
]
```

L'input `@ngx-translate/http-loader` continuera à charger `/i18n/fr.json` comme aujourd'hui.

### 4.3 Étape 3 — Constantes globales via `define`

Le builder `@angular/build:application` expose une option `define` équivalente à `DefinePlugin` :

```json
"options": {
  "define": {
    "__VERSION__": "\"0.0.1\"",
    "SERVER_API_URL": "\"\"",
    "I18N_HASH": "\"__I18N_HASH_PLACEHOLDER__\""
  }
}
```

Les valeurs doivent être des **strings JSON valides** — noter les guillemets échappés.

Pour le `I18N_HASH` dynamique : générer `angular.json` juste-à-temps ne marche pas proprement. Deux options :

**Option A (recommandée) — Wrapper de script au lieu de `ng build` direct**

Un mini script `scripts/build-app.js` remplace l'appel direct au builder :

```js
#!/usr/bin/env node
const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// 1. Génère i18n + hash
require('./build-i18n.js');   // OU: execSync('node scripts/build-i18n.js', { stdio: 'inherit' });

// 2. Lit le hash pour l'injecter en env-var interprétée par angular.json
const { hash } = JSON.parse(fs.readFileSync(path.resolve(__dirname, '..', 'target', '.i18n-hash.json'), 'utf8'));

// 3. Appelle ng build avec --define en surcharge (Angular CLI supporte les flags CLI dynamiques)
const configuration = process.argv[2] || 'production';
execSync(
  `ng build --configuration ${configuration} --define="I18N_HASH=\\"${hash}\\""`,
  { stdio: 'inherit', shell: true }
);
```

Package scripts :

```json
"webapp:build:dev":  "node scripts/build-app.js development",
"webapp:build:prod": "node scripts/build-app.js production",
"webapp:build:tauri": "node scripts/build-app.js tauri"
```

**Option B — Import du hash depuis un fichier généré**

`scripts/build-i18n.js` écrit :

```ts
// pharmaSmart-app/src/main/webapp/environments/i18n-hash.ts (généré, gitignoré)
export const I18N_HASH = 'ab12cd34…';
```

Les consommateurs remplacent `I18N_HASH` (const global) par `import { I18N_HASH } from 'environments/i18n-hash'` — **116 fichiers concernés**. Rejeté (trop invasif).

### 4.4 Étape 4 — Configuration `angular.json`

Substituer builder + options :

```json
"architect": {
  "build": {
    "builder": "@angular/build:application",
    "options": {
      "outputPath": {
        "base": "pharmaSmart-app/target/classes/static/",
        "browser": ""
      },
      "index": "pharmaSmart-app/src/main/webapp/index.html",
      "browser": "pharmaSmart-app/src/main/webapp/main.ts",
      "polyfills": ["zone.js"],
      "tsConfig": "tsconfig.app.json",
      "inlineStyleLanguage": "scss",
      "allowedCommonJsDependencies": ["file-saver", "dayjs"],
      "assets": [ … ],
      "styles": [ … ],
      "scripts": [],
      "define": {
        "__VERSION__": "\"0.0.1\"",
        "SERVER_API_URL": "\"\"",
        "I18N_HASH": "\"__I18N_HASH_PLACEHOLDER__\""
      }
    },
    "configurations": {
      "production": {
        "optimization": true,
        "outputHashing": "all",
        "sourceMap": false,
        "extractLicenses": true,
        "serviceWorker": "ngsw-config.json",
        "budgets": [ … ]
      },
      "tauri": {
        "optimization": { "scripts": true, "styles": { "minify": true, "inlineCritical": false }, "fonts": true },
        "outputHashing": "all",
        "sourceMap": false,
        "extractLicenses": true,
        "serviceWorker": false,
        "baseHref": "./",
        "fileReplacements": [
          { "replace": "pharmaSmart-app/src/main/webapp/environments/environment.ts",
            "with":    "pharmaSmart-app/src/main/webapp/environments/environment.tauri.ts" }
        ],
        "budgets": [ … ]
      },
      "development": {
        "optimization": false,
        "extractLicenses": false,
        "sourceMap": true,
        "fileReplacements": [
          { "replace": "pharmaSmart-app/src/main/webapp/environments/environment.ts",
            "with":    "pharmaSmart-app/src/main/webapp/environments/environment.development.ts" }
        ]
      }
    },
    "defaultConfiguration": "production"
  },
  "serve": {
    "builder": "@angular/build:dev-server",
    "options": {
      "buildTarget": "warehouse:build:development",
      "port": 4200,
      "proxyConfig": "webpack/proxy.conf.js"
    },
    "configurations": {
      "production": { "buildTarget": "warehouse:build:production" },
      "development": { "buildTarget": "warehouse:build:development" }
    },
    "defaultConfiguration": "development"
  }
}
```

**Points d'attention** :

- `main` → `browser` (rename obligatoire)
- `outputPath` devient un objet `{ base, browser }` (la sortie déplace `main.js` et compagnie sous `<base>/<browser>`)
- `vendorChunk`, `namedChunks`, `buildOptimizer` — options webpack-only, à **retirer**
- `serviceWorker` : booléen → chemin string ou `false`
- `configuration.electron` retirée si obsolète (aucune preuve d'usage dans le repo aujourd'hui)
- `proxyConfig` accepte directement le chemin du fichier JS (l'ancien `proxy.conf.js` fonctionne tel quel, exporte une fonction)

### 4.5 Étape 5 — Analyseur bundle

`BundleAnalyzerPlugin` non disponible. Deux remplaçants :

**esbuild-visualizer** (recommandé) — installer en devDep, ajouter le flag `--stats-json` au build, puis lancer :

```bash
npx esbuild-visualizer --metadata pharmaSmart-app/target/classes/static/stats.json --filename stats.html
```

Script package :

```json
"webapp:analyze": "node scripts/build-app.js production --stats-json && esbuild-visualizer --metadata pharmaSmart-app/target/classes/static/stats.json --filename stats.html"
```

### 4.6 Étape 6 — Nettoyage dépendances

Retirer de `package.json > devDependencies` :

```
@angular-builders/custom-webpack
@angular-devkit/build-angular   ← non retiré, il reste transitivement pour d'autres outils si besoin, mais on peut essayer
copy-webpack-plugin
folder-hash                     ← retiré uniquement si le script build-i18n.js le résout via require normal
merge-jsons-webpack-plugin
webpack
webpack-bundle-analyzer
webpack-merge
webpack-notifier
```

Ajouter :
```
esbuild-visualizer      ← si Étape 5 retenue
```

Retirer `webpack/webpack.custom.js` et `webpack/environment.js`. Conserver `webpack/proxy.conf.js` (utilisé par `serve.proxyConfig`) — le renommer éventuellement `scripts/proxy.conf.js`.

---

## 5. Points de vigilance

### 5.1 CommonJS interop

Le mode esbuild est **plus strict** sur les modules CJS. Les libs déjà déclarées dans `allowedCommonJsDependencies` (`file-saver`, `dayjs`) doivent le rester. À surveiller pendant la migration : `chart.js`, `bootstrap`, `bootswatch`, `gridstack`, `buffer` — peuvent nécessiter d'y être ajoutés selon le comportement observé.

### 5.2 Sass `@import` deprecated

Le nouveau builder passe par **Dart Sass moderne**. Les warnings `@import`, `darken()`, `lighten()` déjà visibles avec webpack deviendront plus stridents ; certains scénarios peuvent devenir bloquants sur Dart Sass 3.0. Ce n'est **pas un nouveau problème** — mais à traiter en parallèle (chantier séparé).

### 5.3 Ordre des styles

esbuild peut concaténer les styles dans un ordre légèrement différent de webpack. Vérifier visuellement :
- `vendor.scss` charge `bootstrap` et `bootswatch/yeti` avant `global.scss`
- Les `@import` dans les `styleUrl` de composants doivent rester scopés

### 5.4 Injection des constantes globales dans les tests

Jest passe par `@angular-builders/jest` qui utilise **encore webpack via ts-jest** (indirectement). Les `declare const I18N_HASH` continuent à fonctionner en jest via une injection dans `jest-globals` ou `setup-jest.ts`. À vérifier après migration — vraisemblablement rien à changer.

### 5.5 Tauri

Le build Tauri consomme le résultat de `ng build --configuration tauri` (dossier `pharmaSmart-app/target/classes/static/`). Le contrat de sortie reste identique tant qu'`outputPath.base` est préservé. **Aucun impact prévu** côté Rust / Cargo.

### 5.6 HMR sur Angular 22

Le HMR (hot module replacement) est **activé par défaut** sur `@angular/build:dev-server`. Le flag `--hmr` du script `start` devient inutile ; le retirer :

```json
"start": "ng serve"
```

au lieu de `"ng serve --hmr"`.

---

## 6. Stratégie de migration — phasage

### Phase 1 — Sur branche dédiée, iso-fonctionnel (1 sprint)

Créer `feat/builder-esbuild`. Objectif : substitution builder + parité fonctionnelle stricte.

- **Jour 1** : `scripts/build-i18n.js` + `prewebapp:*` hooks + `.gitignore`. Vérifier que `i18n/fr.json` et `en.json` générés sont identiques à ceux produits par MergeJsonWebpackPlugin
- **Jour 2** : `scripts/build-app.js` + modif `angular.json` — d'abord la configuration `development` uniquement. Test `npm start` (dev + proxy)
- **Jour 3** : configurations `production` et `tauri` — test `webapp:build:prod` et `tauri:build:bundled-jre`
- **Jour 4** : Nettoyage `package.json`, retrait des plugins webpack. Test complet: dev, prod, tauri
- **Jour 5** : Remplacement `BundleAnalyzerPlugin` par `esbuild-visualizer`, script `webapp:analyze`. QA visuelle sur 4-5 écrans emblématiques

### Phase 2 — Vérification en staging (0.5 sprint)

- Déploiement staging avec le nouveau builder
- Batterie de tests smoke : login, POS, création commande, impression ticket, exports PDF/Excel, i18n (bascule fr → en), dashboard graphs
- Comparaison bundle size et temps de build vs baseline webpack

### Phase 3 — Merge et retrait webpack (0.25 sprint)

- Merge dans `master`
- Suppression des fichiers `webpack/webpack.custom.js`, `webpack/environment.js`
- Suppression `webpack.custom.js` de la doc CLAUDE.md si mentionné
- Communication interne : nouvelle procédure de build, changelog des scripts npm

**Total** : **~2 sprints** (~1 mois calendaire).

---

## 7. Risques et compensations

| Risque | Probabilité | Impact | Compensation |
|---|---|---|---|
| Régression i18n (JSON mal fusionnés) | Moyenne | Haut | Diff binaire des JSON générés avant/après la migration sur `fr` et `en` avant de merger |
| `SERVER_API_URL` non injecté correctement | Basse | Haut | Test smoke immédiat : ouvrir la console réseau, vérifier que les requêtes API partent vers `/api/…` |
| Modules CJS non listés dans `allowedCommonJsDependencies` | Moyenne | Moyen | Les warnings du build listent explicitement les modules à ajouter |
| Différence de taille bundle prod | Basse | Moyen | esbuild génère typiquement des bundles légèrement plus petits ; si régression, activer `sourceMap: true` pour investiguer |
| Bug HMR ou proxy dev | Basse | Bas | Rollback rapide sur `master` — la migration est un merge unique |
| Régression Tauri (chemins d'assets) | Moyenne | Haut | Tester `tauri:build:bundled-jre:debug` avant merge |
| Perte du notifier desktop | Certaine | Trivial | Assumé — pas de remplacement, feature marginale |
| `--define="I18N_HASH=…"` casser sur Windows PowerShell | Moyenne | Bas | Le script `build-app.js` normalise l'échappement ; tester sur PowerShell + bash |
| Divergence entre `angular.json > options.define` et le hash dynamique | Faible | Moyen | Le placeholder `__I18N_HASH_PLACEHOLDER__` est repéré facilement dans le bundle si oubli |

---

## 8. Ce qui **ne change pas**

- Le contenu des composants Angular
- `@angular-builders/jest` reste (les tests continuent via Jest — chantier séparé pour passer à `@angular/build:unit-test` si souhaité)
- Le proxy dev (`proxy.conf.js`) reste tel quel, reçoit un chemin string
- Les configurations d'environnement (`environment.ts`, `environment.tauri.ts`, `environment.development.ts`)
- Tauri, PostgreSQL, Spring Boot — aucun impact
- La structure des i18n (`i18n/fr/*.json`, `i18n/en/*.json`)
- Les 116 fichiers qui référencent `I18N_HASH`, `__VERSION__`, `SERVER_API_URL` — leur syntaxe `declare const` reste opérationnelle via `define`

---

## 9. Livrables

- ✅ Ce plan (`docs/PLAN-MIGRATION-BUILDER-ESBUILD.md`)
- 🔲 `scripts/build-i18n.js` — merge JSON + hash
- 🔲 `scripts/build-app.js` — wrapper `ng build` avec injection du hash dynamique
- 🔲 `angular.json` — nouveau builder + define
- 🔲 `.gitignore` — exclusion des `i18n/{fr,en}.json` générés
- 🔲 `package.json` — scripts renommés + retrait des devDeps webpack + ajout `esbuild-visualizer`
- 🔲 Suppression `webpack/webpack.custom.js` et `webpack/environment.js`
- 🔲 Note dans `CLAUDE.md` sur la nouvelle chaîne de build

---

## 10. Timing recommandé

**Ne pas lancer avant** :
- La stabilisation post-upgrade Angular 22 (déjà OK — le build v22 passe)
- Le début du chantier PrimeNG → ng-bootstrap (cf. `PLAN-MIGRATION-PRIMENG-VERS-NGBOOTSTRAP.md`)

**Bonne fenêtre** : intégrer comme **Phase 0.5** du chantier PrimeNG. Ça consolide l'infra front avant de toucher aux composants — bénéfice de temps de build sur toute la durée du chantier PrimeNG.

**Report acceptable** : jusqu'à mi-2026 sans risque, tant qu'Angular 22 est notre version cible.

---

## 11. Décisions ouvertes

1. **Timing** — intégrer en Phase 0.5 du chantier PrimeNG (recommandé) ou plan indépendant à démarrer immédiatement ?
2. **`I18N_HASH` dynamique** — Option A (wrapper `build-app.js` avec `--define`) ou Option B (import depuis fichier généré) ? Recommandé : A.
3. **`webpack-bundle-analyzer` → `esbuild-visualizer`** — remplacement direct ou on met en pause l'outillage d'analyse pendant la migration ?
4. **`WebpackNotifierPlugin`** — assumer la perte définitive, ou script watcher externe (`chokidar` + `node-notifier`) équivalent ?
