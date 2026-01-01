# Plan de Migration Angular 20 vers 21 et PrimeNG 20 vers 21

## Sommaire

1. [Vue d'ensemble](#vue-densemble)
2. [Breaking Changes majeurs](#breaking-changes-majeurs)
3. [Prerequis](#prerequis)
4. [Etape 1 : Preparation](#etape-1--preparation)
5. [Etape 2 : Migration Angular 21](#etape-2--migration-angular-21)
6. [Etape 3 : Migration PrimeNG 21](#etape-3--migration-primeng-21)
7. [Etape 4 : Mise a jour des dependances](#etape-4--mise-a-jour-des-dependances)
8. [Etape 5 : Migrations automatiques](#etape-5--migrations-automatiques)
9. [Etape 6 : Adaptations manuelles](#etape-6--adaptations-manuelles)
10. [Etape 7 : Tests et Validation](#etape-7--tests-et-validation)
11. [Package.json final](#packagejson-final)
12. [Rollback](#rollback)

---

## Vue d'ensemble

| Package | Version actuelle | Version cible |
|---------|-----------------|---------------|
| Angular | 20.3.14 | 21.0.x |
| PrimeNG | 20.3.0 | 21.0.x |
| @primeuix/themes | 1.2.5 | 2.0.2+ |
| ng-bootstrap | 19.0.1 | 20.0.0 |
| ng-select | 20.6.1 | 21.x |
| TypeScript | 5.9.2 | 5.10.x |
| Node.js | >= 22.14.0 | >= 22.14.0 |

**Date de release Angular 21 :** 20 novembre 2025

---

## Breaking Changes majeurs

### Angular 21

| Changement | Impact | Action requise |
|------------|--------|----------------|
| **Karma remplace par Vitest** | CRITIQUE | Migrer de Jest/Karma vers Vitest (ou garder Jest avec `jest-preset-angular`) |
| **Zoneless par defaut** (nouveaux projets) | Moyen | Projet existant non affecte, migration optionnelle |
| **HttpClient fourni par defaut** | Faible | `provideHttpClient()` devient optionnel |
| **ngClass deprecie** | Moyen | Migrer vers `[class.xxx]` bindings |
| **ngStyle deprecie** | Moyen | Migrer vers `[style.xxx]` bindings |

### PrimeNG 21

| Changement | Impact | Action requise |
|------------|--------|----------------|
| **Animations CSS natives** | Moyen | `showTransitionOptions` et `hideTransitionOptions` ignores |
| **Attributs PT directive** | Faible | `ptInputText` devient `pInputTextPT` (v22) |
| **contextMenuSelectionMode** | Faible | Migrer vers mode "separate" |
| **@primeuix minimum** | Critique | Mettre a jour vers v2.0.2+ |

---

## Prerequis

### Verifications avant migration

```bash
# Verifier la version de Node.js
node -v  # Doit etre >= 22.14.0

# Verifier la version de npm
npm -v  # Doit etre >= 11.0.0

# Nettoyer le cache npm
npm cache clean --force
```

### Sauvegarde du projet

```bash
# Creer une branche de migration
git checkout -b migration-angular-21
git add .
git commit -m "chore: backup before Angular 21 migration"

# Sauvegarder les fichiers critiques
cp package-lock.json package-lock.json.backup
cp package.json package.json.backup
```

---

## Etape 1 : Preparation

### 1.1 Nettoyer le projet

```bash
# Supprimer node_modules et lock file
rimraf node_modules
rimraf package-lock.json

# Nettoyer le build
npm run cleanup
```

### 1.2 Identifier les usages deprecies

```bash
# Rechercher ngClass (deprecie)
grep -r "ngClass" src/main/webapp/app/ --include="*.html"

# Rechercher ngStyle (deprecie)
grep -r "ngStyle" src/main/webapp/app/ --include="*.html"

# Rechercher les animations PrimeNG
grep -r "showTransitionOptions\|hideTransitionOptions" src/main/webapp/app/
```

---

## Etape 2 : Migration Angular 21

### 2.1 Mise a jour via ng update

```bash
# Installer temporairement pour avoir acces a ng update
npm install

# Mettre a jour Angular CLI d'abord
ng update @angular/cli@21 --force

# Mettre a jour Angular Core
ng update @angular/core@21 --force
```

### 2.2 Nouvelles fonctionnalites Angular 21

#### Signal Forms (Experimental)

```typescript
// Nouvelle API de formulaires bases sur les signaux
import { form, required, minLength } from '@angular/forms';

const myForm = form({
  name: ['', [required(), minLength(3)]],
  email: ['', [required()]]
});
```

#### HttpClient par defaut

```typescript
// Avant (Angular 20) - dans app.config.ts
provideHttpClient()  // Requis

// Apres (Angular 21) - optionnel
// HttpClient est fourni par defaut
```

#### SimpleChanges generique

```typescript
// Avant
ngOnChanges(changes: SimpleChanges) {
  if (changes['customer']) { ... }
}

// Apres - avec typage
ngOnChanges(changes: SimpleChanges<MyComponent>) {
  if (changes.customer) { ... }  // Type-safe
}
```

---

## Etape 3 : Migration PrimeNG 21

### 3.1 Mise a jour des packages

```bash
# Mettre a jour PrimeNG et ses dependances
npm install primeng@21 @primeuix/themes@^2.0.2 @primeuix/styles@^2.0.2 primeicons@^8.0.0
```

### 3.2 Migration des animations

**Avant (PrimeNG 20) :**
```html
<p-toast [showTransitionOptions]="'500ms'" [hideTransitionOptions]="'250ms'"></p-toast>
```

**Apres (PrimeNG 21) - CSS natif :**
```scss
// Dans styles.scss ou composant
.p-toast {
  --p-toast-transition-duration: 500ms;
}
```

### 3.3 Composants affectes par les animations

- Toast
- Dialog (note: le projet utilise ngbModal)
- Sidebar
- OverlayPanel
- Menu/TieredMenu/SlideMenu
- Tooltip

---

## Etape 4 : Mise a jour des dependances

### 4.1 Dependances Angular ecosystem

```bash
# ng-bootstrap (v20 pour Angular 21)
npm install @ng-bootstrap/ng-bootstrap@20

# ng-select
npm install @ng-select/ng-select@21

# ngx-translate
npm install @ngx-translate/core@18 @ngx-translate/http-loader@18

# ngx-webstorage
npm install ngx-webstorage@21

# ngx-infinite-scroll
npm install ngx-infinite-scroll@21

# ngx-spinner
npm install ngx-spinner@20
```

### 4.2 Dependances Dev

```bash
# Angular DevKit et builders
npm install -D @angular-devkit/build-angular@21 @angular/cli@21

# Angular builders (custom-webpack, jest)
npm install -D @angular-builders/custom-webpack@21 @angular-builders/jest@21

# Angular ESLint
npm install -D angular-eslint@21

# TypeScript
npm install -D typescript@5.10

# Jest preset (si on garde Jest)
npm install -D jest-preset-angular@15
```

---

## Etape 5 : Migrations automatiques

### 5.1 Migration ngClass vers class bindings

```bash
ng generate @angular/core:ngclass-to-class
```

**Avant :**
```html
<div [ngClass]="{'active': isActive, 'disabled': isDisabled}">
```

**Apres :**
```html
<div [class.active]="isActive" [class.disabled]="isDisabled">
```

### 5.2 Migration ngStyle vers style bindings

```bash
ng generate @angular/core:ngstyle-to-style
```

**Avant :**
```html
<div [ngStyle]="{'color': textColor, 'font-size': fontSize + 'px'}">
```

**Apres :**
```html
<div [style.color]="textColor" [style.font-size.px]="fontSize">
```

### 5.3 Migration tests (optionnel - si migration vers Vitest)

```bash
# Migration Jasmine/Jest vers Vitest
ng g @schematics/angular:refactor-jasmine-vitest
```

> **Note :** Le projet utilise Jest avec `jest-preset-angular`. Cette migration est optionnelle. Jest reste supporte mais deprecie (suppression prevue v22).

---

## Etape 6 : Adaptations manuelles

### 6.1 Configuration Jest (si conservation)

Le projet peut continuer a utiliser Jest avec `jest-preset-angular`. Angular 21 deprecie le support officiel de Jest mais les packages communautaires restent fonctionnels.

```json
// jest.config.js - aucun changement requis si utilisation de jest-preset-angular
```

### 6.2 Verification Tauri

```bash
# Tester le build Tauri apres migration
npm run tauri:build:debug
```

### 6.3 Fichiers a verifier manuellement

| Fichier | Verification |
|---------|--------------|
| `angular.json` | Versions des builders |
| `tsconfig.json` | Target et lib TypeScript |
| `jest.config.js` | Preset et transformers |
| `src/main/webapp/app/app.config.ts` | Providers (HttpClient optionnel) |

---

## Etape 7 : Tests et Validation

### 7.1 Commandes de validation

```bash
# Installer les dependances
npm install

# Verifier le linting
npm run lint

# Build de developpement
npm run webapp:build

# Build de production
npm run webapp:prod

# Lancer les tests
npm test

# Tester Tauri
npm run tauri:build:debug
```

### 7.2 Checklist de validation

- [ ] `npm install` sans erreurs
- [ ] `npm run lint` passe
- [ ] `npm run webapp:build` passe
- [ ] `npm run webapp:prod` passe
- [ ] `npm test` - tous les tests passent
- [ ] Application demarre correctement (`npm start`)
- [ ] Navigation fonctionne
- [ ] Composants PrimeNG s'affichent correctement
- [ ] AG Grid fonctionne
- [ ] Formulaires fonctionnent
- [ ] Modals ng-bootstrap fonctionnent
- [ ] Toast/notifications fonctionnent
- [ ] Tauri build fonctionne

---

## Package.json final

```json
{
  "name": "warehouse",
  "version": "0.0.1-SNAPSHOT",
  "private": true,
  "description": "Description for Warehouse",
  "license": "UNLICENSED",
  "scripts": {
    "build": "npm run webapp:prod --",
    "clean-www": "rimraf target/classes/static/",
    "cleanup": "rimraf target/",
    "lint": "eslint .",
    "lint:fix": "npm run lint -- --fix",
    " ": "husky",
    "prettier:check": "prettier --check \"{,src/**/,webpack/,.blueprint/**/}*.{md,json,yml,js,cjs,mjs,ts,cts,mts,java,html,css,scss}\"",
    "prettier:format": "prettier --write \"{,src/**/,webpack/,.blueprint/**/}*.{md,json,yml,js,cjs,mjs,ts,cts,mts,java,html,css,scss}\"",
    "serve": "npm run start --",
    "start": "ng serve --hmr",
    "start-tls": "npm run webapp:dev-ssl",
    "tauri": "tauri",
    "tauri:build": "npm run webapp:build:tauri && tauri build",
    "tauri:build:bundled": "npm run webapp:build:tauri && npm run tauri:prepare-sidecar && tauri build --config src-tauri/tauri.bundled.conf.json --features bundled-backend",
    "tauri:build:bundled-jre": "npm run webapp:build:tauri && npm run tauri:prepare-sidecar && tauri build --config src-tauri/tauri.bundled-jre.conf.json --features bundled-backend",
    "tauri:build:bundled-jre:debug": "npm run webapp:build:tauri && npm run tauri:prepare-sidecar && tauri build --debug --config src-tauri/tauri.bundled-jre.conf.json --features bundled-backend",
    "tauri:build:bundled-jre:fast": "npm run tauri:prepare-sidecar && tauri build --config src-tauri/tauri.bundled-jre.conf.json --features bundled-backend",
    "tauri:build:bundled:debug": "npm run webapp:build:tauri && npm run tauri:prepare-sidecar && tauri build --debug --config src-tauri/tauri.bundled.conf.json --features bundled-backend",
    "tauri:build:bundled:fast": "npm run tauri:prepare-sidecar && tauri build --config src-tauri/tauri.bundled.conf.json --features bundled-backend",
    "tauri:build:debug": "npm run webapp:build:tauri && tauri build --debug",
    "tauri:build:fast": "tauri build",
    "tauri:dev": "tauri dev",
    "tauri:prepare-sidecar": "node scripts/prepare-sidecar.js",
    "pretest": "npm run lint",
    "test": "ng test --coverage --log-heap-usage -w=2",
    "test:watch": "npm run test -- --watch",
    "webapp:build": "npm run clean-www && npm run webapp:build:dev",
    "webapp:build:dev": "ng build --configuration development",
    "webapp:build:prod": "ng build --configuration production",
    "webapp:build:tauri": "npm run clean-www && ng build --configuration tauri",
    "webapp:dev": "ng serve",
    "webapp:dev-ssl": "ng serve --ssl",
    "webapp:dev-verbose": "ng serve --verbose",
    "webapp:prod": "npm run clean-www && npm run webapp:build:prod",
    "webapp:test": "npm run test --"
  },
  "config": {
    "backend_port": "8080",
    "default_environment": "prod",
    "packaging": "jar"
  },
  "dependencies": {
    "@ag-grid-community/locale": "~34.3.1",
    "@angular/animations": "21.0.6",
    "@angular/common": "21.0.6",
    "@angular/compiler": "21.0.6",
    "@angular/core": "21.0.6",
    "@angular/forms": "21.0.6",
    "@angular/localize": "21.0.6",
    "@angular/platform-browser": "21.0.6",
    "@angular/platform-browser-dynamic": "21.0.6",
    "@angular/router": "21.0.6",
    "@fortawesome/angular-fontawesome": "3.0.0",
    "@fortawesome/fontawesome-svg-core": "7.1.0",
    "@fortawesome/free-solid-svg-icons": "7.1.0",
    "@ng-bootstrap/ng-bootstrap": "20.0.0",
    "@ng-select/ng-select": "21.0.0",
    "@ngx-translate/core": "18.0.0",
    "@ngx-translate/http-loader": "18.0.0",
    "@popperjs/core": "2.11.8",
    "@primeuix/themes": "^2.0.2",
    "@tauri-apps/api": "^2.9.0",
    "@tauri-apps/plugin-dialog": "^2.4.2",
    "@tauri-apps/plugin-fs": "^2.4.4",
    "@tauri-apps/plugin-shell": "^2.3.3",
    "ag-grid-angular": "^34.3.1",
    "ag-grid-community": "^34.3.1",
    "bootstrap": "~5.3.8",
    "bootstrap-icons": "^1.13.1",
    "bootswatch": "5.3.8",
    "chart.js": "4.5.1",
    "dayjs": "1.11.18",
    "file-saver": "^2.0.5",
    "gridstack": "^12.3.3",
    "jspdf": "^3.0.3",
    "ngx-infinite-scroll": "21.0.0",
    "ngx-spinner": "^20.0.0",
    "ngx-webstorage": "21.0.0",
    "primeicons": "^8.0.0",
    "primeng": "^21.0.2",
    "rxjs": "7.8.2",
    "tslib": "2.8.1",
    "zone.js": "0.15.1"
  },
  "devDependencies": {
    "@angular-builders/custom-webpack": "21.0.0",
    "@angular-builders/jest": "21.0.0",
    "@angular-devkit/build-angular": "21.0.6",
    "@angular/cli": "21.0.6",
    "@angular/compiler-cli": "21.0.6",
    "@angular/service-worker": "21.0.6",
    "@eslint/js": "9.25.1",
    "@tauri-apps/cli": "^2.1.0",
    "@types/file-saver": "^2.0.7",
    "@types/jest": "29.5.14",
    "@types/node": "^24.9.1",
    "angular-eslint": "21.0.0",
    "browser-sync": "^3.0.4",
    "buffer": "6.0.3",
    "copy-webpack-plugin": "13.0.0",
    "cross-env": "^10.1.0",
    "eslint": "^9.28.0",
    "eslint-config-prettier": "^10.1.8",
    "eslint-plugin-prettier": "^5.5.4",
    "folder-hash": "4.1.1",
    "globals": "16.0.0",
    "husky": "9.1.7",
    "jest": "29.7.0",
    "jest-date-mock": "^1.0.10",
    "jest-environment-jsdom": "29.7.0",
    "jest-junit": "^15.0.0",
    "jest-preset-angular": "^15.0.0",
    "merge-jsons-webpack-plugin": "2.0.1",
    "moment": "^2.30.1",
    "prettier": "3.4.2",
    "prettier-plugin-java": "2.6.7",
    "prettier-plugin-packagejson": "2.5.8",
    "rimraf": "^6.0.1",
    "ts-jest": "29.2.5",
    "typescript": "5.10.0",
    "typescript-eslint": "^8.33.1",
    "wait-on": "^8.0.2",
    "webpack": "^5.101.3",
    "webpack-bundle-analyzer": "4.10.2",
    "webpack-merge": "6.0.1",
    "webpack-notifier": "1.15.0"
  },
  "engines": {
    "node": ">=22.14.0"
  }
}
```

---

## Rollback

En cas de probleme, revenir a la version precedente :

```bash
# Restaurer les fichiers
cp package.json.backup package.json
cp package-lock.json.backup package-lock.json

# Reinstaller
rimraf node_modules
npm install

# Ou via git
git checkout package.json package-lock.json
npm install
```

---

## Resume des commandes de migration

```bash
# 1. Preparation
git checkout -b migration-angular-21
cp package.json package.json.backup
cp package-lock.json package-lock.json.backup
rimraf node_modules package-lock.json

# 2. Mise a jour Angular
npm install
ng update @angular/cli@21 --force
ng update @angular/core@21 --force

# 3. Migrations automatiques
ng generate @angular/core:ngclass-to-class
ng generate @angular/core:ngstyle-to-style

# 4. Mise a jour des autres packages
npm install primeng@21 @primeuix/themes@^2.0.2 primeicons@^8.0.0
npm install @ng-bootstrap/ng-bootstrap@20
npm install @ng-select/ng-select@21
npm install @ngx-translate/core@18 @ngx-translate/http-loader@18
npm install ngx-webstorage@21 ngx-infinite-scroll@21 ngx-spinner@20
npm install -D @angular-builders/custom-webpack@21 @angular-builders/jest@21
npm install -D angular-eslint@21 typescript@5.10 jest-preset-angular@15

# 5. Validation
npm run lint
npm run webapp:build
npm test
npm run tauri:build:debug

# 6. Commit
git add .
git commit -m "chore: migrate to Angular 21 and PrimeNG 21"
```

---

## Sources

- [Angular v21 Release](https://angular.dev/events/v21)
- [Angular Blog - Announcing Angular v21](https://blog.angular.dev/announcing-angular-v21-57946c34f14b)
- [Angular Update Guide](https://angular.dev/update-guide)
- [PrimeNG v21 Migration](https://primeng.org/migration/v21)
- [ng-bootstrap Releases](https://github.com/ng-bootstrap/ng-bootstrap/releases)
- [Angular 21 - What's New](https://angular.love/angular-21-whats-new/)
- [Angular 20 to 21 Upgrade Guide](https://dev.to/turingsoracle/angular-20-to-21-upgrade-the-practical-survival-guide-1km9)
